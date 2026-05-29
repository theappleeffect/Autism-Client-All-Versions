package autismclient.util;

import autismclient.gui.packui.PackUiAssets;
import autismclient.gui.packui.PackUiHeaderControls;
import autismclient.gui.packui.PackUiInsets;
import autismclient.gui.packui.PackUiLabel;
import autismclient.gui.packui.PackUiListRenderer;
import autismclient.gui.packui.PackUiRenderContext;
import autismclient.gui.packui.PackUiScrollViewport;
import autismclient.gui.packui.PackUiSurface;
import autismclient.gui.packui.PackUiScrollbar;
import autismclient.gui.packui.PackUiSmoothScroll;
import autismclient.gui.packui.PackUiSizing;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTextField;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import autismclient.gui.packui.PackUiViewport;
import autismclient.gui.packui.PackUiViewportSlot;
import autismclient.gui.packui.PackUiWindowNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.Packet;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PackUtilPacketSelectorOverlay extends PackUtilOverlayBase {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int MIN_PANEL_WIDTH = 230;
    private static final int ROW_HEIGHT = 16;
    private static final int MAX_VISIBLE_ROWS = 12;
    private static final int PAD = 6;
    private static final int VIEWPORT_BORDER = 1;
    private static final int HEADER_CONTROL = 12;
    private static final int HEADER_ARROW_WIDTH = 10;
    private static final int HEADER_ARROW_GAP = 3;
    private static final float HEADER_CLICK_DRAG_THRESHOLD = 3.0f;
    private static final int PACKET_LIST_SCROLLBAR_WIDTH = 6;

    private final Font textRenderer;
    private final PackUiTheme theme = new PackUiTheme();
    private final PackUiWindowNode windowNode = new PackUiWindowNode("Select Packet");
    private final PackUiSurface surface = new PackUiSurface(theme, windowNode);
    private final PackUiTextField searchField = new PackUiTextField();
    private final PackUiLabel summaryLabel = new PackUiLabel("", PackUiTone.MUTED).setTrimToBounds(true);
    private final PackUiViewportSlot listSlot = new PackUiViewportSlot();
    private PackUiScrollViewport listViewport = null;

    private final List<Class<? extends Packet<?>>> allPackets = new ArrayList<>();
    private List<Class<? extends Packet<?>>> activePool = List.of();
    private List<Class<? extends Packet<?>>> filteredPackets = List.of();
    private Set<Class<? extends Packet<?>>> excludedPackets = Set.of();
    private Set<Class<? extends Packet<?>>> selectedPackets = Set.of();

    private boolean visible = false;
    private boolean collapsed = false;
    private boolean dragging = false;
    private boolean dragMoved = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private float pressStartUiX;
    private float pressStartUiY;
    private int pressStartPanelX;
    private int pressStartPanelY;
    private float closeHover = 0.0f;
    private float closeVisibility = 1.0f;
    private int panelX;
    private int panelY;
    private int panelWidth = MIN_PANEL_WIDTH;
    private int panelHeight = 0;
    private final PackUiSmoothScroll listScroll = new PackUiSmoothScroll();
    private int contentHeight = 0;
    private boolean scrollbarDragging = false;
    private int scrollbarGrabOffset = 0;

    private Consumer<Class<? extends Packet<?>>> onSelect;
    private BiConsumer<Class<? extends Packet<?>>, Boolean> onToggleSelect;
    private boolean closeOnSelect = true;
    private boolean toggleMode = false;

    public PackUtilPacketSelectorOverlay(Font textRenderer) {
        this.textRenderer = textRenderer;
        this.allPackets.addAll(PackUtilPacketRegistry.getC2SPackets());
        this.allPackets.addAll(PackUtilPacketRegistry.getS2CPackets());
        this.allPackets.sort(Comparator.comparing(PackUtilPacketNamer::getFriendlyName, String.CASE_INSENSITIVE_ORDER));
        this.activePool = List.copyOf(allPackets);
        this.filteredPackets = List.copyOf(allPackets);
        buildUi();
    }

    private void buildUi() {
        panelWidth = panelMinimumWidth();
        windowNode.setCenterTitle(false);
        windowNode.setTitleTone(PackUiTone.LABEL);
        windowNode.setTitleAreaInsets(panelPadding() + 2, panelPadding() + headerControlSize() + headerArrowWidth() + headerArrowGap() + 12);
        windowNode.content().setGap(windowContentGap()).setPadding(PackUiInsets.all(panelPadding()));

        searchField
            .setPlaceholder("Search packets...")
            .setFieldHeight(searchFieldHeight())
            .setGrowX(true)
            .setOnChange(text -> {
                updateFilter(text);
                listScroll.jumpTo(0, 0);
            });

        rebuildUi();
    }

    private void rebuildUi() {
        windowNode.content().clearChildren();
        windowNode.setTitle(toggleMode ? "Packet Selector [Toggle]" : "Packet Selector");
        windowNode.content().add(searchField);
        listSlot.setPreferredHeight(computeViewportHeight(filteredPackets.size()));
        windowNode.content().add(listSlot);
        summaryLabel.setText(toggleMode
            ? filteredPackets.size() + " packets | Selected " + selectedPackets.size()
            : filteredPackets.size() + " packets");
        windowNode.content().add(summaryLabel);
        contentHeight = filteredPackets.size() * rowHeight();
    }

    private int computeViewportHeight(int itemCount) {
        int visibleRows = Math.max(5, Math.min(maxVisibleRows(), Math.max(1, itemCount)));
        return visibleRows * rowHeight() + (VIEWPORT_BORDER * 2);
    }

    public void open(Consumer<Class<? extends Packet<?>>> onSelect) {
        openWith(onSelect, allPackets, Set.of(), true);
    }

    public void openC2S(Consumer<Class<? extends Packet<?>>> onSelect) {
        openC2S(onSelect, Set.of(), true);
    }

    public void openC2S(Consumer<Class<? extends Packet<?>>> onSelect,
                        Collection<Class<? extends Packet<?>>> excludedPackets,
                        boolean closeOnSelect) {
        List<Class<? extends Packet<?>>> c2s = new ArrayList<>(PackUtilPacketRegistry.getC2SPackets());
        c2s.sort(Comparator.comparing(PackUtilPacketNamer::getFriendlyName, String.CASE_INSENSITIVE_ORDER));
        openWith(onSelect, c2s, excludedPackets, closeOnSelect);
    }

    public void openS2C(Consumer<Class<? extends Packet<?>>> onSelect) {
        openS2C(onSelect, Set.of(), true);
    }

    public void openS2C(Consumer<Class<? extends Packet<?>>> onSelect,
                        Collection<Class<? extends Packet<?>>> excludedPackets,
                        boolean closeOnSelect) {
        List<Class<? extends Packet<?>>> s2c = new ArrayList<>(PackUtilPacketRegistry.getS2CPackets());
        s2c.sort(Comparator.comparing(PackUtilPacketNamer::getFriendlyName, String.CASE_INSENSITIVE_ORDER));
        openWith(onSelect, s2c, excludedPackets, closeOnSelect);
    }

    public void openToggleC2S(BiConsumer<Class<? extends Packet<?>>, Boolean> onToggleSelect,
                              Collection<Class<? extends Packet<?>>> selectedPackets) {
        List<Class<? extends Packet<?>>> c2s = new ArrayList<>(PackUtilPacketRegistry.getC2SPackets());
        c2s.sort(Comparator.comparing(PackUtilPacketNamer::getFriendlyName, String.CASE_INSENSITIVE_ORDER));
        openToggleWith(onToggleSelect, c2s, selectedPackets);
    }

    public void openToggleS2C(BiConsumer<Class<? extends Packet<?>>, Boolean> onToggleSelect,
                              Collection<Class<? extends Packet<?>>> selectedPackets) {
        List<Class<? extends Packet<?>>> s2c = new ArrayList<>(PackUtilPacketRegistry.getS2CPackets());
        s2c.sort(Comparator.comparing(PackUtilPacketNamer::getFriendlyName, String.CASE_INSENSITIVE_ORDER));
        openToggleWith(onToggleSelect, s2c, selectedPackets);
    }

    private void openWith(Consumer<Class<? extends Packet<?>>> onSelect,
                          List<Class<? extends Packet<?>>> pool,
                          Collection<Class<? extends Packet<?>>> excludedPackets,
                          boolean closeOnSelect) {
        this.visible = true;
        this.collapsed = false;
        this.onSelect = onSelect;
        this.onToggleSelect = null;
        this.closeOnSelect = closeOnSelect;
        this.toggleMode = false;
        this.excludedPackets = excludedPackets == null ? Set.of() : new LinkedHashSet<>(excludedPackets);
        this.selectedPackets = Set.of();
        this.activePool = filterExcluded(pool);
        updateFilter("");
        finishOpen();
    }

    private void openToggleWith(BiConsumer<Class<? extends Packet<?>>, Boolean> onToggleSelect,
                                List<Class<? extends Packet<?>>> pool,
                                Collection<Class<? extends Packet<?>>> selectedPackets) {
        this.visible = true;
        this.collapsed = false;
        this.onSelect = null;
        this.onToggleSelect = onToggleSelect;
        this.closeOnSelect = false;
        this.toggleMode = true;
        this.excludedPackets = Set.of();
        this.selectedPackets = selectedPackets == null ? new LinkedHashSet<>() : new LinkedHashSet<>(selectedPackets);
        this.activePool = new ArrayList<>(pool);
        updateFilter("");
        finishOpen();
    }

    private void finishOpen() {
        listScroll.jumpTo(0, 0);
        rebuildUi();
        PackUiRenderContext metrics = surface.measurementContext();
        if (metrics != null) {
            panelHeight = Math.round(windowNode.preferredHeight(metrics, panelWidth));
        }
        PackUiViewport viewport = surface.viewport();
        panelX = Math.max(8, Math.round((viewport.uiWidth() - panelWidth) / 2.0f));
        panelY = Math.max(8, Math.round((viewport.uiHeight() - panelHeight) / 2.0f));
        PackUtilWindowLayout clamped = clampToViewport(new PackUtilWindowLayout(panelX, panelY, panelWidth, panelHeight, visible, collapsed));
        panelX = clamped.x;
        panelY = clamped.y;
        panelWidth = clamped.width;
        panelHeight = clamped.height;
        searchField.setText("");
        searchField.setFocused(true);
        windowNode.syncShowBody(true);
        PackUtilOverlayManager.get().bringToFrontParent(this);
    }

    public void close() {
        visible = false;
        collapsed = false;
        dragging = false;
        dragMoved = false;
        onSelect = null;
        onToggleSelect = null;
        excludedPackets = Set.of();
        selectedPackets = Set.of();
        closeOnSelect = true;
        toggleMode = false;
        surface.clearFocusedTextInputs();
        windowNode.syncShowBody(true);
    }

    public boolean isVisible() {
        return visible;
    }

    private void updateFilter(String query) {
        String filter = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (filter.isEmpty()) {
            filteredPackets = new ArrayList<>(filterExcluded(activePool));
        } else {
            filteredPackets = filterExcluded(activePool).stream()
                .filter(packetClass -> packetClass.getSimpleName().toLowerCase(Locale.ROOT).contains(filter)
                    || PackUtilPacketNamer.getFriendlyName(packetClass).toLowerCase(Locale.ROOT).contains(filter))
                .collect(Collectors.toList());
        }
        contentHeight = filteredPackets.size() * rowHeight();
        rebuildUi();
    }

    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!visible || MC == null || MC.font == null) return;

        rebuildUi();

        PackUiViewport viewport = surface.viewport();
        float uiMouseX = viewport.toUiX(mouseX);
        float uiMouseY = viewport.toUiY(mouseY);
        boolean headerHovered = isOverHeaderUi(uiMouseX, uiMouseY);

        PackUiRenderContext metrics = new PackUiRenderContext(context, MC.font, viewport, theme, uiMouseX, uiMouseY, delta);
        windowNode.setShowBody(!collapsed);
        windowNode.setActive(true);
        windowNode.setHeaderHovered(headerHovered);

        panelHeight = Math.round(windowNode.preferredHeight(metrics, panelWidth));
        panelHeight = Math.max(theme.headerHeight(), panelHeight);
        windowNode.setBounds(panelX, panelY, panelWidth, panelHeight);

        surface.render(context, mouseX, mouseY, delta);
        context.nextStratum();
        PackUiText.interOverlayFlush(context);
        if (!collapsed) {
            renderlistViewport(context, viewport, uiMouseX, uiMouseY, delta);
        }
        renderHeaderControls(context, viewport, uiMouseX, uiMouseY, delta);
    }

    private void renderlistViewport(GuiGraphics context, PackUiViewport viewport, float uiMouseX, float uiMouseY, float delta) {
        int viewX = Math.round(listSlot.x());
        int viewY = Math.round(listSlot.y());
        int viewW = Math.round(listSlot.width());
        int viewH = Math.round(listSlot.height());
        if (viewW <= 2 || viewH <= 2) return;

        int contentHeight = filteredPackets.size() * rowHeight();

        if (listViewport == null || listViewport.getX() != viewX || listViewport.getY() != viewY
            || listViewport.getWidth() != viewW || listViewport.getHeight() != viewH) {
            listViewport = new PackUiScrollViewport(viewX, viewY, viewW, viewH, rowHeight(), PACKET_LIST_SCROLLBAR_WIDTH);
        }
        listViewport.setContentHeight(contentHeight);

        listViewport.beginRender(context, theme.borderSoft(), theme.listFill());
        try {
            listViewport.renderSimple(context, filteredPackets.size(), (idx, bnd) -> {
                Class<? extends Packet<?>> packetClass = filteredPackets.get(idx);
                renderPacketRowSimple(context, packetClass, bnd.x, bnd.y, bnd.width, idx);
            });
        } finally {
            listViewport.endRender(context);
        }

        renderScrollbar(context, viewX, viewY, viewW, viewH, uiMouseX, uiMouseY);
    }

    private void renderPacketRowSimple(GuiGraphics context, Class<? extends Packet<?>> packetClass, int x, int y, int width, int index) {
        boolean c2s = PackUtilPacketRegistry.getC2SPackets().contains(packetClass);
        boolean selected = toggleMode && selectedPackets.contains(packetClass);
        int rowColor = selected ? PackUtilColors.packetRowSelectedBg(false) : PackUtilColors.packetRowBg(c2s, index, false);
        int color = selected ? PackUtilColors.packetRowSelectedText() : PackUtilColors.packetRowText(c2s, index);
        PackUiText.fill(context, x, y, x + width, y + rowHeight(), rowColor);

        String name = PackUtilPacketNamer.getFriendlyName(packetClass);
        int textWidth = width - 10;
        String trimmed = PackUiText.trimToWidth(textRenderer, name, textWidth, theme.fontFor(PackUiTone.BODY), color);
        int textY = PackUiSizing.alignTextY(y, rowHeight(), theme.fontHeight(PackUiTone.BODY), theme.bodyTextNudge());
        PackUiText.draw(context, textRenderer, trimmed, theme.fontFor(PackUiTone.BODY), color, x + 6, textY, false);
        PackUiText.fill(context, x + 4, y + rowHeight() - 1, x + width - 4, y + rowHeight(), PackUtilColors.packetRowDivider());
    }

    private void renderPacketRow(GuiGraphics context, PackUiRenderContext rowContext, Class<? extends Packet<?>> packetClass, int x, int y, int width, int index) {
        boolean c2s = PackUtilPacketRegistry.getC2SPackets().contains(packetClass);
        boolean hovered = uiContains(x, y, width, rowHeight(), rowContext.mouseX(), rowContext.mouseY());
        boolean selected = toggleMode && selectedPackets.contains(packetClass);
        int rowColor = selected
            ? PackUtilColors.packetRowSelectedBg(hovered)
            : PackUtilColors.packetRowBg(c2s, index, hovered);
        int color = selected
            ? PackUtilColors.packetRowSelectedText()
            : PackUtilColors.packetRowText(c2s, index);
        PackUiText.fill(context, x + 2, y, x + width - 2, y + rowHeight(), rowColor);

        String name = PackUtilPacketNamer.getFriendlyName(packetClass);
        String trimmed = PackUiText.trimToWidth(textRenderer, name, width - markerReserveWidth(), theme.fontFor(PackUiTone.BODY), color);
        int textY = PackUiSizing.alignTextY(y, rowHeight(), theme.fontHeight(PackUiTone.BODY), theme.bodyTextNudge());
        PackUiText.draw(context, textRenderer, trimmed, theme.fontFor(PackUiTone.BODY), color, x + 6, textY, false);
        PackUiText.fill(context, x + 4, y + rowHeight() - 1, x + width - 4, y + rowHeight(), PackUtilColors.packetRowDivider());
    }

    private void renderScrollbar(GuiGraphics context, int viewX, int viewY, int viewW, int viewH, float uiMouseX, float uiMouseY) {
        if (listViewport != null) {
            listViewport.renderScrollbar(context, uiMouseX, uiMouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        PackUiViewport viewport = surface.viewport();
        float uiMouseX = viewport.toUiX(mouseX);
        float uiMouseY = viewport.toUiY(mouseY);

        if (button == 0 && isOverCloseButton(uiMouseX, uiMouseY) && closeVisibility > 0.01f) {
            close();
            return true;
        }

        if (button == 0 && isOverHeaderUi(uiMouseX, uiMouseY)) {
            dragging = true;
            dragMoved = false;
            dragOffsetX = uiMouseX - panelX;
            dragOffsetY = uiMouseY - panelY;
            pressStartUiX = uiMouseX;
            pressStartUiY = uiMouseY;
            pressStartPanelX = panelX;
            pressStartPanelY = panelY;
            return true;
        }

        if (!collapsed && surface.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (!collapsed && button == 0 && uiContains(listSlot.x(), listSlot.y(), listSlot.width(), listSlot.height(), uiMouseX, uiMouseY)) {
            if (listViewport != null && listViewport.mouseClicked(uiMouseX, uiMouseY, button)) {
                return true;
            }
        }

        if (!collapsed && button == 0 && uiContains(listSlot.x(), listSlot.y(), listSlot.width(), listSlot.height(), uiMouseX, uiMouseY)) {
            int index = (int) ((uiMouseY - listSlot.y() + listViewport.getScrollOffset()) / rowHeight());
            if (index >= 0 && index < filteredPackets.size()) {
                Class<? extends Packet<?>> selectedPacket = filteredPackets.get(index);
                if (toggleMode) {
                    selectedPackets = new LinkedHashSet<>(selectedPackets);
                    boolean nowSelected;
                    if (selectedPackets.contains(selectedPacket)) {
                        selectedPackets.remove(selectedPacket);
                        nowSelected = false;
                    } else {
                        selectedPackets.add(selectedPacket);
                        nowSelected = true;
                    }
                    if (onToggleSelect != null) onToggleSelect.accept(selectedPacket, nowSelected);
                    rebuildUi();
                    searchField.setFocused(true);
                } else {
                    if (onSelect != null) onSelect.accept(selectedPacket);
                    if (closeOnSelect) {
                        close();
                    } else {
                        excludedPackets = new LinkedHashSet<>(excludedPackets);
                        excludedPackets.add(selectedPacket);
                        activePool = filterExcluded(activePool);
                        updateFilter(searchField.text());
                        searchField.setFocused(true);
                    }
                }
                return true;
            }
        }

        if (!uiContains(panelX, panelY, panelWidth, panelHeight, uiMouseX, uiMouseY)) {
            close();
            return true;
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        PackUiViewport viewport = surface.viewport();
        float uiMouseX = viewport.toUiX(mouseX);
        float uiMouseY = viewport.toUiY(mouseY);

        if (button == 0 && dragging) {
            boolean moved = dragMoved
                || Math.abs(uiMouseX - pressStartUiX) >= HEADER_CLICK_DRAG_THRESHOLD
                || Math.abs(uiMouseY - pressStartUiY) >= HEADER_CLICK_DRAG_THRESHOLD
                || panelX != pressStartPanelX
                || panelY != pressStartPanelY;
            dragging = false;
            dragMoved = false;
            if (!moved && isOverHeaderUi(uiMouseX, uiMouseY) && !isOverCloseButton(uiMouseX, uiMouseY)) {
                collapsed = !collapsed;
                if (collapsed) surface.clearFocusedTextInputs();
                windowNode.setShowBody(!collapsed);
            }
            return true;
        }

        if (button == 0 && listViewport != null) {
            listViewport.mouseReleased();
        }

        if (!collapsed && surface.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return visible && uiContains(panelX, panelY, panelWidth, panelHeight, uiMouseX, uiMouseY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!visible) return false;

        PackUiViewport viewport = surface.viewport();
        float uiMouseX = viewport.toUiX(mouseX);
        float uiMouseY = viewport.toUiY(mouseY);

        if (dragging && button == 0) {
            int nextX = Math.round(uiMouseX - dragOffsetX);
            int nextY = Math.round(uiMouseY - dragOffsetY);
            if (nextX != panelX || nextY != panelY) dragMoved = true;
            PackUtilWindowLayout clamped = clampToViewport(new PackUtilWindowLayout(nextX, nextY, panelWidth, panelHeight, visible, collapsed));
            panelX = clamped.x;
            panelY = clamped.y;
            return true;
        }

        if (!collapsed && uiContains(listSlot.x(), listSlot.y(), listSlot.width(), listSlot.height(), uiMouseX, uiMouseY)) {
            if (listViewport != null && listViewport.isScrollbarDragging()) {
                listViewport.mouseDragged(uiMouseX, uiMouseY);
                return true;
            }
        }

        if (!collapsed && surface.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        return visible && uiContains(panelX, panelY, panelWidth, panelHeight, uiMouseX, uiMouseY);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (searchField.isFocused()) {
                searchField.setFocused(false);
                return true;
            }
            close();
            return true;
        }

        surface.keyPressed(keyCode, scanCode, modifiers);
        if (searchField.isFocused()) return true;
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        return visible && surface.charTyped(chr, modifiers);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible) return false;

        PackUiViewport viewport = surface.viewport();
        float uiMouseX = viewport.toUiX(mouseX);
        float uiMouseY = viewport.toUiY(mouseY);

        if (!collapsed && uiContains(listSlot.x(), listSlot.y(), listSlot.width(), listSlot.height(), uiMouseX, uiMouseY)) {
            if (listViewport != null) {
                listViewport.mouseScrolled(uiMouseX, uiMouseY, amount);
                return true;
            }
        }

        if (searchField.isFocused()) return false;

        return uiContains(panelX, panelY, panelWidth, panelHeight, uiMouseX, uiMouseY);
    }

    private List<Class<? extends Packet<?>>> filterExcluded(List<Class<? extends Packet<?>>> packets) {
        if (excludedPackets.isEmpty()) return new ArrayList<>(packets);
        return packets.stream()
            .filter(packetClass -> !excludedPackets.contains(packetClass))
            .collect(Collectors.toList());
    }

    public boolean hasTextFieldFocused() {
        return visible && surface.hasFocusedTextInput();
    }

    private float animate(float current, float target, float delta) {
        return PackUiHeaderControls.animate(current, target, delta);
    }

    private void renderHeaderControls(GuiGraphics context, PackUiViewport viewport, float uiMouseX, float uiMouseY, float delta) {
        closeVisibility = animate(closeVisibility, 1.0f, delta);
        closeHover = animate(closeHover, isOverCloseButton(uiMouseX, uiMouseY) ? 1.0f : 0.0f, delta);
        viewport.push(context);
        try {
            int arrowX = collapseArrowX();
            int closeX = closeButtonX();
            int controlY = controlButtonY();
            drawCollapseArrow(context, arrowX, controlY);
            drawCloseButton(context, closeX, controlY, headerControlSize(), headerControlSize(), closeHover, closeVisibility);
        } finally {
            viewport.pop(context);
        }
    }

    private void drawCollapseArrow(GuiGraphics context, int x, int y) {
        PackUiHeaderControls.drawAnimatedArrow(context, x, y + 1, headerArrowWidth(), collapsed ? 0.0f : 1.0f, 1.0f);
    }

    private void drawCloseButton(GuiGraphics context, int x, int y, int width, int height, float hover, float visibility) {
        PackUiHeaderControls.drawCloseButton(context, x, y, width, height, hover, true, visibility);
    }

    private boolean isOverHeaderUi(float uiMouseX, float uiMouseY) {
        return uiMouseX >= panelX && uiMouseX < panelX + panelWidth
            && uiMouseY >= panelY && uiMouseY < panelY + theme.headerHeight();
    }

    private boolean isOverCloseButton(float uiMouseX, float uiMouseY) {
        return PackUiHeaderControls.isCloseHit(closeVisibility, uiMouseX, uiMouseY, closeButtonX(), controlButtonY(), headerControlSize());
    }

    private int controlButtonY() {
        return PackUiHeaderControls.controlY(panelY, theme.headerHeight(), headerControlSize());
    }

    private int closeButtonX() {
        return PackUiHeaderControls.closeX(panelX, panelWidth, headerControlSize(), 2);
    }

    private int collapseArrowX() {
        return PackUiHeaderControls.expandedArrowX(closeButtonX(), headerArrowGap(), headerArrowWidth());
    }

    private PackUtilWindowLayout clampToViewport(PackUtilWindowLayout bounds) {
        PackUiViewport viewport = surface.viewport();
        int width = Math.max(panelMinimumWidth(), Math.min(bounds.width, Math.round(viewport.uiWidth())));
        int minHeight = bounds.collapsed ? theme.headerHeight() : theme.headerHeight() + 20;
        int height = Math.max(minHeight, Math.min(bounds.height, Math.round(viewport.uiHeight())));
        int x = Math.max(0, Math.min(bounds.x, Math.max(0, Math.round(viewport.uiWidth()) - width)));
        int y = Math.max(0, Math.min(bounds.y, Math.max(0, Math.round(viewport.uiHeight()) - theme.headerHeight())));
        return new PackUtilWindowLayout(x, y, width, height, bounds.visible, bounds.collapsed);
    }

    private boolean uiContains(float x, float y, float width, float height, float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

        private int panelMinimumWidth() {
        return 230;
    }

    private int rowHeight() {
        return 16;
    }

    private int maxVisibleRows() {
        return 12;
    }

    private int panelPadding() {
        return 6;
    }

    private int windowContentGap() {
        return 4;
    }

    private int searchFieldHeight() {
        return 16;
    }

    private int markerReserveWidth() {
        return 10;
    }

    private int headerControlSize() {
        return 12;
    }

    private int headerArrowWidth() {
        return 10;
    }

    private int headerArrowGap() {
        return 3;
    }
}
