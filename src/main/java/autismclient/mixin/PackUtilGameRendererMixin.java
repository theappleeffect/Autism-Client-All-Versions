package autismclient.mixin;

import autismclient.gui.packui.PackUiText;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class PackUtilGameRendererMixin {

    // Inject at the end of the render method (after the GUI has drawn) rather than
    // at a specific internal call site, which is renamed across versions.
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void packutil$afterGuiRender(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        PackUiText.flushPendingOverlayText();
    }
}
