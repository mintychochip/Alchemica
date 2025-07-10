package org.aincraft.providers;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;

//Versions 1.13-1.16
public class v1_13_CauldronProvider extends AbstractCauldronProvider {

  @Override
  public boolean isWaterCauldron(Block block) {
    Material material = block.getType();
    if (material != Material.CAULDRON) {
      return false;
    }
    BlockData blockData = block.getBlockData();
    Levelled levelled = (Levelled) blockData;
    Chaos chaos = new Chaos(new ArrayList<>());
    chaos.aList.add(2);
    return levelled.getLevel() > 0;
  }

  static final class Chaos {

    List<Integer> aList;

    Chaos(List<Integer> aList) {
      this.aList = aList;
    }
  }
}
