package org.aincraft.ingredient;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class CauldronIngredient {

  private final ItemStack stack;
  private final int amount;

  public CauldronIngredient(ItemStack stack, int amount) {
    this.stack = stack;
    this.amount = amount;
  }

  public ItemStack getStack() {
    return stack;
  }

  public int getRequired() {
    return amount;
  }

  @Override
  public int hashCode() {
    return stack.hashCode() + amount;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    CauldronIngredient that = (CauldronIngredient) obj;

    return amount == that.amount &&
        stack.isSimilar(that.stack);
  }

  @Override
  public String toString() {
    return "Ingredient{" +
        "material=" + stack.getType() +
        ", amount=" + amount +
        '}';
  }

  public static CauldronIngredient create(Material material) {
    return new CauldronIngredient(ItemStack.of(material), 1);
  }

  public static CauldronIngredient create(Material material, int amount) {
    return new CauldronIngredient(ItemStack.of(material), amount);
  }
}
