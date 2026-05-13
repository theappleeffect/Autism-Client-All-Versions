package autismclient.util;

import autismclient.AutismClientAddon;
import autismclient.mixin.accessor.PackUtilMinecraftAccessor;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.Services;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class PackUtilAccountSessionSwitcher {
    private static User originalUser;

    private PackUtilAccountSessionSwitcher() {
    }

    public static User getOriginalUser() {
        if (originalUser == null) originalUser = Minecraft.getInstance().getUser();
        return originalUser;
    }

    public static boolean setSession(User user) {
        return setSession(user, new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy()));
    }

    public static boolean setSession(User user, YggdrasilAuthenticationService authService) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (originalUser == null) originalUser = mc.getUser();
            PackUtilMinecraftAccessor accessor = (PackUtilMinecraftAccessor) mc;
            Services services = Services.create(authService, mc.gameDirectory);
            UserApiService apiService = authService.createUserApiService(user.getAccessToken());
            Path skinCachePath = mc.gameDirectory.toPath().resolve("assets").resolve("skins");

            accessor.packutil$setServices(services);
            accessor.packutil$setUser(user);
            accessor.packutil$setUserApiService(apiService);
            accessor.packutil$setPlayerSocialManager(new PlayerSocialManager(mc, apiService));
            accessor.packutil$setProfileKeyPairManager(ProfileKeyPairManager.create(apiService, user, mc.gameDirectory.toPath()));
            accessor.packutil$setReportingContext(ReportingContext.create(ReportEnvironment.local(), apiService));
            accessor.packutil$setProfileFuture(CompletableFuture.supplyAsync(() -> mc.services().sessionService().fetchProfile(mc.getUser().getProfileId(), true), Util.nonCriticalIoPool()));
            accessor.packutil$setSkinManager(new SkinManager(skinCachePath, services, new SkinTextureDownloader(mc.getProxy(), mc.getTextureManager(), mc), mc));
            return true;
        } catch (Exception e) {
            AutismClientAddon.LOG.error("Failed to switch PackUtil account session", e);
            return false;
        }
    }
}
