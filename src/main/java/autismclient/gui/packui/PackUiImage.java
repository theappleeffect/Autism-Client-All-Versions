package autismclient.gui.packui;

import net.minecraft.resources.Identifier;

public class PackUiImage extends PackUiNode {
    private final Identifier textureId;

    public PackUiImage(Identifier textureId, float width, float height) {
        this.textureId = textureId;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        return width;
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        return height;
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible || textureId == null || context.drawContext() == null) return;
        context.drawTexturedQuad(textureId, Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height));
    }
}
