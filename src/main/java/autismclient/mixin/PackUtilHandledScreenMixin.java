package autismclient.mixin;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import autismclient.modules.PackUtilModule;
import autismclient.gui.packui.PackUiBannerRenderer;
import autismclient.gui.packui.PackUiTheme;
import autismclient.util.PackUtilClientMessaging;
import autismclient.util.PackUtilColors;
import autismclient.util.PackUtilCustomFilterOverlay;
import autismclient.util.PackUtilCustomFilterPresetOverlay;
import autismclient.util.PackUtilFabricatorOverlay;
import autismclient.util.PackUtilFabricatorRegistry;
import autismclient.util.PackUtilLANSync;
import autismclient.util.PackUtilLANSyncOverlay;
import autismclient.util.PackUtilLauncherOverlay;
import autismclient.util.PackUtilMacroListOverlay;
import autismclient.util.PackUtilQueueEditorOverlay;
import autismclient.util.IPackUtilOverlay;
import autismclient.util.PackUtilInventoryMoveHelper;
import autismclient.util.PackUtilOverlayManager;
import autismclient.util.PackUtilMacroEditorOverlay;
import autismclient.util.PackUtilPacketLoggerOverlay;
import autismclient.util.PackUtilText;
import autismclient.util.PackUtilUiScale;

import autismclient.util.PackUtilSharedState;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Shadow;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

@Mixin(AbstractContainerScreen.class)
public abstract class PackUtilHandledScreenMixin<T extends AbstractContainerMenu> extends Screen {
    @Unique private static final PackUiTheme PACKUI_THEME = new PackUiTheme();
    @Shadow @Nullable protected Slot hoveredSlot;
    @Shadow protected abstract void slotClicked(Slot slot, int slotId, int button, ContainerInput actionType);
    @Unique private static final Minecraft MC = Minecraft.getInstance();
    @Unique private Slot packutil$blockedFocusedSlot;

    @Unique private PackUtilLauncherOverlay launcherOverlay;
    @Unique private PackUtilFabricatorOverlay fabricatorOverlay;
    @Unique private PackUtilLANSyncOverlay lanSyncOverlay;
    @Unique private PackUtilMacroListOverlay macroListOverlay;
    @Unique private PackUtilQueueEditorOverlay queueEditorOverlay;
    @Unique private PackUtilPacketLoggerOverlay packetLoggerOverlay;
    @Unique private PackUtilCustomFilterOverlay customFilterOverlay;
    @Unique private PackUtilCustomFilterPresetOverlay customFilterPresetOverlay;
    @Unique private PackUtilMacroEditorOverlay macroEditorOverlay;
    @Unique private autismclient.util.PackUtilKeybindOverlay keybindOverlay;
    @Unique private autismclient.util.PackUtilServerInfoOverlay serverInfoOverlay;

