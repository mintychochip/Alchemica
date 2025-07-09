package org.aincraft.providers;

import com.google.common.base.Preconditions;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.block.CauldronLevelChangeEvent;

abstract class AbstractCauldronProvider implements ICauldronProvider {

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
  public int getOldLevel(CauldronLevelChangeEvent event) {
    return event.getOldLevel();
  }

  @Override
  public int getNewLevel(CauldronLevelChangeEvent event) {
    return event.getNewLevel();
  }
}
