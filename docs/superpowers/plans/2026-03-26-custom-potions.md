# Custom Potions Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow server owners to define potions in `potions.yml` with custom name, color, lore, and raw-tick effect durations, referenced by namespaced key from recipes in `general.yml`.

**Architecture:** Add a `CustomMeta` inner class to `PotionContext` (in `PotionResult.java`). A custom potion's recipe consumer sets `customMeta` and pre-fills `potionMetaMap` with its effects — the same map the existing modifier pipeline (redstone, glowstone, splash, lingering) already writes to. `buildResult()` in `RecipeRegistry` checks `customMeta` first and applies name/color/lore when present. Raw tick durations are handled by a new `RawDurationStage` that implements `IDurationStage` with fixed ticks and no chain (duration modifiers become no-ops on custom effects; splash/lingering still work).

**Tech Stack:** Java 17, Bukkit/Spigot API, JUnit 5, MockBukkit 1.21, Gradle

---

## File Map

| Action | File | Purpose |
|--------|------|---------|
| Create | `common/src/main/java/org/aincraft/internal/RawDurationStage.java` | `IDurationStage` impl wrapping a fixed tick count |
| Modify | `common/src/main/java/org/aincraft/internal/PotionResult.java` | Add `CustomMeta` inner class to `PotionContext` |
| Modify | `common/src/main/java/org/aincraft/internal/RecipeRegistry.java` | Update `buildResult()` to apply `customMeta` |
| Modify | `common/src/main/java/org/aincraft/internal/RecipeRegistryFactory.java` | Add `potionsConfiguration` param, `createCustomPotionConsumer()`, update `createBaseRecipe()` |
| Modify | `common/src/main/java/org/aincraft/internal/Internal.java` | Load `potions.yml`, pass to `RecipeRegistryFactory` |
| Create | `common/src/main/resources/potions.yml` | Default custom potion definitions |
| Modify | `common/src/test/java/org/aincraft/internal/RecipeRegistryTest.java` | Add custom potion tests |

---

## Chunk 1: RawDurationStage + CustomMeta + buildResult()

### Task 1: RawDurationStage

**Files:**
- Create: `common/src/main/java/org/aincraft/internal/RawDurationStage.java`
- Modify: `common/src/test/java/org/aincraft/internal/RecipeRegistryTest.java`

- [ ] **Step 1: Write the failing test**

Add to `RecipeRegistryTest.java`:

```java
@Test
void rawDurationStage_returnsTicks() {
    RawDurationStage stage = new RawDurationStage(3600);
    assertEquals(3600, stage.getTicks());
    assertFalse(stage.hasNext());
    assertFalse(stage.hasPrevious());
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :common:test --tests "org.aincraft.internal.RecipeRegistryTest.rawDurationStage_returnsTicks"
```

Expected: FAIL — `RawDurationStage` does not exist yet.

- [ ] **Step 3: Implement RawDurationStage**

Create `common/src/main/java/org/aincraft/internal/RawDurationStage.java`:

```java
package org.aincraft.internal;

import org.aincraft.IDurationStage;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class RawDurationStage implements IDurationStage {

  private static final NamespacedKey KEY = new NamespacedKey("alchemica", "raw");

  private final int ticks;

  RawDurationStage(int ticks) {
    this.ticks = ticks;
  }

  @Override
  public int getTicks() {
    return ticks;
  }

  @Override
  public NamespacedKey getKey() {
    return KEY;
  }

  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public boolean hasPrevious() {
    return false;
  }

  @Override
  public IDurationStage next() {
    throw new IllegalStateException("RawDurationStage has no next stage");
  }

  @Override
  public IDurationStage previous() {
    throw new IllegalStateException("RawDurationStage has no previous stage");
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :common:test --tests "org.aincraft.internal.RecipeRegistryTest.rawDurationStage_returnsTicks"
```

Expected: PASS

---

