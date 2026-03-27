package org.aincraft.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Dispatches inventory click events to the correct {@link AlchemicaGui} implementation
 * by checking the inventory holder type. Cancels all clicks to prevent item movement.
 */
public final class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AlchemicaGui gui)) return;
        event.setCancelled(true);
        gui.onClick(event);
    }
}
