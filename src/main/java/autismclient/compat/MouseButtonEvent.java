package autismclient.compat;

/** Pre-1.21.9 stand-in for net.minecraft.client.input.MouseButtonEvent. */
public record MouseButtonEvent(double x, double y, MouseButtonInfo buttonInfo) {
    public int button() { return buttonInfo.button(); }
    public int modifiers() { return buttonInfo.modifiers(); }
    public int input() { return buttonInfo.input(); }
}
