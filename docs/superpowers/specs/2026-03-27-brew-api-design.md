# Alchemica Brew API — Design Spec

## Overview

Add a programmatic recipe registration API and a `BrewCompleteEvent` to Alchemica so external plugins can integrate with the brewing system without touching internal code.

## Goals

- Fire a cancellable `BrewCompleteEvent` when a brew succeeds, carrying enough context for plugins to react (Jobs XP, achievements, logging, result swapping).
- Allow external plugins to register custom recipes programmatically, producing any `ItemStack` result they choose.
- Expose the API via both a static singleton (`AlchemicaAPI.getInstance()`) and Bukkit's `ServicesManager`.

## Out of Scope

- Custom modifier behavior per-recipe (e.g. Glowstone only amplifying specific effects on a custom recipe).
- Potion consumption hooks — plugins use `PlayerItemConsumeEvent` for that.
- Persisting programmatic recipes to YAML.

---

## Module: `api`

### `CustomRecipe`

A data class built via a fluent builder.

Fields:
- `NamespacedKey key` — unique identifier for the recipe
- `List<String> ingredients` — material keys (e.g. `"minecraft:nether_wart"`)
- `Function<BrewContext, ItemStack> resultFn` — called at brew time to produce the result item
- `String permission` — defaults to `"alchemica:<key>"`
- `Set<String> disabledModifiers` — modifier keys blocked for this recipe, defaults empty

Builder usage:
```java
CustomRecipe recipe = new CustomRecipe.Builder(new NamespacedKey(plugin, "jobs_potion"))
    .ingredients("minecraft:nether_wart", "minecraft:gold_ingot")
    .result(ctx -> myJobsPotionItem)
    .permission("myjobs.brew.jobs_potion")
    .disabledModifier("lesser-amplify")
    .build();
```

### `BrewContext`

Immutable value object passed to the result function.

Fields:
- `Player player`
- `List<CauldronIngredient> ingredients` — unmodifiable

### `IBrewAPI`

```java
public interface IBrewAPI {
    void registerRecipe(CustomRecipe recipe);
    void unregisterRecipe(NamespacedKey key);
}
```

### `AlchemicaAPI`

Static accessor. `common` calls `AlchemicaAPI.setInstance(impl)` on enable.

```java
public final class AlchemicaAPI {
    public static IBrewAPI getInstance() { ... }
    public static void setInstance(IBrewAPI api) { ... }
}
```

### `BrewCompleteEvent`

Cancellable Bukkit event fired after a successful brew, before the item is given to the player.

Fields:
- `Player player` (read-only)
- `List<CauldronIngredient> ingredients` (unmodifiable)
- `ItemStack result` — mutable via `setResult(ItemStack)`

If cancelled, the player receives nothing (cauldron level still decrements).

---

## Module: `common`

### `RecipeRegistry`

Add `List<CustomRecipe> customRecipes` field alongside the existing `recipes`, `effects`, `modifiers` lists.

In `search()`:
1. Existing YAML `BaseRecipe` match loop runs first.
2. If no YAML match, fall through to check `customRecipes` using the same `isSubset` logic.
3. Custom recipe result is produced by calling `recipe.resultFn().apply(new BrewContext(player, ingredients))`.
4. Modifiers from remaining ingredients are **not** applied to custom recipe results — the result `ItemStack` comes entirely from the plugin's supplier. The `disabledModifiers` check still runs to allow the recipe to reject invalid combos.
5. Return `PotionResult(SUCCESS, itemStack, null)`.

### `CauldronListener`

After obtaining a `SUCCESS` result (line ~210), before giving the item:

```java
BrewCompleteEvent event = new BrewCompleteEvent(player, ingredients, result.getStack());
Bukkit.getPluginManager().callEvent(event);
if (event.isCancelled()) return;
ItemStack finalItem = event.getResult();
// give finalItem to player
```

### Plugin lifecycle (`Brew.java` / `onEnable` / `onDisable`)

On enable:
```java
IBrewAPI api = new BrewAPIImpl(recipeRegistry);
AlchemicaAPI.setInstance(api);
getServer().getServicesManager().register(IBrewAPI.class, api, this, ServicePriority.Normal);
```

On disable:
```java
getServer().getServicesManager().unregisterAll(this);
AlchemicaAPI.setInstance(null);
```

---

## Data Flow

```
Player stirs cauldron
  → RecipeRegistry.search()
      → check YAML BaseRecipes
      → if no match, check CustomRecipes
      → return IPotionResult(SUCCESS, itemStack)

CauldronListener receives SUCCESS
  → fire BrewCompleteEvent(player, ingredients, itemStack)
      → if cancelled: give nothing
      → if result swapped: give event.getResult()
      → otherwise: give original itemStack
  → decrement cauldron level
```

---

## External Plugin Example

```java
// MyPlugin.onEnable()
AlchemicaAPI.getInstance().registerRecipe(
    new CustomRecipe.Builder(new NamespacedKey(this, "jobs_potion"))
        .ingredients("minecraft:nether_wart", "minecraft:gold_ingot")
        .result(ctx -> buildJobsPotionItem())
        .build()
);

// Listening for brews
@EventHandler
public void onBrew(BrewCompleteEvent e) {
    Jobs.addXp(e.getPlayer(), "brewing", 50);
}
```
