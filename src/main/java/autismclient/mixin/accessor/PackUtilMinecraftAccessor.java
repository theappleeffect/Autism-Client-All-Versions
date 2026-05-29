package autismclient.mixin.accessor;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.Services;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public interface PackUtilMinecraftAccessor {
    @Mutable
    @Accessor("user")
    void packutil$setUser(User user);

    @Mutable
    @Accessor("profileKeyPairManager")
    void packutil$setProfileKeyPairManager(ProfileKeyPairManager manager);

    @Mutable
    @Accessor("userApiService")
    void packutil$setUserApiService(UserApiService service);

    @Mutable
    @Accessor("skinManager")
    void packutil$setSkinManager(SkinManager manager);

    @Mutable
    @Accessor("playerSocialManager")
    void packutil$setPlayerSocialManager(PlayerSocialManager manager);

    @Mutable
    @Accessor("reportingContext")
    void packutil$setReportingContext(ReportingContext context);

    @Mutable
    @Accessor("profileFuture")
    void packutil$setProfileFuture(CompletableFuture<ProfileResult> future);

    // The client `services` bundle (net.minecraft.server.Services) field exists only from 1.21.9.
    //? if >=1.21.9 {
    @Mutable
    @Accessor("services")
    void packutil$setServices(Services services);
    //?}

    // Pre-1.21.9 the client holds the session service directly instead of a Services bundle.
    //? if <1.21.9 {
    /*@Mutable
    @Accessor("minecraftSessionService")
    void packutil$setMinecraftSessionService(com.mojang.authlib.minecraft.MinecraftSessionService service);
    *///?}
}
