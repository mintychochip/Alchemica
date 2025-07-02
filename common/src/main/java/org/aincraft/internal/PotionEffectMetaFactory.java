package org.aincraft.internal;

import org.aincraft.IDurationStage;
import org.aincraft.IPotionEffectMeta;
import org.bukkit.potion.PotionEffectType;

final class PotionEffectMetaFactory {

  private final IPotionDurationMap durationMap;

  PotionEffectMetaFactory(IPotionDurationMap durationMap) {
    this.durationMap = durationMap;
  }

  IPotionEffectMeta create(PotionEffectType type) throws IllegalArgumentException {
    return new PotionEffectMeta(durationMap.getDuration(type));
  }

  private static final class PotionEffectMeta implements IPotionEffectMeta {

    private IDurationStage duration;
    private int amplifier;
    private boolean ambient;
    private boolean particles;

    PotionEffectMeta(IDurationStage duration) {
      this.duration = duration;
      this.amplifier = 0;
      this.ambient = false;
      this.particles = true;
    }

    @Override
    public boolean hasParticles() {
      return particles;
    }

    @Override
    public void setParticles(boolean state) {
      this.particles = state;
    }

    @Override
    public boolean isAmbient() {
      return ambient;
    }

    @Override
    public void setAmbient(boolean state) {
      this.ambient = state;
    }

    @Override
    public int getAmplifier() {
      return amplifier;
    }

    @Override
    public void setAmplifier(int amplifier) {
      this.amplifier = amplifier;
    }

    @Override
    public IDurationStage getDuration() {
      return duration;
    }

    @Override
    public void setDuration(IDurationStage durationStage) {
      this.duration = durationStage;
    }
  }
}
