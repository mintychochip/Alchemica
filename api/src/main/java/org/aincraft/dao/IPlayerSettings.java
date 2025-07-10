package org.aincraft.dao;

import com.google.common.primitives.UnsignedInteger;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.ApiStatus.AvailableSince;

@AvailableSince("1.0.2")
public interface IPlayerSettings extends IDaoObject<UUID> {
  default UUID getPlayerId() {
    return getKey();
  }
  OfflinePlayer getPlayer();
  UnsignedInteger getModifierCount();
  UnsignedInteger getEffectCount();
  void setModifierCount(UnsignedInteger modifierCount);
  void setEffectCount(UnsignedInteger effectCount);
}
