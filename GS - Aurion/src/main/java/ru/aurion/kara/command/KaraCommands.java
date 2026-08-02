package ru.aurion.kara.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import ru.aurion.kara.AurionKaraPlugin;
import ru.aurion.kara.model.Fine;
import ru.aurion.kara.model.GuardCall;
import ru.aurion.kara.util.Text;

import java.util.*;
import java.util.stream.Stream;

public final class KaraCommands implements CommandExecutor, TabCompleter {
    private final AurionKaraPlugin plugin;

    public KaraCommands(AurionKaraPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "kara" -> kara(sender, args);
            case "911" -> call911(sender, args);
            case "karamenu" -> menu(sender);
            case "guard" -> guard(sender, args);
            default -> false;
        };
    }

    private boolean kara(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            if (!(sender instanceof Player player)) return playerOnly(sender);
            if (args.length == 1) {
                showOwnFines(player);
            } else if (plugin.isGuard(player)) {
                plugin.menus().openFines(player, 0, args[1]);
            } else plugin.message(sender, "guard-only");
            return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("paid")) {
            if (!canManage(sender)) return denied(sender);
            Integer id = integer(args[1]);
            if (id == null || plugin.store().fine(id).isEmpty()) {
                sender.sendMessage(plugin.prefix() + Text.color("&cШтраф не найден."));
                return true;
            }
            plugin.store().setFinePaid(id, true);
            plugin.message(sender, "fine-paid", "{id}", String.valueOf(id));
            plugin.store().fine(id).map(Fine::playerId).map(Bukkit::getPlayer).ifPresent(p ->
                    plugin.message(p, "fine-paid", "{id}", String.valueOf(id)));
            return true;
        }
        if (!canIssue(sender)) return denied(sender);
        if (args.length < 4) {
            sender.sendMessage(plugin.prefix() + Text.color("&eИспользование: &f/kara <игрок> <сумма> <время> <причина>"));
            sender.sendMessage(plugin.prefix() + Text.color("&7Время: 30m, 2h, 3d, 1w или 1d12h."));
            return true;
        }
        OfflinePlayer target = plugin.findKnownPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.prefix() + Text.color("&cИгрок не найден среди ранее заходивших."));
            return true;
        }
        double amount;
        try { amount = Double.parseDouble(args[1].replace(',', '.')); }
        catch (NumberFormatException ex) { amount = -1; }
        long duration;
        try { duration = Text.parseDuration(args[2]); }
        catch (RuntimeException ex) { duration = -1; }
        if (!Double.isFinite(amount) || amount <= 0 || amount > 1_000_000_000) {
            sender.sendMessage(plugin.prefix() + Text.color("&cСумма должна быть от 0 до 1 000 000 000."));
            return true;
        }
        if (duration < 1000 || duration > 3_153_600_000_000L) {
            sender.sendMessage(plugin.prefix() + Text.color("&cНеверное время. Примеры: 30m, 2h, 3d, 1w."));
            return true;
        }
        String reason = Text.plain(String.join(" ", Arrays.copyOfRange(args, 3, args.length)), 180);
        Player issuer = sender instanceof Player p ? p : null;
        Fine fine = plugin.store().addFine(target.getUniqueId(),
                target.getName() == null ? args[0] : target.getName(), amount,
                System.currentTimeMillis() + duration, reason,
                issuer == null ? null : issuer.getUniqueId(), sender.getName());
        plugin.message(sender, "fine-issued", "{id}", String.valueOf(fine.id()),
                "{player}", fine.playerName(), "{amount}", Text.money(amount),
                "{deadline}", Text.date(fine.deadline()));
        if (target.isOnline() && target.getPlayer() != null) {
            plugin.message(target.getPlayer(), "fine-received", "{id}", String.valueOf(fine.id()),
                    "{amount}", Text.money(amount), "{deadline}", Text.date(fine.deadline()),
                    "{reason}", reason);
        }
        return true;
    }

    private boolean call911(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return playerOnly(sender);
        if (args.length == 0) {
            player.sendMessage(plugin.prefix() + Text.color("&eОпишите происшествие: &f/911 <сообщение>"));
            return true;
        }
        String message = Text.plain(String.join(" ", args), 180);
        Location l = player.getLocation();
        GuardCall call = plugin.store().addCall(player.getUniqueId(), player.getName(), message,
                l.getWorld().getName(), l.getX(), l.getY(), l.getZ());
        plugin.message(player, "call-created", "{id}", String.valueOf(call.id()));
        for (Player guard : Bukkit.getOnlinePlayers()) {
            if (!plugin.isGuard(guard)) continue;
            plugin.message(guard, "call-alert", "{id}", String.valueOf(call.id()),
                    "{player}", player.getName(), "{message}", message, "{world}", l.getWorld().getName(),
                    "{x}", String.valueOf(l.getBlockX()), "{y}", String.valueOf(l.getBlockY()),
                    "{z}", String.valueOf(l.getBlockZ()));
        }
        return true;
    }

    private boolean menu(CommandSender sender) {
        if (!(sender instanceof Player player)) return playerOnly(sender);
        if (!plugin.isGuard(player)) return denied(sender);
        plugin.menus().openMain(player);
        return true;
    }

    private boolean guard(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.prefix() + Text.color("&e/guard apply &7— подать заявку"));
            if (isChief(sender)) sender.sendMessage(plugin.prefix() + Text.color(
                    "&e/guard add|remove <ник>, /guard setchief|removechief <ник>, /guard list"));
            return true;
        }
        if (args[0].equalsIgnoreCase("apply")) {
            if (!(sender instanceof Player player)) return playerOnly(sender);
            if (plugin.isGuard(player)) {
                player.sendMessage(plugin.prefix() + Text.color("&eВы уже состоите в гвардии."));
                return true;
            }
            plugin.store().apply(player.getUniqueId(), player.getName());
            plugin.message(player, "application-sent");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (plugin.isChief(online)) online.sendMessage(plugin.prefix()
                        + Text.color("&eНовая заявка в гвардию от &f" + player.getName() + "&e."));
            }
            return true;
        }
        if (!isChief(sender)) return denied(sender);
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(plugin.prefix() + Text.color("&eГвардейцев: &f" + plugin.store().guards().size()));
            for (UUID uuid : plugin.store().guards()) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                sender.sendMessage(Text.color(" &8• &f" + (p.getName() == null ? uuid : p.getName())
                        + (p.isOnline() ? " &a●" : " &7●")));
            }
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.prefix() + Text.color("&cУкажите ник игрока."));
            return true;
        }
        OfflinePlayer target = plugin.findKnownPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.prefix() + Text.color("&cИгрок не найден среди ранее заходивших."));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                plugin.store().addGuard(target.getUniqueId());
                sender.sendMessage(plugin.prefix() + Text.color("&a" + target.getName() + " принят в гвардию."));
            }
            case "remove" -> {
                if (plugin.isChief(target)) {
                    sender.sendMessage(plugin.prefix() + Text.color("&cНельзя исключить главного гвардейца."));
                } else {
                    plugin.store().removeGuard(target.getUniqueId());
                    sender.sendMessage(plugin.prefix() + Text.color("&a" + target.getName() + " исключён из гвардии."));
                }
            }
            case "setchief" -> {
                List<String> names = new ArrayList<>(plugin.getConfig().getStringList("chief-guards.names"));
                List<String> uuids = new ArrayList<>(plugin.getConfig().getStringList("chief-guards.uuids"));
                if (target.getName() != null && names.stream().noneMatch(target.getName()::equalsIgnoreCase)) {
                    names.add(target.getName());
                }
                if (!uuids.contains(target.getUniqueId().toString())) uuids.add(target.getUniqueId().toString());
                plugin.getConfig().set("chief-guards.names", names);
                plugin.getConfig().set("chief-guards.uuids", uuids);
                plugin.saveConfig();
                plugin.store().addGuard(target.getUniqueId());
                sender.sendMessage(plugin.prefix() + Text.color("&aДобавлен главный гвардеец: &e" + target.getName()));
            }
            case "removechief" -> {
                List<String> names = new ArrayList<>(plugin.getConfig().getStringList("chief-guards.names"));
                List<String> uuids = new ArrayList<>(plugin.getConfig().getStringList("chief-guards.uuids"));
                names.removeIf(name -> target.getName() != null && name.equalsIgnoreCase(target.getName()));
                uuids.removeIf(uuid -> uuid.equalsIgnoreCase(target.getUniqueId().toString()));
                if (target.getName() != null && target.getName().equalsIgnoreCase(
                        plugin.getConfig().getString("chief-guard.name", ""))) {
                    plugin.getConfig().set("chief-guard.name", "");
                    plugin.getConfig().set("chief-guard.uuid", "");
                }
                plugin.getConfig().set("chief-guards.names", names);
                plugin.getConfig().set("chief-guards.uuids", uuids);
                plugin.saveConfig();
                sender.sendMessage(plugin.prefix() + Text.color("&aСнят главный гвардеец: &e" + target.getName()));
            }
            default -> sender.sendMessage(plugin.prefix() + Text.color("&cНеизвестное действие."));
        }
        return true;
    }

    private void showOwnFines(Player player) {
        List<Fine> fines = plugin.store().fines().stream()
                .filter(f -> f.playerId().equals(player.getUniqueId()) && !f.paid()).toList();
        if (fines.isEmpty()) {
            player.sendMessage(plugin.prefix() + Text.color("&aУ вас нет активных штрафов."));
            return;
        }
        player.sendMessage(plugin.prefix() + Text.color("&eВаши активные штрафы:"));
        fines.forEach(f -> player.sendMessage(Text.color("&8#" + f.id() + " &6" + Text.money(f.amount())
                + " &7до &f" + Text.date(f.deadline()) + " &8— &7" + f.reason())));
    }

    private boolean canIssue(CommandSender sender) {
        return sender.hasPermission("aurionkara.fine.issue")
                || sender instanceof Player p && plugin.isGuard(p);
    }

    private boolean canManage(CommandSender sender) {
        return sender.hasPermission("aurionkara.fine.manage")
                || sender instanceof Player p && plugin.isGuard(p);
    }

    private boolean isChief(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender.hasPermission("aurionkara.chief")
                || sender instanceof Player p && plugin.isChief(p);
    }

    private boolean denied(CommandSender sender) { plugin.message(sender, "no-permission"); return true; }
    private boolean playerOnly(CommandSender sender) { plugin.message(sender, "players-only"); return true; }
    private static Integer integer(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ex) { return null; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("kara")) {
            if (args.length == 1) return match(args[0], Stream.concat(Stream.of("list", "paid"),
                    Bukkit.getOnlinePlayers().stream().map(Player::getName)));
            if (args.length == 2 && !args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("paid"))
                return match(args[1], Stream.of("100", "500", "1000"));
            if (args.length == 3) return match(args[2], Stream.of("30m", "2h", "1d", "3d", "1w"));
        }
        if (name.equals("guard")) {
            if (args.length == 1) return match(args[0],
                    Stream.of("apply", "add", "remove", "setchief", "removechief", "list"));
            if (args.length == 2) return match(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName));
        }
        return List.of();
    }

    private static List<String> match(String prefix, Stream<String> values) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        return values.filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lowered)).sorted().toList();
    }
}
