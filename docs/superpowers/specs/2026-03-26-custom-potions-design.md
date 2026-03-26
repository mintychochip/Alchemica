# Custom Potions Design

## Overview

Allow server owners to define potions entirely in config — custom name, color, lore, and arbitrary effects — without being tied to any vanilla `PotionType`. Custom potions integrate into the existing recipe and modifier systems transparently.

## Config Format

Custom potions are defined in a new `potions.yml` resource file and referenced by namespaced key from `general.yml` recipes.

### potions.yml

```yaml
fire-god-potion:
  name: "&cFire God Potion"
  color: "FF4500"
  lore:
    - "&7Burns with the fury of the nether"
    - "&7Duration depends on modifiers added"
  effects:
    - type: fire_resistance
      duration: 3600
      amplifier: 0
    - type: strength
      duration: 2400
      amplifier: 1
```

### general.yml (recipe referencing a custom potion)

```yaml
recipes:
  fire-god-recipe:
    ingredients:
      - nether_wart
      - blaze_rod
      - magma_cream
    result: alchemica:fire-god-potion   # namespaced key; "alchemica:" namespace = potions.yml
```

Recipes without a `result` key continue to work as today (vanilla `potion-type` field).

## Architecture

### PotionContext — add CustomMeta

```java
static final class CustomMeta {
    String name;        // supports color codes
    Color color;        // org.bukkit.Color
    List<String> lore;  // supports color codes
}

// In PotionContext:
CustomMeta customMeta = null;
```

The custom potion consumer (built by `RecipeRegistryFactory`) populates:
1. `context.customMeta` with name/color/lore
2. `context.potionMetaMap` with the defined effects (same map the modifier system writes to)

Because effects land in `potionMetaMap`, all existing modifiers — redstone (duration), glowstone (amplifier), gunpowder (splash), dragon breath (lingering) — work on custom potions with zero extra code.

### buildResult() changes

```
if context.customMeta != null:
    apply customMeta.name as display name
    apply customMeta.color via PotionMeta.setColor()
    apply customMeta.lore via ItemMeta.setLore()
else:
    existing vanilla name/display logic (unchanged)
```

`PotionMeta.setColor(Color)` was added in Bukkit 1.11. For 1.8–1.10, color is skipped gracefully (try/catch `NoSuchMethodError`).

### RecipeRegistryFactory — custom potion loading

New method `createCustomPotionConsumer(String key, ConfigurationSection section)`:
1. Parses name, color (`Color.fromRGB(hex)`), lore, effects
2. Returns a `Consumer<PotionContext>` that sets `customMeta` and pre-fills `potionMetaMap`

In `createBaseRecipe()`, if the section contains `result` (a namespaced key string) instead of `potion-type`, delegate to the custom potion path. If the key is in the `alchemica:` namespace, look it up in `potions.yml`.

### New file: potions.yml

Loaded via `IPluginConfiguration.get("potions")` — same mechanism as `general.yml` and `database.yml`. Registered in `Internal.create()`.

## Data Flow

```
potions.yml parsed → CustomPotionDefinition per key
                              ↓
RecipeRegistryFactory reads recipe "result: alchemica:fire-god-potion"
                              ↓
Builds Consumer<PotionContext> that fills customMeta + potionMetaMap
                              ↓
Player adds ingredients → RecipeRegistry.search()
  → base consumer runs (fills customMeta + potionMetaMap)
  → effect/modifier consumers run (modify potionMetaMap entries)
  → buildResult() checks customMeta → applies name/color/lore
  → returns IPotionResult with SUCCESS + ItemStack
```

## Error Handling

- Unknown `result` key → log warning, skip recipe (same as unknown `potion-type` today)
- Invalid hex color → log warning, skip color (potion renders with default purple)
- Unknown effect type in potions.yml → log warning, skip that effect
- Missing `name` field → default to the potion's config key formatted as a title

## Testing

- Unit test in `RecipeRegistryTest`: custom potion recipe returns `SUCCESS` with correct item
- Verify `customMeta` fields (name, color, lore) are applied to the resulting `ItemStack`
- Verify modifier stacking still works after a custom potion base (e.g. splash modifier applies)
- Verify unknown result key is skipped without crashing

## Files Changed

| File | Change |
|---|---|
| `common/src/main/resources/potions.yml` | New — default custom potion definitions |
| `common/src/main/java/.../PotionResult.java` | Add `CustomMeta` inner class to `PotionContext` |
| `common/src/main/java/.../RecipeRegistry.java` | Update `buildResult()` to handle `customMeta` |
| `common/src/main/java/.../RecipeRegistryFactory.java` | Add `createCustomPotionConsumer()`, update `createBaseRecipe()` |
| `common/src/main/java/.../Internal.java` | Load `potions.yml`, pass to factory |
| `common/src/test/.../RecipeRegistryTest.java` | Add custom potion tests |
