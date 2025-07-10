/*
package org.aincraft.internal;

import com.google.common.primitives.UnsignedInteger;
import java.util.UUID;
import org.aincraft.dao.IDao;
import org.aincraft.dao.IPlayerSettings;
import org.aincraft.internal.PotionResult.PotionResultContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PotionContextEvaluator {

  private final IDao<IPlayerSettings, UUID> playerSettingsDao;

  PotionContextEvaluator(IDao<IPlayerSettings, UUID> playerSettingsDao) {
    this.playerSettingsDao = playerSettingsDao;
  }

  @Nullable
  public PotionResult fromContext(@NotNull Player player, @NotNull PotionResultContext context) {
    IPlayerSettings playerSettings;
    try {
      playerSettings = playerSettingsDao.get(player.getUniqueId(),
          () -> new PlayerSettings(player.getUniqueId(),
              UnsignedInteger.ONE, UnsignedInteger.ONE));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }
}
*/
