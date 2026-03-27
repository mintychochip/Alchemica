package org.aincraft.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * All Alchemica GUI classes implement this interface.
 * The {@link GuiListener} dispatches {@link InventoryClickEvent} to the correct GUI
 * by checking the holder type.
 */
public interface AlchemicaGui extends InventoryHolder {

    /** Called when the player clicks any slot in this GUI's inventory. */
    void onClick(InventoryClickEvent event);
}
