package autismclient.util;

import autismclient.gui.packui.PackUiAssets;
import autismclient.gui.packui.PackUiBannerRenderer;
import autismclient.gui.packui.PackUiButtonFeedback;
import autismclient.gui.packui.PackUiControlGlyphs;
import autismclient.gui.packui.PackUiHeaderControls;
import autismclient.gui.packui.PackUiInsets;
import autismclient.gui.packui.PackUiListRenderer;
import autismclient.gui.packui.PackUiOverlayButton;
import autismclient.gui.packui.PackUiRenderContext;
import autismclient.gui.packui.PackUiScrollbar;
import autismclient.gui.packui.PackUiSizing;
import autismclient.gui.packui.PackUiSurface;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import autismclient.gui.packui.PackUiViewport;
import autismclient.gui.packui.PackUiViewportSlot;
import autismclient.gui.packui.PackUiWindowNode;
import autismclient.util.macro.ClickAction;
import autismclient.util.macro.CloseGuiAction;
import autismclient.util.macro.CraftAction;
import autismclient.util.macro.DelayAction;
import autismclient.util.macro.DelayPacketsAction;
import autismclient.util.macro.DesyncAction;
import autismclient.util.macro.DisconnectAction;
import autismclient.util.macro.DropAction;
import autismclient.util.macro.GoToAction;
import autismclient.util.macro.InventoryAuditAction;
import autismclient.util.macro.InventoryAction;
import autismclient.util.macro.InstaBreakAction;
import autismclient.util.macro.ItemAction;
import autismclient.util.macro.JumpAction;
import autismclient.util.macro.LookAtBlockAction;
import autismclient.util.macro.MacroAction;
import autismclient.util.macro.MacroExecutor;
import autismclient.util.macro.MineAction;
import autismclient.util.macro.MoveAction;
import autismclient.util.macro.NbtBookAction;
import autismclient.util.macro.OpenContainerAction;
import autismclient.util.macro.PayAction;
import autismclient.util.macro.PayloadAction;
import autismclient.util.macro.RepeatAction;
import autismclient.util.macro.RestoreGuiAction;
import autismclient.util.macro.RevisionSyncAction;
import autismclient.util.macro.RotateAction;
import autismclient.util.macro.SaveGuiAction;
import autismclient.util.macro.SelectSlotAction;
import autismclient.util.macro.SendChatAction;
import autismclient.util.macro.SendPacketAction;
import autismclient.util.macro.SendToggleAction;
import autismclient.util.macro.ServerTickSyncAction;
import autismclient.util.macro.SneakAction;
import autismclient.util.macro.SprintAction;
import autismclient.util.macro.StartMacroAction;
import autismclient.util.macro.StopMacroAction;
import autismclient.util.macro.StoreItemAction;
import autismclient.util.macro.SwapSlotsAction;
import autismclient.util.macro.TickSyncAction;
import autismclient.util.macro.ToggleModuleAction;
import autismclient.util.macro.UseItemAction;
import autismclient.util.macro.WaitForBlockAction;
import autismclient.util.macro.WaitForChatAction;
import autismclient.util.macro.WaitForCooldownAction;
import autismclient.util.macro.WaitForEntityAction;
import autismclient.util.macro.WaitForGuiAction;
import autismclient.util.macro.WaitForHealthAction;
import autismclient.util.macro.WaitForLanStepAction;
import autismclient.util.macro.WaitForMacroStepAction;
import autismclient.util.macro.WaitForPacketAction;
import autismclient.util.macro.WaitForSlotChangeAction;
import autismclient.util.macro.WaitForSoundAction;
import autismclient.util.macro.WaitPosAction;
import autismclient.util.macro.XCarryAction;
import autismclient.gui.macro.editor.ActionEditorOverlay;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;

public class PackUtilMacroEditorOverlay extends PackUtilOverlayBase {
   private static final Minecraft MC = Minecraft.getInstance();
   private static PackUtilMacroEditorOverlay sharedOverlay;
   private static final int DEFAULT_PANEL_WIDTH = 252;
   private static final int HISTORY_LIMIT = 30;
   private static final int PACKUI_HEADER_CONTROL = 12;
   private static final int PACKUI_HEADER_ARROW_WIDTH = 10;
   private static final int PACKUI_HEADER_ARROW_GAP = 3;
   private static final float HEADER_CLICK_DRAG_THRESHOLD = 3.0F;
   private static final int STEP_ROW_CONTROL_SIZE = 12;
   private static final int STEP_ROW_CONTROL_GAP = 4;
   private final Font textRenderer;
   private final PackUiTheme theme = new PackUiTheme();
   private final PackUiWindowNode windowNode = new PackUiWindowNode("Macro Editor");
   private final PackUiSurface surface = new PackUiSurface(this.theme, this.windowNode);
   private final PackUiViewportSlot shellBody = new PackUiViewportSlot();
   private boolean visible = false;
   private PackUtilMacro macro;
   private boolean isNew = false;
   private boolean reopenMacroListOnClose = false;
   private int panelX;
   private int panelY;
   private int PANEL_WIDTH = 258;
   private int PANEL_HEIGHT = 308;
   private List<String> btnLabels = new ArrayList<>();
   private List<int[]> btnBounds = new ArrayList<>();
   private List<Runnable> btnActions = new ArrayList<>();
   private List<Boolean> btnEnabled = new ArrayList<>();
   private int runBtnIndex = -1;
   private PackUtilChatField nameField;
   private PackUtilChatField loopCountField;
   private boolean isBindingKey = false;
   private int loopMode = 0;
   private int scrollOffset = 0;
   private static final int ACTION_HEIGHT = 18;
   private static final float STEP_DRAG_THRESHOLD = 3.0F;
   private static final int STEP_DRAG_AUTO_SCROLL_EDGE = 18;
   private static final int STEP_DRAG_AUTO_SCROLL_MAX_SPEED = 12;
   private int draggingIndex = -1;
   private int pressedStepIndex = -1;
   private int stepDragTargetIndex = -1;
   private int stepDragGrabOffsetY = 0;
   private int stepDragMouseY = 0;
   private double stepPressMouseX = 0.0;
   private double stepPressMouseY = 0.0;
   private boolean isWindowDragging = false;
   private boolean isWindowResizing = false;
   private double dragOffsetX = 0.0;
   private double dragOffsetY = 0.0;
   private double headerPressMouseX = 0.0;
   private double headerPressMouseY = 0.0;
   private int headerPressPanelX = 0;
   private int headerPressPanelY = 0;
   private boolean headerDragMoved = false;
   private double resizeStartMouseX = 0.0;
   private double resizeStartMouseY = 0.0;
   private int resizeStartWidth = 0;
   private int resizeStartHeight = 0;
   private float closeHover = 0.0F;
   private float closeVisibility = 1.0F;
   private boolean collapsed = false;
   private static final int MOD_LIST_ROWS = 8;
   private static final int MOD_LIST_ROW_H = 14;
   private String lastModuleSearch = "";
   private List<String> entityRegistryCache = null;
   private PackUtilMacroEditorOverlay.StepPickerMode stepPickerMode = null;
   private List<PackUtilMacroEditorOverlay.StepPickerCategory> stepPickerCategories = Collections.emptyList();
   private List<PackUtilMacroEditorOverlay.StepPickerEntry> stepPickerEntries = Collections.emptyList();
   private int stepPickerScrollOffset = 0;
   private int activeScrollbarDrag = 0;
   private int scrollbarGrabOffset = 0;
   private static final int SCROLLBAR_NONE = 0;
   private static final int SCROLLBAR_STEP_LIST = 1;
   private static final int SCROLLBAR_STEP_PICKER = 2;
   private static final float STEP_ROW_ENTER_SECONDS = 0.14F;
   private static final float STEP_ROW_MOVE_SECONDS = 0.14F;
   private static final float STEP_ROW_EXIT_SECONDS = 0.12F;
   private static final int STEP_ROW_ENTER_OFFSET = 18;
   private static final int STEP_ROW_EXIT_OFFSET = 20;
   private String hoveredTooltip = null;
   private int tooltipX;
   private int tooltipY;
   private final Deque<CompoundTag> undoHistory = new ArrayDeque<>();
   private final Deque<CompoundTag> redoHistory = new ArrayDeque<>();
   private PackUtilMacro originalMacro;
   private boolean tempSavedForRun = false;
   private CompoundTag originalMacroSnapshot = null;
   private boolean saveBtnDirty = false;
   private final IdentityHashMap<MacroAction, Integer> stepRowStableIds = new IdentityHashMap<>();
   private final List<PackUtilMacroEditorOverlay.StepRowSnapshot> lastStepRowSnapshots = new ArrayList<>();
   private final List<PackUtilMacroEditorOverlay.StepRowMotion> stepRowMotions = new ArrayList<>();
   private int nextStepRowStableId = 1;
   private long stepRowAnimationClockNanos = System.nanoTime();

   public static PackUtilMacroEditorOverlay getSharedOverlay() {
      if (sharedOverlay == null) {
         sharedOverlay = new PackUtilMacroEditorOverlay(Minecraft.getInstance().font);
         sharedOverlay.restoreLayout();
         PackUtilOverlayManager.get().register(sharedOverlay);
      }

      sharedOverlay.restorePosition();
      return sharedOverlay;
   }

   private PackUiOverlayButton createOverlayButton(
      int x, int y, int w, int h, String label, PackUiOverlayButton.Variant variant, boolean enabled, Runnable action
   ) {
      PackUiOverlayButton button = PackUiOverlayButton.create(x, y, w, h, Component.literal(label), ignored -> {
         if (action != null) {
            action.run();
         }
      });
      button.setWidth(w);
      button.setVariant(variant);
      button.active = enabled;
      return button;
   }

   private PackUiOverlayButton createStepRowControlButtonView(int x, int y, int width, int height, boolean danger, String label) {
      return this.createOverlayButton(
         x,
         y,
         width,
         height,
         label == null ? "" : label,
         danger ? PackUiOverlayButton.Variant.DANGER : PackUiOverlayButton.Variant.SECONDARY,
         true,
         null
      );
   }

   private void drawOverlayButton(
      GuiGraphicsExtractor context, int x, int y, int w, int h, String label, PackUiOverlayButton.Variant variant, boolean enabled, int mouseX, int mouseY
   ) {
      PackUiOverlayButton button = this.createOverlayButton(x, y, w, h, label, variant, enabled, null);
      PackUiOverlayButton.renderStyled(context, this.textRenderer, button, mouseX, mouseY);
   }

   public PackUtilMacroEditorOverlay(Font textRenderer) {
      this.textRenderer = textRenderer;
      this.PANEL_WIDTH = this.defaultPanelWidth();
      this.PANEL_HEIGHT = this.defaultPanelHeight();
      this.windowNode.setCenterTitle(false);
      this.windowNode.setTitleTone(PackUiTone.LABEL);
      this.windowNode.setTitleAreaInsets(this.titleLeftInset(), this.titleRightInset());
      this.windowNode.content().setGap(0).setPadding(PackUiInsets.NONE);
      this.windowNode.content().add(this.shellBody);
   }

   private int getStepBuilderLabelY() {
      return this.getTopControlsBottomY() + this.stepBuilderLabelOffset();
   }

   private int getStepBuilderButtonY() {
      return this.getStepBuilderLabelY() + this.theme.lineHeight(PackUiTone.LABEL, 1) + this.stepBuilderLabelGap();
   }

   private int getStepListLabelY() {
      return this.getStepBuilderButtonY() + this.editorButtonHeight() + this.stepBuilderSectionGap();
   }

   private int getStepListY() {
      return this.getStepListLabelY() + this.theme.lineHeight(PackUiTone.LABEL, 1) + 1;
   }

   private int getStepListFrameX() {
      return this.panelX + 8;
   }

   private int getStepListFrameWidth() {
      return this.PANEL_WIDTH - 16;
   }

   private int getStepListContentLeft() {
      return this.getStepListFrameX() + 1;
    }

    private int getActionRowControlsWidth(MacroAction action) {
      int count = action != null && this.hasActionEditor(action) ? 5 : 4;
      return count * this.stepRowControlSize() + (count - 1) * this.stepRowControlGap();
   }

   private int getActionRowControlsX(MacroAction action) {
      return this.getStepListContentRight() - this.getActionRowControlsWidth(action) - 1;
   }

   private int getFooterButtonsY() {
      return this.panelY + this.PANEL_HEIGHT - this.footerBottomInset();
   }

   private int getFooterTopY() {
      return this.getFooterButtonsY() - this.footerSectionGap();
   }

   private int getStepListAvailableHeight() {
      return Math.max(0, this.getFooterTopY() - this.getStepListY());
   }

   private int getStepListHeight() {
      return Math.max(0, this.getSteplistViewportHeight() + 2);
   }

   private int getSteplistViewportHeight() {
      return this.alignViewportHeight(Math.max(0, this.getStepListAvailableHeight() - 2), this.actionRowHeight());
   }

   private int getStepListClipTop() {
      return this.getStepListY() + 1;
   }

   private int getStepListClipBottom() {
      return this.getStepListClipTop() + this.getSteplistViewportHeight();
   }

   private int getMaxStepListScroll() {
      return this.macro == null ? 0 : Math.max(0, this.macro.actions.size() * this.actionRowHeight() - this.getSteplistViewportHeight());
   }

   private boolean isStepRowFullyVisible(int rowY) {
      return rowY >= this.getStepListY() && rowY + this.actionRowHeight() <= this.getStepListClipBottom();
   }

      private int defaultPanelWidth() {
      return 258;
   }

   private int defaultPanelHeight() {
      return 308;
   }

   private int minimumPanelHeight() {
      return 272;
   }

   private int titleLeftInset() {
      return 10;
   }

   private int titleRightInset() {
      return 39;
   }

   private int stepBuilderLabelOffset() {
      return 5;
   }

   private int stepBuilderLabelGap() {
      return 1;
   }

   private int stepBuilderSectionGap() {
      return 5;
   }

   private int stepRowControlSize() {
      return 13;
   }

   private int stepRowControlGap() {
      return 2;
   }

      private int stepListScrollbarGutter() {
         return 6;
      }

      private int getStepListContentRight() {
         return this.getStepListFrameX() + this.getStepListFrameWidth() - this.stepListScrollbarGutter() - 1;
      }

      private int getStepListContentRightForAction(MacroAction action) {
         int buttonsX = this.getActionRowControlsX(action);
         return buttonsX - 1;
      }

     private int getStepListScrollbarX() {
       return this.getStepListFrameX() + this.getStepListFrameWidth() - 4;
    }

    private PackUiScrollbar.Metrics getStepListScrollbarMetrics() {
       this.clampStepListScrollOffset();
       int listHeight = this.getStepListHeight();
       int viewHeight = Math.max(1, this.getSteplistViewportHeight());
       return PackUiScrollbar.compute(
          this.macro.actions.size() * this.actionRowHeight(),
          viewHeight,
          this.getStepListScrollbarX(),
          this.getStepListClipTop(),
          3,
          Math.max(1, listHeight - 2),
          this.scrollOffset
       );
    }

    private int contentInsetX() {
       return 8;
    }

   private int footerBottomInset() {
      return 16;
   }

   private int footerSectionGap() {
      return 12;
   }

   private int actionRowHeight() {
      return ACTION_HEIGHT;
   }

   private int defaultScreenInsetX() {
      return 16;
   }

   private int defaultScreenInsetY() {
      return 30;
   }

   private int editorButtonHeight() {
      return 14;
   }

   private int editorControlGap() {
      return 3;
   }

   private int deleteButtonWidth() {
      return 48;
   }

   private int textFieldHeight() {
      return 13;
   }

   private int secondRowGap() {
      return 15;
   }

   private int buttonTextPadding() {
      return 9;
   }

   private int runButtonWidth() {
      return 34;
   }

   private int lanButtonWidth() {
      return 28;
   }

   private int loopCountFieldWidth() {
      return 36;
   }

   private int loopCountFieldHeight() {
      return 13;
   }

   private int builderButtonGap() {
      return 2;
   }

   private int footerPrimaryButtonWidth() {
      return 49;
   }

   private int footerSecondaryButtonWidth() {
      return 44;
   }

   private int footerButtonGap() {
      return 3;
   }

   private int topRowOffset() {
      return 20;
   }

   private int getTopControlsBottomY() {
      return this.panelY + this.topRowOffset() + this.secondRowGap() + this.editorButtonHeight();
   }

   private int warningTextOffset() {
      return 32;
   }

   private int builderLabelTextOffset() {
      return 3;
   }

   private int stepsHeaderOffset() {
      return 9;
   }

   private int actionTextOffset() {
      return 7;
   }

   private int actionControlOffset() {
      return 5;
   }

   private int stepRowControlStride() {
      return this.stepRowControlSize() + this.stepRowControlGap();
   }

   private int stepRowTextY(int rowY) {
      return PackUiSizing.alignTextY(rowY, this.actionRowHeight(), this.theme.fontHeight(PackUiTone.BODY), this.theme.bodyTextNudge());
   }

   private int stepRowControlY(int rowY) {
      return PackUiSizing.alignMiddle(rowY, this.actionRowHeight(), this.stepRowControlSize());
   }

