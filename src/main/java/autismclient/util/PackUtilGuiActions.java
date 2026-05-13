package autismclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

public final class PackUtilGuiActions {
    private PackUtilGuiActions() {
    }

    public static boolean saveCurrentGui(Minecraft mc) {
        return saveCurrentGui(mc, true);
    }

    public static boolean saveCurrentGui(Minecraft mc, boolean notify) {
        if (mc == null || mc.screen == null || mc.player == null) {
            if (notify) {
                PackUtilClientMessaging.sendPrefixed("Failed to store GUI.");
            }
            return false;
        }

        PackUtilSharedState.get().storeScreen(mc.screen, mc.player.containerMenu);
        if (notify) {
            PackUtilClientMessaging.sendPrefixed(savedGuiMessage());
        }
        return true;
    }

    private static String savedGuiMessage() {
        int keyCode = PackUtilConfig.getGlobal().keybindLoadGui;
        if (keyCode == -1) return "GUI stored.";
        return "GUI stored. Press \u00a7a\u00a7l" + PackUtilKeybindOverlay.getKeyName(keyCode) + "\u00a7r to restore.";
    }

    public static boolean closeCurrentScreen(Minecraft mc, boolean sendPacket) {
        return closeCurrentScreen(mc, sendPacket, true);
    }

    public static boolean closeCurrentScreen(Minecraft mc, boolean sendPacket, boolean notify) {
        if (mc == null || mc.screen == null) return false;

        Screen screen = mc.screen;
        if (screen instanceof PackUtilSpecialGuiActions special) {
            if (sendPacket) special.packutil$closeWithPacket(notify);
            else special.packutil$closeWithoutPacket(notify);
            return true;
        }

        if (sendPacket) {
            if (mc.player != null
                && mc.player.containerMenu != null
                && mc.player.containerMenu != mc.player.inventoryMenu) {
                mc.player.closeContainer();
            } else {
                mc.setScreen(null);
            }
        } else {
            if (mc.player != null
                && mc.player.containerMenu != null
                && mc.player.containerMenu != mc.player.inventoryMenu) {
                PackUtilSharedState.get().setSuppressNextContainerClosePacket(true);
                mc.player.closeContainer();
                if (notify) {
                    PackUtilClientMessaging.sendPrefixed("GUI closed without packet.");
                }
            } else {
                mc.setScreen(null);
                if (notify) {
                    PackUtilClientMessaging.sendPrefixed("Screen closed locally.");
                }
            }
        }
        return true;
    }

    public static boolean desyncCurrentScreen(Minecraft mc) {
        return desyncCurrentScreen(mc, true);
    }

    public static boolean desyncCurrentScreen(Minecraft mc, boolean notify) {
        if (mc == null || mc.screen == null) return false;

        Screen screen = mc.screen;
        if (screen instanceof PackUtilSpecialGuiActions special) {
            special.packutil$desync(notify);
            return true;
        }

        if (mc.getConnection() == null || mc.player == null || mc.player.containerMenu == null) {
            return false;
        }

        mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
        if (notify) {
            PackUtilClientMessaging.sendPrefixed("GUI desynced: close packet sent while client screen stays open.");
        }
        return true;
    }
}
