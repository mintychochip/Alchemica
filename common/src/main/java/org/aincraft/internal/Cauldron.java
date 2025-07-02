package org.aincraft.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.aincraft.CauldronIngredient;
import org.bukkit.Location;

public final class Cauldron {

  private final UUID id;

  private final Location location;

  private final List<CauldronIngredient> ingredients;

  private int completed;

  public Cauldron(UUID id, Location location, List<CauldronIngredient> ingredients, int completed) {
    this.id = id;
    this.location = location;
    this.ingredients = ingredients;
    this.completed = completed;
  }

  static Cauldron create(Location location) {
    UUID id = UUID.randomUUID();
    return new Cauldron(id,location,new ArrayList<>(), 0);
  }

  public Location getLocation() {
    return location;
  }

  public UUID getId() {
    return id;
  }

  public List<CauldronIngredient> getIngredients() {
    return ingredients;
  }

  public void addIngredient(CauldronIngredient ingredient) {
    if (ingredients.isEmpty()) {
      ingredients.add(ingredient);
      return;
    }
    CauldronIngredient lastIngredient = ingredients.get(ingredients.size() - 1);
    if (lastIngredient.key().equals(ingredient.key())) {
      lastIngredient.updateAmount(amount -> amount + ingredient.getAmount());
    } else {
      ingredients.add(ingredient);
    }
  }

  public void setCompleted(boolean completed) {
    this.completed = completed ? 1 : 0;
  }

  public boolean isCompleted() {
    return completed == 1;
  }
}
