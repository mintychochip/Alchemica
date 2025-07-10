package org.aincraft.dao;

import com.google.common.primitives.UnsignedInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Function;
import org.aincraft.IStorage;
import org.aincraft.internal.PlayerSettings;

public final class PlayerSettingsDao extends AbstractDao<IPlayerSettings, UUID> {

  private final IStorage storage;

  private static final String GET_SETTINGS = "SELECT modifier_count,effect_count FROM player_settings WHERE id=?";
  private static final String INSERT_SETTINGS = "INSERT INTO player_settings (id,modifier_count,effect_count) VALUES (?,?,?)";
  private static final String DELETE_SETTINGS = "DELETE FROM player_settings WHERE id=?";
  private static final String UPDATE_SETTINGS = "UPDATE player_settings SET modifier_count=?,effect_count=? WHERE id=?";
  private static final String HAS_SETTINGS = "SELECT 1 FROM player_settings WHERE id=?";

  public PlayerSettingsDao(IStorage storage) {
    this.storage = storage;
  }

  @Override
  protected IDaoQueryObject<IPlayerSettings, UUID> delegate() {
    return new IDaoQueryObject<IPlayerSettings, UUID>() {
      @Override
      public IStorage getStorage() {
        return storage;
      }

      @Override
      public String getSelectQuery() {
        return GET_SETTINGS;
      }

      @Override
      public String getInsertQuery() {
        return INSERT_SETTINGS;
      }

      @Override
      public String getDeleteQuery() {
        return DELETE_SETTINGS;
      }

      @Override
      public String getUpdateQuery() {
        return UPDATE_SETTINGS;
      }

      @Override
      public String getExistsQuery() {
        return HAS_SETTINGS;
      }

      @Override
      public Object[] getExistsQueryArguments(UUID key) {
        return new Object[]{key.toString()};
      }

      @Override
      public Object[] insertQueryArguments(IPlayerSettings object) {
        return new Object[]{object.getKey().toString(), object.getModifierCount().intValue(),
            object.getEffectCount().intValue()};
      }

      @Override
      public Object[] getDeleteQueryArguments(UUID key) {
        return new Object[]{key.toString()};
      }

      @Override
      public Object[] getUpdateQueryArguments(IPlayerSettings object) {
        return new Object[]{object.getModifierCount().intValue(),
            object.getEffectCount().intValue(), object.getKey().toString()};
      }

      @Override
      public Object[] getSelectQueryArguments(UUID key) {
        return new Object[]{key.toString()};
      }

      @Override
      public Function<ResultSet, IPlayerSettings> getSelectIfExists(UUID key) {
        return result -> {
          try {
            int modifierCount = result.getInt("modifier_count");
            int effectCount = result.getInt("effect_count");
            return new PlayerSettings(key,UnsignedInteger.fromIntBits(modifierCount),UnsignedInteger.fromIntBits(effectCount));
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        };
      }
    };
  }
}
