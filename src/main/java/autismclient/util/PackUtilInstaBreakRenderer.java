package autismclient.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class PackUtilInstaBreakRenderer {
    private static volatile BlockPos targetPos;
    private static final int RED = 0xFFFF0000;
    private static final float LINE_WIDTH = 2.0f;

    private PackUtilInstaBreakRenderer() {
    }

    public static void initialize() {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            BlockPos pos = targetPos;
            if (pos == null) return;
            Vec3 camera = context.levelState().cameraRenderState.pos;
            double x = pos.getX() - camera.x;
            double y = pos.getY() - camera.y;
            double z = pos.getZ() - camera.z;
            PoseStack poseStack = context.poseStack();
            context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> renderBox(pose, buffer, x, y, z));
        });
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

    private static void renderBox(PoseStack.Pose pose, VertexConsumer buffer, double x, double y, double z) {
        double x2 = x + 1.0;
        double y2 = y + 1.0;
        double z2 = z + 1.0;
        line(pose, buffer, x, y, z, x2, y, z);
        line(pose, buffer, x2, y, z, x2, y, z2);
        line(pose, buffer, x2, y, z2, x, y, z2);
        line(pose, buffer, x, y, z2, x, y, z);
        line(pose, buffer, x, y2, z, x2, y2, z);
        line(pose, buffer, x2, y2, z, x2, y2, z2);
        line(pose, buffer, x2, y2, z2, x, y2, z2);
        line(pose, buffer, x, y2, z2, x, y2, z);
        line(pose, buffer, x, y, z, x, y2, z);
        line(pose, buffer, x2, y, z, x2, y2, z);
        line(pose, buffer, x2, y, z2, x2, y2, z2);
        line(pose, buffer, x, y, z2, x, y2, z2);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer buffer, double x1, double y1, double z1, double x2, double y2, double z2) {
        Vector3f normal = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
        buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(RED).setNormal(pose, normal).setLineWidth(LINE_WIDTH);
        buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(RED).setNormal(pose, normal).setLineWidth(LINE_WIDTH);
    }
}
