package org.aincraft.container;

import org.bukkit.inventory.ItemStack;

public final class RegistrableItem {

  private final String itemKey;
  private final ItemStack stack;

  public RegistrableItem(String itemKey, ItemStack stack) {
    this.itemKey = itemKey;
    this.stack = stack;
  }

  public String getItemKey() {
    return itemKey;
  }

  public ItemStack getStack() {
    return stack;
  }
}
