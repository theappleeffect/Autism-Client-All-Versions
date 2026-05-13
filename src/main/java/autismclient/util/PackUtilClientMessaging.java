package autismclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;

public final class PackUtilClientMessaging {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final long DUPLICATE_WINDOW_MS = 600L;
    private static final String PREFIX_TEXT = "[PackUtil] ";

    private static String lastMessageKey = "";
    private static long lastMessageAtMs = 0L;

    private PackUtilClientMessaging() {}

    public static void send(String message) {
        send(buildBodyText(message));
    }

    public static void sendPrefixed(String message) {
        MutableComponent text = Component.empty()
            .append(styled("[", PackUtilColors.textMuted()))
            .append(styled("PackUtil", PackUtilColors.accent(), true))
            .append(styled("] ", PackUtilColors.textMuted()))
            .append(buildBodyText(message));
        send(text);
    }

    public static void send(Component text) {
        if (MC == null || text == null) return;

        String key = normalizeDuplicateKey(text.getString());
        long now = System.currentTimeMillis();
        synchronized (PackUtilClientMessaging.class) {
            if (!key.isEmpty() && key.equals(lastMessageKey) && now - lastMessageAtMs <= DUPLICATE_WINDOW_MS) {
                return;
            }
            lastMessageKey = key;
            lastMessageAtMs = now;
        }

        MC.execute(() -> {
            if (MC.player != null) {
                MC.player.sendSystemMessage(text);
            } else if (MC.getChatListener() != null) {
                MC.getChatListener().handleSystemMessage(text, false);
            }
        });
    }

    private static MutableComponent buildBodyText(String message) {
        String normalized = normalizeLegacyFormatting(message);
        MutableComponent lanChat = tryBuildLanChatText(normalized);
        if (lanChat != null) return lanChat;

        int defaultColor = inferDefaultColor(stripLegacyCodes(normalized));
        if (normalized.indexOf('\u00a7') >= 0) {
            return parseLegacyStyled(normalized, defaultColor);
        }

        return colorizeStructuredText(normalized, defaultColor);
    }

    private static MutableComponent tryBuildLanChatText(String message) {
        if (message == null || !message.startsWith("[LAN] <")) return null;

        int close = message.indexOf("> ");
        if (close <= 7) return null;

        String sender = message.substring(7, close);
        String body = message.substring(close + 2);
        return Component.empty()
            .append(styled("[", PackUtilColors.textMuted()))
            .append(styled("LAN", PackUtilColors.packetCyan(), true))
            .append(styled("] ", PackUtilColors.textMuted()))
            .append(styled("<", PackUtilColors.textMuted()))
            .append(styled(sender, PackUtilColors.packetPink(), true))
            .append(styled("> ", PackUtilColors.textMuted()))
            .append(colorizeTokenStream(body, PackUtilColors.textPrimary()));
    }

    private static MutableComponent colorizeStructuredText(String message, int defaultColor) {
        if (message == null || message.isEmpty()) return Component.empty();

        int colon = message.indexOf(':');
        if (colon > 0 && colon < 24 && isLikelyLabel(message.substring(0, colon))) {
            String label = message.substring(0, colon);
            String rest = message.substring(colon + 1);
            MutableComponent root = Component.empty()
                .append(styled(label, PackUtilColors.packetGray(), true))
                .append(styled(":", PackUtilColors.textMuted()));

            if (!rest.isEmpty()) {
                root.append(styled(leadingWhitespace(rest), PackUtilColors.textMuted()));
                root.append(colorizeTokenStream(rest.stripLeading(), inferDefaultColor(rest.trim())));
            }
            return root;
        }

        return colorizeTokenStream(message, defaultColor);
    }

    private static MutableComponent colorizeTokenStream(String message, int defaultColor) {
        MutableComponent root = Component.empty();
        if (message == null || message.isEmpty()) return root;

        StringBuilder token = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '#' || ch == '.' || ch == '/') {
                token.append(ch);
                continue;
            }

            if (token.length() > 0) {
                String word = token.toString();
                root.append(styled(word, colorForToken(word, defaultColor), isEmphasizedToken(word)));
                token.setLength(0);
            }

