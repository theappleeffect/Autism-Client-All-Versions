package autismclient.util.macro;

import autismclient.util.PackUtilDropAction;
import autismclient.util.PackUtilClientMessaging;
import autismclient.util.PackUtilDropHelper;
import autismclient.util.PackUtilInventoryHelper;
import autismclient.util.PackUtilPacketSender;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.HashedStack;

import java.util.ArrayList;
import java.util.List;

public class ItemAction implements MacroAction, WaitsForGui {
    public List<String> itemNames = new ArrayList<>();
    public List<ItemTarget> itemTargets = new ArrayList<>();
    public List<Integer> itemTimes = new ArrayList<>();
    public List<Integer> itemActionIdx = new ArrayList<>();
    public List<Integer> itemButtons = new ArrayList<>();
    public int targetSlot = -1;
    public boolean useSlot = false;
    public int actionIndex = 0;
    public int button = 0;
    public int times = 1;
    public boolean waitForGui = false;
    public String guiName = "";
    public boolean waitForItem = false;

    private static final Minecraft MC = Minecraft.getInstance();
    private static final PackUtilDropAction[] ACTIONS = PackUtilDropAction.values();
    private static final int[] ITEM_CLICK_ACTIONS = {
        PackUtilDropAction.PICKUP.ordinal(),
        PackUtilDropAction.QUICK_MOVE.ordinal(),
        PackUtilDropAction.SWAP.ordinal(),
        PackUtilDropAction.CLONE.ordinal(),
        PackUtilDropAction.DROP_ITEM.ordinal(),
        PackUtilDropAction.DROP_STACK.ordinal()
    };

    public ItemAction() {}

    public int getItemTime(int index) {
        return (index < itemTimes.size() && itemTimes.get(index) > 0) ? itemTimes.get(index) : 1;
    }

    public int getItemActionIdx(int index) {
        int stored = (index < itemActionIdx.size())
            ? itemActionIdx.get(index)
            : actionIndex;
        return normalizeItemClickActionIndex(stored);
    }

    public int getItemButton(int index) {
        return (index < itemButtons.size()) ? itemButtons.get(index) : button;
    }

    public PackUtilDropAction getItemAction(int index) {
        return ACTIONS[getItemActionIdx(index)];
    }

    public String getItemButtonName(int index) {
        if (getItemAction(index) == PackUtilDropAction.SWAP) {
            int hotbarSlot = Math.max(0, Math.min(8, getItemButton(index))) + 1;
            return "Hotbar " + hotbarSlot;
        }
        return switch (getItemButton(index)) {
            case 0 -> "Left"; case 1 -> "Right"; case 2 -> "Middle"; default -> "?";
        };
    }

    public void cycleItemAction(int index) {
        while (itemActionIdx.size() <= index) itemActionIdx.add(actionIndex);
        itemActionIdx.set(index, cycleItemClickActionIndex(itemActionIdx.get(index), 1));
    }

    public void cycleItemActionBackwards(int index) {
        while (itemActionIdx.size() <= index) itemActionIdx.add(actionIndex);
        itemActionIdx.set(index, cycleItemClickActionIndex(itemActionIdx.get(index), -1));
    }

    public void cycleItemButton(int index) {
        while (itemButtons.size() <= index) itemButtons.add(button);
        int limit = getItemAction(index) == PackUtilDropAction.SWAP ? 9 : 3;
        int current = Math.max(0, itemButtons.get(index));
        itemButtons.set(index, (current + 1) % limit);
    }

    public void cycleItemButtonBackwards(int index) {
        while (itemButtons.size() <= index) itemButtons.add(button);
        int limit = getItemAction(index) == PackUtilDropAction.SWAP ? 9 : 3;
        int current = Math.max(0, itemButtons.get(index));
        itemButtons.set(index, (current - 1 + limit) % limit);
    }

    @Override
    public void execute(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            PackUtilClientMessaging.sendPrefixed("Â§cNo network connection!");
            return;
        }

        AbstractContainerMenu handler = mc.player.containerMenu;
        if (handler == null) {
            PackUtilClientMessaging.sendPrefixed("Â§cNo screen handler available!");
            return;
        }

