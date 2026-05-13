package autismclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class PackUtilUiScale {
    public static final int FIXED_GUI_SCALE = 2;
    private static int overlayScaleDepth = 0;

    private PackUtilUiScale() {}

    public static double toVirtual(double value) {
        return value / getOverlayDrawScale();
    }

    public static int toVirtualInt(double value) {
        double virtual = toVirtual(value);
        return virtual < 0 ? (int) Math.floor(virtual) : (int) Math.round(virtual);
    }

    public static int getVirtualScreenWidth() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return 0;
        int width = (int) (mc.getWindow().getWidth() / (double) FIXED_GUI_SCALE);
        return mc.getWindow().getWidth() / (double) FIXED_GUI_SCALE > width ? width + 1 : width;
    }

    public static int getVirtualScreenHeight() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return 0;
        int height = (int) (mc.getWindow().getHeight() / (double) FIXED_GUI_SCALE);
        return mc.getWindow().getHeight() / (double) FIXED_GUI_SCALE > height ? height + 1 : height;
    }

    public static float getOverlayDrawScale() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || mc.getWindow().getGuiScale() <= 0) return 1.0f;
        return (float) FIXED_GUI_SCALE / (float) mc.getWindow().getGuiScale();
    }

    public static int virtualToFramebufferX(int x) {
        return (int) Math.floor(x * (double) FIXED_GUI_SCALE);
    }

    public static int virtualToFramebufferY(int y) {
        return (int) Math.floor(y * (double) FIXED_GUI_SCALE);
    }

    public static int virtualToFramebufferSize(int size) {
        return Math.max(0, (int) Math.ceil(size * (double) FIXED_GUI_SCALE));
    }

    public static void pushOverlayScale(GuiGraphicsExtractor context) {
        if (context == null) return;
        context.pose().pushMatrix();
        if (overlayScaleDepth == 0) {
            float scale = getOverlayDrawScale();
            context.pose().scale(scale, scale);
        }
        overlayScaleDepth++;
    }

    public static void popOverlayScale(GuiGraphicsExtractor context) {
        if (context == null) return;
        if (overlayScaleDepth > 0) overlayScaleDepth--;
        context.pose().popMatrix();
    }

    public static void enableOverlayScissor(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2) {
        context.enableScissor(x1, y1, x2, y2);
    }
}
