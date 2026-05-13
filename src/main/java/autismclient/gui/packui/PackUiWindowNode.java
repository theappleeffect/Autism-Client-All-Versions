package autismclient.gui.packui;

import net.minecraft.resources.Identifier;

public class PackUiWindowNode extends PackUiContainer {
    private static final float BODY_FADE_DURATION_SECONDS = 0.0f;

    private final PackUiColumn content = new PackUiColumn();
    private String title;
    private Identifier titleIcon;
    private boolean showBody = true;
    private float bodyFadeAlpha = 1.0f;
    private boolean active = true;
    private boolean headerHovered = false;
    private boolean centerTitle = true;
    private PackUiTone titleTone = PackUiTone.TITLE;
    private int titleLeftInset = 10;
    private int titleRightInset = 10;
    private long lastAnimationNanos = System.nanoTime();

    public PackUiWindowNode(String title) {
        this.title = title == null ? "" : title;
        content.setGap(4).setPadding(PackUiInsets.all(6));
        add(content);
    }

    public PackUiColumn content() {
        return content;
    }

    public PackUiWindowNode setTitle(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    public PackUiWindowNode setTitleIcon(Identifier titleIcon) {
        this.titleIcon = titleIcon;
        return this;
    }

    public PackUiWindowNode setShowBody(boolean showBody) {
        this.showBody = showBody;
        return this;
    }

    public PackUiWindowNode restoreShowBody(boolean showBody) {
        this.showBody = showBody;
        this.bodyFadeAlpha = showBody ? 1.0f : 0.0f;
        this.lastAnimationNanos = System.nanoTime();
        return this;
    }

    public PackUiWindowNode syncShowBody(boolean showBody) {
        return restoreShowBody(showBody);
    }

    public float bodyFadeAlpha() {
        updateBodyFadeAlpha();
        return bodyFadeAlpha;
    }

    public PackUiWindowNode setActive(boolean active) {
        this.active = active;
        return this;
    }

    public PackUiWindowNode setHeaderHovered(boolean headerHovered) {
        this.headerHovered = headerHovered;
        return this;
    }

    public PackUiWindowNode setCenterTitle(boolean centerTitle) {
        this.centerTitle = centerTitle;
        return this;
    }

    public PackUiWindowNode setTitleTone(PackUiTone titleTone) {
        this.titleTone = titleTone == null ? PackUiTone.TITLE : titleTone;
        return this;
    }

    public PackUiWindowNode setTitleAreaInsets(int titleLeftInset, int titleRightInset) {
        this.titleLeftInset = Math.max(0, titleLeftInset);
        this.titleRightInset = Math.max(0, titleRightInset);
        return this;
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        updateBodyFadeAlpha();
        int headerH = Math.max(context.theme().headerHeight(), context.theme().lineHeight(titleTone, 4));
        float bodyHeight = shouldShowBodyArea() ? content.preferredHeight(context, availableWidth) : 0.0f;
        return headerH + bodyHeight;
    }

    @Override
    protected void layoutChildren(PackUiRenderContext context) {
        updateBodyFadeAlpha();
        int headerH = Math.max(context.theme().headerHeight(), context.theme().lineHeight(titleTone, 4));
        boolean showContent = shouldShowBodyArea();
        content.setVisible(showContent);
        content.setBounds(x, y + headerH, width, showContent ? Math.max(0, height - headerH) : 0);
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        updateBodyFadeAlpha();
        PackUiRenderContext drawContext = context.withAlpha(active ? 1.0f : 0.56f);
        int drawX = Math.round(x);
        int drawY = Math.round(y);
        int drawW = Math.round(width);
        int drawH = Math.round(height);
        int headerH = Math.max(context.theme().headerHeight(), context.theme().lineHeight(titleTone, 4));
        int titleColor = context.theme().color(titleTone);
        int titleFont = context.theme().fontHeight(titleTone);
        int frameFill = active ? context.theme().windowFill() : context.theme().windowFillInactive();
        int headerFill = active ? context.theme().headerFill() : context.theme().headerFillInactive();
        int border = active ? context.theme().borderColor() : context.theme().borderSoft();
        int accent = active ? context.theme().headerAccent() : 0xAA7A2020;
        int scaledTitleLeftInset = context.theme().scale(titleLeftInset);
        int scaledTitleRightInset = context.theme().scale(titleRightInset);

        drawContext.drawContext().fill(drawX, drawY, drawX + drawW, drawY + drawH, drawContext.applyAlpha(frameFill));
        drawContext.drawContext().fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + headerH - 1, drawContext.applyAlpha(headerFill));
        drawContext.drawContext().fill(drawX + 1, drawY + headerH, drawX + drawW - 1, drawY + drawH - 1, drawContext.applyAlpha(active ? 0x14000000 : 0x0F000000));
        drawContext.drawContext().fill(drawX, drawY + headerH - 1, drawX + drawW, drawY + headerH, drawContext.applyAlpha(accent));
        drawContext.drawContext().fill(drawX, drawY, drawX + drawW, drawY + 1, drawContext.applyAlpha(border));
        drawContext.drawContext().fill(drawX, drawY + drawH - 1, drawX + drawW, drawY + drawH, drawContext.applyAlpha(border));
        drawContext.drawContext().fill(drawX, drawY, drawX + 1, drawY + drawH, drawContext.applyAlpha(border));
        drawContext.drawContext().fill(drawX + drawW - 1, drawY, drawX + drawW, drawY + drawH, drawContext.applyAlpha(border));
        int titleX = drawX + scaledTitleLeftInset;
        int titleY = PackUiSizing.alignTextY(drawY, headerH, titleFont, context.theme().bodyTextNudge());
        int titleAreaX = drawX + scaledTitleLeftInset;
        int titleAreaWidth = Math.max(20, drawW - scaledTitleLeftInset - scaledTitleRightInset);
        if (titleIcon != null) {
            int iconSize = context.theme().scale(10);
            int iconY = drawY + Math.max(1, (headerH - iconSize) / 2);
            int iconX = drawX + context.theme().scale(10);
            drawContext.drawTexturedQuad(titleIcon, iconX, iconY, iconX + iconSize, iconY + iconSize);
            titleAreaX = drawX + scaledTitleLeftInset + context.theme().scale(14);
            titleAreaWidth = Math.max(20, drawW - (titleAreaX - drawX) - scaledTitleRightInset);
            titleX = titleAreaX;
        } else if (centerTitle) {
            titleX = titleAreaX;
        }
        String displayTitle = PackUiText.trimToWidth(drawContext.textRenderer(), title, titleAreaWidth, drawContext.theme().fontFor(titleTone), titleColor);
        if (centerTitle) {
            int titleWidth = PackUiText.width(drawContext.textRenderer(), displayTitle, drawContext.theme().fontFor(titleTone), titleColor);
            titleX = PackUiSizing.centerInside(titleAreaX, titleAreaWidth, titleWidth);
        }
        PackUiText.draw(drawContext.drawContext(), drawContext.textRenderer(), displayTitle, drawContext.theme().fontFor(titleTone), drawContext.applyAlpha(titleColor), titleX, titleY, false);

        if (shouldShowBodyArea()) {
            int clipTop = Math.round(y + headerH);
            int clipBottom = Math.round(y + height);
            if (clipBottom > clipTop) {
                drawContext.viewport().enableScissor(drawContext.drawContext(), x, y + headerH, x + width, y + height);
                try {
                    super.render(drawContext);
                } finally {
                    drawContext.viewport().disableScissor(drawContext.drawContext());
                }
                float coverAlpha = 1.0f - bodyFadeAlpha;
                if (coverAlpha > 0.001f) {
                    float easedCover = 1.0f - (float) Math.pow(1.0f - coverAlpha, 2.0);
                    float fadeAlpha = Math.min(0.96f, 0.10f + (easedCover * 0.86f));
                    drawContext.drawContext().fill(drawX + 1, clipTop, drawX + drawW - 1, clipBottom, drawContext.applyAlpha(context.theme().inactiveBodyFadeFill(), fadeAlpha));
                }
            }
        }
    }

    private boolean shouldShowBodyArea() {
        return showBody || bodyFadeAlpha > 0.001f;
    }

    private void updateBodyFadeAlpha() {
        lastAnimationNanos = System.nanoTime();
        bodyFadeAlpha = showBody ? 1.0f : 0.0f;
    }
}
