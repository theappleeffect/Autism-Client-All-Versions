package autismclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import autismclient.modules.PackUtilModule;
import autismclient.util.PackUtilOverlayManager;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.CreativeModeTab;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class PackUtilCreativeScreenMixin {
    // renderTabButton gained (mouseX, mouseY) params in 1.21.11; before that it is 2-arg
    // (GuiGraphics, CreativeModeTab) with no mouse coords, so the cursor-suppression recall below
    // can't be expressed. Gate the whole sub-feature to 1.21.11 (checkTabHovering still blocks the
    // hover on all versions).
    //? if >=1.21.11 {
    @Shadow protected abstract void renderTabButton(GuiGraphics graphics, int mouseX, int mouseY, CreativeModeTab tab);

    @org.spongepowered.asm.mixin.Unique
    private static final ThreadLocal<Boolean> packutil$inSafeRecall = ThreadLocal.withInitial(() -> Boolean.FALSE);
    //?}

    @Inject(method = "checkTabHovering", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$blockCoveredTabHover(GuiGraphics graphics, CreativeModeTab tab, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        PackUtilModule module = PackUtilModule.get();
        if (module == null || !module.isActive()) return;
        if (PackUtilOverlayManager.get().shouldBlockUnderlyingHover(mouseX, mouseY)) {
            cir.setReturnValue(false);
        }
    }

    //? if >=1.21.11 {
    @Inject(method = "renderTabButton", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$blockCoveredTabCursor(GuiGraphics graphics, int mouseX, int mouseY, CreativeModeTab tab, CallbackInfo ci) {
        if (packutil$inSafeRecall.get()) return;
        PackUtilModule module = PackUtilModule.get();
        if (module == null || !module.isActive()) return;
        if (!PackUtilOverlayManager.get().shouldBlockUnderlyingHover(mouseX, mouseY)) return;

        packutil$inSafeRecall.set(Boolean.TRUE);
        try {
            this.renderTabButton(graphics, PackUtilOverlayManager.HOVER_BLOCKED_MOUSE, PackUtilOverlayManager.HOVER_BLOCKED_MOUSE, tab);
        } finally {
            packutil$inSafeRecall.set(Boolean.FALSE);
        }
        ci.cancel();
    }
    //?}

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        PackUtilModule module = PackUtilModule.get();
        if (module == null || !module.isActive()) return;

        if (PackUtilOverlayManager.get().handleMouseClicked(event.x(), event.y(), event.button())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$mouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        PackUtilModule module = PackUtilModule.get();
        if (module == null || !module.isActive()) return;

        if (PackUtilOverlayManager.get().handleMouseReleased(event.x(), event.y(), event.button())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$mouseDragged(MouseButtonEvent event, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        PackUtilModule module = PackUtilModule.get();
        if (module == null || !module.isActive()) return;

        if (PackUtilOverlayManager.get().handleMouseDragged(event.x(), event.y(), event.button(), deltaX, deltaY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        PackUtilModule module = PackUtilModule.get();
        if (module == null || !module.isActive()) return;

        if (PackUtilOverlayManager.get().handleMouseScrolled(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        PackUtilModule module = PackUtilModule.get();
        if (module == null || !module.isActive()) return;

        if (PackUtilOverlayManager.get().handleKeyPressed(input.key(), input.scancode(), input.modifiers())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$charTyped(CharacterEvent input, CallbackInfoReturnable<Boolean> cir) {
        PackUtilModule module = PackUtilModule.get();
        if (module == null || !module.isActive()) return;

        if (PackUtilOverlayManager.get().handleCharTyped((char) input.codepoint(), 0)) {
            cir.setReturnValue(true);
        }
    }
}
