package autismclient.gui.packui;

public class PackUiMetricLabel extends PackUiNode {
    private String key;
    private String value;
    private int keyColor = 0xFFB79E9E;
    private int valueColor = 0xFFF3ECE7;

    public PackUiMetricLabel(String key, String value) {
        this.key = key == null ? "" : key;
        this.value = value == null ? "" : value;
        this.height = 9;
    }

    public PackUiMetricLabel setKey(String key) {
        this.key = key == null ? "" : key;
        return this;
    }

    public PackUiMetricLabel setValue(String value) {
        this.value = value == null ? "" : value;
        return this;
    }

    public PackUiMetricLabel setKeyColor(int keyColor) {
        this.keyColor = keyColor;
        return this;
    }

    public PackUiMetricLabel setValueColor(int valueColor) {
        this.valueColor = valueColor;
        return this;
    }

    @Override
    public PackUiMetricLabel setGrowX(boolean growX) {
        super.setGrowX(growX);
        return this;
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        String combined = key + value;
        return PackUiText.width(context.textRenderer(), combined, context.theme().fontFor(PackUiTone.BODY), valueColor);
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        return Math.max(context.theme().fontHeight(PackUiTone.BODY) + context.theme().scale(2), context.theme().lineHeight(PackUiTone.BODY, 2));
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        String keyComponent = key == null ? "" : key;
        String valueComponent = value == null ? "" : value;
        int drawX = Math.round(x);
        int drawY = Math.round(y);
        PackUiText.draw(
            context.drawContext(),
            context.textRenderer(),
            keyComponent,
            context.theme().fontFor(PackUiTone.BODY),
            context.applyAlpha(keyColor),
            drawX,
            PackUiSizing.alignTextY(drawY, Math.round(height), context.theme().fontHeight(PackUiTone.BODY), context.theme().bodyTextNudge()),
            false
        );
        int keyWidth = PackUiText.width(context.textRenderer(), keyComponent, context.theme().fontFor(PackUiTone.BODY), keyColor);
        String trimmedValue = PackUiText.trimToWidth(
            context.textRenderer(),
            valueComponent,
            Math.max(1, Math.round(width) - keyWidth),
            context.theme().fontFor(PackUiTone.BODY),
            valueColor
        );
        PackUiText.draw(
            context.drawContext(),
            context.textRenderer(),
            trimmedValue,
            context.theme().fontFor(PackUiTone.BODY),
            context.applyAlpha(valueColor),
            drawX + keyWidth,
            PackUiSizing.alignTextY(drawY, Math.round(height), context.theme().fontHeight(PackUiTone.BODY), context.theme().bodyTextNudge()),
            false
        );
    }
}
