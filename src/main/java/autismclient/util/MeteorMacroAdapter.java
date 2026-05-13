package autismclient.util;

import java.util.List;

public class MeteorMacroAdapter {
    public static List<PackUtilMacro> getMeteorMacros() {
        return PackUtilCompatManager.getMeteorMacros();
    }

    public static void importToMeteor(PackUtilMacro packUtilMacro) {
        if (PackUtilCompatManager.importToMeteor(packUtilMacro)) {
            PackUtilClientMessaging.sendPrefixed("Â§aImported to Meteor: " + packUtilMacro.name);
        } else {
            PackUtilClientMessaging.sendPrefixed("Â§cFailed to import to Meteor");
        }
    }

    public static boolean importToPackUtil(String macroName) {
        PackUtilMacro packUtilMacro = PackUtilCompatManager.getMeteorMacro(macroName);
        if (packUtilMacro == null) {
            PackUtilClientMessaging.sendPrefixed("Â§cMeteor macro not found: " + macroName);
            return false;
        }

        PackUtilMacro imported = PackUtilMacroManager.get().addImportedCopy(packUtilMacro, packUtilMacro.name);
        if (imported == null) return false;

        if (!imported.name.equals(packUtilMacro.name)) {
            PackUtilClientMessaging.sendPrefixed("Â§eImported Meteor macro as: " + imported.name);
        }
        return true;
    }
}
