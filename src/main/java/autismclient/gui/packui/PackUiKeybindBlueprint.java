package autismclient.gui.packui;

public final class PackUiKeybindBlueprint {
    private PackUiKeybindBlueprint() {
    }

    public static PackUiWindowNode buildPreview() {
        PackUiWindowNode window = new PackUiWindowNode("Settings");
        PackUiColumn content = window.content();
        content.add(new PackUiLabel("In-game only (no GUI open)", PackUiTone.MUTED));
        content.add(new PackUiSpacer(0, 4));

        addRow(content, "Load GUI", "None");
        addRow(content, "Flush Queue", "None");
        addRow(content, "Clear Queue", "None");
        addRow(content, "Toggle Logger", "None");
        addRow(content, "Toggle Send", "None");
        addRow(content, "Toggle Delay", "None");

        return window;
    }

    private static void addRow(PackUiColumn content, String label, String binding) {
        PackUiRow row = new PackUiRow().setGap(6);
        row.add(new PackUiLabel(label, PackUiTone.BODY).setGrowX(true));
        row.add(new PackUiButton(binding, PackUiButton.Variant.SECONDARY, () -> { }));
        row.add(new PackUiButton("X", PackUiButton.Variant.DANGER, () -> { }));
        content.add(row);
    }
}
