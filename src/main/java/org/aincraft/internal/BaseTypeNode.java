package org.aincraft.internal;

import net.kyori.adventure.key.Key;
import org.aincraft.container.PotionBaseType;
import org.aincraft.ingredient.CauldronIngredient;

final class BaseTypeNode extends PotionNode {

  public BaseTypeNode(Key key, CauldronIngredient ingredient, PotionBaseType type) {
    super(key, ingredient, container -> container.setBaseType(type));
  }

}
