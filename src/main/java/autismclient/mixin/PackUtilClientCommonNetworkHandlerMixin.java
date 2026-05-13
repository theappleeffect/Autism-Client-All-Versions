package autismclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import autismclient.modules.PackUtilModule;
import autismclient.util.PackUtilClientMessaging;
import autismclient.util.PackUtilSharedState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class PackUtilClientCommonNetworkHandlerMixin {
    @Shadow @Final protected Minecraft minecraft;

    @Shadow public abstract void send(Packet<?> packet);

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void yang$onResourcePackSend(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        if (!isPackUtilActive()) return;

        PackUtilSharedState shared = PackUtilSharedState.get();
        boolean shouldForceDeny = shared.shouldForceDenyResourcePack();
        boolean shouldBypass = shared.shouldBypassResourcePack();

        if (!(shouldForceDeny || (shouldBypass && packet.required()))) return;

        if (shouldBypass) {
            send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
            send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        } else {
            send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.DECLINED));
        }

        PackUtilClientMessaging.sendPrefixed("PackUtil denied server resource pack.");

        ci.cancel();
    }

    private boolean isPackUtilActive() {
        PackUtilModule module = PackUtilModule.get();
        return module != null && module.arePacketHooksActive();
    }
}
