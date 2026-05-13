package autismclient.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class PackUtilPacketCapture {
    private static final Map<Packet<?>, PacketSnapshot> PACKET_SNAPSHOTS =
        Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Channel, EncryptionSnapshot> ENCRYPTION_SNAPSHOTS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private PackUtilPacketCapture() {
    }

    public static void capturePlaintext(Packet<?> packet, String direction, String protocolPhase,
                                        PacketType<?> packetType, ByteBuf source) {
        if (packet == null || source == null) return;
        byte[] bytes = copyReadableBytes(source);
        int numericPacketId = readLeadingVarInt(bytes);
        PACKET_SNAPSHOTS.put(packet, new PacketSnapshot(
            direction,
            protocolPhase,
            packet.getClass().getName(),
            packetType == null ? "" : packetType.toString(),
            numericPacketId,
            bytes,
            System.currentTimeMillis()
        ));
    }

    public static PacketSnapshot snapshot(Packet<?> packet) {
        return packet == null ? null : PACKET_SNAPSHOTS.get(packet);
    }

    public static void markEncryptionEnabled(Channel channel) {
        if (channel == null) return;
        EncryptionSnapshot previous = ENCRYPTION_SNAPSHOTS.get(channel);
        ENCRYPTION_SNAPSHOTS.put(channel, previous == null
            ? EncryptionSnapshot.newEnabled()
            : previous.withEnabled(true));
    }

    public static void captureCiphertext(Channel channel, String direction, ByteBuf source) {
        if (channel == null || source == null) return;
        EncryptionSnapshot previous = ENCRYPTION_SNAPSHOTS.get(channel);
        if (previous == null) previous = EncryptionSnapshot.newEnabled();
        ENCRYPTION_SNAPSHOTS.put(channel, previous.withCiphertext(direction, copyReadableBytes(source)));
    }

    public static EncryptionSnapshot encryptionSnapshot(Channel channel) {
        return channel == null ? null : ENCRYPTION_SNAPSHOTS.get(channel);
    }

    public static byte[] copyReadableBytes(ByteBuf source) {
        if (source == null) return new byte[0];
        int length = Math.max(0, source.readableBytes());
        byte[] bytes = new byte[length];
        if (length > 0) {
            source.getBytes(source.readerIndex(), bytes);
        }
        return bytes;
    }

    public static String compactHex(byte[] bytes, int maxBytes) {
        return PackUtilPayloadSupport.toCompactHex(bytes, maxBytes);
    }

    private static int readLeadingVarInt(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return -1;
        int value = 0;
        int shift = 0;
        for (int i = 0; i < Math.min(5, bytes.length); i++) {
            int b = bytes[i] & 0xFF;
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return value;
            shift += 7;
        }
        return -1;
    }

    public record PacketSnapshot(String direction, String protocolPhase, String packetClassName,
                                 String packetType, int numericPacketId, byte[] plaintextBytes,
                                 long capturedAtMs) {
        public byte[] plaintextBytes() {
            return plaintextBytes == null ? new byte[0] : plaintextBytes.clone();
        }
    }

    public record EncryptionSnapshot(boolean enabled, long enabledAtMs, byte[] lastInboundCiphertext,
                                     byte[] lastOutboundCiphertext, long lastInboundAtMs,
                                     long lastOutboundAtMs) {
        static EncryptionSnapshot newEnabled() {
            return new EncryptionSnapshot(true, System.currentTimeMillis(), new byte[0], new byte[0], 0L, 0L);
        }

        EncryptionSnapshot withEnabled(boolean value) {
            return new EncryptionSnapshot(value, value && enabledAtMs == 0L ? System.currentTimeMillis() : enabledAtMs,
                lastInboundCiphertext, lastOutboundCiphertext, lastInboundAtMs, lastOutboundAtMs);
        }

        EncryptionSnapshot withCiphertext(String direction, byte[] bytes) {
            long now = System.currentTimeMillis();
            if ("C2S".equalsIgnoreCase(direction) || "OUT".equalsIgnoreCase(direction)) {
                return new EncryptionSnapshot(enabled, enabledAtMs, lastInboundCiphertext,
                    bytes == null ? new byte[0] : bytes.clone(), lastInboundAtMs, now);
            }
            return new EncryptionSnapshot(enabled, enabledAtMs, bytes == null ? new byte[0] : bytes.clone(),
                lastOutboundCiphertext, now, lastOutboundAtMs);
        }

        public byte[] lastInboundCiphertext() {
            return lastInboundCiphertext == null ? new byte[0] : lastInboundCiphertext.clone();
        }

        public byte[] lastOutboundCiphertext() {
            return lastOutboundCiphertext == null ? new byte[0] : lastOutboundCiphertext.clone();
        }
    }
}
