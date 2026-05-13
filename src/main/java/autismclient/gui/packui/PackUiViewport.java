package autismclient.gui.packui;

import autismclient.util.PackUtilUiScale;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class PackUiViewport {
    private final float uiWidth;
    private final float uiHeight;

    private PackUiViewport(float uiWidth, float uiHeight) {
        this.uiWidth = Math.max(1.0f, uiWidth);
        this.uiHeight = Math.max(1.0f, uiHeight);
    }

    public static PackUiViewport current(float density) {
        return new PackUiViewport(
            PackUtilUiScale.getVirtualScreenWidth(),
            PackUtilUiScale.getVirtualScreenHeight()
        );
    }

    public float uiWidth() {
        return uiWidth;
    }

    public float uiHeight() {
        return uiHeight;
    }

    public float drawScaleX() {
        return 1.0f;
    }

    public float drawScaleY() {
        return 1.0f;
    }

    public float toUiX(double screenX) {
        return (float) screenX;
    }

    public float toUiY(double screenY) {
        return (float) screenY;
    }

    public void push(GuiGraphicsExtractor context) {
        PackUtilUiScale.pushOverlayScale(context);
    }

    public void pop(GuiGraphicsExtractor context) {
        PackUtilUiScale.popOverlayScale(context);
    }

    public void enableScissor(GuiGraphicsExtractor context, float x1, float y1, float x2, float y2) {
        PackUtilUiScale.enableOverlayScissor(
            context,
            Math.round(x1),
            Math.round(y1),
            Math.round(x2),
            Math.round(y2)
        );
    }

    public void disableScissor(GuiGraphicsExtractor context) {
        context.disableScissor();
    }
}
