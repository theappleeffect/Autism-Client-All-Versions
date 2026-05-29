package autismclient.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

/**
 * Pre-1.21.5 NBT compatibility shim.
 *
 * <p>1.21.5 rewrote the {@link CompoundTag}/{@link ListTag} getters: the value getters now return
 * {@link Optional} and a {@code getXxxOr(key, default)} convenience family was added. On older
 * versions those don't exist, so Stonecutter rewrites the call sites to these statics (see the
 * {@code current.parsed < "1.21.5"} block in stonecutter.gradle.kts). This class is excluded from
 * compilation on 1.21.5+ (where the old String-returning getters were removed).
 *
 * <p>NOTE: parameters are named {@code t}/{@code l} (never {@code tag}) so the Stonecutter
 * receiver-based replace rules don't rewrite this shim's own internals.
 */
public final class PackUtilNbt {
    private PackUtilNbt() {
    }

    // ---- getXxxOr(key, default) convenience family ----
    public static String getStringOr(CompoundTag t, String key, String def) {
        return t != null && t.contains(key) ? t.getString(key) : def;
    }

    public static boolean getBooleanOr(CompoundTag t, String key, boolean def) {
        return t != null && t.contains(key) ? t.getBoolean(key) : def;
    }

    public static int getIntOr(CompoundTag t, String key, int def) {
        return t != null && t.contains(key) ? t.getInt(key) : def;
    }

    public static double getDoubleOr(CompoundTag t, String key, double def) {
        return t != null && t.contains(key) ? t.getDouble(key) : def;
    }

    public static float getFloatOr(CompoundTag t, String key, float def) {
        return t != null && t.contains(key) ? t.getFloat(key) : def;
    }

    public static long getLongOr(CompoundTag t, String key, long def) {
        return t != null && t.contains(key) ? t.getLong(key) : def;
    }

    public static short getShortOr(CompoundTag t, String key, short def) {
        return t != null && t.contains(key) ? t.getShort(key) : def;
    }

    public static byte getByteOr(CompoundTag t, String key, byte def) {
        return t != null && t.contains(key) ? t.getByte(key) : def;
    }

    // ---- array getters (Optional-returning on 1.21.5+) ----
    public static Optional<byte[]> getByteArray(CompoundTag t, String key) {
        return t != null && t.contains(key) ? Optional.of(t.getByteArray(key)) : Optional.empty();
    }

    public static Optional<int[]> getIntArray(CompoundTag t, String key) {
        return t != null && t.contains(key) ? Optional.of(t.getIntArray(key)) : Optional.empty();
    }

    public static Optional<long[]> getLongArray(CompoundTag t, String key) {
        return t != null && t.contains(key) ? Optional.of(t.getLongArray(key)) : Optional.empty();
    }

    public static ListTag getListOrEmpty(CompoundTag t, String key) {
        return t != null && t.get(key) instanceof ListTag list ? list : new ListTag();
    }

    // ---- Optional-returning getters ----
    public static Optional<ListTag> getList(CompoundTag t, String key) {
        return t != null && t.get(key) instanceof ListTag list ? Optional.of(list) : Optional.empty();
    }

    public static Optional<CompoundTag> getCompound(CompoundTag t, String key) {
        return t != null && t.get(key) instanceof CompoundTag compound ? Optional.of(compound) : Optional.empty();
    }

    public static Optional<String> getString(CompoundTag t, String key) {
        return t != null && t.contains(key) ? Optional.of(t.getString(key)) : Optional.empty();
    }

    public static Optional<String> getString(ListTag l, int index) {
        return l != null && index >= 0 && index < l.size() ? Optional.of(l.getString(index)) : Optional.empty();
    }

    // Tag.asString() returns Optional<String> on 1.21.5+; pre-1.21.5 use getAsString() (always a String).
    public static Optional<String> asString(Tag t) {
        return t == null ? Optional.empty() : Optional.of(t.getAsString());
    }
}
