package org.aincraft.internal;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

final class PotionEffectMetaFactory {

  private final IPotionDurationMap durationMap;

  PotionEffectMetaFactory(IPotionDurationMap durationMap) {
    this.durationMap = durationMap;
  }

  PotionEffectMeta create(PotionEffectType type) throws IllegalArgumentException {
    return new PotionEffectMeta(durationMap.getDuration(type), 0, false, true);
  }

  PotionEffectMeta create(PotionEffect effect) {
    return new PotionEffectMeta(durationMap.getDuration(effect.getType()),effect.getAmplifier(),
        effect.isAmbient(), effect.hasParticles());
  }
}
