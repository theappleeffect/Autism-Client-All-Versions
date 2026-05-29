package autismclient.gui.packui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public final class PackUiControlGlyphs {
    public enum ChevronDirection {
        RIGHT,
        DOWN,
        UP
    }

    private PackUiControlGlyphs() {
    }

    public static boolean isCloseIcon(Identifier icon) {
        return PackUiAssets.ICON_WINDOW_CLOSE.equals(icon);
    }

    public static boolean isChevronIcon(Identifier icon) {
        return PackUiAssets.ICON_WINDOW_CHEVRON_RIGHT.equals(icon)
            || PackUiAssets.ICON_WINDOW_CHEVRON_DOWN.equals(icon)
            || PackUiAssets.ICON_WINDOW_CHEVRON_UP.equals(icon);
    }

    public static void drawKnownIcon(GuiGraphics context, Identifier icon, int x, int y, int size, int color, int shadowColor, float alpha) {
        if (icon == null || context == null || size <= 0) return;
        drawTexture(context, icon, x, y, size, alpha);
    }

    public static void drawClose(GuiGraphics context, int x, int y, int size, int color, int shadowColor, float alpha) {
        drawTexture(context, PackUiAssets.ICON_WINDOW_CLOSE, x, y, size, alpha);
    }

    public static void drawChevron(GuiGraphics context, int x, int y, int size, ChevronDirection direction, int color, int shadowColor, float alpha) {
        drawTexture(context, chevronIcon(direction), x, y, size, alpha);
    }

    public static void drawChevronProgress(GuiGraphics context, int x, int y, int size, float progress, int color, int shadowColor, float alpha) {
        float clamped = Math.max(0.0f, Math.min(1.0f, progress));
        drawTexture(
            context,
            clamped >= 0.5f ? PackUiAssets.ICON_WINDOW_CHEVRON_DOWN : PackUiAssets.ICON_WINDOW_CHEVRON_RIGHT,
            x,
            y,
            size,
            alpha
        );
    }

    private static Identifier chevronIcon(ChevronDirection direction) {
        return switch (direction) {
            case DOWN -> PackUiAssets.ICON_WINDOW_CHEVRON_DOWN;
            case UP -> PackUiAssets.ICON_WINDOW_CHEVRON_UP;
            case RIGHT -> PackUiAssets.ICON_WINDOW_CHEVRON_RIGHT;
        };
    }

    private static void drawLineCloseFallback(GuiGraphics context, int x, int y, int size, int color, int shadowColor, float alpha) {
        if (context == null || size <= 0 || alpha <= 0.001f) return;
        int stroke = Math.max(1, size / 7);
        int inset = Math.max(2, Math.round(size * 0.25f));
        int shadow = applyAlpha(shadowColor, alpha);
        int fg = applyAlpha(color, alpha);
        int left = x + inset;
        int right = x + size - inset - 1;
        int top = y + inset;
        int bottom = y + size - inset - 1;
        drawLine(context, left + 1, top + 1, right + 1, bottom + 1, stroke, shadow);
        drawLine(context, right + 1, top + 1, left + 1, bottom + 1, stroke, shadow);
        drawLine(context, left, top, right, bottom, stroke, fg);
        drawLine(context, right, top, left, bottom, stroke, fg);
    }

    private static void drawLineChevronFallback(GuiGraphics context, int x, int y, int size, ChevronDirection direction, int color, int shadowColor, float alpha) {
        if (context == null || size <= 0 || alpha <= 0.001f) return;
        int stroke = Math.max(1, size / 7);
        int inset = Math.max(2, Math.round(size * 0.25f));
        int shadow = applyAlpha(shadowColor, alpha);
        int fg = applyAlpha(color, alpha);
        int left = x + inset;
        int right = x + size - inset - 1;
        int top = y + inset;
        int bottom = y + size - inset - 1;
        int midX = x + size / 2;
        int midY = y + size / 2;

        switch (direction) {
            case RIGHT -> {
                drawLine(context, left + 1, top + 1, right + 1, midY + 1, stroke, shadow);
                drawLine(context, right + 1, midY + 1, left + 1, bottom + 1, stroke, shadow);
                drawLine(context, left, top, right, midY, stroke, fg);
                drawLine(context, right, midY, left, bottom, stroke, fg);
            }
            case DOWN -> {
                drawLine(context, left + 1, top + 1, midX + 1, bottom + 1, stroke, shadow);
                drawLine(context, midX + 1, bottom + 1, right + 1, top + 1, stroke, shadow);
                drawLine(context, left, top, midX, bottom, stroke, fg);
                drawLine(context, midX, bottom, right, top, stroke, fg);
            }
            case UP -> {
                drawLine(context, left + 1, bottom + 1, midX + 1, top + 1, stroke, shadow);
                drawLine(context, midX + 1, top + 1, right + 1, bottom + 1, stroke, shadow);
                drawLine(context, left, bottom, midX, top, stroke, fg);
                drawLine(context, midX, top, right, bottom, stroke, fg);
            }
        }
    }

    private static int applyAlpha(int color, float alpha) {
        int baseAlpha = (color >>> 24) & 0xFF;
        int outAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * Math.max(0.0f, Math.min(1.0f, alpha)))));
        return (outAlpha << 24) | (color & 0x00FFFFFF);
    }

    private static void drawLine(GuiGraphics context, int x0, int y0, int x1, int y1, int thickness, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int radius = Math.max(0, thickness - 1);

        while (true) {
            context.fill(x0, y0, x0 + 1 + radius, y0 + 1 + radius, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static void drawTexture(GuiGraphics context, Identifier icon, int x, int y, int size, float alpha) {
        if (context == null || icon == null || size <= 0 || alpha <= 0.001f) return;
        autismclient.util.PackUtilRender.iconBlit(context, icon, x, y, x + size, y + size);
        if (alpha < 0.999f) {
            int overlayAlpha = Math.max(0, Math.min(255, Math.round((1.0f - alpha) * 255.0f)));
            context.fill(x, y, x + size, y + size, (overlayAlpha << 24) | 0x00070709);
        }
    }
}
