package autismclient.gui.packui;

import net.minecraft.client.gui.Font;

public final class PackUiLegacyLayout {
    private PackUiLegacyLayout() {
    }

    public static int contentWidth(int panelWidth, int horizontalPadding) {
        return Math.max(0, panelWidth - (horizontalPadding * 2));
    }

    public static int reserveScrollbar(int width, boolean hasScroll, int gutter) {
        return Math.max(0, width - (hasScroll ? gutter : 0));
    }

    public static int fitOverlayButtonWidth(Font renderer, PackUiTheme theme, PackUiTone tone, String label, int horizontalPadding, int minWidth, int maxWidth) {
        if (renderer == null || theme == null) return Math.max(minWidth, 0);
        return PackUiSizing.fitTextWidthInt(
            renderer,
            label == null ? "" : label,
            theme.fontFor(tone == null ? PackUiTone.BODY : tone),
            theme.color(tone == null ? PackUiTone.BODY : tone),
            horizontalPadding,
            minWidth,
            maxWidth
        );
    }

    public static int rightAlign(int leftX, int totalWidth, int childWidth, int rightInset) {
        return leftX + Math.max(0, totalWidth - childWidth - rightInset);
    }

    public static int rowY(int startY, int rowHeight, int gap, int index) {
        return startY + (Math.max(0, index) * (rowHeight + Math.max(0, gap)));
    }
}
