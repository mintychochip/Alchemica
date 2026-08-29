package org.aincraft.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.aincraft.CauldronIngredient;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class BrewCompleteDomainEvent
    implements org.aincraft.api.event.Event, org.aincraft.api.event.Cancellable {

  private final Player player;
  private final List<CauldronIngredient> ingredients;
  private final NamespacedKey recipeKey;
  private final ItemStack initialResult;
  private ItemStack result;
  private boolean cancelled;

  BrewCompleteDomainEvent(
      Player player,
      List<CauldronIngredient> ingredients,
      ItemStack result,
      NamespacedKey recipeKey) {
    this.player = player;
    this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
    this.initialResult = result;
    this.result = result;
    this.recipeKey = recipeKey;
  }

  Player getPlayer() {
    return player;
  }

  List<CauldronIngredient> getIngredients() {
    return ingredients;
  }

  @Nullable
  NamespacedKey getRecipeKey() {
    return recipeKey;
  }

  @Nullable
  ItemStack getResult() {
    return result;
  }

  ItemStack getInitialResult() {
    return initialResult;
  }

  void setResult(@Nullable ItemStack result) {
    this.result = result;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
