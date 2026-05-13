package autismclient.util;

import autismclient.gui.packui.PackUiHudPanelRenderer;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class PackUtilQueueRenderer {
    private static final PackUiTheme THEME = new PackUiTheme();
    private static final long CACHE_REFRESH_NANOS = 50_000_000L;
    private static final int ENTRY_COLOR = 0xFFE7DCDD;
    private static final int ENTRY_SENDING_COLOR = 0xFFF6D28D;
    private static final int STATUS_COLOR = 0xFF9EDAB0;
    private static final int ACTIVE_ACCENT_COLOR = 0xFF73D98E;
    private static final Identifier MUTED_FONT = THEME.fontFor(PackUiTone.MUTED);
    private static final Identifier BODY_FONT = THEME.fontFor(PackUiTone.BODY);
    private static final Identifier LABEL_FONT = THEME.fontFor(PackUiTone.LABEL);
    private static final int MUTED_COLOR = THEME.color(PackUiTone.MUTED);
    private static final int HEADER_COLOR = THEME.color(PackUiTone.BODY);

    private static boolean cachedSending;
    private static String cachedModeStr = null;
    private static int cachedMaxLines = Integer.MIN_VALUE;
    private static int cachedPanelWidth = Integer.MIN_VALUE;
    private static int cachedPacketCount = Integer.MIN_VALUE;
    private static int cachedQueueRevision = Integer.MIN_VALUE;
    private static String cachedTitle = "";
    private static int cachedAccentColor;
    private static List<PackUiHudPanelRenderer.Row> cachedRows = List.of();
    private static long lastCacheBuildNanos;

    private PackUtilQueueRenderer() {}

    public static void render(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int width, int maxLines) {
        if (textRenderer == null) return;

        PackUtilSharedState shared = PackUtilSharedState.get();

        boolean isSending = shared.hasStaggeredPackets();
        String modeStr = shared.getQueueDisplayDelayMode() == PackUtilSharedState.DelayMode.MS ? "ms" : "t";
        int panelWidth = width + 12;
        int queueRevision = shared.getQueueRenderRevision();
        long now = System.nanoTime();
        if (shouldRebuildCache(isSending, modeStr, maxLines, panelWidth, queueRevision, now)) {
            rebuildCache(textRenderer, shared.getQueueRenderSnapshot(isSending, maxLines), isSending, modeStr, maxLines, panelWidth, queueRevision, now);
        }

        PackUiHudPanelRenderer.renderPreTrimmed(context, textRenderer, x - 6, y - 8, panelWidth, cachedTitle, cachedRows, cachedAccentColor);
    }

    private static boolean shouldRebuildCache(boolean isSending, String modeStr, int maxLines, int panelWidth, int queueRevision, long now) {
        boolean layoutChanged = cachedSending != isSending
            || !java.util.Objects.equals(cachedModeStr, modeStr)
            || cachedMaxLines != maxLines
            || cachedPanelWidth != panelWidth;
        if (layoutChanged) {
            return true;
        }
        if (cachedRows.isEmpty()) {
            return true;
        }
        if (cachedQueueRevision == queueRevision) {
            return false;
        }
        return now - lastCacheBuildNanos >= CACHE_REFRESH_NANOS;
    }

    private static void rebuildCache(Font textRenderer, PackUtilSharedState.QueueRenderSnapshot snapshot, boolean isSending, String modeStr, int maxLines, int panelWidth, int queueRevision, long now) {
        List<PackUtilSharedState.QueuedPacket> packets = snapshot.packets();
        int totalCount = snapshot.totalCount();
        int visibleCount = Math.min(packets.size(), maxLines);
        int contentWidth = panelWidth - (6 * 2);
        ArrayList<PackUiHudPanelRenderer.Row> rows = new ArrayList<>(visibleCount + 2);
        rows.add(new PackUiHudPanelRenderer.Row(
            PackUiText.trimToWidth(textRenderer, isSending ? "Sending " + totalCount + " left [" + modeStr + "]" : totalCount + " queued [" + modeStr + "]", contentWidth, BODY_FONT, STATUS_COLOR),
            BODY_FONT,
            STATUS_COLOR
        ));

        if (packets.isEmpty()) {
            rows.add(new PackUiHudPanelRenderer.Row(
                PackUiText.trimToWidth(textRenderer, "Queue empty", contentWidth, MUTED_FONT, MUTED_COLOR),
                MUTED_FONT,
                MUTED_COLOR
            ));
        } else {
            for (int i = 0; i < visibleCount; i++) {
                PackUtilSharedState.QueuedPacket qp = packets.get(i);
                String simpleName = PackUtilPacketNamer.getFriendlyName(qp.packet);
                String delayStr = qp.getDelay() > 0 ? " +" + qp.getDelay() + modeStr : "";
                String label = "#" + qp.getId() + " " + simpleName + delayStr;
                int color = (isSending && i == 0) ? ENTRY_SENDING_COLOR : ENTRY_COLOR;
                rows.add(new PackUiHudPanelRenderer.Row(
                    PackUiText.trimToWidth(textRenderer, label, contentWidth, BODY_FONT, color),
                    BODY_FONT,
                    color
                ));
            }
        }

        cachedSending = isSending;
        cachedModeStr = modeStr;
        cachedMaxLines = maxLines;
        cachedPanelWidth = panelWidth;
        cachedPacketCount = totalCount;
        cachedQueueRevision = queueRevision;
        cachedTitle = PackUiText.trimToWidth(textRenderer, "PACKET QUEUE", contentWidth, LABEL_FONT, HEADER_COLOR);
        cachedAccentColor = isSending ? ACTIVE_ACCENT_COLOR : THEME.headerAccent();
        cachedRows = rows;
        lastCacheBuildNanos = now;
    }
}
