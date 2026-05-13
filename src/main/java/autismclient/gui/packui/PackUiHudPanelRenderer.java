package autismclient.gui.packui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class PackUiHudPanelRenderer {
    private static final PackUiTheme THEME = new PackUiTheme();

    public record Row(String text, Identifier font, int color) {
        public static Row body(String text, int color) {
            return new Row(text, THEME.fontFor(PackUiTone.BODY), color);
        }

        public static Row muted(String text) {
            return new Row(text, THEME.fontFor(PackUiTone.MUTED), THEME.color(PackUiTone.MUTED));
        }
    }

    private PackUiHudPanelRenderer() {
    }

    public static int render(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int width, String title, List<Row> rows, int accentColor) {
        String titleTrimmed = PackUiText.trimToWidth(textRenderer, title, width - (6 * 2), THEME.fontFor(PackUiTone.LABEL), THEME.color(PackUiTone.BODY));
        java.util.ArrayList<Row> trimmedRows = new java.util.ArrayList<>(rows.size());
        for (Row row : rows) {
            trimmedRows.add(new Row(
                PackUiText.trimToWidth(textRenderer, row.text(), width - (6 * 2), row.font(), row.color()),
                row.font(),
                row.color()
            ));
        }
        return renderPreTrimmed(context, textRenderer, x, y, width, titleTrimmed, trimmedRows, accentColor);
    }

    public static int renderPreTrimmed(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int width, String title, List<Row> rows, int accentColor) {
        int headerHeight = Math.max(THEME.headerHeight(), THEME.lineHeight(PackUiTone.LABEL, 2));
        int rowHeight = THEME.lineHeight(PackUiTone.BODY, 2);
        int bodyPadding = 6;
        int contentHeight = Math.max(rowHeight, rows.size() * rowHeight);
        int height = headerHeight + bodyPadding + contentHeight + 6;

        context.fill(x, y, x + width, y + height, THEME.windowFill());
        context.fill(x + 1, y + 1, x + width - 1, y + headerHeight, THEME.headerFill());
        context.fill(x, y, x + width, y + 1, accentColor);
        context.fill(x, y + height - 1, x + width, y + height, THEME.borderColor());
        context.fill(x, y, x + 1, y + height, THEME.borderColor());
        context.fill(x + width - 1, y, x + width, y + height, THEME.borderColor());
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x24FFFFFF);

        int padX = 6;
        int titleY = PackUiSizing.alignTextY(y, headerHeight, THEME.fontHeight(PackUiTone.LABEL), THEME.bodyTextNudge());
        PackUiText.draw(context, textRenderer, title, THEME.fontFor(PackUiTone.LABEL), THEME.color(PackUiTone.BODY), x + padX, titleY, false);

        int rowY = y + headerHeight + 4;
        for (Row row : rows) {
            PackUiText.draw(context, textRenderer, row.text(), row.font(), row.color(), x + padX, PackUiSizing.alignTextY(rowY, rowHeight, THEME.fontHeight(PackUiTone.BODY), THEME.bodyTextNudge()), false);
            rowY += rowHeight;
        }

        return height;
    }
}
