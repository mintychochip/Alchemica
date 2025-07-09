package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.aincraft.CauldronIngredient;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.aincraft.internal.PotionResult.PotionResultContext;
import org.aincraft.providers.IPotionProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PotionTrieFactory {

  private final PotionEffectMetaFactory metaFactory;
  private final IYamlConfiguration configuration;
  private final IPotionProvider potionProvider;
  private final Map<String, Integer> inDegrees = new HashMap<>();
  private final Map<String, Node> nodeMap = new HashMap<>();

  PotionTrieFactory(PotionEffectMetaFactory metaFactory, IYamlConfiguration configuration,
      IPotionProvider potionProvider) {
    this.metaFactory = metaFactory;
    this.configuration = configuration;
    this.potionProvider = potionProvider;
  }

  Trie create() {
    Preconditions.checkArgument(configuration.contains("nodes"));
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
        Bukkit.getLogger().info("created node: " + nodeKey);
        nodeMap.put(nodeKey, node);
      } catch (IllegalArgumentException ex) {
        Bukkit.getLogger().info(String.format("ignoring node: %s", nodeKey));
      }
    }

    for (String childKey : nodeConfigurationSection.getKeys(false)) {
      ConfigurationSection nodeSection = nodeConfigurationSection.getConfigurationSection(childKey);
      if (nodeSection == null) {
        Bukkit.getLogger()
            .info(String.format("node section for: %s was null, skipping the section", childKey));
        continue;
      }
      if (!nodeSection.contains("parents") || !nodeMap.containsKey(childKey)) {
        continue;
      }
      Node childNode = nodeMap.get(childKey);
      for (String parentKey : nodeSection.getStringList("parents")) {
        List<Node> parentNodeList = getParentNodeList(parentKey);
        if (parentNodeList != null) {
          for (Node node : parentNodeList) {
            Bukkit.getLogger().info(node.toString());
            node.addChild(childNode);
          }
        }
        if (nodeMap.containsKey(parentKey)) {
          Bukkit.getLogger().info(parentKey + "->" + childKey);
          Node parentNode = nodeMap.get(parentKey);
          parentNode.addChild(childNode);
        }
        inDegrees.compute(childKey, (k, v) -> v == null ? 1 : v + 1);
      }
    }
    try {
      return new Trie(getRoot(nodeMap));
    } catch (IllegalStateException | IllegalArgumentException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Nullable
  private List<Node> getParentNodeList(String nodeKey) throws IllegalArgumentException {
    for (NodeType type : NodeType.values()) {
      String nodeTypeString = type.toString().toLowerCase(Locale.ENGLISH);
      if (nodeKey.startsWith(nodeTypeString + "[") && nodeKey.endsWith("]")) {
        int first = nodeKey.lastIndexOf(nodeTypeString + "[") + 1;
        int last = nodeKey.indexOf("]") - 1;
        if (first > last) {
          throw new IllegalArgumentException("invalid node key");
        }
        String regexPart = nodeKey.substring(nodeTypeString.length() + 1, nodeKey.length() - 1);
        return nodeMap.entrySet().stream()
            .filter(entry ->
                entry.getKey().matches(regexPart) && entry.getValue().getType() == type)
            .map(Entry::getValue)
            .collect(Collectors.toList());
      }
    }
    return null;
  }

  private Node createNode(@NotNull ConfigurationSection nodeSection)
      throws IllegalArgumentException {
    Preconditions.checkArgument(nodeSection.contains("type"));
    Preconditions.checkArgument(nodeSection.contains("item"));
    String nodeTypeString = nodeSection.getString("type");
    NodeType nodeType = NodeType.valueOf(nodeTypeString.toUpperCase(Locale.ENGLISH));
    String itemString = nodeSection.getString("item");
    Consumer<PotionResultContext> consumer = createConsumer(nodeSection, nodeType);
    CauldronIngredient ingredient = new CauldronIngredient(Brew.createKey(itemString),
        nodeSection.getInt("amount", 1));
    @NotNull String permissionString =
        "alchemica." + nodeSection.getName().toLowerCase(Locale.ENGLISH);
    Bukkit.getPluginManager().addPermission(new Permission(permissionString, PermissionDefault.OP));
    return new Node(nodeType,
        ingredient,
        consumer, permissionString);
  }

  @NotNull
  private Node getRoot(Map<String, Node> nodeMap)
      throws IllegalArgumentException, IllegalStateException {
    if (configuration.contains("root")) {
      String rootString = configuration.getString("root");
      if (!nodeMap.containsKey(rootString)) {
        throw new IllegalArgumentException(
            String.format("forced root: %s is not in map", rootString));
      }
      Bukkit.getLogger().info("forced root: " + rootString);
      return nodeMap.get(rootString);
    }
    Bukkit.getLogger().info("root node was not specified");
    for (Entry<String, Integer> entry : inDegrees.entrySet()) {
      Node node = nodeMap.get(entry.getKey());
      if (entry.getValue() == 0 && !node.getChildren().isEmpty()) {
        Bukkit.getLogger().info("implied root: " + entry.getKey());
        return node;
      }
    }
    throw new IllegalStateException(
        "unable to locate a root node, at least one node must have no parents and more than 1 child");
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
              NamespacedKey potionKey = potionProvider.getKey(potionType);
              builder.potionTypeConsumer = meta -> {
                List<PotionEffect> effects = potionProvider.getEffects(potionType);
                for (PotionEffect effect : effects) {
                  builder.potionMetaMap.put(effect.getType(), metaFactory.create(effect));
                }
              };
              builder.potionNameBuilder.withBase(createPotionName(potionKey));
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
                  context -> {
                    context.potionMaterial = material;
                    context.potionNameBuilder.withPrefix(
                        material == Material.SPLASH_POTION ? "Splash" : "Lingering");
                  });
          }
        }
    }
    return consumer;
  }

  private static String createPotionName(@NotNull NamespacedKey potionKey) {
    String name = potionKey.getKey();
    String[] splitName = name.split("_");
    StringBuilder potionNameBuilder = new StringBuilder("Potion of ");
    if ("turtle_master".equals(name)) {
      potionNameBuilder.append("the ");
    }
    for (String fragment : splitName) {
      potionNameBuilder.append(Character.toUpperCase(fragment.charAt(0)))
          .append(fragment.substring(1)).append(' ');
    }
    String potionName = potionNameBuilder.toString();
    return potionName.substring(0, potionName.length() - 1);
  }
}
