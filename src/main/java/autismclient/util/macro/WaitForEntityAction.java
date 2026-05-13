package autismclient.util.macro;

import autismclient.util.PackUtilRegistryLabels;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.List;

public class WaitForEntityAction implements MacroAction {
    public enum CheckMode { RADIUS, LOOKING_AT, WITHIN_REACH }

    public List<String> entityIds = new ArrayList<>();

    public boolean mustBeLookingAt = false;
    public CheckMode checkMode = CheckMode.RADIUS;

    public boolean centerOnPlayer = true;

    public double radius = 6.0;

    public double x = 0, y = 0, z = 0;
    private boolean enabled = true;

    public WaitForEntityAction() {}

    @Override public void execute(Minecraft mc) {}

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", getType().name());
        ListTag list = new ListTag();
        for (String id : entityIds) list.add(StringTag.valueOf(id));
        tag.put("entityIds", list);
        tag.putBoolean("mustBeLookingAt", mustBeLookingAt);
        tag.putBoolean("centerOnPlayer", centerOnPlayer);
        tag.putDouble("radius", radius);
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putBoolean("enabled", enabled);
        tag.putString("checkMode", checkMode.name());
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        entityIds.clear();
        if (tag.contains("entityIds")) {
            ListTag list = tag.getList("entityIds").orElse(new ListTag());
            for (Tag el : list) {
                String s = el.asString().orElse("");
                if (!s.isEmpty()) entityIds.add(s);
            }
        } else if (tag.contains("entityName")) {

            String oldName = tag.getStringOr("entityName", "");
            if (!oldName.isEmpty()) {

                String tryId = "minecraft:" + oldName.toLowerCase().replace(" ", "_");
                try {
                    net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.parse(tryId);
                    if (net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                        entityIds.add(tryId);
                    }
                } catch (Exception ignored) {}
            }
        }
        mustBeLookingAt = tag.getBooleanOr("mustBeLookingAt", false);
        if (tag.contains("checkMode")) {
            try { checkMode = CheckMode.valueOf(tag.getStringOr("checkMode", "RADIUS")); } catch (Exception e) { checkMode = CheckMode.RADIUS; }
        } else {
            checkMode = mustBeLookingAt ? CheckMode.LOOKING_AT : CheckMode.RADIUS;
        }
        centerOnPlayer = tag.getBooleanOr("centerOnPlayer", true);
        radius = tag.getDoubleOr("radius", 6.0);
        x = tag.getDoubleOr("x", 0);
        y = tag.getDoubleOr("y", 0);
        z = tag.getDoubleOr("z", 0);
        if (tag.contains("enabled")) enabled = tag.getBooleanOr("enabled", true);
    }

    @Override public MacroActionType getType() { return MacroActionType.WAIT_ENTITY; }

    @Override
    public String getDisplayName() {
        String first;
        if (entityIds.isEmpty()) {
            first = "any";
        } else {
            String e0 = entityIds.get(0);
            if (e0.startsWith("~")) {

                String[] p = e0.split("~", 4);
                String display = p.length >= 4 ? p[3] : "";
                String type = p.length >= 3 ? PackUtilRegistryLabels.entity(p[2]) : "?";
                first = "SPEC " + (display == null || display.isBlank() ? type : display);
            } else {
                first = PackUtilRegistryLabels.entity(e0);
            }
            if (entityIds.size() > 1) first += " (+" + (entityIds.size() - 1) + ")";
        }
        String modeStr = switch (checkMode) {
            case RADIUS -> "r=" + (int) radius;
            case LOOKING_AT -> "look";
            case WITHIN_REACH -> "reach";
        };
        return "Wait Entity: " + first + " (" + modeStr + ")";
    }

    @Override public String getIcon() { return "ENT"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }
}