            root.append(styled(String.valueOf(ch), punctuationColor(ch, defaultColor)));
        }

        if (token.length() > 0) {
            String word = token.toString();
            root.append(styled(word, colorForToken(word, defaultColor), isEmphasizedToken(word)));
        }

        return root;
    }

    private static boolean isLikelyLabel(String value) {
        if (value == null || value.isBlank()) return false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (!(Character.isLetterOrDigit(ch) || Character.isWhitespace(ch) || ch == '[' || ch == ']' || ch == '+' || ch == '-' || ch == '/' || ch == '#')) {
                return false;
            }
        }
        return true;
    }

    private static String leadingWhitespace(String value) {
        if (value == null || value.isEmpty()) return "";
        int i = 0;
        while (i < value.length() && Character.isWhitespace(value.charAt(i))) i++;
        return value.substring(0, i);
    }

    private static int punctuationColor(char ch, int defaultColor) {
        return switch (ch) {
            case '[', ']', '(', ')', '<', '>', ':', '|', ',' -> PackUtilColors.textMuted();
            case '"' -> PackUtilColors.packetLightYellow();
            default -> defaultColor;
        };
    }

    private static boolean isEmphasizedToken(String token) {
        String normalized = normalizeToken(token);
        return normalized.equals("enabled")
            || normalized.equals("disabled")
            || normalized.equals("started")
            || normalized.equals("stopped")
            || normalized.equals("failed")
            || normalized.equals("error")
            || normalized.equals("queue")
            || normalized.equals("packet")
            || normalized.equals("packets")
            || normalized.equals("gui")
            || normalized.equals("macro")
            || normalized.equals("macros")
            || normalized.equals("sync")
            || normalized.equals("lan");
    }

    private static int colorForToken(String token, int defaultColor) {
        String normalized = normalizeToken(token);
        if (normalized.isEmpty()) return defaultColor;

        if (isNumericToken(normalized)) return PackUtilColors.packetLightYellow();

        if (normalized.equals("enabled")
            || normalized.equals("started")
            || normalized.equals("joined")
            || normalized.equals("restored")
            || normalized.equals("saved")
            || normalized.equals("loaded")
            || normalized.equals("copied")
            || normalized.equals("imported")
            || normalized.equals("received")
            || normalized.equals("executed")
            || normalized.equals("finished")
            || normalized.equals("reconnected")
            || normalized.equals("host")
            || normalized.equals("sent")
            || normalized.equals("on")
            || normalized.equals("true")) {
            return PackUtilColors.successText();
        }

        if (normalized.equals("disabled")
            || normalized.equals("stopped")
            || normalized.equals("empty")
            || normalized.equals("already")
            || normalized.equals("requested")
            || normalized.equals("left")
            || normalized.equals("attempting")
            || normalized.equals("timeout")
            || normalized.equals("timed")
            || normalized.equals("out")
            || normalized.equals("off")
            || normalized.equals("false")) {
            return PackUtilColors.packetYellow();
        }

        if (normalized.equals("failed")
            || normalized.equals("error")
            || normalized.equals("errors")
            || normalized.equals("denied")
            || normalized.equals("blocked")
            || normalized.equals("crashed")
            || normalized.equals("cannot")
            || normalized.equals("missing")
            || normalized.equals("unavailable")
            || normalized.equals("invalid")
            || normalized.equals("not")
            || normalized.equals("null")) {
            return PackUtilColors.dangerText();
        }

        if (normalized.equals("queue")
            || normalized.equals("packet")
            || normalized.equals("packets")
            || normalized.equals("sync")
            || normalized.equals("lan")
            || normalized.equals("session")
            || normalized.equals("tcp")) {
            return PackUtilColors.packetCyan();
        }

        if (normalized.equals("gui")
            || normalized.equals("screen")
            || normalized.equals("inventory")
            || normalized.equals("slot")
            || normalized.equals("slots")) {
            return PackUtilColors.packetOrange();
        }

        if (normalized.equals("macro")
            || normalized.equals("macros")
            || normalized.equals("preset")
            || normalized.equals("presets")) {
            return PackUtilColors.packetPink();
        }

        return defaultColor;
    }

    private static boolean isNumericToken(String token) {
        int start = token.startsWith("-") ? 1 : 0;
        if (start >= token.length()) return false;
        for (int i = start; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (!Character.isDigit(ch) && ch != '.' && ch != '/' && ch != '#') return false;
        }
        return true;
    }

    private static String normalizeToken(String token) {
        if (token == null || token.isEmpty()) return "";
        int start = 0;
        int end = token.length();
        while (start < end && !Character.isLetterOrDigit(token.charAt(start))) start++;
        while (end > start && !Character.isLetterOrDigit(token.charAt(end - 1))) end--;
        if (start >= end) return "";
        return token.substring(start, end).toLowerCase();
    }

    private static MutableComponent parseLegacyStyled(String message, int defaultColor) {
        MutableComponent root = Component.empty();
        if (message == null || message.isEmpty()) return root;

        StringBuilder buffer = new StringBuilder();
        int currentColor = defaultColor;
        boolean bold = false;

        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            if (ch == '\u00a7' && i + 1 < message.length()) {
                if (buffer.length() > 0) {
                    root.append(styled(buffer.toString(), currentColor, bold));
                    buffer.setLength(0);
                }

                char code = Character.toLowerCase(message.charAt(++i));
                Integer mapped = mapLegacyColor(code);
                if (mapped != null) {
                    currentColor = mapped;
                    if (isColorCode(code) || code == 'r') bold = false;
                    continue;
                }

                if (code == 'l') {
                    bold = true;
                } else if (code == 'r') {
                    currentColor = defaultColor;
                    bold = false;
                }
                continue;
            }
            buffer.append(ch);
        }

        if (buffer.length() > 0) {
            root.append(styled(buffer.toString(), currentColor, bold));
        }

        return root;
    }

    private static MutableComponent styled(String value, int color) {
        return styled(value, color, false);
    }

    private static MutableComponent styled(String value, int color, boolean bold) {
        Style style = Style.EMPTY.withColor(color);
        if (bold) style = style.withBold(true);
        return Component.literal(value).setStyle(style);
    }

    private static boolean isColorCode(char code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }

    private static Integer mapLegacyColor(char code) {
        return switch (code) {
            case '0' -> 0xFF5E4B4E;
            case '1' -> 0xFF6B92D8;
            case '2' -> 0xFF59B974;
            case '3' -> PackUtilColors.packetCyan();
            case '4' -> PackUtilColors.dangerText();
            case '5' -> 0xFFE78ACB;
            case '6' -> PackUtilColors.packetOrange();
            case '7' -> PackUtilColors.packetGray();
            case '8' -> PackUtilColors.textMuted();
            case '9' -> PackUtilColors.packetBlue();
            case 'a' -> PackUtilColors.successText();
            case 'b' -> PackUtilColors.packetCyan();
            case 'c' -> PackUtilColors.dangerText();
            case 'd' -> PackUtilColors.packetPink();
            case 'e' -> PackUtilColors.packetYellow();
            case 'f' -> PackUtilColors.textSecondary();
            case 'r' -> null;
            default -> null;
        };
    }

    private static String normalizeLegacyFormatting(String message) {
        if (message == null) return "";
        return message
            .replace("ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§", "\u00a7")
            .replace("Ãƒâ€šÃ‚Â§", "\u00a7")
            .replace("Ã‚Â§", "\u00a7")
            .replace("Â§", "\u00a7")
            .replace("\u00c2\u00a7", "\u00a7");
    }

    private static String stripLegacyCodes(String message) {
        if (message == null || message.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            if (ch == '\u00a7' && i + 1 < message.length()) {
                i++;
                continue;
            }
            sb.append(ch);
        }
        return sb.toString().trim();
    }

    private static int inferDefaultColor(String message) {
        String lower = message == null ? "" : message.trim().toLowerCase();
        if (lower.isEmpty()) return PackUtilColors.textSecondary();

        if (lower.contains("failed")
            || lower.contains("error")
            || lower.startsWith("no ")
            || lower.startsWith("not ")
            || lower.contains(" not ")
            || lower.contains("cannot")
            || lower.contains("denied")
            || lower.contains("blocked")
            || lower.contains("crashed")) {
            return PackUtilColors.dangerText();
        }

        if (lower.contains("enabled")
            || lower.contains("started")
            || lower.contains("joined")
            || lower.contains("restored")
            || lower.contains("saved")
            || lower.contains("loaded")
            || lower.contains("copied")
            || lower.contains("imported")
            || lower.contains("received")
            || lower.contains("executed")
            || lower.contains("finished")
            || lower.startsWith("sent ")
            || lower.startsWith("signed ")
            || lower.startsWith("deleted ")
            || lower.startsWith("broadcasted ")
            || lower.startsWith("queue sent")) {
            return PackUtilColors.successText();
        }

        if (lower.contains("disabled")
            || lower.contains("stopped")
            || lower.contains("empty")
            || lower.contains("already")
            || lower.contains("requested")
            || lower.contains("left")
            || lower.contains("attempting")
            || lower.contains("timed out")) {
            return PackUtilColors.packetYellow();
        }

        if (lower.startsWith("[lan] <")) {
            return PackUtilColors.textPrimary();
        }

        return PackUtilColors.textSecondary();
    }

    private static String normalizeDuplicateKey(String message) {
        if (message == null) return "";
        return stripLegacyCodes(normalizeLegacyFormatting(message))
            .replace(PREFIX_TEXT, "")
            .replaceAll("\\s+", " ")
            .trim()
            .toLowerCase();
    }
}
