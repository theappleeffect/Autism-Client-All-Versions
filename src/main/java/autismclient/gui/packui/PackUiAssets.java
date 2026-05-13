package autismclient.gui.packui;

import net.minecraft.resources.Identifier;

public final class PackUiAssets {
    public static final Identifier FONT_BODY = Identifier.fromNamespaceAndPath("autismclient", "pack_ui_body");
    public static final Identifier FONT_LABEL = Identifier.fromNamespaceAndPath("autismclient", "pack_ui_label");
    public static final Identifier FONT_TITLE = Identifier.fromNamespaceAndPath("autismclient", "pack_ui_title");
    public static final Identifier FONT_FALLBACK = Identifier.fromNamespaceAndPath("autismclient", "pack_ui_fallback");

    public static final Identifier ICON_KEYBINDS = icon("keybinds");
    public static final Identifier ICON_LANSYNC = icon("lansync");
    public static final Identifier ICON_MACROS = icon("macros");
    public static final Identifier ICON_FABRICATOR = icon("fabricator");
    public static final Identifier ICON_FILTER = icon("filter");
    public static final Identifier ICON_PACKET_LOGGER = icon("packetlogger");
    public static final Identifier ICON_PACKET_Q_EDITOR = icon("packetqeditor");
    public static final Identifier ICON_SERVER_INFO = icon("serverinfo");
    public static final Identifier ICON_MAIN_MENU_CATEGORY = icon("mainmenucategory");
    public static final Identifier ICON_PACKET_CATEGORY = icon("packetcategory");
    public static final Identifier ICON_SCREEN_CATEGORY = icon("screencategory");
    public static final Identifier ICON_CHAT_CATEGORY = icon("chatcategory");
    public static final Identifier ICON_WINDOW_CLOSE = icon("windowclose");
    public static final Identifier ICON_WINDOW_CHEVRON_UP = icon("windowchevronup");
    public static final Identifier ICON_WINDOW_CHEVRON_DOWN = icon("windowchevrondown");
    public static final Identifier ICON_WINDOW_CHEVRON_RIGHT = icon("windowchevronright");
    public static final Identifier ICON_LIST_SELECTED = icon("listselected");

    private PackUiAssets() {
    }

    public static Identifier bodyFont(int scalePercent) {
        return FONT_BODY;
    }

    public static Identifier labelFont(int scalePercent) {
        return FONT_LABEL;
    }

    public static Identifier titleFont(int scalePercent) {
        return FONT_TITLE;
    }

    public static Identifier fallbackFont(int scalePercent) {
        return FONT_FALLBACK;
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath("autismclient", "textures/gui/packui/icons/" + name + ".png");
    }
}
