package autismclient.mixin;

import autismclient.modules.PackUtilModule;
import autismclient.util.PackUtilClientMessaging;
import autismclient.util.PackUtilCustomFilterOverlay;
import autismclient.util.PackUtilCustomFilterPresetOverlay;
import autismclient.util.PackUtilKeybindOverlay;
import autismclient.util.PackUtilLANSync;
import autismclient.util.PackUtilLANSyncOverlay;
import autismclient.util.PackUtilLauncherOverlay;
import autismclient.util.PackUtilMacroEditorOverlay;
import autismclient.util.PackUtilMacroListOverlay;
import autismclient.util.PackUtilOverlayManager;
import autismclient.util.PackUtilPacketLoggerOverlay;
import autismclient.util.PackUtilQueueEditorOverlay;
import autismclient.util.PackUtilSharedState;
import autismclient.util.PackUtilSpecialGuiActions;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BookSignScreen.class)
public abstract class PackUtilBookSignScreenMixin extends Screen implements PackUtilSpecialGuiActions {
    @Unique private static final Minecraft MC = Minecraft.getInstance();

    @Unique private PackUtilLauncherOverlay launcherOverlay;
    @Unique private PackUtilLANSyncOverlay lanSyncOverlay;
    @Unique private PackUtilMacroListOverlay macroListOverlay;
    @Unique private PackUtilQueueEditorOverlay queueEditorOverlay;
    @Unique private PackUtilPacketLoggerOverlay packetLoggerOverlay;
    @Unique private PackUtilCustomFilterOverlay customFilterOverlay;
    @Unique private PackUtilCustomFilterPresetOverlay customFilterPresetOverlay;
    @Unique private PackUtilMacroEditorOverlay macroEditorOverlay;
    @Unique private PackUtilKeybindOverlay keybindOverlay;
    @Unique private autismclient.util.PackUtilServerInfoOverlay serverInfoOverlay;

    protected PackUtilBookSignScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void packutil$init(CallbackInfo ci) {
        if (!packutil$isPackUtilActive()) return;

        PackUtilLANSync.getInstance().setOnSessionStateChanged(() -> {});

        lanSyncOverlay = PackUtilLANSyncOverlay.getSharedOverlay(this.font);
        macroListOverlay = new PackUtilMacroListOverlay(this.font);
        queueEditorOverlay = new PackUtilQueueEditorOverlay(this.font);
        customFilterOverlay = new PackUtilCustomFilterOverlay(this.font);
        customFilterPresetOverlay = customFilterOverlay.getPresetManagerOverlay();

        lanSyncOverlay.restoreState();
        macroListOverlay.restoreState();
        queueEditorOverlay.restoreState();
        customFilterOverlay.restoreLayout();
        if (customFilterPresetOverlay != null) customFilterPresetOverlay.restoreLayout();

        macroEditorOverlay = PackUtilMacroEditorOverlay.getSharedOverlay();
        if (macroEditorOverlay != null) macroEditorOverlay.restoreState();

        PackUtilOverlayManager manager = PackUtilOverlayManager.get();
        manager.clear();
        manager.register(lanSyncOverlay);
        manager.register(macroListOverlay);
        manager.register(queueEditorOverlay);
        manager.register(customFilterOverlay);
        if (customFilterPresetOverlay != null) manager.register(customFilterPresetOverlay);
        if (macroEditorOverlay != null) manager.register(macroEditorOverlay);

        PackUtilModule packutilModule = PackUtilModule.get();
        if (packutilModule != null) {
            packetLoggerOverlay = packutilModule.getPacketLoggerOverlay();
            if (packetLoggerOverlay != null) {
                packetLoggerOverlay.restoreLayout();
                manager.register(packetLoggerOverlay);
            }
        }

        keybindOverlay = new PackUtilKeybindOverlay();
        keybindOverlay.restoreLayout();
        manager.register(keybindOverlay);

        serverInfoOverlay = PackUtilModule.get().getServerDataOverlay();
        manager.register(serverInfoOverlay);

        launcherOverlay = new PackUtilLauncherOverlay(macroListOverlay, null, lanSyncOverlay, queueEditorOverlay, packetLoggerOverlay, customFilterOverlay);
        launcherOverlay.setKeybindOverlay(keybindOverlay);
        launcherOverlay.setServerDataOverlay(serverInfoOverlay);
        launcherOverlay.restoreLayout();
        manager.register(launcherOverlay);

        Screen screen = (Screen) (Object) this;
        ScreenEvents.afterRender(screen).register((scrn, drawContext, mouseX, mouseY, tickDelta) -> {
            if (packutil$isPackUtilActive()) {
                PackUtilOverlayManager.get().renderAll(drawContext, mouseX, mouseY, tickDelta);
            }
        });
    }

