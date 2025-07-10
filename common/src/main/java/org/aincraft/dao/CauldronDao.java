package org.aincraft.dao;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.aincraft.CauldronIngredient;
import org.aincraft.IStorage;
import org.aincraft.container.LocationKey;
import org.aincraft.internal.Cauldron;

public final class CauldronDao extends AbstractDao<ICauldron, LocationKey> {

  private final IStorage storage;
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

  public CauldronDao(IStorage storage, Gson gson) {
    this.storage = storage;
    this.gson = gson;
  }

  @Override
  protected IDaoQueryObject<ICauldron, LocationKey> delegate() {
    return new IDaoQueryObject<ICauldron, LocationKey>() {
      @Override
      public IStorage getStorage() {
        return storage;
      }

      @Override
      public String getSelectQuery() {
        return GET_CAULDRON_BY_LOCATION;
      }

      @Override
      public String getInsertQuery() {
        return INSERT_CAULDRON;
      }

      @Override
      public String getDeleteQuery() {
        return DELETE_CAULDRON_BY_LOCATION;
      }

      @Override
      public String getUpdateQuery() {
        return UPDATE_CAULDRON;
      }

      @Override
      public String getExistsQuery() {
        return CHECK_CAULDRON_EXISTS_BY_LOCATION;
      }

      @Override
      public Object[] getExistsQueryArguments(LocationKey key) {
        return getSpreadLocationKey(key);
      }

      @Override
      public Object[] insertQueryArguments(ICauldron object) {
        LocationKey key = object.getKey();
        String ingredientJson = gson.toJson(object.getIngredients());
        return new Object[]{object.getCauldronId().toString(), key.getWorldName(), key.getBlockX(), key.getBlockY(),
            key.getBlockZ(), ingredientJson, object.isCompleted()};
      }

      @Override
      public Object[] getDeleteQueryArguments(LocationKey key) {
        return getSpreadLocationKey(key);
      }

      @Override
      public Object[] getUpdateQueryArguments(ICauldron object) {
        String ingredientJson = gson.toJson(object.getIngredients());
        return new Object[]{ingredientJson, object.isCompleted(), object.getCauldronId().toString()};
      }

      @Override
      public Object[] getSelectQueryArguments(LocationKey key) {
        return getSpreadLocationKey(key);
      }

      @Override
      public Function<ResultSet, ICauldron> getSelectIfExists(LocationKey key) {
       return scanner -> {
         try {
           UUID id = UUID.fromString(scanner.getString("id"));
           String rawIngredientString = scanner.getString("ingredients");
           List<CauldronIngredient> cauldronIngredients = gson.fromJson(
               rawIngredientString,
               new TypeToken<List<CauldronIngredient>>() {
               }.getType()
           );
           int completed = scanner.getInt("completed");
           return new Cauldron(id, key.getLocation(), cauldronIngredients, completed);
         } catch (SQLException e) {
           throw new RuntimeException(e);
         }
       };
      }
    };
  }

  private static Object[] getSpreadLocationKey(LocationKey key) {
    return new Object[]{key.getWorldName(), key.getBlockX(), key.getBlockY(), key.getBlockZ()};
  }
}
