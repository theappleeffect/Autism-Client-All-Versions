package autismclient.util;

import autismclient.AutismClientAddon;
import autismclient.gui.packui.PackUiRenderContext;
import autismclient.gui.packui.PackUiTextField;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiViewport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class PackUtilChatField {
    private static final PackUiTheme THEME = new PackUiTheme();

    private final Minecraft mc;
    private final Font textRenderer;
    private final PackUiTextField field = new PackUiTextField()
        .setHorizontalPadding(3)
        .setTextYOffset(1);

    private int x;
    private int y;
    private int width;
    private int height;
    private boolean submitOnEnter;
    private Predicate<String> submitHandler;
    private String placeholderText = "";

    public PackUtilChatField(Minecraft mc, Font textRenderer, int x, int y, int width, int height, boolean submitOnEnter) {
        this.mc = mc;
        this.textRenderer = textRenderer;
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.submitOnEnter = submitOnEnter;
        syncBounds();
    }

    public void setSubmitOnEnter(boolean submitOnEnter) {
        this.submitOnEnter = submitOnEnter;
    }

    public void setSubmitHandler(Predicate<String> submitHandler) {
        this.submitHandler = submitHandler;
    }

    public void setChangedListener(Consumer<String> changedListener) {
        field.setOnChange(changedListener);
    }

    public void setDisplayTextProvider(Function<String, Component> displayTextProvider) {
        field.setDisplayTextProvider(displayTextProvider);
    }

    public void setMultiline(boolean multiline) {
        field.setMultiline(multiline);
    }

    public void setHistoryNavigationEnabled(boolean historyNavigationEnabled) {
        field.setHistoryNavigationEnabled(historyNavigationEnabled);
    }

    public void setPlaceholder(Component placeholder) {
        placeholderText = placeholder == null ? "" : PackUtilText.sanitizeUiLabel(placeholder.getString());
        field.setPlaceholder(placeholderText);
    }

    public String getPlaceholderText() {
        return placeholderText;
    }

    public void setDrawsBackground(boolean drawsBackground) {
        field.setDrawBackground(drawsBackground);
    }

    public void setEditable(boolean editable) {
        field.setEditable(editable);
    }

    public void setFilter(Predicate<String> filter) {
        field.setFilter(filter);
    }

    public void setNumericOnly(boolean allowNegative) {
        field.setFilter(next -> {
            if (next == null || next.isEmpty()) return true;
            for (int i = 0; i < next.length(); i++) {
                char chr = next.charAt(i);
                if (allowNegative && chr == '-' && i == 0) continue;
                if (!Character.isDigit(chr)) return false;
            }
            return true;
        });
    }

    public boolean isFocused() {
        return field.isFocused();
    }

    public void setFocused(boolean focused) {
        field.setFocused(focused);
    }

    public void setMaxLength(int maxLength) {
        field.setMaxLength(maxLength);
    }

    public void setText(String text) {
        field.setText(text);
    }

    public String getText() {
        return field.text();
    }

    public void write(String content) {
        if (content == null || content.isEmpty() || !field.isFocused() || !field.isEditable()) return;
        PackUiRenderContext context = inputContext();
        for (int i = 0; i < content.length(); i++) {
            field.charTyped(context, content.charAt(i), 0);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        syncBounds();
        if (!contains(mouseX, mouseY)) {
            field.setFocused(false);
            return false;
        }
        return field.mouseClicked(inputContext(), (float) mouseX, (float) mouseY, button);
    }

    public boolean mouseClicked(MouseButtonEvent click, boolean ignored) {
        return mouseClicked(click.x(), click.y(), 0);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        syncBounds();
        if (!contains(mouseX, mouseY)) return false;
        return field.mouseScrolled(inputContext(), (float) mouseX, (float) mouseY, (float) amount);
    }

    public boolean keyPressed(KeyEvent keyInput) {
        if (!field.isFocused()) return false;

        int key = keyInput.key();
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (submitOnEnter) {
                return handleSubmit();
            }
            field.setFocused(false);
            return true;
        }

        return field.keyPressed(inputContext(), key, keyInput.scancode(), keyInput.modifiers());
    }

    public boolean charTyped(CharacterEvent charInput) {
        return field.isFocused() && field.charTyped(inputContext(), (char) charInput.codepoint(), 0);
    }

    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        syncBounds();
        field.render(renderContext(context, mouseX, mouseY, delta));
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
        syncBounds();
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
        syncBounds();
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = Math.max(1, width);
        syncBounds();
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = Math.max(1, height);
        syncBounds();
    }

    public void setSelectionEnd(int selectionEnd) {
        field.setSelectionEnd(selectionEnd);
    }

    private void syncBounds() {
        field.setBounds(x, y, width, height);
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean handleSubmit() {
        String value = field.text().trim();
        if (value.isEmpty()) return true;

        boolean submitted = submitHandler != null ? submitHandler.test(value) : sendChat(value);
        if (!submitted) return true;

        field.addHistoryEntry(value);
        mc.commandHistory().addCommand(value);
        field.setText("");
        field.setFocused(false);
        return true;
    }

    private boolean sendChat(String message) {
        if (mc.getConnection() == null) {
            AutismClientAddon.LOG.warn("Failed to send PackUtil chat message because network handler was null.");
            return false;
        }

        if (message.startsWith("/") && message.length() > 1) {
            mc.getConnection().sendCommand(message.substring(1));
        } else {
            mc.getConnection().sendChat(message);
        }
        return true;
    }

    private PackUiRenderContext inputContext() {
        return new PackUiRenderContext(null, textRenderer, PackUiViewport.current(PackUiTheme.DEFAULT_DENSITY), THEME, 0, 0, 0);
    }

    private PackUiRenderContext renderContext(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        return new PackUiRenderContext(
            drawContext,
            textRenderer,
            PackUiViewport.current(PackUiTheme.DEFAULT_DENSITY),
            THEME,
            mouseX,
            mouseY,
            delta
        );
    }
}
