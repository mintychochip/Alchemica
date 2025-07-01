package org.aincraft.container;

import org.aincraft.ingredient.CauldronIngredient;
import org.aincraft.potion.IPotionResult;
import org.aincraft.potion.IPotionResult.IPotionResultBuilder;

public interface IPotionTrie {
  IPotionResult search(IPotionResultBuilder builder, CauldronIngredient ... ingredients);
}
