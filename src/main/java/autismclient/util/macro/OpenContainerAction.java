package autismclient.util.macro;

import autismclient.util.PackUtilContainerTarget;
import autismclient.util.PackUtilRegistryLabels;
import autismclient.util.PackUtilSharedState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.BlockPos;

public class OpenContainerAction implements MacroAction, WaitsForGui {
    public enum TargetMode { BLOCK, ENTITY, LAST_TARGET }

    public TargetMode targetMode = TargetMode.BLOCK;
    public BlockPos blockPos = BlockPos.ZERO;
    public String entityTarget = "";
    public boolean waitForGui = true;
    public String guiName = "";
    private boolean enabled = true;

    @Override
    public void execute(Minecraft mc) {
        if (mc == null || mc.player == null) return;

        PackUtilContainerTarget target = switch (targetMode) {
            case BLOCK -> PackUtilContainerTarget.forBlock(blockPos);
            case ENTITY -> PackUtilContainerTarget.forEntityRef(entityTarget);
            case LAST_TARGET -> PackUtilSharedState.get().getLastContainerTarget();
        };

        if (target != null) {
            target.interact(mc);
        }
    }

    public void captureCurrentLookTarget(Minecraft mc) {
        if (mc == null) return;
        if (mc.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() != null) {
            targetMode = TargetMode.ENTITY;
            entityTarget = PackUtilContainerTarget.toSpecificEntityRef(entityHit.getEntity());
        } else if (mc.hitResult instanceof BlockHitResult blockHit) {
            targetMode = TargetMode.BLOCK;
            blockPos = blockHit.getBlockPos();
            entityTarget = "";
        }
    }

    public String entityTargetLabel() {
        if (entityTarget == null || entityTarget.isBlank()) return "(none)";
        if (entityTarget.startsWith("~")) {
            String[] parts = entityTarget.split("~", 4);
            String name = parts.length >= 4 ? parts[3] : "?";
            String type = parts.length >= 3 ? PackUtilRegistryLabels.entity(parts[2]) : "?";
            return name.isBlank() ? type : name + " (" + type + ")";
        }
        return PackUtilRegistryLabels.entity(entityTarget);
    }

    @Override public boolean isWaitForGui() { return waitForGui; }
    @Override public void setWaitForGui(boolean v) { this.waitForGui = v; }
    @Override public String getWaitGuiName() { return ""; }
    @Override public void setWaitGuiName(String name) { this.guiName = ""; }

    @Override public MacroActionType getType() { return MacroActionType.OPEN_CONTAINER; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }
    @Override public String getIcon() { return "OC"; }

    @Override
    public String getDisplayName() {
        String targetLabel = switch (targetMode) {
            case BLOCK -> blockPos.getX() + "," + blockPos.getY() + "," + blockPos.getZ();
            case ENTITY -> entityTargetLabel();
            case LAST_TARGET -> "Last Target";
        };
        String base = "Open Container " + targetLabel;
        return waitForGui ? base + " (wait)" : base;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "OPEN_CONTAINER");
        tag.putString("targetMode", targetMode.name());
        tag.putInt("x", blockPos.getX());
        tag.putInt("y", blockPos.getY());
        tag.putInt("z", blockPos.getZ());
        tag.putString("entityTarget", entityTarget);
        ListTag entityTargets = new ListTag();
        if (entityTarget != null && !entityTarget.isBlank()) {
            entityTargets.add(StringTag.valueOf(entityTarget));
        }
        tag.put("entityTargets", entityTargets);
        tag.putBoolean("waitForGui", waitForGui);
        tag.putString("guiName", guiName);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        int x = tag.getIntOr("x", 0), y = tag.getIntOr("y", 0), z = tag.getIntOr("z", 0);
        blockPos = new BlockPos(x, y, z);
        try {
            targetMode = TargetMode.valueOf(tag.getStringOr("targetMode", "BLOCK"));
        } catch (IllegalArgumentException ignored) {
            targetMode = TargetMode.BLOCK;
        }
        entityTarget = tag.getStringOr("entityTarget", "");
        if ((entityTarget == null || entityTarget.isBlank()) && tag.contains("entityTargets")) {
            ListTag entityTargets = tag.getList("entityTargets").orElse(new ListTag());
            for (Tag element : entityTargets) {
                String value = element.asString().orElse("");
                if (value != null && !value.isBlank()) {
                    entityTarget = value;
                    break;
                }
            }
        }
        waitForGui = tag.getBooleanOr("waitForGui", true);
        guiName = tag.getStringOr("guiName", "");
        enabled = tag.getBooleanOr("enabled", true);
    }
}
