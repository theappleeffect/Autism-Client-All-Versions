package autismclient.mixin;

import autismclient.modules.PackUtilModule;
import autismclient.util.PackUtilPacketCapture;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PacketDecoder.class)
public abstract class PackUtilPacketDecoderMixin<T extends PacketListener> {
    @Shadow @Final private ProtocolInfo<T> protocolInfo;
    @Unique private byte[] packutil$incomingPlaintext = new byte[0];

    @Inject(method = "decode", at = @At("HEAD"))
    private void packutil$captureIncomingPlaintext(ChannelHandlerContext ctx, ByteBuf input, List<Object> out, CallbackInfo ci) {
        packutil$incomingPlaintext = packutil$packetHooksActive()
            ? PackUtilPacketCapture.copyReadableBytes(input)
            : new byte[0];
    }

    @Inject(method = "decode", at = @At("TAIL"))
    private void packutil$attachIncomingPlaintext(ChannelHandlerContext ctx, ByteBuf input, List<Object> out, CallbackInfo ci) {
        if (!packutil$packetHooksActive() || packutil$incomingPlaintext.length == 0 || out.isEmpty()) return;
        Object decoded = out.get(out.size() - 1);
        if (decoded instanceof Packet<?> packet) {
            PackUtilPacketCapture.capturePlaintext(packet, "S2C", protocolInfo.id().id(), packet.type(),
                Unpooled.wrappedBuffer(packutil$incomingPlaintext));
        }
    }

    @Unique
    private static boolean packutil$packetHooksActive() {
        PackUtilModule module = PackUtilModule.get();
        return module != null && (module.arePacketHooksActive() || module.isPassivePayloadCaptureActive());
    }
}
