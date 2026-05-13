package autismclient.util;

import autismclient.AutismClientAddon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class PackUtilConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(AutismClientAddon.FOLDER, "config.json");
    private static PackUtilConfig globalInstance;

    public static PackUtilConfig getGlobal() {
        if (globalInstance == null) globalInstance = load();
        return globalInstance;
    }

    public static void setGlobal(PackUtilConfig config) {
        globalInstance = config;
    }

    public transient boolean sendGuiPackets = true;
    public transient boolean delayGuiPackets = false;
    public boolean useCustomPackets = false;
    public List<String> c2sPackets = new ArrayList<>();
    public List<String> s2cPackets = new ArrayList<>();
    public boolean allowSignEditing = true;
    public boolean autoDenyResourcePack = false;
    public boolean pretendPackAccepted = false;
    public boolean inventoryMove = false;
    public boolean xCarry = true;
    public boolean noPauseOnLostFocus = true;
    public boolean showItemIds = true;
    public boolean lanSyncEnabled = true;
    public boolean staggeredPacketSend = false;
    public int staggeredSendDelay = 1;
    public String executionPreset = "DEFAULT";
    public boolean packetBurstMode = true;
    public boolean useMsSleepMode = false;
    public int msSleepInterval = 5;
    public boolean instantExecutionMode = true;
    public int actionDelayUs = 0;
    public boolean useDirectFlush = true;
    public boolean forceChannelFlush = true;
    public boolean flushQueueOnDelayDisable = true;

    public int keybindLoadGui = org.lwjgl.glfw.GLFW.GLFW_KEY_V;
    public int keybindFlushQueue = -1;
    public int keybindClearQueue = -1;
    public int keybindToggleLogger = -1;
    public int keybindToggleSend = -1;
    public int keybindToggleDelay = -1;

    public boolean keybindInsideGui = false;

    public static PackUtilConfig load() {
        if (!FILE.exists()) return new PackUtilConfig();

        try (FileReader reader = new FileReader(FILE)) {
            PackUtilConfig loaded = GSON.fromJson(reader, PackUtilConfig.class);
            PackUtilConfig config = loaded != null ? loaded : new PackUtilConfig();
            config.applyRuntimeDefaults();
            return config;
        } catch (IOException e) {
            AutismClientAddon.LOG.error("Failed to load PackUtil config", e);
            return new PackUtilConfig();
        }
    }

    public void save() {
        FILE.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            AutismClientAddon.LOG.error("Failed to save PackUtil config", e);
        }
    }

    public void applyRuntimeDefaults() {
        sendGuiPackets = true;
        delayGuiPackets = false;
        staggeredPacketSend = false;
    }
}
