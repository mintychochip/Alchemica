package org.aincraft.providers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.block.CauldronLevelChangeEvent;

// Versions 1.17-1.21
public final class ModernCauldronProvider extends AbstractCauldronProvider {

  @Override
  public int getNewLevel(CauldronLevelChangeEvent event) {
    BlockState newState = event.getNewState();
    BlockData newBlock = newState.getBlockData();
    return (newBlock instanceof Levelled) ? ((Levelled) newBlock).getLevel()
        : ((newBlock.getMaterial() == Material.CAULDRON) ? 0 : 3);
  }

  @Override
  public boolean isWaterCauldron(Block block) {
    return block.getType() == Material.WATER_CAULDRON;
  }

  @Override
  public int getOldLevel(CauldronLevelChangeEvent event) {
    Block block = event.getBlock();
    BlockData blockData = block.getBlockData();
    return blockData instanceof Levelled ? ((Levelled) blockData).getLevel()
        : (block.getType() == Material.CAULDRON ? 0 : 3);
  }
}
