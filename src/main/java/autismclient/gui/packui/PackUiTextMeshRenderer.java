package autismclient.gui.packui;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import autismclient.util.PackUtilUiScale;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;

final class PackUiTextMeshRenderer {

    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f MODEL_VIEW = new Matrix4f();

    private PackUiTextMeshRenderer() {
    }

    static void draw(PackUiTextMeshBuilder mesh, PackUiTextTexture texture, Matrix3x2fc transform) {
        if (mesh.isBuilding()) mesh.end();
        if (mesh.getIndicesCount() <= 0) return;
        drawRange(mesh, texture, transform, 0, mesh.getIndicesCount());
    }

    static void drawRange(PackUiTextMeshBuilder mesh, PackUiTextTexture texture, Matrix3x2fc transform, int firstIndex, int indexCount) {
        drawRange(mesh, texture, transform, firstIndex, indexCount, null, 1.0f);
    }

    static void drawRange(PackUiTextMeshBuilder mesh, PackUiTextTexture texture, Matrix3x2fc transform, int firstIndex, int indexCount, PackUiAtlasTextRenderer.GuiRect scissor) {
        drawRange(mesh, texture, transform, firstIndex, indexCount, scissor, 1.0f);
    }

    static void drawRange(PackUiTextMeshBuilder mesh, PackUiTextTexture texture, Matrix3x2fc transform, int firstIndex, int indexCount, PackUiAtlasTextRenderer.GuiRect scissor, float alphaMultiplier) {
        if (indexCount <= 0) return;
        if (mesh.isBuilding()) mesh.end();
        if (mesh.getIndicesCount() <= 0) return;
        GpuBuffer vertexBuffer = mesh.getVertexBuffer();
        GpuBuffer indexBuffer = mesh.getIndexBuffer();

        PROJECTION.setOrtho(
            0.0f,
            Minecraft.getInstance().getWindow().getGuiScaledWidth(),
            Minecraft.getInstance().getWindow().getGuiScaledHeight(),
            0.0f,
            -1000.0f,
            1000.0f
        );
        MODEL_VIEW.set(
            transform.m00(), transform.m10(), 0.0f, 0.0f,
            transform.m01(), transform.m11(), 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            transform.m20(), transform.m21(), 0.0f, 1.0f
        );

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().mul(MODEL_VIEW);

        GpuBufferSlice meshData = PackUiTextMeshUniforms.write(PROJECTION, RenderSystem.getModelViewStack(), alphaMultiplier);
        RenderPass pass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
                () -> "PackUI Component",
                Minecraft.getInstance().getMainRenderTarget().getColorTextureView(),
                java.util.OptionalInt.empty()
            );

        pass.setPipeline(PackUiTextPipelines.UI_TEXT);
        pass.setUniform("MeshData", meshData);
        pass.bindTexture("u_Texture", texture.getTextureView(), texture.getSampler());
        pass.setVertexBuffer(0, vertexBuffer);
        pass.setIndexBuffer(indexBuffer, VertexFormat.IndexType.INT);
        if (scissor != null) {
            int windowHeight = Minecraft.getInstance().getWindow().getHeight();
            int x = PackUtilUiScale.virtualToFramebufferX(scissor.x0());
            int y = windowHeight - PackUtilUiScale.virtualToFramebufferY(scissor.y1());
            int width = PackUtilUiScale.virtualToFramebufferSize(scissor.x1() - scissor.x0());
            int height = PackUtilUiScale.virtualToFramebufferSize(scissor.y1() - scissor.y0());
            pass.enableScissor(x, y, width, height);
        }
        pass.drawIndexed(0, firstIndex, indexCount, 1);
        pass.close();

        RenderSystem.getModelViewStack().popMatrix();
    }
}
