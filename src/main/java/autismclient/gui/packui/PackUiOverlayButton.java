package autismclient.gui.packui;

import autismclient.util.PackUtilText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class PackUiOverlayButton {
    private static final PackUiTheme THEME = new PackUiTheme();
    private static final int HORIZONTAL_PADDING = 4;

    public enum Variant {
        SECONDARY,
        DANGER,
        SUCCESS,
        PRIMARY,
        GHOST,
        FILTER_ON,
        FILTER_OFF
    }

    public boolean active = true;
    public boolean visible = true;

    private int x;
    private int y;
    private int width;
    private int height;
    private Component message;
    private Variant variant = Variant.SECONDARY;
    private PackUiTone tone = PackUiTone.BODY;
    private final PressAction primaryAction;
    private final PressAction secondaryAction;

    private static Identifier resolvedFont(int height) {
        return THEME.fontFor(PackUiTone.BODY);
    }

    private static int fittedWidth(int requestedWidth, int height, Component message) {
        if (requestedWidth > 0) return requestedWidth;
        Minecraft client = Minecraft.getInstance();
        Font renderer = client == null ? null : client.font;
        if (renderer == null) return requestedWidth;

        String label = PackUtilText.sanitizeUiLabel(message == null ? "" : message.getString());
        Identifier font = resolvedFont(height);
        int measured = PackUiSizing.fitTextWidthInt(
            renderer,
            label,
            font,
            THEME.color(PackUiTone.BODY),
            HORIZONTAL_PADDING,
            requestedWidth,
            Integer.MAX_VALUE
        );
        return Math.max(requestedWidth, measured);
    }

    private PackUiOverlayButton(int x, int y, int width, int height, Component message, PressAction primaryAction, PressAction secondaryAction) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = message == null ? Component.empty() : message;
        this.primaryAction = primaryAction;
        this.secondaryAction = secondaryAction;
    }

    public static PackUiOverlayButton create(int x, int y, int width, int height, Component message, PressAction pressAction) {
        return new PackUiOverlayButton(x, y, fittedWidth(width, height, message), height, message, pressAction, null);
    }

    public static PackUiOverlayButton create(int x, int y, int width, int height, Component message, PressAction pressAction, PressAction secondaryPressAction) {
        return new PackUiOverlayButton(x, y, fittedWidth(width, height, message), height, message, pressAction, secondaryPressAction);
    }

    public PackUiOverlayButton setTone(PackUiTone tone) {
        this.tone = tone == null ? PackUiTone.BODY : tone;
        return this;
    }

    public static boolean fireIfHit(PackUiOverlayButton button, double mouseX, double mouseY, int mouseButton) {
        if (button == null || !button.visible || !button.active) return false;
        if ((mouseButton != 0 && mouseButton != 1) || !button.contains(mouseX, mouseY)) return false;
        feedbackFor(button).triggerPress((float) (mouseX - button.x), (float) (mouseY - button.y), button.width, button.height);
        PressAction action = mouseButton == 1 ? button.secondaryAction : button.primaryAction;
        if (action == null) return false;
        action.onPress(button);
        return true;
    }

    public static void renderStyled(GuiGraphics context, Font textRenderer, PackUiOverlayButton button, int mouseX, int mouseY) {
        if (button == null || !button.visible) return;

        String label = PackUtilText.sanitizeUiLabel(button.message.getString());
        String upperLabel = label.toUpperCase();
        boolean compactSymbol = label.length() == 1 && button.width <= 14;
        PackUiTone resolvedTone = compactSymbol ? PackUiTone.LABEL : button.tone;
        Identifier font = THEME.fontFor(resolvedTone);
        boolean inferredDanger = "x".equalsIgnoreCase(label) || label.toLowerCase().contains("delete") || label.toLowerCase().contains("clear");
        boolean toggleOn = label.startsWith("[x]") || label.startsWith("[X]") || label.startsWith("[\u2713]")
            || upperLabel.equals("ON") || upperLabel.endsWith(": ON")
            || upperLabel.equals("MODE: ENABLE") || upperLabel.equals("ENABLE")
            || upperLabel.equals("ENABLED") || upperLabel.endsWith(": ENABLED");
        boolean toggleOff = label.startsWith("[ ]")
            || upperLabel.equals("OFF") || upperLabel.endsWith(": OFF")
            || upperLabel.equals("MODE: DISABLE") || upperLabel.equals("DISABLE")
            || upperLabel.equals("DISABLED") || upperLabel.endsWith(": DISABLED");
        Variant variant = button.variant;
        if (variant == Variant.SECONDARY && inferredDanger) variant = Variant.DANGER;
        if (variant == Variant.SECONDARY && toggleOn) variant = Variant.SUCCESS;
        if (variant == Variant.SECONDARY && toggleOff) variant = Variant.DANGER;
        boolean hovered = button.contains(mouseX, mouseY);
        PackUiButtonFeedback feedback = feedbackFor(button);
        float hover = feedback.update(
            hovered && button.active,
            button.active && PackUiButtonFeedback.isPrimaryPointerDown(),
            mouseX - button.x,
            mouseY - button.y,
            button.width,
            button.height
        );
        float clickGlow = feedback.clickGlowProgress();

        int bg = THEME.overlayButtonFill(variant, button.active);
        int border = THEME.overlayButtonBorder(variant, button.active);
        int borderGlow = THEME.overlayButtonBorderGlow(variant);
        int textColor = THEME.overlayButtonTextColor(variant, button.active);
        int rippleRing = THEME.overlayButtonRippleRing(variant);
        int rippleFill = THEME.overlayButtonRippleFill(variant);
        float hoverVisual = Math.max(hover * 0.9f, clickGlow * 0.55f);
        border = PackUiSizing.lerpColor(border, borderGlow, Math.min(1.0f, (hover * 0.35f) + (clickGlow * 0.85f)));

        context.fill(button.x, button.y, button.x + button.width, button.y + button.height, bg);
        context.fill(button.x, button.y, button.x + button.width, button.y + 1, border);
        context.fill(button.x, button.y + button.height - 1, button.x + button.width, button.y + button.height, border);
        context.fill(button.x, button.y, button.x + 1, button.y + button.height, border);
        context.fill(button.x + button.width - 1, button.y, button.x + button.width, button.y + button.height, border);
        if (hoverVisual > 0.0f && button.active) {
            int hoverTint = switch (variant) {
                case SUCCESS -> (((int) Math.min(38.0f, (hover * 24.0f) + (clickGlow * 12.0f))) << 24) | 0x0032D86A;
                case DANGER -> (((int) Math.min(38.0f, (hover * 24.0f) + (clickGlow * 12.0f))) << 24) | 0x00FF3B3B;
                case PRIMARY -> (((int) Math.min(38.0f, (hover * 24.0f) + (clickGlow * 12.0f))) << 24) | 0x00FF5E5E;
                case FILTER_ON -> (((int) Math.min(42.0f, (hover * 26.0f) + (clickGlow * 14.0f))) << 24) | 0x004D78FF;
                case FILTER_OFF -> (((int) Math.min(28.0f, (hover * 18.0f) + (clickGlow * 10.0f))) << 24) | 0x00808A96;
                case SECONDARY, GHOST -> (((int) Math.min(30.0f, (hover * 18.0f) + (clickGlow * 10.0f))) << 24) | 0x00FF3B3B;
            };
            context.fill(button.x + 1, button.y + 1, button.x + button.width - 1, button.y + button.height - 1, hoverTint);
        }
        if (clickGlow > 0.0f && button.active) {
            int pressGlow = ((int) (clickGlow * 14.0f) << 24) | (rippleFill & 0x00FFFFFF);
            context.fill(button.x + 1, button.y + 1, button.x + button.width - 1, button.y + button.height - 1, pressGlow);
        }
        feedback.render(
            context,
            button.x + 1,
            button.y + 1,
            Math.max(0, button.width - 2),
            Math.max(0, button.height - 2),
            rippleRing,
            rippleFill,
            1.0f
        );

        int availableTextWidth = Math.max(1, button.width - (compactSymbol ? 2 : 6));
        String displayText = PackUiText.trimToWidth(textRenderer, label, availableTextWidth, font, textColor);
        int textWidth = PackUiText.width(textRenderer, displayText, font, textColor);
        int textX = button.x + Math.max(2, (button.width - textWidth) / 2);
        int textY = PackUiSizing.alignTextY(button.y, button.height, THEME.fontHeight(resolvedTone), compactSymbol ? 0 : THEME.buttonTextNudge());
        PackUiText.draw(context, textRenderer, displayText, font, textColor, textX, textY, false);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public Component getMessage() {
        return message;
    }

    public void setMessage(Component message) {
        this.message = message == null ? Component.empty() : message;
    }

    public Variant getVariant() {
        return variant;
    }

    public PackUiOverlayButton setVariant(Variant variant) {
        this.variant = variant == null ? Variant.SECONDARY : variant;
        return this;
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static PackUiButtonFeedback feedbackFor(PackUiOverlayButton button) {
        return PackUiButtonFeedback.forKey(
            "overlay:"
                + button.x + ':'
                + button.y + ':'
                + button.width + ':'
                + button.height + ':'
                + button.variant + ':'
                + button.tone + ':'
                + PackUtilText.sanitizeUiLabel(button.message == null ? "" : button.message.getString())
        );
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(PackUiOverlayButton button);
    }
}
