package autismclient.util;

import autismclient.AutismClientAddon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PackUtilMacroManager {
    private static PackUtilMacroManager INSTANCE;
    private List<PackUtilMacro> macros = new ArrayList<>();
    private final File saveFile;
    private volatile long revision;

    private PackUtilMacroManager() {
        saveFile = new File(AutismClientAddon.FOLDER, "packutil_macros.nbt");
        load();
    }

    public static synchronized PackUtilMacroManager get() {
        if (INSTANCE == null) {
            INSTANCE = new PackUtilMacroManager();
        }
        return INSTANCE;
    }

    public synchronized String createUniqueName(String preferredName) {
        String baseName = preferredName == null || preferredName.isBlank() ? "New Macro" : preferredName.trim();
        String candidate = baseName;
        int suffix = 1;
        while (get(candidate) != null) {
            candidate = baseName + " (" + suffix++ + ")";
        }
        return candidate;
    }

    public synchronized PackUtilMacro addImportedCopy(PackUtilMacro source, String preferredName) {
        if (source == null) return null;

        PackUtilMacro copy = source.deepCopy();
        copy.name = createUniqueName(preferredName != null && !preferredName.isBlank() ? preferredName : source.name);
        add(copy);
        return copy;
    }

    public synchronized void add(PackUtilMacro macro) {
        macros.add(macro);
        save();
    }

    public synchronized PackUtilMacro get(String name) {
        for (PackUtilMacro macro : macros) {
            if (macro.name.equalsIgnoreCase(name)) return macro;
        }
        return null;
    }

    public synchronized List<PackUtilMacro> getAll() {
        return new ArrayList<>(macros);
    }

    public long getRevision() {
        return revision;
    }

    public synchronized void remove(PackUtilMacro macro) {
        if (autismclient.util.macro.MacroExecutor.isMacroRunning(macro.name)) {
            autismclient.util.macro.MacroExecutor.stopMacro(macro.name);
            PackUtilClientMessaging.sendPrefixed("Ãƒâ€šÃ‚Â§eStopped running macro before deletion: " + macro.name);
        }

        if (macros.remove(macro)) {
            save();
            PackUtilClientMessaging.sendPrefixed("ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§aDeleted macro: " + macro.name);

        PackUtilMacroEditorOverlay editor = PackUtilMacroEditorOverlay.getSharedOverlay();
            if (editor != null && editor.isEditingMacro(macro)) {
                editor.close();
            }

            if (PackUtilLANSync.getInstance().isInSession()) {
                PackUtilLANSync.getInstance().broadcastMacroDeletion(macro.name);
            }
        }
    }

    public void delete(PackUtilMacro macro) {
        remove(macro);
    }

    public void executeMacro(String name) {
        PackUtilMacro macro = get(name);
        if (macro != null) {
            macro.execute();
            PackUtilClientMessaging.sendPrefixed("ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§aExecuting macro: " + macro.name);
        } else {
            PackUtilClientMessaging.sendPrefixed("ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§cMacro not found: " + name);
        }
    }

    public void stopMacro() {
        if (autismclient.util.macro.MacroExecutor.isRunning()) {
            autismclient.util.macro.MacroExecutor.stop();
        } else {
            PackUtilClientMessaging.sendPrefixed("ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§eNo macro is currently running.");
        }
    }

    public synchronized void save() {
        revision++;
        try {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (PackUtilMacro macro : macros) {
                list.add(macro.toTag());
            }
            tag.put("macros", list);
            NbtIo.write(tag, saveFile.toPath());
        } catch (Exception e) {
            AutismClientAddon.LOG.error("Failed to save PackUtil macros", e);
        }

        if (PackUtilLANSync.getInstance().isInSession()) {
            PackUtilLANSync.getInstance().broadcastMacroList();
        }
    }

    public synchronized void load() {
        if (!saveFile.exists()) return;

        try {
            CompoundTag tag = NbtIo.read(saveFile.toPath());
            if (tag != null && tag.contains("macros")) {
                macros.clear();
                ListTag list = (ListTag) tag.get("macros");
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof CompoundTag) {
                        PackUtilMacro macro = new PackUtilMacro();
                        macro.fromTag((CompoundTag) list.get(i));
                        macros.add(macro);
                    }
                }
            }
            revision++;
        } catch (Exception e) {
            AutismClientAddon.LOG.error("Failed to load PackUtil macros", e);
        }
    }
}
