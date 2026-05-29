package autismclient.util;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ClickType;

/**
 * Pre-1.21.5 packet compatibility shim.
 *
 * <p>1.21.5 changed {@link ServerboundContainerClickPacket}: the carried item became a hashed
 * {@code HashedStack} and the constructor argument order changed (changed-slots map then carried,
 * vs. carried then changed-slots on 1.21.4). Stonecutter rewrites the {@code new
 * ServerboundContainerClickPacket(...)} call sites to this factory on older versions (see the
 * {@code current.parsed < "1.21.5"} block in stonecutter.gradle.kts).
 *
 * <p>The pre-1.21.5 overload takes {@code Object} for the changed/carried args (the mod only ever
 * passes empty values there) and rebuilds the packet with the 1.21.4 order/types. The constructor
 * is written with the fully-qualified name so the simple-form replace rule does not rewrite it.
 */
public final class PackUtilCompat {
    private PackUtilCompat() {
    }

    // Stand-ins for HashedStack.EMPTY / HashedStack.class on pre-1.21.5 (HashedStack didn't exist).
    // Only referenced where the value is ignored (click()'s carried arg) or in a dead type-dispatch
    // branch, so null / Void.class are fine. Unique names keep the Stonecutter reverse unambiguous.
    public static final Object EMPTY = null;
    public static final Class<?> HASHED_CLASS = Void.class;

    //? if >=1.21.5 {
    public static ServerboundContainerClickPacket click(int syncId, int revision, short slot, byte button,
            ClickType type, it.unimi.dsi.fastutil.ints.Int2ObjectMap<net.minecraft.network.HashedStack> changed,
            net.minecraft.network.HashedStack carried) {
        return new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(
            syncId, revision, slot, button, type, changed, carried);
    }
    //?} else {
    /*public static ServerboundContainerClickPacket click(int syncId, int revision, short slot, byte button,
            ClickType type, Object changed, Object carried) {
        return new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(
            syncId, revision, slot, button, type, net.minecraft.world.item.ItemStack.EMPTY,
            new it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap<>());
    }
    *///?}
}
