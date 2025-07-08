package org.aincraft;

import com.google.common.collect.BiMap;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface IItemRegistry extends BiMap<String, ItemStack> {

  boolean registered(ItemStack stack);

  boolean isRegistered(String itemKey);

  ItemStack getItem(String itemKey);

  NamespacedKey getOrDefault(ItemStack stack, NamespacedKey def);

  @Nullable
  NamespacedKey getKey(ItemStack stack);
}
