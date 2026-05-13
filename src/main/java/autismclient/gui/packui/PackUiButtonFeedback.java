package autismclient.gui.packui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PackUiButtonFeedback {
    private static final float HOVER_IN_SECONDS = 0.20f;
    private static final float HOVER_OUT_SECONDS = 0.10f;
    private static final float CLICK_GLOW_SECONDS = 0.18f;
    private static final float RIPPLE_SECONDS = 0.42f;
    private static final int MAX_RIPPLES = 2;
    private static final long STALE_NANOS = 12_000_000_000L;
    private static final int MAX_TRACKED_KEYS = 768;

    private static final Map<String, PackUiButtonFeedback> KEYED_STATES = new ConcurrentHashMap<>();
    private static long lastPruneNanos = System.nanoTime();

    private final ArrayDeque<Ripple> ripples = new ArrayDeque<>();
    private long lastUpdateNanos = System.nanoTime();
    private long lastSeenNanos = lastUpdateNanos;
    private float hoverProgress;
    private float clickGlowProgress;
    private boolean pointerDown;

    public static PackUiButtonFeedback forKey(String key) {
        pruneIfNeeded();
        PackUiButtonFeedback feedback = KEYED_STATES.computeIfAbsent(key, ignored -> new PackUiButtonFeedback());
        feedback.lastSeenNanos = System.nanoTime();
        return feedback;
    }

    public static boolean isPrimaryPointerDown() {
        Minecraft client = Minecraft.getInstance();
        return client != null
            && client.getWindow() != null
            && GLFW.glfwGetMouseButton(client.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    public float update(boolean hovered, boolean pressed, float localMouseX, float localMouseY, float width, float height) {
        float delta = tick();
        hoverProgress = animate(hoverProgress, hovered ? 1.0f : 0.0f, hovered ? HOVER_IN_SECONDS : HOVER_OUT_SECONDS, delta);
        if (hovered && pressed && !pointerDown) {
            triggerPress(localMouseX, localMouseY, width, height);
        }
        pointerDown = pressed;
        lastSeenNanos = System.nanoTime();
        return hoverProgress;
    }

    public void triggerPress(float localMouseX, float localMouseY, float width, float height) {
        float safeWidth = Math.max(1.0f, width);
        float safeHeight = Math.max(1.0f, height);
        float clampedX = clamp(localMouseX, 0.0f, safeWidth);
        float clampedY = clamp(localMouseY, 0.0f, safeHeight);
        float maxRadius = Math.max(
            Math.max(distance(clampedX, clampedY, 0.0f, 0.0f), distance(clampedX, clampedY, safeWidth, 0.0f)),
            Math.max(distance(clampedX, clampedY, 0.0f, safeHeight), distance(clampedX, clampedY, safeWidth, safeHeight))
        ) + 1.5f;

        if (ripples.size() >= MAX_RIPPLES) ripples.removeFirst();
        ripples.addLast(new Ripple(clampedX, clampedY, maxRadius));
        clickGlowProgress = 1.0f;
        pointerDown = true;
        lastSeenNanos = System.nanoTime();
    }

    float hoverProgress() {
        return hoverProgress;
    }

    public float clickGlowProgress() {
        return clickGlowProgress;
    }

    public void render(GuiGraphicsExtractor context, int x, int y, int width, int height, int ringRgb, int fillRgb, float alphaScale) {
        if (context == null || width <= 0 || height <= 0 || alphaScale <= 0.001f || ripples.isEmpty()) return;

        int clipX2 = x + width;
        int clipY2 = y + height;
        context.enableScissor(x, y, clipX2, clipY2);
        try {
            for (Ripple ripple : ripples) {
                float t = clamp(ripple.age / RIPPLE_SECONDS, 0.0f, 1.0f);
                float eased = 1.0f - (float) Math.pow(1.0f - t, 3.0f);
                float outerRadius = ripple.maxRadius * eased;
                float thickness = Math.max(1.2f, Math.min(4.0f, outerRadius * 0.18f));
                float innerRadius = Math.max(0.0f, outerRadius - thickness);
                float fade = 1.0f - t;

                drawRing(
                    context,
                    x + ripple.centerX,
                    y + ripple.centerY,
                    innerRadius,
                    outerRadius,
                    alphaColor(ringRgb, alphaScale * 0.26f * fade)
                );

                drawDisc(
                    context,
                    x + ripple.centerX,
                    y + ripple.centerY,
                    Math.max(0.9f, outerRadius * 0.20f),
                    alphaColor(fillRgb, alphaScale * 0.11f * fade)
                );
            }
        } finally {
            context.disableScissor();
        }
    }

    private float tick() {
        long now = System.nanoTime();
        float delta = clamp((now - lastUpdateNanos) / 1_000_000_000.0f, 0.0f, 0.05f);
        lastUpdateNanos = now;

        if (clickGlowProgress > 0.0f) {
            clickGlowProgress = Math.max(0.0f, clickGlowProgress - (delta / CLICK_GLOW_SECONDS));
        }

        Iterator<Ripple> iterator = ripples.iterator();
        while (iterator.hasNext()) {
            Ripple ripple = iterator.next();
            ripple.age += delta;
            if (ripple.age >= RIPPLE_SECONDS) iterator.remove();
        }

        return delta;
    }

    private static void pruneIfNeeded() {
        long now = System.nanoTime();
        if (KEYED_STATES.size() < MAX_TRACKED_KEYS && now - lastPruneNanos < 2_000_000_000L) return;
        lastPruneNanos = now;
        KEYED_STATES.entrySet().removeIf(entry -> now - entry.getValue().lastSeenNanos > STALE_NANOS);
    }

    private static float animate(float current, float target, float durationSeconds, float deltaSeconds) {
        if (durationSeconds <= 0.0f) return target;
        float amount = clamp(deltaSeconds / durationSeconds, 0.0f, 1.0f);
        if (current < target) return Math.min(target, current + amount);
        if (current > target) return Math.max(target, current - amount);
        return current;
    }

    private static int alphaColor(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(clamp(alpha, 0.0f, 1.0f) * 255.0f)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static void drawDisc(GuiGraphicsExtractor context, float centerX, float centerY, float radius, int color) {
        if (((color >>> 24) & 0xFF) <= 0 || radius <= 0.5f) return;
        float radiusSq = radius * radius;
        int top = (int) Math.floor(centerY - radius);
        int bottom = (int) Math.ceil(centerY + radius);

        for (int py = top; py < bottom; py++) {
            float dy = (py + 0.5f) - centerY;
            float dxSq = radiusSq - (dy * dy);
            if (dxSq <= 0.0f) continue;
            float dx = (float) Math.sqrt(dxSq);
            int x1 = (int) Math.floor(centerX - dx);
            int x2 = (int) Math.ceil(centerX + dx);
            if (x2 > x1) context.fill(x1, py, x2, py + 1, color);
        }
    }

    private static void drawRing(GuiGraphicsExtractor context, float centerX, float centerY, float innerRadius, float outerRadius, int color) {
        if (((color >>> 24) & 0xFF) <= 0 || outerRadius <= 0.5f) return;
        float outerSq = outerRadius * outerRadius;
        float innerSq = innerRadius * innerRadius;
        int top = (int) Math.floor(centerY - outerRadius);
        int bottom = (int) Math.ceil(centerY + outerRadius);

        for (int py = top; py < bottom; py++) {
            float dy = (py + 0.5f) - centerY;
            float outerDxSq = outerSq - (dy * dy);
            if (outerDxSq <= 0.0f) continue;

            float outerDx = (float) Math.sqrt(outerDxSq);
            float innerDx = innerRadius > Math.abs(dy) ? (float) Math.sqrt(Math.max(0.0f, innerSq - (dy * dy))) : 0.0f;
            int leftOuter = (int) Math.floor(centerX - outerDx);
            int leftInner = (int) Math.ceil(centerX - innerDx);
            int rightInner = (int) Math.floor(centerX + innerDx);
            int rightOuter = (int) Math.ceil(centerX + outerDx);

            if (innerDx <= 0.5f || rightInner <= leftInner) {
                if (rightOuter > leftOuter) context.fill(leftOuter, py, rightOuter, py + 1, color);
                continue;
            }

            if (leftInner > leftOuter) context.fill(leftOuter, py, leftInner, py + 1, color);
            if (rightOuter > rightInner) context.fill(rightInner, py, rightOuter, py + 1, color);
        }
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt((dx * dx) + (dy * dy));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Ripple {
        private final float centerX;
        private final float centerY;
        private final float maxRadius;
        private float age;

        private Ripple(float centerX, float centerY, float maxRadius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.maxRadius = maxRadius;
        }
    }
}
