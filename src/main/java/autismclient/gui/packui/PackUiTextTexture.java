package autismclient.gui.packui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

final class PackUiTextTexture extends AbstractTexture {
    private final GpuTexture texture;
    private final GpuSampler sampler;
    private final GpuTextureView textureView;
    private static PackUiTextTexture whiteTexture;

    PackUiTextTexture(int width, int height) {
        texture = RenderSystem.getDevice().createTexture("", 15, TextureFormat.RED8, width, height, 1, 1);
        sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, false);
        textureView = RenderSystem.getDevice().createTextureView(texture);
    }

    void upload(ByteBuffer buffer) {
        NativeImage image = new NativeImage(NativeImage.Format.LUMINANCE, texture.getWidth(0), texture.getHeight(0), false);
        buffer.rewind();
        long byteCount = Math.min(buffer.remaining(), (long) texture.getWidth(0) * texture.getHeight(0));
        MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), image.getPointer(), byteCount);
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, image);
        image.close();
    }

    static PackUiTextTexture getWhiteTexture() {
        if (whiteTexture == null) {
            whiteTexture = new PackUiTextTexture(1, 1);
            ByteBuffer buffer = MemoryUtil.memAlloc(1);
            buffer.put((byte) 0xFF);
            buffer.flip();
            whiteTexture.upload(buffer);
            MemoryUtil.memFree(buffer);
        }
        return whiteTexture;
    }

    public GpuTextureView getTextureView() {
        return textureView;
    }

    public GpuSampler getSampler() {
        return sampler;
    }
}
