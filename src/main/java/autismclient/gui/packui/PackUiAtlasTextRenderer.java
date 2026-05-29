package autismclient.gui.packui;

//? if >=1.21.6 {
import autismclient.util.PackUtilUiScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackRange;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

final class PackUiAtlasTextRenderer {
    private static final String PACK_UI_FONT_RESOURCE_PATH = "/assets/autismclient/font/dm_sans/pack_ui_regular.ttf";
    private static final String FONT_RESOURCE_PATH = PACK_UI_FONT_RESOURCE_PATH;
    private static final int ATLAS_SIZE = 2048;
    private static final int[] RANGE_STARTS = {32, 160, 256, 880, 1024, 8734};
    private static final int[] RANGE_COUNTS = {95, 96, 128, 144, 256, 1};
    private static final Map<FontRole, PackUiAtlasFont> FONT_CACHE = new ConcurrentHashMap<>();
    private static GuiGraphics managedContext;
    private static int managedLayerDepth;

    private static PackUiTextMeshBuilder frameMesh = new PackUiTextMeshBuilder();
    private static PackUiTextMeshBuilder fillMesh = new PackUiTextMeshBuilder();
    private static boolean batching;
    private static boolean hasFills;
    private static PackUiTextTexture batchTexture;

    private static org.joml.Matrix3x2f savedPose = new org.joml.Matrix3x2f();

    private static final ArrayList<PendingBatch> pendingBatches = new ArrayList<>();
    private static final ArrayList<OverlaySlice> overlaySlices = new ArrayList<>();
    private static final ArrayList<Integer> fillBoundaries = new ArrayList<>();
    private static final ArrayList<TextOccluder> textOccluders = new ArrayList<>();
    private static final float COVERED_TEXT_ALPHA = 0.25f;

    record OverlaySlice(int indexCount, PackUiTextTexture texture) {}
    public record GuiRect(int x0, int y0, int x1, int y1) {}
    private record TextOccluder(GuiRect rect, int occludesSlicesBefore) {}
    private record PendingBatch(PackUiTextMeshBuilder textMesh, PackUiTextMeshBuilder fillMesh,
                                PackUiTextTexture texture, org.joml.Matrix3x2f pose,
                                List<OverlaySlice> overlaySlices, List<Integer> fillBoundaries,
                                List<TextOccluder> textOccluders, boolean hasFills) {}

    private static final Map<String, ByteBuffer> cachedFontBuffers = new ConcurrentHashMap<>();

    private static final AtomicBoolean fontsReady = new AtomicBoolean(false);
    private static final AtomicBoolean initStarted = new AtomicBoolean(false);

    private static final Map<FontRole, PrecomputedAtlas> precomputedAtlases = new ConcurrentHashMap<>();

    private PackUiAtlasTextRenderer() {
    }

    public static void eagerInit() {
        if (!initStarted.compareAndSet(false, true)) return;
        try {
            ByteBuffer fontBuffer = getCachedFontBuffer(PACK_UI_FONT_RESOURCE_PATH);
            PrecomputedAtlas shared = precomputeSharedAtlas(fontBuffer);
            for (FontRole role : FontRole.values()) {
                precomputedAtlases.put(role, shared);
            }
            fontsReady.set(true);
        } catch (IOException e) {
            fontsReady.set(false);
        }
    }

    public static boolean isReady() {
        return fontsReady.get();
    }

    public static boolean supports(Identifier fontId) {
        return PackUiAssets.FONT_BODY.equals(fontId)
            || PackUiAssets.FONT_LABEL.equals(fontId)
            || PackUiAssets.FONT_TITLE.equals(fontId)
            || PackUiAssets.FONT_FALLBACK.equals(fontId);
    }

    public static int width(String value, Identifier fontId) {
        if (!supports(fontId) || value == null || value.isEmpty()) return 0;
        PackUiAtlasFont f = fontOrNull(fontId);
        if (f == null) return 0;
        return f.width(value);
    }

    public static int height(Identifier fontId) {
        if (!supports(fontId)) return 0;
        PackUiAtlasFont f = fontOrNull(fontId);
        if (f == null) return 0;
        return f.height();
    }

