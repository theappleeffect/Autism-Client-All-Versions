package autismclient.gui.packui;

import net.minecraft.resources.Identifier;

public class PackUiButton extends PackUiNode {

    public enum ContentAlignment {
        CENTER,
        START
    }

    public enum Variant {
        PRIMARY,
        SECONDARY,
        GHOST,
        DANGER,
        SUCCESS
    }

    private String text;
    private Variant variant;
    private Runnable onPress;
    private float minWidth = 26.0f;
    private float maxWidth = Float.MAX_VALUE;
    private float preferredWidth = -1.0f;
    private int horizontalPadding = 8;
    private int fixedHeight = -1;
    private int textYOffset = 0;
    private Identifier leadingIcon;
    private int iconSize = 10;
    private int iconGap = 4;
    private ContentAlignment contentAlignment = ContentAlignment.CENTER;
    private PackUiTone tone = PackUiTone.BODY;

    private Identifier resolvedFont(PackUiTheme theme, int height) {
        return theme.fontFor(tone);
    }

    private PackUiButtonFeedback feedbackState() {
        return PackUiButtonFeedback.forKey(
            "button:"
                + Math.round(x) + ':'
                + Math.round(y) + ':'
                + Math.round(width) + ':'
                + Math.round(height) + ':'
                + variant + ':'
                + tone + ':'
                + contentAlignment + ':'
                + (leadingIcon == null ? "none" : leadingIcon.toString())
        );
    }

    public PackUiButton(String text, Variant variant, Runnable onPress) {
        this.text = text == null ? "" : text;
        this.variant = variant == null ? Variant.SECONDARY : variant;
        this.onPress = onPress;
        this.height = 16;
    }

    public PackUiButton setText(String text) {
        this.text = text == null ? "" : text;
        return this;
    }

    public PackUiButton setVariant(Variant variant) {
        this.variant = variant == null ? Variant.SECONDARY : variant;
        return this;
    }

    public PackUiButton setOnPress(Runnable onPress) {
        this.onPress = onPress;
        return this;
    }

    public PackUiButton setMinWidth(float minWidth) {
        this.minWidth = Math.max(0.0f, minWidth);
        return this;
    }

    public PackUiButton setMaxWidth(float maxWidth) {
        this.maxWidth = Math.max(this.minWidth, maxWidth);
        return this;
    }

    public PackUiButton setPreferredWidth(float preferredWidth) {
        this.preferredWidth = preferredWidth;
        return this;
    }

    public PackUiButton setHorizontalPadding(int horizontalPadding) {
        this.horizontalPadding = Math.max(0, horizontalPadding);
        return this;
    }

    public PackUiButton setButtonHeight(int fixedHeight) {
        this.fixedHeight = Math.max(1, fixedHeight);
        return this;
    }

    public PackUiButton setTextYOffset(int textYOffset) {
        this.textYOffset = textYOffset;
        return this;
    }

    public PackUiButton setLeadingIcon(Identifier leadingIcon) {
        this.leadingIcon = leadingIcon;
        return this;
    }

    public PackUiButton setIconSize(int iconSize) {
        this.iconSize = Math.max(1, iconSize);
        return this;
    }

    public PackUiButton setIconGap(int iconGap) {
        this.iconGap = Math.max(0, iconGap);
        return this;
    }

    public PackUiButton setContentAlignment(ContentAlignment contentAlignment) {
        this.contentAlignment = contentAlignment == null ? ContentAlignment.CENTER : contentAlignment;
        return this;
    }

    public PackUiButton setTone(PackUiTone tone) {
        this.tone = tone == null ? PackUiTone.BODY : tone;
        return this;
    }

