package autismclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import autismclient.gui.packui.PackUiBannerRenderer;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTheme;
import autismclient.modules.PackUtilModule;
import autismclient.util.PackUtilMacroProgressRenderer;
import autismclient.util.PackUtilOverlayManager;
import autismclient.util.PackUtilQueueRenderer;
import autismclient.util.PackUtilServerInfoOverlay;
import autismclient.util.PackUtilSharedState;
import autismclient.util.PackUtilUiScale;
import autismclient.util.macro.MacroExecutor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;

@Mixin(Gui.class)
public abstract class PackUtilInGameHudMixin {
    @Unique private static final Minecraft MC = Minecraft.getInstance();
    @Unique private static final PackUiTheme PACKUI_THEME = new PackUiTheme();
    @Unique private static final int PACKUTIL_RIGHT_PANEL_Y = 20;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void yang$renderPackUtilQueue(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!isPackUtilActive()) return;
        MacroExecutor.onRender(1.0f);
        if (MC.options.hideGui) return;

        PackUtilSharedState shared = PackUtilSharedState.get();

        Runnable renderHudElements = () -> {
            boolean blockCap = shared.hasBlockCaptureCallback();
            boolean entityCap = shared.hasEntityCaptureCallback();
            boolean attackCap = shared.hasAttackCaptureCallback();
            boolean gbreakCap = shared.isGBreakCapturing();
            if (blockCap || entityCap || attackCap || gbreakCap) {
                int sw = PackUtilUiScale.getVirtualScreenWidth();
                int baseY = 8;
                String title = gbreakCap
                    ? "GBreak Capture"
                    : (blockCap ? "Block Capture" : (entityCap ? "Entity Capture" : "Position Capture"));
                String line1 = gbreakCap
                    ? "Break a block to capture the insta-break packet. Esc = cancel"
                    : (blockCap
                    ? "Right-click a block to capture it. Esc = cancel"
                    : (entityCap
                        ? "Right-click an entity to capture it. Esc = cancel"
                        : "Left-click to capture the target position. Esc = cancel"));
                String line2 = "";
                if (gbreakCap) {
                    line2 = "Waiting for the block-break packet from your next block break";
                } else if (blockCap && MC.hitResult != null
                        && MC.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                        && MC.level != null) {
                    net.minecraft.world.phys.BlockHitResult bhr = (net.minecraft.world.phys.BlockHitResult) MC.hitResult;
                    String bn = MC.level.getBlockState(bhr.getBlockPos()).getBlock().getName().getString();
                    net.minecraft.core.BlockPos bp = bhr.getBlockPos();
                    line2 = "\u00a77Aimed at: \u00a7f" + bn + " \u00a78(" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + ")";
                } else if (entityCap && MC.crosshairPickEntity != null && MC.crosshairPickEntity != MC.player) {
                    String eName = MC.crosshairPickEntity.getType().getDescription().getString();
                    String eId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(MC.crosshairPickEntity.getType()).toString();
                    line2 = "\u00a77Aimed at: \u00a7f" + eName + " \u00a78(" + eId + ")";
                }
                line2 = line2.replaceAll("\u00a7.", "");
                int boxWidth = Math.min(sw - 16, Math.max(270, Math.max(
                    autismclient.gui.packui.PackUiText.width(MC.font, title, PACKUI_THEME.fontFor(autismclient.gui.packui.PackUiTone.LABEL), PACKUI_THEME.color(autismclient.gui.packui.PackUiTone.BODY)),
                    Math.max(
                        autismclient.gui.packui.PackUiText.width(MC.font, line1, PACKUI_THEME.fontFor(autismclient.gui.packui.PackUiTone.BODY), PACKUI_THEME.color(autismclient.gui.packui.PackUiTone.BODY)),
                        line2.isEmpty() ? 0 : autismclient.gui.packui.PackUiText.width(MC.font, line2, PACKUI_THEME.fontFor(autismclient.gui.packui.PackUiTone.MUTED), PACKUI_THEME.color(autismclient.gui.packui.PackUiTone.MUTED))
                    )
                ) + 18));
                int boxX = (sw - boxWidth) / 2;
                PackUiBannerRenderer.drawFloatingBanner(context, MC.font, PACKUI_THEME, boxX, baseY, boxWidth, title, line1, line2, PACKUI_THEME.headerAccent());
            }

            boolean macroRunning = MacroExecutor.isRunning();
            boolean queueSending = shared.hasStaggeredPackets();
            boolean queueVisible = shared.shouldDelayGuiPackets()
                || shared.hasDelayedPackets()
                || queueSending;

            int screenWidth = PackUtilUiScale.getVirtualScreenWidth();
            int y = PACKUTIL_RIGHT_PANEL_Y;

            if (queueVisible) {
                int x = screenWidth - 170;
                PackUtilQueueRenderer.render(context, MC.font, x, y, 160, 8);
            }

            if (macroRunning) {
                int macroW = 170;
                int macroX;
                if (queueVisible) {

                    macroX = screenWidth - 176 - 5 - macroW - 6;
                } else {
                    macroX = screenWidth - macroW - 10;
                }
                PackUtilMacroProgressRenderer.render(context, MC.font, macroX, y, macroW, 10);
            }
        };

        PackUtilUiScale.pushOverlayScale(context);
        try {
            if (MC.screen != null && MC.screen.isPauseScreen()) {
                PackUiText.withVanillaRender(renderHudElements);
            } else {
                renderHudElements.run();
            }
        } finally {
            PackUtilUiScale.popOverlayScale(context);
        }

        if (MC.screen == null) {
            PackUtilServerInfoOverlay serverInfoOverlay = PackUtilModule.get().getServerDataOverlayIfExists();
            if (serverInfoOverlay != null && serverInfoOverlay.shouldRenderBackgroundProbeBanner()) {
                PackUtilUiScale.pushOverlayScale(context);
                try {
                    serverInfoOverlay.renderBackgroundProbeBanner(context);
                } finally {
                    PackUtilUiScale.popOverlayScale(context);
                }
            }

            PackUtilOverlayManager.get().renderAll(context, -1, -1, 0f);
        }
    }

    @Unique
    private boolean isPackUtilActive() {
        PackUtilModule module = PackUtilModule.get();
        return module != null && module.isActive();
    }
}
