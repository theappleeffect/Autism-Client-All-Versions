package autismclient.util;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Render-rewrite compatibility helpers for pre-1.21.6.
 *
 * <p>1.21.6 added {@code GuiGraphics.nextStratum()} (z-layer separation), the deferred
 * {@code submitSkinRenderState(...)} skin renderer, and {@code GameRenderer.getPanorama()}.
 * Older versions lack these; Stonecutter routes the call sites here (see the
 * {@code current.parsed < "1.21.6"} block in stonecutter.gradle.kts) where they become no-ops
 * (strata, the 3D account-skin preview, and the spinning menu panorama are cosmetic).
 *
 * <p>NOTE: the parameter is named {@code gfx} (never {@code graphics}/{@code context}/{@code ctx})
 * so the receiver-based render replace rules don't rewrite this helper's own calls.
 */
public final class PackUtilRender {
    private PackUtilRender() {
    }

    public static void nextStratum(GuiGraphics gfx) {
        //? if >=1.21.6 {
        gfx.nextStratum();
        //?}
    }

    public static void submitSkin(GuiGraphics gfx, Object model, Object texture, float scale,
                                  float rotX, float rotY, float z, int x0, int y0, int x1, int y1) {
        //? if >=1.21.6 {
        gfx.submitSkinRenderState((net.minecraft.client.model.player.PlayerModel) model,
            (net.minecraft.resources.Identifier) texture, scale, rotX, rotY, z, x0, y0, x1, y1);
        //?}
    }

    public static void renderMenuPanorama(GuiGraphics gfx, int width, int height) {
        //? if >=1.21.6 {
        net.minecraft.client.Minecraft.getInstance().gameRenderer.getPanorama().render(gfx, width, height, true);
        //?}
    }

    // Full-texture icon blit into a rect. 1.21.6+ has blit(tex, x0,y0,x1,y1, u0,u1,v0,v1);
    // pre-1.21.6 takes a Function<Identifier,RenderType> + (x,y,u,v,w,h,texW,texH).
    public static void iconBlit(GuiGraphics gfx, Object icon, int x0, int y0, int x1, int y1) {
        //? if >=1.21.6 {
        gfx.blit((net.minecraft.resources.Identifier) icon, x0, y0, x1, y1, 0.0f, 1.0f, 0.0f, 1.0f);
        //?} else {
        /*gfx.blit(RenderPipelines.GUI_TEXTURED, (net.minecraft.resources.Identifier) icon, x0, y0, 0.0f, 0.0f, x1 - x0, y1 - y0, x1 - x0, y1 - y0);
        *///?}
    }
}
