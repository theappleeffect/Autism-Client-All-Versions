package autismclient.gui.packui;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

final class PackUiVertexFormats {
    static final VertexFormat POS2_TEXTURE_COLOR = VertexFormat.builder()
        .add("Position", PackUiVertexFormatElements.POS2)
        .add("Texture", VertexFormatElement.UV)
        .add("Color", VertexFormatElement.COLOR)
        .build();

    private PackUiVertexFormats() {
    }
}
