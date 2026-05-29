package autismclient.util;

import autismclient.mixin.accessor.PackUtilInteractPacketAccessor;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Helpers for reading and rebuilding {@link ServerboundInteractPacket}.
 *
 * <p>The packet exposes no public accessors for its hand/position/target; the data lives behind a
 * private {@code Action} visited via {@link ServerboundInteractPacket#dispatch}, and the target id is
 * a private field reached through {@link PackUtilInteractPacketAccessor}.
 */
public final class PackUtilInteractPackets {
    private PackUtilInteractPackets() {
    }

    public static int entityId(ServerboundInteractPacket packet) {
        return ((PackUtilInteractPacketAccessor) (Object) packet).packutil$getEntityId();
    }

    public static InteractionHand hand(ServerboundInteractPacket packet) {
        InteractionHand[] out = {InteractionHand.MAIN_HAND};
        packet.dispatch(new ServerboundInteractPacket.Handler() {
            @Override public void onInteraction(InteractionHand hand) { out[0] = hand; }
            @Override public void onInteraction(InteractionHand hand, Vec3 pos) { out[0] = hand; }
            @Override public void onAttack() { }
        });
        return out[0];
    }

    public static Vec3 location(ServerboundInteractPacket packet) {
        Vec3[] out = {null};
        packet.dispatch(new ServerboundInteractPacket.Handler() {
            @Override public void onInteraction(InteractionHand hand) { }
            @Override public void onInteraction(InteractionHand hand, Vec3 pos) { out[0] = pos; }
            @Override public void onAttack() { }
        });
        return out[0];
    }

    public static ServerboundInteractPacket recreate(Entity target, ServerboundInteractPacket original) {
        boolean secondary = original.isUsingSecondaryAction();
        ServerboundInteractPacket[] out = {ServerboundInteractPacket.createAttackPacket(target, secondary)};
        original.dispatch(new ServerboundInteractPacket.Handler() {
            @Override public void onInteraction(InteractionHand hand) {
                out[0] = ServerboundInteractPacket.createInteractionPacket(target, secondary, hand);
            }
            @Override public void onInteraction(InteractionHand hand, Vec3 pos) {
                out[0] = ServerboundInteractPacket.createInteractionPacket(target, secondary, hand, pos);
            }
            @Override public void onAttack() {
                out[0] = ServerboundInteractPacket.createAttackPacket(target, secondary);
            }
        });
        return out[0];
    }
}
