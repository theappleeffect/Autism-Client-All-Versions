package autismclient.mixin;

import autismclient.modules.PackUtilModule;
import autismclient.util.PackUtilPacketCapture;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.PacketEncoder;
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

@Mixin(PacketEncoder.class)
public abstract class PackUtilPacketEncoderMixin<T extends PacketListener> {
    @Shadow @Final private ProtocolInfo<T> protocolInfo;

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V",
        at = @At("TAIL"), require = 0)
    private void packutil$captureEncodedPlaintext(ChannelHandlerContext ctx, Packet<T> packet, ByteBuf output, CallbackInfo ci) {
        if (!packutil$packetHooksActive()) return;
        PackUtilPacketCapture.capturePlaintext(packet, "C2S", protocolInfo.id().id(), packet.type(), output);
    }

    @Unique
    private static boolean packutil$packetHooksActive() {
        PackUtilModule module = PackUtilModule.get();
        return module != null && (module.arePacketHooksActive() || module.isPassivePayloadCaptureActive());
    }
}
