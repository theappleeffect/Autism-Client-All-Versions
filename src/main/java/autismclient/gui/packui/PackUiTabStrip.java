package autismclient.gui.packui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class PackUiTabStrip extends PackUiContainer {
    private final PackUiRow row = new PackUiRow().setGap(2);
    private final List<PackUiButton> buttons = new ArrayList<>();
    private String[] tabs = new String[0];
    private int activeIndex = 0;
    private IntConsumer onSelect;

    public PackUiTabStrip() {
        add(row);
    }

    public PackUiTabStrip setTabs(String... tabs) {
        this.tabs = tabs == null ? new String[0] : tabs.clone();
        rebuildButtons();
        return this;
    }

    public PackUiTabStrip setActiveIndex(int activeIndex) {
        this.activeIndex = Math.max(0, Math.min(Math.max(0, tabs.length - 1), activeIndex));
        refreshButtonVariants();
        return this;
    }

    public PackUiTabStrip setOnSelect(IntConsumer onSelect) {
        this.onSelect = onSelect;
        rebuildButtons();
        return this;
    }

    @Override
    public float preferredHeight(PackUiRenderContext context, float availableWidth) {
        return row.preferredHeight(context, availableWidth);
    }

    @Override
    protected void layoutChildren(PackUiRenderContext context) {
        row.setBounds(x, y, width, row.preferredHeight(context, width));
    }

    private void rebuildButtons() {
        row.clearChildren();
        buttons.clear();
        for (int i = 0; i < tabs.length; i++) {
            final int tabIndex = i;
            PackUiButton button = new PackUiButton(tabs[i], i == activeIndex ? PackUiButton.Variant.PRIMARY : PackUiButton.Variant.GHOST, () -> {
                setActiveIndex(tabIndex);
                if (onSelect != null) onSelect.accept(tabIndex);
            }).setGrowX(true).setTone(PackUiTone.LABEL);
            buttons.add(button);
            row.add(button);
        }
    }

    private void refreshButtonVariants() {
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setVariant(i == activeIndex ? PackUiButton.Variant.PRIMARY : PackUiButton.Variant.GHOST);
        }
    }
}
