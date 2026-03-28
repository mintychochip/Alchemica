package org.aincraft;

import org.bukkit.NamespacedKey;

public interface IBrewAPI {

  /**
   * Registers a custom recipe. If a recipe with the same key is already registered,
   * it is replaced and a warning is logged.
   */
  void registerRecipe(CustomRecipe recipe);

  /**
   * Unregisters a recipe by key. Takes effect on the next search() call;
   * cauldrons with cached results are unaffected.
   */
  void unregisterRecipe(NamespacedKey key);
}
