package ru.aurion.kara.gui;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.aurion.kara.AurionKaraPlugin;
import ru.aurion.kara.model.Fine;
import ru.aurion.kara.model.GuardCall;
import ru.aurion.kara.util.Text;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MenuManager implements Listener {
    private static final int PAGE_SIZE = 45;
    private final AurionKaraPlugin plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey idKey;
    private final NamespacedKey uuidKey;

    public MenuManager(AurionKaraPlugin plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "action");
        this.idKey = new NamespacedKey(plugin, "id");
        this.uuidKey = new NamespacedKey(plugin, "uuid");
    }

    public void openMain(Player player) {
        KaraMenuHolder holder = new KaraMenuHolder("main", 0, "");
        Inventory inv = Bukkit.createInventory(holder, 27, Text.color("&8Управление гвардией"));
        holder.inventory(inv);
        long calls = activeCalls().size();
        long overdue = plugin.store().fines().stream()
                .filter(f -> !f.paid() && f.deadline() <= System.currentTimeMillis()).count();
        long debtors = plugin.store().fines().stream().filter(f -> !f.paid())
                .map(Fine::playerId).distinct().count();
        inv.setItem(10, item(Material.BELL, "&cАктивные вызовы",
                List.of("&7Ожидают: &f" + calls, "", "&eНажмите, чтобы открыть"), "calls", null, null));
        inv.setItem(12, item(Material.GOLD_INGOT, "&6Должники",
                List.of("&7Игроков с долгом: &f" + debtors, "&7Просроченных штрафов: &c" + overdue,
                        "", "&eНажмите, чтобы открыть"), "debtors", null, null));
        inv.setItem(14, item(Material.BOOK, "&eВсе штрафы",
                List.of("&7Всего записей: &f" + plugin.store().fines().size(), "",
                        "&eНажмите, чтобы открыть"), "fines", null, null));
        inv.setItem(16, item(Material.IRON_CHESTPLATE, "&bСостав гвардии",
                List.of("&7Гвардейцев: &f" + plugin.store().guards().size(),
                        "&7Заявок: &f" + plugin.store().applications().size(), "",
                        "&eНажмите, чтобы открыть"), "guards", null, null));
        player.openInventory(inv);
    }

    public void openCalls(Player player, int page) {
        List<GuardCall> calls = activeCalls();
        Inventory inv = pageInventory("calls", page, "", "&8Вызовы гвардии");
        fillPage(inv, calls, page, c -> item(Material.WRITABLE_BOOK, "&cВызов #" + c.id(),
                List.of("&7Игрок: &f" + c.playerName(), "&7Сообщение: &f" + c.message(),
                        "&7Мир: &f" + c.world(), "&7Координаты: &f" + (int)c.x() + ", " + (int)c.y() + ", " + (int)c.z(),
                        "&7Создан: &f" + Text.date(c.createdAt()), "", "&eЛКМ — телепортироваться",
                        "&aПКМ — закрыть вызов"), "call", c.id(), null));
        nav(inv, page, calls.size());
        player.openInventory(inv);
    }

    public void openDebtors(Player player, int page) {
        Map<UUID, List<Fine>> grouped = plugin.store().fines().stream().filter(f -> !f.paid())
                .collect(Collectors.groupingBy(Fine::playerId, LinkedHashMap::new, Collectors.toList()));
        List<Map.Entry<UUID, List<Fine>>> debtors = new ArrayList<>(grouped.entrySet());
        debtors.sort(Comparator.comparingDouble((Map.Entry<UUID, List<Fine>> e) ->
                e.getValue().stream().mapToDouble(Fine::amount).sum()).reversed());
        Inventory inv = pageInventory("debtors", page, "", "&8Должники");
        fillPage(inv, debtors, page, entry -> {
            List<Fine> fines = entry.getValue();
            Fine first = fines.getFirst();
            double total = fines.stream().mapToDouble(Fine::amount).sum();
            long deadline = fines.stream().mapToLong(Fine::deadline).min().orElse(0);
            long overdue = fines.stream().filter(f -> f.deadline() <= System.currentTimeMillis()).count();
            return head(entry.getKey(), first.playerName(), "&6" + first.playerName(),
                    List.of("&7Общий долг: &6" + Text.money(total), "&7Штрафов: &f" + fines.size(),
                            "&7Ближайший срок: &f" + Text.date(deadline),
                            "&7Просрочено: &c" + overdue, "", "&eНажмите — открыть штрафы"),
                    "debtor", null);
        });
        nav(inv, page, debtors.size());
        player.openInventory(inv);
    }

    public void openFines(Player player, int page, String playerFilter) {
        String filter = playerFilter == null ? "" : playerFilter;
        List<Fine> fines = plugin.store().fines().stream()
                .filter(f -> filter.isBlank() || f.playerName().equalsIgnoreCase(filter))
                .sorted(Comparator.comparingInt(Fine::id).reversed()).toList();
        Inventory inv = pageInventory("fines", page, filter,
                filter.isBlank() ? "&8Все штрафы" : "&8Штрафы: " + filter);
        fillPage(inv, fines, page, f -> {
            boolean overdue = !f.paid() && f.deadline() <= System.currentTimeMillis();
            Material icon = f.paid() ? Material.LIME_DYE : overdue ? Material.RED_DYE : Material.PAPER;
            List<String> lore = new ArrayList<>(List.of(
                    "&7Игрок: &f" + f.playerName(),
                    "&7Сумма: &6" + Text.money(f.amount()),
                    "&7Срок: &f" + Text.date(f.deadline()),
                    "&7Причина: &f" + f.reason(),
                    "&7Выдал: &f" + f.issuerName(),
                    "&7Статус: " + (f.paid() ? "&aоплачен" : overdue ? "&cпросрочен" : "&eожидает оплаты")));
            if (!f.paid()) lore.addAll(List.of("", "&aShift + ПКМ — отметить оплаченным"));
            return item(icon, "&eШтраф #" + f.id(), lore, "fine", f.id(), null);
        });
        nav(inv, page, fines.size());
        player.openInventory(inv);
    }

    public void openGuards(Player player, int page) {
        List<UUID> guards = new ArrayList<>(plugin.store().guards());
        guards.sort(Comparator.comparing(uuid -> {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            return offline.getName() == null ? "" : offline.getName();
        }));
        List<RosterEntry> entries = new ArrayList<>();
        if (plugin.isChief(player)) {
            plugin.store().applications().forEach((uuid, name) ->
                    entries.add(new RosterEntry(uuid, name, true)));
        }
        for (UUID uuid : guards) {
            OfflinePlayer guard = Bukkit.getOfflinePlayer(uuid);
            entries.add(new RosterEntry(uuid,
                    guard.getName() == null ? uuid.toString() : guard.getName(), false));
        }
        Inventory inv = pageInventory("guards", page, "", "&8Состав гвардии");
        fillPage(inv, entries, page, entry -> {
            if (entry.application()) {
                return head(entry.uuid(), entry.name(), "&eЗаявка: " + entry.name(),
                        List.of("&aЛКМ — принять", "&cПКМ — отклонить"),
                        "application", entry.uuid());
            }
            OfflinePlayer guard = Bukkit.getOfflinePlayer(entry.uuid());
            List<String> lore = new ArrayList<>(List.of(
                    "&7Статус: " + (guard.isOnline() ? "&aонлайн" : "&7оффлайн")));
            if (plugin.isChief(player) && !plugin.isChief(guard)) {
                lore.addAll(List.of("", "&cShift + ПКМ — исключить"));
            }
            return head(entry.uuid(), entry.name(), "&b" + entry.name(), lore, "guard", entry.uuid());
        });
        nav(inv, page, entries.size());
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof KaraMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || clicked.getItemMeta() == null) return;
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;
        Integer id = meta.getPersistentDataContainer().get(idKey, PersistentDataType.INTEGER);
        String rawUuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
        UUID uuid = parseUuid(rawUuid);

        switch (action) {
            case "calls" -> openCalls(player, 0);
            case "debtors" -> openDebtors(player, 0);
            case "fines" -> openFines(player, 0, "");
            case "guards" -> openGuards(player, 0);
            case "back" -> openMain(player);
            case "prev" -> reopen(player, holder, holder.page() - 1);
            case "next" -> reopen(player, holder, holder.page() + 1);
            case "debtor" -> {
                if (uuid == null) return;
                OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
                openFines(player, 0, target.getName() == null ? "" : target.getName());
            }
            case "fine" -> {
                if (id != null && event.isShiftClick() && event.isRightClick()
                        && (player.hasPermission("aurionkara.fine.manage") || plugin.isGuard(player))) {
                    plugin.store().setFinePaid(id, true);
                    plugin.message(player, "fine-paid", "{id}", String.valueOf(id));
                    openFines(player, holder.page(), holder.filter());
                }
            }
            case "call" -> handleCall(player, id, event.isRightClick(), holder.page());
            case "application" -> handleApplication(player, uuid, event.isRightClick());
            case "guard" -> {
                if (uuid != null && event.isShiftClick() && event.isRightClick()
                        && plugin.isChief(player) && !plugin.isChief(Bukkit.getOfflinePlayer(uuid))) {
                    plugin.store().removeGuard(uuid);
                    player.sendMessage(plugin.prefix() + Text.color("&aГвардеец исключён."));
                    openGuards(player, holder.page());
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof KaraMenuHolder) event.setCancelled(true);
    }

    private void handleCall(Player player, Integer id, boolean resolve, int page) {
        if (id == null) return;
        Optional<GuardCall> optional = plugin.store().call(id);
        if (optional.isEmpty()) return;
        GuardCall call = optional.get();
        if (resolve) {
            plugin.store().resolveCall(id);
            player.sendMessage(plugin.prefix() + Text.color("&aВызов #" + id + " закрыт."));
            openCalls(player, page);
            return;
        }
        World world = Bukkit.getWorld(call.world());
        if (world == null) {
            player.sendMessage(plugin.prefix() + Text.color("&cМир вызова не найден."));
            return;
        }
        player.closeInventory();
        player.teleportAsync(new Location(world, call.x(), call.y(), call.z())).thenAccept(success -> {
            if (success) player.sendMessage(plugin.prefix() + Text.color("&aВы телепортированы к вызову #" + id + "."));
        });
    }

    private void handleApplication(Player player, UUID uuid, boolean reject) {
        if (uuid == null || !plugin.isChief(player)) return;
        OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
        if (reject) {
            plugin.store().reject(uuid);
            player.sendMessage(plugin.prefix() + Text.color("&cЗаявка отклонена."));
        } else {
            plugin.store().addGuard(uuid);
            player.sendMessage(plugin.prefix() + Text.color("&a" + target.getName() + " принят в гвардию."));
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(plugin.prefix() + Text.color("&aВаша заявка принята."));
            }
        }
        openGuards(player, 0);
    }

    private void reopen(Player player, KaraMenuHolder holder, int page) {
        if (page < 0) return;
        switch (holder.type()) {
            case "calls" -> openCalls(player, page);
            case "debtors" -> openDebtors(player, page);
            case "fines" -> openFines(player, page, holder.filter());
            case "guards" -> openGuards(player, page);
            default -> openMain(player);
        }
    }

    private Inventory pageInventory(String type, int page, String filter, String title) {
        KaraMenuHolder holder = new KaraMenuHolder(type, Math.max(0, page), filter);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.color(title));
        holder.inventory(inv);
        return inv;
    }

    private <T> void fillPage(Inventory inv, List<T> values, int page, Function<T, ItemStack> mapper) {
        int from = Math.min(values.size(), Math.max(0, page) * PAGE_SIZE);
        int to = Math.min(values.size(), from + PAGE_SIZE);
        for (int i = from; i < to; i++) inv.setItem(i - from, mapper.apply(values.get(i)));
    }

    private void nav(Inventory inv, int page, int total) {
        inv.setItem(49, item(Material.BARRIER, "&cНазад", List.of("&7В главное меню"), "back", null, null));
        if (page > 0) inv.setItem(45, item(Material.ARROW, "&eПредыдущая страница", List.of(), "prev", null, null));
        if ((page + 1) * PAGE_SIZE < total)
            inv.setItem(53, item(Material.ARROW, "&eСледующая страница", List.of(), "next", null, null));
    }

    private List<GuardCall> activeCalls() {
        long configuredMinutes = plugin.getConfig().getLong("settings.call-expire-minutes", 0);
        long cutoff = configuredMinutes <= 0 ? Long.MIN_VALUE
                : System.currentTimeMillis() - configuredMinutes * 60_000L;
        return plugin.store().calls().stream()
                .filter(c -> !c.resolved() && c.createdAt() >= cutoff)
                .sorted(Comparator.comparingLong(GuardCall::createdAt).reversed()).toList();
    }

    private ItemStack item(Material material, String name, List<String> lore,
                           String action, Integer id, UUID uuid) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        decorate(meta, name, lore, action, id, uuid);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack head(UUID owner, String fallbackName, String name, List<String> lore,
                           String action, UUID uuid) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        decorate(meta, name, lore, action, null, uuid);
        item.setItemMeta(meta);
        return item;
    }

    private void decorate(ItemMeta meta, String name, List<String> lore,
                          String action, Integer id, UUID uuid) {
        meta.setDisplayName(Text.color(name));
        meta.setLore(lore.stream().map(Text::color).toList());
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (id != null) meta.getPersistentDataContainer().set(idKey, PersistentDataType.INTEGER, id);
        if (uuid != null) meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, uuid.toString());
    }

    private static UUID parseUuid(String raw) {
        try { return raw == null ? null : UUID.fromString(raw); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private record RosterEntry(UUID uuid, String name, boolean application) {}
}
