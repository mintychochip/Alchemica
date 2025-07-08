package org.aincraft.providers;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public interface IMaterialProvider {

  NamespacedKey getMaterialKey(ItemStack stack);

}