    @Override
    public void removed() {
        if (packutil$isPackUtilActive()) {
            if (lanSyncOverlay != null) lanSyncOverlay.saveState();
            if (macroListOverlay != null) macroListOverlay.saveState();
            if (queueEditorOverlay != null) queueEditorOverlay.saveState();
            if (macroEditorOverlay != null) macroEditorOverlay.saveState();
            if (launcherOverlay != null) launcherOverlay.saveLayout();
            if (packetLoggerOverlay != null) packetLoggerOverlay.saveLayout();
            if (customFilterOverlay != null) customFilterOverlay.saveLayout();
            if (customFilterPresetOverlay != null) customFilterPresetOverlay.saveLayout();
            if (keybindOverlay != null) keybindOverlay.saveLayout();
            if (serverInfoOverlay != null) serverInfoOverlay.saveState();
            PackUtilOverlayManager.get().clear();
        }
        super.removed();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (packutil$isPackUtilActive() && PackUtilOverlayManager.get().handleMouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (packutil$isPackUtilActive() && PackUtilOverlayManager.get().handleMouseReleased(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (packutil$isPackUtilActive() && PackUtilOverlayManager.get().handleMouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (packutil$isPackUtilActive() && PackUtilOverlayManager.get().handleMouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (!packutil$isPackUtilActive()) return;
        if (PackUtilOverlayManager.get().handleKeyPressed(input.key(), input.scancode(), input.modifiers())) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (packutil$isPackUtilActive() && PackUtilOverlayManager.get().handleCharTyped((char) input.codepoint(), 0)) {
            return true;
        }
        return super.charTyped(input);
    }

    @Unique
    private boolean packutil$isPackUtilActive() {
        PackUtilModule module = PackUtilModule.get();
        return module != null && module.isActive();
    }

    @Override
    public void packutil$closeWithPacket() {
        packutil$closeWithPacket(true);
    }

    @Override
    public void packutil$closeWithPacket(boolean notify) {
        if (MC.getConnection() != null) {
            PackUtilSharedState.get().setForceNextBookEditPacket(true);
            packutil$invokeSaveChanges();
        }
        MC.setScreen(null);
    }

    @Override
    public void packutil$closeWithoutPacket() {
        packutil$closeWithoutPacket(true);
    }

    @Override
    public void packutil$closeWithoutPacket(boolean notify) {
        MC.setScreen(null);
        if (notify) PackUtilClientMessaging.sendPrefixed("Book signing closed without packet.");
    }

    @Override
    public void packutil$desync() {
        packutil$desync(true);
    }

    @Override
    public void packutil$desync(boolean notify) {
        if (MC.getConnection() == null) {
            if (notify) PackUtilClientMessaging.sendPrefixed("Failed to desync: no network.");
            return;
        }
        PackUtilSharedState.get().setForceNextBookEditPacket(true);
        packutil$invokeSaveChanges();
        if (notify) PackUtilClientMessaging.sendPrefixed("Signed book packet sent; GUI intentionally stays open.");
    }

    @Invoker("saveChanges")
    protected abstract void packutil$invokeSaveChanges();
}
