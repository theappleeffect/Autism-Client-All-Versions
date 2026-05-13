package autismclient.gui.packui;

public class PackUiProgressBar extends PackUiNode {
    private float progress = 0.0f;
    private int startColor = 0xFFFF5A5A;
    private int endColor = 0xFF66E08A;
    private int borderColor = 0xFF8F3131;
    private int fillBackground = 0x7C0A090C;

    public PackUiProgressBar() {
        this.height = 12;
    }

    public PackUiProgressBar setProgress(float progress) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
        return this;
    }

    public PackUiProgressBar setGradient(int startColor, int endColor) {
        this.startColor = startColor;
        this.endColor = endColor;
        return this;
    }

    public PackUiProgressBar setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        return this;
    }

    public PackUiProgressBar setFillBackground(int fillBackground) {
        this.fillBackground = fillBackground;
        return this;
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        return 12.0f;
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        int drawX = Math.round(x);
        int drawY = Math.round(y);
        int drawW = Math.round(width);
        int drawH = Math.round(height);
        int innerW = Math.max(0, drawW - 2);
        int fillW = Math.max(0, Math.min(innerW, Math.round(innerW * progress)));
        int fillColor = PackUiSizing.lerpColor(startColor, endColor, progress);

        context.drawContext().fill(drawX, drawY, drawX + drawW, drawY + drawH, context.applyAlpha(fillBackground));
        context.drawContext().fill(drawX, drawY, drawX + drawW, drawY + 1, context.applyAlpha(borderColor));
        context.drawContext().fill(drawX, drawY + drawH - 1, drawX + drawW, drawY + drawH, context.applyAlpha(borderColor));
        context.drawContext().fill(drawX, drawY, drawX + 1, drawY + drawH, context.applyAlpha(borderColor));
        context.drawContext().fill(drawX + drawW - 1, drawY, drawX + drawW, drawY + drawH, context.applyAlpha(borderColor));
        if (fillW > 0) {
            context.drawContext().fill(drawX + 1, drawY + 1, drawX + 1 + fillW, drawY + drawH - 1, context.applyAlpha(fillColor));
        }
    }
}
