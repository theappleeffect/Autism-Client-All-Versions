package autismclient.mixin;

import autismclient.util.PackUtilOverlayManager;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class PackUtilClickableWidgetMixin {
    @Shadow protected boolean isHovered;

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractWidget;isHovered:Z", shift = At.Shift.AFTER), require = 0)
    private void packutil$suppressHoverWhenOverlayBlocks(net.minecraft.client.gui.GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (PackUtilOverlayManager.get().shouldBlockUnderlyingHover(mouseX, mouseY)) {
            isHovered = false;
        }
    }
}
