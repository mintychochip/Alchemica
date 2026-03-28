package org.aincraft.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.aincraft.CustomRecipe;
import org.aincraft.IBrewAPI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

final class BrewAPIImpl implements IBrewAPI {

  private final ConcurrentHashMap<NamespacedKey, CustomRecipe> customRecipes =
      new ConcurrentHashMap<>();

  @Override
  public void registerRecipe(CustomRecipe recipe) {
    if (customRecipes.containsKey(recipe.getKey())) {
      Logger.getLogger("Alchemica").warning(
          "[Alchemica] Custom recipe '" + recipe.getKey() + "' replaced an existing registration.");
    }
    customRecipes.put(recipe.getKey(), recipe);
    String perm = recipe.getPermission();
    if (Bukkit.getPluginManager().getPermission(perm) == null) {
      Bukkit.getPluginManager().addPermission(new Permission(perm, PermissionDefault.TRUE));
    }
  }

  @Override
  public void unregisterRecipe(NamespacedKey key) {
    customRecipes.remove(key);
  }

  Map<NamespacedKey, CustomRecipe> getCustomRecipes() {
    return customRecipes;
  }
}
