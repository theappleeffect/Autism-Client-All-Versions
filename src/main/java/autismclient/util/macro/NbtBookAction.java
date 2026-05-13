package autismclient.util.macro;

import autismclient.util.PackUtilClientMessaging;
import autismclient.util.PackUtilInventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.concurrent.ThreadLocalRandom;

public class NbtBookAction implements MacroAction {
    public int pages = 100;
    public String title = "EldenRingLore";
    public boolean onlyAscii = false;
    public String customComponent = "";
    public int delayTicks = 20;
    public int bookCount = 1;
    private boolean enabled = true;

    public transient String pasteInfo = "";

    private static final int MAX_PAGES = 100;
    private static final int LINES_PER_PAGE = 14;
    private static final float LINE_WIDTH_PX = 114f;

    public NbtBookAction() {}

    public boolean executeSingleBook(Minecraft mc, int bookIndex, int totalBooks) {
        if (mc.player == null || mc.getConnection() == null) {
            PackUtilClientMessaging.sendPrefixed("Â§cNo player or network connection!");
            return false;
        }

        int bookSlot = findWritableBook(mc);
        if (bookSlot < 0) {
            if (bookIndex == 0) {
                PackUtilClientMessaging.sendPrefixed("Â§cNo Book & Quill found in inventory!");
            } else {
                PackUtilClientMessaging.sendPrefixed("Â§eRan out of books after " + bookIndex + " signed.");
            }
            return false;
        }

        int hotbarSlot = bookSlot;
        if (bookSlot > 8) {
            int targetHotbar = 0;
            for (int i = 0; i <= 8; i++) {
                if (mc.player.getInventory().getItem(i).isEmpty()) {
                    targetHotbar = i;
                    break;
                }
            }
            PackUtilInventoryHelper.swapInventorySlots(mc, bookSlot, targetHotbar);
            hotbarSlot = targetHotbar;
        }

        PackUtilInventoryHelper.selectHotbarSlot(mc, hotbarSlot);

        int numPages = Math.min(MAX_PAGES, Math.max(1, pages));
        List<String> pageList;
        if (!customComponent.isEmpty()) {
            pageList = splitTextIntoPages(mc.font, customComponent, numPages);
        } else {
            pageList = generatePages(mc, numPages);
        }

        String bookTitle = title;
        if (totalBooks > 1 && bookIndex > 0) {
            bookTitle = title + " #" + (bookIndex + 1);
        }

        mc.getConnection().send(new ServerboundEditBookPacket(hotbarSlot, pageList, Optional.of(bookTitle)));

        int estimatedBytes = estimateBytes(pageList);
        PackUtilClientMessaging.sendPrefixed("Â§aSigned book " + (bookIndex + 1) + "/" + totalBooks +
            " (~" + (estimatedBytes / 1024) + "KB, " + pageList.size() + " pages)");
        return true;
    }

    @Override
    public void execute(Minecraft mc) {

        executeSingleBook(mc, 0, 1);
    }

