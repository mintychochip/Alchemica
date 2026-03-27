package org.aincraft.gui.browser;

import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.aincraft.io.RecipeYmlWriter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class RecipeDeleteConfirmGui implements AlchemicaGui {

    private final Player player;
    private final String recipeKey;
    private final boolean isCustom;
    private final RecipeYmlWriter writer;
    private final Runnable onRefresh;
    private final Runnable onReturn; // returns to browser
    private Inventory currentInventory;

    public RecipeDeleteConfirmGui(Player player, String recipeKey, boolean isCustom,
            RecipeYmlWriter writer, Runnable onRefresh, Runnable onReturn) {
        this.player = player;
        this.recipeKey = recipeKey;
        this.isCustom = isCustom;
        this.writer = writer;
        this.onRefresh = onRefresh;
        this.onReturn = onReturn;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(this, 27, "Delete '" + recipeKey + "'?");
        inv.setItem(11, GuiUtils.named(Material.LIME_DYE, "&aConfirm Delete",
            "&7This cannot be undone."));
        inv.setItem(15, GuiUtils.named(Material.RED_DYE, "&cCancel"));
        GuiUtils.fillEmpty(inv);
        currentInventory = inv;
        player.openInventory(inv);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 15) { onReturn.run(); return; }
        if (slot == 11) {
            try {
                writer.delete(recipeKey, isCustom);
                onRefresh.run();
                player.sendMessage(ChatColor.GREEN + "Recipe '" + recipeKey + "' deleted.");
            } catch (java.io.IOException e) {
                player.sendMessage(ChatColor.RED + "Failed to delete recipe: " + e.getMessage());
            }
            onReturn.run();
        }
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
