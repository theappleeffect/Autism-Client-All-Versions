package autismclient.gui.packui;

public class PackUiRow extends PackUiContainer {
    private PackUiInsets padding = PackUiInsets.NONE;
    private int gap = 0;

    public PackUiRow setPadding(PackUiInsets padding) {
        this.padding = padding == null ? PackUiInsets.NONE : padding;
        return this;
    }

    public PackUiRow setGap(int gap) {
        this.gap = Math.max(0, gap);
        return this;
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        float tallest = 0;
        float innerWidth = Math.max(0, availableWidth - scaledPadding.horizontal());
        for (PackUiNode child : children) {
            if (child == null || !child.isVisible()) continue;
            tallest = Math.max(tallest, child.preferredHeight(context, innerWidth));
        }
        return scaledPadding.top() + tallest + scaledPadding.bottom();
    }

    @Override
    protected void layoutChildren(PackUiRenderContext context) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        int scaledGap = context.theme().scale(gap);
        float innerX = x + scaledPadding.left();
        float innerY = y + scaledPadding.top();
        float innerWidth = Math.max(0, width - scaledPadding.horizontal());
        float innerHeight = Math.max(0, height - scaledPadding.vertical());

        float fixedWidth = 0;
        int growCount = 0;
        int visibleCount = 0;
        for (PackUiNode child : children) {
            if (child == null || !child.isVisible()) continue;
            visibleCount++;
            if (child.growX()) growCount++;
            else fixedWidth += child.preferredWidth(context);
        }

        float totalGap = Math.max(0, visibleCount - 1) * scaledGap;
        float freeWidth = Math.max(0, innerWidth - fixedWidth - totalGap);
        float growWidth = growCount > 0 ? freeWidth / growCount : 0;

        float cursorX = innerX;
        for (PackUiNode child : children) {
            if (child == null || !child.isVisible()) continue;
            float childWidth = child.growX() ? growWidth : child.preferredWidth(context);
            float childHeight = Math.max(innerHeight, child.preferredHeight(context, childWidth));
            child.setBounds(cursorX, innerY, childWidth, childHeight);
            cursorX += childWidth + scaledGap;
        }
    }
}
