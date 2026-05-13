package autismclient.util;

import autismclient.util.macro.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PackUtilMacro {
    public String name = "New Macro";
    public String description = "";
    public boolean loop = false;
    public int loopCount = -1;
    public int keyCode = -1;
    public List<MacroAction> actions = new ArrayList<>();

    public PackUtilMacro() {}

    public PackUtilMacro(String name) {
        this.name = name;
    }

    public PackUtilMacro deepCopy() {
        return new PackUtilMacro().fromTag(this.toTag());
    }

    public PackUtilMacro deepCopy(String newName) {
        PackUtilMacro copy = deepCopy();
        if (newName != null && !newName.isBlank()) {
            copy.name = newName;
        }
        return copy;
    }

    public void execute() {
        execute(true);
    }

    public void execute(boolean regenerate) {
        if (actions.isEmpty()) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cMacro has no actions!");
            return;
        }

        if (regenerate) {
            regenerateAllPackets();
        }
        MacroExecutor.execute(this);
    }

    public void regenerateAllPackets() {
        for (MacroAction action : actions) {
            if (action instanceof SendPacketAction) {
                ((SendPacketAction) action).regeneratePackets();
            }
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("description", description);
        tag.putBoolean("loop", loop);
        tag.putInt("loopCount", loopCount);
        tag.putInt("keyCode", keyCode);

        ListTag actionsList = new ListTag();
        for (MacroAction action : actions) {
            actionsList.add(action.toTag());
        }
        tag.put("actions", actionsList);

        return tag;
    }

    public PackUtilMacro fromTag(CompoundTag tag) {
        if (tag.contains("name")) name = tag.getStringOr("name", "");
        if (tag.contains("description")) description = tag.getStringOr("description", "");
        if (tag.contains("loop")) loop = tag.getBooleanOr("loop", false);
        if (tag.contains("loopCount")) loopCount = tag.getIntOr("loopCount", -1);
        if (tag.contains("keyCode")) keyCode = tag.getIntOr("keyCode", -1);

        if (tag.contains("actions")) {
            actions.clear();
            ListTag actionsList = (ListTag) tag.get("actions");
            for (Tag element : actionsList) {
                if (element instanceof CompoundTag) {
                    CompoundTag actionTag = (CompoundTag) element;
                    if (actionTag.contains("type")) {
                        try {
                            String typeName = actionTag.getStringOr("type", "");
                            MacroActionType type = MacroActionType.valueOf(typeName);
                            MacroAction action = null;
                            switch (type) {
                                case DELAY: action = new DelayAction(); break;
                                case PACKET: action = new PacketAction(); break;
                                case WAIT_PACKET: action = new WaitForPacketAction(); break;
                                case WAIT_HEALTH: action = new WaitForHealthAction(); break;
                                case WAIT_ITEM: {

                                    WaitForSlotChangeAction migrated = new WaitForSlotChangeAction();
                                    migrated.fromTagLegacyItem(actionTag);
                                    action = migrated;
                                    break;
                                }
                                case WAIT_BLOCK: action = new WaitForBlockAction(); break;
                                case WAIT_GUI: action = new WaitForGuiAction(); break;
                                case CLICK: action = new ClickAction(); break;
                                case ROTATE: action = new RotateAction(); break;
                                case USE_ITEM: action = new UseItemAction(); break;
                                case INVENTORY: action = new InventoryAction(); break;
                                case SEND_PACKET: action = new SendPacketAction(); break;
                                case CRAFT: action = new CraftAction(); break;
                                case SELECT_SLOT: action = new SelectSlotAction(); break;
                                case XCARRY: action = new XCarryAction(); break;
                                case DROP: action = new DropAction(); break;
                                case ITEM: action = new ItemAction(); break;
                                case TICK_SYNC: action = new TickSyncAction(); break;
                                case REVISION_SYNC: action = new RevisionSyncAction(); break;
                                case SERVER_TICK_SYNC: action = new ServerTickSyncAction(); break;
                                case CLOSE_GUI: action = new CloseGuiAction(); break;
                                case SWAP_SLOTS: action = new SwapSlotsAction(); break;
                                case WAIT_COOLDOWN: action = new WaitForCooldownAction(); break;
                                case GO_TO: action = new GoToAction(); break;
                                case WAIT_POS: action = new WaitPosAction(); break;
                                case PAYLOAD: action = new PayloadAction(); break;
                                case DISCONNECT: action = new DisconnectAction(); break;
                                case TOGGLE_MODULE: action = new ToggleModuleAction(); break;
                                case START_MACRO: action = new StartMacroAction(); break;
                                case STOP_MACRO: action = new StopMacroAction(); break;
                                case SNEAK: action = new SneakAction(); break;
                                case JUMP: action = new JumpAction(); break;
                                case SPRINT: action = new SprintAction(); break;
                                case MOVE: action = new MoveAction(); break;
                                case LOOK_AT_BLOCK: action = new LookAtBlockAction(); break;
                                case REPEAT: action = new RepeatAction(); break;
                                case WAIT_CHAT: action = new WaitForChatAction(); break;
                                case WAIT_ENTITY: action = new WaitForEntityAction(); break;
                                case WAIT_SLOT_CHANGE: action = new WaitForSlotChangeAction(); break;
                                case OPEN_CONTAINER: action = new OpenContainerAction(); break;
                                case DESYNC: action = new DesyncAction(); break;
                                case RESTORE_GUI: action = new RestoreGuiAction(); break;
                                case SAVE_GUI: action = new SaveGuiAction(); break;
                                case SEND_TOGGLE: action = new SendToggleAction(); break;
                                case DELAY_PACKETS: action = new DelayPacketsAction(); break;
                                case INVENTORY_AUDIT: action = new InventoryAuditAction(); break;
                                case STORE_ITEM: action = new StoreItemAction(); break;
                                case WAIT_SOUND: action = new WaitForSoundAction(); break;
                                case MINE: action = new MineAction(); break;
                                case INSTA_BREAK: action = new InstaBreakAction(); break;
                                case PAY: action = new PayAction(); break;
                                case NBT_BOOK: action = new NbtBookAction(); break;
                                case SEND_CHAT: action = new SendChatAction(); break;
                                case WAIT_LAN_STEP: action = new WaitForLanStepAction(); break;
                                case WAIT_MACRO_STEP: action = new WaitForMacroStepAction(); break;
                            }

                            if (action != null) {

                                if (type != MacroActionType.WAIT_ITEM) action.fromTag(actionTag);
                                actions.add(action);
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }
        }
        return this;
    }

    public static MacroAction createActionFromTag(CompoundTag actionTag) {
        if (actionTag == null || !actionTag.contains("type")) return null;
        try {
            String typeName = actionTag.getStringOr("type", "");
            MacroActionType type = MacroActionType.valueOf(typeName);
            MacroAction action = null;
            switch (type) {
                case DELAY: action = new DelayAction(); break;
                case PACKET: action = new PacketAction(); break;
                case WAIT_PACKET: action = new WaitForPacketAction(); break;
                case WAIT_HEALTH: action = new WaitForHealthAction(); break;
                case WAIT_ITEM: {
                    WaitForSlotChangeAction migrated = new WaitForSlotChangeAction();
                    migrated.fromTagLegacyItem(actionTag);
                    action = migrated;
                    break;
                }
                case WAIT_BLOCK: action = new WaitForBlockAction(); break;
                case WAIT_GUI: action = new WaitForGuiAction(); break;
                case CLICK: action = new ClickAction(); break;
                case ROTATE: action = new RotateAction(); break;
                case USE_ITEM: action = new UseItemAction(); break;
                case INVENTORY: action = new InventoryAction(); break;
                case SEND_PACKET: action = new SendPacketAction(); break;
                case CRAFT: action = new CraftAction(); break;
                case SELECT_SLOT: action = new SelectSlotAction(); break;
                case XCARRY: action = new XCarryAction(); break;
                case DROP: action = new DropAction(); break;
                case ITEM: action = new ItemAction(); break;
                case TICK_SYNC: action = new TickSyncAction(); break;
                case REVISION_SYNC: action = new RevisionSyncAction(); break;
                case SERVER_TICK_SYNC: action = new ServerTickSyncAction(); break;
                case CLOSE_GUI: action = new CloseGuiAction(); break;
                case SWAP_SLOTS: action = new SwapSlotsAction(); break;
                case WAIT_COOLDOWN: action = new WaitForCooldownAction(); break;
                case GO_TO: action = new GoToAction(); break;
                case WAIT_POS: action = new WaitPosAction(); break;
                case PAYLOAD: action = new PayloadAction(); break;
                case DISCONNECT: action = new DisconnectAction(); break;
                case TOGGLE_MODULE: action = new ToggleModuleAction(); break;
                case START_MACRO: action = new StartMacroAction(); break;
                case STOP_MACRO: action = new StopMacroAction(); break;
                case SNEAK: action = new SneakAction(); break;
                case JUMP: action = new JumpAction(); break;
                case SPRINT: action = new SprintAction(); break;
                case MOVE: action = new MoveAction(); break;
                case LOOK_AT_BLOCK: action = new LookAtBlockAction(); break;
                case REPEAT: action = new RepeatAction(); break;
                case WAIT_CHAT: action = new WaitForChatAction(); break;
                case WAIT_ENTITY: action = new WaitForEntityAction(); break;
                case WAIT_SLOT_CHANGE: action = new WaitForSlotChangeAction(); break;
                case OPEN_CONTAINER: action = new OpenContainerAction(); break;
                case DESYNC: action = new DesyncAction(); break;
                case RESTORE_GUI: action = new RestoreGuiAction(); break;
                case SAVE_GUI: action = new SaveGuiAction(); break;
                case SEND_TOGGLE: action = new SendToggleAction(); break;
                case DELAY_PACKETS: action = new DelayPacketsAction(); break;
                case INVENTORY_AUDIT: action = new InventoryAuditAction(); break;
                case STORE_ITEM: action = new StoreItemAction(); break;
                case WAIT_SOUND: action = new WaitForSoundAction(); break;
                case MINE: action = new MineAction(); break;
                case INSTA_BREAK: action = new InstaBreakAction(); break;
                case PAY: action = new PayAction(); break;
                case NBT_BOOK: action = new NbtBookAction(); break;
                case SEND_CHAT: action = new SendChatAction(); break;
                case WAIT_LAN_STEP: action = new WaitForLanStepAction(); break;
                case WAIT_MACRO_STEP: action = new WaitForMacroStepAction(); break;
            }
            if (action != null) {
                if (type != MacroActionType.WAIT_ITEM) action.fromTag(actionTag);
            }
            return action;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackUtilMacro that = (PackUtilMacro) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

