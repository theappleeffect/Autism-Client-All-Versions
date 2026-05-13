package autismclient.gui.packui;

public class PackUiCompactRow extends PackUiContainer {
    private PackUiInsets padding = PackUiInsets.NONE;
    private int gap = 0;
    private boolean underlineOnHover = false;
    private int underlineColor = 0xFFFF3B3B;
    private float occupiedWidth = 0.0f;

    public PackUiCompactRow setPadding(PackUiInsets padding) {
        this.padding = padding == null ? PackUiInsets.NONE : padding;
        return this;
    }

    public PackUiCompactRow setGap(int gap) {
        this.gap = Math.max(0, gap);
        return this;
    }

    public PackUiCompactRow setUnderlineOnHover(boolean underlineOnHover) {
        this.underlineOnHover = underlineOnHover;
        return this;
    }

    public PackUiCompactRow setUnderlineColor(int underlineColor) {
        this.underlineColor = underlineColor;
        return this;
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        int scaledGap = context.theme().scale(gap);
        float total = scaledPadding.horizontal();
        int visibleCount = 0;
        for (PackUiNode child : children) {
            if (child == null || !child.isVisible()) continue;
            total += child.preferredWidth(context);
            visibleCount++;
        }
        if (visibleCount > 1) total += (visibleCount - 1) * scaledGap;
        return total;
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        float tallest = 0.0f;
        for (PackUiNode child : children) {
            if (child == null || !child.isVisible()) continue;
            tallest = Math.max(tallest, child.preferredHeight(context, availableWidth));
        }
        return scaledPadding.top() + tallest + scaledPadding.bottom();
    }

    @Override
    protected void layoutChildren(PackUiRenderContext context) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        int scaledGap = context.theme().scale(gap);
        float innerX = x + scaledPadding.left();
        float innerY = y + scaledPadding.top();
        float innerWidth = Math.max(0.0f, width - scaledPadding.horizontal());
        float innerHeight = Math.max(0.0f, height - scaledPadding.vertical());

        float fixedWidth = 0.0f;
        int growCount = 0;
        int visibleCount = 0;
        for (PackUiNode child : children) {
            if (child == null || !child.isVisible()) continue;
            visibleCount++;
            if (child.growX()) growCount++;
            else fixedWidth += child.preferredWidth(context);
        }

        float totalGap = Math.max(0, visibleCount - 1) * scaledGap;
        float freeWidth = Math.max(0.0f, innerWidth - fixedWidth - totalGap);
        float growWidth = growCount > 0 ? freeWidth / growCount : 0.0f;

        float cursorX = innerX;
        occupiedWidth = 0.0f;
        for (PackUiNode child : children) {
            if (child == null || !child.isVisible()) continue;
            float childWidth = child.growX() ? growWidth : child.preferredWidth(context);
            float childHeight = Math.min(innerHeight, child.preferredHeight(context, childWidth));
            float childY = innerY + Math.max(0.0f, (innerHeight - childHeight) / 2.0f);
            child.setBounds(cursorX, childY, childWidth, childHeight);
            cursorX += childWidth + scaledGap;
        }
        occupiedWidth = Math.max(0.0f, cursorX - innerX - (visibleCount > 0 ? scaledGap : 0));
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        super.render(context);
        if (underlineOnHover) {
            float hover = updateHover(contains(context.mouseX(), context.mouseY()), context.delta());
            if (hover <= 0.0f || context.drawContext() == null) return;
            PackUiInsets scaledPadding = context.theme().scale(padding);
            int drawX = Math.round(x + scaledPadding.left());
            int drawY = Math.round(y + height - context.theme().scale(1));
            float underlineAreaWidth = Math.max(occupiedWidth, width - scaledPadding.horizontal());
            int underlineWidth = Math.max(1, Math.round(underlineAreaWidth * hover));
            int underlineX = drawX + Math.max(0, Math.round((underlineAreaWidth - underlineWidth) / 2.0f));
            context.drawContext().fill(underlineX, drawY, underlineX + underlineWidth, drawY + context.theme().scale(1), underlineColor);
        }
    }
}