    @Override
    public PackUiButton setGrowX(boolean growX) {
        super.setGrowX(growX);
        return this;
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        int measureHeight = fixedHeight > 0 ? fixedHeight : context.theme().buttonHeight();
        Identifier font = resolvedFont(context.theme(), measureHeight);
        int scaledHorizontalPadding = context.theme().scale(horizontalPadding);
        int scaledIconSize = context.theme().scale(iconSize);
        int scaledIconGap = context.theme().scale(iconGap);
        float fittedWidth = PackUiSizing.fitTextWidth(
            context.textRenderer(),
            text,
            font,
            context.theme().color(PackUiTone.BODY),
            scaledHorizontalPadding,
            minWidth,
            maxWidth
        );
        if (leadingIcon != null) {
            fittedWidth += scaledIconSize + scaledIconGap;
        }
        if (preferredWidth > 0.0f) {
            return PackUiSizing.clamp(Math.max(preferredWidth, fittedWidth), minWidth, maxWidth);
        }
        return PackUiSizing.clamp(fittedWidth, minWidth, maxWidth);
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        int resolvedFixedHeight = fixedHeight > 0 ? fixedHeight : context.theme().buttonHeight();
        int contentHeight = context.theme().fontHeight(tone) + context.theme().scale(4);
        return Math.max(12, Math.max(resolvedFixedHeight, contentHeight));
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        int drawX = Math.round(x);
        int drawY = Math.round(y);
        int drawW = Math.round(width);
        int drawH = Math.round(height);
        Identifier font = resolvedFont(context.theme(), drawH);
        boolean hovered = enabled && contains(context.mouseX(), context.mouseY());
        PackUiButtonFeedback feedback = feedbackState();
        float hover = feedback.update(
            hovered,
            enabled && PackUiButtonFeedback.isPrimaryPointerDown(),
            context.mouseX() - x,
            context.mouseY() - y,
            width,
            height
        );
        float clickGlow = feedback.clickGlowProgress();
        int scaledHorizontalPadding = context.theme().scale(horizontalPadding);
        int scaledIconSize = context.theme().scale(iconSize);
        int scaledIconGap = context.theme().scale(iconGap);
        int borderThickness = Math.max(1, context.theme().scale(1));
        int availableTextWidth = Math.max(1, drawW - (scaledHorizontalPadding * 2) - (leadingIcon != null ? scaledIconSize + scaledIconGap : 0));
        String displayComponent = PackUiText.trimToWidth(
            context.textRenderer(),
            text,
            availableTextWidth,
            font,
            0xFFFFF4F4
        );
        int bg = context.theme().buttonFill(variant);
        int border = context.theme().buttonBorder(variant);
        int borderGlow = context.theme().buttonBorderGlow(variant);
        int textColor = context.theme().buttonTextColor(variant);
        int rippleRing = context.theme().buttonRippleRing(variant);
        int rippleFill = context.theme().buttonRippleFill(variant);
        float hoverVisual = Math.max(hover * 0.9f, clickGlow * 0.55f);
        border = PackUiSizing.lerpColor(border, borderGlow, Math.min(1.0f, (hover * 0.35f) + (clickGlow * 0.85f)));

        context.drawContext().fill(drawX, drawY, drawX + drawW, drawY + drawH, context.applyAlpha(bg));
        if (hoverVisual > 0.0f) {
            int hoverBase = switch (variant) {
                case SUCCESS -> 0x0032D86A;
                case DANGER -> 0x00FF3B3B;
                case PRIMARY -> 0x00FF5E5E;
                case SECONDARY, GHOST -> 0x00FF3B3B;
            };
            int hoverTint = ((int) Math.min(42.0f, (hover * 24.0f) + (clickGlow * 12.0f)) << 24) | hoverBase;
            context.drawContext().fill(drawX + borderThickness, drawY + borderThickness, drawX + drawW - borderThickness, drawY + drawH - borderThickness, context.applyAlpha(hoverTint));
        }
        if (clickGlow > 0.0f) {
            int pressGlow = ((int) (clickGlow * 14.0f) << 24) | (rippleFill & 0x00FFFFFF);
            context.drawContext().fill(drawX + borderThickness, drawY + borderThickness, drawX + drawW - borderThickness, drawY + drawH - borderThickness, context.applyAlpha(pressGlow));
        }
        feedback.render(
            context.drawContext(),
            drawX + borderThickness,
            drawY + borderThickness,
            Math.max(0, drawW - (borderThickness * 2)),
            Math.max(0, drawH - (borderThickness * 2)),
            rippleRing,
            rippleFill,
            context.alpha()
        );
        context.drawContext().fill(drawX, drawY, drawX + drawW, drawY + borderThickness, context.applyAlpha(border));
        context.drawContext().fill(drawX, drawY + drawH - borderThickness, drawX + drawW, drawY + drawH, context.applyAlpha(border));
        context.drawContext().fill(drawX, drawY, drawX + borderThickness, drawY + drawH, context.applyAlpha(border));
        context.drawContext().fill(drawX + drawW - borderThickness, drawY, drawX + drawW, drawY + drawH, context.applyAlpha(border));

        int textWidth = PackUiText.width(context.textRenderer(), displayComponent, font, textColor);
        int contentWidth = textWidth + (leadingIcon != null ? scaledIconSize + scaledIconGap : 0);
        int contentX = contentAlignment == ContentAlignment.START
            ? drawX + scaledHorizontalPadding
            : PackUiSizing.centerInside(drawX, drawW, contentWidth);
        int textY = PackUiSizing.alignTextY(drawY, drawH, context.theme().fontHeight(tone), context.theme().buttonTextNudge() + textYOffset);
        if (leadingIcon != null) {
            int iconY = drawY + Math.max(1, (drawH - scaledIconSize) / 2);
            context.drawTexturedQuad(leadingIcon, contentX, iconY, contentX + scaledIconSize, iconY + scaledIconSize);
        }
        int textX = contentX + (leadingIcon != null ? scaledIconSize + scaledIconGap : 0);
        PackUiText.draw(context.drawContext(), context.textRenderer(), displayComponent, font, context.applyAlpha(textColor), textX, textY, false);
    }

    @Override
    public boolean mouseClicked(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        if (!enabled || !contains(mouseX, mouseY) || button != 0) return false;
        feedbackState().triggerPress(mouseX - x, mouseY - y, width, height);
        if (onPress != null) onPress.run();
        return true;
    }
}
