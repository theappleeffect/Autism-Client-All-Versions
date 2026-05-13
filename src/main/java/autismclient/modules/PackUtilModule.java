package autismclient.modules;

import autismclient.AutismClientAddon;
import autismclient.util.PackUtilClientMessaging;
import autismclient.util.PackUtilConfig;
import autismclient.util.PackUtilLANSync;
import autismclient.util.PackUtilOverlayManager;
import autismclient.util.PackUtilMacro;
import autismclient.util.PackUtilMacroManager;
import autismclient.util.PackUtilPacketLoggerOverlay;
import autismclient.util.PackUtilPacketRegistry;
import autismclient.util.PackUtilServerInfoOverlay;
import autismclient.util.PackUtilSharedState;
import autismclient.util.macro.MacroConditionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PackUtilModule {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final PackUtilModule INSTANCE = new PackUtilModule();
    private static final long PASSIVE_PAYLOAD_CAPTURE_MS = 20_000L;

    private static final Set<Class<?>> C2S_EXCLUDED_DEFAULTS = Set.of(
        net.minecraft.network.protocol.common.ServerboundKeepAlivePacket.class,
        net.minecraft.network.protocol.common.ServerboundPongPacket.class,
        net.minecraft.network.protocol.game.ServerboundClientTickEndPacket.class,
        net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket.class,
        net.minecraft.network.protocol.game.ServerboundClientCommandPacket.class,
        net.minecraft.network.protocol.game.ServerboundContainerClosePacket.class,
        net.minecraft.network.protocol.game.ServerboundSwingPacket.class,
        net.minecraft.network.protocol.game.ServerboundPlayerInputPacket.class,
        net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.class,
        net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot.class,
        net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot.class,
        net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly.class,
        net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos.class
    );

    private PackUtilConfig config;
    private boolean initialized;
    private boolean loadGuiKeyPressed;
    private boolean flushQueueKeyPressed;
    private boolean clearQueueKeyPressed;
    private boolean toggleLoggerKeyPressed;
    private boolean toggleSendKeyPressed;
    private boolean toggleDelayKeyPressed;
    private final java.util.Map<String, Boolean> macroKeyStates = new java.util.HashMap<>();
    private List<PackUtilMacro> cachedKeyboundMacros = List.of();
    private long cachedMacroKeybindRevision = -1L;
    private int autoSendTickCounter;
    private int packetLoggerTickCounter;
    private PackUtilPacketLoggerOverlay packetLoggerOverlay;
    private PackUtilServerInfoOverlay serverInfoOverlay;
    private ItemStack tooltipSizeCacheStack = ItemStack.EMPTY;
    private String tooltipSizeCacheText;
    private volatile boolean joinedPlayConnection;
    private volatile boolean spawnedInWorld;
    private volatile long passivePayloadCaptureUntilMs;

    private PackUtilModule() {
    }

    public static PackUtilModule get() {
        return INSTANCE;
    }

    public void initialize() {
        if (initialized) return;

        config = PackUtilConfig.load();
        config.applyRuntimeDefaults();
        PackUtilConfig.setGlobal(config);
        if (config.c2sPackets.isEmpty()) {
            config.c2sPackets = encodePackets(defaultC2SPackets());
        }

        applyConfigToSharedState();

        if (config.lanSyncEnabled) {
            PackUtilLANSync.getInstance().start();
        }

        initialized = true;
    }

    public void tick() {
        if (!initialized || MC == null) return;

        updateWorldSpawnState();
        PackUtilLANSync lanSync = PackUtilLANSync.getInstance();
        if (lanSync.hasTickWork()) {
            lanSync.tick();
        }
        if (MacroConditionRegistry.hasPendingConditions()) {
            MacroConditionRegistry.onTick(MC);
        }
        PackUtilSharedState.get().tickStaggeredSend();
        updatePassiveXCarryState();
        PackUtilServerInfoOverlay serverInfo = getServerDataOverlayIfExists();
        if (serverInfo != null && (serverInfo.isVisible() || serverInfo.shouldRenderBackgroundProbeBanner())) {
            serverInfo.tickBackground();
        }

        if (PackUtilSharedState.get().shouldDelayGuiPackets() && MC.getConnection() != null) {
            if (autoSendTickCounter++ >= 1) {
                autoSendTickCounter = 0;
            }
        } else {
            autoSendTickCounter = 0;
        }

        if (MC.screen == null) {
            tickKeybinds();
        } else if (config != null && config.keybindInsideGui
                && MC.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
                && !isAnyTextFieldFocused()) {
            tickKeybinds();
        } else {
            loadGuiKeyPressed = false;
            flushQueueKeyPressed = false;
            clearQueueKeyPressed = false;
            toggleLoggerKeyPressed = false;
            toggleSendKeyPressed = false;
            toggleDelayKeyPressed = false;
            macroKeyStates.clear();
        }

        packetLoggerTickCounter++;
        PackUtilPacketLoggerOverlay logger = getPacketLoggerOverlayIfExists();
        if (logger != null) {
            logger.setGameTick(packetLoggerTickCounter);
        }
    }

    public void onGameJoin() {
        joinedPlayConnection = true;
        spawnedInWorld = false;
        beginPassivePayloadCapture();
        applyRuntimePacketFlowDefaults();

        if (config.lanSyncEnabled && !PackUtilLANSync.getInstance().isRunning()) {
            PackUtilLANSync.getInstance().start();
        }

        PackUtilLANSync.getInstance().onGameJoined();
    }

    public void onGameLeft() {
        joinedPlayConnection = false;
        spawnedInWorld = false;
        passivePayloadCaptureUntilMs = 0L;
        PackUtilSharedState.get().clearRealServerVersion();
        loadGuiKeyPressed = false;
        flushQueueKeyPressed = false;
        clearQueueKeyPressed = false;
        toggleLoggerKeyPressed = false;
        toggleSendKeyPressed = false;
        toggleDelayKeyPressed = false;
    }

    public boolean isActive() {
        return initialized && spawnedInWorld && isPlayerSpawnedInWorld();
    }

    public boolean arePacketHooksActive() {
        return isActive();
    }

    public void onConfigurationConnectionStarted() {
        beginPassivePayloadCapture();
    }

    public boolean isPassivePayloadCaptureActive() {
        return System.currentTimeMillis() <= passivePayloadCaptureUntilMs;
    }

    public boolean isPacketLoggerCapturing() {
        PackUtilPacketLoggerOverlay logger = getPacketLoggerOverlayIfExists();
        return logger != null && !logger.isPaused();
    }

    public void capturePassivePayloadPacket(Packet<?> packet, String direction) {
        capturePassivePayloadPacket(packet, direction, "");
    }

    public void capturePassivePayloadPacket(Packet<?> packet, String direction, String protocolPhase) {
        if (!isPassivePayloadCaptureActive()) return;
        if (!PackUtilPacketLoggerOverlay.isPayloadPacket(packet)) return;
        autismclient.util.PackUtilPayloadSupport.rememberPayloadProtocol(packet, protocolPhase);

        PackUtilPacketLoggerOverlay logger = getPacketLoggerOverlay();
        if (logger != null) {
            logger.logPayloadPacketSilently(packet, direction);
        }
    }

    public void toggle() {
        PackUtilClientMessaging.sendPrefixed("PackUtil is always enabled in standalone mode.");
    }

    private void beginPassivePayloadCapture() {
        passivePayloadCaptureUntilMs = Math.max(passivePayloadCaptureUntilMs, System.currentTimeMillis() + PASSIVE_PAYLOAD_CAPTURE_MS);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void appendTooltip(ItemStack stack, List<?> lines) {
        if (!config.showItemIds || stack == null || stack.isEmpty()) return;

        try {
            List rawLines = lines;
            rawLines.add(Component.literal(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));

            if (MC != null && MC.player != null) {
                String sizeComponent = getTooltipSizeText(stack);
                if (sizeComponent != null && !sizeComponent.isEmpty()) {
                    rawLines.add(Component.literal(sizeComponent).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public boolean handlePacketSend(Packet<?> packet) {
        PackUtilPacketLoggerOverlay logger = getPacketLoggerOverlayIfExists();
        if (logger == null) return false;
        if (logger.isPacketBlocked(packet.getClass())) return true;
        logger.logPacket(packet, "C2S");
        return false;
    }

    public boolean handlePacketReceive(Packet<?> packet) {
        PackUtilServerInfoOverlay overlay = getServerDataOverlayIfExists();
        if (packet instanceof net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket suggestions) {
            if (overlay != null) {
                overlay.onCommandSuggestions(suggestions.id(), suggestions);
            }
        }

        if (packet instanceof net.minecraft.network.protocol.game.ClientboundCommandsPacket) {
            if (overlay != null) {
                overlay.onCommandTreeChanged();
            }
        }

        PackUtilPacketLoggerOverlay logger = getPacketLoggerOverlayIfExists();
        if (logger == null) return false;
        if (logger.isPacketBlocked(packet.getClass())) return true;
        logger.logPacket(packet, "S2C");
        return false;
    }

    public PackUtilPacketLoggerOverlay getPacketLoggerOverlay() {
        if (packetLoggerOverlay == null && MC != null && MC.font != null) {
            packetLoggerOverlay = new PackUtilPacketLoggerOverlay(MC.font);
            packetLoggerOverlay.restoreLayout();
        }
        return packetLoggerOverlay;
    }

    public PackUtilPacketLoggerOverlay getPacketLoggerOverlayIfExists() {
        return packetLoggerOverlay;
    }

    public PackUtilServerInfoOverlay getServerDataOverlay() {
        if (serverInfoOverlay == null && MC != null && MC.font != null) {
            serverInfoOverlay = new PackUtilServerInfoOverlay(MC.font);
            serverInfoOverlay.restoreState();
        }
        return serverInfoOverlay;
    }

    public PackUtilServerInfoOverlay getServerDataOverlayIfExists() {
        return serverInfoOverlay;
    }

    public boolean isNoPauseOnLostFocus() {
        return config.noPauseOnLostFocus;
    }

    public boolean isLANSyncEnabled() {
        return config.lanSyncEnabled;
    }

    public boolean isBypassResourcePack() {
        return config.pretendPackAccepted;
    }

    public boolean isInventoryMoveEnabled() {
        return config != null && config.inventoryMove;
    }

    public void setInventoryMoveEnabled(boolean value) {
        if (config == null) return;
        config.inventoryMove = value;
        saveConfig();
    }

    public boolean isXCarryEnabled() {
        return config != null && config.xCarry;
    }

    public void setXCarryEnabled(boolean value) {
        if (config == null) return;
        config.xCarry = value;
        saveConfig();
    }

    public void setBypassResourcePack(boolean value) {
        config.pretendPackAccepted = value;
        PackUtilSharedState.get().setBypassResourcePack(value);
        saveConfig();
    }

    public boolean isForceDenyResourcePack() {
        return config.autoDenyResourcePack;
    }

    public void setForceDenyResourcePack(boolean value) {
        config.autoDenyResourcePack = value;
        PackUtilSharedState.get().setResourcePackForceDeny(value);
        saveConfig();
    }

    public boolean useMsSleepMode() {
        return config.useMsSleepMode;
    }

    public int getMsSleepInterval() {
        return config.msSleepInterval;
    }

    public boolean useInstantExecutionMode() {
        return config.instantExecutionMode;
    }

    public int getActionDelayUs() {
        return config.actionDelayUs;
    }

    public boolean usePacketBurstMode() {
        return config.packetBurstMode;
    }

    public boolean shouldUseDirectFlush() {
        return config.useDirectFlush;
    }

    public boolean shouldForceChannelFlush() {
        return config.forceChannelFlush;
    }

    public boolean shouldFlushQueueOnDelayDisable() {
        return config != null && config.flushQueueOnDelayDisable;
    }

    public void setFlushQueueOnDelayDisable(boolean value) {
        if (config == null) return;
        config.flushQueueOnDelayDisable = value;
        saveConfig();
    }

    public boolean shouldUseCustomPackets() {
        return config.useCustomPackets;
    }

    public void setUseCustomPackets(boolean value) {
        config.useCustomPackets = value;
        PackUtilSharedState.get().setUseCustomPackets(value);
        saveConfig();
    }

    public void setSendGuiPackets(boolean value) {
        config.sendGuiPackets = value;
        PackUtilSharedState.get().setSendGuiPackets(value);
    }

    public boolean applySendGuiPacketsUiBehavior(boolean value) {
        setSendGuiPackets(value);
        saveConfig();
        return value;
    }

    public void setDelayGuiPackets(boolean value) {
        config.delayGuiPackets = value;
        PackUtilSharedState.get().setDelayGuiPackets(value);
    }

    public int applyDelayGuiPacketsUiBehavior(boolean value) {
        setDelayGuiPackets(value);
        saveConfig();

        if (!value && shouldFlushQueueOnDelayDisable() && MC != null && MC.getConnection() != null) {
            return PackUtilSharedState.get().flushDelayedPackets(MC.getConnection());
        }

        return 0;
    }

    public int flushQueuedPacketsUiBehavior() {
        if (MC == null || MC.getConnection() == null) return 0;
        return PackUtilSharedState.get().flushDelayedPackets(MC.getConnection());
    }

    public int clearQueuedPacketsUiBehavior() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        int count = shared.clearQueuedPackets();
        return count;
    }

    public boolean togglePacketLoggerUiBehavior() {
        PackUtilPacketLoggerOverlay overlay = getPacketLoggerOverlay();
        if (overlay == null) return false;
        overlay.toggle();
        return true;
    }

    public boolean restoreSavedScreenUiBehavior() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        if (MC == null) return false;
        if (shared.getStoredScreen() == null || shared.getStoredAbstractContainerMenu() == null) {
            return false;
        }

        MC.execute(() -> {
            MC.setScreen(shared.getStoredScreen());
            AbstractContainerMenu handler = shared.getStoredAbstractContainerMenu();
            if (MC.player != null) MC.player.containerMenu = handler;
        });
        return true;
    }

    public Set<Class<? extends Packet<?>>> getC2SPackets() {
        return new LinkedHashSet<>(PackUtilSharedState.get().getC2SPackets());
    }

    public Set<Class<? extends Packet<?>>> getS2CPackets() {
        return new LinkedHashSet<>(PackUtilSharedState.get().getS2CPackets());
    }

    public void setC2SPackets(Set<Class<? extends Packet<?>>> packets) {
        Set<Class<? extends Packet<?>>> safe = packets == null ? defaultC2SPackets() : new LinkedHashSet<>(packets);
        PackUtilSharedState.get().setC2SPackets(safe);
        config.c2sPackets = encodePackets(safe);
        saveConfig();
    }

    public void setS2CPackets(Set<Class<? extends Packet<?>>> packets) {
        Set<Class<? extends Packet<?>>> safe = packets == null ? defaultS2CPackets() : new LinkedHashSet<>(packets);
        PackUtilSharedState.get().setS2CPackets(safe);
        config.s2cPackets = encodePackets(safe);
        saveConfig();
    }

    public void resetC2SPacketsToDefault() {
        setC2SPackets(defaultC2SPackets());
    }

    public void resetS2CPacketsToDefault() {
        setS2CPackets(defaultS2CPackets());
    }

    public Set<Class<? extends Packet<?>>> defaultC2SPackets() {
        Set<Class<? extends Packet<?>>> defaults = new LinkedHashSet<>();
        for (Class<? extends Packet<?>> packetClass : PackUtilPacketRegistry.getC2SPackets()) {
            if (!C2S_EXCLUDED_DEFAULTS.contains(packetClass)) defaults.add(packetClass);
        }

        defaults.add(net.minecraft.network.protocol.game.ServerboundChatPacket.class);
        defaults.add(net.minecraft.network.protocol.game.ServerboundChatCommandPacket.class);
        defaults.add(net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket.class);
        defaults.add(net.minecraft.network.protocol.game.ServerboundSignUpdatePacket.class);
        return defaults;
    }

    public Set<Class<? extends Packet<?>>> defaultS2CPackets() {
        return new LinkedHashSet<>();
    }

    private void applyConfigToSharedState() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        applyRuntimePacketFlowDefaults();
        shared.setUseCustomPackets(config.useCustomPackets);
        shared.setC2SPackets(resolvePackets(config.c2sPackets, true));
        shared.setS2CPackets(resolvePackets(config.s2cPackets, false));
        shared.setAllowSignEditing(config.allowSignEditing);
        shared.setResourcePackForceDeny(config.autoDenyResourcePack);
        shared.setBypassResourcePack(config.pretendPackAccepted);
        shared.setStaggeredPacketSend(config.staggeredPacketSend);
        shared.setStaggeredSendDelay(config.staggeredSendDelay);
    }

    private void applyRuntimePacketFlowDefaults() {
        if (config != null) {
            config.applyRuntimeDefaults();
        }
        PackUtilSharedState shared = PackUtilSharedState.get();
        shared.setSendGuiPackets(true);
        shared.setDelayGuiPackets(false);
        shared.setStaggeredPacketSend(false);
        shared.setCaptureMode(false);
    }

    private void updatePassiveXCarryState() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        if (shared.isXCarryForced()) return;

        boolean active = false;
        if (config != null && config.xCarry && MC.player != null && MC.player.inventoryMenu != null) {
            active = autismclient.util.macro.XCarryAction.hasStoredItems(MC.player.inventoryMenu, true);
        }

        shared.setXCarryActive(active);
    }

    private void updateWorldSpawnState() {
        if (!joinedPlayConnection) {
            spawnedInWorld = false;
            return;
        }

        spawnedInWorld = isPlayerSpawnedInWorld();
    }

    private boolean isPlayerSpawnedInWorld() {
        return MC != null && MC.getConnection() != null && MC.player != null && MC.level != null;
    }

    private void saveConfig() {
        if (config == null) return;
        config.save();
    }

    private Set<Class<? extends Packet<?>>> resolvePackets(List<String> names, boolean c2s) {
        Set<Class<? extends Packet<?>>> resolved = new LinkedHashSet<>();
        if (names != null) {
            for (String name : names) {
                if (name == null || name.isBlank()) continue;
                Class<? extends Packet<?>> packetClass = PackUtilPacketRegistry.getPacket(name);
                if (packetClass == null) {
                    try {
                        Class<?> direct = Class.forName(name);
                        if (Packet.class.isAssignableFrom(direct)) {
                            @SuppressWarnings("unchecked")
                            Class<? extends Packet<?>> typed = (Class<? extends Packet<?>>) direct;
                            packetClass = typed;
                        }
                    } catch (ClassNotFoundException ignored) {
                    }
                }
                if (packetClass != null) resolved.add(packetClass);
            }
        }

        if (resolved.isEmpty()) {
            return c2s ? defaultC2SPackets() : defaultS2CPackets();
        }

        return resolved;
    }

    private List<String> encodePackets(Set<Class<? extends Packet<?>>> packets) {
        List<String> names = new ArrayList<>();
        for (Class<? extends Packet<?>> packetClass : packets) {
            String name = PackUtilPacketRegistry.getName(packetClass);
            names.add(name != null ? name : packetClass.getName());
        }
        return names;
    }

    private void tickKeybinds() {
        PackUtilConfig cfg = config;
        if (cfg == null) return;
        PackUtilSharedState shared = PackUtilSharedState.get();
        refreshKeyboundMacroCache();

        if (cfg.keybindLoadGui != -1) {
            boolean pressed = isBindPressed(cfg.keybindLoadGui);
            if (pressed && !loadGuiKeyPressed) {
                if (restoreSavedScreenUiBehavior()) {
                    PackUtilClientMessaging.sendPrefixed("GUI restored.");
                } else {
                    PackUtilClientMessaging.sendPrefixed("Ãƒâ€šÃ‚Â§cNo stored GUI.");
                }
            }
            loadGuiKeyPressed = pressed;
        }

        if (cfg.keybindFlushQueue != -1) {
            boolean pressed = isBindPressed(cfg.keybindFlushQueue);
            if (pressed && !flushQueueKeyPressed) {
                int count = flushQueuedPacketsUiBehavior();
                if (count > 0) {
                    if (shared.isStaggering()) shared.setPendingQueueCompletionMessage("Sent " + count + " packets.");
                    else PackUtilClientMessaging.sendPrefixed("Sent " + count + " packets.");
                } else {
                    PackUtilClientMessaging.sendPrefixed("Queue empty.");
                }
            }
            flushQueueKeyPressed = pressed;
        }

        if (cfg.keybindClearQueue != -1) {
            boolean pressed = isBindPressed(cfg.keybindClearQueue);
            if (pressed && !clearQueueKeyPressed) {
                int count = clearQueuedPacketsUiBehavior();
                PackUtilClientMessaging.sendPrefixed(count > 0 ? "Cleared " + count + " packets." : "Queue empty.");
            }
            clearQueueKeyPressed = pressed;
        }

        if (cfg.keybindToggleLogger != -1) {
            boolean pressed = isBindPressed(cfg.keybindToggleLogger);
            if (pressed && !toggleLoggerKeyPressed) {
                togglePacketLoggerUiBehavior();
            }
            toggleLoggerKeyPressed = pressed;
        }

        if (cfg.keybindToggleSend != -1) {
            boolean pressed = isBindPressed(cfg.keybindToggleSend);
            if (pressed && !toggleSendKeyPressed) {
                boolean newValue = !shared.shouldSendGuiPackets();
                applySendGuiPacketsUiBehavior(newValue);
                PackUtilClientMessaging.sendPrefixed("GUI packets: " + (newValue ? "enabled" : "disabled"));
            }
            toggleSendKeyPressed = pressed;
        }

        if (cfg.keybindToggleDelay != -1) {
            boolean pressed = isBindPressed(cfg.keybindToggleDelay);
            if (pressed && !toggleDelayKeyPressed) {
                boolean newValue = !shared.shouldDelayGuiPackets();
                int sent = applyDelayGuiPacketsUiBehavior(newValue);
                if (newValue) {
                    PackUtilClientMessaging.sendPrefixed("Packet delay: enabled");
                } else if (sent > 0) {
                    String completionMessage = "Packet delay: disabled. Sent " + sent + " queued packets.";
                    if (shared.isStaggering()) shared.setPendingQueueCompletionMessage(completionMessage);
                    else PackUtilClientMessaging.sendPrefixed(completionMessage);
                } else if (!shouldFlushQueueOnDelayDisable()
                    && (!shared.getDelayedPackets().isEmpty() || !shared.getStaggeredQueue().isEmpty())) {
                    PackUtilClientMessaging.sendPrefixed("Packet delay: disabled. Queue kept.");
                } else {
                    PackUtilClientMessaging.sendPrefixed("Packet delay: disabled. Queue empty.");
                }
            }
            toggleDelayKeyPressed = pressed;
        }

        for (PackUtilMacro macro : cachedKeyboundMacros) {
            boolean pressed = isBindPressed(macro.keyCode);
            boolean wasPressed = macroKeyStates.getOrDefault(macro.name, false);
            if (pressed && !wasPressed) {
                if (autismclient.util.macro.MacroExecutor.isRunning()) {
                    autismclient.util.macro.MacroExecutor.stop();
                } else {
                    macro.execute();
                }
            }
            macroKeyStates.put(macro.name, pressed);
        }
    }

    private void refreshKeyboundMacroCache() {
        PackUtilMacroManager macroManager = PackUtilMacroManager.get();
        long revision = macroManager.getRevision();
        if (revision == cachedMacroKeybindRevision) return;

        List<PackUtilMacro> keyboundMacros = new ArrayList<>();
        Set<String> activeMacroNames = new HashSet<>();
        for (PackUtilMacro macro : macroManager.getAll()) {
            if (macro == null || macro.keyCode == -1) continue;
            keyboundMacros.add(macro);
            activeMacroNames.add(macro.name);
        }

        macroKeyStates.keySet().removeIf(name -> !activeMacroNames.contains(name));
        cachedKeyboundMacros = keyboundMacros;
        cachedMacroKeybindRevision = revision;
    }

    private String getTooltipSizeText(ItemStack stack) {
        if (!tooltipSizeCacheStack.isEmpty()
            && tooltipSizeCacheStack.getCount() == stack.getCount()
            && ItemStack.isSameItemSameComponents(tooltipSizeCacheStack, stack)) {
            return tooltipSizeCacheText;
        }

        try {
            CompoundTag nbt = (CompoundTag) ItemStack.CODEC.encodeStart(
                MC.player.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack
            ).getOrThrow();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.write(nbt, new DataOutputStream(baos));
            int bytes = baos.size();
            String sizeStr = bytes >= 1024 ? String.format("%.1f kB", bytes / 1024.0) : bytes + " B";

            tooltipSizeCacheStack = stack.copy();
            tooltipSizeCacheText = sizeStr + " data";
            return tooltipSizeCacheText;
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isAnyTextFieldFocused() {
        return PackUtilOverlayManager.get().isAnyTextFieldFocused();
    }

    private boolean isBindPressed(int bindCode) {
        return autismclient.util.PackUtilBindUtil.isBindPressed(MC, bindCode);
    }

    private void restoreSavedScreen() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        if (MC == null) return;

        if (shared.getStoredScreen() == null || shared.getStoredAbstractContainerMenu() == null) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cNo stored GUI.");
            return;
        }

        MC.setScreen(shared.getStoredScreen());
        AbstractContainerMenu handler = shared.getStoredAbstractContainerMenu();
        if (MC.player != null) MC.player.containerMenu = handler;
        PackUtilClientMessaging.sendPrefixed("GUI restored.");
    }
}