    private int findWritableBook(Minecraft mc) {
        if (mc.player == null) return -1;
        Identifier writableBookId = Identifier.fromNamespaceAndPath("minecraft", "writable_book");
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(writableBookId)) {
                return i;
            }
        }
        return -1;
    }

    public static List<String> splitTextIntoPages(Font tr, String text, int maxPages) {
        if (text == null || text.isEmpty()) return new ArrayList<>(List.of(""));

        String[] paragraphs = text.split("\n", -1);
        List<String> visualLines = new ArrayList<>();
        for (String para : paragraphs) {
            if (para.isEmpty()) {
                visualLines.add("");
            } else {
                wrapParagraph(tr, para, visualLines);
            }
        }

        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        int linesOnPage = 0;
        for (String line : visualLines) {
            if (linesOnPage >= LINES_PER_PAGE) {
                pages.add(page.toString());
                if (pages.size() >= maxPages) return pages;
                page = new StringBuilder();
                linesOnPage = 0;
            }
            if (linesOnPage > 0) page.append('\n');
            page.append(line);
            linesOnPage++;
        }
        if (page.length() > 0) {
            pages.add(page.toString());
        }
        if (pages.isEmpty()) pages.add("");
        return pages;
    }

    private static void wrapParagraph(Font tr, String para, List<String> out) {
        int idx = 0;
        while (idx < para.length()) {
            float width = 0;
            int lineEnd = idx;
            int lastSpace = -1;

            while (lineEnd < para.length()) {
                char c = para.charAt(lineEnd);
                float cw = tr.width(String.valueOf(c));
                if (width + cw > LINE_WIDTH_PX && lineEnd > idx) break;
                if (c == ' ') lastSpace = lineEnd;
                width += cw;
                lineEnd++;
            }

            if (lineEnd >= para.length()) {

                out.add(para.substring(idx));
                return;
            }

            if (lastSpace > idx) {

                out.add(para.substring(idx, lastSpace));
                idx = lastSpace + 1;
            } else {

                out.add(para.substring(idx, lineEnd));
                idx = lineEnd;
            }
        }
    }

    public static int calculatePagesNeeded(Font tr, String text) {
        if (text == null || text.isEmpty()) return 0;
        return splitTextIntoPages(tr, text, MAX_PAGES).size();
    }

    private List<String> generatePages(Minecraft mc, int numPages) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<String> result = new ArrayList<>(numPages);

        if (onlyAscii) {

            PrimitiveIterator.OfInt chars = rng.ints(0x21, 0x7F)
                .filter(i -> !Character.isWhitespace(i))
                .iterator();
            for (int p = 0; p < numPages && chars.hasNext(); p++) {
                StringBuilder page = new StringBuilder();
                for (int line = 0; line < LINES_PER_PAGE && chars.hasNext(); line++) {
                    if (line > 0) page.append('\n');
                    float lineWidth = 0;
                    while (chars.hasNext()) {
                        int c = chars.nextInt();
                        float cw = mc.font.width(Character.toString((char) c));
                        if (cw <= 0) cw = 6f;
                        if (lineWidth + cw > LINE_WIDTH_PX) break;
                        lineWidth += cw;
                        page.append((char) c);
                    }
                }
                result.add(page.toString());
            }
        } else {

            int charsPerPage = 512;
            int charsPerLine = charsPerPage / LINES_PER_PAGE;
            int remainder = charsPerPage % LINES_PER_PAGE;
            PrimitiveIterator.OfInt chars = rng.ints(0x0800, 0x10000)
                .filter(i -> !Character.isWhitespace(i) && i != '\r' && i != '\n')
                .iterator();
            for (int p = 0; p < numPages && chars.hasNext(); p++) {
                StringBuilder page = new StringBuilder();
                for (int line = 0; line < LINES_PER_PAGE && chars.hasNext(); line++) {
                    if (line > 0) page.append('\n');
                    int lineMax = charsPerLine + (line < remainder ? 1 : 0);
                    for (int ci = 0; ci < lineMax && chars.hasNext(); ci++) {
                        page.append((char) chars.nextInt());
                    }
                }
                result.add(page.toString());
            }
        }
        return result;
    }

    private int estimateBytes(List<String> pageList) {
        int bytes = 0;
        for (String page : pageList) {
            for (int i = 0; i < page.length(); i++) {
                char c = page.charAt(i);
                if (c < 0x80) bytes += 1;
                else if (c < 0x800) bytes += 2;
                else bytes += 3;
            }
        }
        return bytes;
    }

    @Override
    public MacroActionType getType() { return MacroActionType.NBT_BOOK; }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", getType().name());
        tag.putInt("pages", pages);
        tag.putString("title", title);
        tag.putBoolean("onlyAscii", onlyAscii);
        tag.putString("customText", customComponent);
        tag.putInt("delayTicks", delayTicks);
        tag.putInt("bookCount", bookCount);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        if (tag.contains("pages")) pages = tag.getIntOr("pages", 100);
        else if (tag.contains("targetSizeKB")) pages = Math.min(100, tag.getIntOr("targetSizeKB", 150));
        if (tag.contains("title")) title = tag.getStringOr("title", "EldenRingLore");
        if (tag.contains("onlyAscii")) onlyAscii = tag.getBooleanOr("onlyAscii", false);
        if (tag.contains("customText")) customComponent = tag.getStringOr("customText", "");
        if (tag.contains("delayTicks")) delayTicks = tag.getIntOr("delayTicks", 20);
        if (tag.contains("bookCount")) bookCount = tag.getIntOr("bookCount", 1);
        if (tag.contains("enabled")) enabled = tag.getBooleanOr("enabled", true);
    }

    @Override
    public String getDisplayName() {
        String mode = customComponent.isEmpty() ? (onlyAscii ? "ASCII" : "Random") : "Custom";
        String count = bookCount > 1 ? ", x" + bookCount : "";
        return "NBT Book (" + pages + "pg, " + mode + count + ")";
    }

    @Override public String getIcon() { return "B"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }
}
