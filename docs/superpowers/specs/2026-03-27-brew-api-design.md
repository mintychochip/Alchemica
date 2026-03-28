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
- `List<CauldronIngredient> ingredients` — converted from strings at build time (see below)
- `Function<BrewContext, ItemStack> resultFn` — called at brew time to produce the result item; **must not return null** (a null return is treated as a failed brew and logged as a warning)
- `String permission` — defaults to `"alchemica:<key>"`; registered with Bukkit as `PermissionDefault.TRUE` at registration time
- `Set<String> disabledModifiers` — modifier keys blocked for this recipe; defaults empty

The builder accepts `ingredients(String... keys)` and converts each string to a `CauldronIngredient` at build time (not at match time), so the conversion cost is paid once. Each ingredient string is parsed via `NamespacedKey.fromString(key)` — if the result is null (malformed key), the builder throws `IllegalArgumentException` immediately. Each valid key maps to `new CauldronIngredient(namespacedKey)` with `amount=1`.

Custom recipes do not support requiring more than one of the same ingredient key. `CauldronIngredient.isSimilar()` compares only `itemKey` (not `amount`), so the `isSubset` match is key-only at runtime. This means a player can satisfy a custom recipe ingredient requirement with any number of that item in the cauldron — the recipe will match as long as at least one is present. Duplicate-count requirements are not enforceable.

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
- `NamespacedKey recipeKey` — the key of the matched `CustomRecipe`
- `List<CauldronIngredient> ingredients` — unmodifiable. This is the raw list passed into `search()` (i.e. the full cauldron ingredient list), before any base-recipe removal. No `removeSubset` is called for custom recipes.

Note: The `api` module already depends on Bukkit (`CauldronIngredient` uses `NamespacedKey`), so `Player` is a safe addition here.

### `IBrewAPI`

```java
public interface IBrewAPI {
    /**
     * Registers a custom recipe. If a recipe with the same key is already registered,
     * it is replaced and a warning is logged.
     */
    void registerRecipe(CustomRecipe recipe);

    /**
     * Unregisters a recipe by key. Takes effect on the next search() call;
     * cauldrons that already have ingredients cached are unaffected.
     */
    void unregisterRecipe(NamespacedKey key);
}
```

### `AlchemicaAPI`

Static accessor. `common` calls `AlchemicaAPI.setInstance(impl)` on enable.

```java
public final class AlchemicaAPI {
    /**
     * Returns the IBrewAPI instance.
     * @throws IllegalStateException if called before Alchemica has enabled.
     */
    public static IBrewAPI getInstance() { ... }

    /**
     * Internal use only — called by common on enable/disable.
     * Throws IllegalStateException if an instance is already set and the new value is non-null
     * (prevents external plugins from replacing the instance).
     * Passing null clears the instance (called on disable).
     */
    public static void setInstance(@Nullable IBrewAPI api) { ... }
}
```

`setInstance` uses a guard: if called with a non-null value while an instance is already set, it throws `IllegalStateException`. This prevents external plugins from hijacking the singleton. Passing `null` (called on disable) clears the instance and resets the guard, so a subsequent enable can call `setInstance(impl)` again successfully. The codebase does not use JPMS, so package-private would not provide meaningful protection.

### `BrewCompleteEvent`

Cancellable Bukkit event fired after a successful brew, before the item is given to the player. Fires for **both YAML-defined and custom recipes**.

Fields:
- `Player player` (read-only)
- `List<CauldronIngredient> ingredients` (unmodifiable)
- `@Nullable NamespacedKey recipeKey` — the key of the matched custom recipe, or `null` for YAML-defined recipes (which have no programmatic key)
- `ItemStack result` — mutable via `setResult(ItemStack)`. **Initialised to the `ItemStack` passed at construction** (the brew result). Never null at construction time.

Behaviour:
- If cancelled (`isCancelled() == true`), the player receives nothing regardless of `getResult()`.
- If not cancelled and `getResult()` returns null (i.e. a handler called `setResult(null)`), the player receives nothing. This is silent by design — it is a deliberate plugin choice, not an error, so no warning is logged (unlike `resultFn` returning null during `search()`, which is an implementation bug and is warned).
- If not cancelled and `getResult()` returns a non-null item, that item is given to the player.
- Cancellation takes precedence over result swapping: a cancelled event with a non-null result still gives nothing.
- Cauldron level **always** decrements regardless of cancellation or result (the vanilla `CauldronLevelChangeEvent` is cancelled by Alchemica and the level is decremented manually after the event, unconditionally).

---

## Module: `common`

### `BrewAPIImpl`

Implements `IBrewAPI`. Owns a stable `ConcurrentHashMap<NamespacedKey, CustomRecipe> customRecipes` that is **not** rebuilt on `/creload`. This means custom recipes registered by external plugins survive Alchemica reloads without any action from the registering plugin.

On `registerRecipe`:
1. If a recipe with the same key exists, log a warning and replace it.
2. Register the permission node with Bukkit (`PermissionDefault.TRUE`) if not already registered.
3. Store in `customRecipes`.

