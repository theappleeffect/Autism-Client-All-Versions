package autismclient.gui.packui;

import net.minecraft.resources.Identifier;

public class PackUiIconButton extends PackUiNode {
    private final Identifier icon;
    private final PackUiButton.Variant variant;
    private Runnable onPress;
    private int iconPadding = 4;

    public PackUiIconButton(Identifier icon, PackUiButton.Variant variant, Runnable onPress) {
        this.icon = icon;
        this.variant = variant == null ? PackUiButton.Variant.GHOST : variant;
        this.onPress = onPress;
        this.width = 20;
        this.height = 20;
    }

    private PackUiButtonFeedback feedbackState() {
        return PackUiButtonFeedback.forKey(
            "icon-button:"
                + Math.round(x) + ':'
                + Math.round(y) + ':'
                + Math.round(width) + ':'
                + Math.round(height) + ':'
                + variant + ':'
                + (icon == null ? "none" : icon.toString())
        );
    }

    public PackUiIconButton setOnPress(Runnable onPress) {
        this.onPress = onPress;
        return this;
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        return width;
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        return height;
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        int drawX = Math.round(x);
        int drawY = Math.round(y);
        int drawW = Math.round(width);
        int drawH = Math.round(height);
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
        int bg = context.theme().buttonFill(variant);
        int border = context.theme().buttonBorder(variant);
        int borderGlow = context.theme().buttonBorderGlow(variant);
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
            context.drawContext().fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawH - 1, context.applyAlpha(hoverTint));
        }
        if (clickGlow > 0.0f) {
            int pressGlow = ((int) (clickGlow * 14.0f) << 24) | (rippleFill & 0x00FFFFFF);
            context.drawContext().fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawH - 1, context.applyAlpha(pressGlow));
        }
        feedback.render(
            context.drawContext(),
            drawX + 1,
            drawY + 1,
            Math.max(0, drawW - 2),
            Math.max(0, drawH - 2),
            rippleRing,
            rippleFill,
            context.alpha()
        );
        context.drawContext().fill(drawX, drawY, drawX + drawW, drawY + 1, context.applyAlpha(border));
        context.drawContext().fill(drawX, drawY + drawH - 1, drawX + drawW, drawY + drawH, context.applyAlpha(border));
        context.drawContext().fill(drawX, drawY, drawX + 1, drawY + drawH, context.applyAlpha(border));
        context.drawContext().fill(drawX + drawW - 1, drawY, drawX + drawW, drawY + drawH, context.applyAlpha(border));
        context.drawContext().fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + 2, context.applyAlpha(0x24FFFFFF));
        context.drawContext().fill(drawX + 1, drawY + 1, drawX + 2, drawY + drawH - 1, context.applyAlpha(0x24FFFFFF));
        context.drawContext().fill(drawX + 1, drawY + drawH - 2, drawX + drawW - 1, drawY + drawH - 1, context.applyAlpha(0x18000000));
        context.drawContext().fill(drawX + drawW - 2, drawY + 1, drawX + drawW - 1, drawY + drawH - 1, context.applyAlpha(0x18000000));
        if (icon != null) {
            int iconSize = Math.max(1, Math.min(drawW, drawH) - (iconPadding * 2));
            if (PackUiControlGlyphs.isCloseIcon(icon) || PackUiControlGlyphs.isChevronIcon(icon)) {
                PackUiControlGlyphs.drawKnownIcon(
                    context.drawContext(),
                    icon,
                    drawX + iconPadding,
                    drawY + iconPadding,
                    iconSize,
                    context.theme().color(PackUiTone.BODY),
                    0xC43D171B,
                    context.alpha()
                );
            } else {
                context.drawTexturedQuad(icon, drawX + iconPadding, drawY + iconPadding, drawX + drawW - iconPadding, drawY + drawH - iconPadding);
            }
        }
    }

    @Override
    public boolean mouseClicked(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        if (!enabled || !contains(mouseX, mouseY) || button != 0) return false;
        feedbackState().triggerPress(mouseX - x, mouseY - y, width, height);
        if (onPress != null) onPress.run();
        return true;
    }
}
