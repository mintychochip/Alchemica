package org.aincraft.providers;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public class MaterialProvider implements IMaterialProvider {

  @Override
  public NamespacedKey getMaterialKey(ItemStack stack) {
    return stack.getType().getKey();
  }
}
