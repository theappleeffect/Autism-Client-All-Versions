package autismclient.util;

import autismclient.gui.packui.PackUiAccordionSection;
import autismclient.gui.packui.PackUiAssets;
import autismclient.gui.packui.PackUiButton;
import autismclient.gui.packui.PackUiCompactRow;
import autismclient.gui.packui.PackUiIconLabel;
import autismclient.gui.packui.PackUiInsets;
import autismclient.gui.packui.PackUiMetricLabel;
import autismclient.gui.packui.PackUiPanelNode;
import autismclient.gui.packui.PackUiRenderContext;
import autismclient.gui.packui.PackUiSpacer;
import autismclient.gui.packui.PackUiSurface;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTextField;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import autismclient.gui.packui.PackUiViewport;
import autismclient.mixin.accessor.PackUtilHandledScreenAccessor;
import autismclient.modules.PackUtilModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class PackUtilLauncherOverlay extends PackUtilOverlayBase {
    private static final Minecraft MC = Minecraft.getInstance();

    private static final int DEFAULT_X = 0;
    private static final int DEFAULT_Y = 0;
    private static final int PANEL_PAD = 0;
    private static final int SECTION_PAD = 4;
    private static final int SECTION_GAP = 3;
    private static final int ROW_GAP = 2;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CHAT_HEIGHT = 20;
    private static final int CHAT_MIN_WIDTH = 136;
    private static final int TEXT_PAD = 8;
    private static final int ICON_SIZE = 14;
    private static final int ICON_GAP = 4;
    private static final int LABEL_ICON_SIZE = 13;
    private static final int MIN_PAIR_WIDTH = 58;
    private static final int MIN_PANEL_WIDTH = 164;
    private static final float HEADER_CLICK_DRAG_THRESHOLD = 3.0f;

    private final PackUiTheme theme = new PackUiTheme();
    private final PackUiPanelNode panelNode = new PackUiPanelNode();
    private final PackUiSurface surface = new PackUiSurface(theme, panelNode);

    private final PackUtilMacroListOverlay macroListOverlay;
    private final PackUtilFabricatorOverlay fabricatorOverlay;
    private final PackUtilLANSyncOverlay lanSyncOverlay;
    private final PackUtilQueueEditorOverlay queueEditorOverlay;
    private final PackUtilPacketLoggerOverlay packetLoggerOverlay;
    private final PackUtilCustomFilterOverlay customFilterOverlay;
    private Runnable closeWithoutPacketAction;
    private Runnable desyncAction;
    private PackUtilKeybindOverlay keybindOverlay;
    private PackUtilServerInfoOverlay serverInfoOverlay;

    private PackUiButton sendButton;
    private PackUiButton delayButton;
    private PackUiTextField chatField;
    private PackUiMetricLabel revMetric;
    private PackUiMetricLabel syncMetric;
    private PackUiMetricLabel slotMetric;
    private PackUiAccordionSection mainMenuSection;

    private int panelX = DEFAULT_X;
    private int panelY = DEFAULT_Y;
    private int panelWidth = MIN_PANEL_WIDTH;
    private int panelHeight = 220;
    private boolean visible = true;
    private boolean dragging = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private float pressStartUiX;
    private float pressStartUiY;
    private int pressStartPanelX;
    private int pressStartPanelY;
    private boolean dragMoved = false;

    public PackUtilLauncherOverlay(PackUtilMacroListOverlay macroListOverlay,
                                   PackUtilFabricatorOverlay fabricatorOverlay,
                                   PackUtilLANSyncOverlay lanSyncOverlay,
                                   PackUtilQueueEditorOverlay queueEditorOverlay,
                                   PackUtilPacketLoggerOverlay packetLoggerOverlay,
                                   PackUtilCustomFilterOverlay customFilterOverlay) {
        this.macroListOverlay = macroListOverlay;
        this.fabricatorOverlay = fabricatorOverlay;
        this.lanSyncOverlay = lanSyncOverlay;
        this.queueEditorOverlay = queueEditorOverlay;
        this.packetLoggerOverlay = packetLoggerOverlay;
        this.customFilterOverlay = customFilterOverlay;
        buildUi();
        restoreLayout();
        if (mainMenuSection != null) {
            mainMenuSection.syncExpanded(mainMenuSection.isExpanded());
        }
    }

    public void setCloseWithoutPacketAction(Runnable action) {
        this.closeWithoutPacketAction = action;
        buildUi();
    }

    public void setDesyncAction(Runnable action) {
        this.desyncAction = action;
        buildUi();
    }

    public void setKeybindOverlay(PackUtilKeybindOverlay overlay) {
        this.keybindOverlay = overlay;
    }

    public void setServerDataOverlay(PackUtilServerInfoOverlay overlay) {
        this.serverInfoOverlay = overlay;
    }

    private void buildUi() {
        panelNode.setPadding(PackUiInsets.all(PANEL_PAD)).setDrawBorder(false).setDrawFill(false);
        panelNode.content().clearChildren();
        panelNode.content().setPadding(PackUiInsets.NONE).setGap(0);

        mainMenuSection = new PackUiAccordionSection("MAIN MENU")
            .setHeaderHeight(sectionHeaderHeight())
            .setContentTopGap(sectionContentTopGap())
            .setExpanded(true);
        mainMenuSection.content().setPadding(new PackUiInsets(0, sectionPadding(), 0, sectionPadding())).setGap(rowGap());
        panelNode.content().add(mainMenuSection);

        addPairRow(mainMenuSection.content(),
            actionButton("Macros", PackUiAssets.ICON_MACROS, PackUiButton.Variant.SECONDARY, () -> {
                PackUtilMacroEditorOverlay macroEditorOverlay = PackUtilMacroEditorOverlay.getSharedOverlay();
                if (macroEditorOverlay != null && macroEditorOverlay.isVisible()) {
                    if (macroListOverlay != null) macroListOverlay.setVisible(false);
                    PackUtilOverlayManager.get().bringToFront(macroEditorOverlay);
                } else if (macroListOverlay != null) {
                    macroListOverlay.toggle();
                }
            }).setGrowX(true),
            actionButton("Fabricator", PackUiAssets.ICON_FABRICATOR, PackUiButton.Variant.SECONDARY, () -> {
                if (fabricatorOverlay != null) fabricatorOverlay.toggle();
            }).setGrowX(true)
        );
        addPairRow(mainMenuSection.content(),
            actionButton("LAN Sync", PackUiAssets.ICON_LANSYNC, PackUiButton.Variant.SECONDARY, () -> {
                if (lanSyncOverlay != null) lanSyncOverlay.toggle();
            }).setGrowX(true),
            actionButton("Queue", PackUiAssets.ICON_PACKET_Q_EDITOR, PackUiButton.Variant.SECONDARY, () -> {
                if (queueEditorOverlay != null) queueEditorOverlay.toggle();
            }).setGrowX(true)
        );
        addPairRow(mainMenuSection.content(),
            actionButton("Logger", PackUiAssets.ICON_PACKET_LOGGER, PackUiButton.Variant.SECONDARY, () -> {
                if (packetLoggerOverlay != null) packetLoggerOverlay.toggle();
            }).setGrowX(true),
            actionButton("Packets", PackUiAssets.ICON_FILTER, PackUiButton.Variant.SECONDARY, () -> {
                if (customFilterOverlay != null) customFilterOverlay.toggle();
            }).setGrowX(true)
        );
        addPairRow(mainMenuSection.content(),
            actionButton("Server Info", PackUiAssets.ICON_SERVER_INFO, PackUiButton.Variant.SECONDARY, () -> {
                if (serverInfoOverlay != null) serverInfoOverlay.toggle();
            }).setGrowX(true),
            actionButton("Settings", PackUiAssets.ICON_KEYBINDS, PackUiButton.Variant.SECONDARY, () -> {
                if (keybindOverlay != null) keybindOverlay.toggle();
            }).setGrowX(true)
        );

        addSubCategory("PACKET", PackUiAssets.ICON_PACKET_CATEGORY);
        sendButton = actionButton("Send Off", null, PackUiButton.Variant.SECONDARY, this::toggleSendPackets).setGrowX(true);
        delayButton = actionButton("Delay Off", null, PackUiButton.Variant.SECONDARY, this::toggleDelayPackets).setGrowX(true);
        addPairRow(mainMenuSection.content(), sendButton, delayButton);
        addPairRow(mainMenuSection.content(),
            actionButton("Flush", null, PackUiButton.Variant.SECONDARY, this::flushQueue).setGrowX(true),
            actionButton("Clear", null, PackUiButton.Variant.DANGER, this::clearQueue).setGrowX(true)
        );

        addSubCategory("SCREEN", PackUiAssets.ICON_SCREEN_CATEGORY);
        addPairRow(mainMenuSection.content(),
            actionButton("Close", null, PackUiButton.Variant.SECONDARY,
                closeWithoutPacketAction != null ? closeWithoutPacketAction : () -> PackUtilGuiActions.closeCurrentScreen(MC, false)).setGrowX(true),
            actionButton("De-sync", null, PackUiButton.Variant.DANGER, desyncAction != null ? desyncAction : this::sendDesync).setGrowX(true)
        );
        addPairRow(mainMenuSection.content(),
            actionButton("Save", null, PackUiButton.Variant.SECONDARY, this::saveGui).setGrowX(true),
            actionButton("Load", null, PackUiButton.Variant.SECONDARY, this::loadGui).setGrowX(true)
        );
        addPairRow(mainMenuSection.content(),
                actionButton("Title", null, PackUiButton.Variant.SECONDARY, PackUtilGuiClipboardUtil::copyGuiTitleJson).setGrowX(true),
            actionButton("Disc+Send", null, PackUiButton.Variant.DANGER, this::disconnectAndSend).setGrowX(true)
        );

        addSubCategory("CHAT", PackUiAssets.ICON_CHAT_CATEGORY);
        chatField = new PackUiTextField()
            .setGrowX(true)
            .setFieldHeight(chatHeight())
            .setPreferredWidth(chatMinWidth())
            .setMinWidth(chatMinWidth())
            .setHorizontalPadding(6)
            .setPlaceholder("Type message or /command...")
            .setHistoryNavigationEnabled(true)
            .setOnSubmit(this::submitChat);
        mainMenuSection.content().add(chatField);

        PackUiCompactRow syncRow = new PackUiCompactRow().setGap(syncRowGap()).setPadding(PackUiInsets.NONE).setUnderlineOnHover(false);
        syncRow.setGrowX(true);
        revMetric = syncRow.add(new PackUiMetricLabel("Rev: ", "--").setKeyColor(0xFFB79E9E).setValueColor(0xFFFF4A4A).setGrowX(true));
        syncMetric = syncRow.add(new PackUiMetricLabel("SyncID: ", "--").setKeyColor(0xFFB79E9E).setValueColor(0xFFF3ECE7).setGrowX(true));
        slotMetric = syncRow.add(new PackUiMetricLabel("Slot: ", "--").setKeyColor(0xFFB79E9E).setValueColor(0xFF8FD7FF).setGrowX(true));
        mainMenuSection.content().add(syncRow);
    }

    private void addSubCategory(String label, Identifier icon) {
        if (!mainMenuSection.content().children().isEmpty()) {
            mainMenuSection.content().add(new PackUiSpacer(0, sectionGap()));
        }
        mainMenuSection.content().add(new PackUiIconLabel(label, icon).setTone(PackUiTone.LABEL).setIconSize(labelIconSize()));
    }

    private PackUiButton addFullButton(autismclient.gui.packui.PackUiColumn parent, String label, Identifier icon, Runnable action) {
        PackUiButton button = actionButton(label, icon, PackUiButton.Variant.SECONDARY, action).setGrowX(true);
        parent.add(button);
        return button;
    }

    private void addPairRow(autismclient.gui.packui.PackUiColumn parent, PackUiButton left, PackUiButton right) {
        PackUiCompactRow row = new PackUiCompactRow().setGap(rowGap()).setPadding(PackUiInsets.NONE).setUnderlineOnHover(false);
        row.add(left);
        row.add(right);
        row.setGrowX(true);
        parent.add(row);
    }

    private PackUiButton actionButton(String label, Identifier icon, PackUiButton.Variant variant, Runnable action) {
        PackUiButton button = new PackUiButton(label, variant, action)
            .setGrowX(false)
            .setButtonHeight(buttonHeight())
            .setHorizontalPadding(buttonPadding())
            .setMinWidth(requiredButtonWidth(label, icon))
            .setTextYOffset(0);
        if (icon != null) {
            button.setLeadingIcon(icon)
                .setContentAlignment(PackUiButton.ContentAlignment.START)
                .setIconSize(buttonIconSize())
                .setIconGap(buttonIconGap());
        }
        return button;
    }

    @Override
    public String getOverlayId() {
        return "packutil-launcher";
    }

    @Override
    public int getMinWidth() {
        return computePanelWidth();
    }

    @Override
    public int getMinHeight() {
        return minimumPanelHeight();
    }

    @Override
    public PackUtilWindowLayout getBounds() {
        return new PackUtilWindowLayout(panelX, panelY, panelWidth, panelHeight, visible, !mainMenuSection.isExpanded());
    }

    @Override
    public void setBounds(PackUtilWindowLayout bounds) {
        if (bounds == null) return;
        PackUtilWindowLayout clamped = clampToViewport(bounds);
        panelX = clamped.x;
        panelY = clamped.y;
        panelWidth = clamped.width;
        panelHeight = clamped.height;
        visible = clamped.visible;
        if (mainMenuSection != null) {
            mainMenuSection.syncExpanded(!clamped.collapsed);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        PackUtilSharedState shared = PackUtilSharedState.get();
        if (sendButton != null) {
            boolean sending = shared.shouldSendGuiPackets();
            sendButton.setText("Send " + (sending ? "On" : "Off"));
            sendButton.setVariant(sending ? PackUiButton.Variant.SUCCESS : PackUiButton.Variant.SECONDARY);
        }
        if (delayButton != null) {
            boolean delaying = shared.shouldDelayGuiPackets();
            delayButton.setText("Delay " + (delaying ? "On" : "Off"));
            delayButton.setVariant(delaying ? PackUiButton.Variant.PRIMARY : PackUiButton.Variant.SECONDARY);
        }
        updateSyncMetrics();

        panelWidth = computePanelWidth();
        PackUiViewport viewport = surface.viewport();
        float uiMouseX = viewport.toUiX(mouseX);
        float uiMouseY = viewport.toUiY(mouseY);
        PackUiRenderContext metrics = new PackUiRenderContext(context, MC.font, viewport, theme, uiMouseX, uiMouseY, delta);
        panelHeight = Math.max(getMinHeight(), Math.round(panelNode.preferredHeight(metrics, panelWidth)));
        PackUtilWindowLayout clamped = clampToViewport(new PackUtilWindowLayout(
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            visible,
            mainMenuSection != null && !mainMenuSection.isExpanded()
        ));
        panelX = clamped.x;
        panelY = clamped.y;
        panelWidth = clamped.width;
        panelHeight = clamped.height;
        panelNode.setActive(true);
        panelNode.setBounds(panelX, panelY, panelWidth, panelHeight);
        surface.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        PackUiViewport viewport = surface.viewport();
        float uiMouseX = viewport.toUiX(mouseX);
        float uiMouseY = viewport.toUiY(mouseY);

        if (button == 0 && isOverMainMenuHeader(uiMouseX, uiMouseY) && mainMenuSection != null) {
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

        if (surface.mouseClicked(mouseX, mouseY, button)) return true;
        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            saveLayout();
            return true;
        }
        return surface.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            PackUiViewport viewport = surface.viewport();
            float uiMouseX = viewport.toUiX(mouseX);
            float uiMouseY = viewport.toUiY(mouseY);
            PackUtilWindowLayout clamped = clampToViewport(new PackUtilWindowLayout(
                Math.round(uiMouseX - dragOffsetX),
                Math.round(uiMouseY - dragOffsetY),
                panelWidth,
                panelHeight,
                visible,
                mainMenuSection != null && !mainMenuSection.isExpanded()
            ));
            panelX = clamped.x;
            panelY = clamped.y;
            dragMoved = dragMoved
                || Math.abs(uiMouseX - pressStartUiX) >= HEADER_CLICK_DRAG_THRESHOLD
                || Math.abs(uiMouseY - pressStartUiY) >= HEADER_CLICK_DRAG_THRESHOLD
                || panelX != pressStartPanelX
                || panelY != pressStartPanelY;
            return true;
        }
        return surface.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return surface.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return surface.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return surface.charTyped(chr, modifiers);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) {
            surface.clearFocusedTextInputs();
            dragging = false;
            dragMoved = false;
        } else if (mainMenuSection != null) {
            mainMenuSection.syncExpanded(mainMenuSection.isExpanded());
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible) return false;
        PackUiViewport viewport = surface.viewport();
        float uiX = viewport.toUiX(mouseX);
        float uiY = viewport.toUiY(mouseY);
        return uiX >= panelX && uiX < panelX + panelWidth
            && uiY >= panelY && uiY < panelY + panelHeight;
    }

    @Override
    public boolean isOverDragBar(double mouseX, double mouseY) {
        if (!visible) return false;
        PackUiViewport viewport = surface.viewport();
        return isOverMainMenuHeader(viewport.toUiX(mouseX), viewport.toUiY(mouseY));
    }

    @Override
    public boolean usesSharedHeaderClickCollapse() {
        return true;
    }

    @Override
    public boolean isCollapsed() {
        return mainMenuSection != null && !mainMenuSection.isExpanded();
    }

    @Override
    public void setCollapsed(boolean collapsed) {
        if (mainMenuSection != null) {
            mainMenuSection.syncExpanded(!collapsed);
        }
    }

    @Override
    public boolean hasTextFieldFocused() {
        return surface.hasFocusedTextInput();
    }

    @Override
    public void clearTextFieldFocus() {
        surface.clearFocusedTextInputs();
    }

    private boolean isOverMainMenuHeader(float uiMouseX, float uiMouseY) {
        return mainMenuSection != null
            && uiMouseX >= panelX
            && uiMouseX < panelX + panelWidth
            && uiMouseY >= panelY
            && uiMouseY < panelY + sectionHeaderHeight();
    }

    private PackUtilWindowLayout clampToViewport(PackUtilWindowLayout bounds) {
        PackUiViewport viewport = surface.viewport();
        int width = Math.max(getMinWidth(), Math.min(bounds.width, Math.round(viewport.uiWidth())));
        int minHeight = bounds.collapsed ? sectionHeaderHeight() : getMinHeight();
        int height = Math.max(minHeight, Math.min(bounds.height, Math.round(viewport.uiHeight())));
        int x = Math.max(0, Math.min(bounds.x, Math.max(0, Math.round(viewport.uiWidth()) - width)));
        int y = Math.max(0, Math.min(bounds.y, Math.max(0, Math.round(viewport.uiHeight()) - sectionHeaderHeight())));
        return new PackUtilWindowLayout(x, y, width, height, bounds.visible, bounds.collapsed);
    }

    private int computePanelWidth() {
        if (MC.font == null) return minPanelWidth();
        int pairWidth = minPairWidth();
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Macros", PackUiAssets.ICON_MACROS));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Fabricator", PackUiAssets.ICON_FABRICATOR));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("LAN Sync", PackUiAssets.ICON_LANSYNC));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Queue", PackUiAssets.ICON_PACKET_Q_EDITOR));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Logger", PackUiAssets.ICON_PACKET_LOGGER));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Packets", PackUiAssets.ICON_FILTER));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Server Info", PackUiAssets.ICON_SERVER_INFO));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Settings", PackUiAssets.ICON_KEYBINDS));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Send Off", null));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Delay Off", null));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Flush", null));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Clear", null));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Close", null));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("De-sync", null));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Save", null));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Load", null));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Title", null));
        pairWidth = Math.max(pairWidth, requiredButtonWidth("Disc+Send", null));

        int chatWidth = Math.max(chatMinWidth(), requiredTextFieldWidth("Type message or /command..."));
        int headerWidth = PackUiText.width(MC.font, "MAIN MENU", theme.fontFor(PackUiTone.LABEL), theme.color(PackUiTone.LABEL))
            + labelIconSize() + buttonIconGap() + headerReserveWidth();
        int subLabelWidth = Math.max(requiredIconLabelWidth("PACKET"), Math.max(requiredIconLabelWidth("SCREEN"), requiredIconLabelWidth("CHAT")));
        int contentWidth = Math.max(headerWidth, Math.max(subLabelWidth, Math.max((pairWidth * 2) + rowGap(), chatWidth)));
        return Math.max(minPanelWidth(), contentWidth + (sectionPadding() * 2) + panelWidthReserve());
    }

    private int requiredButtonWidth(String label, Identifier icon) {
        if (MC.font == null) return minPairWidth();
        int width = PackUiText.width(MC.font, label, theme.fontFor(PackUiTone.BODY), theme.color(PackUiTone.BODY)) + (buttonPadding() * 2) + buttonOutlineReserve();
        if (icon != null) width += buttonIconSize() + buttonIconGap();
        return width;
    }

    private int requiredIconLabelWidth(String label) {
        return PackUiText.width(MC.font, label, theme.fontFor(PackUiTone.LABEL), theme.color(PackUiTone.LABEL)) + labelIconSize() + buttonIconGap() + buttonOutlineReserve();
    }

    private int requiredTextFieldWidth(String placeholder) {
        return PackUiText.width(MC.font, placeholder, theme.fontFor(PackUiTone.BODY), theme.color(PackUiTone.MUTED)) + textFieldExtraWidth() + buttonOutlineReserve();
    }

    private int sectionPadding() {
        return 3;
    }

    private int sectionGap() {
        return 2;
    }

    private int rowGap() {
        return 2;
    }

    private int buttonHeight() {
        return 15;
    }

    private int buttonPadding() {
        return 6;
    }

    private int buttonIconSize() {
        return 12;
    }

    private int buttonIconGap() {
        return 3;
    }

    private int labelIconSize() {
        return 11;
    }

    private int minPairWidth() {
        return 54;
    }

    private int minPanelWidth() {
        return 152;
    }

    private int minimumPanelHeight() {
        return 56;
    }

    private int sectionHeaderHeight() {
        return 20;
    }

    private int sectionContentTopGap() {
        return 2;
    }

    private int chatHeight() {
        return 16;
    }

    private int chatMinWidth() {
        return 124;
    }

    private int syncRowGap() {
        return 4;
    }

    private int headerReserveWidth() {
        return 16;
    }

    private int textFieldExtraWidth() {
        return 10;
    }

    private int buttonOutlineReserve() {
        return 2;
    }

    private int panelWidthReserve() {
        return 2;
    }

        private void toggleSendPackets() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        boolean newValue = !shared.shouldSendGuiPackets();
        PackUtilModule.get().applySendGuiPacketsUiBehavior(newValue);
        PackUtilClientMessaging.sendPrefixed("Packets: " + (newValue ? "enabled" : "disabled"));
    }

    private void toggleDelayPackets() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        boolean newValue = !shared.shouldDelayGuiPackets();
        PackUtilModule module = PackUtilModule.get();
        int sent = module.applyDelayGuiPacketsUiBehavior(newValue);
        if (newValue) {
            PackUtilClientMessaging.sendPrefixed("Packet delay: enabled");
        } else if (sent > 0) {
            String completionMessage = "Packet delay: disabled. Sent " + sent + " queued packets.";
            if (shared.isStaggering()) shared.setPendingQueueCompletionMessage(completionMessage);
            else PackUtilClientMessaging.sendPrefixed(completionMessage);
        } else if (!module.shouldFlushQueueOnDelayDisable()
            && (!shared.getDelayedPackets().isEmpty() || !shared.getStaggeredQueue().isEmpty())) {
            PackUtilClientMessaging.sendPrefixed("Packet delay: disabled. Queue kept.");
        } else {
            PackUtilClientMessaging.sendPrefixed("Packet delay: disabled. Queue empty.");
        }
    }

    private void flushQueue() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        int count = PackUtilModule.get().flushQueuedPacketsUiBehavior();
        if (count > 0) {
            if (shared.isStaggering()) shared.setPendingQueueCompletionMessage("Sent " + count + " packets.");
            else PackUtilClientMessaging.sendPrefixed("Sent " + count + " packets.");
        } else {
            PackUtilClientMessaging.sendPrefixed("Queue empty.");
        }
    }

    private void clearQueue() {
        int count = PackUtilModule.get().clearQueuedPacketsUiBehavior();
        PackUtilClientMessaging.sendPrefixed(count > 0 ? "Cleared " + count + " packets." : "Queue empty.");
    }

    private void sendDesync() {
        if (!PackUtilGuiActions.desyncCurrentScreen(MC)) {
            PackUtilClientMessaging.sendPrefixed("Failed to desync: no open networked GUI.");
        }
    }

    private void saveGui() {
        PackUtilGuiActions.saveCurrentGui(MC);
    }

    private void loadGui() {
        if (PackUtilModule.get().restoreSavedScreenUiBehavior()) {
            PackUtilClientMessaging.sendPrefixed("GUI restored.");
        } else {
            PackUtilClientMessaging.sendPrefixed("No stored GUI.");
        }
    }

    private void disconnectAndSend() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        PackUtilModule.get().setDelayGuiPackets(false);
        if (MC.getConnection() != null) {
            shared.flushDelayedPackets(MC.getConnection());
            MC.getConnection().getConnection().disconnect(Component.literal("Disconnecting (PackUtil)"));
        }
    }

    private void submitChat(String raw) {
        if (raw == null) return;
        String message = raw.trim();
        if (message.isEmpty() || MC.getConnection() == null) return;

        if (message.startsWith("/") && message.length() > 1) {
            MC.getConnection().sendCommand(message.substring(1));
        } else {
            MC.getConnection().sendChat(message);
        }

        if (chatField != null) {
            chatField.addHistoryEntry(message);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            mc.commandHistory().addCommand(message);
        }
        if (chatField != null) {
            chatField.setText("");
            chatField.setFocused(false);
        }
    }

    private void updateSyncMetrics() {
        String revisionText = "--";
        String syncIdText = "--";
        String slotText = "--";

        if (MC.player != null && MC.player.containerMenu != null) {
            revisionText = Integer.toString(MC.player.containerMenu.getStateId());
            syncIdText = Integer.toString(MC.player.containerMenu.containerId);
            if (MC.screen instanceof AbstractContainerScreen<?> handledScreen) {
                Slot focusedSlot = ((PackUtilHandledScreenAccessor) handledScreen).getFocusedSlot();
                if (focusedSlot != null) {
                    slotText = Integer.toString(PackUtilInventoryHelper.toUserVisibleSlot(MC, focusedSlot.index));
                }
            }
        }

        if (revMetric != null) revMetric.setValue(revisionText);
        if (syncMetric != null) syncMetric.setValue(syncIdText);
        if (slotMetric != null) slotMetric.setValue(slotText);
    }
}
