package autismclient.gui.packui;

public class PackUiAccordionSection extends PackUiContainer {
    private static final float BODY_FADE_DURATION_SECONDS = 0.14f;

    private final PackUiColumn content = new PackUiColumn();
    private String title;
    private boolean expanded = true;
    private float bodyFadeAlpha = 1.0f;
    private int headerHeight = 18;
    private int contentTopGap = 2;
    private long lastAnimationNanos = System.nanoTime();

    public PackUiAccordionSection(String title) {
        this.title = title == null ? "" : title;
        content.setPadding(PackUiInsets.NONE).setGap(2);
        add(content);
    }

    public PackUiColumn content() {
        return content;
    }

    public PackUiAccordionSection setTitle(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    public PackUiAccordionSection setExpanded(boolean expanded) {
        this.expanded = expanded;
        return this;
    }

    public PackUiAccordionSection syncExpanded(boolean expanded) {
        this.expanded = expanded;
        this.bodyFadeAlpha = expanded ? 1.0f : 0.0f;
        this.lastAnimationNanos = System.nanoTime();
        return this;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public PackUiAccordionSection setHeaderHeight(int headerHeight) {
        this.headerHeight = Math.max(12, headerHeight);
        return this;
    }

    public PackUiAccordionSection setContentTopGap(int contentTopGap) {
        this.contentTopGap = Math.max(0, contentTopGap);
        return this;
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        return Math.max(PackUiText.width(context.textRenderer(), title, context.theme().fontFor(PackUiTone.LABEL), context.theme().color(PackUiTone.LABEL)) + context.theme().scale(18),
            content.preferredWidth(context));
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        updateBodyFadeAlpha();
        int scaledHeaderHeight = Math.max(context.theme().scale(headerHeight), context.theme().lineHeight(PackUiTone.LABEL, 4));
        if (!shouldShowBodyArea()) return scaledHeaderHeight;
        int scaledContentTopGap = context.theme().scale(contentTopGap);
        float bodyHeight = scaledContentTopGap + content.preferredHeight(context, availableWidth);
        return scaledHeaderHeight + bodyHeight;
    }

    @Override
    protected void layoutChildren(PackUiRenderContext context) {
        updateBodyFadeAlpha();
        int scaledHeaderHeight = Math.max(context.theme().scale(headerHeight), context.theme().lineHeight(PackUiTone.LABEL, 4));
        int scaledContentTopGap = context.theme().scale(contentTopGap);
        boolean showContent = shouldShowBodyArea();
        float contentY = y + scaledHeaderHeight + (showContent ? scaledContentTopGap : 0.0f);
        float contentH = showContent ? Math.max(0.0f, height - scaledHeaderHeight - scaledContentTopGap) : 0.0f;
        content.setVisible(showContent);
        content.setBounds(x, contentY, width, contentH);
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        updateBodyFadeAlpha();
        int drawX = Math.round(x);
        int drawY = Math.round(y);
        int drawW = Math.round(width);
        int drawHeaderH = Math.max(context.theme().scale(headerHeight), context.theme().lineHeight(PackUiTone.LABEL, 4));
        boolean hovered = context.mouseX() >= x && context.mouseX() <= x + width && context.mouseY() >= y && context.mouseY() <= y + drawHeaderH;
        float hover = updateHover(hovered, context.delta());

        int fill = expanded ? context.theme().headerFill() : context.theme().headerFillInactive();
        int border = context.theme().borderColor();
        context.drawContext().fill(drawX, drawY, drawX + drawW, drawY + drawHeaderH, context.applyAlpha(fill));
        if (hover > 0.0f) {
            int hoverTint = ((int) (hover * 18) << 24) | 0x00FF3B3B;
            context.drawContext().fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawHeaderH - 1, context.applyAlpha(hoverTint));
        }
        context.drawContext().fill(drawX, drawY, drawX + drawW, drawY + 1, context.applyAlpha(border));
        context.drawContext().fill(drawX, drawY + drawHeaderH - 1, drawX + drawW, drawY + drawHeaderH, context.applyAlpha(border));
        context.drawContext().fill(drawX, drawY, drawX + 1, drawY + drawHeaderH, context.applyAlpha(border));
        context.drawContext().fill(drawX + drawW - 1, drawY, drawX + drawW, drawY + drawHeaderH, context.applyAlpha(border));
        context.drawContext().fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + 2, context.applyAlpha(0x356B2222));
        context.drawContext().fill(drawX + 1, drawY + 1, drawX + 2, drawY + drawHeaderH - 1, context.applyAlpha(0x356B2222));
        context.drawContext().fill(drawX + 1, drawY + drawHeaderH - 2, drawX + drawW - 1, drawY + drawHeaderH - 1, context.applyAlpha(0x22000000));
        context.drawContext().fill(drawX + drawW - 2, drawY + 1, drawX + drawW - 1, drawY + drawHeaderH - 1, context.applyAlpha(0x22000000));

        int titleColor = context.theme().color(PackUiTone.LABEL);
        int titleY = PackUiSizing.alignTextY(drawY, drawHeaderH, context.theme().fontHeight(PackUiTone.LABEL), context.theme().bodyTextNudge());
        PackUiText.draw(context.drawContext(), context.textRenderer(), title, context.theme().fontFor(PackUiTone.LABEL), context.applyAlpha(titleColor), drawX + context.theme().scale(6), titleY, false);

        int toggleSize = context.theme().scale(10);
        int toggleX = drawX + drawW - context.theme().scale(6) - toggleSize;
        int toggleY = drawY + Math.max(1, (drawHeaderH - toggleSize) / 2);
        PackUiHeaderControls.drawAnimatedArrow(context.drawContext(), toggleX, toggleY, toggleSize, expanded ? 1.0f : 0.0f, 1.0f);

        if (shouldShowBodyArea()) {
            int clipTop = Math.round(y + drawHeaderH);
            int clipBottom = Math.round(y + height);
            if (clipBottom > clipTop) {
                context.viewport().enableScissor(context.drawContext(), x, y + drawHeaderH, x + width, y + height);
                try {
                    super.render(context);
                } finally {
                    context.viewport().disableScissor(context.drawContext());
                }
                float coverAlpha = 1.0f - bodyFadeAlpha;
                if (coverAlpha > 0.001f) {
                    float easedCover = 1.0f - (float) Math.pow(1.0f - coverAlpha, 2.0);
                    float fadeAlpha = Math.min(0.96f, 0.10f + (easedCover * 0.86f));
                    context.drawContext().fill(drawX + 1, clipTop, drawX + drawW - 1, clipBottom, context.applyAlpha(context.theme().inactiveBodyFadeFill(), fadeAlpha));
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        int scaledHeaderHeight = Math.max(context.theme().scale(headerHeight), context.theme().lineHeight(PackUiTone.LABEL, 4));
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + scaledHeaderHeight) {
            expanded = !expanded;
            return true;
        }
        return expanded && super.mouseClicked(context, mouseX, mouseY, button);
    }

    private boolean shouldShowBodyArea() {
        return expanded || bodyFadeAlpha > 0.001f;
    }

    private void updateBodyFadeAlpha() {
        long now = System.nanoTime();
        float elapsedSeconds = Math.max(0.0f, (now - lastAnimationNanos) / 1_000_000_000.0f);
        lastAnimationNanos = now;
        float target = expanded ? 1.0f : 0.0f;
        if (bodyFadeAlpha == target) return;
        float step = BODY_FADE_DURATION_SECONDS <= 0.0f ? 1.0f : Math.min(1.0f, elapsedSeconds / BODY_FADE_DURATION_SECONDS);
        if (target > bodyFadeAlpha) bodyFadeAlpha = Math.min(target, bodyFadeAlpha + step);
        else bodyFadeAlpha = Math.max(target, bodyFadeAlpha - step);
        if (Math.abs(bodyFadeAlpha - target) < 0.01f) {
            bodyFadeAlpha = target;
        }
    }
}
