package autismclient.mixin;

import autismclient.gui.packui.PackUiTextMeshUniforms;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class PackUtilRenderSystemMixin {
    // Capture no target args (only CallbackInfo): flipFrame's parameter list
    // changes across versions, and we don't use the args anyway.
    @Inject(method = "flipFrame", at = @At("TAIL"), require = 0)
    private static void packutil$flipFrame(CallbackInfo info) {
        PackUiTextMeshUniforms.flipFrame();
    }
}
