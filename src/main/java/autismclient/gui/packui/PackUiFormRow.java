package autismclient.gui.packui;

public class PackUiFormRow extends PackUiContainer {
    private final PackUiNode labelNode;
    private final PackUiNode controlNode;
    private PackUiInsets padding = PackUiInsets.NONE;
    private int gap = 0;
    private float labelWidth = -1.0f;
    private boolean alignControlEnd = false;

    public PackUiFormRow(PackUiNode labelNode, PackUiNode controlNode) {
        this.labelNode = add(labelNode);
        this.controlNode = add(controlNode);
    }

    public PackUiFormRow setPadding(PackUiInsets padding) {
        this.padding = padding == null ? PackUiInsets.NONE : padding;
        return this;
    }

    public PackUiFormRow setGap(int gap) {
        this.gap = Math.max(0, gap);
        return this;
    }

    public PackUiFormRow setLabelWidth(float labelWidth) {
        this.labelWidth = Math.max(0.0f, labelWidth);
        return this;
    }

    public PackUiFormRow setAlignControlEnd(boolean alignControlEnd) {
        this.alignControlEnd = alignControlEnd;
        return this;
    }

    public PackUiNode labelNode() {
        return labelNode;
    }

    public PackUiNode controlNode() {
        return controlNode;
    }

    private float resolvedLabelWidth(PackUiRenderContext context) {
        if (labelWidth > 0.0f) return labelWidth;
        return labelNode == null ? 0.0f : labelNode.preferredWidth(context);
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        int scaledGap = context.theme().scale(gap);
        float total = scaledPadding.horizontal();
        total += resolvedLabelWidth(context);
        if (labelNode != null && labelNode.isVisible() && controlNode != null && controlNode.isVisible()) total += scaledGap;
        if (controlNode != null && controlNode.isVisible()) total += controlNode.preferredWidth(context);
        return total;
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        float labelHeight = labelNode != null && labelNode.isVisible() ? labelNode.preferredHeight(context, availableWidth) : 0.0f;
        float controlHeight = controlNode != null && controlNode.isVisible() ? controlNode.preferredHeight(context, availableWidth) : 0.0f;
        return scaledPadding.top() + Math.max(labelHeight, controlHeight) + scaledPadding.bottom();
    }

    @Override
    protected void layoutChildren(PackUiRenderContext context) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        int scaledGap = context.theme().scale(gap);
        float innerX = x + scaledPadding.left();
        float innerY = y + scaledPadding.top();
        float innerWidth = Math.max(0.0f, width - scaledPadding.horizontal());
        float innerHeight = Math.max(0.0f, height - scaledPadding.vertical());

        float resolvedLabelWidth = Math.min(resolvedLabelWidth(context), innerWidth);
        float controlX = innerX;

        if (labelNode != null && labelNode.isVisible()) {
            float labelHeight = Math.min(innerHeight, labelNode.preferredHeight(context, resolvedLabelWidth));
            float labelY = innerY + Math.max(0.0f, (innerHeight - labelHeight) / 2.0f);
            labelNode.setBounds(innerX, labelY, resolvedLabelWidth, labelHeight);
            controlX = innerX + resolvedLabelWidth + (controlNode != null && controlNode.isVisible() ? scaledGap : 0);
        }

        if (controlNode != null && controlNode.isVisible()) {
            float controlWidth = Math.max(0.0f, innerX + innerWidth - controlX);
            if (!controlNode.growX()) {
                controlWidth = Math.min(controlWidth, controlNode.preferredWidth(context));
            }
            if (alignControlEnd && !controlNode.growX()) {
                controlX = Math.max(controlX, innerX + innerWidth - controlWidth);
            }
            float controlHeight = Math.min(innerHeight, controlNode.preferredHeight(context, controlWidth));
            float controlY = innerY + Math.max(0.0f, (innerHeight - controlHeight) / 2.0f);
            controlNode.setBounds(controlX, controlY, controlWidth, controlHeight);
        }
    }
}
