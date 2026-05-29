package autismclient.mixin;

import autismclient.gui.PackUtilLoadingOverlay;
import autismclient.gui.macro.editor.ActionEditorOverlay;
import autismclient.gui.screen.PackUtilTitleScreen;
import autismclient.modules.PackUtilModule;
import autismclient.util.PackUtilSharedState;
import autismclient.util.PackUtilWindowBranding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(Minecraft.class)
public class PackUtilMinecraftClientMixin {
    @Unique
    private static final String PACKUTIL_WINDOW_TITLE = PackUtilWindowBranding.WINDOW_TITLE;

    @Unique
    private boolean packutil$escapeWasDown;
    @Unique
    private boolean packutil$inventoryWasDown;

    @Redirect(
        method = "*",
        at = @At(value = "NEW", target = "net/minecraft/client/gui/screens/LoadingOverlay")
    )
    private static LoadingOverlay packutil$replaceLoadingOverlay(
        Minecraft minecraft, ReloadInstance reload,
        Consumer<Optional<Throwable>> onFinish, boolean fadeIn
    ) {
        return new PackUtilLoadingOverlay(minecraft, reload, onFinish, fadeIn);
    }

    @Inject(method = "createTitle", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$createCustomWindowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(PACKUTIL_WINDOW_TITLE);
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$replaceTitleScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof TitleScreen) {
            ((Minecraft) (Object) this).setScreen(new PackUtilTitleScreen());
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void packutil$onTickHead(CallbackInfo ci) {
        PackUtilSharedState.get().onClientTickStart();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (PackUtilSharedState.get().hasAttackCaptureCallback()) {
            PackUtilSharedState.get().consumeAttackCaptureCallback();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$cancelCaptureOnEscape(CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        if (client.getWindow() == null) return;

        boolean escapeDown = GLFW.glfwGetKey(client.getWindow().handle(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
        boolean justPressed = escapeDown && !packutil$escapeWasDown;
        packutil$escapeWasDown = escapeDown;
        boolean inventoryDown = client.options != null && client.options.keyInventory.isDown();
        boolean inventoryJustPressed = inventoryDown && !packutil$inventoryWasDown;
        packutil$inventoryWasDown = inventoryDown;

        if (justPressed || inventoryJustPressed) {
            if (PackUtilSharedState.get().consumeCaptureCancelCallback()) {
                ci.cancel();
                return;
            }

            ActionEditorOverlay actionEditor = ActionEditorOverlay.getSharedOverlayIfExists();
            if (actionEditor != null && actionEditor.cancelCaptureIfActive()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true, require = 0)
    private void packutil$cancelLostFocusPause(boolean suppressPauseMenuIfWeReallyArePausing, CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        if (!client.isWindowActive()) {
            PackUtilModule module = PackUtilModule.get();
            if (module != null && module.isActive() && module.isNoPauseOnLostFocus()) {
                ci.cancel();
            }
        }
    }
}
