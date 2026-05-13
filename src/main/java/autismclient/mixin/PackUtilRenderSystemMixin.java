package autismclient.mixin;

import autismclient.gui.packui.PackUiTextMeshUniforms;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class PackUtilRenderSystemMixin {
    @Inject(method = "flipFrame", at = @At("TAIL"))
    private static void packutil$flipFrame(TracyFrameCapture tracyFrameCapture, CallbackInfo info) {
        PackUiTextMeshUniforms.flipFrame();
    }
}
