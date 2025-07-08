package org.aincraft.providers;

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public class CauldronProvider implements ICauldronProvider {

  @Override
  public Material[] getHeatSources() {
    return new Material[]{Material.FIRE, Material.SOUL_CAMPFIRE, Material.CAMPFIRE,
        Material.SOUL_FIRE, Material.MAGMA_BLOCK, Material.LAVA};
  }

  @Override
  public boolean isWaterCauldron(Block block) {
    return block.getType() == Material.WATER_CAULDRON;
  }

  @Override
  public int getCauldronLevel(Block block) throws IllegalArgumentException {
    Preconditions.checkArgument(isWaterCauldron(block));
    BlockData blockData = block.getBlockData();
    Levelled levelled = (Levelled) blockData;
    return levelled.getLevel();
  }

  @Override
  public void setCauldronLevel(Block block, int level) throws IllegalArgumentException {
    Preconditions.checkArgument(level <= 3 && level >= 0);
    Preconditions.checkArgument(isWaterCauldron(block));
    BlockData blockData = block.getBlockData();
    Levelled levelled = (Levelled) blockData;
    if (level > 0) {
      levelled.setLevel(level);
      block.setBlockData(blockData);
      return;
    }
    block.setType(Material.CAULDRON);
  }

  @Override
  public void playAddIngredientEffect(Block block, Player player) {
    Location blockLocation = block.getLocation();
    player.spawnParticle(Particle.WITCH, blockLocation.clone().add(0.5, 1, 0.5), 10, 0.1, 0.1, 0.1);
    player.playSound(blockLocation, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
  }

  @Override
  public void playStirEffect(Block block, Player player) {
    player.playSound(player, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
  }

  @Override
  public int getNewLevel(CauldronLevelChangeEvent event) {
    BlockState newState = event.getNewState();
    BlockData newBlock = newState.getBlockData();
    return (newBlock instanceof Levelled) ? ((Levelled) newBlock).getLevel()
        : ((newBlock.getMaterial() == Material.CAULDRON) ? 0 : 3);
  }

  @Override
  public int getOldLevel(CauldronLevelChangeEvent event) {
    Block block = event.getBlock();
    BlockData blockData = block.getBlockData();
    return blockData instanceof Levelled ? ((Levelled) blockData).getLevel()
        : (block.getType() == Material.CAULDRON ? 0 : 3);
  }
}
