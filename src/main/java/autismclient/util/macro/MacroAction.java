package autismclient.util.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public interface MacroAction {
    void execute(Minecraft mc);

    CompoundTag toTag();
    void fromTag(CompoundTag tag);

    MacroActionType getType();
    String getDisplayName();
    String getIcon();

    default boolean isEnabled() { return true; }

    default void setEnabled(boolean enabled) {}
}
