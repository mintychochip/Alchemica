package org.aincraft.internal;

import java.util.List;
import org.aincraft.CauldronIngredient;
import org.aincraft.IFactory;
import org.aincraft.IPotionTrie;
import org.aincraft.internal.Node.ConsumerNode;
import org.aincraft.internal.Node.NodeType;
import org.bukkit.Material;
import org.bukkit.potion.PotionType;

final class PotionTrieFactory implements IFactory<IPotionTrie> {

  private static final ConsumerNode SPLASH;

  private static final ConsumerNode LINGERING;

  static {
    SPLASH = createMaterialPotion(Material.SPLASH_POTION,
        CauldronIngredient.fromMaterial(Material.GUNPOWDER));
    LINGERING = createMaterialPotion(Material.LINGERING_POTION,
        CauldronIngredient.fromMaterial(Material.DRAGON_BREATH));
    SPLASH.add(LINGERING);
  }

  private static ConsumerNode createMaterialPotion(Material potionMaterial,
      CauldronIngredient ingredient) {
    return new ConsumerNode(NodeType.MODIFIER,
        ingredient,
        container -> container.setType(potionMaterial));
  }

  private final List<ConsumerNode> effectNodes;
  private final List<ConsumerNode> modifierNodes;

  PotionTrieFactory(List<ConsumerNode> effectNodes, List<ConsumerNode> modifierNodes) {
    this.effectNodes = effectNodes;
    this.modifierNodes = modifierNodes;
  }


  @Override
  public IPotionTrie create() {
    Node root = new Node();
    root.add(SPLASH);
    ConsumerNode awkward = ConsumerNode.base(
        PotionType.AWKWARD,
        CauldronIngredient.fromMaterial(Material.NETHER_WART)
    );
    root.add(awkward.add(SPLASH));
    root.add(ConsumerNode.base(PotionType.MUNDANE,
        CauldronIngredient.fromMaterial(Material.GHAST_TEAR)).add(SPLASH));
    root.add(ConsumerNode.base(PotionType.MUNDANE,
        CauldronIngredient.fromMaterial(Material.REDSTONE)).add(SPLASH));
    root.add(ConsumerNode.base(PotionType.MUNDANE,
        CauldronIngredient.fromMaterial(Material.MAGMA_CREAM)).add(SPLASH));
    root.add(ConsumerNode.base(PotionType.MUNDANE,
        CauldronIngredient.fromMaterial(Material.RABBIT_FOOT)).add(SPLASH));
    root.add(ConsumerNode.base(PotionType.MUNDANE,
        CauldronIngredient.fromMaterial(Material.BLAZE_POWDER)).add(SPLASH));
    root.add(ConsumerNode.base(PotionType.MUNDANE,
        CauldronIngredient.fromMaterial(Material.GLISTERING_MELON_SLICE)).add(SPLASH));
    root.add(ConsumerNode.base(PotionType.MUNDANE,
        CauldronIngredient.fromMaterial(Material.SPIDER_EYE)).add(SPLASH));
    root.add(ConsumerNode.base(PotionType.THICK,
        CauldronIngredient.fromMaterial(Material.GLOWSTONE_DUST)).add(SPLASH));
    for (ConsumerNode effect : effectNodes) {
      awkward.add(effect);
      for (ConsumerNode to : effectNodes) {
        if (!effect.equals(to)) {
          effect.add(to);
        }
      }
      for (ConsumerNode modifier : modifierNodes) {
        effect.add(modifier);
        for (ConsumerNode to : modifierNodes) {
          modifier.add(to);
          modifier.add(SPLASH);
        }
        effect.add(SPLASH);
      }
    }
    return new Trie(root);
  }
}
