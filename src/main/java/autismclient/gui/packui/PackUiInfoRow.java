package autismclient.gui.packui;

public class PackUiInfoRow extends PackUiNode {
    private String label;
    private String value;
    private float labelWidth = 76.0f;
    private PackUiTone labelTone = PackUiTone.MUTED;
    private PackUiTone valueTone = PackUiTone.BODY;
    private Integer valueColorOverride;
    private int textYOffset = 0;
    private Runnable onPress;

    public PackUiInfoRow(String label, String value) {
        this.label = label == null ? "" : label;
        this.value = value == null ? "--" : value;
        this.height = 12;
    }

    public PackUiInfoRow setLabel(String label) {
        this.label = label == null ? "" : label;
        return this;
    }

    public PackUiInfoRow setValue(String value) {
        this.value = value == null ? "--" : value;
        return this;
    }

    public PackUiInfoRow setLabelWidth(float labelWidth) {
        this.labelWidth = Math.max(24.0f, labelWidth);
        return this;
    }

    public PackUiInfoRow setLabelTone(PackUiTone labelTone) {
        this.labelTone = labelTone == null ? PackUiTone.MUTED : labelTone;
        return this;
    }

    public PackUiInfoRow setValueTone(PackUiTone valueTone) {
        this.valueTone = valueTone == null ? PackUiTone.BODY : valueTone;
        return this;
    }

    public PackUiInfoRow setValueColorOverride(Integer valueColorOverride) {
        this.valueColorOverride = valueColorOverride;
        return this;
    }

    public PackUiInfoRow setTextYOffset(int textYOffset) {
        this.textYOffset = textYOffset;
        return this;
    }

    public PackUiInfoRow setOnPress(Runnable onPress) {
        this.onPress = onPress;
        return this;
    }

    private boolean isClickable() {
        return onPress != null && enabled;
    }

    private PackUiButtonFeedback feedbackState() {
        return PackUiButtonFeedback.forKey(
            "info-row:"
                + Math.round(x) + ':'
                + Math.round(y) + ':'
                + Math.round(width) + ':'
                + Math.round(height) + ':'
                + label + ':'
                + value
        );
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        int baselineHeight = 12;
        int labelHeight = context.theme().fontHeight(labelTone) + context.theme().scale(3);
        int valueHeight = context.theme().fontHeight(valueTone) + context.theme().scale(3);
        return Math.max(baselineHeight, Math.max(labelHeight, valueHeight));
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        int drawX = Math.round(x);
        int drawY = Math.round(y);
        int drawW = Math.round(width);
        int drawH = Math.round(height);
        int minSegment = context.theme().scale(24);
        int labelW = Math.round(Math.min(labelWidth, Math.max(minSegment, drawW - minSegment)));
        int valueX = drawX + labelW;
        int valueW = Math.max(1, drawW - labelW);
        boolean clickable = isClickable();
        boolean hovered = clickable && contains(context.mouseX(), context.mouseY());
        PackUiButtonFeedback feedback = clickable ? feedbackState() : null;
        float hover = clickable
            ? feedback.update(
                hovered,
                PackUiButtonFeedback.isPrimaryPointerDown(),
                context.mouseX() - x,
                context.mouseY() - y,
                width,
                height
            )
            : 0.0f;
        float clickGlow = clickable ? feedback.clickGlowProgress() : 0.0f;
        if (clickable) {
            if (hover > 0.0f || clickGlow > 0.0f) {
                int hoverTint = ((int) Math.min(22.0f, (hover * 12.0f) + (clickGlow * 8.0f)) << 24) | 0x00FF3B3B;
                context.drawContext().fill(drawX, drawY, drawX + drawW, drawY + drawH, context.applyAlpha(hoverTint));
            }
            feedback.render(
                context.drawContext(),
                drawX,
                drawY,
                drawW,
                drawH,
                0xFFFF6E6E,
                0xFFFFCDCD,
                context.alpha()
            );
        }
        int labelColor = clickable && hovered
            ? PackUiSizing.lerpColor(context.theme().color(labelTone), 0xFFF3ECE7, 0.35f)
            : context.theme().color(labelTone);
        int valueColor = valueColorOverride != null ? valueColorOverride : context.theme().color(valueTone);
        if (clickable && hovered) {
            valueColor = PackUiSizing.lerpColor(valueColor, 0xFFFFF6F6, 0.28f);
        }
        int labelTextY = PackUiSizing.alignTextY(drawY, drawH, context.theme().fontHeight(labelTone), context.theme().bodyTextNudge() + textYOffset);
        int valueTextY = PackUiSizing.alignTextY(drawY, drawH, context.theme().fontHeight(valueTone), context.theme().bodyTextNudge() + textYOffset);

        String displayLabel = PackUiText.trimToWidth(
            context.textRenderer(),
            label,
            Math.max(1, labelW - context.theme().scale(4)),
            context.theme().fontFor(labelTone),
            labelColor
        );
        String displayValue = PackUiText.trimToWidth(
            context.textRenderer(),
            value,
            Math.max(1, valueW - context.theme().scale(2)),
            context.theme().fontFor(valueTone),
            valueColor
        );

        PackUiText.draw(context.drawContext(), context.textRenderer(), displayLabel, context.theme().fontFor(labelTone), context.applyAlpha(labelColor), drawX, labelTextY, false);
        PackUiText.draw(context.drawContext(), context.textRenderer(), displayValue, context.theme().fontFor(valueTone), context.applyAlpha(valueColor), valueX, valueTextY, false);
    }

    @Override
    public boolean mouseClicked(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        if (button != 0 || !isClickable() || !contains(mouseX, mouseY)) return false;
        feedbackState().triggerPress(mouseX - x, mouseY - y, width, height);
        onPress.run();
        return true;
    }
}
