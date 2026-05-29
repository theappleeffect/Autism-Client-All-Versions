package autismclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import autismclient.modules.PackUtilModule;
import autismclient.util.macro.MacroExecutor;
import autismclient.util.PackUtilClientMessaging;
import autismclient.util.PackUtilContainerTarget;
import autismclient.util.PackUtilSharedState;
import autismclient.AutismClientAddon;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.client.Minecraft;

@Mixin(Connection.class)
public abstract class PackUtilClientConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void yang$onSendPacket(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        PacketListener packetListener = ((Connection) (Object) this).getPacketListener();
        PackUtilModule module = PackUtilModule.get();
        boolean normalLoggerPath = module != null && module.arePacketHooksActive() && isPlayConnectionActive();
        if (module != null && (!normalLoggerPath || !module.isPacketLoggerCapturing())) {
            module.capturePassivePayloadPacket(packet, "C2S", packutil$protocolHint(packetListener));
        }
        if (!normalLoggerPath) return;

        if (packet instanceof ServerboundUseItemOnPacket pibp) {
            if (PackUtilSharedState.get().consumeBlockCaptureCallback(pibp.getHitResult().getBlockPos(), pibp.getHitResult().getDirection())) {
                ci.cancel();
                return;
            }
            PackUtilSharedState.get().setLastInteractedBlockPos(pibp.getHitResult().getBlockPos());
            PackUtilSharedState.get().setLastContainerTarget(PackUtilContainerTarget.forBlockHit(pibp.getHitResult(), pibp.getHand()));
        }

        if (packet instanceof ServerboundInteractPacket entityPacket) {
            net.minecraft.world.entity.Entity targeted = Minecraft.getInstance().crosshairPickEntity;
            if (targeted != null && targeted != Minecraft.getInstance().player) {
                net.minecraft.world.InteractionHand capturedHand = autismclient.util.PackUtilInteractPackets.hand(entityPacket);
                net.minecraft.world.phys.Vec3 capturedHitPos = autismclient.util.PackUtilInteractPackets.location(entityPacket);
                PackUtilSharedState.get().setLastContainerTarget(
                    capturedHitPos != null
                        ? PackUtilContainerTarget.forEntityAt(targeted, capturedHand, capturedHitPos)
                        : PackUtilContainerTarget.forEntity(targeted, capturedHand)
                );
            }
        }

        if (packet instanceof ServerboundInteractPacket && PackUtilSharedState.get().hasEntityCaptureCallback()) {
            net.minecraft.world.entity.Entity targeted = Minecraft.getInstance().crosshairPickEntity;
            if (targeted != null && targeted != Minecraft.getInstance().player) {
                PackUtilSharedState state = PackUtilSharedState.get();
                String payload;
                if (state.isEntityCaptureSpecific()) {

                    String uuid = targeted.getStringUUID();
                    String typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(targeted.getType()).toString();
                    String dispName = targeted.getDisplayName().getString().replaceAll("\u00a7.", "").trim();
                    payload = "~" + uuid + "~" + typeId + "~" + dispName;
                    state.setEntityCaptureSpecific(false);
                } else {
                    payload = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(targeted.getType()).toString();
                }
                state.consumeEntityCaptureCallback(payload);
            }
        }

        PackUtilSharedState shared = PackUtilSharedState.get();

        if (packet instanceof ServerboundContainerClosePacket && shared.consumeSuppressNextContainerClosePacket()) {
            ci.cancel();
            return;
        }

        if (packet instanceof ServerboundSignUpdatePacket && shared.consumeSuppressNextSignUpdatePacket()) {
            ci.cancel();
            return;
        }

        if (packet instanceof ServerboundEditBookPacket && shared.consumeSuppressNextBookEditPacket()) {
            ci.cancel();
            return;
        }

        boolean forceBookOrSignPacket =
            packet instanceof ServerboundSignUpdatePacket && shared.consumeForceNextSignUpdatePacket()
                || packet instanceof ServerboundEditBookPacket && shared.consumeForceNextBookEditPacket();

        if (packet instanceof ServerboundSignUpdatePacket && !shared.shouldEditSigns()) {
            shared.setAllowSignEditing(true);
            if (!forceBookOrSignPacket) {
                ci.cancel();
                return;
            }
        }

        if (packet instanceof ServerboundEditBookPacket && !shared.shouldUpdateBook()) {
            shared.setAllowBookUpdate(true);
            if (!forceBookOrSignPacket) {
                ci.cancel();
                return;
            }
        }

        if (packet instanceof ServerboundContainerClosePacket closePacket && packutil$shouldKeepXCarryOpen(shared, closePacket)) {
            ci.cancel();
            return;
        }

        if (shared.isGBreakCapturing()) {
            if (packet instanceof ServerboundPlayerActionPacket) {

                shared.onGBreakPacket(packet);
            }

            return;
        }

        if (PackUtilModule.get().handlePacketSend(packet)) {
            ci.cancel();
            return;
        }

        if (forceBookOrSignPacket) return;

        boolean anyFeatureActive = shared.shouldDelayGuiPackets()
            || !shared.shouldSendGuiPackets()
            || shared.shouldUseCustomPackets();
        if (!anyFeatureActive) return;

        if (shared.isFlushing()) return;

        boolean shouldHandle = false;

        if (shared.shouldUseCustomPackets()) {

            shouldHandle = shared.getC2SPackets().contains(packet.getClass());
        } else {

            shouldHandle = isGuiPacket(packet);
        }

        if (!shouldHandle) return;

        AutismClientAddon.LOG.debug("[PackUtil] Packet detected: {} | Send={} Delay={} | Custom={}",
            packet.getClass().getSimpleName(), shared.shouldSendGuiPackets(),
            shared.shouldDelayGuiPackets(), shared.shouldUseCustomPackets());

        if (!shared.shouldSendGuiPackets()) {
            AutismClientAddon.LOG.debug("[PackUtil] CANCELLED packet (send disabled)");
            ci.cancel();
            return;
        }

        if (shared.shouldDelayGuiPackets()) {
            AutismClientAddon.LOG.debug("[PackUtil] QUEUED packet (delay enabled)");
            shared.enqueuePacket(packet);
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("TAIL"), require = 0)
    private void yang$afterSendPacket(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        if (!isPackUtilActive()) return;
        if (!isPlayConnectionActive()) return;
        MacroExecutor.onPacketSent(packet);
    }

    @Unique
    private boolean isGuiPacket(Packet<?> packet) {
        return packet instanceof ServerboundContainerClickPacket
            || packet instanceof ServerboundContainerButtonClickPacket
            || packet instanceof ServerboundSetCreativeModeSlotPacket
            || packet instanceof ServerboundPlayerActionPacket
            || packet instanceof ServerboundUseItemPacket
            || packet instanceof ServerboundSignUpdatePacket
            || packet instanceof ServerboundEditBookPacket
            || packet instanceof ServerboundChatPacket
            || packet instanceof ServerboundChatCommandPacket
            || packet instanceof ServerboundChatCommandSignedPacket;
    }

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true, require = 0)
    private void yang$onReceivePacket(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        PacketListener listener = ((Connection) (Object) this).getPacketListener();
        PackUtilModule module = PackUtilModule.get();
        boolean normalLoggerPath = module != null && module.arePacketHooksActive() && isPlayReceiveListener(listener);
        if (module != null && (!normalLoggerPath || !module.isPacketLoggerCapturing())) {
            module.capturePassivePayloadPacket(packet, "S2C", packutil$protocolHint(listener));
        }
        if (!normalLoggerPath) return;

        autismclient.util.macro.ServerTickTracker.onS2CPacket(packet);
        MacroExecutor.onPacketReceived(packet);

        if (module.handlePacketReceive(packet)) {
            ci.cancel();
            return;
        }

        PackUtilSharedState shared = PackUtilSharedState.get();

        boolean anyFeatureActive = shared.shouldDelayGuiPackets()
            || !shared.shouldSendGuiPackets()
            || shared.shouldUseCustomPackets();
        if (!anyFeatureActive) return;

        boolean shouldHandle = false;

        if (shared.shouldUseCustomPackets()) {

            shouldHandle = shared.getS2CPackets().contains(packet.getClass());
        }

        if (!shouldHandle) return;

        AutismClientAddon.LOG.debug("[PackUtil] S2C Packet detected: {} | Send={} Delay={}",
            packet.getClass().getSimpleName(), shared.shouldSendGuiPackets(),
            shared.shouldDelayGuiPackets());

        if (!shared.shouldSendGuiPackets()) {
            ci.cancel();
            return;
        }

        if (shared.shouldDelayGuiPackets()) {
            shared.enqueuePacket(packet);
            ci.cancel();
        }
    }

    @Unique
    private boolean isPackUtilActive() {
        PackUtilModule module = PackUtilModule.get();
        return module != null && module.arePacketHooksActive();
    }

    @Unique
    private boolean isPlayConnectionActive() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.getConnection() != null;
    }

    @Unique
    private static boolean isPlayReceiveListener(PacketListener listener) {
        return listener instanceof ClientGamePacketListener;
    }

    @Unique
    private static String packutil$protocolHint(PacketListener listener) {
        if (listener == null) return "";
        if (listener instanceof ClientGamePacketListener) return "play";
        String name = listener.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        if (name.contains("configuration")) return "configuration";
        return "";
    }

    @Unique
    private boolean packutil$shouldKeepXCarryOpen(PackUtilSharedState shared, ServerboundContainerClosePacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return false;
        PackUtilModule module = PackUtilModule.get();
        boolean allowPassiveXCarry = module != null && module.isXCarryEnabled();
        if (!allowPassiveXCarry && !shared.isXCarryForced()) return false;
        if (packet.getContainerId() != client.player.inventoryMenu.containerId) return false;

        boolean hasItems = autismclient.util.macro.XCarryAction.hasStoredItems(client.player.inventoryMenu, true);

        shared.setXCarryActive(hasItems);
        shared.setXCarryForced(hasItems && shared.isXCarryForced());
        return hasItems;
    }
}
