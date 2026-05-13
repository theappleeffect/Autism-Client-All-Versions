package autismclient.gui.screen;

import autismclient.gui.packui.PackUiOverlayButton;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import autismclient.util.PackUtilProxyManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class PackUtilProxyConfigScreen extends Screen {
    private static final PackUiTheme THEME = new PackUiTheme();
    private static final int BG = 0xFF121212;
    private static final int PANEL_BG = 0xFF1A1A1A;
    private static final int BORDER = 0xFF2A2A2A;
    private static final int TEXT = 0xFFEAEAEA;
    private static final int PANEL_WIDTH = 360;

    private final Screen parent;
    private final List<PackUiOverlayButton> buttons = new ArrayList<>();
    private final List<ConfigRow> rows = new ArrayList<>();

    public PackUtilProxyConfigScreen(Screen parent) {
        super(Component.literal("Proxy Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuildRows();
    }

    private void rebuildRows() {
        buttons.clear();
        rows.clear();
        int center = this.width / 2;
        int panelX = center - PANEL_WIDTH / 2;
        int y = 40;
        PackUtilProxyManager mgr = PackUtilProxyManager.get();

        rows.add(addConfigRow("Timeout (ms)", Integer.toString(mgr.getTimeoutMs()), (v) -> {
            try { mgr.setTimeoutMs(Integer.parseInt(v)); } catch (NumberFormatException ignored) {}
        }, panelX, y));
        y += 28;
        rows.add(addConfigRow("Threads", Integer.toString(mgr.getThreads()), (v) -> {
            try { mgr.setThreads(Integer.parseInt(v)); } catch (NumberFormatException ignored) {}
        }, panelX, y));
        y += 28;
        rows.add(addConfigRow("Retries", Integer.toString(mgr.getRetries()), (v) -> {
            try { mgr.setRetries(Integer.parseInt(v)); } catch (NumberFormatException ignored) {}
        }, panelX, y));
        y += 28;
        rows.add(addConfigRow("Prune latency (ms)", Integer.toString(mgr.getPruneLatency()), (v) -> {
            try { mgr.setPruneLatency(Integer.parseInt(v)); } catch (NumberFormatException ignored) {}
        }, panelX, y));
        y += 28;
        rows.add(addConfigRow("Prune to count", Integer.toString(mgr.getPruneToCount()), (v) -> {
            try { mgr.setPruneToCount(Integer.parseInt(v)); } catch (NumberFormatException ignored) {}
        }, panelX, y));
        y += 28;
        rows.add(addConfigRow("Sort by latency", Boolean.toString(mgr.isSortByLatency()), (v) -> {
            mgr.setSortByLatency(Boolean.parseBoolean(v));
        }, panelX, y));
        y += 28;
        rows.add(addConfigRow("Prune dead", Boolean.toString(mgr.isPruneDead()), (v) -> {
            mgr.setPruneDead(Boolean.parseBoolean(v));
        }, panelX, y));
        y += 36;

        buttons.add(PackUiOverlayButton.create(center - 50, y, 100, 20, Component.literal("Back"), b -> this.minecraft.setScreen(parent)));
    }

    private ConfigRow addConfigRow(String label, String value, java.util.function.Consumer<String> onChange, int panelX, int y) {
        EditBox field = new EditBox(this.font, panelX + PANEL_WIDTH - 150, y, 120, 20, Component.literal(label));
        field.setValue(value);
        field.setResponder(v -> onChange.accept(v));
        field.setMaxLength(16);
        this.addRenderableWidget(field);
        return new ConfigRow(label, field, y);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, BG);
        int center = this.width / 2;
        int panelX = center - PANEL_WIDTH / 2;
        drawPanel(graphics, panelX, 20, PANEL_WIDTH, rows.size() * 28 + 68);

        PackUiText.beginManagedLayer(graphics);
        try {
            drawText(graphics, this.title.getString(), center, 26, TEXT, true);

            for (ConfigRow row : rows) {
                drawText(graphics, row.label, panelX + 14, row.y + 4, TEXT, false);
            }

            for (PackUiOverlayButton button : buttons) {
                PackUiOverlayButton.renderStyled(graphics, this.font, button, mouseX, mouseY);
            }
        } finally {
            PackUiText.endManagedLayer(graphics);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        for (PackUiOverlayButton button : buttons) {
            if (PackUiOverlayButton.fireIfHit(button, event.x(), event.y(), event.button())) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL_BG);
        graphics.fill(x, y, x + w, y + 1, BORDER);
        graphics.fill(x, y + h - 1, x + w, y + h, BORDER);
        graphics.fill(x, y, x + 1, y + h, BORDER);
        graphics.fill(x + w - 1, y, x + w, y + h, BORDER);
    }

    private void drawText(GuiGraphicsExtractor graphics, String text, int x, int y, int color, boolean center) {
        Font renderer = this.font;
        Identifier font = THEME.fontFor(PackUiTone.BODY);
        int w = PackUiText.width(renderer, text, font, color);
        int drawX = center ? x - w / 2 : x;
        PackUiText.draw(graphics, renderer, text, font, color, drawX, y, false);
    }

    private static class ConfigRow {
        final String label;
        final EditBox field;
        final int y;

        ConfigRow(String label, EditBox field, int y) {
            this.label = label;
            this.field = field;
            this.y = y;
        }
    }
}
