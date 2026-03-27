package org.aincraft.gui.browser;

import org.aincraft.gui.AlchemicaGui;
import org.aincraft.io.RecipeYmlWriter;
import org.aincraft.wizard.WizardSession;
import org.aincraft.wizard.WizardSessionFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

/** Stub — full implementation in Task 18. */
public final class RecipeDetailGui implements AlchemicaGui {
    public RecipeDetailGui(Plugin plugin, Player player, String recipeKey,
            WizardSessionFactory sessionFactory, Runnable onBack,
            java.util.function.Consumer<WizardSession> onEdit,
            Runnable onDelete, RecipeYmlWriter writer) {}
    public void open() {}
    @Override public void onClick(InventoryClickEvent event) {}
    @Override public Inventory getInventory() { return null; }
}