        if (!itemNames.isEmpty()) {

            for (int ei = 0; ei < itemNames.size(); ei++) {
                String entry = itemNames.get(ei);
                int clickCount = getItemTime(ei);
                PackUtilDropAction eiAction = getItemAction(ei);
                int eiButton = getItemButton(ei);
                if (eiAction.usesFixedButton()) {
                    eiButton = eiAction.getButton();
                }
                ItemTarget entryTarget = getItemTarget(ei);
                String entryName = parseEntryName(entryTarget.toLegacyEntry());
                int configuredEntrySlot = entryTarget.hasSlot() ? entryTarget.slot : -1;
                int slotToUse;
                if (configuredEntrySlot >= 0 && !entryName.isEmpty()) {
                    int resolvedSlot = PackUtilInventoryHelper.resolveConfiguredHandlerSlot(mc, configuredEntrySlot);
                    if (resolvedSlot < 0) continue;
                    slotToUse = findExactMatchingSlot(handler, entryTarget, resolvedSlot);
                } else if (configuredEntrySlot >= 0) {
                    slotToUse = PackUtilInventoryHelper.resolveConfiguredHandlerSlot(mc, configuredEntrySlot);
                } else if (useSlot && targetSlot >= 0) {
                    int configuredSlot = PackUtilInventoryHelper.resolveConfiguredHandlerSlot(mc, targetSlot);
                    if (configuredSlot < 0) continue;
                    slotToUse = findExactMatchingSlot(handler, entryTarget, configuredSlot);
                } else {
                    slotToUse = findSlotByItemTarget(handler, entryTarget);
                }
                if (slotToUse < 0) continue;
                final int fs = slotToUse;
                final int fb = eiButton;
                final PackUtilDropAction fa = eiAction;
                if (fa == PackUtilDropAction.DROP_ITEM || fa == PackUtilDropAction.DROP_STACK) {
                    PackUtilDropHelper.dropFromHandlerSlot(mc, fs, fa == PackUtilDropAction.DROP_STACK ? 0 : clickCount);
                    continue;
                }
                for (int j = 0; j < clickCount; j++) {
                    ServerboundContainerClickPacket packet = new ServerboundContainerClickPacket(
                        handler.containerId,
                        handler.getStateId(),
                        (short) fs,
                        (byte) fb,
                        fa.toContainerInput(),
                        new Int2ObjectArrayMap<>(),
                        HashedStack.EMPTY
                    );
                    PackUtilPacketSender.send(packet);
                }
            }
        } else if (useSlot && targetSlot >= 0) {

            PackUtilDropAction action = getAction();
            int effectiveButton = button;
            if (action.usesFixedButton()) {
                effectiveButton = action.getButton();
            }
            int resolvedTargetSlot = PackUtilInventoryHelper.resolveConfiguredHandlerSlot(mc, targetSlot);
            if (resolvedTargetSlot < 0) {
                PackUtilClientMessaging.sendPrefixed("Ã‚Â§cConfigured slot " + targetSlot + " is not available in this screen!");
                return;
            }
            if (action == PackUtilDropAction.DROP_ITEM || action == PackUtilDropAction.DROP_STACK) {
                PackUtilDropHelper.dropFromHandlerSlot(mc, resolvedTargetSlot, action == PackUtilDropAction.DROP_STACK ? 0 : times);
                return;
            }

            for (int i = 0; i < times; i++) {
                ServerboundContainerClickPacket packet = new ServerboundContainerClickPacket(
                    handler.containerId,
                    handler.getStateId(),
                    (short) resolvedTargetSlot,
                    (byte) effectiveButton,
                    action.toContainerInput(),
                    new Int2ObjectArrayMap<>(),
                    HashedStack.EMPTY
                );
                PackUtilPacketSender.send(packet);
            }
        } else {
            PackUtilClientMessaging.sendPrefixed("Â§cNo item name or slot specified!");
        }
    }

    private int findSlotByItemName(AbstractContainerMenu handler, String name) {
        return findSlotByItemTarget(handler, ItemTarget.fromLegacyEntry(name));
    }

    private int findSlotByItemTarget(AbstractContainerMenu handler, ItemTarget target) {
        int bestSlot = -1;
        int bestScore = -1;
        if (!target.hasIdentity()) return -1;

        for (Slot slot : handler.slots) {
            if (slot == null || slot.getItem().isEmpty()) continue;
            int visibleSlot = PackUtilInventoryHelper.toUserVisibleSlot(MC, slot.index);
            int score = target.score(slot.getItem(), visibleSlot);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot.index;
            }
        }

        return bestScore >= 0 ? bestSlot : -1;
    }

    private int findExactMatchingSlot(AbstractContainerMenu handler, String name, int requiredSlot) {
        return findExactMatchingSlot(handler, ItemTarget.fromLegacyEntry(name), requiredSlot);
    }

    private int findExactMatchingSlot(AbstractContainerMenu handler, ItemTarget target, int requiredSlot) {
        if (requiredSlot < 0 || requiredSlot >= handler.slots.size()) return -1;
        Slot slot = handler.slots.get(requiredSlot);
        if (slot == null || slot.getItem().isEmpty()) return -1;
        if (!target.hasIdentity()) return requiredSlot;
        int visibleSlot = PackUtilInventoryHelper.toUserVisibleSlot(MC, requiredSlot);
        return target.matches(slot.getItem(), visibleSlot) ? requiredSlot : -1;
    }

    @Override public boolean isWaitForGui() { return waitForGui; }
    @Override public void setWaitForGui(boolean v) { this.waitForGui = v; }
    @Override public String getWaitGuiName() { return guiName; }
    @Override public void setWaitGuiName(String name) { this.guiName = name; }
    @Override public boolean isWaitForGuiChange() { return true; }

    @Override
    public MacroActionType getType() { return MacroActionType.ITEM; }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "ITEM");
        tag.put("itemNames", ItemTarget.toTagList(resolvedItemTargets()));
        ListTag times2 = new ListTag();
        for (int t : itemTimes) times2.add(StringTag.valueOf(String.valueOf(t)));
        tag.put("itemTimes", times2);
        ListTag aidx = new ListTag();
        for (int a : itemActionIdx) aidx.add(StringTag.valueOf(String.valueOf(a)));
        tag.put("itemActionIdx", aidx);
        ListTag btns = new ListTag();
        for (int b : itemButtons) btns.add(StringTag.valueOf(String.valueOf(b)));
        tag.put("itemButtons", btns);
        tag.putInt("targetSlot", targetSlot);
        tag.putBoolean("useSlot", useSlot);
        tag.putInt("actionIndex", actionIndex);
        tag.putInt("button", button);
        tag.putInt("times", times);
        tag.putBoolean("waitForGui", waitForGui);
        tag.putString("guiName", guiName);
        tag.putBoolean("waitForItem", waitForItem);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        itemTargets.clear();
        itemNames.clear();
        if (tag.contains("itemNames")) {
            ListTag nl = tag.getList("itemNames").orElse(new ListTag());
            itemTargets.addAll(ItemTarget.fromElementList(nl));
        } else if (tag.contains("itemName")) {
            String old = tag.getStringOr("itemName", "");
            if (!old.isEmpty()) itemTargets.add(ItemTarget.fromLegacyEntry(old));
        }
        syncLegacyItemNames();
        itemTimes.clear();
        if (tag.contains("itemTimes")) {
            ListTag tl = tag.getList("itemTimes").orElse(new ListTag());
            for (Tag el : tl) {
                try { itemTimes.add(Integer.parseInt(el.asString().orElse("1"))); }
                catch (NumberFormatException e) { itemTimes.add(1); }
            }
        }

        while (itemTimes.size() < itemNames.size()) itemTimes.add(1);
        itemActionIdx.clear();
        if (tag.contains("itemActionIdx")) {
            ListTag al = tag.getList("itemActionIdx").orElse(new ListTag());
            for (Tag el : al) {
                try { itemActionIdx.add(Integer.parseInt(el.asString().orElse("0"))); }
                catch (NumberFormatException e) { itemActionIdx.add(0); }
            }
        }
        while (itemActionIdx.size() < itemNames.size()) itemActionIdx.add(0);
        for (int i = 0; i < itemActionIdx.size(); i++) {
            itemActionIdx.set(i, normalizeItemClickActionIndex(itemActionIdx.get(i)));
        }
        itemButtons.clear();
        if (tag.contains("itemButtons")) {
            ListTag bl = tag.getList("itemButtons").orElse(new ListTag());
            for (Tag el : bl) {
                try { itemButtons.add(Integer.parseInt(el.asString().orElse("0"))); }
                catch (NumberFormatException e) { itemButtons.add(0); }
            }
        }
        while (itemButtons.size() < itemNames.size()) itemButtons.add(0);
        targetSlot = tag.getIntOr("targetSlot", -1);
        useSlot = tag.getBooleanOr("useSlot", false);
        actionIndex = normalizeItemClickActionIndex(tag.getIntOr("actionIndex", 0));
        button = tag.getIntOr("button", 0);
        times = tag.getIntOr("times", 1);
        if (tag.contains("waitForGui")) waitForGui = tag.getBooleanOr("waitForGui", false);
        if (tag.contains("guiName")) guiName = tag.getStringOr("guiName", "");
        if (tag.contains("waitForItem")) waitForItem = tag.getBooleanOr("waitForItem", false);
    }

    @Override
    public String getDisplayName() {
        PackUtilDropAction action = ACTIONS[Math.min(actionIndex, ACTIONS.length - 1)];
        String actionName = action.displayName;

        List<ItemTarget> targets = resolvedItemTargets();
        if (!targets.isEmpty()) {
            String e0 = targets.get(0).toLegacyEntry();
            String first;
            int slotNumber = parseEntrySlot(e0);
            String itemName = parseEntryName(e0);
            if (slotNumber >= 0 && !itemName.isEmpty()) {
                first = itemName + " @ " + slotNumber;
            } else if (slotNumber >= 0) {
                first = "Slot " + slotNumber;
            } else {
                first = itemName;
            }
            String shortName = first.length() > 12 ? first.substring(0, 10) + ".." : first;
            String suffix = itemNames.size() > 1 ? " (+" + (itemNames.size() - 1) + ")" : "";
            String base = "Item " + shortName + suffix + " (" + actionName + ")";
            return waitForItem ? base + " [wait]" : base;
        } else if (useSlot && targetSlot >= 0) {
            return "Item Slot " + targetSlot + " (" + actionName + ")";
        }
        return "Item (empty)";
    }

    @Override
    public String getIcon() { return "I"; }

    public PackUtilDropAction getAction() {
        return ACTIONS[normalizeItemClickActionIndex(actionIndex)];
    }

    public void cycleAction() {
        actionIndex = cycleItemClickActionIndex(actionIndex, 1);
    }

    public void cycleActionBackwards() {
        actionIndex = cycleItemClickActionIndex(actionIndex, -1);
    }

    public String getButtonName() {
        if (getAction() == PackUtilDropAction.SWAP) {
            int hotbarSlot = Math.max(0, Math.min(8, button)) + 1;
            return "Hotbar " + hotbarSlot;
        }
        return switch (button) {
            case 0 -> "Left";
            case 1 -> "Right";
            case 2 -> "Middle";
            default -> "?";
        };
    }

    public void cycleButton() {
        int limit = getAction() == PackUtilDropAction.SWAP ? 9 : 3;
        button = (Math.max(0, button) + 1) % limit;
    }

    public void cycleButtonBackwards() {
        int limit = getAction() == PackUtilDropAction.SWAP ? 9 : 3;
        button = (Math.max(0, button) - 1 + limit) % limit;
    }

    private static int normalizeItemClickActionIndex(int rawIndex) {
        for (int allowed : ITEM_CLICK_ACTIONS) {
            if (rawIndex == allowed) {
                return rawIndex;
            }
        }
        return PackUtilDropAction.PICKUP.ordinal();
    }

    private static int cycleItemClickActionIndex(int current, int direction) {
        int currentOrdinal = normalizeItemClickActionIndex(current);
        int selected = 0;
        for (int i = 0; i < ITEM_CLICK_ACTIONS.length; i++) {
            if (ITEM_CLICK_ACTIONS[i] == currentOrdinal) {
                selected = i;
                break;
            }
        }

        int next = (selected + direction) % ITEM_CLICK_ACTIONS.length;
        if (next < 0) next += ITEM_CLICK_ACTIONS.length;
        return ITEM_CLICK_ACTIONS[next];
    }

    private static int parseEntrySlot(String entry) {
        if (entry == null || !entry.startsWith("#")) return -1;
        int separator = entry.indexOf('|');
        String raw = separator >= 0 ? entry.substring(1, separator) : entry.substring(1);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String parseEntryName(String entry) {
        if (entry == null || entry.isBlank()) return "";
        if (!entry.startsWith("#")) return entry;
        int separator = entry.indexOf('|');
        return separator >= 0 && separator + 1 < entry.length() ? entry.substring(separator + 1).trim() : "";
    }

    private List<ItemTarget> resolvedItemTargets() {
        if (!itemTargets.isEmpty()) return itemTargets;
        for (String itemName : itemNames) {
            ItemTarget target = ItemTarget.fromLegacyEntry(itemName);
            if (target.hasSlot() || target.hasIdentity()) itemTargets.add(target);
        }
        return itemTargets;
    }

    private ItemTarget getItemTarget(int index) {
        List<ItemTarget> targets = resolvedItemTargets();
        if (index >= 0 && index < targets.size()) return targets.get(index);
        return new ItemTarget();
    }

    private void syncLegacyItemNames() {
        itemNames.clear();
        for (ItemTarget target : itemTargets) {
            if (target == null) continue;
            String entry = target.toLegacyEntry();
            if (!entry.isBlank()) itemNames.add(entry);
        }
    }
}