On reload (`Brew.refresh()`): `RecipeRegistry` is rebuilt with a fresh YAML recipe list, but it receives the same `BrewAPIImpl` reference. The `customRecipes` map in `BrewAPIImpl` is untouched.

### `RecipeRegistry`

`RecipeRegistry` receives a reference to `BrewAPIImpl` at construction (instead of owning its own custom recipe list). This reference is stable across reloads.

In `search()`:
1. Existing YAML `BaseRecipe` match loop runs first (unchanged).
2. If no YAML match, iterate `brewAPI.getCustomRecipes()` using the same `isSubset` logic (the ingredient lists are already `List<CauldronIngredient>`, so no conversion is needed at match time).
3. Check `player.hasPermission(recipe.getPermission())` — return `Status.NO_PERMISSION` if denied.
4. Check `disabledModifiers` for any remaining modifier ingredients — return `Status.BAD_RECIPE_PATH`. This matches the YAML path (the `FAILED` constant in the existing code is simply `new PotionResult(Status.BAD_RECIPE_PATH, null, null)`). Note: since modifiers are never suggested to players for custom recipes, a player would only hit this path by manually adding a modifier ingredient. The result is the same as no recipe matching — the brew fails silently.
5. If `resultFn.apply(brewContext)` returns `null`, log a warning and return `Status.BAD_RECIPE_PATH`.
6. Otherwise return `new PotionResult(Status.SUCCESS, itemStack, null)`.

Note: Modifiers (Glowstone, Redstone) do **not** apply to custom recipe results. The result `ItemStack` comes entirely from the plugin's supplier. The remaining ingredient loop still runs the `disabledModifiers` check to reject invalid combos, but does not chain `metaConsumer` onto the result.

### `getSuggestions()`

Custom recipes participate in the suggestion system. The existing `getSuggestions()` loop is extended to also iterate `brewAPI.getCustomRecipes()`. Only **branch 1** of the suggestion logic applies to custom recipes: if the player's current items are a subset of a custom recipe's required ingredients, suggest the missing required ingredients. **Branch 2** (suggest effects and modifiers once the base recipe is fully matched) does **not** apply to custom recipes — custom recipes do not use the YAML effect/modifier system, so no effects or modifiers are ever suggested for them.

### `CauldronLevelListener` (1.9+ bottle fill path)

`CauldronLevelListener` handles `CauldronLevelChangeEvent` at `MONITOR` priority and immediately calls `event.setCancelled(true)` — the vanilla level decrement never actually occurs. Alchemica then manually decrements the cauldron level at the end of the handler, unconditionally (even if the event is cancelled or the result is empty). This is existing behaviour and must be preserved.

`BrewCompleteEvent` fires after the result is obtained and before the item is given to the player. `cauldron.getIngredients()` at this point is the full raw ingredient list as stored in the cauldron DAO — `removeSubset` is only called inside `search()` and does not mutate the stored list.

```java
// after result obtained, before inventory.addItem:
BrewCompleteEvent event = new BrewCompleteEvent(player, cauldron.getIngredients(), result.getStack());
Bukkit.getPluginManager().callEvent(event);
if (event.isCancelled() || event.getResult() == null) {
    // skip giving item — cauldron level still decrements below
} else {
    ItemStack finalItem = event.getResult();
    // give finalItem to player
}
// cauldron level decrement always happens here (existing lines 106-111)
```

### `CauldronListener.handleBottleFill18()` (1.8 bottle fill path)

The same `BrewCompleteEvent` is fired identically. `cauldron.getIngredients()` is the same full raw list. Both code paths must fire the event — the 1.8 path is not exempt.

### Plugin lifecycle (`Brew.java`)

On enable:
```java
BrewAPIImpl api = new BrewAPIImpl();  // stable; not rebuilt on reload
AlchemicaAPI.setInstance(api);
getServer().getServicesManager().register(IBrewAPI.class, api, this, ServicePriority.Normal);
// pass api reference into RecipeRegistry construction
```

On reload (`refresh()`):
```java
// BrewAPIImpl is NOT recreated — same instance reused
// Internal.create is updated to accept the existing BrewAPIImpl:
//   static Internal create(Brew brew, BrewAPIImpl brewAPI)
// RecipeRegistry is rebuilt receiving a reference to the existing BrewAPIImpl
internal = Internal.create(this, existingBrewAPIImpl);
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
      → check YAML BaseRecipes (unchanged)
      → if no match, check CustomRecipes via BrewAPIImpl
      → return IPotionResult(SUCCESS, itemStack)

CauldronLevelListener (1.9+) or CauldronListener.handleBottleFill18 (1.8) receives SUCCESS
  → fire BrewCompleteEvent(player, ingredients, itemStack)
      → if cancelled or result == null: give nothing
      → if result swapped: give event.getResult()
      → otherwise: give original itemStack
  → decrement cauldron level
```

The event fires in the **listener layer**, not inside `RecipeRegistry.search()`. It fires for both YAML-defined and custom recipe successes.

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

External plugins do **not** need to re-register recipes on Alchemica reload — the `BrewAPIImpl` survives reloads and retains all registered recipes.
