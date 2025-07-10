package org.aincraft;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IPotionResult {

  @Nullable
  ItemStack getStack();

  @Nullable
  NamespacedKey getItemKey();

  enum Status {
    NO_PERMISSION,
    BAD_RECIPE_PATH,
    MANY_EFFECTS,
    MANY_MODS,
    SUCCESS;
  };

  @NotNull
  Status getStatus();

}
