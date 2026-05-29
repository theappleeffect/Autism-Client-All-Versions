package autismclient.util;

import autismclient.gui.packui.PackUiText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import autismclient.util.PackUtilUiScale;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class PackUtilOverlayManager {
    private static final PackUtilOverlayManager INSTANCE = new PackUtilOverlayManager();
    public static final int HOVER_BLOCKED_MOUSE = -10000;
    private static final double HEADER_CLICK_DRAG_THRESHOLD = 3.0;

    private final List<IPackUtilOverlay> overlays = new CopyOnWriteArrayList<>();
    private IPackUtilOverlay focusedOverlay = null;

    private PackUtilOverlayManager() {}

    public static PackUtilOverlayManager get() {
        return INSTANCE;
    }

    public List<IPackUtilOverlay> getOverlays() {
        return overlays;
    }

    public void register(IPackUtilOverlay overlay) {
        if (overlay == null) return;
        if (!overlays.contains(overlay)) {
            overlays.add(overlay);
        }
        restoreSavedOverlayOrder();
        normalizeOverlayStack();
        if (overlay instanceof PackUtilLauncherOverlay || "packutil-launcher".equals(overlay.getOverlayId())) {
            reclampAllOverlays();
        }
    }

    public void unregister(IPackUtilOverlay overlay) {
        if (overlay == null) return;
        overlays.remove(overlay);
        if (focusedOverlay == overlay) focusedOverlay = null;
        saveOverlayOrder();
    }

    public void clear() {
        overlays.clear();
        draggingOverlay = null;
        resizingOverlay = null;
        headerCollapseOverlay = null;
        focusedOverlay = null;
        headerCollapseMoved = false;
        headerCollapseStartBounds = null;
        resizeStartBounds = null;
        inventoryMouseDown = false;
    }

    public void bringToFront(IPackUtilOverlay overlay) {
        if (overlay == null) return;
        overlays.remove(overlay);
        overlays.add(overlay);
        focusedOverlay = overlay;
        PackUtilSharedState.get().setFocusedOverlayId(overlay.getOverlayId());
        normalizeOverlayStack();
        saveOverlayOrder();
    }

    public void bringToFrontParent(Object childComponent) {
        if (childComponent == null) return;
        for (IPackUtilOverlay overlay : overlays) {
            if (overlay instanceof PackUtilCustomFilterOverlay filterOverlay
                && filterOverlay.getPacketSelectorOverlay() == childComponent) {
                bringToFront(overlay);
                return;
            }
        }
    }

    private void restoreSavedOverlayOrder() {
        if (overlays.size() < 2) {
            restoreFocusedOverlay();
            normalizeOverlayStack();
            return;
        }

        List<String> savedOrder = PackUtilSharedState.get().getOverlayOrder();
        if (savedOrder.isEmpty()) {
            restoreFocusedOverlay();
            normalizeOverlayStack();
            return;
        }

        String focusedId = PackUtilSharedState.get().getFocusedOverlayId();

        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < savedOrder.size(); i++) {
            positions.putIfAbsent(savedOrder.get(i), i);
        }

        List<IPackUtilOverlay> ordered = new ArrayList<>(overlays);
        ordered.sort(Comparator
            .comparingInt((IPackUtilOverlay overlay) -> positions.getOrDefault(overlay.getOverlayId(), Integer.MAX_VALUE))
            .thenComparingInt(overlay -> focusedId.equals(overlay.getOverlayId()) ? 1 : 0));
        overlays.clear();
        overlays.addAll(ordered);
        restoreFocusedOverlay();
        normalizeOverlayStack();
    }

    private void restoreFocusedOverlay() {
        String focusedId = PackUtilSharedState.get().getFocusedOverlayId();
        if (focusedId.isEmpty()) {
            focusedOverlay = null;
            return;
        }
        focusedOverlay = null;
        for (int i = overlays.size() - 1; i >= 0; i--) {
            IPackUtilOverlay overlay = overlays.get(i);
            if (overlay != null && focusedId.equals(overlay.getOverlayId()) && overlay.isVisible()) {
                focusedOverlay = overlay;
                break;
            }
        }
    }

    private void saveOverlayOrder() {
        List<String> order = new ArrayList<>(overlays.size());
        for (IPackUtilOverlay overlay : overlays) {
            String id = overlay.getOverlayId();
            if (id == null || id.isEmpty() || order.contains(id) || isLauncherOverlay(overlay)) continue;
            order.add(id);
        }
        PackUtilSharedState.get().setOverlayOrder(order);
    }

    private void normalizeOverlayStack() {
        if (overlays.isEmpty()) return;

        List<IPackUtilOverlay> launchers = new ArrayList<>();
        List<IPackUtilOverlay> others = new ArrayList<>();
        for (IPackUtilOverlay overlay : overlays) {
            if (isLauncherOverlay(overlay)) launchers.add(overlay);
            else others.add(overlay);
        }

        if (launchers.isEmpty()) return;

        overlays.clear();
        overlays.addAll(launchers);
        overlays.addAll(others);
    }

    private boolean isLauncherOverlay(IPackUtilOverlay overlay) {
        return overlay instanceof PackUtilLauncherOverlay || (overlay != null && "packutil-launcher".equals(overlay.getOverlayId()));
    }

    public void reclampAllOverlays() {
        for (IPackUtilOverlay overlay : overlays) {
            if (overlay == null) continue;
            overlay.setBounds(overlay.getBounds());
        }
    }

    private IPackUtilOverlay draggingOverlay = null;
    private IPackUtilOverlay resizingOverlay = null;
    private IPackUtilOverlay headerCollapseOverlay = null;
    private int lastScreenWidth = -1;
    private int lastScreenHeight = -1;
    private boolean headerCollapseMoved = false;

    private boolean inventoryMouseDown = false;
    private PackUtilWindowLayout headerCollapseStartBounds = null;
    private PackUtilWindowLayout resizeStartBounds = null;
    private double headerCollapseStartMouseX = 0;
    private double headerCollapseStartMouseY = 0;
    private double resizeStartMouseX = 0;
    private double resizeStartMouseY = 0;

    public void renderAll(GuiGraphics context, int mouseX, int mouseY, float delta) {

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null) {
            int sw = PackUtilUiScale.getVirtualScreenWidth();
            int sh = PackUtilUiScale.getVirtualScreenHeight();
            if (sw != lastScreenWidth || sh != lastScreenHeight) {
                lastScreenWidth = sw;
                lastScreenHeight = sh;
                for (IPackUtilOverlay overlay : overlays) {
                    overlay.setBounds(overlay.getBounds());
                }
            }
        }

        int virtualMouseX = (int) Math.round(PackUtilUiScale.toVirtual(mouseX));
        int virtualMouseY = (int) Math.round(PackUtilUiScale.toVirtual(mouseY));
        int hoveredOverlayIndex = -1;
        for (int i = overlays.size() - 1; i >= 0; i--) {
            IPackUtilOverlay overlay = overlays.get(i);
            if (overlay.isVisible() && overlay.isMouseOver(virtualMouseX, virtualMouseY)) {
                hoveredOverlayIndex = i;
                break;
            }
        }
        PackUiText.beginManagedLayer(context);
        PackUtilUiScale.pushOverlayScale(context);
        try {
            boolean firstRendered = true;
            for (int i = 0; i < overlays.size(); i++) {
                IPackUtilOverlay overlay = overlays.get(i);
                if (overlay.isVisible()) {

                    if (!firstRendered) {
                        context.nextStratum();
                    }
                    firstRendered = false;
                    PackUtilWindowLayout bounds = overlay.getBounds();
                    int renderedHeight = bounds.collapsed ? 16 : bounds.height;
                    PackUiText.addTextOccluder(context, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + renderedHeight);
                    boolean hoverBlocked = hoveredOverlayIndex > i;
                    overlay.render(context,
                        hoverBlocked ? HOVER_BLOCKED_MOUSE : virtualMouseX,
                        hoverBlocked ? HOVER_BLOCKED_MOUSE : virtualMouseY,
                        delta);
                    PackUiText.interOverlayFlush(context);
                }
            }
        } finally {
            PackUiText.endManagedLayer(context);
            PackUtilUiScale.popOverlayScale(context);
        }
    }

    public boolean isMouseOverAnyOverlay(double mouseX, double mouseY) {
        return isMouseOverAnyOverlayVirtual(PackUtilUiScale.toVirtual(mouseX), PackUtilUiScale.toVirtual(mouseY));
    }

    private boolean isMouseOverAnyOverlayVirtual(double mouseX, double mouseY) {
        for (IPackUtilOverlay overlay : overlays) {
            if (overlay.isVisible() && overlay.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldBlockUnderlyingHover(double mouseX, double mouseY) {

        return isMouseOverAnyOverlayVirtual(
            PackUtilUiScale.toVirtual(mouseX),
            PackUtilUiScale.toVirtual(mouseY)
        );
    }

    private void clearFocusedTextFields() {
        for (IPackUtilOverlay overlay : overlays) {
            if (overlay != null && overlay.isVisible() && overlay.hasTextFieldFocused()) {
                overlay.clearTextFieldFocus();
            }
        }
    }

    private IPackUtilOverlay getTopmostOverlayAt(double mouseX, double mouseY) {
        for (int i = overlays.size() - 1; i >= 0; i--) {
            IPackUtilOverlay overlay = overlays.get(i);
            if (overlay.isVisible() && overlay.isMouseOver(mouseX, mouseY)) {
                return overlay;
            }
        }
        return null;
    }

    public boolean isTopOverlay(IPackUtilOverlay overlay) {
        if (overlay == null || overlays.isEmpty()) return false;
        for (int i = overlays.size() - 1; i >= 0; i--) {
            IPackUtilOverlay candidate = overlays.get(i);
            if (candidate != null && candidate.isVisible()) {
                return candidate == overlay;
            }
        }
        return false;
    }

    public boolean isFocusedOverlay(IPackUtilOverlay overlay) {
        return overlay != null && overlay == focusedOverlay;
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        inventoryMouseDown = false;
        mouseX = PackUtilUiScale.toVirtual(mouseX);
        mouseY = PackUtilUiScale.toVirtual(mouseY);
        IPackUtilOverlay topOverlay = getTopmostOverlayAt(mouseX, mouseY);
        if (topOverlay == null) {
            focusedOverlay = null;
            PackUtilSharedState.get().setFocusedOverlayId("");
            headerCollapseOverlay = null;
            headerCollapseMoved = false;
            headerCollapseStartBounds = null;
            inventoryMouseDown = true;
            return false;
        }

        clearFocusedTextFields();
        if (button == 0) {
            if (topOverlay.isOverResizeHandle(mouseX, mouseY)) {
                resizingOverlay = topOverlay;
                resizeStartBounds = topOverlay.getBounds();
                resizeStartMouseX = mouseX;
                resizeStartMouseY = mouseY;
                bringToFront(topOverlay);
                return true;
            }

            if (topOverlay.isOverDragBar(mouseX, mouseY)) {
                draggingOverlay = topOverlay;
                if (topOverlay.usesSharedHeaderClickCollapse()) {
                    headerCollapseOverlay = topOverlay;
                    headerCollapseMoved = false;
                    headerCollapseStartMouseX = mouseX;
                    headerCollapseStartMouseY = mouseY;
                    headerCollapseStartBounds = topOverlay.getBounds();
                } else {
                    headerCollapseOverlay = null;
                    headerCollapseMoved = false;
                    headerCollapseStartBounds = null;
                }
                bringToFront(topOverlay);
                topOverlay.mouseClicked(mouseX, mouseY, button);
                return true;
            }
        }

        headerCollapseOverlay = null;
        headerCollapseMoved = false;
        headerCollapseStartBounds = null;

        bringToFront(topOverlay);
        if (topOverlay.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return true;
    }

    public boolean handleMouseReleased(double mouseX, double mouseY, int button) {
        mouseX = PackUtilUiScale.toVirtual(mouseX);
        mouseY = PackUtilUiScale.toVirtual(mouseY);
        boolean wasDraggingOrResizing = (draggingOverlay != null || resizingOverlay != null);
        IPackUtilOverlay prevDragging = draggingOverlay;
        IPackUtilOverlay prevResizing = resizingOverlay;
        boolean shouldToggleHeaderCollapse = button == 0
            && prevDragging != null
            && prevDragging == headerCollapseOverlay
            && prevDragging.usesSharedHeaderClickCollapse()
            && !headerCollapseMoved;
        PackUtilWindowLayout headerStartBounds = headerCollapseStartBounds;
        if (button == 0) {
            draggingOverlay = null;
            if (resizingOverlay != null) resizingOverlay.saveLayout();
            resizingOverlay = null;
            resizeStartBounds = null;
            headerCollapseOverlay = null;
            headerCollapseMoved = false;
            headerCollapseStartBounds = null;
        }

        if (prevDragging != null) prevDragging.mouseReleased(mouseX, mouseY, button);
        if (prevResizing != null && prevResizing != prevDragging) prevResizing.mouseReleased(mouseX, mouseY, button);

        if (shouldToggleHeaderCollapse && prevDragging != null && prevDragging.isVisible()) {
            if (headerStartBounds != null) {
                PackUtilWindowLayout current = prevDragging.getBounds();
                prevDragging.setBounds(new PackUtilWindowLayout(
                    headerStartBounds.x,
                    headerStartBounds.y,
                    current.width,
                    current.height,
                    current.visible,
                    current.collapsed
                ));
            }
            prevDragging.toggleCollapsed();
            prevDragging.saveLayout();
            return true;
        }

        if (wasDraggingOrResizing) return true;

        if (inventoryMouseDown) {
            inventoryMouseDown = false;
            return false;
        }

        for (int i = overlays.size() - 1; i >= 0; i--) {
            IPackUtilOverlay overlay = overlays.get(i);
            if (overlay.isVisible()) {
                if (overlay.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        return isMouseOverAnyOverlayVirtual(mouseX, mouseY);
    }

    public boolean handleMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        mouseX = PackUtilUiScale.toVirtual(mouseX);
        mouseY = PackUtilUiScale.toVirtual(mouseY);
        deltaX = PackUtilUiScale.toVirtual(deltaX);
        deltaY = PackUtilUiScale.toVirtual(deltaY);
        if (resizingOverlay != null && resizeStartBounds != null && resizingOverlay.isVisible()) {
            PackUtilWindowLayout current = resizingOverlay.getBounds();
            PackUtilWindowLayout resized = new PackUtilWindowLayout(
                resizeStartBounds.x,
                resizeStartBounds.y,
                Math.max(resizingOverlay.getMinWidth(), resizeStartBounds.width + (int) Math.round(mouseX - resizeStartMouseX)),
                Math.max(resizingOverlay.getMinHeight(), resizeStartBounds.height + (int) Math.round(mouseY - resizeStartMouseY)),
                current.visible,
                current.collapsed
            );
            resizingOverlay.setBounds(resized);
            return true;
        }

        if (draggingOverlay != null && draggingOverlay.isVisible()) {
            if (draggingOverlay == headerCollapseOverlay && !headerCollapseMoved) {
                if (Math.abs(mouseX - headerCollapseStartMouseX) >= HEADER_CLICK_DRAG_THRESHOLD
                    || Math.abs(mouseY - headerCollapseStartMouseY) >= HEADER_CLICK_DRAG_THRESHOLD) {
                    headerCollapseMoved = true;
                }
            }
            return draggingOverlay.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        if (inventoryMouseDown) return false;

        for (int i = overlays.size() - 1; i >= 0; i--) {
            IPackUtilOverlay overlay = overlays.get(i);
            if (overlay.isVisible()) {
                if (overlay.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            }
        }

        return isMouseOverAnyOverlayVirtual(mouseX, mouseY);
    }

    public boolean handleMouseScrolled(double mouseX, double mouseY, double amount) {
        mouseX = PackUtilUiScale.toVirtual(mouseX);
        mouseY = PackUtilUiScale.toVirtual(mouseY);
        for (int i = overlays.size() - 1; i >= 0; i--) {
            IPackUtilOverlay overlay = overlays.get(i);
            if (overlay.isVisible() && overlay.isMouseOver(mouseX, mouseY)) {
                bringToFront(overlay);
                if (overlay.mouseScrolled(mouseX, mouseY, amount)) {
                    return true;
                }

                return true;
            }
        }
        return false;
    }

    public boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        for (int i = overlays.size() - 1; i >= 0; i--) {
            IPackUtilOverlay overlay = overlays.get(i);
            if (overlay.isVisible() && overlay.wantsKeyboardCapture()) {
                if (overlay.keyPressed(keyCode, scanCode, modifiers)) {
                    bringToFront(overlay);
                    return true;
                }
            }
        }

        IPackUtilOverlay focusedTextOverlay = getTextFieldFocusOverlay();
        if (focusedTextOverlay != null) {
            focusedTextOverlay.keyPressed(keyCode, scanCode, modifiers);
            focusedOverlay = focusedTextOverlay;
            PackUtilSharedState.get().setFocusedOverlayId(focusedTextOverlay.getOverlayId());
            return true;
        }

        IPackUtilOverlay keyboardTarget = getKeyboardTargetOverlay();
        if (keyboardTarget != null && keyboardTarget.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        return isAnyTextFieldFocused();
    }

    public boolean handleCharTyped(char chr, int modifiers) {
        IPackUtilOverlay focusedTextOverlay = getTextFieldFocusOverlay();
        if (focusedTextOverlay != null) {
            focusedTextOverlay.charTyped(chr, modifiers);
            focusedOverlay = focusedTextOverlay;
            PackUtilSharedState.get().setFocusedOverlayId(focusedTextOverlay.getOverlayId());
            return true;
        }

        IPackUtilOverlay keyboardTarget = getKeyboardTargetOverlay();
        if (keyboardTarget != null && keyboardTarget.charTyped(chr, modifiers)) {
            return true;
        }
        return isAnyTextFieldFocused();
    }

    private IPackUtilOverlay getTextFieldFocusOverlay() {
        for (int i = overlays.size() - 1; i >= 0; i--) {
            IPackUtilOverlay overlay = overlays.get(i);
            if (overlay != null && overlay.isVisible() && overlay.hasTextFieldFocused()) {
                return overlay;
            }
        }
        return null;
    }

    private IPackUtilOverlay getKeyboardTargetOverlay() {
        if (focusedOverlay != null && focusedOverlay.isVisible()) {
            return focusedOverlay;
        }

        for (int i = overlays.size() - 1; i >= 0; i--) {
            IPackUtilOverlay overlay = overlays.get(i);
            if (overlay != null && overlay.isVisible()) {
                return overlay;
            }
        }

        return null;
    }

    public boolean isAnyTextFieldFocused() {
        for (IPackUtilOverlay overlay : overlays) {
            if (overlay.isVisible() && overlay.hasTextFieldFocused()) {
                return true;
            }
        }
        return false;
    }
}
