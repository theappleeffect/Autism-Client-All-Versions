package autismclient.util;

import autismclient.gui.packui.PackUiAssets;
import autismclient.gui.packui.PackUiListRenderer;
import autismclient.gui.packui.PackUiOverlayButton;
import autismclient.gui.packui.PackUiScrollbar;
import autismclient.gui.packui.PackUiSmoothScroll;
import autismclient.util.macro.CraftAction;
import autismclient.util.macro.ItemTarget;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class PackUtilFabricatorOverlay extends PackUtilOverlayBase {
    private static final Minecraft MC = Minecraft.getInstance();
    private static PackUtilFabricatorOverlay sharedOverlay;
    private int DEFAULT_PANEL_WIDTH = 236;
    private int LINE_HEIGHT = 14;
    private int CRAFT_LIST_HEIGHT = 84;
    private static final int CRAFT_LIST_ROWS = 6;
    private int CRAFT_PLAN_HEIGHT = 52;
    private static final int CRAFT_PLAN_ROWS = 4;
    private static final int SCROLLBAR_GUTTER = 8;

    private enum FabricatorAction {
        CLICK("Click", "Use Left or Right on the resolved slot.") {
            @Override boolean usesClickSelector() { return true; }
            @Override PackUtilDropAction toPacketAction(boolean dropWholeStack) { return PackUtilDropAction.PICKUP; }
        },
        QUICK_MOVE("Quick Move", "Shift-click the resolved slot.") {
            @Override PackUtilDropAction toPacketAction(boolean dropWholeStack) { return PackUtilDropAction.QUICK_MOVE; }
        },
        PICKUP_ALL("Pickup All", "Double-click the resolved slot. Usually needs you to hold a matching stack first.") {
            @Override PackUtilDropAction toPacketAction(boolean dropWholeStack) { return PackUtilDropAction.PICKUP_ALL; }
        },
        CRAFT_RESULT("Craft", "Choose a craftable item, set the total amount, then send, queue, or add it to a macro.") {
            @Override boolean isCraftAction() { return true; }
            @Override PackUtilDropAction toPacketAction(boolean dropWholeStack) { return PackUtilDropAction.QUICK_MOVE; }
        },
        DROP("Drop", "Drop from the resolved slot. If slot and item are both set, both must match.") {
            @Override boolean usesDropToggle() { return true; }
            @Override PackUtilDropAction toPacketAction(boolean dropWholeStack) {
                return dropWholeStack ? PackUtilDropAction.DROP_STACK : PackUtilDropAction.DROP_ITEM;
            }
            @Override String getDescription(boolean dropWholeStack) {
                return dropWholeStack
                    ? "Drops the whole stack from the selected slot."
                    : "Drops one item at a time from the selected slot.";
            }
        };

        final String displayName;
        final String description;

        FabricatorAction(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        boolean usesClickSelector() { return false; }
        boolean usesDropToggle() { return false; }
        boolean isCraftAction() { return false; }
        String getDescription(boolean dropWholeStack) { return description; }
        abstract PackUtilDropAction toPacketAction(boolean dropWholeStack);
    }

    private int panelX = 220;
    private int panelY = 5;
    private int PANEL_WIDTH = DEFAULT_PANEL_WIDTH;
    private int PANEL_HEIGHT = 392;
    private int FIELD_WIDTH = 100;
    private int BUTTON_HEIGHT = 20;
    private int LABEL_WIDTH = 64;

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
    private AbstractContainerScreen<?> parentScreen;
    private final List<PackUtilChatField> textFields = new ArrayList<>();

    private PackUtilChatField slotField;
    private PackUtilChatField itemNameField;
    private PackUtilChatField timesField;
    private PackUtilChatField craftSearchField;
    private int currentActionIndex = 0;
    private final FabricatorAction[] actions = FabricatorAction.values();

    private int currentButtonIndex = 0;
    private final String[] buttonTypes = {"Left Click", "Right Click"};
    private boolean dropWholeStack = false;

    private Integer selectedSlotId = null;
    private String statusMessage = "";
    private ChatFormatting statusColor = ChatFormatting.GRAY;
    private long statusExpiresAtMs = 0L;
    private final List<PackUtilCraftingHelper.CraftableRecipeOption> craftableRecipes = new ArrayList<>();
    private List<PackUtilCraftingHelper.CraftableRecipeOption> filteredCraftableRecipes = new ArrayList<>();
    private int craftScrollOffset = 0;
    private final PackUiSmoothScroll craftListScrollState = new PackUiSmoothScroll();
    private PackUtilCraftingHelper.CraftableRecipeOption selectedCraftOption = null;
    private ItemStack selectedCraftResult = ItemStack.EMPTY;
    private final List<CraftAction.CraftEntry> plannedCraftEntries = new ArrayList<>();
    private int craftPlanSelectedIndex = -1;
    private int craftPlanScrollOffset = 0;
    private final PackUiSmoothScroll craftPlanScrollState = new PackUiSmoothScroll();
    private long lastCraftRefreshAt = 0L;
    private volatile boolean craftExecutionInProgress = false;
    private boolean craftUseMaxAmount = false;
    private int activeScrollbarDrag = 0;
    private int scrollbarGrabOffset = 0;

    private static final int SCROLLBAR_NONE = 0;
    private static final int SCROLLBAR_CRAFT_PLAN = 1;
    private static final int SCROLLBAR_CRAFT_LIST = 2;

    private String savedSlotValue = "0";
    private String savedItemNameValue = "";
    private ItemTarget savedItemTarget = new ItemTarget();
    private String savedTimesValue = "1";
    private String savedCraftSearchValue = "";
    private int savedCraftSelectedRecipeId = -1;
    private String savedCraftSelectedRecipeKey = "";

    private int getFieldWidth() {
        return Math.max(108, PANEL_WIDTH - LABEL_WIDTH - 26);
    }

    private int getBottomButtonY() {
        return panelY + PANEL_HEIGHT - BUTTON_HEIGHT - 24;
    }

    private int getStatusY() {
        return getBottomButtonY() - 34;
    }

    private int getActionInfoY() {
        return getBottomButtonY() - 18;
    }

    private boolean isCraftMode() {
        return currentAction.isCraftAction();
    }

    private boolean showsTargetFields() {
        return !isCraftMode();
    }

    private int getActionRowY() {
        return panelY + 30;
    }

    private int getSlotRowY() {
        return getActionRowY() + 28;
    }

    private int getItemRowY() {
        return getSlotRowY() + 28;
    }

    private int getOptionRowY() {
        return getItemRowY() + 28;
    }

    private int getStandardTimesRowY() {
        return (isClickSelectorRelevant() || isDropModeRelevant()) ? getOptionRowY() + 28 : getItemRowY() + 28;
    }

    private int getCraftSearchY() {
        return getCraftSearchLabelY() + 12;
    }

    private int getCraftAmountRowY() {
        return getCraftSummaryY() + 16;
    }

    private int getCraftAmountToggleWidth() {
        return 78;
    }

    private int getCraftAmountFieldX() {
        return panelX + LABEL_WIDTH + 15;
    }

    private int getCraftAmountToggleX() {
        return getCraftAmountFieldX() + getCraftAmountFieldWidth() + 4;
    }

    private int getCraftAddButtonWidth() {
        return 44;
    }

    private int getCraftAmountFieldWidth() {
        return Math.max(48, getFieldWidth() - getCraftAmountToggleWidth() - getCraftAddButtonWidth() - 8);
    }

    private int getCraftAddButtonX() {
        return getCraftAmountToggleX() + getCraftAmountToggleWidth() + 4;
    }

    private int getCraftPlanHeaderY() {
        return getSlotRowY() + 3;
    }

    private int getCraftPlanListY() {
        return getSlotRowY() + 16;
    }

    private int getCraftSummaryY() {
        return getCraftPlanListY() + CRAFT_PLAN_HEIGHT + 4;
    }

    private int getCraftSearchLabelY() {
        return getCraftAmountRowY() + 24;
    }

    private boolean isClickSelectorRelevant() {
        return currentAction.usesClickSelector();
    }

    private boolean isDropModeRelevant() {
        return currentAction.usesDropToggle();
    }

    private int getEffectiveButton() {
        if (currentAction.usesDropToggle()) {
            return dropWholeStack ? 1 : 0;
        }
        PackUtilDropAction packetAction = currentAction.toPacketAction(dropWholeStack);
        return packetAction.usesFixedButton() ? packetAction.getButton() : currentButtonIndex;
    }

    private String getClickSelectorLabel() {
        if (isClickSelectorRelevant()) return buttonTypes[currentButtonIndex];
        if (isDropModeRelevant()) return dropWholeStack ? "Whole Stack" : "Single Item";
        return "Auto";
    }

    private void drawOverlayButton(GuiGraphics context, int x, int y, int w, int h, String label, PackUiOverlayButton.Variant variant, boolean enabled, int mouseX, int mouseY) {
        PackUiOverlayButton button = PackUiOverlayButton.create(x, y, w, h, Component.literal(label), ignored -> {});
        button.setWidth(w);
        button.setVariant(variant);
        button.active = enabled;
        PackUiOverlayButton.renderStyled(context, MC.font, button, mouseX, mouseY);
    }

    public PackUtilFabricatorOverlay(AbstractContainerScreen<?> parentScreen) {
        this.parentScreen = parentScreen;
        applyPresetMetrics();
        this.PANEL_WIDTH = DEFAULT_PANEL_WIDTH;
        this.PANEL_HEIGHT = craftModePanelHeight();
    }

    public static synchronized PackUtilFabricatorOverlay getSharedOverlay(AbstractContainerScreen<?> parentScreen) {
        if (sharedOverlay == null) {
            sharedOverlay = new PackUtilFabricatorOverlay(parentScreen);
            sharedOverlay.restoreState();
        } else {
            sharedOverlay.parentScreen = parentScreen;
        }
        return sharedOverlay;
    }

    @Override
    public int getMinWidth() {
        return DEFAULT_PANEL_WIDTH;
    }

    @Override
    public int getMinHeight() {
        return standardPanelHeight();
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
        PANEL_WIDTH = DEFAULT_PANEL_WIDTH;
        PANEL_HEIGHT = Math.max(getMinHeight(), clamped.height);
        visible = clamped.visible;
        collapsed = clamped.collapsed;
        if (visible) initWidgets();
    }

    public void toggle() {
        setVisible(!visible);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            initWidgets();
            PackUtilOverlayManager.get().bringToFront(this);
        } else {
            clearWidgets();
        }
        saveState();
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
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible) return false;
        int panelHeight = collapsed ? 18 : PANEL_HEIGHT;
        return mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
               mouseY >= panelY && mouseY <= panelY + panelHeight;
    }

    @Override
    public boolean isOverDragBar(double mouseX, double mouseY) {
        if (!visible) return false;
        PackUtilWindowLayout bounds = getBounds();
        return mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
               mouseY >= panelY && mouseY <= panelY + 18 &&
               !isOverWindowControl(mouseX, mouseY, bounds);
    }

    @Override
    public boolean hasTextFieldFocused() {
        for (PackUtilChatField field : textFields) {
            if (field.isFocused()) return true;
        }
        return false;
    }

    @Override
    public void clearTextFieldFocus() {
        for (PackUtilChatField field : textFields) {
            field.setFocused(false);
        }
    }

    public void saveState() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        shared.setFabricatorOverlayVisible(visible);
        shared.setFabricatorOverlayX(panelX);
        shared.setFabricatorOverlayY(panelY);

        shared.setFabricatorSlotValue(savedSlotValue);
        shared.setFabricatorItemNameValue(savedItemNameValue);
        shared.setFabricatorTimesValue(savedTimesValue);
        shared.setFabricatorActionIndex(currentActionIndex);
        shared.setFabricatorButtonIndex(currentButtonIndex);
        shared.setFabricatorDropWholeStack(dropWholeStack);
        shared.setFabricatorCraftUseMaxAmount(craftUseMaxAmount);
        shared.setFabricatorCraftSearchValue(savedCraftSearchValue);
        shared.setFabricatorCraftSelectedRecipeId(savedCraftSelectedRecipeId);
        shared.setFabricatorCraftSelectedRecipeKey(savedCraftSelectedRecipeKey);
        shared.setFabricatorCraftScrollOffset(craftScrollOffset);
        shared.setFabricatorCraftPlanEntries(plannedCraftEntries);
        shared.setFabricatorCraftPlanSelectedIndex(craftPlanSelectedIndex);
        shared.setFabricatorCraftPlanScrollOffset(craftPlanScrollOffset);
        saveLayout();
    }

    public void restoreState() {
        PackUtilSharedState shared = PackUtilSharedState.get();
        restoreLayout();
        this.visible = shared.isFabricatorOverlayVisible();
        this.panelX = shared.getFabricatorOverlayX();
        this.panelY = shared.getFabricatorOverlayY();

        this.savedSlotValue = shared.getFabricatorSlotValue();
        this.savedItemNameValue = shared.getFabricatorItemNameValue();
        this.savedTimesValue = shared.getFabricatorTimesValue();
        this.currentActionIndex = shared.getFabricatorActionIndex();
        this.currentButtonIndex = shared.getFabricatorButtonIndex();
        this.dropWholeStack = shared.isFabricatorDropWholeStack();
        this.craftUseMaxAmount = shared.isFabricatorCraftUseMaxAmount();
        this.savedCraftSearchValue = shared.getFabricatorCraftSearchValue();
        this.savedCraftSelectedRecipeId = shared.getFabricatorCraftSelectedRecipeId();
        this.savedCraftSelectedRecipeKey = shared.getFabricatorCraftSelectedRecipeKey();
        this.craftScrollOffset = shared.getFabricatorCraftScrollOffset();
        this.craftListScrollState.restore(this.craftScrollOffset * LINE_HEIGHT);
        this.plannedCraftEntries.clear();
        this.plannedCraftEntries.addAll(shared.getFabricatorCraftPlanEntries());
        this.craftPlanSelectedIndex = shared.getFabricatorCraftPlanSelectedIndex();
        this.craftPlanScrollOffset = shared.getFabricatorCraftPlanScrollOffset();
        this.craftPlanScrollState.restore(this.craftPlanScrollOffset * 13);
        this.selectedCraftOption = null;
        this.selectedCraftResult = ItemStack.EMPTY;

        if (currentActionIndex < 0 || currentActionIndex >= actions.length) {
            currentActionIndex = 0;
        }
        this.currentAction = actions[currentActionIndex];
        if (currentButtonIndex < 0 || currentButtonIndex >= buttonTypes.length) {
            currentButtonIndex = 0;
        }

        if (visible) {
            initWidgets();
        }
    }

    private void initWidgets() {
        clearWidgets();

        if (!isResizing && !isDragging) {
            PANEL_HEIGHT = isCraftMode() ? craftModePanelHeight() : standardPanelHeight();
        }

        int fieldX = panelX + LABEL_WIDTH + 15;
        int fieldWidth = getFieldWidth();
        currentAction = actions[currentActionIndex];

        slotField = new PackUtilChatField(MC, MC.font, fieldX, getSlotRowY(), fieldWidth, 16, false);
        slotField.setPlaceholder(Component.literal("Optional slot - right-click to fill"));
        slotField.setText(savedSlotValue);
        slotField.setMaxLength(5);
        slotField.setChangedListener(text -> {
            savedSlotValue = text;
            clearStatus();

            try {
                int stableSlot = Integer.parseInt(text);
                int slotId = PackUtilInventoryHelper.resolveConfiguredHandlerSlot(MC, stableSlot);
                ItemTarget slotTarget = getItemTargetFromSlot(slotId);
                String itemName = slotTarget.editorText();
                if (!itemName.isEmpty()) {
                    savedItemTarget = slotTarget;
                    savedItemNameValue = itemName;
                    if (itemNameField != null) {
                        itemNameField.setText(itemName);
                    }
                }
            } catch (NumberFormatException ignored) {}
        });
        if (showsTargetFields()) {
            textFields.add(slotField);
        }

        itemNameField = new PackUtilChatField(MC, MC.font, fieldX, getItemRowY(), fieldWidth, 16, false);
        itemNameField.setPlaceholder(Component.literal("Optional item match").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)));
        itemNameField.setText(savedItemNameValue);
        itemNameField.setMaxLength(50);
        itemNameField.setDisplayTextProvider(value -> {
            Component rich = savedItemTarget == null ? null : savedItemTarget.editorComponent(value);
            return rich != null && value != null && value.equals(rich.getString()) ? rich.copy() : null;
        });
        itemNameField.setChangedListener(text -> {
            savedItemNameValue = text;
            if (savedItemTarget != null && !text.equals(savedItemTarget.editorText())) {
                savedItemTarget = savedItemTarget.hasRichText()
                        ? savedItemTarget.withEditedDisplay(text)
                        : ItemTarget.fromLegacyEntry(text);
            }
            clearStatus();
        });
        if (showsTargetFields()) {
            textFields.add(itemNameField);
        }

        int timesY = isCraftMode() ? getCraftAmountRowY() : getStandardTimesRowY();
        int timesX = isCraftMode() ? getCraftAmountFieldX() : fieldX;
        int timesWidth = isCraftMode() ? getCraftAmountFieldWidth() : fieldWidth;
        timesField = new PackUtilChatField(MC, MC.font, timesX, timesY, timesWidth, 16, false);
        timesField.setPlaceholder(Component.literal("1"));
        timesField.setText(savedTimesValue);
        timesField.setMaxLength(5);
        timesField.setEditable(!(isCraftMode() && craftUseMaxAmount));
        timesField.setChangedListener(text -> {
            savedTimesValue = text;
            if (isCraftMode() && craftPlanSelectedIndex >= 0 && craftPlanSelectedIndex < plannedCraftEntries.size()) {
                try {
                    int val = Integer.parseInt(text.replaceAll("[^0-9]", ""));
                    plannedCraftEntries.get(craftPlanSelectedIndex).amount = Math.max(1, val);
                } catch (Exception ignored) {}
            }
            clearStatus();
        });
        textFields.add(timesField);

        if (isCraftMode()) {
            int searchY = getCraftSearchY();
            craftSearchField = new PackUtilChatField(MC, MC.font, panelX + 10, searchY, PANEL_WIDTH - 20, 16, false);
            craftSearchField.setPlaceholder(Component.literal("Search crafting recipes..."));
            craftSearchField.setText(savedCraftSearchValue);
            craftSearchField.setChangedListener(text -> {
                savedCraftSearchValue = text;
                clearStatus();
                updateCraftRecipeFilter(text, true);
                saveState();
            });
            textFields.add(craftSearchField);
            refreshCraftableRecipes(true);
        } else {
            craftSearchField = null;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        if (false && button == 0 && !collapsed && isResizeActive(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT)) {
            isResizing = true;
            resizeStartMouseX = mouseX;
            resizeStartMouseY = mouseY;
            resizeStartWidth = PANEL_WIDTH;
            resizeStartHeight = PANEL_HEIGHT;
            return true;
        }

        if (button == 0 && mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
            mouseY >= panelY && mouseY <= panelY + 20) {
            PackUtilWindowLayout bounds = new PackUtilWindowLayout(panelX, panelY, PANEL_WIDTH, collapsed ? 18 : PANEL_HEIGHT, visible, collapsed);

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
            for (PackUtilChatField field : textFields) {
                field.setFocused(false);
            }
            return true;
        }

        if (collapsed) return false;

        MouseButtonEvent click = new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));

        boolean clickedField = false;

        for (PackUtilChatField field : textFields) {
            boolean wasClicked = field.mouseClicked(click, false);
            if (wasClicked) {
                field.setFocused(true);
                clickedField = true;
            } else {
                field.setFocused(false);
            }
        }

        if (clickedField) return true;

        if (isCraftMode()) {
            refreshCraftableRecipes(false);
            int listX = getCraftListX();
            int listWidth = getCraftListWidth();
            int planContentWidth = getCraftPlanContentWidth();
            int recipeContentWidth = getCraftListContentWidth();
            int planHeaderY = getCraftPlanHeaderY();
            int planListY = getCraftPlanListY();
            int listY = getCraftListY();

            if (button == 0) {
                PackUiScrollbar.Metrics craftPlanScrollbar = getCraftPlanScrollbarMetrics();
                if (craftPlanScrollbar.hasScroll() && craftPlanScrollbar.contains((int) mouseX, (int) mouseY)) {
                    activeScrollbarDrag = SCROLLBAR_CRAFT_PLAN;
                    scrollbarGrabOffset = Math.max(0, (int) mouseY - craftPlanScrollbar.thumbY());
                    craftPlanScrollOffset = toRowScroll(craftPlanScrollbar, (int) mouseY, scrollbarGrabOffset, 13, Math.max(0, plannedCraftEntries.size() - CRAFT_PLAN_ROWS));
                    craftPlanScrollState.jumpTo(craftPlanScrollOffset * 13, Math.max(0, plannedCraftEntries.size() * 13 - CRAFT_PLAN_HEIGHT));
                    saveState();
                    return true;
                }

                PackUiScrollbar.Metrics craftListScrollbar = getCraftListScrollbarMetrics();
                if (craftListScrollbar.hasScroll() && craftListScrollbar.contains((int) mouseX, (int) mouseY)) {
                    activeScrollbarDrag = SCROLLBAR_CRAFT_LIST;
                    scrollbarGrabOffset = Math.max(0, (int) mouseY - craftListScrollbar.thumbY());
                    craftScrollOffset = toRowScroll(craftListScrollbar, (int) mouseY, scrollbarGrabOffset, LINE_HEIGHT, Math.max(0, filteredCraftableRecipes.size() - CRAFT_LIST_ROWS));
                    craftListScrollState.jumpTo(craftScrollOffset * LINE_HEIGHT, Math.max(0, filteredCraftableRecipes.size() * LINE_HEIGHT - CRAFT_LIST_HEIGHT));
                    saveState();
                    return true;
                }
            }

            if (!plannedCraftEntries.isEmpty()
                && mouseX >= panelX + PANEL_WIDTH - 74 && mouseX < panelX + PANEL_WIDTH - 10
                && mouseY >= planHeaderY - 2 && mouseY < planHeaderY + 10
                && button == 0) {
                plannedCraftEntries.clear();
                craftPlanSelectedIndex = -1;
                craftPlanScrollOffset = 0;
                craftPlanScrollState.jumpTo(0, 0);
                clearStatus();
                saveState();
                return true;
            }

            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= planListY && mouseY <= planListY + CRAFT_PLAN_HEIGHT && button == 0) {
                int maxPlanScrollPx = Math.max(0, plannedCraftEntries.size() * 13 - CRAFT_PLAN_HEIGHT);
                int drawPlanScroll = craftPlanScrollState.tick(0.0f, maxPlanScrollPx);
                int firstIndex = drawPlanScroll / 13;
                int rowY = planListY - (drawPlanScroll % 13);
                for (int index = firstIndex; index < plannedCraftEntries.size() && rowY < planListY + CRAFT_PLAN_HEIGHT; index++, rowY += 13) {
                    if (mouseY < rowY || mouseY >= rowY + 12) continue;
                    if (mouseX >= listX + planContentWidth - 16 && mouseX < listX + planContentWidth - 2) {
                        removePlannedCraftEntry(index);
                        return true;
                    }
                    if (mouseX >= listX && mouseX < listX + planContentWidth) {
                        if (craftPlanSelectedIndex == index) {
                            selectCraftPlanEntry(-1);
                        } else {
                            selectCraftPlanEntry(index);
                        }
                        for (PackUtilChatField field : textFields) field.setFocused(false);
                        return true;
                    }
                }
                if (mouseX >= listX && mouseX < listX + planContentWidth) {
                    selectCraftPlanEntry(-1);
                    for (PackUtilChatField field : textFields) field.setFocused(false);
                    return true;
                }
            }

            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + CRAFT_LIST_HEIGHT && button == 0) {
                int maxRecipeScrollPx = Math.max(0, filteredCraftableRecipes.size() * LINE_HEIGHT - CRAFT_LIST_HEIGHT);
                int drawRecipeScroll = craftListScrollState.tick(0.0f, maxRecipeScrollPx);
                int firstIndex = drawRecipeScroll / LINE_HEIGHT;
                int rowY = listY - (drawRecipeScroll % LINE_HEIGHT);
                for (int index = firstIndex; index < filteredCraftableRecipes.size() && rowY < listY + CRAFT_LIST_HEIGHT; index++, rowY += LINE_HEIGHT) {
                    if (mouseX >= listX && mouseX < listX + recipeContentWidth && mouseY >= rowY && mouseY < rowY + LINE_HEIGHT) {
                        selectCraftRecipe(filteredCraftableRecipes.get(index));
                        addOrUpdatePlannedCraftEntry();
                        for (PackUtilChatField field : textFields) field.setFocused(false);
                        return true;
                    }
                }
                if (mouseX >= listX && mouseX < listX + recipeContentWidth) {
                    clearCraftRecipeSelection();
                    for (PackUtilChatField field : textFields) field.setFocused(false);
                    saveState();
                    return true;
                }
            }
        }

        int fieldXR = panelX + LABEL_WIDTH + 15;
        int fieldWidth = getFieldWidth();
        int cycleY1 = getOptionRowY();
        int cycleY2 = getActionRowY();
        int cycleBtnH = 16;
        if (!isCraftMode() && (isClickSelectorRelevant() || isDropModeRelevant())
            && mouseY >= cycleY1 && mouseY < cycleY1 + cycleBtnH && mouseX >= fieldXR && mouseX < fieldXR + fieldWidth) {
            int halfWidth = (fieldWidth - 2) / 2;
            if (isClickSelectorRelevant()) {
                if (button == 0 || button == 1) {
                    currentButtonIndex = mouseX < fieldXR + halfWidth ? 0 : 1;
                    clearStatus();
                    saveState();
                }
                for (PackUtilChatField field : textFields) field.setFocused(false);
                return true;
            }
            if (isDropModeRelevant()) {
                if (button == 0 || button == 1) {
                    dropWholeStack = mouseX >= fieldXR + halfWidth + 2;
                    clearStatus();
                    saveState();
                }
                for (PackUtilChatField field : textFields) field.setFocused(false);
                return true;
            }
            for (PackUtilChatField field : textFields) field.setFocused(false);
            return true;
        }
        if (mouseY >= cycleY2 && mouseY < cycleY2 + cycleBtnH && mouseX >= fieldXR && mouseX < fieldXR + fieldWidth) {
            if (button == 1) cycleActionBackwards(); else cycleAction();
            for (PackUtilChatField field : textFields) field.setFocused(false);
            return true;
        }

        if (isCraftMode()) {
            int toggleX = getCraftAmountToggleX();
            int toggleY = getCraftAmountRowY();
            int toggleW = getCraftAmountToggleWidth();
            if (mouseY >= toggleY && mouseY < toggleY + 16 && mouseX >= toggleX && mouseX < toggleX + toggleW && (button == 0 || button == 1)) {
                craftUseMaxAmount = !craftUseMaxAmount;
                if (timesField != null) timesField.setEditable(!craftUseMaxAmount);
                if (craftPlanSelectedIndex >= 0 && craftPlanSelectedIndex < plannedCraftEntries.size()) {
                    plannedCraftEntries.get(craftPlanSelectedIndex).useMaxAmount = craftUseMaxAmount;
                }
                clearStatus();
                saveState();
                for (PackUtilChatField field : textFields) field.setFocused(false);
                return true;
            }
        }

        int btnY = getBottomButtonY();
        int btnArea = PANEL_WIDTH - 20;
        int gap = 2;
        int bw = (btnArea - gap * 2) / 3;
        int bx = panelX + 10;
        if (mouseY >= btnY && mouseY < btnY + BUTTON_HEIGHT && button == 0) {
            if (mouseX >= bx && mouseX < bx + bw) {
                send(false);
                for (PackUtilChatField field : textFields) field.setFocused(false);
                return true;
            }
            bx += bw + gap;
            if (mouseX >= bx && mouseX < bx + bw) {
                send(true);
                for (PackUtilChatField field : textFields) field.setFocused(false);
                return true;
            }
            bx += bw + gap;
            if (mouseX >= bx && mouseX < bx + bw) {

                PackUtilMacroEditorOverlay macroEditor = null;
                for (IPackUtilOverlay ov : PackUtilOverlayManager.get().getOverlays()) {
                    if (ov instanceof PackUtilMacroEditorOverlay) {
                        macroEditor = (PackUtilMacroEditorOverlay) ov;
                        break;
                    }
                }
                if (macroEditor != null && macroEditor.isVisible()) {
                    sendToMacro(macroEditor);
                } else {
                    setStatus("Ã‚Â§cOpen a macro editor first!", ChatFormatting.RED);
                }
                for (PackUtilChatField field : textFields) field.setFocused(false);
                return true;
            }
        }

        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && activeScrollbarDrag != SCROLLBAR_NONE) {
            activeScrollbarDrag = SCROLLBAR_NONE;
            saveState();
            return true;
        }
        if (button == 0) {
            if (isDragging || isResizing) {
                saveState();
            }
            isDragging = false;
            isResizing = false;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (activeScrollbarDrag == SCROLLBAR_CRAFT_PLAN) {
            craftPlanScrollOffset = toRowScroll(getCraftPlanScrollbarMetrics(), (int) mouseY, scrollbarGrabOffset, 13, Math.max(0, plannedCraftEntries.size() - CRAFT_PLAN_ROWS));
            craftPlanScrollState.jumpTo(craftPlanScrollOffset * 13, Math.max(0, plannedCraftEntries.size() * 13 - CRAFT_PLAN_HEIGHT));
            return true;
        }
        if (activeScrollbarDrag == SCROLLBAR_CRAFT_LIST) {
            craftScrollOffset = toRowScroll(getCraftListScrollbarMetrics(), (int) mouseY, scrollbarGrabOffset, LINE_HEIGHT, Math.max(0, filteredCraftableRecipes.size() - CRAFT_LIST_ROWS));
            craftListScrollState.jumpTo(craftScrollOffset * LINE_HEIGHT, Math.max(0, filteredCraftableRecipes.size() * LINE_HEIGHT - CRAFT_LIST_HEIGHT));
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
            initWidgets();
            return true;
        }
        if (isDragging) {
            PackUtilWindowLayout nextBounds = clampToScreen(this,
                new PackUtilWindowLayout((int) (mouseX - dragOffsetX), (int) (mouseY - dragOffsetY),
                    PANEL_WIDTH, PANEL_HEIGHT, visible, collapsed));
            panelX = nextBounds.x;
            panelY = nextBounds.y;
            initWidgets();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible || collapsed) return false;
        if (isCraftMode()) {
            int planX = getCraftListX();
            int planY = getCraftPlanListY();
            int listWidth = getCraftListWidth();
            if (mouseX >= planX && mouseX <= planX + listWidth && mouseY >= planY && mouseY <= planY + CRAFT_PLAN_HEIGHT) {
            int maxPlanScroll = Math.max(0, plannedCraftEntries.size() - CRAFT_PLAN_ROWS);
            craftPlanScrollOffset = Math.max(0, Math.min(maxPlanScroll, craftPlanScrollOffset - (int) Math.signum(amount)));
            craftPlanScrollState.setTarget(craftPlanScrollOffset * 13, Math.max(0, plannedCraftEntries.size() * 13 - CRAFT_PLAN_HEIGHT));
            saveState();
            return true;
        }

            int listX = getCraftListX();
            int listY = getCraftListY();
            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + CRAFT_LIST_HEIGHT) {
            int maxScroll = Math.max(0, filteredCraftableRecipes.size() - CRAFT_LIST_ROWS);
            craftScrollOffset = Math.max(0, Math.min(maxScroll, craftScrollOffset - (int) Math.signum(amount)));
            craftListScrollState.setTarget(craftScrollOffset * LINE_HEIGHT, Math.max(0, filteredCraftableRecipes.size() * LINE_HEIGHT - CRAFT_LIST_HEIGHT));
            saveState();
            return true;
        }
        }
        return false;
    }

    private String getItemNameFromSlot(int slotId) {
        ItemTarget target = getItemTargetFromSlot(slotId);
        return target.editorText();
    }

    private ItemTarget getItemTargetFromSlot(int slotId) {
        AbstractContainerMenu handler = getActiveHandler();
        if (handler == null || slotId < 0 || slotId >= handler.slots.size()) return new ItemTarget();

        Slot slot = handler.slots.get(slotId);
        if (slot == null || slot.getItem().isEmpty()) return new ItemTarget();

        int visibleSlot = PackUtilInventoryHelper.toUserVisibleSlot(MC, slot.index);
        return ItemTarget.capture(slot.getItem(), visibleSlot);
    }

    private void refreshCraftableRecipes(boolean force) {
        if (!isCraftMode() || MC.player == null || MC.level == null) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastCraftRefreshAt < PackUtilCraftingHelper.CRAFT_REFRESH_DEBOUNCE_MS) return;
        lastCraftRefreshAt = now;

        craftableRecipes.clear();
        filteredCraftableRecipes = new ArrayList<>();

        craftableRecipes.addAll(PackUtilCraftingHelper.getCraftableRecipes(MC));
        updateCraftRecipeFilter(craftSearchField != null ? craftSearchField.getText() : savedCraftSearchValue, false);
        clearSelectedCraftIfInvalid();
        restoreSelectedCraftIfNeeded();
    }

    private void updateCraftRecipeFilter(String query, boolean resetScroll) {
        savedCraftSearchValue = query == null ? "" : query;
        filteredCraftableRecipes = PackUtilCraftingHelper.filterRecipes(craftableRecipes, query);
        int maxScroll = Math.max(0, filteredCraftableRecipes.size() - CRAFT_LIST_ROWS);
        craftScrollOffset = resetScroll ? 0 : Math.max(0, Math.min(maxScroll, craftScrollOffset));
    }

    private void clearSelectedCraftIfInvalid() {
        if (selectedCraftOption == null) return;
        for (PackUtilCraftingHelper.CraftableRecipeOption option : craftableRecipes) {
            if ((!option.recipeKey.isBlank() && option.recipeKey.equalsIgnoreCase(selectedCraftOption.recipeKey))
                || (option.recipeId >= 0 && option.recipeId == selectedCraftOption.recipeId)
                || (option.syncedRecipeId >= 0 && option.syncedRecipeId == selectedCraftOption.syncedRecipeId)) {
                selectedCraftOption = option;
                selectedCraftResult = option.result.copy();
                savedCraftSelectedRecipeId = option.recipeId;
                savedCraftSelectedRecipeKey = option.recipeKey;
                return;
            }
        }
        selectedCraftOption = null;
        selectedCraftResult = ItemStack.EMPTY;
        savedCraftSelectedRecipeId = -1;
        savedCraftSelectedRecipeKey = "";
    }

    private void restoreSelectedCraftIfNeeded() {
        if ((savedCraftSelectedRecipeKey == null || savedCraftSelectedRecipeKey.isBlank()) && savedCraftSelectedRecipeId < 0) return;
        if (selectedCraftOption != null) {
            if (!savedCraftSelectedRecipeKey.isBlank() && savedCraftSelectedRecipeKey.equalsIgnoreCase(selectedCraftOption.recipeKey)) return;
            if (savedCraftSelectedRecipeId >= 0 && (selectedCraftOption.recipeId == savedCraftSelectedRecipeId || selectedCraftOption.syncedRecipeId == savedCraftSelectedRecipeId)) return;
        }
        for (PackUtilCraftingHelper.CraftableRecipeOption option : craftableRecipes) {
            if ((!savedCraftSelectedRecipeKey.isBlank() && savedCraftSelectedRecipeKey.equalsIgnoreCase(option.recipeKey))
                || (savedCraftSelectedRecipeId >= 0 && option.recipeId == savedCraftSelectedRecipeId)
                || (savedCraftSelectedRecipeId >= 0 && option.syncedRecipeId == savedCraftSelectedRecipeId)) {
                selectedCraftOption = option;
                selectedCraftResult = option.result.copy();
                savedItemTarget = ItemTarget.capture(option.result, -1);
                savedItemNameValue = option.label;
                savedCraftSelectedRecipeKey = option.recipeKey;
                savedCraftSelectedRecipeId = option.recipeId;
                if (itemNameField != null) itemNameField.setText(savedItemNameValue);
                return;
            }
        }
    }

    private int getCraftListY() {
        return getCraftSearchY() + 24;
    }

    private int getCraftListX() {
        return panelX + 10;
    }

    private int getCraftListWidth() {
        return PANEL_WIDTH - 20;
    }

    private int getCraftPlanContentWidth() {
        return Math.max(40, getCraftListWidth() - (getCraftPlanScrollbarMetrics().hasScroll() ? SCROLLBAR_GUTTER : 0));
    }

    private int getCraftListContentWidth() {
        return Math.max(40, getCraftListWidth() - (getCraftListScrollbarMetrics().hasScroll() ? SCROLLBAR_GUTTER : 0));
    }

    private PackUiScrollbar.Metrics getCraftPlanScrollbarMetrics() {
        int listX = getCraftListX();
        int listY = getCraftPlanListY();
        int listWidth = getCraftListWidth();
        int contentHeight = plannedCraftEntries.size() * 13;
        int maxScroll = Math.max(0, contentHeight - CRAFT_PLAN_HEIGHT);
        return PackUiScrollbar.compute(
            contentHeight,
            CRAFT_PLAN_HEIGHT,
            listX + listWidth - 5,
            listY,
            3,
            CRAFT_PLAN_HEIGHT,
            craftPlanScrollState.tick(0.0f, maxScroll)
        );
    }

    private PackUiScrollbar.Metrics getCraftListScrollbarMetrics() {
        int listX = getCraftListX();
        int listY = getCraftListY();
        int listWidth = getCraftListWidth();
        int contentHeight = filteredCraftableRecipes.size() * LINE_HEIGHT;
        int maxScroll = Math.max(0, contentHeight - CRAFT_LIST_HEIGHT);
        return PackUiScrollbar.compute(
            contentHeight,
            CRAFT_LIST_HEIGHT,
            listX + listWidth - 5,
            listY,
            3,
            CRAFT_LIST_HEIGHT,
            craftListScrollState.tick(0.0f, maxScroll)
        );
    }

    private int toRowScroll(PackUiScrollbar.Metrics metrics, int mouseY, int grabOffset, int rowHeight, int maxScrollRows) {
        int pixelScroll = PackUiScrollbar.scrollFromThumb(metrics, mouseY, grabOffset);
        return Math.max(0, Math.min(maxScrollRows, Math.round(pixelScroll / (float) rowHeight)));
    }

    private void selectCraftRecipe(PackUtilCraftingHelper.CraftableRecipeOption option) {
        if (option == null) return;
        selectedCraftOption = option;
        selectedCraftResult = option.result.copy();
        savedItemTarget = ItemTarget.capture(option.result, -1);
        savedItemNameValue = option.label;
        savedCraftSelectedRecipeId = option.recipeId;
        savedCraftSelectedRecipeKey = option.recipeKey;
        if (itemNameField != null) itemNameField.setText(savedItemNameValue);
        setStatus(
            "Selected craft: " + option.label + (option.craftableNow ? " (x" + option.maxCraftsNow + ")" : ""),
            option.craftableNow ? ChatFormatting.GREEN : ChatFormatting.GOLD
        );
        saveState();
    }

    private void clearCraftRecipeSelection() {
        craftPlanSelectedIndex = -1;
        selectedCraftOption = null;
        selectedCraftResult = ItemStack.EMPTY;
        savedItemTarget = new ItemTarget();
        savedCraftSelectedRecipeId = -1;
        savedCraftSelectedRecipeKey = "";
    }

    private PackUtilCraftingHelper.CraftableRecipeOption resolveCraftOption(CraftAction.CraftEntry entry) {
        if (entry == null) return null;
        return PackUtilCraftingHelper.findInList(craftableRecipes, entry.recipeKey, entry.recipeId);
    }

    private CraftAction.CraftEntry buildConfiguredCraftEntry() {
        if (selectedCraftOption == null || selectedCraftResult.isEmpty()) return null;
        int configuredAmount = parseConfiguredCraftAmount();
        if (configuredAmount <= 0) return null;
        return CraftAction.CraftEntry.fromOption(selectedCraftOption, configuredAmount, craftUseMaxAmount);
    }

    private CraftAction.CraftEntry getSelectedPlannedEntry() {
        if (craftPlanSelectedIndex < 0 || craftPlanSelectedIndex >= plannedCraftEntries.size()) return null;
        return plannedCraftEntries.get(craftPlanSelectedIndex);
    }

    private void selectCraftPlanEntry(int index) {
        if (index < 0 || index >= plannedCraftEntries.size()) {
            craftPlanSelectedIndex = -1;
            selectedCraftOption = null;
            selectedCraftResult = ItemStack.EMPTY;
            savedCraftSelectedRecipeId = -1;
            savedCraftSelectedRecipeKey = "";
            saveState();
            return;
        }

        craftPlanSelectedIndex = index;
        CraftAction.CraftEntry entry = plannedCraftEntries.get(index);
        if (entry != null) {
            craftUseMaxAmount = entry.useMaxAmount;
            savedTimesValue = String.valueOf(Math.max(1, entry.amount));
            if (timesField != null) {
                timesField.setText(savedTimesValue);
                timesField.setEditable(!craftUseMaxAmount);
            }
            PackUtilCraftingHelper.CraftableRecipeOption option = resolveCraftOption(entry);
            if (option != null) {
                selectCraftRecipe(option);
            } else {
                savedCraftSelectedRecipeId = entry.recipeId;
                savedCraftSelectedRecipeKey = entry.recipeKey;
                selectedCraftOption = null;
                selectedCraftResult = ItemStack.EMPTY;
            }
        }
        saveState();
    }

    private void addOrUpdatePlannedCraftEntry() {
        CraftAction.CraftEntry draft = buildConfiguredCraftEntry();
        if (draft == null) {
            setStatus("Select a recipe and amount first", ChatFormatting.RED);
            return;
        }

        int existingIndex = -1;
        for (int i = 0; i < plannedCraftEntries.size(); i++) {
            CraftAction.CraftEntry existing = plannedCraftEntries.get(i);
            if (existing == null) continue;
            if ((!draft.recipeKey.isBlank() && draft.recipeKey.equalsIgnoreCase(existing.recipeKey))
                || (draft.recipeId >= 0 && draft.recipeId == existing.recipeId)) {
                existingIndex = i;
                break;
            }
        }

        if (craftPlanSelectedIndex >= 0 && craftPlanSelectedIndex < plannedCraftEntries.size()) {
            plannedCraftEntries.set(craftPlanSelectedIndex, draft);
        } else if (existingIndex >= 0) {
            plannedCraftEntries.set(existingIndex, draft);
            craftPlanSelectedIndex = existingIndex;
        } else {
            plannedCraftEntries.add(draft);
            craftPlanSelectedIndex = plannedCraftEntries.size() - 1;
        }

        int maxPlanScroll = Math.max(0, plannedCraftEntries.size() - CRAFT_PLAN_ROWS);
        craftPlanScrollOffset = Math.max(0, Math.min(maxPlanScroll, craftPlanSelectedIndex));
        saveState();
    }

    private void removePlannedCraftEntry(int index) {
        if (index < 0 || index >= plannedCraftEntries.size()) return;
        plannedCraftEntries.remove(index);
        if (craftPlanSelectedIndex == index) craftPlanSelectedIndex = -1;
        else if (craftPlanSelectedIndex > index) craftPlanSelectedIndex--;
        craftPlanScrollOffset = Math.max(0, Math.min(Math.max(0, plannedCraftEntries.size() - CRAFT_PLAN_ROWS), craftPlanScrollOffset));
        saveState();
    }

    private List<CraftAction.CraftEntry> getActiveCraftEntries() {
        if (!plannedCraftEntries.isEmpty()) {
            List<CraftAction.CraftEntry> copies = new ArrayList<>(plannedCraftEntries.size());
            for (CraftAction.CraftEntry entry : plannedCraftEntries) {
                if (entry != null && entry.hasRecipe()) copies.add(entry.copy());
            }
            return copies;
        }

        CraftAction.CraftEntry draft = buildConfiguredCraftEntry();
        return draft == null ? List.of() : List.of(draft);
    }

    private CraftAction buildCraftPlanAction() {
        List<CraftAction.CraftEntry> activeEntries = getActiveCraftEntries();
        if (activeEntries.isEmpty()) return null;
        CraftAction action = new CraftAction();
        action.setEntries(activeEntries);
        return action;
    }

    private Component getCraftPlanRowLabel(CraftAction.CraftEntry entry) {
        if (entry == null || !entry.hasRecipe()) return Component.literal("(empty)");
        PackUtilCraftingHelper.CraftableRecipeOption option = resolveCraftOption(entry);
        String amount = entry.useMaxAmount ? "Max" : "x" + Math.max(1, entry.amount);
        Component resultName = option != null ? option.result.getHoverName().copy() : Component.literal(entry.resultName);
        return Component.empty().append(resultName).append(Component.literal(" | " + amount + (option != null && option.craftableNow ? " | x" + option.maxCraftsNow : "")));
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;

        boolean anyFocused = false;
        for (PackUtilChatField field : textFields) {
            if (field.isFocused()) {
                anyFocused = true;
                break;
            }
        }

        if (anyFocused) {
            if (keyCode == 256) {
                for (PackUtilChatField field : textFields) {
                    field.setFocused(false);
                }
                return true;
            }
            KeyEvent input = new KeyEvent(keyCode, scanCode, modifiers);
            for (PackUtilChatField field : textFields) {
                if (field.isFocused()) {
                    field.keyPressed(input);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        int codepoint = (int) chr;
        if (!visible) return false;

        CharacterEvent input = new CharacterEvent(codepoint, 0);
        for (PackUtilChatField field : textFields) {
            if (field.isFocused() && field.charTyped(input)) {
                return true;
            }
        }

        return false;
    }

    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        expireTransientStatus();

        int panelHeight = PANEL_HEIGHT;
        PackUtilWindowLayout bounds = new PackUtilWindowLayout(panelX, panelY, PANEL_WIDTH, panelHeight, visible, collapsed);
        renderWindowFrame(context, mouseX, mouseY, bounds, "Fabricator", collapsed, isDragging);
        boolean clipBody = beginWindowBodyClip(context, bounds, collapsed);
        if (!clipBody) {
            renderWindowInactiveOverlay(context, bounds, collapsed, isDragging);
            return;
        }

        try {
        int labelX = panelX + 10;
        int fieldX = panelX + LABEL_WIDTH + 15;
        int fieldWidth = getFieldWidth();

        PackUtilText.draw(context, MC.font, "Action", PackUtilText.Tone.MUTED, labelX, getActionRowY() + 3, false);
        PackUtilText.draw(context, MC.font, "Cycle", PackUtilText.Tone.MUTED, labelX + 36, getActionRowY() + 3, false);
        drawOverlayButton(context, fieldX, getActionRowY(), fieldWidth, 16,
            currentAction.displayName, PackUiOverlayButton.Variant.SECONDARY, true, mouseX, mouseY);

        for (PackUtilChatField field : textFields) {
            field.render(context, mouseX, mouseY, delta);
        }

        if (showsTargetFields()) {
            PackUtilText.draw(context, MC.font, "Slot", PackUtilText.Tone.MUTED, labelX, getSlotRowY() + 3, false);
            PackUtilText.draw(context, MC.font, "Item", PackUtilText.Tone.MUTED, labelX, getItemRowY() + 3, false);

            if (isClickSelectorRelevant() || isDropModeRelevant()) {
                PackUtilText.draw(context, MC.font, isDropModeRelevant() ? "Drop Mode" : "Click", PackUtilText.Tone.MUTED, labelX, getOptionRowY() + 3, false);
                int halfWidth = (fieldWidth - 2) / 2;
                if (isClickSelectorRelevant()) {
                    drawOverlayButton(context, fieldX, getOptionRowY(), halfWidth, 16, "Left",
                        currentButtonIndex == 0 ? PackUiOverlayButton.Variant.PRIMARY : PackUiOverlayButton.Variant.SECONDARY, true, mouseX, mouseY);
                    drawOverlayButton(context, fieldX + halfWidth + 2, getOptionRowY(), fieldWidth - halfWidth - 2, 16, "Right",
                        currentButtonIndex == 1 ? PackUiOverlayButton.Variant.PRIMARY : PackUiOverlayButton.Variant.SECONDARY, true, mouseX, mouseY);
                } else {
                    drawOverlayButton(context, fieldX, getOptionRowY(), halfWidth, 16, "Single",
                        !dropWholeStack ? PackUiOverlayButton.Variant.PRIMARY : PackUiOverlayButton.Variant.SECONDARY, true, mouseX, mouseY);
                    drawOverlayButton(context, fieldX + halfWidth + 2, getOptionRowY(), fieldWidth - halfWidth - 2, 16, "Stack",
                        dropWholeStack ? PackUiOverlayButton.Variant.PRIMARY : PackUiOverlayButton.Variant.SECONDARY, true, mouseX, mouseY);
                }
            }

            PackUtilText.draw(context, MC.font, getTimesLabel(), PackUtilText.Tone.MUTED, labelX, getStandardTimesRowY() + 3, false);
        } else {
            refreshCraftableRecipes(false);
            int listX = getCraftListX();
            int listWidth = getCraftListWidth();

            PackUiListRenderer.drawHeader(context, MC.font, "Craft Plan", labelX, getCraftPlanHeaderY());
            if (!plannedCraftEntries.isEmpty()) {
                PackUiListRenderer.drawHeaderAction(context, MC.font, "Clear All", true, listX, listWidth, getCraftPlanHeaderY(), mouseX, mouseY);
            }

            int planListY = getCraftPlanListY();
            int planContentWidth = getCraftPlanContentWidth();
            PackUiListRenderer.drawFrame(context, listX, planListY, listWidth, CRAFT_PLAN_HEIGHT, craftPlanSelectedIndex >= 0);
            if (plannedCraftEntries.isEmpty()) {
                PackUiListRenderer.drawEmptyState(context, MC.font, "(no plan - pick recipe and Add)", listX, planListY + 2, planContentWidth);
            } else {
                int maxPlanScrollPx = Math.max(0, plannedCraftEntries.size() * 13 - CRAFT_PLAN_HEIGHT);
                craftPlanScrollOffset = Math.max(0, Math.min(craftPlanScrollOffset, Math.max(0, plannedCraftEntries.size() - CRAFT_PLAN_ROWS)));
                craftPlanScrollState.setTarget(craftPlanScrollOffset * 13, maxPlanScrollPx);
                int drawPlanScroll = craftPlanScrollState.tick(delta, maxPlanScrollPx);
                PackUtilUiScale.enableOverlayScissor(context, listX + 1, planListY + 1, listX + listWidth - 1, planListY + CRAFT_PLAN_HEIGHT - 1);
                int startIndex = drawPlanScroll / 13;
                int rowY = planListY - (drawPlanScroll % 13);
                for (int index = startIndex; index < plannedCraftEntries.size() && rowY < planListY + CRAFT_PLAN_HEIGHT; index++, rowY += 13) {
                    boolean hovered = mouseX >= listX && mouseX < listX + planContentWidth && mouseY >= rowY && mouseY < rowY + 12;
                    boolean selected = index == craftPlanSelectedIndex;
                    PackUiListRenderer.drawRow(
                        context,
                        MC.font,
                        getCraftPlanRowLabel(plannedCraftEntries.get(index)),
                        listX,
                        rowY,
                        planContentWidth,
                        12,
                        hovered,
                        selected,
                        PackUiListRenderer.RowTone.NORMAL,
                        true
                    );
                    boolean removeHovered = mouseX >= listX + planContentWidth - 16 && mouseX < listX + planContentWidth - 2 && mouseY >= rowY && mouseY < rowY + 12;
                    PackUiListRenderer.drawIconButton(context, listX + planContentWidth - 14, rowY, 12, PackUiAssets.ICON_WINDOW_CLOSE, removeHovered, true);
                }
                context.disableScissor();
            }
            PackUiScrollbar.Metrics craftPlanScrollbar = getCraftPlanScrollbarMetrics();
            PackUiScrollbar.draw(context, craftPlanScrollbar, craftPlanScrollbar.contains(mouseX, mouseY), activeScrollbarDrag == SCROLLBAR_CRAFT_PLAN);

            if (selectedCraftResult.isEmpty()) {
                PackUtilText.draw(context, MC.font, "Recipe: none selected", PackUtilText.Tone.MUTED, listX, getCraftSummaryY(), false);
            } else {
                Component summaryText = Component.empty()
                    .append(selectedCraftResult.getHoverName().copy())
                    .append(Component.literal(" x" + selectedCraftResult.getCount()
                        + (selectedCraftOption != null && selectedCraftOption.craftableNow ? " | x" + selectedCraftOption.maxCraftsNow : "")));
            PackUtilText.draw(context, MC.font, summaryText.getString(), PackUtilColors.textMuted(), listX, getCraftSummaryY(), false);
            }

            PackUtilText.draw(context, MC.font, "Amount", PackUtilText.Tone.MUTED, labelX, getCraftAmountRowY() + 3, false);
            if (craftUseMaxAmount) {
                drawOverlayButton(context, getCraftAmountToggleX(), getCraftAmountRowY(), getCraftAmountToggleWidth(), 16, "Use Max", PackUiOverlayButton.Variant.PRIMARY, true, mouseX, mouseY);
            } else {
                drawOverlayButton(context, getCraftAmountToggleX(), getCraftAmountRowY(), getCraftAmountToggleWidth(), 16, "Use Max", PackUiOverlayButton.Variant.SECONDARY, true, mouseX, mouseY);
            }

            PackUiListRenderer.drawHeader(context, MC.font, "Recipes", listX, getCraftSearchLabelY());

            int listY = getCraftListY();
            int recipeContentWidth = getCraftListContentWidth();
            PackUiListRenderer.drawFrame(context, listX, listY, listWidth, CRAFT_LIST_HEIGHT, selectedCraftOption != null);
            PackUtilUiScale.enableOverlayScissor(context, listX + 1, listY + 1, listX + listWidth - 1, listY + CRAFT_LIST_HEIGHT - 1);

            if (filteredCraftableRecipes.isEmpty()) {
                String emptyText = MC.player != null && MC.player.containerMenu instanceof CraftingMenu
                    ? "No crafting recipes match this search."
                    : "Open inventory or stand near a table to load recipes.";
                PackUiListRenderer.drawEmptyState(context, MC.font, emptyText, listX, listY + 4, recipeContentWidth);
            } else {
                int maxRecipeScrollPx = Math.max(0, filteredCraftableRecipes.size() * LINE_HEIGHT - CRAFT_LIST_HEIGHT);
                craftScrollOffset = Math.max(0, Math.min(craftScrollOffset, Math.max(0, filteredCraftableRecipes.size() - CRAFT_LIST_ROWS)));
                craftListScrollState.setTarget(craftScrollOffset * LINE_HEIGHT, maxRecipeScrollPx);
                int drawRecipeScroll = craftListScrollState.tick(delta, maxRecipeScrollPx);
                int startIndex = drawRecipeScroll / LINE_HEIGHT;
                int rowY = listY - (drawRecipeScroll % LINE_HEIGHT);
                for (int index = startIndex; index < filteredCraftableRecipes.size() && rowY < listY + CRAFT_LIST_HEIGHT; index++, rowY += LINE_HEIGHT) {
                    PackUtilCraftingHelper.CraftableRecipeOption option = filteredCraftableRecipes.get(index);
                    boolean hovered = mouseX >= listX && mouseX <= listX + recipeContentWidth && mouseY >= rowY && mouseY < rowY + LINE_HEIGHT;
                    boolean selected = selectedCraftOption != null && option.recipeKey.equalsIgnoreCase(selectedCraftOption.recipeKey);
                    Component richLabel = Component.empty()
                        .append(option.result.getHoverName().copy())
                        .append(Component.literal(" x" + option.result.getCount() + (option.craftableNow ? " | x" + option.maxCraftsNow : "")));
                    PackUiListRenderer.drawRow(
                        context,
                        MC.font,
                        richLabel,
                        listX + 2,
                        rowY,
                        recipeContentWidth - 2,
                        LINE_HEIGHT,
                        hovered,
                        selected,
                        option.craftableNow ? PackUiListRenderer.RowTone.READY : PackUiListRenderer.RowTone.WARNING,
                        true
                    );
                    PackUiListRenderer.drawDivider(context, listX + 2, rowY + LINE_HEIGHT - 1, recipeContentWidth - 2);
                }
            }

            context.disableScissor();
            PackUiScrollbar.Metrics craftListScrollbar = getCraftListScrollbarMetrics();
            PackUiScrollbar.draw(context, craftListScrollbar, craftListScrollbar.contains(mouseX, mouseY), activeScrollbarDrag == SCROLLBAR_CRAFT_LIST);
            long readyCount = craftableRecipes.stream().filter(option -> option.craftableNow).count();
            String recipeCountLine = PackUtilText.trimToWidth(
                MC.font,
                readyCount + " ready / " + craftableRecipes.size() + " total",
                listWidth,
                PackUtilText.Tone.MUTED
            );
            PackUtilText.draw(context, MC.font, recipeCountLine, PackUtilText.Tone.MUTED, listX, listY + CRAFT_LIST_HEIGHT + 2, false);
        }

        int btnY = getBottomButtonY();
        int btnArea = PANEL_WIDTH - 20;
        int gap = 2;
        int bw = (btnArea - gap * 2) / 3;
        int bx = panelX + 10;
        String sendLabel = isCraftMode() ? (craftExecutionInProgress ? "Crafting..." : "Craft") : "Send";
        drawOverlayButton(context, bx, btnY, bw, BUTTON_HEIGHT, sendLabel, PackUiOverlayButton.Variant.SECONDARY, true, mouseX, mouseY);
        bx += bw + gap;
        drawOverlayButton(context, bx, btnY, bw, BUTTON_HEIGHT, "Queue", PackUiOverlayButton.Variant.SECONDARY, true, mouseX, mouseY);
        bx += bw + gap;
        drawOverlayButton(context, bx, btnY, bw, BUTTON_HEIGHT, "Macro", PackUiOverlayButton.Variant.SECONDARY, true, mouseX, mouseY);

        int infoWidth = PANEL_WIDTH - 20;
        if (!statusMessage.isEmpty()) {
            String trimmedStatus = PackUtilText.trimToWidth(MC.font, statusMessage, infoWidth, PackUtilText.Tone.BODY);
            Integer colorInt = statusColor.getColor();
            int color = colorInt != null ? colorInt | 0xFF000000 : 0xFFFFFFFF;
            PackUtilText.draw(context, MC.font, trimmedStatus, color, panelX + 10, getStatusY(), true);
        } else {
            String summary = PackUtilText.trimToWidth(MC.font, getTargetSummary(), infoWidth, PackUtilText.Tone.MUTED);
            PackUtilText.draw(context, MC.font, summary, PackUtilText.Tone.MUTED, panelX + 10, getStatusY(), false);
        }

        String actionLine = PackUtilText.trimToWidth(MC.font, currentAction.getDescription(dropWholeStack), infoWidth, PackUtilText.Tone.MUTED);
        PackUtilText.draw(context, MC.font, actionLine, PackUtilText.Tone.MUTED, panelX + 10, getActionInfoY(), false);

        String tipLine = "Right-click a slot to fill name + slot.";
        tipLine = PackUtilText.trimToWidth(MC.font, tipLine, infoWidth, PackUtilText.Tone.MUTED);
        PackUtilText.draw(context, MC.font, tipLine, PackUtilText.Tone.MUTED, panelX + 10, panelY + panelHeight - 15, false);
        } finally {
            endWindowBodyClip(context, clipBody);
            renderWindowInactiveOverlay(context, bounds, collapsed, isDragging);
        }
    }

    private FabricatorAction currentAction = FabricatorAction.CLICK;

    private void cycleAction() {
        currentActionIndex = (currentActionIndex + 1) % actions.length;
        currentAction = actions[currentActionIndex];
        clearStatus();
        initWidgets();
        saveState();
    }

    private void cycleActionBackwards() {
        currentActionIndex = (currentActionIndex - 1 + actions.length) % actions.length;
        currentAction = actions[currentActionIndex];
        clearStatus();
        initWidgets();
        saveState();
    }

    private void clearWidgets() {
        textFields.clear();
        craftSearchField = null;
    }

    public void onSlotClick(Slot slot, int button) {
        if (slot == null) return;
        if (isCraftMode()) return;

        int stableSlotId = PackUtilInventoryHelper.toUserVisibleSlot(MC, slot.index);
        selectedSlotId = stableSlotId;
        savedSlotValue = String.valueOf(stableSlotId);

        if (slotField != null) {
            slotField.setText(savedSlotValue);
        }

        ItemTarget clickedTarget = getItemTargetFromSlot(slot.index);
        String itemName = clickedTarget.editorText();
        savedSlotValue = String.valueOf(stableSlotId);
        if (slotField != null) slotField.setText(savedSlotValue);

        if (!itemName.isEmpty()) {
            savedItemTarget = clickedTarget;
            savedItemNameValue = itemName;
            if (itemNameField != null) itemNameField.setText(itemName);
            setStatus("Selected slot " + stableSlotId + ": " + itemName, ChatFormatting.GREEN);
        } else {
            savedItemTarget = new ItemTarget();
            savedItemNameValue = "";
            if (itemNameField != null) itemNameField.setText("");
            setStatus("Selected slot " + stableSlotId + " (Empty)", ChatFormatting.YELLOW);
        }

        saveState();
    }
    private void sendToMacro(PackUtilMacroEditorOverlay macroEditor) {
        try {
            if (isCraftMode()) {
                CraftAction craftAction = buildCraftPlanAction();
                if (craftAction == null || !craftAction.hasEntries()) {
                    setStatus("\u00a7cSelect a recipe or add a craft entry first!", ChatFormatting.RED);
                    return;
                }
                macroEditor.addAction(craftAction);
                return;
            }

            Integer enteredSlot = parseEnteredSlot();
            String itemName = getEnteredItemName();
            int repeats = resolveRepeatCount(-1);
            if (repeats <= 0) return;

            if (enteredSlot == null && itemName.isEmpty()) {
                setStatus("\u00a7cPick a slot or enter an item name first!", ChatFormatting.RED);
                return;
            }

            AbstractContainerMenu handler = getActiveHandler();
            int resolvedSlot = enteredSlot == null ? -1 : PackUtilInventoryHelper.resolveConfiguredHandlerSlot(MC, enteredSlot);
            if (enteredSlot != null && !isValidHandlerSlot(handler, resolvedSlot)) {
                setStatus("\u00a7cSlot " + enteredSlot + " is not available in this screen!", ChatFormatting.RED);
                return;
            }

            if (currentAction == FabricatorAction.DROP) {
                autismclient.util.macro.DropAction dropAction = new autismclient.util.macro.DropAction();
                dropAction.useHandlerSlots = true;
                dropAction.mode = dropWholeStack
                    ? autismclient.util.macro.DropAction.DropMode.ALL
                    : autismclient.util.macro.DropAction.DropMode.TIMES;
                dropAction.dropCount = dropWholeStack ? 0 : repeats;
                String entry = enteredSlot != null
                    ? ("#" + enteredSlot + (!itemName.isEmpty() ? "|" + itemName : ""))
                    : itemName;
                if (!entry.isBlank()) {
                    ItemTarget target = buildEnteredItemTarget(enteredSlot, itemName);
                    entry = target.toLegacyEntry();
                    dropAction.itemTargets.add(target);
                    dropAction.itemNames.add(entry);
                    dropAction.itemCounts.add(dropWholeStack ? 0 : repeats);
                }
                macroEditor.addAction(dropAction);
                setStatus("\u00a7aAdded Drop action to macro!", ChatFormatting.GREEN);
                return;
            }

            autismclient.util.macro.ItemAction itemAction = new autismclient.util.macro.ItemAction();
            itemAction.actionIndex = currentAction.toPacketAction(dropWholeStack).ordinal();
            itemAction.button = getEffectiveButton();
            itemAction.times = repeats;

            if (enteredSlot != null) {
                itemAction.useSlot = true;
                itemAction.targetSlot = enteredSlot;
            }

            if (!itemName.isEmpty()) {
                ItemTarget target = buildEnteredItemTarget(enteredSlot, itemName);
                itemAction.itemTargets.add(target);
                itemAction.itemNames.add(target.toLegacyEntry());
                itemAction.itemTimes.add(repeats);
                itemAction.itemActionIdx.add(itemAction.actionIndex);
                itemAction.itemButtons.add(itemAction.button);
            }

            macroEditor.addAction(itemAction);
            setStatus("\u00a7aAdded " + currentAction.displayName + " action to macro!", ChatFormatting.GREEN);
        } catch (NumberFormatException e) {
            setStatus("\u00a7cInvalid value", ChatFormatting.RED);
        }
    }

    private ItemTarget buildEnteredItemTarget(Integer enteredSlot, String itemName) {
        String safeName = itemName == null ? "" : itemName.trim();
        ItemTarget target = savedItemTarget != null && safeName.equals(savedItemTarget.editorText())
                ? savedItemTarget.copy()
                : ItemTarget.fromLegacyEntry(safeName);
        if (enteredSlot != null) target.slot = enteredSlot;
        return target;
    }

    private net.minecraft.network.protocol.Packet<?> buildFabricatedPacket(int slot) {
        if (MC.player == null) return null;
        net.minecraft.world.inventory.AbstractContainerMenu handler = MC.player.containerMenu;
        if (handler == null) return null;

        net.minecraft.world.inventory.ClickType actionType = currentAction.toPacketAction(dropWholeStack).toContainerInput();
        return new ServerboundContainerClickPacket(
            handler.containerId, handler.getStateId(),
            (short) slot, (byte) getEffectiveButton(), actionType,
            new it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap<>(),
            HashedStack.EMPTY
        );
    }

    private void send(boolean queue) {
        if (isCraftMode()) {
            sendCraft(queue);
            return;
        }

        try {
            int slot = resolveTargetSlotForBuild();
            if (slot < 0) return;
            int repeats = resolveRepeatCount(slot);
            if (repeats <= 0) return;

            ClientPacketListener handler = MC.getConnection();
            if (handler == null) return;

            if (currentAction == FabricatorAction.DROP && !queue) {
                int queuedCount = PackUtilSharedState.get().flushDelayedPackets(handler);
                int sent = PackUtilDropHelper.dropFromHandlerSlot(MC, slot, dropWholeStack ? 0 : repeats);
                if (sent <= 0) {
                    setStatus("Nothing to drop", ChatFormatting.YELLOW);
                } else if (queuedCount > 0) {
                    setStatus("Sent " + queuedCount + " queued + drop action", ChatFormatting.GREEN);
                } else {
                    setStatus(dropWholeStack ? "Dropped stack" : "Dropped " + repeats + " item(s)", ChatFormatting.GREEN);
                }
                return;
            }

            sendFabricatedPackets(slot, repeats, queue, handler);

        } catch (NumberFormatException e) {
            setStatus("Invalid number format", ChatFormatting.RED);
        }
    }

    private void sendCraft(boolean queue) {
        List<CraftAction.CraftEntry> entries = getActiveCraftEntries();
        if (entries.isEmpty()) {
            setStatus("Select a recipe or add a craft entry first", ChatFormatting.RED);
            return;
        }

        if (queue) {
            for (CraftAction.CraftEntry entry : entries) {
                PackUtilCraftingHelper.CraftableRecipeOption option = PackUtilCraftingHelper.findCraftableRecipe(MC, entry.recipeKey);
                if (option == null) option = PackUtilCraftingHelper.findCraftableRecipe(MC, entry.recipeId);
                if (option == null) {
                    String msg = "Recipe not found: " + entry.resultName;
                    setStatus(msg, ChatFormatting.RED);
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§c" + msg);
                    return;
                }
                int desiredAmount = PackUtilCraftingHelper.getEffectiveRequestedOutput(option, entry.amount, entry.useMaxAmount);
                if (desiredAmount <= 0) {
                    String msg = "No space or materials for " + entry.resultName;
                    setStatus(msg, ChatFormatting.RED);
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§c" + msg);
                    return;
                }

                List<Packet<?>> packets = PackUtilCraftingHelper.buildCraftSequence(MC, entry.recipeKey, entry.recipeId, desiredAmount);
                if (packets.isEmpty()) {
                    String msg = option.hasSyncedRecipe()
                        ? "Failed to build craft queue for " + entry.resultName
                        : "Queue craft needs a synced recipe for " + entry.resultName;
                    setStatus(msg, ChatFormatting.RED);
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§c" + msg);
                    return;
                }

                for (Packet<?> packet : packets) {
                    PackUtilSharedState.get().enqueuePacket(packet);
                }
            }
            return;
        }

        if (craftExecutionInProgress) {
            setStatus("A craft is already running", ChatFormatting.YELLOW);
            return;
        }

        craftExecutionInProgress = true;
        Thread craftThread = new Thread(() -> {
            PackUtilCraftingHelper.CraftExecutionResult result = null;
            for (CraftAction.CraftEntry entry : entries) {
                PackUtilCraftingHelper.CraftableRecipeOption option = PackUtilCraftingHelper.findCraftableRecipe(MC, entry.recipeKey);
                if (option == null) option = PackUtilCraftingHelper.findCraftableRecipe(MC, entry.recipeId);
                if (option == null) {
                    result = PackUtilCraftingHelper.CraftExecutionResult.failure("Recipe not found: " + entry.resultName);
                    break;
                }
                int desiredAmount = PackUtilCraftingHelper.getEffectiveRequestedOutput(option, entry.amount, entry.useMaxAmount);
                if (desiredAmount <= 0) {
                    result = PackUtilCraftingHelper.CraftExecutionResult.failure("No space or materials for " + entry.resultName + ".");
                    break;
                }

                result = PackUtilCraftingHelper.executeCraftImmediately(MC, entry.recipeKey, entry.recipeId, desiredAmount);
                if (!result.success) break;
            }
            craftExecutionInProgress = false;
            PackUtilCraftingHelper.CraftExecutionResult finalResult = result;
            if (finalResult != null && !finalResult.success) {
                MC.execute(() -> {
                    setStatus(finalResult.message, ChatFormatting.RED);
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§c" + finalResult.message);
                });
            }
        }, "PackUtil-Fabricator-Craft");
        craftThread.setDaemon(true);
        craftThread.start();
    }

    private void sendFabricatedPackets(int slot, int repeats, boolean queue, ClientPacketListener handler) {
        List<Packet<?>> packets = buildFabricatedSequence(slot, repeats);
        if (packets.isEmpty()) {
            setStatus("Failed to build packet", ChatFormatting.RED);
            return;
        }

        if (queue) {
            for (Packet<?> packet : packets) {
                PackUtilSharedState.get().enqueuePacket(packet);
            }
            setStatus("Queued " + packets.size() + " packet(s)", ChatFormatting.GREEN);
            return;
        }

        int queuedCount = PackUtilSharedState.get().flushDelayedPackets(handler);
        for (Packet<?> packet : packets) {
            PackUtilPacketSender.send(packet);
        }

        if (queuedCount > 0) {
            setStatus("Sent " + queuedCount + " queued + " + packets.size() + " new packet(s)", ChatFormatting.GREEN);
        } else {
            setStatus("Sent " + packets.size() + " packet(s)", ChatFormatting.GREEN);
        }
    }

    private void setStatus(String message, ChatFormatting color) {
        this.statusMessage = message;
        this.statusColor = color;
        this.statusExpiresAtMs = 0L;
    }

    private void clearStatus() {
        this.statusMessage = "";
        this.statusColor = ChatFormatting.GRAY;
        this.statusExpiresAtMs = 0L;
    }

    private void expireTransientStatus() {
        if (statusExpiresAtMs <= 0L) return;
        if (System.currentTimeMillis() >= statusExpiresAtMs) {
            clearStatus();
        }
    }

    private String getTimesLabel() {
        if (isCraftMode()) return "Amount";
        if (isDropModeRelevant() && !dropWholeStack) return "Items";
        return "Times";
    }

    private String getTargetSummary() {
        if (isCraftMode()) {
            String planInfo = plannedCraftEntries.isEmpty()
                ? "Plan: none"
                : "Plan: " + plannedCraftEntries.size() + " entr" + (plannedCraftEntries.size() == 1 ? "y" : "ies");
            if (selectedCraftResult.isEmpty()) return planInfo + " | Recipe: none selected";
            String source = selectedCraftOption != null && selectedCraftOption.craftSource == PackUtilCraftingHelper.CraftSource.TABLE_3X3 ? "Table" : "2x2";
            String availability = selectedCraftOption != null ? PackUtilCraftingHelper.getAvailabilityLabel(selectedCraftOption) : "Unavailable";
            String amountMode = craftUseMaxAmount
                ? "Max"
                : "x" + Math.max(1, parseConfiguredCraftAmount());
            return planInfo + " | Recipe: " + selectedCraftResult.getHoverName().getString() + " | " + source + " | " + availability + " | " + amountMode;
        }

        Integer slot = parseEnteredSlot();
        String itemName = getEnteredItemName();
        if (slot != null && !itemName.isEmpty()) {
            return "Target: '" + itemName + "' in exact slot " + slot;
        }
        if (slot != null) {
            return "Target: exact slot " + slot;
        }
        if (!itemName.isEmpty()) {
            return "Target: best matching '" + itemName + "'";
        }
        return "Target: choose a slot or enter an item name";
    }

    private AbstractContainerMenu getActiveHandler() {
        if (MC.player != null && MC.player.containerMenu != null) return MC.player.containerMenu;
        return parentScreen != null ? ((autismclient.mixin.accessor.AbstractContainerScreenAccessor) parentScreen).packutil$getMenu() : null;
    }

    private Integer parseEnteredSlot() {
        if (slotField == null) return null;
        String text = slotField.getText().trim();
        if (text.isEmpty()) return null;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String getEnteredItemName() {
        return itemNameField == null ? "" : itemNameField.getText().trim();
    }

    private boolean isValidHandlerSlot(AbstractContainerMenu handler, int slotId) {
        return handler != null && slotId >= 0 && slotId < handler.slots.size();
    }

    private boolean stackMatchesQuery(ItemStack stack, String query) {
        if (stack == null || stack.isEmpty() || query == null || query.isBlank()) return false;
        return ItemTarget.fromLegacyEntry(query).score(stack, -1) >= 0;
    }

    private boolean slotMatchesItemQuery(AbstractContainerMenu handler, int slotId, String itemQuery) {
        if (!isValidHandlerSlot(handler, slotId)) return false;
        Slot slot = handler.slots.get(slotId);
        return slot != null && stackMatchesQuery(slot.getItem(), itemQuery);
    }

    private int resolveTargetSlotForBuild() {
        AbstractContainerMenu handler = getActiveHandler();
        if (handler == null) {
            setStatus("No screen is open right now", ChatFormatting.RED);
            return -1;
        }

        Integer enteredSlot = parseEnteredSlot();
        String itemName = getEnteredItemName();
        int resolvedSlot = enteredSlot == null ? -1 : PackUtilInventoryHelper.resolveConfiguredHandlerSlot(MC, enteredSlot);

        if (enteredSlot != null && !isValidHandlerSlot(handler, resolvedSlot)) {
            setStatus("Slot " + enteredSlot + " is not available in this screen", ChatFormatting.RED);
            return -1;
        }

        if (enteredSlot != null && !itemName.isEmpty()) {
            if (slotMatchesItemQuery(handler, resolvedSlot, itemName)) return resolvedSlot;
            setStatus("Slot " + enteredSlot + " does not contain '" + itemName + "'", ChatFormatting.RED);
            return -1;
        }

        if (enteredSlot != null) {
            return resolvedSlot;
        }

        if (!itemName.isEmpty()) {
            Integer foundSlot = findSlotByItemName(itemName);
            if (foundSlot == null) {
                setStatus("No item matching '" + itemName + "' found", ChatFormatting.RED);
                return -1;
            }
            return foundSlot;
        }

        setStatus("Pick a slot or enter an item name first", ChatFormatting.RED);
        return -1;
    }

    private int resolveRepeatCount(int slot) {
        int value;
        try {
            value = Integer.parseInt(timesField.getText().trim());
        } catch (NumberFormatException e) {
            setStatus(currentAction.isCraftAction() ? "Invalid amount" : "Invalid repeat count", ChatFormatting.RED);
            return -1;
        }

        if (value <= 0) {
            setStatus(currentAction.isCraftAction() ? "Amount must be at least 1" : "Repeat count must be at least 1", ChatFormatting.RED);
            return -1;
        }

        if (!currentAction.isCraftAction()) {
            return value;
        }

        if (selectedCraftOption == null || selectedCraftResult.isEmpty()) {
            setStatus("Select a recipe first", ChatFormatting.RED);
            return -1;
        }

        if (!(MC.player != null && MC.player.containerMenu instanceof CraftingMenu craftingHandler)) {
            setStatus("Open a crafting screen first", ChatFormatting.RED);
            return -1;
        }

        int outputPerCraft = Math.max(1, selectedCraftResult.getCount());
        int repeats = Math.max(1, (int) Math.ceil((double) value / outputPerCraft));
        return repeats;
    }

    private int resolveCraftAmount() {
        if (selectedCraftOption == null || selectedCraftResult.isEmpty()) {
            setStatus("Select a recipe first", ChatFormatting.RED);
            return -1;
        }

        if (craftUseMaxAmount) {
            int maxAmount = selectedCraftOption.maxOutputNow;
            if (maxAmount <= 0) {
                setStatus(selectedCraftOption.hasMaterialsNow ? "No room to store crafted items" : "Missing materials", ChatFormatting.RED);
                return -1;
            }
            return maxAmount;
        }

        int value = parseConfiguredCraftAmount();
        if (value <= 0) {
            setStatus("Invalid craft amount", ChatFormatting.RED);
            return -1;
        }
        return value;
    }

    private int parseConfiguredCraftAmount() {
        if (timesField == null) return 1;
        try {
            int value = Integer.parseInt(timesField.getText().trim());
            return Math.max(1, value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private List<Packet<?>> buildFabricatedSequence(int slot, int repeats) {
        List<Packet<?>> packets = new ArrayList<>();

        for (int i = 0; i < repeats; i++) {
            Packet<?> built = buildFabricatedPacket(slot);
            if (built == null) return List.of();
            packets.add(normalizeFabricatorPacket(built));
        }

        return packets;
    }

    private Packet<?> normalizeFabricatorPacket(Packet<?> packet) {
        if (packet instanceof ServerboundContainerClickPacket clickSlotPacket) {
            return PacketRegenerator.regenerate(clickSlotPacket);
        }
        return packet;
    }

    private Integer findSlotByItemName(String searchName) {
        if (searchName == null || searchName.trim().isEmpty()) {
            return null;
        }

        AbstractContainerMenu handler = getActiveHandler();
        if (handler == null) return null;

        ItemTarget target = ItemTarget.fromLegacyEntry(searchName);
        Integer bestMatch = null;
        int bestScore = -1;

        for (Slot slot : handler.slots) {
            if (slot == null || slot.getItem().isEmpty()) continue;

            int score = target.score(slot.getItem(), -1);

            if (score > bestScore) {
                bestScore = score;
                bestMatch = slot.index;
            }
        }

        return bestScore >= 0 ? bestMatch : null;
    }

    private void applyPresetMetrics() {
        DEFAULT_PANEL_WIDTH = 236;
        LINE_HEIGHT = 14;
        CRAFT_LIST_HEIGHT = 84;
        CRAFT_PLAN_HEIGHT = 52;
        FIELD_WIDTH = 100;
        BUTTON_HEIGHT = 20;
        LABEL_WIDTH = 64;
    }

    private int standardPanelHeight() {
        return 240;
    }

    private int craftModePanelHeight() {
        return 384;
    }
}
