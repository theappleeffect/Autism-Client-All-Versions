package autismclient.util;

import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class PackUtilWindowBranding {
    public static final String WINDOW_TITLE = "AUTISM Client";
    private static final String ICON_ROOT = "assets/autismclient/icons/window/";
    private static final int[] ICON_SIZES = {16, 32, 48, 128, 256};
    private static boolean applied = false;

    private PackUtilWindowBranding() {
    }

    public static void apply(Minecraft client) {
        if (applied || client == null || client.getWindow() == null) return;

        client.getWindow().setTitle(WINDOW_TITLE);
        applyIcon(client.getWindow().handle());
        applied = true;
    }

    private static void applyIcon(long windowHandle) {
        if (windowHandle == 0L) return;

        if (MacosUtil.IS_MACOS) {
            try {
                MacosUtil.loadIcon(() -> openIconStream("mac_icon.png"));
            } catch (Throwable ignored) {
            }
            return;
        }

        List<ByteBuffer> allocatedBuffers = new ArrayList<>(ICON_SIZES.length);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer icons = GLFWImage.malloc(ICON_SIZES.length, stack);

            for (int i = 0; i < ICON_SIZES.length; i++) {
                int size = ICON_SIZES[i];
                try (InputStream stream = openIconStream("icon_" + size + "x" + size + ".png");
                     NativeImage image = NativeImage.read(stream)) {
                    ByteBuffer pixels = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4);
                    allocatedBuffers.add(pixels);
                    pixels.asIntBuffer().put(image.getPixelsABGR());
                    icons.position(i);
                    icons.width(image.getWidth());
                    icons.height(image.getHeight());
                    icons.pixels(pixels);
                }
            }

            GLFW.glfwSetWindowIcon(windowHandle, icons.position(0));
        } catch (Throwable ignored) {
        } finally {
            allocatedBuffers.forEach(MemoryUtil::memFree);
        }
    }

    private static InputStream openIconStream(String fileName) throws IOException {
        InputStream stream = PackUtilWindowBranding.class.getClassLoader().getResourceAsStream(ICON_ROOT + fileName);
        if (stream == null) {
            throw new IOException("Missing PackUtil window icon resource: " + fileName);
        }
        return stream;
    }
}
