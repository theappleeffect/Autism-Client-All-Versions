package autismclient.util;

import net.minecraft.network.protocol.Packet;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Registry of vanilla play/common packet classes, split by direction.
 *
 * <p>Reconstructed for the public source drop, which referenced this class but never shipped it.
 * Discovery is done by scanning the Minecraft jar for {@code Serverbound*}/{@code Clientbound*}
 * packet classes using plain reflection, so it compiles unchanged on every supported version and
 * fails closed (empty sets) rather than crashing if the layout is unexpected.
 */
public final class PackUtilPacketRegistry {
    /** All discovered packet classes (serverbound + clientbound). */
    public static final Set<Class<? extends Packet<?>>> PACKETS;

    private static final Set<Class<? extends Packet<?>>> C2S = new LinkedHashSet<>();
    private static final Set<Class<? extends Packet<?>>> S2C = new LinkedHashSet<>();
    private static final Map<String, Class<? extends Packet<?>>> BY_NAME = new HashMap<>();

    static {
        try {
            discover();
        } catch (Throwable ignored) {
            // Fail closed: an empty registry keeps the mod loading.
        }
        PACKETS = Collections.unmodifiableSet(new LinkedHashSet<>(union()));
    }

    private PackUtilPacketRegistry() {
    }

    public static Set<Class<? extends Packet<?>>> getC2SPackets() {
        return C2S;
    }

    public static Set<Class<? extends Packet<?>>> getS2CPackets() {
        return S2C;
    }

    /** Registered (simple class) name for a packet class, or {@code null}. */
    public static String getName(Class<? extends Packet<?>> packetClass) {
        if (packetClass == null) return null;
        return packetClass.getSimpleName();
    }

    /** Packet class for a registered name, tolerant of suffix/casing differences, or {@code null}. */
    public static Class<? extends Packet<?>> getPacket(String name) {
        if (name == null || name.isEmpty()) return null;
        Class<? extends Packet<?>> direct = BY_NAME.get(name);
        if (direct != null) return direct;
        String key = normalize(name);
        for (Map.Entry<String, Class<? extends Packet<?>>> entry : BY_NAME.entrySet()) {
            if (normalize(entry.getKey()).equals(key)) return entry.getValue();
        }
        return null;
    }

    private static Set<Class<? extends Packet<?>>> union() {
        Set<Class<? extends Packet<?>>> all = new LinkedHashSet<>();
        all.addAll(C2S);
        all.addAll(S2C);
        return all;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    @SuppressWarnings("unchecked")
    private static void discover() throws Exception {
        URL location = Packet.class.getProtectionDomain().getCodeSource().getLocation();
        if (location == null) return;
        File file = new File(location.toURI());
        if (!file.isFile()) return;

        ClassLoader loader = Packet.class.getClassLoader();
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (!entryName.startsWith("net/minecraft/network/protocol/")) continue;
                if (!entryName.endsWith("Packet.class")) continue;

                String className = entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
                String simpleName = className.substring(className.lastIndexOf('.') + 1);
                boolean serverbound = simpleName.startsWith("Serverbound");
                boolean clientbound = simpleName.startsWith("Clientbound");
                if (!serverbound && !clientbound) continue;

                Class<?> clazz;
                try {
                    clazz = Class.forName(className, false, loader);
                } catch (Throwable ignored) {
                    continue;
                }
                if (!Packet.class.isAssignableFrom(clazz)) continue;
                if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;

                Class<? extends Packet<?>> packetClass = (Class<? extends Packet<?>>) clazz;
                BY_NAME.putIfAbsent(simpleName, packetClass);
                if (serverbound) {
                    C2S.add(packetClass);
                } else {
                    S2C.add(packetClass);
                }
            }
        }
    }
}
