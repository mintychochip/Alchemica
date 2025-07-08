package org.aincraft.providers;

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public final class LegacyCauldronProvider implements ICauldronProvider {

  @Override
  public Material[] getHeatSources() {
    return new Material[]{Material.FIRE, Material.LAVA};

  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean isWaterCauldron(Block block) {
    Material material = block.getType();
    if (material != Material.CAULDRON) {
      return false;
    }
    return block.getData() > 0;
  }

  @SuppressWarnings("deprecation")
  @Override
  public int getCauldronLevel(Block block) throws IllegalArgumentException {
    Preconditions.checkArgument(isWaterCauldron(block));
    return block.getData();
  }

  @SuppressWarnings("deprecation")
  @Override
  public void setCauldronLevel(Block block, int level) throws IllegalArgumentException {
    Preconditions.checkArgument(level <= 3 && level >= 0);
    Preconditions.checkArgument(isWaterCauldron(block));
    block.setData((byte) level);
  }

  @Override
  public void playAddIngredientEffect(Block block, Player player) {
    Location blockLocation = block.getLocation();
    player.spawnParticle(Particle.SPELL_WITCH, blockLocation.clone().add(0.5, 1.0, 0.5), 10, 0.3,
        0.3, 0.3);
    player.playSound(blockLocation, Sound.ENTITY_ILLUSION_ILLAGER_CAST_SPELL, 1.0f, 1.0f);
  }

  @Override
  public void playStirEffect(Block block, Player player) {
    player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
  }

  @Override
  public int getOldLevel(CauldronLevelChangeEvent event) {
    return event.getOldLevel();
  }

  @Override
  public int getNewLevel(CauldronLevelChangeEvent event) {
    return event.getNewLevel();
  }
}
