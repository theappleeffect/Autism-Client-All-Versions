package autismclient.gui;

import autismclient.util.PackUtilColors;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.Optional;
import java.util.function.Consumer;

public class PackUtilLoadingOverlay extends LoadingOverlay {
    private static final Identifier CUSTOM_LOGO =
        Identifier.fromNamespaceAndPath("autismclient", "textures/gui/title/loading_logo.png");
    private static final int LOGO_WIDTH = 2106;
    private static final int LOGO_HEIGHT = 1297;
    private static final int BG_COLOR = PackUtilColors.loadingBg();
    private static final int BAR_R = 236;
    private static final int BAR_G = 32;
    private static final int BAR_B = 39;

    private static final long FADE_OUT_TIME = 1000L;
    private static final long FADE_IN_TIME = 500L;

    private final Minecraft packutil$minecraft;
    private final ReloadInstance packutil$reload;
    private final Consumer<Optional<Throwable>> packutil$onFinish;
    private final boolean packutil$fadeIn;
    private float packutil$currentProgress;
    private long packutil$fadeOutStart = -1L;
    private long packutil$fadeInStart = -1L;

    public PackUtilLoadingOverlay(Minecraft minecraft, ReloadInstance reload,
                                   Consumer<Optional<Throwable>> onFinish, boolean fadeIn) {
        super(minecraft, reload, onFinish, fadeIn);
        this.packutil$minecraft = minecraft;
        this.packutil$reload = reload;
        this.packutil$onFinish = onFinish;
        this.packutil$fadeIn = fadeIn;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        // Pre-1.21.9 Overlay has no tick(); drive the fade-out/finish logic from render() so the
        // loading overlay actually completes and hands off to the title screen.
        //? if <1.21.9 {
        /*this.tick();
        *///?}
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        long now = Util.getMillis();

        if (this.packutil$fadeIn && this.packutil$fadeInStart == -1L) {
            this.packutil$fadeInStart = now;
        }

        float fadeOutAnim = this.packutil$fadeOutStart > -1L ? (float) (now - this.packutil$fadeOutStart) / (float) FADE_OUT_TIME : -1.0F;
        float fadeInAnim = this.packutil$fadeInStart > -1L ? (float) (now - this.packutil$fadeInStart) / (float) FADE_IN_TIME : -1.0F;
        float logoAlpha;

        if (fadeOutAnim >= 1.0F) {
            if (this.packutil$minecraft.screen != null) {
                this.packutil$minecraft.screen.renderWithTooltipAndSubtitles(graphics, 0, 0, a);
            } else {
                ;
            }

            int alpha = Mth.ceil((1.0F - Mth.clamp(fadeOutAnim - 1.0F, 0.0F, 1.0F)) * 255.0F);
            graphics.nextStratum();
            graphics.fill(0, 0, width, height, replaceAlpha(BG_COLOR, alpha));
            logoAlpha = 1.0F - Mth.clamp(fadeOutAnim - 1.0F, 0.0F, 1.0F);
        } else if (this.packutil$fadeIn) {
            if (this.packutil$minecraft.screen != null && fadeInAnim < 1.0F) {
                this.packutil$minecraft.screen.renderWithTooltipAndSubtitles(graphics, mouseX, mouseY, a);
            } else {
                ;
            }

            int alpha = Mth.ceil(Mth.clamp((double) fadeInAnim, 0.15, 1.0) * 255.0);
            graphics.nextStratum();
            graphics.fill(0, 0, width, height, replaceAlpha(BG_COLOR, alpha));
            logoAlpha = Mth.clamp(fadeInAnim, 0.0F, 1.0F);
        } else {
            graphics.fill(0, 0, width, height, BG_COLOR);
            logoAlpha = 1.0F;
        }

        if (logoAlpha > 0.0F) {
            drawCustomLogo(graphics, width, height, logoAlpha);
        }

        float actualProgress = this.packutil$reload.getActualProgress();
        this.packutil$currentProgress = Mth.clamp(this.packutil$currentProgress * 0.95F + actualProgress * 0.050000012F, 0.0F, 1.0F);

        if (fadeOutAnim < 1.0F) {
            drawProgressBar(graphics, width, height, fadeOutAnim);
        }

        if (fadeOutAnim >= 2.0F) {
            this.packutil$minecraft.setOverlay(null);
        }
    }

