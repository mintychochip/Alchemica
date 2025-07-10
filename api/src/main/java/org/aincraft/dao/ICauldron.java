package org.aincraft.dao;

import java.util.List;
import java.util.UUID;
import org.aincraft.CauldronIngredient;
import org.aincraft.container.LocationKey;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface ICauldron extends IDaoObject<LocationKey> {
  @NotNull
  List<CauldronIngredient> getIngredients();
  UUID getCauldronId();
  boolean isCompleted();
  void setCompleted(boolean completed);
  Location getLocation();
}
