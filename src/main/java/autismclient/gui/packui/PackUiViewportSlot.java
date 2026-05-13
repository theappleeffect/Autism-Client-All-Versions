package autismclient.gui.packui;

public class PackUiViewportSlot extends PackUiNode {
    private float preferredHeight = 64.0f;

    public PackUiViewportSlot setPreferredHeight(float preferredHeight) {
        this.preferredHeight = Math.max(0.0f, preferredHeight);
        return this;
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        return context.theme().scale(preferredHeight);
    }
}
