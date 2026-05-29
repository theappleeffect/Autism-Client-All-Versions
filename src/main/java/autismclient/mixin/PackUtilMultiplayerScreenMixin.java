package autismclient.mixin;

import autismclient.gui.packui.PackUiOverlayButton;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import autismclient.gui.screen.PackUtilAccountsScreen;
import autismclient.gui.screen.PackUtilProxiesScreen;
import autismclient.util.PackUtilUiScale;
import autismclient.util.PackUtilProxy;
import autismclient.util.PackUtilProxyManager;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JoinMultiplayerScreen.class, priority = 2000)
public abstract class PackUtilMultiplayerScreenMixin extends Screen {
    @Unique
    private static final int PACKUTIL_BUTTON_WIDTH = 75;
    @Unique
    private static final int PACKUTIL_BUTTON_HEIGHT = 20;
    @Unique
    private static final int PACKUTIL_MARGIN = 3;
    @Unique
    private static final int PACKUTIL_GAP = 2;
    @Unique
    private static final int TEXT_COLOR_WHITE = 0xFFFFFFFF;
    @Unique
    private static final int TEXT_COLOR_MUTED = 0xFFAFAFAF;
    @Unique
    private static final int BUTTON_FILL = 0x66120E11;
    @Unique
    private static final int BUTTON_FILL_HOVER = 0x8A1E0E10;
    @Unique
    private static final int BUTTON_BORDER = 0xAA6B2020;
    @Unique
    private static final int BUTTON_BORDER_HOVER = 0xFFFF6464;

    @Unique
    private static final PackUiTheme THEME = new PackUiTheme();

    @Unique
    private PackUiOverlayButton packutil$accountsButton;
    @Unique
    private PackUiOverlayButton packutil$proxiesButton;
    @Unique
    private boolean packutil$afterExtractRegistered;

