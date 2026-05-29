package autismclient.compat;

/** Pre-1.21.9 stand-in for net.minecraft.client.input.MouseButtonInfo. */
public record MouseButtonInfo(int button, int modifiers) {
    public int input() { return button; }
}
