package autismclient.gui.packui;

public class PackUiColumn extends PackUiContainer {
    private PackUiInsets padding = PackUiInsets.NONE;
    private int gap = 0;

    public PackUiColumn setPadding(PackUiInsets padding) {
        this.padding = padding == null ? PackUiInsets.NONE : padding;
        return this;
    }

    public PackUiColumn setGap(int gap) {
        this.gap = Math.max(0, gap);
        return this;
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        int scaledGap = context.theme().scale(gap);
        float innerWidth = Math.max(0, availableWidth - scaledPadding.horizontal());
        float total = scaledPadding.top() + scaledPadding.bottom();
        boolean first = true;
        for (PackUiNode child : children) {
            if (child == null || !child.isVisible()) continue;
            if (!first) total += scaledGap;
            total += child.preferredHeight(context, innerWidth);
            first = false;
        }
        return total;
    }

    @Override
    protected void layoutChildren(PackUiRenderContext context) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        int scaledGap = context.theme().scale(gap);
        float innerX = x + scaledPadding.left();
        float cursorY = y + scaledPadding.top();
        float innerWidth = Math.max(0, width - scaledPadding.horizontal());
        for (PackUiNode child : children) {
            if (child == null || !child.isVisible()) continue;
            float childHeight = child.preferredHeight(context, innerWidth);
            child.setBounds(innerX, cursorY, innerWidth, childHeight);
            cursorY += childHeight + scaledGap;
        }
    }
}
