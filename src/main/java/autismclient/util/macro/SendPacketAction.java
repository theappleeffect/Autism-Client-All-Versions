package autismclient.util.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import autismclient.util.PacketRegenerator;
import autismclient.util.PackUtilClipboardHelper;
import autismclient.util.PackUtilPacketNamer;
import autismclient.util.PackUtilPacketSender;
import autismclient.util.PackUtilSharedState;
import autismclient.util.PackUtilClientMessaging;
import java.util.ArrayList;
import java.util.List;

public class SendPacketAction implements MacroAction, WaitsForGui {
    public List<PackUtilSharedState.QueuedPacket> packets = new ArrayList<>();
    public String customName = "";
    public boolean waitForGui = false;
    public String guiName = "";

    public SendPacketAction() {}

    public List<PackUtilSharedState.QueuedPacket> getPackets() {
        return packets;
    }

    public void regeneratePackets() {
        List<PackUtilSharedState.QueuedPacket> newPackets = new ArrayList<>(packets.size());
        for (PackUtilSharedState.QueuedPacket qp : packets) {
            if (qp.packet != null) {
                Packet<?> regenerated = PacketRegenerator.regenerate(qp.packet);
                if (regenerated != null) {

                    newPackets.add(new PackUtilSharedState.QueuedPacket(regenerated, qp.getDelay(), qp.getId()));
                }

            }
        }
        packets.clear();
        packets.addAll(newPackets);
    }

    @Override
    public void execute(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            PackUtilClientMessaging.sendPrefixed("Â§cNo network connection!");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (PackUtilSharedState.QueuedPacket qp : packets) {
            if (qp.packet == null) {
                failCount++;
                continue;
            }

            try {
                PackUtilPacketSender.send(qp.packet);
                successCount++;
            } catch (Exception e) {
                PackUtilClientMessaging.sendPrefixed("Â§cSend failed: " + e.getMessage());
                failCount++;
            }
        }

        if (failCount > 0) {
            PackUtilClientMessaging.sendPrefixed(String.format("Â§eSent %d/%d packets (%d failed)",
                successCount, packets.size(), failCount));
        }
    }

    @Override public boolean isWaitForGui() { return waitForGui; }
    @Override public void setWaitForGui(boolean v) { this.waitForGui = v; }
    @Override public String getWaitGuiName() { return guiName; }
    @Override public void setWaitGuiName(String name) { this.guiName = name; }

    @Override
    public MacroActionType getType() {
        return MacroActionType.SEND_PACKET;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "SEND_PACKET");
        tag.putString("customName", customName);
        tag.putBoolean("waitForGui", waitForGui);
        tag.putString("guiName", guiName);

        ListTag packetList = new ListTag();
        for (PackUtilSharedState.QueuedPacket qp : packets) {
            CompoundTag packetTag = PackUtilClipboardHelper.serializeQueuedPacket(qp);
            if (packetTag != null) {
                packetList.add(packetTag);
            }
        }
        tag.put("packets", packetList);

        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        packets.clear();
        customName = tag.getStringOr("customName", "");
        if (tag.contains("waitForGui")) waitForGui = tag.getBooleanOr("waitForGui", false);
        if (tag.contains("guiName")) guiName = tag.getStringOr("guiName", "");
        if (tag.contains("packets")) {
            ListTag packetList = (ListTag) tag.get("packets");
            if (packetList != null) {
                for (int i = 0; i < packetList.size(); i++) {
                    Tag element = packetList.get(i);
                    if (element instanceof CompoundTag) {
                        PackUtilSharedState.QueuedPacket qp = PackUtilClipboardHelper.deserializeQueuedPacket((CompoundTag) element);
                        if (qp != null) {
                            packets.add(qp);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getDisplayName() {

        if (customName != null && !customName.trim().isEmpty()) {
            return "Send " + customName.trim();
        }

        if (packets.isEmpty()) {
            return "Send (empty)";
        }

        java.util.Map<String, Integer> nameCounts = new java.util.LinkedHashMap<>();
        for (PackUtilSharedState.QueuedPacket qp : packets) {
            if (qp.packet != null) {
                String name = PackUtilPacketNamer.getFriendlyName(qp.packet);
                nameCounts.merge(name, 1, Integer::sum);
            }
        }

        if (nameCounts.isEmpty()) {
            return "Send (empty)";
        }

        if (nameCounts.size() == 1) {
            java.util.Map.Entry<String, Integer> entry = nameCounts.entrySet().iterator().next();
            String pktName = entry.getKey();
            int count = entry.getValue();

            if (count == 1) {

                return "Send " + pktName;
            } else {

                return "Send " + pktName + " x" + count;
            }
        }

        return "Send Group (" + packets.size() + ")";
    }

    @Override
    public String getIcon() {
        return "P";
    }
}