    public static String trimToWidth(String value, int maxWidth, Identifier fontId) {
        if (!supports(fontId) || value == null || value.isEmpty()) return value == null ? "" : value;
        if (maxWidth <= 0) return "";

        PackUiAtlasFont font = fontOrNull(fontId);
        if (font == null) return value;
        int allowedWidth = maxWidth + 4;
        if (font.width(value) <= allowedWidth) return value;

        String suffix = "...";
        int suffixWidth = font.width(suffix);
        if (suffixWidth > allowedWidth) return "";

        StringBuilder builder = new StringBuilder();
        String bestFit = "";
        float runningWidth = 0.0f;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            runningWidth += font.advance(codePoint);
            builder.appendCodePoint(codePoint);
            if (Math.round(runningWidth) <= allowedWidth) {
                bestFit = builder.toString();
            }
            if (Math.round(runningWidth) + suffixWidth > allowedWidth) break;
            i += Character.charCount(codePoint);
        }

        return bestFit.isEmpty() ? "" : bestFit + suffix;
    }

    public static void draw(GuiGraphics context, String value, Identifier fontId, int color, int x, int y, boolean shadow) {
        if (context == null || value == null || value.isEmpty()) return;
        if (!supports(fontId)) return;
        capturePose(context);

        PackUiAtlasFont f = fontOrNull(fontId);
        if (f == null) return;

        if (batching) {

            if (batchTexture == null) {
                batchTexture = f.texture;
            }
            f.appendTo(frameMesh, value, color, x, y, shadow);
        } else {

            beginBatch();
            batchTexture = f.texture;
            f.appendTo(frameMesh, value, color, x, y, shadow);
        }
    }

    public static void fill(GuiGraphics context, int x0, int y0, int x1, int y1, int color) {
        if (context == null) return;
        capturePose(context);
        if (!batching) beginBatch();
        fillMesh.addColoredQuad(x0, y0, x1, y1, color);
        hasFills = true;
    }

    public static void addTextOccluder(GuiGraphics context, int x0, int y0, int x1, int y1) {
        if (context == null) return;
        capturePose(context);
        if (!batching) beginBatch();
        int minX = Math.min(x0, x1);
        int minY = Math.min(y0, y1);
        int maxX = Math.max(x0, x1);
        int maxY = Math.max(y0, y1);
        if (maxX <= minX || maxY <= minY) return;
        textOccluders.add(new TextOccluder(new GuiRect(minX, minY, maxX, maxY), overlaySlices.size()));
    }

    private static void capturePose(GuiGraphics context) {
        if (context != null) {
            savedPose.set(context.pose());
        }
    }

    static void beginBatch() {
        if (batching) return;
        overlaySlices.clear();
        fillBoundaries.clear();
        textOccluders.clear();
        frameMesh.begin();
        fillMesh.begin();
        hasFills = false;
        batchTexture = null;
        batching = true;
    }

    private static void flushCurrentBatch() {

    }

    static void endBatch(GuiGraphics context) {
        if (!batching) return;
        batching = false;

        if (frameMesh.isBuilding()) frameMesh.end();
        if (fillMesh.isBuilding()) fillMesh.end();

        if (context != null) {
            org.joml.Matrix3x2fc pose = context.pose();
            savedPose.set(pose);
        }

        enqueueCurrentBatch();
    }

    static void interOverlayFlush(GuiGraphics context) {
        if (!batching) beginBatch();
        if (batchTexture != null || hasFills) {
            overlaySlices.add(new OverlaySlice(frameMesh.snapshotIndicesCount(), batchTexture));
            fillBoundaries.add(fillMesh.snapshotIndicesCount());
        }
    }

    static void beginManagedLayer(GuiGraphics context) {
        if (context == null) return;

        if (managedContext == context) {
            managedLayerDepth++;
            return;
        }

        if (managedLayerDepth > 0) {
            if (batching) endBatch(context);
            else enqueueCurrentBatch();
            managedContext = null;
            managedLayerDepth = 0;
        }

        managedContext = context;
        managedLayerDepth = 1;
        beginBatch();
    }

    static void endManagedLayer(GuiGraphics context) {
        if (context == null || managedContext != context) return;

        managedLayerDepth--;
        if (managedLayerDepth > 0) return;

        endBatch(context);
        managedContext = null;
        managedLayerDepth = 0;
    }

    public static void flushPendingOverlayText() {
        if (frameMesh.isBuilding()) frameMesh.end();
        if (fillMesh.isBuilding()) fillMesh.end();
        if (batching) {
            batching = false;
            enqueueCurrentBatch();
        } else {
            enqueueCurrentBatch();
        }

        for (PendingBatch batch : pendingBatches) {
            drawPendingBatch(batch);
        }
        pendingBatches.clear();
        managedContext = null;
        managedLayerDepth = 0;
    }
    public static void discardPendingOverlayText() {
        if (frameMesh.isBuilding()) frameMesh.end();
        if (fillMesh.isBuilding()) fillMesh.end();
        batching = false;
        managedContext = null;
        managedLayerDepth = 0;
        pendingBatches.clear();
        overlaySlices.clear();
        fillBoundaries.clear();
        textOccluders.clear();
        frameMesh = new PackUiTextMeshBuilder();
        fillMesh = new PackUiTextMeshBuilder();
        hasFills = false;
        batchTexture = null;
    }

    private static void enqueueCurrentBatch() {
        boolean hasText = batchTexture != null && frameMesh.getIndicesCount() > 0;
        boolean hasFillGeometry = hasFills && fillMesh.getIndicesCount() > 0;
        if (hasText || hasFillGeometry) {
            pendingBatches.add(new PendingBatch(
                frameMesh,
                fillMesh,
                batchTexture,
                new org.joml.Matrix3x2f(savedPose),
                new ArrayList<>(overlaySlices),
                new ArrayList<>(fillBoundaries),
                new ArrayList<>(textOccluders),
                hasFills
            ));
        }
        frameMesh = new PackUiTextMeshBuilder();
        fillMesh = new PackUiTextMeshBuilder();
        overlaySlices.clear();
        fillBoundaries.clear();
        textOccluders.clear();
        hasFills = false;
        batchTexture = null;
    }

    private static void drawPendingBatch(PendingBatch batch) {
        if (batch.overlaySlices().isEmpty()) {
            if (batch.hasFills()) {
                PackUiTextMeshRenderer.drawRange(batch.fillMesh(), PackUiTextTexture.getWhiteTexture(), batch.pose(), 0, batch.fillMesh().getIndicesCount());
            }
            if (batch.texture() != null && batch.textMesh().getIndicesCount() > 0) {
                PackUiTextMeshRenderer.drawRange(batch.textMesh(), batch.texture(), batch.pose(), 0, batch.textMesh().getIndicesCount());
            }
            return;
        }

        int prevTextIndex = 0;
        int prevFillIndex = 0;
        for (int i = 0; i < batch.overlaySlices().size(); i++) {
            int fillBoundary = i < batch.fillBoundaries().size() ? batch.fillBoundaries().get(i) : prevFillIndex;
            int fillCount = fillBoundary - prevFillIndex;
            if (fillCount > 0) {
                PackUiTextMeshRenderer.drawRange(batch.fillMesh(), PackUiTextTexture.getWhiteTexture(), batch.pose(), prevFillIndex, fillCount);
            }
            prevFillIndex = fillBoundary;

            OverlaySlice textSlice = batch.overlaySlices().get(i);
            int textCount = textSlice.indexCount - prevTextIndex;
            if (textCount > 0) {
                drawTextSliceWithOcclusion(batch, i, textSlice.texture, prevTextIndex, textCount);
            }
            prevTextIndex = textSlice.indexCount;
        }

        int remainingFill = batch.fillMesh().getIndicesCount() - prevFillIndex;
        if (remainingFill > 0) {
            PackUiTextMeshRenderer.drawRange(batch.fillMesh(), PackUiTextTexture.getWhiteTexture(), batch.pose(), prevFillIndex, remainingFill);
        }

        int remainingText = batch.textMesh().getIndicesCount() - prevTextIndex;
        if (remainingText > 0 && batch.texture() != null) {
            PackUiTextMeshRenderer.drawRange(batch.textMesh(), batch.texture(), batch.pose(), prevTextIndex, remainingText);
        }
    }

    private static void drawTextSliceWithOcclusion(PendingBatch batch, int sliceIndex, PackUiTextTexture texture, int firstIndex, int indexCount) {
        if (texture == null) return;
        ArrayList<GuiRect> visible = new ArrayList<>();
        ArrayList<GuiRect> covered = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        GuiRect screen = new GuiRect(0, 0, PackUtilUiScale.getVirtualScreenWidth(), PackUtilUiScale.getVirtualScreenHeight());
        visible.add(screen);
        for (TextOccluder occluder : batch.textOccluders()) {
            if (sliceIndex < occluder.occludesSlicesBefore()) {
                GuiRect clipped = intersect(screen, occluder.rect());
                if (clipped != null) {
                    addCoveredRect(covered, clipped);
                    subtractRect(visible, clipped);
                }
            }
        }
        for (GuiRect rect : visible) {
            PackUiTextMeshRenderer.drawRange(batch.textMesh(), texture, batch.pose(), firstIndex, indexCount, rect);
        }
        for (GuiRect rect : covered) {
            PackUiTextMeshRenderer.drawRange(batch.textMesh(), texture, batch.pose(), firstIndex, indexCount, rect, COVERED_TEXT_ALPHA);
        }
    }

    private static GuiRect intersect(GuiRect a, GuiRect b) {
        int x0 = Math.max(a.x0(), b.x0());
        int y0 = Math.max(a.y0(), b.y0());
        int x1 = Math.min(a.x1(), b.x1());
        int y1 = Math.min(a.y1(), b.y1());
        return x1 > x0 && y1 > y0 ? new GuiRect(x0, y0, x1, y1) : null;
    }

    private static void addCoveredRect(ArrayList<GuiRect> covered, GuiRect rect) {
        ArrayList<GuiRect> fragments = new ArrayList<>();
        fragments.add(rect);
        for (GuiRect existing : covered) {
            subtractRect(fragments, existing);
            if (fragments.isEmpty()) return;
        }
        covered.addAll(fragments);
    }

    private static void subtractRect(ArrayList<GuiRect> visible, GuiRect cut) {
        for (int i = visible.size() - 1; i >= 0; i--) {
            GuiRect base = visible.remove(i);
            int ix0 = Math.max(base.x0(), cut.x0());
            int iy0 = Math.max(base.y0(), cut.y0());
            int ix1 = Math.min(base.x1(), cut.x1());
            int iy1 = Math.min(base.y1(), cut.y1());
            if (ix1 <= ix0 || iy1 <= iy0) {
                visible.add(base);
                continue;
            }
            if (base.y0() < iy0) visible.add(new GuiRect(base.x0(), base.y0(), base.x1(), iy0));
            if (iy1 < base.y1()) visible.add(new GuiRect(base.x0(), iy1, base.x1(), base.y1()));
            if (base.x0() < ix0) visible.add(new GuiRect(base.x0(), iy0, ix0, iy1));
            if (ix1 < base.x1()) visible.add(new GuiRect(ix1, iy0, base.x1(), iy1));
        }
    }

    public static void resetFrameUploadGate() {

    }

    private static PackUiAtlasFont fontOrNull(Identifier fontId) {
        FontRole role = FontRole.fromIdentifier(fontId);
        PackUiAtlasFont cached = FONT_CACHE.get(role);
        if (cached != null) return cached;

        if (!fontsReady.get()) return null;

        PrecomputedAtlas pre = precomputedAtlases.get(FontRole.BODY);
        if (pre == null) return null;

        PackUiTextTexture sharedTexture = new PackUiTextTexture(ATLAS_SIZE, ATLAS_SIZE);
        sharedTexture.upload(pre.bitmap);

        for (FontRole r : FontRole.values()) {
            PrecomputedAtlas roleAtlas = precomputedAtlases.get(r);
            if (roleAtlas != null) {
                FONT_CACHE.put(r, new PackUiAtlasFont(roleAtlas, r, sharedTexture));
            }
        }
        return FONT_CACHE.get(role);
    }

    private enum FontRole {
        BODY,
        LABEL,
        TITLE,
        FALLBACK;

        private static FontRole fromIdentifier(Identifier fontId) {
            if (PackUiAssets.FONT_TITLE.equals(fontId)) return TITLE;
            if (PackUiAssets.FONT_LABEL.equals(fontId)) return LABEL;
            if (PackUiAssets.FONT_FALLBACK.equals(fontId)) return FALLBACK;
            return BODY;
        }
    }

    private static final class PrecomputedAtlas {
        final Map<Integer, Glyph> glyphs;
        final Glyph fallbackGlyph;
        final float ascent;
        final int packedHeight;
        final ByteBuffer bitmap;

        PrecomputedAtlas(Map<Integer, Glyph> glyphs, Glyph fallbackGlyph, float ascent,
                         int packedHeight, ByteBuffer bitmap) {
            this.glyphs = glyphs;
            this.fallbackGlyph = fallbackGlyph;
            this.ascent = ascent;
            this.packedHeight = packedHeight;
            this.bitmap = bitmap;
        }
    }

    private static PrecomputedAtlas precomputeSharedAtlas(ByteBuffer fontBuffer) {
        fontBuffer.rewind();
        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
            throw new IllegalStateException("Failed to initialize font " + FONT_RESOURCE_PATH);
        }

        int packedHeight = packedPixelHeight(FontRole.TITLE);
        ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE);
        STBTTPackContext packContext = STBTTPackContext.create();
        STBTTPackedchar.Buffer[] packedRanges = {
            STBTTPackedchar.create(95),
            STBTTPackedchar.create(96),
            STBTTPackedchar.create(128),
            STBTTPackedchar.create(144),
            STBTTPackedchar.create(256),
            STBTTPackedchar.create(1)
        };

        STBTruetype.stbtt_PackBegin(packContext, bitmap, ATLAS_SIZE, ATLAS_SIZE, 0, 1);
        STBTruetype.stbtt_PackSetOversampling(packContext, 2, 2);

        STBTTPackRange.Buffer packRanges = STBTTPackRange.create(packedRanges.length);
        packRanges.put(STBTTPackRange.create().set(packedHeight, 32, null, 95, packedRanges[0], (byte) 2, (byte) 2));
        packRanges.put(STBTTPackRange.create().set(packedHeight, 160, null, 96, packedRanges[1], (byte) 2, (byte) 2));
        packRanges.put(STBTTPackRange.create().set(packedHeight, 256, null, 128, packedRanges[2], (byte) 2, (byte) 2));
        packRanges.put(STBTTPackRange.create().set(packedHeight, 880, null, 144, packedRanges[3], (byte) 2, (byte) 2));
        packRanges.put(STBTTPackRange.create().set(packedHeight, 1024, null, 256, packedRanges[4], (byte) 2, (byte) 2));
        packRanges.put(STBTTPackRange.create().set(packedHeight, 8734, null, 1, packedRanges[5], (byte) 2, (byte) 2));
        packRanges.flip();

        STBTruetype.stbtt_PackFontRanges(packContext, fontBuffer, 0, packRanges);
        STBTruetype.stbtt_PackEnd(packContext);

        float stbScale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, packedHeight);
        int fontAscent;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascentBuffer = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascentBuffer, null, null);
            fontAscent = ascentBuffer.get(0);
        }

        Map<Integer, Glyph> glyphs = new HashMap<>();
        for (int i = 0; i < packedRanges.length; i++) {
            STBTTPackedchar.Buffer chars = packedRanges[i];
            int offset = packRanges.get(i).first_unicode_codepoint_in_range();
            for (int j = 0; j < chars.capacity(); j++) {
                STBTTPackedchar glyph = chars.get(j);
                glyphs.put(j + offset, new Glyph(
                    glyph.xoff(),
                    glyph.yoff(),
                    glyph.xoff2(),
                    glyph.yoff2(),
                    glyph.x0() / (float) ATLAS_SIZE,
                    glyph.y0() / (float) ATLAS_SIZE,
                    glyph.x1() / (float) ATLAS_SIZE,
                    glyph.y1() / (float) ATLAS_SIZE,
                    glyph.xadvance()
                ));
            }
        }

        Glyph fallbackGlyph = glyphs.getOrDefault(32,
            new Glyph(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Math.max(2.0f, packedHeight / 3.0f)));
        float ascent = fontAscent * stbScale;

        return new PrecomputedAtlas(glyphs, fallbackGlyph, ascent, packedHeight, bitmap);
    }

    private static final class PackUiAtlasFont {
        private final PackUiTextTexture texture;
        private final Map<Integer, Glyph> glyphs;
        private final Glyph fallbackGlyph;
        private final float ascent;
        private final int displayLineHeight;
        private final float drawScale;

        private PackUiAtlasFont(PrecomputedAtlas pre, FontRole role, PackUiTextTexture sharedTexture) {
            this.glyphs = pre.glyphs;
            this.fallbackGlyph = pre.fallbackGlyph;
            this.ascent = pre.ascent;
            this.displayLineHeight = displayPixelHeight(role);
            this.drawScale = this.displayLineHeight / (float) pre.packedHeight;
            this.texture = sharedTexture;
        }

        private int width(String value) {
            float width = 0.0f;
            for (int i = 0; i < value.length(); ) {
                int codePoint = value.codePointAt(i);
                width += advance(codePoint);
                i += Character.charCount(codePoint);
            }
            return Math.round(width);
        }

        private float advance(int codePoint) {
            return glyph(codePoint).advance * drawScale;
        }

        private int height() {
            return displayLineHeight;
        }

        private void appendTo(PackUiTextMeshBuilder mesh, String value, int color, int x, int y, boolean shadow) {
            if (shadow) {
                int shadowAlpha = Math.max(0, Math.min(255, ((color >>> 24) & 0xFF) - 90));
                int shadowColor = (shadowAlpha << 24) | 0x00101010;
                appendGlyphs(mesh, value, shadowColor, x + Math.max(0.75f, drawScale), y + Math.max(0.75f, drawScale));
            }
            appendGlyphs(mesh, value, color, x, y);
        }

        private void appendGlyphs(PackUiTextMeshBuilder mesh, String value, int color, float x, float y) {
            float cursorX = x;
            float baselineY = y + (ascent * drawScale);
            mesh.ensureCapacity(value.length() * 4, value.length() * 6);

            for (int i = 0; i < value.length(); ) {
                int codePoint = value.codePointAt(i);
                Glyph glyph = glyph(codePoint);
                float x0 = cursorX + (glyph.x0 * drawScale);
                float y0 = baselineY + (glyph.y0 * drawScale);
                float x1 = cursorX + (glyph.x1 * drawScale);
                float y1 = baselineY + (glyph.y1 * drawScale);

                if (x1 > x0 && y1 > y0) {
                    int i1 = mesh.pos(x0, y0).tex(glyph.u0, glyph.v0).color(color).next();
                    int i2 = mesh.pos(x0, y1).tex(glyph.u0, glyph.v1).color(color).next();
                    int i3 = mesh.pos(x1, y1).tex(glyph.u1, glyph.v1).color(color).next();
                    int i4 = mesh.pos(x1, y0).tex(glyph.u1, glyph.v0).color(color).next();
                    mesh.quad(i1, i2, i3, i4);
                }

                cursorX += glyph.advance * drawScale;
                i += Character.charCount(codePoint);
            }
        }

        private Glyph glyph(int codePoint) {
            return glyphs.getOrDefault(codePoint, fallbackGlyph);
        }
    }

    private static synchronized ByteBuffer getCachedFontBuffer(String resourcePath) throws IOException {
        ByteBuffer cached = cachedFontBuffers.get(resourcePath);
        if (cached != null) {
            cached.rewind();
            return cached;
        }
        try (InputStream input = PackUiAtlasFont.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("Missing font resource " + resourcePath);
            }
            byte[] bytes = input.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            cachedFontBuffers.put(resourcePath, buffer);
            return buffer;
        }
    }

    private static int packedPixelHeight(FontRole role) {
        return switch (role) {
            case BODY, FALLBACK -> 30;
            case LABEL -> 33;
            case TITLE -> 36;
        };
    }

    private static int displayPixelHeight(FontRole role) {
        return switch (role) {
            case BODY, FALLBACK -> 11;
            case LABEL -> 12;
            case TITLE -> 13;
        };
    }

    private record Glyph(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float advance) {
    }
}
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

