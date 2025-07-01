package org.aincraft.internal;

import net.kyori.adventure.key.Key;
import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.aincraft.container.IFactory;
import org.aincraft.container.IPotionTrie;
import org.aincraft.container.IRegistry;
import org.aincraft.container.PotionBaseType;
import org.aincraft.ingredient.CauldronIngredient;
import org.aincraft.internal.PotionNode.EffectNode;
import org.aincraft.internal.PotionNode.ModifierNode;
import org.bukkit.Material;

final class PotionTrieFactory implements IFactory<IPotionTrie> {

  private final IRegistry<EffectNode> effectNodes;
  private final IRegistry<ModifierNode> modifierNodes;
  private final IYamlConfiguration configuration;

  PotionTrieFactory(IRegistry<EffectNode> effectNodes, IRegistry<ModifierNode> modifierNodes,
      IYamlConfiguration configuration) {
    this.effectNodes = effectNodes;
    this.modifierNodes = modifierNodes;
    this.configuration = configuration;
  }

  @Override
  public IPotionTrie create() {
    BaseTypeNode root = new BaseTypeNode(Key.key("brew:water"),
        CauldronIngredient.create(Material.WATER_BUCKET), PotionBaseType.WATER);
    BaseTypeNode awkward = new BaseTypeNode(Key.key("brew:awkward"),
        CauldronIngredient.create(Material.NETHER_WART), PotionBaseType.AWKWARD);
    root.addChild(awkward);
    for (EffectNode effect : effectNodes) {
      awkward.addChild(effect);
      for (EffectNode to : effectNodes) {
        if (!effect.equals(to)) {
          effect.addChild(to);
        }
      }
      for (ModifierNode modifier : modifierNodes) {
        effect.addChild(modifier);
        for (ModifierNode to : modifierNodes) {
          modifier.addChild(to);
        }
      }
    }
    return new PotionTrie(root);
  }
}
