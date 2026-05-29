package autismclient.mixin;

import autismclient.modules.PackUtilModule;
import autismclient.util.PackUtilClientMessaging;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class PackUtilChatScreenMixin {
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true, require = 0)
    private void yang$onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        if (!"^togglepackutil".equalsIgnoreCase(message.trim())) return;

        PackUtilModule module = PackUtilModule.get();

        module.toggle();

        Minecraft mc = Minecraft.getInstance();
        PackUtilClientMessaging.sendPrefixed("PackUtil is now " + (module.isActive() ? "enabled" : "disabled") + ".");

        mc.commandHistory().addCommand(message);
        mc.setScreen(null);
        ci.cancel();
    }
}
