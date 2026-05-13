package autismclient.gui.packui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class PackUiText {
    private static final Map<Identifier, FontDescription> FONT_CACHE = new ConcurrentHashMap<>();
    private static final int TRIM_CACHE_LIMIT = 2048;
    private static final ThreadLocal<Integer> VANILLA_RENDER_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final Map<TrimKey, String> TRIM_CACHE = java.util.Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TrimKey, String> eldest) {
            return size() > TRIM_CACHE_LIMIT;
        }
    });

    private PackUiText() {
    }

    public static MutableComponent literal(String value, Identifier fontId, int color) {
        return Component.literal(value == null ? "" : value)
            .setStyle(Style.EMPTY.withFont(fontSource(fontId)).withColor(color));
    }

    private static FontDescription fontSource(Identifier fontId) {
        if (fontId == null) return FONT_CACHE.computeIfAbsent(PackUiAssets.FONT_FALLBACK, FontDescription.Resource::new);
        return FONT_CACHE.computeIfAbsent(fontId, FontDescription.Resource::new);
    }

    public static int width(Font renderer, String value, Identifier fontId, int color) {
        if (PackUiAtlasTextRenderer.supports(fontId) && PackUiAtlasTextRenderer.isReady()) {
            int w = PackUiAtlasTextRenderer.width(value, fontId);
            if (w > 0) return w;
        }
        return renderer.width(literal(value, fontId, color));
    }

    public static int fontHeight(Identifier fontId) {
        if (PackUiAtlasTextRenderer.supports(fontId) && PackUiAtlasTextRenderer.isReady()) {
            int h = PackUiAtlasTextRenderer.height(fontId);
            if (h > 0) return h;
        }
        return 9;
    }

    public static String trimToWidth(Font renderer, String value, int maxWidth, Identifier fontId, int color) {
        String safeValue = value == null ? "" : value;
        TrimKey key = new TrimKey(safeValue, maxWidth, fontId, color);
        String cached = TRIM_CACHE.get(key);
        if (cached != null) return cached;
        String trimmed;
        if (PackUiAtlasTextRenderer.supports(fontId) && PackUiAtlasTextRenderer.isReady()) {
            trimmed = PackUiAtlasTextRenderer.trimToWidth(safeValue, maxWidth, fontId);
            TRIM_CACHE.put(key, trimmed);
            return trimmed;
        }
        if (safeValue.isEmpty()) return "";
        int allowedWidth = maxWidth + 4;
        if (width(renderer, safeValue, fontId, color) <= allowedWidth) {
            TRIM_CACHE.put(key, safeValue);
            return safeValue;
        }

        String suffix = "...";
        if (width(renderer, suffix, fontId, color) > allowedWidth) return "";

        String bestFit = renderer.plainSubstrByWidth(safeValue, Math.max(0, allowedWidth - width(renderer, suffix, fontId, color)));
        trimmed = bestFit.isEmpty() ? "" : bestFit + suffix;
        TRIM_CACHE.put(key, trimmed);
        return trimmed;
    }

    public static void draw(GuiGraphicsExtractor context, Font renderer, String value, Identifier fontId, int color, int x, int y, boolean shadow) {
        if (!isVanillaRenderForced() && PackUiAtlasTextRenderer.supports(fontId) && PackUiAtlasTextRenderer.isReady()) {
            PackUiAtlasTextRenderer.draw(context, value, fontId, color, x, y, shadow);
            return;
        }

        context.text(renderer, literal(value, fontId, color), x, y, color, shadow);
    }

    public static void eagerInitFonts() {
        PackUiAtlasTextRenderer.eagerInit();
    }

    public static void beginManagedLayer(GuiGraphicsExtractor context) {
        PackUiAtlasTextRenderer.beginManagedLayer(context);
    }

    public static void endManagedLayer(GuiGraphicsExtractor context) {
        PackUiAtlasTextRenderer.endManagedLayer(context);
    }

    public static void interOverlayFlush(GuiGraphicsExtractor context) {
        PackUiAtlasTextRenderer.interOverlayFlush(context);
    }

    public static void fill(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1, int color) {
        if (isVanillaRenderForced()) {
            context.fill(x0, y0, x1, y1, color);
            return;
        }
        PackUiAtlasTextRenderer.fill(context, x0, y0, x1, y1, color);
    }

    public static void addTextOccluder(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1) {
        PackUiAtlasTextRenderer.addTextOccluder(context, x0, y0, x1, y1);
    }

    public static void flushPendingOverlayText() {
        PackUiAtlasTextRenderer.flushPendingOverlayText();
    }

    public static void discardPendingOverlayText() {
        PackUiAtlasTextRenderer.discardPendingOverlayText();
    }

    public static void withVanillaRender(Runnable runnable) {
        int depth = VANILLA_RENDER_DEPTH.get();
        VANILLA_RENDER_DEPTH.set(depth + 1);
        try {
            runnable.run();
        } finally {
            if (depth <= 0) VANILLA_RENDER_DEPTH.remove();
            else VANILLA_RENDER_DEPTH.set(depth);
        }
    }

    private static boolean isVanillaRenderForced() {
        return VANILLA_RENDER_DEPTH.get() > 0;
    }

    private record TrimKey(String value, int maxWidth, Identifier fontId, int color) {
    }
}