### Task 2: Add CustomMeta to PotionContext

**Files:**
- Modify: `common/src/main/java/org/aincraft/internal/PotionResult.java`

Read the file first: `common/src/main/java/org/aincraft/internal/PotionResult.java`

- [ ] **Step 1: Add `CustomMeta` and field to `PotionContext`**

In `PotionResult.java`, inside the `PotionContext` static inner class, add:

```java
// At the top of PotionContext, before existing fields:
static final class CustomMeta {
  final String name;
  final org.bukkit.Color color;   // nullable
  final java.util.List<String> lore; // nullable

  CustomMeta(String name, org.bukkit.Color color, java.util.List<String> lore) {
    this.name = name;
    this.color = color;
    this.lore = lore;
  }
}

// New field alongside the existing ones:
CustomMeta customMeta = null;
```

No test needed here — this is a plain data class verified implicitly by Task 3.

---

### Task 3: Update buildResult() to apply CustomMeta

**Files:**
- Modify: `common/src/main/java/org/aincraft/internal/RecipeRegistry.java`
- Modify: `common/src/test/java/org/aincraft/internal/RecipeRegistryTest.java`

Read `RecipeRegistry.java` first (specifically the `buildResult()` method).

- [ ] **Step 1: Write failing tests**

Add to `RecipeRegistryTest.java`:

```java
@Test
void search_customPotion_hasCorrectDisplayName() {
    NamespacedKey ingredientKey = NamespacedKey.minecraft("nether_wart");
    CauldronIngredient ingredient = new CauldronIngredient(ingredientKey);

    PotionResult.PotionContext.CustomMeta meta =
        new PotionResult.PotionContext.CustomMeta(
            "&cFire Potion",
            org.bukkit.Color.fromRGB(0xFF, 0x45, 0x00),
            List.of("&7Burns forever")
        );

    RecipeRegistry.BaseRecipe recipe = new RecipeRegistry.BaseRecipe(
        List.of(ingredient),
        ctx -> {
          ctx.customMeta = meta;
          // no potionMetaMap entries needed — just testing name/color/lore
        },
        "alchemica.test"
    );

    RecipeRegistry registry = new RecipeRegistry(List.of(recipe), List.of(), List.of());
    server.addPlayer("tester");
    PlayerMock player = server.getPlayerExact("tester");
    player.addAttachment(plugin, "alchemica.test", true);

    IPlayerSettings settings = settingsFor(player);
    IPotionResult result = registry.search(settings, List.of(ingredient));

    assertEquals(IPotionResult.Status.SUCCESS, result.getStatus());
    assertNotNull(result.getStack());
    // ChatColor.translateAlternateColorCodes converts & codes
    String expectedName = org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cFire Potion");
    assertEquals(expectedName,
        result.getStack().getItemMeta().getDisplayName());
}

@Test
void search_customPotion_hasLore() {
    NamespacedKey ingredientKey = NamespacedKey.minecraft("nether_wart");
    CauldronIngredient ingredient = new CauldronIngredient(ingredientKey);

    PotionResult.PotionContext.CustomMeta meta =
        new PotionResult.PotionContext.CustomMeta(
            "Test Potion", null, List.of("&7Line one", "&7Line two")
        );

    RecipeRegistry.BaseRecipe recipe = new RecipeRegistry.BaseRecipe(
        List.of(ingredient),
        ctx -> ctx.customMeta = meta,
        "alchemica.test"
    );

    RecipeRegistry registry = new RecipeRegistry(List.of(recipe), List.of(), List.of());
    server.addPlayer("tester2");
    PlayerMock player = server.getPlayerExact("tester2");
    player.addAttachment(plugin, "alchemica.test", true);

    IPotionResult result = registry.search(settingsFor(player), List.of(ingredient));

    assertEquals(IPotionResult.Status.SUCCESS, result.getStatus());
    List<String> lore = result.getStack().getItemMeta().getLore();
    assertNotNull(lore);
    assertEquals(2, lore.size());
    assertTrue(lore.get(0).contains("Line one"));
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :common:test --tests "org.aincraft.internal.RecipeRegistryTest.search_customPotion*"
```

