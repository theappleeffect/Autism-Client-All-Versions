package autismclient.util;

import autismclient.gui.packui.PackUiAssets;
import autismclient.gui.packui.PackUiListRenderer;
import autismclient.gui.packui.PackUiOverlayButton;
import autismclient.gui.packui.PackUiRenderContext;
import autismclient.gui.packui.PackUiScrollbar;
import autismclient.gui.packui.PackUiSmoothScroll;
import autismclient.gui.packui.PackUiTextField;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import autismclient.gui.packui.PackUiViewport;
import autismclient.modules.PackUtilModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PackUtilCustomFilterOverlay extends PackUtilOverlayBase {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int DEFAULT_PANEL_WIDTH = 220;
    private static final int MIN_PANEL_WIDTH = 220;
    private static final int ACTION_HEIGHT = 13;
    private static final int ACTION_GAP = 2;
    private static final int SECTION_GAP = 6;
    private static final int SEARCH_HEIGHT = 16;
    private static final int FILTER_ROW_HEIGHT = 14;
    private static final int FILTER_FOOTER_HEIGHT = 12;
    private static final int FILTER_FOOTER_GAP = 4;
    private static final int FILTER_ROW_STEP = FILTER_ROW_HEIGHT + 1;
    private static final int SCROLLBAR_GUTTER = 8;
    private static final int SCROLLBAR_TRACK_WIDTH = 3;
    private static final int SCROLLBAR_NONE = 0;
    private static final int SCROLLBAR_C2S = 1;
    private static final int SCROLLBAR_S2C = 2;

    private final Font textRenderer;
    private final PackUiTheme theme = new PackUiTheme();
    private final PackUiTextField c2sSearchField;
    private final PackUiTextField s2cSearchField;
    private final PackUtilPacketSelectorOverlay packetSelectorOverlay;
    private final PackUtilCustomFilterPresetOverlay presetManagerOverlay;
    private final List<ActionButton> buttons = new ArrayList<>();
    private final PacketListState c2sListState = new PacketListState();
    private final PacketListState s2cListState = new PacketListState();

    private int panelX = 260;
    private int panelY = 36;
    private int panelWidth = DEFAULT_PANEL_WIDTH;
    private int panelHeight = 326;
    private boolean visible = false;
    private boolean collapsed = false;

    private boolean dragging = false;
    private boolean resizing = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;
    private double resizeStartMouseX = 0;
    private double resizeStartMouseY = 0;
    private int resizeStartWidth = 0;
    private int resizeStartHeight = 0;
    private int activeScrollbarDrag = SCROLLBAR_NONE;
    private int scrollbarGrabOffset = 0;

    public PackUtilCustomFilterOverlay(Font textRenderer) {
        this.textRenderer = textRenderer;
        this.panelWidth = defaultPanelWidth();
        this.panelHeight = defaultPanelHeight();
        this.c2sSearchField = createField("Search C2S packets...");
        this.s2cSearchField = createField("Search S2C packets...");
        this.packetSelectorOverlay = new PackUtilPacketSelectorOverlay(textRenderer);
        this.presetManagerOverlay = new PackUtilCustomFilterPresetOverlay(textRenderer);

        this.c2sSearchField.setOnChange(text -> c2sListState.scroll.jumpTo(0, 0));
        this.s2cSearchField.setOnChange(text -> s2cListState.scroll.jumpTo(0, 0));
    }

    private PackUiTextField createField(String placeholder) {
        PackUiTextField field = new PackUiTextField()
            .setPlaceholder(placeholder)
            .setFieldHeight(searchHeight())
            .setMinWidth(120)
            .setPreferredWidth(120)
            .setTextTone(PackUiTone.BODY)
            .setPlaceholderTone(PackUiTone.MUTED);
        return field;
    }

    public PackUtilCustomFilterPresetOverlay getPresetManagerOverlay() {
        return presetManagerOverlay;
    }

    public PackUtilPacketSelectorOverlay getPacketSelectorOverlay() {
        return packetSelectorOverlay;
    }

    @Override
    public String getOverlayId() {
        return "packutil-custom-filter";
    }

    @Override
    public int getMinWidth() {
        return defaultPanelWidth();
    }

    @Override
    public int getMinHeight() {
        return defaultPanelHeight();
    }

    @Override
    public PackUtilWindowLayout getBounds() {
        return new PackUtilWindowLayout(panelX, panelY, panelWidth, panelHeight, visible, collapsed);
    }

    @Override
    public void setBounds(PackUtilWindowLayout bounds) {
        if (bounds == null) return;
        PackUtilWindowLayout clamped = clampToScreen(this, bounds);
        panelX = clamped.x;
        panelY = clamped.y;
        panelWidth = defaultPanelWidth();
        panelHeight = Math.max(getMinHeight(), clamped.height);
        visible = clamped.visible;
        collapsed = clamped.collapsed;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            PackUtilOverlayManager.get().bringToFront(this);
        } else {
            clearFocus();
            packetSelectorOverlay.close();
        }
        saveLayout();
    }

    public void toggle() {
        setVisible(!visible);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isCollapsed() {
        return collapsed;
    }

    @Override
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        saveLayout();
    }

    @Override
    public boolean usesSharedHeaderClickCollapse() {
        return true;
    }

    @Override
    public boolean isOverDragBar(double mouseX, double mouseY) {
        if (!visible) return false;
        PackUtilWindowLayout bounds = getBounds();
        return mouseX >= panelX && mouseX <= panelX + panelWidth
            && mouseY >= panelY && mouseY <= panelY + HEADER_HEIGHT
            && !isOverWindowControl(mouseX, mouseY, bounds);
    }

    @Override
    public boolean hasTextFieldFocused() {
        return packetSelectorOverlay.isVisible()
            || c2sSearchField.isFocused()
            || s2cSearchField.isFocused();
    }

    @Override
    public void clearTextFieldFocus() {
        clearFocus();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        buttons.clear();
        PackUtilWindowLayout clamped = clampToScreen(this, new PackUtilWindowLayout(panelX, panelY, panelWidth, panelHeight, visible, collapsed));
        panelX = clamped.x;
        panelY = clamped.y;
        panelWidth = clamped.width;
        panelHeight = clamped.height;
        PackUtilWindowLayout bounds = new PackUtilWindowLayout(panelX, panelY, panelWidth, panelHeight, visible, collapsed);
        renderWindowFrame(context, mouseX, mouseY, bounds, "Packet Delay", collapsed, dragging || resizing);
        boolean clipBody = beginWindowBodyClip(context, bounds, collapsed);
        if (!clipBody) {
            renderWindowInactiveOverlay(context, bounds, collapsed, dragging || resizing);
            return;
        }

        try {
            int x = panelX + panelInset();
            int y = panelY + HEADER_HEIGHT + topInset();
            int width = panelWidth - (panelInset() * 2);
            PackUtilModule module = PackUtilModule.get();

            y = drawAction(context, mouseX, mouseY, x, y, width,
                "Custom Packets " + (module.shouldUseCustomPackets() ? "On" : "Off"),
                () -> module.setUseCustomPackets(!module.shouldUseCustomPackets()),
                module.shouldUseCustomPackets() ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.DANGER,
                true);

            PackUtilText.draw(context, textRenderer,
                "C2S " + module.getC2SPackets().size() + " | S2C " + module.getS2CPackets().size(),
                PackUtilText.Tone.MUTED, x, y, false);
            y += infoLineHeight() + actionGap();

            y = drawAction(context, mouseX, mouseY, x, y, width, "Presets", () -> {
                presetManagerOverlay.toggle();
                PackUtilOverlayManager.get().bringToFront(presetManagerOverlay);
            }, PackUiOverlayButton.Variant.SECONDARY, true);
            y += sectionGap() - actionGap();

            int availableHeight = Math.max(sectionMinimumHeight() * 2 + sectionGap(), panelHeight - (y - panelY) - panelInset());
            int sectionHeight = Math.max(sectionMinimumHeight(), (availableHeight - sectionGap()) / 2);

            y = renderPacketSection(context, mouseX, mouseY, delta, x, y, width, sectionHeight, true, module, c2sSearchField, c2sListState);
            renderPacketSection(context, mouseX, mouseY, delta, x, y, width, sectionHeight, false, module, s2cSearchField, s2cListState);
        } finally {
            endWindowBodyClip(context, clipBody);
            renderWindowInactiveOverlay(context, bounds, collapsed, dragging || resizing);
        }

        if (packetSelectorOverlay.isVisible()) {
            packetSelectorOverlay.render(context, mouseX, mouseY, delta);
        }
    }

    private int renderPacketSection(GuiGraphics context, int mouseX, int mouseY, float delta,
                                    int x, int y, int width, int sectionHeight, boolean c2s,
                                    PackUtilModule module, PackUiTextField searchField, PacketListState listState) {
        int sectionStartY = y;
        String label = c2s ? "C2S Packets" : "S2C Packets";
        List<Class<? extends Packet<?>>> packets = new ArrayList<>(c2s ? module.getC2SPackets() : module.getS2CPackets());
        packets.sort(Comparator.comparing(PackUtilPacketNamer::getFriendlyName, String.CASE_INSENSITIVE_ORDER));

        List<Class<? extends Packet<?>>> filteredPackets = filterPackets(packets, searchField.text());
        listState.filteredPackets = filteredPackets;

        PackUiListRenderer.drawHeader(context, textRenderer, label + " (" + packets.size() + ")", x, y);
        y += headerLabelHeight() + 2;

        searchField.setBounds(x, y, width, searchHeight());
        searchField.render(renderContext(context, mouseX, mouseY, delta));
        y += searchHeight() + actionGap();

        y = drawAction(context, mouseX, mouseY, x, y, width, c2s ? "Add C2S" : "Add S2C", () -> {
            if (c2s) {
                packetSelectorOverlay.openToggleC2S((packetClass, selected) -> setPacketSelected(true, packetClass, selected), module.getC2SPackets());
            } else {
                packetSelectorOverlay.openToggleS2C((packetClass, selected) -> setPacketSelected(false, packetClass, selected), module.getS2CPackets());
            }
        }, PackUiOverlayButton.Variant.SECONDARY, true);

        int listHeight = Math.max(listMinimumHeight(), sectionHeight - (y - sectionStartY) - footerHeight() - footerGap());
        int viewHeight = Math.max(1, alignViewportHeight(Math.max(1, listHeight - 4), filterRowStep()));
        int contentHeight = filteredPackets.size() * filterRowStep();
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        int visualScroll = listState.scroll.tick(delta, maxScroll);
        boolean focused = PackUtilOverlayManager.get().isFocusedOverlay(this) || PackUtilOverlayManager.get().isTopOverlay(this);
        PackUiListRenderer.drawFrame(context, x, y, width, listHeight, focused);
        PackUiScrollbar.Metrics scrollbar = computePacketScrollbarMetrics(x, y, width, listHeight, contentHeight, visualScroll);
        int contentWidth = Math.max(24, width - 4 - (scrollbar.hasScroll() ? scrollbarGutter() : 0));
        listState.setBounds(x, y, width, listHeight, scrollbar);

        if (filteredPackets.isEmpty()) {
            PackUiListRenderer.drawEmptyState(context, textRenderer, "No packets", x, y, contentWidth);
        } else {
            PackUiViewport viewport = PackUiViewport.current(1.0f);
            viewport.enableScissor(context, x + 2, y + 2, x + 2 + contentWidth, y + 2 + viewHeight);
            try {
                int scrollPixels = visualScroll;
                int firstVisible = scrollPixels / filterRowStep();
                int rowY = y + 2 - (scrollPixels % filterRowStep());
                for (int i = firstVisible; i < filteredPackets.size() && rowY < y + 2 + viewHeight; i++) {
                    Class<? extends Packet<?>> packetClass = filteredPackets.get(i);
                    if (rowY + filterRowHeight() > y + 2) {
                        boolean hovered = mouseX >= x + 2 && mouseX < x + 2 + contentWidth && mouseY >= rowY && mouseY < rowY + filterRowHeight();
                        String packetName = PackUtilPacketNamer.getFriendlyName(packetClass);
                        PackUiListRenderer.drawRow(
                            context,
                            textRenderer,
                            packetName,
                            x + 2,
                            rowY,
                            contentWidth,
                            filterRowHeight(),
                            hovered,
                            false,
                            c2s ? PackUiListRenderer.RowTone.WARNING : PackUiListRenderer.RowTone.NORMAL
                        );
                        int iconSize = filterRowHeight() - 2;
                        int iconX = x + 2 + contentWidth - iconSize - 1;
                        int iconY = rowY + 1;
                        PackUiListRenderer.drawIconButton(context, iconX, iconY, iconSize, PackUiAssets.ICON_WINDOW_CLOSE, hovered, true);
                        PackUiListRenderer.drawDivider(context, x + 2, rowY + filterRowHeight(), contentWidth);
                        buttons.add(new ActionButton(x + 2, rowY, contentWidth, filterRowHeight(), () -> removePacket(c2s, packetClass), true));
                    }
                    rowY += filterRowStep();
                }
            } finally {
                viewport.disableScissor(context);
            }
        }

        PackUiScrollbar.draw(
            context,
            scrollbar,
            scrollbar.contains(mouseX, mouseY),
            activeScrollbarDrag == (c2s ? SCROLLBAR_C2S : SCROLLBAR_S2C)
        );

        String footer = filteredPackets.isEmpty()
            ? ""
            : (maxScroll > 0
                ? "Scroll: " + (Math.min(filteredPackets.size(), Math.round(listState.scroll.targetOffset() / (float) filterRowStep()) + 1))
                    + "/" + (Math.max(1, Math.round(maxScroll / (float) filterRowStep()) + 1))
                : "Click a row to remove");
        if (!footer.isEmpty()) {
            PackUtilText.draw(context, textRenderer, footer, PackUtilText.Tone.MUTED, x + 4, y + listHeight + footerGap(), false);
        }

        return sectionStartY + sectionHeight + sectionGap();
    }

    private List<Class<? extends Packet<?>>> filterPackets(List<Class<? extends Packet<?>>> packets, String query) {
        if (query == null || query.isBlank()) return packets;

        String lowered = query.toLowerCase();
        List<Class<? extends Packet<?>>> filtered = new ArrayList<>();
        for (Class<? extends Packet<?>> packetClass : packets) {
            String name = PackUtilPacketNamer.getFriendlyName(packetClass).toLowerCase();
            String fullName = packetClass.getName().toLowerCase();
            if (name.contains(lowered) || fullName.contains(lowered)) filtered.add(packetClass);
        }
        return filtered;
    }

    private void addPacket(boolean c2s, Class<? extends Packet<?>> packetClass) {
        PackUtilModule module = PackUtilModule.get();
        Set<Class<? extends Packet<?>>> packets = new LinkedHashSet<>(c2s ? module.getC2SPackets() : module.getS2CPackets());
        if (packets.add(packetClass)) {
            if (c2s) module.setC2SPackets(packets);
            else module.setS2CPackets(packets);
        }
    }

    private void removePacket(boolean c2s, Class<? extends Packet<?>> packetClass) {
        PackUtilModule module = PackUtilModule.get();
        Set<Class<? extends Packet<?>>> packets = new LinkedHashSet<>(c2s ? module.getC2SPackets() : module.getS2CPackets());
        if (packets.remove(packetClass)) {
            if (c2s) module.setC2SPackets(packets);
            else module.setS2CPackets(packets);
        }
    }

    private void setPacketSelected(boolean c2s, Class<? extends Packet<?>> packetClass, boolean selected) {
        if (selected) {
            addPacket(c2s, packetClass);
        } else {
            removePacket(c2s, packetClass);
        }
    }

    private int drawAction(GuiGraphics context, int mouseX, int mouseY, int x, int y, int width, String label, Runnable action,
                           PackUiOverlayButton.Variant variant, boolean enabled) {
        PackUiOverlayButton button = PackUiOverlayButton.create(x, y, width, actionHeight(), Component.literal(label), ignored -> action.run());
        button.setVariant(enabled ? variant : PackUiOverlayButton.Variant.GHOST);
        button.active = enabled;
        PackUiOverlayButton.renderStyled(context, textRenderer, button, mouseX, mouseY);
        buttons.add(new ActionButton(x, y, width, actionHeight(), action, enabled));
        return y + actionHeight() + actionGap();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (packetSelectorOverlay.isVisible()) return packetSelectorOverlay.mouseClicked(mouseX, mouseY, button);

        if (false && button == 0 && !collapsed && isResizeActive(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
            resizing = true;
            resizeStartMouseX = mouseX;
            resizeStartMouseY = mouseY;
            resizeStartWidth = panelWidth;
            resizeStartHeight = panelHeight;
            return true;
        }

        if (button == 0 && mouseX >= panelX && mouseX < panelX + panelWidth && mouseY >= panelY && mouseY < panelY + HEADER_HEIGHT) {
            PackUtilWindowLayout bounds = getBounds();
            if (isOverCloseButton(mouseX, mouseY, bounds)) {
                setVisible(false);
                return true;
            }
            if (isOverCollapseButton(mouseX, mouseY, bounds)) {
                toggleCollapsed();
                return true;
            }
            dragging = true;
            dragOffsetX = mouseX - panelX;
            dragOffsetY = mouseY - panelY;
            clearFocus();
            return true;
        }

        if (collapsed) return false;

        if (button == 0) {
            if (tryStartScrollbarDrag(c2sListState, SCROLLBAR_C2S, mouseX, mouseY)) return true;
            if (tryStartScrollbarDrag(s2cListState, SCROLLBAR_S2C, mouseX, mouseY)) return true;
        }

        if (handleTextFieldClick(c2sSearchField, mouseX, mouseY, button)) {
            c2sListState.scroll.jumpTo(0, 0);
            return true;
        }
        if (handleTextFieldClick(s2cSearchField, mouseX, mouseY, button)) {
            s2cListState.scroll.jumpTo(0, 0);
            return true;
        }

        clearFocus();
        if (button == 0) {
            for (ActionButton actionButton : buttons) {
                if (actionButton.contains(mouseX, mouseY)) {
                    if (!actionButton.enabled()) return true;
                    actionButton.action.run();
                    return true;
                }
            }
        }

        return isMouseOver(mouseX, mouseY);
    }

    private boolean handleTextFieldClick(PackUiTextField field, double mouseX, double mouseY, int button) {
        boolean clicked = field.mouseClicked(inputContext(mouseX, mouseY), (float) mouseX, (float) mouseY, button);
        if (clicked) {
            clearFocus();
            field.setFocused(true);
        }
        return clicked;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (packetSelectorOverlay.isVisible() && packetSelectorOverlay.mouseReleased(mouseX, mouseY, button)) return true;
        if (c2sSearchField.mouseReleased(inputContext(mouseX, mouseY), (float) mouseX, (float) mouseY, button)) return true;
        if (s2cSearchField.mouseReleased(inputContext(mouseX, mouseY), (float) mouseX, (float) mouseY, button)) return true;
        if (button == 0 && activeScrollbarDrag != SCROLLBAR_NONE) {
            activeScrollbarDrag = SCROLLBAR_NONE;
            return true;
        }
        if (button == 0) {
            if (dragging || resizing) saveLayout();
            dragging = false;
            resizing = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (packetSelectorOverlay.isVisible() && packetSelectorOverlay.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        if (c2sSearchField.mouseDragged(inputContext(mouseX, mouseY), (float) mouseX, (float) mouseY, button, (float) deltaX, (float) deltaY)) return true;
        if (s2cSearchField.mouseDragged(inputContext(mouseX, mouseY), (float) mouseX, (float) mouseY, button, (float) deltaX, (float) deltaY)) return true;
        if (activeScrollbarDrag == SCROLLBAR_C2S) {
            c2sListState.scroll.setFromThumbStepped(c2sListState.scrollbarMetrics, mouseY, scrollbarGrabOffset, filterRowStep());
            return true;
        }
        if (activeScrollbarDrag == SCROLLBAR_S2C) {
            s2cListState.scroll.setFromThumbStepped(s2cListState.scrollbarMetrics, mouseY, scrollbarGrabOffset, filterRowStep());
            return true;
        }
        if (resizing && button == 0) {
            PackUtilWindowLayout nextBounds = clampToScreen(this,
                new PackUtilWindowLayout(panelX, panelY,
                    resizeStartWidth + (int) Math.round(mouseX - resizeStartMouseX),
                    resizeStartHeight + (int) Math.round(mouseY - resizeStartMouseY),
                    visible, collapsed));
            panelX = nextBounds.x;
            panelY = nextBounds.y;
            panelWidth = nextBounds.width;
            panelHeight = nextBounds.height;
            return true;
        }
        if (dragging && button == 0) {
            PackUtilWindowLayout nextBounds = clampToScreen(this,
                new PackUtilWindowLayout((int) (mouseX - dragOffsetX), (int) (mouseY - dragOffsetY),
                    panelWidth, panelHeight, visible, collapsed));
            panelX = nextBounds.x;
            panelY = nextBounds.y;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (packetSelectorOverlay.isVisible()) return packetSelectorOverlay.mouseScrolled(mouseX, mouseY, amount);
        if (c2sListState.contains(mouseX, mouseY)) {
            int maxScroll = Math.max(0, (c2sListState.filteredPackets.size() * filterRowStep()) - alignViewportHeight(Math.max(1, c2sListState.height - 4), filterRowStep()));
            c2sListState.scroll.nudge(amount, filterRowStep(), maxScroll);
            return true;
        }
        if (s2cListState.contains(mouseX, mouseY)) {
            int maxScroll = Math.max(0, (s2cListState.filteredPackets.size() * filterRowStep()) - alignViewportHeight(Math.max(1, s2cListState.height - 4), filterRowStep()));
            s2cListState.scroll.nudge(amount, filterRowStep(), maxScroll);
            return true;
        }
        return false;
    }

    private PackUiScrollbar.Metrics computePacketScrollbarMetrics(int x, int y, int width, int listHeight, int contentHeight, int scrollOffset) {
        int viewHeight = Math.max(1, alignViewportHeight(Math.max(1, listHeight - 4), filterRowStep()));
        return PackUiScrollbar.compute(
            contentHeight,
            viewHeight,
            x + width - 5,
            y + 2,
            scrollbarTrackWidth(),
            Math.max(1, listHeight - 4),
            scrollOffset
        );
    }

    private boolean tryStartScrollbarDrag(PacketListState listState, int dragId, double mouseX, double mouseY) {
        PackUiScrollbar.Metrics metrics = listState.scrollbarMetrics;
        if (metrics == null || !metrics.hasScroll()) return false;
        if (!metrics.contains(mouseX, mouseY)) {
            return false;
        }

        activeScrollbarDrag = dragId;
        scrollbarGrabOffset = Math.max(0, (int) mouseY - metrics.thumbY());
        listState.scroll.setFromThumbStepped(metrics, mouseY, scrollbarGrabOffset, filterRowStep());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (packetSelectorOverlay.isVisible()) return packetSelectorOverlay.keyPressed(keyCode, scanCode, modifiers);
        if (!visible) return false;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            clearFocus();
            return false;
        }

        PackUiTextField focusedField = getFocusedField();
        if (focusedField == null) return false;
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) return true;

        focusedField.keyPressed(inputContext(0, 0), keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (packetSelectorOverlay.isVisible()) return packetSelectorOverlay.charTyped(chr, modifiers);
        if (!visible) return false;

        PackUiTextField focusedField = getFocusedField();
        if (focusedField == null) return false;
        return focusedField.charTyped(inputContext(0, 0), chr, modifiers);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible) return false;
        if (packetSelectorOverlay.isVisible()) return true;

        PackUtilWindowLayout bounds = getBounds();
        int frameHeight = collapsed ? HEADER_HEIGHT : panelHeight;
        if (mouseX >= bounds.x && mouseX <= bounds.x + bounds.width && mouseY >= bounds.y && mouseY <= bounds.y + frameHeight) return true;
        return isMouseOverField(c2sSearchField, mouseX, mouseY)
            || isMouseOverField(s2cSearchField, mouseX, mouseY);
    }

    private PackUiTextField getFocusedField() {
        if (c2sSearchField.isFocused()) return c2sSearchField;
        if (s2cSearchField.isFocused()) return s2cSearchField;
        return null;
    }

        private int defaultPanelWidth() {
        return 220;
    }

    private int defaultPanelHeight() {
        return 308;
    }

    private int panelInset() {
        return 10;
    }

    private int topInset() {
        return 6;
    }

    private int actionHeight() {
        return 13;
    }

    private int actionGap() {
        return 2;
    }

    private int sectionGap() {
        return 6;
    }

    private int searchHeight() {
        return 16;
    }

    private int filterRowHeight() {
        return 14;
    }

    private int filterRowStep() {
        return filterRowHeight() + 1;
    }

    private int footerHeight() {
        return 12;
    }

    private int footerGap() {
        return 4;
    }

    private int scrollbarGutter() {
        return 8;
    }

    private int scrollbarTrackWidth() {
        return 3;
    }

    private int infoLineHeight() {
        return 10;
    }

    private int headerLabelHeight() {
        return 10;
    }

    private int sectionMinimumHeight() {
        return 86;
    }

    private int listMinimumHeight() {
        return 40;
    }

    private void clearFocus() {
        c2sSearchField.setFocused(false);
        s2cSearchField.setFocused(false);
    }

    private PackUiRenderContext renderContext(GuiGraphics context, int mouseX, int mouseY, float delta) {
        PackUiViewport viewport = PackUiViewport.current(1.0f);
        return new PackUiRenderContext(context, textRenderer, viewport, theme, viewport.toUiX(mouseX), viewport.toUiY(mouseY), delta);
    }

    private PackUiRenderContext inputContext(double mouseX, double mouseY) {
        PackUiViewport viewport = PackUiViewport.current(1.0f);
        return new PackUiRenderContext(null, textRenderer, viewport, theme, viewport.toUiX(mouseX), viewport.toUiY(mouseY), 0.0f);
    }

    private boolean isMouseOverField(PackUiTextField field, double mouseX, double mouseY) {
        return mouseX >= field.x() && mouseX <= field.x() + field.width()
            && mouseY >= field.y() && mouseY <= field.y() + field.height();
    }

    private record ActionButton(int x, int y, int width, int height, Runnable action, boolean enabled) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private static final class PacketListState {
        private int x;
        private int y;
        private int width;
        private int height;
        private final PackUiSmoothScroll scroll = new PackUiSmoothScroll();
        private PackUiScrollbar.Metrics scrollbarMetrics = null;
        private List<Class<? extends Packet<?>>> filteredPackets = List.of();

        private void setBounds(int x, int y, int width, int height, PackUiScrollbar.Metrics scrollbarMetrics) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.scrollbarMetrics = scrollbarMetrics;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
