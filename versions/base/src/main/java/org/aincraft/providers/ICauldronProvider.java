package org.aincraft.providers;

import org.bukkit.block.Block;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public interface ICauldronProvider {

  boolean isWaterCauldron(Block block);

  int getCauldronLevel(Block block) throws IllegalArgumentException;

  void setCauldronLevel(Block block, int level) throws IllegalArgumentException;

  int getOldLevel(final CauldronLevelChangeEvent event);

  int getNewLevel(final CauldronLevelChangeEvent event);
}
