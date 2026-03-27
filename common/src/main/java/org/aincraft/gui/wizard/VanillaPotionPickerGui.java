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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

public final class VanillaPotionPickerGui implements AlchemicaGui {

    private static final List<PotionType> TYPES;
    static {
        List<PotionType> list = new ArrayList<>();
        for (PotionType t : PotionType.values()) {
            list.add(t);
        }
        TYPES = List.copyOf(list);
    }

    private static final int PAGE_SIZE = 45;

    private final Plugin plugin;
    private final Player player;
    private final Consumer<PotionType> onSelect;
    private final Runnable onBack;
    private int page = 0;
    private Inventory currentInventory;

    public VanillaPotionPickerGui(Plugin plugin, Player player,
            Consumer<PotionType> onSelect, Runnable onBack) {
        this.plugin = plugin;
        this.player = player;
        this.onSelect = onSelect;
        this.onBack = onBack;
    }

    public void open() { buildAndOpen(0); }

    private void buildAndOpen(int p) {
        this.page = p;
        Inventory inv = Bukkit.createInventory(this, 54, "Select Potion Type");
        int start = p * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < TYPES.size(); i++) {
            PotionType type = TYPES.get(start + i);
            ItemStack stack = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) stack.getItemMeta();
            if (meta != null) {
                try {
                    meta.setBasePotionType(type);
                } catch (NoSuchMethodError ignored) {
                    // pre-1.20.5 fallback
                    try {
                        meta.getClass().getMethod("setBasePotionData",
                            org.bukkit.potion.PotionData.class)
                            .invoke(meta, new org.bukkit.potion.PotionData(type));
                    } catch (Exception e2) { /* ignore */ }
                }
                meta.setDisplayName("&f" + type.name().toLowerCase().replace('_', ' '));
                stack.setItemMeta(meta);
            }
            inv.setItem(i, stack);
        }
        if (p > 0) inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Previous"));
        inv.setItem(49, GuiUtils.named(Material.BARRIER, "&cBack"));
        if (start + PAGE_SIZE < TYPES.size())
            inv.setItem(53, GuiUtils.named(Material.ARROW, "&7Next"));
        GuiUtils.fillRow(inv, 5);
        currentInventory = inv;
        player.openInventory(inv);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) { onBack.run(); return; }
        if (slot == 45 && page > 0) { buildAndOpen(page - 1); return; }
        if (slot == 53 && (page + 1) * PAGE_SIZE < TYPES.size()) {
            buildAndOpen(page + 1); return;
        }
        if (slot < 45) {
            int idx = page * PAGE_SIZE + slot;
            if (idx < TYPES.size()) onSelect.accept(TYPES.get(idx));
        }
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
