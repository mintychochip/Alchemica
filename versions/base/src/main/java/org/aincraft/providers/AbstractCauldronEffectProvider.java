package org.aincraft.providers;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

abstract class AbstractCauldronEffectProvider implements ICauldronEffectProvider {

  protected abstract Particle getAddIngredientParticle();

  protected abstract Sound getAddIngredientSound();

  protected Sound getStirSound() {
    return Sound.BLOCK_BREWING_STAND_BREW;
  }

  @Override
  public void playAddIngredientEffect(Block block, Player player) {
    Location blockLocation = block.getLocation();
    player.spawnParticle(getAddIngredientParticle(), blockLocation.clone().add(0.5, 1.0, 0.5), 10,
        0.3,
        0.3, 0.3);
    player.playSound(blockLocation, getAddIngredientSound(), 1.0f, 1.0f);
  }

  @Override
  public void playStirEffect(Block block, Player player) {
    player.playSound(player.getLocation(), getStirSound(), 1.0f, 1.0f);
  }
}
