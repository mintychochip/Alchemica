package org.aincraft.gui.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class MaterialPickerGui implements AlchemicaGui {

    private static final List<Material> MATERIALS;

    static {
        List<Material> list = new ArrayList<>();
        for (Material m : Material.values()) {
            if (m.isItem() && m != Material.AIR && !m.isLegacy()) {
                list.add(m);
            }
        }
        MATERIALS = List.copyOf(list);
    }

    private static final int PAGE_SIZE = 45; // 5 rows × 9

    private final Plugin plugin;
    private final Player player;
    private final Consumer<Material> onSelect;
    private final Runnable onBack;
    private int page = 0;
    private Inventory currentInventory;

    public MaterialPickerGui(Plugin plugin, Player player,
            Consumer<Material> onSelect, Runnable onBack) {
        this.plugin = plugin;
        this.player = player;
        this.onSelect = onSelect;
        this.onBack = onBack;
    }

    public void open() {
        buildAndOpen(0);
    }

    private void buildAndOpen(int page) {
        this.page = page;
        Inventory inv = Bukkit.createInventory(this, 54, "Select Ingredient");
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < MATERIALS.size(); i++) {
            Material m = MATERIALS.get(start + i);
            inv.setItem(i, GuiUtils.named(m, "&f" + m.name().toLowerCase()
                .replace('_', ' ')));
        }
        // Controls row (row 6)
        if (page > 0) {
            inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Previous"));
        }
        inv.setItem(49, GuiUtils.named(Material.BARRIER, "&cBack"));
        if (start + PAGE_SIZE < MATERIALS.size()) {
            inv.setItem(53, GuiUtils.named(Material.ARROW, "&7Next"));
        }
        GuiUtils.fillRow(inv, 5);
        currentInventory = inv;
        player.openInventory(inv);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) { onBack.run(); return; }
        if (slot == 45 && page > 0) { buildAndOpen(page - 1); return; }
        if (slot == 53 && (page + 1) * PAGE_SIZE < MATERIALS.size()) {
            buildAndOpen(page + 1); return;
        }
        if (slot < 45) {
            int idx = page * PAGE_SIZE + slot;
            if (idx < MATERIALS.size()) {
                onSelect.accept(MATERIALS.get(idx));
            }
        }
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
