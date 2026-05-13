package autismclient.util.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public class InstaBreakAction implements MacroAction {
    public BlockPos blockPos = BlockPos.ZERO;
    public Direction direction = Direction.UP;
    public int delayTicks = 0;
    public int times = 1;
    public boolean autoPickaxe = true;
    public boolean manualDirection = false;
    private boolean enabled = true;

    @Override
    public void execute(Minecraft mc) {
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", getType().name());
        tag.putInt("x", blockPos.getX());
        tag.putInt("y", blockPos.getY());
        tag.putInt("z", blockPos.getZ());
        tag.putString("direction", direction.name());
        tag.putInt("delayTicks", delayTicks);
        tag.putInt("times", times);
        tag.putBoolean("autoPickaxe", autoPickaxe);
        tag.putBoolean("manualDirection", manualDirection);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        if (tag.contains("x") && tag.contains("y") && tag.contains("z")) {
            blockPos = new BlockPos(
                tag.getIntOr("x", 0),
                tag.getIntOr("y", 0),
                tag.getIntOr("z", 0)
            );
        }
        if (tag.contains("direction")) {
            try {
                direction = Direction.valueOf(tag.getStringOr("direction", "UP"));
            } catch (IllegalArgumentException ignored) {
                direction = Direction.UP;
            }
        }
        delayTicks = Math.max(0, tag.getIntOr("delayTicks", tag.getIntOr("delay", 0)));
        times = Math.max(0, tag.getIntOr("times", 1));
        autoPickaxe = tag.getBooleanOr("autoPickaxe", true);
        manualDirection = tag.getBooleanOr("manualDirection", false);
        if (tag.contains("enabled")) enabled = tag.getBooleanOr("enabled", true);
    }

    @Override
    public MacroActionType getType() {
        return MacroActionType.INSTA_BREAK;
    }

    @Override
    public String getDisplayName() {
        return "InstaBreak @ " + blockPos.getX() + "," + blockPos.getY() + "," + blockPos.getZ()
            + (times == 0 ? " ∞" : " x" + times);
    }

    @Override
    public String getIcon() {
        return "IB";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
