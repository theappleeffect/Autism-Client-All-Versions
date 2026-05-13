package autismclient.util;

import autismclient.gui.packui.PackUiAssets;
import autismclient.gui.packui.PackUiControlGlyphs;
import autismclient.gui.packui.PackUiLegacyLayout;
import autismclient.gui.packui.PackUiListRenderer;
import autismclient.gui.packui.PackUiOverlayButton;
import autismclient.gui.packui.PackUiScrollbar;
import autismclient.gui.packui.PackUiScrollList;
import autismclient.gui.packui.PackUiScrollViewport;
import autismclient.gui.packui.PackUiSmoothScroll;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import autismclient.modules.PackUtilModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PackUtilLANSyncOverlay extends PackUtilOverlayBase {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final PackUiTheme PACKUI_THEME = new PackUiTheme();
    private static PackUtilLANSyncOverlay sharedOverlay;
    private static final int CONTENT_SCROLLBAR_WIDTH = 4;
    private static final int CONTENT_SCROLLBAR_GUTTER = 12;
    private static final int SAME_MACRO_LIST_MAX_VISIBLE_ROWS = 6;
    private static final int SAME_MACRO_LIST_SCROLLBAR_WIDTH = 3;
    private int PANEL_BASE_WIDTH = 292;

    private int panelX = 500;
    private int panelY = 5;
    private int PANEL_WIDTH = PANEL_BASE_WIDTH;
    private int PANEL_HEIGHT = 220;
    private static final int HEADER_HEIGHT = 16;
    private int TAB_HEIGHT = 16;
    private int BUTTON_HEIGHT = 16;
    private int ROW_HEIGHT = 14;
    private int CONTENT_PADDING = 8;
    private int CHAT_FIELD_HEIGHT = 16;
    private int CHAT_AREA_HEIGHT = CHAT_FIELD_HEIGHT + 10;

    private boolean isDragging = false;
    private boolean isResizing = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;
    private double resizeStartMouseX = 0;
    private double resizeStartMouseY = 0;
    private int resizeStartWidth = 0;
    private int resizeStartHeight = 0;
    private boolean collapsed = false;
    private boolean visible = false;
    private final Font textRenderer;

    private static final String[] TAB_NAMES = {"Info", "Peers", "Macros", "Queue"};
    private int activeTab = 0;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int renderScrollOffset = 0;
    private boolean scrollbarDragging = false;
    private int scrollbarGrabOffset = 0;
    private final PackUiSmoothScroll contentScrollState = new PackUiSmoothScroll();

    private PackUiScrollList<PackUtilMacro> sameMacroList = null;

    private int sameMacroListX = 0;
    private int sameMacroListY = 0;
    private int sameMacroListWidth = 0;
    private int sameMacroListHeight = 0;

    private PackUiScrollViewport perUserlistViewport = null;

    private int perUserListX = 0;
    private int perUserListY = 0;
    private int perUserListWidth = 0;
    private int perUserListHeight = 0;

    private int tickCounter = 0;
    private static final int REFRESH_INTERVAL = 10;

    private String selectedMacroName = null;

    private boolean perUserMode = false;
    private final java.util.Map<String, String> perUserAssignments = new java.util.LinkedHashMap<>();

    private String expandedExecutePeer = null;

    private String selectedPeer = null;

    private final List<ClickRegion> clickRegions = new ArrayList<>();
    private PackUtilChatField lanChatField;

    private static class ClickRegion {
        final int x, y, width, height;
        final Runnable action;

        ClickRegion(int x, int y, int width, int height, Runnable action) {
            this.x = x; this.y = y; this.width = width; this.height = height;
            this.action = action;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + width && my >= y && my < y + height;
        }
    }

    private static class MacroSyncStatus {
        final List<String> missingPeers;
        final Map<String, Integer> differentPeers;

        MacroSyncStatus(List<String> missingPeers, Map<String, Integer> differentPeers) {
            this.missingPeers = missingPeers;
            this.differentPeers = differentPeers;
        }

        boolean canExecute() {
            return missingPeers.isEmpty();
        }
    }

    private static class AssignmentSyncStatus {
        final Map<String, String> assignments;
        final Map<String, String> missingAssignments;

        AssignmentSyncStatus(Map<String, String> assignments, Map<String, String> missingAssignments) {
            this.assignments = assignments;
            this.missingAssignments = missingAssignments;
        }

        boolean hasAssignments() {
            return !assignments.isEmpty();
        }

        boolean canExecute() {
            return hasAssignments() && missingAssignments.isEmpty();
        }
    }

    public PackUtilLANSyncOverlay(Font textRenderer) {
        this.textRenderer = textRenderer;
        applyPresetMetrics();
        this.PANEL_WIDTH = PANEL_BASE_WIDTH;
        this.PANEL_HEIGHT = defaultPanelHeight();

        PackUtilLANSync sync = PackUtilLANSync.getInstance();
        sync.setOnClientJoined(() -> {});
        sync.setOnClientLeft(() -> {});
        sync.setOnSyncStateChanged(() -> {});
        sync.setOnSpreadCalculated(() -> {});
        sync.setOnPeerStatusChanged(() -> {});
    }

    public static synchronized PackUtilLANSyncOverlay getSharedOverlay(Font textRenderer) {
        if (sharedOverlay == null) {
            sharedOverlay = new PackUtilLANSyncOverlay(textRenderer);
            sharedOverlay.restoreState();
        }
        return sharedOverlay;
    }

    @Override
    public int getMinWidth() {
        return PANEL_BASE_WIDTH;
    }

    @Override
    public int getMinHeight() {
        return minimumPanelHeight();
    }

    @Override
    public PackUtilWindowLayout getBounds() {
        return new PackUtilWindowLayout(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, visible, collapsed);
    }

    @Override
    public void setBounds(PackUtilWindowLayout bounds) {
        if (bounds == null) return;
        PackUtilWindowLayout clamped = clampToScreen(this, bounds);
        panelX = clamped.x;
        panelY = clamped.y;
        PANEL_WIDTH = PANEL_BASE_WIDTH;
        PANEL_HEIGHT = Math.max(getMinHeight(), clamped.height);
        visible = clamped.visible;
        collapsed = clamped.collapsed;
    }

    public void toggle() { setVisible(!visible); }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            PackUtilLANSync sync = PackUtilLANSync.getInstance();
            PackUtilModule module = PackUtilModule.get();
            if (module.isActive() && module.isLANSyncEnabled()) {
                if (!sync.isRunning()) sync.start();
            }
            PackUtilOverlayManager.get().bringToFront(this);
        } else if (lanChatField != null) {
            lanChatField.setFocused(false);
        }
        saveState();
    }

    @Override public boolean isVisible() { return visible; }
    @Override public boolean isCollapsed() { return collapsed; }
    @Override public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        saveState();
    }

    @Override
    public boolean usesSharedHeaderClickCollapse() {
        return true;
    }

    @Override
    public boolean hasTextFieldFocused() {
        return lanChatField != null && lanChatField.isFocused();
    }

    @Override
    public void clearTextFieldFocus() {
        if (lanChatField != null && lanChatField.isFocused()) {
            lanChatField.setFocused(false);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible) return false;
        int h = collapsed ? HEADER_HEIGHT : getPanelHeight();
        return mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH && mouseY >= panelY && mouseY <= panelY + h;
    }

    @Override
    public boolean isOverDragBar(double mouseX, double mouseY) {
        if (!visible) return false;
        PackUtilWindowLayout bounds = getBounds();
        return mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH
            && mouseY >= panelY && mouseY <= panelY + HEADER_HEIGHT
            && !isOverWindowControl(mouseX, mouseY, bounds);
    }

    private int getPanelHeight() {
        PackUtilLANSync sync = PackUtilLANSync.getInstance();
        if (!sync.isRunning()) {

            return HEADER_HEIGHT + 40;
        }
        if (!sync.isInSession()) {
            if (sync.isSearching()) {

                return HEADER_HEIGHT + 60;
            }

            return HEADER_HEIGHT + 70;
        }

        int contentH = estimateTabContentHeight(sync);
        int total = HEADER_HEIGHT + TAB_HEIGHT + 2 + contentH + CHAT_AREA_HEIGHT + 8;

        return Math.max(140, Math.min(total, 500));
    }

    private int estimateTabContentHeight(PackUtilLANSync sync) {
        switch (activeTab) {
            case 0: {
                int h = 4 + 12 * 4 + 4 + 14;
                if (sync.getLastSpreadResult() != null) h += 12;
                if (!sync.getSpreadHistory().isEmpty()) h += 14;
                h += 4 + BUTTON_HEIGHT + 4;
                return h;
            }
            case 1: {
                int peerCount = sync.getConnectedCount();
                int h = 4 + peerCount * (ROW_HEIGHT + 1);
                if (selectedPeer != null) h += 64;
                return Math.max(h + 4, 60);
            }
            case 2: {
                int h = 4 + BUTTON_HEIGHT + 6;
                if (perUserMode) {
                    int listContentHeight = estimatePerUserListContentHeight(sync);
                    h += 14 + cappedlistViewportHeight(listContentHeight);
                    h += 12 + 6 + BUTTON_HEIGHT + 6;
                } else {
                    int macros = PackUtilMacroManager.get().getAll().size();
                    h += 14 + (macros == 0 ? 12 : macrolistViewportHeight(macros)) + 8;

                    if (selectedMacroName != null) h += 4 * 12;
                    h += BUTTON_HEIGHT + 6;
                }
                if (autismclient.util.macro.MacroExecutor.isRunning()) h += BUTTON_HEIGHT + 6;
                h += 1 + 6 + 14 + 14;
                return h;
            }
            case 3: {
                return 4 + 12 + 12 + 16 + BUTTON_HEIGHT + 8 + BUTTON_HEIGHT + 4;
            }
            default: return 120;
        }
    }

    public void saveState() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        shared.setLanSyncOverlayVisible(visible);
        shared.setLanSyncOverlayX(panelX);
        shared.setLanSyncOverlayY(panelY);
        shared.setLanSyncOverlayActiveTab(activeTab);
        shared.setLanSyncOverlayScrollOffset(scrollOffset);
        shared.setLanSyncOverlayPerUserMode(perUserMode);
        shared.setLanSyncOverlaySelectedMacroName(selectedMacroName);
        shared.setLanSyncOverlayExpandedExecutePeer(expandedExecutePeer);
        shared.setLanSyncOverlaySelectedPeer(selectedPeer);
        saveLayout();
    }

    public void restoreState() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        restoreLayout();
        this.visible = shared.isLanSyncOverlayVisible();
        this.panelX = shared.getLanSyncOverlayX();
        this.panelY = shared.getLanSyncOverlayY();
        this.activeTab = Math.max(0, Math.min(TAB_NAMES.length - 1, shared.getLanSyncOverlayActiveTab()));
        this.scrollOffset = Math.max(0, shared.getLanSyncOverlayScrollOffset());
        this.contentScrollState.restore(this.scrollOffset);
        this.perUserMode = shared.isLanSyncOverlayPerUserMode();
        this.selectedMacroName = emptyToNull(shared.getLanSyncOverlaySelectedMacroName());
        this.expandedExecutePeer = emptyToNull(shared.getLanSyncOverlayExpandedExecutePeer());
        this.selectedPeer = emptyToNull(shared.getLanSyncOverlaySelectedPeer());
        this.PANEL_WIDTH = PANEL_BASE_WIDTH;
    }

    private void clampScrollOffset() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private PackUiScrollbar.Metrics getContentScrollbarMetrics(int contentY, int contentHeight) {
        int totalContentHeight = maxScroll + contentHeight;
        return PackUiScrollbar.compute(
            totalContentHeight,
            contentHeight,
            panelX + PANEL_WIDTH - 6,
            contentY,
            CONTENT_SCROLLBAR_WIDTH,
            contentHeight,
            contentScrollState.tick(0.0f, Math.max(0, totalContentHeight - contentHeight))
        );
    }

    private boolean hasScrollableSessionContent(PackUtilLANSync sync) {
        return sync.isRunning() && sync.isInSession() && !sync.isSearching();
    }

    private int getTabContentY() {
        return panelY + HEADER_HEIGHT + TAB_HEIGHT + 2;
    }

    private int getTabContentHeight(int panelHeight) {
        int chatTopY = panelY + panelHeight - CHAT_AREA_HEIGHT;
        return Math.max(40, chatTopY - 4 - getTabContentY());
    }

    private int getContentWidth() {
        return PackUiLegacyLayout.contentWidth(PANEL_WIDTH, CONTENT_PADDING);
    }

    private int getListWidth(boolean hasScroll) {
        return PackUiLegacyLayout.reserveScrollbar(getContentWidth(), hasScroll, CONTENT_SCROLLBAR_GUTTER);
    }

    private int sameMacroListRowPitch() {
        return ROW_HEIGHT + 1;
    }

    private int macrolistViewportHeight(int macroCount) {
        int visibleRows = Math.max(1, Math.min(SAME_MACRO_LIST_MAX_VISIBLE_ROWS, macroCount));

        return Math.max(sameMacroListRowPitch(), visibleRows * sameMacroListRowPitch());
    }

    private int macroListContentHeight(int macroCount) {
        if (macroCount <= 0) return 0;

        return macroCount * sameMacroListRowPitch();
    }

    private int cappedlistViewportHeight(int contentHeight) {

        int maxVisibleRows = Math.max(1, SAME_MACRO_LIST_MAX_VISIBLE_ROWS);
        int maxHeight = maxVisibleRows * sameMacroListRowPitch();

        int clampedHeight = Math.max(sameMacroListRowPitch(), Math.min(contentHeight, maxHeight));
        return ((clampedHeight + sameMacroListRowPitch() - 1) / sameMacroListRowPitch()) * sameMacroListRowPitch();
    }

    private void clearSameMacrolistViewport() {
        sameMacroList = null;
        sameMacroListX = 0;
        sameMacroListY = 0;
        sameMacroListWidth = 0;
        sameMacroListHeight = 0;
    }

    private boolean hasSameMacrolistViewport() {
        return sameMacroList != null && sameMacroListWidth > 0 && sameMacroListHeight > 0;
    }

    private boolean isOverSameMacroList(double mouseX, double mouseY) {
        return hasSameMacrolistViewport()
            && mouseX >= sameMacroListX && mouseX < sameMacroListX + sameMacroListWidth
            && mouseY >= sameMacroListY && mouseY < sameMacroListY + sameMacroListHeight;
    }

    private void clearperUserlistViewport() {
        perUserlistViewport = null;
        perUserListX = 0;
        perUserListY = 0;
        perUserListWidth = 0;
        perUserListHeight = 0;
    }

    private boolean hasperUserlistViewport() {
        return perUserlistViewport != null && perUserListWidth > 0 && perUserListHeight > 0;
    }

    private boolean isOverPerUserList(double mouseX, double mouseY) {
        return hasperUserlistViewport()
            && mouseX >= perUserListX && mouseX < perUserListX + perUserListWidth
            && mouseY >= perUserListY && mouseY < perUserListY + perUserListHeight;
    }

    private void syncPerUserAssignments(PackUtilLANSync sync) {
        java.util.Set<String> connectedPeers = sync.getConnectedClients().keySet();
        for (String peer : connectedPeers) {
            perUserAssignments.putIfAbsent(peer, "");
        }
        perUserAssignments.keySet().retainAll(connectedPeers);

        Map<String, String> synced = sync.getSyncedAssignments();
        for (Map.Entry<String, String> se : synced.entrySet()) {
            if (connectedPeers.contains(se.getKey())) {
                perUserAssignments.put(se.getKey(), se.getValue());
            }
        }
    }

    private int estimatePerUserListContentHeight(PackUtilLANSync sync) {
        syncPerUserAssignments(sync);
        int contentHeight = 0;
        for (Map.Entry<String, String> entry : new ArrayList<>(perUserAssignments.entrySet())) {
            String peer = entry.getKey();
            contentHeight += sameMacroListRowPitch();
            if (peer.equals(expandedExecutePeer)) {
                contentHeight += estimateExpandedPeerPickerHeight(sync, peer) + 4;
            }
        }
        return Math.max(sameMacroListRowPitch(), contentHeight);
    }

    private int estimateExpandedPeerPickerHeight(PackUtilLANSync sync, String peer) {
        int contentHeight = 0;
        List<PackUtilMacro> myMacroList = PackUtilMacroManager.get().getAll();
        java.util.Map<String, Integer> myMacroMap = new java.util.LinkedHashMap<>();
        for (PackUtilMacro macro : myMacroList) {
            myMacroMap.put(macro.name, macro.actions != null ? macro.actions.size() : 0);
        }

        if (peer.equals(sync.getMyUsername())) {
            return myMacroList.size() * sameMacroListRowPitch();
        }

        Map<String, Map<String, PackUtilMacro>> allRemote = sync.getAllRemoteMacros();
        Map<String, PackUtilMacro> peerMacroData = allRemote.getOrDefault(peer, java.util.Collections.emptyMap());
        java.util.Map<String, Integer> peerMacroMap = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, PackUtilMacro> entry : peerMacroData.entrySet()) {
            peerMacroMap.put(entry.getKey(), entry.getValue().actions != null ? entry.getValue().actions.size() : 0);
        }

        List<String> shared = new ArrayList<>();
        java.util.Map<String, Integer> different = new java.util.LinkedHashMap<>();
        List<String> peerOnly = new ArrayList<>();
        List<String> mineOnly = new ArrayList<>();

        for (String name : peerMacroMap.keySet()) {
            if (myMacroMap.containsKey(name)) {
                int diffs = countActionDifferences(PackUtilMacroManager.get().get(name), peerMacroData.get(name));
                if (diffs == 0) {
                    shared.add(name);
                } else {
                    different.put(name, diffs);
                }
            } else {
                peerOnly.add(name);
            }
        }
        for (String name : myMacroMap.keySet()) {
            if (!peerMacroMap.containsKey(name)) {
                mineOnly.add(name);
            }
        }

        int rowPitch = sameMacroListRowPitch();
        if (!shared.isEmpty()) {
            contentHeight += shared.size() * rowPitch;
        }
        contentHeight += different.size() * rowPitch;
        if (!peerOnly.isEmpty()) {
            contentHeight += peerOnly.size() * rowPitch;
        }
        if (!mineOnly.isEmpty()) {
            contentHeight += mineOnly.size() * rowPitch;
        }
        if (shared.isEmpty() && different.isEmpty() && peerOnly.isEmpty() && mineOnly.isEmpty()) {
            contentHeight += rowPitch;
        }

        return Math.max(rowPitch, contentHeight);
    }

    private void drawUiText(GuiGraphicsExtractor ctx, String text, PackUiTone tone, int color, int x, int y) {
        PackUiText.draw(ctx, textRenderer, text, PACKUI_THEME.fontFor(tone), color, x, y, false);
    }

    private void drawOverlayButton(GuiGraphicsExtractor ctx, int x, int y, int w, int h, String label, PackUiOverlayButton.Variant variant, boolean enabled, int mx, int my) {
        drawOverlayButton(ctx, x, y, w, h, label, variant, enabled, PackUiTone.BODY, mx, my);
    }

    private void drawOverlayButton(GuiGraphicsExtractor ctx, int x, int y, int w, int h, String label, PackUiOverlayButton.Variant variant, boolean enabled, PackUiTone tone, int mx, int my) {
        PackUiOverlayButton button = PackUiOverlayButton.create(x, y, w, h, Component.literal(label), ignored -> {});
        button.setWidth(w);
        button.setVariant(variant);
        button.setTone(tone);
        button.active = enabled;
        PackUiOverlayButton.renderStyled(ctx, textRenderer, button, mx, my);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        PackUtilLANSync sync = PackUtilLANSync.getInstance();
        PackUtilModule module = PackUtilModule.get();

        tickCounter++;
        if (tickCounter >= REFRESH_INTERVAL) {
            tickCounter = 0;
            if (!sync.isRunning() && module.isActive() && module.isLANSyncEnabled()) {
                sync.start();
            }
        }

        clickRegions.clear();
        int panelHeight = getPanelHeight();
        PackUtilWindowLayout bounds = new PackUtilWindowLayout(panelX, panelY, PANEL_WIDTH, panelHeight, visible, collapsed);
        renderWindowFrame(ctx, mouseX, mouseY, bounds, "LAN Sync", collapsed, isDragging);
        boolean clipBody = beginWindowBodyClip(ctx, bounds, collapsed);
        if (!clipBody) {
            renderWindowInactiveOverlay(ctx, bounds, collapsed, isDragging);
            return;
        }

        try {
            ensureLanChatField(panelHeight);

        int contentY = panelY + HEADER_HEIGHT;
        int chatTopY = panelY + panelHeight - CHAT_AREA_HEIGHT;

        if (!sync.isRunning()) {
            renderNotRunning(ctx, mouseX, mouseY, contentY);
        } else if (!sync.isInSession() && !sync.isSearching()) {
            renderNoSession(ctx, mouseX, mouseY, contentY);
        } else if (sync.isSearching()) {
            renderSearching(ctx, mouseX, mouseY, contentY);
        } else {

            renderTabBar(ctx, mouseX, mouseY, contentY);
            int tabContentY = getTabContentY();
            int actualTabContentH = getTabContentHeight(panelHeight);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            contentScrollState.setTarget(scrollOffset, maxScroll);
            renderScrollOffset = contentScrollState.tick(delta, maxScroll);
            maxScroll = 0;

        PackUtilUiScale.enableOverlayScissor(ctx, panelX, tabContentY, panelX + PANEL_WIDTH, tabContentY + actualTabContentH);
            switch (activeTab) {
                case 0: renderDashboard(ctx, mouseX, mouseY, tabContentY, actualTabContentH); break;
                case 1: renderPeers(ctx, mouseX, mouseY, tabContentY, actualTabContentH); break;
                case 2: renderExecute(ctx, mouseX, mouseY, tabContentY, actualTabContentH); break;
                case 3: renderQueue(ctx, mouseX, mouseY, tabContentY, actualTabContentH); break;
            }
            ctx.disableScissor();
            PackUiScrollbar.Metrics scrollbarMetrics = getContentScrollbarMetrics(tabContentY, actualTabContentH);
            PackUiScrollbar.draw(ctx, scrollbarMetrics, scrollbarMetrics.contains(mouseX, mouseY), scrollbarDragging);
        }

            if (sync.isRunning() && sync.isInSession()) {
                renderLanChatField(ctx, mouseX, mouseY);
            }
        } finally {
            endWindowBodyClip(ctx, clipBody);
            renderWindowInactiveOverlay(ctx, bounds, collapsed, isDragging);
        }
    }

    private void renderNotRunning(GuiGraphicsExtractor ctx, int mx, int my, int y) {
        int lx = panelX + CONTENT_PADDING;
        drawUiText(ctx, "Not running", PackUiTone.LABEL, 0xFFFF5555, lx, y + 8);
        drawUiText(ctx, "LAN Sync is currently stopped", PackUiTone.MUTED, PackUtilColors.textSecondary(), lx, y + 22);
    }

    private void renderNoSession(GuiGraphicsExtractor ctx, int mx, int my, int y) {
        int lx = panelX + CONTENT_PADDING;
        drawUiText(ctx, "Quick Start", PackUiTone.LABEL, PACKUI_THEME.color(PackUiTone.BODY), lx, y + 6);
        drawUiText(ctx, "Host creates, others search & join", PackUiTone.MUTED, PackUtilColors.textSecondary(), lx, y + 20);

        int btnY = y + 40;
        int halfW = (PANEL_WIDTH - 24) / 2;

        drawOverlayButton(ctx, panelX + 8, btnY, halfW, BUTTON_HEIGHT, "Create", PackUiOverlayButton.Variant.SECONDARY, true, mx, my);
        clickRegions.add(new ClickRegion(panelX + 8, btnY, halfW, BUTTON_HEIGHT, () -> {
            PackUtilLANSync.getInstance().createSession();
        }));

        drawOverlayButton(ctx, panelX + 12 + halfW, btnY, halfW, BUTTON_HEIGHT, "Search", PackUiOverlayButton.Variant.SECONDARY, true, mx, my);
        clickRegions.add(new ClickRegion(panelX + 12 + halfW, btnY, halfW, BUTTON_HEIGHT, () -> {
            PackUtilLANSync.getInstance().startSearching();
        }));
    }

    private void renderSearching(GuiGraphicsExtractor ctx, int mx, int my, int y) {
        int secondsLeft = PackUtilLANSync.getInstance().getSearchSecondsRemaining();
        String text = "Searching... " + secondsLeft + "s";
        int tw = PackUiText.width(textRenderer, text, PACKUI_THEME.fontFor(PackUiTone.LABEL), 0xFFFFAA00);
        drawUiText(ctx, text, PackUiTone.LABEL, 0xFFFFAA00, panelX + (PANEL_WIDTH - tw) / 2, y + 10);

        int btnY = y + 35;
        drawOverlayButton(ctx, panelX + 30, btnY, PANEL_WIDTH - 60, BUTTON_HEIGHT, "Cancel Search", PackUiOverlayButton.Variant.SECONDARY, true, mx, my);
        clickRegions.add(new ClickRegion(panelX + 30, btnY, PANEL_WIDTH - 60, BUTTON_HEIGHT, () -> {
            PackUtilLANSync.getInstance().cancelSearch();
        }));
    }

    private void ensureLanChatField(int panelHeight) {
        if (lanChatField == null) {
            lanChatField = new PackUtilChatField(MC, textRenderer, panelX + CONTENT_PADDING, panelY, PANEL_WIDTH - (CONTENT_PADDING * 2), CHAT_FIELD_HEIGHT, true);
            lanChatField.setHistoryNavigationEnabled(true);
            lanChatField.setSubmitHandler(message -> PackUtilLANSync.getInstance().sendChatMessage(message));
        }

        int fieldX = panelX + CONTENT_PADDING;
        int fieldY = panelY + panelHeight - CHAT_FIELD_HEIGHT - 6;
        int fieldWidth = PANEL_WIDTH - (CONTENT_PADDING * 2);
        lanChatField.setX(fieldX);
        lanChatField.setY(fieldY);
        lanChatField.setWidth(fieldWidth);

        boolean enabled = PackUtilLANSync.getInstance().isRunning() && PackUtilLANSync.getInstance().isInSession();
        lanChatField.setEditable(enabled);
        lanChatField.setSubmitOnEnter(enabled);
        lanChatField.setPlaceholder(Component.literal(enabled ? "Sync message to all peers..." : "Join or create a session first"));
        if (!enabled) {
            lanChatField.setFocused(false);
        }
    }

    private void renderLanChatField(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        if (lanChatField == null) return;

        int labelY = lanChatField.getY() - 10;
        PackUtilText.draw(ctx, textRenderer, "Sync Send", PackUtilText.Tone.MUTED, lanChatField.getX(), labelY, false);
        lanChatField.render(ctx, mouseX, mouseY, 0.0f);
    }

    private void renderTabBar(GuiGraphicsExtractor ctx, int mx, int my, int y) {
        int tabW = PANEL_WIDTH / TAB_NAMES.length;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int tx = panelX + (i * tabW);
            if (i == activeTab) {
                drawOverlayButton(ctx, tx, y, tabW, TAB_HEIGHT, TAB_NAMES[i], PackUiOverlayButton.Variant.PRIMARY, true, PackUiTone.LABEL, mx, my);
            } else {
                drawOverlayButton(ctx, tx, y, tabW, TAB_HEIGHT, TAB_NAMES[i], PackUiOverlayButton.Variant.SECONDARY, true, PackUiTone.LABEL, mx, my);
            }
            final int tabIdx = i;
            clickRegions.add(new ClickRegion(tx, y, tabW, TAB_HEIGHT, () -> {
                activeTab = tabIdx;
                scrollOffset = 0;
                contentScrollState.jumpTo(0, 0);
                clearSameMacrolistViewport();
                clearperUserlistViewport();
                saveState();
            }));
        }
    }

    private void renderDashboard(GuiGraphicsExtractor ctx, int mx, int my, int y, int h) {
        PackUtilLANSync sync = PackUtilLANSync.getInstance();
        int lx = panelX + CONTENT_PADDING;
        int cy = y + 4;

        drawUiText(ctx, "Session", PackUiTone.MUTED, PackUtilColors.textSecondary(), lx, cy);
        drawUiText(ctx, sync.getSessionId(), PackUiTone.BODY, 0xFFFFFFFF, lx + 50, cy);
        cy += 12;

        String role = sync.isHost() ? "Host" : "Client";
        int roleColor = sync.isHost() ? 0xFFFFAA00 : 0xFF55FF55;
        drawUiText(ctx, "Role", PackUiTone.MUTED, PackUtilColors.textSecondary(), lx, cy);
        drawUiText(ctx, role, PackUiTone.BODY, roleColor, lx + 50, cy);
        cy += 12;

        drawUiText(ctx, "Peers", PackUiTone.MUTED, PackUtilColors.textSecondary(), lx, cy);
        drawUiText(ctx, String.valueOf(sync.getConnectedCount()), PackUiTone.BODY, 0xFFFFFFFF, lx + 50, cy);
        cy += 12;

        if (sync.isReconnecting()) {
            drawUiText(ctx, "Reconnecting...", PackUiTone.LABEL, 0xFFFFAA00, lx, cy);
            cy += 12;
        }

        cy += 4;
        PackUtilLANSync.SyncState state = sync.getSyncState();
        String stateStr = state.name();
        int stateColor = getSyncStateColor(state);
        drawUiText(ctx, "Sync", PackUiTone.MUTED, PackUtilColors.textSecondary(), lx, cy);
        drawUiText(ctx, stateStr, PackUiTone.BODY, stateColor, lx + 50, cy);
        cy += 14;

        PackUtilLANSync.SpreadResult spread = sync.getLastSpreadResult();
        if (spread != null) {
            String spreadStr = String.format("%.1fms", spread.getSpreadMs());
            int spreadColor = getSpreadColor(spread.getSpreadMs());
            drawUiText(ctx, "Spread", PackUiTone.MUTED, PackUtilColors.textSecondary(), lx, cy);
            drawUiText(ctx, spreadStr, PackUiTone.BODY, spreadColor, lx + 50, cy);
            if (spread.lateClient != null && !spread.lateClient.isEmpty() && spread.getSpreadMs() > 0) {
                drawUiText(ctx, " (" + spread.lateClient + ")", PackUiTone.MUTED, PackUtilColors.textDim(),
                    lx + 50 + PackUiText.width(textRenderer, spreadStr, PACKUI_THEME.fontFor(PackUiTone.BODY), spreadColor), cy);
            }
            cy += 12;
        }

        List<PackUtilLANSync.SpreadResult> history = sync.getSpreadHistory();
        if (!history.isEmpty()) {
            drawUiText(ctx, "History", PackUiTone.MUTED, PackUtilColors.textSecondary(), lx, cy);
            int dotX = lx + 52;
            for (PackUtilLANSync.SpreadResult sr : history) {
                int dotColor = getSpreadColor(sr.getSpreadMs());
                PackUiText.fill(ctx, dotX, cy + 2, dotX + 6, cy + 8, dotColor);
                dotX += 9;
            }
            cy += 14;
        }

        int btnY = y + h - BUTTON_HEIGHT - 4;
        drawOverlayButton(ctx, panelX + CONTENT_PADDING, btnY, PANEL_WIDTH - CONTENT_PADDING * 2, BUTTON_HEIGHT, "Leave Session", PackUiOverlayButton.Variant.SECONDARY, true, mx, my);
        clickRegions.add(new ClickRegion(panelX + CONTENT_PADDING, btnY, PANEL_WIDTH - CONTENT_PADDING * 2, BUTTON_HEIGHT, () -> {
            PackUtilLANSync.getInstance().leaveSession();
        }));
    }

    private void renderPeers(GuiGraphicsExtractor ctx, int mx, int my, int y, int h) {
        PackUtilLANSync sync = PackUtilLANSync.getInstance();
        int lx = panelX + CONTENT_PADDING;
        int cy = y + 4;
        int rowWidth = getContentWidth() + 4;

        Map<String, PackUtilLANSync.ClientInfo> clients = sync.getConnectedClients();
        List<PackUtilLANSync.ClientInfo> sortedClients = new ArrayList<>(clients.values());
        sortedClients.sort((a, b) -> Boolean.compare(b.isHost, a.isHost));

        for (PackUtilLANSync.ClientInfo client : sortedClients) {
            boolean isSelected = client.username.equals(selectedPeer);
            boolean isHov = isHovered(mx, my, lx - 2, cy, PANEL_WIDTH - CONTENT_PADDING * 2 + 4, ROW_HEIGHT);
            PackUiListRenderer.drawRow(
                ctx,
                textRenderer,
                "",
                lx - 2,
                cy,
                rowWidth,
                ROW_HEIGHT,
                isHov,
                isSelected,
                PackUiListRenderer.RowTone.NORMAL
            );

            PackUiControlGlyphs.drawChevron(
                ctx,
                lx + 1,
                cy + 3,
                8,
                isSelected ? PackUiControlGlyphs.ChevronDirection.DOWN : PackUiControlGlyphs.ChevronDirection.RIGHT,
                isSelected ? 0xFFF6EFEF : 0xFFE2D4D4,
                0xB8381418,
                1.0f
            );

            PackUtilLANSync.PeerStatus status = sync.getPeerStatus(client.username);
            int dotColor = status == PackUtilLANSync.PeerStatus.HEALTHY ? 0xFF55FF55
                : status == PackUtilLANSync.PeerStatus.STALE ? 0xFFFFAA00 : 0xFF888888;
            PackUiText.fill(ctx, lx + 10, cy + 4, lx + 15, cy + 9, dotColor);

            boolean isSelf = client.username.equals(sync.getMyUsername());
            int nameColor = isHov ? 0xFFDDBBFF : (client.isHost ? 0xFFFFAA00 : 0xFFFFFFFF);

            int rightReserve = 4;
            if (isSelf) rightReserve += PackUiText.width(textRenderer, " (YOU)", PACKUI_THEME.fontFor(PackUiTone.MUTED), PackUtilColors.textSecondary()) + 2;
            if (client.isHost) rightReserve += PackUiText.width(textRenderer, " [HOST]", PACKUI_THEME.fontFor(PackUiTone.LABEL), 0xFFFFAA00) + 2;
            int offset = client.delayOffsetMs;
            if (offset > 0) rightReserve += PackUiText.width(textRenderer, "+" + offset + "ms", PACKUI_THEME.fontFor(PackUiTone.BODY), 0xFF55DDFF) + 4;
            int nameMaxW = Math.max(20, rowWidth - 18 - rightReserve);
            String displayName = PackUtilText.trimToWidth(textRenderer, client.username, nameMaxW, PackUtilText.Tone.BODY);
            PackUiText.draw(ctx, textRenderer, displayName, PACKUI_THEME.fontFor(PackUiTone.BODY), nameColor, lx + 18, cy + 3, false);

            int badgeX = lx + 18 + PackUiText.width(textRenderer, displayName, PACKUI_THEME.fontFor(PackUiTone.BODY), nameColor) + 4;

            if (isSelf) {
                drawUiText(ctx, "(YOU)", PackUiTone.MUTED, PackUtilColors.textSecondary(), badgeX, cy + 3);
                badgeX += PackUiText.width(textRenderer, "(YOU)", PACKUI_THEME.fontFor(PackUiTone.MUTED), PackUtilColors.textSecondary()) + 4;
            }

            if (client.isHost) {
                drawUiText(ctx, "[HOST]", PackUiTone.LABEL, 0xFFFFAA00, badgeX, cy + 3);
            }

            if (offset > 0) {
                String offsetStr = "+" + offset + "ms";
                int ofw = PackUiText.width(textRenderer, offsetStr, PACKUI_THEME.fontFor(PackUiTone.BODY), 0xFF55DDFF);
                drawUiText(ctx, offsetStr, PackUiTone.BODY, 0xFF55DDFF, panelX + PANEL_WIDTH - CONTENT_PADDING - ofw, cy + 3);
            }

            final String peerName = client.username;
            clickRegions.add(new ClickRegion(lx - 2, cy, rowWidth, ROW_HEIGHT, () -> {
                selectedPeer = peerName.equals(selectedPeer) ? null : peerName;
                saveState();
            }));

            cy += ROW_HEIGHT + 1;

            if (isSelected) {
                int detailBg = PackUtilColors.background();
                int detailH = 62;
                PackUiText.fill(ctx, lx + 4, cy, panelX + PANEL_WIDTH - CONTENT_PADDING - 4, cy + detailH, detailBg);
                PackUiText.fill(ctx, lx + 4, cy, lx + 5, cy + detailH, PackUtilColors.secondary());

                int dx = lx + 10;
                int dy = cy + 4;

                drawUiText(ctx, "Status: " + status.name(), PackUiTone.BODY,
                    status == PackUtilLANSync.PeerStatus.HEALTHY ? 0xFF55FF55 : 0xFFFFAA00, dx, dy);
                dy += 12;

                drawUiText(ctx, isSelf ? "(This is you)" : "Connected peer", PackUiTone.MUTED, PackUtilColors.textDim(), dx, dy);
                dy += 13;

                drawUiText(ctx, "Exec delay", PackUiTone.MUTED, PackUtilColors.textSecondary(), dx, dy);
                dy += 11;

                int ctrlX = dx + 4;
                int btnS = 14;

                drawOverlayButton(ctx, ctrlX, dy, btnS, btnS, "-", PackUiOverlayButton.Variant.SECONDARY, true, mx, my);
                clickRegions.add(new ClickRegion(ctrlX, dy, btnS, btnS, () -> {
                    int cur = sync.getPlayerDelayOffset(peerName);
                    sync.setPlayerDelayOffset(peerName, Math.max(0, cur - 1));
                }));
                ctrlX += btnS + 3;

                String valStr = offset + "ms";
                int valW = PackUiText.width(textRenderer, valStr, PACKUI_THEME.fontFor(PackUiTone.BODY), 0xFFFFFFFF) + 8;
                PackUiText.fill(ctx, ctrlX, dy, ctrlX + valW, dy + btnS, PackUtilColors.rowNormal());
                drawUiText(ctx, valStr, PackUiTone.BODY, 0xFFFFFFFF, ctrlX + 4, dy + 2);
                ctrlX += valW + 3;

                drawOverlayButton(ctx, ctrlX, dy, btnS, btnS, "+", PackUiOverlayButton.Variant.SECONDARY, true, mx, my);
                clickRegions.add(new ClickRegion(ctrlX, dy, btnS, btnS, () -> {
                    int cur = sync.getPlayerDelayOffset(peerName);
                    sync.setPlayerDelayOffset(peerName, cur + 1);
                }));
                ctrlX += btnS + 5;

                int resetW = PackUiText.width(textRenderer, "Reset", PACKUI_THEME.fontFor(PackUiTone.BODY), PACKUI_THEME.color(PackUiTone.BODY)) + 10;
                drawOverlayButton(ctx, ctrlX, dy, resetW, btnS, "Reset", PackUiOverlayButton.Variant.SECONDARY, true, mx, my);
                clickRegions.add(new ClickRegion(ctrlX, dy, resetW, btnS, () -> {
                    sync.setPlayerDelayOffset(peerName, 0);
                }));

                cy += detailH + 2;
            }
        }

        maxScroll = 0;
        clampScrollOffset();
    }

    private void renderQueue(GuiGraphicsExtractor ctx, int mx, int my, int y, int h) {
        PackUtilLANSync sync = PackUtilLANSync.getInstance();
        int lx = panelX + CONTENT_PADDING;
        int cy = y + 6;

        PackUtilSharedState state = PackUtilSharedState.get();
        int queueSize = state.getDelayedPackets() != null ? state.getDelayedPackets().size() : 0;

        drawUiText(ctx, "My Queue", PackUiTone.LABEL, 0xFFFFFFFF, lx, cy);
        cy += 12;
        drawUiText(ctx, queueSize + " packets queued", PackUiTone.BODY, queueSize > 0 ? 0xFF55FF55 : PackUtilColors.textDim(), lx, cy);
        cy += 16;

        int btnW = PANEL_WIDTH - CONTENT_PADDING * 2;
        drawOverlayButton(ctx, lx, cy, btnW, BUTTON_HEIGHT, "Share Queue", PackUiOverlayButton.Variant.SECONDARY, true, mx, my);
        clickRegions.add(new ClickRegion(lx, cy, btnW, BUTTON_HEIGHT, () -> {
            try {
                String queueData = PackUtilClipboardHelper.serializeQueueToBase64(
                    PackUtilSharedState.get().getDelayedPackets());
                sync.broadcastQueueSync(queueData);
                PackUtilClientMessaging.sendPrefixed("Ã‚Â§aQueue broadcasted to " + (sync.getConnectedCount() - 1) + " peers");
            } catch (Exception e) {
                PackUtilClientMessaging.sendPrefixed("Ã‚Â§cFailed to broadcast queue");
            }
        }));
        cy += BUTTON_HEIGHT + 8;

        drawOverlayButton(ctx, lx, cy, btnW, BUTTON_HEIGHT, "Sync Execute", PackUiOverlayButton.Variant.PRIMARY, true, mx, my);
        clickRegions.add(new ClickRegion(lx, cy, btnW, BUTTON_HEIGHT, () -> {
            sync.sendQueuedPackets();
        }));
        maxScroll = 0;
    }

    private void renderExecute(GuiGraphicsExtractor ctx, int mx, int my, int y, int h) {
        PackUtilLANSync sync = PackUtilLANSync.getInstance();
        int lx = panelX + CONTENT_PADDING;
        int cy = y + 4;
        int btnW = PANEL_WIDTH - CONTENT_PADDING * 2;

        String modeLabel = perUserMode ? "Mode: Per-User" : "Mode: Shared Macro";
        drawOverlayButton(ctx, lx, cy, btnW, BUTTON_HEIGHT, modeLabel, PackUiOverlayButton.Variant.PRIMARY, true, mx, my);
        clickRegions.add(new ClickRegion(lx, cy, btnW, BUTTON_HEIGHT, () -> {
            perUserMode = !perUserMode;
            expandedExecutePeer = null;
            scrollOffset = 0;
            contentScrollState.jumpTo(0, 0);
            clearSameMacrolistViewport();
            clearperUserlistViewport();
            if (perUserMode) {

                perUserAssignments.clear();
                Map<String, String> synced = sync.getSyncedAssignments();
                for (String peer : sync.getConnectedClients().keySet()) {
                    perUserAssignments.put(peer, synced.getOrDefault(peer, ""));
                }
            }
            saveState();
        }));
        cy += BUTTON_HEIGHT + 6;

        if (perUserMode) {

            clearSameMacrolistViewport();
            cy = renderExecutePerUser(ctx, mx, my, cy, lx, btnW, sync);
        } else {

            clearperUserlistViewport();
            cy = renderExecuteSameMacro(ctx, mx, my, cy, lx, btnW, sync);
        }

        boolean anyRunning = autismclient.util.macro.MacroExecutor.isRunning();
        if (anyRunning) {
            drawOverlayButton(ctx, lx, cy, btnW, BUTTON_HEIGHT, "Sync Stop All", PackUiOverlayButton.Variant.DANGER, true, mx, my);
            clickRegions.add(new ClickRegion(lx, cy, btnW, BUTTON_HEIGHT, () -> sync.broadcastStopAll()));
            cy += BUTTON_HEIGHT + 6;
        }

        PackUiText.fill(ctx, lx - 2, cy, panelX + PANEL_WIDTH - CONTENT_PADDING + 2, cy + 1, PackUtilColors.secondary());
        cy += 6;

        PackUtilLANSync.SyncState state = sync.getSyncState();
        String stateStr = formatSyncState(state, sync);
        int stateColor = getSyncStateColor(state);
        drawUiText(ctx, "Status: " + stateStr, PackUiTone.BODY, stateColor, lx, cy);
        cy += 14;

        String setBy = sync.getAssignmentSetBy();
        if (perUserMode && !setBy.isEmpty()) {
            drawUiText(ctx, "Assigned by: " + setBy, PackUiTone.MUTED, PackUtilColors.textDim(), lx, cy);
            cy += 12;
        }

        PackUtilLANSync.SpreadResult spread = sync.getLastSpreadResult();
        if (spread != null) {
            String spreadStr = String.format("Last spread: %.1fms (%d clients)", spread.getSpreadMs(), spread.clientCount);
            drawUiText(ctx, spreadStr, PackUiTone.BODY, getSpreadColor(spread.getSpreadMs()), lx, cy);
            cy += 14;
        }

        maxScroll = 0;
        clampScrollOffset();
    }

    private int renderExecuteSameMacro(GuiGraphicsExtractor ctx, int mx, int my, int cy, int lx, int btnW, PackUtilLANSync sync) {
        PackUiListRenderer.drawHeader(ctx, textRenderer, "Local Macros", lx, cy);
        cy += 14;

        List<PackUtilMacro> localMacros = PackUtilMacroManager.get().getAll();
        if (localMacros.isEmpty()) {
            clearSameMacrolistViewport();
            drawUiText(ctx, "No macros", PackUiTone.MUTED, PackUtilColors.textDim(), lx, cy);
            cy += 12;
        } else {

            sameMacroListX = lx - 2;
            sameMacroListY = cy;
            sameMacroListWidth = getContentWidth() + 4;
            sameMacroListHeight = macrolistViewportHeight(localMacros.size());

            if (sameMacroList == null || sameMacroList.getX() != sameMacroListX || sameMacroList.getY() != sameMacroListY
                || sameMacroList.getWidth() != sameMacroListWidth || sameMacroList.getHeight() != sameMacroListHeight) {
                sameMacroList = new PackUiScrollList<>(sameMacroListX, sameMacroListY, sameMacroListWidth, sameMacroListHeight,
                    sameMacroListRowPitch(), SAME_MACRO_LIST_SCROLLBAR_WIDTH,
                    macro -> macro.name,
                    (macro, selected) -> {
                        if (selected) selectedMacroName = macro.name;
                    });
                sameMacroList.setSecondaryTextExtractor(macro -> {
                    int actionCount = macro.actions != null ? macro.actions.size() : 0;
                    return "(" + actionCount + ")";
                });
                sameMacroList.setSelectedIndex(-1);
            }
            sameMacroList.setItems(localMacros);

            for (int i = 0; i < localMacros.size(); i++) {
                if (localMacros.get(i).name.equals(selectedMacroName)) {
                    sameMacroList.setSelectedIndex(i);
                    break;
                }
            }

            sameMacroList.render(ctx, textRenderer, mx, my);

            cy += sameMacroListHeight;
        }
        cy += 8;

        MacroSyncStatus syncStatus = analyzeSameMacroStatus(sync, selectedMacroName);
        if (selectedMacroName != null) {
            String selectedLine = PackUtilText.trimToWidth(textRenderer, "Selected: " + selectedMacroName, btnW, PackUtilText.Tone.BODY);
            drawUiText(ctx, selectedLine, PackUiTone.BODY, 0xFFFFFFFF, lx, cy);
            cy += 12;

            if (!syncStatus.missingPeers.isEmpty()) {
                String missingLine = PackUtilText.trimToWidth(
                    textRenderer,
                    "Missing on: " + formatNameSummary(syncStatus.missingPeers),
                    btnW,
                    PackUtilText.Tone.BODY
                );
                drawUiText(ctx, missingLine, PackUiTone.BODY, 0xFFFF6B6B, lx, cy);
                cy += 12;
            }

            if (!syncStatus.differentPeers.isEmpty()) {
                String differentLine = PackUtilText.trimToWidth(
                    textRenderer,
                    "Different on: " + formatDifferenceSummary(syncStatus.differentPeers),
                    btnW,
                    PackUtilText.Tone.BODY
                );
                drawUiText(ctx, differentLine, PackUiTone.BODY, 0xFFFFC857, lx, cy);
                cy += 12;
            }

            if (syncStatus.missingPeers.isEmpty() && syncStatus.differentPeers.isEmpty()) {
                String readyLine = sync.getConnectedCount() > 1 ? "All peers have this macro" : "Ready to execute";
                drawUiText(ctx, readyLine, PackUiTone.BODY, 0xFF69E59B, lx, cy);
                cy += 12;
            } else if (syncStatus.missingPeers.isEmpty()) {
                drawUiText(ctx, "Execution allowed in Same Macro mode", PackUiTone.BODY, 0xFF69E59B, lx, cy);
                cy += 12;
            }
        }

        int shareW = Math.min(112, Math.max(94, PackUiText.width(textRenderer, "Send To Peers", PACKUI_THEME.fontFor(PackUiTone.BODY), PackUtilColors.textPrimary()) + 12));
        int executeX = lx + shareW + 4;
        int executeW = btnW - shareW - 4;
        boolean shareEnabled = selectedMacroName != null && sync.getConnectedCount() > 1;
        boolean executeEnabled = selectedMacroName != null && syncStatus.canExecute();

        if (shareEnabled) {
            drawOverlayButton(ctx, lx, cy, shareW, BUTTON_HEIGHT, "Send To Peers", PackUiOverlayButton.Variant.SUCCESS, true, mx, my);
            clickRegions.add(new ClickRegion(lx, cy, shareW, BUTTON_HEIGHT, () -> {
                if (selectedMacroName != null) {
                    sync.shareMacroWithPeers(selectedMacroName);
                }
            }));
        } else {
            drawOverlayButton(ctx, lx, cy, shareW, BUTTON_HEIGHT, "Send To Peers", PackUiOverlayButton.Variant.SUCCESS, false, mx, my);
        }

        String executeLabel;
        if (selectedMacroName == null) {
            executeLabel = "Select a macro";
        } else if (!executeEnabled) {
            executeLabel = "Need all peers to have it";
        } else {
            executeLabel = "Sync Execute";
        }

        if (executeEnabled) {
            drawOverlayButton(ctx, executeX, cy, executeW, BUTTON_HEIGHT, executeLabel, PackUiOverlayButton.Variant.PRIMARY, true, mx, my);
            clickRegions.add(new ClickRegion(executeX, cy, executeW, BUTTON_HEIGHT, () -> {
                if (selectedMacroName != null) sync.executeMacroSynchronized(selectedMacroName);
            }));
        } else {
            drawOverlayButton(ctx, executeX, cy, executeW, BUTTON_HEIGHT, executeLabel, PackUiOverlayButton.Variant.PRIMARY, false, mx, my);
        }
        cy += BUTTON_HEIGHT + 6;
        return cy;
    }

    private int renderExecutePerUser(GuiGraphicsExtractor ctx, int mx, int my, int cy, int lx, int btnW, PackUtilLANSync sync) {
        syncPerUserAssignments(sync);

        int rowW = getContentWidth() + 4;
        perUserListX = lx - 2;
        perUserListY = cy;
        perUserListWidth = rowW;
        int perUserListContentHeight = estimatePerUserListContentHeight(sync);
        perUserListHeight = cappedlistViewportHeight(perUserListContentHeight);

        if (perUserlistViewport == null || perUserlistViewport.getX() != perUserListX || perUserlistViewport.getY() != perUserListY
            || perUserlistViewport.getWidth() != perUserListWidth || perUserlistViewport.getHeight() != perUserListHeight) {
            perUserlistViewport = new PackUiScrollViewport(perUserListX, perUserListY, perUserListWidth, perUserListHeight,
                sameMacroListRowPitch(), SAME_MACRO_LIST_SCROLLBAR_WIDTH);
        }
        perUserlistViewport.setContentHeight(perUserListContentHeight);

        int drawScroll = perUserlistViewport.getScrollOffset();
        final int clipTop = perUserListY + 1;
        final int clipBottom = perUserListY + perUserListHeight - 1;
        final int rowPitch = perUserlistViewport.getRowHeight();

        PackUiText.fill(ctx, perUserListX, perUserListY, perUserListX + perUserListWidth, perUserListY + perUserListHeight, PACKUI_THEME.listFill());
        PackUiText.fill(ctx, perUserListX, perUserListY, perUserListX + perUserListWidth, perUserListY + 1, PACKUI_THEME.borderSoft());
        PackUiText.fill(ctx, perUserListX, perUserListY + perUserListHeight - 1, perUserListX + perUserListWidth, perUserListY + perUserListHeight, PACKUI_THEME.borderSoft());
        PackUiText.fill(ctx, perUserListX, perUserListY, perUserListX + 1, perUserListY + perUserListHeight, PACKUI_THEME.borderSoft());
        PackUiText.fill(ctx, perUserListX + perUserListWidth - 1, perUserListY, perUserListX + perUserListWidth, perUserListY + perUserListHeight, PACKUI_THEME.borderSoft());

        PackUtilUiScale.enableOverlayScissor(ctx, perUserListX + 1, perUserListY + 1, perUserListX + perUserListWidth - 1, perUserListY + perUserListHeight - 1);
        try {
            cy = perUserListY - drawScroll;

            for (Map.Entry<String, String> entry : new ArrayList<>(perUserAssignments.entrySet())) {
                String peer = entry.getKey();
                String assigned = entry.getValue();
                boolean isExpanded = peer.equals(expandedExecutePeer);
                boolean isMe = peer.equals(sync.getMyUsername());

                boolean rowVisible = cy + rowPitch > clipTop && cy < clipBottom;
                boolean rowHov = rowVisible && isHovered(mx, my, lx - 2, cy, rowW, rowPitch);

                int rowBg = isExpanded ? PackUtilColors.rowSelected() : (rowHov ? PackUtilColors.rowHover() : PackUtilColors.rowNormal());
                String peerLabel = peer + (isMe ? " (you)" : "");
                String assignLabel = assigned.isEmpty() ? "(none)" : assigned;
                int assignColor = assigned.isEmpty() ? PackUtilColors.textDim() : 0xFF55FF55;

                int maxAssignW = Math.min((rowW - 12) / 2, PackUiText.width(textRenderer, assignLabel, PACKUI_THEME.fontFor(PackUiTone.BODY), assignColor));
                int peerNameMaxW = Math.max(20, rowW - 10 - maxAssignW - 8 - 12);
                if (rowVisible) {

                    int rowTop = Math.max(cy, clipTop);
                    int rowBottom = Math.min(cy + rowPitch, clipBottom);
                    if (rowBottom > rowTop) {

                        PackUiText.fill(ctx, perUserListX + 1, rowTop, perUserListX + perUserListWidth - 1, rowBottom, rowBg);

                        int chevronY = Math.max(cy + 3, clipTop + 1);
                        if (chevronY < clipBottom - 10) {
                            PackUiControlGlyphs.drawChevron(
                                ctx,
                                lx + 1,
                                chevronY,
                                8,
                                isExpanded ? PackUiControlGlyphs.ChevronDirection.DOWN : PackUiControlGlyphs.ChevronDirection.RIGHT,
                                isExpanded ? 0xFFF6EFEF : 0xFFE2D4D4,
                                0xB8381418,
                                1.0f
                            );
                        }

                        int textY = Math.max(cy + 3, clipTop + 1);
                        if (textY < clipBottom - 2) {
                            String truncPeerLabel = PackUtilText.trimToWidth(textRenderer, peerLabel, peerNameMaxW, PackUtilText.Tone.BODY);
                            drawUiText(ctx, truncPeerLabel, PackUiTone.BODY, isMe ? PackUtilColors.textSecondary() : 0xFFFFFFFF, lx + 10, textY);

                            String truncAssign = PackUtilText.trimToWidth(textRenderer, assignLabel, maxAssignW, PackUtilText.Tone.BODY);
                            int truncAssignW = PackUiText.width(textRenderer, truncAssign, PACKUI_THEME.fontFor(PackUiTone.BODY), assignColor);

                            drawUiText(ctx, truncAssign, PackUiTone.BODY, assignColor, PackUiLegacyLayout.rightAlign(lx, rowW - 12, truncAssignW, 4), textY);
                        }
                    }
                }

                final String peerKey = peer;
                if (rowVisible) {
                    clickRegions.add(new ClickRegion(lx - 2, cy, rowW, rowPitch, () -> {
                        expandedExecutePeer = peerKey.equals(expandedExecutePeer) ? null : peerKey;
                        saveState();
                    }));
                }
                cy += rowPitch;

                if (isExpanded) {
                    int detailLx = lx + 6;
                    int detailRx = panelX + PANEL_WIDTH - CONTENT_PADDING - 4;

                    List<PackUtilMacro> myMacroList = PackUtilMacroManager.get().getAll();
                    java.util.Map<String, Integer> myMacroMap = new java.util.LinkedHashMap<>();
                    for (PackUtilMacro m : myMacroList) {
                        myMacroMap.put(m.name, m.actions != null ? m.actions.size() : 0);
                    }

                    if (isMe) {

                        for (PackUtilMacro macro : myMacroList) {
                            cy = renderPickerRowClipped(ctx, mx, my, cy, detailLx, detailRx, rowW,
                                macro.name, assigned, null, peerKey, sync, false, clipTop, clipBottom, rowPitch);
                        }
                    } else {

                        Map<String, Map<String, PackUtilMacro>> allRemote = sync.getAllRemoteMacros();
                        Map<String, PackUtilMacro> peerMacroData = allRemote.getOrDefault(peer, java.util.Collections.emptyMap());
                        java.util.Map<String, Integer> peerMacroMap = new java.util.LinkedHashMap<>();
                        for (Map.Entry<String, PackUtilMacro> e : peerMacroData.entrySet()) {
                            peerMacroMap.put(e.getKey(), e.getValue().actions != null ? e.getValue().actions.size() : 0);
                        }

                        List<String> shared = new ArrayList<>();
                        java.util.Map<String, Integer> different = new java.util.LinkedHashMap<>();
                        List<String> peerOnly = new ArrayList<>();
                        List<String> mineOnly = new ArrayList<>();

                        for (String name : peerMacroMap.keySet()) {
                            if (myMacroMap.containsKey(name)) {
                                int diffs = countActionDifferences(
                                    PackUtilMacroManager.get().get(name), peerMacroData.get(name));
                                if (diffs == 0) {
                                    shared.add(name);
                                } else {
                                    different.put(name, diffs);
                                }
                            } else {
                                peerOnly.add(name);
                            }
                        }
                        for (String name : myMacroMap.keySet()) {
                            if (!peerMacroMap.containsKey(name)) {
                                mineOnly.add(name);
                            }
                        }

                        if (!shared.isEmpty()) {
                            for (String name : shared) {
                                cy = renderPickerRowClipped(ctx, mx, my, cy, detailLx, detailRx, rowW,
                                    name, assigned, null, peerKey, sync, false, clipTop, clipBottom, rowPitch);
                            }
                        }

                        if (!different.isEmpty()) {
                            for (Map.Entry<String, Integer> de : different.entrySet()) {
                                String name = de.getKey();
                                int diff = de.getValue();
                                String badge = " +" + diff + " Difference" + (diff > 1 ? "s" : "");
                                cy = renderPickerRowClipped(ctx, mx, my, cy, detailLx, detailRx, rowW,
                                    name, assigned, badge, peerKey, sync, false, clipTop, clipBottom, rowPitch);
                            }
                        }

                        if (!peerOnly.isEmpty()) {
                            for (String name : peerOnly) {
                                cy = renderPickerRowClipped(ctx, mx, my, cy, detailLx, detailRx, rowW,
                                    name, assigned, null, peerKey, sync, true, clipTop, clipBottom, rowPitch);
                            }
                        }

                        if (!mineOnly.isEmpty()) {
                            for (String name : mineOnly) {
                                cy = renderPickerRowClipped(ctx, mx, my, cy, detailLx, detailRx, rowW,
                                    name, assigned, null, peerKey, sync, false, clipTop, clipBottom, rowPitch);
                            }
                        }

                        if (shared.isEmpty() && different.isEmpty() && peerOnly.isEmpty() && mineOnly.isEmpty()) {
                            if (cy + rowPitch > clipTop && cy < clipBottom) {
                                int rowTop = Math.max(cy, clipTop);
                                int rowBottom = Math.min(cy + rowPitch, clipBottom);
                                if (rowBottom > rowTop) {
            PackUiText.fill(ctx, detailLx - 2, rowTop, detailRx, rowBottom, PACKUI_THEME.rowFillHovered());
                                    int textY = Math.max(cy + 3, clipTop + 1);
                                    if (textY < clipBottom - 2) {
                                        drawUiText(ctx, "No macros available", PackUiTone.MUTED, PackUtilColors.textDim(), detailLx, textY);
                                    }
                                }
                            }
                            cy += rowPitch;
                        }
                    }

                    cy += 4;
                }
            }
        } finally {
            ctx.disableScissor();
        }

        perUserlistViewport.renderScrollbar(ctx, mx, my);

        cy = perUserListY + perUserListHeight;

        AssignmentSyncStatus status = analyzeAssignmentStatus(sync);
        java.util.LinkedHashSet<String> shareableMissingMacros = getShareableMissingMacros(status);

        cy += 6;

        if (!status.missingAssignments.isEmpty()) {
            String missingLine = PackUtilText.trimToWidth(
                textRenderer,
                "Missing: " + formatMissingAssignmentsSummary(status.missingAssignments),
                btnW,
                PackUtilText.Tone.BODY
            );
            drawUiText(ctx, missingLine, PackUiTone.BODY, 0xFFFF6B6B, lx, cy);
            cy += 12;
        } else if (status.hasAssignments()) {
            drawUiText(ctx, "Assigned macros are ready", PackUiTone.BODY, 0xFF69E59B, lx, cy);
            cy += 12;
        }

        int shareW = Math.min(112, Math.max(96, PackUiText.width(textRenderer, "Send Missing", PACKUI_THEME.fontFor(PackUiTone.BODY), PackUtilColors.textPrimary()) + 12));
        int executeX = lx + shareW + 4;
        int executeW = btnW - shareW - 4;

        if (!shareableMissingMacros.isEmpty()) {
            String sendLabel = shareableMissingMacros.size() > 1 ? "Send Missing (" + shareableMissingMacros.size() + ")" : "Send Missing";
            drawOverlayButton(ctx, lx, cy, shareW, BUTTON_HEIGHT, sendLabel, PackUiOverlayButton.Variant.SUCCESS, true, mx, my);
            clickRegions.add(new ClickRegion(lx, cy, shareW, BUTTON_HEIGHT, () -> {
                for (String macroName : shareableMissingMacros) {
                    sync.shareMacroWithPeers(macroName);
                }
            }));
        } else {
            drawOverlayButton(ctx, lx, cy, shareW, BUTTON_HEIGHT, "Send Missing", PackUiOverlayButton.Variant.SUCCESS, false, mx, my);
        }

        String puLabel;
        if (!status.hasAssignments()) {
            puLabel = "Assign macros to users above";
        } else if (!status.canExecute()) {
            puLabel = "Need missing macros first";
        } else {
            puLabel = "Sync Execute (Per-User)";
        }

        if (status.canExecute()) {
            drawOverlayButton(ctx, executeX, cy, executeW, BUTTON_HEIGHT, puLabel, PackUiOverlayButton.Variant.PRIMARY, true, mx, my);
            clickRegions.add(new ClickRegion(executeX, cy, executeW, BUTTON_HEIGHT, () -> {
                if (!status.assignments.isEmpty()) {
                    sync.executeMacrosSynchronized(status.assignments);
                }
            }));
        } else {
            drawOverlayButton(ctx, executeX, cy, executeW, BUTTON_HEIGHT, puLabel, PackUiOverlayButton.Variant.PRIMARY, false, mx, my);
        }
        cy += BUTTON_HEIGHT + 6;
        return cy;
    }

    private MacroSyncStatus analyzeSameMacroStatus(PackUtilLANSync sync, String macroName) {
        List<String> missingPeers = new ArrayList<>(sync.getPeersMissingMacro(macroName));
        Map<String, Integer> differentPeers = new java.util.LinkedHashMap<>();
        if (macroName == null || macroName.isBlank()) {
            return new MacroSyncStatus(missingPeers, differentPeers);
        }

        PackUtilMacro localMacro = PackUtilMacroManager.get().get(macroName);
        if (localMacro == null) {
            return new MacroSyncStatus(missingPeers, differentPeers);
        }

        java.util.Set<String> missingPeerSet = new java.util.HashSet<>(missingPeers);
        Map<String, Map<String, PackUtilMacro>> allRemote = sync.getAllRemoteMacros();
        List<String> peers = new ArrayList<>(sync.getConnectedClients().keySet());
        java.util.Collections.sort(peers);
        for (String peer : peers) {
            if (peer.equals(sync.getMyUsername()) || missingPeerSet.contains(peer)) continue;
            Map<String, PackUtilMacro> peerMacros = allRemote.get(peer);
            if (peerMacros == null) continue;
            PackUtilMacro remoteMacro = peerMacros.get(macroName);
            int differences = countActionDifferences(localMacro, remoteMacro);
            if (differences > 0) {
                differentPeers.put(peer, differences);
            }
        }

        return new MacroSyncStatus(missingPeers, differentPeers);
    }

    private AssignmentSyncStatus analyzeAssignmentStatus(PackUtilLANSync sync) {
        Map<String, String> assignments = new java.util.LinkedHashMap<>();
        java.util.Set<String> connectedPeers = new java.util.LinkedHashSet<>(sync.getConnectedClients().keySet());
        for (Map.Entry<String, String> entry : new java.util.LinkedHashMap<>(perUserAssignments).entrySet()) {
            String username = entry.getKey();
            String macroName = entry.getValue();
            if (!connectedPeers.contains(username)) continue;
            if (macroName == null || macroName.isBlank()) continue;
            assignments.put(username, macroName);
        }

        Map<String, String> missingAssignments = sync.getMissingAssignedMacros(assignments);
        return new AssignmentSyncStatus(assignments, missingAssignments);
    }

    private java.util.LinkedHashSet<String> getShareableMissingMacros(AssignmentSyncStatus status) {
        java.util.LinkedHashSet<String> shareable = new java.util.LinkedHashSet<>();
        for (String macroName : status.missingAssignments.values()) {
            if (macroName == null || macroName.isBlank()) continue;
            if (PackUtilMacroManager.get().get(macroName) != null) {
                shareable.add(macroName);
            }
        }
        return shareable;
    }

    private String formatNameSummary(List<String> names) {
        if (names == null || names.isEmpty()) return "none";
        return String.join(", ", names);
    }

    private String formatDifferenceSummary(Map<String, Integer> differences) {
        if (differences == null || differences.isEmpty()) return "none";
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : differences.entrySet()) {
            int diffCount = Math.max(1, entry.getValue());
            parts.add(entry.getKey() + " (+" + diffCount + ")");
        }
        return String.join(", ", parts);
    }

    private String formatMissingAssignmentsSummary(Map<String, String> missingAssignments) {
        if (missingAssignments == null || missingAssignments.isEmpty()) return "none";
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : missingAssignments.entrySet()) {
            parts.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join(", ", parts);
    }

    private int renderPickerRowClipped(GuiGraphicsExtractor ctx, int mx, int my, int cy,
            int detailLx, int detailRx, int rowW,
            String macroName, String assigned, String badge,
            String peerKey, PackUtilLANSync sync, boolean allowImport,
            int clipTop, int clipBottom, int rowPitch) {
        boolean isCurrent = macroName.equals(assigned);
        int importW = allowImport ? 42 : 0;
        int assignW = Math.max(24, (detailRx - (detailLx - 2)) - importW - 2);

        boolean rowVisible = cy + rowPitch > clipTop && cy < clipBottom;
        if (!rowVisible) {
            return cy + rowPitch;
        }

        int drawY = Math.max(cy, clipTop);
        int drawH = Math.min(cy + rowPitch, clipBottom) - drawY;
        if (drawH <= 0) {
            return cy + rowPitch;
        }

        boolean mHov = isHovered(mx, my, detailLx - 2, cy, assignW, rowPitch);
        PackUiListRenderer.drawRow(
            ctx,
            textRenderer,
            "",
            detailLx - 2,
            drawY,
            detailRx - (detailLx - 2),
            drawH,
            mHov,
            isCurrent,
            badge != null ? PackUiListRenderer.RowTone.WARNING : PackUiListRenderer.RowTone.NORMAL
        );

        int tx = detailLx;
        int nameMaxW = Math.max(24, detailRx - tx - importW - (badge != null ? 74 : 4));
        String displayName = PackUtilText.trimToWidth(textRenderer, macroName, nameMaxW, PackUtilText.Tone.BODY);
        int textY = Math.max(cy + 3, clipTop + 1);
        if (textY < clipBottom - 2) {
            PackUiText.draw(ctx, textRenderer, displayName, PACKUI_THEME.fontFor(PackUiTone.BODY), isCurrent ? PackUtilColors.rowSelectedText() : 0xFFFFFFFF, tx, textY, false);
        }

        if (badge != null && textY < clipBottom - 2) {
            int badgeX = tx + PackUiText.width(textRenderer, displayName, PACKUI_THEME.fontFor(PackUiTone.BODY), isCurrent ? PackUtilColors.rowSelectedText() : 0xFFFFFFFF) + 4;
            drawUiText(ctx, badge, PackUiTone.MUTED, 0xFFFFAA00, badgeX, textY);
        }

        final String mName = macroName;
        if (rowVisible) {
            clickRegions.add(new ClickRegion(detailLx - 2, cy, assignW, rowPitch, () -> {
                perUserAssignments.put(peerKey, mName.equals(perUserAssignments.get(peerKey)) ? "" : mName);
                sync.broadcastAssignments(perUserAssignments);
            }));
        }

        if (allowImport) {
            int importX = detailRx - importW + 2;
            int importY = Math.max(cy + 1, clipTop + 1);
            if (importY < clipBottom - 4) {
                drawOverlayButton(ctx, importX, importY, importW - 4, Math.min(rowPitch - 2, clipBottom - importY - 1), "Import", PackUiOverlayButton.Variant.SECONDARY, true, mx, my);
            }
            if (rowVisible) clickRegions.add(new ClickRegion(importX, cy, importW - 4, rowPitch, () -> {
                Map<String, Map<String, PackUtilMacro>> allRemote = sync.getAllRemoteMacros();
                Map<String, PackUtilMacro> peerMacros = allRemote.get(peerKey);
                PackUtilMacro sourceMacro = peerMacros != null ? peerMacros.get(mName) : null;
                if (sourceMacro == null) {
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§cMacro not found: " + mName);
                    return;
                }

                PackUtilMacro imported = PackUtilMacroManager.get().addImportedCopy(sourceMacro, sourceMacro.name);
                if (imported == null) {
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§cFailed to import macro: " + mName);
                    return;
                }

                PackUtilClientMessaging.sendPrefixed("Ã‚Â§aImported macro: " + imported.name);
            }));
        }
        return cy + rowPitch;
    }

    private static int countActionDifferences(PackUtilMacro a, PackUtilMacro b) {
        if (a == null || b == null) return -1;
        List<autismclient.util.macro.MacroAction> aActions = a.actions != null ? a.actions : java.util.Collections.emptyList();
        List<autismclient.util.macro.MacroAction> bActions = b.actions != null ? b.actions : java.util.Collections.emptyList();

        int diffs = 0;
        int maxLen = Math.max(aActions.size(), bActions.size());
        for (int i = 0; i < maxLen; i++) {
            if (i >= aActions.size() || i >= bActions.size()) {
                diffs++;
            } else {
                net.minecraft.nbt.CompoundTag tagA = aActions.get(i).toTag();
                net.minecraft.nbt.CompoundTag tagB = bActions.get(i).toTag();
                if (!tagA.equals(tagB)) diffs++;
            }
        }
        return diffs;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        if (false && button == 0 && !collapsed && isResizeActive(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, getPanelHeight())) {
            isResizing = true;
            resizeStartMouseX = mouseX;
            resizeStartMouseY = mouseY;
            resizeStartWidth = PANEL_WIDTH;
            resizeStartHeight = PANEL_HEIGHT;
            return true;
        }

        if (button == 0 && mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH
            && mouseY >= panelY && mouseY <= panelY + HEADER_HEIGHT) {
            PackUtilWindowLayout bounds = new PackUtilWindowLayout(panelX, panelY, PANEL_WIDTH, collapsed ? HEADER_HEIGHT : getPanelHeight(), visible, collapsed);

            if (isOverCollapseButton(mouseX, mouseY, bounds)) {
                toggleCollapsed();
                return true;
            }

            if (isOverCloseButton(mouseX, mouseY, bounds)) {
                setVisible(false);
                return true;
            }

            isDragging = true;
            dragOffsetX = mouseX - panelX;
            dragOffsetY = mouseY - panelY;
            return true;
        }

        if (collapsed) return false;

        if (lanChatField != null) {
            MouseButtonEvent click = new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
            if (lanChatField.mouseClicked(click, false)) {
                lanChatField.setFocused(true);
                return true;
            }
            lanChatField.setFocused(false);
        }

        PackUtilLANSync sync = PackUtilLANSync.getInstance();
        if (button == 0 && hasScrollableSessionContent(sync)) {
            int panelHeight = getPanelHeight();
            int tabContentY = getTabContentY();
            int tabContentH = getTabContentHeight(panelHeight);
            PackUiScrollbar.Metrics scrollbarMetrics = getContentScrollbarMetrics(tabContentY, tabContentH);
            if (scrollbarMetrics.hasScroll() && scrollbarMetrics.contains(mouseX, mouseY)) {
                scrollbarDragging = true;
                scrollbarGrabOffset = Math.max(0, (int) Math.round(mouseY) - scrollbarMetrics.thumbY());
                scrollOffset = PackUiScrollbar.scrollFromThumb(scrollbarMetrics, (int) Math.round(mouseY), scrollbarGrabOffset);
                contentScrollState.jumpTo(scrollOffset, scrollbarMetrics.maxScroll());
                saveState();
                return true;
            }
        }

        if (button == 0 && activeTab == 2 && !perUserMode && hasSameMacrolistViewport() && sameMacroList.contains(mouseX, mouseY)) {

            if (sameMacroList.mouseClicked(mouseX, mouseY, button)) {

                if (!sameMacroList.isScrollbarDragging()) {
                    PackUtilMacro selected = sameMacroList.getSelectedItem();
                    if (selected != null) {
                        selectedMacroName = selected.name;
                        saveState();
                    }
                }
                return true;
            }
        }

        if (button == 0 && activeTab == 2 && perUserMode && hasperUserlistViewport() && perUserlistViewport.contains(mouseX, mouseY)) {
            if (perUserlistViewport.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (button == 0) {
            for (ClickRegion region : clickRegions) {
                if (region.contains(mouseX, mouseY)) {
                    region.action.run();
                    return true;
                }
            }
        }

        return isMouseOver(mouseX, mouseY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (hasSameMacrolistViewport()) sameMacroList.mouseReleased();
            if (hasperUserlistViewport()) perUserlistViewport.mouseReleased();
            if (scrollbarDragging) saveState();
            scrollbarDragging = false;
            if (isDragging || isResizing) saveState();
            isDragging = false;
            isResizing = false;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrollbarDragging) {
            int panelHeight = getPanelHeight();
            int tabContentY = getTabContentY();
            int tabContentH = getTabContentHeight(panelHeight);
            PackUiScrollbar.Metrics scrollbarMetrics = getContentScrollbarMetrics(tabContentY, tabContentH);
            scrollOffset = PackUiScrollbar.scrollFromThumb(scrollbarMetrics, (int) Math.round(mouseY), scrollbarGrabOffset);
            contentScrollState.jumpTo(scrollOffset, scrollbarMetrics.maxScroll());
            return true;
        }
        if (hasSameMacrolistViewport() && sameMacroList.isScrollbarDragging()) {
            sameMacroList.mouseDragged(mouseX, mouseY);
            return true;
        }
        if (hasperUserlistViewport() && perUserlistViewport.isScrollbarDragging()) {
            perUserlistViewport.mouseDragged(mouseX, mouseY);
            return true;
        }
        if (isResizing) {
            PackUtilWindowLayout nextBounds = clampToScreen(this,
                new PackUtilWindowLayout(panelX, panelY,
                    resizeStartWidth + (int) Math.round(mouseX - resizeStartMouseX),
                    resizeStartHeight + (int) Math.round(mouseY - resizeStartMouseY),
                    visible, collapsed));
            panelX = nextBounds.x;
            panelY = nextBounds.y;
            PANEL_WIDTH = nextBounds.width;
            PANEL_HEIGHT = nextBounds.height;
            return true;
        }
        if (isDragging) {
            PackUtilWindowLayout nextBounds = clampToScreen(this,
                new PackUtilWindowLayout((int) (mouseX - dragOffsetX), (int) (mouseY - dragOffsetY),
                    PANEL_WIDTH, PANEL_HEIGHT, visible, collapsed));
            panelX = nextBounds.x;
            panelY = nextBounds.y;
            return true;
        }
        return false;
    }

    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (lanChatField != null && lanChatField.isFocused()) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                lanChatField.setFocused(false);
                return true;
            }

            lanChatField.keyPressed(new KeyEvent(key, scancode, modifiers));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible || collapsed || !isMouseOver(mouseX, mouseY)) return false;
        if (activeTab == 2 && !perUserMode && hasSameMacrolistViewport()) {
            return sameMacroList.mouseScrolled(mouseX, mouseY, amount);
        }
        if (activeTab == 2 && perUserMode && hasperUserlistViewport()) {
            return perUserlistViewport.mouseScrolled(mouseX, mouseY, amount);
        }
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(amount * 14)));
        saveState();
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (lanChatField != null && lanChatField.isFocused()) {
            return lanChatField.charTyped(new CharacterEvent(chr));
        }
        return false;
    }

    private boolean isHovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int getSyncStateColor(PackUtilLANSync.SyncState state) {
        switch (state) {
            case IDLE: return PackUtilColors.textDim();
            case PREPARING: return 0xFFFFAA00;
            case ALL_READY: return 0xFF55FF55;
            case DISPATCHING_GO: return 0xFF55FFFF;
            case EXECUTING: return 0xFF55FF55;
            case REPORTING: return 0xFFFFAA00;
            case DONE: return 0xFF55FF55;
            default: return PackUtilColors.textSecondary();
        }
    }

    private String formatSyncState(PackUtilLANSync.SyncState state, PackUtilLANSync sync) {
        switch (state) {
            case IDLE: return "Idle";
            case PREPARING:
                int ready = 0;
                int total = sync.getConnectedCount();

                return "Preparing (" + ready + "/" + total + " ready)";
            case ALL_READY: return "All Ready!";
            case DISPATCHING_GO: return "GO!";
            case EXECUTING: return "Executing...";
            case REPORTING: return "Collecting results...";
            case DONE: return "Done";
            default: return state.name();
        }
    }

    private int getSpreadColor(double spreadMs) {
        if (spreadMs <= 1) return 0xFF55FF55;
        if (spreadMs <= 5) return 0xFFFFAA00;
        return 0xFFFF5555;
    }

        private void applyPresetMetrics() {
        PANEL_BASE_WIDTH = 292;
        TAB_HEIGHT = 16;
        BUTTON_HEIGHT = 16;
        ROW_HEIGHT = 14;
        CONTENT_PADDING = 8;
        CHAT_FIELD_HEIGHT = 16;
        CHAT_AREA_HEIGHT = CHAT_FIELD_HEIGHT + 10;
    }

    private int minimumPanelHeight() {
        return 140;
    }

    private int defaultPanelHeight() {
        return 220;
    }
}