    protected PackUtilMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "repositionElements", at = @At("TAIL"), require = 0)
    private void packutil$repositionElements(CallbackInfo ci) {
        packutil$suppressMeteorWidgets();
        packutil$layoutButtons();
        packutil$afterExtractRegistered = false;
        packutil$registerAfterExtract();
    }

    @Unique
    private void packutil$registerAfterExtract() {
        packutil$suppressMeteorWidgets();
        packutil$layoutButtons();
        if (packutil$afterExtractRegistered) return;
        packutil$afterExtractRegistered = true;
        ScreenEvents.afterRender(this).register((screen, graphics, mouseX, mouseY, tickDelta) -> packutil$renderAfterExtract(graphics, mouseX, mouseY, tickDelta));
    }

    @Unique
    private void packutil$layoutButtons() {
        int screenWidth = Math.max(this.width, PackUtilUiScale.getVirtualScreenWidth());
        int x1 = screenWidth - PACKUTIL_MARGIN - PACKUTIL_BUTTON_WIDTH;
        int x2 = x1 - PACKUTIL_GAP - PACKUTIL_BUTTON_WIDTH;
        int y = PACKUTIL_MARGIN;

        packutil$accountsButton = PackUiOverlayButton.create(x1, y, PACKUTIL_BUTTON_WIDTH, PACKUTIL_BUTTON_HEIGHT,
            Component.literal("Accounts"), b -> this.minecraft.setScreen(new PackUtilAccountsScreen(this)));
        packutil$proxiesButton = PackUiOverlayButton.create(x2, y, PACKUTIL_BUTTON_WIDTH, PACKUTIL_BUTTON_HEIGHT,
            Component.literal("Proxies"), b -> this.minecraft.setScreen(new PackUtilProxiesScreen(this)));
    }

    @Unique
    private void packutil$suppressMeteorWidgets() {
        if (!FabricLoader.getInstance().isModLoaded("meteor-client")) return;
        packutil$disableMeteorMultiplayerUiConfig();
        for (GuiEventListener child : this.children()) {
            if (child instanceof Button button) {
                String msg = button.getMessage().getString();
                if ("Accounts".equals(msg) || "Proxies".equals(msg)) {
                    button.visible = false;
                    button.active = false;
                }
            }
        }
    }

    @Unique
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void packutil$disableMeteorMultiplayerUiConfig() {
        try {
            Class<?> configClass = Class.forName("meteordevelopment.meteorclient.systems.config.Config");
            Object config = configClass.getMethod("get").invoke(null);
            Class<?> buttonPositionClass = Class.forName("meteordevelopment.meteorclient.systems.config.Config$ButtonPosition");
            Object hidden = Enum.valueOf((Class<? extends Enum>) buttonPositionClass.asSubclass(Enum.class), "Hidden");
            packutil$setMeteorSetting(configClass.getField("accountButtonAnchor").get(config), hidden);
            packutil$setMeteorSetting(configClass.getField("proxiesButtonAnchor").get(config), hidden);
            packutil$setMeteorSetting(configClass.getField("showAccountStatus").get(config), false);
            packutil$setMeteorSetting(configClass.getField("showProxiesStatus").get(config), false);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Unique
    private static void packutil$setMeteorSetting(Object setting, Object value) throws ReflectiveOperationException {
        setting.getClass().getSuperclass().getMethod("set", Object.class).invoke(setting, value);
    }

    @Unique
    private void packutil$renderAfterExtract(GuiGraphics graphics, int mouseX, int mouseY, float deltaTicks) {
        packutil$suppressMeteorWidgets();
        packutil$layoutButtons();

        Font renderer = this.font;
        Identifier fontId = THEME.fontFor(PackUiTone.BODY);
        int x = PACKUTIL_MARGIN;
        int y = PACKUTIL_MARGIN;
        int virtualMouseX = PackUtilUiScale.toVirtualInt(mouseX);
        int virtualMouseY = PackUtilUiScale.toVirtualInt(mouseY);

        PackUtilUiScale.pushOverlayScale(graphics);
        PackUiText.beginManagedLayer(graphics);
        try {
            String loggedInAs = "Logged in as ";
            String username = this.minecraft.getUser().getName();
            int lineY = y;
            PackUiText.draw(graphics, renderer, loggedInAs, fontId, TEXT_COLOR_WHITE, x, lineY, false);
            int loggedInAsWidth = PackUiText.width(renderer, loggedInAs, fontId, TEXT_COLOR_WHITE);
            PackUiText.draw(graphics, renderer, username, fontId, TEXT_COLOR_MUTED, x + loggedInAsWidth, lineY, false);
            lineY += PackUiText.fontHeight(fontId) + 3;

            PackUtilProxy proxy = PackUtilProxyManager.get().getEnabled();
            String left = proxy != null ? "Using proxy " : "Not using a proxy";
            String right = proxy != null ? (proxy.name != null && !proxy.name.isEmpty() ? "(" + proxy.name + ") " : "") + proxy.address + ":" + proxy.port : null;
            PackUiText.draw(graphics, renderer, left, fontId, TEXT_COLOR_WHITE, x, lineY, false);
            int leftWidth = PackUiText.width(renderer, left, fontId, TEXT_COLOR_WHITE);
            if (right != null) {
                PackUiText.draw(graphics, renderer, right, fontId, TEXT_COLOR_MUTED, x + leftWidth, lineY, false);
            }

            if (packutil$accountsButton != null) {
                packutil$renderCornerButton(graphics, renderer, packutil$accountsButton, virtualMouseX, virtualMouseY);
            }
            if (packutil$proxiesButton != null) {
                packutil$renderCornerButton(graphics, renderer, packutil$proxiesButton, virtualMouseX, virtualMouseY);
            }
        } finally {
            PackUiText.endManagedLayer(graphics);
            PackUtilUiScale.popOverlayScale(graphics);
        }
    }

    @Unique
    private static void packutil$renderCornerButton(GuiGraphics graphics, Font renderer, PackUiOverlayButton button, int mouseX, int mouseY) {
        int x = button.getX();
        int y = button.getY();
        int w = button.getWidth();
        int h = button.getHeight();
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int fill = hovered ? BUTTON_FILL_HOVER : BUTTON_FILL;
        int border = hovered ? BUTTON_BORDER_HOVER : BUTTON_BORDER;
        int text = hovered ? 0xFFFFF4F4 : THEME.color(PackUiTone.BODY);
        Identifier font = THEME.fontFor(PackUiTone.BODY);
        String label = button.getMessage().getString();
        String display = PackUiText.trimToWidth(renderer, label, Math.max(1, w - 8), font, text);
        int textWidth = PackUiText.width(renderer, display, font, text);
        int textX = x + Math.max(2, (w - textWidth) / 2);
        int textY = y + Math.max(1, (h - PackUiText.fontHeight(font)) / 2);

        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w, y + 1, border);
        graphics.fill(x, y + h - 1, x + w, y + h, border);
        graphics.fill(x, y, x + 1, y + h, border);
        graphics.fill(x + w - 1, y, x + w, y + h, border);
        PackUiText.draw(graphics, renderer, display, font, text, textX, textY, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        double virtualX = PackUtilUiScale.toVirtual(event.x());
        double virtualY = PackUtilUiScale.toVirtual(event.y());
        if (packutil$accountsButton != null && PackUiOverlayButton.fireIfHit(packutil$accountsButton, virtualX, virtualY, event.button())) return true;
        if (packutil$proxiesButton != null && PackUiOverlayButton.fireIfHit(packutil$proxiesButton, virtualX, virtualY, event.button())) return true;
        return super.mouseClicked(event, doubleClick);
    }
}
