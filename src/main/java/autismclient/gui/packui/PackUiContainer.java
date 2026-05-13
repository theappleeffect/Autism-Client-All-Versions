package autismclient.gui.packui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PackUiContainer extends PackUiNode {
    protected final List<PackUiNode> children = new ArrayList<>();

    public <T extends PackUiNode> T add(T child) {
        if (child != null) children.add(child);
        return child;
    }

    public void clearChildren() {
        children.clear();
    }

    public List<PackUiNode> children() {
        return Collections.unmodifiableList(children);
    }

    protected abstract void layoutChildren(PackUiRenderContext context);

    @Override
    public void layout(PackUiRenderContext context) {
        if (!visible) return;
        layoutChildren(context);
        for (PackUiNode child : children) {
            if (child != null && child.isVisible()) {
                child.layout(context);
            }
        }
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;
        layout(context);
        for (PackUiNode child : children) {
            if (child != null && child.isVisible()) {
                child.render(context);
            }
        }
    }

    @Override
    public boolean mouseClicked(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        if (!visible) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            PackUiNode child = children.get(i);
            if (child != null && child.isVisible() && child.contains(mouseX, mouseY) && child.mouseClicked(context, mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        if (!visible) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            PackUiNode child = children.get(i);
            if (child != null && child.isVisible() && child.mouseReleased(context, mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(PackUiRenderContext context, float mouseX, float mouseY, int button, float deltaX, float deltaY) {
        if (!visible) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            PackUiNode child = children.get(i);
            if (child != null && child.isVisible() && child.mouseDragged(context, mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(PackUiRenderContext context, float mouseX, float mouseY, float amount) {
        if (!visible) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            PackUiNode child = children.get(i);
            if (child != null && child.isVisible() && child.contains(mouseX, mouseY) && child.mouseScrolled(context, mouseX, mouseY, amount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(PackUiRenderContext context, int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            PackUiNode child = children.get(i);
            if (child != null && child.isVisible() && child.keyPressed(context, keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(PackUiRenderContext context, char chr, int modifiers) {
        if (!visible) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            PackUiNode child = children.get(i);
            if (child != null && child.isVisible() && child.charTyped(context, chr, modifiers)) {
                return true;
            }
        }
        return false;
    }
}
