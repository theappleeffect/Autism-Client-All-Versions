package autismclient.gui.packui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class PackUiListRenderer {
    private static final PackUiTheme THEME = new PackUiTheme();
    private static final int HEADER_ACTION_RIGHT_INSET = 2;

    public enum RowTone {
        NORMAL,
        READY,
        WARNING,
        DANGER
    }

    private PackUiListRenderer() {
    }

    public static void drawFrame(GuiGraphics context, int x, int y, int width, int height, boolean focused) {
        int bg = focused ? THEME.listFillFocused() : THEME.listFill();
        int border = focused ? THEME.headerAccent() : THEME.borderColor();
        context.fill(x, y, x + width, y + height, bg);
        context.fill(x, y, x + width, y + 1, border);
        context.fill(x, y + height - 1, x + width, y + height, border);
        context.fill(x, y, x + 1, y + height, border);
        context.fill(x + width - 1, y, x + width, y + height, border);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x16FFFFFF);
    }

    public static void drawHeader(GuiGraphics context, Font textRenderer, String title, int x, int y) {
        PackUiText.draw(context, textRenderer, title, THEME.fontFor(PackUiTone.LABEL), THEME.color(PackUiTone.MUTED), x, y, false);
    }

    public static void drawHeaderAction(
        GuiGraphics context,
        Font textRenderer,
        String label,
        boolean enabled,
        int listX,
        int listWidth,
        int headerY,
        int mouseX,
        int mouseY
    ) {
        int buttonHeight = 13;
        int buttonWidth = Math.max(54, PackUiText.width(textRenderer, label, THEME.fontFor(PackUiTone.BODY), THEME.color(PackUiTone.BODY)) + 12);
        int buttonX = listX + listWidth - buttonWidth - HEADER_ACTION_RIGHT_INSET;
        int buttonY = headerY + Math.max(0, (THEME.lineHeight(PackUiTone.LABEL, 1) - buttonHeight) / 2) - 1;
        PackUiOverlayButton button = PackUiOverlayButton.create(buttonX, buttonY, buttonWidth, buttonHeight, Component.literal(label), b -> {});
        button.active = enabled;
        button.setVariant(enabled ? PackUiOverlayButton.Variant.SECONDARY : PackUiOverlayButton.Variant.GHOST);
        PackUiOverlayButton.renderStyled(context, textRenderer, button, mouseX, mouseY);
    }

    public static void drawEmptyState(GuiGraphics context, Font textRenderer, String text, int x, int y, int width) {
        String trimmed = PackUiText.trimToWidth(textRenderer, text, Math.max(1, width - 10), THEME.fontFor(PackUiTone.MUTED), THEME.color(PackUiTone.MUTED));
        int textY = PackUiSizing.alignTextY(y, 14, THEME.fontHeight(PackUiTone.MUTED), THEME.bodyTextNudge());
        PackUiText.draw(context, textRenderer, trimmed, THEME.fontFor(PackUiTone.MUTED), THEME.color(PackUiTone.MUTED), x + 6, textY, false);
    }

    public static void drawRow(
        GuiGraphics context,
        Font textRenderer,
        String label,
        int x,
        int y,
        int width,
        int height,
        boolean hovered,
        boolean selected,
        RowTone tone
    ) {
        drawRow(context, textRenderer, Component.literal(label == null ? "" : label), x, y, width, height, hovered, selected, tone, false);
    }

    public static void drawRow(
        GuiGraphics context,
        Font textRenderer,
        Component label,
        int x,
        int y,
        int width,
        int height,
        boolean hovered,
        boolean selected,
        RowTone tone,
        boolean useMinecraftText
    ) {
        int bg = selected ? THEME.rowFillSelected() : hovered ? THEME.rowFillHovered() : THEME.rowFillNormal();
        int fg = switch (tone) {
            case READY -> selected ? 0xFFE8FFF0 : 0xFF9DF0BA;
            case WARNING -> selected ? 0xFFFFF3E0 : 0xFFFFC67A;
            case DANGER -> selected ? 0xFFFFEAEA : 0xFFFF8C8C;
            case NORMAL -> selected ? 0xFFFFF4F4 : THEME.color(PackUiTone.BODY);
        };
        int rightInset = 6;
        int textX = x + 6;

        context.fill(x, y, x + width, y + height, bg);

        int textWidth = Math.max(1, (x + width - rightInset) - textX);
        int textY = PackUiSizing.alignTextY(y, height, THEME.fontHeight(PackUiTone.BODY), THEME.bodyTextNudge());
        if (useMinecraftText) {
            List<FormattedCharSequence> wrapped = textRenderer.split(label == null ? Component.empty() : label, textWidth);
            FormattedCharSequence line = wrapped.isEmpty() ? Component.empty().getVisualOrderText() : wrapped.get(0);
            context.drawString(textRenderer, line, textX, textY, fg, false);
        } else {
            String trimmed = PackUiText.trimToWidth(textRenderer, label == null ? "" : label.getString(), textWidth, THEME.fontFor(PackUiTone.BODY), fg);
            PackUiText.draw(context, textRenderer, trimmed, THEME.fontFor(PackUiTone.BODY), fg, textX, textY, false);
        }
    }

    public static void drawDivider(GuiGraphics context, int x, int y, int width) {
        context.fill(x + 5, y, x + width - 5, y + 1, 0x2AFFFFFF);
    }

    public static void drawIconButton(
        GuiGraphics context,
        int x,
        int y,
        int size,
        Identifier icon,
        boolean hovered,
        boolean danger
    ) {
        PackUiButtonFeedback feedback = PackUiButtonFeedback.forKey("list-icon:" + x + ':' + y + ':' + size + ':' + danger + ':' + icon);
        float hover = feedback.update(
            hovered,
            PackUiButtonFeedback.isPrimaryPointerDown(),
            0.5f * size,
            0.5f * size,
            size,
            size
        );
        float clickGlow = feedback.clickGlowProgress();

        int bg = danger ? THEME.dangerFill() : THEME.listFill();
        int border = danger ? THEME.dangerBorder() : THEME.borderColor();
        int borderGlow = danger ? 0xFFFF9090 : 0xFFFF6464;
        int rippleRing = danger ? 0xFFFF8A8A : 0xFFFF7070;
        int rippleFill = danger ? 0xFFFFD1D1 : 0xFFFFCACA;
        float hoverVisual = Math.max(hover * 0.9f, clickGlow * 0.55f);
        border = PackUiSizing.lerpColor(border, borderGlow, Math.min(1.0f, (hover * 0.35f) + (clickGlow * 0.85f)));

        context.fill(x, y, x + size, y + size, bg);
        if (hoverVisual > 0.0f) {
            int hoverTint = danger
                ? (((int) Math.min(38.0f, (hover * 24.0f) + (clickGlow * 12.0f))) << 24) | 0x00FF3B3B
                : (((int) Math.min(30.0f, (hover * 18.0f) + (clickGlow * 10.0f))) << 24) | 0x00FF3B3B;
            context.fill(x + 1, y + 1, x + size - 1, y + size - 1, hoverTint);
        }
        if (clickGlow > 0.0f) {
            int pressGlow = ((int) (clickGlow * 14.0f) << 24) | (rippleFill & 0x00FFFFFF);
            context.fill(x + 1, y + 1, x + size - 1, y + size - 1, pressGlow);
        }
        feedback.render(context, x + 1, y + 1, Math.max(0, size - 2), Math.max(0, size - 2), rippleRing, rippleFill, 1.0f);
        context.fill(x, y, x + size, y + 1, border);
        context.fill(x, y + size - 1, x + size, y + size, border);
        context.fill(x, y, x + 1, y + size, border);
        context.fill(x + size - 1, y, x + size, y + size, border);
        int iconInset = Math.max(1, (size - 10) / 2);
        int iconSize = Math.max(1, size - (iconInset * 2));
        if (PackUiControlGlyphs.isCloseIcon(icon) || PackUiControlGlyphs.isChevronIcon(icon)) {
            int iconColor = danger ? 0xFFFFBBBB : 0xFFF2E8E8;
            int iconShadow = danger ? 0xD6491A1F : 0xB83A1418;
            PackUiControlGlyphs.drawKnownIcon(context, icon, x + iconInset, y + iconInset, iconSize, iconColor, iconShadow, 1.0f);
        } else {
            autismclient.util.PackUtilRender.iconBlit(context, icon, x + iconInset, y + iconInset, x + size - iconInset, y + size - iconInset);
        }
    }

    public static int rowTextWidth(int totalWidth, boolean selected, boolean hasTrailingButton) {
        int reserved = 12 + (hasTrailingButton ? 18 : 6);
        return Math.max(1, totalWidth - reserved);
    }
}
