package autismclient.util.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public class SendChatAction implements MacroAction, WaitsForGui {
    public String message = "";
    public boolean waitForGui = true;
    public String guiName = "";
    private boolean enabled = true;

    public SendChatAction() {}

    public SendChatAction(String message) {
        this.message = message;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", MacroActionType.SEND_CHAT.name());
        tag.putString("message", message);
        tag.putBoolean("waitForGui", waitForGui);
        tag.putString("guiName", guiName);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        if (tag.contains("message")) {
            this.message = tag.getStringOr("message", "");
        }
        if (tag.contains("waitForGui")) {
            this.waitForGui = tag.getBooleanOr("waitForGui", false);
        }
        if (tag.contains("guiName")) {
            this.guiName = tag.getStringOr("guiName", "");
        }
        if (tag.contains("enabled")) enabled = tag.getBooleanOr("enabled", true);
    }

    @Override
    public void execute(Minecraft mc) {
        if (message == null || message.isEmpty()) return;
        if (mc.getConnection() == null) return;
        if (message.startsWith("/") && message.length() > 1) {
            mc.getConnection().sendCommand(message.substring(1));
        } else {
            mc.getConnection().sendChat(message);
        }
    }

    @Override
    public MacroActionType getType() { return MacroActionType.SEND_CHAT; }

    @Override public boolean isWaitForGui() { return waitForGui; }
    @Override public void setWaitForGui(boolean v) { this.waitForGui = v; }
    @Override public String getWaitGuiName() { return guiName; }
    @Override public void setWaitGuiName(String name) { this.guiName = name != null ? name : ""; }

    @Override
    public String getDisplayName() {
        String preview = message.length() > 20 ? message.substring(0, 20) + "..." : message;
        return "Chat: " + preview;
    }

    @Override
    public String getIcon() { return "CH"; }

    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }
}
