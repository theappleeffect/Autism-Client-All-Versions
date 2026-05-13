package autismclient.gui.packui;

public final class PackUiTheme {
    public static final float DEFAULT_DENSITY = 2.0f;
    private static final int BODY_FONT_HEIGHT = 11;
    private static final int LABEL_FONT_HEIGHT = 12;
    private static final int TITLE_FONT_HEIGHT = 13;
    private static final int HEADER_HEIGHT = 16;
    private static final int CONTENT_PADDING = 4;
    private static final int ROW_GAP = 2;
    private static final int BUTTON_HEIGHT = 14;
    private static final int LABEL_GAP = 1;

    private final PackUiFontRegistry fonts = new PackUiFontRegistry();
    private final PackUiIconRegistry icons = new PackUiIconRegistry();

    private final int windowRadius = 0;

    public static int lessTransparent30(int color) {
        return boostAlpha(color, 13, 10);
    }

    private static int boostAlpha(int color, int numerator, int denominator) {
        int alpha = (color >>> 24) & 0xFF;
        int boosted = Math.min(255, (alpha * numerator) / denominator);
        return (boosted << 24) | (color & 0x00FFFFFF);
    }

    public PackUiTheme() {
        fonts.register("default", PackUiAssets.FONT_BODY);
        fonts.register("title", PackUiAssets.FONT_TITLE);
        fonts.register("body", PackUiAssets.FONT_BODY);
        fonts.register("label", PackUiAssets.FONT_LABEL);

        icons.register("keybinds", PackUiAssets.ICON_KEYBINDS);
        icons.register("lansync", PackUiAssets.ICON_LANSYNC);
        icons.register("macros", PackUiAssets.ICON_MACROS);
        icons.register("fabricator", PackUiAssets.ICON_FABRICATOR);
        icons.register("filter", PackUiAssets.ICON_FILTER);
        icons.register("packet_logger", PackUiAssets.ICON_PACKET_LOGGER);
        icons.register("packet_q_editor", PackUiAssets.ICON_PACKET_Q_EDITOR);
        icons.register("server_info", PackUiAssets.ICON_SERVER_INFO);
        icons.register("main_menu_category", PackUiAssets.ICON_MAIN_MENU_CATEGORY);
        icons.register("packet_category", PackUiAssets.ICON_PACKET_CATEGORY);
        icons.register("screen_category", PackUiAssets.ICON_SCREEN_CATEGORY);
        icons.register("chat_category", PackUiAssets.ICON_CHAT_CATEGORY);
    }

    public PackUiFontRegistry fonts() {
        return fonts;
    }

    public PackUiIconRegistry icons() {
        return icons;
    }

    public net.minecraft.resources.Identifier fontFor(PackUiTone tone) {
        return switch (tone) {
            case TITLE -> PackUiAssets.FONT_TITLE;
            case LABEL -> PackUiAssets.FONT_LABEL;
            case BODY, MUTED, ACCENT -> PackUiAssets.FONT_BODY;
        };
    }

    public int windowRadius() {
        return windowRadius;
    }

    public int headerHeight() {
        return Math.max(HEADER_HEIGHT, lineHeight(PackUiTone.LABEL, 3));
    }

    public int contentPadding() {
        return CONTENT_PADDING;
    }

    public int rowGap() {
        return ROW_GAP;
    }

    public int buttonHeight() {
        return Math.max(BUTTON_HEIGHT, lineHeight(PackUiTone.BODY, 3));
    }

    public int labelGap() {
        return LABEL_GAP;
    }

    public float scaleFactor() {
        return 1.0f;
    }

    public int scale(int value) {
        return value;
    }

    public float scale(float value) {
        return value;
    }

    public PackUiInsets scale(PackUiInsets insets) {
        if (insets == null) return PackUiInsets.NONE;
        return new PackUiInsets(
            scale(insets.left()),
            scale(insets.top()),
            scale(insets.right()),
            scale(insets.bottom())
        );
    }

    public int fontHeight(PackUiTone tone) {
        net.minecraft.resources.Identifier fontId = fontFor(tone);
        if (PackUiAtlasTextRenderer.supports(fontId) && PackUiAtlasTextRenderer.isReady()) {
            int atlasHeight = PackUiAtlasTextRenderer.height(fontId);
            if (atlasHeight > 0) return atlasHeight;
        }
        return switch (tone) {
            case TITLE -> TITLE_FONT_HEIGHT;
            case LABEL -> LABEL_FONT_HEIGHT;
            case BODY, MUTED, ACCENT -> BODY_FONT_HEIGHT;
        };
    }

    public int lineHeight(PackUiTone tone, int extraSpacing) {
        int minimumSpacing = switch (tone) {
            case TITLE -> 3;
            case LABEL -> 2;
            case BODY, MUTED, ACCENT -> 2;
        };
        return fontHeight(tone) + Math.max(scale(extraSpacing), minimumSpacing);
    }

