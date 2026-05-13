package autismclient.util.macro;

import autismclient.util.PackUtilInventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public class SelectSlotAction implements MacroAction {
    public int slot = 0;
    public String itemName = "";
    public ItemTarget itemTarget = new ItemTarget();

    public SelectSlotAction() {}

    @Override
    public void execute(Minecraft mc) {
        if (mc.player == null) return;

        ItemTarget target = resolvedItemTarget();
        if (target.hasSlot() || target.hasIdentity()) {
            int selectedSlot = PackUtilInventoryHelper.selectHotbarItem(mc, target, slot);
            if (selectedSlot >= 0) return;
        }

        int actualSlot = Math.max(0, Math.min(8, slot));
        PackUtilInventoryHelper.selectHotbarSlot(mc, actualSlot);
    }

    @Override
    public MacroActionType getType() {
        return MacroActionType.SELECT_SLOT;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "SELECT_SLOT");
        tag.putInt("slot", slot);
        ItemTarget target = resolvedItemTarget();
        if (target.hasSlot() || target.hasIdentity()) tag.put("itemName", target.toTag());
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        this.slot = tag.getIntOr("slot", 0);
        this.itemTarget = tag.getCompound("itemName").map(ItemTarget::fromTag).orElseGet(() -> ItemTarget.fromLegacyEntry(tag.getStringOr("itemName", "")));
        this.itemName = itemTarget.toLegacyEntry();
    }

    @Override
    public String getDisplayName() {
        ItemTarget target = resolvedItemTarget();
        if (target.hasSlot() || target.hasIdentity()) {
            return "Select \"" + target.summaryText() + "\" -> " + (Math.max(0, Math.min(8, slot)) + 1);
        }
        return "Select Slot " + (slot + 1);
    }

    @Override
    public String getIcon() {
        return "S";
    }

    private ItemTarget resolvedItemTarget() {
        if (itemTarget != null && (itemTarget.hasSlot() || itemTarget.hasIdentity())) return itemTarget;
        itemTarget = ItemTarget.fromLegacyEntry(itemName);
        return itemTarget;
    }
}
