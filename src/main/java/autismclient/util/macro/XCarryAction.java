package autismclient.util.macro;

import autismclient.util.PackUtilContainerTarget;
import autismclient.util.PackUtilSharedState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

public class XCarryAction implements MacroAction {
    public static final int MAX_ENTRIES = 4;

    public enum Mode { PUT_IN, TAKE_OUT, DROP }
    public enum TransferMode { FAST, CLICK }

    static final class PutInMove {
        final int sourceSlotId;
        final int targetSlotId;

        PutInMove(int sourceSlotId, int targetSlotId) {
            this.sourceSlotId = sourceSlotId;
            this.targetSlotId = targetSlotId;
        }
    }

    public final List<String> entries = new ArrayList<>();
    public final List<ItemTarget> entryTargets = new ArrayList<>();
    public Mode mode = Mode.PUT_IN;
    public TransferMode transferMode = TransferMode.FAST;
    public boolean carryCursor = false;
    private boolean enabled = true;

    @Override
    public void execute(Minecraft mc) {
        if (mc == null || mc.player == null || mc.gameMode == null) return;

        if (mc.player.containerMenu != mc.player.inventoryMenu) {
            executeWithContainerOpen(mc);
            return;
        }

        InventoryMenu handler = mc.player.inventoryMenu;
        if (!handler.getCarried().isEmpty() && !carryCursor) return;
        if (!handler.getCarried().isEmpty() && carryCursor) {
            updateXCarryState(handler, true);
            return;
        }

        switch (mode) {
            case PUT_IN -> executePutIn(handler, mc);
            case TAKE_OUT -> executeTakeOut(handler, mc);
            case DROP -> executeDrop(handler, mc);
        }
    }

    private void executeWithContainerOpen(Minecraft mc) {
        if (mc.getConnection() == null) return;

        PackUtilContainerTarget containerTarget = PackUtilSharedState.get().getLastContainerTarget();
        if (containerTarget == null) return;

        InventoryMenu playerHandler = mc.player.inventoryMenu;
        if (!playerHandler.getCarried().isEmpty() && !carryCursor) return;
        if (!playerHandler.getCarried().isEmpty() && carryCursor) {
            updateXCarryState(playerHandler, true);
            return;
        }

        AbstractContainerMenu containerHandler = mc.player.containerMenu;

        if (mode == Mode.PUT_IN) {
            List<ItemTarget> targets = resolvedEntryTargets();
            int limit = Math.min(MAX_ENTRIES, targets.size());
            for (int i = 0; i < limit; i++) {
                ItemTarget itemTarget = targets.get(i);
                if (itemTarget == null || itemTarget.hasSlot() || !itemTarget.hasIdentity()) continue;
                for (Slot slot : containerHandler.slots) {
                    if (slot.getItem().isEmpty()) continue;
                    if (slot.container instanceof net.minecraft.world.entity.player.Inventory) continue;
                    if (matchesTarget(slot.getItem(), itemTarget, -1)) {
                        mc.gameMode.handleInventoryMouseClick(containerHandler.containerId, slot.index, 0, ClickType.QUICK_MOVE, mc.player);
                        break;
                    }
                }
            }
        }

        mc.getConnection().send(new ServerboundContainerClosePacket(containerHandler.containerId));
        mc.player.containerMenu = playerHandler;

        switch (mode) {
            case PUT_IN -> executePutIn(playerHandler, mc);
            case TAKE_OUT -> executeTakeOut(playerHandler, mc);
            case DROP -> executeDrop(playerHandler, mc);
        }

        mc.player.containerMenu = containerHandler;
        containerTarget.interact(mc);
    }

    private void executePutIn(InventoryMenu handler, Minecraft mc) {
        int moved = 0;
        while (true) {
            PutInMove move = findNextPutInMove(handler);
            if (move == null) break;
            if (moveStack(handler, mc, move.sourceSlotId, move.targetSlotId)) {
                moved++;
                continue;
            }
            if (!handler.getCarried().isEmpty()) break;
        }
        if (moved > 0 || hasStoredItems(handler, carryCursor)) {
            updateXCarryState(handler, carryCursor);
        }
    }

