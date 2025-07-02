package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.aincraft.CauldronIngredient;
import org.aincraft.IFactory;
import org.aincraft.internal.Node.ConsumerNode;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

final class EffectNodeRegistryFactory implements IFactory<List<ConsumerNode>> {

  private final IYamlConfiguration configuration;
  private final KeyParser parser;
  private final Plugin plugin;


  EffectNodeRegistryFactory(Plugin plugin, IYamlConfiguration configuration,
      KeyParser parser) {
    this.configuration = configuration;
    this.parser = parser;
    this.plugin = plugin;
  }

  private record EffectNodeFactory(KeyParser parser, Plugin plugin) {

    public ConsumerNode create(ConfigurationSection section, PotionEffectType type)
        throws IllegalArgumentException {
      Preconditions.checkArgument(section.contains("item", false));
      Preconditions.checkArgument(section.contains("amount", false));
      String itemKeyString = section.getString("item");
      assert itemKeyString != null;
      Key key = parser.parse(itemKeyString);
      int amount = section.getInt("amount");
      return ConsumerNode.effect(type,new CauldronIngredient(key,amount));
    }
  }

  @Override
  public List<ConsumerNode> create() {
    Preconditions.checkArgument(configuration.contains("potion_ingredients"));
    ConfigurationSection vanillaPotionIngredientSection = configuration.getConfigurationSection(
        "potion_ingredients");
    EffectNodeFactory effectNodeFactory = new EffectNodeFactory(parser, plugin);
    List<ConsumerNode> effectNodes = new ArrayList<>();
    for (PotionEffectType effectType : PotionEffectType.values()) {
      String name = effectType.getName().toLowerCase();
      if (!vanillaPotionIngredientSection.contains(name)) {
        Bukkit.getLogger().info("unable to find effect ingredient section");
        continue;
      }
      ConfigurationSection ingredientSection = vanillaPotionIngredientSection.getConfigurationSection(
          name);
      if (ingredientSection == null) {
        Bukkit.getLogger().info("potion effect type section is null: %s".formatted(name));
        continue;
      }
      try {
        ConsumerNode effectNode = effectNodeFactory.create(ingredientSection, effectType);
        effectNodes.add(effectNode);
      } catch (IllegalArgumentException ex) {
        Bukkit.getLogger().info(ex.getMessage());
      }
    }
    return effectNodes;
  }
}
