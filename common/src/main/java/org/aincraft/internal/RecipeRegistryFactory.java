package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
  private final IYamlConfiguration potionsConfiguration;
  private final IPotionProvider potionProvider;

  RecipeRegistryFactory(PotionEffectMetaFactory metaFactory, IYamlConfiguration configuration,
      IYamlConfiguration potionsConfiguration, IPotionProvider potionProvider) {
    this.metaFactory = metaFactory;
    this.configuration = configuration;
    this.potionsConfiguration = potionsConfiguration;
    this.potionProvider = potionProvider;
  }

  RecipeRegistry create(BrewAPIImpl brewAPI) {
    List<BaseRecipe> recipes = new ArrayList<>();
    List<RegistryStep> effects = new ArrayList<>();
    List<RegistryStep> modifiers = new ArrayList<>();

    ConfigurationSection recipesSection = configuration.getConfigurationSection("recipes");
    if (recipesSection != null) {
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

    ConfigurationSection effectsSection = configuration.getConfigurationSection("effects");
    if (effectsSection != null) {
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

    ConfigurationSection modifiersSection = configuration.getConfigurationSection("modifiers");
    if (modifiersSection != null) {
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

    return new RecipeRegistry(recipes, effects, modifiers, brewAPI);
  }

  private BaseRecipe createBaseRecipe(String key, ConfigurationSection section) {
    Preconditions.checkArgument(section.contains("ingredients"), "missing ingredients");

    List<String> ingredientStrings = section.getStringList("ingredients");
    Preconditions.checkArgument(!ingredientStrings.isEmpty(), "ingredients list must not be empty");
    List<CauldronIngredient> ingredients = new ArrayList<>();
    for (String itemString : ingredientStrings) {
      ingredients.add(new CauldronIngredient(Brew.createKey(itemString)));
    }

    Consumer<PotionContext> consumer;
    if (section.contains("result")) {
      consumer = createCustomPotionConsumer(section.getString("result"));
    } else {
      Preconditions.checkArgument(section.contains("potion-type"), "missing potion-type");
      String potionTypeString = section.getString("potion-type");
      PotionType potionType = potionProvider.getType(NamespacedKey.minecraft(potionTypeString));
      consumer = buildBaseConsumer(potionType);
    }

    String permission = section.getString("permission",
        "alchemica." + key.toLowerCase(Locale.ENGLISH));
    Bukkit.getPluginManager().addPermission(new Permission(permission, PermissionDefault.TRUE));

    // Read disabled-modifiers list from yml
    List<String> disabledList = section.getStringList("disabled-modifiers");
    Set<String> disabledModifiers = disabledList.isEmpty()
        ? Collections.emptySet()
        : new HashSet<>(disabledList);

    return new BaseRecipe(ingredients, consumer, permission, disabledModifiers);
  }

  private Consumer<PotionContext> createCustomPotionConsumer(String resultKeyString) {
    NamespacedKey resultKey = Brew.createKey(resultKeyString);
    String sectionKey = resultKey.getKey();
    ConfigurationSection section = potionsConfiguration.getConfigurationSection(sectionKey);
    Preconditions.checkArgument(section != null,
        "custom potion not found in potions.yml: " + resultKeyString);

    String name = section.getString("name", sectionKey);
    org.bukkit.Color color = parseColor(section.getString("color", null));
    List<String> lore = section.getStringList("lore");
    List<String> finalLore = lore.isEmpty() ? null : lore;

    List<PotionEffectType> types = new ArrayList<>();
    List<Integer> durations = new ArrayList<>();
    List<Integer> amplifiers = new ArrayList<>();

    for (Map<?, ?> effectMap : section.getMapList("effects")) {
      String typeString = (String) effectMap.get("type");
      if (typeString == null) {
        Bukkit.getLogger().warning("[Alchemica] Effect entry in potions.yml is missing 'type' field, skipping");
        continue;
      }
      int duration = effectMap.containsKey("duration")
          ? ((Number) effectMap.get("duration")).intValue() : 3600;
      int amplifier = effectMap.containsKey("amplifier")
          ? ((Number) effectMap.get("amplifier")).intValue() : 0;
      try {
        PotionEffectType type = potionProvider.getEffectType(NamespacedKey.minecraft(typeString));
        types.add(type);
        durations.add(duration);
        amplifiers.add(amplifier);
      } catch (IllegalArgumentException e) {
        Bukkit.getLogger().warning("[Alchemica] Unknown effect type in potions.yml: " + typeString);
      }
    }

    return context -> {
      context.customMeta = new PotionResult.PotionContext.CustomMeta(name, color, finalLore);
      for (int i = 0; i < types.size(); i++) {
        PotionEffectType type = types.get(i);
        PotionEffectMeta meta = new PotionEffectMeta(
            new RawDurationStage(durations.get(i)), amplifiers.get(i), false, true);
        context.potionMetaMap.put(type, meta);
      }
    };
  }

  private static org.bukkit.Color parseColor(String hex) {
    if (hex == null || hex.isEmpty()) {
      return null;
    }
    try {
      int rgb = Integer.parseInt(hex.replace("#", ""), 16);
      return org.bukkit.Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    } catch (NumberFormatException e) {
      Bukkit.getLogger().warning("[Alchemica] Invalid color in potions.yml: " + hex);
      return null;
    }
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

    return new RegistryStep(ingredient, consumer, permission, key);
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

    return new RegistryStep(ingredient, consumer, permission, key);
  }
}
