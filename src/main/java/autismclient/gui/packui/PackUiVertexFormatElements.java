package autismclient.gui.packui;

import com.mojang.blaze3d.vertex.VertexFormatElement;

final class PackUiVertexFormatElements {
    static final VertexFormatElement POS2 = VertexFormatElement.register(nextId(), 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 2);

    private PackUiVertexFormatElements() {
    }

    private static int nextId() {
        int id = 0;
        while (VertexFormatElement.byId(id) != null) {
            id++;
            if (id >= 32) {
                throw new IllegalStateException("Too many mods registering VertexFormatElements");
            }
        }
        return id;
    }
}
