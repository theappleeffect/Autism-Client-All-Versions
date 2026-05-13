package autismclient.util.macro;

import autismclient.util.PackUtilSharedState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public class RestoreGuiAction implements MacroAction, WaitsForGui {
    public boolean waitForGui = true;
    private boolean enabled = true;

    public RestoreGuiAction() {}

    @Override
    public void execute(Minecraft mc) {
        PackUtilSharedState shared = PackUtilSharedState.get();
        if (shared.getStoredScreen() == null || shared.getStoredAbstractContainerMenu() == null) return;
        mc.setScreen(shared.getStoredScreen());
        if (mc.player != null) {
            mc.player.containerMenu = shared.getStoredAbstractContainerMenu();
        }
    }

    @Override public boolean isWaitForGui()         { return waitForGui; }
    @Override public void setWaitForGui(boolean v)  { this.waitForGui = v; }
    @Override public String getWaitGuiName()        { return ""; }
    @Override public void setWaitGuiName(String n)  {}

    @Override
    public MacroActionType getType() { return MacroActionType.RESTORE_GUI; }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "RESTORE_GUI");
        tag.putBoolean("waitForGui", waitForGui);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        if (tag.contains("waitForGui")) this.waitForGui = tag.getBooleanOr("waitForGui", true);
        if (tag.contains("enabled"))    this.enabled    = tag.getBooleanOr("enabled", true);
    }

    @Override
    public String getDisplayName() {
        return waitForGui ? "Restore GUI (wait)" : "Restore GUI";
    }

    @Override public String getIcon()           { return "R"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }
}
