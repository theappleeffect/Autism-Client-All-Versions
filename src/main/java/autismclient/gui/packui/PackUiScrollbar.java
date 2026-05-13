package autismclient.gui.packui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class PackUiScrollbar {
    public record Metrics(int trackX, int trackY, int trackWidth, int trackHeight, int thumbY, int thumbHeight, int maxScroll) {
        public boolean hasScroll() {
            return maxScroll > 0;
        }

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= trackX && mouseX < trackX + trackWidth && mouseY >= trackY && mouseY < trackY + trackHeight;
        }

        public boolean overThumb(double mouseX, double mouseY) {
            return hasScroll()
                && mouseX >= trackX && mouseX < trackX + trackWidth
                && mouseY >= thumbY && mouseY < thumbY + thumbHeight;
        }
    }

    private static final PackUiTheme THEME = new PackUiTheme();

    private PackUiScrollbar() {
    }

    public static Metrics compute(int contentPixels, int viewPixels, int trackX, int trackY, int trackWidth, int trackHeight, int scrollOffset) {
        return compute(contentPixels, viewPixels, trackX, trackY, trackWidth, trackHeight, (float) scrollOffset);
    }

    public static Metrics compute(int contentPixels, int viewPixels, int trackX, int trackY, int trackWidth, int trackHeight, float scrollOffset) {
        if (contentPixels <= 0 || viewPixels <= 0 || trackHeight <= 0) {
            return new Metrics(trackX, trackY, trackWidth, trackHeight, trackY, trackHeight, 0);
        }

        int maxScroll = Math.max(0, contentPixels - viewPixels);
        if (maxScroll <= 0) {
            return new Metrics(trackX, trackY, trackWidth, trackHeight, trackY, trackHeight, 0);
        }

        int thumbHeight = Math.max(18, (int) (((float) viewPixels / Math.max(1, contentPixels)) * trackHeight));
        thumbHeight = Math.min(trackHeight, thumbHeight);
        int travel = Math.max(1, trackHeight - thumbHeight);
        float clampedScroll = Math.max(0.0f, Math.min(scrollOffset, maxScroll));
        int thumbY = trackY + Math.round((clampedScroll / maxScroll) * travel);
        return new Metrics(trackX, trackY, trackWidth, trackHeight, thumbY, thumbHeight, maxScroll);
    }

    public static void draw(GuiGraphicsExtractor context, Metrics metrics, boolean hovered, boolean dragging) {
        if (metrics == null || !metrics.hasScroll()) return;

        int trackColor = THEME.overlaySurfaceSoft(0x00101114);
        int thumbColor = dragging ? THEME.headerAccent() : (hovered ? 0xFFD86B6B : THEME.borderSoft());
        int shine = hovered || dragging ? 0x28FFFFFF : 0x14FFFFFF;

        context.fill(metrics.trackX(), metrics.trackY(), metrics.trackX() + metrics.trackWidth(), metrics.trackY() + metrics.trackHeight(), trackColor);
        context.fill(metrics.trackX(), metrics.trackY(), metrics.trackX() + metrics.trackWidth(), metrics.trackY() + 1, THEME.borderColor());
        context.fill(metrics.trackX(), metrics.trackY() + metrics.trackHeight() - 1, metrics.trackX() + metrics.trackWidth(), metrics.trackY() + metrics.trackHeight(), THEME.borderColor());

        context.fill(metrics.trackX(), metrics.thumbY(), metrics.trackX() + metrics.trackWidth(), metrics.thumbY() + metrics.thumbHeight(), thumbColor);
        context.fill(metrics.trackX(), metrics.thumbY(), metrics.trackX() + metrics.trackWidth(), metrics.thumbY() + 1, shine);
    }

    public static int scrollFromThumb(Metrics metrics, double mouseY, int grabOffset) {
        if (metrics == null || !metrics.hasScroll()) return 0;
        int travel = Math.max(1, metrics.trackHeight() - metrics.thumbHeight());
        int thumbTop = (int) Math.round(mouseY) - grabOffset;
        thumbTop = Math.max(metrics.trackY(), Math.min(metrics.trackY() + travel, thumbTop));
        float progress = (float) (thumbTop - metrics.trackY()) / travel;
        return Math.max(0, Math.min(metrics.maxScroll(), Math.round(progress * metrics.maxScroll())));
    }
}
