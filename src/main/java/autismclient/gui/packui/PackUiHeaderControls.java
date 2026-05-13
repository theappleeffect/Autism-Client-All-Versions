package autismclient.gui.packui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class PackUiHeaderControls {
    private static final PackUiTheme THEME = new PackUiTheme();
    private static final float ANIMATION_DURATION_SECONDS = 0.0f;
    private static final float COLLAPSED_CLOSE_VISIBILITY = 0.72f;

    private PackUiHeaderControls() {
    }

    public static float animate(float current, float target, float delta) {
        float amount = ANIMATION_DURATION_SECONDS <= 0.0f
            ? 1.0f
            : Math.max(0.03f, Math.min(1.0f, delta / ANIMATION_DURATION_SECONDS));
        if (current < target) return Math.min(target, current + amount);
        if (current > target) return Math.max(target, current - amount);
        return current;
    }

    public static int controlY(int panelY, int headerHeight, int controlSize) {
        return panelY + Math.max(1, (headerHeight - controlSize) / 2);
    }

    public static int closeX(int panelX, int panelWidth, int controlSize, int rightInset) {
        return panelX + panelWidth - controlSize - rightInset;
    }

    public static int expandedArrowX(int closeX, int gap, int arrowWidth) {
        return closeX - gap - arrowWidth;
    }

    public static boolean isCloseHit(float visibility, double mouseX, double mouseY, int x, int y, int controlSize) {
        return mouseX >= x && mouseX <= x + controlSize
            && mouseY >= y && mouseY <= y + controlSize;
    }

    public static void drawAnimatedArrow(GuiGraphicsExtractor context, int x, int y, int size, float expandedProgress, float alpha) {
        PackUiControlGlyphs.drawChevron(
            context,
            x,
            y,
            size,
            clamp01(expandedProgress) >= 0.5f ? PackUiControlGlyphs.ChevronDirection.DOWN : PackUiControlGlyphs.ChevronDirection.RIGHT,
            0xFFF4EDED,
            0xC43D171B,
            alpha
        );
    }

    public static void drawCloseButton(GuiGraphicsExtractor context, int x, int y, int width, int height, float hover, boolean active, float visibility) {
        float shownVisibility = COLLAPSED_CLOSE_VISIBILITY + ((1.0f - COLLAPSED_CLOSE_VISIBILITY) * clamp01(visibility));
        float alpha = clamp01((active ? 1.0f : 0.56f) * shownVisibility);
        int bg = active ? THEME.headerControlFillActive() : THEME.headerControlFill();
        int border = active ? THEME.headerControlBorderActive() : THEME.headerControlBorder();
        context.fill(x, y, x + width, y + height, PackUiRenderContext.applyAlpha(bg, alpha));
        context.fill(x, y, x + width, y + 1, PackUiRenderContext.applyAlpha(border, alpha));
        context.fill(x, y + height - 1, x + width, y + height, PackUiRenderContext.applyAlpha(border, alpha));
        context.fill(x, y, x + 1, y + height, PackUiRenderContext.applyAlpha(border, alpha));
        context.fill(x + width - 1, y, x + width, y + height, PackUiRenderContext.applyAlpha(border, alpha));
        if (hover > 0.0f) {
            int hoverTint = ((int) (clamp01(hover) * 18) << 24) | 0x00FF4A4A;
            context.fill(x + 1, y + 1, x + width - 1, y + height - 1, PackUiRenderContext.applyAlpha(hoverTint, alpha));
        }
        PackUiControlGlyphs.drawClose(context, x + 1, y + 1, Math.max(1, Math.min(width, height) - 2), 0xFFF6EEEE, 0xD64A1A1F, alpha);
    }

    private static void drawIcon(GuiGraphicsExtractor context, Identifier icon, int x, int y, int size, float alpha) {
        if (context == null || icon == null || size <= 0 || alpha <= 0.001f) return;
        if (PackUiControlGlyphs.isCloseIcon(icon) || PackUiControlGlyphs.isChevronIcon(icon)) {
            PackUiControlGlyphs.drawKnownIcon(context, icon, x, y, size, 0xFFF4EDED, 0xC43D171B, alpha);
            return;
        }
        context.blit(icon, x, y, x + size, y + size, 0.0f, 1.0f, 0.0f, 1.0f);
        if (alpha < 0.999f) {
            int overlayAlpha = Math.max(0, Math.min(255, Math.round((1.0f - alpha) * 255.0f)));
            context.fill(x, y, x + size, y + size, (overlayAlpha << 24) | 0x00070709);
        }
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