Expected: FAIL — `CustomMeta` not yet referenced in `buildResult()`.

- [ ] **Step 3: Update buildResult() in RecipeRegistry**

In `RecipeRegistry.buildResult()`, replace the current name/display block:

```java
// BEFORE (existing):
if (context.potionkey != null) {
    String base = createPotionName(context.potionkey);
    ...
    potionMeta.setDisplayName(ChatColor.RESET + base);
} else {
    ...
}

// AFTER:
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
        List<String> translated = context.customMeta.lore.stream()
            .map(l -> ChatColor.translateAlternateColorCodes('&', l))
            .collect(Collectors.toList());
        potionMeta.setLore(translated);
    }
} else if (context.potionkey != null) {
    // existing potionkey block — unchanged
} else {
    // existing basePotionData block — unchanged
}
```

Add `import java.util.stream.Collectors;` if not already present.

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :common:test --tests "org.aincraft.internal.RecipeRegistryTest.search_customPotion*"
```

Expected: PASS

- [ ] **Step 5: Run full test suite**

```bash
./gradlew :common:test
```

Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/org/aincraft/internal/RawDurationStage.java \
        common/src/main/java/org/aincraft/internal/PotionResult.java \
        common/src/main/java/org/aincraft/internal/RecipeRegistry.java \
        common/src/test/java/org/aincraft/internal/RecipeRegistryTest.java
git commit -m "feat: add CustomMeta to PotionContext and update buildResult"
```

---

## Chunk 2: RecipeRegistryFactory + potions.yml + wiring

### Task 4: RecipeRegistryFactory handles `result:` key

**Files:**
- Modify: `common/src/main/java/org/aincraft/internal/RecipeRegistryFactory.java`
- Modify: `common/src/test/java/org/aincraft/internal/RecipeRegistryTest.java`

Read `RecipeRegistryFactory.java` fully before starting.

- [ ] **Step 1: Write failing test**

Add to `RecipeRegistryTest.java`:

```java
@Test
void factory_customPotionRecipe_searchReturnsSuccess() {
    // Build a fake potions config section
    org.bukkit.configuration.file.YamlConfiguration potionsYaml =
        new org.bukkit.configuration.file.YamlConfiguration();
    potionsYaml.set("test-potion.name", "&aTest Potion");
    potionsYaml.set("test-potion.color", "00FF00");
    potionsYaml.set("test-potion.lore", List.of("&7A test potion"));
    // effects as a list of maps
    List<Map<String, Object>> effects = new ArrayList<>();
    Map<String, Object> eff = new HashMap<>();
    eff.put("type", "speed");
    eff.put("duration", 3600);
    eff.put("amplifier", 0);
    effects.add(eff);
    potionsYaml.set("test-potion.effects", effects);

    // Build a fake general config with a recipe using result:
    org.bukkit.configuration.file.YamlConfiguration generalYaml =
        new org.bukkit.configuration.file.YamlConfiguration();
    generalYaml.set("recipes.test-recipe.ingredients", List.of("nether_wart"));
    generalYaml.set("recipes.test-recipe.result", "alchemica:test-potion");

    // Use real IYamlConfiguration wrappers (or adapt to your actual wrapper type)
    // NOTE: Check how IYamlConfiguration wraps a ConfigurationSection in existing tests/code.
    // The factory constructor takes IYamlConfiguration for both general and potions configs.
    // You may need to create a thin test helper that wraps YamlConfiguration.

    // ... construct factory with both configs, call create(), then search()
    // Assert result.getStatus() == SUCCESS
    // Assert result.getStack().getItemMeta().getDisplayName() contains "Test Potion"
}
```