// Pre-1.21.6 stub: the GPU text engine needs the 1.21.6+ render API, so PackUiText
// routes through vanilla Font instead (isReady()/supports() report false). These no-ops
// keep PackUiText and the rest of the UI compiling.
final class PackUiAtlasTextRenderer {
    private PackUiAtlasTextRenderer() {
    }

    public static void eagerInit() {
    }

    public static boolean isReady() {
        return false;
    }

    public static boolean supports(Identifier fontId) {
        return false;
    }

    public static int width(String value, Identifier fontId) {
        return 0;
    }

    public static int height(Identifier fontId) {
        return 0;
    }

    public static String trimToWidth(String value, int maxWidth, Identifier fontId) {
        return value == null ? "" : value;
    }

    public static void draw(GuiGraphics context, String value, Identifier fontId, int color, int x, int y, boolean shadow) {
    }

    public static void fill(GuiGraphics context, int x0, int y0, int x1, int y1, int color) {
        context.fill(x0, y0, x1, y1, color);
    }

    public static void addTextOccluder(GuiGraphics context, int x0, int y0, int x1, int y1) {
    }

    static void beginBatch() {
    }

    static void endBatch(GuiGraphics context) {
    }

    static void interOverlayFlush(GuiGraphics context) {
    }

    static void beginManagedLayer(GuiGraphics context) {
    }

    static void endManagedLayer(GuiGraphics context) {
    }

    public static void flushPendingOverlayText() {
    }

    public static void discardPendingOverlayText() {
    }

    public static void resetFrameUploadGate() {
    }
}
*///?}
