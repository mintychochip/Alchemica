package org.aincraft.internal;

import org.aincraft.IDurationStage;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class PotionEffectMeta {

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

  IDurationStage getDuration() {
    return duration;
  }

  void setDuration(IDurationStage duration) {
    this.duration = duration;
  }

  int getAmplifier() {
    return amplifier;
  }

  void setAmplifier(int amplifier) {
    this.amplifier = amplifier;
  }

  boolean isAmbient() {
    return ambient;
  }

  void setAmbient(boolean ambient) {
    this.ambient = ambient;
  }

  boolean isParticles() {
    return particles;
  }

  void setParticles(boolean particles) {
    this.particles = particles;
  }
}
