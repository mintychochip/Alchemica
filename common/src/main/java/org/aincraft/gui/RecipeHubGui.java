package org.aincraft.gui;

import org.aincraft.wizard.WizardSessionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * The hub GUI: offers "Create Recipe" and "Browse Recipes".
 * The calling code is responsible for opening it with {@link Player#openInventory}.
 */
public final class RecipeHubGui implements AlchemicaGui {

    private final Inventory inventory;
    private final Player player;
    private final WizardSessionManager sessionManager;
    private final Runnable openWizard;
    private final Runnable openBrowser;

    public RecipeHubGui(Player player, WizardSessionManager sessionManager,
            Runnable openWizard, Runnable openBrowser) {
        this.player = player;
        this.sessionManager = sessionManager;
        this.openWizard = openWizard;
        this.openBrowser = openBrowser;
        this.inventory = buildInventory();
    }

    private Inventory buildInventory() {
        Inventory inv = Bukkit.createInventory(this, 27, "Alchemica \u2014 Recipe Manager");
        inv.setItem(11, GuiUtils.named(Material.CAULDRON, "&aCreate Recipe",
            "&7Start the recipe wizard"));
        inv.setItem(15, GuiUtils.named(Material.BOOK, "&eBrowse Recipes",
            "&7View and edit existing recipes"));
        GuiUtils.fillEmpty(inv);
        return inv;
    }

    @Override
    public Inventory getInventory() { return inventory; }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 11) openWizard.run();
        else if (slot == 15) openBrowser.run();
    }
}