    private void executeTakeOut(InventoryMenu handler, Minecraft mc) {
        for (int slotId = 1; slotId <= 4 && slotId < handler.slots.size(); slotId++) {
            Slot slot = handler.slots.get(slotId);
            if (slot.getItem().isEmpty()) continue;
            if (!entries.isEmpty() && !matchesCraftingSlot(slot, slotId)) continue;
            mc.gameMode.handleInventoryMouseClick(handler.containerId, slotId, 0, ClickType.QUICK_MOVE, mc.player);
        }
        updateXCarryState(handler, carryCursor);
    }

    private void executeDrop(InventoryMenu handler, Minecraft mc) {
        for (int slotId = 1; slotId <= 4 && slotId < handler.slots.size(); slotId++) {
            Slot slot = handler.slots.get(slotId);
            if (slot.getItem().isEmpty()) continue;
            if (!entries.isEmpty() && !matchesCraftingSlot(slot, slotId)) continue;
            mc.gameMode.handleInventoryMouseClick(handler.containerId, slotId, 1, ClickType.THROW, mc.player);
        }
        updateXCarryState(handler, carryCursor);
    }

    private boolean matchesCraftingSlot(Slot slot, int slotId) {
        for (ItemTarget entryTarget : resolvedEntryTargets()) {
            if (entryTarget == null) continue;
            if (entryTarget.hasSlot()) {
                if (entryTarget.slot == slotId && (!entryTarget.hasIdentity() || matchesTarget(slot.getItem(), entryTarget, slotId))) return true;
            } else if (entryTarget.hasIdentity() && matchesTarget(slot.getItem(), entryTarget, slotId)) {
                return true;
            }
        }
        return false;
    }

    PutInMove findNextPutInMove(InventoryMenu handler) {
        if (handler == null) return null;

        List<ItemTarget> targets = resolvedEntryTargets();
        int limit = Math.min(MAX_ENTRIES, targets.size());
        int nextTarget = 1;
        for (int i = 0; i < limit; i++) {
            int sourceSlotId = findSourceSlot(handler, targets.get(i));
            if (sourceSlotId < 0) continue;

            while (nextTarget <= 4 && !handler.slots.get(nextTarget).getItem().isEmpty()) {
                nextTarget++;
            }
            if (nextTarget > 4) return null;

            int targetSlotId = nextTarget++;
            if (sourceSlotId == targetSlotId) continue;
            return new PutInMove(sourceSlotId, targetSlotId);
        }

        return null;
    }

    List<Integer> collectContainerTransferSlots(AbstractContainerMenu containerHandler) {
        List<Integer> slotIds = new ArrayList<>();
        if (containerHandler == null) return slotIds;

        List<ItemTarget> targets = resolvedEntryTargets();
        int limit = Math.min(MAX_ENTRIES, targets.size());
        for (int i = 0; i < limit; i++) {
            ItemTarget itemTarget = targets.get(i);
            if (itemTarget == null || itemTarget.hasSlot() || !itemTarget.hasIdentity()) continue;
            for (Slot slot : containerHandler.slots) {
                if (slot.getItem().isEmpty()) continue;
                if (slot.container instanceof net.minecraft.world.entity.player.Inventory) continue;
                if (!matchesTarget(slot.getItem(), itemTarget, -1)) continue;
                slotIds.add(slot.index);
                break;
            }
        }

        return slotIds;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", MacroActionType.XCARRY.name());
        tag.putString("mode", mode.name());
        tag.putString("transferMode", transferMode.name());
        tag.putBoolean("carryCursor", carryCursor);
        List<ItemTarget> targets = resolvedEntryTargets();
        tag.put("entries", ItemTarget.toTagList(targets.subList(0, Math.min(MAX_ENTRIES, targets.size()))));
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        entries.clear();
        entryTargets.clear();
        String modeStr = tag.getStringOr("mode", "PUT_IN");
        try {
            mode = Mode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            mode = Mode.PUT_IN;
        }

        String transferModeStr = tag.getStringOr("transferMode", "FAST");
        try {
            transferMode = TransferMode.valueOf(transferModeStr);
        } catch (IllegalArgumentException e) {
            transferMode = TransferMode.FAST;
        }

        if (tag.contains("entries")) {
            ListTag list = tag.getList("entries").orElse(new ListTag());
            for (ItemTarget target : ItemTarget.fromElementList(list)) {
                if (target == null || (!target.hasSlot() && !target.hasIdentity())) continue;
                entryTargets.add(target);
                if (entryTargets.size() >= MAX_ENTRIES) break;
            }
        }
        syncLegacyEntries();

        enabled = tag.getBooleanOr("enabled", true);
        carryCursor = tag.getBooleanOr("carryCursor", false);
    }

