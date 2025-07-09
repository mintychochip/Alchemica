package org.aincraft.providers;

import org.bukkit.Particle;
import org.bukkit.Sound;

// Versions 1.21
public final class ModernCauldronEffectProvider extends AbstractCauldronEffectProvider {

  @Override
  protected Particle getAddIngredientParticle() {
    return Particle.WITCH;
  }

  @Override
  protected Sound getAddIngredientSound() {
    return Sound.ENTITY_ILLUSIONER_CAST_SPELL;
  }
}
