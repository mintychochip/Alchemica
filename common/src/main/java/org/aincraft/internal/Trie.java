package org.aincraft.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.aincraft.CauldronIngredient;
import org.aincraft.IPotionResult;
import org.aincraft.internal.PotionResult.PotionResultContext;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
final class Trie {

  private final Node root;

  Trie(Node root) {
    this.root = root;
  }

  public IPotionResult search(Collection<CauldronIngredient> ingredients) {
    Node node = root;
    PotionResultContext context = new PotionResultContext();
    List<CauldronIngredient> working = ingredients.stream().map(CauldronIngredient::deepCopy)
        .collect(Collectors.toList());
    int index = 0;
    while (index < working.size()) {
      CauldronIngredient current = working.get(index);
      Node child = node.search(current);
      if (child == null) {
        return null;
      }
      CauldronIngredient required = child.getIngredient();
      current.updateAmount(amount -> amount - required.getAmount());
      child.getConsumer().accept(context);
      node = child;
      if (current.getAmount() == 0) {
        index++;
      }
    }
    return fromContext(context);
  }

  private static PotionResult fromContext(@NotNull PotionResultContext context) {
    ItemStack itemStack = new ItemStack(context.potionMaterialSupplier.get());
    ItemMeta itemMeta = itemStack.getItemMeta();

    PotionMeta potionMeta = (PotionMeta) itemMeta;
    if (context.potionTypeConsumer != null) {
      context.potionTypeConsumer.accept(potionMeta);
    }

    for (Entry<PotionEffectType, PotionEffectMeta> entry : context.potionMetaMap.entrySet()) {
      PotionEffectMeta meta = entry.getValue();
      PotionEffectType type = entry.getKey();
      context.metaConsumer.accept(meta);
      potionMeta.addCustomEffect(new PotionEffect(type,
          meta.getDuration().getTicks(),
          meta.getAmplifier(),
          meta.isAmbient(),
          meta.isParticles()), true);
    }
    itemStack.setItemMeta(potionMeta);
    return new PotionResult(itemStack, null);
  }
}