    protected PackUtilHandledScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void yang$init(CallbackInfo ci) {

        if (!isPackUtilActive()) return;

        PackUtilLANSync.getInstance().setOnSessionStateChanged(() -> {});

        AbstractContainerScreen<?> handledScreen = (AbstractContainerScreen<?>) (Object) this;
        fabricatorOverlay = PackUtilFabricatorOverlay.getSharedOverlay(handledScreen);
        lanSyncOverlay = PackUtilLANSyncOverlay.getSharedOverlay(this.font);
        macroListOverlay = new PackUtilMacroListOverlay(this.font);
        queueEditorOverlay = new PackUtilQueueEditorOverlay(this.font);
        customFilterOverlay = new PackUtilCustomFilterOverlay(this.font);
        customFilterPresetOverlay = customFilterOverlay.getPresetManagerOverlay();
        fabricatorOverlay.restoreState();
        lanSyncOverlay.restoreState();
        macroListOverlay.restoreState();
        queueEditorOverlay.restoreState();
        customFilterOverlay.restoreLayout();
        if (customFilterPresetOverlay != null) {
            customFilterPresetOverlay.restoreLayout();
        }

        macroEditorOverlay = PackUtilMacroEditorOverlay.getSharedOverlay();
        if (macroEditorOverlay != null) {
            macroEditorOverlay.restoreState();
        }

        PackUtilOverlayManager manager = PackUtilOverlayManager.get();
        manager.clear();
        manager.register(fabricatorOverlay);
        manager.register(lanSyncOverlay);
        manager.register(macroListOverlay);
        manager.register(queueEditorOverlay);
        manager.register(customFilterOverlay);
        if (customFilterPresetOverlay != null) {
            manager.register(customFilterPresetOverlay);
        }
        if (macroEditorOverlay != null) {
            manager.register(macroEditorOverlay);
        }

        manager.register(autismclient.gui.macro.editor.ActionEditorOverlay.getSharedOverlay());

        PackUtilModule packutilModule = PackUtilModule.get();
        if (packutilModule != null) {
            packetLoggerOverlay = packutilModule.getPacketLoggerOverlay();
            if (packetLoggerOverlay != null) {
                packetLoggerOverlay.restoreLayout();
                manager.register(packetLoggerOverlay);
            }
        }
        keybindOverlay = new autismclient.util.PackUtilKeybindOverlay();
        keybindOverlay.restoreLayout();
        manager.register(keybindOverlay);

        serverInfoOverlay = autismclient.modules.PackUtilModule.get().getServerDataOverlay();
        manager.register(serverInfoOverlay);

        launcherOverlay = new PackUtilLauncherOverlay(macroListOverlay, fabricatorOverlay, lanSyncOverlay, queueEditorOverlay, packetLoggerOverlay, customFilterOverlay);
        launcherOverlay.setKeybindOverlay(keybindOverlay);
        launcherOverlay.setServerDataOverlay(serverInfoOverlay);
        launcherOverlay.restoreLayout();
        manager.register(launcherOverlay);

        Screen screen = (Screen)(Object)this;
        ScreenEvents.afterExtract(screen).register((scrn, drawContext, mouseX, mouseY, tickDelta) -> {
            if (isPackUtilActive()) {
                PackUtilOverlayManager.get().renderAll(drawContext, mouseX, mouseY, tickDelta);
                PackUtilUiScale.pushOverlayScale(drawContext);
                try {
                    renderMacroCaptureBanner(drawContext);
                } finally {
                    PackUtilUiScale.popOverlayScale(drawContext);
                }
            }
        });

        refreshButtonVisibility();
    }

    @Unique private boolean coreExpanded = true;
    @Unique private boolean queueExpanded = false;
    @Unique private boolean toolsExpanded = false;
    @Unique private int coreButtonsStartY, queueHeaderY, queueButtonsStartY, queueButtonsEndY;
    @Unique private int toolsHeaderY, toolsButtonsStartY, toolsButtonsEndY;

    @Unique
    private void refreshButtonVisibility() {

    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void yang$blockCoveredSlotHover(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isPackUtilActive()) return;

        if (PackUtilOverlayManager.get().shouldBlockUnderlyingHover(mouseX, mouseY)) {
            packutil$blockedFocusedSlot = hoveredSlot;
            hoveredSlot = null;
        } else {
            packutil$blockedFocusedSlot = null;
        }
    }

