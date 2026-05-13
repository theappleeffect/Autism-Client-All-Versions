package autismclient.util;

import org.jetbrains.annotations.Nullable;

public class PackUtilFabricatorRegistry {
    private static volatile PackUtilFabricatorOverlay activeOverlay = null;

    public static void setActiveOverlay(@Nullable PackUtilFabricatorOverlay overlay) {
        activeOverlay = overlay;
    }

    @Nullable
    public static PackUtilFabricatorOverlay getActiveOverlay() {
        return activeOverlay;
    }
}
