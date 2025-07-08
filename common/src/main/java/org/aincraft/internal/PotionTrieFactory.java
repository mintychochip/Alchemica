package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.aincraft.CauldronIngredient;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.aincraft.internal.PotionResult.PotionResultContext;
import org.aincraft.providers.IPotionProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

final class PotionTrieFactory {

  private final PotionEffectMetaFactory metaFactory;
  private final IYamlConfiguration configuration;
  private final IPotionProvider potionProvider;
  private final Map<String, Integer> inDegrees = new HashMap<>();

  PotionTrieFactory(PotionEffectMetaFactory metaFactory, IYamlConfiguration configuration,
      IPotionProvider potionProvider) {
    this.metaFactory = metaFactory;
    this.configuration = configuration;
    this.potionProvider = potionProvider;
  }

  Trie create() {
    Preconditions.checkArgument(configuration.contains("nodes"));
    Map<String, Node> nodeMap = new HashMap<>();
    ConfigurationSection nodeConfigurationSection = configuration.getConfigurationSection("nodes");
    for (String nodeKey : nodeConfigurationSection.getKeys(false)) {
      ConfigurationSection nodeSection = nodeConfigurationSection.getConfigurationSection(
          nodeKey);
      if (nodeSection == null) {
        Bukkit.getLogger()
            .info(String.format("node section for: %s was null, skipping the section", nodeKey));
        continue;
      }
      try {
        Node node = createNode(nodeSection);
        nodeMap.put(nodeKey, node);
        inDegrees.put(nodeKey, 0);
      } catch (IllegalArgumentException ex) {
        Bukkit.getLogger().info(ex.getMessage());
      }
    }

    for (String nodeKey : nodeConfigurationSection.getKeys(false)) {
      ConfigurationSection nodeSection = nodeConfigurationSection.getConfigurationSection(nodeKey);
      if (nodeSection == null) {
        Bukkit.getLogger()
            .info(String.format("node section for: %s was null, skipping the section", nodeKey));
        continue;
      }
      if (!nodeSection.contains("children") || !nodeMap.containsKey(nodeKey)) {
        continue;
      }
      Node parentNode = nodeMap.get(nodeKey);
      for (String childKey : nodeSection.getStringList("children")) {
        if (!nodeMap.containsKey(childKey)) {
          continue;
        }
        Node node = nodeMap.get(childKey);
        parentNode.addChild(node);
        inDegrees.compute(childKey, (k, v) -> v == null ? 1 : v + 1);
      }
    }
    Node root = null;
    for (Entry<String, Integer> entry : inDegrees.entrySet()) {
      Node node = nodeMap.get(entry.getKey());
      if (entry.getValue() == 0 && !node.getChildren().isEmpty()) {
        root = node;
      }
    }
    return new Trie(root);
  }

  private Node createNode(ConfigurationSection nodeSection)
      throws IllegalArgumentException {
    Preconditions.checkArgument(nodeSection.contains("type"));
    Preconditions.checkArgument(nodeSection.contains("item"));
    String nodeTypeString = nodeSection.getString("type");
    NodeType nodeType = NodeType.valueOf(nodeTypeString.toUpperCase(Locale.ENGLISH));
    String itemString = nodeSection.getString("item");
    CauldronIngredient ingredient = new CauldronIngredient(Brew.createKey(itemString),
        nodeSection.getInt("amount", 1));
    Consumer<PotionResultContext> consumer = createConsumer(nodeSection, nodeType);
    return new Node(nodeType,
        ingredient,
        consumer);
  }

  @NotNull
  private Consumer<PotionResultContext> createConsumer(ConfigurationSection nodeSection,
      NodeType nodeType) throws IllegalArgumentException {
    Consumer<PotionResultContext> consumer = builder -> {
    };
    switch (nodeType) {
      case BASE:
        String potionTypeString = nodeSection.getString("potion-type");
        NamespacedKey potionTypeKey = NamespacedKey.minecraft(potionTypeString);
        PotionType potionType = potionProvider.getType(potionTypeKey);
        consumer = builder -> {
          switch (potionType) {
            case WATER:
            case AWKWARD:
            case THICK:
            case MUNDANE:
              builder.potionTypeConsumer = meta -> potionProvider.setBasePotionType(meta,
                  potionType);
              break;
            default:
              builder.potionTypeConsumer = meta -> {
                List<PotionEffectType> effectTypes = potionProvider.getEffectTypes(potionType);
                for (PotionEffectType effectType : effectTypes) {
                  builder.potionMetaMap.put(effectType, metaFactory.create(effectType));
                }
                NamespacedKey potionKey = potionProvider.getKey(potionType);
                potionProvider.setDisplayName(meta, createPotionName(potionKey));
              };
              break;
          }
        };
        break;
      case EFFECT:
        String effectTypeString = nodeSection.getString("effect");
        PotionEffectType effectType = potionProvider.getEffectType(
            NamespacedKey.minecraft(effectTypeString));
        consumer = builder -> {
          PotionEffectMeta effectMeta = metaFactory.create(effectType);
          builder.potionMetaMap.put(effectType, effectMeta);
        };
        break;
      case MODIFIER:
        ConfigurationSection modifierSection = nodeSection.getConfigurationSection("modifiers");
        for (String modifierKey : modifierSection.getKeys(false)) {
          switch (modifierKey) {
            case "duration":
            case "amplifier":
              int steps = modifierSection.getInt(modifierKey, 0);
              consumer = consumer.andThen(builder -> {
                builder.metaConsumer = builder.metaConsumer.andThen(
                    ("duration".equals(modifierKey) ? ModifierTypes.DURATION
                        : ModifierTypes.AMPLIFIER).create(steps));
              });
              break;
            case "ambient":
            case "particles":
              boolean state = modifierSection.getBoolean(modifierKey, false);
              consumer = consumer.andThen(builder -> {
                builder.metaConsumer = builder.metaConsumer.andThen(("ambient".equals(modifierKey)
                    ? ModifierTypes.AMBIENT
                    : ModifierTypes.PARTICLES).create(state));
              });
              break;
            case "potion_material":
              String materialString = modifierSection.getString(modifierKey);
              Material material = Material.valueOf(materialString.toUpperCase(Locale.ENGLISH));
              if (!(material == Material.POTION || material == Material.SPLASH_POTION
                  || material == Material.LINGERING_POTION)) {
                throw new IllegalArgumentException(String.format(
                    "material %s: is invalid, ensure the material provided has PotionMeta when turned into an ItemStack.",
                    materialString));
              }

              consumer = consumer.andThen(
                  context -> context.potionMaterialSupplier = () -> material);
          }
        }
    }
    return consumer;
  }

  private static String createPotionName(NamespacedKey potionKey) {
    String name = potionKey.getKey();
    String[] splitName = name.split("_");
    StringBuilder potionNameBuilder = new StringBuilder("Potion of ");
    for (String fragment : splitName) {
      potionNameBuilder.append(Character.toUpperCase(fragment.charAt(0)))
          .append(fragment.substring(1)).append(' ');
    }
    return potionNameBuilder.toString();
  }
}
