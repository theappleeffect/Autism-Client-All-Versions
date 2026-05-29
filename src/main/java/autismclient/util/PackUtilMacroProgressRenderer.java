package autismclient.util;

import autismclient.gui.packui.PackUiHudPanelRenderer;
import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import autismclient.util.macro.MacroAction;
import autismclient.util.macro.MacroExecutor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class PackUtilMacroProgressRenderer {
    private static final PackUiTheme THEME = new PackUiTheme();
    private static final long CACHE_REFRESH_NANOS = 50_000_000L;
    private static final int STEP_DONE_COLOR = 0xFF555555;
    private static final int STEP_NOW_COLOR = 0xFF6FD38B;
    private static final int STEP_WAIT_COLOR = 0xFFC6C6C6;
    private static final Identifier MUTED_FONT = THEME.fontFor(PackUiTone.MUTED);
    private static final Identifier BODY_FONT = THEME.fontFor(PackUiTone.BODY);
    private static final Identifier LABEL_FONT = THEME.fontFor(PackUiTone.LABEL);
    private static final int MUTED_COLOR = THEME.color(PackUiTone.MUTED);
    private static final int HEADER_COLOR = THEME.color(PackUiTone.BODY);

    private static String cachedRunKey = "";
    private static int cachedCurrent = Integer.MIN_VALUE;
    private static int cachedTotal = Integer.MIN_VALUE;
    private static int cachedScroll = Integer.MIN_VALUE;
    private static int cachedVisibleRows = Integer.MIN_VALUE;
    private static int cachedPanelWidth = Integer.MIN_VALUE;
    private static String cachedMacroName = null;
    private static String cachedTitle = "";
    private static List<PackUiHudPanelRenderer.Row> cachedRows = List.of();
    private static long lastCacheBuildNanos;

    private PackUtilMacroProgressRenderer() {
    }

    public static void render(GuiGraphics context, Font textRenderer, int x, int y, int width, int maxLines) {
        if (textRenderer == null || !MacroExecutor.isRunning()) return;

        List<MacroExecutor.MacroRunSnapshot> runs = MacroExecutor.getActiveRunSnapshots();
        if (runs.isEmpty()) {
            clearCache();
            return;
        }

        MacroExecutor.MacroRunSnapshot primary = runs.get(0);
        PackUtilMacro macro = primary.macro();
        if (macro == null || macro.actions == null || macro.actions.isEmpty()) {
            clearCache();
            return;
        }

        int total = Math.max(primary.totalSteps(), macro.actions.size());
        int current = Math.max(0, primary.currentStepIndex());
        int rowsPerRun = Math.max(2, maxLines / Math.max(1, runs.size()));
        int visibleRows = Math.min(rowsPerRun, total);
        int scroll = total > rowsPerRun ? Math.max(0, Math.min(current - rowsPerRun / 2, total - rowsPerRun)) : 0;
        int panelWidth = width + 12;
        long now = System.nanoTime();
        String runKey = buildRunKey(runs, rowsPerRun);
        if (shouldRebuildCache(runKey, current, total, scroll, visibleRows, panelWidth, now)) {
            rebuildCache(textRenderer, runs, rowsPerRun, panelWidth, now);
        }

        PackUiHudPanelRenderer.renderPreTrimmed(context, textRenderer, x - 6, y - 8, panelWidth, cachedTitle, cachedRows, THEME.headerAccent());
    }

    private static String buildRunKey(List<MacroExecutor.MacroRunSnapshot> runs, int rowsPerRun) {
        StringBuilder key = new StringBuilder();
        key.append(rowsPerRun).append('|');
        for (MacroExecutor.MacroRunSnapshot run : runs) {
            key.append(run.name()).append(':')
                .append(run.currentStepIndex()).append('/')
                .append(run.totalSteps()).append('/')
                .append(run.lastCompletedStep()).append('|');
        }
        return key.toString();
    }

    private static boolean shouldRebuildCache(String runKey, int current, int total, int scroll, int visibleRows, int panelWidth, long now) {
        boolean layoutChanged = !java.util.Objects.equals(cachedRunKey, runKey)
            || cachedTotal != total
            || cachedVisibleRows != visibleRows
            || cachedPanelWidth != panelWidth;
        if (layoutChanged) {
            return true;
        }
        if (cachedRows.isEmpty()) {
            return true;
        }
        if (cachedCurrent == current && cachedScroll == scroll) {
            return false;
        }
        return now - lastCacheBuildNanos >= CACHE_REFRESH_NANOS;
    }

    private static void rebuildCache(Font textRenderer, List<MacroExecutor.MacroRunSnapshot> runs, int rowsPerRun, int panelWidth, long now) {
        int contentWidth = panelWidth - (6 * 2);
        ArrayList<PackUiHudPanelRenderer.Row> rows = new ArrayList<>();

        for (MacroExecutor.MacroRunSnapshot run : runs) {
            PackUtilMacro macro = run.macro();
            if (macro == null || macro.actions == null || macro.actions.isEmpty()) continue;
            List<MacroAction> actions = macro.actions;
            int total = Math.max(run.totalSteps(), actions.size());
            int current = Math.max(0, run.currentStepIndex());
            int visibleRows = Math.min(rowsPerRun, total);
            int scroll = total > rowsPerRun ? Math.max(0, Math.min(current - rowsPerRun / 2, total - rowsPerRun)) : 0;
            rows.add(new PackUiHudPanelRenderer.Row(
                PackUiText.trimToWidth(textRenderer, run.name() + "  " + Math.min(current + 1, total) + "/" + total, contentWidth, LABEL_FONT, HEADER_COLOR),
                LABEL_FONT,
                HEADER_COLOR
            ));
            for (int i = scroll; i < scroll + visibleRows && i < total && i < actions.size(); i++) {
                MacroAction action = actions.get(i);
                int color = i < run.lastCompletedStep() ? STEP_DONE_COLOR : (i == current ? STEP_NOW_COLOR : STEP_WAIT_COLOR);
                rows.add(new PackUiHudPanelRenderer.Row(
                    PackUiText.trimToWidth(textRenderer, (i + 1) + ". " + action.getDisplayName(), contentWidth, BODY_FONT, color),
                    BODY_FONT,
                    color
                ));
            }
            if (run.status() != null && !run.status().isBlank()) {
                rows.add(new PackUiHudPanelRenderer.Row(
                    PackUiText.trimToWidth(textRenderer, run.status(), contentWidth, MUTED_FONT, MUTED_COLOR),
                    MUTED_FONT,
                    MUTED_COLOR
                ));
            }
        }

        MacroExecutor.MacroRunSnapshot primary = runs.get(0);
        cachedRunKey = buildRunKey(runs, rowsPerRun);
        cachedCurrent = primary.currentStepIndex();
        cachedTotal = primary.totalSteps();
        cachedScroll = 0;
        cachedVisibleRows = rowsPerRun;
        cachedPanelWidth = panelWidth;
        cachedMacroName = runs.size() == 1 ? primary.name() : runs.size() + " macros";
        cachedTitle = PackUiText.trimToWidth(textRenderer, runs.size() == 1 ? "MACRO" : "MACROS", contentWidth, LABEL_FONT, HEADER_COLOR);
        cachedRows = rows;
        lastCacheBuildNanos = now;
    }

    private static void clearCache() {
        cachedRunKey = "";
        cachedCurrent = Integer.MIN_VALUE;
        cachedTotal = Integer.MIN_VALUE;
        cachedScroll = Integer.MIN_VALUE;
        cachedVisibleRows = Integer.MIN_VALUE;
        cachedPanelWidth = Integer.MIN_VALUE;
        cachedMacroName = null;
        cachedTitle = "";
        cachedRows = List.of();
        lastCacheBuildNanos = 0L;
    }
}
