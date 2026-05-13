package autismclient.gui.macro.editor;

import autismclient.gui.macro.editor.FieldType;
import autismclient.gui.packui.PackUiAssets;
import autismclient.gui.packui.PackUiButtonFeedback;
import autismclient.gui.packui.PackUiListRenderer;
import autismclient.gui.packui.PackUiOverlayButton;
import autismclient.gui.packui.PackUiRenderContext;
import autismclient.gui.packui.PackUiScrollbar;
import autismclient.gui.packui.PackUiSizing;
import autismclient.gui.packui.PackUiScrollViewport;
import autismclient.gui.packui.PackUiScrollList;
import autismclient.gui.packui.PackUiSmoothScroll;
import autismclient.util.macro.CraftAction;
import autismclient.util.PackUtilCraftingHelper;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import autismclient.util.IPackUtilOverlay;
import autismclient.util.PackUtilChatField;
import autismclient.util.PackUtilClipboardHelper;
import autismclient.util.PackUtilClientMessaging;
import autismclient.util.PackUtilColors;
import autismclient.util.PackUtilOverlayManager;
import autismclient.util.PackUtilOverlayBase;
import autismclient.util.PackUtilPacketNamer;
import autismclient.util.PackUtilPacketRegistry;
import autismclient.util.PackUtilPayloadJsonSupport;
import autismclient.util.PackUtilPayloadSupport;
import autismclient.util.PackUtilRegistryLabels;
import autismclient.util.PackUtilPacketSelectorOverlay;
import autismclient.util.PackUtilSharedState;
import autismclient.util.PackUtilUiScale;
import autismclient.util.PackUtilWindowLayout;
import autismclient.util.PackUtilMacro;
import autismclient.util.PackUtilMacroManager;
import autismclient.util.macro.ItemTarget;
import autismclient.util.macro.MacroAction;
import autismclient.util.macro.MacroActionType;
import autismclient.util.macro.PayloadAction;
import autismclient.util.macro.SendPacketAction;
import autismclient.util.macro.WaitForPacketAction;
import autismclient.util.macro.WaitForSlotChangeAction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;

public class ActionEditorOverlay extends PackUtilOverlayBase {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final int DEFAULT_W          = 280;
    private static final int COMPACT_W          = 184;
    private static final int MIN_W              = 168;
    private static final int MIN_H              = 96;
    private static final int COMPACT_MIN_H      = 88;
    private static final int PAD                = 4;
    private static final int ROW_H              = 15;
    private static final int ROW_GAP            = 2;
    private static final int FOOTER_H           = 22;
    private static final int LABEL_MIN_W        = 44;
    private static final int LABEL_MAX_W        = 68;
    private static final int FIELD_GAP          = 3;
    private static final int CATALOG_LIST_H     = 60;
    private static final int CATALOG_ITEM_H     = 13;
    private static final int CAPTURE_BTN_W      = 52;
    private static final int SEL_LIST_MAX_VIS   = 4;
    private static final int SEL_ITEM_H         = 15;
    private static final int CONTAINER_LIST_VISIBLE_ROWS = 2;
    private static final int CRAFT_LIST_ROWS    = 4;
    private static final int CRAFT_LIST_H       = CATALOG_ITEM_H * CRAFT_LIST_ROWS;
    private static final int SCROLLBAR_W        = 5;
    private static final int WAIT_CHAT_ROW_H    = 34;
    private static final int WAIT_CHAT_VISIBLE_ROWS = 4;
    private static final int WAIT_CHAT_PATTERN_H = 40;
    private static final double WAIT_ENTITY_NEARBY_LIST_RADIUS = 16.0;
    private static final int EDITOR_HINT_ROW_H = 11;
    private static final int EDITOR_HINT_MAX_CHARS = "Waits until the main hand item cooldown is ready again.".length();
    private static final int EDITOR_GHOST_MAX_CHARS = 16;
    private static final int PAYLOAD_CONTENT_H = 74;
    private static final int PAYLOAD_JSON_H = PAYLOAD_CONTENT_H;
    private static final int PAYLOAD_TEXT_H = 30;
    private static final int PAYLOAD_RAW_H = 42;
    private static final int PAYLOAD_JAVA_H = 48;
    private static final Gson PAYLOAD_TEXT_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final long CAPTURE_TOAST_LIFETIME_MS = 700L;
    private static final float CAPTURE_TOAST_ENTER_MS = 140.0f;
    private static final float CAPTURE_TOAST_EXIT_MS = 180.0f;
    private static final int CAPTURE_TOAST_MAX_VISIBLE = 4;
    private static final int CAPTURE_TOAST_GAP = 4;
    private static final int CAPTURE_TOAST_HEIGHT = 18;
    private static final int CAPTURE_TOAST_SUCCESS = 0xFF66E08A;
    private static final int CAPTURE_TOAST_ERROR = 0xFFFF6B6B;
    private static final int MACRO_SELECT_VISIBLE_ROWS = 5;

    private static List<String> ALL_BLOCK_IDS;
    private static List<String> ALL_SOUND_IDS;
    private static List<String> ALL_ENTITY_IDS;
    private static List<String> ALL_ITEM_IDS;

    private final Font textRenderer;
    private final PackUiTheme  theme = new PackUiTheme();
    private final PackUtilPacketSelectorOverlay packetSelectorOverlay;

    private int     panelX = 320, panelY = 60;
    private int     panelW = DEFAULT_W, panelH = MIN_H;
    private boolean visible    = false;
    private boolean dragging   = false;
    private double  dragOffX, dragOffY;
    private boolean restoreVisibleAfterCapture = false;
    private Screen  screenBeforeGBreak;
    private Screen  screenBeforeCapture;
    private boolean entitySpecificCaptureMode = false;

    private MacroAction      targetAction;
    private CompoundTag      workingTag;
    private ActionFieldSchema schema;
    private Runnable          onSaveCallback;

    private final Map<String, PackUtilChatField> textFields   = new LinkedHashMap<>();

    private final Map<String, Boolean>           toggleStates = new LinkedHashMap<>();

    private final Map<String, Integer>           enumIndices  = new LinkedHashMap<>();

    private final Map<String, List<String>>      stringLists  = new LinkedHashMap<>();

    private final Map<String, ItemTarget>        editorItemFields = new HashMap<>();
    private final Map<String, List<ItemTarget>>  editorItemLists  = new HashMap<>();

    private final Map<String, PackUtilChatField> addFields    = new LinkedHashMap<>();

    private final Map<String, Integer>           catalogScrollOffsets  = new HashMap<>();
    private final Map<String, PackUiSmoothScroll> catalogScrollStates  = new HashMap<>();

    private final Map<String, PackUiScrollViewport> catalogScrollViewports = new HashMap<>();

    private final Map<String, int[]>             catalogListBounds     = new HashMap<>();

    private final Map<String, Integer>           stringListEditIndex = new HashMap<>();

    private final Map<String, String>            stringListEditPendingText = new HashMap<>();

    private final Map<String, Integer>           selectedScrollOffsets = new HashMap<>();
    private final Map<String, PackUiSmoothScroll> selectedScrollStates = new HashMap<>();

    private final Map<String, PackUiScrollViewport> selectedScrollViewports = new HashMap<>();

    private final Map<String, int[]>             selectedListBounds    = new HashMap<>();

    private int scrollOffset = 0;

    private autismclient.util.macro.ItemAction itemAction;

    private String itemSlotCapturePendingKey;

    private List<IPackUtilOverlay> captureHiddenOverlays;
    private final List<CaptureToast> captureToasts = new ArrayList<>();

    private List<CraftAction.CraftEntry>                     craftEntries;
    private List<PackUtilCraftingHelper.CraftableRecipeOption> craftAllRecipes;
    private List<PackUtilCraftingHelper.CraftableRecipeOption> craftFilteredRecipes;
    private PackUtilCraftingHelper.CraftableRecipeOption     craftSelectedRecipe;
    private int      craftRecipeScrollOffset;
    private boolean  craftUseMax;
    private String   craftLastQuery;
    private int[]    craftRecipeListBounds;

    private autismclient.util.macro.DropAction dropAction;

    private PayloadAction payloadAction;
    private boolean standalonePayloadEditor = false;
    private PayloadContentMode payloadContentMode = PayloadContentMode.BINARY_REPLAY;
    private boolean payloadContentEdited = false;
    private boolean payloadRawEdited = false;
    private boolean payloadChannelEdited = false;
    private boolean payloadJsonEdited = false;
    private boolean suppressPayloadEditorChange = false;
    private int itemEditIndex = -1;
    private int dropEditIndex = -1;
    private int wscEditIndex  = -1;

    private List<WaitForSlotChangeAction.WaitEntry> wscEntries;

    private WaitForSlotChangeAction.WaitMode wscAddMode  = WaitForSlotChangeAction.WaitMode.NOT_EMPTY;
    private int                              wscAddCount = 1;
    private boolean suppressItemEntryLiveUpdate = false;
    private boolean suppressDropEntryLiveUpdate = false;
    private boolean suppressWscLiveUpdate       = false;
    private boolean suppressDropCountEditorUpdate = false;
    private boolean waitChatFuzzySliderDragging = false;
    private int waitChatFuzzySliderX = -1;
    private int waitChatFuzzySliderY = -1;
    private int waitChatFuzzySliderW = 0;
    private int waitChatFuzzySliderH = 0;
    private boolean rotateSmoothnessSliderDragging = false;
    private int rotateSmoothnessSliderX = -1;
    private int rotateSmoothnessSliderY = -1;
    private int rotateSmoothnessSliderW = 0;
    private int rotateSmoothnessSliderH = 0;
    private boolean suppressWaitChatPatternSync = false;

    private enum PayloadContentMode {
        UTF8_TEXT,
        BRAND_STRING,
        COMMAND_INT,
        BINARY_REPLAY
    }

    private List<autismclient.util.macro.WaitForLanStepAction.LanStepEntry> lanStepEntries;
    private final List<String> payScannedPlayers = new ArrayList<>();
    private boolean payPlayerScanPerformed = false;
    private final List<String> meteorModuleNames = new ArrayList<>();
    private List<autismclient.util.macro.ToggleModuleAction.ModuleEntry> toggleModuleEntries;

    private Runnable onPreSave;

    private final List<HitRegion> hitRegions = new ArrayList<>();
    private float frameDelta = 0.0f;

    @FunctionalInterface
    private interface HitRegionAction {
        boolean fire(double mx, double my, int mouseButton);
    }

    private static final class HitRegion {
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final HitRegionAction action;

        private HitRegion(int x, int y, int w, int h, Runnable action) {
            this(x, y, w, h, (mx, my, mouseButton) -> {
                if (mouseButton != 0) return false;
                action.run();
                return true;
            });
        }

        private HitRegion(PackUiOverlayButton button, Runnable action) {
            this(button.getX(), button.getY(), button.getWidth(), button.getHeight(),
                    (mx, my, mouseButton) -> PackUiOverlayButton.fireIfHit(button, mx, my, mouseButton));
        }

        private HitRegion(int x, int y, int w, int h, HitRegionAction action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
        }

        boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        boolean fire(double mx, double my, int mouseButton) {
            return action != null && action.fire(mx, my, mouseButton);
        }
    }

    private record CaptureToast(String message, long shownAtNanos, int accentColor) {
    }

    private record CaptureListAddResult(boolean added, String message, int accentColor) {
    }

    private record ScrollDragRegion(int x, int y, int w, int h, java.util.function.IntConsumer handler) {
        boolean contains(int mx, int my) { return mx >= x && mx < x+w && my >= y && my < y+h; }
    }
    private final List<ScrollDragRegion> scrollDragRegions = new ArrayList<>();

    private java.util.function.IntConsumer activeScrollDragHandler = null;

    private static ActionEditorOverlay sharedInstance;

    public static ActionEditorOverlay getSharedOverlay() {
        if (sharedInstance == null) {
            sharedInstance = new ActionEditorOverlay(MC.font);
            PackUtilOverlayManager.get().register(sharedInstance);
        }
        return sharedInstance;
    }

    private static boolean isCompactEditorType(MacroActionType type) {
        if (type == null) return false;
        return switch (type) {
            case INVENTORY, RESTORE_GUI, SAVE_GUI, SEND_TOGGLE, DESYNC -> true;
            default -> false;
        };
    }

    private int preferredPanelWidthFor(MacroAction action) {
        return isCompactEditorType(action == null ? null : action.getType()) ? COMPACT_W : DEFAULT_W;
    }

    private int currentMinPanelHeight() {
        return isCompactEditorType(targetAction == null ? null : targetAction.getType()) ? COMPACT_MIN_H : MIN_H;
    }

    public static boolean supportsActionEditor(MacroAction action) {
        if (action == null) return false;
        return switch (action.getType()) {
            case CRAFT, DROP, ITEM, PAYLOAD, WAIT_LAN_STEP, WAIT_PACKET, WAIT_SLOT_CHANGE -> true;
            default -> !ActionFieldRegistry.get(action.getType()).fields().isEmpty();
        };
    }

    public static ActionEditorOverlay getSharedOverlayIfExists() {
        return sharedInstance;
    }

    public ActionEditorOverlay(Font textRenderer) {
        this.textRenderer = textRenderer;
        this.packetSelectorOverlay = new PackUtilPacketSelectorOverlay(textRenderer);
    }

    public void open(MacroAction action, Runnable onPreSave, Runnable onSave) {
        this.standalonePayloadEditor = false;
        this.targetAction   = action;
        this.onPreSave      = onPreSave;
        this.onSaveCallback = onSave;
        this.workingTag     = action.toTag();
        this.schema         = ActionFieldRegistry.get(action.getType());
        this.scrollOffset   = 0;

        textFields.clear();
        toggleStates.clear();
        enumIndices.clear();
        stringLists.clear();
        editorItemFields.clear();
        editorItemLists.clear();
        addFields.clear();
        catalogScrollOffsets.clear();
        catalogScrollStates.clear();
        catalogScrollViewports.clear();
        catalogListBounds.clear();
        selectedScrollOffsets.clear();
        selectedScrollStates.clear();
        selectedScrollViewports.clear();
        selectedListBounds.clear();
        packetSelectorOverlay.close();
        prepareWorkingTagForEditor(action);

        for (FieldDef field : schema.fields()) {
            String key = field.key();
            switch (field.type()) {

                case TOGGLE ->
                    toggleStates.put(key, workingTag.contains(key)
                            ? workingTag.getBooleanOr(key, false) : false);

                case NUMBER -> {
                    int v = workingTag.contains(key) ? workingTag.getIntOr(key, 0) : 0;
                    PackUtilChatField f = makeField(80);
                    f.setNumericOnly(true);
                    f.setText(String.valueOf(v));
                    textFields.put(key, f);
                }

                case DECIMAL -> {
                    double v = workingTag.contains(key) ? workingTag.getDoubleOr(key, 0.0) : 0.0;
                    PackUtilChatField f = makeField(80);
                    f.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d*\\.?\\d*"));
                    f.setText(fmtDouble(v));
                    textFields.put(key, f);
                }

                case TEXT, MACRO_SELECT -> {
                    String v = workingTag.contains(key) ? workingTag.getStringOr(key, "") : "";
                    PackUtilChatField f = makeField(80);
                    f.setText(v);
                    textFields.put(key, f);
                }

                case ENUM -> {
                    String v = workingTag.contains(key) ? workingTag.getStringOr(key, "") : "";
                    List<String> opts = field.enumOptions();
                    int idx = opts.indexOf(v);
                    if (idx < 0) idx = opts.indexOf(v.toUpperCase());
                    enumIndices.put(key, Math.max(0, idx));
                }

                case SLOT -> {
                    int v = workingTag.contains(key) ? workingTag.getIntOr(key, 0) : 0;
                    PackUtilChatField f = makeField(50);
                    f.setNumericOnly(false);
                    f.setText(String.valueOf(v));
                    textFields.put(key, f);
                }

                case BLOCK_POS -> {
                    String[] xyzKeys = field.xyzKeys();
                    boolean dbl      = field.xyzDouble();
                    for (int i = 0; i < 3; i++) {
                        PackUtilChatField f = makeField(50);
                        if (dbl) {
                            f.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d*\\.?\\d*"));
                            double v = workingTag.contains(xyzKeys[i]) ? workingTag.getDoubleOr(xyzKeys[i], 0.0) : 0.0;
                            f.setText(fmtDouble(v));
                        } else {
                            f.setNumericOnly(true);
                            int v = workingTag.contains(xyzKeys[i]) ? workingTag.getIntOr(xyzKeys[i], 0) : 0;
                            f.setText(String.valueOf(v));
                        }
                        textFields.put(key + "_" + i, f);
                    }
                }

                case STRING_LIST -> {
                    List<String> list = new ArrayList<>();
                    if (workingTag.contains(key)) {
                        ListTag nl = workingTag.getList(key).orElse(new ListTag());
                        for (Tag el : nl) {
                            String s = el.asString().orElse("");
                            if (!s.isEmpty()) list.add(s);
                        }
                    }
                    stringLists.put(key, list);

                    PackUtilChatField af = makeField(80);
                    if (field.captureMode() == CaptureMode.BLOCK_CATALOG) {
                        af.setPlaceholder(Component.literal("Search blocks..."));
                    } else {
                        af.setPlaceholder(Component.literal("Search / " + field.addLabel()));
                        af.setSubmitHandler(text -> {
                            if (!text.isBlank()) {
                                List<String> entries = stringLists.get(key);
                                if (entries != null && addStringListEntry(field, entries, text.strip())) {
                                    af.setText("");
                                }
                            }
                            return true;
                        });
                    }
                    addFields.put(key, af);
                }
            }
        }

        itemAction              = null;
        itemSlotCapturePendingKey = null;
        itemEditIndex = -1;
        dropEditIndex = -1;
        if (action instanceof autismclient.util.macro.ItemAction ia) {
            itemAction = new autismclient.util.macro.ItemAction();
            itemAction.itemNames     = new ArrayList<>(ia.itemNames);
            itemAction.itemTargets   = copyEditorTargets(ia.itemTargets, ia.itemNames);
            itemAction.itemTimes     = new ArrayList<>(ia.itemTimes);
            itemAction.itemActionIdx = new ArrayList<>(ia.itemActionIdx);
            itemAction.itemButtons   = new ArrayList<>(ia.itemButtons);
            while (itemAction.itemTimes.size()     < itemAction.itemNames.size()) itemAction.itemTimes.add(1);
            while (itemAction.itemActionIdx.size() < itemAction.itemNames.size()) itemAction.itemActionIdx.add(0);
            while (itemAction.itemButtons.size()   < itemAction.itemNames.size()) itemAction.itemButtons.add(0);
            itemAction.targetSlot  = ia.targetSlot;
            itemAction.useSlot     = ia.useSlot;
            itemAction.actionIndex = ia.actionIndex;
            itemAction.button      = ia.button;
            itemAction.times       = ia.times;
            itemAction.waitForGui  = ia.waitForGui;
            itemAction.guiName     = ia.guiName != null ? ia.guiName : "";
            itemAction.waitForItem = ia.waitForItem;

            for (int i = 0; i < itemAction.itemNames.size(); i++) {
                PackUtilChatField f = makeField(28);
                f.setNumericOnly(true);
                f.setText(String.valueOf(itemAction.getItemTime(i)));
                textFields.put("item_times_" + i, f);
            }

            PackUtilChatField addF = makeField(120);
            addF.setPlaceholder(Component.literal("Item name"));
            addF.setChangedListener(text -> handleItemEntryEditorChanged());
            addFields.put("_item_add", addF);
            PackUtilChatField addSlotF = makeField(52);
            addSlotF.setNumericOnly(true);
            addSlotF.setPlaceholder(Component.literal("Slot"));
            addSlotF.setChangedListener(text -> handleItemEntryEditorChanged());
            textFields.put("item_entrySlot", addSlotF);

            toggleStates.put("item_waitForGui",  ia.waitForGui);
            toggleStates.put("item_waitForItem", ia.waitForItem);
            PackUtilChatField guiF = makeField(80);
            guiF.setText(ia.guiName != null ? ia.guiName : "");
            textFields.put("item_guiName", guiF);

        }

        wscEditIndex = -1;
        wscEntries   = new ArrayList<>();
        wscAddMode   = WaitForSlotChangeAction.WaitMode.NOT_EMPTY;
        wscAddCount  = 1;
        if (action instanceof WaitForSlotChangeAction wsc) {
            for (WaitForSlotChangeAction.WaitEntry e : wsc.entries) wscEntries.add(e.copy());
        }
        if (action.getType() == MacroActionType.WAIT_SLOT_CHANGE) {
            PackUtilChatField wscAddF = makeField(120);
            wscAddF.setPlaceholder(Component.literal("Item name (optional)"));
            wscAddF.setChangedListener(text -> handleWscEntryEditorChanged());
            addFields.put("_wsc_add", wscAddF);
            PackUtilChatField wscSlotF = makeField(52);
            wscSlotF.setNumericOnly(true);
            wscSlotF.setPlaceholder(Component.literal("Slot #"));
            wscSlotF.setChangedListener(text -> handleWscEntryEditorChanged());
            textFields.put("wsc_slot", wscSlotF);
            PackUtilChatField wscCountF = makeField(36);
            wscCountF.setNumericOnly(true);
            wscCountF.setPlaceholder(Component.literal("Count"));
            wscCountF.setChangedListener(text -> handleWscCountChanged());
            textFields.put("wsc_count", wscCountF);
        }

        craftEntries            = null;
        craftAllRecipes         = null;
        craftFilteredRecipes    = null;
        craftSelectedRecipe     = null;
        craftRecipeScrollOffset = 0;
        craftUseMax             = false;
        craftLastQuery          = null;
        craftRecipeListBounds   = null;
        if (action instanceof CraftAction craftAction) {
            craftEntries = craftAction.copyEntries();
            for (int i = 0; i < craftEntries.size(); i++) {
                CraftAction.CraftEntry entry = craftEntries.get(i);
                PackUtilChatField f = makeField(44);
                f.setNumericOnly(true);
                f.setText(String.valueOf(entry.amount));
                textFields.put("craft_amount_" + i, f);
                toggleStates.put("craft_useMax_" + i, entry.useMaxAmount);
            }

            PackUtilChatField amtF = makeField(44);
            amtF.setNumericOnly(true);
            amtF.setText("1");
            textFields.put("_craft_amount", amtF);

            PackUtilChatField srchF = makeField(180);
            srchF.setPlaceholder(Component.literal("Search recipes..."));
            addFields.put("_craft_search", srchF);

            craftAllRecipes      = PackUtilCraftingHelper.getCraftableRecipes(MC);
            craftFilteredRecipes = PackUtilCraftingHelper.filterRecipes(craftAllRecipes, "");
        }

        if (action instanceof WaitForPacketAction waitForPacketAction) {
            stringLists.put("packetNames", sanitizeWaitPacketTargets(waitForPacketAction.effectiveList()));
        }

        dropAction = null;
        if (action instanceof autismclient.util.macro.DropAction da) {
            dropAction = new autismclient.util.macro.DropAction();
            dropAction.mode            = da.mode;
            dropAction.dropCount       = da.dropCount;
            dropAction.itemNames       = new ArrayList<>(da.itemNames);
            dropAction.itemTargets     = copyEditorTargets(da.itemTargets, da.itemNames);
            dropAction.itemCounts      = new ArrayList<>(da.itemCounts);
            dropAction.waitForGui      = da.waitForGui;
            dropAction.guiName         = da.guiName != null ? da.guiName : "";
            dropAction.useHandlerSlots = da.useHandlerSlots;
            while (dropAction.itemCounts.size() < dropAction.itemNames.size()) dropAction.itemCounts.add(1);

            for (int i = 0; i < dropAction.itemNames.size(); i++) {
                PackUtilChatField f = makeField(32);
                f.setNumericOnly(true);
                f.setText(String.valueOf(dropAction.itemCounts.get(i)));
                textFields.put("drop_count_" + i, f);
            }

            PackUtilChatField cntF = makeField(60);
            cntF.setNumericOnly(true);
            cntF.setText(String.valueOf(da.dropCount));
            cntF.setChangedListener(text -> handleDropCountEditorChanged());
            textFields.put("drop_globalCount", cntF);
            PackUtilChatField addDropF = makeField(120);
            addDropF.setPlaceholder(Component.literal("Item name..."));
            addDropF.setChangedListener(text -> handleDropEntryEditorChanged());
            addFields.put("_drop_add", addDropF);
            PackUtilChatField addDropSlotF = makeField(52);
            addDropSlotF.setNumericOnly(true);
            addDropSlotF.setPlaceholder(Component.literal("Slot"));
            addDropSlotF.setChangedListener(text -> handleDropEntryEditorChanged());
            textFields.put("drop_entrySlot", addDropSlotF);
            PackUtilChatField dropGuiF = makeField(80);
            dropGuiF.setText(da.guiName != null ? da.guiName : "");
            textFields.put("drop_guiName", dropGuiF);

            toggleStates.put("drop_waitForGui",      da.waitForGui);
            toggleStates.put("drop_useHandlerSlots", da.useHandlerSlots);
            autismclient.util.macro.DropAction.DropMode[] modes =
                    autismclient.util.macro.DropAction.DropMode.values();
            int mi = 0;
            for (int i = 0; i < modes.length; i++) if (modes[i] == da.mode) { mi = i; break; }
            enumIndices.put("drop_mode", mi);
            syncDropCountEditorField();
        }

        payloadAction = null;
        if (action instanceof PayloadAction pa) {
            payloadAction = new PayloadAction();
            payloadAction.fromTag(pa.toTag());
            initializePayloadEditorFields(payloadAction);
        }

        lanStepEntries = null;
        if (action instanceof autismclient.util.macro.WaitForLanStepAction wls) {
            lanStepEntries = new ArrayList<>();
            for (autismclient.util.macro.WaitForLanStepAction.LanStepEntry e : wls.entries)
                lanStepEntries.add(new autismclient.util.macro.WaitForLanStepAction.LanStepEntry(e.username, e.step));

            for (int i = 0; i < lanStepEntries.size(); i++) {
                autismclient.util.macro.WaitForLanStepAction.LanStepEntry e = lanStepEntries.get(i);
                PackUtilChatField uf = makeField(80); uf.setText(e.username);
                textFields.put("lan_user_" + i, uf);
                PackUtilChatField sf = makeField(40); sf.setNumericOnly(true);
                sf.setText(String.valueOf(e.step));
                textFields.put("lan_step_" + i, sf);
            }
            PackUtilChatField newUF = makeField(80);
            newUF.setPlaceholder(Component.literal("Peer name..."));
            addFields.put("_lan_user_add", newUF);
            PackUtilChatField newSF = makeField(40);
            newSF.setNumericOnly(true);
            newSF.setText("1");
            addFields.put("_lan_step_add", newSF);
            PackUtilChatField dsF = makeField(60);
            dsF.setNumericOnly(true);
            dsF.setText(String.valueOf(wls.defaultStep));
            textFields.put("lan_defaultStep", dsF);
            toggleStates.put("lan_filterByUser", wls.filterByUser);

        }

        entitySpecificCaptureMode = false;
        if (action instanceof autismclient.util.macro.WaitForSoundAction) {
            PackUtilChatField search = addFields.get("soundIds");
            if (search != null) search.setPlaceholder(Component.literal("Search sound id..."));
        }
        if (action instanceof autismclient.util.macro.WaitForEntityAction) {
            PackUtilChatField search = addFields.get("entityIds");
            if (search != null) search.setPlaceholder(Component.literal("Search entity type..."));
        }
        if (action instanceof autismclient.util.macro.LookAtBlockAction) {
            PackUtilChatField search = addFields.get("entityIds");
            if (search != null) search.setPlaceholder(Component.literal("Search entity type..."));
        }
        if (action instanceof autismclient.util.macro.StoreItemAction) {
            PackUtilChatField search = addFields.get("targetItems");
            if (search != null) search.setPlaceholder(Component.literal("Search item id..."));
        }
        if (action instanceof autismclient.util.macro.InventoryAuditAction) {
            PackUtilChatField search = addFields.get("targetItems");
            if (search != null) search.setPlaceholder(Component.literal("Item name"));
        }
        payScannedPlayers.clear();
        payPlayerScanPerformed = false;
        if (action instanceof autismclient.util.macro.PayAction) {
            PackUtilChatField search = addFields.get("players");
            if (search != null) search.setPlaceholder(Component.literal("Search or add player..."));
        }
        meteorModuleNames.clear();
        toggleModuleEntries = null;
        if (action instanceof autismclient.util.macro.ToggleModuleAction) {
            PackUtilChatField search = makeField(180);
            search.setPlaceholder(Component.literal("Search Meteor module..."));
            addFields.put("_toggle_module_search", search);
            refreshMeteorModuleNames();
            autismclient.util.macro.ToggleModuleAction toggleModuleAction = (autismclient.util.macro.ToggleModuleAction) action;
            toggleModuleEntries = new ArrayList<>();
            for (autismclient.util.macro.ToggleModuleAction.ModuleEntry entry : toggleModuleAction.entries) {
                if (entry != null && entry.moduleName != null && !entry.moduleName.isBlank()) {
                    toggleModuleEntries.add(new autismclient.util.macro.ToggleModuleAction.ModuleEntry(entry.moduleName, entry.toggleMode));
                }
            }
        }
        if (action instanceof autismclient.util.macro.WaitForChatAction) {
            PackUtilChatField search = makeField(180);
            search.setPlaceholder(Component.literal("Search recent chat..."));
            addFields.put("_wait_chat_search", search);
            waitChatFuzzySliderDragging = false;
            clearWaitChatFuzzySliderBounds();
            PackUtilChatField patternField = textFields.get("pattern");
            if (patternField != null) {
                patternField.setMultiline(true);
                patternField.setHeight(WAIT_CHAT_PATTERN_H);
                Component initialPattern = getWaitChatPatternComponent(workingTag.getStringOr("pattern", ""));
                String visiblePattern = initialPattern.getString();
                workingTag.putString("pattern", visiblePattern);
                workingTag.putString("patternJson",
                        autismclient.util.macro.MacroExecutor.serializeTextComponent(initialPattern));
                patternField.setDisplayTextProvider(this::getWaitChatPatternComponent);
                suppressWaitChatPatternSync = true;
                patternField.setText(visiblePattern);
                suppressWaitChatPatternSync = false;
                patternField.setChangedListener(value -> {
                    if (suppressWaitChatPatternSync) return;
                    String visibleValue = value == null ? "" : value;
                    Component currentPattern = getWaitChatPatternComponent(workingTag.getStringOr("pattern", ""));
                    Component updatedPattern = rebuildWaitChatPatternComponent(currentPattern, visibleValue);
                    workingTag.putString("pattern", visibleValue);
                    workingTag.putString("patternJson",
                            autismclient.util.macro.MacroExecutor.serializeTextComponent(updatedPattern));
                });
            }
        } else {
            suppressWaitChatPatternSync = false;
            waitChatFuzzySliderDragging = false;
            clearWaitChatFuzzySliderBounds();
        }

        if (action instanceof autismclient.util.macro.RotateAction) {
            rotateSmoothnessSliderDragging = false;
            clearRotateSmoothnessSliderBounds();
            autismclient.util.macro.RotateAction rotateAction = (autismclient.util.macro.RotateAction) action;
            PackUtilChatField smoothnessField = textFields.get("smoothness");
            int smoothness = autismclient.util.macro.RotateAction.clampSmoothness(rotateAction.smoothness);
            workingTag.putInt("smoothness", smoothness);
            if (smoothnessField != null) smoothnessField.setText(String.valueOf(smoothness));
        } else if (action instanceof autismclient.util.macro.LookAtBlockAction lookAtAction) {
            rotateSmoothnessSliderDragging = false;
            clearRotateSmoothnessSliderBounds();
            PackUtilChatField smoothnessField = textFields.get("smoothness");
            int smoothness = autismclient.util.macro.RotateAction.clampSmoothness(lookAtAction.smoothness);
            workingTag.putInt("smoothness", smoothness);
            if (smoothnessField != null) smoothnessField.setText(String.valueOf(smoothness));
        } else {
            rotateSmoothnessSliderDragging = false;
            clearRotateSmoothnessSliderBounds();
        }

        if (action.getType() == MacroActionType.INVENTORY
                || action.getType() == MacroActionType.CLOSE_GUI
                || action.getType() == MacroActionType.SAVE_GUI) {
            toggleStates.put("sendPacket", !workingTag.getBooleanOr("sendPacket", true));
        }
        if (action.getType() == MacroActionType.STORE_ITEM) {
            toggleStates.put("closeSendPkt", !workingTag.getBooleanOr("closeSendPkt", true));
        }

        refreshItemTextDisplayProviders();
        applyEditorPlaceholders();

        panelW = preferredPanelWidthFor(action);
        int contentH = computeContentH();
        int desiredH = HEADER_HEIGHT + PAD + contentH + FOOTER_H + PAD;
        int minH     = currentMinPanelHeight();
        int maxH     = Math.max(minH, PackUtilUiScale.getVirtualScreenHeight() * 4 / 5);
        panelH = Math.max(minH, Math.min(maxH, desiredH));

        this.visible   = true;
        PackUtilOverlayManager.get().bringToFront(this);
    }

    public void openStandalonePayloadEditor(PayloadAction action) {
        open(action, null, null);
        this.standalonePayloadEditor = true;
    }

    @Override public String getOverlayId()  { return "packutil-action-editor"; }
    @Override public int    getMinWidth()   { return MIN_W; }
    @Override public int    getMinHeight()  { return currentMinPanelHeight(); }
    @Override public boolean isVisible()    { return visible; }
    @Override public void   setVisible(boolean v) {
        visible = v;

        if (v) {
            hitRegions.clear();
            scrollDragRegions.clear();
        }
    }

    @Override
    public PackUtilWindowLayout getBounds() {

        int neededH = HEADER_HEIGHT + PAD + computeContentH() + FOOTER_H + PAD;
        int minH = currentMinPanelHeight();
        int maxH = Math.max(minH, PackUtilUiScale.getVirtualScreenHeight() * 4 / 5);
        int h = Math.max(minH, Math.min(maxH, neededH));
        return new PackUtilWindowLayout(panelX, panelY, panelW, h, visible, false);
    }

    @Override
    public void setBounds(PackUtilWindowLayout b) {
        PackUtilWindowLayout c = clampToScreen(this, b);
        panelX    = c.x;
        panelY    = c.y;
        panelW    = Math.max(MIN_W, c.width);
        panelH    = Math.max(currentMinPanelHeight(), c.height);
        visible   = c.visible;
    }

    @Override
    public boolean isMouseOver(double mx, double my) {
        return visible
            && (packetSelectorOverlay.isVisible()
            || mx >= panelX && mx < panelX + panelW
            && my >= panelY && my < panelY + panelH);
    }

    @Override
    public boolean isOverDragBar(double mx, double my) {
        return visible
            && mx >= panelX && mx < panelX + panelW
            && my >= panelY && my < panelY + HEADER_HEIGHT;
    }

    @Override
    public boolean hasTextFieldFocused() {
        if (packetSelectorOverlay.hasTextFieldFocused()) return true;
        for (PackUtilChatField f : textFields.values()) if (f.isFocused()) return true;
        for (PackUtilChatField f : addFields.values())  if (f.isFocused()) return true;
        return false;
    }

    @Override
    public void clearTextFieldFocus() {
        textFields.values().forEach(f -> f.setFocused(false));
        addFields.values().forEach(f  -> f.setFocused(false));
    }

    public boolean wantsItemSlotCapture() {
        return itemSlotCapturePendingKey != null;
    }

    public boolean shouldRenderAbstractContainerScreenCaptureBanner() {
        return itemSlotCapturePendingKey != null;
    }

    public String getAbstractContainerScreenCaptureTitle() {
        return "Capturing " + getAbstractContainerScreenCaptureTargetLabel() + " - " + getCaptureActionLabel();
    }

    public String getAbstractContainerScreenCaptureInstruction() {
        if ("_item_entries".equals(itemSlotCapturePendingKey) || "_drop_entries".equals(itemSlotCapturePendingKey)) {
            return "Right-click a slot to set slot + item. Esc = cancel";
        }
        if ("_wsc_entries".equals(itemSlotCapturePendingKey)) {
            return "Right-click slots to add items or exact slots. Esc = done";
        }
        if (stringLists.containsKey(itemSlotCapturePendingKey)) {
            if (isXCarryListKey(itemSlotCapturePendingKey)) {
                return "Right-click slots to add items, or empty slots for exact slots. Esc = done";
            }
            return "Right-click slots to add item names. Esc = done";
        }
        return "Right-click a slot in this screen. Esc = cancel";
    }

    public String getAbstractContainerScreenCaptureHoverText(net.minecraft.world.inventory.Slot slot, String itemName, String registryId) {
        if (slot == null) return "";
        int visibleSlot = autismclient.util.PackUtilInventoryHelper.toUserVisibleSlot(MC, slot.index);
        String slotText = visibleSlot >= 0 ? String.valueOf(visibleSlot) : "Handler " + slot.index;
        String slotDetail = "Handler " + slot.index;
        String itemText = !registryId.isEmpty() ? registryId : (!itemName.isEmpty() ? itemName : "Empty slot");
        return slotText.equals(slotDetail)
                ? "Hover: " + slotText + " | " + itemText
                : "Hover: " + slotText + " | " + slotDetail + " | " + itemText;
    }

    public boolean cancelCaptureIfActive() {
        if (itemSlotCapturePendingKey == null) return false;
        itemSlotCapturePendingKey = null;
        exitCaptureMode(false, false);
        return true;
    }

    public boolean hasActiveCaptureSession() {
        PackUtilSharedState state = PackUtilSharedState.get();
        return itemSlotCapturePendingKey != null
            || captureHiddenOverlays != null
            || restoreVisibleAfterCapture
            || screenBeforeCapture != null
            || screenBeforeGBreak != null
            || state.hasCaptureCancelCallback()
            || state.hasBlockCaptureCallback()
            || state.hasEntityCaptureCallback()
            || state.hasAttackCaptureCallback()
            || state.isGBreakCapturing();
    }

    public boolean onInventorySlotCapture(net.minecraft.world.inventory.Slot slot,
                                          String itemName, String registryId) {
        int visibleSlot = slot != null
                ? autismclient.util.PackUtilInventoryHelper.toUserVisibleSlot(MC, slot.index)
                : -1;
        ItemTarget capturedTarget = captureItemTarget(slot, itemName, registryId, visibleSlot);
        if (itemSlotCapturePendingKey == null || (itemName.isEmpty() && visibleSlot < 0)) return false;
        String key = itemSlotCapturePendingKey;

        if ("_item_entries".equals(key) && itemAction != null) {
            applyCapturedItemEntry(capturedTarget);
            itemSlotCapturePendingKey = null;
            exitCaptureMode(false, false);
            return true;
        }

        if ("_drop_entries".equals(key) && dropAction != null) {
            applyCapturedDropEntry(capturedTarget);
            itemSlotCapturePendingKey = null;
            exitCaptureMode(false, false);
            return true;
        }

        if ("_wsc_entries".equals(key)) {
            String rawTarget = capturedTarget.toLegacyEntry();
            if (rawTarget == null) {
                showCaptureToast("Nothing to add from that slot", CAPTURE_TOAST_ERROR);
                return true;
            }
            String norm = autismclient.util.macro.StoreItemAction.normalizeTargetEntry(rawTarget);
            if (norm == null || norm.isBlank()) {
                showCaptureToast("Nothing to add from that slot", CAPTURE_TOAST_ERROR);
                return true;
            }
            String disp = autismclient.util.macro.StoreItemAction.formatTargetEntry(norm);
            if (wscEditIndex >= 0 && wscEditIndex < wscEntries.size()) {

                if (!wscTargetExistsOtherThan(norm, wscEditIndex)) {
                    WaitForSlotChangeAction.WaitEntry entry = wscEntries.get(wscEditIndex);
                    entry.target = norm;
                    entry.itemTarget = capturedTarget.copy();
                    syncWscEditorFromEntry(entry);
                    showCaptureToast("Updated: " + disp, CAPTURE_TOAST_SUCCESS);
                } else {
                    showCaptureToast("Already in list: " + disp, CAPTURE_TOAST_ERROR);
                }
            } else {
                if (wscTargetExists(norm)) {
                    showCaptureToast("Already added: " + disp, CAPTURE_TOAST_ERROR);
                } else {
                    WaitForSlotChangeAction.WaitEntry entry = new WaitForSlotChangeAction.WaitEntry(norm, wscAddMode, wscAddCount);
                    entry.itemTarget = capturedTarget.copy();
                    entry.target = norm;
                    wscEntries.add(entry);
                    showCaptureToast("Added: " + disp, CAPTURE_TOAST_SUCCESS);
                }
            }
            return true;
        }

        if (stringLists.containsKey(key)) {
            List<String> list = stringLists.get(key);
            if (list == null) return false;
            CaptureListAddResult result = isStoreItemTargetListKey(key)
                    ? tryAddCapturedStoreItemEntry(slot, itemName, registryId, visibleSlot, list)
                    : tryAddCapturedStringListEntry(
                            findField(key),
                            key,
                            list,
                            (isXCarryListKey(key) || isInventoryAuditTargetListKey(key))
                                    ? stripSlotFromTarget(capturedTarget).toLegacyEntry()
                                    : (usesStoreTargetFormatting(key)
                                            ? capturedTarget.toLegacyEntry()
                                            : itemName)
                    );
        if (result != null && result.added() && (usesStoreTargetFormatting(key) || isXCarryListKey(key) || isInventoryAuditTargetListKey(key))) {
                ItemTarget preservedTarget = (isStoreItemTargetListKey(key) || isInventoryAuditTargetListKey(key))
                        ? stripSlotFromTarget(capturedTarget)
                        : capturedTarget;
                preserveCapturedListTarget(key, list, preservedTarget);
            }
            if (result != null && result.message() != null && !result.message().isBlank()) {
                showCaptureToast(result.message(), result.accentColor());
            }
            return true;
        }

        PackUtilChatField tf = textFields.get(key);
        if (tf != null) {
            FieldDef field = findField(key);
            if (field != null && field.type() == FieldType.SLOT) {
                tf.setText(String.valueOf(Math.max(0, visibleSlot)));
            } else {
                tf.setText(capturedTarget.editorText());
                if ("itemName".equals(key) || "fromItemName".equals(key) || "toItemName".equals(key) || editorItemFields.containsKey(key)) {
                    editorItemFields.put(key, capturedTarget.copy());
                }
            }
            itemSlotCapturePendingKey = null;
            exitCaptureMode(false, false);
            return true;
        }

        return false;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        frameDelta = delta;

        hitRegions.clear();
        scrollDragRegions.clear();
        catalogListBounds.clear();

        {
            int neededH = HEADER_HEIGHT + PAD + computeContentH() + FOOTER_H + PAD;
            int minH    = currentMinPanelHeight();
            int maxH    = Math.max(minH, PackUtilUiScale.getVirtualScreenHeight() * 4 / 5);
            panelH = Math.max(minH, Math.min(maxH, neededH));
        }

        String title = (targetAction != null)
                ? formatTypeName(targetAction.getType())
                : "Edit Action";

        PackUtilWindowLayout bounds = getBounds();
        renderWindowFrame(context, mouseX, mouseY, bounds, title, false, dragging);
        boolean clipBody = beginWindowBodyClip(context, bounds, false);
        if (clipBody) {
            try {
                int frameH = getRenderedFrameHeight(bounds, false);
                int bodyTop = panelY + HEADER_HEIGHT;
                int bodyBtm = panelY + frameH;
                if (bodyBtm > bodyTop + 1) {
                    renderBody(context, mouseX, mouseY, delta, bodyTop, bodyBtm);
                }
            } finally {
                endWindowBodyClip(context, true);
            }
        }
        renderWindowInactiveOverlay(context, bounds, false, dragging);

        if (packetSelectorOverlay.isVisible()) {
            packetSelectorOverlay.render(context, mouseX, mouseY, delta);
        }
    }

    private void renderBody(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta,
                            int bodyTop, int bodyBtm) {

        if (itemAction != null) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderItemPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (payloadAction != null) {
            int contentBtm = bodyBtm - FOOTER_H;
            int contentAreaH = contentBtm - bodyTop;
            int totalContentH = computeContentH();
            final int maxScroll = Math.max(0, totalContentH - contentAreaH);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            boolean needsScroll = maxScroll > 0;
            int sbReserve = needsScroll ? SCROLLBAR_W + 1 : 0;
            int x = panelX + PAD;
            int w = panelW - PAD * 2 - sbReserve;

            PackUtilUiScale.enableOverlayScissor(context,
                    panelX + 1, bodyTop, panelX + panelW - 1, contentBtm);
            renderPayloadPanel(context, x, bodyTop - scrollOffset, w, mouseX, mouseY, delta);
            context.disableScissor();

            if (needsScroll) {
                int sbX = panelX + panelW - SCROLLBAR_W - 1;
                drawScrollbar(context, sbX, bodyTop, contentAreaH, totalContentH, contentAreaH, 1, scrollOffset);
                final int capSbY = bodyTop, capSbH = contentAreaH;
                scrollDragRegions.add(new ScrollDragRegion(sbX, bodyTop, SCROLLBAR_W, contentAreaH, my -> {
                    int rel = Math.max(0, Math.min(capSbH, my - capSbY));
                    scrollOffset = Math.max(0, Math.min(maxScroll, rel * maxScroll / Math.max(1, capSbH)));
                }));
            }
        } else if (targetAction != null && targetAction.getType() == MacroActionType.SEND_PACKET) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderSendPacketPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_PACKET) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderWaitPacketPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_SOUND) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderWaitSoundPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_ENTITY) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderWaitEntityPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.USE_ITEM) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderUseItemPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.LOOK_AT_BLOCK) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderLookAtPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.ROTATE) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderRotatePanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.GO_TO) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderGoToPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.SWAP_SLOTS) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderSwapSlotsPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.CLICK) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderClickPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.DISCONNECT) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderDisconnectPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_GUI) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderWaitGuiPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_CHAT) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderWaitChatPanel(context, x, bodyTop, bodyBtm - FOOTER_H, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.SELECT_SLOT) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderSelectSlotPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_COOLDOWN) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderWaitCooldownPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.OPEN_CONTAINER) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderOpenContainerPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_SLOT_CHANGE) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderWaitSlotChangePanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.DELAY_PACKETS) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderDelayPacketsPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.MINE) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderMinePanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.PAY) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderPayPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.INVENTORY_AUDIT) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderInventoryAuditPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.STORE_ITEM) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderStoreItemPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (targetAction != null && targetAction.getType() == MacroActionType.TOGGLE_MODULE) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderToggleModulePanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (craftEntries != null) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderCraftPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (dropAction != null) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderDropPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (lanStepEntries != null) {
            int x = panelX + PAD;
            int w = panelW - PAD * 2;
            renderLanStepPanel(context, x, bodyTop, w, mouseX, mouseY, delta);
        } else if (schema != null && !schema.fields().isEmpty()) {
            int contentBtm   = bodyBtm - FOOTER_H;
            int contentAreaH = contentBtm - bodyTop;
            int totalContentH = computeContentH();
            final int maxScroll = Math.max(0, totalContentH - contentAreaH);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            boolean needsScroll = maxScroll > 0;
            int sbReserve = needsScroll ? SCROLLBAR_W + 1 : 0;
            int x = panelX + PAD;
            int w = panelW - PAD * 2 - sbReserve;

            PackUtilUiScale.enableOverlayScissor(context,
                    panelX + 1, bodyTop, panelX + panelW - 1, contentBtm);

            int y = bodyTop + PAD - scrollOffset;

            for (FieldDef field : schema.fields()) {
                if (field.type() == FieldType.STRING_LIST) continue;
                if (!isFieldVisible(field)) continue;
                renderRow(context, field, x, y, w, mouseX, mouseY, delta);
                y += rowH(field) + ROW_GAP;
            }

            for (FieldDef field : schema.fields()) {
                if (field.type() != FieldType.STRING_LIST) continue;
                if (!isFieldVisible(field)) continue;
                renderRow(context, field, x, y, w, mouseX, mouseY, delta);
                y += rowH(field) + ROW_GAP;
            }

            if (targetAction != null && targetAction.getType() == MacroActionType.PACKET) {
                renderPacketActionButtons(context, x, y, w, mouseX, mouseY);
                y += 52;
            }
            if (targetAction != null && targetAction.getType() == MacroActionType.SEND_PACKET) {
                renderSendPacketButtons(context, x, y, w, mouseX, mouseY);
                y += 52;
            }
            if (targetAction != null && targetAction.getType() == MacroActionType.DELAY_PACKETS) {
                renderDelayPacketsPresetButtons(context, x, y, w, mouseX, mouseY);
            }

            context.disableScissor();

            if (needsScroll) {
                int sbX = panelX + panelW - SCROLLBAR_W - 1;
                drawScrollbar(context, sbX, bodyTop, contentAreaH, totalContentH, contentAreaH, 1, scrollOffset);
                final int capSbY = bodyTop, capSbH = contentAreaH;
                scrollDragRegions.add(new ScrollDragRegion(sbX, bodyTop, SCROLLBAR_W, contentAreaH, my -> {
                    int rel = Math.max(0, Math.min(capSbH, my - capSbY));
                    scrollOffset = Math.max(0, Math.min(maxScroll, rel * maxScroll / Math.max(1, capSbH)));
                }));
            }
        }

        renderFooter(context, bodyBtm - FOOTER_H, mouseX, mouseY);
    }

    private void renderItemPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font   = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;
        int headerBtnW = 44;

        PackUiText.draw(ctx, textRenderer,
                "Items / Exact Slots (" + itemAction.itemNames.size() + ")", font,
                PackUtilColors.textSecondary(), x, cy + 2, false);
        boolean canClearAll = !itemAction.itemNames.isEmpty();
        int clearX = x + w - headerBtnW;
        renderOverlayButton(ctx, clearX, cy, headerBtnW, 14, "Clear", PackUiOverlayButton.Variant.DANGER, canClearAll, mx, my, () -> {
            itemAction.itemNames.clear();
            itemAction.itemTimes.clear();
            itemAction.itemActionIdx.clear();
            itemAction.itemButtons.clear();
            clearItemEditSelection();
            rebuildItemFields();
        });
        cy += 13;

        int selAreaH = SEL_LIST_MAX_VIS * SEL_ITEM_H;
        int sbX = x + w - SCROLLBAR_W;

        PackUiScrollViewport itemViewport = getOrCreateViewport(selectedScrollViewports, "_item_entries",
            x, cy, w, selAreaH, SEL_ITEM_H, SCROLLBAR_W);
        itemViewport.setContentHeight(itemAction.itemNames.size() * SEL_ITEM_H);
        selectedListBounds.put("_item_entries", new int[]{cy, selAreaH});

        itemViewport.renderScrollbar(ctx, mx, my);

        autismclient.util.PackUtilDropAction[] ACTIONS = autismclient.util.PackUtilDropAction.values();
        int sbGap   = SCROLLBAR_W + 2;
        int delW    = 13;
        int itemW   = w - sbGap - delW - 2;

        if (!itemAction.itemNames.isEmpty()) {
            itemViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
            int firstVis = itemViewport.getFirstVisibleRow();
            int iy0 = cy - (itemViewport.getScrollOffset() % SEL_ITEM_H);
            for (int i = firstVis; i < itemAction.itemNames.size() && i <= itemViewport.getLastVisibleRow(); i++) {
                int iy = itemViewport.getRowScreenY(i);
                if (iy == Integer.MIN_VALUE) continue;
                String entry      = itemAction.itemNames.get(i);
                int eiActIdx      = itemAction.getItemActionIdx(i);
                autismclient.util.PackUtilDropAction eiAct = ACTIONS[eiActIdx];
                int eiBtn         = itemAction.getItemButton(i);
                boolean selected  = i == itemEditIndex;

                ItemTarget entryTarget = targetAt(itemAction.itemTargets, itemAction.itemNames, i);
                Component displayName = formatItemTargetText(entryTarget, entry);
                boolean rowHovered = mx >= x && mx < x + itemW && my >= iy && my < iy + 13;

                String summaryAct = eiAct.shortName;
                String summaryBtn = eiAct == autismclient.util.PackUtilDropAction.SWAP
                        ? "H" + Math.max(1, Math.min(9, eiBtn + 1))
                        : switch (eiBtn) { case 1 -> "R"; case 2 -> "M"; default -> "L"; };
                int times = itemAction.getItemTime(i);
                String summary = " \u2022 " + summaryAct + " " + summaryBtn + (times != 1 ? " \u00d7" + times : "");
                Component rowLabel = Component.empty().append(displayName).append(
                        Component.literal(summary).withStyle(s -> s.withColor(0xFF888888)));

                PackUiListRenderer.drawRow(
                        ctx,
                        textRenderer,
                        rowLabel,
                        x,
                        iy,
                        itemW,
                        13,
                        rowHovered,
                        selected,
                        PackUiListRenderer.RowTone.NORMAL,
                        true
                );

                if (iy + 13 > cy && iy < cy + selAreaH) {
                    final int fi = i;
                    hitRegions.add(new HitRegion(x, Math.max(cy, iy), itemW, Math.min(iy + 13, cy + selAreaH) - Math.max(cy, iy), () -> toggleItemEditSelection(fi)));
                }

                if (iy + 13 > cy && iy < cy + selAreaH) {
                    int delX = x + itemW + 2;
                    final int fi = i;
                    renderIconDeleteButton(ctx, delX, iy, delW, mx, my, () -> {
                        itemAction.itemNames.remove(fi);
                        if (fi < itemAction.itemTargets.size()) itemAction.itemTargets.remove(fi);
                        if (fi < itemAction.itemTimes.size()) itemAction.itemTimes.remove(fi);
                        if (fi < itemAction.itemActionIdx.size()) itemAction.itemActionIdx.remove(fi);
                        if (fi < itemAction.itemButtons.size()) itemAction.itemButtons.remove(fi);
                        if (itemEditIndex == fi) clearItemEditSelection();
                        else if (itemEditIndex > fi) itemEditIndex--;
                        rebuildItemFields();
                        PackUiScrollViewport vp = selectedScrollViewports.get("_item_entries");
                        if (vp != null) vp.scrollBy(-1);
                    });
                }
            }
            itemViewport.endRender(ctx);
        } else {
            PackUiListRenderer.drawEmptyState(ctx, textRenderer, "No rows yet. Add an item or exact slot.", x, cy, w - SCROLLBAR_W - 1);
        }
        cy += selAreaH;

        ctx.fill(x, cy + 2, x + w, cy + 3, PackUtilColors.subPanelBorder());
        cy += 6;

        int addPickW = 32;
        int addBtnW  = 34;
        int slotW    = 26;
        PackUtilChatField addF = addFields.get("_item_add");
        PackUtilChatField entrySlotF = textFields.get("item_entrySlot");
        if (addF != null && entrySlotF != null) {
            int pickX = x + w - addBtnW - 3 - addPickW;
            int plusX = x + w - addBtnW;
            int slotX = pickX - 3 - slotW;
            addF.setX(x); addF.setY(cy + 1); addF.setWidth(slotX - x - 2);
            addF.render(ctx, mx, my, delta);
            entrySlotF.setX(slotX); entrySlotF.setY(cy + 1); entrySlotF.setWidth(slotW);
            entrySlotF.render(ctx, mx, my, delta);

            boolean capturing = "_item_entries".equals(itemSlotCapturePendingKey);
            String pickLbl = capturing ? "Done" : "Pick";
            renderOverlayButton(
                    ctx,
                    pickX,
                    cy,
                    addPickW,
                    14,
                    pickLbl,
                    capturing ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.PRIMARY,
                    true,
                    mx,
                    my,
                    () -> {
                if ("_item_entries".equals(itemSlotCapturePendingKey)) {
                    itemSlotCapturePendingKey = null;
                    exitCaptureMode(false, false);
                } else {
                    itemSlotCapturePendingKey = "_item_entries";
                    enterCaptureMode();
                }
            });

            String addLbl = itemEditIndex >= 0 ? "New" : "+Add";
            renderOverlayButton(ctx, plusX, cy, addBtnW, 14, addLbl, PackUiOverlayButton.Variant.SUCCESS, true, mx, my, () -> {
                if (itemEditIndex >= 0) clearItemEditSelection();
                else applyItemEntryEditor();
            });
        }
        cy += 16;
        cy += 4;

        if (itemEditIndex >= 0 && itemEditIndex < itemAction.itemNames.size()) {
            int eiActIdx = itemAction.getItemActionIdx(itemEditIndex);
            autismclient.util.PackUtilDropAction eiAct = ACTIONS[eiActIdx];
            int eiBtn = itemAction.getItemButton(itemEditIndex);
            boolean btnActive = eiAct == autismclient.util.PackUtilDropAction.PICKUP
                    || eiAct == autismclient.util.PackUtilDropAction.SWAP;
            final int editIdx = itemEditIndex;

            int editActW = 54;
            int editBtnW = 28;
            int editTimesW = 28;
            int editGap = 3;
            renderOverlayButton(ctx, x, cy, editActW, 14, eiAct.shortName, PackUiOverlayButton.Variant.SECONDARY, true, mx, my, () -> itemAction.cycleItemAction(editIdx));
            String btnLbl = eiAct == autismclient.util.PackUtilDropAction.SWAP
                    ? "H" + Math.max(1, Math.min(9, eiBtn + 1))
                    : switch (eiBtn) { case 1 -> "R"; case 2 -> "M"; default -> "L"; };
            renderOverlayButton(ctx, x + editActW + editGap, cy, editBtnW, 14, btnLbl,
                    eiAct == autismclient.util.PackUtilDropAction.SWAP ? PackUiOverlayButton.Variant.PRIMARY : PackUiOverlayButton.Variant.GHOST,
                    btnActive, mx, my, () -> itemAction.cycleItemButton(editIdx));
            PackUtilChatField tf = textFields.get("item_times_" + itemEditIndex);
            if (tf != null) { tf.setX(x + editActW + editGap + editBtnW + editGap); tf.setY(cy + 1); tf.setWidth(editTimesW); tf.render(ctx, mx, my, delta); }
            cy += 16;
        }

        String itemHint = "Blank name + slot = exact slot. Name + slot = that item in that slot.";
        if (itemEditIndex >= 0 && itemEditIndex < itemAction.itemNames.size()
                && itemAction.getItemAction(itemEditIndex) == autismclient.util.PackUtilDropAction.SWAP) {
            itemHint = "Swap uses the blue H1-H9 button to pick which hotbar slot gets swapped.";
        }
        cy = renderEditorHint(ctx, x, cy, w, itemHint);

        renderInlineToggle(ctx, font, "item_waitForGui", "Wait for GUI", x, cy, w, mx, my);
        cy += ROW_H + ROW_GAP;

        if (toggleStates.getOrDefault("item_waitForGui", false)) {
            PackUtilChatField guiF = textFields.get("item_guiName");
            if (guiF != null) {
                int lw = labelWidth(w, "GUI Name", font);
                drawLabel(ctx, "GUI Name", x, cy, lw, font);
                guiF.setX(controlX(x, lw)); guiF.setY(cy + 2); guiF.setWidth(controlWidth(w, lw));
                guiF.render(ctx, mx, my, delta);
            }
            cy += ROW_H + ROW_GAP;
        } else {
            PackUtilChatField guiF = textFields.get("item_guiName");
            if (guiF != null) guiF.setX(-1000);
        }

        renderInlineToggle(ctx, font, "item_waitForItem", "Wait for Item", x, cy, w, mx, my);
        cy += ROW_H + ROW_GAP;

    }

    private void addItemEntry(ItemTarget target) {
        addTargetEntry(itemAction.itemTargets, itemAction.itemNames, target);
        while (itemAction.itemTimes.size()     < itemAction.itemNames.size()) itemAction.itemTimes.add(1);
        while (itemAction.itemActionIdx.size() < itemAction.itemNames.size()) itemAction.itemActionIdx.add(0);
        while (itemAction.itemButtons.size()   < itemAction.itemNames.size()) itemAction.itemButtons.add(0);
        int idx = itemAction.itemNames.size() - 1;
        PackUtilChatField f = makeField(28);
        f.setNumericOnly(true);
        f.setText("1");
        textFields.put("item_times_" + idx, f);
    }

    private void rebuildItemFields() {
        textFields.keySet().removeIf(k -> k.startsWith("item_times_"));

        trimTargetEntries(itemAction.itemTargets, itemAction.itemNames.size());
        while (itemAction.itemTimes.size()     > itemAction.itemNames.size()) itemAction.itemTimes.remove(itemAction.itemTimes.size() - 1);
        while (itemAction.itemActionIdx.size() > itemAction.itemNames.size()) itemAction.itemActionIdx.remove(itemAction.itemActionIdx.size() - 1);
        while (itemAction.itemButtons.size()   > itemAction.itemNames.size()) itemAction.itemButtons.remove(itemAction.itemButtons.size() - 1);
        for (int i = 0; i < itemAction.itemNames.size(); i++) {
            PackUtilChatField f = makeField(28);
            f.setNumericOnly(true);
            f.setText(String.valueOf(itemAction.getItemTime(i)));
            textFields.put("item_times_" + i, f);
        }
    }

    private void toggleItemEditSelection(int index) {
        if (itemEditIndex == index) {
            clearItemEditSelection();
            return;
        }
        itemEditIndex = index;
        syncItemEntryEditorFromSelection();
    }

    private void clearItemEditSelection() {
        itemEditIndex = -1;
        PackUtilChatField addF = addFields.get("_item_add");
        suppressItemEntryLiveUpdate = true;
        if (addF != null) addF.setText("");
        PackUtilChatField slotF = textFields.get("item_entrySlot");
        if (slotF != null) slotF.setText("");
        suppressItemEntryLiveUpdate = false;
    }

    private void syncItemEntryEditorFromSelection() {
        PackUtilChatField addF = addFields.get("_item_add");
        PackUtilChatField slotF = textFields.get("item_entrySlot");
        if (addF == null || slotF == null) return;
        suppressItemEntryLiveUpdate = true;
        if (itemEditIndex < 0 || itemEditIndex >= itemAction.itemNames.size()) {
            addF.setText("");
            slotF.setText("");
            suppressItemEntryLiveUpdate = false;
            return;
        }
        ItemTarget target = targetAt(itemAction.itemTargets, itemAction.itemNames, itemEditIndex);
        addF.setText(target.editorText());
        slotF.setText(target.hasSlot() ? String.valueOf(target.slot) : "");
        suppressItemEntryLiveUpdate = false;
    }

    private void applyItemEntryEditor() {
        applyItemEntryEditor(false);
    }

    private void handleItemEntryEditorChanged() {
        if (suppressItemEntryLiveUpdate || itemAction == null) return;
        if (itemEditIndex < 0 || itemEditIndex >= itemAction.itemNames.size()) return;
        applyItemEntryEditor(true);
    }

    private void applyItemEntryEditor(boolean preserveSelection) {
        if (itemAction == null) return;
        PackUtilChatField addF = addFields.get("_item_add");
        PackUtilChatField slotF = textFields.get("item_entrySlot");
        if (addF == null || slotF == null) return;
        String nameText = addF.getText().strip();
        String slotText = slotF.getText().strip();
        ItemTarget target = buildEntryTargetFromEditor(nameText, slotText, targetAt(itemAction.itemTargets, itemAction.itemNames, itemEditIndex));
        String entry = target.toLegacyEntry();
        if (entry == null || entry.isBlank()) return;

        if (itemEditIndex >= 0 && itemEditIndex < itemAction.itemNames.size()) {
            if (!containsEntryOtherThan(itemAction.itemNames, entry, itemEditIndex)) {
                setTargetAt(itemAction.itemTargets, itemAction.itemNames, itemEditIndex, target);
            }
        } else if (!itemAction.itemNames.contains(entry)) {
            addItemEntry(target);
        }

        if (target.hasSlot()) {
            itemAction.targetSlot = target.slot;
            itemAction.useSlot = true;
        } else {
            itemAction.targetSlot = -1;
            itemAction.useSlot = false;
        }

        if (!preserveSelection) {
            clearItemEditSelection();
        }
    }

    private void applyCapturedItemEntry(ItemTarget target) {
        if (itemAction == null) return;

        String entry = target == null ? "" : target.toLegacyEntry();
        if (entry.isBlank()) return;

        int targetIndex = itemEditIndex;
        if (targetIndex >= 0 && targetIndex < itemAction.itemNames.size()) {
            if (!containsEntryOtherThan(itemAction.itemNames, entry, targetIndex)) {
                setTargetAt(itemAction.itemTargets, itemAction.itemNames, targetIndex, target.copy());
            }
        } else {
            targetIndex = itemAction.itemNames.indexOf(entry);
            if (targetIndex < 0) {
                addItemEntry(target.copy());
                targetIndex = itemAction.itemNames.size() - 1;
            }
        }

        itemEditIndex = targetIndex;
        PackUtilChatField addF = addFields.get("_item_add");
        if (addF != null) addF.setText(target.editorText());
    }

    private void renderInlineToggle(GuiGraphicsExtractor ctx, Identifier font, String stateKey, String label,
                                    int x, int y, int w, int mx, int my) {
        boolean val = toggleStates.getOrDefault(stateKey, false);
        int lw = labelWidth(w, label, font, 34);
        drawLabel(ctx, label, x, y, lw, font);
        int btnW = 34, btnH = 14;
        int btnX = x + w - btnW;
        renderOverlayButton(
                ctx,
                btnX,
                y + 2,
                btnW,
                btnH,
                val ? "ON" : "OFF",
                val ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.DANGER,
                true,
                mx,
                my,
                () -> toggleStates.put(stateKey, !toggleStates.getOrDefault(stateKey, false))
        );
    }

    private void renderCraftPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w,
                                  int mx, int my, float delta) {
        Identifier font   = theme.fontFor(PackUiTone.BODY);
        int removeW       = 13;
        int maxTogW       = 42;
        int amountW       = 44;
        int headerBtnW    = 44;

        int cy = bodyTop + PAD;

        PackUiText.draw(ctx, textRenderer,
                "Craft Entries (" + craftEntries.size() + ")", font,
                PackUtilColors.textSecondary(), x, cy + 2, false);
        boolean canClearEntries = !craftEntries.isEmpty();
        renderOverlayButton(ctx, x + w - headerBtnW, cy, headerBtnW, 14, "Clear",
                PackUiOverlayButton.Variant.DANGER, canClearEntries, mx, my, () -> {
                    craftEntries.clear();
                    rebuildCraftFields();
                    PackUiScrollViewport craftVp = selectedScrollViewports.get("_craft_entries");
                if (craftVp != null) craftVp.jumpTo(0);
                });
        cy += 13;

        int selAreaH = SEL_LIST_MAX_VIS * SEL_ITEM_H;

        PackUiScrollViewport craftViewport = getOrCreateViewport(selectedScrollViewports, "_craft_entries",
            x, cy, w, selAreaH, SEL_ITEM_H, SCROLLBAR_W);
        craftViewport.setContentHeight(craftEntries.size() * SEL_ITEM_H);
        selectedListBounds.put("_craft_entries", new int[]{cy, selAreaH});

        craftViewport.renderScrollbar(ctx, mx, my);

        int sbGap = SCROLLBAR_W + 2;
        if (!craftEntries.isEmpty()) {
            craftViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
            int firstSel = craftViewport.getFirstVisibleRow();
            for (int i = firstSel; i < craftEntries.size() && i <= craftViewport.getLastVisibleRow(); i++) {
                int siy = craftViewport.getRowScreenY(i);
                if (siy == Integer.MIN_VALUE) continue;
                CraftAction.CraftEntry entry = craftEntries.get(i);
                boolean useMax = toggleStates.getOrDefault("craft_useMax_" + i, false);

                int removeX = x + w - sbGap - removeW;
                final int fi = i;
                renderIconDeleteButton(ctx, removeX, siy + 1, removeW, mx, my, () -> {
                    craftEntries.remove(fi);
                    rebuildCraftFields();
                    PackUiScrollViewport vp = selectedScrollViewports.get("_craft_entries");
                    if (vp != null) vp.scrollBy(-1);
                });

                int maxX = removeX - 3 - maxTogW;
                String mLbl = useMax ? "Max: On" : "Max: Off";
                final int fii = i;
                renderOverlayButton(
                        ctx,
                        maxX,
                        siy + 1,
                        maxTogW,
                        13,
                        mLbl,
                        useMax ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.GHOST,
                        true,
                        mx,
                        my,
                        () -> toggleStates.put("craft_useMax_" + fii,
                                !toggleStates.getOrDefault("craft_useMax_" + fii, false))
                );

                if (!useMax) {
                    int amtX = maxX - 3 - amountW;
                    PackUtilChatField af = textFields.get("craft_amount_" + i);
                    if (af != null) { af.setX(amtX); af.setY(siy + 1); af.setWidth(amountW); af.render(ctx, mx, my, delta); }
                }

                int nameW = useMax ? (maxX - 3 - x) : (maxX - 3 - amountW - 3 - x);
                boolean rowHovered = mx >= x && mx < x + nameW && my >= siy && my < siy + 13;
                PackUiListRenderer.drawRow(
                        ctx,
                        textRenderer,
                        entry.resultNameComponent(),
                        x,
                        siy,
                        Math.max(1, nameW),
                        13,
                        rowHovered,
                        false,
                        useMax ? PackUiListRenderer.RowTone.READY : PackUiListRenderer.RowTone.NORMAL,
                        true
                );
            }
            craftViewport.endRender(ctx);
        } else {
            PackUiListRenderer.drawEmptyState(ctx, textRenderer, "No craft entries yet.", x, cy, w - SCROLLBAR_W - 1);
        }
        cy += selAreaH;

        ctx.fill(x, cy + 2, x + w, cy + 3, PackUtilColors.subPanelBorder());
        cy += 6;

        int refreshW = 44;
        PackUtilChatField srchF = addFields.get("_craft_search");
        if (srchF != null) {
            srchF.setX(x); srchF.setY(cy); srchF.setWidth(w - refreshW - 3);
            srchF.render(ctx, mx, my, delta);
        }
        int rfX = x + w - refreshW;
        renderOverlayButton(ctx, rfX, cy, refreshW, 14, "Reload",
                PackUiOverlayButton.Variant.SECONDARY, true, mx, my, this::refreshCraftRecipes);
        cy += 16;

        String query = srchF != null ? srchF.getText() : "";
        if (!query.equals(craftLastQuery)) {
            craftFilteredRecipes = PackUtilCraftingHelper.filterRecipes(
                    craftAllRecipes != null ? craftAllRecipes : List.of(), query);
            craftLastQuery = query;
            craftRecipeScrollOffset = 0;
            if (craftSelectedRecipe != null
                    && (craftFilteredRecipes == null || !craftFilteredRecipes.contains(craftSelectedRecipe)))
                craftSelectedRecipe = null;
        }
        List<PackUtilCraftingHelper.CraftableRecipeOption> filtered =
                craftFilteredRecipes != null ? craftFilteredRecipes : List.of();

        int recipeListH  = CRAFT_LIST_H;

        PackUiScrollViewport recipeViewport = getOrCreateViewport(catalogScrollViewports, "_craft_recipe_browser",
            x, cy, w, recipeListH, CATALOG_ITEM_H, SCROLLBAR_W);
        recipeViewport.setContentHeight(filtered.size() * CATALOG_ITEM_H);
        craftRecipeListBounds = new int[]{cy, recipeListH};

        recipeViewport.renderScrollbar(ctx, mx, my);

        int recW = w - SCROLLBAR_W - 1;
        if (filtered.isEmpty()) {
            PackUiListRenderer.drawEmptyState(
                    ctx,
                    textRenderer,
                    craftAllRecipes == null || craftAllRecipes.isEmpty()
                            ? "No craftable recipes. Use Reload."
                            : "No recipes match the search.",
                    x,
                    cy,
                    recW
            );
        } else {
            recipeViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
            int firstRec = recipeViewport.getFirstVisibleRow();
            for (int i = firstRec; i < filtered.size() && i <= recipeViewport.getLastVisibleRow(); i++) {
                int ry = recipeViewport.getRowScreenY(i);
                if (ry == Integer.MIN_VALUE) continue;
                PackUtilCraftingHelper.CraftableRecipeOption opt = filtered.get(i);
                boolean inList = craftEntries.stream().anyMatch(e ->
                        (opt.recipeKey != null && opt.recipeKey.equals(e.recipeKey))
                        || (opt.recipeId >= 0 && opt.recipeId == e.recipeId));
                boolean hov = !inList && mx >= x && mx < x + recW && my >= ry && my < ry + CATALOG_ITEM_H;
                PackUiListRenderer.RowTone tone = inList
                        ? PackUiListRenderer.RowTone.READY
                        : (opt.craftableNow ? PackUiListRenderer.RowTone.NORMAL : PackUiListRenderer.RowTone.WARNING);
                PackUiListRenderer.drawRow(ctx, textRenderer, opt.labelComponent, x, ry, recW, CATALOG_ITEM_H, hov, inList, tone, true);
                if (opt.result != null && opt.result.getCount() > 1) {
                    String cnt = "\u00d7" + opt.result.getCount();
                    PackUiText.draw(ctx, textRenderer, cnt, font, PackUtilColors.textDim(),
                            x + recW - uiWidth(font, cnt) - 2, ry + 2, false);
                }
                final PackUtilCraftingHelper.CraftableRecipeOption fOpt = opt;
                final boolean fInList = inList;
                hitRegions.add(new HitRegion(x, ry, recW, CATALOG_ITEM_H, () -> {
                    if (fInList) {
                        craftEntries.removeIf(e ->
                                (fOpt.recipeKey != null && fOpt.recipeKey.equals(e.recipeKey))
                                || (fOpt.recipeId >= 0 && fOpt.recipeId == e.recipeId));
                        rebuildCraftFields();
                    } else {
                        CraftAction.CraftEntry newEntry = CraftAction.CraftEntry.fromOption(fOpt, 1, false);
                        craftEntries.add(newEntry);
                        int idx = craftEntries.size() - 1;
                        PackUtilChatField f = makeField(44);
                        f.setNumericOnly(true);
                        f.setText("1");
                        textFields.put("craft_amount_" + idx, f);
                        toggleStates.put("craft_useMax_" + idx, false);
                    }
                }));
            }
            recipeViewport.endRender(ctx);
        }
    }

    private void refreshCraftRecipes() {
        craftAllRecipes      = PackUtilCraftingHelper.getCraftableRecipes(MC);
        craftLastQuery       = null;
        craftSelectedRecipe  = null;
        craftRecipeScrollOffset = 0;
    }

    private void addCraftEntry() {
        if (craftSelectedRecipe == null) return;
        PackUtilChatField amtF = textFields.get("_craft_amount");
        int amount = 1;
        if (amtF != null && !craftUseMax) {
            try { amount = Math.max(1, Integer.parseInt(amtF.getText().strip())); }
            catch (NumberFormatException ignored) {}
        }
        CraftAction.CraftEntry newEntry = CraftAction.CraftEntry.fromOption(craftSelectedRecipe, amount, craftUseMax);

        for (int i = 0; i < craftEntries.size(); i++) {
            CraftAction.CraftEntry existing = craftEntries.get(i);
            if ((newEntry.recipeKey != null && newEntry.recipeKey.equals(existing.recipeKey))
                    || (newEntry.recipeId >= 0 && newEntry.recipeId == existing.recipeId)) {
                existing.amount       = newEntry.amount;
                existing.useMaxAmount = newEntry.useMaxAmount;
                toggleStates.put("craft_useMax_" + i, newEntry.useMaxAmount);
                PackUtilChatField f = textFields.get("craft_amount_" + i);
                if (f != null) f.setText(String.valueOf(newEntry.amount));
                return;
            }
        }

        craftEntries.add(newEntry);
        int idx = craftEntries.size() - 1;
        PackUtilChatField f = makeField(44);
        f.setNumericOnly(true);
        f.setText(String.valueOf(newEntry.amount));
        textFields.put("craft_amount_" + idx, f);
        toggleStates.put("craft_useMax_" + idx, newEntry.useMaxAmount);
    }

    private void rebuildCraftFields() {
        textFields.keySet().removeIf(k -> k.startsWith("craft_amount_"));
        for (int i = 0; i < craftEntries.size(); i++) {
            CraftAction.CraftEntry entry = craftEntries.get(i);
            PackUtilChatField f = makeField(44);
            f.setNumericOnly(true);
            f.setText(String.valueOf(entry.amount));
            textFields.put("craft_amount_" + i, f);
            if (!toggleStates.containsKey("craft_useMax_" + i))
                toggleStates.put("craft_useMax_" + i, entry.useMaxAmount);
        }
        toggleStates.keySet().removeIf(k -> {
            if (!k.startsWith("craft_useMax_")) return false;
            try { return Integer.parseInt(k.substring("craft_useMax_".length())) >= craftEntries.size(); }
            catch (NumberFormatException e) { return true; }
        });
    }

    private void renderDropPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;
        int headerBtnW = 44;

        PackUiText.draw(ctx, textRenderer,
                "Drop Entries (" + dropAction.itemNames.size() + ")", font,
                PackUtilColors.textSecondary(), x, cy + 2, false);
        boolean canClearAll = !dropAction.itemNames.isEmpty();
        int clearX = x + w - headerBtnW;
        renderOverlayButton(ctx, clearX, cy, headerBtnW, 14, "Clear",
                PackUiOverlayButton.Variant.DANGER, canClearAll, mx, my, () -> {
                    dropAction.itemNames.clear();
                    dropAction.itemCounts.clear();
                    clearDropEditSelection();
                    rebuildDropFields();
                });
        cy += 13;

        int selAreaH = SEL_LIST_MAX_VIS * SEL_ITEM_H;

        PackUiScrollViewport dropViewport = getOrCreateViewport(selectedScrollViewports, "_drop_entries",
            x, cy, w, selAreaH, SEL_ITEM_H, SCROLLBAR_W);
        dropViewport.setContentHeight(dropAction.itemNames.size() * SEL_ITEM_H);
        selectedListBounds.put("_drop_entries", new int[]{cy, selAreaH});

        dropViewport.renderScrollbar(ctx, mx, my);

        int sbGap   = SCROLLBAR_W + 2;
        int delW    = 13;
        int dropItemW = w - sbGap - delW - 2;

        if (!dropAction.itemNames.isEmpty()) {
            dropViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
            int firstVis = dropViewport.getFirstVisibleRow();
            for (int i = firstVis; i < dropAction.itemNames.size() && i <= dropViewport.getLastVisibleRow(); i++) {
                int iy = dropViewport.getRowScreenY(i);
                if (iy == Integer.MIN_VALUE) continue;
                String entry = dropAction.itemNames.get(i);
                boolean selected = i == dropEditIndex;

                ItemTarget entryTarget = targetAt(dropAction.itemTargets, dropAction.itemNames, i);
                Component displayName = formatItemTargetText(entryTarget, entry);

                int cnt = i < dropAction.itemCounts.size() ? dropAction.itemCounts.get(i) : 0;
                String summary = " \u2022 " + (cnt == 0 ? "all" : "\u00d7" + cnt);
                Component rowLabel = Component.empty().append(displayName).append(
                        Component.literal(summary).withStyle(s -> s.withColor(0xFF888888)));

                boolean rowHovered = mx >= x && mx < x + dropItemW && my >= iy && my < iy + 13;
                PackUiListRenderer.drawRow(
                        ctx,
                        textRenderer,
                        rowLabel,
                        x,
                        iy,
                        dropItemW,
                        13,
                        rowHovered,
                        selected,
                        PackUiListRenderer.RowTone.NORMAL,
                        true
                );

                if (iy + 13 > cy && iy < cy + selAreaH) {
                    final int fi = i;
                    hitRegions.add(new HitRegion(x, Math.max(cy, iy), dropItemW, Math.min(iy + 13, cy + selAreaH) - Math.max(cy, iy), () -> toggleDropEditSelection(fi)));
                }

                if (iy + 13 > cy && iy < cy + selAreaH) {
                    int delX = x + dropItemW + 2;
                    final int fi = i;
                    renderIconDeleteButton(ctx, delX, iy, delW, mx, my, () -> {
                        dropAction.itemNames.remove(fi);
                        if (fi < dropAction.itemTargets.size()) dropAction.itemTargets.remove(fi);
                        if (fi < dropAction.itemCounts.size()) dropAction.itemCounts.remove(fi);
                        if (dropEditIndex == fi) clearDropEditSelection();
                        else if (dropEditIndex > fi) dropEditIndex--;
                        rebuildDropFields();
                        PackUiScrollViewport vp = selectedScrollViewports.get("_drop_entries");
                        if (vp != null) vp.scrollBy(-1);
                    });
                }
            }
            dropViewport.endRender(ctx);
        } else {
            PackUiListRenderer.drawEmptyState(ctx, textRenderer, "No entries yet. Add an item or exact slot.", x, cy, w - SCROLLBAR_W - 1);
        }
        cy += selAreaH;

        ctx.fill(x, cy + 2, x + w, cy + 3, PackUtilColors.subPanelBorder());
        cy += 6;

        int addPickW = 32;
        int addBtnW  = 34;
        int slotW    = 44;
        PackUtilChatField addF = addFields.get("_drop_add");
        PackUtilChatField entrySlotF = textFields.get("drop_entrySlot");
        if (addF != null && entrySlotF != null) {
            int pickX = x + w - addBtnW - 3 - addPickW;
            int plusX = x + w - addBtnW;
            int slotX = pickX - 3 - slotW;
            addF.setX(x); addF.setY(cy + 1); addF.setWidth(slotX - x - 2);
            addF.render(ctx, mx, my, delta);
            entrySlotF.setX(slotX); entrySlotF.setY(cy + 1); entrySlotF.setWidth(slotW);
            entrySlotF.render(ctx, mx, my, delta);

            boolean capturing = "_drop_entries".equals(itemSlotCapturePendingKey);
            String pickLbl = capturing ? "Done" : "Pick";
            renderOverlayButton(
                    ctx,
                    pickX,
                    cy,
                    addPickW,
                    14,
                    pickLbl,
                    capturing ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.PRIMARY,
                    true,
                    mx,
                    my,
                    () -> {
                if ("_drop_entries".equals(itemSlotCapturePendingKey)) {
                    itemSlotCapturePendingKey = null;
                    exitCaptureMode(false, false);
                } else {
                    itemSlotCapturePendingKey = "_drop_entries";
                    enterCaptureMode();
                }
            });

            String addLbl = dropEditIndex >= 0 ? "New" : "+Add";
            renderOverlayButton(ctx, plusX, cy, addBtnW, 14, addLbl,
                    PackUiOverlayButton.Variant.SUCCESS, true, mx, my, () -> {
                if (dropEditIndex >= 0) clearDropEditSelection();
                else applyDropEntryEditor();
            });
        }
        cy += 16;
        cy += 4;
        boolean editingSelectedDrop = dropEditIndex >= 0 && dropEditIndex < dropAction.itemNames.size();

        String dropHint = editingSelectedDrop
                ? "Editing this row. 0 means drop the full stack from that slot or item."
                : "No row selected. The controls below set defaults for new rows you add.";
        cy = renderEditorHint(ctx, x, cy, w, dropHint);

        {
            boolean dropAllSelected = editingSelectedDrop
                    ? getDropEntryCount(dropEditIndex) == 0
                    : dropAction.mode == autismclient.util.macro.DropAction.DropMode.ALL;
            int lw = labelWidth(w, editingSelectedDrop ? "Selected Row" : "Default Mode", font);
            drawLabel(ctx, editingSelectedDrop ? "Selected Row" : "Default Mode", x, cy, lw, font);
            int ctrlX = controlX(x, lw);
            int ctrlW = controlWidth(w, lw);
            int leftW = (ctrlW - 2) / 2;
            int rightX = ctrlX + leftW + 2;
            int rightW = ctrlW - leftW - 2;
            renderOverlayButton(ctx, ctrlX, cy + 2, leftW, 14, "Drop All",
                    dropAllSelected ? PackUiOverlayButton.Variant.PRIMARY : PackUiOverlayButton.Variant.GHOST,
                    true, mx, my, () -> {
                if (dropEditIndex >= 0 && dropEditIndex < dropAction.itemNames.size()) {
                    setDropEntryCount(dropEditIndex, 0);
                } else {
                    enumIndices.put("drop_mode", 0);
                    dropAction.mode = autismclient.util.macro.DropAction.DropMode.ALL;
                }
                syncDropCountEditorField();
            });
            renderOverlayButton(ctx, rightX, cy + 2, rightW, 14, "Times",
                    !dropAllSelected ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.GHOST,
                    true, mx, my, () -> {
                if (dropEditIndex >= 0 && dropEditIndex < dropAction.itemNames.size()) {
                    setDropEntryCount(dropEditIndex, Math.max(1, getDropEntryCount(dropEditIndex)));
                } else {
                    enumIndices.put("drop_mode", 1);
                    dropAction.mode = autismclient.util.macro.DropAction.DropMode.TIMES;
                }
                syncDropCountEditorField();
            });
        }
        cy += ROW_H + ROW_GAP;

        PackUtilChatField cntF = textFields.get("drop_globalCount");
        syncDropCountEditorField();
        if (cntF != null) {
            int lw = labelWidth(w, editingSelectedDrop ? "Selected Count" : "Default Count", font);
            drawLabel(ctx, editingSelectedDrop ? "Selected Count" : "Default Count", x, cy, lw, font);
            cntF.setX(controlX(x, lw)); cntF.setY(cy + 2); cntF.setWidth(controlWidth(w, lw));
            cntF.render(ctx, mx, my, delta);
            cy += ROW_H + ROW_GAP;
        }

        renderInlineToggle(ctx, font, "drop_waitForGui", "Wait for GUI", x, cy, w, mx, my);
        cy += ROW_H + ROW_GAP;

        if (toggleStates.getOrDefault("drop_waitForGui", false)) {
            PackUtilChatField guiF = textFields.get("drop_guiName");
            if (guiF != null) {
                int lw = labelWidth(w, "GUI Name", font);
                drawLabel(ctx, "GUI Name", x, cy, lw, font);
                guiF.setX(controlX(x, lw)); guiF.setY(cy + 2); guiF.setWidth(controlWidth(w, lw));
                guiF.render(ctx, mx, my, delta);
            }
            cy += ROW_H + ROW_GAP;
        } else {
            PackUtilChatField guiF = textFields.get("drop_guiName");
            if (guiF != null) guiF.setX(-1000);
        }

    }

    private void addDropEntry(ItemTarget target, int count) {
        if (target == null) return;
        String entry = target.toLegacyEntry();
        if (entry.isEmpty()) return;
        addTargetEntry(dropAction.itemTargets, dropAction.itemNames, target);
        int safeCount = Math.max(0, count);
        dropAction.itemCounts.add(safeCount);
        int idx = dropAction.itemNames.size() - 1;
        PackUtilChatField f = makeField(32);
        f.setNumericOnly(true);
        f.setText(String.valueOf(safeCount));
        final int fieldIndex = idx;
        f.setChangedListener(text -> {
            try {
                setDropEntryCount(fieldIndex, Math.max(0, Integer.parseInt(text.strip())));
                if (fieldIndex == dropEditIndex) syncDropCountEditorField();
            } catch (NumberFormatException ignored) {
            }
        });
        textFields.put("drop_count_" + idx, f);
    }

    private void rebuildDropFields() {
        textFields.keySet().removeIf(k -> k.startsWith("drop_count_"));
        trimTargetEntries(dropAction.itemTargets, dropAction.itemNames.size());
        while (dropAction.itemCounts.size() > dropAction.itemNames.size())
            dropAction.itemCounts.remove(dropAction.itemCounts.size() - 1);
        for (int i = 0; i < dropAction.itemNames.size(); i++) {
            PackUtilChatField f = makeField(32);
            f.setNumericOnly(true);
            f.setText(String.valueOf(dropAction.itemCounts.get(i)));
            final int fieldIndex = i;
            f.setChangedListener(text -> {
                try {
                    setDropEntryCount(fieldIndex, Math.max(0, Integer.parseInt(text.strip())));
                    if (fieldIndex == dropEditIndex) syncDropCountEditorField();
                } catch (NumberFormatException ignored) {
                }
            });
            textFields.put("drop_count_" + i, f);
        }
    }

    private void toggleDropEditSelection(int index) {
        if (dropEditIndex == index) {
            clearDropEditSelection();
            return;
        }
        dropEditIndex = index;
        syncDropEntryEditorFromSelection();
        syncDropCountEditorField();
    }

    private void clearDropEditSelection() {
        dropEditIndex = -1;
        PackUtilChatField addF = addFields.get("_drop_add");
        suppressDropEntryLiveUpdate = true;
        if (addF != null) addF.setText("");
        PackUtilChatField slotF = textFields.get("drop_entrySlot");
        if (slotF != null) slotF.setText("");
        suppressDropEntryLiveUpdate = false;
        syncDropCountEditorField();
    }

    private void syncDropEntryEditorFromSelection() {
        PackUtilChatField addF = addFields.get("_drop_add");
        PackUtilChatField slotF = textFields.get("drop_entrySlot");
        if (addF == null || slotF == null) return;
        suppressDropEntryLiveUpdate = true;
        if (dropEditIndex < 0 || dropEditIndex >= dropAction.itemNames.size()) {
            addF.setText("");
            slotF.setText("");
            suppressDropEntryLiveUpdate = false;
            return;
        }
        ItemTarget target = targetAt(dropAction.itemTargets, dropAction.itemNames, dropEditIndex);
        addF.setText(target.editorText());
        slotF.setText(target.hasSlot() ? String.valueOf(target.slot) : "");
        suppressDropEntryLiveUpdate = false;
        syncDropCountEditorField();
    }

    private void applyDropEntryEditor() {
        applyDropEntryEditor(false);
    }

    private void handleDropEntryEditorChanged() {
        if (suppressDropEntryLiveUpdate || dropAction == null) return;
        if (dropEditIndex < 0 || dropEditIndex >= dropAction.itemNames.size()) return;
        applyDropEntryEditor(true);
    }

    private void applyDropEntryEditor(boolean preserveSelection) {
        if (dropAction == null) return;
        PackUtilChatField addF = addFields.get("_drop_add");
        PackUtilChatField slotF = textFields.get("drop_entrySlot");
        if (addF == null || slotF == null) return;
        String nameText = addF.getText().strip();
        String slotText = slotF.getText().strip();
        ItemTarget target = buildEntryTargetFromEditor(nameText, slotText, targetAt(dropAction.itemTargets, dropAction.itemNames, dropEditIndex));
        String entry = target.toLegacyEntry();
        if (entry.isBlank()) return;

        if (dropEditIndex >= 0 && dropEditIndex < dropAction.itemNames.size()) {
            if (!containsEntryOtherThan(dropAction.itemNames, entry, dropEditIndex)) {
                setTargetAt(dropAction.itemTargets, dropAction.itemNames, dropEditIndex, target);
            }
        } else if (!dropAction.itemNames.contains(entry)) {
            addDropEntry(target, currentDropEditorCount());
        }

        if (!preserveSelection) {
            clearDropEditSelection();
        }
    }

    private void applyCapturedDropEntry(ItemTarget target) {
        if (dropAction == null) return;

        String entry = target == null ? "" : target.toLegacyEntry();
        if (entry.isBlank()) return;

        int targetIndex = dropEditIndex;
        if (targetIndex >= 0 && targetIndex < dropAction.itemNames.size()) {
            if (!containsEntryOtherThan(dropAction.itemNames, entry, targetIndex)) {
                setTargetAt(dropAction.itemTargets, dropAction.itemNames, targetIndex, target.copy());
            }
        } else {
            targetIndex = dropAction.itemNames.indexOf(entry);
            if (targetIndex < 0) {
                addDropEntry(target.copy(), currentDropEditorCount());
                targetIndex = dropAction.itemNames.size() - 1;
            }
        }

        dropEditIndex = targetIndex;
        syncDropEntryEditorFromSelection();
        syncDropCountEditorField();
    }

    private int currentDropEditorCount() {
        PackUtilChatField globalCountField = textFields.get("drop_globalCount");
        if (dropEditIndex >= 0 && dropEditIndex < dropAction.itemCounts.size()) {
            return Math.max(0, dropAction.itemCounts.get(dropEditIndex));
        }
        if (enumIndices.getOrDefault("drop_mode", 0) == 0) {
            return 0;
        }
        if (globalCountField != null) {
            try {
                return Math.max(0, Integer.parseInt(globalCountField.getText().strip()));
            } catch (NumberFormatException ignored) {}
        }
        return Math.max(0, dropAction.dropCount);
    }

    private int getDropEntryCount(int index) {
        if (dropAction == null || index < 0 || index >= dropAction.itemNames.size()) return 1;
        while (dropAction.itemCounts.size() <= index) dropAction.itemCounts.add(1);
        return Math.max(0, dropAction.itemCounts.get(index));
    }

    private void setDropEntryCount(int index, int count) {
        if (dropAction == null || index < 0 || index >= dropAction.itemNames.size()) return;
        while (dropAction.itemCounts.size() <= index) dropAction.itemCounts.add(1);
        int safeCount = Math.max(0, count);
        dropAction.itemCounts.set(index, safeCount);
        PackUtilChatField rowField = textFields.get("drop_count_" + index);
        if (rowField != null) rowField.setText(String.valueOf(safeCount));
    }

    private void syncDropCountEditorField() {
        if (dropAction == null) return;
        PackUtilChatField countField = textFields.get("drop_globalCount");
        if (countField == null) return;

        boolean editingSelectedDrop = dropEditIndex >= 0 && dropEditIndex < dropAction.itemNames.size();
        boolean dropAll = editingSelectedDrop
                ? getDropEntryCount(dropEditIndex) == 0
                : dropAction.mode == autismclient.util.macro.DropAction.DropMode.ALL;
        int displayCount = editingSelectedDrop
                ? Math.max(1, getDropEntryCount(dropEditIndex))
                : Math.max(1, dropAction.dropCount);

        suppressDropCountEditorUpdate = true;
        countField.setText(String.valueOf(displayCount));
        countField.setEditable(!dropAll);
        suppressDropCountEditorUpdate = false;
    }

    private void handleDropCountEditorChanged() {
        if (suppressDropCountEditorUpdate || dropAction == null) return;
        PackUtilChatField countField = textFields.get("drop_globalCount");
        if (countField == null) return;

        boolean editingSelectedDrop = dropEditIndex >= 0 && dropEditIndex < dropAction.itemNames.size();
        boolean dropAll = editingSelectedDrop
                ? getDropEntryCount(dropEditIndex) == 0
                : dropAction.mode == autismclient.util.macro.DropAction.DropMode.ALL;
        if (dropAll) return;

        try {
            int value = Math.max(1, Integer.parseInt(countField.getText().strip()));
            if (editingSelectedDrop) {
                setDropEntryCount(dropEditIndex, value);
            } else {
                dropAction.dropCount = value;
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void renderLanStepPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;
        boolean filterByUser = toggleStates.getOrDefault("lan_filterByUser", false);

        renderInlineToggle(ctx, font, "lan_filterByUser", "Specific Peers", x, cy, w, mx, my);
        cy += ROW_H + ROW_GAP;

        PackUtilChatField dsF = textFields.get("lan_defaultStep");
        if (dsF != null && (!filterByUser || lanStepEntries.isEmpty())) {
            int lw = labelWidth(w, filterByUser ? "Fallback Step" : "Any Peer Step", font);
            drawLabel(ctx, filterByUser ? "Fallback Step" : "Any Peer Step", x, cy, lw, font);
            dsF.setX(controlX(x, lw)); dsF.setY(cy + 2); dsF.setWidth(controlWidth(w, lw));
            dsF.render(ctx, mx, my, delta);
            cy += ROW_H + ROW_GAP;
        }

        String summary = !filterByUser
                ? "Continue when any LAN peer reaches the target step."
                : lanStepEntries.isEmpty()
                    ? "Add peers below to narrow it down. Until then, it waits for any peer at the fallback step."
                    : "Each peer below must reach its step before this continues.";
        cy = renderEditorHint(ctx, x, cy, w, summary);

        if (!filterByUser) return;

        if (!lanStepEntries.isEmpty()) {
            PackUiText.draw(ctx, textRenderer,
                    "Peers (" + lanStepEntries.size() + ")", font,
                    PackUtilColors.textSecondary(), x, cy + 2, false);
            cy += 13;
        }

        boolean hasEntries = !lanStepEntries.isEmpty();
        int visibleRows  = hasEntries ? Math.min(3, Math.max(1, lanStepEntries.size())) : 1;
        int selAreaH     = hasEntries ? visibleRows * SEL_ITEM_H : 12;

        PackUiScrollViewport lanViewport = null;
        if (hasEntries) {
            lanViewport = getOrCreateViewport(selectedScrollViewports, "_lan_entries",
                x, cy, w, selAreaH, SEL_ITEM_H, SCROLLBAR_W);
            lanViewport.setContentHeight(lanStepEntries.size() * SEL_ITEM_H);
        }
        selectedListBounds.put("_lan_entries", new int[]{cy, selAreaH});

        if (hasEntries && lanViewport != null) {
            lanViewport.renderScrollbar(ctx, mx, my);
        }

        int sbGap   = SCROLLBAR_W + 2;
        int removeW = 13;
        int stepW   = 40;
        int gapPx   = 2;
        int removeX = x + w - sbGap - removeW;
        int stepX   = removeX - gapPx - stepW;
        int userW   = stepX - x - gapPx;

        if (hasEntries && lanViewport != null) {
            lanViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
            int firstVis = lanViewport.getFirstVisibleRow();
            for (int i = firstVis; i < lanStepEntries.size() && i <= lanViewport.getLastVisibleRow(); i++) {
                int iy = lanViewport.getRowScreenY(i);
                if (iy == Integer.MIN_VALUE) continue;

                ctx.fill(x, iy, stepX - gapPx, iy + 13, 0xFF130C0C);

                PackUtilChatField uf = textFields.get("lan_user_" + i);
                if (uf != null) { uf.setX(x); uf.setY(iy + 1); uf.setWidth(userW); uf.render(ctx, mx, my, delta); }

                PackUtilChatField sf = textFields.get("lan_step_" + i);
                if (sf != null) { sf.setX(stepX); sf.setY(iy + 1); sf.setWidth(stepW); sf.render(ctx, mx, my, delta); }

                final int fi = i;
                renderIconDeleteButton(ctx, removeX, iy + 1, removeW, mx, my, () -> {
                    lanStepEntries.remove(fi);
                    rebuildLanStepFields();
                    PackUiScrollViewport vp = selectedScrollViewports.get("_lan_entries");
                    if (vp != null) vp.scrollBy(-1);
                });
            }
            lanViewport.endRender(ctx);
        } else {
            PackUiListRenderer.drawEmptyState(ctx, textRenderer, "No peer filters yet.", x, cy, w - SCROLLBAR_W - 1);
        }
        cy += selAreaH;

        ctx.fill(x, cy + 2, x + w, cy + 3, PackUtilColors.subPanelBorder());
        cy += 6;

        int addBtnW = 36;
        PackUtilChatField newUF = addFields.get("_lan_user_add");
        PackUtilChatField newSF = addFields.get("_lan_step_add");
        if (newUF != null && newSF != null) {
            int plusX  = x + w - addBtnW;
            int stepAX = plusX - 3 - stepW;
            int userAW = stepAX - x - 2;
            newUF.setX(x); newUF.setY(cy + 1); newUF.setWidth(userAW);
            newUF.render(ctx, mx, my, delta);
            newSF.setX(stepAX); newSF.setY(cy + 1); newSF.setWidth(stepW);
            newSF.render(ctx, mx, my, delta);

            renderOverlayButton(ctx, plusX, cy, addBtnW, 14, "+Add",
                    PackUiOverlayButton.Variant.SUCCESS, true, mx, my, () -> {
                String uname = newUF.getText().strip();
                int step = 1;
                try { step = Math.max(1, Integer.parseInt(newSF.getText().strip())); } catch (NumberFormatException ignored) {}
                lanStepEntries.add(new autismclient.util.macro.WaitForLanStepAction.LanStepEntry(uname, step));
                int idx = lanStepEntries.size() - 1;
                PackUtilChatField uf2 = makeField(80); uf2.setText(uname);
                textFields.put("lan_user_" + idx, uf2);
                PackUtilChatField sf2 = makeField(40); sf2.setNumericOnly(true); sf2.setText(String.valueOf(step));
                textFields.put("lan_step_" + idx, sf2);
                newUF.setText(""); newSF.setText("1");
            });
        }
    }

    private void rebuildLanStepFields() {
        textFields.keySet().removeIf(k -> k.startsWith("lan_user_") || k.startsWith("lan_step_"));
        for (int i = 0; i < lanStepEntries.size(); i++) {
            autismclient.util.macro.WaitForLanStepAction.LanStepEntry e = lanStepEntries.get(i);
            PackUtilChatField uf = makeField(80); uf.setText(e.username);
            textFields.put("lan_user_" + i, uf);
            PackUtilChatField sf = makeField(40); sf.setNumericOnly(true); sf.setText(String.valueOf(e.step));
            textFields.put("lan_step_" + i, sf);
        }
    }

    private void renderPacketActionButtons(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int btnH = 14;
        int halfW = (w - 4) / 2;
        int topY = y + 2;
        int bottomY = y + 20;

        String info = buildPacketActionInfo();
        PackUiText.draw(ctx, textRenderer,
                PackUiText.trimToWidth(textRenderer, info, w, font, -1),
                font, PackUtilColors.textDim(), x, y - 10, false);

        renderActionButton(ctx, x, topY, halfW, btnH, "Queue First", mx, my, () -> {
            List<PackUtilSharedState.QueuedPacket> queue = PackUtilSharedState.get().getDelayedPackets();
            if (queue == null || queue.isEmpty()) {
                PackUtilClientMessaging.sendPrefixed("Queue is empty");
                return;
            }
            setRawPacketActionData(queue.get(0));
            PackUtilClientMessaging.sendPrefixed("Loaded first queued packet");
        });
        renderActionButton(ctx, x + halfW + 4, topY, halfW, btnH, "Paste Base64", mx, my, () -> {
            List<PackUtilSharedState.QueuedPacket> pasted = PackUtilClipboardHelper.pasteFromClipboard();
            if (pasted == null || pasted.isEmpty()) {
                PackUtilClientMessaging.sendPrefixed("Failed to paste packet data");
                return;
            }
            setRawPacketActionData(pasted.get(0));
            PackUtilClientMessaging.sendPrefixed("Loaded first pasted packet");
        });
        renderActionButton(ctx, x, bottomY, w, btnH, "Clear Raw Packet", mx, my, () -> {
            workingTag.putString("packetData", "");
            PackUtilClientMessaging.sendPrefixed("Cleared raw packet data");
        });
    }

    private void renderWaitPacketPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;
        boolean filterByUser = toggleStates.getOrDefault("lan_filterByUser", false);
        int halfW = (w - 4) / 2;
        List<String> c2sTargets = getWaitPacketTargets("C2S");
        List<String> s2cTargets = getWaitPacketTargets("S2C");

        String summary = c2sTargets.isEmpty() && s2cTargets.isEmpty()
            ? "No packets selected. This step will continue on the next packet in either direction."
            : "This step continues as soon as any selected C2S or S2C packet arrives.";
        cy = renderEditorHint(ctx, x, cy, w, summary);

        renderActionButton(ctx, x, cy, halfW, 14, "Add C2S", mx, my, () -> openWaitPacketSelector(true));
        renderActionButton(ctx, x + halfW + 4, cy, halfW, 14, "Add S2C", mx, my, () -> openWaitPacketSelector(false));
        cy += 18;

        renderActionButton(ctx, x, cy, halfW, 14, "Load Queue", mx, my, this::loadWaitPacketTargetsFromQueue);
        renderActionButton(ctx, x + halfW + 4, cy, halfW, 14, "Clear All", mx, my, () -> {
            getOrCreateWaitPacketTargets().clear();
            PackUiScrollViewport vpC2s = selectedScrollViewports.get("wait_packet_c2s");
            if (vpC2s != null) vpC2s.jumpTo(0);
            PackUiScrollViewport vpS2c = selectedScrollViewports.get("wait_packet_s2c");
            if (vpS2c != null) vpS2c.jumpTo(0);
        });
        cy += 20;

        cy = renderSimpleSelectedList(
            ctx,
            x,
            cy,
            w,
            mx,
            my,
            "wait_packet_c2s",
            "C2S Packets",
            c2sTargets,
            this::removeWaitPacketTarget,
            this::formatWaitPacketTarget,
            ignored -> null,
            () -> clearWaitPacketTargets("C2S"),
            "No C2S packets selected"
        );
        cy += 4;

        renderSimpleSelectedList(
            ctx,
            x,
            cy,
            w,
            mx,
            my,
            "wait_packet_s2c",
            "S2C Packets",
            s2cTargets,
            this::removeWaitPacketTarget,
            this::formatWaitPacketTarget,
            ignored -> null,
            () -> clearWaitPacketTargets("S2C"),
            "No S2C packets selected"
        );
    }

    private void renderSendPacketPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        List<PackUtilSharedState.QueuedPacket> actionPackets = getWorkingQueuedPackets();
        List<PackUtilSharedState.QueuedPacket> queuePackets = PackUtilSharedState.get().getDelayedPackets();
        if (queuePackets == null) queuePackets = Collections.emptyList();
        final List<PackUtilSharedState.QueuedPacket> finalQueuePackets = queuePackets;

        PackUiText.draw(ctx, textRenderer,
                "Action Packets (" + actionPackets.size() + ")", font,
                PackUtilColors.textSecondary(), x, cy + 2, false);
        renderOverlayButton(ctx, x + w - 44, cy, 44, 14, "Clear",
                PackUiOverlayButton.Variant.DANGER, !actionPackets.isEmpty(), mx, my, () -> {
                    setWorkingQueuedPackets(Collections.emptyList());
                });
        cy += 13;

        cy = renderQueuedPacketList(ctx, x, cy, w, mx, my, delta,
                "_send_packet_action", actionPackets, true, packetIndex -> {
                    List<PackUtilSharedState.QueuedPacket> updated = getWorkingQueuedPackets();
                    if (packetIndex < 0 || packetIndex >= updated.size()) return;
                    updated.remove(packetIndex);
                    setWorkingQueuedPackets(updated);
                });

        ctx.fill(x, cy + 2, x + w, cy + 3, PackUtilColors.subPanelBorder());
        cy += 6;

        PackUtilChatField nameField = textFields.get("customName");
        if (nameField != null) {
            int lw = labelWidth(w, "Name", font);
            drawLabel(ctx, "Name", x, cy, lw, font);
            nameField.setX(controlX(x, lw)); nameField.setY(cy + 2); nameField.setWidth(controlWidth(w, lw));
            nameField.render(ctx, mx, my, delta);
            cy += ROW_H + ROW_GAP;
        }

        renderInlineToggle(ctx, font, "waitForGui", "Wait for GUI", x, cy, w, mx, my);
        cy += ROW_H + ROW_GAP;

        if (toggleStates.getOrDefault("waitForGui", false)) {
            PackUtilChatField guiField = textFields.get("guiName");
            if (guiField != null) {
                int lw = labelWidth(w, "GUI Name", font);
                drawLabel(ctx, "GUI Name", x, cy, lw, font);
                guiField.setX(controlX(x, lw)); guiField.setY(cy + 2); guiField.setWidth(controlWidth(w, lw));
                guiField.render(ctx, mx, my, delta);
                cy += ROW_H + ROW_GAP;
            }
        } else {
            PackUtilChatField guiField = textFields.get("guiName");
            if (guiField != null) guiField.setX(-1000);
        }

        cy += 2;
        int halfW = (w - 4) / 2;
        renderActionButton(ctx, x, cy, halfW, 14, "From Queue", mx, my, () -> {
            setWorkingQueuedPackets(finalQueuePackets);
            PackUtilClientMessaging.sendPrefixed("Loaded " + finalQueuePackets.size() + " packets from queue");
        });
        renderActionButton(ctx, x + halfW + 4, cy, halfW, 14, "Paste Base64", mx, my, () -> {
            List<PackUtilSharedState.QueuedPacket> pasted = PackUtilClipboardHelper.pasteFromClipboard();
            if (pasted == null || pasted.isEmpty()) {
                PackUtilClientMessaging.sendPrefixed("Failed to paste packets from clipboard");
                return;
            }
            setWorkingQueuedPackets(pasted);
            PackUtilClientMessaging.sendPrefixed("Pasted " + pasted.size() + " packets");
        });
        cy += 18;
        renderActionButton(ctx, x, cy, halfW, 14, "Clear", mx, my, () -> {
            setWorkingQueuedPackets(Collections.emptyList());
            PackUtilClientMessaging.sendPrefixed("Cleared packet list");
        });
        renderActionButton(ctx, x + halfW + 4, cy, halfW, 14, "GBreak", mx, my, this::startGBreakCaptureForEditor);
        cy += 20;

        ctx.fill(x, cy + 2, x + w, cy + 3, PackUtilColors.subPanelBorder());
        cy += 6;

        PackUiText.draw(ctx, textRenderer,
                "Current Queue (" + finalQueuePackets.size() + ")", font,
                PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 13;

        renderQueuedPacketList(ctx, x, cy, w, mx, my, delta,
                "_send_packet_queue", finalQueuePackets, false, packetIndex -> {
                    if (packetIndex < 0 || packetIndex >= finalQueuePackets.size()) return;
                    List<PackUtilSharedState.QueuedPacket> updated = getWorkingQueuedPackets();
                    updated.add(finalQueuePackets.get(packetIndex));
                    setWorkingQueuedPackets(updated);
                });
    }

    private int renderQueuedPacketList(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my, float delta,
                                       String listKey, List<PackUtilSharedState.QueuedPacket> packets,
                                       boolean removable, java.util.function.IntConsumer rowAction) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int listH = SEL_LIST_MAX_VIS * SEL_ITEM_H;

        PackUiScrollViewport packetViewport = getOrCreateViewport(selectedScrollViewports, listKey,
            x, y, w, listH, SEL_ITEM_H, SCROLLBAR_W);
        packetViewport.setContentHeight(packets.size() * SEL_ITEM_H);
        selectedListBounds.put(listKey, new int[]{y, listH});

        packetViewport.renderScrollbar(ctx, mx, my);

        int removeW = removable ? 13 : 0;
        int textW = w - SCROLLBAR_W - 2 - (removable ? (removeW + 2) : 0);
        if (packets.isEmpty()) {
            PackUiText.draw(ctx, textRenderer, removable ? "(none - use GBreak, queue, or Paste)" : "(queue empty)",
                    font, PackUtilColors.textDim(), x, y + 2, false);
            return y + listH;
        }

        packetViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
        int firstVis = packetViewport.getFirstVisibleRow();
        for (int i = firstVis; i < packets.size() && i <= packetViewport.getLastVisibleRow(); i++) {
            int iy = packetViewport.getRowScreenY(i);
            if (iy == Integer.MIN_VALUE) continue;
            PackUtilSharedState.QueuedPacket qp = packets.get(i);
            String pname = qp != null && qp.packet != null ? PackUtilPacketNamer.getFriendlyName(qp.packet) : "???";
            String rowText = (i + 1) + ". " + pname + " d=" + (qp != null ? qp.getDelay() : 0);
            boolean hovered = mx >= x && mx < x + textW && my >= iy && my < iy + 13;
            PackUiListRenderer.drawRow(
                    ctx,
                    textRenderer,
                    rowText,
                    x,
                    iy,
                    textW,
                    13,
                    hovered,
                    false,
                    PackUiListRenderer.RowTone.NORMAL
            );
            final int rowIndex = i;
            if (!removable) {
                hitRegions.add(new HitRegion(x, iy, textW, 13, () -> rowAction.accept(rowIndex)));
            }

            if (removable) {
                int removeX = x + textW + 2;
                renderIconDeleteButton(ctx, removeX, iy, removeW, mx, my, () -> rowAction.accept(rowIndex));
            }
        }
        packetViewport.endRender(ctx);
        return y + listH;
    }

    private void renderSendPacketButtons(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int btnH = 14;
        int halfW = (w - 4) / 2;
        int topY = y + 2;
        int bottomY = y + 20;

        String info = buildSendPacketInfo();
        PackUiText.draw(ctx, textRenderer,
                PackUiText.trimToWidth(textRenderer, info, w, font, -1),
                font, PackUtilColors.textDim(), x, y - 10, false);

        renderActionButton(ctx, x, topY, halfW, btnH, "From Queue", mx, my, () -> {
            List<PackUtilSharedState.QueuedPacket> queue = PackUtilSharedState.get().getDelayedPackets();
            setWorkingQueuedPackets(queue);
            PackUtilClientMessaging.sendPrefixed("Loaded " + getWorkingQueuedPackets().size() + " packets from queue");
        });
        renderActionButton(ctx, x + halfW + 4, topY, halfW, btnH, "Paste Base64", mx, my, () -> {
            List<PackUtilSharedState.QueuedPacket> pasted = PackUtilClipboardHelper.pasteFromClipboard();
            if (pasted == null || pasted.isEmpty()) {
                PackUtilClientMessaging.sendPrefixed("Failed to paste packets from clipboard");
                return;
            }
            setWorkingQueuedPackets(pasted);
            PackUtilClientMessaging.sendPrefixed("Pasted " + pasted.size() + " packets");
        });
        renderActionButton(ctx, x, bottomY, halfW, btnH, "Clear", mx, my, () -> {
            setWorkingQueuedPackets(Collections.emptyList());
            PackUtilClientMessaging.sendPrefixed("Cleared packet list");
        });
        renderActionButton(ctx, x + halfW + 4, bottomY, halfW, btnH, "GBreak", mx, my, () -> {
            PackUtilSharedState.get().startGBreakCapture(() -> MC.execute(() -> {
                List<PackUtilSharedState.QueuedPacket> captured = PackUtilSharedState.get().getGBreakCapturedPackets();
                setWorkingQueuedPackets(captured);
                PackUtilClientMessaging.sendPrefixed(captured.isEmpty()
                        ? "GBreak capture finished with no packet"
                        : "GBreak packet captured");
            }));
            PackUtilClientMessaging.sendPrefixed("Break a block now to capture the GBreak packet");
        });
    }

    private record PayloadEditorState(String channel, byte[] rawBytes, boolean commandApiRecognized,
                                      int commandApiValue, PayloadContentMode contentMode, String contentText) {
        private PayloadEditorState {
            rawBytes = rawBytes == null ? new byte[0] : rawBytes.clone();
            contentMode = contentMode == null ? PayloadContentMode.BINARY_REPLAY : contentMode;
            contentText = contentText == null ? "" : contentText;
        }

        @Override
        public byte[] rawBytes() {
            return rawBytes.clone();
        }

    }

    private void initializePayloadEditorFields(PayloadAction seedAction) {
        PayloadEditorState state = resolvePayloadEditorState(seedAction);
        payloadContentMode = state.contentMode();
        payloadContentEdited = false;
        payloadRawEdited = false;
        payloadChannelEdited = false;
        payloadJsonEdited = false;
        suppressPayloadEditorChange = false;

        PackUtilChatField channelField = makeField(120);
        channelField.setText(state.channel());
        channelField.setPlaceholder(Component.literal("namespace:channel"));
        channelField.setChangedListener(text -> {
            if (!suppressPayloadEditorChange) payloadChannelEdited = true;
        });
        textFields.put("payload_channel", channelField);

        PackUtilChatField contentField = makeField(120);
        contentField.setMultiline(true);
        contentField.setHeight(PAYLOAD_CONTENT_H);
        contentField.setMaxLength(32767);
        contentField.setEditable(state.contentMode() != PayloadContentMode.BINARY_REPLAY);
        if (state.contentMode() == PayloadContentMode.COMMAND_INT) {
            contentField.setFilter(s -> s == null || s.isBlank() || s.equals("-") || s.matches("-?\\d+"));
        }
        contentField.setPlaceholder(Component.literal(state.contentMode() == PayloadContentMode.COMMAND_INT
            ? "Integer value"
            : state.contentMode() == PayloadContentMode.BRAND_STRING
                ? PackUtilPayloadSupport.defaultBrandPayloadString()
                : "Readable payload text"));
        contentField.setText(state.contentText());
        contentField.setChangedListener(text -> {
            if (!suppressPayloadEditorChange) payloadContentEdited = true;
        });
        textFields.put("payload_content", contentField);
    }

    private PayloadEditorState resolvePayloadEditorState(PayloadAction seedAction) {
        PayloadAction action = new PayloadAction();
        if (seedAction != null) {
            action.fromTag(seedAction.toTag());
        }

        String channel = action.channel == null ? "" : action.channel.strip();
        byte[] bytes = new byte[0];
        boolean haveBytes = false;

        if (action.payloadData != null && !action.payloadData.isBlank()) {
            try {
                bytes = PackUtilPayloadSupport.parsePayloadBytes(action.payloadData);
                haveBytes = true;
            } catch (Exception ignored) {
                bytes = new byte[0];
            }
        }

        if (action.payloadJson != null && !action.payloadJson.isBlank()) {
            try {
                action.payloadJson = PackUtilPayloadJsonSupport.normalizeJson(action.payloadJson);
                PackUtilPayloadJsonSupport.EncodedPayload encoded = PackUtilPayloadJsonSupport.encodeAction(action);
                if (channel.isBlank() && encoded.channel() != null && !encoded.channel().isBlank()) {
                    channel = encoded.channel();
                }
                if (!haveBytes) {
                    bytes = encoded.bytes();
                    haveBytes = bytes.length > 0 || action.payloadData == null || action.payloadData.isBlank();
                }
            } catch (Exception ignored) {
            }
        }

        if (channel.isBlank()) {
            channel = "minecraft:brand";
        }

        if (PackUtilPayloadSupport.isBrandChannel(channel)) {
            String brandText = PackUtilPayloadSupport.decodeMinecraftStringPayload(bytes);
            if (brandText != null || bytes.length == 0) {
                return new PayloadEditorState(channel, bytes, false, action.commandApiValue,
                    PayloadContentMode.BRAND_STRING,
                    brandText != null ? brandText : PackUtilPayloadSupport.defaultBrandPayloadString());
            }
        }

        Integer commandValue = PackUtilPayloadSupport.tryParseCommandApiValue(null, channel, bytes);
        boolean commandRecognized = action.commandApiRecognized || commandValue != null;
        int resolvedCommandValue = commandValue != null ? commandValue : action.commandApiValue;
        PayloadContentMode mode = PayloadContentMode.BINARY_REPLAY;
        String contentText = "";
        if (commandRecognized) {
            mode = PayloadContentMode.COMMAND_INT;
            contentText = String.valueOf(resolvedCommandValue);
        } else {
            String readableText = PackUtilPayloadSupport.decodeLikelyUtf8Text(bytes);
            if (!readableText.isBlank() || bytes.length == 0) {
                mode = PayloadContentMode.UTF8_TEXT;
                contentText = prettifyPayloadText(readableText);
            } else {
                contentText = "Binary payload (" + bytes.length + " bytes). Safe replay keeps the captured bytes.";
            }
        }
        return new PayloadEditorState(channel, bytes, commandRecognized, resolvedCommandValue, mode, contentText);
    }

    private String prettifyPayloadText(String text) {
        if (text == null || text.isBlank()) return "";
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String stripped = normalized.strip();
        if (!(stripped.startsWith("{") || stripped.startsWith("["))) {
            return normalized;
        }
        try {
            return PAYLOAD_TEXT_GSON.toJson(JsonParser.parseString(stripped));
        } catch (Throwable ignored) {
            return normalized;
        }
    }

    private void formatPayloadJsonField(boolean syncPreview) {
        PackUtilChatField jsonField = textFields.get("payload_json");
        if (jsonField == null) return;
        jsonField.setText(PackUtilPayloadJsonSupport.normalizeJson(jsonField.getText()));
        if (syncPreview) {
            syncPayloadPreviewFromJson(true);
        }
    }

    private void syncPayloadPreviewFromJson(boolean notifyOnError) {
        PackUtilChatField jsonField = textFields.get("payload_json");
        if (jsonField == null) return;

        PayloadAction preview = new PayloadAction();
        if (payloadAction != null) {
            preview.fromTag(payloadAction.toTag());
        }

        PackUtilChatField channelField = textFields.get("payload_channel");
        PackUtilChatField rawField = textFields.get("payload_data");
        PackUtilChatField textField = textFields.get("payload_text");
        PackUtilChatField commandField = textFields.get("payload_command_value");

        preview.payloadJson = jsonField.getText();
        preview.channel = channelField == null ? preview.channel : channelField.getText().strip();
        preview.payloadData = rawField == null ? preview.payloadData : rawField.getText();
        preview.commandApiRecognized = toggleStates.getOrDefault("payload_command_api", preview.commandApiRecognized);

        if (commandField != null) {
            try {
                preview.commandApiValue = Integer.parseInt(commandField.getText().strip());
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            preview.payloadJson = PackUtilPayloadJsonSupport.normalizeJson(preview.payloadJson);
            PackUtilPayloadJsonSupport.EncodedPayload encoded = PackUtilPayloadJsonSupport.encodeAction(preview);
            suppressPayloadEditorChange = true;
            try {
                jsonField.setText(preview.payloadJson);
                if (channelField != null) {
                    channelField.setText(encoded.channel());
                }
                if (rawField != null) {
                    rawField.setText(PackUtilPayloadSupport.toHex(encoded.bytes()));
                }
                if (textField != null) {
                    textField.setText(PackUtilPayloadSupport.decodeLikelyUtf8Text(encoded.bytes()));
                }
                payloadChannelEdited = true;
                payloadRawEdited = true;
                payloadJsonEdited = false;
            } finally {
                suppressPayloadEditorChange = false;
            }

            Integer commandValue = PackUtilPayloadSupport.tryParseCommandApiValue(null, encoded.channel(), encoded.bytes());
            if (commandValue != null) {
                toggleStates.put("payload_command_api", true);
                if (commandField != null) {
                    commandField.setText(String.valueOf(commandValue));
                }
            }
        } catch (Exception e) {
            if (notifyOnError) {
                PackUtilClientMessaging.sendPrefixed("Â§cCould not rebuild payload from JSON: " + PackUtilPayloadSupport.safeMessage(e));
            }
        }
    }

    private void renderCleanPayloadPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        PackUiText.draw(ctx, textRenderer, "Custom Payload", font,
            PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 13;

        cy = renderPayloadStatus(ctx, x, cy, w);

        PackUtilChatField channelField = textFields.get("payload_channel");
        if (channelField != null) {
            int lw = labelWidth(w, "Channel", font, 64);
            drawLabel(ctx, "Channel", x, cy, lw, font);
            int ctrlX = controlX(x, lw);
            int ctrlW = controlWidth(w, lw);
            channelField.setX(ctrlX);
            channelField.setY(cy + 2);
            channelField.setWidth(ctrlW);
            channelField.render(ctx, mx, my, delta);
            cy += ROW_H + ROW_GAP;
        }

        PackUiText.draw(ctx, textRenderer, payloadContentLabel(), font,
            PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 13;

        PackUtilChatField contentField = textFields.get("payload_content");
        if (contentField != null) {
            contentField.setX(x);
            contentField.setY(cy);
            contentField.setWidth(w);
            contentField.setHeight(PAYLOAD_CONTENT_H);
            contentField.setEditable(payloadContentMode != PayloadContentMode.BINARY_REPLAY);
            contentField.render(ctx, mx, my, delta);
            cy += PAYLOAD_CONTENT_H + 4;
        }

        int halfW = (w - 4) / 2;
        renderOverlayButton(ctx, x, cy, halfW, 16, "Send", PackUiOverlayButton.Variant.SUCCESS, true, mx, my, () -> {
            try {
                PayloadAction action = buildPayloadActionFromEditor();
                action.execute(MC);
            } catch (Exception e) {
                PackUtilClientMessaging.sendPrefixed("Â§cPayload send failed: " + PackUtilPayloadSupport.safeMessage(e));
            }
        });
        renderOverlayButton(ctx, x + halfW + 4, cy, halfW, 16, "Reset", PackUiOverlayButton.Variant.SECONDARY, true, mx, my, () -> {
            resetPayloadEditorFields();
            refreshInteractiveLayout();
        });
        cy += 20;

        renderEditorHint(ctx, x, cy, w, payloadEditorHint());
    }

    private int renderPayloadStatus(GuiGraphicsExtractor ctx, int x, int y, int w) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        String status = payloadReplayStatus();
        String transport = "Transport: normal encoder";
        PackUiText.draw(ctx, textRenderer,
            PackUiText.trimToWidth(textRenderer, status, Math.max(10, w), font, PackUtilColors.textPrimary()),
            font, PackUtilColors.textPrimary(), x, y + 2, false);
        y += 11;
        PackUiText.draw(ctx, textRenderer,
            PackUiText.trimToWidth(textRenderer, transport, Math.max(10, w), font, PackUtilColors.textDim()),
            font, PackUtilColors.textDim(), x, y + 2, false);
        return y + 13;
    }

    private String payloadReplayStatus() {
        if (payloadContentEdited) {
            return switch (payloadContentMode) {
                case COMMAND_INT -> "Edited: integer override will rebuild payload";
                case BRAND_STRING -> "Edited: brand will use Minecraft string encoding";
                case UTF8_TEXT -> "Edited: text will rebuild payload bytes";
                case BINARY_REPLAY -> "Safe replay: binary bytes locked";
            };
        }
        if (payloadChannelEdited) {
            return "Edited: channel override, bytes untouched";
        }
        return switch (payloadContentMode) {
            case BINARY_REPLAY -> "Safe replay: binary bytes locked";
            case BRAND_STRING -> "Safe replay: captured brand bytes untouched";
            default -> "Safe replay: captured bytes untouched";
        };
    }

    private String payloadContentLabel() {
        return switch (payloadContentMode) {
            case COMMAND_INT -> "Payload Value";
            case BRAND_STRING -> "Brand";
            case UTF8_TEXT -> "Payload Component";
            case BINARY_REPLAY -> "Payload";
        };
    }

    private String payloadEditorHint() {
        if (payloadContentMode == PayloadContentMode.BRAND_STRING) {
            return "Brand edits are encoded as a Minecraft string. Untouched replay keeps the exact captured bytes.";
        }
        if (standalonePayloadEditor && payloadContentMode == PayloadContentMode.BINARY_REPLAY) {
            return "Binary content is hidden on purpose. Send replays the exact captured bytes.";
        }
        return payloadContentMode == PayloadContentMode.BINARY_REPLAY
            ? "Binary content is hidden on purpose. Send or Save Macro replays the exact captured bytes."
            : "Leave content untouched for exact replay. Editing content rebuilds only that payload body.";
    }

    private void resetPayloadEditorFields() {
        PayloadEditorState state = resolvePayloadEditorState(payloadAction);
        payloadContentMode = state.contentMode();
        suppressPayloadEditorChange = true;
        try {
            PackUtilChatField channelField = textFields.get("payload_channel");
            if (channelField != null) {
                channelField.setText(state.channel());
            }
            PackUtilChatField contentField = textFields.get("payload_content");
            if (contentField != null) {
                contentField.setEditable(state.contentMode() != PayloadContentMode.BINARY_REPLAY);
                contentField.setText(state.contentText());
            }
            payloadChannelEdited = false;
            payloadContentEdited = false;
            payloadRawEdited = false;
            payloadJsonEdited = false;
        } finally {
            suppressPayloadEditorChange = false;
        }
    }

    private PayloadAction buildCleanPayloadActionFromEditor() {
        PayloadAction action = new PayloadAction();
        if (payloadAction != null) {
            action.fromTag(payloadAction.toTag());
        }
        action.payloadClassName = "";
        action.commandApiOverride = false;
        action.javaSource = "";
        action.payloadScriptEnabled = false;

        PackUtilChatField channelField = textFields.get("payload_channel");
        if (channelField != null) {
            String editorChannel = channelField.getText().strip();
            if (!editorChannel.isBlank()) {
                action.channel = editorChannel;
            }
        }

        PackUtilChatField contentField = textFields.get("payload_content");
        if (payloadContentEdited && contentField != null) {
            if (payloadContentMode == PayloadContentMode.COMMAND_INT) {
                try {
                    int value = Integer.parseInt(contentField.getText().strip());
                    byte[] bytes = PackUtilPayloadSupport.parsePayloadBytes(action.payloadData);
                    action.payloadData = PackUtilPayloadSupport.toHex(PackUtilPayloadSupport.withCommandApiValue(bytes, value));
                    action.commandApiRecognized = true;
                    action.commandApiOverride = true;
                    action.commandApiValue = value;
                } catch (Exception ignored) {
                }
            } else if (payloadContentMode == PayloadContentMode.BRAND_STRING
                && PackUtilPayloadSupport.isBrandChannel(action.channel)) {
                action.payloadData = PackUtilPayloadSupport.toHex(
                    PackUtilPayloadSupport.encodeMinecraftStringPayload(contentField.getText()));
                action.commandApiRecognized = false;
                action.commandApiOverride = false;
            } else if (payloadContentMode == PayloadContentMode.UTF8_TEXT
                || payloadContentMode == PayloadContentMode.BRAND_STRING) {
                action.payloadData = PackUtilPayloadSupport.toHex(contentField.getText().getBytes(StandardCharsets.UTF_8));
                action.commandApiRecognized = false;
                action.commandApiOverride = false;
            }
        }

        if (action.channel == null || action.channel.isBlank()) {
            action.channel = payloadAction != null && payloadAction.channel != null && !payloadAction.channel.isBlank()
                ? payloadAction.channel
                : "minecraft:brand";
        }
        return action;
    }

    private void renderPayloadPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        if (payloadAction != null || targetAction != null) {
            renderCleanPayloadPanel(ctx, x, bodyTop, w, mx, my, delta);
            return;
        }
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        PackUiText.draw(ctx, textRenderer, "Custom Payload Editor", font,
            PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 13;

        if (false) {
            PackUiText.draw(ctx, textRenderer, "Packet JSON", font,
                PackUtilColors.textSecondary(), x, cy + 2, false);
            cy += 13;

            PackUtilChatField editorJsonField = textFields.get("payload_json");
            if (editorJsonField != null) {
                editorJsonField.setX(x);
                editorJsonField.setY(cy);
                editorJsonField.setWidth(w);
                editorJsonField.setHeight(PAYLOAD_JSON_H);
                editorJsonField.render(ctx, mx, my, delta);
                cy += PAYLOAD_JSON_H + 4;
            }

            renderOverlayButton(ctx, x, cy, w, 14, "Send Now", PackUiOverlayButton.Variant.SUCCESS, true, mx, my, () -> {
                try {
                    PayloadAction action = buildPayloadActionFromEditor();
                    action.execute(MC);
                } catch (Exception e) {
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§cPayload send failed: " + PackUtilPayloadSupport.safeMessage(e));
                }
            });
            cy += 14;

            return;
        }

        PackUiText.draw(ctx, textRenderer, "Packet JSON", font,
            PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 13;

        PackUtilChatField jsonField = textFields.get("payload_json");
        if (jsonField != null) {
            jsonField.setX(x);
            jsonField.setY(cy);
            jsonField.setWidth(w);
            jsonField.setHeight(PAYLOAD_JSON_H);
            jsonField.render(ctx, mx, my, delta);
            cy += PAYLOAD_JSON_H + 4;
        }

        int halfW = (w - 4) / 2;
        renderActionButton(ctx, x, cy, halfW, 14, "Pretty JSON", mx, my, () -> {
            formatPayloadJsonField(false);
            refreshInteractiveLayout();
        });
        renderActionButton(ctx, x + halfW + 4, cy, halfW, 14, "Apply JSON", mx, my, () -> {
            syncPayloadPreviewFromJson(true);
            refreshInteractiveLayout();
        });
        cy += 18;

        PackUtilChatField channelField = textFields.get("payload_channel");
        if (channelField != null) {
            int lw = labelWidth(w, "Channel", font, 92);
            drawLabel(ctx, "Channel", x, cy, lw, font);
            int ctrlX = controlX(x, lw);
            int ctrlW = controlWidth(w, lw);
            channelField.setX(ctrlX);
            channelField.setY(cy + 2);
            channelField.setWidth(ctrlW);
            channelField.render(ctx, mx, my, delta);
            cy += ROW_H + ROW_GAP;
        }

        if (toggleStates.getOrDefault("payload_command_api", false)) {
            PackUtilChatField commandField = textFields.get("payload_command_value");
            if (commandField != null) {
                int lw = labelWidth(w, "CommandApi", font, 92);
                drawLabel(ctx, "CommandApi", x, cy, lw, font);
                int ctrlX = controlX(x, lw);
                int ctrlW = controlWidth(w, lw);
                int btnW = 30;
                int fieldW = Math.max(32, ctrlW - btnW - 2);
                commandField.setX(ctrlX);
                commandField.setY(cy + 2);
                commandField.setWidth(fieldW);
                commandField.render(ctx, mx, my, delta);
                renderOverlayButton(ctx, ctrlX + fieldW + 2, cy + 2, btnW, 14, "Sync",
                    PackUiOverlayButton.Variant.GHOST, true, mx, my, () -> {
                        syncPayloadRawFieldFromCommand();
                        refreshInteractiveLayout();
                    });
                cy += ROW_H + ROW_GAP;
            }
        }

        PackUiText.draw(ctx, textRenderer, "UTF-8 Component", font,
            PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 13;

        PackUtilChatField textField = textFields.get("payload_text");
        if (textField != null) {
            textField.setX(x);
            textField.setY(cy);
            textField.setWidth(w);
            textField.setHeight(PAYLOAD_TEXT_H);
            textField.render(ctx, mx, my, delta);
            cy += PAYLOAD_TEXT_H + 4;
        }

        int legacyHalfW = (w - 4) / 2;
        renderActionButton(ctx, x, cy, legacyHalfW, 14, "Component -> Hex", mx, my, () -> {
            syncPayloadRawFieldFromText();
            refreshInteractiveLayout();
        });
        renderActionButton(ctx, x + legacyHalfW + 4, cy, legacyHalfW, 14, "Hex -> Component", mx, my, () -> {
            syncPayloadTextFieldFromRaw();
            refreshInteractiveLayout();
        });
        cy += 18;

        PackUiText.draw(ctx, textRenderer, "Raw Payload", font,
            PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 13;

        PackUtilChatField rawField = textFields.get("payload_data");
        if (rawField != null) {
            rawField.setX(x);
            rawField.setY(cy);
            rawField.setWidth(w);
            rawField.setHeight(PAYLOAD_RAW_H);
            rawField.render(ctx, mx, my, delta);
            cy += PAYLOAD_RAW_H + 4;
        }

        renderActionButton(ctx, x, cy, halfW, 14, "Parse Int", mx, my, () -> {
            syncPayloadCommandFieldFromRaw();
            refreshInteractiveLayout();
        });
        renderActionButton(ctx, x + halfW + 4, cy, halfW, 14, "Normalize", mx, my, () -> {
            PackUtilChatField field = textFields.get("payload_data");
            if (field == null) return;
            try {
                field.setText(PackUtilPayloadSupport.toHex(PackUtilPayloadSupport.parsePayloadBytes(field.getText())));
            } catch (Exception e) {
                PackUtilClientMessaging.sendPrefixed("Â§cInvalid payload data: " + PackUtilPayloadSupport.safeMessage(e));
            }
        });
        cy += 18;

        renderActionButton(ctx, x, cy, halfW, 14, "Seed From Int", mx, my, () -> {
            syncPayloadRawFieldFromCommand();
            refreshInteractiveLayout();
        });
        renderOverlayButton(ctx, x + halfW + 4, cy, halfW, 14, "Send Now", PackUiOverlayButton.Variant.SUCCESS, true, mx, my, () -> {
            try {
                PayloadAction action = buildPayloadActionFromEditor();
                action.execute(MC);
            } catch (Exception e) {
                PackUtilClientMessaging.sendPrefixed("Â§cPayload send failed: " + PackUtilPayloadSupport.safeMessage(e));
            }
        });
        cy += 20;

        PackUiText.draw(ctx, textRenderer, "Runtime Java", font,
            PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 13;

        PackUtilChatField javaField = textFields.get("payload_java");
        if (javaField != null) {
            javaField.setX(x);
            javaField.setY(cy);
            javaField.setWidth(w);
            javaField.setHeight(PAYLOAD_JAVA_H);
            javaField.render(ctx, mx, my, delta);
            cy += PAYLOAD_JAVA_H + 4;
        }

        renderEditorHint(ctx, x, cy, w,
            "Raw replay is exact by default. Use Apply JSON only when you want the JSON view to rebuild the raw bytes.");
    }

    private PayloadAction buildPayloadActionFromEditor() {
        if (payloadAction != null || targetAction != null) {
            return buildCleanPayloadActionFromEditor();
        }
        PayloadAction action = new PayloadAction();
        if (payloadAction != null) {
            action.fromTag(payloadAction.toTag());
        }
        action.payloadClassName = "";

        PackUtilChatField jsonField = textFields.get("payload_json");
        if (jsonField != null) {
            action.payloadJson = jsonField.getText();
        }

        boolean jsonUsable = false;
        PackUtilPayloadJsonSupport.EncodedPayload jsonEncoded = null;
        if (action.payloadJson != null && !action.payloadJson.isBlank()) {
            action.payloadJson = PackUtilPayloadJsonSupport.normalizeJson(action.payloadJson);
            try {
                jsonEncoded = PackUtilPayloadJsonSupport.encodeAction(action);
                jsonUsable = true;
            } catch (Exception ignored) {
            }
        }

        PackUtilChatField channelField = textFields.get("payload_channel");
        if (channelField != null) {
            String editorChannel = channelField.getText().strip();
            if (!editorChannel.isBlank()) {
                action.channel = editorChannel;
            }
        }

        PackUtilChatField rawField = textFields.get("payload_data");
        if (rawField != null) {
            action.payloadData = rawField.getText();
        }
        if (action.payloadData == null || action.payloadData.isBlank()) {
            PackUtilChatField textField = textFields.get("payload_text");
            if (textField != null && !textField.getText().isBlank()) {
                action.payloadData = PackUtilPayloadSupport.toHex(textField.getText().getBytes(StandardCharsets.UTF_8));
            }
        }

        PackUtilChatField javaField = textFields.get("payload_java");
        if (javaField != null) {
            action.javaSource = javaField.getText();
        }

        action.commandApiRecognized = toggleStates.getOrDefault("payload_command_api", false);
        PackUtilChatField commandField = textFields.get("payload_command_value");
        if (commandField != null) {
            try {
                action.commandApiValue = Integer.parseInt(commandField.getText().strip());
            } catch (NumberFormatException ignored) {
            }
        }

        if (payloadJsonEdited && jsonUsable && jsonEncoded != null && !payloadRawEdited && !payloadChannelEdited) {
            try {
                jsonEncoded = PackUtilPayloadJsonSupport.encodeAction(action);
                action.channel = jsonEncoded.channel();
                action.payloadData = PackUtilPayloadSupport.toHex(jsonEncoded.bytes());
            } catch (Exception ignored) {
            }
        }

        if (action.commandApiRecognized) {
            try {
                byte[] bytes = PackUtilPayloadSupport.parsePayloadBytes(action.payloadData);
                action.payloadData = PackUtilPayloadSupport.toHex(PackUtilPayloadSupport.withCommandApiValue(bytes, action.commandApiValue));
            } catch (Exception ignored) {
            }
        }

        action.payloadClassName = "";

        if (action.channel == null || action.channel.isBlank()) {
            action.channel = payloadAction != null && payloadAction.channel != null && !payloadAction.channel.isBlank()
                ? payloadAction.channel
                : "minecraft:brand";
        }
        return action;
    }

    private void syncPayloadCommandFieldFromRaw() {
        PackUtilChatField rawField = textFields.get("payload_data");
        PackUtilChatField commandField = textFields.get("payload_command_value");
        PackUtilChatField channelField = textFields.get("payload_channel");
        if (rawField == null || commandField == null) return;
        try {
            byte[] bytes = PackUtilPayloadSupport.parsePayloadBytes(rawField.getText());
            Integer value = PackUtilPayloadSupport.tryParseCommandApiValue(null,
                channelField == null ? "" : channelField.getText(), bytes);
            if (value == null) {
                PackUtilClientMessaging.sendPrefixed("Â§cCould not parse a CommandApi integer from the current payload bytes.");
                return;
            }
            toggleStates.put("payload_command_api", true);
            commandField.setText(String.valueOf(value));
        } catch (Exception e) {
            PackUtilClientMessaging.sendPrefixed("Â§cInvalid payload data: " + PackUtilPayloadSupport.safeMessage(e));
        }
    }

    private void syncPayloadRawFieldFromCommand() {
        PackUtilChatField rawField = textFields.get("payload_data");
        PackUtilChatField commandField = textFields.get("payload_command_value");
        if (rawField == null || commandField == null) return;
        try {
            byte[] bytes = PackUtilPayloadSupport.parsePayloadBytes(rawField.getText());
            int value = Integer.parseInt(commandField.getText().strip());
            toggleStates.put("payload_command_api", true);
            rawField.setText(PackUtilPayloadSupport.toHex(PackUtilPayloadSupport.withCommandApiValue(bytes, value)));
        } catch (Exception e) {
            PackUtilClientMessaging.sendPrefixed("Â§cFailed to sync CommandApi value: " + PackUtilPayloadSupport.safeMessage(e));
        }
    }

    private void syncPayloadTextFieldFromRaw() {
        PackUtilChatField rawField = textFields.get("payload_data");
        PackUtilChatField textField = textFields.get("payload_text");
        if (rawField == null || textField == null) return;
        try {
            byte[] bytes = PackUtilPayloadSupport.parsePayloadBytes(rawField.getText());
            String text = PackUtilPayloadSupport.decodeLikelyUtf8Text(bytes);
            if (text.isBlank()) {
                PackUtilClientMessaging.sendPrefixed("Ã‚Â§cThe current payload bytes do not look like readable UTF-8 text.");
                return;
            }
            textField.setText(text);
        } catch (Exception e) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cInvalid payload data: " + PackUtilPayloadSupport.safeMessage(e));
        }
    }

    private void syncPayloadRawFieldFromText() {
        PackUtilChatField rawField = textFields.get("payload_data");
        PackUtilChatField textField = textFields.get("payload_text");
        if (rawField == null || textField == null) return;
        rawField.setText(PackUtilPayloadSupport.toHex(textField.getText().getBytes(StandardCharsets.UTF_8)));
    }

    private void renderDelayPacketsPresetButtons(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my) {
        int btnH = 14;
        int halfW = (w - 4) / 2;
        int x2 = x + halfW + 4;
        renderOverlayButton(ctx, x, y + 2, halfW, btnH, "Default", PackUiOverlayButton.Variant.SECONDARY, true, mx, my, () -> {
            autismclient.util.macro.DelayPacketsAction tmp = new autismclient.util.macro.DelayPacketsAction();
            tmp.applyDefaultPreset();
            stringLists.put("c2sPackets", new ArrayList<>(tmp.c2sPacketNames));
            stringLists.put("s2cPackets", new ArrayList<>(tmp.s2cPacketNames));
        });
        renderOverlayButton(ctx, x2, y + 2, halfW, btnH, "Module", PackUiOverlayButton.Variant.PRIMARY, true, mx, my, () -> {
            autismclient.util.macro.DelayPacketsAction tmp = new autismclient.util.macro.DelayPacketsAction();
            tmp.applyModulePreset();
            stringLists.put("c2sPackets", new ArrayList<>(tmp.c2sPacketNames));
            stringLists.put("s2cPackets", new ArrayList<>(tmp.s2cPacketNames));
        });
    }

    private PackUiOverlayButton renderOverlayButton(
            GuiGraphicsExtractor ctx,
            int x,
            int y,
            int w,
            int h,
            String label,
            PackUiOverlayButton.Variant variant,
            boolean active,
            int mx,
            int my,
            Runnable action
    ) {
        PackUiOverlayButton button = PackUiOverlayButton.create(x, y, w, h, Component.literal(label), ignored -> action.run());
        button.setVariant(variant);
        button.active = active;
        PackUiOverlayButton.renderStyled(ctx, textRenderer, button, mx, my);
        hitRegions.add(new HitRegion(button, action));
        return button;
    }

    private void renderIconDeleteButton(GuiGraphicsExtractor ctx, int x, int y, int size, int mx, int my, Runnable action) {
        boolean hovered = mx >= x && mx < x + size && my >= y && my < y + size;
        PackUiListRenderer.drawIconButton(ctx, x, y, size, PackUiAssets.ICON_WINDOW_CLOSE, hovered, true);
        hitRegions.add(new HitRegion(x, y, size, size, (mouseX, mouseY, mouseButton) -> {
            if (mouseButton != 0) return false;
            PackUiButtonFeedback.forKey("list-icon:" + x + ':' + y + ':' + size + ':' + true + ':' + PackUiAssets.ICON_WINDOW_CLOSE)
                    .triggerPress((float) (mouseX - x), (float) (mouseY - y), size, size);
            action.run();
            return true;
        }));
    }

    private void renderActionButton(GuiGraphicsExtractor ctx, int x, int y, int w, int h,
                                    String label, int mx, int my, Runnable action) {
        renderOverlayButton(ctx, x, y, w, h, label, PackUiOverlayButton.Variant.SECONDARY, true, mx, my, action);
    }

    private int renderEditorHint(GuiGraphicsExtractor ctx, int x, int y, int w, String text) {
        return renderEditorHint(ctx, x, y, w, text, PackUtilColors.textDim());
    }

    private int renderEditorHint(GuiGraphicsExtractor ctx, int x, int y, int w, String text, int color) {
        String hint = formatEditorHint(text);
        if (hint.isEmpty()) return y;
        Identifier font = theme.fontFor(PackUiTone.BODY);
        PackUiText.draw(ctx, textRenderer,
                PackUiText.trimToWidth(textRenderer, hint, Math.max(10, w), font, color),
                font, color, x, y + 2, false);
        return y + EDITOR_HINT_ROW_H;
    }

    private String formatEditorHint(String text) {
        if (text == null) return "";
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) return "";

        String exact = switch (normalized) {
            case "Blank name + slot = exact slot. Name + slot = that item in that slot." -> "Blank name + slot = exact slot.";
            case "Swap uses the blue H1-H9 button to pick which hotbar slot gets swapped." -> "Use H1-H9 to pick the swap slot.";
            case "Editing this row. 0 means drop the full stack from that slot or item." -> "Editing row. 0 = full stack.";
            case "No row selected. The controls below set defaults for new rows you add." -> "No row picked. Controls set new-row defaults.";
            case "Continue when any LAN peer reaches the target step." -> "Waits for any LAN peer step.";
            case "Add peers below to narrow it down. Until then, it waits for any peer at the fallback step." -> "Add peers below or use fallback.";
            case "Each peer below must reach its step before this continues." -> "All listed peers must reach their step.";
            case "No packets selected. This step will continue on the next packet in either direction." -> "No filters: next packet continues.";
            case "This step continues as soon as any selected C2S or S2C packet arrives." -> "Waits for any chosen packet.";
            case "Capture only marks the block. It will not open it." -> "Capture only marks the block.";
            case "0,0,0 uses the current container when open, otherwise the closest nearby one." -> "0,0,0 = current or nearest container.";
            case "Pick on an item adds the item. Pick on an empty slot adds that exact slot." -> "Filled slot = item. Empty slot = exact slot.";
            case "Pick only accepts chest/custom GUI slots here." -> "Pick only works on GUI slots here.";
            case "Pick only accepts player inventory slots here." -> "Pick only works in your inventory here.";
            case "No payment targets selected yet." -> "No players selected.";
            case "Click a scanned name to toggle it, or use All to add the filtered scan results." -> "Click names to toggle, or use All.";
            case "The search box also lets you manually add a player by pressing Enter." -> "Press Enter to add a typed player.";
            case "Pick modules below, then click the mode chip on each row." -> "Pick modules, then set each mode.";
            case "Each row runs with its own Toggle / Enable / Disable setting." -> "Each row keeps its own mode.";
            case "Capture View fills in your current yaw and pitch." -> "Capture View fills yaw and pitch.";
            case "Wait for Arrival continues only after Baritone finishes." -> "Waits until Baritone arrives.";
            case "Simple disconnect. Delay only controls how long to wait before closing the connection." -> "Simple disconnect after the delay.";
            case "Sends lag packets, then the selected kick packet. Packet Count controls how hard it pushes." -> "Sends lag, then the kick packet.";
            case "Kick Dupe will run the next eligible macro actions inside the lag sandwich, then kick." -> "Runs next actions inside the dupe kick.";
            case "Kick Dupe will try to use a bundle, then kick." -> "Tries bundle dupe, then kicks.";
            case "Automatically detects teleport/world change and disconnects at the perfect timing for dupe exploits." -> "Auto-disconnects at the perfect dupe timing.";
            case "Waits until the current GUI closes." -> "Waits for the GUI to close.";
            case "Waits until a GUI opens." -> "Waits for any GUI to open.";
            case "Blank pattern waits for any incoming server or plugin message." -> "Blank pattern = any server message.";
            case "Blank pattern waits for any incoming chat line." -> "Blank pattern = any chat line.";
            case "Regex is on, so the pattern must match with Java regex rules." -> "Regex mode uses Java regex rules.";
            case "No Meteor modules found right now." -> "No modules found.";
            case "Press Scan to load the current server players." -> "Press Scan for players.";
            case "Click a row to use that message." -> "Click a row to use it.";
            case "Pick target blocks, then choose the stop rule you want." -> "Pick blocks, then choose a stop rule.";
            default -> "";
        };
        if (!exact.isEmpty()) return fitEditorHintText(exact);

        normalized = normalized
                .replace("Waits until", "Waits for")
                .replace("Continue when", "Continues when")
                .replace("This step continues as soon as", "Continues when")
                .replace("the selected", "chosen")
                .replace("the current", "this")
                .replace("current ", "")
                .replace("number of times", "times")
                .replace("the chosen number of times", "times")
                .replace("the chosen ticks", "set ticks")
                .replace("only after", "after")
                .replace("It will not", "Won't")
                .replace("selected kick packet", "kick packet")
                .replace("Packet Count controls how hard it pushes", "Packet Count controls force")
                .replace("the open handler", "the open GUI")
                .replace("player inventory", "inventory")
                .replace("inventory slot", "slot")
                .replace("the matched item into", "it into")
                .replace("first matching visible slot", "first visible match")
                .replace("contains an item that matches the optional name", "matches the optional item")
                .replace("becomes empty", "turns empty")
                .replace("reaches the target count", "hits the target count")
                .replace("drops below the target count", "drops below the target")
                .replace("on the first content or count change in that exact slot", "on the first exact slot change")
                .replace("the block under your crosshair", "your target block")
                .replace("the entity under your crosshair", "your target entity")
                .replace("whatever is under your crosshair", "your target")
                .replace("will attack or break", "acts on")
                .replace("will interact with", "uses")
                .replace("the currently selected hotbar item", "the held item");

        return fitEditorHintText(normalized);
    }

    private void applyEditorPlaceholders() {
        if (schema != null) {
            for (FieldDef field : schema.fields()) {
                switch (field.type()) {
                    case TEXT, MACRO_SELECT, NUMBER, DECIMAL, SLOT -> applyTextFieldPlaceholder(field.key(), resolveFieldPlaceholder(field));
                    case BLOCK_POS -> applyBlockPosPlaceholders(field.key());
                    default -> {
                    }
                }
            }
        }

        applyTextFieldPlaceholder("item_guiName", "Any GUI");
        applyTextFieldPlaceholder("drop_guiName", "Any GUI");
        applyTextFieldPlaceholder("lan_defaultStep", "Any step");
        applyTextFieldPlaceholder("item_entrySlot", "Slot");
        applyTextFieldPlaceholder("drop_entrySlot", "Slot #");
        applyTextFieldPlaceholder("_craft_amount", "1");
        applyTextFieldPlaceholder("_lan_step_add", "1");
        applyTextFieldPlaceholder("drop_globalCount", "1");
        applyTextFieldPlaceholder("amountInput", "1k");

        applyAddFieldPlaceholder("_item_add", "Any item");
        applyAddFieldPlaceholder("_drop_add", "Any item");
        applyAddFieldPlaceholder("_craft_search", "Find recipe");
        applyAddFieldPlaceholder("_lan_user_add", "Peer name");
        applyAddFieldPlaceholder("soundIds", "Find sound");
        applyAddFieldPlaceholder("entityIds", "Find entity");
        applyAddFieldPlaceholder("targetItems", "Item name");
        applyAddFieldPlaceholder("players", "Find player");
        applyAddFieldPlaceholder("_toggle_module_search", "Find module");
        applyAddFieldPlaceholder("_wait_chat_search", "Find chat");
    }

    private void applyTextFieldPlaceholder(String key, String placeholder) {
        PackUtilChatField field = textFields.get(key);
        setEditorPlaceholder(field, placeholder);
    }

    private void applyAddFieldPlaceholder(String key, String placeholder) {
        PackUtilChatField field = addFields.get(key);
        setEditorPlaceholder(field, placeholder);
    }

    private void applyBlockPosPlaceholders(String key) {
        applyTextFieldPlaceholder(key + "_0", "X");
        applyTextFieldPlaceholder(key + "_1", "Y");
        applyTextFieldPlaceholder(key + "_2", "Z");
    }

    private void setEditorPlaceholder(PackUtilChatField field, String placeholder) {
        if (field == null || placeholder == null || placeholder.isBlank()) return;
        field.setPlaceholder(Component.literal(fitEditorGhostText(placeholder)));
    }

    private String resolveFieldPlaceholder(FieldDef field) {
        if (field == null) return "";
        return switch (field.key()) {
            case "guiName", "waitGuiName" -> "Any GUI";
            case "guiTitle" -> "Any GUI";
            case "pattern" -> "Any chat";
            case "message" -> "Type message";
            case "description" -> "Quick note";
            case "customText" -> "Optional text";
            case "title" -> "Book title";
            case "customName" -> "Optional name";
            case "commandTemplate" -> "/pay <p> <amt>";
            case "amountInput" -> "1k";
            case "itemName", "fromItemName", "toItemName" -> "Any item";
            case "moduleName" -> "Pick below";
            case "slot", "fromSlot", "toSlot", "slotNumber", "targetSlot" -> "Slot #";
            case "healthThreshold" -> "0-20";
            case "yaw", "pitch" -> "0";
            case "delayMs" -> "0";
            case "delayTicks" -> "0";
            case "clickCount", "useCount", "holdTicks", "packetCount", "maxDistance", "stopSlotsUsed",
                 "slotsUsedThreshold", "minedCountTarget", "timeoutSeconds", "durationTicks",
                 "tickOffset", "preGenCount", "revisionOffset", "maxWaitMs", "bufferMs" -> "1";
            default -> switch (field.type()) {
                case TEXT, MACRO_SELECT -> fitEditorGhostText(field.label());
                case SLOT -> "Slot #";
                case NUMBER, DECIMAL -> "0";
                default -> "";
            };
        };
    }

    private String fitEditorGhostText(String text) {
        String normalized = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= EDITOR_GHOST_MAX_CHARS) return normalized;
        int cutoff = normalized.lastIndexOf(' ', EDITOR_GHOST_MAX_CHARS - 3);
        if (cutoff < 5) cutoff = EDITOR_GHOST_MAX_CHARS - 3;
        return normalized.substring(0, Math.max(1, cutoff)).trim() + "...";
    }

    private String fitEditorHintText(String text) {
        String normalized = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= EDITOR_HINT_MAX_CHARS) return normalized;
        int cutoff = normalized.lastIndexOf(' ', EDITOR_HINT_MAX_CHARS - 3);
        if (cutoff < 8) cutoff = EDITOR_HINT_MAX_CHARS - 3;
        return normalized.substring(0, Math.max(1, cutoff)).trim() + "...";
    }

    private void renderRow(GuiGraphicsExtractor ctx, FieldDef field, int x, int y, int w,
                           int mx, int my, float delta) {
        switch (field.type()) {
            case TOGGLE      -> renderToggle   (ctx, field, x, y, w, mx, my);
            case NUMBER,
                 DECIMAL,
                 TEXT,
                 MACRO_SELECT,
                 SLOT        -> renderTextField(ctx, field, x, y, w, mx, my, delta);
            case ENUM        -> renderEnum     (ctx, field, x, y, w, mx, my);
            case BLOCK_POS   -> renderBlockPos (ctx, field, x, y, w, mx, my, delta);
            case STRING_LIST -> {
                if (field.captureMode() == CaptureMode.BLOCK_CATALOG)
                    renderStringListCatalog(ctx, field, x, y, w, mx, my, delta);
                else
                    renderStringList(ctx, field, x, y, w, mx, my, delta);
            }
        }
    }

    private void renderToggle(GuiGraphicsExtractor ctx, FieldDef field, int x, int y, int w,
                              int mx, int my) {
        String key = field.key();
        boolean val = toggleStates.getOrDefault(key, false);
        Identifier font = theme.fontFor(PackUiTone.BODY);

        int btnW = 34;
        int btnH = 14;
        int lw = labelWidth(w, field.label(), font, btnW);
        drawLabel(ctx, field.label(), x, y, lw, font);

        int btnX = x + w - btnW;
        renderOverlayButton(
                ctx,
                btnX,
                y + 2,
                btnW,
                btnH,
                val ? "ON" : "OFF",
                val ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.DANGER,
                true,
                mx,
                my,
                () -> {
            boolean nowOn = !toggleStates.getOrDefault(key, false);
            toggleStates.put(key, nowOn);
            if (nowOn && field.hasMutualExclusion()) {
                for (String other : field.mutuallyExclusiveWith()) toggleStates.put(other, false);
            }
        });
    }

    private void renderTextField(GuiGraphicsExtractor ctx, FieldDef field, int x, int y, int w,
                                 int mx, int my, float delta) {
        String key = field.key();
        PackUtilChatField tf = textFields.get(key);
        if (tf == null) return;
        Identifier font = theme.fontFor(PackUiTone.BODY);

        if (field.type() == FieldType.MACRO_SELECT) {
            renderMacroSelector(ctx, field, tf, x, y, w, mx, my);
            return;
        }

        if (isWaitChatPatternField(field)) {
            PackUiText.draw(ctx, textRenderer, field.label(), font,
                    PackUtilColors.textSecondary(), x, y + 2, false);
            tf.setX(x);
            tf.setY(y + 13);
            tf.setWidth(w);
            tf.setHeight(WAIT_CHAT_PATTERN_H);
            tf.render(ctx, mx, my, delta);
            return;
        }

        int lw = labelWidth(w, field.label(), font, field.captureMode() == CaptureMode.ITEM_SLOT ? 88 : 56);
        drawLabel(ctx, field.label(), x, y, lw, font);

        int tfX = controlX(x, lw);

        if (field.captureMode() == CaptureMode.ITEM_SLOT) {

            int pickW  = 30;
            int tfW    = controlWidth(w, lw) - pickW - 2;
            tf.setX(tfX); tf.setY(y + 2); tf.setWidth(tfW);
            tf.render(ctx, mx, my, delta);

            int pkX       = tfX + tfW + 2;
            boolean capt  = key.equals(itemSlotCapturePendingKey);
            final String fKey = key;
            renderOverlayButton(
                    ctx,
                    pkX,
                    y + 2,
                    pickW,
                    14,
                    capt ? "Done" : "Pick",
                    capt ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.PRIMARY,
                    true,
                    mx,
                    my,
                    () -> {
                        if (fKey.equals(itemSlotCapturePendingKey)) {
                            itemSlotCapturePendingKey = null;
                            exitCaptureMode(false, false);
                        } else {
                            itemSlotCapturePendingKey = fKey;
                            enterCaptureMode();
                        }
                    }
            );
        } else {
            int tfW = controlWidth(w, lw);
            tf.setX(tfX); tf.setY(y + 2); tf.setWidth(tfW);
            tf.render(ctx, mx, my, delta);
        }
    }

    private void renderMacroSelector(GuiGraphicsExtractor ctx, FieldDef field, PackUtilChatField backingField,
                                     int x, int y, int w, int mx, int my) {
        List<String> macros = availableMacroNames(backingField.getText());
        Identifier font = theme.fontFor(PackUiTone.BODY);
        String current = backingField.getText() == null ? "" : backingField.getText();
        if (current.isBlank() && !macros.isEmpty()) {
            backingField.setText(macros.get(0));
            current = backingField.getText();
        }

        PackUiText.draw(ctx, textRenderer, field.label(), font, PackUtilColors.textSecondary(), x, y + 2, false);
        int badgeW = Math.min(150, Math.max(60, w / 2));
        int badgeX = x + w - badgeW;
        ctx.fill(badgeX, y + 1, badgeX + badgeW, y + 15, 0xFF160E0E);
        String selectedLabel = current.isBlank() ? "No macro selected" : current;
        String selectedTrimmed = PackUiText.trimToWidth(textRenderer, selectedLabel, badgeW - 6, font, -1);
        PackUiText.draw(ctx, textRenderer, selectedTrimmed, font, current.isBlank() ? PackUtilColors.textMuted() : PackUtilColors.textPrimary(), badgeX + 3, y + 3, false);

        int listY = y + 17;
        int listH = MACRO_SELECT_VISIBLE_ROWS * SEL_ITEM_H;
        int itemW = w - SCROLLBAR_W - 1;
        String viewportKey = "macro_select_" + field.key();
        PackUiScrollViewport viewport = getOrCreateViewport(selectedScrollViewports, viewportKey, x, listY, w, listH, SEL_ITEM_H, SCROLLBAR_W);
        viewport.setContentHeight(macros.size() * SEL_ITEM_H);
        selectedListBounds.put(viewportKey, new int[]{listY, listH});
        viewport.renderScrollbar(ctx, mx, my);

        if (macros.isEmpty()) {
            ctx.fill(x, listY, x + itemW, listY + 13, PackUtilColors.rowNormal());
            PackUiListRenderer.drawEmptyState(ctx, textRenderer, "No saved macros", x, listY, itemW);
            return;
        }

        viewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
        int first = viewport.getFirstVisibleRow();
        for (int i = first; i < macros.size() && i <= viewport.getLastVisibleRow(); i++) {
            String macroName = macros.get(i);
            int iy = viewport.getRowScreenY(i);
            if (iy == Integer.MIN_VALUE) continue;
            boolean selected = macroName.equals(current);
            boolean hovered = mx >= x && mx < x + itemW && my >= iy && my < iy + SEL_ITEM_H;
            PackUiListRenderer.drawRow(ctx, textRenderer, macroName, x, iy, itemW, SEL_ITEM_H, hovered, selected, PackUiListRenderer.RowTone.NORMAL);
            final String selectedMacro = macroName;
            hitRegions.add(new HitRegion(x, iy, itemW, SEL_ITEM_H, () -> backingField.setText(selectedMacro)));
        }
        viewport.endRender(ctx);
    }

    private List<String> availableMacroNames(String currentValue) {
        List<String> names = new ArrayList<>();
        for (PackUtilMacro macro : PackUtilMacroManager.get().getAll()) {
            if (macro != null && macro.name != null && !macro.name.isBlank()) {
                names.add(macro.name);
            }
        }
        if (currentValue != null && !currentValue.isBlank() && !names.contains(currentValue)) {
            names.add(0, currentValue);
        }
        return names;
    }

    private boolean isWaitChatPatternField(FieldDef field) {
        return field != null
                && "pattern".equals(field.key())
                && targetAction != null
                && targetAction.getType() == MacroActionType.WAIT_CHAT;
    }

    private void renderEnum(GuiGraphicsExtractor ctx, FieldDef field, int x, int y, int w,
                            int mx, int my) {
        String key = field.key();
        List<String> opts = field.enumOptions();
        if (opts.isEmpty()) return;
        int idx = Math.min(enumIndices.getOrDefault(key, 0), opts.size() - 1);
        String val  = opts.get(idx);
        Identifier font = theme.fontFor(PackUiTone.BODY);

        int lw = labelWidth(w, field.label(), font, 72);
        drawLabel(ctx, field.label(), x, y, lw, font);

        int ctrlX = controlX(x, lw);
        int ctrlW = controlWidth(w, lw);
        int arwW  = 10, arwH = 14;
        int valW  = ctrlW - arwW * 2 - 4;

        renderOverlayButton(
                ctx,
                ctrlX,
                y + 2,
                arwW,
                arwH,
                "<",
                PackUiOverlayButton.Variant.GHOST,
                true,
                mx,
                my,
                () -> enumIndices.put(key, (enumIndices.getOrDefault(key, 0) - 1 + opts.size()) % opts.size())
        );

        int valX = ctrlX + arwW + 2;
        ctx.fill(valX, y + 2, valX + valW, y + 2 + arwH, 0xFF160E0E);
        String trimmed = PackUiText.trimToWidth(textRenderer, val, valW - 4, font, -1);
        int textX = valX + Math.max(0, (valW - uiWidth(font, trimmed)) / 2);
        int enumTextY = PackUiSizing.alignTextY(y + 2, arwH, theme.fontHeight(PackUiTone.BODY), theme.buttonTextNudge());
        PackUiText.draw(ctx, textRenderer, trimmed, font, PackUtilColors.textPrimary(), textX, enumTextY, false);

        int nxtX = ctrlX + ctrlW - arwW;
        renderOverlayButton(
                ctx,
                nxtX,
                y + 2,
                arwW,
                arwH,
                ">",
                PackUiOverlayButton.Variant.GHOST,
                true,
                mx,
                my,
                () -> enumIndices.put(key, (enumIndices.getOrDefault(key, 0) + 1) % opts.size())
        );
    }

    private void renderBlockPos(GuiGraphicsExtractor ctx, FieldDef field, int x, int y, int w,
                                int mx, int my, float delta) {
        String key  = field.key();
        Identifier font = theme.fontFor(PackUiTone.BODY);
        String[] axis = {"X", "Y", "Z"};
        boolean hasCapture = field.captureMode() != CaptureMode.NONE;

        int capBtnW = CAPTURE_BTN_W, capBtnH = 14;
        int labelW  = hasCapture ? w - capBtnW - 4 : w;
        PackUiText.draw(ctx, textRenderer, field.label(), font,
                PackUtilColors.textSecondary(), x, y + 3, false);

        if (hasCapture) {
            int cbX = x + labelW + 4;
            renderOverlayButton(
                    ctx,
                    cbX,
                    y,
                    capBtnW,
                    capBtnH,
                    "Capture",
                    PackUiOverlayButton.Variant.PRIMARY,
                    true,
                    mx,
                    my,
                    () -> startBlockPosCapture(field)
            );
        }

        int fieldW = (w - 4) / 3;
        for (int i = 0; i < 3; i++) {
            PackUtilChatField tf = textFields.get(key + "_" + i);
            if (tf == null) continue;
            int fx = x + i * (fieldW + 2);
            PackUiText.draw(ctx, textRenderer, axis[i], font,
                    PackUtilColors.textDim(), fx, y + ROW_H + 3, false);
            tf.setX(fx + 9); tf.setY(y + ROW_H + 1); tf.setWidth(fieldW - 9);
            tf.render(ctx, mx, my, delta);
        }
    }

    private void renderStringList(GuiGraphicsExtractor ctx, FieldDef field, int x, int y, int w,
                                  int mx, int my, float delta) {
        String key        = field.key();
        List<String> lst  = stringLists.getOrDefault(key, Collections.emptyList());
        Identifier font   = theme.fontFor(PackUiTone.BODY);
        PackUtilChatField af = addFields.get(key);
        boolean editable  = isEditableStringList(key);
        int editIdx       = editable ? stringListEditIndex.getOrDefault(key, -1) : -1;

        if (editIdx >= lst.size()) { editIdx = -1; stringListEditIndex.put(key, -1); }
        String filter     = (!editable && af != null) ? af.getText().toLowerCase() : "";
        String emptyHint  = getStringListEmptyHint(key);

        List<Integer> filtered = new ArrayList<>();
        for (int i = 0; i < lst.size(); i++) {
            if (filter.isEmpty() || lst.get(i).toLowerCase().contains(filter)) filtered.add(i);
        }

        String sectionLabel = filter.isEmpty()
                ? field.label() + " (" + lst.size() + ")"
                : field.label() + " (" + filtered.size() + "/" + lst.size() + ")";
        PackUiText.draw(ctx, textRenderer, sectionLabel, font,
                PackUtilColors.textSecondary(), x, y + 2, false);

        boolean hasCapture = field.captureMode() != CaptureMode.NONE;
        boolean hasSlotField = editable && usesMinecraftTextRendering(key);
        int btnH      = 14;
        int addBtnW   = 34;
        int slotW     = hasSlotField ? 44 : 0;
        int capBtnW   = hasCapture ? CAPTURE_BTN_W : 0;
        int capBtnX   = x + w - capBtnW;
        int addBtnX   = (hasCapture ? capBtnX : x + w) - 3 - addBtnW;
        int slotX     = hasSlotField ? addBtnX - 2 - slotW : 0;
        int addY      = y + 13;

        PackUtilChatField slotField = hasSlotField ? textFields.get(key + "_slot") : null;
        if (hasSlotField && slotField == null) {
            slotField = makeField(slotW);
            slotField.setNumericOnly(true);
            slotField.setPlaceholder(Component.literal("Slot#"));
            textFields.put(key + "_slot", slotField);
        }
        if (af != null) {
            af.setX(x); af.setY(addY + 1); af.setWidth((hasSlotField ? slotX : addBtnX) - x - 2);
            af.render(ctx, mx, my, delta);
        }
        if (hasSlotField && slotField != null) {
            slotField.setX(slotX); slotField.setY(addY + 1); slotField.setWidth(slotW);
            slotField.render(ctx, mx, my, delta);
        }

        if (editable && editIdx >= 0 && editIdx < lst.size() && af != null) {
            String nameText = af.getText().strip();
            String slotText = hasSlotField && slotField != null ? slotField.getText().strip() : "";
            String entry = buildEntryFromNameAndSlot(nameText, slotText);
            if (!entry.isEmpty()) {
                String normalized = isXCarryListKey(key) ? normalizeXCarryEntry(entry)
                        : usesStoreTargetFormatting(key)
                            ? autismclient.util.macro.StoreItemAction.normalizeTargetEntry(entry) : entry;
                if (normalized != null && !normalized.isBlank() && !normalized.equals(lst.get(editIdx))) {
                    lst.set(editIdx, normalized);
                    if (usesMinecraftTextRendering(key)) {
                        editorItemLists.put(key, buildStructuredListTargets(key));
                    }
                }
            }
        }

        final PackUtilChatField fSlotField = slotField;
        if (af != null) {
            renderOverlayButton(ctx, addBtnX, addY, addBtnW, btnH, "+Add", PackUiOverlayButton.Variant.SUCCESS, true, mx, my, () -> {
                String nameText = af.getText().strip();
                String slotText = hasSlotField && fSlotField != null ? fSlotField.getText().strip() : "";
                String entry = buildEntryFromNameAndSlot(nameText, slotText);
                if (!entry.isEmpty() && addStringListEntry(field, lst, entry)) {
                    af.setText("");
                    if (fSlotField != null) fSlotField.setText("");
                    stringListEditIndex.put(key, -1);
                }
            });
        }

        if (hasCapture) {
            boolean isItemSlot = field.captureMode() == CaptureMode.ITEM_SLOT;
            boolean capturing  = isItemSlot && key.equals(itemSlotCapturePendingKey);
            final String fKey = key;
            final FieldDef fField = field;
            final List<String> fLst = lst;
            renderOverlayButton(
                    ctx,
                    capBtnX,
                    addY,
                    capBtnW,
                    btnH,
                    isItemSlot ? (capturing ? "Done" : "Pick") : "Capture",
                    capturing ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.PRIMARY,
                    true,
                    mx,
                    my,
                    () -> {
                if (isItemSlot) {
                    if (fKey.equals(itemSlotCapturePendingKey)) {
                        itemSlotCapturePendingKey = null;
                        exitCaptureMode(false, false);
                    } else {
                        itemSlotCapturePendingKey = fKey;
                        enterCaptureMode();
                    }
                } else {
                    startCapture(fField, fLst);
                }
            });
        }

        int itemsY       = addY + btnH + 2;
        int selAreaH     = SEL_LIST_MAX_VIS * SEL_ITEM_H;
        int delW         = 13;

        PackUiScrollViewport strViewport = getOrCreateViewport(selectedScrollViewports, key,
            x, itemsY, w, selAreaH, SEL_ITEM_H, SCROLLBAR_W);
        strViewport.setContentHeight(filtered.size() * SEL_ITEM_H);
        selectedListBounds.put(key, new int[]{itemsY, selAreaH});

        strViewport.renderScrollbar(ctx, mx, my);

        if (!filtered.isEmpty()) {
            int rowAreaW = w - SCROLLBAR_W - 1 - delW - 2;
            strViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
            int firstVi = strViewport.getFirstVisibleRow();
            for (int vi = firstVi; vi < filtered.size() && vi <= strViewport.getLastVisibleRow(); vi++) {
                int ri   = filtered.get(vi);
                int iy   = strViewport.getRowScreenY(vi);
                if (iy == Integer.MIN_VALUE) continue;
                Component displayValue = formatStringListEntryText(key, lst.get(ri), ri);
                boolean selected = editable && ri == editIdx;
                boolean hovered = mx >= x && mx < x + rowAreaW && my >= iy && my < iy + 13;
                PackUiListRenderer.drawRow(
                        ctx,
                        textRenderer,
                        displayValue,
                        x,
                        iy,
                        rowAreaW,
                        13,
                        hovered,
                        selected,
                        PackUiListRenderer.RowTone.NORMAL,
                        usesMinecraftTextRendering(key)
                );

                if (editable) {
                    final int fri = ri;
                    final PackUtilChatField fSlotF2 = hasSlotField ? textFields.get(key + "_slot") : null;
                    hitRegions.add(new HitRegion(x, iy, rowAreaW, 13, () -> {
                        int curIdx = stringListEditIndex.getOrDefault(key, -1);
                        if (curIdx == fri) {
                            stringListEditIndex.put(key, -1);
                            if (af != null) af.setText("");
                            if (fSlotF2 != null) fSlotF2.setText("");
                        } else {
                            stringListEditIndex.put(key, fri);
                            String raw = lst.get(fri);
                            if (af != null) af.setText(parseHandlerEntryName(raw));
                            if (fSlotF2 != null) {
                                ItemTarget parsed = ItemTarget.fromLegacyEntry(raw);
                                fSlotF2.setText(parsed.hasSlot() ? String.valueOf(parsed.slot) : "");
                            }
                        }
                    }));
                }

                int delX = x + rowAreaW + 2;
                final int fRi = ri;
                renderIconDeleteButton(ctx, delX, iy, delW, mx, my, () -> {
                    lst.remove(fRi);
                    if (editable && stringListEditIndex.getOrDefault(key, -1) == fRi) {
                        stringListEditIndex.put(key, -1);
                        if (af != null) af.setText("");
                    } else if (editable && stringListEditIndex.getOrDefault(key, -1) > fRi) {
                        stringListEditIndex.put(key, stringListEditIndex.get(key) - 1);
                    }
                    PackUiScrollViewport vp = selectedScrollViewports.get(key);
                    if (vp != null) vp.scrollBy(-1);
                    if (usesMinecraftTextRendering(key)) {
                        editorItemLists.put(key, buildStructuredListTargets(key));
                    }
                });
            }
            strViewport.endRender(ctx);
        } else if (emptyHint != null && !emptyHint.isBlank()) {
            int itemW = w - SCROLLBAR_W - 1;
            ctx.fill(x, itemsY, x + itemW, itemsY + 12, PackUtilColors.rowNormal());
            PackUiListRenderer.drawEmptyState(ctx, textRenderer, emptyHint, x, itemsY, itemW);
        }
    }

    private String getStringListEmptyHint(String key) {
        if (isInventoryAuditTargetListKey(key)) {
            List<String> selected = stringLists.getOrDefault(key, Collections.emptyList());
            if (selected.isEmpty()) {
                return "Add at least one target before running this audit.";
            }
        }
        return null;
    }

    private void renderStringListCatalog(GuiGraphicsExtractor ctx, FieldDef field, int x, int y, int w,
                                         int mx, int my, float delta) {
        String key        = field.key();
        List<String> sel  = stringLists.getOrDefault(key, Collections.emptyList());
        Identifier font   = theme.fontFor(PackUiTone.BODY);
        PackUtilChatField af = addFields.get(key);
        String filter     = (af != null) ? af.getText().toLowerCase() : "";

        int cy = y;

        PackUiText.draw(ctx, textRenderer, field.label() + " (" + sel.size() + ")", font,
                PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 13;

        int delW        = 13;
        int selAreaH    = SEL_LIST_MAX_VIS * SEL_ITEM_H;

        PackUiScrollViewport catSelViewport = getOrCreateViewport(selectedScrollViewports, key,
            x, cy, w, selAreaH, SEL_ITEM_H, SCROLLBAR_W);
        catSelViewport.setContentHeight(sel.size() * SEL_ITEM_H);
        selectedListBounds.put(key, new int[]{cy, selAreaH});

        catSelViewport.renderScrollbar(ctx, mx, my);

        if (!sel.isEmpty()) {
            int delX = x + w - SCROLLBAR_W - 2 - delW;
            catSelViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
            int firstSel = catSelViewport.getFirstVisibleRow();
            for (int i = firstSel; i < sel.size() && i <= catSelViewport.getLastVisibleRow(); i++) {
                int siy = catSelViewport.getRowScreenY(i);
                if (siy == Integer.MIN_VALUE) continue;
                String display = PackUtilRegistryLabels.block(sel.get(i));
                PackUiListRenderer.drawRow(
                        ctx,
                        textRenderer,
                        display,
                        x,
                        siy,
                        delX - x - 1,
                        13,
                        mx >= x && mx < delX - 1 && my >= siy && my < siy + 13,
                        false,
                        PackUiListRenderer.RowTone.NORMAL
                );
                final int fi = i;
                renderIconDeleteButton(ctx, delX, siy, delW, mx, my, () -> {
                    sel.remove(fi);
                    PackUiScrollViewport vp = selectedScrollViewports.get(key);
                    if (vp != null) vp.scrollBy(-1);
                });
            }
            catSelViewport.endRender(ctx);
        } else {
            PackUiListRenderer.drawEmptyState(ctx, textRenderer, "(none selected)", x, cy, w - SCROLLBAR_W - 1);
        }
        cy += selAreaH;

        ctx.fill(x, cy + 1, x + w, cy + 2, PackUtilColors.subPanelBorder());
        cy += 5;

        if (af != null) {
            af.setX(x); af.setY(cy); af.setWidth(w);
            af.render(ctx, mx, my, delta);
        }
        cy += 14;

        int headerH = 15;
        int capBtnH2 = 14;
        int cbX = x + w - CAPTURE_BTN_W;
        int headerTextW = Math.max(24, cbX - x - 4);
        String headerLabel = PackUiText.trimToWidth(
            textRenderer,
            "Available Blocks",
            headerTextW,
            font,
            PackUtilColors.textSecondary()
        );
        int headerTextY = PackUiSizing.alignTextY(cy, headerH, theme.fontHeight(PackUiTone.BODY), theme.bodyTextNudge());
        renderOverlayButton(ctx, cbX, cy, CAPTURE_BTN_W, capBtnH2, "Capture", PackUiOverlayButton.Variant.PRIMARY, true, mx, my, () -> startCapture(field, sel));
        PackUiText.draw(ctx, textRenderer, headerLabel, font, PackUtilColors.textSecondary(), x, headerTextY, false);
        cy += headerH + 2;

        List<String> all    = getAllBlockIds();
        List<String> filtered = new ArrayList<>();
        for (String id : all) {
            if (matchesListFilter(filter, id, trimMinecraftPrefix(id), PackUtilRegistryLabels.block(id))) {
                filtered.add(id);
            }
        }

        int catItemW  = w - SCROLLBAR_W - 1;

        PackUiScrollViewport catViewport = getOrCreateViewport(catalogScrollViewports, key,
            x, cy, w, CATALOG_LIST_H, CATALOG_ITEM_H, SCROLLBAR_W);
        catViewport.setContentHeight(filtered.size() * CATALOG_ITEM_H);
        catalogListBounds.put(key, new int[]{cy, CATALOG_LIST_H});

        catViewport.renderScrollbar(ctx, mx, my);

        catViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
        int firstItem = catViewport.getFirstVisibleRow();
        for (int i = firstItem; i < filtered.size() && i <= catViewport.getLastVisibleRow(); i++) {
            int iy = catViewport.getRowScreenY(i);
            if (iy == Integer.MIN_VALUE) continue;
            String id      = filtered.get(i);
            String display = PackUtilRegistryLabels.block(id);
            boolean already = sel.contains(id);
            boolean hov = mx >= x && mx < x + catItemW && my >= iy && my < iy + CATALOG_ITEM_H;
            PackUiListRenderer.drawRow(
                ctx,
                textRenderer,
                display,
                x,
                iy,
                catItemW,
                CATALOG_ITEM_H,
                hov,
                already,
                already ? PackUiListRenderer.RowTone.READY : PackUiListRenderer.RowTone.NORMAL
            );
            if (!already) {
                hitRegions.add(new HitRegion(x, iy, catItemW, CATALOG_ITEM_H, () -> {
                    if (!sel.contains(id)) sel.add(id);
                }));
            } else {

                hitRegions.add(new HitRegion(x, iy, catItemW, CATALOG_ITEM_H, () -> sel.remove(id)));
            }
        }
        catViewport.endRender(ctx);
    }

    private void renderFooter(GuiGraphicsExtractor ctx, int footerY, int mx, int my) {
        int btnH  = 16;
        int btnY  = footerY + (FOOTER_H - btnH) / 2;
        if (standalonePayloadEditor) {
            int closeW = 72;
            int closeX = panelX + (panelW - closeW) / 2;
            ctx.fill(panelX + PAD, footerY, panelX + panelW - PAD, footerY + 1,
                    PackUtilColors.subPanelBorder());
            renderOverlayButton(ctx, closeX, btnY, closeW, btnH, "Close", PackUiOverlayButton.Variant.SECONDARY, true, mx, my, () -> closeEditor(false));
            return;
        }
        int btnW  = payloadAction != null ? 72 : 58;
        int gap   = 6;
        int total = btnW * 2 + gap;
        int sx    = panelX + (panelW - total) / 2;

        ctx.fill(panelX + PAD, footerY, panelX + panelW - PAD, footerY + 1,
                PackUtilColors.subPanelBorder());

        renderOverlayButton(ctx, sx, btnY, btnW, btnH, "Cancel", PackUiOverlayButton.Variant.SECONDARY, true, mx, my, () -> closeEditor(false));

        int saveX = sx + btnW + gap;
        renderOverlayButton(ctx, saveX, btnY, btnW, btnH, payloadAction != null ? "Save Macro" : "Save", PackUiOverlayButton.Variant.PRIMARY, true, mx, my, () -> closeEditor(true));
    }

    private void enterCaptureMode() {
        clearCaptureToasts();
        captureHiddenOverlays = new ArrayList<>();
        for (IPackUtilOverlay o : PackUtilOverlayManager.get().getOverlays()) {
            if (o != this && o.isVisible()) {
                o.saveLayout();
                captureHiddenOverlays.add(o);
                o.setVisible(false);
            }
        }
        saveLayout();
        clearTextFieldFocus();
        restoreVisibleAfterCapture = visible;
        visible = false;
    }

    private void enterEditorOnlyCaptureMode() {
        clearCaptureToasts();
        captureHiddenOverlays = null;
        saveLayout();
        clearTextFieldFocus();
        restoreVisibleAfterCapture = visible;
        visible = false;
    }

    private void restoreCaptureHiddenOverlays() {
        if (captureHiddenOverlays == null) return;
        PackUtilOverlayManager manager = PackUtilOverlayManager.get();
        for (IPackUtilOverlay overlay : captureHiddenOverlays) {
            manager.register(overlay);
            overlay.setVisible(true);
            overlay.saveLayout();
        }
        captureHiddenOverlays = null;
    }

    private void clearCaptureCallbacks(boolean cancelGBreak) {
        PackUtilSharedState state = PackUtilSharedState.get();
        state.setCaptureCancelCallback(null);
        state.setBlockCaptureCallback(null);
        state.setEntityCaptureCallback(null);
        state.setAttackCaptureCallback(null);
        state.setEntityCaptureSpecific(false);
        if (cancelGBreak && state.isGBreakCapturing()) {
            state.cancelGBreakCapture();
        }
    }

    private void exitCaptureMode(boolean reopenInventory, boolean closeCurrentScreen) {
        restoreCaptureHiddenOverlays();
        clearCaptureCallbacks(false);
        visible = restoreVisibleAfterCapture;
        restoreVisibleAfterCapture = false;

        hitRegions.clear();
        scrollDragRegions.clear();
        PackUtilOverlayManager.get().bringToFront(this);
        if (reopenInventory && screenBeforeCapture != null) {
            MC.execute(() -> { if (MC.screen == null) MC.setScreen(screenBeforeCapture); });
        }
        if (closeCurrentScreen) {
            MC.execute(() -> { if (MC.screen != null) MC.setScreen(null); });
        }
        screenBeforeCapture = null;
        refreshInteractiveLayout();
    }

    private void closeEditor(boolean save) {
        restoreCaptureHiddenOverlays();
        restoreVisibleAfterCapture = false;
        clearCaptureCallbacks(true);
        clearCaptureToasts();

        if (save && targetAction != null) {
            if (onPreSave != null) onPreSave.run();
            flushToWorkingTag();
            targetAction.fromTag(workingTag);
            if (onSaveCallback != null) onSaveCallback.run();
        }
        packetSelectorOverlay.close();
        visible                   = false;
        targetAction              = null;
        standalonePayloadEditor   = false;
        payloadAction             = null;
        workingTag                = null;
        schema                    = null;
        itemAction                = null;
        itemSlotCapturePendingKey = null;
        craftEntries              = null;
        craftAllRecipes         = null;
        craftFilteredRecipes    = null;
        craftSelectedRecipe     = null;
        craftRecipeListBounds   = null;
        dropAction              = null;
        editorItemFields.clear();
        editorItemLists.clear();
        lanStepEntries          = null;
        toggleModuleEntries     = null;
        onPreSave               = null;
        onSaveCallback          = null;
        screenBeforeGBreak      = null;
        screenBeforeCapture     = null;
    }

    private void clearCaptureToasts() {
        captureToasts.clear();
    }

    private void pruneCaptureToasts(long nowNanos) {
        captureToasts.removeIf(toast -> (nowNanos - toast.shownAtNanos()) / 1_000_000L >= CAPTURE_TOAST_LIFETIME_MS);
    }

    private void showCaptureToast(String message, int accentColor) {
        if (message == null || message.isBlank()) return;
        long nowNanos = System.nanoTime();
        pruneCaptureToasts(nowNanos);
        if (captureToasts.size() >= CAPTURE_TOAST_MAX_VISIBLE) {
            captureToasts.remove(0);
        }
        captureToasts.add(new CaptureToast(message, nowNanos, accentColor));
    }

    public boolean hasAbstractContainerScreenCaptureToasts() {
        pruneCaptureToasts(System.nanoTime());
        return !captureToasts.isEmpty();
    }

    public void renderAbstractContainerScreenCaptureToasts(GuiGraphicsExtractor context, int anchorX, int anchorY, int anchorWidth) {
        if (context == null || textRenderer == null || anchorWidth <= 0) return;

        long nowNanos = System.nanoTime();
        pruneCaptureToasts(nowNanos);
        if (captureToasts.isEmpty()) return;

        int y = anchorY;
        for (int i = captureToasts.size() - 1; i >= 0; i--) {
            CaptureToast toast = captureToasts.get(i);
            float ageMs = Math.max(0.0f, (nowNanos - toast.shownAtNanos()) / 1_000_000.0f);
            float enter = clamp01(ageMs / CAPTURE_TOAST_ENTER_MS);
            float exit = clamp01((CAPTURE_TOAST_LIFETIME_MS - ageMs) / CAPTURE_TOAST_EXIT_MS);
            float alpha = Math.min(easeOutCubic(enter), easeOutCubic(exit));
            if (alpha <= 0.001f) continue;

            float scale = 0.90f + (0.10f * easeOutBack(enter));
            float offsetY = ((1.0f - easeOutCubic(enter)) * -10.0f) + ((1.0f - exit) * 4.0f);
            int maxToastWidth = Math.min(anchorWidth, 260);
            String trimmed = PackUiText.trimToWidth(textRenderer, toast.message(), maxToastWidth - 14, theme.fontFor(PackUiTone.BODY), theme.color(PackUiTone.BODY));
            int textWidth = PackUiText.width(textRenderer, trimmed, theme.fontFor(PackUiTone.BODY), theme.color(PackUiTone.BODY));
            int toastWidth = Math.max(124, Math.min(maxToastWidth, textWidth + 18));
            int drawX = anchorX + Math.max(0, (anchorWidth - toastWidth) / 2);
            int drawY = Math.round(y + offsetY);
            int drawH = CAPTURE_TOAST_HEIGHT;

            float centerX = drawX + (toastWidth / 2.0f);
            float centerY = drawY + (drawH / 2.0f);
            context.pose().pushMatrix();
            context.pose().translate(centerX, centerY);
            context.pose().scale(scale, scale);
            context.pose().translate(-centerX, -centerY);

            int fill = PackUiRenderContext.applyAlpha(0xD6121014, alpha);
            int border = PackUiRenderContext.applyAlpha(toast.accentColor(), alpha);
            int highlight = PackUiRenderContext.applyAlpha(0x24FFFFFF, alpha);
            int textColor = PackUiRenderContext.applyAlpha(0xFFF4F4F4, alpha);

            context.fill(drawX, drawY, drawX + toastWidth, drawY + drawH, fill);
            context.fill(drawX, drawY, drawX + toastWidth, drawY + 1, border);
            context.fill(drawX, drawY + drawH - 1, drawX + toastWidth, drawY + drawH, border);
            context.fill(drawX, drawY, drawX + 1, drawY + drawH, border);
            context.fill(drawX + toastWidth - 1, drawY, drawX + toastWidth, drawY + drawH, border);
            context.fill(drawX + 1, drawY + 1, drawX + toastWidth - 1, drawY + 2, highlight);
            int toastTextY = PackUiSizing.alignTextY(drawY, drawH, theme.fontHeight(PackUiTone.BODY), theme.bodyTextNudge());
            PackUiText.draw(context, textRenderer, trimmed, theme.fontFor(PackUiTone.BODY), textColor, drawX + 8, toastTextY, false);

            context.pose().popMatrix();
            y += drawH + CAPTURE_TOAST_GAP;
        }
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private float easeOutCubic(float value) {
        float t = clamp01(value);
        float inv = 1.0f - t;
        return 1.0f - (inv * inv * inv);
    }

    private float easeOutBack(float value) {
        float t = clamp01(value);
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float x = t - 1.0f;
        return 1.0f + (c3 * x * x * x) + (c1 * x * x);
    }

    private void flushToWorkingTag() {
        if (workingTag == null) return;

        if (itemAction != null) {
            if (itemEditIndex >= 0 && itemEditIndex < itemAction.itemNames.size()) {
                applyItemEntryEditor();
            }
            for (int i = 0; i < itemAction.itemNames.size(); i++) {
                PackUtilChatField tf = textFields.get("item_times_" + i);
                if (tf != null) {
                    try {
                        while (itemAction.itemTimes.size() <= i) itemAction.itemTimes.add(1);
                        itemAction.itemTimes.set(i, Math.max(1, Integer.parseInt(tf.getText().strip())));
                    } catch (NumberFormatException ignored) {}
                }
            }
            itemAction.waitForGui  = toggleStates.getOrDefault("item_waitForGui",  false);
            itemAction.waitForItem = toggleStates.getOrDefault("item_waitForItem", false);
            PackUtilChatField guiF = textFields.get("item_guiName");
            if (guiF != null) itemAction.guiName = guiF.getText();
            if (!itemAction.itemNames.isEmpty()) {
                itemAction.useSlot = false;
                itemAction.targetSlot = -1;
                itemAction.actionIndex = 0;
                itemAction.button = 0;
                itemAction.times = 1;
            }
            workingTag = itemAction.toTag();
            return;
        }

        if (craftEntries != null) {
            for (int i = 0; i < craftEntries.size(); i++) {
                CraftAction.CraftEntry entry = craftEntries.get(i);
                PackUtilChatField af = textFields.get("craft_amount_" + i);
                if (af != null) {
                    try { entry.amount = Math.max(1, Integer.parseInt(af.getText().strip())); }
                    catch (NumberFormatException ignored) {}
                }
                entry.useMaxAmount = toggleStates.getOrDefault("craft_useMax_" + i, false);
            }
            ListTag entryTags = new ListTag();
            for (CraftAction.CraftEntry entry : craftEntries) {
                if (entry.hasRecipe()) entryTags.add(entry.toTag());
            }
            workingTag.put("entries", entryTags);
            return;
        }

        if (dropAction != null) {
            if (dropEditIndex >= 0 && dropEditIndex < dropAction.itemNames.size()) {
                applyDropEntryEditor();
            }
            for (int i = 0; i < dropAction.itemNames.size(); i++) {
                PackUtilChatField f = textFields.get("drop_count_" + i);
                if (f != null) {
                    try { dropAction.itemCounts.set(i, Math.max(0, Integer.parseInt(f.getText().strip()))); }
                    catch (NumberFormatException ignored) {}
                }
            }
            while (dropAction.itemCounts.size() < dropAction.itemNames.size()) dropAction.itemCounts.add(1);
            PackUtilChatField cntF = textFields.get("drop_globalCount");
            if (cntF != null) { try { dropAction.dropCount = Math.max(1, Integer.parseInt(cntF.getText().strip())); } catch (NumberFormatException ignored) {} }
            autismclient.util.macro.DropAction.DropMode[] modes = autismclient.util.macro.DropAction.DropMode.values();
            dropAction.mode = modes[Math.min(enumIndices.getOrDefault("drop_mode", 0), modes.length - 1)];
            dropAction.waitForGui      = toggleStates.getOrDefault("drop_waitForGui", false);
            dropAction.useHandlerSlots = true;
            PackUtilChatField guiF = textFields.get("drop_guiName");
            if (guiF != null) dropAction.guiName = guiF.getText();
            workingTag = dropAction.toTag();
            return;
        }

        if (payloadAction != null) {
            payloadAction = buildPayloadActionFromEditor();
            workingTag = payloadAction.toTag();
            return;
        }

        if (lanStepEntries != null) {
            for (int i = 0; i < lanStepEntries.size(); i++) {
                autismclient.util.macro.WaitForLanStepAction.LanStepEntry e = lanStepEntries.get(i);
                PackUtilChatField uf = textFields.get("lan_user_" + i);
                if (uf != null) e.username = uf.getText();
                PackUtilChatField sf = textFields.get("lan_step_" + i);
                if (sf != null) { try { e.step = Math.max(1, Integer.parseInt(sf.getText().strip())); } catch (NumberFormatException ignored) {} }
            }
            ListTag entryList = new ListTag();
            for (autismclient.util.macro.WaitForLanStepAction.LanStepEntry e : lanStepEntries)
                entryList.add(e.toTag());
            workingTag.put("entries", entryList);
            workingTag.putBoolean("filterByUser", toggleStates.getOrDefault("lan_filterByUser", false));
            PackUtilChatField dsF = textFields.get("lan_defaultStep");
            if (dsF != null) { try { workingTag.putInt("defaultStep", Math.max(1, Integer.parseInt(dsF.getText().strip()))); } catch (NumberFormatException ignored) {} }

        }

        if (toggleModuleEntries != null) {
            ListTag entriesTag = new ListTag();
            for (autismclient.util.macro.ToggleModuleAction.ModuleEntry entry : toggleModuleEntries) {
                if (entry != null && entry.moduleName != null && !entry.moduleName.isBlank()) {
                    entriesTag.add(entry.toTag());
                }
            }
            workingTag.put("entries", entriesTag);
            workingTag.putString("moduleName", "");
            workingTag.putString("toggleMode", autismclient.util.macro.ToggleModuleAction.ToggleMode.TOGGLE.name());
            return;
        }

        if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_PACKET) {
            List<String> targets = sanitizeWaitPacketTargets(getOrCreateWaitPacketTargets());
            ListTag packetList = new ListTag();
            for (String target : targets) packetList.add(StringTag.valueOf(target));
            workingTag.put("packetNames", packetList);
            workingTag.putString("packetName", targets.isEmpty() ? "" : targets.get(0));
            return;
        }

        if (schema == null) return;
        for (FieldDef field : schema.fields()) {
            String key = field.key();
            switch (field.type()) {

                case TOGGLE ->
                    workingTag.putBoolean(key, toggleStates.getOrDefault(key, false));

                case NUMBER, SLOT -> {
                    PackUtilChatField f = textFields.get(key);
                    if (f != null) {
                        try { workingTag.putInt(key, Integer.parseInt(f.getText().strip())); }
                        catch (NumberFormatException ignored) {}
                    }
                }

                case DECIMAL -> {
                    PackUtilChatField f = textFields.get(key);
                    if (f != null) {
                        try { workingTag.putDouble(key, Double.parseDouble(f.getText().strip())); }
                        catch (NumberFormatException ignored) {}
                    }
                }

                case TEXT, MACRO_SELECT -> {
                    PackUtilChatField f = textFields.get(key);
                    if (f != null) workingTag.putString(key, f.getText());
                }

                case ENUM -> {
                    List<String> opts = field.enumOptions();
                    if (!opts.isEmpty()) {
                        int idx = Math.min(enumIndices.getOrDefault(key, 0), opts.size() - 1);
                        workingTag.putString(key, opts.get(idx));
                    }
                }

                case BLOCK_POS -> {
                    String[] xyzKeys = field.xyzKeys();
                    boolean  dbl     = field.xyzDouble();
                    for (int i = 0; i < 3; i++) {
                        PackUtilChatField f = textFields.get(key + "_" + i);
                        if (f == null) continue;
                        String t = f.getText().strip();
                        if (dbl) {
                            try { workingTag.putDouble(xyzKeys[i], Double.parseDouble(t)); }
                            catch (NumberFormatException ignored) {}
                        } else {
                            try { workingTag.putInt(xyzKeys[i], Integer.parseInt(t)); }
                            catch (NumberFormatException ignored) {}
                        }
                    }
                }

                case STRING_LIST -> {
                    List<String> items = stringLists.getOrDefault(key, Collections.emptyList());
                    ListTag nbt = new ListTag();
                    for (String s : items) nbt.add(StringTag.valueOf(s));
                    workingTag.put(key, nbt);
                }
            }
        }

        rewriteStructuredEditorTargets();

        if (targetAction != null && targetAction.getType() == MacroActionType.INVENTORY) {
            String mode = currentEnumValue("mode");
            if ("CLOSE".equals(mode)) {
                workingTag.putBoolean("waitForGui", false);
            }
            workingTag.putString("guiName", "");
            workingTag.putBoolean("sendPacket", !toggleStates.getOrDefault("sendPacket", false));
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_SLOT_CHANGE) {

            net.minecraft.nbt.ListTag nbtEntries = new net.minecraft.nbt.ListTag();
            if (wscEntries != null) {
                for (WaitForSlotChangeAction.WaitEntry e : wscEntries) {
                    net.minecraft.nbt.CompoundTag ec = new net.minecraft.nbt.CompoundTag();
                    ItemTarget target = e.resolvedTarget();
                    if (target.hasSlot() || target.hasIdentity()) ec.put("target", target.toTag());
                    ec.putString("mode",   e.waitMode.name());
                    ec.putInt   ("count",  Math.max(1, e.targetCount));
                    nbtEntries.add(ec);
                }
            }
            workingTag.put("entries", nbtEntries);
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.USE_ITEM) {
            String mode = workingTag.getStringOr("useMode", "AUTOMATIC");
            if ("CUSTOM_HOLD".equals(mode)) {
                workingTag.putInt("useCount", 1);
                if (workingTag.getIntOr("holdTicks", 0) <= 0) workingTag.putInt("holdTicks", 20);
            } else if (workingTag.getIntOr("useCount", 0) <= 0) {
                workingTag.putInt("useCount", 1);
            }
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.SWAP_SLOTS) {
            boolean fromUseItemName = workingTag.getBooleanOr("fromUseItemName", false);
            boolean toUseItemName = workingTag.getBooleanOr("toUseItemName", false);
            if (fromUseItemName) workingTag.putInt("fromSlot", -1);
            else workingTag.putString("fromItemName", "");
            if (toUseItemName) workingTag.putInt("toSlot", -1);
            else workingTag.putString("toItemName", "");
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.GO_TO && !workingTag.getBooleanOr("waitForArrival", false)) {
            workingTag.putDouble("arrivalRadius", 2.0);
            workingTag.putInt("timeoutMs", 60000);
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.CLICK) {
            if (!workingTag.getBooleanOr("waitForGui", false)) {
                workingTag.putString("guiName", "");
            }
            if ("MIDDLE".equals(workingTag.getStringOr("clickType", "RIGHT"))) {
                workingTag.putString("clickType", "RIGHT");
            }
            if (workingTag.getIntOr("clickCount", 0) <= 0) workingTag.putInt("clickCount", 1);
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.DISCONNECT) {
            String mode = workingTag.getStringOr("mode", "DISCONNECT");
            if ("DISCONNECT".equals(mode)) {
                workingTag.putBoolean("useNextAction", false);
            } else if (workingTag.getIntOr("packetCount", 0) <= 0) {
                workingTag.putInt("packetCount", 200);
            }
            if (!"KICK_DUPE".equals(mode)) {
                workingTag.putBoolean("useNextAction", false);
            }
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.CLOSE_GUI) {
            workingTag.putBoolean("sendPacket", !toggleStates.getOrDefault("sendPacket", false));
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.SAVE_GUI) {
            workingTag.putBoolean("sendPacket", !toggleStates.getOrDefault("sendPacket", false));
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.STORE_ITEM) {
            workingTag.putBoolean("closeSendPkt", !toggleStates.getOrDefault("closeSendPkt", false));
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.OPEN_CONTAINER) {
            workingTag.putString("guiName", "");
            List<String> selectedTargets = stringLists.getOrDefault("entityTargets", Collections.emptyList());
            String entityTarget = selectedTargets.isEmpty() ? "" : selectedTargets.get(0);
            workingTag.putString("entityTarget", entityTarget);
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_CHAT) {
            workingTag.putInt("fuzzyPercent",
                    autismclient.util.macro.WaitForChatAction.clampFuzzyPercent(getWaitChatFuzzyPercent()));
            if (!workingTag.getBooleanOr("waitForGui", false)) {
                workingTag.putString("waitGuiName", "");
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) return false;
        if (packetSelectorOverlay.isVisible()) return packetSelectorOverlay.mouseClicked(mx, my, button);
        int imx = (int) mx, imy = (int) my;

        if (isOverCloseButton(mx, my, getBounds())) {
            closeEditor(false);
            return true;
        }
        if (isOverDragBar(mx, my)) {
            dragging   = true;
            dragOffX   = mx - panelX;
            dragOffY   = my - panelY;
            PackUtilOverlayManager.get().bringToFront(this);
            return true;
        }
        if (!isMouseOver(mx, my)) return false;

        PackUtilOverlayManager.get().bringToFront(this);

        MouseButtonEvent click = new MouseButtonEvent(imx, imy, new MouseButtonInfo(button, 0));
        PackUtilChatField focused = null;
        for (Map.Entry<String, PackUtilChatField> entry : textFields.entrySet()) {

            String baseKey = entry.getKey().replaceAll("_\\d+$", "");
            FieldDef fld = getField(baseKey);
            if (fld != null && !isFieldVisible(fld)) continue;
            if (entry.getValue().mouseClicked(click, false)) { focused = entry.getValue(); break; }
        }
        if (focused == null) {
            for (Map.Entry<String, PackUtilChatField> entry : addFields.entrySet()) {
                FieldDef fld = getField(entry.getKey());
                if (fld != null && !isFieldVisible(fld)) continue;
                if (entry.getValue().mouseClicked(click, false)) { focused = entry.getValue(); break; }
            }
        }
        if (focused != null) {
            PackUtilChatField ff = focused;
            textFields.values().forEach(f -> f.setFocused(f == ff));
            addFields.values().forEach(f  -> f.setFocused(f == ff));
            return true;
        }

        clearTextFieldFocus();

        if (button == 0 && isOverWaitChatFuzzySlider(imx, imy)) {
            waitChatFuzzySliderDragging = true;
            updateWaitChatFuzzyPercentFromMouse(imx);
            return true;
        }
        if (button == 0 && isOverRotateSmoothnessSlider(imx, imy)) {
            rotateSmoothnessSliderDragging = true;
            updateRotateSmoothnessFromMouse(imx);
            return true;
        }

        for (PackUiScrollViewport vp : selectedScrollViewports.values()) {
            if (vp.isScrollbarHovered(mx, my)) {
                vp.mouseClicked(mx, my, button);
                return true;
            }
        }
        for (PackUiScrollViewport vp : catalogScrollViewports.values()) {
            if (vp.isScrollbarHovered(mx, my)) {
                vp.mouseClicked(mx, my, button);
                return true;
            }
        }

        for (ScrollDragRegion r : scrollDragRegions) {
            if (r.contains(imx, imy)) {
                activeScrollDragHandler = r.handler();
                activeScrollDragHandler.accept(imy);
                return true;
            }
        }

        for (HitRegion r : hitRegions) {
            if (r.contains(imx, imy) && r.fire(mx, my, button)) {
                refreshInteractiveLayout();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (packetSelectorOverlay.isVisible() && packetSelectorOverlay.mouseReleased(mx, my, button)) return true;
        boolean consumed = false;

        for (PackUiScrollViewport vp : selectedScrollViewports.values()) {
            if (vp.isScrollbarDragging()) { vp.mouseReleased(); consumed = true; }
        }
        for (PackUiScrollViewport vp : catalogScrollViewports.values()) {
            if (vp.isScrollbarDragging()) { vp.mouseReleased(); consumed = true; }
        }

        if (activeScrollDragHandler != null) { activeScrollDragHandler = null; consumed = true; }
        if (waitChatFuzzySliderDragging) { waitChatFuzzySliderDragging = false; consumed = true; }
        if (rotateSmoothnessSliderDragging) { rotateSmoothnessSliderDragging = false; consumed = true; }
        if (dragging) { dragging = false; consumed = true; }
        return consumed;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (packetSelectorOverlay.isVisible() && packetSelectorOverlay.mouseDragged(mx, my, button, dx, dy)) return true;
        if (waitChatFuzzySliderDragging) {
            updateWaitChatFuzzyPercentFromMouse((int) mx);
            return true;
        }
        if (rotateSmoothnessSliderDragging) {
            updateRotateSmoothnessFromMouse((int) mx);
            return true;
        }
        if (dragging) {
            panelX = (int)(mx - dragOffX);
            panelY = (int)(my - dragOffY);
            PackUtilWindowLayout c = clampToScreen(this);
            panelX = c.x; panelY = c.y;
            return true;
        }

        for (PackUiScrollViewport vp : selectedScrollViewports.values()) {
            if (vp.isScrollbarDragging()) { vp.mouseDragged(mx, my); return true; }
        }
        for (PackUiScrollViewport vp : catalogScrollViewports.values()) {
            if (vp.isScrollbarDragging()) { vp.mouseDragged(mx, my); return true; }
        }

        if (activeScrollDragHandler != null) {
            activeScrollDragHandler.accept((int) my);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (packetSelectorOverlay.isVisible()) return packetSelectorOverlay.mouseScrolled(mx, my, amount);
        if (!visible || !isMouseOver(mx, my)) return false;

        for (PackUtilChatField field : textFields.values()) {
            if (field.mouseScrolled(mx, my, amount)) return true;
        }
        for (PackUtilChatField field : addFields.values()) {
            if (field.mouseScrolled(mx, my, amount)) return true;
        }

        for (Map.Entry<String, PackUiScrollViewport> entry : selectedScrollViewports.entrySet()) {
            PackUiScrollViewport vp = entry.getValue();
            if (vp.contains(mx, my)) {
                vp.mouseScrolled(mx, my, amount);
                return true;
            }
        }

        for (Map.Entry<String, PackUiScrollViewport> entry : catalogScrollViewports.entrySet()) {
            PackUiScrollViewport vp = entry.getValue();
            if (vp.contains(mx, my)) {
                vp.mouseScrolled(mx, my, amount);
                return true;
            }
        }

        if (craftRecipeListBounds != null
                && my >= craftRecipeListBounds[0]
                && my < craftRecipeListBounds[0] + craftRecipeListBounds[1]) {
            craftRecipeScrollOffset = Math.max(0, craftRecipeScrollOffset - (int)(amount * CATALOG_ITEM_H));
            return true;
        }

        scrollOffset = Math.max(0, scrollOffset - (int)(amount * 12));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        if (packetSelectorOverlay.isVisible()) return packetSelectorOverlay.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (cancelCaptureIfActive()) return true;
            if (hasTextFieldFocused()) { clearTextFieldFocus(); return true; }
            closeEditor(false);
            return true;
        }
        KeyEvent ki = new KeyEvent(keyCode, scanCode, modifiers);

        for (PackUtilChatField f : textFields.values()) {
            if (f.isFocused()) { f.keyPressed(ki); return true; }
        }
        for (PackUtilChatField f : addFields.values()) {
            if (f.isFocused()) { f.keyPressed(ki); return true; }
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!visible) return false;
        if (packetSelectorOverlay.isVisible()) return packetSelectorOverlay.charTyped(chr, modifiers);
        CharacterEvent ci = new CharacterEvent(chr);
        for (PackUtilChatField f : textFields.values()) {
            if (f.isFocused() && f.charTyped(ci)) return true;
        }
        for (PackUtilChatField f : addFields.values()) {
            if (f.isFocused() && f.charTyped(ci)) return true;
        }
        return false;
    }

    private boolean isFieldVisible(FieldDef field) {
        if (targetAction != null && targetAction.getType() == MacroActionType.INVENTORY) {
            String mode = currentEnumValue("mode");
            if ("waitForGui".equals(field.key()) || "guiName".equals(field.key())) {
                return "OPEN".equals(mode);
            }
            if ("sendPacket".equals(field.key())) {
                return "CLOSE".equals(mode);
            }
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_ENTITY) {
            String checkMode = currentEnumValue("checkMode");
            if ("centerOnPlayer".equals(field.key())
                    || "radius".equals(field.key())
                    || "mustBeLookingAt".equals(field.key())) {
                return "RADIUS".equals(checkMode);
            }
            if ("pos".equals(field.key())) {
                return "RADIUS".equals(checkMode) && !toggleStates.getOrDefault("centerOnPlayer", false);
            }
        }

        if (targetAction != null && targetAction.getType() == MacroActionType.INVENTORY_AUDIT) {
            String mode = currentEnumValue("mode");
            String openMode = currentEnumValue("openMode");
            boolean isDupeMode = "DUPE".equals(mode) || "DUPE_SPAM".equals(mode);
            boolean isDupeSpam = "DUPE_SPAM".equals(mode);

            if ("targetItems".equals(field.key())) {
                return isDupeMode;
            }

            if ("openCommand".equals(field.key())) {
                return isDupeMode && "COMMAND".equals(openMode);
            }

            if ("containerPos".equals(field.key())) {
                return isDupeMode && "CONTAINER".equals(openMode);
            }

            if ("spamCount".equals(field.key()) || "spamDelayMs".equals(field.key())) {
                return isDupeSpam;
            }

            if ("openMode".equals(field.key()) ||
                "dupeVector".equals(field.key()) ||
                "iterations".equals(field.key()) ||
                "maxTransferAttempts".equals(field.key()) ||
                "transferRetryDelayMs".equals(field.key())) {
                return isDupeMode;
            }
        }
        if (!field.hasShowWhen()) return true;
        boolean cond;
        if (field.showWhenValue() != null && !field.showWhenValue().isEmpty()) {
            String currentValue = currentEnumValue(field.showWhenKey());
            cond = false;
            for (String allowed : field.showWhenValue().split("\\|")) {
                if (allowed.equals(currentValue)) {
                    cond = true;
                    break;
                }
            }
        } else {
            cond = toggleStates.getOrDefault(field.showWhenKey(), false);
        }
        boolean visible = field.showWhenInverted() ? !cond : cond;

        if (visible && !field.showWhenInverted()) {
            FieldDef dep = getField(field.showWhenKey());
            if (dep != null && !isFieldVisible(dep)) visible = false;
        }
        return visible;
    }

    private String currentEnumValue(String key) {
        if (schema == null) return "";
        for (FieldDef field : schema.fields()) {
            if (!field.key().equals(key) || field.type() != FieldType.ENUM) continue;
            List<String> opts = field.enumOptions();
            if (opts.isEmpty()) return "";
            int idx = Math.min(enumIndices.getOrDefault(key, 0), opts.size() - 1);
            return opts.get(idx);
        }
        return "";
    }

    private void prepareWorkingTagForEditor(MacroAction action) {
        if (action == null || workingTag == null) return;

        if (action instanceof autismclient.util.macro.SelectSlotAction selectSlotAction) {
            primeStructuredField("itemName", selectSlotAction.itemTarget, selectSlotAction.itemName, null);
        }
        if (action instanceof autismclient.util.macro.UseItemAction useItemAction) {
            primeStructuredField("itemName", useItemAction.itemTarget, useItemAction.itemName, null);
        }
        if (action instanceof autismclient.util.macro.WaitForCooldownAction waitForCooldownAction) {
            primeStructuredField("itemName", waitForCooldownAction.itemTarget, waitForCooldownAction.itemName, null);
        }
        if (action instanceof autismclient.util.macro.CloseGuiAction closeGuiAction) {
            primeStructuredField("itemName", closeGuiAction.itemTarget, closeGuiAction.itemName, "targetSlot");
        }
        if (action instanceof autismclient.util.macro.SwapSlotsAction swapSlotsAction) {
            primeStructuredField("fromItemName", swapSlotsAction.fromItemTarget, swapSlotsAction.fromItemName, null);
            primeStructuredField("toItemName", swapSlotsAction.toItemTarget, swapSlotsAction.toItemName, null);
        }
        if (action instanceof autismclient.util.macro.StoreItemAction storeItemAction) {
            primeStructuredList("targetItems", storeItemAction.itemTargets, storeItemAction.targetItems);
        }
        if (action instanceof autismclient.util.macro.XCarryAction xCarryAction) {
            primeStructuredList("entries", xCarryAction.entryTargets, xCarryAction.entries);
        }
        if (action instanceof autismclient.util.macro.InventoryAuditAction inventoryAuditAction) {
            primeStructuredList("targetItems", inventoryAuditAction.itemTargets, inventoryAuditAction.targetItems);
        }
    }

    private void primeStructuredField(String key, ItemTarget source, String legacyValue, String slotKey) {
        ItemTarget resolved = resolveEditorTarget(source, legacyValue);
        if (!resolved.hasSlot() && !resolved.hasIdentity()) return;

        editorItemFields.put(key, resolved.copy());
        workingTag.putString(key, resolved.editorText());
        if (slotKey != null && resolved.hasSlot()) {
            workingTag.putInt(slotKey, resolved.slot);
        }
    }

    private void refreshItemTextDisplayProviders() {
        bindStructuredItemFieldDisplay("itemName");
        bindStructuredItemFieldDisplay("fromItemName");
        bindStructuredItemFieldDisplay("toItemName");
        bindTransientItemEditorDisplay("_item_add");
        bindTransientItemEditorDisplay("_drop_add");
        bindTransientItemEditorDisplay("_wsc_add");
        bindTransientItemEditorDisplay("targetItems");
    }

    private void bindStructuredItemFieldDisplay(String key) {
        PackUtilChatField field = textFields.get(key);
        if (field == null) return;
        field.setDisplayTextProvider(value -> resolveStructuredItemFieldDisplay(key, value));
    }

    private void bindTransientItemEditorDisplay(String key) {
        PackUtilChatField field = addFields.get(key);
        if (field == null) return;
        field.setDisplayTextProvider(value -> resolveTransientItemEditorDisplay(key, value));
    }

    private Component resolveStructuredItemFieldDisplay(String key, String value) {
        return richItemFieldDisplay(editorItemFields.get(key), value);
    }

    private Component resolveTransientItemEditorDisplay(String key, String value) {
        return switch (key) {
            case "_item_add" -> richItemFieldDisplay(
                    itemAction != null && itemEditIndex >= 0 && itemEditIndex < itemAction.itemNames.size()
                            ? targetAt(itemAction.itemTargets, itemAction.itemNames, itemEditIndex)
                            : null,
                    value
            );
            case "_drop_add" -> richItemFieldDisplay(
                    dropAction != null && dropEditIndex >= 0 && dropEditIndex < dropAction.itemNames.size()
                            ? targetAt(dropAction.itemTargets, dropAction.itemNames, dropEditIndex)
                            : null,
                    value
            );
            case "_wsc_add" -> richItemFieldDisplay(
                    wscEntries != null && wscEditIndex >= 0 && wscEditIndex < wscEntries.size()
                            ? wscEntries.get(wscEditIndex).resolvedTarget()
                            : null,
                    value
            );
            case "targetItems" -> richItemFieldDisplay(resolveSelectedTargetItemsEditorTarget(), value);
            default -> null;
        };
    }

    private ItemTarget resolveSelectedTargetItemsEditorTarget() {
        int storeIndex = stringListEditIndex.getOrDefault("store_items_selected", -1);
        int auditIndex = stringListEditIndex.getOrDefault("audit_items_selected", -1);
        int index = storeIndex >= 0 ? storeIndex : auditIndex;
        if (index < 0) return null;
        List<ItemTarget> targets = buildStructuredListTargets("targetItems");
        if (index >= 0 && index < targets.size()) {
            ItemTarget target = targets.get(index);
            if (target != null && (target.hasSlot() || target.hasIdentity())) return target;
        }
        return null;
    }

    private Component richItemFieldDisplay(ItemTarget target, String value) {
        if (target == null) return null;
        Component rich = target.editorComponent(value);
        if (rich == null) return null;
        String safeValue = value == null ? "" : value;
        return safeValue.equals(rich.getString()) ? rich.copy() : null;
    }

    private void primeStructuredList(String key, List<ItemTarget> source, List<String> legacyValues) {
        List<ItemTarget> resolved = copyEditorTargets(source, legacyValues);
        if (resolved.isEmpty()) return;

        editorItemLists.put(key, ItemTarget.copyList(resolved));
        ListTag listTag = new ListTag();
        for (ItemTarget target : resolved) {
            if (target == null) continue;
            String entry = target.toLegacyEntry();
            if (!entry.isBlank()) listTag.add(StringTag.valueOf(entry));
        }
        workingTag.put(key, listTag);
    }

    private ItemTarget resolveEditorTarget(ItemTarget source, String legacyValue) {
        if (source != null && (source.hasSlot() || source.hasIdentity())) return source.copy();
        return ItemTarget.fromLegacyEntry(legacyValue);
    }

    private List<ItemTarget> copyEditorTargets(List<ItemTarget> source, List<String> legacyValues) {
        List<ItemTarget> resolved = ItemTarget.copyList(source);
        if (!resolved.isEmpty()) return resolved;

        List<ItemTarget> parsed = new ArrayList<>();
        if (legacyValues == null) return parsed;
        for (String legacyValue : legacyValues) {
            ItemTarget target = ItemTarget.fromLegacyEntry(legacyValue);
            if (target.hasSlot() || target.hasIdentity()) parsed.add(target);
        }
        return parsed;
    }

    private ItemTarget buildStructuredFieldTarget(String key, String slotKey) {
        PackUtilChatField valueField = textFields.get(key);
        String text = valueField == null ? "" : valueField.getText().strip();
        ItemTarget previous = editorItemFields.get(key);
        int slot = -1;
        boolean slotDriven = false;
        if (slotKey != null) {
            PackUtilChatField slotField = textFields.get(slotKey);
            slot = slotField == null ? -1 : parseHandlerSlotField(slotField.getText(), -1);
            slotDriven = true;
        } else if (previous != null && previous.hasSlot()) {
            slot = previous.slot;
        }

        if (text.isEmpty() && (!slotDriven || slot < 0)) {
            return new ItemTarget();
        }

        if (previous != null && text.equals(previous.editorText())) {
            ItemTarget preserved = previous.copy();
            if (slotDriven) preserved.slot = slot;
            return preserved;
        }

        if (previous != null && previous.hasRichText() && !text.isEmpty()) {
            ItemTarget preserved = previous.withEditedDisplay(text);
            if (slot >= 0) preserved.slot = slot;
            else if (slotDriven) preserved.slot = -1;
            return preserved;
        }

        String raw;
        if (slot >= 0 && !text.isEmpty()) raw = "#" + slot + "|" + text;
        else if (slot >= 0) raw = "#" + slot;
        else raw = text;
        return ItemTarget.fromLegacyEntry(raw);
    }

    private List<ItemTarget> buildStructuredListTargets(String key) {
        List<String> values = stringLists.getOrDefault(key, Collections.emptyList());
        List<ItemTarget> previous = editorItemLists.getOrDefault(key, Collections.emptyList());
        boolean[] used = new boolean[previous.size()];
        List<ItemTarget> rebuilt = new ArrayList<>();
        for (String value : values) {
            String entry = value == null ? "" : value.strip();
            if (entry.isEmpty()) continue;

            ItemTarget preserved = null;
            for (int i = 0; i < previous.size(); i++) {
                ItemTarget candidate = previous.get(i);
                if (used[i] || candidate == null) continue;
                if (entry.equals(candidate.toLegacyEntry())) {
                    preserved = candidate.copy();
                    used[i] = true;
                    break;
                }
            }
            if (preserved == null) preserved = ItemTarget.fromLegacyEntry(entry);
            if (preserved.hasSlot() || preserved.hasIdentity()) rebuilt.add(preserved);
        }
        editorItemLists.put(key, ItemTarget.copyList(rebuilt));
        return rebuilt;
    }

    private void rewriteStructuredEditorTargets() {
        if (targetAction == null || workingTag == null) return;

        if (targetAction instanceof autismclient.util.macro.SelectSlotAction) {
            writeStructuredField("itemName", buildStructuredFieldTarget("itemName", null));
        }
        if (targetAction instanceof autismclient.util.macro.UseItemAction) {
            writeStructuredField("itemName", buildStructuredFieldTarget("itemName", null));
        }
        if (targetAction instanceof autismclient.util.macro.WaitForCooldownAction) {
            writeStructuredField("itemName", buildStructuredFieldTarget("itemName", null));
        }
        if (targetAction instanceof autismclient.util.macro.CloseGuiAction) {
            writeStructuredField("itemName", buildStructuredFieldTarget("itemName", "targetSlot"));
        }
        if (targetAction instanceof autismclient.util.macro.SwapSlotsAction) {
            writeStructuredField("fromItemName", buildStructuredFieldTarget("fromItemName", null));
            writeStructuredField("toItemName", buildStructuredFieldTarget("toItemName", null));
        }
        if (targetAction instanceof autismclient.util.macro.StoreItemAction) {
            writeStructuredList("targetItems", buildStructuredListTargets("targetItems"));
        }
        if (targetAction instanceof autismclient.util.macro.XCarryAction) {
            writeStructuredList("entries", buildStructuredListTargets("entries"));
        }
        if (targetAction instanceof autismclient.util.macro.InventoryAuditAction) {
            writeStructuredList("targetItems", buildStructuredListTargets("targetItems"));
        }
    }

    private void writeStructuredField(String key, ItemTarget target) {
        if (target == null || (!target.hasSlot() && !target.hasIdentity())) {
            workingTag.remove(key);
            editorItemFields.remove(key);
            return;
        }
        workingTag.put(key, target.toTag());
        editorItemFields.put(key, target.copy());
    }

    private void writeStructuredList(String key, List<ItemTarget> targets) {
        workingTag.put(key, ItemTarget.toTagList(targets));
    }

    private void refreshInteractiveLayout() {
        hitRegions.clear();
        scrollDragRegions.clear();
        catalogListBounds.clear();
        selectedListBounds.clear();
        int neededH = HEADER_HEIGHT + PAD + computeContentH() + FOOTER_H + PAD;
        int minH = currentMinPanelHeight();
        int maxH = Math.max(minH, PackUtilUiScale.getVirtualScreenHeight() * 4 / 5);
        panelH = Math.max(minH, Math.min(maxH, neededH));
        PackUtilWindowLayout clamped = clampToScreen(this, new PackUtilWindowLayout(panelX, panelY, panelW, panelH, visible, false));
        panelX = clamped.x;
        panelY = clamped.y;
        panelW = clamped.width;
        panelH = clamped.height;
    }

    private void toggleWscEditSelection(int index) {
        if (wscEditIndex == index) {
            wscEditIndex = -1;
            clearWscEditorFields();
        } else {
            wscEditIndex = index;
            if (wscEntries != null && index < wscEntries.size()) {
                syncWscEditorFromEntry(wscEntries.get(index));
            }
        }
    }

    private void syncWscEditorFromEntry(WaitForSlotChangeAction.WaitEntry e) {
        PackUtilChatField addF   = addFields.get("_wsc_add");
        PackUtilChatField slotF  = textFields.get("wsc_slot");
        PackUtilChatField countF = textFields.get("wsc_count");
        if (addF == null || slotF == null) return;
        suppressWscLiveUpdate = true;
        ItemTarget target = e.resolvedTarget();
        addF.setText(target.editorText());
        slotF.setText(target.hasSlot() ? String.valueOf(target.slot) : "");
        if (countF != null) countF.setText(String.valueOf(Math.max(1, e.targetCount)));
        suppressWscLiveUpdate = false;
    }

    private void clearWscEditorFields() {
        PackUtilChatField addF   = addFields.get("_wsc_add");
        PackUtilChatField slotF  = textFields.get("wsc_slot");
        PackUtilChatField countF = textFields.get("wsc_count");
        suppressWscLiveUpdate = true;
        if (addF   != null) addF.setText("");
        if (slotF  != null) slotF.setText("");
        if (countF != null) countF.setText("");
        suppressWscLiveUpdate = false;
    }

    private void handleWscEntryEditorChanged() {
        if (suppressWscLiveUpdate || wscEntries == null) return;
        if (wscEditIndex < 0 || wscEditIndex >= wscEntries.size()) return;
        PackUtilChatField addF  = addFields.get("_wsc_add");
        PackUtilChatField slotF = textFields.get("wsc_slot");
        if (addF == null || slotF == null) return;
        String nameText = addF.getText().strip();
        String slotText = slotF.getText().strip();
        WaitForSlotChangeAction.WaitEntry entry = wscEntries.get(wscEditIndex);
        ItemTarget target = buildEntryTargetFromEditor(nameText, slotText, entry.resolvedTarget());
        String rawTarget = target.toLegacyEntry();
        String norm = rawTarget.isEmpty() ? "" :
                autismclient.util.macro.StoreItemAction.normalizeTargetEntry(rawTarget);
        if (norm == null) norm = "";
        if (!wscTargetExistsOtherThan(norm, wscEditIndex)) {
            entry.target = norm;
            entry.itemTarget = target;
        }
    }

    private void handleWscCountChanged() {
        if (suppressWscLiveUpdate || wscEntries == null) return;
        PackUtilChatField countF = textFields.get("wsc_count");
        if (countF == null) return;
        int count;
        try { count = Integer.parseInt(countF.getText().strip()); }
        catch (NumberFormatException ignored) { return; }
        count = Math.max(1, count);
        if (wscEditIndex >= 0 && wscEditIndex < wscEntries.size()) {
            wscEntries.get(wscEditIndex).targetCount = count;
        } else {
            wscAddCount = count;
        }
    }

    private void applyWscAddEntry(PackUtilChatField addF, PackUtilChatField slotF) {
        if (wscEntries == null) return;
        String nameText = addF.getText().strip();
        String slotText = slotF.getText().strip();
        ItemTarget target = buildEntryTargetFromEditor(nameText, slotText, null);
        String rawTarget = target.toLegacyEntry();
        String norm = rawTarget.isEmpty() ? "" :
                autismclient.util.macro.StoreItemAction.normalizeTargetEntry(rawTarget);
        if (norm == null) norm = "";
        if (!wscTargetExists(norm)) {
            WaitForSlotChangeAction.WaitEntry entry = new WaitForSlotChangeAction.WaitEntry(norm, wscAddMode, wscAddCount);
            entry.itemTarget = target;
            entry.target = norm;
            wscEntries.add(entry);
            addF.setText("");
            slotF.setText("");
        }
    }

    private boolean wscTargetExists(String normTarget) {
        if (wscEntries == null) return false;
        for (WaitForSlotChangeAction.WaitEntry e : wscEntries) {
            if (e.target.equals(normTarget)) return true;
        }
        return false;
    }

    private boolean wscTargetExistsOtherThan(String normTarget, int ignoreIdx) {
        if (wscEntries == null) return false;
        for (int i = 0; i < wscEntries.size(); i++) {
            if (i != ignoreIdx && wscEntries.get(i).target.equals(normTarget)) return true;
        }
        return false;
    }

    private boolean isWaitSlotChangeEntryKey(String key) {
        return targetAction != null
                && targetAction.getType() == MacroActionType.WAIT_SLOT_CHANGE
                && "targetEntries".equals(key);
    }

    private boolean isStoreItemTargetListKey(String key) {
        return targetAction != null
                && targetAction.getType() == MacroActionType.STORE_ITEM
                && "targetItems".equals(key);
    }

    private boolean isInventoryAuditTargetListKey(String key) {
        return targetAction != null
                && targetAction.getType() == MacroActionType.INVENTORY_AUDIT
                && "targetItems".equals(key);
    }

    private boolean usesStoreTargetFormatting(String key) {
        return isStoreItemTargetListKey(key) || isInventoryAuditTargetListKey(key);
    }

    private String buildHandlerEntryFromEditor(String nameText, String slotText) {
        String trimmedName = nameText == null ? "" : nameText.trim();
        int slot = parseHandlerSlotField(slotText, -1);
        if (!trimmedName.isEmpty() && slot >= 0) return "#" + slot + "|" + trimmedName;
        if (!trimmedName.isEmpty()) return trimmedName;
        return slot >= 0 ? "#" + slot : null;
    }

    private ItemTarget captureItemTarget(net.minecraft.world.inventory.Slot slot, String itemName, String registryId, int visibleSlot) {
        if (slot != null && slot.getItem() != null && !slot.getItem().isEmpty()) {
            return ItemTarget.capture(slot.getItem(), visibleSlot);
        }
        if (visibleSlot >= 0) return ItemTarget.slotOnly(visibleSlot);
        String fallback = registryId != null && !registryId.isBlank()
                ? registryId.trim()
                : (itemName == null ? "" : itemName.trim());
        return ItemTarget.fromLegacyEntry(fallback);
    }

    private static ItemTarget stripSlotFromTarget(ItemTarget target) {
        if (target == null) return new ItemTarget();
        if (!target.hasSlot()) return target;
        ItemTarget copy = target.copy();
        copy.slot = -1;
        return copy;
    }

    private ItemTarget buildEntryTargetFromEditor(String nameText, String slotText, ItemTarget previous) {
        String trimmedName = nameText == null ? "" : nameText.strip();
        int slot = parseHandlerSlotField(slotText, -1);
        if (trimmedName.isEmpty() && slot < 0) return new ItemTarget();

        if (previous != null && trimmedName.equals(previous.editorText())) {
            int previousSlot = previous.hasSlot() ? previous.slot : -1;
            if (previousSlot == slot) return previous.copy();
        }

        if (previous != null && previous.hasRichText() && !trimmedName.isEmpty()) {
            ItemTarget preserved = previous.withEditedDisplay(trimmedName);
            preserved.slot = slot;
            return preserved;
        }

        String raw;
        if (slot >= 0 && !trimmedName.isEmpty()) raw = "#" + slot + "|" + trimmedName;
        else if (slot >= 0) raw = "#" + slot;
        else raw = trimmedName;
        return ItemTarget.fromLegacyEntry(raw);
    }

    private ItemTarget targetAt(List<ItemTarget> targets, List<String> legacyEntries, int index) {
        if (targets != null && index >= 0 && index < targets.size()) {
            ItemTarget target = targets.get(index);
            if (target != null && (target.hasSlot() || target.hasIdentity())) return target;
        }
        if (legacyEntries != null && index >= 0 && index < legacyEntries.size()) {
            return ItemTarget.fromLegacyEntry(legacyEntries.get(index));
        }
        return new ItemTarget();
    }

    private void preserveCapturedListTarget(String key, List<String> entries, ItemTarget capturedTarget) {
        if (key == null || entries == null || capturedTarget == null) return;
        String normalized = capturedTarget.toLegacyEntry();
        if (usesStoreTargetFormatting(key) || isWaitSlotChangeEntryKey(key)) {
            normalized = autismclient.util.macro.StoreItemAction.normalizeTargetEntry(normalized);
        } else if (isXCarryListKey(key)) {
            normalized = normalizeXCarryEntry(normalized);
        }
        if (normalized == null || normalized.isBlank()) return;

        int index = entries.indexOf(normalized);
        if (index < 0) return;

        List<ItemTarget> targets = buildStructuredListTargets(key);
        while (targets.size() < entries.size()) {
            targets.add(new ItemTarget());
        }
        ItemTarget stored = capturedTarget.copy();
        if (!normalized.equals(stored.toLegacyEntry())) {
            ItemTarget normalizedTarget = ItemTarget.fromLegacyEntry(normalized);
            stored.slot = normalizedTarget.slot;
            if (!normalizedTarget.hasSlot() && !stored.hasIdentity()) {
                stored = normalizedTarget;
            }
        }
        targets.set(index, stored);
        editorItemLists.put(key, ItemTarget.copyList(targets));
    }

    private void setTargetAt(List<ItemTarget> targets, List<String> legacyEntries, int index, ItemTarget target) {
        if (targets == null || legacyEntries == null || index < 0) return;
        while (targets.size() <= index) targets.add(new ItemTarget());
        while (legacyEntries.size() <= index) legacyEntries.add("");
        ItemTarget stored = target == null ? new ItemTarget() : target;
        targets.set(index, stored);
        legacyEntries.set(index, stored.toLegacyEntry());
    }

    private void addTargetEntry(List<ItemTarget> targets, List<String> legacyEntries, ItemTarget target) {
        if (targets == null || legacyEntries == null || target == null) return;
        targets.add(target);
        legacyEntries.add(target.toLegacyEntry());
    }

    private void trimTargetEntries(List<ItemTarget> targets, int size) {
        if (targets == null) return;
        while (targets.size() > size) targets.remove(targets.size() - 1);
        while (targets.size() < size) targets.add(new ItemTarget());
    }

    private boolean isEditableStringList(String key) {
        return isXCarryListKey(key) || isStoreItemTargetListKey(key)
                || isInventoryAuditTargetListKey(key) || isWaitSlotChangeEntryKey(key);
    }

    private boolean isXCarryListKey(String key) {
        return targetAction != null
                && targetAction.getType() == MacroActionType.XCARRY
                && "entries".equals(key);
    }

    private boolean isOpenContainerEntityListKey(String key) {
        return targetAction != null
                && targetAction.getType() == MacroActionType.OPEN_CONTAINER
                && "entityTargets".equals(key);
    }

    private CaptureListAddResult tryAddCapturedStoreItemEntry(net.minecraft.world.inventory.Slot slot,
                                                              String itemName,
                                                              String registryId,
                                                              int visibleSlot,
                                                              List<String> entries) {
        if (slot == null || visibleSlot < 0) {
            return new CaptureListAddResult(false, "Could not read that slot", CAPTURE_TOAST_ERROR);
        }

        String mode = currentEnumValue("mode");
        boolean playerInventorySlot = autismclient.util.PackUtilInventoryHelper.isInventorySlot(MC, slot);
        if ("LOOT".equals(mode) && playerInventorySlot) {
            return new CaptureListAddResult(false, "Steal only accepts chest/custom GUI slots", CAPTURE_TOAST_ERROR);
        }
        if ("STORE".equals(mode) && !playerInventorySlot) {
            return new CaptureListAddResult(false, "Store only accepts player inventory slots", CAPTURE_TOAST_ERROR);
        }

        String rawEntry = stripSlotFromTarget(captureItemTarget(slot, itemName, registryId, visibleSlot)).toLegacyEntry();
        return tryAddCapturedStringListEntry(findField("targetItems"), "targetItems", entries, rawEntry);
    }

    private String normalizeXCarryEntry(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (!trimmed.startsWith("#") && trimmed.matches("\\d+")) {
            trimmed = "#" + trimmed;
        }
        return autismclient.util.macro.XCarryAction.normalizeEntry(trimmed);
    }

    private boolean addStringListEntry(FieldDef field, List<String> entries, String rawEntry) {
        if (entries == null || rawEntry == null) return false;
        String entry = rawEntry.strip();
        if (entry.isEmpty()) return false;

        if (field != null && isXCarryListKey(field.key())) {
            entry = normalizeXCarryEntry(entry);
            if (entry == null || entries.contains(entry)) return false;
            if (entries.size() >= autismclient.util.macro.XCarryAction.MAX_ENTRIES) {
                PackUtilClientMessaging.sendPrefixed("XCarry can only store 4 crafting-grid entries.");
                return false;
            }
            entries.add(entry);
            return true;
        }

        if (field != null && isOpenContainerEntityListKey(field.key())) {
            if (entry.isBlank()) return false;
            entries.clear();
            entries.add(entry);
            return true;
        }

        if (field != null && (usesStoreTargetFormatting(field.key()) || isWaitSlotChangeEntryKey(field.key()))) {
            entry = autismclient.util.macro.StoreItemAction.normalizeTargetEntry(entry);
            if (entry == null || entry.isBlank() || entries.contains(entry)) return false;
            entries.add(entry);
            return true;
        }

        if (field != null && "players".equals(field.key())) {
            if (containsIgnoreCase(entries, entry)) return false;
            entries.add(entry);
            return true;
        }

        entries.add(entry);
        return true;
    }

    private CaptureListAddResult tryAddCapturedStringListEntry(FieldDef field, String key, List<String> entries, String rawEntry) {
        if (entries == null) return null;
        String entry = rawEntry == null ? "" : rawEntry.strip();
        if (entry.isEmpty()) {
            return new CaptureListAddResult(false, "Nothing to add from that slot", CAPTURE_TOAST_ERROR);
        }

        if (field != null && isXCarryListKey(field.key())) {
            entry = normalizeXCarryEntry(entry);
            if (entry == null || entry.isBlank()) {
                return new CaptureListAddResult(false, "Could not read that XCarry entry", CAPTURE_TOAST_ERROR);
            }
            String formatted = formatStringListEntry(key, entry);
            if (entries.contains(entry)) {
                return new CaptureListAddResult(false, "Already added " + formatted, CAPTURE_TOAST_ERROR);
            }
            if (entries.size() >= autismclient.util.macro.XCarryAction.MAX_ENTRIES) {
                return new CaptureListAddResult(false, "XCarry limit reached: 4 entries max", CAPTURE_TOAST_ERROR);
            }
            entries.add(entry);
            return new CaptureListAddResult(true, "Added " + formatted, CAPTURE_TOAST_SUCCESS);
        }

        if (field != null && (usesStoreTargetFormatting(field.key()) || isWaitSlotChangeEntryKey(field.key()))) {
            entry = autismclient.util.macro.StoreItemAction.normalizeTargetEntry(entry);
            if (entry == null || entry.isBlank()) {
                return new CaptureListAddResult(false, "Could not read that slot target", CAPTURE_TOAST_ERROR);
            }
        }

        if (field != null && isOpenContainerEntityListKey(field.key())) {
            entries.clear();
        }

        String formatted = formatStringListEntry(key, entry);
        if (entries.contains(entry)) {
            return new CaptureListAddResult(false, "Already added " + formatted, CAPTURE_TOAST_ERROR);
        }
        entries.add(entry);
        return new CaptureListAddResult(true, "Added " + formatted, CAPTURE_TOAST_SUCCESS);
    }

    private String formatStringListEntry(String key, String entry) {
        if (isXCarryListKey(key)) {
            String formatted = autismclient.util.macro.XCarryAction.formatEntry(entry);
            if (!formatted.isEmpty()) return formatted;
        }
        if (usesStoreTargetFormatting(key) || isWaitSlotChangeEntryKey(key)) {
            return autismclient.util.macro.StoreItemAction.formatTargetEntry(entry);
        }
        if (isOpenContainerEntityListKey(key)) {
            return formatEntityEntry(entry);
        }
        return entry == null ? "" : entry;
    }

    private Component formatStringListEntryText(String key, String entry, int index) {
        if (usesMinecraftTextRendering(key)) {
            ItemTarget target = resolveStructuredListTarget(key, index, entry);
            return formatItemTargetText(target, formatStringListEntry(key, entry));
        }
        return Component.literal(formatStringListEntry(key, entry));
    }

    private ItemTarget resolveStructuredListTarget(String key, int index, String entry) {
        List<ItemTarget> targets = buildStructuredListTargets(key);
        if (index >= 0 && index < targets.size()) {
            ItemTarget target = targets.get(index);
            if (target != null && (target.hasSlot() || target.hasIdentity())) return target.copy();
        }
        ItemTarget parsed = ItemTarget.fromLegacyEntry(entry);
        return (parsed.hasSlot() || parsed.hasIdentity()) ? parsed : null;
    }

    private boolean usesMinecraftTextRendering(String key) {
        return usesStoreTargetFormatting(key) || isXCarryListKey(key);
    }

    private Component formatItemTargetText(ItemTarget target, String fallback) {
        String safeFallback = fallback == null ? "" : fallback;
        if (target == null) return Component.literal(safeFallback);

        boolean hasSlot = target.hasSlot();
        boolean hasIdentity = target.hasIdentity();
        if (hasSlot && hasIdentity) {
            return Component.literal(target.slot + ": ").append(target.listComponent().copy());
        }
        if (hasSlot) {
            return Component.literal("#" + target.slot);
        }
        if (hasIdentity) {
            Component display = target.listComponent();
            if (display != null && !display.getString().isBlank()) return display.copy();
        }
        return Component.literal(safeFallback);
    }

    private static String buildEntryFromNameAndSlot(String name, String slotText) {
        String n = name == null ? "" : name.strip();
        int slot = -1;
        if (slotText != null && !slotText.isBlank()) {
            try { slot = Integer.parseInt(slotText.replaceAll("[^0-9]", "")); }
            catch (NumberFormatException ignored) {}
        }
        if (slot >= 0 && !n.isEmpty()) return "#" + slot + "|" + n;
        if (slot >= 0) return "#" + slot;
        return n;
    }

    private String parseHandlerEntryName(String entry) {
        if (entry == null || entry.isBlank()) return "";
        if (!entry.startsWith("#")) return entry;
        int separator = entry.indexOf('|');
        return separator >= 0 && separator + 1 < entry.length() ? entry.substring(separator + 1).trim() : "";
    }

    private int parseHandlerSlotField(String text, int fallback) {
        String raw = text == null ? "" : text.trim();
        if (raw.isEmpty()) return fallback;
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) return fallback;
        try {
            int slot = Integer.parseInt(cleaned);
            return slot < 0 ? fallback : slot;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int parseSlotEntryValue(String entry) {
        if (entry == null || !entry.startsWith("#")) return -1;
        try {
            int separator = entry.indexOf('|');
            String raw = separator >= 0 ? entry.substring(1, separator) : entry.substring(1);
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private boolean containsEntryOtherThan(List<String> entries, String entry, int ignoreIndex) {
        for (int i = 0; i < entries.size(); i++) {
            if (i != ignoreIndex && entries.get(i).equals(entry)) return true;
        }
        return false;
    }

    private List<PackUtilSharedState.QueuedPacket> getWorkingQueuedPackets() {
        if (workingTag == null) return new ArrayList<>();
        SendPacketAction action = new SendPacketAction();
        action.fromTag(workingTag);
        return new ArrayList<>(action.packets);
    }

    private void setWorkingQueuedPackets(List<PackUtilSharedState.QueuedPacket> packets) {
        if (workingTag == null) return;
        net.minecraft.nbt.ListTag packetList = new net.minecraft.nbt.ListTag();
        if (packets != null) {
            for (PackUtilSharedState.QueuedPacket qp : packets) {
                CompoundTag packetTag = PackUtilClipboardHelper.serializeQueuedPacket(qp);
                if (packetTag != null) packetList.add(packetTag);
            }
        }
        workingTag.put("packets", packetList);
    }

    private String buildSendPacketInfo() {
        List<PackUtilSharedState.QueuedPacket> packets = getWorkingQueuedPackets();
        if (packets.isEmpty()) return "Packets: 0";

        String autoName = "";
        for (PackUtilSharedState.QueuedPacket qp : packets) {
            if (qp != null && qp.packet != null) {
                autoName = PackUtilPacketNamer.getFriendlyName(qp.packet);
                break;
            }
        }
        if (autoName.isEmpty()) return "Packets: " + packets.size();
        return packets.size() == 1 ? "Packets: 1 - " + autoName : "Packets: " + packets.size() + " - " + autoName;
    }

    private void setRawPacketActionData(PackUtilSharedState.QueuedPacket packet) {
        if (workingTag == null || packet == null) return;
        List<PackUtilSharedState.QueuedPacket> single = java.util.Collections.singletonList(packet);
        String base64 = PackUtilClipboardHelper.serializeQueueToBase64(single);
        if (base64 == null || base64.isBlank()) return;
        workingTag.putString("packetData", base64);
        if (!workingTag.contains("description") || workingTag.getStringOr("description", "").isBlank()) {
            String name = packet.packet != null ? PackUtilPacketNamer.getFriendlyName(packet.packet) : "Packet";
            workingTag.putString("description", name);
        }
    }

    private String buildPacketActionInfo() {
        if (workingTag == null) return "Raw Packet: empty";
        String packetData = workingTag.getStringOr("packetData", "");
        if (packetData == null || packetData.isBlank()) return "Raw Packet: empty";
        String description = workingTag.getStringOr("description", "");
        return description == null || description.isBlank() ? "Raw Packet: loaded" : "Raw Packet: " + description;
    }

    private void startGBreakCaptureForEditor() {
        PackUtilSharedState state = PackUtilSharedState.get();
        screenBeforeGBreak = MC.screen;
        enterEditorOnlyCaptureMode();
        state.setCaptureCancelCallback(() -> {
            state.cancelGBreakCapture();
            if (screenBeforeGBreak != null) {
                MC.setScreen(screenBeforeGBreak);
                screenBeforeGBreak = null;
            }
            exitCaptureMode(false, false);
        });
        MC.execute(() -> { if (MC.screen != null) MC.setScreen(null); });
        PackUtilClientMessaging.sendPrefixed("GBreak: Break a block to capture the insta-break packet");
        state.startGBreakCapture(() -> MC.execute(() -> {
            List<PackUtilSharedState.QueuedPacket> captured = PackUtilSharedState.get().getGBreakCapturedPackets();
            if (!captured.isEmpty()) {
                setWorkingQueuedPackets(java.util.Collections.singletonList(captured.get(0)));
                workingTag.putString("customName", "GBreak");
                PackUtilChatField customNameField = textFields.get("customName");
                if (customNameField != null) customNameField.setText("GBreak");
                PackUtilClientMessaging.sendPrefixed("GBreak packet captured");
            } else {
                PackUtilClientMessaging.sendPrefixed("GBreak capture finished with no packet");
            }

            if (screenBeforeGBreak != null) {
                MC.setScreen(screenBeforeGBreak);
                screenBeforeGBreak = null;
            }
            exitCaptureMode(false, false);
            PackUtilOverlayManager.get().bringToFront(this);
        }));
    }

    private void startLookAtEntityCapture() {
        if (!(targetAction instanceof autismclient.util.macro.LookAtBlockAction)) return;
        Screen previousScreen = MC.screen;
        enterEditorOnlyCaptureMode();
        PackUtilSharedState state = PackUtilSharedState.get();
        state.setCaptureCancelCallback(() -> {
            if (previousScreen != null) MC.setScreen(previousScreen);
            state.setEntityCaptureCallback(null);
            exitCaptureMode(false, false);
        });
        MC.execute(() -> { if (MC.screen != null) MC.setScreen(null); });
        state.setEntityCaptureSpecific(entitySpecificCaptureMode);
        state.setEntityCaptureCallback(payload -> MC.execute(() -> {
            List<String> selected = stringLists.get("entityIds");
            if (selected != null && payload != null && !payload.isBlank()) {
                if (entitySpecificCaptureMode) {
                    selected.removeIf(existing -> existing.startsWith(entitySpecificEntryPrefix(payload)));
                }
                if (!selected.contains(payload)) selected.add(payload);
            }
            if (previousScreen != null) MC.setScreen(previousScreen);
            exitCaptureMode(false, false);
            PackUtilOverlayManager.get().bringToFront(this);
        }));
    }

    private void renderWaitSoundPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "waitForGui", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "waitGuiName", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "checkDistance", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "maxDistance", x, cy, w, mx, my, delta);

        List<String> selected = stringLists.getOrDefault("soundIds", Collections.emptyList());
        cy = renderSimpleSelectedList(ctx, x, cy, w, mx, my, "wait_sound_selected", "Sounds", selected, value -> {
            selected.remove(value);
            PackUiScrollViewport vp = selectedScrollViewports.get("wait_sound_selected");
            if (vp != null) vp.scrollBy(-1);
        }, PackUtilRegistryLabels::sound, "(any sound - add to filter)");

        PackUtilChatField search = addFields.get("soundIds");
        if (search != null) {
            search.setX(x);
            search.setY(cy + 1);
            search.setWidth(w);
            search.render(ctx, mx, my, delta);
        }
        cy += 18;

        List<String> filtered = new ArrayList<>();
        String filter = search != null ? search.getText().trim().toLowerCase() : "";
        for (String id : getAllSoundIds()) {
            if (matchesListFilter(filter, id, trimMinecraftPrefix(id), PackUtilRegistryLabels.sound(id))) filtered.add(id);
        }
        renderSearchRegistryList(
            ctx,
            x,
            cy,
            w,
            mx,
            my,
            "wait_sound_search",
            filtered,
            6,
            value -> {
                if (selected.contains(value)) selected.remove(value);
                else selected.add(value);
            },
            selected::contains,
            PackUtilRegistryLabels::sound
        );
    }

    private void renderWaitEntityPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "checkMode", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "radius", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "centerOnPlayer", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "pos", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "mustBeLookingAt", x, cy, w, mx, my, delta);

        List<String> selected = stringLists.getOrDefault("entityIds", Collections.emptyList());
        cy = renderSimpleSelectedList(ctx, x, cy, w, mx, my, "wait_entity_selected", "Entities", selected, selected::remove,
                this::formatEntityEntry, "(any entity - add type or specific)");

        PackUtilChatField search = addFields.get("entityIds");
        int searchW = w - 66;
        if (search != null) {
            search.setX(x);
            search.setY(cy + 1);
            search.setWidth(searchW);
            search.render(ctx, mx, my, delta);
        }
        int modeX = x + searchW + 2;
        String modeLabel = entitySpecificCaptureMode ? "[Spec]" : "[Type]";
        renderActionButton(ctx, modeX, cy, 30, 14, modeLabel, mx, my, () -> entitySpecificCaptureMode = !entitySpecificCaptureMode);
        renderActionButton(ctx, modeX + 34, cy, 32, 14, "CAP", mx, my, this::startWaitEntityCapture);
        cy += 18;

        List<String> filtered = new ArrayList<>();
        String filter = search != null ? search.getText().trim().toLowerCase() : "";
        for (String id : getAllEntityIds()) {
            if (matchesListFilter(filter, id, trimMinecraftPrefix(id), PackUtilRegistryLabels.entity(id))) filtered.add(id);
        }
        renderSearchRegistryList(
            ctx,
            x,
            cy,
            w,
            mx,
            my,
            "wait_entity_search",
            filtered,
            4,
            value -> {
                if (selected.contains(value)) selected.remove(value);
                else selected.add(value);
            },
            selected::contains,
            PackUtilRegistryLabels::entity
        );
        cy += 4 + 4 * CATALOG_ITEM_H + 10;

        PackUiText.draw(ctx, textRenderer, "Nearby Entities", font, PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 13;
        List<String> nearbyEntries = getNearbyEntityEntries();
        renderSearchRegistryList(
            ctx,
            x,
            cy,
            w,
            mx,
            my,
            "wait_entity_nearby",
            nearbyEntries,
            3,
            value -> {
                String stored = entitySpecificCaptureMode ? value : extractEntityTypeFromNearbyEntry(value);
                if (stored.isBlank()) return;
                if (entitySpecificCaptureMode) {
                    boolean removed = selected.removeIf(existing -> existing.startsWith(entitySpecificEntryPrefix(stored)));
                    if (!removed) selected.add(stored);
                } else if (selected.contains(stored)) {
                    selected.remove(stored);
                } else {
                    selected.add(stored);
                }
            },
            value -> {
                String stored = entitySpecificCaptureMode ? value : extractEntityTypeFromNearbyEntry(value);
                return entitySpecificCaptureMode
                    ? selected.stream().anyMatch(existing -> existing.startsWith(entitySpecificEntryPrefix(stored)))
                    : selected.contains(stored);
            },
            this::formatNearbyEntityEntry
        );
    }

    private void renderOpenContainerPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "targetMode", x, cy, w, mx, my, delta);
        String targetMode = currentEnumValue("targetMode");
        if ("ENTITY".equals(targetMode)) {
            cy = renderFieldByKey(ctx, "entityTargets", x, cy, w, mx, my, delta);
        } else if ("BLOCK".equals(targetMode)) {
            cy = renderFieldByKey(ctx, "pos", x, cy, w, mx, my, delta);
        }
        cy = renderFieldByKey(ctx, "waitForGui", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "guiName", x, cy, w, mx, my, delta);
        if ("ENTITY".equals(targetMode)) {
            cy = renderEditorHint(ctx, x, cy, w, "Pick captures the exact container entity. Last Target reuses the most recent open target.");
            List<String> selected = stringLists.getOrDefault("entityTargets", Collections.emptyList());
            cy += 13;
            List<String> nearbyEntries = getNearbyEntityEntries();
            renderSearchRegistryList(
                ctx,
                x,
                cy,
                w,
                mx,
                my,
                "open_container_entity_nearby",
                nearbyEntries,
                3,
                value -> {
                    selected.clear();
                    selected.add(value);
                },
                selected::contains,
                this::formatNearbyEntityEntry
            );
        } else if ("LAST_TARGET".equals(targetMode)) {
            renderEditorHint(ctx, x, cy, w, "Uses the last block or entity container you actually opened.");
        } else {
            cy = renderEditorHint(ctx, x, cy, w, "Capture only marks the block. It will not open it.");
            renderNearbyContainerList(ctx, x, cy, w, mx, my, "open_container_nearby", "pos");
        }
    }

    private void renderNearbyContainerList(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my,
                                           String listKey, String fieldKey) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        PackUiText.draw(ctx, textRenderer, "Nearby Containers", font, PackUtilColors.textSecondary(), x, y + 2, false);
        y += 13;
        List<BlockPos> nearbyContainers = getNearbyContainerPositions();
        renderSearchRegistryList(
            ctx,
            x,
            y,
            w,
            mx,
            my,
            listKey,
            nearbyContainers,
            CONTAINER_LIST_VISIBLE_ROWS,
            pos -> fillBlockPosField(fieldKey, pos),
            pos -> isCurrentBlockPosField(fieldKey, pos),
            this::formatContainerEntry
        );
    }

    private void renderStoreItemPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "mode", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "allItems", x, cy, w, mx, my, delta);

        List<String> selected = stringLists.getOrDefault("targetItems", Collections.emptyList());
        if (!toggleStates.getOrDefault("allItems", false)) {
            cy = renderSimpleSelectedList(ctx, x, cy, w, mx, my, "store_items_selected", "Target Items", selected,
                    selected::remove, autismclient.util.macro.StoreItemAction::formatTargetEntry,
                    value -> formatStringListEntryText("targetItems", value, selected.indexOf(value)),
                    selected::clear,
                    "(nothing selected - search, type #slot, or capture)", true);

            int storeEditIdx = stringListEditIndex.getOrDefault("store_items_selected", -1);
            boolean storeEditing = storeEditIdx >= 0 && storeEditIdx < selected.size();
            PackUtilChatField search = addFields.get("targetItems");
            int pickW   = 32;
            int addBtnW = 34;
            int slotW   = 44;
            int capBtnX = x + w - pickW;
            int addBtnX = capBtnX - 3 - addBtnW;
            int slotX   = addBtnX - 2 - slotW;
            int nameW   = slotX - x - 2;

            PackUtilChatField storeSlotF = textFields.get("store_slot");
            if (storeSlotF == null) {
                storeSlotF = makeField(slotW);
                storeSlotF.setNumericOnly(true);
                storeSlotF.setPlaceholder(Component.literal("Slot#"));
                textFields.put("store_slot", storeSlotF);
            }

            String pendingText = stringListEditPendingText.remove("store_items_selected");
            if (pendingText != null) {
                if (search != null) search.setText(parseHandlerEntryName(pendingText));
                ItemTarget parsed = ItemTarget.fromLegacyEntry(pendingText);
                storeSlotF.setText(parsed.hasSlot() ? String.valueOf(parsed.slot) : "");
            }

            if (search != null) {
                search.setX(x); search.setY(cy + 1); search.setWidth(nameW);
                search.render(ctx, mx, my, delta);
            }
            storeSlotF.setX(slotX); storeSlotF.setY(cy + 1); storeSlotF.setWidth(slotW);
            storeSlotF.render(ctx, mx, my, delta);

            if (storeEditing && search != null) {
                String nameText = search.getText().strip();
                String slotText = storeSlotF.getText().strip();
                String entry = buildEntryFromNameAndSlot(nameText, slotText);
                if (!entry.isEmpty()) {
                    String normalized = autismclient.util.macro.StoreItemAction.normalizeTargetEntry(entry);
                    if (normalized != null && !normalized.isBlank() && !normalized.equals(selected.get(storeEditIdx))) {
                        selected.set(storeEditIdx, normalized);
                        editorItemLists.put("targetItems", buildStructuredListTargets("targetItems"));
                    }
                }
            }

            final PackUtilChatField fStoreSlotF = storeSlotF;
            renderOverlayButton(ctx, addBtnX, cy, addBtnW, 14, "+Add", PackUiOverlayButton.Variant.SUCCESS, true, mx, my, () -> {
                if (search == null) return;
                String nameText = search.getText().strip();
                String slotText = fStoreSlotF.getText().strip();
                String entry = buildEntryFromNameAndSlot(nameText, slotText);
                if (!entry.isEmpty()) {
                    String normalized = autismclient.util.macro.StoreItemAction.normalizeTargetEntry(entry);
                    if (normalized != null && !normalized.isBlank() && !selected.contains(normalized)) {
                        selected.add(normalized);
                        editorItemLists.put("targetItems", buildStructuredListTargets("targetItems"));
                    }
                    search.setText("");
                    fStoreSlotF.setText("");
                    stringListEditIndex.put("store_items_selected", -1);
                }
            });
            boolean capturing = "targetItems".equals(itemSlotCapturePendingKey);
            renderOverlayButton(ctx, capBtnX, cy, pickW, 14, capturing ? "Done" : "Pick",
                    capturing ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.PRIMARY,
                    true, mx, my, () -> {
                if ("targetItems".equals(itemSlotCapturePendingKey)) {
                    itemSlotCapturePendingKey = null;
                    exitCaptureMode(false, false);
                } else {
                    itemSlotCapturePendingKey = "targetItems";
                    enterCaptureMode();
                }
            });
            cy += 18;

            String filter = search != null ? search.getText().trim().toLowerCase() : "";
            List<String> filtered = new ArrayList<>();
            for (String id : getAllItemIds()) {
                if (matchesListFilter(filter, id, trimMinecraftPrefix(id), PackUtilRegistryLabels.item(id))) {
                    filtered.add(id);
                }
            }
            renderSearchRegistryList(
                ctx,
                x,
                cy,
                w,
                mx,
                my,
                "store_items_search",
                filtered,
                6,
                value -> {
                    if (selected.contains(value)) selected.remove(value);
                    else selected.add(value);
                    stringListEditIndex.put("store_items_selected", -1);
                    editorItemLists.put("targetItems", buildStructuredListTargets("targetItems"));
                },
                selected::contains,
                PackUtilRegistryLabels::item
            );
            cy += 6 * CATALOG_ITEM_H + 4;

            cy = renderEditorHint(ctx, x, cy, w, "Pick on an item adds the item. Pick on an empty slot adds that exact slot.");

            String modeHint = "LOOT".equals(currentEnumValue("mode"))
                    ? "Pick only accepts chest/custom GUI slots here."
                    : "Pick only accepts player inventory slots here.";
            cy = renderEditorHint(ctx, x, cy, w, modeHint);
        }

        cy = renderFieldByKey(ctx, "persistent", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "closeAfter", x, cy, w, mx, my, delta);
        renderFieldByKey(ctx, "closeSendPkt", x, cy, w, mx, my, delta);
    }

    private void renderInventoryAuditPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "mode", x, cy, w, mx, my, delta);

        List<String> selected = stringLists.getOrDefault("targetItems", Collections.emptyList());
        FieldDef targetItemsField = getField("targetItems");
        if (targetItemsField != null && isFieldVisible(targetItemsField)) {
            String emptyLabel = getStringListEmptyHint("targetItems");
            if (emptyLabel == null || emptyLabel.isBlank()) emptyLabel = "(nothing selected)";
            boolean multipleStacks = toggleStates.getOrDefault("multipleStacks", false);
            int listTop = cy;

            cy = renderSimpleSelectedList(ctx, x, cy, w, mx, my, "audit_items_selected", "Targets", selected,
                    selected::remove, autismclient.util.macro.StoreItemAction::formatTargetEntry,
                    value -> formatStringListEntryText("targetItems", value, selected.indexOf(value)),
                    selected::clear,
                    emptyLabel,
                    true);

            int multiBtnW = 96;
            int clearBtnW = 44;
            int multiBtnX = x + w - clearBtnW - 3 - multiBtnW;
            renderOverlayButton(ctx, multiBtnX, listTop, multiBtnW, 14, "Multiple Stacks",
                    multipleStacks ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.GHOST,
                    true, mx, my, () -> toggleStates.put("multipleStacks", !toggleStates.getOrDefault("multipleStacks", false)));

            int auditEditIdx = stringListEditIndex.getOrDefault("audit_items_selected", -1);
            boolean auditEditing = auditEditIdx >= 0 && auditEditIdx < selected.size();
            PackUtilChatField search = addFields.get("targetItems");
            int pickW = 32;
            int addBtnW = 34;
            int slotW = 26;
            int pickX = x + w - addBtnW - 3 - pickW;
            int addBtnX = x + w - addBtnW;
            int slotX = pickX - 3 - slotW;
            int nameW = slotX - x - 2;

            PackUtilChatField auditSlotF = textFields.get("audit_slot");
            if (auditSlotF == null) {
                auditSlotF = makeField(slotW);
                auditSlotF.setNumericOnly(true);
                auditSlotF.setPlaceholder(Component.literal("Slot"));
                textFields.put("audit_slot", auditSlotF);
            }

            String pendingText = stringListEditPendingText.remove("audit_items_selected");
            if (pendingText != null) {
                if (search != null) search.setText(parseHandlerEntryName(pendingText));
                ItemTarget parsed = ItemTarget.fromLegacyEntry(pendingText);
                auditSlotF.setText(parsed.hasSlot() ? String.valueOf(parsed.slot) : "");
            }

            if (search != null) {
                search.setX(x); search.setY(cy + 1); search.setWidth(nameW);
                search.render(ctx, mx, my, delta);
            }
            auditSlotF.setX(slotX); auditSlotF.setY(cy + 1); auditSlotF.setWidth(slotW);
            auditSlotF.render(ctx, mx, my, delta);

            if (auditEditing && search != null) {
                String nameText = search.getText().strip();
                String slotText = auditSlotF.getText().strip();
                String entry = buildEntryFromNameAndSlot(nameText, slotText);
                if (!entry.isEmpty()) {
                    String normalized = autismclient.util.macro.StoreItemAction.normalizeTargetEntry(entry);
                    if (normalized != null && !normalized.isBlank() && !normalized.equals(selected.get(auditEditIdx))) {
                        selected.set(auditEditIdx, normalized);
                        editorItemLists.put("targetItems", buildStructuredListTargets("targetItems"));
                    }
                }
            }

            final PackUtilChatField fAuditSlotF = auditSlotF;
            boolean capturing = "targetItems".equals(itemSlotCapturePendingKey);
            renderOverlayButton(ctx, pickX, cy, pickW, 14, capturing ? "Done" : "Pick",
                    capturing ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.PRIMARY,
                    true, mx, my, () -> {
                if ("targetItems".equals(itemSlotCapturePendingKey)) {
                    itemSlotCapturePendingKey = null;
                    exitCaptureMode(false, false);
                } else {
                    itemSlotCapturePendingKey = "targetItems";
                    enterCaptureMode();
                }
            });

            String addLabel = auditEditing ? "New" : "+Add";
            renderOverlayButton(ctx, addBtnX, cy, addBtnW, 14, addLabel, PackUiOverlayButton.Variant.SUCCESS, true, mx, my, () -> {
                if (search == null) return;
                if (stringListEditIndex.getOrDefault("audit_items_selected", -1) >= 0) {
                    stringListEditIndex.put("audit_items_selected", -1);
                    stringListEditPendingText.put("audit_items_selected", "");
                    search.setText("");
                    fAuditSlotF.setText("");
                    return;
                }

                String nameText = search.getText().strip();
                String slotText = fAuditSlotF.getText().strip();
                String entry = buildEntryFromNameAndSlot(nameText, slotText);
                if (!entry.isEmpty()) {
                    String normalized = autismclient.util.macro.StoreItemAction.normalizeTargetEntry(entry);
                    if (normalized != null && !normalized.isBlank() && !selected.contains(normalized)) {
                        selected.add(normalized);
                        editorItemLists.put("targetItems", buildStructuredListTargets("targetItems"));
                    }
                    search.setText("");
                    fAuditSlotF.setText("");
                    stringListEditIndex.put("audit_items_selected", -1);
                    stringListEditPendingText.put("audit_items_selected", "");
                }
            });
            cy += 18;
        }

        cy = renderFieldByKey(ctx, "openMode", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "openCommand", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "containerPos", x, cy, w, mx, my, delta);
        if ("CONTAINER".equals(currentEnumValue("openMode"))) {
            cy = renderEditorHint(ctx, x, cy, w, "Capture stores the clicked block position. Nearby also includes likely server-handled GUI blocks.");
            renderNearbyContainerList(ctx, x, cy, w, mx, my, "inventory_audit_nearby", "containerPos");
            cy += CONTAINER_LIST_VISIBLE_ROWS * CATALOG_ITEM_H + 17;
        }
        cy = renderFieldByKey(ctx, "dupeVector", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "iterations", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "maxTransferAttempts", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "transferRetryDelayMs", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "spamCount", x, cy, w, mx, my, delta);
        renderFieldByKey(ctx, "spamDelayMs", x, cy, w, mx, my, delta);
    }

    private void renderPayPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "commandTemplate", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "amountInput", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "delayEnabled", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "delayMs", x, cy, w, mx, my, delta);

        List<String> selected = stringLists.getOrDefault("players", Collections.emptyList());
        cy = renderSimpleSelectedList(ctx, x, cy, w, mx, my, "pay_players_selected", "Players", selected,
                value -> removeStringIgnoreCase(selected, value), value -> value, ignored -> null, selected::clear,
                "(scan the server or add one manually)");

        PackUtilChatField search = addFields.get("players");
        int scanW = 34;
        int allW = 28;
        int searchW = Math.max(80, w - scanW - allW - 4);
        if (search != null) {
            search.setX(x);
            search.setY(cy + 1);
            search.setWidth(searchW);
            search.render(ctx, mx, my, delta);
        }
        int scanX = x + searchW + 2;
        int allX = scanX + scanW + 2;
        renderActionButton(ctx, scanX, cy, scanW, 14, "Scan", mx, my, this::refreshPayScannedPlayers);
        renderActionButton(ctx, allX, cy, allW, 14, "All", mx, my, () -> addFilteredPayPlayers(
                selected, search != null ? search.getText() : ""
        ));
        cy += 18;

        List<String> filtered = filterEntries(payScannedPlayers, search != null ? search.getText() : "");
        if (!payPlayerScanPerformed) {
            renderRegistryPlaceholder(ctx, x, cy, w, "pay_players_scan", 6, "Press Scan to load the current server players.");
        } else {
            renderSearchRegistryList(
                ctx,
                x,
                cy,
                w,
                mx,
                my,
                "pay_players_scan",
                filtered,
                6,
                value -> togglePayPlayerSelection(selected, value),
                value -> containsIgnoreCase(selected, value),
                value -> value
            );
        }
        cy += 6 * CATALOG_ITEM_H + 4;

        PackUtilChatField amountField = textFields.get("amountInput");
        long amount = autismclient.util.macro.PayAction.parseAmount(amountField != null ? amountField.getText() : "");
        String summary = selected.isEmpty()
                ? "No players selected."
                : "Pays " + selected.size() + " player(s) " + autismclient.util.macro.PayAction.formatAmount(amount) + " each.";
        cy = renderEditorHint(ctx, x, cy, w, summary);

        String hint = payPlayerScanPerformed
                ? "Click a scanned name to toggle it, or use All to add the filtered scan results."
                : "The search box also lets you manually add a player by pressing Enter.";
        renderEditorHint(ctx, x, cy, w, hint);
    }

    private void renderToggleModulePanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;
        List<autismclient.util.macro.ToggleModuleAction.ModuleEntry> selected =
                toggleModuleEntries != null ? toggleModuleEntries : Collections.emptyList();

        PackUiText.draw(ctx, textRenderer, "Module Actions (" + selected.size() + ")", font, PackUtilColors.textSecondary(), x, cy + 2, false);
        boolean canClear = !selected.isEmpty();
        int clearX = x + w - 44;
        renderOverlayButton(ctx, clearX, cy, 44, 14, "Clear",
                PackUiOverlayButton.Variant.DANGER, canClear, mx, my, () -> {
                    selected.clear();
                    PackUiScrollViewport vp = selectedScrollViewports.get("toggle_module_selected");
                    if (vp != null) vp.jumpTo(0);
                });
        cy += 13;

        int listH = 4 * SEL_ITEM_H;
        int itemW = w - SCROLLBAR_W - 1;

        PackUiScrollViewport toggleViewport = getOrCreateViewport(selectedScrollViewports, "toggle_module_selected",
            x, cy, w, listH, SEL_ITEM_H, SCROLLBAR_W);
        toggleViewport.setContentHeight(selected.size() * SEL_ITEM_H);
        selectedListBounds.put("toggle_module_selected", new int[]{cy, listH});

        toggleViewport.renderScrollbar(ctx, mx, my);

        if (selected.isEmpty()) {
            PackUiListRenderer.drawEmptyState(ctx, textRenderer, "Pick modules below.", x, cy, itemW);
        } else {
            toggleViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
            int first = toggleViewport.getFirstVisibleRow();
            for (int i = first; i < selected.size() && i <= toggleViewport.getLastVisibleRow(); i++) {
                int iy = toggleViewport.getRowScreenY(i);
                if (iy == Integer.MIN_VALUE) continue;
                autismclient.util.macro.ToggleModuleAction.ModuleEntry entry = selected.get(i);
                int removeW = 13;
                int modeW = 52;
                int rowW = itemW - removeW - modeW - 4;
                boolean hovered = mx >= x && mx < x + rowW && my >= iy && my < iy + 13;
                PackUiListRenderer.drawRow(
                        ctx,
                        textRenderer,
                        entry.moduleName,
                        x,
                        iy,
                        rowW,
                        13,
                        hovered,
                        false,
                        PackUiListRenderer.RowTone.NORMAL
                );

                int modeX = x + rowW + 2;
                String modeLabel = formatToggleModeShort(entry.toggleMode);
                final int entryIndex = i;
                renderOverlayButton(ctx, modeX, iy, modeW, 13, modeLabel,
                        entry.toggleMode == autismclient.util.macro.ToggleModuleAction.ToggleMode.ENABLE
                                ? PackUiOverlayButton.Variant.SUCCESS
                                : (entry.toggleMode == autismclient.util.macro.ToggleModuleAction.ToggleMode.DISABLE
                                    ? PackUiOverlayButton.Variant.DANGER
                                    : PackUiOverlayButton.Variant.GHOST),
                        true, mx, my, () -> cycleToggleModuleEntryMode(entryIndex));

                int removeX = modeX + modeW + 2;
                renderIconDeleteButton(ctx, removeX, iy, removeW, mx, my, () -> {
                    if (entryIndex >= 0 && entryIndex < selected.size()) {
                        selected.remove(entryIndex);
                        PackUiScrollViewport vp = selectedScrollViewports.get("toggle_module_selected");
                        if (vp != null) vp.scrollBy(-1);
                    }
                });
            }
            toggleViewport.endRender(ctx);
        }
        cy += listH + 4;

        PackUtilChatField search = addFields.get("_toggle_module_search");
        int refreshW = 52;
        int addW = 40;
        int searchW = Math.max(80, w - refreshW - addW - 4);
        if (search != null) {
            search.setX(x);
            search.setY(cy + 1);
            search.setWidth(searchW);
            search.render(ctx, mx, my, delta);
        }
        int refreshX = x + searchW + 2;
        int addX = refreshX + refreshW + 2;
        renderActionButton(ctx, refreshX, cy, refreshW, 14, "Refresh", mx, my, this::refreshMeteorModuleNames);
        renderActionButton(ctx, addX, cy, addW, 14, "+Add", mx, my, () -> addFirstFilteredModule(search != null ? search.getText() : ""));
        cy += 18;

        List<String> filtered = filterEntries(meteorModuleNames, search != null ? search.getText() : "");
        if (meteorModuleNames.isEmpty()) {
            renderRegistryPlaceholder(ctx, x, cy, w, "toggle_module_registry", 6, "No Meteor modules found right now.");
        } else {
            renderSearchRegistryList(ctx, x, cy, w, mx, my, "toggle_module_registry", filtered, 6,
                    this::addToggleModuleEntry,
                    value -> containsModuleEntry(selected, value),
                    value -> value);
        }
        cy += 6 * CATALOG_ITEM_H + 4;

        String hint = selected.isEmpty()
                ? "Pick modules below, then click the mode chip on each row."
                : "Each row runs with its own Toggle / Enable / Disable setting.";
        renderEditorHint(ctx, x, cy, w, hint);
    }

    private void renderRegistryPlaceholder(GuiGraphicsExtractor ctx, int x, int y, int w, String listKey, int visibleRows, String message) {
        int listH = visibleRows * CATALOG_ITEM_H;
        int itemW = w - SCROLLBAR_W - 1;
        catalogListBounds.put(listKey, new int[]{y, listH});
        ctx.fill(x, y, x + itemW, y + 12, PackUtilColors.rowNormal());
        PackUiListRenderer.drawEmptyState(ctx, textRenderer, formatEditorHint(message), x, y, itemW);
    }

    private void refreshPayScannedPlayers() {
        payScannedPlayers.clear();
        payPlayerScanPerformed = true;
        PackUiScrollViewport vp = catalogScrollViewports.get("pay_players_scan");
        if (vp != null) vp.jumpTo(0);
        if (MC.getConnection() == null) return;

        for (net.minecraft.client.multiplayer.PlayerInfo entry : MC.getConnection().getListedOnlinePlayers()) {
            if (entry == null || entry.getProfile() == null) continue;
            String name = entry.getProfile().name();
            if (name != null && !name.isBlank() && !containsIgnoreCase(payScannedPlayers, name)) {
                payScannedPlayers.add(name);
            }
        }
        payScannedPlayers.sort(String::compareToIgnoreCase);
    }

    private void refreshMeteorModuleNames() {
        meteorModuleNames.clear();
        PackUiScrollViewport vp = catalogScrollViewports.get("toggle_module_registry");
        if (vp != null) vp.jumpTo(0);
        for (String moduleName : autismclient.util.PackUtilCompatManager.getMeteorModuleNames()) {
            if (moduleName != null && !moduleName.isBlank() && !containsIgnoreCase(meteorModuleNames, moduleName)) {
                meteorModuleNames.add(moduleName);
            }
        }
        meteorModuleNames.sort(String::compareToIgnoreCase);
    }

    private List<String> filterEntries(List<String> source, String query) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        String filter = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (filter.isEmpty()) return new ArrayList<>(source);

        List<String> filtered = new ArrayList<>();
        for (String value : source) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(filter)) {
                filtered.add(value);
            }
        }
        return filtered;
    }

    private void addFilteredPayPlayers(List<String> selected, String query) {
        if (selected == null) return;
        for (String player : filterEntries(payScannedPlayers, query)) {
            if (!containsIgnoreCase(selected, player)) selected.add(player);
        }
    }

    private void togglePayPlayerSelection(List<String> selected, String player) {
        if (selected == null || player == null || player.isBlank()) return;
        if (containsIgnoreCase(selected, player)) removeStringIgnoreCase(selected, player);
        else selected.add(player);
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        if (values == null || target == null) return false;
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(target)) return true;
        }
        return false;
    }

    private void removeStringIgnoreCase(List<String> values, String target) {
        if (values == null || target == null) return;
        values.removeIf(value -> value != null && value.equalsIgnoreCase(target));
    }

    private void addFirstFilteredModule(String query) {
        List<String> filtered = filterEntries(meteorModuleNames, query);
        if (!filtered.isEmpty()) addToggleModuleEntry(filtered.get(0));
    }

    private void addToggleModuleEntry(String moduleName) {
        if (toggleModuleEntries == null || moduleName == null || moduleName.isBlank() || containsModuleEntry(toggleModuleEntries, moduleName)) return;
        toggleModuleEntries.add(new autismclient.util.macro.ToggleModuleAction.ModuleEntry(moduleName, autismclient.util.macro.ToggleModuleAction.ToggleMode.TOGGLE));
    }

    private boolean containsModuleEntry(List<autismclient.util.macro.ToggleModuleAction.ModuleEntry> entries, String moduleName) {
        if (entries == null || moduleName == null) return false;
        for (autismclient.util.macro.ToggleModuleAction.ModuleEntry entry : entries) {
            if (entry != null && entry.moduleName != null && entry.moduleName.equalsIgnoreCase(moduleName)) return true;
        }
        return false;
    }

    private void cycleToggleModuleEntryMode(int index) {
        if (toggleModuleEntries == null || index < 0 || index >= toggleModuleEntries.size()) return;
        autismclient.util.macro.ToggleModuleAction.ModuleEntry entry = toggleModuleEntries.get(index);
        entry.toggleMode = switch (entry.toggleMode) {
            case TOGGLE -> autismclient.util.macro.ToggleModuleAction.ToggleMode.ENABLE;
            case ENABLE -> autismclient.util.macro.ToggleModuleAction.ToggleMode.DISABLE;
            case DISABLE -> autismclient.util.macro.ToggleModuleAction.ToggleMode.TOGGLE;
        };
    }

    private String formatToggleModeShort(autismclient.util.macro.ToggleModuleAction.ToggleMode mode) {
        return switch (mode) {
            case ENABLE -> "Enable";
            case DISABLE -> "Disable";
            default -> "Toggle";
        };
    }

    private void renderWaitSlotChangePanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;
        int headerBtnW = 44;

        List<WaitForSlotChangeAction.WaitEntry> entries =
                wscEntries != null ? wscEntries : new ArrayList<>();
        if (wscEditIndex >= entries.size()) wscEditIndex = -1;

        PackUiText.draw(ctx, textRenderer,
                "Items / Slots (" + entries.size() + ")", font,
                PackUtilColors.textSecondary(), x, cy + 2, false);
        boolean canClear = !entries.isEmpty();
        int clearX = x + w - headerBtnW;
        renderOverlayButton(ctx, clearX, cy, headerBtnW, 14, "Clear",
                PackUiOverlayButton.Variant.DANGER, canClear, mx, my, () -> {
            entries.clear(); wscEditIndex = -1; clearWscEditorFields();
        });
        cy += 13;

        int delW    = 13;
        int rowW    = w - SCROLLBAR_W - 1 - delW - 2;

        int selAreaH  = SEL_LIST_MAX_VIS * SEL_ITEM_H;

        PackUiScrollViewport wscViewport = getOrCreateViewport(selectedScrollViewports, "_wsc_entries",
            x, cy, w, selAreaH, SEL_ITEM_H, SCROLLBAR_W);
        wscViewport.setContentHeight(entries.size() * SEL_ITEM_H);
        selectedListBounds.put("_wsc_entries", new int[]{cy, selAreaH});

        wscViewport.renderScrollbar(ctx, mx, my);

        if (!entries.isEmpty()) {
            wscViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
            int firstVis = wscViewport.getFirstVisibleRow();
            for (int i = firstVis; i < entries.size() && i <= wscViewport.getLastVisibleRow(); i++) {
                int iy = wscViewport.getRowScreenY(i);
                if (iy == Integer.MIN_VALUE) continue;
                boolean selected = (i == wscEditIndex);
                WaitForSlotChangeAction.WaitEntry e = entries.get(i);

                Component targetDisp = formatItemTargetText(e.resolvedTarget(), "(any slot)");
                String modeSummary = " \u2022 " + e.modeLabel();
                Component rowLabel = Component.empty().append(targetDisp).append(
                        Component.literal(modeSummary).withStyle(s -> s.withColor(0xFF888888)));
                boolean rowHovered = mx >= x && mx < x + rowW && my >= iy && my < iy + 13;
                PackUiListRenderer.drawRow(
                        ctx,
                        textRenderer,
                        rowLabel,
                        x,
                        iy,
                        rowW,
                        13,
                        rowHovered,
                        selected,
                        PackUiListRenderer.RowTone.NORMAL,
                        true
                );

                final int fi = i;
                if (iy + 13 > cy && iy < cy + selAreaH) {
                    hitRegions.add(new HitRegion(x, Math.max(cy, iy), rowW, Math.min(iy + 13, cy + selAreaH) - Math.max(cy, iy), () -> toggleWscEditSelection(fi)));
                }

                int delX = x + rowW + 2;
                renderIconDeleteButton(ctx, delX, iy, delW, mx, my, () -> {
                    if (wscEditIndex == fi) { wscEditIndex = -1; clearWscEditorFields(); }
                    else if (wscEditIndex > fi) wscEditIndex--;
                    entries.remove(fi);
                    PackUiScrollViewport vp = selectedScrollViewports.get("_wsc_entries");
                    if (vp != null) vp.scrollBy(-1);
                });
            }
            wscViewport.endRender(ctx);
        } else {
            PackUiListRenderer.drawEmptyState(ctx, textRenderer, "No entries yet. Add an item or slot below.", x, cy, w - SCROLLBAR_W - 1);
        }
        cy += selAreaH;

        ctx.fill(x, cy + 2, x + w, cy + 3, PackUtilColors.subPanelBorder());
        cy += 6;

        PackUtilChatField wscAddF  = addFields.get("_wsc_add");
        PackUtilChatField wscSlotF = textFields.get("wsc_slot");
        if (wscAddF != null && wscSlotF != null) {
            int pickW = 32;
            int slotW = 44;
            int pickX = x + w - pickW;
            int slotX = pickX - 2 - slotW;
            wscAddF.setX(x);      wscAddF.setY(cy + 1);  wscAddF.setWidth(slotX - x - 2);
            wscSlotF.setX(slotX); wscSlotF.setY(cy + 1); wscSlotF.setWidth(slotW);
            wscAddF.render(ctx, mx, my, delta);
            wscSlotF.render(ctx, mx, my, delta);

            boolean capturing = "_wsc_entries".equals(itemSlotCapturePendingKey);
            String pickLbl = capturing ? "Done" : "Pick";
            renderOverlayButton(ctx, pickX, cy, pickW, 14, pickLbl,
                    capturing ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.PRIMARY,
                    true, mx, my, () -> {
                if ("_wsc_entries".equals(itemSlotCapturePendingKey)) {
                    itemSlotCapturePendingKey = null;
                    exitCaptureMode(false, false);
                } else {
                    itemSlotCapturePendingKey = "_wsc_entries";
                    enterCaptureMode();
                }
            });
        }
        cy += 16;

        {
            boolean editing = wscEditIndex >= 0 && wscEditIndex < entries.size();
            WaitForSlotChangeAction.WaitMode curMode  =
                    editing ? entries.get(wscEditIndex).waitMode  : wscAddMode;
            int curCount = editing ? entries.get(wscEditIndex).targetCount : wscAddCount;

            int addBtnW = 34;
            int cntW    = 36;
            int modBtnW = w - addBtnW - 2 - cntW - 2;

            String modeFull = curMode.name().replace("_", " ");
            renderOverlayButton(ctx, x, cy, modBtnW, 14, modeFull,
                    PackUiOverlayButton.Variant.GHOST, true, mx, my, () -> {
                WaitForSlotChangeAction.WaitMode[] modes = WaitForSlotChangeAction.WaitMode.values();
                if (wscEditIndex >= 0 && wscEditIndex < entries.size()) {
                    entries.get(wscEditIndex).cycleMode();
                } else {
                    wscAddMode = modes[(wscAddMode.ordinal() + 1) % modes.length];
                }
            });

            int cntX = x + modBtnW + 2;
            PackUtilChatField countF = textFields.get("wsc_count");
            boolean countRelevant = curMode == WaitForSlotChangeAction.WaitMode.COUNT_AT_LEAST
                                 || curMode == WaitForSlotChangeAction.WaitMode.COUNT_BELOW;
            if (countF != null) {
                if (countRelevant) {
                    countF.setX(cntX); countF.setY(cy + 1); countF.setWidth(cntW);
                    if (!countF.isFocused() && !suppressWscLiveUpdate) {
                        suppressWscLiveUpdate = true;
                        countF.setText(String.valueOf(curCount));
                        suppressWscLiveUpdate = false;
                    }
                    countF.render(ctx, mx, my, delta);
                } else {
                    ctx.fill(cntX, cy, cntX + cntW, cy + 14, 0xFF0E0A0A);
                    fillBorder(ctx, cntX, cy, cntW, 14, 0xFF2A1A1A);
                    PackUiText.draw(ctx, textRenderer, "-", font, PackUtilColors.textDim(),
                            cntX + (cntW - uiWidth(font, "-")) / 2, cy + 3, false);
                }
            }

            int plusX = cntX + cntW + 2;
            renderOverlayButton(ctx, plusX, cy, addBtnW, 14, "+Add",
                    PackUiOverlayButton.Variant.SUCCESS, true, mx, my, () -> {
                if (wscAddF != null && wscSlotF != null) {
                    applyWscAddEntry(wscAddF, wscSlotF);
                    wscEditIndex = -1;
                    clearWscEditorFields();
                }
            });
        }
        cy += 16;

        renderEditorHint(ctx, x, cy, w,
                wscEditIndex >= 0 ? "Editing selected row. Use Mode below to cycle. ALL entries must match."
                        : "Click a row to edit. ALL entries must match before proceeding.");
    }

    private void renderUseItemPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "itemName", x, cy, w, mx, my, delta);
        int btnW = (w - 4) / 2;
        renderActionButton(ctx, x, cy, btnW, 14, "Use Held", mx, my, this::fillUseItemFromHeld);
        renderActionButton(ctx, x + btnW + 4, cy, btnW, 14, "Use Current", mx, my, () -> {
            PackUtilChatField field = textFields.get("itemName");
            if (field != null) field.setText("");
        });
        cy += 18;

        cy = renderFieldByKey(ctx, "useMode", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "holdTicks", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "useCount", x, cy, w, mx, my, delta);

        String itemTarget = currentUseItemTargetLabel();
        String mode = currentEnumValue("useMode");
        String hint = "CUSTOM_HOLD".equals(mode)
                ? "Hold-uses " + itemTarget + " for the set ticks."
                : "Uses " + itemTarget + " for the set count.";
        renderEditorHint(ctx, x, cy, w, hint);
    }

    private void renderRotatePanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "yaw", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "pitch", x, cy, w, mx, my, delta);
        renderActionButton(ctx, x, cy, w, 14, "Capture View", mx, my, this::fillRotateFromCurrentView);
        cy += 18;
        cy = renderFieldByKey(ctx, "smooth", x, cy, w, mx, my, delta);
        if (toggleStates.getOrDefault("smooth", false)) {
            cy = renderRotateSmoothnessSlider(ctx, x, cy, w, mx, my);
        } else {
            rotateSmoothnessSliderDragging = false;
            clearRotateSmoothnessSliderBounds();
        }
        cy = renderFieldByKey(ctx, "waitForCompletion", x, cy, w, mx, my, delta);
        renderEditorHint(ctx, x, cy, w,
                toggleStates.getOrDefault("smooth", false)
                        ? "Capture View fills in your current yaw and pitch. Smoothness 1 is faster, 10 is slower."
                        : "Capture View fills in your current yaw and pitch.");
    }

    private void renderLookAtPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "targetMode", x, cy, w, mx, my, delta);
        String mode = currentEnumValue("targetMode");
        if ("BLOCK".equals(mode)) {
            cy = renderFieldByKey(ctx, "searchRadius", x, cy, w, mx, my, delta);
            cy = renderFieldByKey(ctx, "blockIds", x, cy, w, mx, my, delta);
        } else if ("ENTITY".equals(mode)) {
            cy = renderFieldByKey(ctx, "searchRadius", x, cy, w, mx, my, delta);
            cy = renderLookAtEntitySelector(ctx, x, cy, w, mx, my, delta);
        } else {
            cy = renderFieldByKey(ctx, "blockPos", x, cy, w, mx, my, delta);
        }

        cy = renderFieldByKey(ctx, "smooth", x, cy, w, mx, my, delta);
        if (toggleStates.getOrDefault("smooth", false)) {
            cy = renderRotateSmoothnessSlider(ctx, x, cy, w, mx, my);
        } else {
            rotateSmoothnessSliderDragging = false;
            clearRotateSmoothnessSliderBounds();
        }
        cy = renderFieldByKey(ctx, "waitForCompletion", x, cy, w, mx, my, delta);

        String hint = switch (mode) {
            case "BLOCK" -> "Search blocks and it faces the nearest matching block in range.";
            case "ENTITY" -> "Pick entity types or specific captures and it faces the nearest match in range.";
            default -> "Pick a specific block position to face.";
        };
        if (toggleStates.getOrDefault("smooth", false)) hint += " Smoothness 1 is faster, 10 is slower.";
        renderEditorHint(ctx, x, cy, w, hint);
    }

    private int renderLookAtEntitySelector(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = y;

        List<String> selected = stringLists.getOrDefault("entityIds", Collections.emptyList());
        cy = renderSimpleSelectedList(ctx, x, cy, w, mx, my, "look_at_entity_selected", "Entities", selected, selected::remove,
                this::formatEntityEntry, "(add entity types or specific captures)");

        PackUtilChatField search = addFields.get("entityIds");
        int searchW = w - 66;
        if (search != null) {
            search.setX(x);
            search.setY(cy + 1);
            search.setWidth(searchW);
            search.render(ctx, mx, my, delta);
        }
        int modeX = x + searchW + 2;
        String modeLabel = entitySpecificCaptureMode ? "[Spec]" : "[Type]";
        renderActionButton(ctx, modeX, cy, 30, 14, modeLabel, mx, my, () -> entitySpecificCaptureMode = !entitySpecificCaptureMode);
        renderActionButton(ctx, modeX + 34, cy, 32, 14, "CAP", mx, my, this::startLookAtEntityCapture);
        cy += 18;

        List<String> filtered = new ArrayList<>();
        String filter = search != null ? search.getText().trim().toLowerCase() : "";
        for (String id : getAllEntityIds()) {
            if (matchesListFilter(filter, id, trimMinecraftPrefix(id), PackUtilRegistryLabels.entity(id))) filtered.add(id);
        }
        renderSearchRegistryList(
            ctx,
            x,
            cy,
            w,
            mx,
            my,
            "look_at_entity_search",
            filtered,
            4,
            value -> {
                if (selected.contains(value)) selected.remove(value);
                else selected.add(value);
            },
            selected::contains,
            PackUtilRegistryLabels::entity
        );
        cy += 4 + 4 * CATALOG_ITEM_H + 10;

        PackUiText.draw(ctx, textRenderer, "Nearby Entities", font, PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 13;
        List<String> nearbyEntries = getNearbyEntityEntries();
        renderSearchRegistryList(
            ctx,
            x,
            cy,
            w,
            mx,
            my,
            "look_at_entity_nearby",
            nearbyEntries,
            3,
            value -> {
                String stored = entitySpecificCaptureMode ? value : extractEntityTypeFromNearbyEntry(value);
                if (stored.isBlank()) return;
                if (entitySpecificCaptureMode) {
                    boolean removed = selected.removeIf(existing -> existing.startsWith(entitySpecificEntryPrefix(stored)));
                    if (!removed) selected.add(stored);
                } else if (selected.contains(stored)) {
                    selected.remove(stored);
                } else {
                    selected.add(stored);
                }
            },
            value -> {
                String stored = entitySpecificCaptureMode ? value : extractEntityTypeFromNearbyEntry(value);
                return entitySpecificCaptureMode
                    ? selected.stream().anyMatch(existing -> existing.startsWith(entitySpecificEntryPrefix(stored)))
                    : selected.contains(stored);
            },
            this::formatNearbyEntityEntry
        );
        return cy + 3 * CATALOG_ITEM_H + 4;
    }

    private void renderGoToPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;
        FieldDef posField = getField("pos");
        if (posField != null) {
            cy = renderBlockPosWithoutCapture(ctx, posField, x, cy, w, mx, my, delta);
        }
        renderActionButton(ctx, x, cy, w, 14, "Capture Here", mx, my, this::fillGoToFromPlayer);
        cy += 18;
        cy = renderFieldByKey(ctx, "waitForArrival", x, cy, w, mx, my, delta);
        renderEditorHint(ctx, x, cy, w, "Wait for Arrival continues only after Baritone finishes.");
    }

    private void renderSwapSlotsPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        PackUiText.draw(ctx, textRenderer, "From", font, PackUtilColors.textSecondary(), x, cy + 2, false);
        renderActionButton(ctx, x + w - 68, cy, 68, 14, "Flip Ends", mx, my, this::swapSwapSlotEndpoints);
        cy += 16;
        cy = renderFieldByKey(ctx, "fromUseItemName", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "fromItemName", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "fromSlot", x, cy, w, mx, my, delta);

        ctx.fill(x, cy + 2, x + w, cy + 3, PackUtilColors.subPanelBorder());
        cy += 8;

        PackUiText.draw(ctx, textRenderer, "To", font, PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 16;
        cy = renderFieldByKey(ctx, "toUseItemName", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "toItemName", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "toSlot", x, cy, w, mx, my, delta);

        renderEditorHint(ctx, x, cy, w, buildSwapSlotsSummary());
    }

    private void renderClickPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "clickType", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "clickCount", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "waitForGui", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "guiName", x, cy, w, mx, my, delta);

        renderEditorHint(ctx, x, cy, w, buildClickSummary());
    }

    private void renderDisconnectPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "mode", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "delayMs", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "lagMethod", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "kickMethod", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "packetCount", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "useNextAction", x, cy, w, mx, my, delta);

        cy = renderFieldByKey(ctx, "trigger", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "tolerance", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "bufferMs", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "timeoutSec", x, cy, w, mx, my, delta);

        String mode = currentEnumValue("mode");
        String hint = switch (mode) {
            case "DISCONNECT" -> "Simple disconnect. Delay only controls how long to wait before closing the connection.";
            case "KICK" -> "Sends lag packets, then the selected kick packet. Packet Count controls how hard it pushes.";
            case "AUTO_DISCONNECT" -> "Auto-disconnects at the perfect dupe timing.";
            case "KICK_DUPE" -> toggleStates.getOrDefault("useNextAction", false)
                    ? "Kick Dupe will run the next eligible macro actions inside the lag sandwich, then kick."
                    : "Kick Dupe will try to use a bundle, then kick.";
            default -> "Unknown mode";
        };
        renderEditorHint(ctx, x, cy, w, hint, "DISCONNECT".equals(mode) ? PackUtilColors.textDim() : 0xFFFFB36B);
    }

    private void renderWaitGuiPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "waitMode", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "guiTitle", x, cy, w, mx, my, delta);

        String mode = currentEnumValue("waitMode");
        String hint = "CLOSE".equals(mode)
                ? "Waits until the current GUI closes."
                : "Waits until a GUI opens.";
        renderEditorHint(ctx, x, cy, w, hint);
    }

    private void renderSelectSlotPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "itemName", x, cy, w, mx, my, delta);

        int btnW = (w - 4) / 2;
        renderActionButton(ctx, x, cy, btnW, 14, "Held", mx, my, this::fillSelectSlotFromHeld);
        renderActionButton(ctx, x + btnW + 4, cy, btnW, 14, "Use Slot Only", mx, my, this::clearSelectSlotItemName);
        cy += 18;

        PackUiText.draw(ctx, textRenderer, "Fallback Hotbar Slot", font, PackUtilColors.textSecondary(), x, cy + 2, false);
        cy += 12;

        cy = renderSelectSlotHotbarPicker(ctx, x, cy, w, mx, my);

        renderEditorHint(ctx, x, cy, w, buildSelectSlotSummary());
    }

    private int renderSelectSlotHotbarPicker(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int selectedSlot = getSelectSlotHotbarIndex();
        int slotGap = 2;
        int cellW = Math.max(16, Math.min(24, (w - slotGap * 8) / 9));
        int totalW = cellW * 9 + slotGap * 8;
        int startX = x + Math.max(0, (w - totalW) / 2);

        for (int slot = 0; slot < 9; slot++) {
            int sx = startX + slot * (cellW + slotGap);
            boolean selected = selectedSlot == slot;
            String label = String.valueOf(slot + 1);
            final int clickedSlot = slot;
            renderOverlayButton(ctx, sx, y, cellW, 14, label,
                    selected ? PackUiOverlayButton.Variant.PRIMARY : PackUiOverlayButton.Variant.GHOST,
                    true, mx, my, () -> setSelectSlotHotbarIndex(clickedSlot));
        }

        return y + 18;
    }

    private void renderWaitCooldownPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "itemName", x, cy, w, mx, my, delta);

        int halfW = (w - 4) / 2;
        renderActionButton(ctx, x, cy, halfW, 14, currentWaitCooldownHandLabel(), mx, my, this::toggleWaitCooldownHand);
        renderActionButton(ctx, x + halfW + 4, cy, halfW, 14, "Capture Held", mx, my, this::fillWaitCooldownFromHeld);
        cy += 18;

        renderActionButton(ctx, x, cy, Math.min(130, w), 14, "Use InteractionHand Item", mx, my, this::clearWaitCooldownItemName);
        cy += 18;

        renderEditorHint(ctx, x, cy, w, buildWaitCooldownSummary());
    }

    private void renderWaitChatPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int contentBottom, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;

        cy = renderFieldByKey(ctx, "pattern", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "useRegex", x, cy, w, mx, my, delta);
        if (!toggleStates.getOrDefault("useRegex", false)) {
            cy = renderWaitChatFuzzySlider(ctx, x, cy, w, mx, my);
        } else {
            waitChatFuzzySliderDragging = false;
            clearWaitChatFuzzySliderBounds();
        }
        cy = renderFieldByKey(ctx, "timeoutMs", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "waitForGui", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "waitGuiName", x, cy, w, mx, my, delta);

        String pattern = textFields.containsKey("pattern") ? textFields.get("pattern").getText().trim() : "";
        boolean regex = toggleStates.getOrDefault("useRegex", false);
        int fuzzyPercent = getWaitChatFuzzyPercent();
        String hint = pattern.isEmpty()
                ? "Blank pattern = any chat line."
                : (regex ? "Regex mode uses Java regex rules."
                         : "Fuzzy mode ignores case/punctuation. " + fuzzyPercent + "% is looser.");
        cy = renderEditorHint(ctx, x, cy, w, hint);

        PackUtilChatField searchField = addFields.get("_wait_chat_search");
        if (searchField != null) {
            searchField.setX(x);
            searchField.setY(cy);
            searchField.setWidth(w);
            searchField.render(ctx, mx, my, delta);
            cy += 18;
        }

        List<autismclient.util.macro.MacroExecutor.RecentChatMessage> history = filterWaitChatHistory();
        PackUiText.draw(ctx, textRenderer, "Recent Messages (" + history.size() + ")",
                font, PackUtilColors.textSecondary(), x, cy + 2, false);
        PackUiText.draw(ctx, textRenderer,
                PackUiText.trimToWidth(textRenderer, formatEditorHint("Click a row to use that message."), Math.max(20, w - 122), font, -1),
                font, PackUtilColors.textDim(), x + 118, cy + 2, false);
        cy += 13;
        int availableListHeight = Math.max(24, contentBottom - cy - PAD);
        renderWaitChatHistoryList(ctx, x, cy, w, availableListHeight, mx, my, history);
    }

    private int renderWaitChatFuzzySlider(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int percent = getWaitChatFuzzyPercent();
        PackUiText.draw(ctx, textRenderer, "Match Strength", font, PackUtilColors.textSecondary(), x, y + 2, false);
        String label = percent + "%";
        PackUiText.draw(ctx, textRenderer, label, font, PackUtilColors.textDim(),
                x + w - uiWidth(font, label), y + 2, false);
        y += 13;

        int sliderW = w;
        int sliderH = 14;
        int trackH = 4;
        int trackY = y + (sliderH - trackH) / 2;
        waitChatFuzzySliderX = x;
        waitChatFuzzySliderY = y;
        waitChatFuzzySliderW = sliderW;
        waitChatFuzzySliderH = sliderH;

        ctx.fill(x, y, x + sliderW, y + sliderH, 0xFF0D0D18);
        fillBorder(ctx, x, y, sliderW, sliderH, PackUtilColors.subPanelBorder());
        ctx.fill(x + 1, trackY, x + sliderW - 1, trackY + trackH, 0xFF150C0C);

        int[] steps = {40, 50, 60, 70, 80, 90, 100};
        int knobIndex = Math.max(0, Math.min(6, (percent - 40) / 10));
        int knobCenterX = x + Math.round((sliderW - 1) * (knobIndex / 6.0f));
        ctx.fill(x + 1, trackY, knobCenterX, trackY + trackH, 0xFF345C86);

        for (int i = 0; i < steps.length; i++) {
            int step = steps[i];
            int cx = x + Math.round((sliderW - 1) * (i / 6.0f));
            int tickColor = step <= percent ? 0xFF9DCEFF : 0xFF5A4040;
            ctx.fill(cx, y + 2, cx + 1, y + sliderH - 2, tickColor);
        }

        int knobW = 16;
        int knobX = Math.max(x, Math.min(x + sliderW - knobW, knobCenterX - knobW / 2));
        boolean hovered = isOverWaitChatFuzzySlider(mx, my);
        int knobFill = waitChatFuzzySliderDragging ? 0xFF274A6D : (hovered ? 0xFF2A4462 : 0xFF223B56);
        int knobBorder = waitChatFuzzySliderDragging ? 0xFFA5D5FF : 0xFF88BBFF;
        ctx.fill(knobX, y, knobX + knobW, y + sliderH, knobFill);
        fillBorder(ctx, knobX, y, knobW, sliderH, knobBorder);
        return y + sliderH + ROW_GAP;
    }

    private int getWaitChatFuzzyPercent() {
        PackUtilChatField field = textFields.get("fuzzyPercent");
        if (field == null) return 100;
        try {
            return autismclient.util.macro.WaitForChatAction.clampFuzzyPercent(Integer.parseInt(field.getText().trim()));
        } catch (NumberFormatException ignored) {
            return 100;
        }
    }

    private void setWaitChatFuzzyPercent(int percent) {
        PackUtilChatField field = textFields.get("fuzzyPercent");
        if (field != null) field.setText(String.valueOf(autismclient.util.macro.WaitForChatAction.clampFuzzyPercent(percent)));
    }

    private void clearWaitChatFuzzySliderBounds() {
        waitChatFuzzySliderX = -1;
        waitChatFuzzySliderY = -1;
        waitChatFuzzySliderW = 0;
        waitChatFuzzySliderH = 0;
    }

    private boolean isOverWaitChatFuzzySlider(int mx, int my) {
        return waitChatFuzzySliderW > 0
                && waitChatFuzzySliderH > 0
                && mx >= waitChatFuzzySliderX
                && mx < waitChatFuzzySliderX + waitChatFuzzySliderW
                && my >= waitChatFuzzySliderY
                && my < waitChatFuzzySliderY + waitChatFuzzySliderH;
    }

    private void updateWaitChatFuzzyPercentFromMouse(int mouseX) {
        if (waitChatFuzzySliderW <= 1) return;
        float normalized = (mouseX - waitChatFuzzySliderX) / (float) (waitChatFuzzySliderW - 1);
        normalized = Math.max(0.0f, Math.min(1.0f, normalized));
        int stepIndex = Math.round(normalized * 6.0f);
        setWaitChatFuzzyPercent(40 + stepIndex * 10);
    }

    private int renderRotateSmoothnessSlider(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int smoothness = getRotateSmoothness();
        PackUiText.draw(ctx, textRenderer, "Smoothness", font, PackUtilColors.textSecondary(), x, y + 2, false);
        String label = smoothness + " / 10";
        PackUiText.draw(ctx, textRenderer, label, font, PackUtilColors.textDim(),
                x + w - uiWidth(font, label), y + 2, false);
        y += 13;

        int sliderW = w;
        int sliderH = 14;
        int trackH = 4;
        int trackY = y + (sliderH - trackH) / 2;
        rotateSmoothnessSliderX = x;
        rotateSmoothnessSliderY = y;
        rotateSmoothnessSliderW = sliderW;
        rotateSmoothnessSliderH = sliderH;

        ctx.fill(x, y, x + sliderW, y + sliderH, 0xFF0D0D18);
        fillBorder(ctx, x, y, sliderW, sliderH, PackUtilColors.subPanelBorder());
        ctx.fill(x + 1, trackY, x + sliderW - 1, trackY + trackH, 0xFF150C0C);

        int knobIndex = Math.max(0, Math.min(9, smoothness - 1));
        int knobCenterX = x + Math.round((sliderW - 1) * (knobIndex / 9.0f));
        ctx.fill(x + 1, trackY, knobCenterX, trackY + trackH, 0xFF345C86);

        for (int i = 0; i < 10; i++) {
            int cx = x + Math.round((sliderW - 1) * (i / 9.0f));
            int tickColor = i <= knobIndex ? 0xFF9DCEFF : 0xFF5A4040;
            ctx.fill(cx, y + 2, cx + 1, y + sliderH - 2, tickColor);
        }

        int knobW = 16;
        int knobX = Math.max(x, Math.min(x + sliderW - knobW, knobCenterX - knobW / 2));
        boolean hovered = isOverRotateSmoothnessSlider(mx, my);
        int knobFill = rotateSmoothnessSliderDragging ? 0xFF274A6D : (hovered ? 0xFF2A4462 : 0xFF223B56);
        int knobBorder = rotateSmoothnessSliderDragging ? 0xFFA5D5FF : 0xFF88BBFF;
        ctx.fill(knobX, y, knobX + knobW, y + sliderH, knobFill);
        fillBorder(ctx, knobX, y, knobW, sliderH, knobBorder);
        return y + sliderH + ROW_GAP;
    }

    private int getRotateSmoothness() {
        PackUtilChatField field = textFields.get("smoothness");
        if (field == null) return 9;
        try {
            return autismclient.util.macro.RotateAction.clampSmoothness(Integer.parseInt(field.getText().trim()));
        } catch (NumberFormatException ignored) {
            return 9;
        }
    }

    private void setRotateSmoothness(int smoothness) {
        PackUtilChatField field = textFields.get("smoothness");
        if (field != null) field.setText(String.valueOf(autismclient.util.macro.RotateAction.clampSmoothness(smoothness)));
    }

    private void clearRotateSmoothnessSliderBounds() {
        rotateSmoothnessSliderX = -1;
        rotateSmoothnessSliderY = -1;
        rotateSmoothnessSliderW = 0;
        rotateSmoothnessSliderH = 0;
    }

    private boolean isOverRotateSmoothnessSlider(int mx, int my) {
        return rotateSmoothnessSliderW > 0
                && rotateSmoothnessSliderH > 0
                && mx >= rotateSmoothnessSliderX
                && mx < rotateSmoothnessSliderX + rotateSmoothnessSliderW
                && my >= rotateSmoothnessSliderY
                && my < rotateSmoothnessSliderY + rotateSmoothnessSliderH;
    }

    private void updateRotateSmoothnessFromMouse(int mouseX) {
        if (rotateSmoothnessSliderW <= 1) return;
        float normalized = (mouseX - rotateSmoothnessSliderX) / (float) (rotateSmoothnessSliderW - 1);
        normalized = Math.max(0.0f, Math.min(1.0f, normalized));
        int stepIndex = Math.round(normalized * 9.0f);
        setRotateSmoothness(1 + stepIndex);
    }

    private void applyWaitChatHistoryEntry(autismclient.util.macro.MacroExecutor.RecentChatMessage entry) {
        PackUtilChatField field = textFields.get("pattern");
        if (field != null && entry != null) {
            Component patternComponent = entry.displayComponent() != null
                    ? entry.displayComponent().copy()
                    : Component.literal(formatWaitChatHistoryEntry(entry));
            setWaitChatPatternField(field, patternComponent);
            toggleStates.put("useRegex", false);
        }
    }

    private void setWaitChatPatternField(PackUtilChatField field, Component component) {
        if (field == null) return;
        Component safeComponent = component != null ? component.copy() : Component.empty();
        String visibleText = safeComponent.getString();
        workingTag.putString("pattern", visibleText);
        workingTag.putString("patternJson",
                autismclient.util.macro.MacroExecutor.serializeTextComponent(safeComponent));
        suppressWaitChatPatternSync = true;
        field.setText(visibleText);
        suppressWaitChatPatternSync = false;
    }

    private Component getWaitChatPatternComponent(String fallbackValue) {
        Component exact = autismclient.util.macro.MacroExecutor.deserializeTextComponent(
                workingTag != null ? workingTag.getStringOr("patternJson", "") : "");
        if (exact != null) return exact.copy();
        return Component.literal(fallbackValue == null ? "" : fallbackValue);
    }

    private Component rebuildWaitChatPatternComponent(Component previousComponent, String editedValue) {
        String safeValue = editedValue == null ? "" : editedValue;
        if (previousComponent == null) return Component.literal(safeValue);

        String previousValue = previousComponent.getString();
        if (previousValue.equals(safeValue)) return previousComponent.copy();
        if (safeValue.isEmpty()) return Component.empty();
        if (previousValue.isEmpty()) return Component.literal(safeValue);

        List<Style> previousStyles = flattenWaitChatStyles(previousComponent, previousValue.length());
        if (previousStyles.isEmpty()) return Component.literal(safeValue);

        int prefix = longestCommonPrefix(previousValue, safeValue);
        int suffix = longestCommonSuffix(previousValue, safeValue, prefix);
        int previousLength = previousValue.length();
        int nextLength = safeValue.length();

        MutableComponent rebuilt = Component.empty();
        StringBuilder segment = new StringBuilder();
        Style segmentStyle = null;
        for (int i = 0; i < nextLength; i++) {
            Style style = styleForEditedWaitChatIndex(previousStyles, previousLength, nextLength, prefix, suffix, i);
            if (segmentStyle == null) {
                segmentStyle = style;
            } else if (!segmentStyle.equals(style)) {
                rebuilt.append(Component.literal(segment.toString()).setStyle(segmentStyle));
                segment.setLength(0);
                segmentStyle = style;
            }
            segment.append(safeValue.charAt(i));
        }
        if (!segment.isEmpty()) {
            rebuilt.append(Component.literal(segment.toString()).setStyle(segmentStyle == null ? Style.EMPTY : segmentStyle));
        }
        return rebuilt;
    }

    private List<Style> flattenWaitChatStyles(Component text, int expectedLength) {
        if (text == null || expectedLength <= 0) return Collections.emptyList();
        List<Style> rawStyles = new ArrayList<>(expectedLength);
        text.visit((style, part) -> {
            if (part != null && !part.isEmpty()) {
                Style safeStyle = style == null ? Style.EMPTY : style;
                for (int i = 0; i < part.length(); i++) rawStyles.add(safeStyle);
            }
            return Optional.empty();
        }, Style.EMPTY);
        List<Style> styles = new ArrayList<>(rawStyles);
        if (styles.isEmpty()) {
            for (int i = 0; i < expectedLength; i++) styles.add(Style.EMPTY);
        } else if (styles.size() < expectedLength) {
            Style fill = styles.get(styles.size() - 1);
            while (styles.size() < expectedLength) styles.add(fill);
        } else if (styles.size() > expectedLength) {
            styles = new ArrayList<>(styles.subList(0, expectedLength));
        }
        return styles;
    }

    private int longestCommonPrefix(String left, String right) {
        int max = Math.min(left.length(), right.length());
        int index = 0;
        while (index < max && left.charAt(index) == right.charAt(index)) index++;
        return index;
    }

    private int longestCommonSuffix(String previous, String next, int prefixLength) {
        int previousRemaining = previous.length() - prefixLength;
        int nextRemaining = next.length() - prefixLength;
        int max = Math.min(previousRemaining, nextRemaining);
        int suffix = 0;
        while (suffix < max
                && previous.charAt(previous.length() - 1 - suffix) == next.charAt(next.length() - 1 - suffix)) {
            suffix++;
        }
        return suffix;
    }

    private Style styleForEditedWaitChatIndex(List<Style> previousStyles, int previousLength, int nextLength,
                                              int prefixLength, int suffixLength, int nextIndex) {
        if (previousStyles.isEmpty()) return Style.EMPTY;
        if (nextIndex < prefixLength) {
            return previousStyles.get(Math.min(nextIndex, previousStyles.size() - 1));
        }

        int nextSuffixStart = nextLength - suffixLength;
        int previousSuffixStart = previousLength - suffixLength;
        if (suffixLength > 0 && nextIndex >= nextSuffixStart) {
            int mappedIndex = previousSuffixStart + (nextIndex - nextSuffixStart);
            return previousStyles.get(Math.max(0, Math.min(mappedIndex, previousStyles.size() - 1)));
        }

        int anchorIndex;
        if (prefixLength > 0) {
            anchorIndex = prefixLength - 1;
        } else if (suffixLength > 0) {
            anchorIndex = previousSuffixStart;
        } else {
            anchorIndex = 0;
        }
        return previousStyles.get(Math.max(0, Math.min(anchorIndex, previousStyles.size() - 1)));
    }

    private List<autismclient.util.macro.MacroExecutor.RecentChatMessage> filterWaitChatHistory() {
        List<autismclient.util.macro.MacroExecutor.RecentChatMessage> history =
                autismclient.util.macro.MacroExecutor.getRecentChatMessages();
        PackUtilChatField searchField = addFields.get("_wait_chat_search");
        String query = searchField != null ? searchField.getText().trim() : "";
        List<autismclient.util.macro.MacroExecutor.RecentChatMessage> filtered = new ArrayList<>();
        String normalizedQuery = query.isEmpty() ? "" : normalizeWaitChatSearch(query);
        for (autismclient.util.macro.MacroExecutor.RecentChatMessage entry : history) {
            if (normalizedQuery.isEmpty()) {
                filtered.add(entry);
                continue;
            }
            String haystack = normalizeWaitChatSearch(formatWaitChatHistoryEntry(entry));
            if (haystack.contains(normalizedQuery)) filtered.add(entry);
        }
        return filtered;
    }

    private String normalizeWaitChatSearch(String text) {
        return autismclient.util.macro.MacroExecutor.normalizeChatText(text);
    }

    private String formatWaitChatHistoryEntry(autismclient.util.macro.MacroExecutor.RecentChatMessage entry) {
        if (entry == null) return "";
        if (entry.displayComponent() != null) {
            String rendered = entry.displayComponent().getString();
            if (rendered != null && !rendered.isBlank()) return rendered;
        }
        String sender = entry.sender() == null ? "" : entry.sender().trim();
        String message = entry.message() == null ? "" : entry.message().trim();
        if (!sender.isEmpty() && !message.isEmpty()) return sender + ": " + message;
        if (!message.isEmpty()) return (entry.source() == autismclient.util.macro.MacroExecutor.ChatSource.SERVER ? "[Server] " : "[Player] ") + message;
        return entry.displayText() == null ? "" : entry.displayText();
    }

    private void renderWaitChatHistoryList(GuiGraphicsExtractor ctx, int x, int y, int w, int listH, int mx, int my,
                                           List<autismclient.util.macro.MacroExecutor.RecentChatMessage> values) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int desiredListH = values.isEmpty()
                ? 24
                : Math.min(WAIT_CHAT_VISIBLE_ROWS, values.size()) * WAIT_CHAT_ROW_H;
        listH = Math.max(24, Math.min(listH, desiredListH));
        int itemW = w - SCROLLBAR_W - 1;

        PackUiScrollViewport chatViewport = getOrCreateViewport(selectedScrollViewports, "wait_chat_recent",
            x, y, w, listH, WAIT_CHAT_ROW_H, SCROLLBAR_W);
        chatViewport.setContentHeight(values.size() * WAIT_CHAT_ROW_H);
        selectedListBounds.put("wait_chat_recent", new int[]{y, listH});

        chatViewport.renderScrollbar(ctx, mx, my);

        if (values.isEmpty()) {
            ctx.fill(x, y, x + itemW, y + 24, PackUtilColors.rowNormal());
            PackUiText.draw(ctx, textRenderer, "No recent messages matched your search.",
                    font, PackUtilColors.textDim(), x + 3, y + 3, false);
            PackUiText.draw(ctx, textRenderer, "Send or receive chat first, then pick it here.",
                    font, PackUtilColors.textDim(), x + 3, y + 13, false);
            return;
        }

        chatViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
        int first = chatViewport.getFirstVisibleRow();
        for (int i = first; i < values.size() && i <= chatViewport.getLastVisibleRow(); i++) {
            int iy = chatViewport.getRowScreenY(i);
            if (iy == Integer.MIN_VALUE) continue;
            int itemH = WAIT_CHAT_ROW_H;

            if (iy + itemH > y + listH) {
                itemH = Math.max(0, y + listH - iy);
            }
            if (itemH <= 0) continue;
            var value = values.get(i);
            boolean hovered = mx >= x && mx < x + itemW && my >= iy && my < iy + itemH;
            ctx.fill(x, iy, x + itemW, iy + itemH, hovered ? PackUtilColors.rowHover() : PackUtilColors.rowNormal());
            int border = value.source() == autismclient.util.macro.MacroExecutor.ChatSource.SERVER
                    ? (hovered ? 0xFFFFB15A : 0xFFCC7A22)
                    : (hovered ? 0xFF7CCEFF : 0xFF4E95C8);
            fillBorder(ctx, x, iy, itemW, itemH, border);

            List<FormattedCharSequence> wrapped = wrapWaitChatDisplayLines(value, Math.max(20, itemW - 6), 3);
            int textY = iy + 3;
            if (wrapped.isEmpty()) {
                PackUiText.draw(ctx, textRenderer, "(empty)", font, PackUtilColors.textDim(), x + 3, textY, false);
            } else {
                for (FormattedCharSequence line : wrapped) {
                    if (textY + 8 > iy + itemH) break;
                    ctx.text(textRenderer, line, x + 3, textY, 0xFFFFFFFF, false);
                    textY += 9;
                }
            }
            final var selected = value;
            hitRegions.add(new HitRegion(x, iy, itemW, itemH, () -> applyWaitChatHistoryEntry(selected)));
        }
        chatViewport.endRender(ctx);
    }

    private int waitChatHistoryListHeight() {
        int count = filterWaitChatHistory().size();
        if (count <= 0) return 24;
        return Math.min(WAIT_CHAT_VISIBLE_ROWS, count) * WAIT_CHAT_ROW_H;
    }

    private List<FormattedCharSequence> wrapWaitChatDisplayLines(autismclient.util.macro.MacroExecutor.RecentChatMessage entry, int maxWidth, int maxLines) {
        Component display = entry != null && entry.displayComponent() != null
                ? entry.displayComponent()
                : Component.literal(formatWaitChatHistoryEntry(entry));
        List<FormattedCharSequence> wrapped = textRenderer.split(display, Math.max(20, maxWidth));
        if (wrapped.size() <= maxLines) return wrapped;
        return new ArrayList<>(wrapped.subList(0, Math.max(0, maxLines)));
    }

    private List<String> wrapWaitChatLines(String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank() || maxLines <= 0) return lines;
        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();
        for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {
            String word = words[wordIndex];
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (uiWidth(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                if (lines.size() == maxLines - 1) {
                    lines.add(trimWaitChatLine(current + " " + joinWaitChatWords(words, wordIndex), maxWidth));
                    return lines;
                }
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                if (lines.size() == maxLines - 1) {
                    lines.add(trimWaitChatLine(joinWaitChatWords(words, wordIndex), maxWidth));
                    return lines;
                }
                lines.add(trimWaitChatLine(word, maxWidth));
            }
        }
        if (!current.isEmpty() && lines.size() < maxLines) lines.add(current.toString());
        return lines;
    }

    private String joinWaitChatWords(String[] words, int startIndex) {
        StringBuilder out = new StringBuilder();
        for (int i = startIndex; i < words.length; i++) {
            if (out.length() > 0) out.append(' ');
            out.append(words[i]);
        }
        return out.toString();
    }

    private String trimWaitChatLine(String text, int maxWidth) {
        String trimmed = text == null ? "" : text;
        while (!trimmed.isEmpty() && uiWidth(trimmed + "...") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.length() < (text == null ? 0 : text.length()) ? trimmed + "..." : trimmed;
    }

    private void renderDelayPacketsPanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        int cy = bodyTop + PAD;
        int halfW = (w - 4) / 2;
        List<String> c2sTargets = getDelayPacketTargets(true);
        List<String> s2cTargets = getDelayPacketTargets(false);

        cy = renderFieldByKey(ctx, "mode", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "flushOnDisable", x, cy, w, mx, my, delta);

        String mode = currentEnumValue("mode");
        if ("ENABLE".equals(mode)) {
            renderActionButton(ctx, x, cy, halfW, 14, "Choose C2S (" + c2sTargets.size() + ")", mx, my, () -> openDelayPacketSelector(true));
            renderActionButton(ctx, x + halfW + 4, cy, halfW, 14, "Choose S2C (" + s2cTargets.size() + ")", mx, my, () -> openDelayPacketSelector(false));
            cy += 18;

            renderActionButton(ctx, x, cy, halfW, 14, "Load Queue", mx, my, this::loadDelayPacketTargetsFromQueue);
            renderActionButton(ctx, x + halfW + 4, cy, halfW, 14, "Clear All", mx, my, this::clearAllDelayPacketTargets);
            cy += 18;

            renderActionButton(ctx, x, cy, halfW, 14, "Default Preset", mx, my, this::applyDefaultDelayPacketPreset);
            renderActionButton(ctx, x + halfW + 4, cy, halfW, 14, "Module Preset", mx, my, this::applyModuleDelayPacketPreset);
        }
    }

    private List<String> getDelayPacketTargets(boolean c2s) {
        return stringLists.computeIfAbsent(c2s ? "c2sPackets" : "s2cPackets", ignored -> new ArrayList<>());
    }

    private void openDelayPacketSelector(boolean c2s) {
        if (c2s) {
            packetSelectorOverlay.openToggleC2S(
                (packetClass, selected) -> setDelayPacketSelected(true, packetClass, selected),
                getSelectedDelayPacketClasses(true)
            );
        } else {
            packetSelectorOverlay.openToggleS2C(
                (packetClass, selected) -> setDelayPacketSelected(false, packetClass, selected),
                getSelectedDelayPacketClasses(false)
            );
        }
    }

    private List<Class<? extends net.minecraft.network.protocol.Packet<?>>> getSelectedDelayPacketClasses(boolean c2s) {
        List<Class<? extends net.minecraft.network.protocol.Packet<?>>> selected = new ArrayList<>();
        for (String target : getDelayPacketTargets(c2s)) {
            Class<? extends net.minecraft.network.protocol.Packet<?>> packetClass = resolveDelayPacketClass(target, c2s);
            if (packetClass != null && !selected.contains(packetClass)) {
                selected.add(packetClass);
            }
        }
        return selected;
    }

    private Class<? extends net.minecraft.network.protocol.Packet<?>> resolveDelayPacketClass(String target, boolean c2s) {
        if (target == null || target.isBlank()) return null;
        List<Class<? extends net.minecraft.network.protocol.Packet<?>>> pool = c2s
            ? new ArrayList<>(PackUtilPacketRegistry.getC2SPackets())
            : new ArrayList<>(PackUtilPacketRegistry.getS2CPackets());
        for (Class<? extends net.minecraft.network.protocol.Packet<?>> candidate : pool) {
            String registryName = PackUtilPacketRegistry.getName(candidate);
            if (packetNameMatches(target, registryName)) return candidate;
            if (packetNameMatches(target, PackUtilPacketNamer.getFriendlyName(candidate))) return candidate;
            if (packetNameMatches(target, candidate.getSimpleName())) return candidate;
        }
        return null;
    }

    private void setDelayPacketSelected(boolean c2s, Class<? extends net.minecraft.network.protocol.Packet<?>> packetClass, boolean selected) {
        if (packetClass == null) return;
        List<String> targets = getDelayPacketTargets(c2s);
        if (selected) {
            if (!containsDelayPacketTarget(targets, packetClass, c2s)) {
                targets.add(PackUtilPacketNamer.getFriendlyName(packetClass));
            }
            return;
        }

        targets.removeIf(existing -> packetClass.equals(resolveDelayPacketClass(existing, c2s)));
    }

    private boolean containsDelayPacketTarget(List<String> targets, Class<? extends net.minecraft.network.protocol.Packet<?>> packetClass, boolean c2s) {
        if (targets == null || packetClass == null) return false;
        for (String target : targets) {
            if (packetClass.equals(resolveDelayPacketClass(target, c2s))) {
                return true;
            }
        }
        return false;
    }

    private void loadDelayPacketTargetsFromQueue() {
        List<PackUtilSharedState.QueuedPacket> queue = PackUtilSharedState.get().getDelayedPackets();
        if (queue == null || queue.isEmpty()) {
            PackUtilClientMessaging.sendPrefixed("Queue is empty");
            return;
        }

        int before = getDelayPacketTargets(true).size() + getDelayPacketTargets(false).size();
        for (PackUtilSharedState.QueuedPacket queuedPacket : queue) {
            if (queuedPacket == null || queuedPacket.packet == null) continue;
            @SuppressWarnings("unchecked")
            Class<? extends net.minecraft.network.protocol.Packet<?>> packetClass =
                (Class<? extends net.minecraft.network.protocol.Packet<?>>) queuedPacket.packet.getClass();
            if (PackUtilPacketRegistry.getC2SPackets().contains(packetClass)) {
                setDelayPacketSelected(true, packetClass, true);
            } else if (PackUtilPacketRegistry.getS2CPackets().contains(packetClass)) {
                setDelayPacketSelected(false, packetClass, true);
            }
        }

        int after = getDelayPacketTargets(true).size() + getDelayPacketTargets(false).size();
        int added = Math.max(0, after - before);
        PackUtilClientMessaging.sendPrefixed(added == 0
            ? "Queue did not add any new packet filters"
            : "Added " + added + " packet filter" + (added == 1 ? "" : "s") + " from queue");
    }

    private void clearAllDelayPacketTargets() {
        getDelayPacketTargets(true).clear();
        getDelayPacketTargets(false).clear();
    }

    private void applyDefaultDelayPacketPreset() {
        autismclient.util.macro.DelayPacketsAction tmp = new autismclient.util.macro.DelayPacketsAction();
        tmp.applyDefaultPreset();
        stringLists.put("c2sPackets", new ArrayList<>(tmp.c2sPacketNames));
        stringLists.put("s2cPackets", new ArrayList<>(tmp.s2cPacketNames));
    }

    private void applyModuleDelayPacketPreset() {
        autismclient.util.macro.DelayPacketsAction tmp = new autismclient.util.macro.DelayPacketsAction();
        tmp.applyModulePreset();
        stringLists.put("c2sPackets", new ArrayList<>(tmp.c2sPacketNames));
        stringLists.put("s2cPackets", new ArrayList<>(tmp.s2cPacketNames));
    }

    private String buildDelayPacketDirectionSummary(boolean c2s) {
        String direction = c2s ? "C2S" : "S2C";
        List<String> targets = getDelayPacketTargets(c2s);
        if (targets.isEmpty()) {
            return direction + ": none";
        }

        List<String> labels = new ArrayList<>();
        for (int i = 0; i < targets.size() && i < 3; i++) {
            String target = targets.get(i);
            Class<? extends net.minecraft.network.protocol.Packet<?>> packetClass = resolveDelayPacketClass(target, c2s);
            labels.add(packetClass != null ? PackUtilPacketNamer.getFriendlyName(packetClass) : target);
        }

        String summary = direction + ": " + targets.size();
        if (!labels.isEmpty()) {
            summary += " - " + String.join(", ", labels);
        }
        if (targets.size() > labels.size()) {
            summary += ", +" + (targets.size() - labels.size());
        }
        return summary;
    }

    private int renderDelayPacketPicker(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my, float delta, boolean c2s) {
        String key = c2s ? "c2sPackets" : "s2cPackets";
        String searchKey = c2s ? "_delay_packets_c2s_search" : "_delay_packets_s2c_search";
        String title = c2s ? "C2S Packets" : "S2C Packets";
        List<String> selected = stringLists.getOrDefault(key, Collections.emptyList());
        y = renderSimpleSelectedList(ctx, x, y, w, mx, my, key + "_selected", title, selected,
                selected::remove, value -> value, "(none selected)");

        PackUtilChatField search = addFields.get(searchKey);
        if (search != null) {
            search.setX(x);
            search.setY(y + 1);
            search.setWidth(w);
            search.render(ctx, mx, my, delta);
        }
        y += 18;

        String filter = search != null ? search.getText().trim().toLowerCase(Locale.ROOT) : "";
        List<String> options = new ArrayList<>();
        for (String packetName : getPacketNames(c2s)) {
            if (filter.isEmpty() || packetName.toLowerCase(Locale.ROOT).contains(filter)) {
                options.add(packetName);
            }
        }
        renderSearchRegistryList(
            ctx,
            x,
            y,
            w,
            mx,
            my,
            key + "_search",
            options,
            5,
            value -> {
                if (selected.contains(value)) selected.remove(value);
                else selected.add(value);
            },
            selected::contains,
            value -> value
        );
        return y + 5 * CATALOG_ITEM_H + 4;
    }

    private void renderMinePanel(GuiGraphicsExtractor ctx, int x, int bodyTop, int w, int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int cy = bodyTop + PAD;
        cy = renderFieldByKey(ctx, "targetBlocks", x, cy, w, mx, my, delta);
        cy += 4;
        cy = renderFieldByKey(ctx, "stopInventoryFull", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "stopSlotsUsed", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "slotsUsedThreshold", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "stopMinedCount", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "minedCountTarget", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "stopAfterTime", x, cy, w, mx, my, delta);
        cy = renderFieldByKey(ctx, "timeoutSeconds", x, cy, w, mx, my, delta);
        renderEditorHint(ctx, x, cy, w, "Pick target blocks, then choose the stop rule you want.");
    }

    private void fillUseItemFromHeld() {
        if (MC.player == null) return;
        PackUtilChatField field = textFields.get("itemName");
        if (field == null) return;
        if (MC.player.getMainHandItem().isEmpty()) {
            field.setText("");
            editorItemFields.remove("itemName");
            return;
        }
        ItemTarget target = ItemTarget.capture(MC.player.getMainHandItem(), MC.player.getInventory().getSelectedSlot());
        editorItemFields.put("itemName", target);
        field.setText(target.editorText());
    }

    private void fillSelectSlotFromHeld() {
        if (MC.player == null) return;
        PackUtilChatField field = textFields.get("itemName");
        if (field == null) return;
        if (MC.player.getMainHandItem().isEmpty()) {
            field.setText("");
            editorItemFields.remove("itemName");
            return;
        }
        ItemTarget target = ItemTarget.capture(MC.player.getMainHandItem(), MC.player.getInventory().getSelectedSlot());
        editorItemFields.put("itemName", target);
        field.setText(target.editorText());
    }

    private void clearSelectSlotItemName() {
        PackUtilChatField field = textFields.get("itemName");
        if (field != null) field.setText("");
        editorItemFields.remove("itemName");
    }

    private int getSelectSlotHotbarIndex() {
        PackUtilChatField field = textFields.get("slot");
        if (field == null) return 0;
        try {
            return Math.max(0, Math.min(8, Integer.parseInt(field.getText().trim())));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void setSelectSlotHotbarIndex(int slot) {
        PackUtilChatField field = textFields.get("slot");
        if (field != null) field.setText(String.valueOf(Math.max(0, Math.min(8, slot))));
    }

    private String buildSelectSlotSummary() {
        int fallbackSlot = getSelectSlotHotbarIndex() + 1;
        PackUtilChatField itemField = textFields.get("itemName");
        String itemName = itemField != null ? itemField.getText().trim() : "";
        return itemName.isEmpty()
                ? "Uses hotbar slot " + fallbackSlot + "."
                : "Uses the named item, else slot " + fallbackSlot + ".";
    }

    private String currentUseItemTargetLabel() {
        PackUtilChatField field = textFields.get("itemName");
        String item = field != null ? field.getText().trim() : "";
        return item.isEmpty() ? "the held item" : "the named item";
    }

    private void toggleWaitCooldownHand() {
        toggleStates.put("checkMainHand", !toggleStates.getOrDefault("checkMainHand", true));
    }

    private String currentWaitCooldownHandLabel() {
        return toggleStates.getOrDefault("checkMainHand", true) ? "Hand: Main" : "Hand: Off";
    }

    private void fillWaitCooldownFromHeld() {
        if (MC.player == null) return;
        PackUtilChatField field = textFields.get("itemName");
        if (field == null) return;
        if (MC.player.getMainHandItem().isEmpty()) {
            field.setText("");
            editorItemFields.remove("itemName");
            return;
        }
        ItemTarget target = ItemTarget.capture(MC.player.getMainHandItem(), MC.player.getInventory().getSelectedSlot());
        editorItemFields.put("itemName", target);
        field.setText(target.editorText());
    }

    private void clearWaitCooldownItemName() {
        PackUtilChatField field = textFields.get("itemName");
        if (field != null) field.setText("");
        editorItemFields.remove("itemName");
    }

    private void fillRotateFromCurrentView() {
        if (MC.player == null) return;
        PackUtilChatField yawField = textFields.get("yaw");
        PackUtilChatField pitchField = textFields.get("pitch");
        if (yawField != null) yawField.setText(fmtDouble(MC.player.getYRot()));
        if (pitchField != null) pitchField.setText(fmtDouble(MC.player.getXRot()));
    }

    private void fillGoToFromPlayer() {
        if (MC.player == null) return;
        PackUtilChatField xField = textFields.get("pos_0");
        PackUtilChatField yField = textFields.get("pos_1");
        PackUtilChatField zField = textFields.get("pos_2");
        if (xField != null) xField.setText(fmtDouble(MC.player.getX()));
        if (yField != null) yField.setText(fmtDouble(MC.player.getY()));
        if (zField != null) zField.setText(fmtDouble(MC.player.getZ()));
    }

    private String buildWaitCooldownSummary() {
        PackUtilChatField itemField = textFields.get("itemName");
        String itemName = itemField != null ? itemField.getText().trim() : "";
        String handLabel = toggleStates.getOrDefault("checkMainHand", true) ? "main hand" : "off hand";
        if (itemName.isEmpty()) {
            return "Waits for the " + handLabel + " cooldown.";
        }
        return "Waits for the named item cooldown.";
    }

    private void swapSwapSlotEndpoints() {
        boolean fromUseName = toggleStates.getOrDefault("fromUseItemName", false);
        boolean toUseName = toggleStates.getOrDefault("toUseItemName", false);
        String fromItem = textFields.containsKey("fromItemName") ? textFields.get("fromItemName").getText() : "";
        String toItem = textFields.containsKey("toItemName") ? textFields.get("toItemName").getText() : "";
        String fromSlot = textFields.containsKey("fromSlot") ? textFields.get("fromSlot").getText() : "";
        String toSlot = textFields.containsKey("toSlot") ? textFields.get("toSlot").getText() : "";

        toggleStates.put("fromUseItemName", toUseName);
        toggleStates.put("toUseItemName", fromUseName);
        PackUtilChatField fromItemField = textFields.get("fromItemName");
        PackUtilChatField toItemField = textFields.get("toItemName");
        PackUtilChatField fromSlotField = textFields.get("fromSlot");
        PackUtilChatField toSlotField = textFields.get("toSlot");
        if (fromItemField != null) fromItemField.setText(toItem);
        if (toItemField != null) toItemField.setText(fromItem);
        if (fromSlotField != null) fromSlotField.setText(toSlot);
        if (toSlotField != null) toSlotField.setText(fromSlot);
    }

    private String buildSwapSlotsSummary() {
        return "Swaps the two targets. Name mode uses the first visible match.";
    }

    private String buildClickSummary() {
        String clickType = currentEnumValue("clickType");
        return switch (clickType) {
            case "LEFT" -> "Left click acts on your target.";
            default -> "Right click uses your target.";
        };
    }

    private String buildWaitSlotSummary(String waitMode) {
        return switch (waitMode) {
            case "IS_EMPTY"       -> "Waits until the slot (or any slot) is empty.";
            case "COUNT_AT_LEAST" -> "Waits until the slot/item hits the target count.";
            case "COUNT_BELOW"    -> "Waits until the slot/item drops below the count.";
            case "ANY_CHANGE"     -> "Waits for the first change in the specified slot.";
            default               -> "Waits until the slot or item is present. -1 scans all slots.";
        };
    }

    private int renderFieldByKey(GuiGraphicsExtractor ctx, String key, int x, int y, int w, int mx, int my, float delta) {
        FieldDef field = getField(key);
        if (field == null || !isFieldVisible(field)) return y;
        renderRow(ctx, field, x, y, w, mx, my, delta);
        return y + rowH(field) + ROW_GAP;
    }

    private FieldDef getField(String key) {
        if (schema == null) return null;
        for (FieldDef field : schema.fields()) {
            if (field.key().equals(key)) return field;
        }
        return null;
    }

    private <T> int renderSimpleSelectedList(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my,
                                             String listKey, String label, List<T> values,
                                             java.util.function.Consumer<T> removeAction,
                                             java.util.function.Function<T, String> formatter,
                                             String emptyLabel) {
        return renderSimpleSelectedList(ctx, x, y, w, mx, my, listKey, label, values, removeAction, formatter, ignored -> null, values::clear, emptyLabel, false);
    }

    private <T> int renderSimpleSelectedList(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my,
                                             String listKey, String label, List<T> values,
                                             java.util.function.Consumer<T> removeAction,
                                             java.util.function.Function<T, String> formatter,
                                             java.util.function.Function<T, Component> richFormatter,
                                             String emptyLabel) {
        return renderSimpleSelectedList(ctx, x, y, w, mx, my, listKey, label, values, removeAction, formatter, richFormatter, values::clear, emptyLabel, false);
    }

    private <T> int renderSimpleSelectedList(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my,
                                             String listKey, String label, List<T> values,
                                             java.util.function.Consumer<T> removeAction,
                                             java.util.function.Function<T, String> formatter,
                                             java.util.function.Function<T, Component> richFormatter,
                                             Runnable clearAction,
                                             String emptyLabel) {
        return renderSimpleSelectedList(ctx, x, y, w, mx, my, listKey, label, values, removeAction, formatter, richFormatter, clearAction, emptyLabel, false);
    }

    private <T> int renderSimpleSelectedList(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my,
                                             String listKey, String label, List<T> values,
                                             java.util.function.Consumer<T> removeAction,
                                             java.util.function.Function<T, String> formatter,
                                             java.util.function.Function<T, Component> richFormatter,
                                             Runnable clearAction,
                                             String emptyLabel,
                                             boolean editable) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        int headerBtnW = 44;
        int editIdx = editable ? stringListEditIndex.getOrDefault(listKey, -1) : -1;
        if (editIdx >= values.size()) { editIdx = -1; stringListEditIndex.put(listKey, -1); }
        PackUiText.draw(ctx, textRenderer, label + " (" + values.size() + ")", font, PackUtilColors.textSecondary(), x, y + 2, false);
        boolean canClear = !values.isEmpty();
        int clearX = x + w - headerBtnW;
        renderOverlayButton(ctx, clearX, y, headerBtnW, 14, "Clear", PackUiOverlayButton.Variant.DANGER, canClear, mx, my, () -> {
            clearAction.run();
            stringListEditIndex.put(listKey, -1);
            PackUiScrollViewport vp = selectedScrollViewports.get(listKey);
            if (vp != null) vp.jumpTo(0);
        });
        y += 13;

        int listH = SEL_ITEM_H * 4;
        int itemW = w - SCROLLBAR_W - 1;

        PackUiScrollViewport simpleViewport = getOrCreateViewport(selectedScrollViewports, listKey,
            x, y, w, listH, SEL_ITEM_H, SCROLLBAR_W);
        simpleViewport.setContentHeight(values.size() * SEL_ITEM_H);
        selectedListBounds.put(listKey, new int[]{y, listH});

        simpleViewport.renderScrollbar(ctx, mx, my);

        if (values.isEmpty()) {
            ctx.fill(x, y, x + itemW, y + 12, PackUtilColors.rowNormal());
            PackUiListRenderer.drawEmptyState(ctx, textRenderer, emptyLabel, x, y, itemW);
            return y + listH + 4;
        }

        simpleViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
        int first = simpleViewport.getFirstVisibleRow();
        for (int i = first; i < values.size() && i <= simpleViewport.getLastVisibleRow(); i++) {
            int iy = simpleViewport.getRowScreenY(i);
            if (iy == Integer.MIN_VALUE) continue;
            T value = values.get(i);
            int removeW = 13;
            int rowW = itemW - removeW - 2;
            boolean selected = editable && i == editIdx;
            boolean hovered = mx >= x && mx < x + rowW && my >= iy && my < iy + 13;
            Component richLabel = richFormatter == null ? null : richFormatter.apply(value);
            if (richLabel != null) {
                PackUiListRenderer.drawRow(
                        ctx,
                        textRenderer,
                        richLabel,
                        x,
                        iy,
                        rowW,
                        13,
                        hovered,
                        selected,
                        PackUiListRenderer.RowTone.NORMAL,
                        true
                );
            } else {
                PackUiListRenderer.drawRow(
                        ctx,
                        textRenderer,
                        formatter.apply(value),
                        x,
                        iy,
                        rowW,
                        13,
                        hovered,
                        selected,
                        PackUiListRenderer.RowTone.NORMAL
                );
            }

            if (editable) {
                final int fi = i;
                final String rawText = value instanceof String rawValue ? rawValue : formatter.apply(value);
                hitRegions.add(new HitRegion(x, iy, rowW, 13, () -> {
                    int curIdx = stringListEditIndex.getOrDefault(listKey, -1);
                    if (curIdx == fi) {
                        stringListEditIndex.put(listKey, -1);
                        stringListEditPendingText.put(listKey, "");
                    } else {
                        stringListEditIndex.put(listKey, fi);
                        stringListEditPendingText.put(listKey, rawText != null ? rawText : "");
                    }
                }));
            }

            {
                int removeX = x + rowW + 2;
                final int fi2 = i;
                renderIconDeleteButton(ctx, removeX, iy, removeW, mx, my, () -> {
                    removeAction.accept(values.get(fi2));
                    if (editable) {
                        int curSel = stringListEditIndex.getOrDefault(listKey, -1);
                        if (curSel == fi2) { stringListEditIndex.put(listKey, -1); stringListEditPendingText.put(listKey, ""); }
                        else if (curSel > fi2) stringListEditIndex.put(listKey, curSel - 1);
                    }
                    PackUiScrollViewport vp = selectedScrollViewports.get(listKey);
                    if (vp != null) vp.scrollBy(-1);
                });
            }
        }
        simpleViewport.endRender(ctx);

        return y + listH + 4;
    }

    private <T> void renderSearchRegistryList(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my, String listKey,
                                              List<T> values, int visibleRows,
                                              java.util.function.Consumer<T> clickAction,
                                              java.util.function.Function<T, String> formatter) {
        renderSearchRegistryList(ctx, x, y, w, mx, my, listKey, values, visibleRows, clickAction, ignored -> false, formatter);
    }

    private <T> void renderSearchRegistryList(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my, String listKey,
                                              List<T> values, int visibleRows,
                                              java.util.function.Consumer<T> clickAction,
                                              java.util.function.Predicate<T> selectedPredicate,
                                              java.util.function.Function<T, String> formatter) {
        int rowH = CATALOG_ITEM_H;
        int listH = visibleRows * rowH;
        int itemW = w - SCROLLBAR_W - 1;

        PackUiScrollViewport regViewport = getOrCreateViewport(catalogScrollViewports, listKey,
            x, y, w, listH, rowH, SCROLLBAR_W);
        regViewport.setContentHeight(values.size() * rowH);
        catalogListBounds.put(listKey, new int[]{y, listH});

        regViewport.renderScrollbar(ctx, mx, my);

        if (values.isEmpty()) {
            ctx.fill(x, y, x + itemW, y + 12, PackUtilColors.rowNormal());
            PackUiListRenderer.drawEmptyState(ctx, textRenderer, "No matches", x, y, itemW);
            return;
        }

        regViewport.beginRender(ctx, theme.borderSoft(), 0x36000000);
        int first = regViewport.getFirstVisibleRow();
        for (int i = first; i < values.size() && i <= regViewport.getLastVisibleRow(); i++) {
            int iy = regViewport.getRowScreenY(i);
            if (iy == Integer.MIN_VALUE) continue;
            T value = values.get(i);
            String display = formatter.apply(value);
            boolean selected = selectedPredicate != null && selectedPredicate.test(value);
            boolean hovered = mx >= x && mx < x + itemW && my >= iy && my < iy + rowH;
            PackUiListRenderer.drawRow(
                ctx,
                textRenderer,
                display,
                x,
                iy,
                itemW,
                rowH,
                hovered,
                selected,
                selected ? PackUiListRenderer.RowTone.READY : PackUiListRenderer.RowTone.NORMAL
            );
            final T clickedValue = value;
            hitRegions.add(new HitRegion(x, iy, itemW, rowH, () -> clickAction.accept(clickedValue)));
        }
        regViewport.endRender(ctx);
    }

    private void startWaitEntityCapture() {
        if (!(targetAction instanceof autismclient.util.macro.WaitForEntityAction)) return;
        Screen previousScreen = MC.screen;
        enterEditorOnlyCaptureMode();
        PackUtilSharedState state = PackUtilSharedState.get();
        state.setCaptureCancelCallback(() -> {
            if (previousScreen != null) MC.setScreen(previousScreen);
            state.setEntityCaptureCallback(null);
            exitCaptureMode(false, false);
        });
        MC.execute(() -> { if (MC.screen != null) MC.setScreen(null); });
        state.setEntityCaptureSpecific(entitySpecificCaptureMode);
        state.setEntityCaptureCallback(payload -> MC.execute(() -> {
            List<String> selected = stringLists.get("entityIds");
            if (selected != null && payload != null && !payload.isBlank()) {
                if (entitySpecificCaptureMode) {
                    selected.removeIf(existing -> existing.startsWith(entitySpecificEntryPrefix(payload)));
                }
                if (!selected.contains(payload)) selected.add(payload);
            }
            if (previousScreen != null) MC.setScreen(previousScreen);
            exitCaptureMode(false, false);
            PackUtilOverlayManager.get().bringToFront(this);
        }));
    }

    private static String trimMinecraftPrefix(String value) {
        return PackUtilRegistryLabels.stripNamespace(value);
    }

    private String formatEntityEntry(String entry) {
        if (entry == null || entry.isBlank()) return "(unknown)";
        if (entry.startsWith("~")) {
            String[] parts = entry.split("~", 4);
            String rawName = parts.length >= 4 ? parts[3] : "";
            String type = parts.length >= 3 ? PackUtilRegistryLabels.entity(parts[2]) : "?";
            String uuid = parts.length >= 2 ? parts[1] : "";
            String suffix = uuid.length() >= 4 ? uuid.substring(uuid.length() - 4) : uuid;
            String name = rawName == null ? "" : rawName.trim();
            if (name.isEmpty() || name.equalsIgnoreCase(type) || name.equalsIgnoreCase(trimMinecraftPrefix(type))) {
                return type + " #" + suffix;
            }
            return name + " (" + type + " #" + suffix + ")";
        }
        return PackUtilRegistryLabels.entity(entry);
    }

    private List<String> getNearbyEntityEntries() {
        boolean supportedAction = targetAction instanceof autismclient.util.macro.WaitForEntityAction
                || targetAction instanceof autismclient.util.macro.LookAtBlockAction
                || targetAction instanceof autismclient.util.macro.OpenContainerAction;
        if (!supportedAction || MC.player == null || MC.level == null) {
            return Collections.emptyList();
        }
        boolean openContainerAction = targetAction instanceof autismclient.util.macro.OpenContainerAction;
        List<String> result = new ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : MC.level.entitiesForRendering()) {
            if (entity == MC.player) continue;
            if (MC.player.distanceTo(entity) > WAIT_ENTITY_NEARBY_LIST_RADIUS) continue;
            if (openContainerAction && !canOpenContainerEntity(entity)) continue;
            String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            String uuid = entity.getStringUUID();
            String displayName = entity.getDisplayName().getString().replaceAll("Â§.", "").trim();
            result.add("~" + uuid + "~" + typeId + "~" + displayName);
        }
        result.sort(java.util.Comparator.comparingDouble(this::distanceForNearbyEntityEntry));
        return result;
    }

    private boolean canOpenContainerEntity(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        return entity instanceof net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer
                || entity instanceof net.minecraft.world.entity.vehicle.boat.ChestBoat
                || entity instanceof net.minecraft.world.entity.animal.equine.AbstractHorse;
    }

    private double distanceForNearbyEntityEntry(String entry) {
        if (MC.player == null || entry == null || !entry.startsWith("~")) return Double.MAX_VALUE;
        String[] parts = entry.split("~", 4);
        if (parts.length < 2 || MC.level == null) return Double.MAX_VALUE;
        try {
            java.util.UUID uuid = java.util.UUID.fromString(parts[1]);
            net.minecraft.world.entity.Entity entity = null;
            for (net.minecraft.world.entity.Entity candidate : MC.level.entitiesForRendering()) {
                if (uuid.equals(candidate.getUUID())) {
                    entity = candidate;
                    break;
                }
            }
            return entity != null ? MC.player.distanceTo(entity) : Double.MAX_VALUE;
        } catch (Exception ignored) {
            return Double.MAX_VALUE;
        }
    }

    private String extractEntityTypeFromNearbyEntry(String entry) {
        if (entry == null || !entry.startsWith("~")) return "";
        String[] parts = entry.split("~", 4);
        return parts.length >= 3 ? parts[2] : "";
    }

    private String entitySpecificEntryPrefix(String entry) {
        if (entry == null || !entry.startsWith("~")) return "";
        String[] parts = entry.split("~", 4);
        return parts.length >= 2 ? "~" + parts[1] + "~" : "";
    }

    private String formatNearbyEntityEntry(String entry) {
        String label = formatEntityEntry(entry);
        double dist = distanceForNearbyEntityEntry(entry);
        if (dist == Double.MAX_VALUE) return label;
        return label + " (" + String.format(java.util.Locale.ROOT, "%.1fm", dist) + ")";
    }

    private List<BlockPos> getNearbyContainerPositions() {
        if (MC.player == null || MC.level == null) return Collections.emptyList();
        BlockPos center = MC.player.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        int range = 10;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!isLikelyContainerCandidate(pos)) continue;
                    found.add(pos);
                }
            }
        }
        found.sort(java.util.Comparator.comparingDouble(pos -> center.getCenter().distanceToSqr(pos.getCenter())));
        return found;
    }

    private boolean isLikelyContainerCandidate(BlockPos pos) {
        if (MC.level == null || pos == null) return false;
        net.minecraft.world.level.block.state.BlockState state = MC.level.getBlockState(pos);
        if (state == null || state.isAir()) return false;
        if (state.getMenuProvider(MC.level, pos) != null) return true;

        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase(Locale.ROOT);
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(Locale.ROOT);
        if (path.contains("chest")
                || path.contains("barrel")
                || path.contains("shulker")
                || path.contains("hopper")
                || path.contains("dispenser")
                || path.contains("dropper")
                || path.contains("furnace")
                || path.contains("smoker")
                || path.contains("blast_furnace")
                || path.contains("brewing")
                || path.contains("anvil")
                || path.contains("beacon")
                || path.contains("crafting")
                || path.contains("loom")
                || path.contains("smithing")
                || path.contains("stonecutter")
                || path.contains("cartography")
                || path.contains("grindstone")
                || path.contains("enchant")
                || path.contains("ender_chest")
                || path.contains("spawner")) {
            return true;
        }

        return blockId.contains("container") || blockId.contains("gui") || blockId.contains("menu");
    }

    private String formatContainerEntry(BlockPos pos) {
        if (pos == null) return "(unknown)";
        String blockName = MC.level != null ? MC.level.getBlockState(pos).getBlock().getName().getString() : "Container";
        return blockName + " (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }

    private void fillBlockPosField(String fieldKey, BlockPos pos) {
        if (fieldKey == null || pos == null) return;
        PackUtilChatField fx = textFields.get(fieldKey + "_0");
        PackUtilChatField fy = textFields.get(fieldKey + "_1");
        PackUtilChatField fz = textFields.get(fieldKey + "_2");
        if (fx != null) fx.setText(String.valueOf(pos.getX()));
        if (fy != null) fy.setText(String.valueOf(pos.getY()));
        if (fz != null) fz.setText(String.valueOf(pos.getZ()));
    }

    private boolean isCurrentBlockPosField(String fieldKey, BlockPos pos) {
        if (fieldKey == null || pos == null) return false;
        PackUtilChatField fx = textFields.get(fieldKey + "_0");
        PackUtilChatField fy = textFields.get(fieldKey + "_1");
        PackUtilChatField fz = textFields.get(fieldKey + "_2");
        if (fx == null || fy == null || fz == null) return false;
        try {
            int x = (int) Math.round(Double.parseDouble(fx.getText().trim()));
            int y = (int) Math.round(Double.parseDouble(fy.getText().trim()));
            int z = (int) Math.round(Double.parseDouble(fz.getText().trim()));
            return x == pos.getX() && y == pos.getY() && z == pos.getZ();
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private List<String> getOrCreateWaitPacketTargets() {
        return stringLists.computeIfAbsent("packetNames", ignored -> new ArrayList<>());
    }

    private List<String> sanitizeWaitPacketTargets(List<String> rawTargets) {
        List<String> sanitized = new ArrayList<>();
        for (String rawTarget : rawTargets == null ? Collections.<String>emptyList() : rawTargets) {
            String normalized = normalizeWaitPacketTarget(rawTarget);
            if (!normalized.isEmpty() && !sanitized.contains(normalized)) {
                sanitized.add(normalized);
            }
        }
        return sanitized;
    }

    private String normalizeWaitPacketTarget(String target) {
        String normalized = WaitForPacketAction.normalizeTarget(target);
        if (normalized.isEmpty()) return "";
        if (!WaitForPacketAction.getDirection(normalized).isEmpty()) return normalized;

        Class<? extends net.minecraft.network.protocol.Packet<?>> packetClass = resolvePacketClassForTarget("", normalized);
        if (packetClass == null) return normalized;
        if (PackUtilPacketRegistry.getC2SPackets().contains(packetClass)) return buildWaitPacketTarget("C2S", packetClass);
        if (PackUtilPacketRegistry.getS2CPackets().contains(packetClass)) return buildWaitPacketTarget("S2C", packetClass);
        return normalized;
    }

    private List<String> getWaitPacketTargets(String direction) {
        List<String> filtered = new ArrayList<>();
        for (String target : getOrCreateWaitPacketTargets()) {
            String normalized = normalizeWaitPacketTarget(target);
            if (!normalized.isEmpty() && direction.equalsIgnoreCase(WaitForPacketAction.getDirection(normalized))) {
                filtered.add(normalized);
            }
        }
        return filtered;
    }

    private String formatWaitPacketTarget(String target) {
        return WaitForPacketAction.getDisplayLabel(target);
    }

    private void clearWaitPacketTargets(String direction) {
        getOrCreateWaitPacketTargets().removeIf(target ->
            direction.equalsIgnoreCase(WaitForPacketAction.getDirection(normalizeWaitPacketTarget(target))));
        PackUiScrollViewport vpC2s = selectedScrollViewports.get("wait_packet_c2s");
        if (vpC2s != null) vpC2s.jumpTo(0);
        PackUiScrollViewport vpS2c = selectedScrollViewports.get("wait_packet_s2c");
        if (vpS2c != null) vpS2c.jumpTo(0);
    }

    private void removeWaitPacketTarget(String target) {
        String normalized = normalizeWaitPacketTarget(target);
        if (normalized.isEmpty()) return;
        getOrCreateWaitPacketTargets().removeIf(existing -> normalized.equals(normalizeWaitPacketTarget(existing)));
    }

    private void openWaitPacketSelector(boolean c2s) {
        if (c2s) {
            packetSelectorOverlay.openToggleC2S(
                (packetClass, selected) -> setWaitPacketTargetSelected("C2S", packetClass, selected),
                getSelectedWaitPacketClasses("C2S")
            );
        } else {
            packetSelectorOverlay.openToggleS2C(
                (packetClass, selected) -> setWaitPacketTargetSelected("S2C", packetClass, selected),
                getSelectedWaitPacketClasses("S2C")
            );
        }
    }

    private List<Class<? extends net.minecraft.network.protocol.Packet<?>>> getSelectedWaitPacketClasses(String direction) {
        List<Class<? extends net.minecraft.network.protocol.Packet<?>>> selected = new ArrayList<>();
        for (String target : getWaitPacketTargets(direction)) {
            Class<? extends net.minecraft.network.protocol.Packet<?>> packetClass = resolvePacketClassForTarget(direction, target);
            if (packetClass != null && !selected.contains(packetClass)) {
                selected.add(packetClass);
            }
        }
        return selected;
    }

    private void setWaitPacketTargetSelected(String direction, Class<? extends net.minecraft.network.protocol.Packet<?>> packetClass, boolean selected) {
        String target = buildWaitPacketTarget(direction, packetClass);
        if (target.isEmpty()) return;

        List<String> targets = getOrCreateWaitPacketTargets();
        if (selected) {
            if (!targets.stream().map(this::normalizeWaitPacketTarget).anyMatch(target::equals)) {
                targets.add(target);
            }
            return;
        }

        targets.removeIf(existing -> target.equals(normalizeWaitPacketTarget(existing)));
    }

    private void loadWaitPacketTargetsFromQueue() {
        List<PackUtilSharedState.QueuedPacket> queue = PackUtilSharedState.get().getDelayedPackets();
        if (queue == null || queue.isEmpty()) {
            PackUtilClientMessaging.sendPrefixed("Queue is empty");
            return;
        }

        List<String> targets = getOrCreateWaitPacketTargets();
        List<String> merged = sanitizeWaitPacketTargets(targets);
        int before = merged.size();
        for (PackUtilSharedState.QueuedPacket queuedPacket : queue) {
            String target = buildWaitPacketTarget(queuedPacket);
            if (!target.isEmpty() && !merged.contains(target)) {
                merged.add(target);
            }
        }
        targets.clear();
        targets.addAll(merged);
        int added = Math.max(0, merged.size() - before);
        PackUtilClientMessaging.sendPrefixed(added == 0
            ? "Queue did not add any new packet targets"
            : "Added " + added + " packet target" + (added == 1 ? "" : "s") + " from queue");
    }

    private String buildWaitPacketTarget(PackUtilSharedState.QueuedPacket queuedPacket) {
        if (queuedPacket == null || queuedPacket.packet == null) return "";
        @SuppressWarnings("unchecked")
        Class<? extends net.minecraft.network.protocol.Packet<?>> packetClass =
            (Class<? extends net.minecraft.network.protocol.Packet<?>>) queuedPacket.packet.getClass();
        if (PackUtilPacketRegistry.getC2SPackets().contains(packetClass)) return buildWaitPacketTarget("C2S", packetClass);
        if (PackUtilPacketRegistry.getS2CPackets().contains(packetClass)) return buildWaitPacketTarget("S2C", packetClass);
        return "";
    }

    private String buildWaitPacketTarget(String direction, Class<? extends net.minecraft.network.protocol.Packet<?>> packetClass) {
        if (packetClass == null) return "";
        String name = PackUtilPacketRegistry.getName(packetClass);
        if (name == null || name.isBlank()) {
            name = PackUtilPacketNamer.getFriendlyName(packetClass);
        }
        return WaitForPacketAction.withDirection(direction, name);
    }

    private Class<? extends net.minecraft.network.protocol.Packet<?>> resolvePacketClassForTarget(String direction, String target) {
        String packetName = WaitForPacketAction.getPacketName(target);
        if (packetName.isBlank()) return null;

        Class<? extends net.minecraft.network.protocol.Packet<?>> direct = PackUtilPacketRegistry.getPacket(packetName);
        if (direct != null) {
            if (direction.isBlank()) return direct;
            if ("C2S".equalsIgnoreCase(direction) && PackUtilPacketRegistry.getC2SPackets().contains(direct)) return direct;
            if ("S2C".equalsIgnoreCase(direction) && PackUtilPacketRegistry.getS2CPackets().contains(direct)) return direct;
        }

        List<Class<? extends net.minecraft.network.protocol.Packet<?>>> pool = new ArrayList<>();
        if ("C2S".equalsIgnoreCase(direction)) {
            pool.addAll(PackUtilPacketRegistry.getC2SPackets());
        } else if ("S2C".equalsIgnoreCase(direction)) {
            pool.addAll(PackUtilPacketRegistry.getS2CPackets());
        } else {
            pool.addAll(PackUtilPacketRegistry.getC2SPackets());
            pool.addAll(PackUtilPacketRegistry.getS2CPackets());
        }

        for (Class<? extends net.minecraft.network.protocol.Packet<?>> candidate : pool) {
            String registryName = PackUtilPacketRegistry.getName(candidate);
            if (packetNameMatches(packetName, registryName)) return candidate;
            if (packetNameMatches(packetName, PackUtilPacketNamer.getFriendlyName(candidate))) return candidate;
            if (packetNameMatches(packetName, candidate.getSimpleName())) return candidate;
        }
        return null;
    }

    private boolean packetNameMatches(String expected, String candidate) {
        if (expected == null || expected.isBlank() || candidate == null || candidate.isBlank()) return false;
        String normalizedExpected = expected.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        String normalizedCandidate = candidate.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalizedExpected.isEmpty() || normalizedCandidate.isEmpty()) return false;
        if (normalizedExpected.equals(normalizedCandidate)) return true;
        if (normalizedCandidate.endsWith("packet")) {
            String stripped = normalizedCandidate.substring(0, normalizedCandidate.length() - "packet".length());
            if (normalizedExpected.equals(stripped)) return true;
        }
        return normalizedExpected.endsWith(normalizedCandidate) || normalizedCandidate.endsWith(normalizedExpected);
    }

    private List<String> getPacketNames(boolean c2s) {
        List<String> names = new ArrayList<>();
        for (Class<? extends net.minecraft.network.protocol.Packet<?>> packetClass : (c2s ? PackUtilPacketRegistry.getC2SPackets() : PackUtilPacketRegistry.getS2CPackets())) {
            String name = PackUtilPacketRegistry.getName(packetClass);
            if (name != null && !name.isBlank()) names.add(name);
        }
        Collections.sort(names);
        return names;
    }

    private int renderBlockPosWithoutCapture(GuiGraphicsExtractor ctx, FieldDef field, int x, int y, int w,
                                             int mx, int my, float delta) {
        Identifier font = theme.fontFor(PackUiTone.BODY);
        drawLabel(ctx, field.label(), x, y, w, font);
        y += 13;
        int bw = (w - 4) / 3;
        for (int i = 0; i < 3; i++) {
            PackUtilChatField f = textFields.get(field.key() + "_" + i);
            if (f == null) continue;
            int fx = x + i * (bw + 2);
            f.setX(fx);
            f.setY(y + 1);
            f.setWidth(bw);
            f.render(ctx, mx, my, delta);
        }
        return y + ROW_H + ROW_GAP;
    }

    private static List<String> getAllSoundIds() {
        if (ALL_SOUND_IDS == null) {
            ALL_SOUND_IDS = new ArrayList<>();
            for (SoundEvent soundEvent : BuiltInRegistries.SOUND_EVENT) {
                Identifier id = BuiltInRegistries.SOUND_EVENT.getKey(soundEvent);
                if (id != null) ALL_SOUND_IDS.add(id.toString());
            }
            Collections.sort(ALL_SOUND_IDS);
        }
        return ALL_SOUND_IDS;
    }

    private static List<String> getAllEntityIds() {
        if (ALL_ENTITY_IDS == null) {
            ALL_ENTITY_IDS = new ArrayList<>();
            for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                ALL_ENTITY_IDS.add(id.toString());
            }
            Collections.sort(ALL_ENTITY_IDS);
        }
        return ALL_ENTITY_IDS;
    }

    private static List<String> getAllItemIds() {
        if (ALL_ITEM_IDS == null) {
            ALL_ITEM_IDS = new ArrayList<>();
            for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
                ALL_ITEM_IDS.add(id.toString());
            }
            Collections.sort(ALL_ITEM_IDS);
        }
        return ALL_ITEM_IDS;
    }

    private int rowH(FieldDef field) {
        if (isWaitChatPatternField(field)) {
            return 13 + WAIT_CHAT_PATTERN_H;
        }
        return switch (field.type()) {
            case MACRO_SELECT -> 17 + MACRO_SELECT_VISIBLE_ROWS * SEL_ITEM_H;
            case BLOCK_POS    -> 13 + ROW_H + ROW_GAP;
            case STRING_LIST  -> {
                if (field.captureMode() == CaptureMode.BLOCK_CATALOG) {

                    yield 13 + SEL_LIST_MAX_VIS * SEL_ITEM_H + 5 + 14 + 13 + CATALOG_LIST_H;
                }

                yield 13 + SEL_LIST_MAX_VIS * SEL_ITEM_H + 16;
            }
            default           -> ROW_H;
        };
    }

    private int computeContentH() {
        if (itemAction != null) {
            int h = PAD + 13 + SEL_LIST_MAX_VIS * SEL_ITEM_H + 6 + 20;
            h += 16;
            h += EDITOR_HINT_ROW_H;
            h += (ROW_H + ROW_GAP);
            if (toggleStates.getOrDefault("item_waitForGui", false))
                h += ROW_H + ROW_GAP;
            h += (ROW_H + ROW_GAP);
            return h;
        }
        if (payloadAction != null) {
            int h = PAD;
            h += 13;
            h += 24;
            h += ROW_H + ROW_GAP;
            h += 13;
            h += PAYLOAD_CONTENT_H + 4;
            h += 20;
            h += EDITOR_HINT_ROW_H;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.SEND_PACKET) {
            int h = PAD + 13 + SEL_LIST_MAX_VIS * SEL_ITEM_H + 6;
            h += ROW_H + ROW_GAP;
            h += ROW_H + ROW_GAP;
            if (toggleStates.getOrDefault("waitForGui", false)) h += ROW_H + ROW_GAP;
            h += 20 + 20 + 6 + 13 + SEL_LIST_MAX_VIS * SEL_ITEM_H;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_SOUND) {
            int h = PAD;
            h += isFieldVisible(getField("waitForGui")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("waitGuiName")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("checkDistance")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("maxDistance")) ? ROW_H + ROW_GAP : 0;
            h += 13 + (4 * SEL_ITEM_H) + 4;
            h += 18;
            h += 6 * CATALOG_ITEM_H;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_ENTITY) {
            int h = PAD;
            h += isFieldVisible(getField("checkMode")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("radius")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("centerOnPlayer")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("pos")) ? rowH(getField("pos")) + ROW_GAP : 0;
            h += isFieldVisible(getField("mustBeLookingAt")) ? ROW_H + ROW_GAP : 0;
            h += 13 + (4 * SEL_ITEM_H) + 4;
            h += 18;
            h += 4 * CATALOG_ITEM_H + 10;
            h += 13 + 3 * CATALOG_ITEM_H;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.LOOK_AT_BLOCK) {
            int h = PAD;
            h += isFieldVisible(getField("targetMode")) ? ROW_H + ROW_GAP : 0;
            String mode = currentEnumValue("targetMode");
            if ("BLOCK".equals(mode)) {
                h += isFieldVisible(getField("searchRadius")) ? ROW_H + ROW_GAP : 0;
                h += rowH(getField("blockIds")) + ROW_GAP;
            } else if ("ENTITY".equals(mode)) {
                h += isFieldVisible(getField("searchRadius")) ? ROW_H + ROW_GAP : 0;
                h += 13 + (4 * SEL_ITEM_H) + 4;
                h += 18;
                h += 4 * CATALOG_ITEM_H + 14;
                h += 13 + (3 * CATALOG_ITEM_H) + 4;
            } else {
                h += rowH(getField("blockPos")) + ROW_GAP;
            }
            h += isFieldVisible(getField("smooth")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("smoothness")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("waitForCompletion")) ? ROW_H + ROW_GAP : 0;
            h += 14;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.ROTATE) {
            int h = PAD;
            h += isFieldVisible(getField("yaw")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("pitch")) ? ROW_H + ROW_GAP : 0;
            h += 18;
            h += isFieldVisible(getField("smooth")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("smoothness")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("waitForCompletion")) ? ROW_H + ROW_GAP : 0;
            h += 14;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.GO_TO) {
            int h = PAD;
            h += rowH(getField("pos")) + ROW_GAP;
            h += 18;
            h += isFieldVisible(getField("waitForArrival")) ? ROW_H + ROW_GAP : 0;
            h += 14;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.USE_ITEM) {
            int h = PAD;
            h += isFieldVisible(getField("itemName")) ? ROW_H + ROW_GAP : 0;
            h += 18;
            h += isFieldVisible(getField("useMode")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("holdTicks")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("useCount")) ? ROW_H + ROW_GAP : 0;
            h += 16;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.INVENTORY_AUDIT) {
            int h = PAD;
            h += isFieldVisible(getField("mode")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("targetItems")) ? 13 + SEL_LIST_MAX_VIS * SEL_ITEM_H + 4 + 18 : 0;
            h += isFieldVisible(getField("openMode")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("openCommand")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("containerPos")) ? rowH(getField("containerPos")) + ROW_GAP : 0;
            if ("CONTAINER".equals(currentEnumValue("openMode"))) {
                h += 13 + CONTAINER_LIST_VISIBLE_ROWS * CATALOG_ITEM_H + 17;
            }
            h += isFieldVisible(getField("dupeVector")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("iterations")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("maxTransferAttempts")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("transferRetryDelayMs")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("spamCount")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("spamDelayMs")) ? ROW_H + ROW_GAP : 0;
            h += 16;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.SWAP_SLOTS) {
            int h = PAD + 16;
            h += isFieldVisible(getField("fromUseItemName")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("fromItemName")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("fromSlot")) ? ROW_H + ROW_GAP : 0;
            h += 8 + 16;
            h += isFieldVisible(getField("toUseItemName")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("toItemName")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("toSlot")) ? ROW_H + ROW_GAP : 0;
            h += 16;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.CLICK) {
            int h = PAD;
            h += isFieldVisible(getField("clickType")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("clickCount")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("waitForGui")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("guiName")) ? ROW_H + ROW_GAP : 0;
            h += 16;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.DISCONNECT) {
            int h = PAD;
            h += isFieldVisible(getField("mode")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("delayMs")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("lagMethod")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("kickMethod")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("packetCount")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("useNextAction")) ? ROW_H + ROW_GAP : 0;

            h += isFieldVisible(getField("trigger")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("tolerance")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("bufferMs")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("timeoutSec")) ? ROW_H + ROW_GAP : 0;
            h += EDITOR_HINT_ROW_H;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_GUI) {
            int h = PAD;
            h += isFieldVisible(getField("waitMode")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("guiTitle")) ? ROW_H + ROW_GAP : 0;
            h += 16;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.SELECT_SLOT) {
            int h = PAD;
            h += isFieldVisible(getField("itemName")) ? ROW_H + ROW_GAP : 0;
            h += 18;
            h += 12 + 18;
            h += 16;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_COOLDOWN) {
            int h = PAD;
            h += isFieldVisible(getField("itemName")) ? ROW_H + ROW_GAP : 0;
            h += 18;
            h += 18;
            h += 16;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_CHAT) {
            int h = PAD;
            h += isFieldVisible(getField("pattern")) ? rowH(getField("pattern")) + ROW_GAP : 0;
            h += isFieldVisible(getField("useRegex")) ? ROW_H + ROW_GAP : 0;
            h += !toggleStates.getOrDefault("useRegex", false) ? 29 : 0;
            h += isFieldVisible(getField("timeoutMs")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("waitForGui")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("waitGuiName")) ? ROW_H + ROW_GAP : 0;
            h += 18;
            h += 18;
            h += 13 + waitChatHistoryListHeight();
            h += 16;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.OPEN_CONTAINER) {
            int h = PAD;
            h += isFieldVisible(getField("targetMode")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("pos")) ? rowH(getField("pos")) + ROW_GAP : 0;
            h += isFieldVisible(getField("entityTargets")) ? rowH(getField("entityTargets")) + ROW_GAP : 0;
            h += isFieldVisible(getField("waitForGui")) ? ROW_H + ROW_GAP : 0;
            String targetMode = currentEnumValue("targetMode");
            h += 12;
            if ("ENTITY".equals(targetMode)) {
                h += 13 + 3 * CATALOG_ITEM_H;
            } else if ("BLOCK".equals(targetMode)) {
                h += 13 + CONTAINER_LIST_VISIBLE_ROWS * CATALOG_ITEM_H;
            }
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_SLOT_CHANGE) {
            int h = PAD;
            h += 13 + SEL_LIST_MAX_VIS * SEL_ITEM_H + 6;
            h += 16;
            h += 16;
            h += EDITOR_HINT_ROW_H;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.DELAY_PACKETS) {
            int h = PAD;
            h += isFieldVisible(getField("mode")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("flushOnDisable")) ? ROW_H + ROW_GAP : 0;
            if ("ENABLE".equals(currentEnumValue("mode"))) {
                h += 18 + 18 + 18;
            }
            h += 8;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.MINE) {
            int h = PAD;
            h += rowH(getField("targetBlocks")) + ROW_GAP + 4;
            h += isFieldVisible(getField("stopInventoryFull")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("stopSlotsUsed")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("slotsUsedThreshold")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("stopMinedCount")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("minedCountTarget")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("stopAfterTime")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("timeoutSeconds")) ? ROW_H + ROW_GAP : 0;
            h += 14;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.PAY) {
            int h = PAD;
            h += isFieldVisible(getField("commandTemplate")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("amountInput")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("delayEnabled")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("delayMs")) ? ROW_H + ROW_GAP : 0;
            h += 13 + (4 * SEL_ITEM_H) + 4;
            h += 18;
            h += 6 * CATALOG_ITEM_H + 4;
            h += 24;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.STORE_ITEM) {
            int h = PAD;
            h += isFieldVisible(getField("mode")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("allItems")) ? ROW_H + ROW_GAP : 0;
            if (!toggleStates.getOrDefault("allItems", false)) {
                h += 13 + (4 * SEL_ITEM_H) + 4;
                h += 18;
                h += 6 * CATALOG_ITEM_H + 4;
                h += EDITOR_HINT_ROW_H * 2;
            }
            h += isFieldVisible(getField("persistent")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("closeAfter")) ? ROW_H + ROW_GAP : 0;
            h += isFieldVisible(getField("closeSendPkt")) ? ROW_H + ROW_GAP : 0;
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.TOGGLE_MODULE) {
            int h = PAD;
            h += 13 + (4 * SEL_ITEM_H) + 4;
            h += 18;
            h += 6 * CATALOG_ITEM_H + 4;
            h += 14;
            return h;
        }
        if (craftEntries != null) {

            return PAD + 13 + SEL_LIST_MAX_VIS * SEL_ITEM_H + 6 + 16 + CRAFT_LIST_H;
        }
        if (dropAction != null) {
            int h = PAD + 13 + SEL_LIST_MAX_VIS * SEL_ITEM_H + 6 + 16 + 4;
            h += 12;
            h += (ROW_H + ROW_GAP);
            h += ROW_H + ROW_GAP;
            h += (ROW_H + ROW_GAP);
            if (toggleStates.getOrDefault("drop_waitForGui", false)) h += ROW_H + ROW_GAP;
            return h;
        }
        if (lanStepEntries != null) {
            boolean filterByUser = toggleStates.getOrDefault("lan_filterByUser", false);
            int h = PAD + ROW_H + ROW_GAP;
            if (!filterByUser || lanStepEntries.isEmpty()) h += ROW_H + ROW_GAP;
            h += 12;
            if (filterByUser) {
                if (!lanStepEntries.isEmpty()) {
                    int visibleRows = Math.min(3, Math.max(1, lanStepEntries.size()));
                    h += 13 + visibleRows * SEL_ITEM_H;
                } else {
                    h += 12;
                }
                h += 6 + 16;
            }
            return h;
        }
        if (targetAction != null && targetAction.getType() == MacroActionType.WAIT_PACKET) {
            return PAD + 14 + 18 + 20 + (13 + SEL_ITEM_H * 4 + 4) * 2 + 4;
        }
        if (schema == null) return 0;
        int h = PAD;
        for (FieldDef field : schema.fields()) {
            if (field.type() == FieldType.STRING_LIST) continue;
            if (!isFieldVisible(field)) continue;
            h += rowH(field) + ROW_GAP;
        }
        for (FieldDef field : schema.fields()) {
            if (field.type() != FieldType.STRING_LIST) continue;
            if (!isFieldVisible(field)) continue;
            h += rowH(field) + ROW_GAP;
        }

        if (targetAction != null && targetAction.getType() == MacroActionType.PACKET) {
            h += 52;
        }

        if (targetAction != null && targetAction.getType() == MacroActionType.DELAY_PACKETS) {
            h += 4 + 14;
        }
        return h;
    }

    private int computeDisconnectMaxH() {
        int h = PAD;
        h += ROW_H + ROW_GAP;
        h += ROW_H + ROW_GAP;
        h += ROW_H + ROW_GAP;
        h += ROW_H + ROW_GAP;
        h += ROW_H + ROW_GAP;
        h += ROW_H + ROW_GAP;

        h += ROW_H + ROW_GAP;
        h += ROW_H + ROW_GAP;
        h += ROW_H + ROW_GAP;
        h += ROW_H + ROW_GAP;
        h += EDITOR_HINT_ROW_H;
        return h;
    }

    private void drawLabel(GuiGraphicsExtractor ctx, String text, int x, int y, int maxW,
                           Identifier font) {
        String trimmed = PackUiText.trimToWidth(textRenderer, text, maxW - 4, font, -1);
        PackUiText.draw(
            ctx,
            textRenderer,
            trimmed,
            font,
            PackUtilColors.textPrimary(),
            x,
            PackUiSizing.alignTextY(y, ROW_H, fontHeight(font), theme.bodyTextNudge()),
            false
        );
    }

    private int fontHeight(Identifier font) {
        if (PackUiAssets.FONT_TITLE.equals(font)) return theme.fontHeight(PackUiTone.TITLE);
        if (PackUiAssets.FONT_LABEL.equals(font)) return theme.fontHeight(PackUiTone.LABEL);
        return theme.fontHeight(PackUiTone.BODY);
    }

    private int uiWidth(Identifier font, String text) {
        return PackUiText.width(textRenderer, text == null ? "" : text, font, PackUtilColors.textPrimary());
    }

    private int uiWidth(String text) {
        return uiWidth(theme.fontFor(PackUiTone.BODY), text);
    }

    private int labelWidth(int totalWidth, String label, Identifier font) {
        return labelWidth(totalWidth, label, font, 56);
    }

    private int labelWidth(int totalWidth, String label, Identifier font, int minControlWidth) {
        int measured = uiWidth(font, label) + 8;
        int availableMax = Math.max(LABEL_MIN_W, totalWidth - Math.max(28, minControlWidth) - FIELD_GAP);
        return Math.max(LABEL_MIN_W, Math.min(measured, availableMax));
    }

    private int controlX(int x, int labelWidth) {
        return x + labelWidth + FIELD_GAP;
    }

    private int controlWidth(int totalWidth, int labelWidth) {
        return Math.max(28, totalWidth - labelWidth - FIELD_GAP);
    }

    private void fillBorder(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,       y,       x + w, y + 1,   color);
        ctx.fill(x,       y+h-1,   x + w, y + h,   color);
        ctx.fill(x,       y,       x + 1, y + h,   color);
        ctx.fill(x+w-1,   y,       x + w, y + h,   color);
    }

    private void drawScrollbar(GuiGraphicsExtractor ctx, int trackX, int trackY, int trackH,
                               int totalCount, int visibleCount, int itemH, int scrollOffset) {
        if (totalCount <= visibleCount) return;
        int viewPixels = Math.max(1, Math.min(trackH, visibleCount * itemH));
        int contentPixels = Math.max(0, totalCount * itemH);
        PackUiScrollbar.Metrics metrics = PackUiScrollbar.compute(contentPixels, viewPixels, trackX, trackY, SCROLLBAR_W, trackH, scrollOffset);
        PackUiScrollbar.draw(ctx, metrics, false, false);
    }

    private PackUiSmoothScroll scrollState(Map<String, PackUiSmoothScroll> states, String key, int initialOffset) {
        return states.computeIfAbsent(key, ignored -> {
            PackUiSmoothScroll state = new PackUiSmoothScroll();
            state.restore(initialOffset);
            return state;
        });
    }

    private PackUiScrollViewport getOrCreateViewport(Map<String, PackUiScrollViewport> viewports, String key,
                                                      int x, int y, int width, int height, int rowHeight, int scrollbarWidth) {
        PackUiScrollViewport vp = viewports.get(key);
        if (vp == null || vp.getX() != x || vp.getY() != y || vp.getWidth() != width || vp.getHeight() != height) {
            vp = new PackUiScrollViewport(x, y, width, height, rowHeight, scrollbarWidth);
            viewports.put(key, vp);
        }
        return vp;
    }

    private int tickListScroll(
        String key,
        Map<String, Integer> targetOffsets,
        Map<String, PackUiSmoothScroll> states,
        int maxScroll,
        float delta
    ) {
        int target = Math.max(0, Math.min(targetOffsets.getOrDefault(key, 0), maxScroll));
        targetOffsets.put(key, target);
        PackUiSmoothScroll state = scrollState(states, key, target);
        state.setTarget(target, maxScroll);
        return state.tick(delta, maxScroll);
    }

    private PackUtilChatField makeField(int w) {
        return new PackUtilChatField(MC, textRenderer, 0, 0, w, 13, false);
    }

    private static String fmtDouble(double v) {
        long lv = (long) v;
        return (v == lv) ? String.valueOf(lv) : String.valueOf(v);
    }

    private static boolean matchesListFilter(String filter, String... candidates) {
        String normalized = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return true;
        for (String candidate : candidates) {
            if (candidate != null && candidate.toLowerCase(Locale.ROOT).contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private void startCapture(FieldDef field, List<String> lst) {
        screenBeforeCapture = MC.screen;
        enterEditorOnlyCaptureMode();
        MC.execute(() -> { if (MC.screen != null) MC.setScreen(null); });

        PackUtilSharedState state = PackUtilSharedState.get();
        state.setCaptureCancelCallback(() -> exitCaptureMode(true, false));
        if (field.captureMode() == CaptureMode.BLOCK_ID || field.captureMode() == CaptureMode.BLOCK_CATALOG) {
            state.setBlockCaptureCallback(pos -> {
                if (MC.level != null) {
                    String id = BuiltInRegistries.BLOCK.getKey(
                            MC.level.getBlockState(pos).getBlock()).toString();
                    if (!lst.contains(id)) lst.add(id);
                }
                exitCaptureMode(true, false);
            });
        } else if (field.captureMode() == CaptureMode.ENTITY_ID) {
            boolean specificCapture = isOpenContainerEntityListKey(field.key());
            state.setEntityCaptureSpecific(specificCapture);
            state.setEntityCaptureCallback(payload -> {
                String value = payload == null ? "" : payload.strip();
                if (!value.isBlank()) {
                    if (specificCapture) lst.clear();
                    if (!lst.contains(value)) lst.add(value);
                }
                exitCaptureMode(true, false);
            });
        }
    }

    private void startBlockPosCapture(FieldDef field) {
        if (targetAction != null && targetAction.getType() == MacroActionType.INSTA_BREAK && "blockPos".equals(field.key())) {
            startInstaBreakCapture();
            return;
        }
        screenBeforeCapture = MC.screen;
        enterEditorOnlyCaptureMode();
        String key   = field.key();
        boolean dbl  = field.xyzDouble();
        MC.execute(() -> { if (MC.screen != null) MC.setScreen(null); });

        PackUtilSharedState state = PackUtilSharedState.get();
        state.setCaptureCancelCallback(() -> exitCaptureMode(true, false));
        state.setBlockCaptureCallback(pos -> {
            PackUtilChatField fx = textFields.get(key + "_0");
            PackUtilChatField fy = textFields.get(key + "_1");
            PackUtilChatField fz = textFields.get(key + "_2");
            if (fx != null) fx.setText(dbl ? fmtDouble(pos.getX()) : String.valueOf(pos.getX()));
            if (fy != null) fy.setText(dbl ? fmtDouble(pos.getY()) : String.valueOf(pos.getY()));
            if (fz != null) fz.setText(dbl ? fmtDouble(pos.getZ()) : String.valueOf(pos.getZ()));
            exitCaptureMode(true, false);
        });
    }

    private void startInstaBreakCapture() {
        screenBeforeCapture = MC.screen;
        enterEditorOnlyCaptureMode();
        MC.execute(() -> { if (MC.screen != null) MC.setScreen(null); });

        PackUtilSharedState state = PackUtilSharedState.get();
        state.setCaptureCancelCallback(() -> exitCaptureMode(true, false));
        state.setDirectionalBlockCaptureCallback((pos, direction) -> {
            fillBlockPosField("blockPos", pos);
            enumIndices.put("direction", direction == null ? 1 : direction.ordinal());
            PackUtilClientMessaging.sendPrefixed("InstaBreak target captured");
            exitCaptureMode(true, false);
        });
        PackUtilClientMessaging.sendPrefixed("InstaBreak: right-click the target block to capture it");
    }

    private static List<String> getAllBlockIds() {
        if (ALL_BLOCK_IDS == null) {
            ALL_BLOCK_IDS = new ArrayList<>();
            for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
                String path = id.getPath();

                if (path.endsWith("_wall_head") || path.endsWith("_wall_sign") || path.endsWith("_wall_banner") || path.endsWith("_wall_torch") || path.endsWith("_wall_skull")) {
                    continue;
                }
                ALL_BLOCK_IDS.add(id.toString());
            }
            Collections.sort(ALL_BLOCK_IDS);
        }
        return ALL_BLOCK_IDS;
    }

    private static String formatTypeName(MacroActionType type) {
        String[] parts = type.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private String getAbstractContainerScreenCaptureTargetLabel() {
        if ("_item_entries".equals(itemSlotCapturePendingKey) || "_drop_entries".equals(itemSlotCapturePendingKey)
                || "_wsc_entries".equals(itemSlotCapturePendingKey)) {
            return "Slot + Item";
        }
        FieldDef field = findField(itemSlotCapturePendingKey);
        return field != null ? field.label() : "Slot";
    }

    private String getCaptureActionLabel() {
        return targetAction != null ? formatTypeName(targetAction.getType()) : "Action";
    }

    private FieldDef findField(String key) {
        if (schema == null || key == null) return null;
        for (FieldDef field : schema.fields()) {
            if (key.equals(field.key())) return field;
        }
        return null;
    }
}
