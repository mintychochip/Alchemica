package org.aincraft.internal;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.aincraft.CauldronIngredient;
import org.aincraft.IExecutor;
import org.aincraft.IStorage;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

final class CauldronDao {

  private final IStorage database;
  private final Gson gson;
  private static final String GET_CAULDRON_BY_LOCATION = "SELECT id,ingredients, completed FROM cauldrons WHERE world=? AND x=? AND y=? AND z=?";
  private static final String INSERT_CAULDRON =
      "INSERT INTO cauldrons (id, world, x, y, z, ingredients, completed) VALUES (?, ?, ?, ?, ?, ?, ?)";
  private static final String DELETE_CAULDRON_BY_LOCATION =
      "DELETE FROM cauldrons WHERE world=? AND x=? AND y=? AND z=?";
  private static final String UPDATE_CAULDRON =
      "UPDATE cauldrons SET ingredients=?,completed=? WHERE id=?";
  private static final String CHECK_CAULDRON_EXISTS_BY_LOCATION =
      "SELECT 1 FROM cauldrons WHERE world=? AND x=? AND y=? AND z=?";

  private final Cache<LocationKey, Cauldron> locationCache = CacheBuilder.newBuilder()
      .expireAfterWrite(10,
          TimeUnit.MINUTES).build();

  CauldronDao(IStorage storage, Gson gson) {
    this.database = storage;
    this.gson = gson;
  }

  @NotNull
  public Cauldron getCauldron(@NotNull Location location, Callable<? extends Cauldron> loader)
      throws Exception {
    Preconditions.checkState(!database.isClosed(), "db is closed");
    if (!hasCauldron(location)) {
      Cauldron cauldron = loader.call();
      insertCauldron(cauldron);
      return cauldron;
    }
    return getCauldronIfExists(location);
  }

  public Cauldron getCauldronIfExists(@NotNull Location location) throws ExecutionException {
//    Preconditions.checkState(!database.isClosed(), "db is closed");
    return locationCache.get(LocationKey.create(location), () -> {
      IExecutor executor = database.getExecutor();
      World world = location.getWorld();
      return executor.queryRow(
          scanner -> {
            try {
              UUID id = UUID.fromString(scanner.getString("id"));
              String rawIngredientString = scanner.getString("ingredients");
              List<CauldronIngredient> cauldronIngredients = gson.fromJson(
                  rawIngredientString,
                  new TypeToken<List<CauldronIngredient>>() {
                  }.getType()
              );
              int completed = scanner.getInt("completed");
              return new Cauldron(id, location, cauldronIngredients, completed);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          },
          GET_CAULDRON_BY_LOCATION,
          world.getName(),
          location.getBlockX(),
          location.getBlockY(),
          location.getBlockZ()
      );
    });
  }

  public boolean insertCauldron(@NotNull Cauldron cauldron) {
    Preconditions.checkState(!database.isClosed(), "db is closed");
    if (hasCauldron(cauldron.getLocation())) {
      return false;
    }
    IExecutor executor = database.getExecutor();
    Location loc = cauldron.getLocation();
    String ingredientsJson = gson.toJson(cauldron.getIngredients());

    boolean result = executor.executeUpdate(
        INSERT_CAULDRON,
        cauldron.getId().toString(),
        loc.getWorld().getName(),
        loc.getBlockX(),
        loc.getBlockY(),
        loc.getBlockZ(),
        ingredientsJson,
        cauldron.isCompleted()
    );

    if (result) {
      locationCache.put(LocationKey.create(loc), cauldron);
      return true;
    }

    return false;
  }

  public boolean hasCauldron(@NotNull Location location) throws IllegalStateException {
    Preconditions.checkState(!database.isClosed(), "db is closed");
    Cauldron cauldron = locationCache.getIfPresent(LocationKey.create(location));
    if (cauldron != null) {
      return true;
    }

    if (database.isClosed()) {
      return false;
    }

    IExecutor executor = database.getExecutor();
    World world = location.getWorld();

    return executor.queryRow(
        scanner -> {
          try {
            return scanner.getBoolean(1);
          } catch (SQLException ex) {
            throw new RuntimeException(ex.getMessage());
          }
        },
        CHECK_CAULDRON_EXISTS_BY_LOCATION,
        world.getName(),
        location.getBlockX(),
        location.getBlockY(),
        location.getBlockZ()
    ) != null;
  }

  public boolean deleteCauldron(@NotNull Location location) throws IllegalStateException {
    Preconditions.checkState(!database.isClosed(), "db is closed");
    if (database.isClosed()) {
      return false;
    }

    IExecutor executor = database.getExecutor();
    World world = location.getWorld();
    boolean result = executor.executeUpdate(
        DELETE_CAULDRON_BY_LOCATION,
        world.getName(),
        location.getBlockX(),
        location.getBlockY(),
        location.getBlockZ()
    );
    if (result) {
      locationCache.invalidate(LocationKey.create(location));
      return true;
    }
    return false;
  }

  public void updateCauldron(@NotNull Cauldron cauldron) throws IllegalStateException {
    Preconditions.checkState(!database.isClosed(), "db is closed");
    if (database.isClosed()) {
      return;
    }
    IExecutor executor = database.getExecutor();
    String ingredientJson = gson.toJson(cauldron.getIngredients());
    String idString = cauldron.getId().toString();
    executor.executeUpdate(UPDATE_CAULDRON, ingredientJson, cauldron.isCompleted(), idString);
    locationCache.put(LocationKey.create(cauldron.getLocation()), cauldron);
  }

  private static final class LocationKey {

    private final String world;
    private final int x;
    private final int y;
    private final int z;

    LocationKey(@NotNull String world, int x, int y, int z) {
      this.world = world;
      this.x = x;
      this.y = y;
      this.z = z;
    }

    @Override
    public int hashCode() {
      int result = world.hashCode();
      result = 31 * result + x;
      result = 31 * result + y;
      result = 31 * result + z;
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
      return world.equals(locationKey.world) &&
          x == locationKey.x &&
          y == locationKey.y &&
          z == locationKey.z;
    }


    private static LocationKey create(Location location) {
      return new LocationKey(location.getWorld().getName(), location.getBlockX(),
          location.getBlockY(), location.getBlockZ());
    }
  }


}
