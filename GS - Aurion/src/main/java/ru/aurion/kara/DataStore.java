package ru.aurion.kara;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.aurion.kara.model.Fine;
import ru.aurion.kara.model.GuardCall;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class DataStore {
    private final AurionKaraPlugin plugin;
    private final File file;
    private final Map<Integer, Fine> fines = new LinkedHashMap<>();
    private final Map<Integer, GuardCall> calls = new LinkedHashMap<>();
    private final Set<UUID> guards = new LinkedHashSet<>();
    private final Map<UUID, String> applications = new LinkedHashMap<>();
    private int nextFineId = 1;
    private int nextCallId = 1;

    DataStore(AurionKaraPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextFineId = yaml.getInt("next-fine-id", 1);
        nextCallId = yaml.getInt("next-call-id", 1);
        for (String raw : yaml.getStringList("guards")) {
            try { guards.add(UUID.fromString(raw)); } catch (IllegalArgumentException ignored) {}
        }
        ConfigurationSection appSection = yaml.getConfigurationSection("applications");
        if (appSection != null) {
            for (String key : appSection.getKeys(false)) {
                try { applications.put(UUID.fromString(key), appSection.getString(key, "Unknown")); }
                catch (IllegalArgumentException ignored) {}
            }
        }
        ConfigurationSection fineSection = yaml.getConfigurationSection("fines");
        if (fineSection != null) {
            for (String key : fineSection.getKeys(false)) {
                ConfigurationSection s = fineSection.getConfigurationSection(key);
                if (s == null) continue;
                try {
                    int id = Integer.parseInt(key);
                    fines.put(id, new Fine(id, UUID.fromString(s.getString("player-uuid", "")),
                            s.getString("player-name", "Unknown"), s.getDouble("amount"),
                            s.getLong("issued-at"), s.getLong("deadline"), s.getString("reason", ""),
                            parseUuid(s.getString("issuer-uuid")), s.getString("issuer-name", "Console"),
                            s.getBoolean("paid"), s.getBoolean("overdue-notified")));
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Не удалось загрузить штраф " + key + ": " + ex.getMessage());
                }
            }
        }
        ConfigurationSection callSection = yaml.getConfigurationSection("calls");
        if (callSection != null) {
            for (String key : callSection.getKeys(false)) {
                ConfigurationSection s = callSection.getConfigurationSection(key);
                if (s == null) continue;
                try {
                    int id = Integer.parseInt(key);
                    calls.put(id, new GuardCall(id, UUID.fromString(s.getString("player-uuid", "")),
                            s.getString("player-name", "Unknown"), s.getString("message", ""),
                            s.getString("world", "world"), s.getDouble("x"), s.getDouble("y"),
                            s.getDouble("z"), s.getLong("created-at"), s.getBoolean("resolved")));
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Не удалось загрузить вызов " + key + ": " + ex.getMessage());
                }
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-fine-id", nextFineId);
        yaml.set("next-call-id", nextCallId);
        yaml.set("guards", guards.stream().map(UUID::toString).toList());
        applications.forEach((uuid, name) -> yaml.set("applications." + uuid, name));
        fines.values().forEach(f -> {
            String p = "fines." + f.id() + ".";
            yaml.set(p + "player-uuid", f.playerId().toString());
            yaml.set(p + "player-name", f.playerName());
            yaml.set(p + "amount", f.amount());
            yaml.set(p + "issued-at", f.issuedAt());
            yaml.set(p + "deadline", f.deadline());
            yaml.set(p + "reason", f.reason());
            yaml.set(p + "issuer-uuid", f.issuerId() == null ? null : f.issuerId().toString());
            yaml.set(p + "issuer-name", f.issuerName());
            yaml.set(p + "paid", f.paid());
            yaml.set(p + "overdue-notified", f.overdueNotified());
        });
        calls.values().forEach(c -> {
            String p = "calls." + c.id() + ".";
            yaml.set(p + "player-uuid", c.playerId().toString());
            yaml.set(p + "player-name", c.playerName());
            yaml.set(p + "message", c.message());
            yaml.set(p + "world", c.world());
            yaml.set(p + "x", c.x());
            yaml.set(p + "y", c.y());
            yaml.set(p + "z", c.z());
            yaml.set(p + "created-at", c.createdAt());
            yaml.set(p + "resolved", c.resolved());
        });
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Не удалось сохранить data.yml: " + ex.getMessage());
        }
    }

    public Fine addFine(UUID player, String name, double amount, long deadline, String reason,
                        UUID issuer, String issuerName) {
        Fine fine = new Fine(nextFineId++, player, name, amount, System.currentTimeMillis(), deadline,
                reason, issuer, issuerName, false, false);
        fines.put(fine.id(), fine);
        save();
        return fine;
    }

    public GuardCall addCall(UUID player, String name, String message, String world,
                             double x, double y, double z) {
        GuardCall call = new GuardCall(nextCallId++, player, name, message, world, x, y, z,
                System.currentTimeMillis(), false);
        calls.put(call.id(), call);
        save();
        return call;
    }

    public Collection<Fine> fines() { return List.copyOf(fines.values()); }
    public Collection<GuardCall> calls() { return List.copyOf(calls.values()); }
    public Set<UUID> guards() { return Set.copyOf(guards); }
    public Map<UUID, String> applications() { return Map.copyOf(applications); }
    public Optional<Fine> fine(int id) { return Optional.ofNullable(fines.get(id)); }
    public Optional<GuardCall> call(int id) { return Optional.ofNullable(calls.get(id)); }

    public void setFinePaid(int id, boolean paid) {
        Fine fine = fines.get(id);
        if (fine != null) { fines.put(id, fine.withPaid(paid)); save(); }
    }

    public void setFineNotified(int id) {
        Fine fine = fines.get(id);
        if (fine != null) { fines.put(id, fine.withOverdueNotified(true)); save(); }
    }

    public void resolveCall(int id) {
        GuardCall call = calls.get(id);
        if (call != null) { calls.put(id, call.withResolved(true)); save(); }
    }

    public void apply(UUID uuid, String name) { applications.put(uuid, name); save(); }
    public void addGuard(UUID uuid) { guards.add(uuid); applications.remove(uuid); save(); }
    public void removeGuard(UUID uuid) { guards.remove(uuid); save(); }
    public void reject(UUID uuid) { applications.remove(uuid); save(); }

    private static UUID parseUuid(String value) {
        try { return value == null ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ex) { return null; }
    }
}
