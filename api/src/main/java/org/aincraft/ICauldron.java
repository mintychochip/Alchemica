package org.aincraft;

import java.util.List;
import java.util.UUID;
import org.bukkit.Location;

public interface ICauldron {
  UUID getId();
  Location getLocation();
  List<CauldronIngredient> getIngredients();
}
