package org.aincraft.internal;

import com.google.common.collect.ForwardingList;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.aincraft.CauldronIngredient;
import org.aincraft.container.LocationKey;
import org.aincraft.dao.ICauldron;
import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class Cauldron implements ICauldron {

  private final UUID cauldronId;

  private final Location location;

  private final List<CauldronIngredient> ingredients;

  private int completed;


  public Cauldron(UUID cauldronId, Location location, List<CauldronIngredient> ingredients,
      int completed) {
    this.cauldronId = cauldronId;
    this.location = location;
    this.ingredients = ingredients;
    this.completed = completed;
  }

  static Cauldron create(UUID id, Location location, List<CauldronIngredient> ingredients, int completed) {
    return new Cauldron(id,location,new CauldronIngredientList(ingredients),completed);
  }

  static Cauldron create(Location location) {
    return create(UUID.randomUUID(), location, new ArrayList<>(), 0);
  }

  @Override
  public Location getLocation() {
    return location;
  }

  @Override
  public @NotNull List<CauldronIngredient> getIngredients() {
    return ingredients;
  }

  @Override
  public UUID getCauldronId() {
    return cauldronId;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed ? 1 : 0;
  }

  public boolean isCompleted() {
    return completed == 1;
  }

  @Override
  public LocationKey getKey() {
    return new LocationKey(location);
  }

  @ApiStatus.Internal
  private static final class CauldronIngredientList extends ForwardingList<CauldronIngredient> {

    private final List<CauldronIngredient> ingredientList;

    private CauldronIngredientList(List<CauldronIngredient> ingredientList) {
      this.ingredientList = ingredientList;
    }

    @Override
    protected List<CauldronIngredient> delegate() {
      return ingredientList;
    }

    @Override
    public boolean add(CauldronIngredient element) {
      if (ingredientList.isEmpty()) {
        return ingredientList.add(element);
      }
      CauldronIngredient last = get(size() - 1);
      if (last.isSimilar(element)) {
        last.setAmount(element.getAmount() + last.getAmount());
        return true;
      }
      return ingredientList.add(element);
    }
  }
}
