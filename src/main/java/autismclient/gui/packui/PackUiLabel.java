package autismclient.gui.packui;

public class PackUiLabel extends PackUiNode {
    private String text;
    private PackUiTone tone;
    private boolean shadow;
    private boolean trimToBounds;

    public PackUiLabel(String text, PackUiTone tone) {
        this.text = text == null ? "" : text;
        this.tone = tone == null ? PackUiTone.BODY : tone;
        this.height = 10;
    }

    public PackUiLabel setText(String text) {
        this.text = text == null ? "" : text;
        return this;
    }

    public PackUiLabel setTone(PackUiTone tone) {
        this.tone = tone == null ? PackUiTone.BODY : tone;
        return this;
    }

    public PackUiLabel setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public PackUiLabel setTrimToBounds(boolean trimToBounds) {
        this.trimToBounds = trimToBounds;
        return this;
    }

    @Override
    public PackUiLabel setGrowX(boolean growX) {
        super.setGrowX(growX);
        return this;
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        return PackUiText.width(context.textRenderer(), text, context.theme().fontFor(tone), context.theme().color(tone));
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        return context.theme().lineHeight(tone, 3);
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        String displayComponent = text;
        if (trimToBounds && width > 0) {
            displayComponent = PackUiText.trimToWidth(
                context.textRenderer(),
                text,
                Math.round(width),
                context.theme().fontFor(tone),
                context.theme().color(tone)
            );
        }
        PackUiText.draw(
            context.drawContext(),
            context.textRenderer(),
            displayComponent,
            context.theme().fontFor(tone),
            context.applyAlpha(context.theme().color(tone)),
            Math.round(x),
            PackUiSizing.alignTextY(Math.round(y), Math.round(height), context.theme().fontHeight(tone), context.theme().bodyTextNudge()),
            shadow
        );
    }
}
