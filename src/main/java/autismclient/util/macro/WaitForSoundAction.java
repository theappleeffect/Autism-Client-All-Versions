package autismclient.util.macro;

import autismclient.util.PackUtilRegistryLabels;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.List;

public class WaitForSoundAction implements MacroAction, WaitsForGui {
    public List<String> soundIds = new ArrayList<>();

    public boolean waitForGui = false;
    public String waitGuiName = "";

    public boolean checkDistance = false;
    public double maxDistance = 16.0;
    private boolean enabled = true;

    @Override public MacroActionType getType() { return MacroActionType.WAIT_SOUND; }
    @Override public void execute(Minecraft mc) {  }

    @Override
    public String getDisplayName() {
        if (soundIds.isEmpty()) return "Wait Sound: Any";
        String first = PackUtilRegistryLabels.sound(soundIds.get(0));
        String s = soundIds.size() == 1 ? first : first + " (+" + (soundIds.size() - 1) + ")";
        return "Wait Sound: " + s;
    }

    @Override public String getIcon() { return "SND"; }

    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    @Override public boolean isWaitForGui() { return waitForGui; }
    @Override public void setWaitForGui(boolean v) { waitForGui = v; }
    @Override public String getWaitGuiName() { return waitGuiName; }
    @Override public void setWaitGuiName(String name) { waitGuiName = name; }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", getType().name());
        ListTag list = new ListTag();
        for (String id : soundIds) list.add(StringTag.valueOf(id));
        tag.put("soundIds", list);
        tag.putBoolean("waitForGui", waitForGui);
        tag.putString("waitGuiName", waitGuiName);
        tag.putBoolean("checkDistance", checkDistance);
        tag.putDouble("maxDistance", maxDistance);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        soundIds.clear();
        if (tag.contains("soundIds")) {
            ListTag list = tag.getList("soundIds").orElse(new ListTag());
            for (Tag el : list) {
                String s = el.asString().orElse("");
                if (!s.isEmpty()) soundIds.add(s);
            }
        }
        waitForGui = tag.getBooleanOr("waitForGui", false);
        waitGuiName = tag.getStringOr("waitGuiName", "");
        checkDistance = tag.getBooleanOr("checkDistance", false);
        maxDistance = tag.getDoubleOr("maxDistance", 16.0);
        if (tag.contains("enabled")) enabled = tag.getBooleanOr("enabled", true);
    }
}