    public int bodyTextNudge() {
        return 1;
    }

    public int buttonTextNudge() {
        return 1;
    }

    public int fieldTextNudge() {
        return 1;
    }

    public int color(PackUiTone tone) {
        return switch (tone) {
            case TITLE, LABEL, BODY -> 0xFFF3ECE7;
            case MUTED -> 0xFFB79E9E;
            case ACCENT -> 0xFFFF4D4D;
        };
    }

    public int overlaySurface(int rgb) {
        return 0xA8000000 | (rgb & 0x00FFFFFF);
    }

    public int overlaySurfaceSoft(int rgb) {
        return 0xA0000000 | (rgb & 0x00FFFFFF);
    }

    public int overlaySurfaceStrong(int rgb) {
        return 0xAE000000 | (rgb & 0x00FFFFFF);
    }

    public int windowFill() {
        return overlaySurface(0x000A0A0C);
    }

    public int windowFillInactive() {
        return overlaySurfaceSoft(0x00070709);
    }

    public int headerFill() {
        return overlaySurface(0x00111114);
    }

    public int headerFillInactive() {
        return overlaySurfaceSoft(0x000A0A0C);
    }

    public int headerAccent() {
        return 0xFFFF3B3B;
    }

    public int borderColor() {
        return 0xFFB32B2B;
    }

    public int borderSoft() {
        return lessTransparent30(0xAA6B2020);
    }

    public int hoverFill() {
        return 0x661E0E10;
    }

    public int dangerFill() {
        return overlaySurface(0x002A0E12);
    }

    public int dangerBorder() {
        return 0xFFFF7D7D;
    }

    public int buttonFill(PackUiButton.Variant variant) {
        return switch (variant) {
            case PRIMARY   -> overlaySurface(0x008F1F24);
            case SECONDARY -> overlaySurface(0x00120E11);
            case GHOST     -> overlaySurfaceSoft(0x00120D0F);
            case DANGER    -> overlaySurface(0x00230D11);
            case SUCCESS   -> overlaySurface(0x0010261A);
        };
    }

    public int buttonFill(PackUiButton.Variant variant, boolean active) {
        if (!active) return buttonFillInactive(variant);
        return buttonFill(variant);
    }

    private int buttonFillInactive(PackUiButton.Variant variant) {
        return switch (variant) {
            case PRIMARY   -> overlaySurfaceSoft(0x008F1F24);
            case SECONDARY -> overlaySurfaceSoft(0x00121316);
            case GHOST     -> 0x70121316;
            case DANGER    -> overlaySurfaceSoft(0x00121316);
            case SUCCESS   -> overlaySurfaceSoft(0x00121316);
        };
    }

    public int buttonBorder(PackUiButton.Variant variant) {
        return switch (variant) {
            case PRIMARY        -> 0xFFFF8787;
            case SECONDARY, GHOST -> borderSoft();
            case DANGER        -> 0xFFB54848;
            case SUCCESS       -> 0xFF81D1A0;
        };
    }

    public int buttonBorderInactive(PackUiButton.Variant variant) {
        return switch (variant) {
            case PRIMARY        -> 0xFFFF8787;
            case SECONDARY, GHOST -> 0xFF6B5050;
            case DANGER        -> 0xFFB54848;
            case SUCCESS       -> 0xFF81D1A0;
        };
    }

    public int buttonBorderGlow(PackUiButton.Variant variant) {
        return switch (variant) {
            case PRIMARY        -> 0xFFFFA4A4;
            case SECONDARY, GHOST -> 0xFFFF6464;
            case DANGER        -> 0xFFFF9090;
            case SUCCESS       -> 0xFFAAEBC0;
        };
    }

    public int buttonTextColor(PackUiButton.Variant variant) {
        return switch (variant) {
            case PRIMARY        -> 0xFFFFF4F4;
            case SECONDARY, GHOST -> color(PackUiTone.BODY);
            case DANGER        -> 0xFFFF5A5A;
            case SUCCESS       -> 0xFFB8F3CB;
        };
    }

    public int buttonTextColorInactive(PackUiButton.Variant variant) {
        return switch (variant) {
            case PRIMARY        -> 0xFFFFF4F4;
            case SECONDARY, GHOST -> color(PackUiTone.MUTED);
            case DANGER        -> 0xFF9C7B7B;
            case SUCCESS       -> 0xFF88A690;
        };
    }

    public int buttonRippleRing(PackUiButton.Variant variant) {
        return switch (variant) {
            case PRIMARY        -> 0xFFFFB2B2;
            case SECONDARY, GHOST -> 0xFFFF7070;
            case DANGER        -> 0xFFFF8A8A;
            case SUCCESS       -> 0xFF9EE6B7;
        };
    }

