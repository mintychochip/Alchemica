package org.aincraft.container;

import org.bukkit.potion.PotionType;

public enum PotionBaseType {
  WATER(PotionType.WATER),
  AWKWARD(PotionType.AWKWARD),
  MUNDANE(PotionType.MUNDANE),
  THICK(PotionType.THICK);

  private final PotionType potionType;

  PotionBaseType(PotionType potionType) {
    this.potionType = potionType;
  }

  public PotionType getPotionType() {
    return potionType;
  }
}