> **Note:** Check `IYamlConfiguration` and `YamlConfiguration` in `api/src/main/java/org/aincraft/` to understand how to wrap a `ConfigurationSection` for tests. Adapt the test accordingly.

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :common:test --tests "org.aincraft.internal.RecipeRegistryTest.factory_customPotionRecipe*"
```

Expected: FAIL

- [ ] **Step 3: Add potionsConfiguration parameter to RecipeRegistryFactory**

In `RecipeRegistryFactory.java`:

```java
// Add field:
private final IYamlConfiguration potionsConfiguration;

// Update constructor:
RecipeRegistryFactory(PotionEffectMetaFactory metaFactory,
    IYamlConfiguration configuration,
    IYamlConfiguration potionsConfiguration,
    IPotionProvider potionProvider) {
  this.metaFactory = metaFactory;
  this.configuration = configuration;
  this.potionsConfiguration = potionsConfiguration;
  this.potionProvider = potionProvider;
}
```

- [ ] **Step 4: Update createBaseRecipe() to branch on `result:` vs `potion-type:`**

```java
private BaseRecipe createBaseRecipe(String key, ConfigurationSection section) {
    Preconditions.checkArgument(section.contains("ingredients"), "missing ingredients");

    List<String> ingredientStrings = section.getStringList("ingredients");
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

    return new BaseRecipe(ingredients, consumer, permission);
}
```

- [ ] **Step 5: Implement createCustomPotionConsumer()**

```java
private Consumer<PotionContext> createCustomPotionConsumer(String resultKeyString) {
    NamespacedKey resultKey = Brew.createKey(resultKeyString);
    // For "alchemica:fire-god-potion", the potions.yml section key is "fire-god-potion"
    String sectionKey = resultKey.getKey();
    ConfigurationSection section = potionsConfiguration.getConfigurationSection(sectionKey);
    Preconditions.checkArgument(section != null,
        "custom potion not found in potions.yml: " + resultKeyString);

    String name = section.getString("name", sectionKey);
    org.bukkit.Color color = parseColor(section.getString("color", null));
    List<String> lore = section.getStringList("lore");
    if (lore.isEmpty()) lore = null;

    // Parse effects
    List<Map<?, ?>> effectMaps = section.getMapList("effects");
    List<PotionEffectType> types = new ArrayList<>();
    List<Integer> durations = new ArrayList<>();
    List<Integer> amplifiers = new ArrayList<>();

    for (Map<?, ?> effectMap : effectMaps) {
        String typeString = (String) effectMap.get("type");
        int duration = effectMap.containsKey("duration")
            ? ((Number) effectMap.get("duration")).intValue() : 3600;
        int amplifier = effectMap.containsKey("amplifier")
            ? ((Number) effectMap.get("amplifier")).intValue() : 0;
        try {
            PotionEffectType type = potionProvider.getEffectType(
                NamespacedKey.minecraft(typeString));
            types.add(type);
            durations.add(duration);
            amplifiers.add(amplifier);
        } catch (IllegalArgumentException e) {
            // unknown effect type — skip with warning
            Bukkit.getLogger().warning("[Alchemica] Unknown effect type: " + typeString);
        }
    }

    List<String> finalLore = lore;
    org.bukkit.Color finalColor = color;

    return context -> {
        context.customMeta = new PotionResult.PotionContext.CustomMeta(name, finalColor, finalLore);
        for (int i = 0; i < types.size(); i++) {
            PotionEffectType type = types.get(i);
            int ticks = durations.get(i);
            int amp = amplifiers.get(i);
            PotionEffectMeta meta = new PotionEffectMeta(
                new RawDurationStage(ticks), amp, false, true);
            context.potionMetaMap.put(type, meta);
        }
    };
}

