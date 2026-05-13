package autismclient.gui.packui;

import net.minecraft.resources.Identifier;

public class PackUiIconLabel extends PackUiNode {
    private String text;
    private Identifier icon;
    private PackUiTone tone = PackUiTone.LABEL;
    private int iconSize = 12;
    private int iconGap = 5;

    public PackUiIconLabel(String text, Identifier icon) {
        this.text = text == null ? "" : text;
        this.icon = icon;
        this.height = 14;
    }

    public PackUiIconLabel setText(String text) {
        this.text = text == null ? "" : text;
        return this;
    }

    public PackUiIconLabel setIcon(Identifier icon) {
        this.icon = icon;
        return this;
    }

    public PackUiIconLabel setTone(PackUiTone tone) {
        this.tone = tone == null ? PackUiTone.LABEL : tone;
        return this;
    }

    public PackUiIconLabel setIconSize(int iconSize) {
        this.iconSize = Math.max(1, iconSize);
        return this;
    }

    public PackUiIconLabel setIconGap(int iconGap) {
        this.iconGap = Math.max(0, iconGap);
        return this;
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        int textWidth = PackUiText.width(context.textRenderer(), text, context.theme().fontFor(tone), context.theme().color(tone));
        int scaledIconSize = context.theme().scale(iconSize);
        int scaledIconGap = context.theme().scale(iconGap);
        return textWidth + (icon != null ? scaledIconSize + scaledIconGap : 0);
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        return Math.max(context.theme().fontHeight(tone) + context.theme().scale(2), context.theme().scale(iconSize + 1));
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        int drawX = Math.round(x);
        int drawY = Math.round(y);
        int drawH = Math.round(height);
        int textColor = context.theme().color(tone);
        int contentX = drawX;
        int scaledIconSize = context.theme().scale(iconSize);
        int scaledIconGap = context.theme().scale(iconGap);

        if (icon != null) {
            int iconY = drawY + Math.max(1, (drawH - scaledIconSize) / 2);
            context.drawTexturedQuad(icon, drawX, iconY, drawX + scaledIconSize, iconY + scaledIconSize);
            contentX += scaledIconSize + scaledIconGap;
        }

        int textY = PackUiSizing.alignTextY(drawY, drawH, context.theme().fontHeight(tone), context.theme().bodyTextNudge());
        PackUiText.draw(context.drawContext(), context.textRenderer(), text, context.theme().fontFor(tone), context.applyAlpha(textColor), contentX, textY, false);
    }
}
