package org.aincraft.providers;

import org.bukkit.Particle;
import org.bukkit.Sound;

//Versions 1.13-1.20
public class v1_13_CauldronEffectProvider extends AbstractCauldronEffectProvider {

  @Override
  protected Particle getAddIngredientParticle() {
    return Particle.SPELL_WITCH;
  }

  @Override
  protected Sound getAddIngredientSound() {
    return Sound.ENTITY_ILLUSIONER_CAST_SPELL;
  }
}
