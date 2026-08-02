package ru.aurion.kara.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.aurion.kara.AurionKaraPlugin;
import ru.aurion.kara.model.Fine;
import ru.aurion.kara.util.Text;

public final class PlayerListener implements Listener {
    private final AurionKaraPlugin plugin;

    public PlayerListener(AurionKaraPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.isChief(player) && !plugin.store().guards().contains(player.getUniqueId())) {
            plugin.store().addGuard(player.getUniqueId());
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            long now = System.currentTimeMillis();
            long active = plugin.store().fines().stream()
                    .filter(f -> f.playerId().equals(player.getUniqueId()) && !f.paid()).count();
            double total = plugin.store().fines().stream()
                    .filter(f -> f.playerId().equals(player.getUniqueId()) && !f.paid())
                    .mapToDouble(Fine::amount).sum();
            if (active > 0) {
                player.sendMessage(plugin.prefix() + Text.color("&eУ вас активных штрафов: &c" + active
                        + "&e, общая сумма: &6" + Text.money(total) + "&e."));
                for (Fine fine : plugin.store().fines()) {
                    if (fine.playerId().equals(player.getUniqueId()) && !fine.paid() && fine.deadline() <= now) {
                        plugin.message(player, "overdue", "{id}", String.valueOf(fine.id()),
                                "{amount}", Text.money(fine.amount()), "{reason}", fine.reason());
                    }
                }
            }
            if (plugin.isGuard(player) && plugin.getConfig().getBoolean("settings.notify-guards-on-join", true)) {
                long calls = plugin.store().calls().stream().filter(c -> !c.resolved()).count();
                long overdue = plugin.store().fines().stream()
                        .filter(f -> !f.paid() && f.deadline() <= now).count();
                player.sendMessage(plugin.prefix() + Text.color("&eАктивных вызовов: &c" + calls
                        + "&e, просроченных штрафов: &c" + overdue + "&e. Меню: &f/karamenu"));
            }
        }, 40L);
    }
}