    @Inject(method = "getHoveredSlot", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$blockCoveredSlotLookup(double mouseX, double mouseY, CallbackInfoReturnable<Slot> cir) {
        if (!isPackUtilActive()) return;

        if (PackUtilOverlayManager.get().shouldBlockUnderlyingHover(mouseX, mouseY)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$blockCoveredSlotHitbox(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        if (!isPackUtilActive()) return;

        if (PackUtilOverlayManager.get().shouldBlockUnderlyingHover(pointX, pointY)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$blockCoveredHandledTooltip(GuiGraphicsExtractor context, int x, int y, CallbackInfo ci) {
        if (!isPackUtilActive()) return;

        if (PackUtilOverlayManager.get().shouldBlockUnderlyingHover(x, y)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    public void yang$render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (packutil$blockedFocusedSlot != null) {
            hoveredSlot = packutil$blockedFocusedSlot;
            packutil$blockedFocusedSlot = null;
        }
    }

    @Unique
    private void renderMacroCaptureBanner(GuiGraphicsExtractor context) {
        autismclient.gui.macro.editor.ActionEditorOverlay actionEditor =
                autismclient.gui.macro.editor.ActionEditorOverlay.getSharedOverlayIfExists();
        boolean macroCapture = macroEditorOverlay != null && macroEditorOverlay.shouldRenderAbstractContainerScreenCaptureBanner();
        boolean actionCapture = actionEditor != null && actionEditor.shouldRenderAbstractContainerScreenCaptureBanner();
        if (!macroCapture && !actionCapture) return;
        if (MC == null || MC.getWindow() == null || this.font == null) return;

        String title = macroCapture
                ? macroEditorOverlay.getAbstractContainerScreenCaptureTitle()
                : actionEditor.getAbstractContainerScreenCaptureTitle();
        String instruction = macroCapture
                ? macroEditorOverlay.getAbstractContainerScreenCaptureInstruction()
                : actionEditor.getAbstractContainerScreenCaptureInstruction();
        String hover = "";

        if (hoveredSlot != null) {
            net.minecraft.world.item.ItemStack stack = hoveredSlot.getItem();
            String itemName = stack.isEmpty() ? "" : stack.getHoverName().getString();
            String registryId = stack.isEmpty() ? "" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            hover = macroCapture
                    ? macroEditorOverlay.getAbstractContainerScreenCaptureHoverText(hoveredSlot, itemName, registryId)
                    : actionEditor.getAbstractContainerScreenCaptureHoverText(hoveredSlot, itemName, registryId);
        }

        int maxTextWidth = Math.max(
            autismclient.gui.packui.PackUiText.width(this.font, title, PACKUI_THEME.fontFor(autismclient.gui.packui.PackUiTone.LABEL), PACKUI_THEME.color(autismclient.gui.packui.PackUiTone.BODY)),
            autismclient.gui.packui.PackUiText.width(this.font, instruction, PACKUI_THEME.fontFor(autismclient.gui.packui.PackUiTone.BODY), PACKUI_THEME.color(autismclient.gui.packui.PackUiTone.BODY))
        );
        if (!hover.isEmpty()) {
            maxTextWidth = Math.max(maxTextWidth, autismclient.gui.packui.PackUiText.width(this.font, hover, PACKUI_THEME.fontFor(autismclient.gui.packui.PackUiTone.MUTED), PACKUI_THEME.color(autismclient.gui.packui.PackUiTone.MUTED)));
        }

        int screenWidth = PackUtilUiScale.getVirtualScreenWidth();
        int boxWidth = Math.min(screenWidth - 16, Math.max(250, maxTextWidth + 18));
        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = 8;
        PackUiBannerRenderer.drawFloatingBanner(context, this.font, PACKUI_THEME, boxX, boxY, boxWidth, title, instruction, hover, PACKUI_THEME.headerAccent());
        if (actionCapture && actionEditor != null && actionEditor.hasAbstractContainerScreenCaptureToasts()) {
            int bannerHeight = hover.isEmpty() ? 34 : 46;
            actionEditor.renderAbstractContainerScreenCaptureToasts(context, boxX, boxY + bannerHeight + 6, boxWidth);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void yang$removed(CallbackInfo ci) {
        if (!isPackUtilActive()) return;
        PackUtilInventoryMoveHelper.releaseMovementKeysIfSafe();

        autismclient.gui.macro.editor.ActionEditorOverlay actionEditor =
                autismclient.gui.macro.editor.ActionEditorOverlay.getSharedOverlayIfExists();
        boolean skipTransientCaptureSave = actionEditor != null && actionEditor.hasActiveCaptureSession();

        if (!skipTransientCaptureSave) {
            if (fabricatorOverlay != null) {
                fabricatorOverlay.saveState();
            }
            if (lanSyncOverlay != null) {
                lanSyncOverlay.saveState();
            }
            if (macroListOverlay != null) {
                macroListOverlay.saveState();
            }
            if (queueEditorOverlay != null) {
                queueEditorOverlay.saveState();
            }
            if (macroEditorOverlay != null) {
                macroEditorOverlay.saveState();
            }
            if (launcherOverlay != null) {
                launcherOverlay.saveLayout();
            }
            if (packetLoggerOverlay != null) {
                packetLoggerOverlay.saveLayout();
            }
            if (customFilterOverlay != null) {
                customFilterOverlay.saveLayout();
            }
            if (customFilterPresetOverlay != null) {
                customFilterPresetOverlay.saveLayout();
            }
            if (keybindOverlay != null) {
                keybindOverlay.saveLayout();
            }
            if (serverInfoOverlay != null) {
                serverInfoOverlay.saveState();
            }
        }

        PackUtilOverlayManager.get().clear();
    }

    @Unique
    private boolean isPackUtilActive() {
        PackUtilModule module = PackUtilModule.get();
        return module != null && module.isActive();
    }

    @Unique
    private void updateButtonLabels() {
    }

    @Unique
    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    @Unique
    private static String stateText(boolean value) {
        return value ? "enabled" : "disabled";
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void yang$mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!isPackUtilActive()) return;

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        PackUtilOverlayManager manager = PackUtilOverlayManager.get();

        if (manager.handleMouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }

        if (button == 1 && hoveredSlot != null) {
            net.minecraft.world.item.ItemStack captureStack = hoveredSlot.getItem();
            String captureItemName   = captureStack.isEmpty() ? "" : captureStack.getHoverName().getString();
            String captureRegistryId = captureStack.isEmpty() ? "" :
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(captureStack.getItem()).toString();

            PackUtilMacroEditorOverlay editor = macroEditorOverlay;
            if (editor != null && editor.wantsSlotCapture()
                    && editor.onSlotRightClick(hoveredSlot, captureItemName, captureRegistryId)) {
                cir.setReturnValue(true);
                return;
            }

            autismclient.gui.macro.editor.ActionEditorOverlay actionEditor =
                    autismclient.gui.macro.editor.ActionEditorOverlay.getSharedOverlayIfExists();
            if (actionEditor != null && actionEditor.wantsItemSlotCapture()
                    && actionEditor.onInventorySlotCapture(hoveredSlot, captureItemName, captureRegistryId)) {
                cir.setReturnValue(true);
                return;
            }
        }

        if (fabricatorOverlay != null && fabricatorOverlay.isVisible() && button == 1 && hoveredSlot != null) {
            fabricatorOverlay.onSlotClick(hoveredSlot, button);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void yang$keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (!isPackUtilActive()) return;

        boolean inventoryKey = MC != null && MC.options != null && MC.options.keyInventory.matches(input);
        if (inventoryKey) {
            if (PackUtilSharedState.get().consumeCaptureCancelCallback()) {
                cir.setReturnValue(true);
                return;
            }

            autismclient.gui.macro.editor.ActionEditorOverlay actionEditor =
                    autismclient.gui.macro.editor.ActionEditorOverlay.getSharedOverlayIfExists();
            if (actionEditor != null && actionEditor.cancelCaptureIfActive()) {
                cir.setReturnValue(true);
                return;
            }
        }

        if (PackUtilOverlayManager.get().handleKeyPressed(input.key(), input.scancode(), input.modifiers())) {
            cir.setReturnValue(true);
            return;
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (PackUtilSharedState.get().consumeCaptureCancelCallback()) {
                cir.setReturnValue(true);
                return;
            }

            autismclient.gui.macro.editor.ActionEditorOverlay actionEditor =
                    autismclient.gui.macro.editor.ActionEditorOverlay.getSharedOverlayIfExists();
            if (actionEditor != null && actionEditor.cancelCaptureIfActive()) {
                cir.setReturnValue(true);
            }
        }

        if (PackUtilInventoryMoveHelper.handleKeyEvent(input, true)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyReleased", at = @At("HEAD"), cancellable = true, require = 0)
    private void yang$keyReleased(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (!isPackUtilActive()) return;

        if (PackUtilInventoryMoveHelper.handleKeyEvent(input, false)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void yang$mouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        if (!isPackUtilActive()) return;

        if (PackUtilOverlayManager.get().handleMouseReleased(click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
            return;
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void yang$mouseDragged(MouseButtonEvent click, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (!isPackUtilActive()) return;

        if (PackUtilOverlayManager.get().handleMouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY)) {
            cir.setReturnValue(true);
            return;
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void yang$mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!isPackUtilActive()) return;

        if (PackUtilOverlayManager.get().handleMouseScrolled(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
            return;
        }
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
        if (!isPackUtilActive()) return super.charTyped(input);

        if (PackUtilOverlayManager.get().handleCharTyped((char) input.codepoint(), 0)) {
            return true;
        }

        return super.charTyped(input);
    }
}
