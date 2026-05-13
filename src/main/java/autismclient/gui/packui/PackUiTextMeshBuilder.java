package autismclient.gui.packui;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memAddress0;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memPutByte;
import static org.lwjgl.system.MemoryUtil.memPutFloat;
import static org.lwjgl.system.MemoryUtil.memPutInt;

final class PackUiTextMeshBuilder {
    private static final VertexFormat FORMAT = PackUiVertexFormats.POS2_TEXTURE_COLOR;
    private static final VertexFormat.Mode DRAW_MODE = VertexFormat.Mode.TRIANGLES;

    private final int vertexSize = FORMAT.getVertexSize();
    private final int primitiveIndicesCount = DRAW_MODE.primitiveLength;

    private ByteBuffer vertices;
    private long verticesPointerStart;
    private long verticesPointer;
    private ByteBuffer indices;
    private long indicesPointer;
    private int vertexI;
    private int indicesCount;
    private boolean building;

    void begin() {
        if (building) throw new IllegalStateException("PackUiTextMeshBuilder.begin() called twice.");
        ensureCapacity(256, 384);
        verticesPointer = verticesPointerStart;
        vertexI = 0;
        indicesCount = 0;
        building = true;
    }

    PackUiTextMeshBuilder pos(double x, double y) {
        long p = verticesPointer;
        memPutFloat(p, (float) x);
        memPutFloat(p + 4, (float) y);
        verticesPointer += 8;
        return this;
    }

    PackUiTextMeshBuilder tex(double u, double v) {
        long p = verticesPointer;
        memPutFloat(p, (float) u);
        memPutFloat(p + 4, (float) v);
        verticesPointer += 8;
        return this;
    }

    PackUiTextMeshBuilder color(int argb) {
        long p = verticesPointer;
        memPutByte(p, (byte) ((argb >>> 16) & 0xFF));
        memPutByte(p + 1, (byte) ((argb >>> 8) & 0xFF));
        memPutByte(p + 2, (byte) (argb & 0xFF));
        memPutByte(p + 3, (byte) ((argb >>> 24) & 0xFF));
        verticesPointer += 4;
        return this;
    }

    int next() {
        return vertexI++;
    }

    void quad(int i1, int i2, int i3, int i4) {
        long p = indicesPointer + (indicesCount * 4L);
        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);
        memPutInt(p + 12, i3);
        memPutInt(p + 16, i4);
        memPutInt(p + 20, i1);
        indicesCount += 6;
    }

    void ensureCapacity(int vertexCount, int indexCount) {
        if (vertices == null || indices == null) {
            vertices = BufferUtils.createByteBuffer(vertexSize * Math.max(256, vertexCount));
            verticesPointerStart = memAddress0(vertices);
            verticesPointer = verticesPointerStart;
            indices = BufferUtils.createByteBuffer(Integer.BYTES * Math.max(384, indexCount));
            indicesPointer = memAddress0(indices);
            return;
        }

        if ((vertexI + vertexCount) * vertexSize >= vertices.capacity()) {
            int offset = (int) (verticesPointer - verticesPointerStart);
            int newSize = Math.max(vertices.capacity() * 2, vertices.capacity() + (vertexCount * vertexSize));
            ByteBuffer newVertices = BufferUtils.createByteBuffer(newSize);
            memCopy(memAddress0(vertices), memAddress0(newVertices), offset);
            vertices = newVertices;
            verticesPointerStart = memAddress0(vertices);
            verticesPointer = verticesPointerStart + offset;
        }

        if ((indicesCount + indexCount) * Integer.BYTES >= indices.capacity()) {
            int newSize = Math.max(indices.capacity() * 2, indices.capacity() + (indexCount * Integer.BYTES));
            ByteBuffer newIndices = BufferUtils.createByteBuffer(newSize);
            memCopy(memAddress0(indices), memAddress0(newIndices), indicesCount * 4L);
            indices = newIndices;
            indicesPointer = memAddress0(indices);
        }
    }

    void end() {
        if (!building) throw new IllegalStateException("PackUiTextMeshBuilder.end() called without begin().");
        building = false;
    }

    void addColoredQuad(float x0, float y0, float x1, float y1, int color) {
        if (!building) return;
        ensureCapacity(4, 6);

        float minX = Math.min(x0, x1);
        float maxX = Math.max(x0, x1);
        float minY = Math.min(y0, y1);
        float maxY = Math.max(y0, y1);

        int i0 = next();
        pos(minX, minY).tex(1, 1).color(color);
        int i1 = next();
        pos(minX, maxY).tex(1, 1).color(color);
        int i2 = next();
        pos(maxX, maxY).tex(1, 1).color(color);
        int i3 = next();
        pos(maxX, minY).tex(1, 1).color(color);
        quad(i0, i1, i2, i3);
    }

    boolean isBuilding() {
        return building;
    }

    int getIndicesCount() {
        return indicesCount;
    }

    int snapshotIndicesCount() {
        return indicesCount;
    }

    GpuBuffer getVertexBuffer() {
        vertices.limit((int) (verticesPointer - verticesPointerStart));
        return FORMAT.uploadImmediateVertexBuffer(vertices);
    }

    GpuBuffer getIndexBuffer() {
        indices.limit(indicesCount * Integer.BYTES);
        return FORMAT.uploadImmediateIndexBuffer(indices);
    }
}
