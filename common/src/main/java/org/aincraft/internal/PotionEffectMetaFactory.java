package org.aincraft.internal;

import org.bukkit.potion.PotionEffectType;

final class PotionEffectMetaFactory {

  private final IPotionDurationMap durationMap;

  PotionEffectMetaFactory(IPotionDurationMap durationMap) {
    this.durationMap = durationMap;
  }

  PotionEffectMeta create(PotionEffectType type) throws IllegalArgumentException {
    return new PotionEffectMeta(durationMap.getDuration(type));
  }




}