    @Override
    public MacroActionType getType() {
        return MacroActionType.XCARRY;
    }

    @Override
    public String getDisplayName() {
        String prefix = switch (mode) {
            case PUT_IN -> transferMode == TransferMode.CLICK ? "XCarry Click" : "XCarry";
            case TAKE_OUT -> "XCarry Out";
            case DROP -> "XCarry Drop";
        };
        if (carryCursor) prefix += " Cursor";
        if (entries.isEmpty()) return prefix;
        String first = formatEntry(entries.get(0));
        if (entries.size() > 1) first += " (+" + (entries.size() - 1) + ")";
        return prefix + " [" + first + "]";
    }

    @Override
    public String getIcon() {
        return "XC";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static String normalizeEntry(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (!trimmed.startsWith("#")) return trimmed;
        int separator = trimmed.indexOf('|');
        String slotPart = separator >= 0 ? trimmed.substring(1, separator) : trimmed.substring(1);
        try {
            int slot = Integer.parseInt(slotPart.trim());
            if (slot < 0) return null;
            String name = separator >= 0 && separator + 1 < trimmed.length()
                    ? trimmed.substring(separator + 1).trim()
                    : "";
            if (!name.isEmpty()) return "#" + slot + "|" + name;
            return "#" + slot;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String formatEntry(String entry) {
        String normalized = normalizeEntry(entry);
        if (normalized == null) return "";
        int slot = parseEntrySlot(normalized);
        String name = parseEntryName(normalized);
        if (slot >= 0 && !name.isEmpty()) return name + " @ Slot " + slot;
        if (slot >= 0) return "Slot " + slot;
        return normalized;
    }

    private static int findSourceSlot(InventoryMenu handler, ItemTarget target) {
        if (target == null) return -1;

        if (target.hasSlot()) {
            int exactSlot = resolvePlayerVisibleSlot(handler, target.slot);
            if (exactSlot < 0) return -1;
            if (!target.hasIdentity()) return exactSlot;
            Slot slot = handler.slots.get(exactSlot);
            return matchesTarget(slot.getItem(), target, target.slot) ? exactSlot : -1;
        }

        for (Slot slot : handler.slots) {
            if (!isEligibleSourceSlot(slot)) continue;
            int visibleSlot = resolveVisibleSlotForPlayerSlot(handler, slot.index);
            if (matchesTarget(slot.getItem(), target, visibleSlot)) return slot.index;
        }
        return -1;
    }

    private static boolean moveStack(InventoryMenu handler, Minecraft mc, int fromSlotId, int toSlotId) {
        if (mc == null || mc.player == null || mc.gameMode == null) return false;
        if (fromSlotId < 0 || fromSlotId >= handler.slots.size()) return false;
        if (toSlotId < 0 || toSlotId >= handler.slots.size()) return false;
        if (fromSlotId == toSlotId) return true;
        if (!handler.getCarried().isEmpty()) return false;

        if (handler.slots.get(fromSlotId).getItem().isEmpty()) return false;
        if (!handler.slots.get(toSlotId).getItem().isEmpty()) return false;

        mc.gameMode.handleInventoryMouseClick(handler.containerId, fromSlotId, 0, ClickType.PICKUP, mc.player);
        if (handler.getCarried().isEmpty()) return false;

        mc.gameMode.handleInventoryMouseClick(handler.containerId, toSlotId, 0, ClickType.PICKUP, mc.player);
        if (handler.getCarried().isEmpty()) {
            return handler.slots.get(fromSlotId).getItem().isEmpty()
                    && !handler.slots.get(toSlotId).getItem().isEmpty();
        }

        mc.gameMode.handleInventoryMouseClick(handler.containerId, fromSlotId, 0, ClickType.PICKUP, mc.player);
        return false;
    }

    public static boolean hasCraftingGridItems(InventoryMenu handler) {
        if (handler == null) return false;
        for (int slotId = 1; slotId <= 4 && slotId < handler.slots.size(); slotId++) {
            if (!handler.slots.get(slotId).getItem().isEmpty()) return true;
        }
        return false;
    }

    public static boolean hasCursorItem(InventoryMenu handler) {
        return handler != null && !handler.getCarried().isEmpty();
    }

    public static boolean hasStoredItems(InventoryMenu handler, boolean includeCursor) {
        return hasCraftingGridItems(handler) || (includeCursor && hasCursorItem(handler));
    }

    private void updateXCarryState(InventoryMenu handler, boolean includeCursor) {
        boolean active = hasStoredItems(handler, includeCursor);
        PackUtilSharedState.get().setXCarryForced(active);
        PackUtilSharedState.get().setXCarryActive(active);
    }

    static void sendOpenTarget(Minecraft mc, PackUtilContainerTarget target) {
        if (target == null) return;
        target.interact(mc);
    }

    private static boolean isEligibleSourceSlot(Slot slot) {
        return slot != null && slot.index >= 5 && !slot.getItem().isEmpty();
    }

    private static int resolvePlayerVisibleSlot(AbstractContainerMenu handler, int configuredSlot) {
        for (Slot slot : handler.slots) {
            if (slot == null || slot.index < 0 || slot.index >= handler.slots.size()) continue;
            if (slot.index <= 4) continue;
            if (slot.index >= 5 && slot.index <= 8) continue;
            if (slot.index == 45) continue;
            if (slot.container == null) continue;
            if (slot.container instanceof net.minecraft.world.entity.player.Inventory) {
                int index = slot.index;
                if (index == configuredSlot) return slot.index;
            }
        }
        return -1;
    }

    private static boolean matchesTarget(ItemStack stack, ItemTarget target, int visibleSlot) {
        if (stack == null || stack.isEmpty()) return false;
        if (target == null || !target.hasIdentity()) return true;
        return target.matches(stack, visibleSlot);
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
        if (!entry.startsWith("#")) return entry.trim();
        int separator = entry.indexOf('|');
        return separator >= 0 && separator + 1 < entry.length() ? entry.substring(separator + 1).trim() : "";
    }

    private static int resolveVisibleSlotForPlayerSlot(InventoryMenu handler, int handlerSlotId) {
        if (handlerSlotId < 0 || handlerSlotId >= handler.slots.size()) return -1;
        Slot slot = handler.slots.get(handlerSlotId);
        if (slot == null || !(slot.container instanceof net.minecraft.world.entity.player.Inventory)) return -1;
        return slot.index;
    }

    private List<ItemTarget> resolvedEntryTargets() {
        if (!entryTargets.isEmpty()) return entryTargets;
        for (String entry : entries) {
            String normalized = normalizeEntry(entry);
            if (normalized == null) continue;
            ItemTarget target = ItemTarget.fromLegacyEntry(normalized);
            if (target.hasSlot() || target.hasIdentity()) entryTargets.add(target);
            if (entryTargets.size() >= MAX_ENTRIES) break;
        }
        return entryTargets;
    }

    private void syncLegacyEntries() {
        entries.clear();
        for (ItemTarget target : entryTargets) {
            if (target == null) continue;
            String entry = normalizeEntry(target.toLegacyEntry());
            if (entry == null || entries.contains(entry)) continue;
            entries.add(entry);
            if (entries.size() >= MAX_ENTRIES) break;
        }
    }
}
