package org.aincraft.internal;

import com.google.common.primitives.UnsignedInteger;
import java.util.UUID;
import org.aincraft.dao.IPlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class PlayerSettings implements IPlayerSettings {

  private final UUID playerId;
  private UnsignedInteger modifierCount;
  private UnsignedInteger effectCount;

  public PlayerSettings(UUID key, UnsignedInteger modifierCount, UnsignedInteger effectCount) {
    this.playerId = key;
    this.modifierCount = modifierCount;
    this.effectCount = effectCount;
  }

  @Override
  public OfflinePlayer getPlayer() {
    return Bukkit.getOfflinePlayer(playerId);
  }

  @Override
  public UnsignedInteger getModifierCount() {
    return modifierCount;
  }

  @Override
  public UnsignedInteger getEffectCount() {
    return effectCount;
  }

  @Override
  public void setModifierCount(UnsignedInteger modifierCount) {
    this.modifierCount = modifierCount;
  }

  @Override
  public void setEffectCount(UnsignedInteger effectCount) {
    this.effectCount = effectCount;
  }

  @Override
  public UUID getKey() {
    return playerId;
  }
}