/** Parses a hex color string like "FF4500" into a Bukkit Color, or null if blank/invalid. */
private static org.bukkit.Color parseColor(String hex) {
    if (hex == null || hex.isEmpty()) return null;
    try {
        int rgb = Integer.parseInt(hex.replace("#", ""), 16);
        return org.bukkit.Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    } catch (NumberFormatException e) {
        Bukkit.getLogger().warning("[Alchemica] Invalid color: " + hex);
        return null;
    }
}
```

Also add these imports to `RecipeRegistryFactory.java`:
```java
import java.util.ArrayList;
import java.util.Map;
import org.bukkit.NamespacedKey;
```

- [ ] **Step 6: Run tests**

```bash
./gradlew :common:test
```

Expected: all tests pass.

---

### Task 5: Load potions.yml in Internal and create default file

**Files:**
- Modify: `common/src/main/java/org/aincraft/internal/Internal.java`
- Create: `common/src/main/resources/potions.yml`

Read `Internal.java` and `IPluginConfiguration.java` before starting.

- [ ] **Step 1: Create potions.yml with example content**

Create `common/src/main/resources/potions.yml`:

```yaml
# Custom potions for Alchemica.
# Reference these in general.yml recipes using:
#   result: alchemica:<key>
#
# Supported properties:
#   name    - Display name (supports & color codes)
#   color   - Hex RGB color for the potion bottle, e.g. FF4500  (1.11+ only)
#   lore    - List of lore lines (supports & color codes)
#   effects - List of {type, duration (ticks), amplifier} entries
#
# Effects added AFTER this base (redstone, glowstone, gunpowder, etc.) still apply normally.

example-potion:
  name: "&6Example Potion"
  color: "FFA500"
  lore:
    - "&7An example of a custom potion."
    - "&7Add redstone to extend effects."
  effects:
    - type: speed
      duration: 3600
      amplifier: 1
    - type: regeneration
      duration: 1200
      amplifier: 0
```

- [ ] **Step 2: Wire potions.yml into Internal.create()**

In `Internal.java`, find the block that loads configs and creates the `RecipeRegistryFactory`. Update:

```java
// Existing:
RecipeRegistry trie = new RecipeRegistryFactory(
    new PotionEffectMetaFactory(durationMap), general, potionProvider).create();

// Replace with:
IYamlConfiguration potions = config.get("potions");
RecipeRegistry trie = new RecipeRegistryFactory(
    new PotionEffectMetaFactory(durationMap), general, potions, potionProvider).create();
```

`config.get("potions")` loads `potions.yml` the same way `config.get("general")` loads `general.yml`. Verify this matches the existing pattern in `Internal.create()`.

- [ ] **Step 3: Run full test suite**

```bash
./gradlew :common:test
```

Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/org/aincraft/internal/RecipeRegistryFactory.java \
        common/src/main/java/org/aincraft/internal/Internal.java \
        common/src/main/resources/potions.yml \
        common/src/test/java/org/aincraft/internal/RecipeRegistryTest.java
git commit -m "feat: custom potions via potions.yml"
```

---

### Task 6: Add a custom recipe to general.yml for testing

**Files:**
- Modify: `common/src/main/resources/general.yml`

- [ ] **Step 1: Add an example custom potion recipe**

In `general.yml` under `recipes:`, add:

```yaml
  example-potion-recipe:
    ingredients:
      - nether_wart
      - blaze_rod
    result: alchemica:example-potion
```

- [ ] **Step 2: Manual verification**

Build and run the plugin on a local Paper server. Steps:
1. `./gradlew :common:shadowJar`
2. Drop jar into `plugins/`
3. Start server, join, and brew `nether_wart` + `blaze_rod` in a cauldron
4. Stir with a blaze rod — cauldron should complete
5. Fill with a glass bottle — receive a potion named "Example Potion" with orange color and the correct lore and effects

- [ ] **Step 3: Commit**

```bash
git add common/src/main/resources/general.yml
git commit -m "feat: add example custom potion recipe to default config"
```
