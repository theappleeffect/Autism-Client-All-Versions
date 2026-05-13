package autismclient.util.macro;

import autismclient.util.PackUtilClientMessaging;
import autismclient.util.PackUtilPayloadJsonSupport;
import autismclient.util.PackUtilPayloadScriptExecutor;
import autismclient.util.PackUtilPayloadSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public class PayloadAction implements MacroAction {
    public String channel = "minecraft:brand";
    public String payloadData = "";
    public String payloadJson = "";
    public String payloadClassName = "";
    public String javaSource = "";
    public boolean commandApiRecognized = false;
    public boolean commandApiOverride = false;
    public int commandApiValue = Integer.MAX_VALUE - 8;
    public String sourceDirection = "C2S";
    public String sourceProtocol = "";
    public boolean payloadScriptEnabled = false;
    private boolean enabled = true;

    @Override
    public void execute(Minecraft mc) {
        if (!enabled) return;
        if (mc == null || mc.getConnection() == null) {
            PackUtilClientMessaging.sendPrefixed("Â§cCannot send payload while disconnected.");
            return;
        }

        try {
            boolean hasRawPayload = payloadData != null && !payloadData.isBlank();
            boolean useJsonModel = !hasRawPayload
                && payloadJson != null && !payloadJson.isBlank()
                && payloadClassName != null && !payloadClassName.isBlank();
            String targetChannel = channel;
            byte[] rawBytes;
            if (useJsonModel) {
                PackUtilPayloadJsonSupport.EncodedPayload encoded = PackUtilPayloadJsonSupport.encodeAction(this);
                targetChannel = encoded.channel();
                rawBytes = encoded.bytes();
            } else {
                rawBytes = PackUtilPayloadSupport.parsePayloadBytes(payloadData);
            }
            if (!useJsonModel && !hasRawPayload && PackUtilPayloadSupport.isBrandChannel(targetChannel)) {
                rawBytes = PackUtilPayloadSupport.encodeMinecraftStringPayload(
                    PackUtilPayloadSupport.defaultBrandPayloadString());
            }
            if (!useJsonModel && commandApiRecognized && commandApiOverride) {
                rawBytes = PackUtilPayloadSupport.withCommandApiValue(rawBytes, commandApiValue);
            }
            PackUtilPayloadScriptExecutor.Context context = new PackUtilPayloadScriptExecutor.Context(targetChannel, rawBytes,
                !useJsonModel && commandApiRecognized && commandApiOverride ? commandApiValue : null);
            PackUtilPayloadScriptExecutor.ScriptResult result = PackUtilPayloadScriptExecutor.execute(
                payloadScriptEnabled ? javaSource : "", context);
            if (PackUtilPayloadSupport.sendPayload(result.channel(), result.bytes(), sourceProtocol)) {
                PackUtilClientMessaging.sendPrefixed("Sent payload: " + result.channel());
            }
        } catch (Exception e) {
            PackUtilClientMessaging.sendPrefixed("Â§cPayload action failed: " + PackUtilPayloadSupport.safeMessage(e));
        }
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", getType().name());
        tag.putBoolean("enabled", enabled);
        tag.putString("channel", channel == null ? "" : channel);
        tag.putString("payloadData", payloadData == null ? "" : payloadData);
        tag.putString("payloadJson", payloadJson == null ? "" : payloadJson);
        tag.putString("payloadClassName", payloadClassName == null ? "" : payloadClassName);
        tag.putString("javaSource", javaSource == null ? "" : javaSource);
        tag.putBoolean("payloadScriptEnabled", payloadScriptEnabled);
        tag.putBoolean("commandApiRecognized", commandApiRecognized);
        tag.putBoolean("commandApiOverride", commandApiOverride);
        tag.putInt("commandApiValue", commandApiValue);
        tag.putString("sourceDirection", sourceDirection == null ? "" : sourceDirection);
        tag.putString("sourceProtocol", sourceProtocol == null ? "" : sourceProtocol);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        enabled = tag.getBooleanOr("enabled", true);
        channel = tag.getStringOr("channel", "minecraft:brand");
        payloadData = tag.getStringOr("payloadData", "");
        payloadJson = tag.getStringOr("payloadJson", "");
        payloadClassName = tag.getStringOr("payloadClassName", "");
        javaSource = tag.getStringOr("javaSource", "");
        payloadScriptEnabled = tag.getBooleanOr("payloadScriptEnabled", false);
        commandApiRecognized = tag.getBooleanOr("commandApiRecognized", false);
        commandApiOverride = tag.getBooleanOr("commandApiOverride", false);
        commandApiValue = tag.getIntOr("commandApiValue", Integer.MAX_VALUE - 8);
        sourceDirection = tag.getStringOr("sourceDirection", "C2S");
        sourceProtocol = tag.getStringOr("sourceProtocol", "");
    }

    @Override
    public MacroActionType getType() {
        return MacroActionType.PAYLOAD;
    }

    @Override
    public String getDisplayName() {
        if (channel == null || channel.isBlank()) return "Payload";
        return "Payload Â· " + channel;
    }

    @Override
    public String getIcon() {
        return "network";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
