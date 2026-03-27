package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import org.aincraft.CauldronIngredient;
import org.aincraft.IPotionResult;
import org.aincraft.IPotionResult.Status;
import org.aincraft.dao.IPlayerSettings;
import org.aincraft.internal.PotionResult.PotionContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
final class RecipeRegistry {

  record BaseRecipe(List<CauldronIngredient> ingredients, Consumer<PotionContext> consumer,
      String permission) {}

  record RegistryStep(CauldronIngredient ingredient, Consumer<PotionContext> consumer,
      String permission) {}

  private static final IPotionResult FAILED = new PotionResult(Status.BAD_RECIPE_PATH, null, null);

  private final List<BaseRecipe> recipes;
  private final List<RegistryStep> effects;
  private final List<RegistryStep> modifiers;

  RecipeRegistry(List<BaseRecipe> recipes, List<RegistryStep> effects,
      List<RegistryStep> modifiers) {
    this.recipes = recipes;
    this.effects = effects;
    this.modifiers = modifiers;
  }

  @NotNull
  IPotionResult search(IPlayerSettings playerSettings,
      Collection<CauldronIngredient> ingredientCollection) {
    Player player = Bukkit.getPlayer(playerSettings.getPlayerId());
    Preconditions.checkArgument(player != null);
    List<CauldronIngredient> ingredients = new ArrayList<>(ingredientCollection);

    // Find the longest base recipe whose ingredient list is a prefix of the current ingredients
    BaseRecipe matched = null;
    for (BaseRecipe recipe : recipes) {
      if (isPrefix(recipe.ingredients(), ingredients) &&
          (matched == null || recipe.ingredients().size() > matched.ingredients().size())) {
        matched = recipe;
      }
    }
    if (matched == null) {
      return FAILED;
    }
    if (!player.hasPermission(matched.permission())) {
      return new PotionResult(Status.NO_PERMISSION, null, null);
    }

    PotionContext context = new PotionContext();
    matched.consumer().accept(context);

    int effectCount = playerSettings.getEffectCount().intValue();
    int modifierCount = playerSettings.getModifierCount().intValue();

    // Match remaining ingredients as effects or modifiers in order
    for (int pos = matched.ingredients().size(); pos < ingredients.size(); pos++) {
      CauldronIngredient ingredient = ingredients.get(pos);

      RegistryStep effect = findStep(effects, ingredient);
      if (effect != null) {
        if (!player.hasPermission(effect.permission())) {
          return new PotionResult(Status.NO_PERMISSION, null, null);
        }
        effectCount--;
        if (effectCount < 0) {
          return new PotionResult(Status.MANY_EFFECTS, null, null);
        }
        effect.consumer().accept(context);
        continue;
      }

      RegistryStep modifier = findStep(modifiers, ingredient);
      if (modifier != null) {
        if (!player.hasPermission(modifier.permission())) {
          return new PotionResult(Status.NO_PERMISSION, null, null);
        }
        modifierCount--;
        if (modifierCount < 0) {
          return new PotionResult(Status.MANY_MODS, null, null);
        }
        modifier.consumer().accept(context);
        continue;
      }

      return FAILED;
    }

    return buildResult(context);
  }

  @NotNull
  Set<CauldronIngredient> getSuggestions(Collection<CauldronIngredient> ingredientCollection) {
    List<CauldronIngredient> ingredients = new ArrayList<>(ingredientCollection);
    Set<CauldronIngredient> suggestions = new HashSet<>();

    // Suggest next step for any recipe that current ingredients are a prefix of
    for (BaseRecipe recipe : recipes) {
      if (isPrefix(ingredients, recipe.ingredients()) &&
          ingredients.size() < recipe.ingredients().size()) {
        suggestions.add(recipe.ingredients().get(ingredients.size()));
      }
    }

    // If a complete base recipe is matched, suggest effects and modifiers
    boolean baseMatched = recipes.stream().anyMatch(r -> isPrefix(r.ingredients(), ingredients));
    if (baseMatched) {
      effects.forEach(e -> suggestions.add(e.ingredient()));
      modifiers.forEach(m -> suggestions.add(m.ingredient()));
    }

    return suggestions;
  }

  @Nullable
  private static RegistryStep findStep(List<RegistryStep> steps, CauldronIngredient ingredient) {
    for (RegistryStep step : steps) {
      if (ingredient.isSimilar(step.ingredient())) {
        return step;
      }
    }
    return null;
  }

  // Returns true if `prefix` is a prefix of `list` (all elements match by key)
  private static boolean isPrefix(List<CauldronIngredient> prefix, List<CauldronIngredient> list) {
    if (prefix.size() > list.size()) {
      return false;
    }
    for (int i = 0; i < prefix.size(); i++) {
      if (!prefix.get(i).isSimilar(list.get(i))) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  private static PotionResult buildResult(@NotNull PotionContext context) {
    ItemStack itemStack = new ItemStack(context.potionMaterial);
    ItemMeta itemMeta = itemStack.getItemMeta();
    PotionMeta potionMeta = (PotionMeta) itemMeta;

    context.potionTypeConsumer.accept(potionMeta);

    String prefix = context.potionMaterial == Material.SPLASH_POTION ? "Splash"
        : context.potionMaterial == Material.LINGERING_POTION ? "Lingering" : "";

    if (context.customMeta != null) {
      potionMeta.setDisplayName(
          ChatColor.translateAlternateColorCodes('&', context.customMeta.name));
      if (context.customMeta.color != null) {
        try {
          potionMeta.setColor(context.customMeta.color);
        } catch (NoSuchMethodError ignored) {
          // PotionMeta.setColor() added in 1.11; skip on older servers
        }
      }
      if (context.customMeta.lore != null && !context.customMeta.lore.isEmpty()) {
        potionMeta.setLore(context.customMeta.lore.stream()
            .map(l -> ChatColor.translateAlternateColorCodes('&', l))
            .toList());
      }
    } else if (context.potionkey != null) {
      String base = createPotionName(context.potionkey);
      if (!prefix.isEmpty()) {
        base = prefix + " " + base;
      }
      potionMeta.setDisplayName(ChatColor.RESET + base);
    } else {
      PotionData basePotionData = potionMeta.getBasePotionData();
      if (basePotionData != null) {
        PotionType potionType = basePotionData.getType();
        if (potionType != PotionType.WATER) {
          String typeString = potionType.toString().toLowerCase(Locale.ENGLISH);
          String potionName = Character.toUpperCase(typeString.charAt(0))
              + typeString.substring(1)
              + ' '
              + prefix
              + "Potion";
          potionMeta.setDisplayName(ChatColor.RESET + potionName);
        } else {
          potionMeta.setDisplayName(ChatColor.RESET + prefix + " Water Bottle");
        }
      }
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
    return new PotionResult(Status.SUCCESS, itemStack, null);
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
