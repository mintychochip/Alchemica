package org.aincraft.providers;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public interface ICauldronEffectProvider {
  void playStirEffect(Block block, Player player);
  void playAddIngredientEffect(Block block, Player player);
}
