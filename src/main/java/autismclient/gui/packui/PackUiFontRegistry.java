package autismclient.gui.packui;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PackUiFontRegistry {
    private final Map<String, Identifier> fonts = new LinkedHashMap<>();
    private Identifier defaultFont;

    public PackUiFontRegistry register(String key, Identifier fontId) {
        if (key == null || key.isBlank() || fontId == null) return this;
        fonts.put(key, fontId);
        if (defaultFont == null) defaultFont = fontId;
        return this;
    }

    public Identifier get(String key) {
        Identifier font = fonts.get(key);
        return font != null ? font : defaultFont;
    }

    public Identifier getDefault() {
        return defaultFont;
    }
}
