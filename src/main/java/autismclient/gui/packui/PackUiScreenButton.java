package autismclient.gui.packui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class PackUiScreenButton {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final Component label;
    private final Runnable action;
    private PackUiOverlayButton.Variant variant;
    private boolean active = true;

    public PackUiScreenButton(int x, int y, int width, int height, Component label, PackUiOverlayButton.Variant variant, Runnable action) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label == null ? Component.empty() : label;
        this.variant = variant == null ? PackUiOverlayButton.Variant.SECONDARY : variant;
        this.action = action;
    }

    public PackUiScreenButton setVariant(PackUiOverlayButton.Variant variant) {
        this.variant = variant == null ? PackUiOverlayButton.Variant.SECONDARY : variant;
        return this;
    }

    public PackUiScreenButton setActive(boolean active) {
        this.active = active;
        return this;
    }

    public void render(GuiGraphicsExtractor context, Font textRenderer, int mouseX, int mouseY) {
        PackUiOverlayButton button = PackUiOverlayButton.create(x, y, width, height, label, ignored -> {
            if (action != null) action.run();
        });
        button.setVariant(variant);
        button.active = active;
        PackUiOverlayButton.renderStyled(context, textRenderer, button, mouseX, mouseY);
    }

    public boolean click(double mouseX, double mouseY, int mouseButton) {
        if (!active || mouseButton != 0) return false;
        if (!contains(mouseX, mouseY)) return false;
        if (action != null) action.run();
        return true;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
