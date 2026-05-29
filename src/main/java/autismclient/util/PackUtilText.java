package autismclient.util;

import autismclient.gui.packui.PackUiAssets;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
//? if >=1.21.9 {
import net.minecraft.network.chat.FontDescription;
//?}
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PackUtilText {
    //? if >=1.21.9 {
    private static final Map<Identifier, FontDescription> FONT_CACHE = new ConcurrentHashMap<>();
    //?} else {
    /*private static final Map<Identifier, Identifier> FONT_CACHE = new ConcurrentHashMap<>();
    *///?}

    private PackUtilText() {
    }

    public enum Tone {
        TITLE,
        LABEL,
        BODY,
        MUTED,
        ACCENT
    }

    public static int colorFor(Tone tone) {
        return switch (tone) {
            case TITLE, LABEL, BODY -> PackUtilColors.textPrimary();
            case MUTED -> PackUtilColors.textMuted();
            case ACCENT -> PackUtilColors.accent();
        };
    }

    public static MutableComponent literal(String value, Tone tone) {
        return Component.literal(value == null ? "" : value).setStyle(styleFor(tone));
    }

    public static MutableComponent literal(String value, int color) {
        Identifier fontId = fontIdFor(Tone.BODY);
        return Component.literal(value == null ? "" : value).setStyle(Style.EMPTY.withFont(fontSource(fontId)).withColor(color));
    }

    public static String sanitizeUiLabel(String value) {
        if (value == null || value.isEmpty()) return "";
        return value
            .replace("ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“", "X")
            .replace("Ã¢Å“â€œ", "X")
            .replace("âœ“", "X")
            .replace("âœ”", "X")
            .replace("\u2713", "X")
            .replace("Ãƒâ€”", "X")
            .replace("Ã—", "X")
            .replace("âœ•", "X")
            .replace("âœ–", "X")
            .replace("\u2715", "X")
            .replace("Ã¢â€ â€™", "->")
            .replace("â†’", "->")
            .replace("\u2192", "->")
            .replace("Ã¢â€ Â", "<-")
            .replace("â†", "<-")
            .replace("\u2190", "<-")
            .replace("Ã¢â‚¬â€", "-")
            .replace("â€”", "-")
            .replace("â€“", "-")
            .replace("\u00b7", "-")
            .replace("Â·", "-")
            .replace("Ã¢â€“Â¼", "v")
            .replace("â–¼", "v")
            .replace("\u25bc", "v")
            .replace("Ã¢â€“Â²", "^")
            .replace("â–²", "^")
            .replace("\u25b2", "^")
            .replace("Ã¢â€“Â¾", "v")
            .replace("â–¾", "v")
            .replace("\u25be", "v")
            .replace("Ã¢â€“Â´", "^")
            .replace("â–´", "^")
            .replace("\u25b4", "^")
            .replace("Ã¢â€”Â", "X")
            .replace("â—", "X")
            .replace("\u25cf", "X")
            .replace("Ã¢â€”â€¹", "O")
            .replace("â—‹", "O")
            .replace("\u25cb", "O")
            .replace("â‰¡", "=")
            .replace("\u2261", "=")
            .replace("â˜…", "*")
            .replace("\u2605", "*")
            .replace("âˆž", "INF")
            .replace("\u221e", "INF")
            .replace("âš ", "WARN")
            .replace("âš¡", "BURST")
            .trim();
    }

    public static Style styleFor(Tone tone) {
        int color = colorFor(tone);
        return Style.EMPTY.withFont(fontSource(fontIdFor(tone))).withColor(color);
    }

    public static int width(Font renderer, String value, Tone tone) {
        return autismclient.gui.packui.PackUiText.width(renderer, value, fontIdFor(tone), colorFor(tone));
    }

    public static String trimToWidth(Font renderer, String value, int maxWidth, Tone tone) {
        return autismclient.gui.packui.PackUiText.trimToWidth(renderer, value, maxWidth, fontIdFor(tone), colorFor(tone));
    }

    public static void draw(GuiGraphics context, Font renderer, String value, Tone tone, int x, int y, boolean shadow) {
        autismclient.gui.packui.PackUiText.draw(context, renderer, value, fontIdFor(tone), colorFor(tone), x, y, shadow);
    }

    public static void draw(GuiGraphics context, Font renderer, String value, int color, int x, int y, boolean shadow) {
        autismclient.gui.packui.PackUiText.draw(context, renderer, value, PackUiAssets.FONT_BODY, color, x, y, shadow);
    }

    private static Identifier fontIdFor(Tone tone) {
        return switch (tone) {
            case TITLE -> PackUiAssets.FONT_TITLE;
            case LABEL -> PackUiAssets.FONT_LABEL;
            case BODY, MUTED, ACCENT -> PackUiAssets.FONT_BODY;
        };
    }

    //? if >=1.21.9 {
    private static FontDescription fontSource(Identifier fontId) {
        return FONT_CACHE.computeIfAbsent(fontId, FontDescription.Resource::new);
    }
    //?} else {
    /*private static Identifier fontSource(Identifier fontId) {
        return fontId;
    }
    *///?}
}
