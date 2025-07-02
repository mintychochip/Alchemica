package org.aincraft;

public interface IPotionModifier {

  void modify(IPotionEffectMeta meta);

  ModifierType type();

  enum ModifierType {
    DURATION,
    AMPLIFIER,
    AMBIENT,
    PARTICLES
  }
}
