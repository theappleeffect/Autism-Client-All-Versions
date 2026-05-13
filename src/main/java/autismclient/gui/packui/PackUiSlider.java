package autismclient.gui.packui;

import java.util.function.Consumer;

public class PackUiSlider extends PackUiNode {
    private float min = 0.0f;
    private float max = 1.0f;
    private float value = 0.0f;
    private float step = 0.0f;
    private boolean dragging = false;
    private Consumer<Float> onChange;
    private Consumer<Float> onRelease;

    public PackUiSlider() {
        this.height = 12;
    }

    public PackUiSlider setRange(float min, float max) {
        this.min = min;
        this.max = Math.max(min, max);
        setValue(value);
        return this;
    }

    public PackUiSlider setStep(float step) {
        this.step = Math.max(0.0f, step);
        setValue(value);
        return this;
    }

    public PackUiSlider setValue(float value) {
        float snapped = snap(clamp(value));
        if (Math.abs(this.value - snapped) > 0.0001f) {
            this.value = snapped;
            if (onChange != null) onChange.accept(this.value);
        }
        return this;
    }

    public float value() {
        return value;
    }

    public PackUiSlider setOnChange(Consumer<Float> onChange) {
        this.onChange = onChange;
        return this;
    }

    public PackUiSlider setOnRelease(Consumer<Float> onRelease) {
        this.onRelease = onRelease;
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
        int centerY = drawY + Math.round(drawH / 2.0f);
        float ratio = normalized();
        int knobX = drawX + Math.round(ratio * drawW);
        int fillColor = PackUiSizing.lerpColor(0xFFFF5A5A, 0xFF66E08A, ratio);
        int trackHalf = Math.max(1, Math.min(3, drawH / 3));
        int knobHalf = Math.max(2, Math.min(4, drawH / 2));
        int knobTop = drawY + Math.max(1, (drawH - Math.max(6, drawH - 2)) / 2);
        int knobBottom = drawY + drawH - Math.max(1, (drawH - Math.max(6, drawH - 2)) / 2);

        context.drawContext().fill(drawX, centerY - trackHalf, drawX + drawW, centerY + trackHalf, context.applyAlpha(0x7C0A090C));
        context.drawContext().fill(drawX, centerY - trackHalf, drawX + drawW, centerY - trackHalf + 1, context.applyAlpha(0xFF8F3131));
        context.drawContext().fill(drawX, centerY + trackHalf - 1, drawX + drawW, centerY + trackHalf, context.applyAlpha(0xFF8F3131));
        context.drawContext().fill(drawX, centerY - trackHalf, knobX, centerY + trackHalf, context.applyAlpha(fillColor));
        context.drawContext().fill(knobX - knobHalf, knobTop, knobX + knobHalf, knobBottom, context.applyAlpha(0xFFF0F0F0));
    }

    @Override
    public boolean mouseClicked(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) return false;
        dragging = true;
        updateFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            if (onRelease != null) onRelease.accept(value);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(PackUiRenderContext context, float mouseX, float mouseY, int button, float deltaX, float deltaY) {
        if (!dragging || button != 0) return false;
        updateFromMouse(mouseX);
        return true;
    }

    private void updateFromMouse(float mouseX) {
        if (width <= 0.0f) return;
        float ratio = (mouseX - x) / width;
        setValue(min + Math.max(0.0f, Math.min(1.0f, ratio)) * (max - min));
    }

    private float normalized() {
        if (max <= min) return 0.0f;
        return (value - min) / (max - min);
    }

    private float clamp(float value) {
        return Math.max(min, Math.min(max, value));
    }

    private float snap(float value) {
        if (step <= 0.0f) return value;
        return min + Math.round((value - min) / step) * step;
    }
}
