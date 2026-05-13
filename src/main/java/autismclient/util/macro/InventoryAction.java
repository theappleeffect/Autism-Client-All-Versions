package autismclient.util.macro;

import autismclient.util.PackUtilGuiActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;

public class InventoryAction implements MacroAction, WaitsForGui {
    public enum InvMode {
        OPEN,
        CLOSE
    }

    public InvMode mode = InvMode.OPEN;
    public boolean waitForGui = true;
    public String guiName = "";

    public boolean sendPacket = true;

    public InventoryAction() {}

    @Override
    public void execute(Minecraft mc) {
        if (mc.player == null) return;

        if (mode == InvMode.OPEN) {
            mc.execute(() -> {
                mc.setScreen(new InventoryScreen(mc.player));
            });
        } else {
            mc.execute(() -> {
                PackUtilGuiActions.closeCurrentScreen(mc, sendPacket, false);
            });
        }
    }

    @Override public boolean isWaitForGui() { return mode == InvMode.OPEN && waitForGui; }
    @Override public void setWaitForGui(boolean v) { this.waitForGui = v; }
    @Override public String getWaitGuiName() { return ""; }
    @Override public void setWaitGuiName(String name) { this.guiName = ""; }

    @Override
    public MacroActionType getType() {
        return MacroActionType.INVENTORY;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "INVENTORY");
        tag.putString("mode", mode.name());
        tag.putBoolean("waitForGui", waitForGui);
        tag.putString("guiName", guiName);
        tag.putBoolean("sendPacket", sendPacket);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        if (tag.contains("mode")) {
            try {
                this.mode = InvMode.valueOf(tag.getStringOr("mode", "OPEN"));
            } catch (IllegalArgumentException ignored) {
                this.mode = InvMode.OPEN;
            }
        }
        if (tag.contains("waitForGui")) this.waitForGui = tag.getBooleanOr("waitForGui", true);
        if (tag.contains("guiName")) this.guiName = tag.getStringOr("guiName", "");
        if (tag.contains("sendPacket")) this.sendPacket = tag.getBooleanOr("sendPacket", true);
    }

    @Override
    public String getDisplayName() {
        if (mode == InvMode.OPEN) return "Inv Open";
        return sendPacket ? "Inv Close" : "Inv Close (no pkt)";
    }

    @Override
    public String getIcon() {
        return "I";
    }
}