   private int stepRowStableId(MacroAction action) {
      if (action == null) {
         return -1;
      } else {
         Integer existing = this.stepRowStableIds.get(action);
         if (existing != null) {
            return existing;
         } else {
            int created = this.nextStepRowStableId++;
            this.stepRowStableIds.put(action, created);
            return created;
         }
      }
   }

   private List<PackUtilMacroEditorOverlay.StepRowSnapshot> captureCurrentStepRowSnapshots() {
      List<PackUtilMacroEditorOverlay.StepRowSnapshot> snapshots = new ArrayList<>();
      if (this.macro == null) {
         return snapshots;
      } else {
         for (int index = 0; index < this.macro.actions.size(); index++) {
            MacroAction action = this.macro.actions.get(index);
            snapshots.add(
               new PackUtilMacroEditorOverlay.StepRowSnapshot(
                  action, this.stepRowStableId(action), action.getDisplayName(), this.isConditionalAction(action), index
               )
            );
         }

         return snapshots;
      }
   }

   private PackUtilMacroEditorOverlay.StepRowSnapshot findStepRowSnapshot(List<PackUtilMacroEditorOverlay.StepRowSnapshot> snapshots, int stableId) {
      for (PackUtilMacroEditorOverlay.StepRowSnapshot snapshot : snapshots) {
         if (snapshot.stableId == stableId) {
            return snapshot;
         }
      }

      return null;
   }

   private PackUtilMacroEditorOverlay.StepRowMotion findStepRowMotion(int stableId) {
      for (PackUtilMacroEditorOverlay.StepRowMotion motion : this.stepRowMotions) {
         if (motion.stableId == stableId) {
            return motion;
         }
      }

      return null;
   }

   private void removeStepRowMotion(int stableId) {
      this.stepRowMotions.removeIf(motion -> motion.stableId == stableId);
   }

   private void syncStepRowAnimations(boolean animateChanges) {
      List<PackUtilMacroEditorOverlay.StepRowSnapshot> current = this.captureCurrentStepRowSnapshots();
      if (!animateChanges) {
         this.stepRowMotions.clear();
         this.lastStepRowSnapshots.clear();
         this.lastStepRowSnapshots.addAll(current);
         this.stepRowAnimationClockNanos = System.nanoTime();
         return;
      }

      this.stepRowMotions.clear();
      for (PackUtilMacroEditorOverlay.StepRowSnapshot snapshot : current) {
         PackUtilMacroEditorOverlay.StepRowSnapshot previous = this.findStepRowSnapshot(this.lastStepRowSnapshots, snapshot.stableId);
         if (previous == null) {
            this.removeStepRowMotion(snapshot.stableId);
            this.stepRowMotions.add(
               new PackUtilMacroEditorOverlay.StepRowMotion(
                  snapshot.stableId,
                  snapshot.label,
                  snapshot.conditional,
                  snapshot.index,
                  snapshot.index,
                  PackUtilMacroEditorOverlay.StepRowMotionKind.ENTER
               )
            );
         } else if (previous.index != snapshot.index) {
            this.removeStepRowMotion(snapshot.stableId);
            this.stepRowMotions.add(
               new PackUtilMacroEditorOverlay.StepRowMotion(
                  snapshot.stableId,
                  snapshot.label,
                  snapshot.conditional,
                  previous.index,
                  snapshot.index,
                  PackUtilMacroEditorOverlay.StepRowMotionKind.MOVE
               )
            );
         }
      }

      for (PackUtilMacroEditorOverlay.StepRowSnapshot snapshot : this.lastStepRowSnapshots) {
         if (this.findStepRowSnapshot(current, snapshot.stableId) == null) {
            this.removeStepRowMotion(snapshot.stableId);
            this.stepRowMotions.add(
               new PackUtilMacroEditorOverlay.StepRowMotion(
                  snapshot.stableId,
                  snapshot.label,
                  snapshot.conditional,
                  snapshot.index,
                  snapshot.index,
                  PackUtilMacroEditorOverlay.StepRowMotionKind.EXIT
               )
            );
         }
      }

      this.lastStepRowSnapshots.clear();
      this.lastStepRowSnapshots.addAll(current);
   }

   private void resetStepRowAnimationState() {
      this.stepRowStableIds.clear();
      this.lastStepRowSnapshots.clear();
      this.stepRowMotions.clear();
      this.nextStepRowStableId = 1;
      this.stepRowAnimationClockNanos = System.nanoTime();
      if (this.macro != null) {
         this.lastStepRowSnapshots.addAll(this.captureCurrentStepRowSnapshots());
      }
   }

   private float tickStepRowAnimations() {
      long now = System.nanoTime();
      float delta = Math.max(0.0F, Math.min(0.05F, (now - this.stepRowAnimationClockNanos) / 1_000_000_000.0F));
      this.stepRowAnimationClockNanos = now;
      if (this.stepRowMotions.isEmpty()) {
         return delta;
      } else {
         this.stepRowMotions.removeIf(motion -> {
            float duration = switch (motion.kind) {
               case ENTER -> STEP_ROW_ENTER_SECONDS;
               case MOVE -> STEP_ROW_MOVE_SECONDS;
               case EXIT -> STEP_ROW_EXIT_SECONDS;
            };
            motion.progress = Math.min(1.0F, motion.progress + (duration <= 0.0F ? 1.0F : delta / duration));
            return motion.progress >= 0.999F;
         });
         return delta;
      }
   }

   private float getStepRowEnterProgress(int stableId) {
      PackUtilMacroEditorOverlay.StepRowMotion motion = this.findStepRowMotion(stableId);
      if (motion != null && motion.kind == PackUtilMacroEditorOverlay.StepRowMotionKind.ENTER) {
         return this.easeOutCubic(motion.progress);
      } else {
         return 1.0F;
      }
   }

   private int getAnimatedStepRowY(PackUtilMacroEditorOverlay.StepRowMotion motion, int baseListY) {
      float eased = this.easeOutCubic(motion.progress);
      int fromY = baseListY + motion.fromRowIndex * this.actionRowHeight();
      int toY = baseListY + motion.toRowIndex * this.actionRowHeight();
      return Math.round(fromY + (toY - fromY) * eased);
   }

   private int getStepRowMoveOffsetX(PackUtilMacroEditorOverlay.StepRowMotion motion) {
      if (motion == null) {
         return 0;
      } else {
         float eased = this.easeOutCubic(motion.progress);
         return switch (motion.kind) {
            case ENTER -> Math.round((1.0F - eased) * (float)(-STEP_ROW_ENTER_OFFSET));
            case EXIT -> Math.round(eased * (float)STEP_ROW_EXIT_OFFSET);
            case MOVE -> 0;
         };
      }
   }

   private float getStepRowAlpha(PackUtilMacroEditorOverlay.StepRowMotion motion) {
      if (motion == null) {
         return 1.0F;
      } else {
         float eased = this.easeOutCubic(motion.progress);
         return switch (motion.kind) {
            case ENTER -> 0.45F + 0.55F * eased;
            case EXIT -> Math.max(0.0F, 1.0F - eased);
            case MOVE -> 1.0F;
         };
      }
   }

   private float easeOutCubic(float value) {
      float clamped = Math.max(0.0F, Math.min(1.0F, value));
      return 1.0F - (float)Math.pow(1.0F - clamped, 3.0);
   }

   private void clampStepListScrollOffset() {
      this.scrollOffset = this.quantizeScrollOffset(this.scrollOffset, this.actionRowHeight(), this.getMaxStepListScroll());
   }

   private int getDraggedRowTop() {
      return this.stepDragMouseY - this.stepDragGrabOffsetY;
   }

   private int getStepInsertSlotForTarget(int sourceIndex, int targetIndex) {
      if (sourceIndex < 0 || targetIndex < 0) {
         return -1;
      } else {
         return targetIndex > sourceIndex ? targetIndex + 1 : targetIndex;
      }
   }

   private int getShiftedVisualIndex(int index, int sourceIndex, int targetIndex) {
      if (sourceIndex < 0 || targetIndex < 0 || sourceIndex == targetIndex) {
         return index;
      } else if (sourceIndex < targetIndex) {
         return index > sourceIndex && index <= targetIndex ? index - 1 : index;
      } else {
         return index >= targetIndex && index < sourceIndex ? index + 1 : index;
      }
   }

   private void beginStepDragPress(int index, double mouseX, double mouseY) {
      this.pressedStepIndex = index;
      this.stepDragTargetIndex = index;
      this.stepDragGrabOffsetY = (int)Math.round(mouseY) - (this.getStepListY() - this.scrollOffset + index * this.actionRowHeight());
      this.stepDragMouseY = (int)Math.round(mouseY);
      this.stepPressMouseX = mouseX;
      this.stepPressMouseY = mouseY;
   }

   private void tryStartStepDrag(double mouseX, double mouseY) {
      if (this.draggingIndex < 0 && this.pressedStepIndex >= 0) {
         boolean movedEnough = Math.abs(mouseX - this.stepPressMouseX) >= STEP_DRAG_THRESHOLD
            || Math.abs(mouseY - this.stepPressMouseY) >= STEP_DRAG_THRESHOLD;
         if (movedEnough) {
            this.draggingIndex = this.pressedStepIndex;
            this.stepDragTargetIndex = this.pressedStepIndex;
            this.stepDragMouseY = (int)Math.round(mouseY);
         }
      }
   }

   private void updateStepDrag(double mouseY) {
      if (this.draggingIndex >= 0 && this.macro != null && !this.macro.actions.isEmpty()) {
         this.stepDragMouseY = (int)Math.round(mouseY);
         this.applyStepDragAutoScroll(mouseY);
         double draggedRowTop = mouseY - this.stepDragGrabOffsetY;
         double draggedRowCenter = draggedRowTop - this.getStepListY() + this.scrollOffset + this.actionRowHeight() / 2.0;
         this.stepDragTargetIndex = this.computeStepDragTargetIndex(draggedRowCenter);
      }
   }

   private int computeStepDragTargetIndex(double draggedRowCenter) {
      if (this.draggingIndex < 0 || this.macro == null || this.macro.actions.isEmpty()) {
         return -1;
      } else {
         int rowHeight = this.actionRowHeight();
         int targetIndex = this.draggingIndex;
         double sourceCenter = this.draggingIndex * rowHeight + rowHeight / 2.0;
         if (draggedRowCenter < sourceCenter) {
            while (targetIndex > 0) {
               double previousCenter = (targetIndex - 1) * rowHeight + rowHeight / 2.0;
               if (draggedRowCenter < previousCenter) {
                  --targetIndex;
               } else {
                  break;
               }
            }
         } else if (draggedRowCenter > sourceCenter) {
            int maxIndex = this.macro.actions.size() - 1;

            while (targetIndex < maxIndex) {
               double nextCenter = (targetIndex + 1) * rowHeight + rowHeight / 2.0;
               if (draggedRowCenter > nextCenter) {
                  ++targetIndex;
               } else {
                  break;
               }
            }
         }

         return targetIndex;
      }
   }

