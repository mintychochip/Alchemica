package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.aincraft.CauldronIngredient;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.aincraft.internal.PotionResult.PotionContext;
import org.aincraft.internal.RecipeRegistry.BaseRecipe;
import org.aincraft.internal.RecipeRegistry.RegistryStep;
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
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class RecipeRegistryFactory {

  private final PotionEffectMetaFactory metaFactory;
  private final IYamlConfiguration configuration;
  private final IPotionProvider potionProvider;

  RecipeRegistryFactory(PotionEffectMetaFactory metaFactory, IYamlConfiguration configuration,
      IPotionProvider potionProvider) {
    this.metaFactory = metaFactory;
    this.configuration = configuration;
    this.potionProvider = potionProvider;
  }

  RecipeRegistry create() {
    List<BaseRecipe> recipes = new ArrayList<>();
    List<RegistryStep> effects = new ArrayList<>();
    List<RegistryStep> modifiers = new ArrayList<>();

    if (configuration.contains("recipes")) {
      ConfigurationSection recipesSection = configuration.getConfigurationSection("recipes");
      for (String key : recipesSection.getKeys(false)) {
        ConfigurationSection section = recipesSection.getConfigurationSection(key);
        if (section == null) {
          continue;
        }
        try {
          recipes.add(createBaseRecipe(key, section));
        } catch (IllegalArgumentException ignored) {
        }
      }
    }

    if (configuration.contains("effects")) {
      ConfigurationSection effectsSection = configuration.getConfigurationSection("effects");
      for (String key : effectsSection.getKeys(false)) {
        ConfigurationSection section = effectsSection.getConfigurationSection(key);
        if (section == null) {
          continue;
        }
        try {
          effects.add(createEffectStep(key, section));
        } catch (IllegalArgumentException ignored) {
        }
      }
    }

    if (configuration.contains("modifiers")) {
      ConfigurationSection modifiersSection = configuration.getConfigurationSection("modifiers");
      for (String key : modifiersSection.getKeys(false)) {
        ConfigurationSection section = modifiersSection.getConfigurationSection(key);
        if (section == null) {
          continue;
        }
        try {
          modifiers.add(createModifierStep(key, section));
        } catch (IllegalArgumentException ignored) {
        }
      }
    }

    return new RecipeRegistry(recipes, effects, modifiers);
  }

  private BaseRecipe createBaseRecipe(String key, ConfigurationSection section) {
    Preconditions.checkArgument(section.contains("ingredients"), "missing ingredients");
    Preconditions.checkArgument(section.contains("potion-type"), "missing potion-type");

    List<String> ingredientStrings = section.getStringList("ingredients");
    List<CauldronIngredient> ingredients = new ArrayList<>();
    for (String itemString : ingredientStrings) {
      ingredients.add(new CauldronIngredient(Brew.createKey(itemString)));
    }

    String potionTypeString = section.getString("potion-type");
    PotionType potionType = potionProvider.getType(NamespacedKey.minecraft(potionTypeString));
    Consumer<PotionContext> consumer = buildBaseConsumer(potionType);

    String permission = section.getString("permission",
        "alchemica." + key.toLowerCase(Locale.ENGLISH));
    Bukkit.getPluginManager().addPermission(new Permission(permission, PermissionDefault.TRUE));

    return new BaseRecipe(ingredients, consumer, permission);
  }

  private Consumer<PotionContext> buildBaseConsumer(PotionType potionType) {
    switch (potionType) {
      case WATER:
      case AWKWARD:
      case THICK:
      case MUNDANE:
        return builder -> builder.potionTypeConsumer = meta -> potionProvider.setBasePotionType(
            meta, potionType);
      default:
        NamespacedKey potionKey = potionProvider.getKey(potionType);
        return builder -> {
          List<PotionEffect> potionEffects = potionProvider.getEffects(potionType);
          for (PotionEffect effect : potionEffects) {
            builder.potionMetaMap.put(effect.getType(), metaFactory.create(effect));
          }
          builder.potionkey = potionKey;
        };
    }
  }

  private RegistryStep createEffectStep(String key, ConfigurationSection section) {
    Preconditions.checkArgument(section.contains("item"), "missing item");
    Preconditions.checkArgument(section.contains("effect"), "missing effect");

    CauldronIngredient ingredient = new CauldronIngredient(
        Brew.createKey(section.getString("item")));
    PotionEffectType effectType = potionProvider.getEffectType(
        NamespacedKey.minecraft(section.getString("effect")));
    Consumer<PotionContext> consumer = builder -> {
      PotionEffectMeta effectMeta = metaFactory.create(effectType);
      builder.potionMetaMap.put(effectType, effectMeta);
    };

    String permission = section.getString("permission",
        "alchemica." + key.toLowerCase(Locale.ENGLISH));
    Bukkit.getPluginManager().addPermission(new Permission(permission, PermissionDefault.TRUE));

    return new RegistryStep(ingredient, consumer, permission);
  }

  private RegistryStep createModifierStep(String key, ConfigurationSection section) {
    Preconditions.checkArgument(section.contains("item"), "missing item");

    CauldronIngredient ingredient = new CauldronIngredient(
        Brew.createKey(section.getString("item")));
    Consumer<PotionContext> consumer = builder -> {
    };

    if (section.contains("duration")) {
      int steps = section.getInt("duration");
      consumer = consumer.andThen(
          builder -> builder.metaConsumer = builder.metaConsumer.andThen(
              ModifierFactories.DURATION.create(steps)));
    }
    if (section.contains("amplifier")) {
      int steps = section.getInt("amplifier");
      consumer = consumer.andThen(
          builder -> builder.metaConsumer = builder.metaConsumer.andThen(
              ModifierFactories.AMPLIFIER.create(steps)));
    }
    if (section.contains("ambient")) {
      boolean state = section.getBoolean("ambient");
      consumer = consumer.andThen(
          builder -> builder.metaConsumer = builder.metaConsumer.andThen(
              ModifierFactories.AMBIENT.create(state)));
    }
    if (section.contains("particles")) {
      boolean state = section.getBoolean("particles");
      consumer = consumer.andThen(
          builder -> builder.metaConsumer = builder.metaConsumer.andThen(
              ModifierFactories.PARTICLES.create(state)));
    }
    if (section.contains("potion_material")) {
      String materialString = section.getString("potion_material");
      Material material = Material.valueOf(materialString.toUpperCase(Locale.ENGLISH));
      if (!(material == Material.POTION || material == Material.SPLASH_POTION
          || material == Material.LINGERING_POTION)) {
        throw new IllegalArgumentException("invalid potion material: " + materialString);
      }
      Consumer<PotionContext> finalConsumer = consumer;
      consumer = finalConsumer.andThen(context -> context.potionMaterial = material);
    }

    String permission = section.getString("permission",
        "alchemica." + key.toLowerCase(Locale.ENGLISH));
    Bukkit.getPluginManager().addPermission(new Permission(permission, PermissionDefault.TRUE));

    return new RegistryStep(ingredient, consumer, permission);
  }
}