    private void drawCustomLogo(GuiGraphics graphics, int width, int height, float alpha) {
        int centerX = width / 2;
        int centerY = height / 2;

        double maxW = width * 0.85;
        double maxH = height * 0.85;
        double scale = Math.min(maxW / LOGO_WIDTH, maxH / LOGO_HEIGHT) * 0.95;

        int drawW = (int) (LOGO_WIDTH * scale);
        int drawH = (int) (LOGO_HEIGHT * scale);

        int x = centerX - drawW / 2;
        int y = centerY - drawH / 2;
        int color = ARGB.white(alpha);

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            CUSTOM_LOGO,
            x, y, 0.0F, 0.0F,
            drawW, drawH,
            LOGO_WIDTH, LOGO_HEIGHT,
            LOGO_WIDTH, LOGO_HEIGHT,
            color
        );
    }

    private void drawProgressBar(GuiGraphics graphics, int width, int height, float fadeOutAnim) {
        float barFade = 1.0F - Mth.clamp(fadeOutAnim, 0.0F, 1.0F);

        double maxW = width * 0.85;
        double maxH = height * 0.85;
        double scale = Math.min(maxW / LOGO_WIDTH, maxH / LOGO_HEIGHT) * 0.95;
        int drawW = (int) (LOGO_WIDTH * scale);
        int drawH = (int) (LOGO_HEIGHT * scale);

        int centerX = width / 2;
        int logoBottom = height / 2 + drawH / 2;
        int barY = logoBottom + 10;

        int x0 = centerX - drawW / 2;
        int y0 = barY - 5;
        int x1 = centerX + drawW / 2;
        int y1 = barY + 5;

        int barWidth = Mth.ceil((x1 - x0 - 2) * this.packutil$currentProgress);
        int alpha = Math.round(barFade * 255.0F);
        int barColor = ARGB.color(alpha, BAR_R, BAR_G, BAR_B);

        if (barWidth > 0) {
            graphics.fill(x0 + 2, y0 + 2, x0 + 2 + barWidth, y1 - 2, barColor);
        }

        graphics.fill(x0 + 1, y0, x1 - 1, y0 + 1, barColor);
        graphics.fill(x0 + 1, y1, x1 - 1, y1 - 1, barColor);
        graphics.fill(x0, y0, x0 + 1, y1, barColor);
        graphics.fill(x1, y0, x1 - 1, y1, barColor);
    }

    private static int replaceAlpha(int color, int alpha) {
        return color & 0x00FFFFFF | (alpha << 24);
    }

    //? if >=1.21.9 {
    @Override
    //?}
    public void tick() {
        if (this.packutil$fadeOutStart == -1L && this.packutil$reload.isDone() && this.packutil$isReadyToFadeOut()) {
            try {
                this.packutil$reload.checkExceptions();
                this.packutil$onFinish.accept(Optional.empty());
            } catch (Throwable t) {
                this.packutil$onFinish.accept(Optional.of(t));
            }

            this.packutil$fadeOutStart = Util.getMillis();
            if (this.packutil$minecraft.screen != null) {
                Window window = this.packutil$minecraft.getWindow();
                this.packutil$minecraft.screen.init(window.getGuiScaledWidth(), window.getGuiScaledHeight());
            }
        }
    }

    private boolean packutil$isReadyToFadeOut() {
        return !this.packutil$fadeIn || this.packutil$fadeInStart > -1L && Util.getMillis() - this.packutil$fadeInStart >= 1000L;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
