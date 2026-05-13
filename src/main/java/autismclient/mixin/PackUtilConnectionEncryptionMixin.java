package autismclient.mixin;

import autismclient.modules.PackUtilModule;
import autismclient.util.PackUtilPacketCapture;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.Cipher;

@Mixin(Connection.class)
public abstract class PackUtilConnectionEncryptionMixin {
    @Shadow private Channel channel;

    @Inject(method = "setEncryptionKey", at = @At("TAIL"))
    private void packutil$observeEncryptionBoundary(Cipher decryptCipher, Cipher encryptCipher, CallbackInfo ci) {
        if (channel == null) return;
        PackUtilPacketCapture.markEncryptionEnabled(channel);
        try {
            if (channel.pipeline().get("packutil_ciphertext_in") == null && channel.pipeline().get("decrypt") != null) {
                channel.pipeline().addBefore("decrypt", "packutil_ciphertext_in", new CiphertextTap("S2C"));
            }
            if (channel.pipeline().get("packutil_ciphertext_out") == null && channel.pipeline().get("encrypt") != null) {
                channel.pipeline().addBefore("encrypt", "packutil_ciphertext_out", new CiphertextTap("C2S"));
            }
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static boolean packutil$packetHooksActive() {
        PackUtilModule module = PackUtilModule.get();
        return module != null && module.arePacketHooksActive();
    }

    @Unique
    private static final class CiphertextTap extends ChannelDuplexHandler {
        private final String direction;

        private CiphertextTap(String direction) {
            this.direction = direction;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (packutil$packetHooksActive() && msg instanceof ByteBuf buf) {
                PackUtilPacketCapture.captureCiphertext(ctx.channel(), direction, buf);
            }
            super.channelRead(ctx, msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (packutil$packetHooksActive() && msg instanceof ByteBuf buf) {
                PackUtilPacketCapture.captureCiphertext(ctx.channel(), direction, buf);
            }
            super.write(ctx, msg, promise);
        }
    }
}
