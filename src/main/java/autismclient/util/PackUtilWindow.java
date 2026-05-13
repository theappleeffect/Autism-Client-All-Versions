package autismclient.util;

import autismclient.gui.packui.PackUiControlGlyphs;
import autismclient.gui.packui.PackUiHeaderControls;
import autismclient.gui.packui.PackUiRenderContext;
import autismclient.gui.packui.PackUiSizing;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class PackUtilWindow {
    protected static final int HEADER_HEIGHT = 16;
    protected static final int RESIZE_HANDLE = 10;
    private static final PackUiTheme THEME = new PackUiTheme();
    private static final int HEADER_PAD_X = 8;
    private static final int CONTROL_SIZE = 12;
    private static final int CONTROL_GAP = 2;
    private static final int CONTROL_MARGIN = 2;
    private static final float BODY_FADE_DURATION_SECONDS = 0.0f;

    private boolean bodyFadeInitialized = false;
    private float bodyFadeAlpha = 1.0f;
    private long lastBodyFadeNanos = System.nanoTime();

    protected boolean isResizeActive(double mouseX, double mouseY, int x, int y, int width, int height) {
        return false;
    }

    protected PackUtilWindowLayout clampToScreen(IPackUtilOverlay overlay) {
        return clampToScreen(overlay, overlay.getBounds());
    }

    protected PackUtilWindowLayout clampToScreen(IPackUtilOverlay overlay, PackUtilWindowLayout bounds) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || bounds == null) return bounds;

        int screenWidth = PackUtilUiScale.getVirtualScreenWidth();
        int screenHeight = PackUtilUiScale.getVirtualScreenHeight();
        int minWidth = Math.min(overlay.getMinWidth(), screenWidth);
        int minHeight = Math.min(overlay.getMinHeight(), screenHeight);

        int width = Math.max(minWidth, Math.min(bounds.width, screenWidth));
        int height = Math.max(minHeight, Math.min(bounds.height, screenHeight));
        int minVisibleWidth = Math.min(width, visibleHeaderWidth());
        int x = Math.max(Math.min(0, screenWidth - width), Math.min(bounds.x, Math.max(0, screenWidth - minVisibleWidth)));
        int y = Math.max(0, Math.min(bounds.y, Math.max(0, screenHeight - HEADER_HEIGHT)));

        return new PackUtilWindowLayout(x, y, width, height, bounds.visible, bounds.collapsed);
    }

    protected void renderWindowFrame(GuiGraphicsExtractor context, int mouseX, int mouseY, PackUtilWindowLayout bounds, String title, boolean collapsed, boolean activeDrag) {
        boolean active = activeDrag || isWindowActive();
        int frameHeight = getRenderedFrameHeight(bounds, collapsed);
        int frameFill = active ? THEME.windowFill() : THEME.windowFillInactive();
        int headerFill = activeDrag ? THEME.headerFill() : (active ? THEME.headerFill() : THEME.headerFillInactive());
        int border = activeDrag ? 0xFFFF5353 : (active ? THEME.borderColor() : THEME.borderSoft());
        int accent = activeDrag ? 0xFFFF6161 : (active ? THEME.headerAccent() : 0xAA7A2020);
        int titleColor = active ? THEME.color(PackUiTone.LABEL) : THEME.color(PackUiTone.MUTED);
        int titleY = PackUiSizing.alignTextY(bounds.y, HEADER_HEIGHT, THEME.fontHeight(PackUiTone.LABEL), THEME.bodyTextNudge());
        int controlLeft = getCollapseButtonLeft(bounds);
        int titleWidthLimit = Math.max(40, controlLeft - bounds.x - (headerPadX() * 2));
        String displayTitle = PackUiText.trimToWidth(
            Minecraft.getInstance().font,
            title,
            titleWidthLimit,
            THEME.fontFor(PackUiTone.LABEL),
            titleColor
        );

        context.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + frameHeight, frameFill);
        context.fill(bounds.x + 1, bounds.y + 1, bounds.x + bounds.width - 1, bounds.y + HEADER_HEIGHT - 1, headerFill);
        if (frameHeight > HEADER_HEIGHT) {
            context.fill(bounds.x + 1, bounds.y + HEADER_HEIGHT, bounds.x + bounds.width - 1, bounds.y + frameHeight - 1, THEME.inactiveCoverFill());
        }
        context.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + 1, border);
        context.fill(bounds.x, bounds.y + frameHeight - 1, bounds.x + bounds.width, bounds.y + frameHeight, border);
        context.fill(bounds.x, bounds.y, bounds.x + 1, bounds.y + frameHeight, border);
        context.fill(bounds.x + bounds.width - 1, bounds.y, bounds.x + bounds.width, bounds.y + frameHeight, border);
        context.fill(bounds.x, bounds.y + HEADER_HEIGHT - 1, bounds.x + bounds.width, bounds.y + HEADER_HEIGHT, accent);
        PackUiText.draw(
            context,
            Minecraft.getInstance().font,
            displayTitle,
            THEME.fontFor(PackUiTone.LABEL),
            titleColor,
            bounds.x + headerPadX(),
            titleY,
            false
        );

        drawWindowControls(context, mouseX, mouseY, bounds, collapsed, active);

    }

    protected boolean beginWindowBodyClip(GuiGraphicsExtractor context, PackUtilWindowLayout bounds, boolean collapsed) {
        int frameHeight = getRenderedFrameHeight(bounds, collapsed);
        if (frameHeight <= HEADER_HEIGHT + 1 || getBodyFadeAlpha(collapsed) <= 0.001f) return false;
        PackUtilUiScale.enableOverlayScissor(context, bounds.x + 1, bounds.y + HEADER_HEIGHT, bounds.x + bounds.width - 1, bounds.y + frameHeight - 1);
        return true;
    }

    protected void endWindowBodyClip(GuiGraphicsExtractor context, boolean clipped) {
        if (clipped) {
            context.disableScissor();
        }
    }

    protected void renderWindowInactiveOverlay(GuiGraphicsExtractor context, PackUtilWindowLayout bounds, boolean collapsed, boolean activeDrag) {
        int frameHeight = getRenderedFrameHeight(bounds, collapsed);
        int bodyTop = bounds.y + HEADER_HEIGHT;
        int bodyBottom = bounds.y + frameHeight - 1;
        if (bodyBottom > bodyTop) {
            float coverAlpha = 1.0f - getBodyFadeAlpha(collapsed);
            if (coverAlpha > 0.001f) {
                float easedCover = 1.0f - (float) Math.pow(1.0f - coverAlpha, 2.0);
                float fadeAlpha = Math.min(0.96f, 0.10f + (easedCover * 0.86f));
                    PackUiText.fill(context, bounds.x + 1, bodyTop, bounds.x + bounds.width - 1, bodyBottom, PackUiRenderContext.applyAlpha(THEME.inactiveBodyFadeFill(), fadeAlpha));
            }
        }
        if (activeDrag || isWindowActive()) return;
        context.fill(bounds.x + 1, bounds.y + 1, bounds.x + bounds.width - 1, bounds.y + frameHeight - 1, THEME.inactiveCoverFill());
    }

    protected int alignViewportHeight(int innerHeight, int rowStep) {
        int safeInnerHeight = Math.max(0, innerHeight);
        int safeRowStep = Math.max(1, rowStep);
        if (safeInnerHeight == 0 || safeRowStep <= 1) {
            return safeInnerHeight;
        }

        int aligned = (safeInnerHeight / safeRowStep) * safeRowStep;
        return aligned > 0 ? aligned : Math.min(safeInnerHeight, safeRowStep);
    }

    protected int quantizeScrollOffset(int offset, int stepSize, int maxScroll) {
        int clampedMax = Math.max(0, maxScroll);
        int clampedOffset = Math.max(0, Math.min(offset, clampedMax));
        int safeStepSize = Math.max(1, stepSize);
        if (safeStepSize <= 1) {
            return clampedOffset;
        }

        int quantized = (clampedOffset / safeStepSize) * safeStepSize;
        if (quantized > clampedMax) {
            quantized = (clampedMax / safeStepSize) * safeStepSize;
        }
        return Math.max(0, Math.min(quantized, clampedMax));
    }

    protected int getRenderedFrameHeight(PackUtilWindowLayout bounds, boolean collapsed) {
        if (bounds == null) return HEADER_HEIGHT;
        float bodyAlpha = getBodyFadeAlpha(collapsed);
        if (collapsed && bodyAlpha <= 0.001f) return HEADER_HEIGHT;
        return Math.max(HEADER_HEIGHT, bounds.height);
    }

    protected boolean isOverCloseButton(double mouseX, double mouseY, PackUtilWindowLayout bounds) {
        int x = getCloseButtonLeft(bounds);
        int y = getControlTop(bounds);
        return mouseX >= x && mouseX <= x + controlSize() && mouseY >= y && mouseY <= y + controlSize();
    }

    protected boolean isOverCollapseButton(double mouseX, double mouseY, PackUtilWindowLayout bounds) {
        if (shouldUseSharedHeaderClickCollapse()) return false;
        int x = getCollapseButtonLeft(bounds);
        int y = getControlTop(bounds);
        return mouseX >= x && mouseX <= x + controlSize() && mouseY >= y && mouseY <= y + controlSize();
    }

    protected boolean isOverWindowControl(double mouseX, double mouseY, PackUtilWindowLayout bounds) {
        return isOverCloseButton(mouseX, mouseY, bounds) || isOverCollapseButton(mouseX, mouseY, bounds);
    }

    private void drawWindowControls(GuiGraphicsExtractor context, int mouseX, int mouseY, PackUtilWindowLayout bounds, boolean collapsed, boolean active) {
        int controlTop = getControlTop(bounds);
        int closeX = getCloseButtonLeft(bounds);
        int collapseX = getCollapseButtonLeft(bounds);
        boolean hoverClose = isOverCloseButton(mouseX, mouseY, bounds);

        PackUiControlGlyphs.drawChevron(
            context,
            collapseX + 1,
            controlTop + 1,
            controlSize() - 2,
            collapsed ? PackUiControlGlyphs.ChevronDirection.RIGHT : PackUiControlGlyphs.ChevronDirection.DOWN,
            0xFFF4EDED,
            0xC43D171B,
            active ? 1.0f : 0.56f
        );
        PackUiHeaderControls.drawCloseButton(context, closeX, controlTop, controlSize(), controlSize(), hoverClose ? 1.0f : 0.0f, active, 1.0f);
    }

    private boolean isWindowActive() {
        if (!(this instanceof IPackUtilOverlay overlay)) return true;
        return PackUtilOverlayManager.get().isFocusedOverlay(overlay) || PackUtilOverlayManager.get().isTopOverlay(overlay);
    }

    private float getBodyFadeAlpha(boolean collapsed) {
        lastBodyFadeNanos = System.nanoTime();
        bodyFadeInitialized = true;
        bodyFadeAlpha = collapsed ? 0.0f : 1.0f;
        return bodyFadeAlpha;
    }

    private int getControlTop(PackUtilWindowLayout bounds) {
        return bounds.y + Math.max(1, (HEADER_HEIGHT - controlSize()) / 2);
    }

    private int getCloseButtonLeft(PackUtilWindowLayout bounds) {
        return bounds.x + bounds.width - controlMargin() - controlSize();
    }

    private int getCollapseButtonLeft(PackUtilWindowLayout bounds) {
        return getCloseButtonLeft(bounds) - controlGap() - controlSize();
    }

    private static int headerPadX() {
        return 8;
    }

    private static int controlSize() {
        return 12;
    }

    private static int controlGap() {
        return 2;
    }

    private static int controlMargin() {
        return 2;
    }

    private static int visibleHeaderWidth() {
        return 56;
    }

    private boolean shouldUseSharedHeaderClickCollapse() {
        return this instanceof IPackUtilOverlay overlay && overlay.usesSharedHeaderClickCollapse();
    }
}
