package autismclient.gui.packui;

public abstract class PackUiNode {
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    protected boolean visible = true;
    protected boolean enabled = true;
    protected boolean growX = false;
    protected float hoverProgress = 0.0f;

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public PackUiNode setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        return this;
    }

    public PackUiNode setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public PackUiNode setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public PackUiNode setGrowX(boolean growX) {
        this.growX = growX;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean growX() {
        return growX;
    }

    public boolean contains(float px, float py) {
        return visible && px >= x && px <= x + width && py >= y && py <= y + height;
    }

    protected float animate(float current, float target, float speed, float delta) {
        float amount = Math.max(0.04f, delta * speed);
        if (current < target) return Math.min(target, current + amount);
        if (current > target) return Math.max(target, current - amount);
        return current;
    }

    protected float updateHover(boolean hovered, float delta) {
        hoverProgress = animate(hoverProgress, hovered ? 1.0f : 0.0f, 7.5f, delta);
        return hoverProgress;
    }

    public float preferredWidth(PackUiRenderContext context) {
        return width;
    }

    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        return height;
    }

    public void layout(PackUiRenderContext context) {
    }

    public void render(PackUiRenderContext context) {
    }

    public boolean mouseClicked(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(PackUiRenderContext context, float mouseX, float mouseY, int button, float deltaX, float deltaY) {
        return false;
    }

    public boolean mouseScrolled(PackUiRenderContext context, float mouseX, float mouseY, float amount) {
        return false;
    }

    public boolean keyPressed(PackUiRenderContext context, int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(PackUiRenderContext context, char chr, int modifiers) {
        return false;
    }
}
