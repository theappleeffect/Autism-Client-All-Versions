package autismclient.gui.packui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class PackUiTextField extends PackUiNode implements PackUiFocusableTextInput {
    private static final int DEFAULT_MAX_LENGTH = 512;

    private String value = "";
    private String placeholder = "";
    private int maxLength = DEFAULT_MAX_LENGTH;
    private float minWidth = 56.0f;
    private float maxWidth = Float.MAX_VALUE;
    private float preferredWidth = -1.0f;
    private int horizontalPadding = 6;
    private int fixedHeight = -1;
    private int textYOffset = 1;
    private boolean editable = true;
    private boolean drawBackground = true;
    private boolean focused = false;
    private int cursor = 0;
    private int selectionAnchor = 0;
    private int viewOffset = 0;
    private int wrapViewLine = 0;
    private boolean manualWrapScroll = false;
    private boolean draggingSelection = false;
    private boolean multiline = false;
    private long blinkStartMs = System.currentTimeMillis();
    private Identifier fontId = null;
    private PackUiTone textTone = PackUiTone.BODY;
    private PackUiTone placeholderTone = PackUiTone.MUTED;
    private Predicate<String> filter = next -> true;
    private Consumer<String> onChange;
    private Consumer<String> onSubmit;
    private Function<String, Component> displayTextProvider;
    private boolean historyNavigationEnabled = false;
    private final List<String> historyEntries = new ArrayList<>();
    private int historyIndex = -1;
    private String historyDraft = "";
    private boolean applyingHistoryValue = false;

    private record WrappedLine(int start, int end, int renderEnd) {
    }

    private record RichMetrics(String text, List<Style> styles) {
    }

    public PackUiTextField() {
        this.height = 16;
    }

    public PackUiTextField setText(String value) {
        String sanitized = sanitize(value);
        if (!Objects.equals(this.value, sanitized)) {
            this.value = sanitized;
            if (cursor > this.value.length()) cursor = this.value.length();
            if (selectionAnchor > this.value.length()) selectionAnchor = this.value.length();
            manualWrapScroll = false;
            if (onChange != null) onChange.accept(this.value);
        }
        ensureSelectionOrder();
        viewOffset = Math.min(viewOffset, this.value.length());
        if (historyNavigationEnabled && !applyingHistoryValue) {
            historyIndex = -1;
            historyDraft = this.value;
        }
        return this;
    }

    public String text() {
        return value;
    }

    public PackUiTextField setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        return this;
    }

    public PackUiTextField setMaxLength(int maxLength) {
        this.maxLength = Math.max(1, maxLength);
        setText(value);
        return this;
    }

    public PackUiTextField setMinWidth(float minWidth) {
        this.minWidth = Math.max(0.0f, minWidth);
        return this;
    }

    public PackUiTextField setMaxWidth(float maxWidth) {
        this.maxWidth = Math.max(this.minWidth, maxWidth);
        return this;
    }

    public PackUiTextField setPreferredWidth(float preferredWidth) {
        this.preferredWidth = preferredWidth;
        return this;
    }

    public PackUiTextField setHorizontalPadding(int horizontalPadding) {
        this.horizontalPadding = Math.max(0, horizontalPadding);
        return this;
    }

    public PackUiTextField setFieldHeight(int fixedHeight) {
        this.fixedHeight = Math.max(1, fixedHeight);
        return this;
    }

    public PackUiTextField setTextYOffset(int textYOffset) {
        this.textYOffset = textYOffset;
        return this;
    }

    public PackUiTextField setEditable(boolean editable) {
        this.editable = editable;
        if (!editable) {
            setFocused(false);
        }
        return this;
    }

    public boolean isEditable() {
        return editable;
    }

    public PackUiTextField setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
        return this;
    }

    public PackUiTextField setFontId(Identifier fontId) {
        this.fontId = fontId;
        return this;
    }

    public PackUiTextField setTextTone(PackUiTone textTone) {
        this.textTone = textTone == null ? PackUiTone.BODY : textTone;
        return this;
    }

    public PackUiTextField setPlaceholderTone(PackUiTone placeholderTone) {
        this.placeholderTone = placeholderTone == null ? PackUiTone.MUTED : placeholderTone;
        return this;
    }

    public PackUiTextField setFilter(Predicate<String> filter) {
        this.filter = filter == null ? next -> true : filter;
        return this;
    }

    public PackUiTextField setOnChange(Consumer<String> onChange) {
        this.onChange = onChange;
        return this;
    }

    public PackUiTextField setOnSubmit(Consumer<String> onSubmit) {
        this.onSubmit = onSubmit;
        return this;
    }

    public PackUiTextField setDisplayTextProvider(Function<String, Component> displayTextProvider) {
        this.displayTextProvider = displayTextProvider;
        return this;
    }

    public PackUiTextField setMultiline(boolean multiline) {
        this.multiline = multiline;
        if (multiline) viewOffset = 0;
        else wrapViewLine = 0;
        return this;
    }

    public PackUiTextField setHistoryNavigationEnabled(boolean historyNavigationEnabled) {
        this.historyNavigationEnabled = historyNavigationEnabled;
        if (!historyNavigationEnabled) {
            historyIndex = -1;
            historyDraft = "";
        }
        return this;
    }

    public PackUiTextField addHistoryEntry(String entry) {
        String sanitized = sanitize(entry);
        if (sanitized.isEmpty()) return this;
        historyEntries.add(sanitized);
        historyIndex = -1;
        historyDraft = "";
        return this;
    }

    @Override
    public PackUiTextField setGrowX(boolean growX) {
        super.setGrowX(growX);
        return this;
    }

    @Override
    public float preferredWidth(PackUiRenderContext context) {
        if (preferredWidth > 0.0f) return PackUiSizing.clamp(preferredWidth, minWidth, maxWidth);
        String sample = value.isEmpty() ? placeholder : value;
        if (sample.isEmpty()) sample = "Component";
        Identifier font = resolvedFont(context);
        int color = context.theme().color(textTone);
        return PackUiSizing.fitTextWidth(context.textRenderer(), sample, font, color, context.theme().scale(horizontalPadding + 2), minWidth, maxWidth);
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        int resolvedFixedHeight = fixedHeight > 0 ? fixedHeight : Math.max(context.theme().buttonHeight(), context.theme().scale(16));
        int contentHeight = context.theme().fontHeight(textTone) + context.theme().scale(5);
        return Math.max(12, Math.max(resolvedFixedHeight, contentHeight));
    }

    @Override
    public void render(PackUiRenderContext context) {
        if (!visible) return;

        int drawX = Math.round(x);
        int drawY = Math.round(y);
        int drawW = Math.round(width);
        int drawH = Math.round(height);
        boolean hovered = contains(context.mouseX(), context.mouseY());
        float hover = updateHover(hovered, context.delta());
        Identifier font = resolvedFont(context);
        int fontHeight = context.theme().fontHeight(textTone);
        int scaledHorizontalPadding = context.theme().scale(horizontalPadding);
        int textYOffsetPx = context.theme().fieldTextNudge() + textYOffset - 1;
        int contentInset = context.theme().scale(3);
        int frameInset = context.theme().scale(2);
        int lineSpacing = context.theme().scale(1);
        int bodyColor = context.theme().color(textTone);
        int placeholderColor = context.theme().color(placeholderTone);
        int bg = focused ? 0xA30C0C0F : 0x7C0A090C;
        int border = !editable ? context.theme().color(PackUiTone.MUTED) : focused ? context.theme().headerAccent() : hover > 0.0f ? 0xFFCC4545 : context.theme().borderColor();
        int innerW = Math.max(1, drawW - (scaledHorizontalPadding * 2) - frameInset);
        RichMetrics richMetrics = value.isEmpty() ? null : buildRichMetrics(value);
        if (!multiline) ensureViewOffset(context.textRenderer(), font, bodyColor, innerW, richMetrics);

        if (drawBackground) {
            context.drawContext().fill(drawX, drawY, drawX + drawW, drawY + drawH, context.applyAlpha(bg));
        }
        if (editable && hover > 0.0f && !focused) {
            int hoverTint = ((int) (hover * 14) << 24) | 0x00FF2F2F;
            context.drawContext().fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawH - 1, context.applyAlpha(hoverTint));
        }
        context.drawContext().fill(drawX, drawY, drawX + drawW, drawY + 1, context.applyAlpha(border));
        context.drawContext().fill(drawX, drawY + drawH - 1, drawX + drawW, drawY + drawH, context.applyAlpha(border));
        context.drawContext().fill(drawX, drawY, drawX + 1, drawY + drawH, context.applyAlpha(border));
        context.drawContext().fill(drawX + drawW - 1, drawY, drawX + drawW, drawY + drawH, context.applyAlpha(border));
        if (!editable) {
            context.drawContext().fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawH - 1, context.applyAlpha(0x66141518));
        } else if (focused) {
            context.drawContext().fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawH - 1, context.applyAlpha(0x12FF3B3B));
        }

        int textX = drawX + scaledHorizontalPadding;

        if (value.isEmpty()) {
            if (multiline) {
                int textY = drawY + contentInset + textYOffsetPx;
                context.viewport().enableScissor(context.drawContext(), drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawH - 1);
                try {
                    for (String line : wrapPlainText(context.textRenderer(), font, placeholderColor, placeholder, innerW)) {
                        if (textY + fontHeight > drawY + drawH - frameInset) break;
                        PackUiText.draw(context.drawContext(), context.textRenderer(), line, font, context.applyAlpha(placeholderColor), textX, textY, false);
                        textY += fontHeight + lineSpacing;
                    }
                } finally {
                    context.viewport().disableScissor(context.drawContext());
                }
                if (focused && blinkVisible()) {
                    context.drawContext().fill(textX, drawY + contentInset, textX + 1, drawY + contentInset + fontHeight, context.applyAlpha(0xFFFFEDED));
                }
            } else {
                int textY = PackUiSizing.alignTextY(drawY, drawH, fontHeight, textYOffsetPx);
                String displayPlaceholder = PackUiText.trimToWidth(context.textRenderer(), placeholder, innerW, font, placeholderColor);
                PackUiText.draw(context.drawContext(), context.textRenderer(), displayPlaceholder, font, context.applyAlpha(placeholderColor), textX, textY, false);
                if (focused && blinkVisible()) {
                    context.drawContext().fill(textX, drawY + contentInset, textX + 1, drawY + drawH - contentInset, context.applyAlpha(0xFFFFEDED));
                }
            }
        } else {
            if (multiline) {
                List<WrappedLine> wrappedLines = wrapValueLines(context.textRenderer(), font, bodyColor, innerW, richMetrics);
                int lineHeight = fontHeight + lineSpacing;
                int visibleLineCount = Math.max(1, Math.max(1, drawH - context.theme().scale(6)) / lineHeight);
                ensureWrapViewLine(visibleLineCount, wrappedLines);
                int selectionStart = Math.min(cursor, selectionAnchor);
                int selectionEnd = Math.max(cursor, selectionAnchor);
                int startLine = Math.min(wrapViewLine, Math.max(0, wrappedLines.size() - 1));
                int endLine = Math.min(wrappedLines.size(), startLine + visibleLineCount);

                context.viewport().enableScissor(context.drawContext(), drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawH - 1);
                try {
                    int lineY = drawY + contentInset + textYOffsetPx;
                    for (int lineIndex = startLine; lineIndex < endLine; lineIndex++) {
                        WrappedLine line = wrappedLines.get(lineIndex);
                        if (selectionStart != selectionEnd) {
                            int visibleSelectionStart = Math.max(selectionStart, line.start());
                            int visibleSelectionEnd = Math.min(selectionEnd, line.end());
                            if (visibleSelectionStart < visibleSelectionEnd) {
                                int selectionX = textX + renderSubstringWidth(context.textRenderer(), font, bodyColor,
                                        value, richMetrics, line.start(), visibleSelectionStart);
                                int selectionW = renderSubstringWidth(context.textRenderer(), font, bodyColor,
                                        value, richMetrics, visibleSelectionStart, visibleSelectionEnd);
                                context.drawContext().fill(selectionX, lineY - 1, selectionX + selectionW,
                                lineY + fontHeight + lineSpacing, context.applyAlpha(0x66FF4A4A));
                            }
                        }

                        if (richMetrics != null) {
                            Component lineComponent = buildRichSubstringText(value, richMetrics, line.start(), line.end());
                            context.drawContext().drawString(context.textRenderer(), lineComponent,
                                    textX, lineY, context.applyAlpha(bodyColor), false);
                        } else {
                            String lineComponent = value.substring(line.start(), line.end());
                            PackUiText.draw(context.drawContext(), context.textRenderer(), lineComponent, font,
                                    context.applyAlpha(bodyColor), textX, lineY, false);
                        }
                        lineY += lineHeight;
                    }
                } finally {
                    context.viewport().disableScissor(context.drawContext());
                }

                if (focused && blinkVisible()) {
                    int cursorLine = lineIndexForCursor(wrappedLines, cursor);
                    if (cursorLine >= startLine && cursorLine < endLine) {
                        WrappedLine line = wrappedLines.get(cursorLine);
                        int visibleCursor = Math.min(cursor, line.end());
                        int caretX = textX + renderSubstringWidth(context.textRenderer(), font, bodyColor,
                                value, richMetrics, line.start(), visibleCursor);
                        int caretY = drawY + contentInset + textYOffsetPx + ((cursorLine - startLine) * lineHeight);
                        context.drawContext().fill(caretX, caretY - 1, caretX + 1,
                                caretY + fontHeight + lineSpacing, context.applyAlpha(0xFFFFEDED));
                    }
                }
            } else {
                int textY = PackUiSizing.alignTextY(drawY, drawH, fontHeight, textYOffsetPx);
                int visibleEnd = visibleEndIndex(context.textRenderer(), font, bodyColor, innerW, richMetrics);
                int selectionStart = Math.min(cursor, selectionAnchor);
                int selectionEnd = Math.max(cursor, selectionAnchor);

                if (selectionStart != selectionEnd) {
                    int visibleSelectionStart = Math.max(selectionStart, viewOffset);
                    int visibleSelectionEnd = Math.min(selectionEnd, visibleEnd);
                    if (visibleSelectionStart < visibleSelectionEnd) {
                        int selectionX = textX + renderSubstringWidth(context.textRenderer(), font, bodyColor, value, richMetrics, viewOffset, visibleSelectionStart);
                        int selectionW = renderSubstringWidth(context.textRenderer(), font, bodyColor, value, richMetrics, visibleSelectionStart, visibleSelectionEnd);
                        context.drawContext().fill(selectionX, drawY + 2, selectionX + selectionW, drawY + drawH - 2, context.applyAlpha(0x66FF4A4A));
                    }
                }

                if (richMetrics != null) {
                    Component visibleRichComponent = buildRichSubstringText(value, richMetrics, viewOffset, visibleEnd);
                    context.viewport().enableScissor(context.drawContext(), drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawH - 1);
                    try {
                        context.drawContext().drawString(context.textRenderer(), visibleRichComponent, textX, textY, context.applyAlpha(bodyColor), false);
                    } finally {
                        context.viewport().disableScissor(context.drawContext());
                    }
                } else {
                    context.viewport().enableScissor(context.drawContext(), drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawH - 1);
                    try {
                        PackUiText.draw(context.drawContext(), context.textRenderer(), value.substring(viewOffset, visibleEnd), font, context.applyAlpha(bodyColor), textX, textY, false);
                    } finally {
                        context.viewport().disableScissor(context.drawContext());
                    }
                }

                if (focused && blinkVisible()) {
                    int caretX = textX + renderSubstringWidth(context.textRenderer(), font, bodyColor, value, richMetrics, viewOffset, cursor);
                    context.drawContext().fill(caretX, drawY + contentInset, caretX + 1, drawY + drawH - contentInset, context.applyAlpha(0xFFFFEDED));
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        if (!editable || button != 0 || !contains(mouseX, mouseY)) return false;
        focused = true;
        draggingSelection = true;
        int innerW = Math.max(1, Math.round(width) - (context.theme().scale(horizontalPadding) * 2) - context.theme().scale(2));
        Identifier font = resolvedFont(context);
        int bodyColor = context.theme().color(textTone);
        RichMetrics richMetrics = buildRichMetrics(value);
        int clickCursor;
        if (multiline) {
            List<WrappedLine> wrappedLines = wrapValueLines(context.textRenderer(), font, bodyColor, innerW, richMetrics);
            int lineHeight = context.theme().fontHeight(textTone) + context.theme().scale(1);
            int visibleLineCount = Math.max(1, Math.max(1, Math.round(height) - context.theme().scale(6)) / lineHeight);
            ensureWrapViewLine(visibleLineCount, wrappedLines);
            clickCursor = cursorFromMousePosition(context.textRenderer(), font, bodyColor,
                    mouseX - (x + context.theme().scale(horizontalPadding)), mouseY - (y + context.theme().scale(3 + textYOffset)), wrappedLines, richMetrics);
        } else {
            ensureViewOffset(context.textRenderer(), font, bodyColor, innerW, richMetrics);
            clickCursor = cursorFromMouseX(context.textRenderer(), font, bodyColor, mouseX - (x + context.theme().scale(horizontalPadding)), richMetrics);
        }
        cursor = clickCursor;
        selectionAnchor = clickCursor;
        manualWrapScroll = false;
        restartBlink();
        return true;
    }

    @Override
    public boolean mouseReleased(PackUiRenderContext context, float mouseX, float mouseY, int button) {
        if (button == 0 && draggingSelection) {
            draggingSelection = false;
            return focused;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(PackUiRenderContext context, float mouseX, float mouseY, int button, float deltaX, float deltaY) {
        if (!editable || !focused || !draggingSelection || button != 0) return false;
        Identifier font = resolvedFont(context);
        int bodyColor = context.theme().color(textTone);
        int innerW = Math.max(1, Math.round(width) - (context.theme().scale(horizontalPadding) * 2) - context.theme().scale(2));
        RichMetrics richMetrics = buildRichMetrics(value);
        if (multiline) {
            List<WrappedLine> wrappedLines = wrapValueLines(context.textRenderer(), font, bodyColor, innerW, richMetrics);
            int lineHeight = context.theme().fontHeight(textTone) + context.theme().scale(1);
            int visibleLineCount = Math.max(1, Math.max(1, Math.round(height) - context.theme().scale(6)) / lineHeight);
            ensureWrapViewLine(visibleLineCount, wrappedLines);
            cursor = cursorFromMousePosition(context.textRenderer(), font, bodyColor,
                    mouseX - (x + context.theme().scale(horizontalPadding)), mouseY - (y + context.theme().scale(3 + textYOffset)), wrappedLines, richMetrics);
        } else {
            ensureViewOffset(context.textRenderer(), font, bodyColor, innerW, richMetrics);
            cursor = cursorFromMouseX(context.textRenderer(), font, bodyColor, mouseX - (x + context.theme().scale(horizontalPadding)), richMetrics);
        }
        manualWrapScroll = false;
        restartBlink();
        return true;
    }

    @Override
    public boolean mouseScrolled(PackUiRenderContext context, float mouseX, float mouseY, float amount) {
        if (!multiline || !contains(mouseX, mouseY)) return false;
        Identifier font = resolvedFont(context);
        int bodyColor = context.theme().color(textTone);
        int innerW = Math.max(1, Math.round(width) - (context.theme().scale(horizontalPadding) * 2) - context.theme().scale(2));
        RichMetrics richMetrics = buildRichMetrics(value);
        List<WrappedLine> wrappedLines = wrapValueLines(context.textRenderer(), font, bodyColor, innerW, richMetrics);
        int lineHeight = context.theme().fontHeight(textTone) + context.theme().scale(1);
        int visibleLineCount = Math.max(1, Math.max(1, Math.round(height) - context.theme().scale(6)) / lineHeight);
        int maxStart = Math.max(0, wrappedLines.size() - visibleLineCount);
        if (maxStart <= 0) return false;

        int step = Math.max(1, Math.round(Math.abs(amount) * 3.0f));
        int next = wrapViewLine + (amount < 0.0f ? step : -step);
        next = Math.max(0, Math.min(maxStart, next));
        if (next == wrapViewLine) return false;
        wrapViewLine = next;
        manualWrapScroll = true;
        return true;
    }

    @Override
    public boolean keyPressed(PackUiRenderContext context, int keyCode, int scanCode, int modifiers) {
        if (!focused || !editable) return false;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                setFocused(false);
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (multiline) {
                    replaceSelection("\n");
                    return true;
                }
                if (onSubmit != null) onSubmit.accept(value);
                return true;
            }
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) {
                    selectionAnchor = 0;
                    cursor = value.length();
                    manualWrapScroll = false;
                    restartBlink();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_C -> {
                if (ctrl) {
                    copySelection();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_X -> {
                if (ctrl) {
                    copySelection();
                    deleteSelection();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) {
                    pasteClipboard();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_LEFT -> {
                moveCursor(-1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveCursor(1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                if (!multiline && historyNavigationEnabled) {
                    navigateHistory(-1);
                    return true;
                }
                if (multiline) {
                    moveCursorVertically(context, -1, shift);
                    return true;
                }
            }
            case GLFW.GLFW_KEY_DOWN -> {
                if (!multiline && historyNavigationEnabled) {
                    navigateHistory(1);
                    return true;
                }
                if (multiline) {
                    moveCursorVertically(context, 1, shift);
                    return true;
                }
            }
            case GLFW.GLFW_KEY_HOME -> {
                setCursor(0, shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                setCursor(value.length(), shift);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (deleteSelection()) return true;
                if (cursor > 0) replaceRange(cursor - 1, cursor, "");
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (deleteSelection()) return true;
                if (cursor < value.length()) replaceRange(cursor, cursor + 1, "");
                return true;
            }
            default -> {
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(PackUiRenderContext context, char chr, int modifiers) {
        if (!focused || !editable || !isAllowedCharacter(chr)) return false;
        replaceSelection(Character.toString(chr));
        return true;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused && editable;
        this.draggingSelection = false;
        restartBlink();
    }

    public PackUiTextField setSelectionEnd(int selectionEnd) {
        cursor = Math.max(0, Math.min(value.length(), selectionEnd));
        manualWrapScroll = false;
        restartBlink();
        return this;
    }

    private Identifier resolvedFont(PackUiRenderContext context) {
        return fontId != null ? fontId : context.theme().fontFor(PackUiTone.BODY);
    }

    private boolean blinkVisible() {
        return ((System.currentTimeMillis() - blinkStartMs) / 530L) % 2L == 0L;
    }

    private void restartBlink() {
        blinkStartMs = System.currentTimeMillis();
    }

    private void ensureSelectionOrder() {
        cursor = Math.max(0, Math.min(cursor, value.length()));
        selectionAnchor = Math.max(0, Math.min(selectionAnchor, value.length()));
    }

    private int visibleEndIndex(Font renderer, Identifier font, int color, int availableWidth, RichMetrics richMetrics) {
        int end = viewOffset;
        while (end < value.length()) {
            if (renderSubstringWidth(renderer, font, color, value, richMetrics, viewOffset, end + 1) > availableWidth) break;
            end++;
        }
        return end;
    }

    private void ensureViewOffset(Font renderer, Identifier font, int color, int availableWidth, RichMetrics richMetrics) {
        ensureSelectionOrder();
        if (cursor < viewOffset) viewOffset = cursor;
        while (renderSubstringWidth(renderer, font, color, value, richMetrics, viewOffset, cursor) > availableWidth && viewOffset < cursor) {
            viewOffset++;
        }
        while (viewOffset > 0) {
            if (renderSubstringWidth(renderer, font, color, value, richMetrics, viewOffset - 1, cursor) > availableWidth) break;
            viewOffset--;
        }
        viewOffset = Math.max(0, Math.min(viewOffset, value.length()));
    }

    private int cursorFromMouseX(Font renderer, Identifier font, int color, float mouseInnerX, RichMetrics richMetrics) {
        float clampedX = Math.max(0.0f, mouseInnerX);
        int best = viewOffset;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = viewOffset; i <= value.length(); i++) {
            int charX = renderSubstringWidth(renderer, font, color, value, richMetrics, viewOffset, i);
            int distance = Math.abs(Math.round(clampedX) - charX);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = i;
            } else if (charX > clampedX) {
                break;
            }
        }
        return best;
    }

    private List<String> wrapPlainText(Font renderer, Identifier font, int color, String text, int availableWidth) {
        if (text == null || text.isEmpty()) return List.of("");
        List<String> wrapped = new ArrayList<>();
        for (WrappedLine line : wrapLinesForText(renderer, font, color, text, availableWidth, null)) {
            wrapped.add(text.substring(line.start(), line.renderEnd()));
        }
        return wrapped.isEmpty() ? List.of("") : wrapped;
    }

    private RichMetrics buildRichMetrics(String sourceValue) {
        if (displayTextProvider == null || sourceValue == null || sourceValue.isEmpty()) return null;
        Component richDisplay = displayTextProvider.apply(sourceValue);
        if (richDisplay == null) return null;
        String richComponent = richDisplay.getString();
        if (!Objects.equals(richComponent, sourceValue)) return null;

        List<Style> rawStyles = new ArrayList<>(richComponent.length());
        richDisplay.visit((style, part) -> {
            if (part != null && !part.isEmpty()) {
                Style safeStyle = style == null ? Style.EMPTY : style;
                for (int i = 0; i < part.length(); i++) rawStyles.add(safeStyle);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);

        List<Style> charStyles = new ArrayList<>(rawStyles);
        if (charStyles.isEmpty()) return null;
        while (charStyles.size() < richComponent.length()) charStyles.add(Style.EMPTY);
        if (charStyles.size() > richComponent.length()) {
            charStyles = new ArrayList<>(charStyles.subList(0, richComponent.length()));
        }
        return new RichMetrics(richComponent, charStyles);
    }

    private Component buildRichSubstringText(String sourceText, RichMetrics richMetrics, int start, int end) {
        int safeStart = Math.max(0, Math.min(start, sourceText.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, sourceText.length()));
        if (richMetrics == null || safeStart >= safeEnd) {
            return Component.literal(sourceText.substring(safeStart, safeEnd));
        }

        MutableComponent out = Component.empty();
        StringBuilder segment = new StringBuilder();
        Style currentStyle = null;
        for (int i = safeStart; i < safeEnd; i++) {
            Style style = richMetrics.styles().get(Math.min(i, richMetrics.styles().size() - 1));
            if (currentStyle == null) {
                currentStyle = style;
            } else if (!currentStyle.equals(style)) {
                out.append(Component.literal(segment.toString()).setStyle(currentStyle));
                segment.setLength(0);
                currentStyle = style;
            }
            segment.append(sourceText.charAt(i));
        }
        if (!segment.isEmpty()) {
            out.append(Component.literal(segment.toString()).setStyle(currentStyle == null ? Style.EMPTY : currentStyle));
        }
        return out;
    }

    private int renderSubstringWidth(Font renderer, Identifier font, int color, String sourceText,
                                     RichMetrics richMetrics, int start, int end) {
        int safeStart = Math.max(0, Math.min(start, sourceText.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, sourceText.length()));
        if (safeStart >= safeEnd) return 0;
        if (richMetrics == null) return substringWidth(renderer, font, color, sourceText.substring(safeStart, safeEnd));
        return renderer.width(buildRichSubstringText(sourceText, richMetrics, safeStart, safeEnd));
    }

    private List<WrappedLine> wrapValueLines(Font renderer, Identifier font, int color, int availableWidth, RichMetrics richMetrics) {
        return wrapLinesForText(renderer, font, color, value, availableWidth, richMetrics);
    }

    private List<WrappedLine> wrapLinesForText(Font renderer, Identifier font, int color, String text, int availableWidth, RichMetrics richMetrics) {
        List<WrappedLine> lines = new ArrayList<>();
        String safeComponent = text == null ? "" : text;
        if (safeComponent.isEmpty()) {
            lines.add(new WrappedLine(0, 0, 0));
            return lines;
        }

        int start = 0;
        while (start < safeComponent.length()) {

            int newlineIdx = safeComponent.indexOf('\n', start);
            if (newlineIdx == start) {

                lines.add(new WrappedLine(start, start + 1, start));
                start = start + 1;
                continue;
            }

            int segmentEnd = (newlineIdx >= 0) ? newlineIdx : safeComponent.length();
            boolean hasTrailingNewline = (newlineIdx >= 0);

            int segStart = start;
            while (segStart < segmentEnd) {
                int end = segStart;
                int lastBreak = -1;
                while (end < segmentEnd) {
                    char ch = safeComponent.charAt(end);
                    if (ch == ' ' || ch == '\t') lastBreak = end + 1;
                    if (renderSubstringWidth(renderer, font, color, safeComponent, richMetrics, segStart, end + 1) > availableWidth) {
                        if (end == segStart) {
                            end++;
                        } else if (lastBreak > segStart) {
                            end = lastBreak;
                        }
                        break;
                    }
                    end++;
                }
                if (end > segmentEnd) end = segmentEnd;
                if (end <= segStart) end = Math.min(segStart + 1, segmentEnd);

                int renderEnd = end;
                while (renderEnd > segStart && (safeComponent.charAt(renderEnd - 1) == ' ' || safeComponent.charAt(renderEnd - 1) == '\t')) renderEnd--;
                if (renderEnd < segStart) renderEnd = segStart;

                int lineEnd = end;
                if (end == segmentEnd && hasTrailingNewline) {
                    lineEnd = segmentEnd + 1;
                }

                lines.add(new WrappedLine(segStart, lineEnd, renderEnd));
                segStart = lineEnd;
            }

            start = segStart;

            if (hasTrailingNewline && start <= newlineIdx) {
                start = newlineIdx + 1;
            }
        }

        return lines.isEmpty() ? List.of(new WrappedLine(0, 0, 0)) : lines;
    }

    private void ensureWrapViewLine(int visibleLineCount, List<WrappedLine> wrappedLines) {
        if (wrappedLines.isEmpty()) {
            wrapViewLine = 0;
            return;
        }
        int cursorLine = lineIndexForCursor(wrappedLines, cursor);
        int maxStart = Math.max(0, wrappedLines.size() - visibleLineCount);
        if (manualWrapScroll) {
            wrapViewLine = Math.max(0, Math.min(wrapViewLine, maxStart));
            return;
        }
        if (cursorLine < wrapViewLine) wrapViewLine = cursorLine;
        if (cursorLine >= wrapViewLine + visibleLineCount) wrapViewLine = cursorLine - visibleLineCount + 1;
        wrapViewLine = Math.max(0, Math.min(wrapViewLine, maxStart));
    }

    private int lineIndexForCursor(List<WrappedLine> wrappedLines, int index) {
        if (wrappedLines.isEmpty()) return 0;
        int clamped = Math.max(0, Math.min(index, value.length()));
        for (int i = 0; i < wrappedLines.size(); i++) {
            WrappedLine line = wrappedLines.get(i);
            if (clamped < line.end()) return i;
            if (clamped == line.end()) {
                if (i == wrappedLines.size() - 1) return i;
                if (clamped == line.start()) return i;
            }
        }
        return wrappedLines.size() - 1;
    }

    private int cursorFromMousePosition(Font renderer, Identifier font, int color,
                                        float mouseInnerX, float mouseInnerY, List<WrappedLine> wrappedLines, RichMetrics richMetrics) {
        if (wrappedLines.isEmpty()) return 0;
        PackUiTheme theme = new PackUiTheme();
        int lineHeight = theme.lineHeight(textTone, 1);
        int lineIndex = wrapViewLine + Math.max(0, Math.round((mouseInnerY - 1.0f) / lineHeight));
        lineIndex = Math.max(0, Math.min(lineIndex, wrappedLines.size() - 1));
        WrappedLine line = wrappedLines.get(lineIndex);
        return cursorFromMouseXForLine(renderer, font, color, mouseInnerX, line, richMetrics);
    }

    private int cursorFromMouseXForLine(Font renderer, Identifier font, int color, float mouseInnerX, WrappedLine line, RichMetrics richMetrics) {
        float clampedX = Math.max(0.0f, mouseInnerX);
        int best = line.start();
        int bestDistance = Integer.MAX_VALUE;
        int visibleEnd = Math.max(line.start(), line.end());
        for (int i = line.start(); i <= visibleEnd; i++) {
            int charX = renderSubstringWidth(renderer, font, color, value, richMetrics, line.start(), i);
            int distance = Math.abs(Math.round(clampedX) - charX);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = i;
            } else if (charX > clampedX) {
                break;
            }
        }
        return Math.max(line.start(), Math.min(best, line.end()));
    }

    private void moveCursorVertically(PackUiRenderContext context, int direction, boolean keepSelection) {
        Identifier font = resolvedFont(context);
        int bodyColor = context.theme().color(textTone);
        int innerW = Math.max(1, Math.round(width) - (context.theme().scale(horizontalPadding) * 2) - context.theme().scale(2));
        RichMetrics richMetrics = buildRichMetrics(value);
        List<WrappedLine> wrappedLines = wrapValueLines(context.textRenderer(), font, bodyColor, innerW, richMetrics);
        if (wrappedLines.isEmpty()) return;
        int currentLineIndex = lineIndexForCursor(wrappedLines, cursor);
        int targetLineIndex = Math.max(0, Math.min(wrappedLines.size() - 1, currentLineIndex + direction));
        if (targetLineIndex == currentLineIndex) return;

        WrappedLine currentLine = wrappedLines.get(currentLineIndex);
        int visibleCursor = Math.min(cursor, currentLine.renderEnd());
        int cursorX = renderSubstringWidth(context.textRenderer(), font, bodyColor, value, richMetrics, currentLine.start(), visibleCursor);
        WrappedLine targetLine = wrappedLines.get(targetLineIndex);
        int targetCursor = cursorFromMouseXForLine(context.textRenderer(), font, bodyColor, cursorX, targetLine, richMetrics);
        setCursor(targetCursor, keepSelection);
    }

    private void moveCursor(int delta, boolean keepSelection) {
        setCursor(Math.max(0, Math.min(value.length(), cursor + delta)), keepSelection);
    }

    private void navigateHistory(int direction) {
        if (historyEntries.isEmpty()) return;
        if (direction < 0) {
            if (historyIndex == -1) {
                historyDraft = value;
                historyIndex = historyEntries.size() - 1;
            } else if (historyIndex > 0) {
                historyIndex--;
            }
            applyHistoryValue(historyEntries.get(historyIndex));
        } else {
            if (historyIndex == -1) return;
            if (historyIndex < historyEntries.size() - 1) {
                historyIndex++;
                applyHistoryValue(historyEntries.get(historyIndex));
            } else {
                historyIndex = -1;
                applyHistoryValue(historyDraft);
            }
        }
    }

    private void applyHistoryValue(String nextValue) {
        applyingHistoryValue = true;
        try {
            setText(nextValue);
        } finally {
            applyingHistoryValue = false;
        }
        setCursor(value.length(), false);
    }

    private void setCursor(int nextCursor, boolean keepSelection) {
        cursor = Math.max(0, Math.min(value.length(), nextCursor));
        if (!keepSelection) selectionAnchor = cursor;
        manualWrapScroll = false;
        restartBlink();
    }

    private boolean hasSelection() {
        return cursor != selectionAnchor;
    }

    private void copySelection() {
        if (!hasSelection()) return;
        int start = Math.min(cursor, selectionAnchor);
        int end = Math.max(cursor, selectionAnchor);
        Minecraft.getInstance().keyboardHandler.setClipboard(value.substring(start, end));
    }

    private void pasteClipboard() {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isEmpty()) return;
        replaceSelection(sanitize(clipboard));
    }

    private boolean deleteSelection() {
        if (!hasSelection()) return false;
        int start = Math.min(cursor, selectionAnchor);
        int end = Math.max(cursor, selectionAnchor);
        replaceRange(start, end, "");
        return true;
    }

    private void replaceSelection(String replacement) {
        int start = Math.min(cursor, selectionAnchor);
        int end = Math.max(cursor, selectionAnchor);
        replaceRange(start, end, replacement);
    }

    private void replaceRange(int start, int end, String replacement) {
        int safeStart = Math.max(0, Math.min(start, value.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, value.length()));
        String sanitizedReplacement = sanitize(replacement);
        String next = value.substring(0, safeStart) + sanitizedReplacement + value.substring(safeEnd);
        if (next.length() > maxLength) {
            next = next.substring(0, maxLength);
        }
        if (!filter.test(next)) return;
        value = next;
        cursor = Math.min(next.length(), safeStart + sanitizedReplacement.length());
        selectionAnchor = cursor;
        manualWrapScroll = false;
        restartBlink();
        if (historyNavigationEnabled && !applyingHistoryValue) {
            historyIndex = -1;
            historyDraft = value;
        }
        if (onChange != null) onChange.accept(value);
    }

    private String sanitize(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length() && sb.length() < maxLength; i++) {
            char chr = input.charAt(i);
            if (!isAllowedCharacter(chr)) continue;
            sb.append(chr);
        }
        String next = sb.toString();
        return filter.test(next) ? next : value;
    }

    private boolean isAllowedCharacter(char chr) {
        if (chr == '\n') return multiline;
        if (chr == '\r') return false;
        return chr >= 32 && chr != 127;
    }

    private int substringWidth(Font renderer, Identifier font, int color, String text) {
        return PackUiText.width(renderer, text, font, color);
    }
}
