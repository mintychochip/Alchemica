package org.aincraft.providers;

import com.google.common.base.Preconditions;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.CauldronLevelChangeEvent;

//Versions 1.12
public final class LegacyCauldronProvider extends AbstractCauldronProvider {

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
  public int getOldLevel(CauldronLevelChangeEvent event) {
    return event.getOldLevel();
  }

  @Override
  public int getNewLevel(CauldronLevelChangeEvent event) {
    return event.getNewLevel();
  }
}
