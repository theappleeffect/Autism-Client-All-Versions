package autismclient.util;

import autismclient.gui.packui.PackUiAssets;
import autismclient.gui.packui.PackUiControlGlyphs;
import autismclient.gui.packui.PackUiLegacyLayout;
import autismclient.gui.packui.PackUiOverlayButton;
import autismclient.gui.packui.PackUiScrollbar;
import autismclient.gui.packui.PackUiSmoothScroll;
import autismclient.gui.packui.PackUiSizing;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class PackUtilPacketLoggerOverlay extends PackUtilOverlayBase {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private static final int HEADER_H = HEADER_HEIGHT;
    private static final int GROUP_THRESHOLD = 10;
    private static final int DEFAULT_PANEL_WIDTH = 323;
    private static final int DEFAULT_PANEL_HEIGHT = 250;

    private static final int CAP_ALL = 800;
    private static final int CAP_INVENTORY = 400;
    private static final int CAP_MOVEMENT = 200;
    private static final int CAP_PAYLOAD = 200;
    private static final long UI_FLUSH_INTERVAL_MS = 500L;

    private static final Set<String> IGNORED_PACKET_KEYS = createIgnoredPacketKeys();

    public enum Category {
        ALL("All"), INVENTORY("INV"), MOVEMENT("Move"), PAYLOAD("Payload");
        public final String label;
        Category(String l) { this.label = l; }
    }

    private static final Set<String> INVENTORY_NAMES = new HashSet<>(Arrays.asList(
        "ClickSlot", "CloseAbstractContainerScreen", "OpenScreen", "AbstractContainerMenu",
        "CreativeInventoryAction", "PickFromInventory", "PlayerAction",
        "InventoryS2C", "AbstractContainerMenuSlotUpdate", "AbstractContainerMenuProperty",
        "SetTradeOffers", "OpenHorseScreen", "CraftRequest",
        "ButtonClick", "RecipeBookData", "UpdateSelectedSlot",
        "HandSwing", "PlayerInteractBlock", "PlayerInteractItem",
        "PlayerInteractEntity", "ItemPickupAnimation",
        "ContainerClick", "ContainerClose", "ContainerSetContent", "ContainerSetData",
        "ContainerSetSlot", "ContainerButtonClick", "SetCreativeModeSlot",
        "SetCarriedItem", "Swing", "UseItem", "UseItemOn", "Interact",
        "SetCursorItem", "SetPlayerInventory", "TakeItemEntity", "MerchantOffers"
    ));

    private static final Set<String> MOVEMENT_NAMES = new HashSet<>(Arrays.asList(
        "PlayerMove", "PlayerMoveFull", "PlayerMovePositionAndOnGround",
        "PlayerMoveLookAndOnGround", "PlayerMoveOnGroundOnly",
        "EntityPosition", "EntityPositionSync", "EntitySetHead",
        "EntityVelocityUpdate", "VehicleMove",
        "MoveRelative", "PacketMoveRelative", "RotateRelative",
        "PacketRotateRelative", "EntityPacketRotate",
        "EntityMoveRelative", "EntityRotate", "TeleportConfirm",
        "ClientTickEnd",
        "ServerboundMovePlayer", "ServerboundMoveVehicle", "ServerboundPlayerInput",
        "ServerboundAcceptTeleportation", "ClientboundMoveEntity", "ClientboundMoveVehicle",
        "ClientboundMoveMinecart", "ClientboundPlayerPosition", "ClientboundPlayerRotation",
        "ClientboundEntityPositionSync", "ClientboundSetEntityMotion", "ClientboundRotateHead",
        "ClientboundTeleportEntity"
    ));

    private static final Set<String> PAYLOAD_NAMES = new HashSet<>(Arrays.asList(
        "CustomPacketPayload",
        "CustomPacketPayloadC2S",
        "CustomPacketPayloadS2C",
        "BrandPayload",
        "PluginMessage",
        "CustomPayload",
        "S2CPayload",
        "DiscardedPayload"
    ));

    private final Font textRenderer;
    private final PackUtilPacketInspectOverlay inspectOverlay;
    private final PackUiTheme theme = new PackUiTheme();
    private final PackUtilContextMenu<LogEntry> ctxMenu;
    private int PANEL_WIDTH = DEFAULT_PANEL_WIDTH;
    private int PANEL_HEIGHT = DEFAULT_PANEL_HEIGHT;
    private int currentPanelHeight = DEFAULT_PANEL_HEIGHT;
    private boolean paused = true;
    private boolean isDragging;
    private boolean isResizing;
    private double dragOffX, dragOffY;
    private double headerPressMouseX;
    private double headerPressMouseY;
    private int headerPressPanelX;
    private int headerPressPanelY;
    private boolean headerDragMoved;
    private double resizeStartMouseX, resizeStartMouseY;
    private int resizeStartWidth, resizeStartHeight;
    private int scrollOffset;
    private final PackUiSmoothScroll contentScrollState = new PackUiSmoothScroll();
    private boolean scrollbarDragging;
    private int scrollbarGrabOffset;

    private String searchFilter = "";
    private final PackUtilChatField searchField;
    private Category activeTab = Category.ALL;
    private boolean groupingEnabled = true;
    private int dirFilter = 0;

    private final Set<Class<?>> blockedClasses = new LinkedHashSet<>();
    private boolean blockedExpanded;

    private final List<LogEntry> bufAll = new CopyOnWriteArrayList<>();
    private final List<LogEntry> bufInventory = new CopyOnWriteArrayList<>();
    private final List<LogEntry> bufMovement = new CopyOnWriteArrayList<>();
    private final List<LogEntry> bufPayload = new CopyOnWriteArrayList<>();
    private final List<LogEntry> pendingEntries = new ArrayList<>();

    private int gameTick;
    private long lastUiFlushMs;

    private List<DisplayRow> displayRows = new ArrayList<>();
    private boolean dirty = true;

    private final Set<String> expandedGroups = new HashSet<>();

    public PackUtilPacketLoggerOverlay(Font textRenderer) {
        super("PackUtilPacketLoggerOverlay", DEFAULT_PANEL_WIDTH, DEFAULT_PANEL_HEIGHT);
        this.textRenderer = textRenderer;
        this.PANEL_WIDTH = defaultPanelWidth();
        this.PANEL_HEIGHT = defaultPanelHeight();
        this.currentPanelHeight = this.PANEL_HEIGHT;
        this.panelX = 200;
        this.panelY = 40;
        this.searchField = new PackUtilChatField(MC, textRenderer, 0, 0, searchFieldWidth(), filterRowHeight(), false);
        this.searchField.setPlaceholder(Component.literal("Search..."));
        this.searchField.setMaxLength(160);
        this.searchField.setChangedListener(value -> {
            searchFilter = value == null ? "" : value;
            dirty = true;
        });
        this.inspectOverlay = new PackUtilPacketInspectOverlay(textRenderer);
        this.ctxMenu = new PackUtilContextMenu<>(theme, textRenderer, this::getCtxItems, lineHeight());
        setContextMenu(ctxMenu);
        PackUtilOverlayManager.get().register(this.inspectOverlay);
    }

    @Override
    public int getMinWidth() {
        return defaultPanelWidth();
    }

    @Override
    public int getMinHeight() {
        return defaultPanelHeight();
    }

    @Override
    public PackUtilWindowLayout getBounds() {
        return new PackUtilWindowLayout(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, visible, collapsed);
    }

    @Override
    public void setBounds(PackUtilWindowLayout bounds) {
        if (bounds == null) return;
        PackUtilWindowLayout clamped = clampToScreen(this, bounds);
        panelX = clamped.x;
        panelY = clamped.y;
        PANEL_WIDTH = Math.max(getMinWidth(), clamped.width);
        PANEL_HEIGHT = Math.max(getMinHeight(), clamped.height);
        visible = clamped.visible;
        collapsed = clamped.collapsed;
    }

    public void setGameTick(int t) { gameTick = t; }
    public boolean isPaused() { return paused; }
    public synchronized void setPaused(boolean paused) {
        if (this.paused == paused) return;
        if (paused) {
            flushPendingLocked();
        } else {

            lastUiFlushMs = System.currentTimeMillis();
        }
        this.paused = paused;
        dirty = true;
    }

    public synchronized void logPacket(Packet<?> packet, String direction) {
        logPacket(packet, direction, false, false);
    }

    public synchronized void logPayloadPacketSilently(Packet<?> packet, String direction) {
        logPacket(packet, direction, true, true);
    }

    private void logPacket(Packet<?> packet, String direction, boolean ignorePaused, boolean payloadOnly) {
        if (packet == null) return;
        if (!ignorePaused && paused) return;
        Class<?> cls = packet.getClass();
        if (blockedClasses.contains(cls)) return;

        String name = PackUtilPacketNamer.getFriendlyName(packet, direction);
        if (shouldIgnorePacket(cls, name)) return;
        boolean isInventory = matchesAny(name, INVENTORY_NAMES);
        boolean isMovement = matchesAny(name, MOVEMENT_NAMES);
        boolean isPayload = isPayloadPacket(cls, name);
        if (payloadOnly && !isPayload) return;
        LogCaptureContext captureContext = captureLogContext(packet);
        PackUtilPayloadSupport.PayloadSnapshot payloadSnapshot = isPayload ? PackUtilPayloadSupport.snapshot(packet, direction) : null;

        LogEntry e = new LogEntry(System.currentTimeMillis(), gameTick, direction, name, cls, packet, isInventory, isMovement, isPayload,
            captureContext.blockStateSummary(), captureContext.screenSummary(), payloadSnapshot);
        pendingEntries.add(e);
        maybeFlushPendingLocked(false);
    }

    private static LogCaptureContext captureLogContext(Packet<?> packet) {
        if (packet == null || MC == null || !MC.isSameThread()) return LogCaptureContext.EMPTY;
        try {
            if (packet instanceof ServerboundPlayerActionPacket actionPacket) {
                return new LogCaptureContext(snapshotBlockState(actionPacket.getPos()), null);
            }
            if (packet instanceof ServerboundUseItemOnPacket interactBlockPacket) {
                return new LogCaptureContext(snapshotBlockState(interactBlockPacket.getHitResult().getBlockPos()), null);
            }
            if (packet instanceof ServerboundContainerClosePacket) {
                return new LogCaptureContext(null, snapshotCurrentScreen());
            }
        } catch (Throwable ignored) {
        }
        return LogCaptureContext.EMPTY;
    }

    private static String snapshotBlockState(BlockPos pos) {
        if (pos == null || MC.level == null) return null;
        try {
            BlockState state = MC.level.getBlockState(pos);
            if (state == null) return null;
            return BuiltInRegistries.BLOCK.getKey(state.getBlock()) + " " + state;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String snapshotCurrentScreen() {
        if (MC.screen == null) return null;
        try {
            String title = MC.screen.getTitle() == null ? "" : MC.screen.getTitle().getString().trim();
            String className = MC.screen.getClass().getSimpleName();
            if (title.isEmpty()) return className;
            if (className == null || className.isBlank()) return title;
            return title + " [" + className + "]";
        } catch (Throwable ignored) {
            return null;
        }
    }

    public boolean isPacketBlocked(Class<?> cls) { return blockedClasses.contains(cls); }
    public Set<Class<?>> getBlockedPacketClasses() { return blockedClasses; }

    private static boolean matchesAny(String name, Set<String> set) {
        for (String s : set) { if (name.contains(s)) return true; }
        return false;
    }

    private static boolean isPayloadPacket(Class<?> cls, String name) {
        if (matchesAny(name, PAYLOAD_NAMES)) return true;
        if (cls == null) return false;
        String simpleName = cls.getSimpleName();
        if (matchesAny(simpleName, PAYLOAD_NAMES)) return true;
        String className = cls.getName();
        return matchesAny(className, PAYLOAD_NAMES);
    }

    private static Set<String> createIgnoredPacketKeys() {
        Set<String> keys = new HashSet<>();
        registerIgnoredPackets(keys,

            "ClientTickEndC2SPacket",
            "ClientTickEndS2CPacket",
            "BundleS2CPacket",
            "LightUpdateS2CPacket",
            "CommonPongC2SPacket",
            "CommonPingS2CPacket",
            "KeepAliveC2SPacket",
            "KeepAliveS2CPacket",
            "EntityPositionS2CPacket",
            "PlayerListHeaderS2CPacket",
            "TeamS2CPacket",
            "EntityPositionSyncS2CPacket",
            "EntityS2CPacket.MoveRelative",
            "EntityVelocityUpdateS2CPacket",
            "EntityS2CPacket.RotateAndMoveRelative",
            "EntitySetHeadYawS2CPacket",
            "EntityTrackerUpdateS2CPacket",
            "PlayerMoveC2SPacket.LookAndOnGround",
            "PlayerMoveC2SPacket.PositionAndOnGround",
            "EntitiesDestroyS2CPacket",
            "EntityStatusS2CPacket",
            "EntitySpawnS2CPacket",
            "EntityDamageS2CPacket",
            "EntityAnimationS2CPacket",
            "EntityAttributesS2CPacket",
            "PlaySoundS2CPacket",
            "BossBarS2CPacket",
            "ParticleS2CPacket",
            "WorldEventS2CPacket",
            "WorldTimeUpdateS2CPacket",
            "OverlayMessageS2CPacket",
            "ScoreboardObjectiveUpdateS2CPacket",
            "BlockUpdateS2CPacket",
            "EntityS2CPacket.Rotate",
            "ChunkDeltaUpdateS2CPacket",
            "GameStateChangeS2CPacket",
            "ChunkDataS2CPacket",
            "UnloadChunkS2CPacket",
            "WaypointS2CPacket",
            "PlayerMoveC2SPacket.Full",
            "PlayerListS2CPacket",
            "AcknowledgeChunksC2SPacket",
            "ChunkSentS2CPacket",
            "StartChunkSendS2CPacket",
            "ChunkRenderDistanceCenterS2CPacket",
            "ClientCommandC2SPacket",
            "PlayerInputC2SPacket",

            "ServerboundClientTickEndPacket",
            "ServerboundPongPacket",
            "ServerboundKeepAlivePacket",
            "ServerboundMovePlayerPacket.Pos",
            "ServerboundMovePlayerPacket.PosRot",
            "ServerboundMovePlayerPacket.Rot",
            "ServerboundMovePlayerPacket.StatusOnly",
            "ServerboundAcceptTeleportationPacket",
            "ServerboundChunkBatchReceivedPacket",
            "ServerboundClientCommandPacket",
            "ServerboundPlayerInputPacket",
            "ClientboundBundlePacket",
            "ClientboundBundleDelimiterPacket",
            "ClientboundLightUpdatePacket",
            "ClientboundPingPacket",
            "ClientboundKeepAlivePacket",
            "ClientboundEntityPositionSyncPacket",
            "ClientboundMoveEntityPacket.Pos",
            "ClientboundMoveEntityPacket.PosRot",
            "ClientboundMoveEntityPacket.Rot",
            "ClientboundSetEntityMotionPacket",
            "ClientboundRotateHeadPacket",
            "ClientboundSetEntityDataPacket",
            "ClientboundRemoveEntitiesPacket",
            "ClientboundEntityEventPacket",
            "ClientboundAddEntityPacket",
            "ClientboundDamageEventPacket",
            "ClientboundAnimatePacket",
            "ClientboundUpdateAttributesPacket",
            "ClientboundSoundPacket",
            "ClientboundSoundEntityPacket",
            "ClientboundBossEventPacket",
            "ClientboundLevelParticlesPacket",
            "ClientboundLevelEventPacket",
            "ClientboundMapItemDataPacket",
            "ClientboundBlockEntityDataPacket",
            "ClientboundContainerSetSlotPacket",
            "ClientboundSetTimePacket",
            "ClientboundSetActionBarTextPacket",
            "ClientboundSetObjectivePacket",
            "ClientboundBlockUpdatePacket",
            "ClientboundSectionBlocksUpdatePacket",
            "ClientboundGameEventPacket",
            "ClientboundLevelChunkWithLightPacket",
            "ClientboundForgetLevelChunkPacket",
            "ClientboundTrackedWaypointPacket",
            "ClientboundPlayerInfoUpdatePacket",
            "ClientboundPlayerInfoRemovePacket",
            "ClientboundChunkBatchFinishedPacket",
            "ClientboundChunkBatchStartPacket",
            "ClientboundSetChunkCacheCenterPacket",
            "ClientboundTabListPacket",
            "ClientboundSetPlayerTeamPacket",
            "ClientboundTickingStatePacket"
        );
        return Collections.unmodifiableSet(keys);
    }

    private static void registerIgnoredPackets(Set<String> keys, String... names) {
        for (String name : names) {
            registerIgnoredPacket(keys, name);
        }
    }

    private static void registerIgnoredPacket(Set<String> keys, String name) {
        if (name == null || name.isBlank()) return;
        keys.add(normalizePacketKey(name));
        if (name.endsWith("Packet")) {
            keys.add(normalizePacketKey(name.substring(0, name.length() - 6)));
        }
    }

    private static String normalizePacketKey(String value) {
        if (value == null || value.isEmpty()) return "";
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    private static boolean shouldIgnorePacket(Class<?> cls, String friendlyName) {
        String normalizedFriendly = normalizePacketKey(friendlyName);
        String normalizedSimple = normalizePacketKey(cls.getSimpleName());
        String normalizedClass = normalizePacketKey(cls.getName());
        String normalizedResolvedClass = "";
        if (Packet.class.isAssignableFrom(cls)) {
            @SuppressWarnings("unchecked")
            Class<? extends Packet<?>> packetClass = (Class<? extends Packet<?>>) cls;
            normalizedResolvedClass = normalizePacketKey(PackUtilPacketNamer.getFriendlyName(packetClass));
        }
        return IGNORED_PACKET_KEYS.contains(normalizedFriendly)
            || IGNORED_PACKET_KEYS.contains(normalizedResolvedClass)
            || IGNORED_PACKET_KEYS.contains(normalizedSimple)
            || IGNORED_PACKET_KEYS.contains(normalizedClass);
    }

    public static boolean isPayloadPacket(Packet<?> packet) {
        if (packet == null) return false;
        Class<?> cls = packet.getClass();
        return isPayloadPacket(cls, PackUtilPacketNamer.getFriendlyName(packet, ""));
    }

    private static void addCapped(List<LogEntry> buf, LogEntry e, int cap) {
        buf.add(e);
        int excess = buf.size() - cap;
        if (excess > 0) {

            buf.subList(0, excess).clear();
        }
    }

    private void maybeFlushPending() {
        synchronized (this) {
            maybeFlushPendingLocked(false);
        }
    }

    private void maybeFlushPendingLocked(boolean force) {
        long now = System.currentTimeMillis();
        if (!force) {
            if (pendingEntries.isEmpty()) return;
            if (lastUiFlushMs != 0L && now - lastUiFlushMs < UI_FLUSH_INTERVAL_MS) return;
        }
        flushPendingLocked();
        lastUiFlushMs = now;
    }

    private void flushPending() {
        synchronized (this) {
            flushPendingLocked();
            lastUiFlushMs = System.currentTimeMillis();
        }
    }

    private void flushPendingLocked() {
        if (pendingEntries.isEmpty()) return;

        for (LogEntry e : pendingEntries) {
            addCapped(bufAll, e, CAP_ALL);
            if (e.isInventory) addCapped(bufInventory, e, CAP_INVENTORY);
            if (e.isMovement) addCapped(bufMovement, e, CAP_MOVEMENT);
            if (e.isPayload) addCapped(bufPayload, e, CAP_PAYLOAD);
        }

        pendingEntries.clear();
        dirty = true;
    }

    @Override public void setVisible(boolean v) {
        visible = v;
        if (v) { scrollOffset = 0; contentScrollState.jumpTo(0, 0); dirty = true; ctxMenu.close(); PackUtilOverlayManager.get().bringToFront(this); }
        else inspectOverlay.close();
        saveLayout();
    }
    public void toggle() { setVisible(!visible); }
    @Override public boolean isVisible() { return visible; }
    @Override public boolean isCollapsed() { return collapsed; }
    @Override public void setCollapsed(boolean c) {
        collapsed = c;
        isDragging = false;
        isResizing = false;
        headerDragMoved = false;
        scrollbarDragging = false;
    }
    @Override public boolean usesSharedHeaderClickCollapse() { return true; }
    @Override public boolean hasTextFieldFocused() { return searchField != null && searchField.isFocused(); }
    @Override public void clearTextFieldFocus() { if (searchField != null) searchField.setFocused(false); }
    @Override public int getZLevel() { return 10; }

    private void drawUiText(GuiGraphics context, String text, PackUiTone tone, int color, int x, int y) {
        PackUiText.draw(context, textRenderer, text, theme.fontFor(tone), color, x, y, false);
    }

    @Override public boolean isMouseOver(double mx, double my) {
        if (!visible) return false;
        int panelHeight = collapsed ? HEADER_H : currentPanelHeight;
        PackUtilWindowLayout bounds = new PackUtilWindowLayout(panelX, panelY, PANEL_WIDTH, panelHeight, visible, collapsed);
        int h = getRenderedFrameHeight(bounds, collapsed);
        boolean overPanel = mx >= panelX && mx <= panelX + PANEL_WIDTH && my >= panelY && my <= panelY + h;
        overPanel |= ctxMenu.isMouseOver(mx, my);
        return overPanel;
    }

    @Override public boolean isOverDragBar(double mx, double my) {
        if (!visible) return false;
        return mx >= panelX && mx <= panelX + PANEL_WIDTH
            && my >= panelY && my <= panelY + HEADER_H
            && !isOverWindowControl(mx, my, getBounds());
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        if (!visible) return;
        maybeFlushPending();
        if (dirty) { rebuildDisplay(); dirty = false; }

        PackUtilWindowLayout clamped = clampToScreen(this, new PackUtilWindowLayout(panelX, panelY, PANEL_WIDTH, calcPanelH(), visible, collapsed));
        panelX = clamped.x;
        panelY = clamped.y;
        PANEL_WIDTH = clamped.width;
        currentPanelHeight = clamped.height;
        int ph = currentPanelHeight;

        int total = 0;
        for (DisplayRow r : displayRows) total += (r.type == RowType.GROUP) ? r.groupCount : 1;

        int bodyMx = mx;
        int bodyMy = my;
        if (ctxMenu.isMouseOver(mx, my)) {
            bodyMx = PackUtilOverlayManager.HOVER_BLOCKED_MOUSE;
            bodyMy = PackUtilOverlayManager.HOVER_BLOCKED_MOUSE;
        }

        String title = "Packet Logger";
        PackUtilWindowLayout bounds = new PackUtilWindowLayout(panelX, panelY, PANEL_WIDTH, ph, visible, collapsed);
        renderWindowFrame(ctx, bodyMx, bodyMy, bounds, title, collapsed, isDragging);
        boolean clipBody = beginWindowBodyClip(ctx, bounds, collapsed);
        if (!clipBody) {
            renderWindowInactiveOverlay(ctx, bounds, collapsed, isDragging);
            return;
        }

        try {

            int tabY = panelY + HEADER_H + 2;
            renderTabs(ctx, bodyMx, bodyMy, tabY, total);

            int filterY = tabY + tabHeight() + 2;
            renderFilterBar(ctx, bodyMx, bodyMy, filterY);

            int contentY = filterY + filterHeight() + 2;
            int contentEndY = contentY + contentAreaHeight();

            if (displayRows.isEmpty()) {
                drawUiText(ctx, "No packets matching filters", PackUiTone.MUTED, PackUtilColors.textDim(), panelX + 10, contentY + 6);
            } else {
                int contentHeight = displayRows.size() * lineHeight();
                int viewHeight = contentAreaHeight();
                int maxScroll = Math.max(0, contentHeight - viewHeight);
                scrollOffset = quantizeScrollOffset(scrollOffset, lineHeight(), maxScroll);
                contentScrollState.setTarget(scrollOffset, maxScroll);
                int drawScroll = contentScrollState.tick(delta, maxScroll);
                PackUtilUiScale.enableOverlayScissor(ctx, panelX, contentY, panelX + PANEL_WIDTH, contentEndY);
                int drawBase = contentY - drawScroll;
                for (int i = 0; i < displayRows.size(); i++) {
                    int ey = drawBase + i * lineHeight();
                    if (ey + lineHeight() > contentY && ey < contentEndY) {
                        DisplayRow row = displayRows.get(i);
                        if (row.type == RowType.GROUP) renderGroup(ctx, row, ey, bodyMx, bodyMy);
                        else renderEntry(ctx, row.entry, ey, bodyMx, bodyMy);
                    }
                }
                ctx.disableScissor();
                PackUiScrollbar.Metrics scrollbarMetrics = getContentScrollbarMetrics();
                PackUiScrollbar.draw(ctx, scrollbarMetrics, scrollbarMetrics.contains(bodyMx, bodyMy), scrollbarDragging);
            }

            if (!blockedClasses.isEmpty()) renderBlocked(ctx, bodyMx, bodyMy, contentEndY, panelY + ph - 2);

        } finally {
            endWindowBodyClip(ctx, clipBody);
            renderWindowInactiveOverlay(ctx, bounds, collapsed, isDragging);
        }

        if (ctxMenu.isOpen()) {

            ctx.nextStratum();
            PackUiText.interOverlayFlush(ctx);
            ctxMenu.render(ctx, mx, my);
        }
    }

    private void renderTabs(GuiGraphics ctx, int mx, int my, int y, int total) {
        int x = panelX + 4;
        for (Category cat : Category.values()) {
            int w = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, cat.label, 5, 32, 64);
            if (activeTab == cat) {
                drawOverlayButton(ctx, x, y, w, tabHeight(), cat.label, PackUiOverlayButton.Variant.PRIMARY, true, mx, my);
            } else {
                drawOverlayButton(ctx, x, y, w, tabHeight(), cat.label, PackUiOverlayButton.Variant.GHOST, true, mx, my);
            }
            x += w + 2;
        }

        String summary = paused ? ("Paused  " + total) : Integer.toString(total);
        int summaryColor = paused ? theme.color(PackUiTone.MUTED) : PackUtilColors.textSecondary();
        int summaryWidth = PackUiText.width(textRenderer, summary, theme.fontFor(PackUiTone.MUTED), summaryColor);
        int summaryX = panelX + PANEL_WIDTH - 6 - summaryWidth;
        int minSummaryX = x + 4;
        if (summaryX >= minSummaryX) {
            int summaryY = PackUiSizing.alignTextY(y, tabHeight(), theme.fontHeight(PackUiTone.MUTED), theme.bodyTextNudge());
            drawUiText(ctx, summary, PackUiTone.MUTED, summaryColor, summaryX, summaryY);
        }
    }

    private int contentAreaY() {
        return panelY + HEADER_H + 2 + tabHeight() + 2 + filterHeight() + 2;
    }

    private int contentAreaHeight() {
        int rawHeight = Math.max(0, currentPanelHeight - HEADER_H - tabHeight() - filterHeight() - 8 - blockedH());
        return alignViewportHeight(rawHeight, lineHeight());
    }

    private PackUiScrollbar.Metrics getContentScrollbarMetrics() {
        int contentHeight = displayRows.size() * lineHeight();
        int viewHeight = contentAreaHeight();
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        return PackUiScrollbar.compute(contentHeight, viewHeight, panelX + PANEL_WIDTH - 5, contentAreaY(), 3, viewHeight, contentScrollState.tick(0.0f, maxScroll));
    }

    private void renderFilterBar(GuiGraphics ctx, int mx, int my, int y) {
        int gap = 2;
        int row1Y = y;
        int row2Y = y + filterRowHeight() + filterRowGap();

        int x = panelX + 4;
        String captureLabel = paused ? "Start" : "Stop";
        int captureW = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, captureLabel, 5, 38, 54);
        if (paused) {
            drawOverlayButton(ctx, x, row1Y, captureW, filterRowHeight(), captureLabel, PackUiOverlayButton.Variant.SUCCESS, true, mx, my);
        } else {
            drawOverlayButton(ctx, x, row1Y, captureW, filterRowHeight(), captureLabel, PackUiOverlayButton.Variant.DANGER, true, mx, my);
        }
        x += captureW + gap;

        int clearW = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, "Clear", 5, 34, 54);
        drawOverlayButton(ctx, x, row1Y, clearW, filterRowHeight(), "Clear", PackUiOverlayButton.Variant.GHOST, true, mx, my);
        x += clearW + gap;
        int copyW = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, "Copy", 5, 34, 52);
        drawOverlayButton(ctx, x, row1Y, copyW, filterRowHeight(), "Copy", PackUiOverlayButton.Variant.GHOST, true, mx, my);
        x += copyW + gap;
        String grpLabel = groupingEnabled ? "Group" : "Ungrp";
        int groupW = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, grpLabel, 5, 38, 58);
        drawOverlayButton(ctx, x, row1Y, groupW, filterRowHeight(), grpLabel, PackUiOverlayButton.Variant.GHOST, true, mx, my);
        x += groupW + gap;
        if (!blockedClasses.isEmpty()) {
            String bt = blockedClasses.size() + "blk";
            int blockedW = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, bt, 5, 34, 52);
            drawOverlayButton(ctx, x, row1Y, blockedW, filterRowHeight(), bt, PackUiOverlayButton.Variant.GHOST, true, mx, my);
        }

        x = panelX + 4;
        int sw = searchFieldWidth();
        searchField.setX(x);
        searchField.setY(row2Y);
        searchField.setWidth(sw);
        searchField.setHeight(filterRowHeight());
        if (!Objects.equals(searchField.getText(), searchFilter)) {
            searchField.setText(searchFilter);
        }
        searchField.render(ctx, mx, my, 0.0f);
        x += sw + 3;

        String[] dirLabels = {"Both", "C2S", "S2C"};
        for (int d = 0; d < 3; d++) {
            int bw = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, dirLabels[d], 4, 30, 48);
            if (dirFilter == d) {
                drawOverlayButton(ctx, x, row2Y, bw, filterRowHeight(), dirLabels[d], PackUiOverlayButton.Variant.PRIMARY, true, mx, my);
            } else {
                drawOverlayButton(ctx, x, row2Y, bw, filterRowHeight(), dirLabels[d], PackUiOverlayButton.Variant.GHOST, true, mx, my);
            }
            x += bw + gap;
        }
    }

    private void drawOverlayButton(GuiGraphics ctx, int x, int y, int w, int h, String label, PackUiOverlayButton.Variant variant, boolean active, int mx, int my) {
        PackUiOverlayButton button = PackUiOverlayButton.create(x, y, w, h, Component.literal(label), ignored -> {});
        button.setWidth(w);
        button.setVariant(variant);
        button.active = active;
        PackUiOverlayButton.renderStyled(ctx, textRenderer, button, mx, my);
    }

    private void renderGroup(GuiGraphics ctx, DisplayRow row, int y, int mx, int my) {
        int x = panelX + 4;
        boolean exp = expandedGroups.contains(row.groupKey);
        boolean hov = mx >= panelX && mx <= panelX + PANEL_WIDTH && my >= y && my < y + lineHeight();
        if (hov) PackUiText.fill(ctx, panelX + 2, y, panelX + PANEL_WIDTH - 2, y + lineHeight(), theme.rowFillHovered());

        PackUiControlGlyphs.drawChevron(
            ctx,
            x,
            y + 2,
            8,
            exp ? PackUiControlGlyphs.ChevronDirection.DOWN : PackUiControlGlyphs.ChevronDirection.RIGHT,
            hov ? 0xFFF5EEEE : 0xFFE7DADA,
            0xB83A1418,
            1.0f
        );
        x += 10;
        String arrow = row.direction.equals("C2S") ? ">" : "<";

        int color = row.direction.equals("C2S") ? 0xFF44CCFF : 0xFFFFAA44;
        drawUiText(ctx, arrow, PackUiTone.BODY, color, x, y + 1);
        x += 12;
        String displayName = row.groupKey.contains(":") ? row.groupKey.substring(row.groupKey.indexOf(':') + 1) : row.groupKey;
        String summary = row.payloadSnapshot == null ? "" : PackUtilPayloadSupport.summarizeForLogger(row.payloadSnapshot, true);
        String line = displayName + " (x" + row.groupCount + ")" + (summary.isBlank() ? "" : " " + summary);
        int maxW = PANEL_WIDTH - (x - panelX) - 32;
        if (PackUiText.width(textRenderer, line, theme.fontFor(PackUiTone.BODY), color) > maxW) {
            line = PackUiText.trimToWidth(textRenderer, line, Math.max(1, maxW), theme.fontFor(PackUiTone.BODY), color);
        }
        drawUiText(ctx, line, PackUiTone.BODY, color, x, y + 1);

        int bx = panelX + PANEL_WIDTH - 28;
        boolean hb = mx >= bx && mx <= bx + 24 && my >= y && my < y + lineHeight();
        drawUiText(ctx, "BLK", PackUiTone.MUTED, hb ? 0xFFFF4444 : PackUtilColors.textDim(), bx, y + 1);
    }

    private void renderEntry(GuiGraphics ctx, LogEntry e, int y, int mx, int my) {
        int x = panelX + 4;
        boolean hov = mx >= panelX && mx <= panelX + PANEL_WIDTH && my >= y && my < y + lineHeight();
        if (hov) PackUiText.fill(ctx, panelX + 2, y, panelX + PANEL_WIDTH - 2, y + lineHeight(), theme.rowFillHovered());

        int color = e.direction.equals("C2S") ? 0xFF44CCFF : 0xFFFFAA44;
        drawUiText(ctx, e.direction.equals("C2S") ? ">" : "<", PackUiTone.BODY, color, x, y + 1);
        x += 12;
        String time = TIME_FMT.format(Instant.ofEpochMilli(e.timestampMs));
        drawUiText(ctx, time, PackUiTone.MUTED, PackUtilColors.textDim(), x, y + 1);
        x += PackUiText.width(textRenderer, time, theme.fontFor(PackUiTone.MUTED), PackUtilColors.textDim()) + 4;
        drawUiText(ctx, "T" + e.gameTick, PackUiTone.MUTED, PackUtilColors.textDim(), x, y + 1);
        x += PackUiText.width(textRenderer, "T" + e.gameTick, theme.fontFor(PackUiTone.MUTED), PackUtilColors.textDim()) + 4;

        int maxW = PANEL_WIDTH - (x - panelX) - 30;
        String name = e.shortName;
        if (e.payloadSnapshot != null) {
            String summary = PackUtilPayloadSupport.summarizeForLogger(e.payloadSnapshot, true);
            if (!summary.isBlank()) {
                name = name + " " + summary;
            }
        }
        if (PackUiText.width(textRenderer, name, theme.fontFor(PackUiTone.BODY), PackUtilColors.textPrimary()) > maxW) {
            name = PackUiText.trimToWidth(textRenderer, name, Math.max(1, maxW), theme.fontFor(PackUiTone.BODY), PackUtilColors.textPrimary());
        }
        drawUiText(ctx, name, PackUiTone.BODY, color, x, y + 1);

        int bx = panelX + PANEL_WIDTH - 16;
        boolean hb = mx >= bx && mx <= bx + 12 && my >= y && my < y + lineHeight();
        drawUiText(ctx, "=", PackUiTone.MUTED, hb ? 0xFFFFDD55 : PackUtilColors.textDim(), bx, y + 1);
    }

    private void renderBlocked(GuiGraphics ctx, int mx, int my, int startY, int endY) {
        PackUiText.fill(ctx, panelX + 4, startY, panelX + PANEL_WIDTH - 4, startY + 1, PackUtilColors.secondary());
        int y = startY + 3;
        boolean hh = mx >= panelX + 4 && mx <= panelX + 150 && my >= y && my < y + lineHeight();
        drawUiText(ctx, (blockedExpanded ? "v" : ">") + " Blocked (" + blockedClasses.size() + ")", PackUiTone.LABEL, hh ? 0xFFFF8888 : 0xFFFF6666, panelX + 6, y + 1);
        int ubX = panelX + PANEL_WIDTH - 58;
        boolean hua = mx >= ubX && mx <= ubX + 54 && my >= y && my < y + lineHeight();
        drawUiText(ctx, "CLR ALL", PackUiTone.MUTED, hua ? 0xFF44FF44 : PackUtilColors.textSecondary(), ubX, y + 1);

        if (!blockedExpanded) return;
        y += lineHeight();
        for (Class<?> cls : blockedClasses) {
            if (y + lineHeight() > endY) break;
            @SuppressWarnings("unchecked")
            String n = PackUtilPacketNamer.getFriendlyName((Class<? extends Packet<?>>) cls);
            drawUiText(ctx, "  " + PackUiText.trimToWidth(textRenderer, n, Math.max(1, PANEL_WIDTH - 50), theme.fontFor(PackUiTone.BODY), 0xFFAA6666), PackUiTone.BODY, 0xFFAA6666, panelX + 8, y + 1);
            int ux = panelX + PANEL_WIDTH - 28;
            boolean hu = mx >= ux && mx <= ux + 24 && my >= y && my < y + lineHeight();
            drawUiText(ctx, "UB", PackUiTone.MUTED, hu ? 0xFF44FF44 : PackUtilColors.textSecondary(), ux, y + 1);
            y += lineHeight();
        }
    }

    private String[] getCtxItems(LogEntry e) {
        boolean isC2S = e != null && "C2S".equalsIgnoreCase(e.direction);
        boolean isPayload = e != null && e.isPayload;
        if (isC2S && isPayload) {
            return new String[]{"Block", "Queue", "Replay", "Send", "Edit Payload", "+PAYLOAD", "+SEND", "+WAIT", "+ Filter", "- Filter", "Copy", "Inspect"};
        }
        return isC2S
                ? new String[]{"Block", "Queue", "Replay", "Send", "+SEND", "+WAIT", "+ Filter", "- Filter", "Copy", "Inspect"}
                : new String[]{"Block", "+WAIT", "+ Filter", "- Filter", "Copy", "Inspect"};
    }

    private void openCtxMenu(LogEntry entry, int mouseX, int mouseY) {
        ctxMenu.open(mouseX, mouseY, entry);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        if (ctxMenu.handleClick(mouseX, mouseY, button, (entry, action, index) -> executeCtxAction(action, entry))) return true;

        if (button != 0 && button != 1) return false;

        if (false && button == 0 && !collapsed && isResizeActive(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, calcPanelH())) {
            isResizing = true;
            resizeStartMouseX = mouseX;
            resizeStartMouseY = mouseY;
            resizeStartWidth = PANEL_WIDTH;
            resizeStartHeight = PANEL_HEIGHT;
            return true;
        }

        if (mouseY >= panelY && mouseY <= panelY + HEADER_H && mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH) {
            PackUtilWindowLayout bounds = new PackUtilWindowLayout(panelX, panelY, PANEL_WIDTH, calcPanelH(), visible, collapsed);
            if (isOverCloseButton(mouseX, mouseY, bounds)) { setVisible(false); return true; }
            if (button == 0) {
                isDragging = true;
                headerDragMoved = false;
                dragOffX = mouseX - panelX;
                dragOffY = mouseY - panelY;
                headerPressMouseX = mouseX;
                headerPressMouseY = mouseY;
                headerPressPanelX = panelX;
                headerPressPanelY = panelY;
            }
            return true;
        }
        if (collapsed) return false;

        int tabY = panelY + HEADER_H + 2;
        int filterY = tabY + tabHeight() + 2;
        int contentY = filterY + filterHeight() + 2;

        if (mouseY >= tabY && mouseY < tabY + tabHeight() && button == 0) {
            int x = panelX + 4;
            for (Category cat : Category.values()) {
                int w = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, cat.label, 5, 32, 64);
                if (mouseX >= x && mouseX < x + w) { activeTab = cat; dirty = true; scrollOffset = 0; return true; }
                x += w + 2;
            }
            return true;
        }

        if (mouseY >= filterY && mouseY < filterY + filterHeight() && button == 0) {
            return handleFilterClick(mouseX, mouseY, filterY);
        }

        if (searchField.isFocused()) searchField.setFocused(false);

        int contentEndY = contentY + contentAreaHeight();
        if (mouseY >= contentY && mouseY < contentEndY) {
            if (button == 0) {
                PackUiScrollbar.Metrics scrollbarMetrics = getContentScrollbarMetrics();
                if (scrollbarMetrics.hasScroll() && scrollbarMetrics.contains((int) mouseX, (int) mouseY)) {
                    scrollbarDragging = true;
                    scrollbarGrabOffset = Math.max(0, (int) mouseY - scrollbarMetrics.thumbY());
                    scrollOffset = quantizeScrollOffset(PackUiScrollbar.scrollFromThumb(scrollbarMetrics, (int) mouseY, scrollbarGrabOffset), lineHeight(), scrollbarMetrics.maxScroll());
                    contentScrollState.jumpTo(scrollOffset, scrollbarMetrics.maxScroll());
                    return true;
                }
            }
            return handleContentClick(mouseX, mouseY, contentY, button);
        }

        if (!blockedClasses.isEmpty() && button == 0) {
            return handleBlockedClick(mouseX, mouseY, contentEndY);
        }

        return false;
    }

    private boolean handleFilterClick(double mouseX, double mouseY, int y) {
        int gap = 2;
        int row1Y = y;
        int row2Y = y + filterRowHeight() + filterRowGap();
        int x = panelX + 4;
        String captureLabel = paused ? "Start" : "Stop";
        int captureW = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, captureLabel, 5, 38, 54);
        if (mouseY >= row1Y && mouseY < row1Y + filterRowHeight() && mouseX >= x && mouseX < x + captureW) {
            setPaused(!paused);
            return true;
        }
        x += captureW + gap;

        int clrW = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, "Clear", 5, 34, 54);
        if (mouseY >= row1Y && mouseY < row1Y + filterRowHeight() && mouseX >= x && mouseX < x + clrW) { clearAll(); return true; }
        x += clrW + gap;
        int cpW = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, "Copy", 5, 34, 52);
        if (mouseY >= row1Y && mouseY < row1Y + filterRowHeight() && mouseX >= x && mouseX < x + cpW) { copyToClipboard(); return true; }
        x += cpW + gap;
        int grpW = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, groupingEnabled ? "Group" : "Ungrp", 5, 38, 58);
        if (mouseY >= row1Y && mouseY < row1Y + filterRowHeight() && mouseX >= x && mouseX < x + grpW) { groupingEnabled = !groupingEnabled; dirty = true; return true; }
        x += grpW + gap;
        if (!blockedClasses.isEmpty()) {
            String bt = blockedClasses.size() + "blk";
            int bw = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, bt, 5, 34, 52);
            if (mouseY >= row1Y && mouseY < row1Y + filterRowHeight() && mouseX >= x && mouseX < x + bw) { blockedExpanded = !blockedExpanded; return true; }
        }

        x = panelX + 4;
        int sw = 148;
        if (mouseY >= row2Y && mouseY < row2Y + filterRowHeight() && mouseX >= x && mouseX < x + sw) {
            searchField.mouseClicked(mouseX, mouseY, 0);
            return true;
        }
        x += sw + 3;

        String[] dirLabels = {"Both", "C2S", "S2C"};
        for (int d = 0; d < 3; d++) {
            int bw = PackUiLegacyLayout.fitOverlayButtonWidth(textRenderer, theme, PackUiTone.BODY, dirLabels[d], 4, 30, 48);
            if (mouseY >= row2Y && mouseY < row2Y + filterRowHeight() && mouseX >= x && mouseX < x + bw) { dirFilter = d; dirty = true; return true; }
            x += bw + gap;
        }
        searchField.setFocused(false);
        return true;
    }

    private boolean handleContentClick(double mouseX, double mouseY, int contentY, int button) {
        int idx = (int) ((mouseY - contentY + contentScrollState.tick(0.0f, Math.max(0, displayRows.size() * lineHeight() - contentAreaHeight()))) / lineHeight());
        if (idx < 0 || idx >= displayRows.size()) return false;
        DisplayRow row = displayRows.get(idx);

        if (row.type == RowType.GROUP) {
            int bx = panelX + PANEL_WIDTH - 28;
            if (button == 0 && mouseX >= bx && mouseX <= bx + 24 && row.packetClass != null) {
                blockedClasses.add(row.packetClass); dirty = true; return true;
            }
            if (button == 0) {
                if (expandedGroups.contains(row.groupKey)) expandedGroups.remove(row.groupKey);
                else expandedGroups.add(row.groupKey);
                dirty = true; return true;
            }
        }

        if (row.type == RowType.ENTRY && row.entry != null) {

            int menuIconX = panelX + PANEL_WIDTH - 16;
            if (button == 1 || (button == 0 && mouseX >= menuIconX)) {
                openCtxMenu(row.entry, (int) mouseX, (int) mouseY);
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private void executeCtxAction(String action, LogEntry e) {
        switch (action) {
            case "Block":
                blockedClasses.add(e.packetClass);
                dirty = true;
                PackUtilClientMessaging.sendPrefixed("Blocked: " + e.shortName);
                break;
            case "Queue":
                PackUtilPacketEntryActions.queue(e);
                break;
            case "Send":
                PackUtilPacketEntryActions.directSend(e);
                break;
            case "+SEND":
                PackUtilPacketEntryActions.addSendActionToVisibleMacro(e);
                break;
            case "+WAIT":
                PackUtilPacketEntryActions.addWaitActionToVisibleMacro(e);
                break;
            case "Edit Payload":
                PackUtilPacketEntryActions.openPayloadEditor(e);
                break;
            case "+PAYLOAD":
                PackUtilPacketEntryActions.addPayloadActionToVisibleMacro(e);
                break;
            case "+ Filter": {
                PackUtilSharedState shared = PackUtilSharedState.get();
                Class<? extends Packet<?>> pktCls = (Class<? extends Packet<?>>) e.packetClass;
                boolean added;
                if (e.direction.equals("C2S")) {
                    added = shared.getC2SPackets().add(pktCls);
                } else {
                    added = shared.getS2CPackets().add(pktCls);
                }
                shared.setUseCustomPackets(true);
                PackUtilClientMessaging.sendPrefixed(added
                    ? "Added to custom " + e.direction + " filter: " + e.shortName
                    : "Already present in custom " + e.direction + " filter: " + e.shortName);
                break;
            }
            case "- Filter": {
                PackUtilSharedState shared = PackUtilSharedState.get();
                Class<? extends Packet<?>> pktCls = (Class<? extends Packet<?>>) e.packetClass;
                boolean removed;
                if (e.direction.equals("C2S")) {
                    removed = shared.getC2SPackets().remove(pktCls);
                } else {
                    removed = shared.getS2CPackets().remove(pktCls);
                }
                PackUtilClientMessaging.sendPrefixed(removed
                    ? "Removed from custom " + e.direction + " filter: " + e.shortName
                    : "Not present in custom " + e.direction + " filter: " + e.shortName);
                break;
            }
            case "Replay":
                if (e.packetRef != null && e.direction.equals("C2S")) {
                    PackUtilPacketEntryActions.directSend(e);
                }
                break;
            case "Copy": {
                String line = e.direction + " " + TIME_FMT.format(Instant.ofEpochMilli(e.timestampMs))
                        + " T" + e.gameTick + " " + e.shortName;
                MC.keyboardHandler.setClipboard(line);
                PackUtilClientMessaging.sendPrefixed("Copied packet info.");
                break;
            }
            case "Inspect": {
                PackUtilPacketInspector.PacketInspection inspection = PackUtilPacketInspector.inspectSafe(e);
                inspectOverlay.open(inspection, e, panelX + PANEL_WIDTH + 10, panelY + 8);
                break;
            }
        }
    }

    private boolean handleBlockedClick(double mouseX, double mouseY, int contentEndY) {
        int y = contentEndY + 3;
        if (mouseY >= y && mouseY < y + lineHeight()) {
            int ubX = panelX + PANEL_WIDTH - 58;
            if (mouseX >= ubX && mouseX <= ubX + 54) { blockedClasses.clear(); dirty = true; return true; }
            blockedExpanded = !blockedExpanded; return true;
        }
        if (blockedExpanded) {
            int ey = y + lineHeight();
            int ux = panelX + PANEL_WIDTH - 28;
            for (Class<?> cls : new ArrayList<>(blockedClasses)) {
                if (mouseX >= ux && mouseX <= ux + 24 && mouseY >= ey && mouseY < ey + lineHeight()) {
                    blockedClasses.remove(cls); dirty = true; return true;
                }
                ey += lineHeight();
            }
        }
        return false;
    }

    @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        if (scrollbarDragging && b == 0) {
            PackUiScrollbar.Metrics scrollbarMetrics = getContentScrollbarMetrics();
            scrollOffset = quantizeScrollOffset(PackUiScrollbar.scrollFromThumb(scrollbarMetrics, (int) my, scrollbarGrabOffset), lineHeight(), scrollbarMetrics.maxScroll());
            contentScrollState.jumpTo(scrollOffset, scrollbarMetrics.maxScroll());
            return true;
        }
        if (isResizing && b == 0) {
            PackUtilWindowLayout nextBounds = clampToScreen(this,
                new PackUtilWindowLayout(panelX, panelY,
                    resizeStartWidth + (int) Math.round(mx - resizeStartMouseX),
                    resizeStartHeight + (int) Math.round(my - resizeStartMouseY),
                    visible, collapsed));
            panelX = nextBounds.x;
            panelY = nextBounds.y;
            PANEL_WIDTH = nextBounds.width;
            PANEL_HEIGHT = nextBounds.height;
            return true;
        }
        if (isDragging && b == 0) {
            PackUtilWindowLayout nextBounds = clampToScreen(this,
                new PackUtilWindowLayout((int) (mx - dragOffX), (int) (my - dragOffY),
                    PANEL_WIDTH, PANEL_HEIGHT, visible, collapsed));
            if (nextBounds.x != panelX || nextBounds.y != panelY) {
                headerDragMoved = true;
            }
            panelX = nextBounds.x;
            panelY = nextBounds.y;
            return true;
        }
        return false;
    }
    @Override public boolean mouseReleased(double mx, double my, int b) {
        if (b == 0 && scrollbarDragging) { scrollbarDragging = false; return true; }
        if (b == 0 && isDragging) {
            boolean moved = headerDragMoved
                || Math.abs(mx - headerPressMouseX) >= 3.0
                || Math.abs(my - headerPressMouseY) >= 3.0
                || panelX != headerPressPanelX
                || panelY != headerPressPanelY;
            PackUtilWindowLayout bounds = new PackUtilWindowLayout(panelX, panelY, PANEL_WIDTH, calcPanelH(), visible, collapsed);
            isDragging = false;
            headerDragMoved = false;
            if (!moved
                && mx >= panelX && mx <= panelX + PANEL_WIDTH
                && my >= panelY && my <= panelY + HEADER_H
                && !isOverCloseButton(mx, my, bounds)) {
                setCollapsed(!collapsed);
            }
            saveLayout();
            return true;
        }
        if (b == 0 && isResizing) { isResizing = false; saveLayout(); return true; }
        return false;
    }
    @Override public boolean mouseScrolled(double mx, double my, double amt) {
        if (!visible || collapsed) return false;
        int totalH = displayRows.size() * lineHeight();
        int visH = contentAreaHeight();
        int maxScroll = Math.max(0, totalH - visH);
        scrollOffset = quantizeScrollOffset(scrollOffset - (int)Math.round(amt * lineHeight() * 3.0), lineHeight(), maxScroll);
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        return searchField.isFocused() && searchField.keyPressed(new KeyEvent(key, scan, mods));
    }
    @Override public boolean charTyped(char c, int mods) {
        return searchField.isFocused() && searchField.charTyped(new CharacterEvent(c, 0));
    }

    private List<LogEntry> getActiveBuffer() {
        switch (activeTab) {
            case INVENTORY: return bufInventory;
            case MOVEMENT: return bufMovement;
            case PAYLOAD: return bufPayload;
            case ALL: default: return bufAll;
        }
    }

    private void rebuildDisplay() {
        List<LogEntry> source = getActiveBuffer();
        if (source == null) source = new ArrayList<>();

        String ls = searchFilter.toLowerCase(Locale.ROOT);
        List<LogEntry> filtered = new ArrayList<>();
        for (LogEntry e : source) {
            if (blockedClasses.contains(e.packetClass)) continue;
            if (!ls.isEmpty() && !e.searchKey.contains(ls)) continue;

            if (dirFilter == 1 && !e.direction.equals("C2S")) continue;
            if (dirFilter == 2 && !e.direction.equals("S2C")) continue;
            filtered.add(e);
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> dirs = new LinkedHashMap<>();
        Map<String, Class<?>> classes = new LinkedHashMap<>();
        for (LogEntry e : filtered) {
            String key = e.groupKey;
            counts.merge(key, 1, Integer::sum);
            dirs.put(key, e.direction);
            classes.put(key, e.packetClass);
        }

        Set<String> groupedKeys = new HashSet<>();
        if (groupingEnabled) {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() >= GROUP_THRESHOLD) groupedKeys.add(entry.getKey());
            }
        }

        List<LogEntry> reversed = new ArrayList<>(filtered);
        java.util.Collections.reverse(reversed);

        List<DisplayRow> rows = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();

        for (LogEntry e : reversed) {
            String key = e.groupKey;
            if (groupedKeys.contains(key) && added.add(key)) {
                DisplayRow h = new DisplayRow();
                h.type = RowType.GROUP; h.groupKey = key; h.groupCount = counts.get(key);
                h.direction = dirs.get(key); h.packetClass = classes.get(key);
                h.payloadSnapshot = e.payloadSnapshot;
                rows.add(h);
                if (expandedGroups.contains(key)) {
                    for (LogEntry child : reversed) {
                        if (child.groupKey.equals(key)) {
                            DisplayRow r = new DisplayRow(); r.type = RowType.ENTRY; r.entry = child; rows.add(r);
                        }
                    }
                }
            }
        }

        for (LogEntry e : reversed) {
            if (!groupedKeys.contains(e.groupKey)) {
                DisplayRow r = new DisplayRow(); r.type = RowType.ENTRY; r.entry = e; rows.add(r);
            }
        }

        displayRows = rows;
        clampScrollOffset();
    }

    private void clampScrollOffset() {
        int totalH = displayRows.size() * lineHeight();
        int visH = currentPanelHeight - HEADER_H - tabHeight() - filterHeight() - 8 - blockedH();
        int maxScroll = Math.max(0, totalH - visH);
        scrollOffset = quantizeScrollOffset(scrollOffset, lineHeight(), maxScroll);
    }

    private int calcPanelH() {
        int rc = Math.min(displayRows.size(), maxVisibleRows());
        return Math.max(PANEL_HEIGHT, HEADER_H + tabHeight() + filterHeight() + 8 + Math.max(rc * lineHeight(), 24) + blockedH() + 4);
    }

    private int blockedH() {
        if (blockedClasses.isEmpty()) return 0;
        int h = lineHeight() + 4;
        if (blockedExpanded) h += blockedClasses.size() * lineHeight();
        return h;
    }

    private void clearAll() {
        pendingEntries.clear();
        bufAll.clear(); bufInventory.clear(); bufMovement.clear(); bufPayload.clear();
        displayRows.clear(); scrollOffset = 0; contentScrollState.jumpTo(0, 0); expandedGroups.clear(); dirty = true;
    }

    private void copyToClipboard() {
        flushPending();
        StringBuilder sb = new StringBuilder();
        sb.append("=== Packet Logger [").append(activeTab.label).append("] ===\n");
        if (!searchFilter.isEmpty()) sb.append("Search: \"").append(searchFilter).append("\"\n");
        sb.append(String.format("%-5s %-14s %-8s %s%n", "DIR", "TIME", "TICK", "PACKET"));
        sb.append("----------------------------------------------\n");

        List<LogEntry> source = getActiveBuffer();
        if (source == null) source = new ArrayList<>();
        String ls = searchFilter.toLowerCase(Locale.ROOT);
        int count = 0;
        for (LogEntry e : source) {
            if (blockedClasses.contains(e.packetClass)) continue;
            if (!ls.isEmpty() && !e.searchKey.contains(ls)) continue;
            if (dirFilter == 1 && !e.direction.equals("C2S")) continue;
            if (dirFilter == 2 && !e.direction.equals("S2C")) continue;
            sb.append(String.format("%-5s %-14s %-8s %s%n",
                    e.direction.equals("C2S") ? "->" : "<-",
                    TIME_FMT.format(Instant.ofEpochMilli(e.timestampMs)),
                    "T" + e.gameTick, e.shortName));
            count++;
        }
        sb.append("----------------------------------------------\n");
        sb.append("Total: ").append(count).append(" packets\n");

        MC.keyboardHandler.setClipboard(sb.toString());
        PackUtilClientMessaging.sendPrefixed("Copied " + count + " packets to clipboard.");
    }

        private int defaultPanelWidth() {
        return 323;
    }

    private int defaultPanelHeight() {
        return 250;
    }

    private int lineHeight() {
        return 12;
    }

    private int tabHeight() {
        return 16;
    }

    private int filterRowHeight() {
        return 16;
    }

    private int filterRowGap() {
        return 2;
    }

    private int filterHeight() {
        return filterRowHeight() * 2 + filterRowGap();
    }

    private int searchFieldWidth() {
        return 148;
    }

    private int maxVisibleRows() {
        return 22;
    }

    enum RowType { ENTRY, GROUP }

    static class DisplayRow {
        RowType type;
        LogEntry entry;
        String groupKey;
        int groupCount;
        String direction;
        Class<?> packetClass;
        PackUtilPayloadSupport.PayloadSnapshot payloadSnapshot;
    }

    private static long idCounter = 0;

    public static class LogEntry {
        public final long id;
        public final long timestampMs;
        public final int gameTick;
        public final String direction;
        public final String shortName;
        public final String searchKey;
        public final String groupKey;
        public final Class<?> packetClass;
        public final Packet<?> packetRef;
        public final boolean isInventory;
        public final boolean isMovement;
        public final boolean isPayload;
        public final String capturedBlockState;
        public final String capturedScreen;
        public final PackUtilPayloadSupport.PayloadSnapshot payloadSnapshot;

        public LogEntry(long ts, int tick, String dir, String name, Class<?> cls, Packet<?> ref,
                        boolean inv, boolean mov, boolean payload, String capturedBlockState, String capturedScreen,
                        PackUtilPayloadSupport.PayloadSnapshot payloadSnapshot) {
            this.id = idCounter++;
            this.timestampMs = ts;
            this.gameTick = tick;
            this.direction = dir;
            this.shortName = name;
            this.payloadSnapshot = payloadSnapshot;
            String payloadSearch = payloadSnapshot == null ? "" : (" " + payloadSnapshot.channel() + " " + payloadSnapshot.rawDump());
            this.searchKey = (name == null ? "" : name) .concat(payloadSearch).toLowerCase(Locale.ROOT);
            this.groupKey = dir + ":" + name;
            this.packetClass = cls;
            this.packetRef = ref;
            this.isInventory = inv;
            this.isMovement = mov;
            this.isPayload = payload;
            this.capturedBlockState = capturedBlockState;
            this.capturedScreen = capturedScreen;
        }
    }

    private record LogCaptureContext(String blockStateSummary, String screenSummary) {
        private static final LogCaptureContext EMPTY = new LogCaptureContext(null, null);
    }
}
