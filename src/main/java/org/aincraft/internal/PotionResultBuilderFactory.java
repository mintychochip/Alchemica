package org.aincraft.internal;

import java.util.ArrayList;
import java.util.HashMap;
import org.aincraft.container.IFactory;
import org.aincraft.internal.PotionResult.PotionResultBuilder;
import org.aincraft.potion.IPotionResult.IPotionResultBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class PotionResultBuilderFactory implements IFactory<IPotionResultBuilder> {

  private final PotionEffectMetaFactory metaFactory;

  public PotionResultBuilderFactory(IPotionDurationMap durationMap) {
    this.metaFactory = new PotionEffectMetaFactory(durationMap);
  }

  @Override
  public IPotionResultBuilder create() throws IllegalArgumentException {
    ItemStack potion = ItemStack.of(Material.POTION);
    return new PotionResultBuilder(metaFactory, potion, null, new ArrayList<>(), new HashMap<>());
  }
}
