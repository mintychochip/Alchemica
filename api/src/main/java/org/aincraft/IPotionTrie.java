package org.aincraft;

import java.util.Collection;
import org.aincraft.IPotionResult.IPotionResultBuilder;

public interface IPotionTrie {
  IPotionResult search(IPotionResultBuilder builder, Collection<CauldronIngredient> ingredients);
}
