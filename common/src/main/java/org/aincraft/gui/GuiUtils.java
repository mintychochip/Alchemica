package org.aincraft.gui;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuiUtils {

    private GuiUtils() {}

    /** Creates a named item with optional lore. */
    public static ItemStack named(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                meta.setLore(List.of(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Fills all null slots in a row (0-based row index) with gray glass panes. */
    public static void fillRow(Inventory inv, int row) {
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        int start = row * 9;
        for (int i = start; i < start + 9; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }

    /** Fills all null slots in the entire inventory with gray glass panes. */
    public static void fillEmpty(Inventory inv) {
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }
}
