package org.aincraft.internal;

import org.bukkit.potion.PotionEffectType;

public interface IPotionDurationMap {

  IDurationStage getDuration(PotionEffectType type) throws IllegalArgumentException;
  boolean hasDuration(PotionEffectType type);
}
