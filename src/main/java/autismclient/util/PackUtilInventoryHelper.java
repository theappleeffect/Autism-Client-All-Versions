package autismclient.util;

import autismclient.util.macro.ItemTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;

public final class PackUtilInventoryHelper {
    public static final int PLAYER_VISIBLE_SLOT_COUNT = 41;
    public static final int FIRST_GUI_SLOT = 100;

    private PackUtilInventoryHelper() {
    }

    public static void selectHotbarSlot(Minecraft mc, int slot) {
        if (mc == null || mc.player == null || mc.getConnection() == null) return;

        int clampedSlot = Math.max(0, Math.min(8, slot));
        if (mc.player.getInventory().getSelectedSlot() == clampedSlot) return;

        mc.player.getInventory().setSelectedSlot(clampedSlot);
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(clampedSlot));
    }

    public static boolean swapInventorySlots(Minecraft mc, int fromSlot, int toSlot) {
        if (mc == null || mc.player == null || mc.gameMode == null) return false;
        if (fromSlot == toSlot) return true;
        if (fromSlot < 0 || fromSlot >= PLAYER_VISIBLE_SLOT_COUNT || toSlot < 0 || toSlot >= PLAYER_VISIBLE_SLOT_COUNT) return false;
        if (!mc.player.containerMenu.getCarried().isEmpty()) return false;

        int fromScreenSlot = toHandlerSlot(mc, fromSlot);
        int toScreenSlot = toHandlerSlot(mc, toSlot);
        if (fromScreenSlot < 0 || toScreenSlot < 0) return false;

        return swapHandlerSlots(mc, fromScreenSlot, toScreenSlot);
    }

    public static int findInventorySlotByName(Minecraft mc, String itemName) {
        return findInventorySlot(mc, ItemTarget.fromLegacyEntry(itemName));
    }

    public static int findInventorySlot(Minecraft mc, ItemTarget target) {
        if (mc == null || mc.player == null || target == null || !target.hasIdentity()) return -1;

        int bestSlot = -1;
        int bestScore = -1;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            int score = target.score(stack, slot);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestScore >= 0 ? bestSlot : -1;
    }

    public static int findUserVisibleSlotByName(Minecraft mc, String itemName) {
        return findUserVisibleSlot(mc, ItemTarget.fromLegacyEntry(itemName));
    }

    public static int findUserVisibleSlot(Minecraft mc, ItemTarget target) {
        if (mc == null || mc.player == null || target == null || !target.hasIdentity()) return -1;

        int bestSlot = -1;
        int bestScore = -1;
        AbstractContainerMenu handler = mc.player.containerMenu;
        if (handler != null) {
            for (Slot slot : handler.slots) {
                if (slot == null) continue;
                int visibleSlot = toUserVisibleSlot(mc, slot.index);
                int score = target.score(slot.getItem(), visibleSlot);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = visibleSlot;
                }
            }
        }

        if (bestScore >= 0) return bestSlot;
        return findInventorySlot(mc, target);
    }

    public static int selectHotbarItemByName(Minecraft mc, String itemName, int preferredHotbarSlot) {
        return selectHotbarItem(mc, ItemTarget.fromLegacyEntry(itemName), preferredHotbarSlot);
    }

    public static int selectHotbarItem(Minecraft mc, ItemTarget target, int preferredHotbarSlot) {
        if (mc == null || mc.player == null || target == null || !target.hasIdentity()) return -1;

        int bestHotbarSlot = -1;
        int bestHotbarScore = -1;
        for (int slot = 0; slot < 9; slot++) {
            int score = target.score(mc.player.getInventory().getItem(slot), slot);
            if (score > bestHotbarScore) {
                bestHotbarScore = score;
                bestHotbarSlot = slot;
            }
        }
        if (bestHotbarScore >= 0) {
            selectHotbarSlot(mc, bestHotbarSlot);
            return bestHotbarSlot;
        }

        int bestInventorySlot = -1;
        int bestInventoryScore = -1;
        for (int slot = 9; slot < 36; slot++) {
            int score = target.score(mc.player.getInventory().getItem(slot), slot);
            if (score > bestInventoryScore) {
                bestInventoryScore = score;
                bestInventorySlot = slot;
            }
        }
        if (bestInventoryScore < 0) return -1;

        int targetHotbarSlot = Math.max(0, Math.min(8, preferredHotbarSlot));
        if (!swapInventorySlots(mc, bestInventorySlot, targetHotbarSlot)) return -1;
        selectHotbarSlot(mc, targetHotbarSlot);
        return targetHotbarSlot;
    }

    public static int toUserVisibleSlot(Minecraft mc, int handlerSlotId) {
        if (mc == null || mc.player == null || mc.player.containerMenu == null) return handlerSlotId;
        AbstractContainerMenu handler = mc.player.containerMenu;
        if (handlerSlotId < 0 || handlerSlotId >= handler.slots.size()) return handlerSlotId;

        Slot slot = handler.slots.get(handlerSlotId);
        int playerSlot = getInventorySlot(mc, slot);
        if (playerSlot >= 0) {
            return playerSlot;
        }

        int extraOrdinal = 0;
        for (Slot currentSlot : handler.slots) {
            if (currentSlot == null || isInventorySlot(mc, currentSlot)) continue;
            if (currentSlot.index == handlerSlotId) return FIRST_GUI_SLOT + extraOrdinal;
            extraOrdinal++;
        }

        return handlerSlotId;
    }

    public static int toHandlerSlot(Minecraft mc, int userVisibleSlot) {
        if (mc == null || mc.player == null || mc.player.containerMenu == null) return -1;
        AbstractContainerMenu handler = mc.player.containerMenu;

        if (userVisibleSlot >= 0 && userVisibleSlot < mc.player.getInventory().getContainerSize()) {
            for (Slot slot : handler.slots) {
                if (getInventorySlot(mc, slot) == userVisibleSlot) return slot.index;
            }
        }

        if (userVisibleSlot >= FIRST_GUI_SLOT) {
            int extraOrdinal = userVisibleSlot - FIRST_GUI_SLOT;
            for (Slot slot : handler.slots) {
                if (slot == null || isInventorySlot(mc, slot)) continue;
                if (extraOrdinal == 0) return slot.index;
                extraOrdinal--;
            }
        }

        return -1;
    }

    public static int resolveConfiguredHandlerSlot(Minecraft mc, int configuredSlot) {
        return toHandlerSlot(mc, configuredSlot);
    }

    public static boolean isInventorySlot(Minecraft mc, Slot slot) {
        return getInventorySlot(mc, slot) >= 0;
    }

    public static boolean swapHandlerSlots(Minecraft mc, int fromScreenSlot, int toScreenSlot) {
        if (mc == null || mc.player == null || mc.gameMode == null) return false;
        if (fromScreenSlot == toScreenSlot) return true;
        AbstractContainerMenu handler = mc.player.containerMenu;
        if (handler == null) return false;
        if (fromScreenSlot < 0 || fromScreenSlot >= handler.slots.size()) return false;
        if (toScreenSlot < 0 || toScreenSlot >= handler.slots.size()) return false;
        if (!handler.getCarried().isEmpty()) return false;

        int syncId = handler.containerId;
        mc.gameMode.handleContainerInput(syncId, fromScreenSlot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(syncId, toScreenSlot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(syncId, fromScreenSlot, 0, ContainerInput.PICKUP, mc.player);
        return true;
    }

    private static int getInventorySlot(Minecraft mc, Slot slot) {
        if (mc == null || mc.player == null || slot == null || slot.container != mc.player.getInventory()) return -1;
        int index = slot.getContainerSlot();
        return index >= 0 && index < mc.player.getInventory().getContainerSize() ? index : -1;
    }
}
