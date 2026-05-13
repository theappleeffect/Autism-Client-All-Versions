package autismclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

public class PackUtilPacketSender {

    public static void send(Packet<?> packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        mc.getConnection().send(packet);
    }

    public static void sendPacketDirect(Packet<?> packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        mc.getConnection().send(packet);
    }

}
