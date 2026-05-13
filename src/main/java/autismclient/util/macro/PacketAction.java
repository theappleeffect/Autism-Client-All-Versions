package autismclient.util.macro;

import autismclient.util.PackUtilClipboardHelper;
import autismclient.util.PacketRegenerator;
import autismclient.util.PackUtilClientMessaging;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;

public class PacketAction implements MacroAction {
    public String packetData = "";
    public boolean regenerate = true;
    public String description = "Packet";

    public PacketAction() {}

    public PacketAction(String packetData, boolean regenerate, String description) {
        this.packetData = packetData;
        this.regenerate = regenerate;
        this.description = description;
    }

    @Override
    public void execute(Minecraft mc) {
        if (mc.getConnection() == null) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cNo network connection!");
            return;
        }

        if (packetData.isEmpty()) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cPacket data is empty!");
            return;
        }

        try {
            Packet<?> packet = PackUtilClipboardHelper.deserializePacketFromBase64(packetData);
            if (packet == null) {
                PackUtilClientMessaging.sendPrefixed("Ã‚Â§cFailed to deserialize packet! Invalid data.");
                return;
            }

            if (regenerate) {
                Packet<?> regenerated = PacketRegenerator.regenerate(packet);
                if (regenerated == null) {
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§cFailed to regenerate packet: " + packet.getClass().getSimpleName());
                    return;
                }
                packet = regenerated;
            }

            mc.getConnection().send(packet);

        } catch (Exception e) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cPacket send error: " + e.getMessage());
            autismclient.AutismClientAddon.LOG.error("[MacroExecutor] Packet action failed", e);
        }
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", getType().name());
        tag.putString("packetData", packetData);
        tag.putBoolean("regenerate", regenerate);
        tag.putString("description", description);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        if (tag.contains("packetData")) packetData = tag.getStringOr("packetData", "");
        if (tag.contains("regenerate")) regenerate = tag.getBooleanOr("regenerate", true);
        if (tag.contains("description")) description = tag.getStringOr("description", "");
    }

    @Override
    public MacroActionType getType() {
        return MacroActionType.PACKET;
    }

    @Override
    public String getDisplayName() {
        return description.isEmpty() ? "Unknown Packet" : description;
    }

    @Override
    public String getIcon() {
        return "Pkt";
    }
}
