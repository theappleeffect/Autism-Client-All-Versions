package autismclient.gui.packui;

public class PackUiPanelNode extends PackUiContainer {
    private final PackUiColumn content = new PackUiColumn();
    private boolean active = true;
    private PackUiInsets padding = PackUiInsets.all(6);
    private boolean drawBorder = true;
    private boolean drawFill = true;

    public PackUiPanelNode() {
        add(content);
    }

    public PackUiColumn content() {
        return content;
    }

    public PackUiPanelNode setActive(boolean active) {
        this.active = active;
        return this;
    }

    public PackUiPanelNode setPadding(PackUiInsets padding) {
        this.padding = padding == null ? PackUiInsets.NONE : padding;
        return this;
    }

    public PackUiPanelNode setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        return this;
    }

    public PackUiPanelNode setDrawFill(boolean drawFill) {
        this.drawFill = drawFill;
        return this;
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        return scaledPadding.horizontal() + content.preferredWidth(context);
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        return scaledPadding.vertical() + content.preferredHeight(context, Math.max(0.0f, availableWidth - scaledPadding.horizontal()));
    }

    @Override
    protected void layoutChildren(PackUiRenderContext context) {
        PackUiInsets scaledPadding = context.theme().scale(padding);
        content.setBounds(
            x + scaledPadding.left(),
            y + scaledPadding.top(),
            Math.max(0.0f, width - scaledPadding.horizontal()),
            Math.max(0.0f, height - scaledPadding.vertical())
        );
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        PackUiRenderContext drawContext = context.withAlpha(active ? 1.0f : 0.56f);
        int drawX = Math.round(x);
        int drawY = Math.round(y);
        int drawW = Math.round(width);
        int drawH = Math.round(height);
        int fill = active ? drawContext.theme().windowFill() : drawContext.theme().windowFillInactive();
        int border = active ? drawContext.theme().borderColor() : drawContext.theme().borderSoft();

        if (drawFill) {
            drawContext.drawContext().fill(drawX, drawY, drawX + drawW, drawY + drawH, drawContext.applyAlpha(fill));
        }
        if (drawBorder) {
            drawContext.drawContext().fill(drawX, drawY, drawX + drawW, drawY + 1, drawContext.applyAlpha(border));
            drawContext.drawContext().fill(drawX, drawY + drawH - 1, drawX + drawW, drawY + drawH, drawContext.applyAlpha(border));
            drawContext.drawContext().fill(drawX, drawY, drawX + 1, drawY + drawH, drawContext.applyAlpha(border));
            drawContext.drawContext().fill(drawX + drawW - 1, drawY, drawX + drawW, drawY + drawH, drawContext.applyAlpha(border));
            drawContext.drawContext().fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + 2, drawContext.applyAlpha(0x24FFFFFF));
            drawContext.drawContext().fill(drawX + 1, drawY + 1, drawX + 2, drawY + drawH - 1, drawContext.applyAlpha(0x24FFFFFF));
            drawContext.drawContext().fill(drawX + 1, drawY + drawH - 2, drawX + drawW - 1, drawY + drawH - 1, drawContext.applyAlpha(0x18000000));
            drawContext.drawContext().fill(drawX + drawW - 2, drawY + 1, drawX + drawW - 1, drawY + drawH - 1, drawContext.applyAlpha(0x18000000));
        }

        super.render(drawContext);
    }
}
