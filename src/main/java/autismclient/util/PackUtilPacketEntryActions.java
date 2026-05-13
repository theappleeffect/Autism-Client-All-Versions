package autismclient.util;

import autismclient.gui.macro.editor.ActionEditorOverlay;
import autismclient.util.macro.PayloadAction;
import autismclient.util.macro.SendPacketAction;
import autismclient.util.macro.WaitForPacketAction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

public final class PackUtilPacketEntryActions {
    private PackUtilPacketEntryActions() {
    }

    public static boolean canQueue(PackUtilPacketLoggerOverlay.LogEntry entry) {
        return entry != null && "C2S".equalsIgnoreCase(entry.direction) && entry.packetRef != null;
    }

    public static boolean canDirectSend(PackUtilPacketLoggerOverlay.LogEntry entry) {
        return canQueue(entry);
    }

    public static boolean directSend(PackUtilPacketLoggerOverlay.LogEntry entry) {
        if (!canDirectSend(entry)) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cOnly C2S packets can be sent.");
            return false;
        }

        if (entry.packetRef instanceof ServerboundCustomPayloadPacket) {
            return directSendPayload(entry);
        }

        Packet<?> regenerated = PacketRegenerator.regenerate(entry.packetRef);
        if (regenerated == null) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cCannot regenerate: " + entry.shortName);
            return false;
        }
        try {
            PackUtilPacketSender.send(regenerated);
            PackUtilClientMessaging.sendPrefixed("Sent: " + entry.shortName);
            return true;
        } catch (Exception e) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cSend failed: " + e.getMessage());
            return false;
        }
    }

    private static boolean directSendPayload(PackUtilPacketLoggerOverlay.LogEntry entry) {
        PackUtilPayloadSupport.PayloadSnapshot snapshot = PackUtilPayloadSupport.snapshotFromEntry(entry);
        if (snapshot == null || snapshot.channel() == null || snapshot.channel().isBlank()) {
            Packet<?> regenerated = PacketRegenerator.regenerate(entry.packetRef);
            if (regenerated != null) {
                try {
                    PackUtilPacketSender.send(regenerated);
                    PackUtilClientMessaging.sendPrefixed("Sent: " + entry.shortName);
                    return true;
                } catch (Exception e) {
                    PackUtilClientMessaging.sendPrefixed("\u00a7cSend failed: " + e.getMessage());
                    return false;
                }
            }
            PackUtilClientMessaging.sendPrefixed("\u00a7cCannot send: no payload data for " + entry.shortName);
            return false;
        }
        byte[] rawBytes = snapshot.rawBytes();
        if (!PackUtilPayloadSupport.sendPayload(snapshot.channel(), rawBytes, snapshot.protocolPhase())) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cFailed to send payload: " + entry.shortName);
            return false;
        }
        PackUtilClientMessaging.sendPrefixed("Sent payload: " + snapshot.channel());
        return true;
    }

    public static boolean canAddSendAction(PackUtilPacketLoggerOverlay.LogEntry entry) {
        return canQueue(entry);
    }

    public static boolean canAddWaitAction(PackUtilPacketLoggerOverlay.LogEntry entry) {
        return entry != null && entry.shortName != null && !entry.shortName.isBlank() && entry.direction != null && !entry.direction.isBlank();
    }

    public static boolean canEditPayload(PackUtilPacketLoggerOverlay.LogEntry entry) {
        return entry != null && entry.isPayload && "C2S".equalsIgnoreCase(entry.direction);
    }

    public static boolean canAddPayloadAction(PackUtilPacketLoggerOverlay.LogEntry entry) {
        return canEditPayload(entry);
    }

    public static boolean queue(PackUtilPacketLoggerOverlay.LogEntry entry) {
        if (!canQueue(entry)) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cOnly C2S packets can be queued.");
            return false;
        }

        PackUtilSharedState.get().enqueuePacket(entry.packetRef);
        PackUtilClientMessaging.sendPrefixed("Queued: " + entry.shortName);
        return true;
    }

    public static boolean addSendActionToVisibleMacro(PackUtilPacketLoggerOverlay.LogEntry entry) {
        if (!canAddSendAction(entry)) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cOnly C2S packets can be added as send actions.");
            return false;
        }

        PackUtilMacroEditorOverlay macroEditor = getOrOpenMacroEditor();
        if (macroEditor == null) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cCannot open the macro editor.");
            return false;
        }

        Packet<?> regenerated = PacketRegenerator.regenerate(entry.packetRef);
        if (regenerated == null) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cCannot regenerate: " + entry.shortName);
            return false;
        }

        SendPacketAction action = new SendPacketAction();
        action.waitForGui = false;
        action.guiName = "";
        action.packets.add(new PackUtilSharedState.QueuedPacket(regenerated, 0));
        macroEditor.addAction(action);
        PackUtilOverlayManager.get().bringToFront(macroEditor);
        PackUtilClientMessaging.sendPrefixed("Added send action: " + entry.shortName);
        return true;
    }

    public static boolean addWaitActionToVisibleMacro(PackUtilPacketLoggerOverlay.LogEntry entry) {
        if (!canAddWaitAction(entry)) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cCannot add this packet as a wait condition.");
            return false;
        }

        PackUtilMacroEditorOverlay macroEditor = getOrOpenMacroEditor();
        if (macroEditor == null) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cCannot open the macro editor.");
            return false;
        }

        String target = WaitForPacketAction.withDirection(entry.direction, entry.shortName);
        if (target.isEmpty()) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cCannot add this packet as a wait condition.");
            return false;
        }

        WaitForPacketAction action = new WaitForPacketAction(target);
        action.packetNames.add(target);
        macroEditor.addAction(action);
        PackUtilOverlayManager.get().bringToFront(macroEditor);
        PackUtilClientMessaging.sendPrefixed("Added wait condition: " + WaitForPacketAction.getDisplayLabel(target));
        return true;
    }

    public static boolean openPayloadEditor(PackUtilPacketLoggerOverlay.LogEntry entry) {
        if (!canEditPayload(entry)) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cOnly captured C2S custom payload packets can be edited.");
            return false;
        }

        PayloadAction action = PackUtilPayloadSupport.seedActionFromEntry(entry);
        if (action == null) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cFailed to seed payload editor from packet.");
            return false;
        }

        ActionEditorOverlay.getSharedOverlay().openStandalonePayloadEditor(action);
        PackUtilOverlayManager.get().bringToFront(ActionEditorOverlay.getSharedOverlay());
        return true;
    }

    public static boolean addPayloadActionToVisibleMacro(PackUtilPacketLoggerOverlay.LogEntry entry) {
        if (!canAddPayloadAction(entry)) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cOnly captured C2S custom payload packets can be added as payload actions.");
            return false;
        }

        PackUtilMacroEditorOverlay macroEditor = getOrOpenMacroEditor();
        if (macroEditor == null) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cCannot open the macro editor.");
            return false;
        }

        PayloadAction action = PackUtilPayloadSupport.seedActionFromEntry(entry);
        if (action == null) {
            PackUtilClientMessaging.sendPrefixed("\u00a7cFailed to create payload action from packet.");
            return false;
        }

        macroEditor.addAction(action);
        PackUtilOverlayManager.get().bringToFront(macroEditor);
        PackUtilClientMessaging.sendPrefixed("Added payload action: " + entry.shortName);
        return true;
    }

    public static boolean hasVisibleMacroEditor() {
        return findVisibleMacroEditor() != null;
    }

    private static PackUtilMacroEditorOverlay getOrOpenMacroEditor() {
        PackUtilMacroEditorOverlay macroEditor = findVisibleMacroEditor();
        if (macroEditor != null) {
            PackUtilOverlayManager.get().bringToFront(macroEditor);
            return macroEditor;
        }

            macroEditor = PackUtilMacroEditorOverlay.getSharedOverlay();
        if (macroEditor == null) return null;

        PackUtilMacro existingMacro = PackUtilSharedState.get().getEditingMacro();
        macroEditor.open(existingMacro);
        PackUtilOverlayManager.get().bringToFront(macroEditor);
        return macroEditor;
    }

    private static PackUtilMacroEditorOverlay findVisibleMacroEditor() {
        for (IPackUtilOverlay overlay : PackUtilOverlayManager.get().getOverlays()) {
            if (overlay instanceof PackUtilMacroEditorOverlay macroEditor && macroEditor.isVisible()) {
                return macroEditor;
            }
        }
        return null;
    }
}
