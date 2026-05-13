package autismclient.util;

import autismclient.AutismClientAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class PackUtilAccountManager implements Iterable<PackUtilAccount> {
    private static final PackUtilAccountManager INSTANCE = new PackUtilAccountManager();
    private final List<PackUtilAccount> accounts = new ArrayList<>();
    private boolean loaded;

    private PackUtilAccountManager() {
    }

    public static PackUtilAccountManager get() {
        INSTANCE.ensureLoaded();
        return INSTANCE;
    }

    private File saveFile() {
        return new File(Minecraft.getInstance().gameDirectory, "packutil-accounts.nbt");
    }

    private synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        File file = saveFile();
        if (!file.exists()) return;
        try {
            CompoundTag tag = NbtIo.read(file.toPath());
            if (tag == null) return;
            accounts.clear();
            ListTag list = tag.getListOrEmpty("accounts");
            for (Tag element : list) {
                if (element instanceof CompoundTag compoundTag) accounts.add(new PackUtilAccount().fromTag(compoundTag));
            }
        } catch (Exception e) {
            AutismClientAddon.LOG.error("Failed to load PackUtil accounts", e);
        }
    }

    public synchronized void save() {
        try {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (PackUtilAccount account : accounts) list.add(account.toTag());
            tag.put("accounts", list);
            NbtIo.write(tag, saveFile().toPath());
        } catch (Exception e) {
            AutismClientAddon.LOG.error("Failed to save PackUtil accounts", e);
        }
    }

    public synchronized List<PackUtilAccount> all() {
        return new ArrayList<>(accounts);
    }

    public synchronized void add(PackUtilAccount account) {
        if (account == null) return;
        accounts.add(account);
        save();
    }

    public synchronized boolean contains(PackUtilAccount account) {
        return accounts.contains(account);
    }

    public synchronized void remove(PackUtilAccount account) {
        if (accounts.remove(account)) save();
    }

    public void login(PackUtilAccount account) {
        if (account == null) return;
        Thread thread = new Thread(() -> {
            if (account.fetchInfo() && account.login()) {
                save();
                PackUtilClientMessaging.sendPrefixed("Logged in as " + account.displayName() + ".");
            } else {
                PackUtilClientMessaging.sendPrefixed("Failed to login account: " + account.displayName());
            }
        }, "PackUtil-Account-Login");
        thread.setDaemon(true);
        thread.start();
    }

    public void loginMicrosoft(PackUtilAccount account) {
        if (account == null || account.type != PackUtilAccountType.Microsoft) return;
        PackUtilMicrosoftLogin.getRefreshToken(refreshToken -> {
            if (refreshToken == null) {
                PackUtilClientMessaging.sendPrefixed("Microsoft login cancelled or failed.");
                return;
            }
            account.label = refreshToken;
            Thread thread = new Thread(() -> {
                if (account.fetchInfo() && account.login()) {
                    synchronized (this) {
                        if (!accounts.contains(account)) accounts.add(account);
                    }
                    save();
                    PackUtilClientMessaging.sendPrefixed("Logged in as " + account.displayName() + ".");
                } else {
                    PackUtilClientMessaging.sendPrefixed("Failed to login Microsoft account.");
                }
            }, "PackUtil-Microsoft-Login");
            thread.setDaemon(true);
            thread.start();
        });
    }

    @Override
    public Iterator<PackUtilAccount> iterator() {
        return all().iterator();
    }
}
