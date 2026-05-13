package autismclient.util.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;

public class WaitForGuiAction implements MacroAction {
    public enum WaitMode { OPEN, CLOSE }

    public WaitMode waitMode = WaitMode.OPEN;
    public String guiTitle = "";
    private boolean enabled = true;

    @Override
    public MacroActionType getType() {
        return MacroActionType.WAIT_GUI;
    }

    @Override
    public void execute(Minecraft mc) {

    }

    @Override
    public String getDisplayName() {
        String prefix = waitMode == WaitMode.CLOSE ? "Wait GUI Close: " : "Wait GUI Open: ";
        return prefix + (guiTitle.isEmpty() ? "(none)" : guiTitle);
    }

    @Override
    public String getIcon() {
        return "GUI";
    }

    public boolean matchesText(String search, String target) {
        if (search.isEmpty() || target.isEmpty()) return false;
        if (search.equals(target)) return true;
        if (target.toLowerCase().contains(search.toLowerCase())) return true;

        String[] searchWords = search.toLowerCase().split("\\s+");
        String[] targetWords = target.toLowerCase().split("\\s+");

        boolean allFound = true;
        for (String word : searchWords) {
            boolean found = false;
            for (String tWord : targetWords) {
                if (tWord.contains(word) || word.contains(tWord)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                allFound = false;
                break;
            }
        }
        return allFound;
    }

    public boolean checkGui(Minecraft mc) {
        if (guiTitle.isEmpty()) return false;

        Screen screen = mc.screen;
        if (screen == null) return false;

        String title = screen.getTitle().getString();

        return matchesText(guiTitle, title);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", getType().name());
        tag.putString("waitMode", waitMode.name());
        tag.putString("guiTitle", guiTitle);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        if (tag.contains("waitMode")) {
            try { waitMode = WaitMode.valueOf(tag.getStringOr("waitMode", "OPEN")); }
            catch (IllegalArgumentException ignored) { waitMode = WaitMode.OPEN; }
        }
        if (tag.contains("guiTitle")) {
            guiTitle = tag.getStringOr("guiTitle", "");
        }
        if (tag.contains("enabled")) enabled = tag.getBooleanOr("enabled", true);
    }

    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }
}
