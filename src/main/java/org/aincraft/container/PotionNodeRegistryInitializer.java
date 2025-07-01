//package org.aincraft.container;
//
//import com.google.inject.Provider;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//import java.util.function.Predicate;
//import org.aincraft.ingredient.CauldronIngredient;
//import org.bukkit.Material;
//
//public final class PotionNodeRegistryInitializer implements Provider<PotionTrie> {
//
//  private static final boolean vanillaRecipes = true;
//
//  private static final Predicate<PotionNode> BASE_TYPE =
//      node -> node.equals(PotionNode.WATER)
//          || node.equals(PotionNode.THICK)
//          || node.equals(PotionNode.MUNDANE)
//          || node.equals(PotionNode.AWKWARD);
//
//  private static final Map<PotionNode, Material> VANILLA_NODE_EFFECT_MAP;
//
//  static {
//    VANILLA_NODE_EFFECT_MAP = new HashMap<>();
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.SPEED, Material.SUGAR);
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.POISON, Material.SPIDER_EYE);
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.REGENERATION, Material.GHAST_TEAR);
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.FIRE_RESISTANCE, Material.MAGMA_CREAM);
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.NIGHT_VISION, Material.GOLDEN_CARROT);
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.STRENGTH, Material.BLAZE_POWDER);
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.INSTANT_HEALTH, Material.GLISTERING_MELON_SLICE);
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.JUMP_BOOST, Material.RABBIT_FOOT);
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.WATER_BREATHING, Material.PUFFERFISH);
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.SLOW_FALLING, Material.PHANTOM_MEMBRANE);
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.SLOWNESS, Material.TURTLE_HELMET);
//    VANILLA_NODE_EFFECT_MAP.put(PotionNode.WEAKNESS, Material.FERMENTED_SPIDER_EYE);
//  }
//
//  @Override
//  public PotionTrie get() {
//    Set<PotionNode> potionNodes = new HashSet<>();
//    potionNodes.add(PotionNode.AWKWARD);
//    potionNodes.add(PotionNode.THICK);
//    potionNodes.add(PotionNode.MUNDANE);
//    potionNodes.add(PotionNode.WATER);
//    potionNodes.add(PotionNode.SPEED);
//    potionNodes.add(PotionNode.SLOWNESS);
//    potionNodes.add(PotionNode.HASTE);
//    potionNodes.add(PotionNode.MINING_FATIGUE);
//    potionNodes.add(PotionNode.STRENGTH);
//    potionNodes.add(PotionNode.INSTANT_HEALTH);
//    potionNodes.add(PotionNode.INSTANT_DAMAGE);
//    potionNodes.add(PotionNode.JUMP_BOOST);
//    potionNodes.add(PotionNode.NAUSEA);
//    potionNodes.add(PotionNode.REGENERATION);
//    potionNodes.add(PotionNode.RESISTANCE);
//    potionNodes.add(PotionNode.FIRE_RESISTANCE);
//    potionNodes.add(PotionNode.WATER_BREATHING);
//    potionNodes.add(PotionNode.INVISIBILITY);
//    potionNodes.add(PotionNode.BLINDNESS);
//    potionNodes.add(PotionNode.NIGHT_VISION);
//    potionNodes.add(PotionNode.HUNGER);
//    potionNodes.add(PotionNode.WEAKNESS);
//    potionNodes.add(PotionNode.POISON);
//    potionNodes.add(PotionNode.WITHER);
//    potionNodes.add(PotionNode.HEALTH_BOOST);
//    potionNodes.add(PotionNode.ABSORPTION);
//    potionNodes.add(PotionNode.SATURATION);
//    potionNodes.add(PotionNode.GLOWING);
//    potionNodes.add(PotionNode.LEVITATION);
//    potionNodes.add(PotionNode.LUCK);
//    potionNodes.add(PotionNode.UNLUCK);
//    potionNodes.add(PotionNode.SLOW_FALLING);
//    potionNodes.add(PotionNode.CONDUIT_POWER);
//    potionNodes.add(PotionNode.DOLPHINS_GRACE);
//    potionNodes.add(PotionNode.BAD_OMEN);
//    potionNodes.add(PotionNode.HERO_OF_THE_VILLAGE);
//    if (vanillaRecipes) {
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.NETHER_WART),
//          PotionNode.AWKWARD);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.GLOWSTONE), PotionNode.THICK);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.REDSTONE), PotionNode.MUNDANE);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.GLISTERING_MELON_SLICE),
//          PotionNode.MUNDANE);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.SPIDER_EYE),
//          PotionNode.MUNDANE);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.SUGAR), PotionNode.MUNDANE);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.MAGMA_CREAM),
//          PotionNode.MUNDANE);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.GHAST_TEAR),
//          PotionNode.MUNDANE);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.RABBIT_FOOT),
//          PotionNode.MUNDANE);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.GOLDEN_CARROT),
//          PotionNode.MUNDANE);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.PUFFERFISH),
//          PotionNode.MUNDANE);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.TURTLE_HELMET),
//          PotionNode.MUNDANE);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.PHANTOM_MEMBRANE),
//          PotionNode.MUNDANE);
//      PotionNode.WATER.addChild(CauldronIngredient.create(Material.FERMENTED_SPIDER_EYE),
//          PotionNode.WEAKNESS);
//      for (PotionNode node : potionNodes) {
//        if (BASE_TYPE.test(node)) {
//          continue;
//        }
//        if (!VANILLA_NODE_EFFECT_MAP.containsKey(node)) {
//          continue;
//        }
//        Material material = VANILLA_NODE_EFFECT_MAP.get(node);
//        PotionNode.AWKWARD.addChild(CauldronIngredient.create(material), node);
//        for (Map.Entry<PotionNode, Material> entry : VANILLA_NODE_EFFECT_MAP.entrySet()) {
//          PotionNode toNode = entry.getKey();
//          Material ingredient = entry.getValue();
//
//          if (node.equals(toNode)) {
//            continue;
//          }
//          node.addChild(CauldronIngredient.create(ingredient), toNode);
//        }
//      }
//    }
//    return new PotionTrie(PotionNode.WATER);
//  }
//}
