package org.aincraft.providers;

import org.bukkit.Particle;
import org.bukkit.Sound;

//Versions 1.12
public class LegacyCauldronEffectProvider extends AbstractCauldronEffectProvider {

  @Override
  protected Particle getAddIngredientParticle() {
    return Particle.SPELL_WITCH;
  }

  @Override
  protected Sound getAddIngredientSound() {
    return Sound.ENTITY_EVOCATION_ILLAGER_CAST_SPELL;
  }
}
