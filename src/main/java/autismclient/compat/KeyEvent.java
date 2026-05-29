package autismclient.compat;

/** Pre-1.21.9 stand-in for net.minecraft.client.input.KeyEvent. */
public record KeyEvent(int key, int scancode, int modifiers) {
    public int input() { return key; }
}