   private void applyStepDragAutoScroll(double mouseY) {
      int maxScroll = this.getMaxStepListScroll();
      if (maxScroll > 0) {
         int listTop = this.getStepListY();
         int listBottom = listTop + this.getStepListHeight();
         int delta = 0;
         if (mouseY < listTop + STEP_DRAG_AUTO_SCROLL_EDGE) {
            double depth = listTop + STEP_DRAG_AUTO_SCROLL_EDGE - mouseY;
            delta = -Math.min(STEP_DRAG_AUTO_SCROLL_MAX_SPEED, Math.max(1, (int)Math.ceil(depth / 3.0)));
         } else if (mouseY > listBottom - STEP_DRAG_AUTO_SCROLL_EDGE) {
            double depth = mouseY - (listBottom - STEP_DRAG_AUTO_SCROLL_EDGE);
            delta = Math.min(STEP_DRAG_AUTO_SCROLL_MAX_SPEED, Math.max(1, (int)Math.ceil(depth / 3.0)));
         }

         if (delta != 0) {
            this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset + delta));
         }
      }
   }

   private void clearStepDragState() {
      this.draggingIndex = -1;
      this.pressedStepIndex = -1;
      this.stepDragTargetIndex = -1;
      this.stepDragGrabOffsetY = 0;
      this.stepDragMouseY = 0;
      this.stepPressMouseX = 0.0;
      this.stepPressMouseY = 0.0;
   }

   private void moveAction(int fromIndex, int toIndex) {
      if (this.macro != null && fromIndex >= 0 && toIndex >= 0 && fromIndex < this.macro.actions.size() && toIndex < this.macro.actions.size() && fromIndex != toIndex) {
         MacroAction moved = this.macro.actions.remove(fromIndex);
         int targetIndex = Math.max(0, Math.min(toIndex, this.macro.actions.size()));
         this.macro.actions.add(targetIndex, moved);
         this.handleMacroActionsChanged();
      }
   }

   private void finishStepDrag() {
      int sourceIndex = this.draggingIndex;
      int targetIndex = this.stepDragTargetIndex;
      this.clearStepDragState();
      if (this.macro != null && sourceIndex >= 0 && targetIndex >= 0 && sourceIndex != targetIndex) {
         this.pushStructuralHistoryStep();
         this.moveAction(sourceIndex, targetIndex);
      }
   }

   private int getStepPickerWidth() {
      return this.getStepPickerViewportWidth() + 12;
   }

   private int getStepPickerViewportWidth() {
      int screenW = MC.getWindow() != null ? PackUtilUiScale.getVirtualScreenWidth() : 320;
      int screenH = MC.getWindow() != null ? PackUtilUiScale.getVirtualScreenHeight() : 320;
      int minViewportWidth = 118;
      int maxViewportWidth = Math.max(minViewportWidth, Math.min(220, screenW - 20));
      int maxContentHeight = Math.max(80, screenH - 36);
      List<PackUtilMacroEditorOverlay.StepPickerSectionLayout> fallbackLayouts = null;
      int fallbackOverflow = Integer.MAX_VALUE;
      int fallbackWidth = maxViewportWidth;

      for (int candidateWidth = minViewportWidth; candidateWidth <= maxViewportWidth; candidateWidth += 6) {
         List<PackUtilMacroEditorOverlay.StepPickerSectionLayout> layouts = this.buildStepPickerLayouts(0, 0, candidateWidth);
         int contentHeight = this.getStepPickerContentHeight(layouts);
         if (contentHeight <= maxContentHeight) {
            return this.widenStepPickerViewportWidth(this.getStepPickerUsedWidth(layouts), minViewportWidth, maxViewportWidth);
         }

         int overflow = contentHeight - maxContentHeight;
         if (overflow < fallbackOverflow) {
            fallbackOverflow = overflow;
            fallbackLayouts = layouts;
            fallbackWidth = candidateWidth;
         }
      }

      return fallbackLayouts != null
         ? this.widenStepPickerViewportWidth(this.getStepPickerUsedWidth(fallbackLayouts), minViewportWidth, fallbackWidth)
         : maxViewportWidth;
   }

   private int widenStepPickerViewportWidth(int width, int minViewportWidth, int maxViewportWidth) {
      int widenedWidth = width + 8;
      return Math.max(minViewportWidth, Math.min(maxViewportWidth, widenedWidth));
   }

   private int getStepPickerContentHeight() {
      List<PackUtilMacroEditorOverlay.StepPickerSectionLayout> layouts = this.buildStepPickerLayouts(0, 0, this.getStepPickerViewportWidth());
      return this.getStepPickerContentHeight(layouts);
   }

   private int getStepPickerHeight() {
      int screenH = MC.getWindow() != null ? PackUtilUiScale.getVirtualScreenHeight() : 320;
      int desiredHeight = 28 + this.getStepPickerContentHeight();
      return Math.max(78, Math.min(desiredHeight, screenH - 8));
   }

   private int getStepPickerX() {
      int pickerW = this.getStepPickerWidth();
      int screenW = MC.getWindow() != null ? PackUtilUiScale.getVirtualScreenWidth() : this.panelX + this.PANEL_WIDTH + pickerW + 8;
      if (this.panelX + this.PANEL_WIDTH + pickerW + 5 <= screenW) {
         return this.panelX + this.PANEL_WIDTH + 5;
      } else {
         return this.panelX - pickerW - 5 >= 0
            ? this.panelX - pickerW - 5
            : Math.max(4, Math.min(screenW - pickerW - 4, this.panelX + (this.PANEL_WIDTH - pickerW) / 2));
      }
   }

   private int getStepPickerY() {
      int pickerH = this.getStepPickerHeight();
      int screenH = MC.getWindow() != null ? PackUtilUiScale.getVirtualScreenHeight() : this.panelY + pickerH + 8;
      int preferredY = this.panelY + 42;
      return Math.max(4, Math.min(screenH - pickerH - 4, preferredY));
   }

   private int getStepPickerCloseX() {
      return this.getStepPickerX() + this.getStepPickerWidth() - 16;
   }

     private int getStepPickerCloseY() {
        return PackUiHeaderControls.controlY(this.getStepPickerY(), 16, 12);
     }

    private PackUiScrollbar.Metrics getStepPickerScrollbarMetrics() {
      int pickerX = this.getStepPickerX();
      int pickerY = this.getStepPickerY();
      int pickerW = this.getStepPickerWidth();
      int pickerH = this.getStepPickerHeight();
      int gridX = pickerX + 6;
      int gridY = pickerY + 22;
      int gridW = pickerW - 12;
      int gridH = pickerH - 28;
      return PackUiScrollbar.compute(this.getStepPickerContentHeight(), gridH, gridX + gridW - 5, gridY, 3, gridH, this.stepPickerScrollOffset);
   }

   private boolean isMouseOverStepPicker(int mouseX, int mouseY) {
      if (!this.isStepPickerOpen()) {
         return false;
      } else {
         int pickerX = this.getStepPickerX();
         int pickerY = this.getStepPickerY();
         int pickerW = this.getStepPickerWidth();
         int pickerH = this.getStepPickerHeight();
         return mouseX >= pickerX && mouseX < pickerX + pickerW && mouseY >= pickerY && mouseY < pickerY + pickerH;
      }
   }

   private boolean isStepPickerOpen() {
      return this.stepPickerMode != null;
   }

   private void closeStepPicker() {
      this.stepPickerMode = null;
      this.stepPickerCategories = Collections.emptyList();
      this.stepPickerEntries = Collections.emptyList();
      this.stepPickerScrollOffset = 0;
   }

   private void addStepPickerEntry(
      List<PackUtilMacroEditorOverlay.StepPickerEntry> entries, String categoryId, String label, String description, Runnable action
   ) {
      entries.add(new PackUtilMacroEditorOverlay.StepPickerEntry(categoryId, label, description, action));
   }

   private void addDefaultRotateAction() {
      if (MC.player != null) {
         this.addAction(new RotateAction(MC.player.getYRot(), MC.player.getXRot(), false, 6));
      } else {
         this.addAction(new RotateAction(0.0F, 0.0F, false, 6));
      }
   }

   private void addDefaultLookAtAction() {
      LookAtBlockAction action = new LookAtBlockAction();
      if (MC.hitResult != null && MC.hitResult.getType() == Type.ENTITY && MC.crosshairPickEntity != null) {
         action.targetMode = LookAtBlockAction.TargetMode.ENTITY;
         action.entityIds.add(LookAtBlockAction.toSpecificEntityEntry(MC.crosshairPickEntity));
      } else if (MC.hitResult != null && MC.hitResult.getType() == Type.BLOCK) {
         BlockHitResult hit = (BlockHitResult)MC.hitResult;
         action.targetMode = LookAtBlockAction.TargetMode.SPECIFIC;
         action.blockX = hit.getBlockPos().getX();
         action.blockY = hit.getBlockPos().getY();
         action.blockZ = hit.getBlockPos().getZ();
      }

      this.addAction(action);
   }

   private boolean hasActionEditor(MacroAction action) {
      return ActionEditorOverlay.supportsActionEditor(action);
   }

   private boolean isConditionalAction(MacroAction action) {
      return action instanceof WaitForHealthAction
         || action instanceof WaitForBlockAction
         || action instanceof WaitForPacketAction
         || action instanceof WaitForGuiAction
         || action instanceof TickSyncAction
         || action instanceof RevisionSyncAction
         || action instanceof ServerTickSyncAction
         || action instanceof WaitForCooldownAction
         || action instanceof WaitPosAction
         || action instanceof WaitForChatAction
         || action instanceof WaitForEntityAction
         || action instanceof WaitForSlotChangeAction
         || action instanceof WaitForSoundAction;
   }

   private void addDefaultOpenContainerAction() {
      OpenContainerAction action = new OpenContainerAction();
      if (MC.hitResult instanceof BlockHitResult hit) {
         action.blockPos = hit.getBlockPos();
      }

      this.addAction(action);
   }

   private void addDefaultInventoryAction() {
      InventoryAction action = new InventoryAction();
      action.mode = InventoryAction.InvMode.OPEN;
      this.addAction(action);
   }

   private void addDefaultDelayPacketsAction() {
      DelayPacketsAction action = new DelayPacketsAction();
      if (PackUtilSharedState.get().shouldUseCustomPackets()) {
         action.applyModulePreset();
      } else {
         action.applyDefaultPreset();
      }

      this.addAction(action);
   }

   private void addDefaultWaitBlockAction() {
      WaitForBlockAction action = new WaitForBlockAction();
      if (MC.hitResult != null && MC.hitResult.getType() == Type.BLOCK) {
         BlockHitResult hit = (BlockHitResult)MC.hitResult;
         action.blockPos = hit.getBlockPos();
      }

      this.addAction(action);
   }

   private void addDefaultWaitEntityAction() {
      WaitForEntityAction action = new WaitForEntityAction();
      if (MC.player != null) {
         action.x = Math.round(MC.player.getX());
         action.y = Math.round(MC.player.getY());
         action.z = Math.round(MC.player.getZ());
      }

      this.addAction(action);
   }

   private void addDefaultInstaBreakAction() {
      InstaBreakAction action = new InstaBreakAction();
      if (MC.hitResult != null && MC.hitResult.getType() == Type.BLOCK) {
         BlockHitResult hit = (BlockHitResult)MC.hitResult;
         action.blockPos = hit.getBlockPos();
         action.direction = hit.getDirection();
      }

      this.addAction(action);
   }

   private List<PackUtilMacroEditorOverlay.StepPickerEntry> buildStepPickerEntries(PackUtilMacroEditorOverlay.StepPickerMode mode) {
      List<PackUtilMacroEditorOverlay.StepPickerEntry> entries = new ArrayList<>();
      if (mode == PackUtilMacroEditorOverlay.StepPickerMode.ACTION) {
         this.addStepPickerEntry(entries, "flow", "Chat", "Send a chat message or slash command.", () -> this.addAction(new SendChatAction()));
         this.addStepPickerEntry(entries, "flow", "Delay", "Pause for a time in ms or ticks.", () -> this.addAction(new DelayAction(1000)));
         this.addStepPickerEntry(entries, "flow", "Repeat", "Repeat the next steps a chosen number of times.", () -> this.addAction(new RepeatAction()));
         this.addStepPickerEntry(entries, "flow", "Stop", "Stop the macro immediately.", () -> this.addAction(new StopMacroAction()));
         this.addStepPickerEntry(entries, "flow", "Send Toggle", "Turn packet sending on or off.", () -> this.addAction(new SendToggleAction()));
         this.addStepPickerEntry(entries, "flow", "Delay Packets", "Queue packets until you flush them.", this::addDefaultDelayPacketsAction);
         this.addStepPickerEntry(entries, "flow", "Save GUI", "Remember the current GUI for later restore.", () -> this.addAction(new SaveGuiAction()));
         this.addStepPickerEntry(entries, "flow", "Restore GUI", "Reopen the last saved GUI state.", () -> this.addAction(new RestoreGuiAction()));
         this.addStepPickerEntry(entries, "movement", "Rotate", "Set player yaw and pitch.", this::addDefaultRotateAction);
         this.addStepPickerEntry(entries, "movement", "Look At", "Face a specific position, nearest block, or nearest entity.", this::addDefaultLookAtAction);
         this.addStepPickerEntry(entries, "movement", "Sneak", "Hold or release sneak.", () -> this.addAction(new SneakAction()));
         this.addStepPickerEntry(entries, "movement", "Jump", "Press jump for a chosen duration.", () -> this.addAction(new JumpAction()));
         this.addStepPickerEntry(entries, "movement", "Sprint", "Toggle sprint state.", () -> this.addAction(new SprintAction()));
         this.addStepPickerEntry(entries, "movement", "Move", "Walk in a direction for N ticks.", () -> this.addAction(new MoveAction()));
         if (PackUtilCompatManager.isBaritoneAvailable()) {
            this.addStepPickerEntry(entries, "movement", "Go To", "Use Baritone to path to a target.", () -> this.addAction(new GoToAction()));
            this.addStepPickerEntry(entries, "movement", "Mine", "Mine blocks with Baritone rules.", () -> this.addAction(new MineAction()));
         }

         this.addStepPickerEntry(entries, "inventory", "Item Click", "Click configured inventory slots.", () -> this.addAction(new ItemAction()));
         this.addStepPickerEntry(entries, "inventory", "Use Item", "Use the held item or select one by name.", () -> this.addAction(new UseItemAction()));
         this.addStepPickerEntry(entries, "inventory", "Open Inventory", "Open the player inventory screen.", this::addDefaultInventoryAction);
         this.addStepPickerEntry(entries, "inventory", "Slot", "Select a hotbar slot or item by name.", () -> this.addAction(new SelectSlotAction()));
         this.addStepPickerEntry(entries, "inventory", "XCarry", "Store up to four items in the player crafting grid.", () -> this.addAction(new XCarryAction()));
         this.addStepPickerEntry(entries, "inventory", "Drop", "Drop items from chosen slots.", () -> this.addAction(new DropAction()));
         this.addStepPickerEntry(entries, "inventory", "Swap", "Swap two inventory slots.", () -> this.addAction(new SwapSlotsAction()));
         this.addStepPickerEntry(entries, "inventory", "Open Container", "Open a targeted container block.", this::addDefaultOpenContainerAction);
         this.addStepPickerEntry(entries, "inventory", "Store", "Move items between player and containers.", () -> this.addAction(new StoreItemAction()));
         this.addStepPickerEntry(entries, "inventory", "Inventory Audit", "Snapshot slots, compare after reopen, or run automated dupe tests.", () -> this.addAction(new InventoryAuditAction()));
         this.addStepPickerEntry(entries, "inventory", "Craft", "Craft one or more recipes.", () -> this.addAction(new CraftAction()));
         this.addStepPickerEntry(
            entries, "inventory", "Mouse Click", "Simulate mouse clicks in world.", () -> this.addAction(new ClickAction(ClickAction.ContainerInput.RIGHT))
         );
         this.addStepPickerEntry(entries, "network", "Packet", "Send captured packets from the queue.", () -> this.addAction(new SendPacketAction()));
         this.addStepPickerEntry(entries, "network", "Payload", "Send and edit custom payload packets with raw bytes or runtime Java.", () -> this.addAction(new PayloadAction()));
         this.addStepPickerEntry(
            entries, "network", "Close GUI", "Close the current screen with optional no-packet mode.", () -> this.addAction(new CloseGuiAction())
         );
         this.addStepPickerEntry(entries, "network", "Desync", "Create GUI desync without closing locally.", () -> this.addAction(new DesyncAction()));
         this.addStepPickerEntry(entries, "network", "NBT Book", "Write large custom books.", () -> this.addAction(new NbtBookAction()));
         this.addStepPickerEntry(entries, "network", "Disconnect", "Disconnect or kick using selected mode.", () -> this.addAction(new DisconnectAction()));
         if (PackUtilCompatManager.isMeteorAvailable()) {
            this.addStepPickerEntry(entries, "automation", "Module", "Toggle a Meteor module.", () -> this.addAction(new ToggleModuleAction()));
         }

         this.addStepPickerEntry(entries, "automation", "Pay", "Send payments to a player list.", () -> this.addAction(new PayAction()));
         this.addStepPickerEntry(entries, "automation", "InstaBreak", "Meteor-style instant rebreak for one captured block.", this::addDefaultInstaBreakAction);
         this.addStepPickerEntry(entries, "automation", "Start Macro", "Start another macro from the macro library.", () -> this.addAction(new StartMacroAction()));
         this.addStepPickerEntry(entries, "automation", "Stop Macro", "Stop this, all, or a selected macro.", () -> this.addAction(new StopMacroAction()));
      } else {
         this.addStepPickerEntry(
            entries, "player", "Wait Health", "Pause until your health drops below or rises above a target.", () -> this.addAction(new WaitForHealthAction(20.0F, true))
         );
         this.addStepPickerEntry(
            entries, "player", "Wait Cooldown", "Pause until an item cooldown is ready.", () -> this.addAction(new WaitForCooldownAction())
         );
         this.addStepPickerEntry(
            entries, "player", "Wait Slot or Item", "Pause until slots or inventory contents change.", () -> this.addAction(new WaitForSlotChangeAction())
         );
         this.addStepPickerEntry(entries, "world", "Wait Position", "Pause until you reach a position or rotation.", () -> this.addAction(new WaitPosAction()));
         this.addStepPickerEntry(entries, "world", "Wait Block", "Pause until a block is placed or broken.", this::addDefaultWaitBlockAction);
         this.addStepPickerEntry(entries, "world", "Wait Entity", "Pause until a matching entity appears.", this::addDefaultWaitEntityAction);
         this.addStepPickerEntry(entries, "world", "Wait Sound", "Pause until a matching sound plays.", () -> this.addAction(new WaitForSoundAction()));
         this.addStepPickerEntry(entries, "events", "WaitGUI", "Pause until a GUI opens or closes.", () -> this.addAction(new WaitForGuiAction()));
         this.addStepPickerEntry(entries, "events", "Wait Chat", "Pause until chat matches a pattern.", () -> this.addAction(new WaitForChatAction()));
         this.addStepPickerEntry(entries, "events", "Wait LAN", "Pause until a LAN peer reaches a step.", () -> this.addAction(new WaitForLanStepAction()));
         this.addStepPickerEntry(entries, "events", "Wait Macro", "Pause until another macro starts, completes a step, or finishes.", () -> this.addAction(new WaitForMacroStepAction()));
         this.addStepPickerEntry(
            entries, "sync", "Wait Packet", "Pause until a chosen C2S or S2C packet appears.", () -> this.addAction(new WaitForPacketAction(""))
         );
         this.addStepPickerEntry(entries, "sync", "Tick Sync", "Wait for the next client tick.", () -> this.addAction(new TickSyncAction()));
         this.addStepPickerEntry(entries, "sync", "Revision Sync", "Wait for handler revision sync.", () -> this.addAction(new RevisionSyncAction()));
         this.addStepPickerEntry(entries, "sync", "Server Sync", "Wait for the server tick tracker.", () -> this.addAction(new ServerTickSyncAction()));
      }

      return entries;
   }

   private List<PackUtilMacroEditorOverlay.StepPickerCategory> buildStepPickerCategories(
      PackUtilMacroEditorOverlay.StepPickerMode mode, List<PackUtilMacroEditorOverlay.StepPickerEntry> entries
   ) {
      List<PackUtilMacroEditorOverlay.StepPickerCategory> categories = new ArrayList<>();
      Consumer<PackUtilMacroEditorOverlay.StepPickerCategory> addCategory = category -> {
         for (PackUtilMacroEditorOverlay.StepPickerEntry entry : entries) {
            if (entry.categoryId.equals(category.id)) {
               categories.add(category);
               return;
            }
         }
      };
      if (mode == PackUtilMacroEditorOverlay.StepPickerMode.ACTION) {
         addCategory.accept(new PackUtilMacroEditorOverlay.StepPickerCategory("flow", "Flow", -11352981));
         addCategory.accept(new PackUtilMacroEditorOverlay.StepPickerCategory("movement", "Movement", -42406));
         addCategory.accept(new PackUtilMacroEditorOverlay.StepPickerCategory("inventory", "Inventory", -11182));
         addCategory.accept(new PackUtilMacroEditorOverlay.StepPickerCategory("network", "Network", -10835457));
         addCategory.accept(new PackUtilMacroEditorOverlay.StepPickerCategory("automation", "Automation", -4555521));
      } else {
         addCategory.accept(new PackUtilMacroEditorOverlay.StepPickerCategory("player", "Player", -42406));
         addCategory.accept(new PackUtilMacroEditorOverlay.StepPickerCategory("world", "World", -11352981));
         addCategory.accept(new PackUtilMacroEditorOverlay.StepPickerCategory("events", "Events", -11182));
         addCategory.accept(new PackUtilMacroEditorOverlay.StepPickerCategory("sync", "Sync", -10835457));
      }

      return categories;
   }

   private void openStepPicker(PackUtilMacroEditorOverlay.StepPickerMode mode) {
      if (mode != null) {
         this.clearStepDragState();
         this.stepPickerMode = mode;
         this.stepPickerEntries = this.buildStepPickerEntries(mode);
         this.stepPickerCategories = this.buildStepPickerCategories(mode, this.stepPickerEntries);
         this.stepPickerScrollOffset = 0;
         this.isBindingKey = false;
         if (this.nameField != null) {
            this.nameField.setFocused(false);
         }

         if (this.loopCountField != null) {
            this.loopCountField.setFocused(false);
         }
      }
   }

   private List<PackUtilMacroEditorOverlay.StepPickerEntry> getVisibleStepPickerEntries() {
      if (!this.isStepPickerOpen()) return Collections.emptyList();
      return new ArrayList<>(this.stepPickerEntries);
   }

   private List<PackUtilMacroEditorOverlay.StepPickerEntry> getStepPickerEntriesForCategory(String categoryId) {
      List<PackUtilMacroEditorOverlay.StepPickerEntry> filtered = new ArrayList<>();
      if (categoryId != null && !categoryId.isEmpty()) {
         for (PackUtilMacroEditorOverlay.StepPickerEntry entry : this.stepPickerEntries) {
            if (categoryId.equals(entry.categoryId)) {
               filtered.add(entry);
            }
         }

         return filtered;
      } else {
         return filtered;
      }
   }

   private List<PackUtilMacroEditorOverlay.StepPickerSectionLayout> buildStepPickerLayouts(int gridX, int startY, int gridW) {
      int cardGap = 4;
      int rowGap = 3;
      int cardH = 16;
      int sectionLabelH = this.stepPickerSectionLabelHeight();
      int sectionGap = 5;
      int labelPadding = 10;
      int minButtonW = 22;
      Identifier pickerFont = this.theme.fontFor(PackUiTone.BODY);
      int pickerColor = this.theme.color(PackUiTone.BODY);
      List<PackUtilMacroEditorOverlay.StepPickerSectionLayout> layouts = new ArrayList<>();
      int sectionY = startY;

      for (PackUtilMacroEditorOverlay.StepPickerCategory category : this.stepPickerCategories) {
         List<PackUtilMacroEditorOverlay.StepPickerEntry> categoryEntries = this.getStepPickerEntriesForCategory(category.id);
         if (!categoryEntries.isEmpty()) {
            List<PackUtilMacroEditorOverlay.StepPickerButtonLayout> buttons = new ArrayList<>();
            int buttonX = gridX;
            int buttonY = sectionY + sectionLabelH + 2;

            for (PackUtilMacroEditorOverlay.StepPickerEntry entry : categoryEntries) {
               int buttonW = Math.max(minButtonW, Math.min(gridW, PackUiText.width(this.textRenderer, entry.label, pickerFont, pickerColor) + labelPadding));
               if (!buttons.isEmpty() && buttonX + buttonW > gridX + gridW) {
                  buttonX = gridX;
                  buttonY += cardH + rowGap;
               }

               buttons.add(new PackUtilMacroEditorOverlay.StepPickerButtonLayout(entry, buttonX, buttonY, buttonW, cardH));
               buttonX += buttonW + cardGap;
            }

            int bottomY = buttons.isEmpty() ? sectionY + sectionLabelH : buttons.get(buttons.size() - 1).y + cardH;
            layouts.add(new PackUtilMacroEditorOverlay.StepPickerSectionLayout(category, sectionY, buttons, bottomY));
            sectionY = bottomY + sectionGap;
         }
      }

      return layouts;
   }

   private int getStepPickerContentHeight(List<PackUtilMacroEditorOverlay.StepPickerSectionLayout> layouts) {
      return layouts != null && !layouts.isEmpty() ? Math.max(0, layouts.get(layouts.size() - 1).bottomY) : 0;
   }

   private int getStepPickerUsedWidth(List<PackUtilMacroEditorOverlay.StepPickerSectionLayout> layouts) {
      if (layouts != null && !layouts.isEmpty()) {
         int maxRight = 0;
         int minLeft = Integer.MAX_VALUE;
         Identifier pickerFont = this.theme.fontFor(PackUiTone.LABEL);
         int pickerColor = this.theme.color(PackUiTone.BODY);

         for (PackUtilMacroEditorOverlay.StepPickerSectionLayout section : layouts) {
            minLeft = Math.min(minLeft, 0);
            maxRight = Math.max(maxRight, PackUiText.width(this.textRenderer, section.category.label, pickerFont, pickerColor));

            for (PackUtilMacroEditorOverlay.StepPickerButtonLayout button : section.buttons) {
               minLeft = Math.min(minLeft, button.x);
               maxRight = Math.max(maxRight, button.x + button.width);
            }
         }

         if (minLeft == Integer.MAX_VALUE) {
            minLeft = 0;
         }

         return Math.max(118, maxRight - minLeft);
      } else {
         return 118;
      }
   }

   @Override
   public int getMinWidth() {
      return this.defaultPanelWidth();
   }

   @Override
   public int getMinHeight() {
      return this.minimumPanelHeight();
   }

   @Override
   public PackUtilWindowLayout getBounds() {
      return new PackUtilWindowLayout(this.panelX, this.panelY, this.PANEL_WIDTH, this.PANEL_HEIGHT, this.visible, this.collapsed);
   }

   @Override
   public void setBounds(PackUtilWindowLayout bounds) {
      if (bounds != null) {
         int requestedWidth = bounds.width == 408 ? this.defaultPanelWidth() : bounds.width;
         PackUtilWindowLayout clamped = this.clampToScreen(
            this, new PackUtilWindowLayout(bounds.x, bounds.y, requestedWidth, bounds.height, bounds.visible, bounds.collapsed)
         );
         this.panelX = clamped.x;
         this.panelY = clamped.y;
         this.PANEL_WIDTH = clamped.width;
         this.PANEL_HEIGHT = clamped.height;
         this.visible = clamped.visible;
         this.collapsed = clamped.collapsed;
         if (this.macro != null) {
            this.recreateComponents();
         }
      }
   }

   private CompoundTag captureMacroSnapshot() {
      return this.macro == null ? null : this.macro.toTag();
   }

   private static boolean snapshotsEqual(CompoundTag a, CompoundTag b) {
      if (a == b) {
         return true;
      } else {
         return a != null && b != null ? a.equals(b) : false;
      }
   }

   private void pushSnapshot(Deque<CompoundTag> history, CompoundTag snapshot) {
      if (history != null && snapshot != null) {
         CompoundTag head = history.peekFirst();
         if (!snapshotsEqual(head, snapshot)) {
            history.addFirst(snapshot);

            while (history.size() > 30) {
               history.removeLast();
            }
         }
      }
   }

   private void resetHistoryState() {
      this.undoHistory.clear();
      this.redoHistory.clear();
   }

   private void pushStructuralHistoryStep() {
      this.pushSnapshot(this.undoHistory, this.captureMacroSnapshot());
      this.redoHistory.clear();
   }

   private boolean canUndoHistory() {
      return this.macro != null && !this.undoHistory.isEmpty();
   }

   private boolean canRedoHistory() {
      return this.macro != null && !this.redoHistory.isEmpty();
   }

   private void restoreHistorySnapshot(CompoundTag snapshot) {
      if (this.macro != null && snapshot != null) {
         this.macro.fromTag(snapshot);
         this.handleMacroActionsChanged();
      }
   }

   private void performUndo() {
      if (this.canUndoHistory()) {
         CompoundTag previous = this.undoHistory.pollFirst();
         if (previous != null) {
            this.pushSnapshot(this.redoHistory, this.captureMacroSnapshot());
            this.restoreHistorySnapshot(previous);
         }
      }
   }

   private void performRedo() {
      if (this.canRedoHistory()) {
         CompoundTag next = this.redoHistory.pollFirst();
         if (next != null) {
            this.pushSnapshot(this.undoHistory, this.captureMacroSnapshot());
            this.restoreHistorySnapshot(next);
         }
      }
   }

   public void open(PackUtilMacro macroToEdit) {
      this.open(macroToEdit, false);
   }

   public void open(PackUtilMacro macroToEdit, boolean reopenMacroListOnClose) {
      if (this.visible) {
         this.closeStepPicker();
         this.isBindingKey = false;
      }

      for (IPackUtilOverlay overlay : PackUtilOverlayManager.get().getOverlays()) {
         if (overlay instanceof PackUtilMacroListOverlay macroListOverlay) {
            macroListOverlay.setVisible(false);
         }
      }

      this.originalMacro = null;
      this.macro = null;
      this.reopenMacroListOnClose = reopenMacroListOnClose;
      this.visible = true;
      PackUtilSharedState.get().setMacroEditorVisible(true);
      if (macroToEdit == null) {
         this.macro = new PackUtilMacro("");
         this.originalMacro = null;
         this.isNew = true;
      } else {
         this.originalMacro = macroToEdit;
         this.macro = new PackUtilMacro();
         this.macro.fromTag(macroToEdit.toTag());
         this.isNew = false;
      }

      PackUtilSharedState.get().setEditingMacro(this.macro);
      this.tempSavedForRun = false;
      this.originalMacroSnapshot = null;
      this.resetHistoryState();
      this.clearStepDragState();
      this.scrollOffset = 0;
      if (!this.macro.loop) {
         this.loopMode = 0;
      } else if (this.macro.loopCount == -1) {
         this.loopMode = 2;
      } else {
         this.loopMode = 1;
      }

      this.resetStepRowAnimationState();
      this.init();
      this.windowNode.restoreShowBody(!this.collapsed);
      PackUtilOverlayManager.get().bringToFront(this);
   }

   public void cancel() {
      this.stopTemporaryEditedMacro();
      if (this.tempSavedForRun) {
         if (this.originalMacro == null) {
            PackUtilMacro savedMacro = PackUtilMacroManager.get().get(this.macro.name);
            if (savedMacro != null) {
               PackUtilMacroManager.get().getAll().remove(savedMacro);
               PackUtilMacroManager.get().save();
            }
         } else if (this.originalMacroSnapshot != null) {
            this.originalMacro.fromTag(this.originalMacroSnapshot);
            PackUtilMacroManager.get().save();
         }
      }

      this.close();
   }

   private void stopTemporaryEditedMacro() {
      PackUtilMacro runningMacro = this.getRunningEditedMacro();
      if (runningMacro != null) {
         if (this.tempSavedForRun || !this.isPersistedMacro(runningMacro)) {
            MacroExecutor.stopMacro(runningMacro);
            return;
         }
      }
      if (this.macro != null && this.macro.name != null && !this.macro.name.isBlank()) {
         PackUtilMacro persisted = PackUtilMacroManager.get().get(this.macro.name);
         if (this.tempSavedForRun || persisted == null) {
            MacroExecutor.stopMacro(this.macro.name);
         }
      }
      if (this.originalMacro != null && this.originalMacro.name != null && !this.originalMacro.name.isBlank()) {
         PackUtilMacro persisted = PackUtilMacroManager.get().get(this.originalMacro.name);
         if (this.tempSavedForRun || persisted == null) {
            MacroExecutor.stopMacro(this.originalMacro.name);
         }
      }
   }

   private boolean isPersistedMacro(PackUtilMacro candidate) {
      if (candidate == null || candidate.name == null || candidate.name.isBlank()) return false;
      PackUtilMacro persisted = PackUtilMacroManager.get().get(candidate.name);
      return persisted == candidate;
   }

   public void close() {
      boolean shouldReopenMacroList = this.reopenMacroListOnClose;
      this.visible = false;
      PackUtilSharedState.get().setMacroEditorVisible(false);
      PackUtilSharedState.get().setEditingMacro(null);
      this.macro = null;
      this.originalMacro = null;
      this.tempSavedForRun = false;
      this.originalMacroSnapshot = null;
      this.reopenMacroListOnClose = false;
      this.closeStepPicker();
      this.isBindingKey = false;
      this.resetHistoryState();
      this.clearStepDragState();
      this.resetStepRowAnimationState();

      if (shouldReopenMacroList) {
         for (IPackUtilOverlay overlay : PackUtilOverlayManager.get().getOverlays()) {
            if (overlay instanceof PackUtilMacroListOverlay macroListOverlay) {
               macroListOverlay.setVisible(true);
               PackUtilOverlayManager.get().bringToFront(macroListOverlay);
               break;
            }
         }
      }
   }

   private boolean isEditingRunningMacro() {
      return this.getRunningEditedMacro() != null;
   }

   private PackUtilMacro getRunningEditedMacro() {
      if (this.originalMacro != null && MacroExecutor.isMacroRunning(this.originalMacro)) return this.originalMacro;
      if (this.macro != null && MacroExecutor.isMacroRunning(this.macro)) return this.macro;
      if (this.macro != null && this.macro.name != null && !this.macro.name.isBlank() && MacroExecutor.isMacroRunning(this.macro.name)) return this.macro;
      if (this.originalMacro != null
         && this.originalMacro.name != null
         && !this.originalMacro.name.isBlank()
         && MacroExecutor.isMacroRunning(this.originalMacro.name)) {
         return this.originalMacro;
      }
      return null;
   }

   private void handleMacroActionsChanged() {
      if (this.macro != null) {
         if (this.macro.actions.isEmpty() && this.isEditingRunningMacro()) {
            MacroExecutor.stopMacro(this.macro.name);
         }

         this.syncStepRowAnimations(true);
         this.clampStepListScrollOffset();
         this.clearStepDragState();
         this.recreateComponents();
      }
   }

   private static String getPlayerInfoName(PlayerInfo entry) {
      if (entry != null && entry.getProfile() != null) {
         String name = entry.getProfile().name();
         return name == null ? "" : name.trim();
      } else {
         return "";
      }
   }

   private static <E extends Enum<E>> E cycleEnum(E current, int step) {
      E[] values = (E[])current.getDeclaringClass().getEnumConstants();
      int index = (current.ordinal() + step) % values.length;
      if (index < 0) {
         index += values.length;
      }

      return values[index];
   }

   private static double cycleRotateSpeed(double current, int step) {
      double[] speeds = new double[]{1.0, 2.0, 3.0, 5.0, 10.0};
      int index = 0;

      for (int i = 0; i < speeds.length; i++) {
         if (current <= speeds[i] + 1.0E-4) {
            index = i;
            break;
         }

         index = speeds.length - 1;
      }

      index = (index + step) % speeds.length;
      if (index < 0) {
         index += speeds.length;
      }

      return speeds[index];
   }

   public boolean isEditingMacro(PackUtilMacro macroToCheck) {
      return macroToCheck != null && this.originalMacro != null
         ? this.originalMacro == macroToCheck || this.originalMacro.name != null && this.originalMacro.name.equals(macroToCheck.name)
         : false;
   }

   public void restorePosition() {
      PackUtilSharedState shared = PackUtilSharedState.get();
      this.panelX = shared.getMacroEditorPanelX();
      this.panelY = shared.getMacroEditorPanelY();
   }

   @Override
   public boolean isVisible() {
      return this.visible;
   }

   @Override
   public void setVisible(boolean visible) {
      this.visible = visible;
      PackUtilSharedState.get().setMacroEditorVisible(visible);
      if (visible) {
         this.windowNode.restoreShowBody(!this.collapsed);
         PackUtilOverlayManager.get().bringToFront(this);
      }
   }

   @Override
   public boolean isCollapsed() {
      return this.collapsed;
   }

   @Override
   public void setCollapsed(boolean collapsed) {
      this.collapsed = collapsed;
      this.isWindowDragging = false;
      this.headerDragMoved = false;
      this.clearStepDragState();
      if (this.nameField != null) {
         this.nameField.setFocused(false);
      }

      if (this.loopCountField != null) {
         this.loopCountField.setFocused(false);
      }
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      if (!this.visible) {
         return false;
      } else {
         int height = this.getRenderedPanelHeight();
         if (mouseX >= this.panelX && mouseX <= this.panelX + this.PANEL_WIDTH && mouseY >= this.panelY && mouseY <= this.panelY + height) {
            return true;
         } else {
            if (this.isStepPickerOpen()) {
               int pickerX = this.getStepPickerX();
               int pickerY = this.getStepPickerY();
               int pickerW = this.getStepPickerWidth();
               int pickerH = this.getStepPickerHeight();
               if (mouseX >= pickerX && mouseX <= pickerX + pickerW && mouseY >= pickerY && mouseY <= pickerY + pickerH) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   @Override
   public boolean isOverDragBar(double mouseX, double mouseY) {
      if (!this.visible) {
         return false;
      } else {
         return mouseX >= this.panelX
               && mouseX <= this.panelX + this.PANEL_WIDTH
               && mouseY >= this.panelY
               && mouseY <= this.panelY + this.theme.headerHeight()
               && !this.isOverCloseButtonUi(mouseX, mouseY);
      }
   }

   @Override
   public boolean hasTextFieldFocused() {
      if (!this.visible) {
         return false;
      } else {
         return this.nameField != null && this.nameField.isFocused() ? true : this.loopCountField != null && this.loopCountField.isFocused();
      }
   }

   @Override
   public boolean wantsKeyboardCapture() {
      return this.visible && this.isBindingKey;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 && (this.draggingIndex >= 0 || this.pressedStepIndex >= 0)) {
         this.finishStepDrag();
         return true;
      } else if (button == 0 && this.activeScrollbarDrag != 0) {
         this.activeScrollbarDrag = 0;
         this.saveState();
         return true;
      } else if (button != 0) {
         return false;
      } else {
         boolean moved = this.headerDragMoved
            || Math.abs(mouseX - this.headerPressMouseX) >= 3.0
            || Math.abs(mouseY - this.headerPressMouseY) >= 3.0
            || this.panelX != this.headerPressPanelX
            || this.panelY != this.headerPressPanelY;
         this.isWindowDragging = false;
         this.isWindowResizing = false;
         if (!moved && this.isOverHeaderUi(mouseX, mouseY) && !this.isOverCloseButtonUi(mouseX, mouseY)) {
            this.setCollapsed(!this.collapsed);
         }

         this.saveState();
         this.headerDragMoved = false;
         return true;
      }
   }

   public void saveState() {
      PackUtilSharedState shared = PackUtilSharedState.get();
      shared.setMacroEditorVisible(this.visible);
      shared.setMacroEditorPanelX(this.panelX);
      shared.setMacroEditorPanelY(this.panelY);
      shared.setEditingMacro(this.macro);
      this.saveLayout();
   }

   public void restoreState() {
      PackUtilSharedState shared = PackUtilSharedState.get();
      this.restoreLayout();
      this.visible = shared.isMacroEditorVisible();
      this.panelX = shared.getMacroEditorPanelX();
      this.panelY = shared.getMacroEditorPanelY();
      this.macro = shared.getEditingMacro();
      this.windowNode.restoreShowBody(!this.collapsed);
      if (this.visible && this.macro != null) {
         this.resetStepRowAnimationState();
         this.recreateComponents();
      }
   }

   private void init() {
      if (MC.getWindow() != null) {
         int screenWidth = PackUtilUiScale.getVirtualScreenWidth();
         int screenHeight = PackUtilUiScale.getVirtualScreenHeight();
         if (this.panelX == 0 && this.panelY == 0) {
            this.panelX = screenWidth - this.PANEL_WIDTH - this.defaultScreenInsetX();
            this.panelY = this.defaultScreenInsetY();
         }

         this.recreateComponents();
      }
   }

   private boolean isValidName(String name) {
      if (name != null && !name.trim().isEmpty()) {
         return !this.isNew && name.equals(PackUtilMacroManager.get().get(name) != null ? name : "") ? true : PackUtilMacroManager.get().get(name) == null;
      } else {
         return false;
      }
   }

   private void recreateComponents() {
      this.btnLabels.clear();
      this.btnBounds.clear();
      this.btnActions.clear();
      this.btnEnabled.clear();
      this.runBtnIndex = -1;
      int bh = this.editorButtonHeight();
      int gap = this.editorControlGap();
      int row1Y = this.panelY + this.topRowOffset();
      int deleteTopW = !this.isNew ? this.deleteButtonWidth() : 0;
      int nameFieldW = this.PANEL_WIDTH - (this.contentInsetX() * 2) - (deleteTopW > 0 ? deleteTopW + gap : 0);
      if (this.nameField == null) {
         this.nameField = new PackUtilChatField(MC, this.textRenderer, this.panelX + this.contentInsetX(), row1Y, nameFieldW, this.textFieldHeight(), false);
         this.nameField.setPlaceholder(Component.literal("Macro Name"));
         this.nameField.setChangedListener(text -> {
            this.macro.name = text;
            this.updateSaveButtonState();
         });
      }

      this.nameField.setX(this.panelX + this.contentInsetX());
      this.nameField.setY(row1Y);
      this.nameField.setWidth(nameFieldW);
      this.nameField.setText(this.macro.name);
      if (!this.isNew) {
         this.addBtn("Delete", this.panelX + this.PANEL_WIDTH - this.contentInsetX() - deleteTopW, row1Y, deleteTopW, bh, () -> {
            PackUtilMacroManager.get().delete(this.macro);
            this.close();
         });
      }
      int row2Y = row1Y + this.secondRowGap();
      int bx = this.panelX + this.contentInsetX();
      String keyText = this.macro.keyCode == -1 ? "Bind Key" : "Key: " + PackUtilBindUtil.getBindName(this.macro.keyCode);

      if (this.isBindingKey) {
         keyText = "Press Key/Mouse...";
      }

      int keyW = PackUiText.width(this.textRenderer, keyText, this.theme.fontFor(PackUiTone.BODY), this.theme.color(PackUiTone.BODY)) + this.buttonTextPadding();
      this.addBtn(keyText, bx, row2Y, keyW, bh, () -> {
         this.isBindingKey = !this.isBindingKey;
         this.recreateComponents();
      });
      bx += keyW + gap;
      this.addBtn("Run", bx, row2Y, this.runButtonWidth(), bh, this::handleRunButton, MacroExecutor.isRunning() || this.canRunMacro());
      this.runBtnIndex = this.btnLabels.size() - 1;
      bx += this.runButtonWidth() + gap;
      if (PackUtilLANSync.getInstance().isInSession() && !this.isNew && this.macro != null) {
         this.addBtn("LAN", bx, row2Y, this.lanButtonWidth(), bh, () -> {
            if (this.macro != null) {
               PackUtilLANSync.getInstance().executeMacroSynchronized(this.macro.name);
            }
         });
         bx += this.lanButtonWidth() + gap;
      }

      String loopText = "Once";
      if (this.loopMode == 1) {
         loopText = "Loop #";
      }

      if (this.loopMode == 2) {
         loopText = "Loop Inf";
      }

      int loopW = PackUiText.width(this.textRenderer, loopText, this.theme.fontFor(PackUiTone.BODY), this.theme.color(PackUiTone.BODY)) + this.buttonTextPadding();
      this.addBtn(loopText, bx, row2Y, loopW, bh, () -> {
         this.loopMode = (this.loopMode + 1) % 3;
         this.updateLoopState();
         this.recreateComponents();
      });
      bx += loopW + gap;
      if (this.loopMode == 1) {
         if (this.loopCountField == null) {
            this.loopCountField = new PackUtilChatField(MC, this.textRenderer, bx, row2Y + 1, this.loopCountFieldWidth(), this.loopCountFieldHeight(), false);
            this.loopCountField.setChangedListener(text -> {
               try {
                  this.macro.loopCount = Integer.parseInt(text.replaceAll("[^0-9-]", ""));
               } catch (NumberFormatException var3x) {
               }
            });
         }

         this.loopCountField.setX(bx);
         this.loopCountField.setY(row2Y + 1);
         this.loopCountField.setWidth(this.loopCountFieldWidth());
         this.loopCountField.setText(String.valueOf(this.macro.loopCount == -1 ? 1 : this.macro.loopCount));
      }

      int builderButtonY = this.getStepBuilderButtonY();
      int builderGap = this.builderButtonGap();
      int builderWidth = (this.PANEL_WIDTH - (this.contentInsetX() * 2) - builderGap) / 2;
      this.addBtn("Add Action", this.panelX + this.contentInsetX(), builderButtonY, builderWidth, bh, () -> this.openStepPicker(PackUtilMacroEditorOverlay.StepPickerMode.ACTION));
      this.addBtn(
         "Add Conditional",
         this.panelX + this.contentInsetX() + builderWidth + builderGap,
         builderButtonY,
         builderWidth,
         bh,
         () -> this.openStepPicker(PackUtilMacroEditorOverlay.StepPickerMode.CONDITIONAL)
      );
      int bottomY = this.getFooterButtonsY();
      bx = this.panelX + this.contentInsetX();
      boolean validName = this.isValidName(this.macro.name);
      this.addBtn("Save", bx, bottomY, this.footerPrimaryButtonWidth(), bh, () -> {
         if (this.isValidName(this.macro.name)) {
            this.save();
         }
      }, validName);
      bx += this.footerPrimaryButtonWidth() + this.footerButtonGap();
      this.addBtn("Cancel", bx, bottomY, this.footerPrimaryButtonWidth(), bh, this::cancel);
      bx += this.footerPrimaryButtonWidth() + this.footerButtonGap();
      this.addBtn("Undo", bx, bottomY, this.footerSecondaryButtonWidth(), bh, this::performUndo, this.canUndoHistory());
      bx += this.footerSecondaryButtonWidth() + this.footerButtonGap();
      this.addBtn("Redo", bx, bottomY, this.footerSecondaryButtonWidth(), bh, this::performRedo, this.canRedoHistory());
   }

   private void addBtn(String label, int x, int y, int w, int h, Runnable action) {
      this.addBtn(label, x, y, w, h, action, true);
   }

   private void addBtn(String label, int x, int y, int w, int h, Runnable action, boolean enabled) {
      this.btnLabels.add(label);
      this.btnBounds.add(new int[]{x, y, w, h});
      this.btnActions.add(action);
      this.btnEnabled.add(enabled);
   }

   private void updateHistoryButtonState() {
      for (int i = 0; i < this.btnLabels.size(); i++) {
         String label = this.btnLabels.get(i);
         if ("Undo".equals(label)) {
            this.btnEnabled.set(i, this.canUndoHistory());
         } else if ("Redo".equals(label)) {
            this.btnEnabled.set(i, this.canRedoHistory());
         }
      }
   }

   private void updateRunButtonState() {
      if (this.runBtnIndex < 0 || this.runBtnIndex >= this.btnEnabled.size()) {
         return;
      }

      this.btnEnabled.set(this.runBtnIndex, this.isEditingRunningMacro() || this.canRunMacro());
   }

   private void handleRunButton() {
      if (this.macro == null) {
         PackUtilClientMessaging.sendPrefixed("No macro to run!");
      } else {
         PackUtilMacro runningMacro = this.getRunningEditedMacro();
         if (runningMacro != null) {
            MacroExecutor.stopMacro(runningMacro);
            this.recreateComponents();
         } else if (this.macro.actions.isEmpty()) {
            PackUtilClientMessaging.sendPrefixed("Macro has no actions!");
            this.recreateComponents();
         } else if (!this.canRunMacro()) {
            PackUtilClientMessaging.sendPrefixed(this.getRunBlockedReason());
            this.recreateComponents();
         } else {
            this.runEditedMacro();
         }
      }
   }

   private boolean isEditorMacroRunning() {
      return this.getRunningEditedMacro() != null;
   }

   private void stopEditorMacro() {
      PackUtilMacro runningMacro = this.getRunningEditedMacro();
      if (runningMacro != null) {
         MacroExecutor.stopMacro(runningMacro);
      } else {
         String name = this.macro != null ? this.macro.name : "";
         if (name != null && !name.isBlank()) MacroExecutor.stopMacro(name);
      }
   }

   private void runEditedMacro() {
      boolean hasValidName = this.macro.name != null && !this.macro.name.trim().isEmpty();
      if (hasValidName) {
         PackUtilMacro existingWithName = PackUtilMacroManager.get().get(this.macro.name);
         boolean isNameConflict = this.isNew && existingWithName != null;
         if (isNameConflict) {
            PackUtilClientMessaging.sendPrefixed("Name already exists: " + this.macro.name);
            return;
         }

         if (this.isNew) {
            PackUtilMacroManager.get().add(this.macro);
            this.originalMacro = PackUtilMacroManager.get().get(this.macro.name);
            this.isNew = false;
            this.tempSavedForRun = true;
         } else {
            if (!this.tempSavedForRun && this.originalMacro != null) {
               this.originalMacroSnapshot = this.originalMacro.toTag();
            }

            if (this.originalMacro != null) {
               this.originalMacro.fromTag(this.macro.toTag());
            }

            PackUtilMacroManager.get().save();
            this.tempSavedForRun = true;
         }

         PackUtilMacro savedMacro = PackUtilMacroManager.get().get(this.macro.name);
         if (savedMacro != null) {
            savedMacro.execute();
         } else {
            this.macro.execute();
         }
      } else {
         String originalName = this.macro.name;
         this.macro.name = "(Unsaved)";
         this.macro.execute();
         this.macro.name = originalName;
      }

      this.recreateComponents();
   }

   private boolean canRunMacro() {
      return this.getRunBlockedReason() == null;
   }

   private String getRunBlockedReason() {
      if (this.macro == null) {
         return "No macro to run!";
      }

      for (MacroAction action : this.macro.actions) {
         if (action instanceof InventoryAuditAction auditAction && !auditAction.hasValidTargetSelection()) {
            return "Inventory Audit needs at least one target.";
         }
      }

      return null;
   }

   private void updateSaveButtonState() {
      this.saveBtnDirty = true;
   }

   private void updateLoopState() {
      if (this.loopMode == 0) {
         this.macro.loop = false;
      } else if (this.loopMode == 1) {
         this.macro.loop = true;

         try {
            if (this.loopCountField != null) {
               this.macro.loopCount = Integer.parseInt(this.loopCountField.getText());
            }
         } catch (Exception var2) {
            this.macro.loopCount = 1;
         }
      } else {
         this.macro.loop = true;
         this.macro.loopCount = -1;
      }
   }

   public void addAction(MacroAction action) {
      this.pushStructuralHistoryStep();
      this.macro.actions.add(action);
      this.handleMacroActionsChanged();
      if (this.hasActionEditor(action)) {
         ActionEditorOverlay.getSharedOverlay().open(
            action,
            this::pushStructuralHistoryStep,
            this::handleMacroActionsChanged
         );
      }
   }

   private void save() {
      if (this.isValidName(this.macro.name)) {
         if (this.isNew) {
            PackUtilMacroManager.get().add(this.macro);
         } else {
            if (this.originalMacro != null) {
               this.originalMacro.name = this.macro.name;
               this.originalMacro.description = this.macro.description;
               this.originalMacro.loop = this.macro.loop;
               this.originalMacro.loopCount = this.macro.loopCount;
               this.originalMacro.keyCode = this.macro.keyCode;
               this.originalMacro.actions.clear();
               this.originalMacro.actions.addAll(this.macro.actions);
            }

            PackUtilMacroManager.get().save();
         }

         this.close();
      }
   }

   private MacroAction createActionCopy(MacroAction original) {
      if (original == null) {
         return null;
      } else {
         try {
            return PackUtilMacro.createActionFromTag(original.toTag());
         } catch (Exception var3) {
            return null;
         }
      }
   }

   @Override
   public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      if (this.saveBtnDirty) {
         this.saveBtnDirty = false;
         this.recreateComponents();
      }

      this.updateHistoryButtonState();
      this.updateRunButtonState();
      if (this.visible) {
         {
            int panelMouseX = mouseX;
            int panelMouseY = mouseY;
            if (this.isMouseOverStepPicker(mouseX, mouseY)) {
               panelMouseX = -10000;
               panelMouseY = -10000;
            }

            String editorTitle = this.isNew ? "Create Macro" : "Edit Macro";
            boolean isRunning = !this.isNew
               && this.macro != null
               && MacroExecutor.isMacroRunning(this.macro.name);
            if (isRunning) {
               editorTitle = editorTitle + " [RUN]";
            }

            if (this.macro != null) {
               editorTitle = editorTitle + " (" + this.macro.actions.size() + " steps)";
            }

            PackUiViewport viewport = this.surface.viewport();
            PackUtilWindowLayout clamped = this.clampToScreen(
               this,
               new PackUtilWindowLayout(this.panelX, this.panelY, this.PANEL_WIDTH, this.PANEL_HEIGHT, this.visible, this.collapsed)
            );
            if (clamped.x != this.panelX || clamped.y != this.panelY || clamped.width != this.PANEL_WIDTH || clamped.height != this.PANEL_HEIGHT) {
               this.panelX = clamped.x;
               this.panelY = clamped.y;
               this.PANEL_WIDTH = clamped.width;
               this.PANEL_HEIGHT = clamped.height;
               this.recreateComponents();
            }
            float uiMouseX = viewport.toUiX(mouseX);
            float uiMouseY = viewport.toUiY(mouseY);
            boolean active = PackUtilOverlayManager.get().isFocusedOverlay(this) || PackUtilOverlayManager.get().isTopOverlay(this);
            boolean headerHovered = uiMouseX >= this.panelX
               && uiMouseX < this.panelX + this.PANEL_WIDTH
               && uiMouseY >= this.panelY
               && uiMouseY < this.panelY + this.theme.headerHeight();
            this.windowNode.setTitle(editorTitle);
            this.windowNode.setShowBody(!this.collapsed);
            this.windowNode.setActive(active);
            this.windowNode.setHeaderHovered(headerHovered);
            int frameHeight = this.getRenderedPanelHeight();
            this.shellBody.setPreferredHeight(Math.max(0, frameHeight - this.theme.headerHeight()));
            this.windowNode.setBounds(this.panelX, this.panelY, this.PANEL_WIDTH, frameHeight);
            this.surface.render(context, mouseX, mouseY, delta);
            this.renderPackUiHeaderControls(context, viewport, uiMouseX, uiMouseY, delta, active);
            int bodyClipTop = this.panelY + this.theme.headerHeight();
            int bodyClipBottom = this.panelY + frameHeight;
            if (bodyClipBottom > bodyClipTop + 1) {
               viewport.enableScissor(context, this.panelX, bodyClipTop, this.panelX + this.PANEL_WIDTH, bodyClipBottom);

               try {
                  this.nameField.render(context, panelMouseX, panelMouseY, delta);
                  if (this.loopMode == 1 && this.loopCountField != null) {
                     this.loopCountField.render(context, panelMouseX, panelMouseY, delta);
                  }

                  if (!this.isValidName(this.macro.name)) {
                     String warning = this.macro.name.isEmpty() ? "Name Required" : "Name Taken";
                     PackUiText.draw(
                        context,
                        this.textRenderer,
                        warning,
                        this.theme.fontFor(PackUiTone.BODY),
                        -43691,
                        this.panelX + 10,
                        this.getFooterButtonsY() - 10,
                        false
                     );
                  }

                  int builderLabelY = this.getStepBuilderLabelY();
                  PackUiText.draw(
                     context,
                     this.textRenderer,
                     "Step Builder",
                     this.theme.fontFor(PackUiTone.LABEL),
                     PackUtilColors.textSecondary(),
                     this.panelX + 10,
                     PackUiSizing.alignTextY(builderLabelY, this.theme.lineHeight(PackUiTone.LABEL, 1), this.theme.fontHeight(PackUiTone.LABEL), this.theme.bodyTextNudge()),
                     false
                  );
                  int listLabelY = this.getStepListLabelY();
                  int listY = this.getStepListY();
                  int listHeight = this.getStepListHeight();
                  int listClipTop = this.getStepListClipTop();
                  int listClipBottom = this.getStepListClipBottom();
                  PackUiText.draw(
                     context,
                     this.textRenderer,
                     "Steps (" + this.macro.actions.size() + ")",
                     this.theme.fontFor(PackUiTone.LABEL),
                     PackUtilColors.textSecondary(),
                     this.panelX + 12,
                     PackUiSizing.alignTextY(listLabelY, this.theme.lineHeight(PackUiTone.LABEL, 1), this.theme.fontHeight(PackUiTone.LABEL), this.theme.bodyTextNudge()),
                     false
                  );
                  this.clampStepListScrollOffset();
                  PackUtilColors.drawInsetPanel(context, this.getStepListFrameX(), listY, this.getStepListFrameWidth(), listHeight, false);
                  PackUtilUiScale.enableOverlayScissor(
                     context,
                     this.getStepListContentLeft(),
                     listClipTop,
                     this.getStepListContentRight(),
                     listClipBottom
                  );

                  try {
                     this.tickStepRowAnimations();
                     int y = listY - this.scrollOffset;
                     int activeDragIndex = this.draggingIndex;
                     int activeDragTarget = this.stepDragTargetIndex;

                     for (int i = 0; i < this.macro.actions.size(); i++) {
                        if (i == activeDragIndex) {
                           continue;
                        }

                        MacroAction action = this.macro.actions.get(i);
                        int visualIndex = this.getShiftedVisualIndex(i, activeDragIndex, activeDragTarget);
                        int actionY = y + visualIndex * this.actionRowHeight();
                        if (this.isStepRowFullyVisible(actionY)) {
                           PackUtilMacroEditorOverlay.StepRowMotion motion = this.findStepRowMotion(this.stepRowStableId(action));
                           int animatedY = motion != null && motion.kind == PackUtilMacroEditorOverlay.StepRowMotionKind.MOVE
                              ? this.getAnimatedStepRowY(motion, y)
                              : actionY;
                           int enterOffset = this.getStepRowMoveOffsetX(motion);
                           float enterAlpha = this.getStepRowAlpha(motion);
                           this.renderStepListRow(
                              context,
                              action,
                              action.getDisplayName(),
                              this.isConditionalAction(action),
                              i,
                              animatedY,
                              panelMouseX,
                              panelMouseY,
                              enterOffset,
                              enterAlpha,
                              true
                           );
                        }
                     }

                     for (PackUtilMacroEditorOverlay.StepRowMotion motion : this.stepRowMotions) {
                        if (motion.kind == PackUtilMacroEditorOverlay.StepRowMotionKind.EXIT) {
                           int actionY = this.getAnimatedStepRowY(motion, y);
                           if (this.isStepRowFullyVisible(actionY)) {
                              this.renderStepListRow(
                                 context,
                                 null,
                                 motion.label,
                                 motion.conditional,
                                 motion.fromRowIndex,
                                 actionY,
                                 -10000,
                                 -10000,
                                 this.getStepRowMoveOffsetX(motion),
                                 this.getStepRowAlpha(motion),
                                 false
                              );
                           }
                        }
                     }

                     if (activeDragIndex >= 0 && activeDragIndex < this.macro.actions.size()) {
                        int insertSlot = this.getStepInsertSlotForTarget(activeDragIndex, activeDragTarget);
                        if (insertSlot >= 0) {
                           int insertY = y + insertSlot * this.actionRowHeight();
                           if (insertY >= listClipTop - 2 && insertY <= listClipBottom + 2) {
                              PackUiText.fill(context, this.panelX + 10, insertY - 1, this.getStepListContentRight() - 1, insertY + 1, this.theme.headerAccent());
                           }
                        }

                        MacroAction draggedAction = this.macro.actions.get(activeDragIndex);
                        int dragRowTop = this.getDraggedRowTop();
                        if (this.isStepRowFullyVisible(dragRowTop)) {
                           PackUiText.fill(context, this.panelX + 9, dragRowTop + 1, this.getStepListContentRight(), dragRowTop + this.actionRowHeight() - 1, this.theme.overlaySurface(0x00464D5A));
                           int badgeColor = this.isConditionalAction(draggedAction) ? -30720 : -12268476;
                           context.fill(this.panelX + 10, dragRowTop + 3, this.panelX + 13, dragRowTop + this.actionRowHeight() - 3, badgeColor);
                           PackUiText.draw(
                              context,
                              this.textRenderer,
                              String.valueOf(activeDragIndex + 1),
                              this.theme.fontFor(PackUiTone.BODY),
                              PackUtilColors.textDim(),
                              this.panelX + 16,
                              this.stepRowTextY(dragRowTop),
                              false
                           );
                           int nameX = this.panelX + 30;
                           int controlsX = this.getActionRowControlsX(draggedAction);
                           int maxNameW = controlsX - nameX - 8;
                           String displayName = PackUiText.trimToWidth(
                              this.textRenderer, draggedAction.getDisplayName(), maxNameW, this.theme.fontFor(PackUiTone.BODY), -1
                           );
                           PackUiText.draw(context, this.textRenderer, displayName, this.theme.fontFor(PackUiTone.BODY), -1, nameX, this.stepRowTextY(dragRowTop), false);
                        }
                     }
                  } finally {
                     context.disableScissor();
                  }

                  this.renderWindowBodyFadeCover(context, bodyClipTop, bodyClipBottom);
                  PackUiScrollbar.Metrics stepListScrollbar = this.getStepListScrollbarMetrics();
                  PackUiScrollbar.draw(context, stepListScrollbar, stepListScrollbar.contains(panelMouseX, panelMouseY), this.activeScrollbarDrag == 1);
                  this.hoveredTooltip = null;
                  boolean macroCurrentlyRunning = this.isEditingRunningMacro();

                  for (int ix = 0; ix < this.btnLabels.size(); ix++) {
                     int[] b = this.btnBounds.get(ix);
                     boolean enabled = this.btnEnabled.get(ix);
                     String lbl = ix == this.runBtnIndex ? (macroCurrentlyRunning ? "Stop" : "Run") : this.btnLabels.get(ix);
                     PackUiOverlayButton.Variant variant = PackUiOverlayButton.Variant.SECONDARY;
                     if (ix == this.runBtnIndex) {
                        variant = macroCurrentlyRunning ? PackUiOverlayButton.Variant.DANGER : PackUiOverlayButton.Variant.SUCCESS;
                     }

                     this.drawOverlayButton(context, b[0], b[1], b[2], b[3], lbl, variant, enabled, panelMouseX, panelMouseY);
                     if (panelMouseX >= b[0] && panelMouseX < b[0] + b[2] && panelMouseY >= b[1] && panelMouseY < b[1] + b[3]) {
                        String tip = this.getTooltipFor(lbl);
                        if (tip != null) {
                           this.hoveredTooltip = tip;
                           this.tooltipX = panelMouseX + 8;
                           this.tooltipY = panelMouseY + 12;
                        }
                     }
                  }
               } finally {
                  viewport.disableScissor(context);
               }

               if (!this.collapsed) {
                  if (this.isStepPickerOpen()) {
                     context.nextStratum();
                     PackUiText.interOverlayFlush(context);
                     this.renderStepPicker(context, mouseX, mouseY);
                  }

                   if (this.hoveredTooltip != null) {
                      context.nextStratum();
                      PackUiText.interOverlayFlush(context);
                      Identifier tooltipFont = this.theme.fontFor(PackUiTone.BODY);
                     int tw = PackUiText.width(this.textRenderer, this.hoveredTooltip, tooltipFont, PackUtilColors.textLight()) + 6;
                     int th = 12;
                     int drawX = Math.min(this.tooltipX, PackUtilUiScale.getVirtualScreenWidth() - tw - 4);
                     int drawY = Math.min(this.tooltipY, PackUtilUiScale.getVirtualScreenHeight() - th - 4);
                     PackUiText.fill(context, drawX - 2, drawY - 2, drawX + tw, drawY + th, PackUtilColors.tooltipBg());
                     PackUiText.fill(context, drawX - 3, drawY - 3, drawX + tw + 1, drawY - 2, PackUtilColors.subPanelBorder());
                     PackUiText.fill(context, drawX - 3, drawY + th, drawX + tw + 1, drawY + th + 1, PackUtilColors.subPanelBorder());
                     PackUiText.fill(context, drawX - 3, drawY - 2, drawX - 2, drawY + th, PackUtilColors.subPanelBorder());
                     PackUiText.fill(context, drawX + tw, drawY - 2, drawX + tw + 1, drawY + th, PackUtilColors.subPanelBorder());
                     PackUiText.draw(
                        context,
                        this.textRenderer,
                        this.hoveredTooltip,
                        tooltipFont,
                        PackUtilColors.textLight(),
                        drawX + 2,
                        drawY + 1,
                        false
                     );
                  }
               }
            }
         }
      }
   }

   private void renderPackUiHeaderControls(GuiGraphicsExtractor context, PackUiViewport viewport, float uiMouseX, float uiMouseY, float delta, boolean active) {
      this.closeVisibility = this.animateHeader(this.closeVisibility, 1.0F, delta);
      this.closeHover = this.animateHeader(this.closeHover, this.isOverCloseButtonUi(uiMouseX, uiMouseY) ? 1.0F : 0.0F, delta);
      viewport.push(context);

      try {
         int arrowX = this.collapseArrowX();
         int closeX = this.closeButtonX();
         int controlY = this.controlButtonY();
         this.drawPackUiCollapseArrow(context, arrowX, controlY, active);
         this.drawPackUiCloseButton(context, closeX, controlY, 12, 12, this.closeHover, active, this.closeVisibility);
      } finally {
         viewport.pop(context);
      }
   }

   private float animateHeader(float current, float target, float delta) {
      return PackUiHeaderControls.animate(current, target, delta);
   }

   private void drawPackUiPopupFrame(GuiGraphicsExtractor context, int x, int y, int width, int height) {
      PackUiBannerRenderer.drawPopupFrame(context, this.theme, x, y, width, height);
   }

   private void drawPackUiCollapseArrow(GuiGraphicsExtractor context, int x, int y, boolean active) {
      PackUiHeaderControls.drawAnimatedArrow(context, x, y + 1, 10, this.collapsed ? 0.0F : 1.0F, active ? 1.0F : 0.56F);
   }

   private void drawPackUiCloseButton(GuiGraphicsExtractor context, int x, int y, int width, int height, float hover, boolean active, float visibility) {
      PackUiHeaderControls.drawCloseButton(context, x, y, width, height, hover, active, visibility);
   }

   private void drawStepRowControlButton(
      GuiGraphicsExtractor context, int x, int y, int width, int height, boolean hovered, boolean danger, Identifier icon, String label
   ) {
      PackUiOverlayButton button = this.createStepRowControlButtonView(x, y, width, height, danger, icon == null ? label : "");
      PackUiOverlayButton.renderStyled(
         context,
         this.textRenderer,
         button,
         hovered ? x + (width / 2) : -1000,
         hovered ? y + (height / 2) : -1000
      );
      if (icon != null) {
         int iconSize = Math.max(8, Math.min(10, Math.min(width, height) - 4));
         int iconX = x + Math.max(1, (width - iconSize) / 2);
         int iconY = y + Math.max(1, (height - iconSize) / 2);
         if (PackUiControlGlyphs.isCloseIcon(icon) || PackUiControlGlyphs.isChevronIcon(icon)) {
            PackUiControlGlyphs.drawKnownIcon(
               context,
               icon,
               iconX,
               iconY,
               iconSize,
               danger ? 0xFFFFBFBF : 0xFFF4EBEB,
               danger ? 0xCC4A1A1F : 0xB53A1418,
               1.0f
            );
         } else {
            context.blit(icon, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
         }
      }
   }

   private String stepPickerFeedbackKey(PackUtilMacroEditorOverlay.StepPickerCategory category, PackUtilMacroEditorOverlay.StepPickerEntry entry) {
      String modeKey = this.stepPickerMode == null ? "none" : this.stepPickerMode.name();
      return "macro-step-picker:" + modeKey + ':' + category.id + ':' + entry.label;
   }

   private void drawStepPickerCard(
      GuiGraphicsExtractor context,
      PackUtilMacroEditorOverlay.StepPickerCategory category,
      PackUtilMacroEditorOverlay.StepPickerEntry entry,
      int x,
      int y,
      int width,
      int height,
      boolean hovered,
      int mouseX,
      int mouseY
   ) {
      PackUiButtonFeedback feedback = PackUiButtonFeedback.forKey(this.stepPickerFeedbackKey(category, entry));
      float hover = feedback.update(
         hovered,
         hovered && PackUiButtonFeedback.isPrimaryPointerDown(),
         mouseX - x,
         mouseY - y,
         width,
         height
      );
      float clickGlow = feedback.clickGlowProgress();
      float emphasis = Math.min(1.0F, hover * 0.85F + clickGlow * 0.55F);
      int categoryRgb = category.color & 0x00FFFFFF;

      int fillColor = PackUiSizing.lerpColor(0x08000000, 0x22000000 | categoryRgb, emphasis);
      int borderColor = PackUiSizing.lerpColor(category.color, 0xFFFFFFFF, Math.min(0.22F, hover * 0.10F + clickGlow * 0.18F));
      int textColor = PackUiSizing.lerpColor(this.theme.color(PackUiTone.BODY), 0xFFFFFFFF, Math.min(0.32F, hover * 0.12F + clickGlow * 0.20F));

      PackUiText.fill(context, x, y, x + width, y + height, fillColor);
      PackUiText.fill(context, x, y, x + width, y + 1, borderColor);
      PackUiText.fill(context, x, y + height - 1, x + width, y + height, borderColor);
      PackUiText.fill(context, x, y, x + 1, y + height, borderColor);
      PackUiText.fill(context, x + width - 1, y, x + width, y + height, borderColor);

      if (emphasis > 0.0F) {
         int hoverTint = (((int)Math.min(30.0F, hover * 18.0F + clickGlow * 10.0F)) << 24) | categoryRgb;
         PackUiText.fill(context, x + 1, y + 1, x + width - 1, y + height - 1, hoverTint);
      }

      feedback.render(
         context,
         x + 1,
         y + 1,
         Math.max(0, width - 2),
         Math.max(0, height - 2),
         categoryRgb,
         PackUiSizing.lerpColor(category.color, 0xFFFFFFFF, 0.55F) & 0x00FFFFFF,
         1.0F
      );

      String label = PackUiText.trimToWidth(this.textRenderer, entry.label, Math.max(0, width - 8), this.theme.fontFor(PackUiTone.BODY), textColor);
      int labelW = PackUiText.width(this.textRenderer, label, this.theme.fontFor(PackUiTone.BODY), textColor);
      int labelX = x + Math.max(3, (width - labelW) / 2);
      int labelY = PackUiSizing.alignTextY(y, height, this.theme.fontHeight(PackUiTone.BODY), this.theme.buttonTextNudge());
      PackUiText.draw(context, this.textRenderer, label, this.theme.fontFor(PackUiTone.BODY), textColor, labelX, labelY, false);
   }

   private void renderStepListRow(
      GuiGraphicsExtractor context,
      MacroAction action,
      String label,
      boolean conditional,
      int actionIndex,
      int actionY,
      int mouseX,
      int mouseY,
      int xOffset,
      float alpha,
      boolean interactive
   ) {
      int rowLeft = this.panelX + 9 + xOffset;
      int rowRight = this.getStepListContentRight() + xOffset;
      boolean hoverRow = interactive
         && mouseX >= rowLeft - 1
         && mouseX <= rowRight + 1
         && mouseY >= actionY
         && mouseY < actionY + this.actionRowHeight();
      int bgColor = hoverRow ? PackUtilColors.rowHover() : PackUtilColors.rowNormal();
      PackUiText.fill(context, rowLeft, actionY + 1, rowRight, actionY + this.actionRowHeight() - 1, PackUiRenderContext.applyAlpha(bgColor, alpha));
      int badgeColor = conditional ? -30720 : -12268476;
      context.fill(
         this.panelX + 10 + xOffset,
         actionY + 3,
         this.panelX + 13 + xOffset,
         actionY + this.actionRowHeight() - 3,
         PackUiRenderContext.applyAlpha(badgeColor, alpha)
      );
      PackUiText.draw(
         context,
         this.textRenderer,
         String.valueOf(actionIndex + 1),
         this.theme.fontFor(PackUiTone.BODY),
         PackUiRenderContext.applyAlpha(PackUtilColors.textDim(), alpha),
         this.panelX + 16 + xOffset,
         this.stepRowTextY(actionY),
         false
       );
       int nameX = this.panelX + 30 + xOffset;
       int controlsX = this.getActionRowControlsX(action) + xOffset;
       int contentRight = this.getStepListContentRightForAction(action) + xOffset;
       int maxNameW = Math.max(1, contentRight - nameX - 8);
       String displayName = PackUiText.trimToWidth(
         this.textRenderer, label, maxNameW, this.theme.fontFor(PackUiTone.BODY), PackUiRenderContext.applyAlpha(-1, alpha)
      );
      PackUiText.draw(
         context,
         this.textRenderer,
         displayName,
         this.theme.fontFor(PackUiTone.BODY),
         PackUiRenderContext.applyAlpha(-1, alpha),
         nameX,
         this.stepRowTextY(actionY),
         false
      );
      if (interactive && action != null) {
         int controlY = this.stepRowControlY(actionY);
         this.drawStepRowControlButton(
            context,
            controlsX,
            controlY,
            this.stepRowControlSize(),
            this.stepRowControlSize(),
            mouseX >= controlsX && mouseX < controlsX + this.stepRowControlSize() && mouseY >= controlY && mouseY < controlY + this.stepRowControlSize(),
            false,
            PackUiAssets.ICON_WINDOW_CHEVRON_UP,
            null
         );
         int cx = controlsX + this.stepRowControlStride();
         this.drawStepRowControlButton(
            context,
            cx,
            controlY,
            this.stepRowControlSize(),
            this.stepRowControlSize(),
            mouseX >= cx && mouseX < cx + this.stepRowControlSize() && mouseY >= controlY && mouseY < controlY + this.stepRowControlSize(),
            false,
            PackUiAssets.ICON_WINDOW_CHEVRON_DOWN,
            null
         );
         cx += this.stepRowControlStride();
         this.drawStepRowControlButton(
            context,
            cx,
            controlY,
            this.stepRowControlSize(),
            this.stepRowControlSize(),
            mouseX >= cx && mouseX < cx + this.stepRowControlSize() && mouseY >= controlY && mouseY < controlY + this.stepRowControlSize(),
            false,
            null,
            "D"
         );
         cx += this.stepRowControlStride();
         boolean hasEditor = this.hasActionEditor(action);
         if (hasEditor) {
            this.drawStepRowControlButton(
               context,
               cx,
               controlY,
               this.stepRowControlSize(),
               this.stepRowControlSize(),
               mouseX >= cx && mouseX < cx + this.stepRowControlSize() && mouseY >= controlY && mouseY < controlY + this.stepRowControlSize(),
               false,
               null,
               "E"
            );
            cx += this.stepRowControlStride();
         }
         this.drawStepRowControlButton(
            context,
            cx,
            controlY,
            this.stepRowControlSize(),
            this.stepRowControlSize(),
            mouseX >= cx && mouseX < cx + this.stepRowControlSize() && mouseY >= controlY && mouseY < controlY + this.stepRowControlSize(),
            true,
            PackUiAssets.ICON_WINDOW_CLOSE,
            null
         );
      }
   }

   private void triggerStepRowIconPress(double mouseX, double mouseY, int button, int x, int y, int size, boolean danger, Identifier icon) {
      PackUiOverlayButton preview = this.createStepRowControlButtonView(x, y, size, size, danger, "");
      PackUiOverlayButton.fireIfHit(preview, mouseX, mouseY, button);
   }

   private void triggerStepRowTextPress(double mouseX, double mouseY, int button, int x, int y, int width, int height, boolean danger, String label) {
      PackUiOverlayButton preview = this.createStepRowControlButtonView(x, y, width, height, danger, label);
      PackUiOverlayButton.fireIfHit(preview, mouseX, mouseY, button);
   }

   private void drawEditorListText(GuiGraphicsExtractor context, String text, int x, int y, int color) {
      PackUiText.draw(context, this.textRenderer, text, this.theme.fontFor(PackUiTone.BODY), color, x, y, false);
   }

   private void drawEditorListLabel(GuiGraphicsExtractor context, String text, int x, int y, int color) {
      PackUiText.draw(context, this.textRenderer, text, this.theme.fontFor(PackUiTone.LABEL), color, x, y, false);
   }

   private void drawEditorListDeleteButton(GuiGraphicsExtractor context, int x, int y, boolean hovered) {
      this.drawStepRowControlButton(context, x, y, 14, 12, hovered, true, PackUiAssets.ICON_WINDOW_CLOSE, null);
   }

   private void drawEditorSelectableRegistryRow(GuiGraphicsExtractor context, int x, int y, int width, String label, boolean hovered, boolean selected) {
      int bg = selected ? PackUtilColors.rowSelected() : (hovered ? PackUtilColors.rowHover() : PackUtilColors.rowNormal());
      PackUiText.fill(context, x, y, x + width, y + 12, bg);
      int textX = x + 3;

      this.drawEditorListText(context, label, textX, y + 2, selected ? PackUtilColors.rowSelectedText() : PackUtilColors.textLight());
   }

   private boolean isOverHeaderUi(double mouseX, double mouseY) {
      return mouseX >= this.panelX && mouseX <= this.panelX + this.PANEL_WIDTH && mouseY >= this.panelY && mouseY <= this.panelY + this.theme.headerHeight();
   }

   private boolean isOverCloseButtonUi(double mouseX, double mouseY) {
      return PackUiHeaderControls.isCloseHit(this.closeVisibility, mouseX, mouseY, this.closeButtonX(), this.controlButtonY(), 12);
   }

   private int controlButtonY() {
      return PackUiHeaderControls.controlY(this.panelY, this.theme.headerHeight(), 12);
   }

   private int closeButtonX() {
      return PackUiHeaderControls.closeX(this.panelX, this.PANEL_WIDTH, 12, 2);
   }

   private int collapseArrowX() {
      return PackUiHeaderControls.expandedArrowX(this.closeButtonX(), 3, 10);
   }

   private int getRenderedPanelHeight() {
      this.shellBody.setPreferredHeight(Math.max(0, this.PANEL_HEIGHT - this.theme.headerHeight()));
      this.windowNode.setShowBody(!this.collapsed);
      PackUiRenderContext metrics = this.surface.measurementContext();
      if (metrics == null) {
         return this.collapsed ? this.theme.headerHeight() : this.PANEL_HEIGHT;
      } else {
         return Math.max(this.theme.headerHeight(), Math.round(this.windowNode.preferredHeight(metrics, this.PANEL_WIDTH)));
      }
   }

   private void renderWindowBodyFadeCover(GuiGraphicsExtractor context, int bodyClipTop, int bodyClipBottom) {
      if (bodyClipBottom <= bodyClipTop) {
         return;
      }

      float coverAlpha = 1.0F - this.windowNode.bodyFadeAlpha();
      if (coverAlpha <= 0.001F) {
         return;
      }

      float easedCover = 1.0F - (float)Math.pow(1.0F - coverAlpha, 2.0);
      float fadeAlpha = Math.min(0.96F, 0.10F + easedCover * 0.86F);
      PackUiText.fill(context,
         this.panelX + 1,
         bodyClipTop,
         this.panelX + this.PANEL_WIDTH - 1,
         bodyClipBottom,
         PackUiRenderContext.applyAlpha(this.theme.inactiveBodyFadeFill(), fadeAlpha)
      );
   }

   private void renderStepPicker(GuiGraphicsExtractor context, int mouseX, int mouseY) {
      int pickerX = this.getStepPickerX();
      int pickerY = this.getStepPickerY();
      int pickerW = this.getStepPickerWidth();
      int pickerH = this.getStepPickerHeight();
      int closeX = this.getStepPickerCloseX();
      int closeY = this.getStepPickerCloseY();
      boolean hoverClose = mouseX >= closeX - 2 && mouseX <= closeX + 10 && mouseY >= closeY - 2 && mouseY <= closeY + 10;
      this.drawPackUiPopupFrame(context, pickerX, pickerY, pickerW, pickerH);
      String title = this.stepPickerMode == PackUtilMacroEditorOverlay.StepPickerMode.ACTION ? "Add Action" : "Add Conditional";
      int pickerTitleY = PackUiSizing.alignTextY(pickerY, 16, this.theme.fontHeight(PackUiTone.LABEL), this.theme.bodyTextNudge());
      PackUiText.draw(
         context, this.textRenderer, title, this.theme.fontFor(PackUiTone.LABEL), this.theme.color(PackUiTone.BODY), pickerX + 10, pickerTitleY, false
      );
      this.drawPackUiCloseButton(context, closeX, closeY, 12, 12, hoverClose ? 1.0F : 0.0F, true, 1.0F);
      int contentX = pickerX + 6;
      int contentY = pickerY + 22;
      int contentW = pickerW - 12;
      int contentH = pickerH - 28;
      int gridX = contentX;
      int gridY = contentY;
      int gridH = contentH;
      List<PackUtilMacroEditorOverlay.StepPickerSectionLayout> layouts = this.buildStepPickerLayouts(contentX, contentY - this.stepPickerScrollOffset, contentW);
      int contentHeight = this.getStepPickerContentHeight();
      int maxScroll = Math.max(0, contentHeight - contentH);
      this.stepPickerScrollOffset = Math.max(0, Math.min(this.stepPickerScrollOffset, maxScroll));
      PackUtilUiScale.enableOverlayScissor(context, contentX, contentY, contentX + contentW, contentY + contentH);

      try {
         for (PackUtilMacroEditorOverlay.StepPickerSectionLayout section : layouts) {
            int sectionTop = section.headerY;
            int sectionBottom = section.bottomY;
            if (sectionBottom >= gridY && sectionTop <= gridY + gridH) {
               PackUiText.draw(
                  context,
                  this.textRenderer,
                  section.category.label,
                  this.theme.fontFor(PackUiTone.LABEL),
                  section.category.color,
                  gridX,
                  PackUiSizing.alignTextY(
                     sectionTop,
                     this.stepPickerSectionLabelHeight(),
                     this.theme.fontHeight(PackUiTone.LABEL),
                     0
                  ),
                  false
               );
            }

            for (PackUtilMacroEditorOverlay.StepPickerButtonLayout button : section.buttons) {
               int cardX = button.x;
               int cardY = button.y;
               int cardW = button.width;
               int cardH = button.height;
               if (cardY + cardH >= gridY && cardY <= gridY + gridH) {
                  boolean hovered = mouseX >= cardX && mouseX < cardX + cardW && mouseY >= cardY && mouseY < cardY + cardH;
                  this.drawStepPickerCard(context, section.category, button.entry, cardX, cardY, cardW, cardH, hovered, mouseX, mouseY);
                  if (hovered) {
                     this.hoveredTooltip = button.entry.description;
                     this.tooltipX = mouseX + 8;
                     this.tooltipY = mouseY + 12;
                  }
               }
            }
         }
      } finally {
         context.disableScissor();
      }

      PackUiScrollbar.Metrics stepPickerScrollbar = this.getStepPickerScrollbarMetrics();
      PackUiScrollbar.draw(context, stepPickerScrollbar, stepPickerScrollbar.contains(mouseX, mouseY), this.activeScrollbarDrag == 2);
      if (maxScroll > 0) {
         String hint = "Scroll for more";
         int hintW = PackUiText.width(this.textRenderer, hint, this.theme.fontFor(PackUiTone.BODY), PackUtilColors.textDim());
         int hintX = Math.max(pickerX + 72, pickerX + pickerW - hintW - 28);
         int hintY = pickerY + 8;
         PackUiText.draw(context, this.textRenderer, hint, this.theme.fontFor(PackUiTone.BODY), PackUtilColors.textDim(), hintX, hintY + 1, false);
      }
   }

   private int stepPickerSectionLabelHeight() {
      return Math.max(12, this.theme.fontHeight(PackUiTone.LABEL));
   }

   private boolean handleStepPickerClick(double mouseX, double mouseY, int button) {
      int pickerX = this.getStepPickerX();
      int pickerY = this.getStepPickerY();
      int pickerW = this.getStepPickerWidth();
      int pickerH = this.getStepPickerHeight();
      int closeX = this.getStepPickerCloseX();
      int closeY = this.getStepPickerCloseY();
      boolean inside = mouseX >= pickerX && mouseX < pickerX + pickerW && mouseY >= pickerY && mouseY < pickerY + pickerH;
      if (!inside) {
         this.closeStepPicker();
         return true;
      } else if (button != 0) {
         return true;
      } else if (mouseX >= closeX && mouseX < closeX + 12 && mouseY >= closeY && mouseY < closeY + 12) {
         this.closeStepPicker();
         return true;
      } else {
         int contentX = pickerX + 6;
         int contentY = pickerY + 22;
         int contentW = pickerW - 12;
         int contentH = pickerH - 28;
         if (!(mouseX < contentX) && !(mouseX >= contentX + contentW) && !(mouseY < contentY) && !(mouseY >= contentY + contentH)) {
            PackUiScrollbar.Metrics stepPickerScrollbar = this.getStepPickerScrollbarMetrics();
            if (stepPickerScrollbar.hasScroll() && stepPickerScrollbar.contains((int)mouseX, (int)mouseY)) {
               this.activeScrollbarDrag = 2;
               this.scrollbarGrabOffset = Math.max(0, (int)mouseY - stepPickerScrollbar.thumbY());
               this.stepPickerScrollOffset = PackUiScrollbar.scrollFromThumb(stepPickerScrollbar, (int)mouseY, this.scrollbarGrabOffset);
               return true;
            } else {
               for (PackUtilMacroEditorOverlay.StepPickerSectionLayout section : this.buildStepPickerLayouts(
                  contentX, contentY - this.stepPickerScrollOffset, contentW
               )) {
                  for (PackUtilMacroEditorOverlay.StepPickerButtonLayout buttonLayout : section.buttons) {
                     int cardX = buttonLayout.x;
                     int cardY = buttonLayout.y;
                     int cardW = buttonLayout.width;
                     int cardH = buttonLayout.height;
                     if (mouseX >= cardX && mouseX < cardX + cardW && mouseY >= cardY && mouseY < cardY + cardH) {
                        PackUiButtonFeedback.forKey(this.stepPickerFeedbackKey(section.category, buttonLayout.entry))
                           .triggerPress((float)(mouseX - cardX), (float)(mouseY - cardY), cardW, cardH);
                        this.closeStepPicker();
                        buttonLayout.entry.action.run();
                        return true;
                     }
                  }
               }

               return true;
            }
         } else {
            return true;
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (!this.visible) {
         return false;
      } else if (this.isBindingKey && PackUtilBindUtil.isAllowedMouseButton(button)) {
         this.macro.keyCode = PackUtilBindUtil.encodeMouseButton(button);
         this.isBindingKey = false;
         this.recreateComponents();
         return true;
      } else if (button == 0 && this.isOverHeaderUi(mouseX, mouseY)) {
         if (this.isOverCloseButtonUi(mouseX, mouseY) && this.closeVisibility > 0.01F) {
            this.close();
            return true;
         }

         this.isWindowDragging = true;
         this.headerDragMoved = false;
         this.dragOffsetX = mouseX - this.panelX;
         this.dragOffsetY = mouseY - this.panelY;
         this.headerPressMouseX = mouseX;
         this.headerPressMouseY = mouseY;
         this.headerPressPanelX = this.panelX;
         this.headerPressPanelY = this.panelY;
         return true;
      } else if (this.collapsed) {
         return false;
      } else if (this.isStepPickerOpen()) {
         return this.handleStepPickerClick(mouseX, mouseY, button);
      } else {
         MouseButtonEvent click = new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
         boolean fieldClicked = false;
         if (this.nameField.mouseClicked(click, false)) {
            this.nameField.setFocused(true);
            if (this.loopCountField != null) {
               this.loopCountField.setFocused(false);
            }

            fieldClicked = true;
         } else if (this.loopMode == 1 && this.loopCountField != null && this.loopCountField.mouseClicked(click, false)) {
            this.loopCountField.setFocused(true);
            this.nameField.setFocused(false);
            fieldClicked = true;
         }

         if (fieldClicked) {
            return true;
         }

         this.nameField.setFocused(false);
         if (this.loopCountField != null) {
            this.loopCountField.setFocused(false);
         }

          this.updateHistoryButtonState();
          for (int i = 0; i < this.btnLabels.size(); i++) {
             int[] b = this.btnBounds.get(i);
             if (mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3]) {
                boolean enabled = this.btnEnabled.get(i);
                boolean macroCurrentlyRunning = this.isEditingRunningMacro();
                String label = i == this.runBtnIndex ? (macroCurrentlyRunning ? "Stop" : "Run") : this.btnLabels.get(i);
                PackUiOverlayButton.Variant variant = PackUiOverlayButton.Variant.SECONDARY;
                if (i == this.runBtnIndex) {
                   variant = macroCurrentlyRunning ? PackUiOverlayButton.Variant.DANGER : PackUiOverlayButton.Variant.SUCCESS;
                }

                PackUiOverlayButton buttonView = this.createOverlayButton(b[0], b[1], b[2], b[3], label, variant, enabled, this.btnActions.get(i));
                PackUiOverlayButton.fireIfHit(buttonView, mouseX, mouseY, button);
                return true;
             }
          }

         int listY = this.getStepListY();
         int listHeight = this.getStepListHeight();
         if (mouseY >= listY && mouseY <= listY + listHeight && mouseX >= this.getStepListFrameX() && mouseX <= this.getStepListFrameX() + this.getStepListFrameWidth()) {
            PackUiScrollbar.Metrics stepListScrollbar = this.getStepListScrollbarMetrics();
            if (button == 0 && stepListScrollbar.hasScroll() && stepListScrollbar.contains((int)mouseX, (int)mouseY)) {
               this.activeScrollbarDrag = 1;
               this.scrollbarGrabOffset = Math.max(0, (int)mouseY - stepListScrollbar.thumbY());
               this.scrollOffset = this.quantizeScrollOffset(
                  PackUiScrollbar.scrollFromThumb(stepListScrollbar, (int)mouseY, this.scrollbarGrabOffset),
                  this.actionRowHeight(),
                  stepListScrollbar.maxScroll()
               );
               return true;
            }

            int y = listY - this.scrollOffset;
            int controlSize = this.stepRowControlSize();
            int controlStride = this.stepRowControlStride();
            int index = (int)((mouseY - y) / this.actionRowHeight());
            if (index >= 0 && index < this.macro.actions.size()) {
               MacroAction action = this.macro.actions.get(index);
               int actionY = y + index * this.actionRowHeight();
               if (!this.isStepRowFullyVisible(actionY)) {
                  return true;
               }
               int controlY = this.stepRowControlY(actionY);
               int cx = this.getActionRowControlsX(action);
               if (mouseX >= cx && mouseX < cx + controlSize) {
                  this.triggerStepRowIconPress(mouseX, mouseY, button, cx, controlY, controlSize, false, PackUiAssets.ICON_WINDOW_CHEVRON_UP);
                  if (index > 0) {
                     this.pushStructuralHistoryStep();
                     Collections.swap(this.macro.actions, index, index - 1);
                     this.handleMacroActionsChanged();
                  }

                  return true;
               }

               cx += controlStride;
               if (mouseX >= cx && mouseX < cx + controlSize) {
                  this.triggerStepRowIconPress(mouseX, mouseY, button, cx, controlY, controlSize, false, PackUiAssets.ICON_WINDOW_CHEVRON_DOWN);
                  if (index < this.macro.actions.size() - 1) {
                     this.pushStructuralHistoryStep();
                     Collections.swap(this.macro.actions, index, index + 1);
                     this.handleMacroActionsChanged();
                  }

                  return true;
               }

               cx += controlStride;
               if (mouseX >= cx && mouseX < cx + controlSize) {
                  this.triggerStepRowTextPress(mouseX, mouseY, button, cx, controlY, controlSize, controlSize, false, "D");
                  this.duplicateAction(index);
                  return true;
               }

               cx += controlStride;
               if (mouseX >= cx && mouseX < cx + controlSize && this.hasActionEditor(action)) {
                  this.triggerStepRowTextPress(mouseX, mouseY, button, cx, controlY, controlSize, controlSize, false, "E");
                  final MacroAction editTarget = action;
                  ActionEditorOverlay.getSharedOverlay().open(
                     editTarget,
                     this::pushStructuralHistoryStep,
                     this::handleMacroActionsChanged
                  );
                  return true;
               }

               if (this.hasActionEditor(action)) {
                  cx += controlStride;
               }
               if (mouseX >= cx && mouseX < cx + controlSize) {
                  this.triggerStepRowIconPress(mouseX, mouseY, button, cx, controlY, controlSize, true, PackUiAssets.ICON_WINDOW_CLOSE);
                  this.pushStructuralHistoryStep();
                  this.macro.actions.remove(index);
                  this.handleMacroActionsChanged();
                  return true;
               }

               if (button == 0 && mouseX < this.getStepListContentRight()) {
                  this.beginStepDragPress(index, mouseX, mouseY);
                  return true;
               }
            }
         }

         return false;
      }
   }

   private void duplicateAction(int index) {
      if (index >= 0 && index < this.macro.actions.size()) {
         MacroAction original = this.macro.actions.get(index);
         this.pushStructuralHistoryStep();
         CompoundTag tag = original.toTag();
         MacroAction copy = PackUtilMacro.createActionFromTag(tag);
         if (copy != null) {
            this.macro.actions.add(index + 1, copy);
            this.handleMacroActionsChanged();
         }
      }
   }

   private String getTooltipFor(String label) {
      String clean = label.replaceAll("Ãƒâ€šÃ‚Â§.", "");

      return switch (clean) {
         case "Save" -> {
            if (this.macro == null) {
               yield "Save this macro";
            }

            String name = this.macro.name == null ? "" : this.macro.name.trim();
            if (name.isEmpty()) {
               yield "Give the macro a name first";
            }

            if (!this.isValidName(this.macro.name)) {
               yield "Pick a unique macro name before saving";
            }

            yield "Save this macro";
         }
         case "Run" -> {
            String blockedReason = this.getRunBlockedReason();
            yield blockedReason != null ? blockedReason : "Run this macro";
         }
         case "Stop" -> "Stop the running macro";
         case "Add Action" -> "Open categorized action picker";
         case "Add Conditional" -> "Open categorized wait/condition picker";
         case "Undo" -> "Undo the last macro edit";
         case "Redo" -> "Redo the last undone macro edit";
         case "Chat" -> "Send a chat message or /command";
         case "Delay" -> "Wait N milliseconds or ticks";
         case "Packet" -> "Send a captured network packet";
         case "Item Click" -> "Click an inventory slot";
         case "Mouse Clk" -> "Simulate L/R/M click in world";
         case "Rotate" -> "Set player yaw & pitch";
         case "Use Item" -> "Use held item (right-click action)";
         case "Open Inv" -> "Open player inventory screen";
         case "Slot" -> "Select a hotbar slot (1-9)";
         case "Drop" -> "Drop items from slot";
         case "Close GUI" -> "Close the current screen/GUI";
         case "Swap" -> "Swap two inventory slots";
         case "Wait HP" -> "Pause until health crosses the selected threshold";
         case "Wait Block" -> "Pause until block changes";
         case "Wait Pkt" -> "Pause until packet received";
         case "Wait Item" -> "Pause until item in inventory";
         case "WaitGUI", "Wait GUI" -> "Pause until screen opens";
         case "Tick Sync" -> "Wait for next client tick";
         case "Rev Sync" -> "Sync handler revision";
         case "Srv Sync" -> "Wait for server tick timing";
         case "Go To" -> "Send Baritone goto command";
         case "Disconn" -> "Disconnect / Kick (lag+kick) / Kick Dupe (lag+bundle/action+kick)";
         case "NBT Book" -> "Sign large-data books (random/custom text, multi-book, delay)";
         case "Wait LAN" -> "Wait for LAN sync peer to reach a macro step";
         case "Module" -> "Toggle a Meteor module";
         case "Sneak" -> "Hold or release sneak key";
         case "Jump" -> "Press jump key for N ticks";
         case "Sprint" -> "Toggle sprint on/off";
         case "Move" -> "Walk in direction for N ticks";
         case "Look At" -> "Look at a block position";
         case "Repeat" -> "Repeat next N steps M times";
         case "Open Cont" -> "Open a container at a block position";
         case "Desync" -> "Send close packet without closing GUI (desync server state)";
         case "Cls w/o Pkt" -> "Close GUI locally without sending close packet to server";
         case "Restore GUI" -> "Restore a previously saved GUI screen and handler";
         case "Save GUI" -> "Save the current GUI screen and handler for later restore";
         case "Send Toggle" -> "Toggle whether packets are sent to server (on/off)";
         case "Delay Pkts" -> "Toggle packet delay mode (queues outgoing packets)";
         case "Store" -> "Loot/store items between container and player, or move slotÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢slot";
         case "Wait CD" -> "Pause until cooldown expires";
         case "Wait Pos" -> "Pause until position reached";
         case "Wait Chat" -> "Pause until chat message received";
         case "Wait Ent" -> "Pause until entity appears nearby";
         case "Wait Slot" -> "Pause until slot content changes";
         case "Wait Snd" -> "Pause until a matching sound plays";
         case "Mine" -> "Mine target blocks via Baritone until stop conditions are met";
         case "Pay" -> "Pay a list of players with a configurable command, amount, and delay";
         default -> null;
      };
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.visible) {
         return false;
      } else if (this.isBindingKey) {
         if (keyCode == 256) {
            this.macro.keyCode = -1;
         } else {
            this.macro.keyCode = keyCode;
         }

         this.isBindingKey = false;
         this.recreateComponents();
         return true;
      } else {
         KeyEvent keyInput = new KeyEvent(keyCode, scanCode, modifiers);
         if (this.isStepPickerOpen()) {
            if (keyCode == 256 || keyCode == 257) {
               this.closeStepPicker();
            }

            return true;
         } else if (keyCode == 256) {
            boolean anyFocused = this.nameField.isFocused() || this.loopMode == 1 && this.loopCountField != null && this.loopCountField.isFocused();
            if (anyFocused) {
               this.nameField.setFocused(false);
               if (this.loopCountField != null) {
                  this.loopCountField.setFocused(false);
               }

               return true;
            } else {

               if (MC.screen != null) {
                  MC.execute(() -> MC.setScreen(null));
               } else {

                  this.close();
               }
               return true;
            }
         } else {
            if (keyCode == 257) {
               if (this.nameField.isFocused()) {
                  this.nameField.setFocused(false);
                  return true;
               }

               if (this.loopMode == 1 && this.loopCountField != null && this.loopCountField.isFocused()) {
                  this.loopCountField.setFocused(false);
                  return true;
               }
            }

            if (this.nameField.keyPressed(keyInput)) {
               return true;
            } else {
               return this.loopMode == 1 && this.loopCountField != null && this.loopCountField.keyPressed(keyInput)
                  ? true
                  : this.nameField.isFocused() || this.loopMode == 1 && this.loopCountField != null && this.loopCountField.isFocused();
            }
         }
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (!this.visible) {
         return false;
      } else if (this.isStepPickerOpen()) {
         return true;
      } else {
         CharacterEvent charInput = new CharacterEvent(chr);
         if (this.nameField.charTyped(charInput)) {
            return true;
         } else if (this.nameField.isFocused()) {
            this.nameField.write(String.valueOf(chr));
            return true;
         } else {
            if (this.loopMode == 1 && this.loopCountField != null) {
               if (this.loopCountField.charTyped(charInput)) {
                  return true;
               }

               if (this.loopCountField.isFocused()) {
                  this.loopCountField.write(String.valueOf(chr));
                  return true;
               }
            }

            return true;
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (!this.visible) {
         return false;
      } else if (this.isStepPickerOpen()) {
         int pickerX = this.getStepPickerX();
         int pickerY = this.getStepPickerY();
         int pickerW = this.getStepPickerWidth();
         int pickerH = this.getStepPickerHeight();
         int contentX = pickerX + 6;
         int contentY = pickerY + 22;
         int contentW = pickerW - 12;
         int contentH = pickerH - 28;
         if (mouseX >= contentX && mouseX < contentX + contentW && mouseY >= contentY && mouseY < contentY + contentH) {
            int contentHeight = this.getStepPickerContentHeight();
            int maxScroll = Math.max(0, contentHeight - contentH);
            this.stepPickerScrollOffset = Math.max(0, Math.min(maxScroll, this.stepPickerScrollOffset - (int)(Math.signum(amount) * 36.0)));
            return true;
         }

         return true;
      } else {
        int listY = this.getStepListY();
        int listHeight = this.getStepListHeight();
        if (mouseY >= listY && mouseY <= listY + listHeight && mouseX >= this.panelX + 10 && mouseX <= this.panelX + this.PANEL_WIDTH - 10) {
           int maxScroll = this.getMaxStepListScroll();
            this.scrollOffset = this.quantizeScrollOffset(
               this.scrollOffset - (int)(Math.signum(amount) * (double)this.actionRowHeight()),
               this.actionRowHeight(),
               maxScroll
            );
            return true;
        }

         return false;
      }
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.activeScrollbarDrag == 2) {
         this.stepPickerScrollOffset = PackUiScrollbar.scrollFromThumb(this.getStepPickerScrollbarMetrics(), (int)mouseY, this.scrollbarGrabOffset);
         return true;
      } else if (this.activeScrollbarDrag == 1) {
         PackUiScrollbar.Metrics metrics = this.getStepListScrollbarMetrics();
         this.scrollOffset = this.quantizeScrollOffset(
            PackUiScrollbar.scrollFromThumb(metrics, (int)mouseY, this.scrollbarGrabOffset),
            this.actionRowHeight(),
            metrics.maxScroll()
         );
         return true;
      } else if (button == 0 && (this.draggingIndex >= 0 || this.pressedStepIndex >= 0)) {
         this.tryStartStepDrag(mouseX, mouseY);
         if (this.draggingIndex >= 0) {
            this.updateStepDrag(mouseY);
         }

         return true;
      } else if (this.isWindowResizing && button == 0) {
         int nextWidth = this.resizeStartWidth + (int)Math.round(mouseX - this.resizeStartMouseX);
         int nextHeight = this.resizeStartHeight + (int)Math.round(mouseY - this.resizeStartMouseY);
         PackUtilWindowLayout nextBounds = this.clampToScreen(
            this, new PackUtilWindowLayout(this.panelX, this.panelY, nextWidth, nextHeight, this.visible, this.collapsed)
         );
         this.PANEL_WIDTH = nextBounds.width;
         this.PANEL_HEIGHT = nextBounds.height;
         this.panelX = nextBounds.x;
         this.panelY = nextBounds.y;
         this.recreateComponents();
         this.saveState();
         return true;
      } else if (this.isWindowDragging && button == 0) {
         this.panelX = (int)(mouseX - this.dragOffsetX);
         this.panelY = (int)(mouseY - this.dragOffsetY);
         int screenW = PackUtilUiScale.getVirtualScreenWidth();
         int screenH = PackUtilUiScale.getVirtualScreenHeight();
         int minVisibleWidth = Math.min(this.PANEL_WIDTH, 96);
         this.panelX = Math.max(Math.min(0, screenW - this.PANEL_WIDTH), Math.min(this.panelX, Math.max(0, screenW - minVisibleWidth)));
         this.panelY = Math.max(0, Math.min(screenH - HEADER_HEIGHT, this.panelY));
         if (this.panelX != this.headerPressPanelX
            || this.panelY != this.headerPressPanelY
            || Math.abs(mouseX - this.headerPressMouseX) >= 3.0
            || Math.abs(mouseY - this.headerPressMouseY) >= 3.0) {
            this.headerDragMoved = true;
         }

         this.recreateComponents();
         this.saveState();
         return true;
      } else {
         return false;
      }
   }

   public boolean shouldRenderAbstractContainerScreenCaptureBanner() {
      return false;
   }

   public String getAbstractContainerScreenCaptureTitle() {
      return "";
   }

   public String getAbstractContainerScreenCaptureInstruction() {
      return "";
   }

   public String getAbstractContainerScreenCaptureHoverText(Slot slot, String itemName, String registryId) {
      return "";
   }

   public boolean wantsSlotCapture() {
      return false;
   }

   public boolean onSlotRightClick(Slot slot, String itemName, String registryId) {
      return false;
   }

   private static final class StepRowSnapshot {
      private final MacroAction action;
      private final int stableId;
      private final String label;
      private final boolean conditional;
      private final int index;

      private StepRowSnapshot(MacroAction action, int stableId, String label, boolean conditional, int index) {
         this.action = action;
         this.stableId = stableId;
         this.label = label;
         this.conditional = conditional;
         this.index = index;
      }
   }

   private enum StepRowMotionKind {
      ENTER,
      MOVE,
      EXIT
   }

   private static final class StepRowMotion {
      private final int stableId;
      private final String label;
      private final boolean conditional;
      private final int fromRowIndex;
      private final int toRowIndex;
      private final PackUtilMacroEditorOverlay.StepRowMotionKind kind;
      private float progress;

      private StepRowMotion(
         int stableId,
         String label,
         boolean conditional,
         int fromRowIndex,
         int toRowIndex,
         PackUtilMacroEditorOverlay.StepRowMotionKind kind
      ) {
         this.stableId = stableId;
         this.label = label;
         this.conditional = conditional;
         this.fromRowIndex = fromRowIndex;
         this.toRowIndex = toRowIndex;
         this.kind = kind;
      }
   }

   private static final class StepPickerButtonLayout {
      private final PackUtilMacroEditorOverlay.StepPickerEntry entry;
      private final int x;
      private final int y;
      private final int width;
      private final int height;

      private StepPickerButtonLayout(PackUtilMacroEditorOverlay.StepPickerEntry entry, int x, int y, int width, int height) {
         this.entry = entry;
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }
   }

   private static final class StepPickerCategory {
      private final String id;
      private final String label;
      private final int color;

      private StepPickerCategory(String id, String label, int color) {
         this.id = id;
         this.label = label;
         this.color = color;
      }
   }

   private static final class StepPickerEntry {
      private final String categoryId;
      private final String label;
      private final String description;
      private final Runnable action;

      private StepPickerEntry(String categoryId, String label, String description, Runnable action) {
         this.categoryId = categoryId;
         this.label = label;
         this.description = description;
         this.action = action;
      }
   }

   private static enum StepPickerMode {
      ACTION,
      CONDITIONAL;
   }

   private static final class StepPickerSectionLayout {
      private final PackUtilMacroEditorOverlay.StepPickerCategory category;
      private final int headerY;
      private final List<PackUtilMacroEditorOverlay.StepPickerButtonLayout> buttons;
      private final int bottomY;

      private StepPickerSectionLayout(
         PackUtilMacroEditorOverlay.StepPickerCategory category, int headerY, List<PackUtilMacroEditorOverlay.StepPickerButtonLayout> buttons, int bottomY
      ) {
         this.category = category;
         this.headerY = headerY;
         this.buttons = buttons;
         this.bottomY = bottomY;
      }
   }
}
