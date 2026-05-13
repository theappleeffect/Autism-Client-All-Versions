package autismclient.util.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public class WaitForChatAction implements MacroAction, WaitsForGui {
    public String pattern = "";
    public String patternJson = "";
    public boolean useRegex = false;
    public int fuzzyPercent = 100;
    public boolean serverMessageOnly = false;
    public int timeoutMs = 0;
    private boolean enabled = true;
    public boolean waitForGui = false;
    public String waitGuiName = "";

    public WaitForChatAction() {}

    public WaitForChatAction(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public void execute(Minecraft mc) {

    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", getType().name());
        tag.putString("pattern", pattern);
        tag.putString("patternJson", patternJson == null ? "" : patternJson);
        tag.putBoolean("useRegex", useRegex);
        tag.putInt("fuzzyPercent", fuzzyPercent);
        tag.putBoolean("serverMessageOnly", false);
        tag.putInt("timeoutMs", timeoutMs);
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("waitForGui", waitForGui);
        tag.putString("waitGuiName", waitGuiName);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        if (tag.contains("pattern")) pattern = tag.getStringOr("pattern", "");
        if (tag.contains("patternJson")) patternJson = tag.getStringOr("patternJson", "");
        else patternJson = "";
        if (tag.contains("useRegex")) useRegex = tag.getBooleanOr("useRegex", false);
        if (tag.contains("fuzzyPercent")) fuzzyPercent = clampFuzzyPercent(tag.getIntOr("fuzzyPercent", 100));
        else fuzzyPercent = 100;
        serverMessageOnly = false;
        if (tag.contains("timeoutMs")) timeoutMs = tag.getIntOr("timeoutMs", 0);
        if (tag.contains("enabled")) enabled = tag.getBooleanOr("enabled", true);
        if (tag.contains("waitForGui")) waitForGui = tag.getBooleanOr("waitForGui", false);
        if (tag.contains("waitGuiName")) waitGuiName = tag.getStringOr("waitGuiName", "");
    }

    @Override
    public MacroActionType getType() {
        return MacroActionType.WAIT_CHAT;
    }

    @Override
    public String getDisplayName() {
        String desc = pattern.isEmpty() ? "Any Chat" : pattern;
        if (!useRegex) desc += " (~" + clampFuzzyPercent(fuzzyPercent) + "%)";
        if (timeoutMs > 0) desc += " (" + timeoutMs + "ms)";
        return "Wait Chat: " + desc;
    }

    @Override
    public String getIcon() {
        return "WCH";
    }

    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    @Override public boolean isWaitForGui() { return waitForGui; }
    @Override public void setWaitForGui(boolean v) { this.waitForGui = v; }
    @Override public String getWaitGuiName() { return waitGuiName; }
    @Override public void setWaitGuiName(String name) { this.waitGuiName = name; }

    public static int clampFuzzyPercent(int percent) {
        int clamped = Math.max(40, Math.min(100, percent));
        int snapped = Math.round(clamped / 10.0f) * 10;
        return Math.max(40, Math.min(100, snapped));
    }
}
