package autismclient.util.macro;

import autismclient.util.PackUtilInventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class UseItemAction implements MacroAction {
    public enum UseMode { AUTOMATIC, CUSTOM_HOLD }

    public String itemName = "";
    public ItemTarget itemTarget = new ItemTarget();
    public UseMode useMode = UseMode.AUTOMATIC;
    public int holdTicks = 20;
    public int useCount = 1;

    private boolean enabled = true;

    public UseItemAction() {}

    @Override
    public void execute(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) return;

        ItemTarget target = resolvedItemTarget();
        if (target.hasSlot() || target.hasIdentity()) {
            PackUtilInventoryHelper.selectHotbarItem(mc, target, mc.player.getInventory().getSelectedSlot());
        }

        mc.getConnection().send(
            new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot())
        );
    }

    public void sendRelease(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
            BlockPos.ZERO,
            Direction.DOWN
        ));
    }

    @Override
    public MacroActionType getType() {
        return MacroActionType.USE_ITEM;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "USE_ITEM");
        ItemTarget target = resolvedItemTarget();
        if (target.hasSlot() || target.hasIdentity()) tag.put("itemName", target.toTag());
        tag.putString("useMode", useMode.name());
        tag.putInt("holdTicks", holdTicks);
        tag.putInt("useCount", useCount);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        itemTarget = tag.getCompound("itemName").map(ItemTarget::fromTag).orElseGet(() -> ItemTarget.fromLegacyEntry(tag.getStringOr("itemName", "")));
        itemName = itemTarget.toLegacyEntry();
        if (tag.contains("useMode")) {
            String modeName = tag.getStringOr("useMode", "AUTOMATIC");
            if ("INSTANT".equals(modeName)) modeName = "AUTOMATIC";
            if ("HOLD".equals(modeName)) modeName = "CUSTOM_HOLD";
            try { useMode = UseMode.valueOf(modeName); }
            catch (IllegalArgumentException ignored) { useMode = UseMode.AUTOMATIC; }
        }
        if (tag.contains("holdTicks")) holdTicks = tag.getIntOr("holdTicks", 20);
        if (tag.contains("useCount")) useCount = Math.max(1, tag.getIntOr("useCount", 1));
        if (tag.contains("enabled")) enabled = tag.getBooleanOr("enabled", true);
    }

    @Override
    public String getDisplayName() {
        ItemTarget target = resolvedItemTarget();
        String item = (target.hasSlot() || target.hasIdentity()) ? target.summaryText() : "current";
        String count = useCount > 1 ? " x" + useCount : "";
        return useMode == UseMode.CUSTOM_HOLD
            ? "Use Item (" + item + ", hold " + holdTicks + "t)"
            : "Use Item (" + item + count + ")";
    }

    @Override public String getIcon() { return "U"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    private ItemTarget resolvedItemTarget() {
        if (itemTarget != null && (itemTarget.hasSlot() || itemTarget.hasIdentity())) return itemTarget;
        itemTarget = ItemTarget.fromLegacyEntry(itemName);
        return itemTarget;
    }
}