    public int buttonRippleFill(PackUiButton.Variant variant) {
        return switch (variant) {
            case PRIMARY        -> 0xFFFFE1E1;
            case SECONDARY, GHOST -> 0xFFFFCACA;
            case DANGER        -> 0xFFFFD1D1;
            case SUCCESS       -> 0xFFD7F7E2;
        };
    }

    public int overlayButtonFill(PackUiOverlayButton.Variant variant, boolean active) {
        if (variant == PackUiOverlayButton.Variant.FILTER_ON) return 0x553D6BFF;
        if (variant == PackUiOverlayButton.Variant.FILTER_OFF) return 0x66272B31;
        return buttonFill(toButtonVariant(variant), active);
    }

    private int overlayButtonFillInactive(PackUiOverlayButton.Variant variant) {
        return buttonFillInactive(toButtonVariant(variant));
    }

    public int overlayButtonBorder(PackUiOverlayButton.Variant variant, boolean active) {
        if (variant == PackUiOverlayButton.Variant.FILTER_ON) return 0xFF6F8DFF;
        if (variant == PackUiOverlayButton.Variant.FILTER_OFF) return 0xFF565D66;
        return active ? buttonBorder(toButtonVariant(variant)) : buttonBorderInactive(toButtonVariant(variant));
    }

    private int overlayButtonBorderInactive(PackUiOverlayButton.Variant variant) {
        return buttonBorderInactive(toButtonVariant(variant));
    }

    public int overlayButtonBorderGlow(PackUiOverlayButton.Variant variant) {
        if (variant == PackUiOverlayButton.Variant.FILTER_ON) return 0xFF91A7FF;
        if (variant == PackUiOverlayButton.Variant.FILTER_OFF) return 0xFF747C87;
        return buttonBorderGlow(toButtonVariant(variant));
    }

    public int overlayButtonTextColor(PackUiOverlayButton.Variant variant, boolean active) {
        if (variant == PackUiOverlayButton.Variant.FILTER_ON) return 0xFFFFFFFF;
        if (variant == PackUiOverlayButton.Variant.FILTER_OFF) return 0xFFB8BEC7;
        return active ? buttonTextColor(toButtonVariant(variant)) : buttonTextColorInactive(toButtonVariant(variant));
    }

    private int overlayButtonTextColorInactive(PackUiOverlayButton.Variant variant) {
        return buttonTextColorInactive(toButtonVariant(variant));
    }

    public int overlayButtonRippleRing(PackUiOverlayButton.Variant variant) {
        if (variant == PackUiOverlayButton.Variant.FILTER_ON) return 0xFFB8C6FF;
        if (variant == PackUiOverlayButton.Variant.FILTER_OFF) return 0xFFD2D7DD;
        return buttonRippleRing(toButtonVariant(variant));
    }

    public int overlayButtonRippleFill(PackUiOverlayButton.Variant variant) {
        if (variant == PackUiOverlayButton.Variant.FILTER_ON) return 0xFFE2E8FF;
        if (variant == PackUiOverlayButton.Variant.FILTER_OFF) return 0xFFE5E8EC;
        return buttonRippleFill(toButtonVariant(variant));
    }

    private PackUiButton.Variant toButtonVariant(PackUiOverlayButton.Variant variant) {
        return switch (variant) {
            case PRIMARY -> PackUiButton.Variant.PRIMARY;
            case SECONDARY -> PackUiButton.Variant.SECONDARY;
            case GHOST -> PackUiButton.Variant.GHOST;
            case DANGER -> PackUiButton.Variant.DANGER;
            case SUCCESS -> PackUiButton.Variant.SUCCESS;
            case FILTER_ON -> PackUiButton.Variant.PRIMARY;
            case FILTER_OFF -> PackUiButton.Variant.GHOST;
        };
    }

    public int listFill() {
        return overlaySurface(0x00101014);
    }

    public int listFillFocused() {
        return overlaySurfaceStrong(0x00131418);
    }

    public int rowFillNormal() {
        return 0x26181A1E;
    }

    public int rowFillHovered() {
        return 0x4F241A1D;
    }

    public int rowFillSelected() {
        return overlaySurface(0x0023382B);
    }

    public int headerControlFill() {
        return overlaySurfaceSoft(0x0015080A);
    }

    public int headerControlFillActive() {
        return overlaySurface(0x00160A0D);
    }

    public int headerControlBorder() {
        return 0xAA7A3131;
    }

    public int headerControlBorderActive() {
        return 0xFFB54848;
    }

    public int inlineBannerFill() {
        return overlaySurface(0x00121012);
    }

    public int inactiveCoverFill() {
        return 0x36000000;
    }

    public int inactiveBodyFadeFill() {
        return overlaySurface(0x0009090B);
    }
}
