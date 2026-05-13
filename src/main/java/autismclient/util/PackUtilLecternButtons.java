package autismclient.util;

import autismclient.gui.packui.PackUiOverlayButton;
import autismclient.gui.packui.PackUiScreenButton;
import autismclient.modules.PackUtilModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class PackUtilLecternButtons {
    private PackUtilLecternButtons() {
    }

    public static List<PackUiScreenButton> build(Minecraft client, PackUtilQueueEditorOverlay queueEditorOverlay) {
        List<PackUiScreenButton> buttons = new ArrayList<>();
        PackUtilSharedState shared = PackUtilSharedState.get();
        int baseX = 5;
        int baseY = 5;
        int defaultWidth = 140;
        int defaultHeight = 20;

        buttons.add(new PackUiScreenButton(
            baseX, baseY, defaultWidth, defaultHeight,
            Component.literal("Close without packet"),
            PackUiOverlayButton.Variant.SECONDARY,
            () -> PackUtilGuiActions.closeCurrentScreen(client, false)
        ));

        buttons.add(new PackUiScreenButton(
            baseX, baseY + 24, defaultWidth, defaultHeight,
            Component.literal("De-sync"),
            PackUiOverlayButton.Variant.DANGER,
            () -> {
                if (!PackUtilGuiActions.desyncCurrentScreen(client)) {
                    PackUtilClientMessaging.sendPrefixed("Failed to desync: no open networked GUI.");
                }
            }
        ));

        buttons.add(new PackUiScreenButton(
            baseX, baseY + 48, defaultWidth, defaultHeight,
            Component.literal("Send packets: " + onOff(shared.shouldSendGuiPackets())),
            shared.shouldSendGuiPackets() ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.DANGER,
            () -> {
                boolean newValue = !shared.shouldSendGuiPackets();
                PackUtilModule.get().applySendGuiPacketsUiBehavior(newValue);
                PackUtilClientMessaging.sendPrefixed("GUI packets: " + stateText(newValue));
            }
        ));

        buttons.add(new PackUiScreenButton(
            baseX, baseY + 72, defaultWidth, defaultHeight,
            Component.literal("Delay packets: " + onOff(shared.shouldDelayGuiPackets())),
            shared.shouldDelayGuiPackets() ? PackUiOverlayButton.Variant.SUCCESS : PackUiOverlayButton.Variant.DANGER,
            () -> {
                boolean newValue = !shared.shouldDelayGuiPackets();
                PackUtilModule module = PackUtilModule.get();
                int sent = module.applyDelayGuiPacketsUiBehavior(newValue);
                if (newValue) {
                    PackUtilClientMessaging.sendPrefixed("Packet delay: enabled");
                } else if (sent > 0) {
                    String completionMessage = "Packet delay: disabled. Sent " + sent + " queued packets.";
                    if (shared.isStaggering()) shared.setPendingQueueCompletionMessage(completionMessage);
                    else PackUtilClientMessaging.sendPrefixed(completionMessage);
                } else if (!module.shouldFlushQueueOnDelayDisable()
                    && (!shared.getDelayedPackets().isEmpty() || !shared.getStaggeredQueue().isEmpty())) {
                    PackUtilClientMessaging.sendPrefixed("Packet delay: disabled. Queue kept.");
                } else {
                    PackUtilClientMessaging.sendPrefixed("Packet delay: disabled. Queue empty.");
                }
            }
        ));

        int thirdWidth = (defaultWidth - 10) / 3;
        buttons.add(new PackUiScreenButton(
            baseX, baseY + 96, thirdWidth, defaultHeight,
            Component.literal("Clear Q"),
            PackUiOverlayButton.Variant.DANGER,
            () -> {
                int count = PackUtilModule.get().clearQueuedPacketsUiBehavior();
                if (count > 0) {
                    PackUtilClientMessaging.sendPrefixed("Queue cleared (" + count + " packets).");
                } else {
                    PackUtilClientMessaging.sendPrefixed("Queue already empty.");
                }
            }
        ));

        buttons.add(new PackUiScreenButton(
            baseX + thirdWidth + 5, baseY + 96, thirdWidth, defaultHeight,
            Component.literal("Q Editor"),
            PackUiOverlayButton.Variant.SECONDARY,
            () -> {
                if (queueEditorOverlay != null) {
                    queueEditorOverlay.setVisible(!queueEditorOverlay.isVisible());
                }
            }
        ));

        buttons.add(new PackUiScreenButton(
            baseX + thirdWidth * 2 + 10, baseY + 96, thirdWidth, defaultHeight,
            Component.literal("Pkt Log"),
            PackUiOverlayButton.Variant.SECONDARY,
            () -> PackUtilModule.get().togglePacketLoggerUiBehavior()
        ));

        int halfWidth = (defaultWidth - 5) / 2;
        buttons.add(new PackUiScreenButton(
            baseX, baseY + 120, halfWidth, defaultHeight,
            Component.literal("Save GUI"),
            PackUiOverlayButton.Variant.SECONDARY,
            () -> PackUtilGuiActions.saveCurrentGui(client)
        ));

        buttons.add(new PackUiScreenButton(
            baseX + halfWidth + 5, baseY + 120, halfWidth, defaultHeight,
            Component.literal("Load GUI"),
            PackUiOverlayButton.Variant.SECONDARY,
            () -> {
                if (shared.getStoredScreen() == null || shared.getStoredAbstractContainerMenu() == null) {
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§cNo stored GUI.");
                    return;
                }
                client.execute(() -> {
                    client.setScreen(shared.getStoredScreen());
                    if (client.player != null) client.player.containerMenu = shared.getStoredAbstractContainerMenu();
                });
                PackUtilClientMessaging.sendPrefixed("GUI restored.");
            }
        ));

        buttons.add(new PackUiScreenButton(
            baseX, baseY + 144, defaultWidth + 45, defaultHeight,
            Component.literal("Disconnect and send packets"),
            PackUiOverlayButton.Variant.DANGER,
            () -> {
                PackUtilModule.get().setDelayGuiPackets(false);
                if (client.getConnection() != null) {
                    int sent = shared.flushDelayedPackets(client.getConnection());
                    client.getConnection().getConnection().disconnect(Component.literal("Disconnecting (PackUtil)"));
                    if (sent > 0) PackUtilClientMessaging.sendPrefixed("Sent " + sent + " packets and disconnected.");
                    else PackUtilClientMessaging.sendPrefixed("Disconnected (queue empty).");
                } else PackUtilClientMessaging.sendPrefixed("Ã‚Â§cFailed to disconnect: No network.");
            }
        ));

        return buttons;
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static String stateText(boolean value) {
        return value ? "enabled" : "disabled";
    }
}
