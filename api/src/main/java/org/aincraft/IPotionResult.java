package org.aincraft;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface IPotionResult {

  ItemStack getStack();

  @Nullable
  NamespacedKey getItemKey();
}
