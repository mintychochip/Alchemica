package org.aincraft.providers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public interface ICauldronProvider {

  Material[] getHeatSources();

  boolean isWaterCauldron(Block block);

  int getCauldronLevel(Block block) throws IllegalArgumentException;

  void setCauldronLevel(Block block, int level) throws IllegalArgumentException;

  void playAddIngredientEffect(Block block, Player player);

  void playStirEffect(Block block, Player player);

  int getOldLevel(final CauldronLevelChangeEvent event);

  int getNewLevel(final CauldronLevelChangeEvent event);
}
