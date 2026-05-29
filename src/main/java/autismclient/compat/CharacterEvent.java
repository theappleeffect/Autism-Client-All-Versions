package autismclient.compat;

/** Pre-1.21.9 stand-in for net.minecraft.client.input.CharacterEvent (Stonecutter-swapped on old versions). */
public record CharacterEvent(int codepoint, int modifiers) {
    public String codepointAsString() { return new String(Character.toChars(codepoint)); }
    public boolean isAllowedChatCharacter() { return codepoint >= 32 && codepoint != 127; }
}
