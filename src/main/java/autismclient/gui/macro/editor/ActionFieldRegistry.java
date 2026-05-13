package autismclient.gui.macro.editor;

import autismclient.util.macro.MacroActionType;
import autismclient.util.macro.WaitForHealthAction;

import java.util.EnumMap;
import java.util.Map;

public final class ActionFieldRegistry {

    private static final ActionFieldSchema EMPTY = ActionFieldSchema.builder().build();
    private static final Map<MacroActionType, ActionFieldSchema> SCHEMAS =
            new EnumMap<>(MacroActionType.class);

    static {

        SCHEMAS.put(MacroActionType.DELAY, ActionFieldSchema.builder()
                .toggle("useTicks",   "Use Ticks")
                .number("delayMs",    "Delay (ms)")   .range(0, 300_000) .hideWhen("useTicks")
                .number("delayTicks", "Delay (ticks)").range(0, 20_000)  .showWhen("useTicks")
                .build());

        SCHEMAS.put(MacroActionType.PACKET, ActionFieldSchema.builder()
                .text  ("description", "Description")
                .toggle("regenerate",  "Regenerate")
                .build());

        SCHEMAS.put(MacroActionType.WAIT_PACKET, EMPTY);

        SCHEMAS.put(MacroActionType.WAIT_HEALTH, ActionFieldSchema.builder()
                .decimal("healthThreshold", "Target Health").decRange(0, 20)
                .enumField("comparison", "Condition",
                        WaitForHealthAction.COMPARISON_DROPS_BELOW,
                        WaitForHealthAction.COMPARISON_RISES_ABOVE)
                .build());

        SCHEMAS.put(MacroActionType.WAIT_ITEM, ActionFieldSchema.builder()
                .stringList("itemNames", "Items").addLabel("Add Item").captureItemSlot()
                .toggle    ("waitForGui",  "Wait for GUI")
                .text      ("guiName",     "GUI Name").showWhen("waitForGui")
                .build());

        SCHEMAS.put(MacroActionType.WAIT_BLOCK, ActionFieldSchema.builder()
                .enumField  ("checkMode",    "Check Mode",    "AT_POSITION", "IN_REACH", "LOOKING_AT")
                .enumField  ("waitBehavior", "Wait For",      "PLACED", "DESTROYED")
                .toggle     ("anyBlock",     "Any Block")
                .stringList ("blockIds",     "Block IDs")    .addLabel("Add Block").captureBlock().hideWhen("anyBlock")
                .blockPos   ("pos",          "Position").captureBlock().showWhenEnum("checkMode", "AT_POSITION")
                .toggle     ("mustBeInReach","Must Be In Reach").showWhenEnum("checkMode", "AT_POSITION")
                .decimal    ("searchRadius", "Search Radius").decRange(0, 32).showWhenEnum("checkMode", "IN_REACH")
                .build());

        SCHEMAS.put(MacroActionType.WAIT_GUI, ActionFieldSchema.builder()
                .enumField("waitMode", "Wait Mode", "OPEN", "CLOSE")
                .text     ("guiTitle", "GUI Title")
                .build());

        SCHEMAS.put(MacroActionType.CLICK, ActionFieldSchema.builder()
                .enumField("clickType",  "Click Type", "LEFT", "RIGHT")
                .number   ("clickCount", "Click Count").range(1, 100)
                .toggle   ("waitForGui", "Wait for GUI")
                .text     ("guiName",    "GUI Name").showWhen("waitForGui")
                .build());

        SCHEMAS.put(MacroActionType.ROTATE, ActionFieldSchema.builder()
                .decimal("yaw",               "Yaw")              .decRange(-180, 180)
                .decimal("pitch",             "Pitch")            .decRange(-90, 90)
                .toggle ("smooth",            "Smooth")
                .number ("smoothness",        "Smoothness")       .range(1, 10).showWhen("smooth")
                .toggle ("waitForCompletion", "Wait for Completion")
                .build());

        SCHEMAS.put(MacroActionType.USE_ITEM, ActionFieldSchema.builder()
                .text     ("itemName", "Item Name").captureItemSlot()
                .enumField("useMode",  "Use Mode", "AUTOMATIC", "CUSTOM_HOLD")
                .number   ("holdTicks","Hold Ticks").range(1, 1000).showWhenEnum("useMode", "CUSTOM_HOLD")
                .number   ("useCount", "Use Count") .range(1, 1000).showWhenEnum("useMode", "AUTOMATIC")
                .build());

        SCHEMAS.put(MacroActionType.INVENTORY, ActionFieldSchema.builder()
                .enumField("mode",       "Mode", "OPEN", "CLOSE")
                .toggle   ("waitForGui", "Wait for GUI").showWhenEnum("mode", "OPEN")
                .toggle   ("sendPacket", "Close without pkt").showWhenEnum("mode", "CLOSE")
                .build());

        SCHEMAS.put(MacroActionType.SEND_PACKET, ActionFieldSchema.builder()
                .text  ("customName", "Custom Name")
                .toggle("waitForGui", "Wait for GUI")
                .text  ("guiName",    "GUI Name").showWhen("waitForGui")
                .build());

        SCHEMAS.put(MacroActionType.PAYLOAD, EMPTY);

        SCHEMAS.put(MacroActionType.CRAFT, EMPTY);

        SCHEMAS.put(MacroActionType.SELECT_SLOT, ActionFieldSchema.builder()
                .slot("slot",     "Slot")
                .text("itemName", "Item Name").captureItemSlot()
                .build());

        SCHEMAS.put(MacroActionType.XCARRY, ActionFieldSchema.builder()
                .enumField  ("mode",    "Mode",  "PUT_IN", "TAKE_OUT", "DROP")
                .enumField  ("transferMode", "Transfer", "FAST", "CLICK").showWhenEnum("mode", "PUT_IN")
                .toggle     ("carryCursor", "Carry Cursor")
                .stringList ("entries", "Items") .addLabel("Add Item").captureItemSlot()
                .build());

        SCHEMAS.put(MacroActionType.DROP, EMPTY);

        SCHEMAS.put(MacroActionType.ITEM, EMPTY);

        SCHEMAS.put(MacroActionType.TICK_SYNC, ActionFieldSchema.builder()
                .number("tickOffset",  "Tick Offset")    .range(0, 20)
                .number("preGenCount", "Pre-gen Count")  .range(0, 100)
                .build());

        SCHEMAS.put(MacroActionType.REVISION_SYNC, ActionFieldSchema.builder()
                .number("revisionOffset", "Revision Offset").range(0, 100)
                .number("preGenCount",    "Pre-gen Count")  .range(0, 100)
                .build());

        SCHEMAS.put(MacroActionType.SERVER_TICK_SYNC, ActionFieldSchema.builder()
                .number("bufferMs",    "Buffer (ms)")    .range(0, 5_000)
                .number("maxWaitMs",   "Max Wait (ms)")  .range(100, 60_000)
                .toggle("ignorePing",  "Ignore Ping")
                .number("preGenCount", "Pre-gen Count")  .range(0, 100)
                .build());

        SCHEMAS.put(MacroActionType.CLOSE_GUI, ActionFieldSchema.builder()
                .text  ("guiName",      "GUI Name")
                .toggle("useItemFilter","Filter by Item")
                .text  ("itemName",     "Item Name") .captureItemSlot().showWhen("useItemFilter")
                .slot  ("targetSlot",   "Target Slot").showWhen("useItemFilter")
                .toggle("sendPacket",   "Close without pkt")
                .build());

        SCHEMAS.put(MacroActionType.SWAP_SLOTS, ActionFieldSchema.builder()
                .toggle("fromUseItemName", "From: Use Item Name")
                .text  ("fromItemName",    "From: Item Name").showWhen("fromUseItemName")
                .slot  ("fromSlot",        "From: Slot")     .captureItemSlot().hideWhen("fromUseItemName")
                .toggle("toUseItemName",   "To: Use Item Name")
                .text  ("toItemName",      "To: Item Name")  .showWhen("toUseItemName")
                .slot  ("toSlot",          "To: Slot")       .captureItemSlot().hideWhen("toUseItemName")
                .build());

        SCHEMAS.put(MacroActionType.WAIT_COOLDOWN, ActionFieldSchema.builder()
                .text  ("itemName",     "Item Name").captureItemSlot()
                .toggle("checkMainHand","Check Main Hand")
                .build());

        SCHEMAS.put(MacroActionType.GO_TO, ActionFieldSchema.builder()
                .blockPos("pos",            "Target Position").xyzDouble(true)
                .toggle  ("waitForArrival", "Wait for Arrival")
                .build());

        SCHEMAS.put(MacroActionType.WAIT_POS, ActionFieldSchema.builder()
                .blockPos("pos",        "Position")      .xyzDouble(true).captureBlock()
                .decimal ("leeway",     "Leeway")        .decRange(0, 100)
                .toggle  ("checkRotation","Check Rotation")
                .decimal ("yaw",        "Yaw")           .decRange(-180, 180).showWhen("checkRotation")
                .decimal ("pitch",      "Pitch")         .decRange(-90, 90)  .showWhen("checkRotation")
                .decimal ("rotLeeway",  "Rotation Leeway").decRange(0, 180)  .showWhen("checkRotation")
                .build());

        SCHEMAS.put(MacroActionType.DISCONNECT, ActionFieldSchema.builder()
                .enumField("mode",          "Mode",         "DISCONNECT", "KICK", "KICK_DUPE", "AUTO_DISCONNECT")
                .number   ("delayMs",       "Delay (ms)")   .range(0, 10_000).showWhenEnum("mode", "DISCONNECT")
                .enumField("lagMethod",     "Lag Method",   "CLICK_SLOT", "BOAT_NBT", "ENTITY_NBT")
                                                           .hideWhenEnum("mode", "DISCONNECT")
                                                           .hideWhenEnum("mode", "AUTO_DISCONNECT")
                .enumField("kickMethod",    "Kick Method",  "HURT", "CLIENT_SETTINGS", "INVALID_SLOT")
                                                           .hideWhenEnum("mode", "DISCONNECT")
                                                           .hideWhenEnum("mode", "AUTO_DISCONNECT")
                .number   ("packetCount",   "Packet Count") .range(1, 1000).hideWhenEnum("mode", "DISCONNECT")
                                                                .hideWhenEnum("mode", "AUTO_DISCONNECT")
                .toggle   ("useNextAction", "Use Next Action").showWhenEnum("mode", "KICK_DUPE")

                .enumField("trigger",       "Trigger",      "TELEPORT", "POSITION", "WORLD_CHANGE", "GUI_CLOSE", "INVENTORY_CLEAR")
                                                           .showWhenEnum("mode", "AUTO_DISCONNECT")
                .decimal  ("tolerance",     "Tolerance")    .decRange(0, 100).showWhenEnum("trigger", "POSITION")
                .number   ("bufferMs",      "Buffer (ms)")  .range(0, 1000).showWhenEnum("mode", "AUTO_DISCONNECT")
                .number   ("timeoutSec",     "Timeout (sec)") .range(1, 300).showWhenEnum("mode", "AUTO_DISCONNECT")
                .build());

        SCHEMAS.put(MacroActionType.TOGGLE_MODULE, ActionFieldSchema.builder()
                .text     ("moduleName",  "Module Name")
                .enumField("toggleMode",  "Toggle Mode", "TOGGLE", "ENABLE", "DISABLE")
                .build());

        SCHEMAS.put(MacroActionType.START_MACRO, ActionFieldSchema.builder()
                .macroSelect("macroName",        "Macro")
                .toggle("restartIfRunning", "Restart If Running")
                .build());

        SCHEMAS.put(MacroActionType.STOP_MACRO, ActionFieldSchema.builder()
                .enumField("target",    "Target", "SELF", "SELECTED", "ALL")
                .macroSelect("macroName", "Macro").showWhenEnum("target", "SELECTED")
                .build());

        SCHEMAS.put(MacroActionType.SNEAK, ActionFieldSchema.builder()
                .toggle("sneak",      "Sneak")
                .toggle("persistent", "Persistent")
                .build());

        SCHEMAS.put(MacroActionType.JUMP, ActionFieldSchema.builder()
                .toggle("tap",           "Tap (single tick)")
                .number("durationTicks", "Duration (ticks)").range(1, 200).hideWhen("tap")
                .build());

        SCHEMAS.put(MacroActionType.SPRINT, ActionFieldSchema.builder()
                .toggle("sprint",     "Sprint")
                .toggle("persistent", "Persistent")
                .build());

        SCHEMAS.put(MacroActionType.MOVE, ActionFieldSchema.builder()
                .enumField("direction",     "Direction",      "FORWARD", "BACKWARD", "LEFT", "RIGHT")
                .number   ("durationTicks", "Duration (ticks)").range(1, 10_000)
                .toggle   ("nonBlocking",   "Non-blocking")
                .build());

        SCHEMAS.put(MacroActionType.LOOK_AT_BLOCK, ActionFieldSchema.builder()
                .enumField("targetMode", "Target Mode", "SPECIFIC", "BLOCK", "ENTITY")
                .blockPos("blockPos", "Block Position").xyzKeys("blockX", "blockY", "blockZ").captureBlock().showWhenEnum("targetMode", "SPECIFIC")
                .decimal("searchRadius", "Search Radius").decRange(1, 64).showWhenEnum("targetMode", "BLOCK").showWhenEnum("targetMode", "ENTITY")
                .stringList("blockIds", "Blocks").captureCatalog().showWhenEnum("targetMode", "BLOCK")
                .stringList("entityIds", "Entities").captureEntity().addLabel("Entity").showWhenEnum("targetMode", "ENTITY")
                .toggle("smooth", "Smooth")
                .number("smoothness", "Smoothness").range(1, 10).showWhen("smooth")
                .toggle("waitForCompletion", "Wait for Completion")
                .build());

        SCHEMAS.put(MacroActionType.REPEAT, ActionFieldSchema.builder()
                .number("stepCount",   "Steps to Repeat").range(1, 1000)
                .number("repeatCount", "Repeat Count")   .range(1, 10_000)
                .build());

        SCHEMAS.put(MacroActionType.WAIT_CHAT, ActionFieldSchema.builder()
                .text  ("pattern",     "Pattern")
                .toggle("useRegex",    "Use Regex")
                .number("fuzzyPercent","Match Strength").range(40, 100).hideWhen("useRegex")
                .number("timeoutMs",   "Timeout (ms)")  .range(0, 300_000)
                .toggle("waitForGui",  "Wait for GUI")
                .text  ("waitGuiName", "GUI Name")      .showWhen("waitForGui")
                .build());

        SCHEMAS.put(MacroActionType.WAIT_ENTITY, ActionFieldSchema.builder()
                .enumField ("checkMode",       "Check Mode",       "RADIUS", "LOOKING_AT", "WITHIN_REACH")
                .stringList("entityIds",       "Entity IDs")       .addLabel("Add Entity").captureEntity()
                .toggle    ("centerOnPlayer",  "Center on Player").showWhenEnum("checkMode", "RADIUS")
                .blockPos  ("pos",             "Position")         .xyzDouble(true).captureBlock()
                .decimal   ("radius",          "Radius")           .decRange(0, 100).showWhenEnum("checkMode", "RADIUS")
                .toggle    ("mustBeLookingAt", "Must Be Looking At").showWhenEnum("checkMode", "RADIUS")
                .build());

        SCHEMAS.put(MacroActionType.WAIT_SLOT_CHANGE, EMPTY);

        SCHEMAS.put(MacroActionType.OPEN_CONTAINER, ActionFieldSchema.builder()
                .enumField("targetMode", "Target", "BLOCK", "ENTITY", "LAST_TARGET")
                .blockPos("pos",       "Container Position").captureBlock().showWhenEnum("targetMode", "BLOCK")
                .stringList("entityTargets", "Container Entity").addLabel("Pick Entity").captureEntity().showWhenEnum("targetMode", "ENTITY")
                .toggle  ("waitForGui","Wait for GUI")
                .build());

        SCHEMAS.put(MacroActionType.DESYNC, EMPTY);

        SCHEMAS.put(MacroActionType.RESTORE_GUI, ActionFieldSchema.builder()
                .toggle("waitForGui", "Wait for GUI")
                .build());

        SCHEMAS.put(MacroActionType.SAVE_GUI, ActionFieldSchema.builder()
                .toggle("closeAfter", "Close After Saving")
                .toggle("sendPacket", "Close without pkt").showWhen("closeAfter")
                .build());

        SCHEMAS.put(MacroActionType.SEND_TOGGLE, ActionFieldSchema.builder()
                .enumField("mode", "Mode", "ENABLE", "DISABLE")
                .build());

        SCHEMAS.put(MacroActionType.DELAY_PACKETS, ActionFieldSchema.builder()
                .enumField ("mode",           "Mode",        "ENABLE", "DISABLE")
                .toggle    ("flushOnDisable",  "Flush on Disable").showWhenEnum("mode", "DISABLE")
                .stringList("c2sPackets",      "C2S Packets") .addLabel("Add C2S Packet").showWhenEnum("mode", "ENABLE")
                .stringList("s2cPackets",      "S2C Packets") .addLabel("Add S2C Packet").showWhenEnum("mode", "ENABLE")
                .build());

        SCHEMAS.put(MacroActionType.INVENTORY_AUDIT, ActionFieldSchema.builder()
                .enumField ("mode",              "Mode",                "DUPE", "DUPE_SPAM")
                .stringList("targetItems",       "Targets")            .addLabel("Add Item").captureItemSlot()

                .enumField ("openMode",          "Open Method",       "COMMAND", "CONTAINER")
                .text      ("openCommand",       "Open Command")
                .blockPos  ("containerPos",      "Container")         .xyzKeys("containerX", "containerY", "containerZ").captureBlock()

                .enumField ("dupeVector",        "Dupe Vector", "DESYNC_REOPEN", "CLOSE_NO_PACKET",
                                                        "SHIFT_CLICK_REOPEN", "DELAYED_PACKETS", "SWAP_HOTBAR", "DROP_EXPLOIT",
                                                        "DELAYED_DESYNC_REOPEN", "SWAP_DESYNC_REOPEN", "DROP_DELAYED_PACKETS")
                .number    ("delayBeforeReopen", "Delay Before (ms)") .range(0, 10_000)
                .number    ("delayAfterReopen",  "Delay After (ms)")  .range(0, 10_000)
                .number    ("iterations",        "Iterations")         .range(1, 100)
                .number    ("maxTransferAttempts", "Max Transfers")   .range(1, 20)
                .number    ("transferRetryDelayMs", "Retry Delay (ms)").range(10, 500)
                .toggle    ("multipleStacks",    "Multiple Stacks")

                .number    ("spamCount",         "Spam Count")        .range(1, 20).showWhenEnum("mode", "DUPE_SPAM")
                .number    ("spamDelayMs",         "Spam Delay (ms)")  .range(10, 1000).showWhenEnum("mode", "DUPE_SPAM")
                .build());

        SCHEMAS.put(MacroActionType.STORE_ITEM, ActionFieldSchema.builder()
                .enumField ("mode",        "Mode",               "LOOT", "STORE")
                .toggle    ("allItems",    "All Items")
                .stringList("targetItems", "Target Items")       .addLabel("Add Item").captureItemSlot().hideWhen("allItems")
                .toggle    ("persistent",  "Loop Forever")
                .toggle    ("closeAfter",  "Close After")        .hideWhen("persistent")
                .toggle    ("closeSendPkt","Close without pkt")  .showWhen("closeAfter")
                .build());

        SCHEMAS.put(MacroActionType.WAIT_SOUND, ActionFieldSchema.builder()
                .stringList("soundIds",      "Sound IDs")    .addLabel("Add Sound ID")
                .toggle    ("waitForGui",    "Wait for GUI")
                .text      ("waitGuiName",   "GUI Name")     .showWhen("waitForGui")
                .toggle    ("checkDistance", "Check Distance")
                .decimal   ("maxDistance",   "Max Distance") .decRange(0, 256).showWhen("checkDistance")
                .build());

        SCHEMAS.put(MacroActionType.MINE, ActionFieldSchema.builder()
                .stringList("targetBlocks",       "Target Blocks")       .addLabel("Add Block").captureCatalog()
                .toggle    ("stopInventoryFull",  "Stop: Inventory Full").exclusiveWith("stopSlotsUsed")
                .toggle    ("stopSlotsUsed",      "Stop: Slots Used")    .exclusiveWith("stopInventoryFull")
                .number    ("slotsUsedThreshold", "Slots Used Threshold").range(1, 36)   .showWhen("stopSlotsUsed")
                .toggle    ("stopMinedCount",     "Stop: Mined Count")
                .number    ("minedCountTarget",   "Mined Count Target")  .range(1, 10_000).showWhen("stopMinedCount")
                .toggle    ("stopAfterTime",      "Stop: After Time")
                .number    ("timeoutSeconds",     "Timeout (seconds)")   .range(1, 86400) .showWhen("stopAfterTime")
                .build());

        SCHEMAS.put(MacroActionType.INSTA_BREAK, ActionFieldSchema.builder()
                .blockPos  ("blockPos",            "Target Block").xyzKeys("x", "y", "z").captureBlock()
                .number    ("delayTicks",          "Delay").range(0, 20)
                .number    ("times",               "Times (0 = Infinite)").range(0, 10_000)
                .toggle    ("autoPickaxe",         "Auto Pickaxe")
                .toggle    ("manualDirection",     "Manual Direction")
                .enumField ("direction",           "Direction", "DOWN", "UP", "NORTH", "SOUTH", "WEST", "EAST").showWhen("manualDirection")
                .build());

        SCHEMAS.put(MacroActionType.PAY, ActionFieldSchema.builder()
                .text      ("commandTemplate", "Command Template")
                .text      ("amountInput",     "Amount")
                .toggle    ("delayEnabled",    "Use Delay")
                .number    ("delayMs",         "Delay (ms)").range(0, 60_000).showWhen("delayEnabled")
                .stringList("players",         "Players")  .addLabel("Add Player")
                .build());

        SCHEMAS.put(MacroActionType.SEND_CHAT, ActionFieldSchema.builder()
                .text  ("message",   "Message")
                .toggle("waitForGui","Wait for GUI")
                .text  ("guiName",   "GUI Name").showWhen("waitForGui")
                .build());

        SCHEMAS.put(MacroActionType.NBT_BOOK, ActionFieldSchema.builder()
                .number("pages",      "Pages")      .range(1, 100)
                .text  ("title",      "Title")
                .toggle("onlyAscii",  "Only ASCII")
                .text  ("customText", "Custom Component")
                .number("delayTicks", "Delay (ticks)").range(0, 200)
                .number("bookCount",  "Book Count")   .range(1, 64)
                .build());

        SCHEMAS.put(MacroActionType.WAIT_LAN_STEP, EMPTY);

        SCHEMAS.put(MacroActionType.WAIT_MACRO_STEP, ActionFieldSchema.builder()
                .macroSelect("macroName", "Macro")
                .enumField("mode",      "Wait For", "COMPLETED_STEP", "STARTED_STEP", "FINISHED")
                .number   ("step",      "Step").range(1, 1000).hideWhenEnum("mode", "FINISHED")
                .number   ("timeoutMs", "Timeout (ms)").range(0, 300_000)
                .build());

    }

    public static ActionFieldSchema get(MacroActionType type) {
        return SCHEMAS.getOrDefault(type, EMPTY);
    }

    private ActionFieldRegistry() {}
}
