package ru.aurion.kara.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class KaraMenuHolder implements InventoryHolder {
    private final String type;
    private final int page;
    private final String filter;
    private Inventory inventory;

    public KaraMenuHolder(String type, int page, String filter) {
        this.type = type;
        this.page = page;
        this.filter = filter;
    }

    void inventory(Inventory inventory) { this.inventory = inventory; }
    public String type() { return type; }
    public int page() { return page; }
    public String filter() { return filter; }
    @Override public Inventory getInventory() { return inventory; }
}
