package autismclient.gui.packui;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public final class PackUiTextMeshUniforms {
    private static final int SIZE = new Std140SizeCalculator()
        .putMat4f()
        .putMat4f()
        .putFloat()
        .get();

    private static final Data DATA = new Data();
    private static final DynamicUniformStorage<Data> STORAGE = new DynamicUniformStorage<>("PackUI Component UBO", SIZE, 16);

    private PackUiTextMeshUniforms() {
    }

    public static void flipFrame() {
        STORAGE.endFrame();
        PackUiAtlasTextRenderer.resetFrameUploadGate();
    }

    static GpuBufferSlice write(Matrix4f proj, Matrix4f modelView) {
        return write(proj, modelView, 1.0f);
    }

    static GpuBufferSlice write(Matrix4f proj, Matrix4f modelView, float alphaMultiplier) {
        DATA.proj = proj;
        DATA.modelView = modelView;
        DATA.alphaMultiplier = alphaMultiplier;
        return STORAGE.writeUniform(DATA);
    }

    private static final class Data implements DynamicUniformStorage.DynamicUniform {
        private Matrix4f proj;
        private Matrix4f modelView;
        private float alphaMultiplier;

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putMat4f(proj)
                .putMat4f(modelView)
                .putFloat(alphaMultiplier);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
