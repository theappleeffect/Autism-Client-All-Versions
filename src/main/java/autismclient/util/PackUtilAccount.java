package autismclient.util;

import com.mojang.authlib.Environment;
import com.mojang.util.UndashedUuid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import de.florianreuth.waybackauthlib.InvalidCredentialsException;
import de.florianreuth.waybackauthlib.WaybackAuthLib;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import java.util.Optional;
import java.util.Objects;

public class PackUtilAccount {
    private static final Environment ALTENING_ENVIRONMENT = new Environment("http://sessionserver.thealtening.com", "http://authserver.thealtening.com", "https://api.mojang.com", "The Altening");

    public PackUtilAccountType type = PackUtilAccountType.Cracked;
    public String label = "";
    public String token = "";
    public String username = "";
    public String uuid = "";

    public PackUtilAccount() {
    }

    public PackUtilAccount(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) fromTag(compoundTag);
    }

    public String displayName() {
        if (username != null && !username.isBlank()) return username;
        return label == null ? "" : label;
    }

    public boolean fetchInfo() {
        return switch (type) {
            case Cracked -> fetchCracked();
            case Session -> fetchSession();
            case Microsoft -> fetchMicrosoft();
            case TheAltening -> fetchTheAltening();
        };
    }

    public boolean login() {
        if ((username == null || username.isBlank()) && !fetchInfo()) return false;
        return switch (type) {
            case Cracked -> PackUtilAccountSessionSwitcher.setSession(new User(username, UUIDUtil.createOfflinePlayerUUID(username), "", Optional.empty(), Optional.empty()));
            case Session, Microsoft -> {
                if (token == null || token.isBlank() || uuid == null || uuid.isBlank()) yield false;
                yield PackUtilAccountSessionSwitcher.setSession(new User(username, UndashedUuid.fromStringLenient(uuid), token, Optional.empty(), Optional.empty()));
            }
            case TheAltening -> loginTheAltening();
        };
    }

    private boolean fetchCracked() {
        if (label == null || label.isBlank()) return false;
        username = label.trim();
        uuid = UUIDUtil.createOfflinePlayerUUID(username).toString();
        return true;
    }

    private boolean fetchSession() {
        if (token == null || token.isBlank()) return false;
        try {
            var profile = PackUtilHttp.getJson("https://api.minecraftservices.com/minecraft/profile", token);
            if (profile == null || !profile.has("id") || !profile.has("name")) return false;
            uuid = profile.get("id").getAsString();
            username = profile.get("name").getAsString();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean fetchMicrosoft() {
        if (label == null || label.isBlank()) return false;
        PackUtilMicrosoftLogin.LoginData data = PackUtilMicrosoftLogin.login(label);
        if (!data.isGood()) return false;
        token = data.mcToken;
        uuid = data.uuid;
        username = data.username;
        label = data.newRefreshToken;
        return true;
    }

    private boolean fetchTheAltening() {
        if (token == null || token.isBlank()) return false;
        try {
            WaybackAuthLib auth = new WaybackAuthLib(ALTENING_ENVIRONMENT.servicesHost());
            auth.setUsername(token);
            auth.setPassword("Meteor on Crack!");
            auth.logIn();
            username = auth.getCurrentProfile().name();
            uuid = auth.getCurrentProfile().id().toString();
            return true;
        } catch (InvalidCredentialsException e) {
            PackUtilClientMessaging.sendPrefixed("Invalid TheAltening credentials.");
            return false;
        } catch (Exception e) {
            PackUtilClientMessaging.sendPrefixed("Failed to fetch TheAltening account info.");
            return false;
        }
    }

    private boolean loginTheAltening() {
        if (token == null || token.isBlank() || username == null || username.isBlank() || uuid == null || uuid.isBlank()) return false;
        try {
            WaybackAuthLib auth = new WaybackAuthLib(ALTENING_ENVIRONMENT.servicesHost());
            auth.setUsername(token);
            auth.setPassword("Meteor on Crack!");
            auth.logIn();
            return PackUtilAccountSessionSwitcher.setSession(
                new User(auth.getCurrentProfile().name(), auth.getCurrentProfile().id(), auth.getAccessToken(), Optional.empty(), Optional.empty()),
                new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy(), ALTENING_ENVIRONMENT)
            );
        } catch (Exception e) {
            PackUtilClientMessaging.sendPrefixed("Failed to login with TheAltening.");
            return false;
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type == null ? PackUtilAccountType.Cracked.name() : type.name());
        tag.putString("label", label == null ? "" : label);
        tag.putString("token", token == null ? "" : token);
        tag.putString("username", username == null ? "" : username);
        tag.putString("uuid", uuid == null ? "" : uuid);
        return tag;
    }

    public PackUtilAccount fromTag(CompoundTag tag) {
        String typeName = tag.getStringOr("type", PackUtilAccountType.Cracked.name());
        try {
            type = PackUtilAccountType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            type = PackUtilAccountType.Cracked;
        }
        label = tag.getStringOr("label", tag.getStringOr("name", ""));
        token = tag.getStringOr("token", "");
        username = tag.getStringOr("username", "");
        uuid = tag.getStringOr("uuid", "");
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PackUtilAccount account)) return false;
        if (type != account.type) return false;
        return Objects.equals(identityKey(), account.identityKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, identityKey());
    }

    private String identityKey() {
        return switch (type) {
            case Cracked -> username != null && !username.isBlank() ? username : label;
            case Session, TheAltening -> token != null && !token.isBlank() ? token : label;
            case Microsoft -> label != null && !label.isBlank() ? label : username;
        };
    }
}
