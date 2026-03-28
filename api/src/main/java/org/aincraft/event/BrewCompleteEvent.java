package org.aincraft.event;

import java.util.Collections;
import java.util.List;
import org.aincraft.CauldronIngredient;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BrewCompleteEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final List<CauldronIngredient> ingredients;
  private final @Nullable NamespacedKey recipeKey;
  private ItemStack result;
  private boolean cancelled;

  public BrewCompleteEvent(Player player, List<CauldronIngredient> ingredients,
      @NotNull ItemStack result, @Nullable NamespacedKey recipeKey) {
    this.player = player;
    this.ingredients = Collections.unmodifiableList(ingredients);
    this.result = java.util.Objects.requireNonNull(result, "result must not be null at construction");
    this.recipeKey = recipeKey;
  }

  public Player getPlayer() { return player; }
  public List<CauldronIngredient> getIngredients() { return ingredients; }
  public @Nullable NamespacedKey getRecipeKey() { return recipeKey; }
  public @Nullable ItemStack getResult() { return result; }
  public void setResult(@Nullable ItemStack result) { this.result = result; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
  @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
  public static HandlerList getHandlerList() { return HANDLERS; }
}
