package org.aincraft.container;

import org.aincraft.internal.IDurationStage;
import org.bukkit.potion.PotionEffectType;

public interface IPotionEffectMeta {

  IDurationStage getDuration();

  void setDuration(IDurationStage durationStage);

  int getAmplifier();

  void setAmplifier(int amplifier);

  boolean isAmbient();

  void setAmbient(boolean state);

  boolean hasParticles();

  void setParticles(boolean state);

  interface IPotionEffectMetaFactory {

    IPotionEffectMeta create(PotionEffectType type) throws IllegalArgumentException;
  }

}
