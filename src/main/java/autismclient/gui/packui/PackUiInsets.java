package autismclient.gui.packui;

public record PackUiInsets(int left, int top, int right, int bottom) {
    public static final PackUiInsets NONE = new PackUiInsets(0, 0, 0, 0);

    public static PackUiInsets all(int value) {
        return new PackUiInsets(value, value, value, value);
    }

    public static PackUiInsets symmetric(int horizontal, int vertical) {
        return new PackUiInsets(horizontal, vertical, horizontal, vertical);
    }

    public int horizontal() {
        return left + right;
    }

    public int vertical() {
        return top + bottom;
    }
}
