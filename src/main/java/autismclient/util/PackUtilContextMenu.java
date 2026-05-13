package autismclient.util;

import autismclient.gui.packui.PackUiText;
import autismclient.gui.packui.PackUiTheme;
import autismclient.gui.packui.PackUiTone;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class PackUtilContextMenu<T> {
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 2;
    private static final int BORDER = 1;

    private final PackUiTheme theme;
    private final Font font;
    private final java.util.function.Function<T, String[]> itemProvider;
    private final int lineHeight;

    private boolean open;
    private int x, y;
    private T target;

    public PackUtilContextMenu(PackUiTheme theme, Font font, java.util.function.Function<T, String[]> itemProvider, int lineHeight) {
        this.theme = theme;
        this.font = font;
        this.itemProvider = itemProvider;
        this.lineHeight = lineHeight;
    }

    public boolean isOpen() {
        return open && target != null;
    }

    public T target() {
        return target;
    }

    public void open(int mouseX, int mouseY, T target) {
        this.target = target;
        this.x = mouseX;
        this.y = mouseY;
        this.open = true;
        clampToScreen();
    }

    public void close() {
        this.open = false;
        this.target = null;
    }

    private String[] currentItems() {
        if (!open || target == null) return new String[0];
        String[] items = itemProvider.apply(target);
        return items == null ? new String[0] : items;
    }

    private int width(String[] items) {
        int maxW = 0;
        for (String s : items) {
            maxW = Math.max(maxW, PackUiText.width(font, s, theme.fontFor(PackUiTone.BODY), 0xFFFFFFFF));
        }
        return maxW + (PADDING_X * 2);
    }

    private int height(String[] items) {
        return items.length * lineHeight + (PADDING_Y * 2);
    }

    private void clampToScreen() {
        if (!open) return;
        String[] items = currentItems();
        int w = width(items);
        int h = height(items);
        int sw = Math.max(w + 4, PackUtilUiScale.getVirtualScreenWidth());
        int sh = Math.max(h + 4, PackUtilUiScale.getVirtualScreenHeight());
        x = Math.max(2, Math.min(x, sw - w - 2));
        y = Math.max(2, Math.min(y, sh - h - 2));
    }

    public boolean isMouseOver(double mx, double my) {
        if (!isOpen()) return false;
        String[] items = currentItems();
        int w = width(items);
        int h = height(items);
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void render(GuiGraphicsExtractor ctx, int mx, int my) {
        if (!isOpen()) return;
        clampToScreen();
        String[] items = currentItems();
        int w = width(items);
        int h = height(items);

        PackUiText.fill(ctx, x - BORDER, y - BORDER, x + w + BORDER, y + h + BORDER, PackUtilColors.primary());
        PackUiText.fill(ctx, x, y, x + w, y + h, PackUtilColors.popupBg());

        for (int i = 0; i < items.length; i++) {
            int iy = y + PADDING_Y + i * lineHeight;
            boolean hov = mx >= x && mx < x + w && my >= iy && my < iy + lineHeight;
            if (hov) PackUiText.fill(ctx, x + 1, iy, x + w - 1, iy + lineHeight, PackUtilColors.popupHover());
            PackUiText.draw(ctx, font, items[i], theme.fontFor(PackUiTone.BODY),
                hov ? 0xFFFFFFFF : PackUtilColors.textLight(),
                x + PADDING_X, iy + PADDING_Y, false);
        }
    }

    public boolean handleClick(double mouseX, double mouseY, int button, ItemPickedCallback<T> onPick) {
        if (!isOpen()) return false;
        if (button != 0) {
            close();
            return true;
        }

        String[] items = currentItems();
        int w = width(items);
        boolean inX = mouseX >= x && mouseX < x + w;
        if (inX) {
            for (int i = 0; i < items.length; i++) {
                int iy = y + PADDING_Y + i * lineHeight;
                if (mouseY >= iy && mouseY < iy + lineHeight) {
                    T t = target;
                    String label = items[i];
                    int idx = i;
                    close();
                    if (onPick != null) onPick.onPicked(t, label, idx);
                    return true;
                }
            }
        }

        close();
        return true;
    }

    @FunctionalInterface
    public interface ItemPickedCallback<T> {
        void onPicked(T target, String label, int index);
    }
}
