package org.aincraft;

public interface IPotionEffectMeta {

  IDurationStage getDuration();

  void setDuration(IDurationStage durationStage);

  int getAmplifier();

  void setAmplifier(int amplifier);

  boolean isAmbient();

  void setAmbient(boolean state);

  boolean hasParticles();

  void setParticles(boolean state);

}
