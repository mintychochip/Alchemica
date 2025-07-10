package org.aincraft.container;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public final class LocationKey {

  @NotNull
  private final Location location;

  public LocationKey(@NotNull Location location) {
    this.location = location;
  }

  @Override
  public int hashCode() {
    World world = location.getWorld();
    String worldName = world.getName();
    int result = worldName.hashCode();
    result = 31 * result + getBlockX();
    result = 31 * result + getBlockY();
    result = 31 * result + getBlockZ();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof LocationKey)) {
      return false;
    }
    LocationKey locationKey = (LocationKey) obj;
    return getWorldName().equals(locationKey.getWorldName())
        && getBlockX() == locationKey.getBlockX() && getBlockY() == locationKey.getBlockY()
        && getBlockZ() == locationKey.getBlockZ();
  }

  public String getWorldName() {
    World world = location.getWorld();
    return world.getName();
  }

  public int getBlockX() {
    return location.getBlockX();
  }

  public int getBlockY() {
    return location.getBlockY();
  }

  public int getBlockZ() {
    return location.getBlockZ();
  }

  public @NotNull Location getLocation() {
    return location;
  }
}


