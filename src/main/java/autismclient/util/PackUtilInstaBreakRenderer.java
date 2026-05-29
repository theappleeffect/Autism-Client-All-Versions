package autismclient.util;

import net.minecraft.core.BlockPos;

public final class PackUtilInstaBreakRenderer {
    private static volatile BlockPos targetPos;

    private PackUtilInstaBreakRenderer() {
    }

    public static void initialize() {
        // Instabreak world highlight is disabled: it relied on a Fabric
        // LevelRenderEvents API that does not exist across these versions.
        // Target tracking (setTarget/clear) still works for other consumers.
    }

    public static void setTarget(BlockPos pos) {
        targetPos = pos == null ? null : pos.immutable();
    }

    public static void clearTarget(BlockPos pos) {
        BlockPos current = targetPos;
        if (current == null || pos == null || current.equals(pos)) targetPos = null;
    }

    public static void clear() {
        targetPos = null;
    }
}
