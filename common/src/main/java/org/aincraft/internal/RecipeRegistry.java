package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import org.aincraft.BrewContext;
import org.aincraft.CauldronIngredient;
import org.aincraft.CustomRecipe;
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
      String permission, Set<String> disabledModifiers) {}

  record RegistryStep(CauldronIngredient ingredient, Consumer<PotionContext> consumer,
      String permission, String key) {}

  private static final IPotionResult FAILED = new PotionResult(Status.BAD_RECIPE_PATH, null, null);

  private final List<BaseRecipe> recipes;
  private final List<RegistryStep> effects;
  private final List<RegistryStep> modifiers;
  private final BrewAPIImpl brewAPI;

  RecipeRegistry(List<BaseRecipe> recipes, List<RegistryStep> effects,
      List<RegistryStep> modifiers, BrewAPIImpl brewAPI) {
    this.recipes = recipes;
    this.effects = effects;
    this.modifiers = modifiers;
    this.brewAPI = brewAPI;
  }

  @NotNull
  IPotionResult search(IPlayerSettings playerSettings,
      Collection<CauldronIngredient> ingredientCollection) {
    Player player = Bukkit.getPlayer(playerSettings.getPlayerId());
    Preconditions.checkArgument(player != null);
    List<CauldronIngredient> ingredients = new ArrayList<>(ingredientCollection);

    // Find the recipe whose required ingredients are a subset of what the player added,
    // preferring the most specific (longest) match. Order does not matter.
    BaseRecipe matched = null;
    for (BaseRecipe recipe : recipes) {
      if (isSubset(recipe.ingredients(), ingredients) &&
          (matched == null || recipe.ingredients().size() > matched.ingredients().size())) {
        matched = recipe;
      }
    }
    if (matched == null) {
      // Try custom recipes
      for (CustomRecipe custom : brewAPI.getCustomRecipes().values()) {
        if (isSubset(custom.getIngredients(), ingredients)) {
          if (!player.hasPermission(custom.getPermission())) {
            return new PotionResult(Status.NO_PERMISSION, null, null);
          }
          // Check disabled modifiers against remaining ingredients
          List<CauldronIngredient> remaining = removeSubset(ingredients, custom.getIngredients());
          for (CauldronIngredient ingredient : remaining) {
            RegistryStep modifier = findStep(modifiers, ingredient);
            if (modifier != null && custom.getDisabledModifiers().contains(modifier.key())) {
              return FAILED;
            }
            if (modifier == null) {
              return FAILED; // unknown ingredient for this custom recipe
            }
          }
          BrewContext ctx = new BrewContext(player, custom.getKey(),
              new ArrayList<>(ingredientCollection));
          ItemStack stack = custom.getResultFn().apply(ctx);
          if (stack == null) {
            Bukkit.getLogger().warning("[Alchemica] Custom recipe '" + custom.getKey()
                + "' returned null from resultFn — treating as failed brew.");
            return FAILED;
          }
          return new PotionResult(Status.SUCCESS, stack, custom.getKey());
        }
      }
      return FAILED;
    }
    if (!player.hasPermission(matched.permission())) {
      return new PotionResult(Status.NO_PERMISSION, null, null);
    }

    PotionContext context = new PotionContext();
    matched.consumer().accept(context);

    int effectCount = playerSettings.getEffectCount().intValue();
    int modifierCount = playerSettings.getModifierCount().intValue();

    // Remove the matched base ingredients from the list to find remaining add-ons.
    List<CauldronIngredient> remaining = removeSubset(ingredients, matched.ingredients());
    Bukkit.getLogger().info("[Alchemica] brew: ingredients=" + ingredients.stream().map(i -> i.getItemKey().getKey()).collect(java.util.stream.Collectors.joining(",")) + " matched=" + matched.permission() + " remaining=" + remaining.stream().map(i -> i.getItemKey().getKey()).collect(java.util.stream.Collectors.joining(",")));

    // Apply remaining ingredients as effects or modifiers in the order they were added.
    // The same modifier may appear multiple times — each application compounds the effect.
    for (CauldronIngredient ingredient : remaining) {
      RegistryStep effect = findStep(effects, ingredient);
      if (effect != null) {
        if (!player.hasPermission(effect.permission())) {
          return new PotionResult(Status.NO_PERMISSION, null, null);
        }
        for (int i = 0; i < ingredient.getAmount(); i++) {
          effectCount--;
          if (effectCount < 0) {
            return new PotionResult(Status.MANY_EFFECTS, null, null);
          }
          effect.consumer().accept(context);
        }
        continue;
      }

      RegistryStep modifier = findStep(modifiers, ingredient);
      if (modifier != null) {
        if (!player.hasPermission(modifier.permission())) {
          return new PotionResult(Status.NO_PERMISSION, null, null);
        }
        if (matched.disabledModifiers().contains(modifier.key())) {
          return FAILED;
        }
        for (int i = 0; i < ingredient.getAmount(); i++) {
          modifierCount--;
          if (modifierCount < 0) {
            return new PotionResult(Status.MANY_MODS, null, null);
          }
          Bukkit.getLogger().info("[Alchemica] modifier=" + modifier.key() + " ingredient=" + ingredient.getItemKey().getKey());
          modifier.consumer().accept(context);
        }
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

    for (BaseRecipe recipe : recipes) {
      if (isSubset(ingredients, recipe.ingredients())) {
        // Player's items are a subset of this recipe — suggest the missing required ingredients
        List<CauldronIngredient> missing = removeSubset(recipe.ingredients(), ingredients);
        suggestions.addAll(missing);
      }
      if (isSubset(recipe.ingredients(), ingredients)) {
        // Base recipe fully matched — suggest effects and modifiers
        effects.forEach(e -> suggestions.add(e.ingredient()));
        modifiers.forEach(m -> suggestions.add(m.ingredient()));
      }
    }

    // Custom recipe branch 1 only: suggest missing required ingredients
    for (CustomRecipe custom : brewAPI.getCustomRecipes().values()) {
      if (isSubset(ingredients, custom.getIngredients())) {
        List<CauldronIngredient> missing = removeSubset(custom.getIngredients(), ingredients);
        suggestions.addAll(missing);
      }
      // Branch 2 (effects/modifiers) intentionally omitted for custom recipes
    }

    return suggestions;
  }

  /**
   * Returns true if every ingredient in {@code subset} has at least one match in {@code list}
   * (order-independent, consumes matches so duplicates require duplicates).
   */
  private static boolean isSubset(List<CauldronIngredient> subset,
      List<CauldronIngredient> list) {
    List<CauldronIngredient> pool = new ArrayList<>(list);
    for (CauldronIngredient required : subset) {
      Iterator<CauldronIngredient> it = pool.iterator();
      boolean found = false;
      while (it.hasNext()) {
        if (it.next().isSimilar(required)) {
          it.remove();
          found = true;
          break;
        }
      }
      if (!found) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a copy of {@code list} with the first occurrence of each ingredient in
   * {@code toRemove} removed (order-independent).
   */
  private static List<CauldronIngredient> removeSubset(List<CauldronIngredient> list,
      List<CauldronIngredient> toRemove) {
    List<CauldronIngredient> result = new ArrayList<>(list);
    for (CauldronIngredient ingredient : toRemove) {
      Iterator<CauldronIngredient> it = result.iterator();
      while (it.hasNext()) {
        if (it.next().isSimilar(ingredient)) {
          it.remove();
          break;
        }
      }
    }
    return result;
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
      Bukkit.getLogger().info("[Alchemica] effect=" + type.getKey().getKey() + " amplifier=" + meta.getAmplifier() + " duration=" + meta.getDuration().getTicks());
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
