package autismclient.gui.packui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class PackUiBannerRenderer {
    private PackUiBannerRenderer() {}

    public static void drawPopupFrame(GuiGraphics context, PackUiTheme theme, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, theme.windowFill());
        context.fill(x + 1, y + 1, x + width - 1, y + theme.headerHeight() - 1, theme.headerFill());
        context.fill(x, y + theme.headerHeight() - 1, x + width, y + theme.headerHeight(), theme.headerAccent());
        context.fill(x, y, x + width, y + 1, theme.borderColor());
        context.fill(x, y + height - 1, x + width, y + height, theme.borderColor());
        context.fill(x, y, x + 1, y + height, theme.borderColor());
        context.fill(x + width - 1, y, x + width, y + height, theme.borderColor());
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x24FFFFFF);
        context.fill(x + 1, y + 1, x + 2, y + height - 1, 0x24FFFFFF);
    }

    public static void drawInlineBanner(GuiGraphics context, Font textRenderer, PackUiTheme theme,
                                        int x, int y, int width, String message, int accentColor) {
        int padX = 5;
        int textHeight = theme.lineHeight(PackUiTone.BODY, 2);
        int height = textHeight + 6;
        int fill = theme.inlineBannerFill();
        context.fill(x, y, x + width, y + height, fill);
        context.fill(x, y, x + width, y + 1, accentColor);
        context.fill(x, y + height - 1, x + width, y + height, accentColor);
        context.fill(x, y, x + 1, y + height, accentColor);
        context.fill(x + width - 1, y, x + width, y + height, accentColor);
        String trimmed = PackUiText.trimToWidth(textRenderer, message, width - (padX * 2), theme.fontFor(PackUiTone.BODY), theme.color(PackUiTone.BODY));
        int textY = PackUiSizing.alignTextY(y, height, theme.fontHeight(PackUiTone.BODY), theme.bodyTextNudge());
        PackUiText.draw(context, textRenderer, trimmed, theme.fontFor(PackUiTone.BODY), theme.color(PackUiTone.BODY), x + padX, textY, false);
    }

    public static void drawFloatingBanner(GuiGraphics context, Font textRenderer, PackUiTheme theme,
                                          int x, int y, int width, String title, String message, String detail, int accentColor) {
        int padX = 6;
        int headerH = Math.max(theme.headerHeight(), theme.lineHeight(PackUiTone.LABEL, 2));
        int bodyGap = 4;
        int bottomPad = 6;
        int messageLineHeight = theme.lineHeight(PackUiTone.BODY, 2);
        int detailLineHeight = theme.lineHeight(PackUiTone.MUTED, 2);
        boolean hasDetail = detail != null && !detail.isEmpty();
        int height = headerH + bodyGap + messageLineHeight + (hasDetail ? bodyGap + detailLineHeight : 0) + bottomPad;
        context.fill(x, y, x + width, y + height, theme.windowFill());
        context.fill(x + 1, y + 1, x + width - 1, y + headerH - 1, theme.headerFill());
        context.fill(x, y, x + width, y + 1, accentColor);
        context.fill(x, y + height - 1, x + width, y + height, theme.borderColor());
        context.fill(x, y, x + 1, y + height, theme.borderColor());
        context.fill(x + width - 1, y, x + width, y + height, theme.borderColor());

        String titleTrimmed = PackUiText.trimToWidth(textRenderer, title, width - (padX * 2), theme.fontFor(PackUiTone.LABEL), theme.color(PackUiTone.BODY));
        String messageTrimmed = PackUiText.trimToWidth(textRenderer, message, width - (padX * 2), theme.fontFor(PackUiTone.BODY), theme.color(PackUiTone.BODY));
        int titleY = PackUiSizing.alignTextY(y, headerH, theme.fontHeight(PackUiTone.LABEL), theme.bodyTextNudge());
        int messageBoxY = y + headerH + bodyGap;
        int messageY = PackUiSizing.alignTextY(messageBoxY, messageLineHeight, theme.fontHeight(PackUiTone.BODY), theme.bodyTextNudge());
        PackUiText.draw(context, textRenderer, titleTrimmed, theme.fontFor(PackUiTone.LABEL), theme.color(PackUiTone.BODY), x + padX, titleY, false);
        PackUiText.draw(context, textRenderer, messageTrimmed, theme.fontFor(PackUiTone.BODY), theme.color(PackUiTone.BODY), x + padX, messageY, false);
        if (hasDetail) {
            String detailTrimmed = PackUiText.trimToWidth(textRenderer, detail, width - (padX * 2), theme.fontFor(PackUiTone.MUTED), theme.color(PackUiTone.MUTED));
            int detailBoxY = messageBoxY + messageLineHeight + bodyGap;
            int detailY = PackUiSizing.alignTextY(detailBoxY, detailLineHeight, theme.fontHeight(PackUiTone.MUTED), theme.bodyTextNudge());
            PackUiText.draw(context, textRenderer, detailTrimmed, theme.fontFor(PackUiTone.MUTED), theme.color(PackUiTone.MUTED), x + padX, detailY, false);
        }
    }
}
