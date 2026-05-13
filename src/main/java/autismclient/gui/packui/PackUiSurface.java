package autismclient.gui.packui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class PackUiSurface {
    private final PackUiTheme theme;
    private final PackUiNode root;
    private final float density;

    public PackUiSurface(PackUiTheme theme, PackUiNode root) {
        this(theme, root, PackUiTheme.DEFAULT_DENSITY);
    }

    public PackUiSurface(PackUiTheme theme, PackUiNode root, float density) {
        this.theme = theme == null ? new PackUiTheme() : theme;
        this.root = root;
        this.density = density <= 0 ? PackUiTheme.DEFAULT_DENSITY : density;
    }

    public PackUiViewport viewport() {
        return PackUiViewport.current(density);
    }

    public void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        if (root == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;

        PackUiViewport viewport = viewport();
        viewport.push(drawContext);
        try {
            PackUiRenderContext context = new PackUiRenderContext(
                drawContext,
                mc.font,
                viewport,
                theme,
                viewport.toUiX(mouseX),
                viewport.toUiY(mouseY),
                delta
            );
            root.render(context);
        } finally {
            viewport.pop(drawContext);
        }
    }

    public PackUiRenderContext measurementContext() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return null;
        PackUiViewport viewport = viewport();
        return new PackUiRenderContext(null, mc.font, viewport, theme, 0, 0, 0);
    }

    public float measurePreferredHeight(float availableWidth) {
        if (root == null) return 0.0f;
        PackUiRenderContext context = measurementContext();
        if (context == null) return 0.0f;
        return root.preferredHeight(context, availableWidth);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (root == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return false;
        PackUiViewport viewport = viewport();
        PackUiRenderContext context = new PackUiRenderContext(null, mc.font, viewport, theme, viewport.toUiX(mouseX), viewport.toUiY(mouseY), 0);
        return root.mouseClicked(context, viewport.toUiX(mouseX), viewport.toUiY(mouseY), button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (root == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return false;
        PackUiViewport viewport = viewport();
        PackUiRenderContext context = new PackUiRenderContext(null, mc.font, viewport, theme, viewport.toUiX(mouseX), viewport.toUiY(mouseY), 0);
        return root.mouseReleased(context, viewport.toUiX(mouseX), viewport.toUiY(mouseY), button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (root == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return false;
        PackUiViewport viewport = viewport();
        PackUiRenderContext context = new PackUiRenderContext(null, mc.font, viewport, theme, viewport.toUiX(mouseX), viewport.toUiY(mouseY), 0);
        return root.mouseDragged(context, viewport.toUiX(mouseX), viewport.toUiY(mouseY), button, (float) (deltaX / viewport.drawScaleX()), (float) (deltaY / viewport.drawScaleY()));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (root == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return false;
        PackUiViewport viewport = viewport();
        PackUiRenderContext context = new PackUiRenderContext(null, mc.font, viewport, theme, viewport.toUiX(mouseX), viewport.toUiY(mouseY), 0);
        return root.mouseScrolled(context, viewport.toUiX(mouseX), viewport.toUiY(mouseY), (float) amount);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (root == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return false;
        PackUiViewport viewport = viewport();
        PackUiRenderContext context = new PackUiRenderContext(null, mc.font, viewport, theme, 0, 0, 0);
        return root.keyPressed(context, keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (root == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return false;
        PackUiViewport viewport = viewport();
        PackUiRenderContext context = new PackUiRenderContext(null, mc.font, viewport, theme, 0, 0, 0);
        return root.charTyped(context, chr, modifiers);
    }

    public boolean hasFocusedTextInput() {
        return hasFocusedTextInput(root);
    }

    public void clearFocusedTextInputs() {
        clearFocusedTextInputs(root);
    }

    private boolean hasFocusedTextInput(PackUiNode node) {
        if (node == null || !node.isVisible()) return false;
        if (node instanceof PackUiFocusableTextInput input && input.isFocused()) return true;
        if (node instanceof PackUiContainer container) {
            for (PackUiNode child : container.children()) {
                if (hasFocusedTextInput(child)) return true;
            }
        }
        return false;
    }

    private void clearFocusedTextInputs(PackUiNode node) {
        if (node == null || !node.isVisible()) return;
        if (node instanceof PackUiFocusableTextInput input) input.setFocused(false);
        if (node instanceof PackUiContainer container) {
            for (PackUiNode child : container.children()) {
                clearFocusedTextInputs(child);
            }
        }
    }
}
