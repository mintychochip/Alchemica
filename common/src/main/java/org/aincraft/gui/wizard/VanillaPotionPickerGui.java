package org.aincraft.gui.wizard;

import java.util.function.Consumer;
import org.aincraft.gui.AlchemicaGui;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

/** Stub — full implementation in Task 13. */
public final class VanillaPotionPickerGui implements AlchemicaGui {
    public VanillaPotionPickerGui(Plugin plugin, Player player,
            Consumer<PotionType> onSelect, Runnable onBack) {}
    public void open() {}
    @Override public void onClick(InventoryClickEvent event) {}
    @Override public Inventory getInventory() { return null; }
}
