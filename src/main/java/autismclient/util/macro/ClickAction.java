package autismclient.util.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ClickAction implements MacroAction, WaitsForGui {
    public enum ContainerInput {
        LEFT, RIGHT
    }

    public ContainerInput type;
    public int clickCount = 1;
    public boolean waitForGui = false;
    public String guiName = "";

    public ClickAction() {}

    public ClickAction(ContainerInput type) {
        this.type = type;
        this.clickCount = 1;
    }

    @Override
    public void execute(Minecraft mc) {
        if (mc.player == null || mc.gameMode == null) return;

        if (type == ContainerInput.LEFT) {
            if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            } else if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
                mc.gameMode.attack(mc.player, ((EntityHitResult) mc.hitResult).getEntity());
                mc.player.swing(InteractionHand.MAIN_HAND);
            } else if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
                mc.gameMode.destroyBlock(((BlockHitResult) mc.hitResult).getBlockPos());
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        } else {
            if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS) {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            } else if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
                mc.gameMode.interactAt(mc.player, ((EntityHitResult) mc.hitResult).getEntity(), (EntityHitResult) mc.hitResult, InteractionHand.MAIN_HAND);
            } else if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult);
            }
        }
    }

    @Override public boolean isWaitForGui() { return waitForGui; }
    @Override public void setWaitForGui(boolean v) { this.waitForGui = v; }
    @Override public String getWaitGuiName() { return guiName; }
    @Override public void setWaitGuiName(String name) { this.guiName = name; }

    @Override
    public MacroActionType getType() {
        return MacroActionType.CLICK;
    }

    @Override
    public net.minecraft.nbt.CompoundTag toTag() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("type", getType().name());
        tag.putString("clickType", (type == null ? ContainerInput.RIGHT : type).name());
        tag.putInt("clickCount", clickCount);
        tag.putBoolean("waitForGui", waitForGui);
        tag.putString("guiName", guiName);
        return tag;
    }

    @Override
    public void fromTag(net.minecraft.nbt.CompoundTag tag) {
        if (tag.contains("clickType")) {
            try {
                type = ContainerInput.valueOf(tag.getStringOr("clickType", "RIGHT"));
            } catch (IllegalArgumentException e) {
                type = ContainerInput.RIGHT;
            }
        }
        if (type == null) type = ContainerInput.RIGHT;
        if (tag.contains("clickCount")) {
            clickCount = tag.getIntOr("clickCount", 1);
        }
        if (tag.contains("waitForGui")) waitForGui = tag.getBooleanOr("waitForGui", false);
        if (tag.contains("guiName")) guiName = tag.getStringOr("guiName", "");
    }

    @Override
    public String getDisplayName() {
        String base = type == ContainerInput.LEFT ? "Left Click" : "Right Click";
        return clickCount > 1 ? base + " x" + clickCount : base;
    }

    @Override
    public String getIcon() {
        return type == ContainerInput.LEFT ? "L" : "R";
    }
}
