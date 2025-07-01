package org.aincraft.internal;

import com.google.common.base.Preconditions;
import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.aincraft.container.IFactory;
import org.aincraft.container.IRegistry;
import org.aincraft.container.SimpleRegistry;
import org.aincraft.internal.PotionNode.EffectNode;
import org.aincraft.ingredient.CauldronIngredient;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

final class EffectNodeRegistryFactory implements IFactory<IRegistry<EffectNode>> {

  private final IYamlConfiguration configuration;
  private final ItemParser parser;
  private final Plugin plugin;

  EffectNodeRegistryFactory(Plugin plugin, IYamlConfiguration configuration,
      ItemParser parser) {
    this.configuration = configuration;
    this.parser = parser;
    this.plugin = plugin;
  }

  private record EffectNodeFactory(ItemParser parser, Plugin plugin) {

    public EffectNode create(ConfigurationSection section, PotionEffectType type)
        throws IllegalArgumentException {
      Preconditions.checkArgument(section.contains("item", false));
      Preconditions.checkArgument(section.contains("amount", false));
      String itemKeyString = section.getString("item");
      assert itemKeyString != null;
      ItemStack itemStack = parser.parse(itemKeyString);
      int amount = section.getInt("amount");
      NamespacedKey typeKey = type.getKey();
      return new EffectNode(new NamespacedKey(plugin, typeKey.value()),
          new CauldronIngredient(itemStack, amount),
          type);
    }
  }

  @Override
  public IRegistry<EffectNode> create() {
    Preconditions.checkArgument(configuration.contains("potion_ingredients"));
    ConfigurationSection vanillaPotionIngredientSection = configuration.getConfigurationSection(
        "potion_ingredients");
    EffectNodeFactory effectNodeFactory = new EffectNodeFactory(parser, plugin);
    SimpleRegistry<EffectNode> effectNodes = new SimpleRegistry<>();
    for (PotionEffectType effectType : Registry.POTION_EFFECT_TYPE) {
      NamespacedKey key = effectType.getKey();
      if (!vanillaPotionIngredientSection.contains(key.value())) {
        Bukkit.getLogger().info("unable to find effect ingredient section");
        continue;
      }
      ConfigurationSection ingredientSection = vanillaPotionIngredientSection.getConfigurationSection(
          key.value());
      if (ingredientSection == null) {
        Bukkit.getLogger().info("potion effect type section is null: %s".formatted(key.value()));
        continue;
      }
      try {
        EffectNode effectNode = effectNodeFactory.create(ingredientSection, effectType);
        effectNodes.register(effectNode.getKey(), effectNode);
      } catch (IllegalArgumentException ex) {
        Bukkit.getLogger().info(ex.getMessage());
      }
    }
    return effectNodes;
  }
}
