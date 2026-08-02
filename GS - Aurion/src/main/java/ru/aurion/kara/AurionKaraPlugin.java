package ru.aurion.kara;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.aurion.kara.command.KaraCommands;
import ru.aurion.kara.gui.MenuManager;
import ru.aurion.kara.listener.PlayerListener;
import ru.aurion.kara.model.Fine;
import ru.aurion.kara.util.Text;

import java.util.Locale;
import java.util.UUID;

public final class AurionKaraPlugin extends JavaPlugin {
    private DataStore store;
    private MenuManager menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new DataStore(this);
        store.load();
        menus = new MenuManager(this);
        KaraCommands commands = new KaraCommands(this);
        register("kara", commands);
        register("911", commands);
        register("karamenu", commands);
        register("guard", commands);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(menus, this);

        long interval = Math.max(20L, getConfig().getLong("settings.overdue-check-seconds", 60) * 20L);
        Bukkit.getScheduler().runTaskTimer(this, this::checkOverdueFines, 100L, interval);
        getLogger().info("AurionKara включён. Загружено штрафов: " + store.fines().size());
    }

    @Override
    public void onDisable() {
        if (store != null) store.save();
    }

    private void register(String name, KaraCommands executor) {
        PluginCommand command = getCommand(name);
        if (command == null) throw new IllegalStateException("Команда " + name + " отсутствует в plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void checkOverdueFines() {
        long now = System.currentTimeMillis();
        for (Fine fine : store.fines()) {
            if (fine.paid() || fine.deadline() > now || fine.overdueNotified()) continue;
            Player debtor = Bukkit.getPlayer(fine.playerId());
            if (debtor != null) {
                message(debtor, "overdue",
                        "{id}", String.valueOf(fine.id()),
                        "{amount}", Text.money(fine.amount()),
                        "{reason}", fine.reason());
            }
            for (Player guard : Bukkit.getOnlinePlayers()) {
                if (isGuard(guard)) {
                    guard.sendMessage(prefix() + Text.color("&cПросрочка #" + fine.id() + ": &e"
                            + fine.playerName() + " &7— &6" + Text.money(fine.amount())));
                }
            }
            store.setFineNotified(fine.id());
        }
    }

    public boolean isChief(OfflinePlayer player) {
        if (player == null) return false;
        for (String rawUuid : getConfig().getStringList("chief-guards.uuids")) {
            try {
                if (player.getUniqueId().equals(UUID.fromString(rawUuid.trim()))) return true;
            } catch (IllegalArgumentException ignored) {}
        }
        if (player.getName() != null && getConfig().getStringList("chief-guards.names").stream()
                .anyMatch(name -> player.getName().equalsIgnoreCase(name.trim()))) {
            return true;
        }

        // Совместимость с конфигурацией версии 1.0.0, где глава был только один.
        String configuredUuid = getConfig().getString("chief-guard.uuid", "").trim();
        if (!configuredUuid.isEmpty()) {
            try {
                if (player.getUniqueId().equals(UUID.fromString(configuredUuid))) return true;
            } catch (IllegalArgumentException ignored) {}
        }
        String configuredName = getConfig().getString("chief-guard.name", "");
        return player.getName() != null && !configuredName.isBlank()
                && !configuredName.equalsIgnoreCase("ВСТАВЬТЕ_НИК")
                && player.getName().equalsIgnoreCase(configuredName);
    }

    public boolean isGuard(Player player) {
        return isChief(player) || store.guards().contains(player.getUniqueId())
                || player.hasPermission("aurionkara.guard");
    }

    public OfflinePlayer findKnownPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        String lowered = name.toLowerCase(Locale.ROOT);
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.getName() != null && player.getName().toLowerCase(Locale.ROOT).equals(lowered)) return player;
        }
        return null;
    }

    public void message(org.bukkit.command.CommandSender target, String key, String... replacements) {
        String value = getConfig().getString("messages." + key, "&cНеизвестное сообщение: " + key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace(replacements[i], replacements[i + 1]);
        }
        target.sendMessage(prefix() + Text.color(value));
    }

    public String prefix() {
        return Text.color(getConfig().getString("messages.prefix", "&6Гвардия &8» &r"));
    }

    public DataStore store() { return store; }
    public MenuManager menus() { return menus; }
}
