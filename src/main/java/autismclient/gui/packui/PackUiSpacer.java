package autismclient.gui.packui;

public class PackUiSpacer extends PackUiNode {
    public PackUiSpacer(float width, float height) {
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        return context.theme().scale(width);
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        return context.theme().scale(height);
    }
}
