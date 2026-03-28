package org.aincraft;

import java.util.Collections;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

public final class BrewContext {

  private final Player player;
  private final NamespacedKey recipeKey;
  private final List<CauldronIngredient> ingredients;

  public BrewContext(Player player, NamespacedKey recipeKey,
      List<CauldronIngredient> ingredients) {
    this.player = player;
    this.recipeKey = recipeKey;
    this.ingredients = Collections.unmodifiableList(ingredients);
  }

  public Player getPlayer() { return player; }
  public NamespacedKey getRecipeKey() { return recipeKey; }
  public List<CauldronIngredient> getIngredients() { return ingredients; }
}
