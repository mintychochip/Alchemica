package org.aincraft.gui.wizard;

import java.util.function.Consumer;
import org.aincraft.gui.AlchemicaGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

/** Stub — full implementation in Task 13. */
public final class MaterialPickerGui implements AlchemicaGui {
    public MaterialPickerGui(Plugin plugin, Player player,
            Consumer<Material> onSelect, Runnable onBack) {}
    public void open() {}
    @Override public void onClick(InventoryClickEvent event) {}
    @Override public Inventory getInventory() { return null; }
}
