# Admin Recipe Manager Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an in-game chest GUI system for server admins to create, browse, edit, and delete Alchemica potion recipes without touching yml files.

**Architecture:** A wizard-style GUI flow backed by a `WizardSession` per player, serialised to/from `general.yml` and `potions.yml` by `RecipeYmlWriter`. A separate browser GUI reads the live registry and launches the wizard in edit mode. All GUI classes implement `InventoryHolder` and are dispatched by a single `GuiListener`.

**Tech Stack:** Java 17, Paper API 1.21 (Bukkit InventoryHolder pattern, AnvilView, AsyncPlayerChatEvent), JUnit Jupiter 5, MockBukkit 4.7

---

## Chunk 1: Foundation — Registry, Permissions, Session Data

### Task 1: Fix `PluginConfigurationFactory` — register `potions.yml`

**Files:**
- Modify: `common/src/main/java/org/aincraft/config/PluginConfigurationFactory.java`

The `Internal.create()` calls `config.get("potions")` but `potions` is never added to the config map, meaning custom potion loading currently returns null and will NPE. Add it.

- [ ] **Step 1: Add `potions` entry**

In `PluginConfigurationFactory#create()`, after the `legacy` line add:
```java
configurations.put("potions", factory.yaml("potions.yml"));
```

- [ ] **Step 2: Run tests to confirm nothing breaks**

```
./gradlew :common:test
```
Expected: all existing tests pass.

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/org/aincraft/config/PluginConfigurationFactory.java
git commit -m "fix: register potions.yml in PluginConfigurationFactory"
```

---

### Task 2: Add `disabled-modifiers` support to `RecipeRegistry`

**Files:**
- Modify: `common/src/main/java/org/aincraft/internal/RecipeRegistry.java`
- Modify: `common/src/main/java/org/aincraft/internal/RecipeRegistryFactory.java`
- Modify: `common/src/test/java/org/aincraft/internal/RecipeRegistryTest.java`

`BaseRecipe` gains a `Set<String> disabledModifiers` field. `search()` skips any modifier whose key is in that set.

- [ ] **Step 1: Write failing test for disabled modifier enforcement**

Add to `RecipeRegistryTest.java`:
```java
@Test
void search_disabledModifier_isSkipped() {
    CauldronIngredient a   = ingredient(KEY_A);
    CauldronIngredient mod = ingredient(KEY_MOD);

    // Recipe disables MOD_KEY
    RecipeRegistry.BaseRecipe recipe = new RecipeRegistry.BaseRecipe(
        List.of(a),
        ctx -> ctx.potionkey = new NamespacedKey("test", "water"),
        PERM_BASE,
        Set.of("mod-one")   // disabled modifier key
    );
    RecipeRegistry.RegistryStep modStep = new RecipeRegistry.RegistryStep(
        mod, ctx -> {}, PERM_MOD, "mod-one"
    );

    RecipeRegistry registry = new RecipeRegistry(
        List.of(recipe), Collections.emptyList(), List.of(modStep)
    );

    // Modifier is globally registered but disabled for this recipe → BAD_RECIPE_PATH
    IPotionResult result = registry.search(settingsFor(player), List.of(a, mod));
    assertEquals(Status.BAD_RECIPE_PATH, result.getStatus());
}
```

- [ ] **Step 2: Run test to confirm it fails**

```
./gradlew :common:test --tests "*.RecipeRegistryTest.search_disabledModifier_isSkipped"
```
Expected: FAIL (compilation error — new constructor args don't exist yet).

- [ ] **Step 3: Update `BaseRecipe` and `RegistryStep` records AND fix existing tests (do atomically)**

⚠️ Changing the record constructors immediately breaks the two existing custom-potion tests in `RecipeRegistryTest.java` that use the 3-arg `BaseRecipe` constructor. Fix the helper methods and direct constructor calls in the same step — before running any tests.

In `RecipeRegistry.java`, update the records:
```java
record BaseRecipe(List<CauldronIngredient> ingredients, Consumer<PotionContext> consumer,
    String permission, Set<String> disabledModifiers) {}

record RegistryStep(CauldronIngredient ingredient, Consumer<PotionContext> consumer,
    String permission, String key) {}
```

In `RecipeRegistryTest.java`, update the helper methods:
```java
private static RecipeRegistry.BaseRecipe baseRecipe(List<CauldronIngredient> ingredients,
        String permission) {
    NamespacedKey dummyKey = new NamespacedKey("test", "water");
    return new RecipeRegistry.BaseRecipe(ingredients, ctx -> ctx.potionkey = dummyKey,
        permission, Collections.emptySet());
}

private static RecipeRegistry.RegistryStep step(CauldronIngredient ingredient,
        String permission) {
    return new RecipeRegistry.RegistryStep(ingredient, ctx -> {}, permission, permission);
}
```

Also update the two custom-potion tests that construct `BaseRecipe` directly — add `Collections.emptySet()` as the fourth argument.

- [ ] **Step 4: Update `search()` to enforce `disabledModifiers`**

In `search()`, before `modifier.consumer().accept(context)`, add:
```java
if (matched.disabledModifiers().contains(modifier.key())) {
    return FAILED;
}
```

Replace the `findStep` call for modifiers to also pass the key check. The full updated modifier block inside the `for` loop:
```java
RegistryStep modifier = findStep(modifiers, ingredient);
if (modifier != null) {
    if (!player.hasPermission(modifier.permission())) {
        return new PotionResult(Status.NO_PERMISSION, null, null);
    }
    if (matched.disabledModifiers().contains(modifier.key())) {
        return FAILED;
    }
    modifierCount--;
    if (modifierCount < 0) {
        return new PotionResult(Status.MANY_MODS, null, null);
    }
    modifier.consumer().accept(context);
    continue;
}
```

- [ ] **Step 5: Fix `RecipeRegistryFactory` — populate new record fields**

In `createBaseRecipe()`:
```java
// Read disabled-modifiers list from yml
List<String> disabledList = section.getStringList("disabled-modifiers");
Set<String> disabledModifiers = disabledList.isEmpty()
    ? Collections.emptySet()
    : new HashSet<>(disabledList);

return new BaseRecipe(ingredients, consumer, permission, disabledModifiers);
```

In `createModifierStep()`, pass the yml key as the `key` field:
```java
return new RegistryStep(ingredient, consumer, permission, key);
```

In `createEffectStep()`, pass the key too:
```java
return new RegistryStep(ingredient, consumer, permission, key);
```

- [ ] **Step 6: Run all tests**

```
./gradlew :common:test
```
Expected: all tests pass including new `search_disabledModifier_isSkipped`.

- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/org/aincraft/internal/RecipeRegistry.java \
        common/src/main/java/org/aincraft/internal/RecipeRegistryFactory.java \
        common/src/test/java/org/aincraft/internal/RecipeRegistryTest.java
git commit -m "feat: enforce disabled-modifiers per recipe in RecipeRegistry"
```

---

### Task 3: Add permissions and `/brew` command to `plugin.yml`

**Files:**
- Modify: `common/src/main/resources/plugin.yml`

- [ ] **Step 1: Add permissions block and brew command**

Edit `plugin.yml` to:
```yaml
name: Alchemica
version: '1.1'
main: org.aincraft.BrewBootstrap
authors: [ mintychochip ]
api-version: '1.13'
commands:
  creload:
    description: hot reloads configs
  brew:
    description: opens the Alchemica admin recipe manager
    usage: /brew
permissions:
  alchemica.admin:
    description: Full admin access to Alchemica recipe management
    default: op
    children:
      alchemica.admin.reload: true
  alchemica.admin.reload:
    description: Allows use of /creload
    default: op
```

- [ ] **Step 2: Run tests**

```
./gradlew :common:test
```
Expected: all pass.

- [ ] **Step 3: Commit**

```bash
git add common/src/main/resources/plugin.yml
git commit -m "feat: add permissions block and /brew command to plugin.yml"
```

---

### Task 4: `RecipeResultType` enum and `WizardSession` data class

**Files:**
- Create: `common/src/main/java/org/aincraft/wizard/RecipeResultType.java`
- Create: `common/src/main/java/org/aincraft/wizard/WizardSession.java`
- Create: `common/src/main/java/org/aincraft/wizard/WizardSessionManager.java`

No tests needed for pure data holders; `WizardSessionManager` is tested implicitly by integration.

- [ ] **Step 1: Create `RecipeResultType`**

```java
package org.aincraft.wizard;

public enum RecipeResultType {
    VANILLA,
    CUSTOM
}
```

- [ ] **Step 2: Create `WizardSession`**

```java
package org.aincraft.wizard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/**
 * Holds in-progress wizard state for one admin player.
 * All fields are mutable — the wizard updates them as the player navigates.
 */
public final class WizardSession {

    // Edit-mode metadata
    public boolean editMode = false;
    public String originalKey = null;

    // Step 1
    public String key = null;

    // Step 2 — ordered list of ingredient materials (unique by Material)
    public final List<Material> ingredients = new ArrayList<>();

    // Step 3
    public RecipeResultType resultType = null;
    public PotionType potionType = null;      // set when resultType == VANILLA

    // Step 4 — custom potion properties (null = unset / omit from yml)
    public String name = null;
    public String color = null;              // 6-char uppercase hex, no '#'
    public List<String> lore = null;

    // Step 4 — effects (custom path only)
    public final List<EffectEntry> effects = new ArrayList<>();

    // Step 5 — modifier toggles: key → enabled
    public final Map<String, Boolean> modifierToggles = new LinkedHashMap<>();

    public static final class EffectEntry {
        public final PotionEffectType type;
        public final int amplifier;
        public final int durationTicks;

        public EffectEntry(PotionEffectType type, int amplifier, int durationTicks) {
            this.type = type;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
        }
    }
}
```

- [ ] **Step 3: Create `WizardSessionManager`**

```java
package org.aincraft.wizard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-safe registry of active {@link WizardSession} objects keyed by player UUID.
 */
public final class WizardSessionManager {

    private final Map<UUID, WizardSession> sessions = new HashMap<>();

    public void put(UUID playerId, WizardSession session) {
        sessions.put(playerId, session);
    }

    @Nullable
    public WizardSession get(UUID playerId) {
        return sessions.get(playerId);
    }

    public boolean has(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public void remove(UUID playerId) {
        sessions.remove(playerId);
    }
}
```

- [ ] **Step 4: Run tests**

```
./gradlew :common:test
```
Expected: all pass (no new tests; compile check).

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/org/aincraft/wizard/
git commit -m "feat: add WizardSession, RecipeResultType, WizardSessionManager"
```

---

## Chunk 2: Persistence Layer

### Task 5: `RecipeYmlWriter`

**Files:**
- Create: `common/src/main/java/org/aincraft/io/RecipeYmlWriter.java`
- Create: `common/src/test/java/org/aincraft/io/RecipeYmlWriterTest.java`

`RecipeYmlWriter` reads both yml files into `YamlConfiguration` objects (Bukkit), mutates them in memory, then writes each to a temp file and renames atomically.

**Important:** Use `Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)` with fallback to `REPLACE_EXISTING` only on `AtomicMoveNotSupportedException`.

- [ ] **Step 1: Write failing tests**

Create `common/src/test/java/org/aincraft/io/RecipeYmlWriterTest.java`:

```java
package org.aincraft.io;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.aincraft.wizard.RecipeResultType;
import org.aincraft.wizard.WizardSession;
import org.aincraft.wizard.WizardSession.EffectEntry;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

class RecipeYmlWriterTest {

    @TempDir File tmpDir;
    private File generalFile;
    private File potionsFile;
    private RecipeYmlWriter writer;
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        generalFile = new File(tmpDir, "general.yml");
        potionsFile = new File(tmpDir, "potions.yml");
        writer = new RecipeYmlWriter(generalFile, potionsFile);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() { MockBukkit.unmock(); }

    private WizardSession vanillaSession(String key) {
        WizardSession s = new WizardSession();
        s.key = key;
        s.resultType = RecipeResultType.VANILLA;
        s.potionType = PotionType.SWIFTNESS;
        s.ingredients.add(Material.NETHER_WART);
        s.ingredients.add(Material.BLAZE_POWDER);
        return s;
    }

    private WizardSession customSession(String key) {
        WizardSession s = new WizardSession();
        s.key = key;
        s.resultType = RecipeResultType.CUSTOM;
        s.ingredients.add(Material.NETHER_WART);
        s.name = "&6My Brew";
        s.color = "FF4500";
        s.lore = List.of("&7First line.");
        s.effects.add(new EffectEntry(PotionEffectType.SPEED, 1, 3600));
        return s;
    }

    @Test
    void write_vanilla_createsRecipeEntry() throws IOException {
        writer.write(vanillaSession("my-brew"));

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(generalFile);
        assertTrue(cfg.contains("recipes.my-brew"));
        assertEquals("swiftness", cfg.getString("recipes.my-brew.potion-type"));
        List<String> ings = cfg.getStringList("recipes.my-brew.ingredients");
        assertEquals(2, ings.size());
        assertTrue(ings.contains("nether_wart"));
        assertTrue(ings.contains("blaze_powder"));
        assertFalse(cfg.contains("recipes.my-brew.disabled-modifiers"));
    }

    @Test
    void write_vanilla_withDisabledModifier_writesDisabledModifiers() throws IOException {
        WizardSession s = vanillaSession("my-brew");
        s.modifierToggles.put("splash-potion", false);
        s.modifierToggles.put("lingering-potion", true);
        writer.write(s);

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(generalFile);
        List<String> disabled = cfg.getStringList("recipes.my-brew.disabled-modifiers");
        assertEquals(List.of("splash-potion"), disabled);
    }

    @Test
    void write_custom_createsEntriesInBothFiles() throws IOException {
        writer.write(customSession("my-brew"));

        YamlConfiguration gen = YamlConfiguration.loadConfiguration(generalFile);
        assertTrue(gen.contains("recipes.my-brew"));
        assertEquals("alchemica:my-brew", gen.getString("recipes.my-brew.result"));

        YamlConfiguration pot = YamlConfiguration.loadConfiguration(potionsFile);
        assertTrue(pot.contains("my-brew"));
        assertEquals("&6My Brew", pot.getString("my-brew.name"));
        assertEquals("FF4500", pot.getString("my-brew.color"));
    }

    @Test
    void write_custom_noOptionalFields_omitsNullFields() throws IOException {
        WizardSession s = new WizardSession();
        s.key = "minimal";
        s.resultType = RecipeResultType.CUSTOM;
        s.ingredients.add(Material.NETHER_WART);
        // name, color, lore all null
        writer.write(s);

        YamlConfiguration pot = YamlConfiguration.loadConfiguration(potionsFile);
        assertFalse(pot.contains("minimal.name"));
        assertFalse(pot.contains("minimal.color"));
        assertFalse(pot.contains("minimal.lore"));
        // effects key must always be written (even empty list), so RecipeRegistryFactory can parse it
        assertTrue(pot.contains("minimal.effects"));
    }

    @Test
    void write_preservesOtherExistingRecipes() throws IOException {
        // pre-populate general.yml with an existing recipe
        YamlConfiguration existing = new YamlConfiguration();
        existing.set("recipes.other-brew.ingredients", List.of("sugar"));
        existing.set("recipes.other-brew.potion-type", "swiftness");
        existing.save(generalFile);

        writer.write(vanillaSession("my-brew"));

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(generalFile);
        assertTrue(cfg.contains("recipes.other-brew"), "other-brew must be preserved");
        assertTrue(cfg.contains("recipes.my-brew"), "new recipe must exist");
    }

    @Test
    void write_replacesExistingEntryOnEdit() throws IOException {
        WizardSession s = vanillaSession("my-brew");
        writer.write(s);

        // edit: change potion type
        WizardSession edit = vanillaSession("my-brew");
        edit.potionType = PotionType.SLOWNESS;
        edit.editMode = true;
        edit.originalKey = "my-brew";
        writer.write(edit);

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(generalFile);
        assertEquals("slowness", cfg.getString("recipes.my-brew.potion-type"));
    }

    @Test
    void write_keyRename_removesOldKeyWritesNewKey() throws IOException {
        writer.write(vanillaSession("old-brew"));

        WizardSession rename = vanillaSession("new-brew");
        rename.editMode = true;
        rename.originalKey = "old-brew";
        writer.write(rename);

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(generalFile);
        assertFalse(cfg.contains("recipes.old-brew"), "old key must be removed");
        assertTrue(cfg.contains("recipes.new-brew"), "new key must exist");
    }

    @Test
    void write_customKeyRename_newKeyExistsOldRemovedBothFiles() throws IOException {
        writer.write(customSession("old-brew"));

        WizardSession rename = customSession("new-brew");
        rename.editMode = true;
        rename.originalKey = "old-brew";
        writer.write(rename);

        YamlConfiguration gen = YamlConfiguration.loadConfiguration(generalFile);
        assertFalse(gen.contains("recipes.old-brew"), "old key must be removed from general.yml");
        assertTrue(gen.contains("recipes.new-brew"), "new key must be in general.yml");

        YamlConfiguration pot = YamlConfiguration.loadConfiguration(potionsFile);
        assertFalse(pot.contains("old-brew"), "old key must be removed from potions.yml");
        assertTrue(pot.contains("new-brew"), "new key must be in potions.yml");
    }

    @Test
    void delete_vanilla_removesFromGeneralOnly() throws IOException {
        writer.write(vanillaSession("my-brew"));
        writer.delete("my-brew", false);

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(generalFile);
        assertFalse(cfg.contains("recipes.my-brew"));
    }

    @Test
    void delete_custom_removesFromBothFiles() throws IOException {
        writer.write(customSession("my-brew"));
        writer.delete("my-brew", true);

        YamlConfiguration gen = YamlConfiguration.loadConfiguration(generalFile);
        assertFalse(gen.contains("recipes.my-brew"));
        YamlConfiguration pot = YamlConfiguration.loadConfiguration(potionsFile);
        assertFalse(pot.contains("my-brew"));
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail (compile error)**

```
./gradlew :common:test --tests "*.RecipeYmlWriterTest"
```
Expected: FAIL — `RecipeYmlWriter` class does not exist.

- [ ] **Step 3: Implement `RecipeYmlWriter`**

Create `common/src/main/java/org/aincraft/io/RecipeYmlWriter.java`:

```java
package org.aincraft.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.aincraft.wizard.RecipeResultType;
import org.aincraft.wizard.WizardSession;
import org.bukkit.configuration.file.YamlConfiguration;

public final class RecipeYmlWriter {

    private final File generalFile;
    private final File potionsFile;

    public RecipeYmlWriter(File generalFile, File potionsFile) {
        this.generalFile = generalFile;
        this.potionsFile = potionsFile;
    }

    /** Exposed so callers can read the current yml for key lookups. */
    public File getGeneralFile() { return generalFile; }

    /**
     * Writes (or overwrites) the recipe described by {@code session}.
     * Handles same-key saves and key renames.
     *
     * @throws IOException if a file write fails
     */
    public void write(WizardSession session) throws IOException {
        boolean keyChanged = session.editMode
            && session.originalKey != null
            && !session.originalKey.equals(session.key);

        if (session.resultType == RecipeResultType.CUSTOM) {
            writeCustom(session, keyChanged);
        } else {
            writeVanilla(session, keyChanged);
        }
    }

    private void writeVanilla(WizardSession session, boolean keyChanged) throws IOException {
        YamlConfiguration gen = loadOrEmpty(generalFile);
        if (keyChanged) {
            gen.set("recipes." + session.originalKey, null);
        }
        setVanillaRecipe(gen, session);
        saveAtomic(gen, generalFile);
    }

    private void writeCustom(WizardSession session, boolean keyChanged) throws IOException {
        if (keyChanged) {
            writeCustomRename(session);
        } else {
            // New recipe or same-key edit: write potions.yml then general.yml
            YamlConfiguration pot = loadOrEmpty(potionsFile);
            setCustomDefinition(pot, session);
            saveAtomic(pot, potionsFile);

            YamlConfiguration gen = loadOrEmpty(generalFile);
            setCustomRecipe(gen, session);
            saveAtomic(gen, generalFile);
        }
    }

    /**
     * 3-write rename sequence from spec:
     * 1. Write new key to potions.yml (old key still present — safe orphan if step 2 fails)
     * 2. Write new key to general.yml + remove old key (commits the rename)
     * 3. Remove old key from potions.yml (cleanup; old entry is harmless if this fails)
     */
    private void writeCustomRename(WizardSession session) throws IOException {
        // Step 1: add new definition to potions.yml (old key still present)
        YamlConfiguration pot = loadOrEmpty(potionsFile);
        setCustomDefinition(pot, session);
        saveAtomic(pot, potionsFile);

        // Step 2: write new key + remove old key in general.yml (commits rename)
        YamlConfiguration gen = loadOrEmpty(generalFile);
        gen.set("recipes." + session.originalKey, null);
        setCustomRecipe(gen, session);
        saveAtomic(gen, generalFile);

        // Step 3: remove old key from potions.yml (old entry now a safe orphan before this)
        YamlConfiguration pot2 = loadOrEmpty(potionsFile);
        pot2.set(session.originalKey, null);
        saveAtomic(pot2, potionsFile);
    }

    private static void setVanillaRecipe(YamlConfiguration gen, WizardSession s) {
        String path = "recipes." + s.key;
        gen.set(path + ".ingredients", ingredientList(s));
        gen.set(path + ".potion-type",
            s.potionType.getKey().getKey());
        setDisabledModifiers(gen, path, s);
    }

    private static void setCustomRecipe(YamlConfiguration gen, WizardSession s) {
        String path = "recipes." + s.key;
        gen.set(path + ".ingredients", ingredientList(s));
        gen.set(path + ".result", "alchemica:" + s.key);
        setDisabledModifiers(gen, path, s);
    }

    private static void setCustomDefinition(YamlConfiguration pot, WizardSession s) {
        String p = s.key;
        if (s.name != null) pot.set(p + ".name", s.name);
        if (s.color != null) pot.set(p + ".color", s.color);
        if (s.lore != null && !s.lore.isEmpty()) pot.set(p + ".lore", s.lore);
        List<Map<String, Object>> effectMaps = new ArrayList<>();
        for (WizardSession.EffectEntry e : s.effects) {
            effectMaps.add(Map.of(
                "type", e.type.getKey().getKey(),
                "duration", e.durationTicks,
                "amplifier", e.amplifier
            ));
        }
        pot.set(p + ".effects", effectMaps);
    }

    private static void setDisabledModifiers(YamlConfiguration gen, String path,
            WizardSession s) {
        List<String> disabled = s.modifierToggles.entrySet().stream()
            .filter(e -> !e.getValue())
            .map(Map.Entry::getKey)
            .toList();
        if (!disabled.isEmpty()) {
            gen.set(path + ".disabled-modifiers", disabled);
        }
    }

    private static List<String> ingredientList(WizardSession s) {
        return s.ingredients.stream()
            .map(m -> m.getKey().getKey())
            .toList();
    }

    /**
     * Deletes a recipe. For vanilla, removes only from general.yml.
     * For custom, removes from general.yml first (to avoid dangling references),
     * then from potions.yml.
     */
    public void delete(String key, boolean isCustom) throws IOException {
        YamlConfiguration gen = loadOrEmpty(generalFile);
        gen.set("recipes." + key, null);
        saveAtomic(gen, generalFile);

        if (isCustom) {
            YamlConfiguration pot = loadOrEmpty(potionsFile);
            pot.set(key, null);
            saveAtomic(pot, potionsFile);
        }
    }

    private static YamlConfiguration loadOrEmpty(File file) {
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        return new YamlConfiguration();
    }

    private static void saveAtomic(YamlConfiguration cfg, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null) parent.mkdirs();
        File temp = File.createTempFile("alchemica-", ".yml.tmp", parent);
        try {
            cfg.save(temp);
            try {
                Files.move(temp.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            temp.delete();
            throw e;
        }
    }
}
```

- [ ] **Step 4: Run tests**

```
./gradlew :common:test --tests "*.RecipeYmlWriterTest"
```
Expected: all pass.

- [ ] **Step 5: Run full test suite**

```
./gradlew :common:test
```
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/org/aincraft/io/RecipeYmlWriter.java \
        common/src/test/java/org/aincraft/io/RecipeYmlWriterTest.java
git commit -m "feat: add RecipeYmlWriter with atomic yml persistence"
```

---

### Task 6: `WizardSessionFactory#fromYml`

**Files:**
- Create: `common/src/main/java/org/aincraft/wizard/WizardSessionFactory.java`
- Create: `common/src/test/java/org/aincraft/wizard/WizardSessionFactoryTest.java`

- [ ] **Step 1: Write failing tests**

Create `common/src/test/java/org/aincraft/wizard/WizardSessionFactoryTest.java`:

```java
package org.aincraft.wizard;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.aincraft.io.RecipeYmlWriter;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class WizardSessionFactoryTest {

    @TempDir File tmpDir;
    private File generalFile;
    private File potionsFile;
    private RecipeYmlWriter writer;
    private WizardSessionFactory factory;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        generalFile = new File(tmpDir, "general.yml");
        potionsFile = new File(tmpDir, "potions.yml");
        writer = new RecipeYmlWriter(generalFile, potionsFile);
        factory = new WizardSessionFactory(generalFile, potionsFile);
    }

    @AfterEach
    void tearDown() { MockBukkit.unmock(); }

    @Test
    void fromYml_vanillaRoundTrip_allFieldsEqual() throws IOException {
        WizardSession s = new WizardSession();
        s.key = "my-brew";
        s.resultType = RecipeResultType.VANILLA;
        s.potionType = PotionType.SWIFTNESS;
        s.ingredients.add(Material.NETHER_WART);
        s.ingredients.add(Material.BLAZE_POWDER);
        s.modifierToggles.put("splash-potion", true);
        s.modifierToggles.put("lingering-potion", false);
        writer.write(s);

        WizardSession loaded = factory.fromYml("my-brew");
        assertNotNull(loaded);
        assertTrue(loaded.editMode);
        assertEquals("my-brew", loaded.originalKey);
        assertEquals("my-brew", loaded.key);
        assertEquals(RecipeResultType.VANILLA, loaded.resultType);
        assertEquals(PotionType.SWIFTNESS, loaded.potionType);
        assertEquals(2, loaded.ingredients.size());
        assertTrue(loaded.ingredients.contains(Material.NETHER_WART));
        // fromYml only populates disabled modifiers (false). Enabled modifiers are absent (null).
        // The Modifiers wizard step in Chunk 4 seeds all global modifiers to true, then
        // overrides with loaded.modifierToggles — so null == enabled by convention.
        assertEquals(Boolean.FALSE, loaded.modifierToggles.get("lingering-potion"));
        assertNull(loaded.modifierToggles.get("splash-potion")); // enabled → absent
    }

    @Test
    void fromYml_customRoundTrip_allFieldsEqual() throws IOException {
        WizardSession s = new WizardSession();
        s.key = "my-brew";
        s.resultType = RecipeResultType.CUSTOM;
        s.ingredients.add(Material.NETHER_WART);
        s.name = "&6My Brew";
        s.color = "FF4500";
        s.lore = List.of("&7Line one.");
        s.effects.add(new WizardSession.EffectEntry(PotionEffectType.SPEED, 1, 3600));
        writer.write(s);

        WizardSession loaded = factory.fromYml("my-brew");
        assertNotNull(loaded);
        assertEquals(RecipeResultType.CUSTOM, loaded.resultType);
        assertEquals("&6My Brew", loaded.name);
        assertEquals("FF4500", loaded.color);
        assertNotNull(loaded.lore);
        assertEquals(1, loaded.lore.size());
        assertEquals(1, loaded.effects.size());
        assertEquals(PotionEffectType.SPEED, loaded.effects.get(0).type);
        assertEquals(1, loaded.effects.get(0).amplifier);
        assertEquals(3600, loaded.effects.get(0).durationTicks);
    }

    @Test
    void fromYml_notFound_returnsNull() {
        WizardSession result = factory.fromYml("nonexistent");
        assertNull(result);
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```
./gradlew :common:test --tests "*.WizardSessionFactoryTest"
```
Expected: FAIL — `WizardSessionFactory` doesn't exist.

- [ ] **Step 3: Implement `WizardSessionFactory`**

Create `common/src/main/java/org/aincraft/wizard/WizardSessionFactory.java`:

```java
package org.aincraft.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

public final class WizardSessionFactory {

    private final File generalFile;
    private final File potionsFile;

    public WizardSessionFactory(File generalFile, File potionsFile) {
        this.generalFile = generalFile;
        this.potionsFile = potionsFile;
    }

    /**
     * Loads an existing recipe into a new {@link WizardSession} ready for editing.
     *
     * @return populated session with {@code editMode=true}, or {@code null} if the
     *         recipe is not found or the yml cannot be parsed.
     */
    @Nullable
    public WizardSession fromYml(String key) {
        if (!generalFile.exists()) return null;
        YamlConfiguration gen = YamlConfiguration.loadConfiguration(generalFile);
        ConfigurationSection recipeSection = gen.getConfigurationSection("recipes." + key);
        if (recipeSection == null) return null;

        WizardSession session = new WizardSession();
        session.editMode = true;
        session.originalKey = key;
        session.key = key;

        // Ingredients
        for (String mat : recipeSection.getStringList("ingredients")) {
            try {
                Material material = Material.matchMaterial(mat.toUpperCase(Locale.ENGLISH));
                if (material != null) session.ingredients.add(material);
            } catch (Exception ignored) {}
        }

        // Result type
        String result = recipeSection.getString("result");
        if (result != null && result.startsWith("alchemica:")) {
            session.resultType = RecipeResultType.CUSTOM;
            loadCustomProperties(session, key);
        } else {
            String potionTypeStr = recipeSection.getString("potion-type");
            if (potionTypeStr == null) return null;
            PotionType potionType = org.bukkit.Registry.POTION.get(
                NamespacedKey.minecraft(potionTypeStr.toLowerCase(Locale.ENGLISH)));
            if (potionType == null) return null;
            session.potionType = potionType;
            session.resultType = RecipeResultType.VANILLA;
        }

        // Modifier toggles — all known modifiers default true; disabled ones set to false
        List<String> disabled = recipeSection.getStringList("disabled-modifiers");
        for (String mod : disabled) {
            session.modifierToggles.put(mod, false);
        }

        return session;
    }

    private void loadCustomProperties(WizardSession session, String key) {
        if (!potionsFile.exists()) return;
        YamlConfiguration pot = YamlConfiguration.loadConfiguration(potionsFile);
        ConfigurationSection def = pot.getConfigurationSection(key);
        if (def == null) return;

        session.name = def.getString("name");
        session.color = def.getString("color");
        List<String> lore = def.getStringList("lore");
        session.lore = lore.isEmpty() ? null : lore;

        for (Map<?, ?> effectMap : def.getMapList("effects")) {
            String typeStr = (String) effectMap.get("type");
            if (typeStr == null) continue;
            try {
                PotionEffectType type = org.bukkit.Registry.EFFECT.get(NamespacedKey.minecraft(typeStr));
                if (type == null) continue;
                int amplifier = effectMap.containsKey("amplifier")
                    ? ((Number) effectMap.get("amplifier")).intValue() : 0;
                int duration = effectMap.containsKey("duration")
                    ? ((Number) effectMap.get("duration")).intValue() : 600;
                session.effects.add(new WizardSession.EffectEntry(type, amplifier, duration));
            } catch (Exception ignored) {}
        }
    }
}
```

- [ ] **Step 4: Run tests**

```
./gradlew :common:test --tests "*.WizardSessionFactoryTest"
```
Expected: all pass.

- [ ] **Step 5: Run full test suite**

```
./gradlew :common:test
```
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/org/aincraft/wizard/WizardSessionFactory.java \
        common/src/test/java/org/aincraft/wizard/WizardSessionFactoryTest.java
git commit -m "feat: add WizardSessionFactory for loading recipes from yml"
```

---

## Chunk 3: GUI Infrastructure, Hub, and Command

### Task 7: Key validation utility

**Files:**
- Create: `common/src/main/java/org/aincraft/wizard/RecipeKeyValidator.java`
- Create: `common/src/test/java/org/aincraft/wizard/RecipeKeyValidatorTest.java`

- [ ] **Step 1: Write failing tests**

Create `common/src/test/java/org/aincraft/wizard/RecipeKeyValidatorTest.java`:

```java
package org.aincraft.wizard;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeKeyValidatorTest {

    @Test
    void validate_empty_fails() {
        assertFalse(RecipeKeyValidator.isValid(""));
    }

    @Test
    void validate_specialChars_fails() {
        assertFalse(RecipeKeyValidator.isValid("my potion!"));
        assertFalse(RecipeKeyValidator.isValid("my_potion"));
    }

    @Test
    void validate_valid_passes() {
        assertTrue(RecipeKeyValidator.isValid("my-potion"));
        assertTrue(RecipeKeyValidator.isValid("speed2"));
        assertTrue(RecipeKeyValidator.isValid("a"));
    }

    @Test
    void normalize_lowercasesAndReplacesSpaces() {
        assertEquals("my-potion", RecipeKeyValidator.normalize("My Potion"));
        assertEquals("speed2", RecipeKeyValidator.normalize("SPEED2"));
    }

    @Test
    void validate_hexColor_valid() {
        assertTrue(RecipeKeyValidator.isValidHex("FF4500"));
        assertTrue(RecipeKeyValidator.isValidHex("ff4500"));
        assertTrue(RecipeKeyValidator.isValidHex("000000"));
    }

    @Test
    void validate_hexColor_invalid() {
        assertFalse(RecipeKeyValidator.isValidHex("#FF4500")); // leading #
        assertFalse(RecipeKeyValidator.isValidHex("FF450"));   // 5 chars
        assertFalse(RecipeKeyValidator.isValidHex("FF450G"));  // non-hex char
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```
./gradlew :common:test --tests "*.RecipeKeyValidatorTest"
```

- [ ] **Step 3: Implement `RecipeKeyValidator`**

```java
package org.aincraft.wizard;

import java.util.Locale;

public final class RecipeKeyValidator {

    private static final java.util.regex.Pattern KEY_PATTERN =
        java.util.regex.Pattern.compile("^[a-z0-9-]+$");
    private static final java.util.regex.Pattern HEX_PATTERN =
        java.util.regex.Pattern.compile("^[0-9a-fA-F]{6}$");

    private RecipeKeyValidator() {}

    public static boolean isValid(String key) {
        return key != null && !key.isEmpty() && KEY_PATTERN.matcher(key).matches();
    }

    public static String normalize(String input) {
        return input.toLowerCase(Locale.ENGLISH).replace(' ', '-');
    }

    public static boolean isValidHex(String hex) {
        return hex != null && HEX_PATTERN.matcher(hex).matches();
    }
}
```

- [ ] **Step 4: Run tests**

```
./gradlew :common:test --tests "*.RecipeKeyValidatorTest"
```
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/org/aincraft/wizard/RecipeKeyValidator.java \
        common/src/test/java/org/aincraft/wizard/RecipeKeyValidatorTest.java
git commit -m "feat: add RecipeKeyValidator for key and hex color validation"
```

---

### Task 8: `LoreCaptureManager`

**Files:**
- Create: `common/src/main/java/org/aincraft/wizard/LoreCaptureManager.java`

Manages chat-based lore capture per player. Registered as a Bukkit listener.

- [ ] **Step 1: Implement `LoreCaptureManager`**

```java
package org.aincraft.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Manages lore-line capture sessions.
 * When active, AsyncPlayerChatEvent messages are intercepted (cancelled) and
 * accumulated. A blank message ends capture and invokes the completion callback
 * with the collected lines.
 */
public final class LoreCaptureManager implements Listener {

    private static final class CaptureState {
        final List<String> lines = new ArrayList<>();
        final Consumer<List<String>> onComplete;

        CaptureState(Consumer<List<String>> onComplete) {
            this.onComplete = onComplete;
        }
    }

    private final Map<UUID, CaptureState> active = new HashMap<>();

    /**
     * Starts capture for {@code player}. {@code onComplete} is called on the main thread
     * once the player sends a blank line.
     */
    public void start(Player player, Consumer<List<String>> onComplete) {
        active.put(player.getUniqueId(), new CaptureState(onComplete));
        player.sendMessage("[Alchemica] Type lore lines. Send a blank message to finish.");
    }

    public boolean isCapturing(UUID playerId) {
        return active.containsKey(playerId);
    }

    public void cancel(UUID playerId) {
        active.remove(playerId);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        CaptureState state = active.get(id);
        if (state == null) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        if (message.isEmpty()) {
            active.remove(id);
            List<String> collected = new ArrayList<>(state.lines);
            // Schedule callback on main thread
            event.getPlayer().getServer().getScheduler().runTask(
                event.getPlayer().getServer().getPluginManager()
                    .getPlugin("Alchemica"),
                () -> state.onComplete.accept(collected)
            );
        } else {
            state.lines.add(message);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
    }
}
```

- [ ] **Step 2: Run tests**

```
./gradlew :common:test
```
Expected: all pass.

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/org/aincraft/wizard/LoreCaptureManager.java
git commit -m "feat: add LoreCaptureManager for chat-based lore input"
```

---

### Task 9: GUI utilities and `GuiListener`

**Files:**
- Create: `common/src/main/java/org/aincraft/gui/GuiUtils.java`
- Create: `common/src/main/java/org/aincraft/gui/GuiListener.java`
- Create: `common/src/main/java/org/aincraft/gui/AlchemicaGui.java`

- [ ] **Step 1: Create `AlchemicaGui` interface (marker + click handler)**

```java
package org.aincraft.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * All Alchemica GUI classes implement this interface.
 * The {@link GuiListener} dispatches {@link InventoryClickEvent} to the correct GUI
 * by checking the holder type.
 */
public interface AlchemicaGui extends InventoryHolder {

    /** Called when the player clicks any slot in this GUI's inventory. */
    void onClick(InventoryClickEvent event);
}
```

- [ ] **Step 2: Create `GuiUtils`**

```java
package org.aincraft.gui;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuiUtils {

    private GuiUtils() {}

    /** Creates a named item with optional lore. */
    public static ItemStack named(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                meta.setLore(List.of(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Fills all null slots in a row (0-based row index) with gray glass panes. */
    public static void fillRow(Inventory inv, int row) {
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        int start = row * 9;
        for (int i = start; i < start + 9; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }

    /** Fills all null slots in the entire inventory with gray glass panes. */
    public static void fillEmpty(Inventory inv) {
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }
}
```

- [ ] **Step 3: Create `GuiListener`**

```java
package org.aincraft.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Dispatches inventory click events to the correct {@link AlchemicaGui} implementation
 * by checking the inventory holder type. Cancels all clicks to prevent item movement.
 */
public final class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AlchemicaGui gui)) return;
        event.setCancelled(true);
        gui.onClick(event);
    }
}
```

- [ ] **Step 4: Run tests**

```
./gradlew :common:test
```
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/org/aincraft/gui/
git commit -m "feat: add GUI infrastructure (AlchemicaGui, GuiListener, GuiUtils)"
```

---

### Task 10: `RecipeHubGui` and `BrewCommand`

**Files:**
- Create: `common/src/main/java/org/aincraft/gui/RecipeHubGui.java`
- Create: `common/src/main/java/org/aincraft/command/BrewCommand.java`
- Create: `common/src/test/java/org/aincraft/command/BrewCommandTest.java`

- [ ] **Step 1: Write failing permission tests**

Create `common/src/test/java/org/aincraft/command/BrewCommandTest.java`:

```java
package org.aincraft.command;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;
import org.aincraft.wizard.WizardSessionManager;
import org.aincraft.wizard.LoreCaptureManager;
import org.bukkit.command.Command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrewCommandTest {

    private ServerMock server;
    private PlayerMock player;
    private PluginMock plugin;
    private BrewCommand command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        player = server.addPlayer();
        WizardSessionManager sessions = new WizardSessionManager();
        LoreCaptureManager lore = new LoreCaptureManager();
        command = new BrewCommand(plugin, sessions, lore, null, null);
    }

    @AfterEach
    void tearDown() { MockBukkit.unmock(); }

    @Test
    void onCommand_withoutPermission_sendsMessageAndReturnsFalse() {
        // player has no alchemica.admin permission
        boolean result = command.onCommand(player, null, "brew", new String[]{});
        assertFalse(result);
        assertEquals(1, player.nextMessage() != null ? 1 : 0);
    }

    @Test
    void onCommand_duringLoreCapture_sendsBlockedMessage() {
        LoreCaptureManager lore = new LoreCaptureManager();
        lore.start(player, lines -> {});
        BrewCommand cmd = new BrewCommand(plugin, new WizardSessionManager(), lore, null, null);
        plugin.getServer().getPluginManager().registerEvents(lore, plugin);
        boolean result = cmd.onCommand(player, null, "brew", new String[]{});
        assertFalse(result);
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```
./gradlew :common:test --tests "*.BrewCommandTest"
```

- [ ] **Step 3: Implement `RecipeHubGui`**

```java
package org.aincraft.gui;

import org.aincraft.wizard.WizardSession;
import org.aincraft.wizard.WizardSessionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * The hub GUI: offers "Create Recipe" and "Browse Recipes".
 * The calling code is responsible for opening it with {@link Player#openInventory}.
 */
public final class RecipeHubGui implements AlchemicaGui {

    private final Inventory inventory;
    private final Player player;
    private final WizardSessionManager sessionManager;
    private final Runnable openWizard;
    private final Runnable openBrowser;

    public RecipeHubGui(Player player, WizardSessionManager sessionManager,
            Runnable openWizard, Runnable openBrowser) {
        this.player = player;
        this.sessionManager = sessionManager;
        this.openWizard = openWizard;
        this.openBrowser = openBrowser;
        this.inventory = buildInventory();
    }

    private Inventory buildInventory() {
        Inventory inv = Bukkit.createInventory(this, 27, "Alchemica — Recipe Manager");
        inv.setItem(11, GuiUtils.named(Material.CAULDRON, "&aCreate Recipe",
            "&7Start the recipe wizard"));
        inv.setItem(15, GuiUtils.named(Material.BOOK, "&eBrowse Recipes",
            "&7View and edit existing recipes"));
        GuiUtils.fillEmpty(inv);
        return inv;
    }

    @Override
    public Inventory getInventory() { return inventory; }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 11) openWizard.run();
        else if (slot == 15) openBrowser.run();
    }
}
```

- [ ] **Step 4: Implement `BrewCommand`**

```java
package org.aincraft.command;

import org.aincraft.gui.RecipeHubGui;
import org.aincraft.wizard.LoreCaptureManager;
import org.aincraft.wizard.WizardSession;
import org.aincraft.wizard.WizardSessionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BrewCommand implements CommandExecutor {

    private final Plugin plugin;
    private final WizardSessionManager sessionManager;
    private final LoreCaptureManager loreCaptureManager;
    /** Called with the player at command execution time; null in unit tests. */
    @Nullable private final java.util.function.Consumer<org.bukkit.entity.Player> openWizardForPlayer;
    @Nullable private final java.util.function.Consumer<org.bukkit.entity.Player> openBrowserForPlayer;

    public BrewCommand(Plugin plugin, WizardSessionManager sessionManager,
            LoreCaptureManager loreCaptureManager,
            @Nullable java.util.function.Consumer<org.bukkit.entity.Player> openWizardForPlayer,
            @Nullable java.util.function.Consumer<org.bukkit.entity.Player> openBrowserForPlayer) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.loreCaptureManager = loreCaptureManager;
        this.openWizardForPlayer = openWizardForPlayer;
        this.openBrowserForPlayer = openBrowserForPlayer;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @Nullable Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /brew.");
            return true;
        }
        if (!player.hasPermission("alchemica.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use /brew.");
            return false;
        }
        if (loreCaptureManager.isCapturing(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW
                + "Finish entering lore first (send a blank line to stop).");
            return false;
        }
        // Discard any existing session
        if (sessionManager.has(player.getUniqueId())) {
            sessionManager.remove(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Previous recipe creation discarded.");
        }

        if (openWizardForPlayer == null || openBrowserForPlayer == null) {
            // Unit-test path — no GUI to open
            return false;
        }

        RecipeHubGui hub = new RecipeHubGui(player, sessionManager,
            () -> openWizardForPlayer.accept(player),
            () -> openBrowserForPlayer.accept(player));
        player.openInventory(hub.getInventory());
        return true;
    }
}
```

- [ ] **Step 5: Run tests**

```
./gradlew :common:test --tests "*.BrewCommandTest"
```
Expected: all pass.

- [ ] **Step 6: Run full test suite**

```
./gradlew :common:test
```
Expected: all pass.

- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/org/aincraft/gui/RecipeHubGui.java \
        common/src/main/java/org/aincraft/command/BrewCommand.java \
        common/src/test/java/org/aincraft/command/BrewCommandTest.java
git commit -m "feat: add RecipeHubGui and BrewCommand"
```

---

### Task 11: Wire everything in `Brew.enable()`

**Files:**
- Modify: `common/src/main/java/org/aincraft/internal/Brew.java`
- Modify: `common/src/main/java/org/aincraft/internal/Internal.java`

Wire `GuiListener`, `LoreCaptureManager`, `WizardSessionManager`, `BrewCommand`, `RecipeYmlWriter`, `WizardSessionFactory` into the plugin lifecycle.

- [ ] **Step 1: Add fields to `Internal`**

Add to `Internal` class:
```java
final WizardSessionManager wizardSessionManager;
final LoreCaptureManager loreCaptureManager;
final RecipeYmlWriter recipeYmlWriter;
final WizardSessionFactory wizardSessionFactory;
```

Add these to the constructor and update `Internal.create()` to instantiate them:
```java
// In Internal.create():
File dataFolder = plugin.getDataFolder();
File generalFile = new File(dataFolder, "general.yml");
File potionsFile = new File(dataFolder, "potions.yml");
WizardSessionManager sessionManager = new WizardSessionManager();
LoreCaptureManager loreManager = new LoreCaptureManager();
RecipeYmlWriter ymlWriter = new RecipeYmlWriter(generalFile, potionsFile);
WizardSessionFactory sessionFactory = new WizardSessionFactory(generalFile, potionsFile);
```

- [ ] **Step 2: Register listeners and commands in `Brew.enable()`**

```java
// In Brew.enable(), after the existing event registrations:
pm.registerEvents(new GuiListener(), plugin);
pm.registerEvents(internal.loreCaptureManager, plugin);

// Register /brew with null openers for now — Task 18 Step 3 replaces these
// with concrete Consumer<Player> lambdas once all GUI classes exist.
BrewCommand brewCommand = new BrewCommand(
    plugin,
    internal.wizardSessionManager,
    internal.loreCaptureManager,
    null,   // replaced in Task 18 Step 3
    null    // replaced in Task 18 Step 3
);
Bukkit.getPluginCommand("brew").setExecutor(brewCommand);
```

Also register a `PlayerQuitListener` to clean up sessions:
```java
pm.registerEvents(new Listener() {
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        internal.wizardSessionManager.remove(e.getPlayer().getUniqueId());
    }
}, plugin);
```

- [ ] **Step 3: Run tests**

```
./gradlew :common:test
```
Expected: all pass.

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/org/aincraft/internal/Brew.java \
        common/src/main/java/org/aincraft/internal/Internal.java
git commit -m "feat: wire GuiListener, LoreCaptureManager, BrewCommand into Brew.enable()"
```

---

## Chunk 4: Wizard Steps 1–4

### Task 12: Wizard framework and Steps 1–3

**Files:**
- Create: `common/src/main/java/org/aincraft/gui/wizard/RecipeWizardGui.java`
- Create: `common/src/main/java/org/aincraft/gui/wizard/WizardStep.java`

The wizard is a single `RecipeWizardGui` object that builds a new `Inventory` for each step and reopens it. A `WizardStep` enum tracks the current step.

- [ ] **Step 1: Create `WizardStep` enum**

```java
package org.aincraft.gui.wizard;

public enum WizardStep {
    KEY,
    INGREDIENTS,
    RESULT_TYPE,
    CUSTOM_PROPERTIES,
    MODIFIERS,
    CONFIRM
}
```

- [ ] **Step 2: Implement `RecipeWizardGui` skeleton with Steps 1–3**

Create `common/src/main/java/org/aincraft/gui/wizard/RecipeWizardGui.java`.

Key design:
- `openStep(WizardStep step)` — builds and opens the inventory for that step
- `back()` / `next()` — move to previous/next visible step
- Step 1 (KEY): Open anvil GUI via `player.openAnvil(null, true)` — rename paper item. Watch `PrepareAnvilEvent` to read result.
- Step 2 (INGREDIENTS): 3-row chest with ingredient slots + Add/Clear/Next buttons
- Step 3 (RESULT_TYPE): 2-row chest with Vanilla and Custom buttons

```java
package org.aincraft.gui.wizard;

import java.util.List;
import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.aincraft.wizard.RecipeKeyValidator;
import org.aincraft.wizard.WizardSession;
import org.aincraft.wizard.WizardSessionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Controls all wizard step screens for one player.
 * Call {@link #open()} to start from Step 1 (or from the current session state if editing).
 */
public final class RecipeWizardGui implements AlchemicaGui {

    private final Plugin plugin;
    private final Player player;
    private final WizardSession session;
    private final WizardSessionManager sessionManager;
    private final Runnable onCancel;   // returns player to hub
    /** Keys that already exist in the live registry. Used to reject duplicates in Step 1. */
    private final java.util.Set<String> existingKeys;

    private WizardStep currentStep = WizardStep.KEY;
    private Inventory currentInventory;

    public RecipeWizardGui(Plugin plugin, Player player, WizardSession session,
            WizardSessionManager sessionManager, Runnable onCancel,
            java.util.Set<String> existingKeys) {
        this.plugin = plugin;
        this.player = player;
        this.session = session;
        this.sessionManager = sessionManager;
        this.onCancel = onCancel;
        this.existingKeys = existingKeys;
        sessionManager.put(player.getUniqueId(), session);
    }

    public void open() {
        openStep(WizardStep.KEY);
    }

    private void openStep(WizardStep step) {
        currentStep = step;
        switch (step) {
            case KEY -> openKeyStep();
            case INGREDIENTS -> openIngredientsStep();
            case RESULT_TYPE -> openResultTypeStep();
            case CUSTOM_PROPERTIES -> openCustomPropertiesStep();
            case MODIFIERS -> openModifiersStep();
            case CONFIRM -> openConfirmStep();
        }
    }

    // ---- Step 1: KEY ----

    private void openKeyStep() {
        // Use an anvil GUI to get text input
        // We open an inventory of type ANVIL via Bukkit
        var anvil = Bukkit.createInventory(this, org.bukkit.event.inventory.InventoryType.ANVIL,
            buildTitle(1));
        ItemStack paper = GuiUtils.named(Material.PAPER,
            session.key != null ? session.key : "Enter recipe key...");
        anvil.setItem(0, paper);
        currentInventory = anvil;
        player.openInventory(anvil);
    }

    // ---- Step 2: INGREDIENTS ----

    private void openIngredientsStep() {
        Inventory inv = Bukkit.createInventory(this, 27, buildTitle(2));
        // Place existing ingredients in slots 0-17
        for (int i = 0; i < session.ingredients.size() && i < 18; i++) {
            inv.setItem(i, new ItemStack(session.ingredients.get(i)));
        }
        // Bottom row controls
        inv.setItem(18, GuiUtils.named(Material.LIME_DYE, "&aAdd Ingredient"));
        inv.setItem(20, GuiUtils.named(Material.RED_DYE, "&cClear All"));
        ItemStack next = session.ingredients.isEmpty()
            ? GuiUtils.named(Material.GRAY_STAINED_GLASS_PANE, "&7Next (add an ingredient first)")
            : GuiUtils.named(Material.ARROW, "&aNext");
        inv.setItem(26, next);
        inv.setItem(0 /* back slot 18 col 0 */, null); // already set above
        inv.setItem(19, GuiUtils.named(Material.ARROW, "&7Back"));
        inv.setItem(25, GuiUtils.named(Material.BARRIER, "&cCancel"));
        GuiUtils.fillEmpty(inv);
        currentInventory = inv;
        player.openInventory(inv);
    }

    // ---- Step 3: RESULT TYPE ----

    private void openResultTypeStep() {
        Inventory inv = Bukkit.createInventory(this, 18, buildTitle(3));
        inv.setItem(2, GuiUtils.named(Material.POTION, "&bVanilla Potion",
            "&7Choose a built-in potion type"));
        inv.setItem(6, GuiUtils.named(Material.CAULDRON, "&dCustom Potion",
            "&7Define your own effects, name, and color"));
        inv.setItem(0, GuiUtils.named(Material.ARROW, "&7Back"));
        inv.setItem(8, GuiUtils.named(Material.BARRIER, "&cCancel"));
        GuiUtils.fillEmpty(inv);
        currentInventory = inv;
        player.openInventory(inv);
    }

    // ---- Steps 4-6 (implemented in Tasks 14-16) ----

    private void openCustomPropertiesStep() { /* Task 14 */ }
    private void openModifiersStep()         { /* Task 15 */ }
    private void openConfirmStep()           { /* Task 16 */ }

    // ---- Click handling ----

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        switch (currentStep) {
            case KEY -> handleKeyClick(slot, event);
            case INGREDIENTS -> handleIngredientsClick(slot, event);
            case RESULT_TYPE -> handleResultTypeClick(slot, event);
            default -> {}
        }
    }

    private void handleKeyClick(int slot, InventoryClickEvent event) {
        // Anvil output slot = 2
        if (slot == 2) {
            ItemStack result = event.getCurrentItem();
            if (result == null || result.getItemMeta() == null) return;
            String raw = result.getItemMeta().getDisplayName();
            String normalized = RecipeKeyValidator.normalize(
                ChatColor.stripColor(raw));
            if (!RecipeKeyValidator.isValid(normalized)) {
                player.sendMessage(ChatColor.RED
                    + "Invalid key. Use only letters, numbers, and hyphens.");
                return;
            }
            // Reject duplicate keys (allow own key in edit mode)
            if (existingKeys.contains(normalized)
                    && !normalized.equals(session.originalKey)) {
                player.sendMessage(ChatColor.RED
                    + "A recipe named '" + normalized + "' already exists.");
                return;
            }
            session.key = normalized;
            openStep(WizardStep.INGREDIENTS);
        }
    }

    private void handleIngredientsClick(int slot, InventoryClickEvent event) {
        if (slot == 19) { back(); return; }
        if (slot == 25) { cancel(); return; }
        if (slot == 18) { openMaterialPicker(); return; }
        if (slot == 20) { session.ingredients.clear(); openStep(WizardStep.INGREDIENTS); return; }
        if (slot == 26 && !session.ingredients.isEmpty()) {
            openStep(WizardStep.RESULT_TYPE); return;
        }
        // Shift-click ingredient slot to remove
        if (slot < 18 && event.isShiftClick()) {
            int idx = slot;
            if (idx < session.ingredients.size()) {
                session.ingredients.remove(idx);
                openStep(WizardStep.INGREDIENTS);
            }
        }
    }

    private void handleResultTypeClick(int slot, InventoryClickEvent event) {
        if (slot == 0) { back(); return; }
        if (slot == 8) { cancel(); return; }
        if (slot == 2) { openVanillaPotionPicker(); return; }
        if (slot == 6) {
            session.resultType = org.aincraft.wizard.RecipeResultType.CUSTOM;
            openStep(WizardStep.CUSTOM_PROPERTIES);
        }
    }

    private void openMaterialPicker() {
        new MaterialPickerGui(plugin, player, material -> {
            if (!session.ingredients.contains(material)) {
                session.ingredients.add(material);
            }
            openStep(WizardStep.INGREDIENTS);
        }, () -> openStep(WizardStep.INGREDIENTS)).open();
    }

    private void openVanillaPotionPicker() {
        // Opens a paginated potion type selector; selected type → session.potionType
        // then opens MODIFIERS step
        new VanillaPotionPickerGui(plugin, player, potionType -> {
            session.potionType = potionType;
            session.resultType = org.aincraft.wizard.RecipeResultType.VANILLA;
            openStep(WizardStep.MODIFIERS);
        }, () -> openStep(WizardStep.RESULT_TYPE)).open();
    }

    private void back() {
        switch (currentStep) {
            case KEY -> cancel();
            case INGREDIENTS -> openStep(WizardStep.KEY);
            case RESULT_TYPE -> openStep(WizardStep.INGREDIENTS);
            case CUSTOM_PROPERTIES -> openStep(WizardStep.RESULT_TYPE);
            case MODIFIERS -> {
                if (session.resultType == org.aincraft.wizard.RecipeResultType.CUSTOM) {
                    openStep(WizardStep.CUSTOM_PROPERTIES);
                } else {
                    openStep(WizardStep.RESULT_TYPE);
                }
            }
            case CONFIRM -> {
                // If there are no modifiers, skip past the Modifiers step
                if (!modifierKeys.isEmpty()) {
                    openStep(WizardStep.MODIFIERS);
                } else if (session.resultType == org.aincraft.wizard.RecipeResultType.CUSTOM) {
                    openStep(WizardStep.CUSTOM_PROPERTIES);
                } else {
                    openStep(WizardStep.RESULT_TYPE);
                }
            }
        }
    }

    private void cancel() {
        sessionManager.remove(player.getUniqueId());
        player.closeInventory();
        onCancel.run();
    }

    private String buildTitle(int stepNum) {
        return "Create Recipe \u2014 Step " + stepNum + " / ?";
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
```

- [ ] **Step 3: Run tests**

```
./gradlew :common:test
```
Expected: all pass (compile check).

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/org/aincraft/gui/wizard/
git commit -m "feat: add RecipeWizardGui skeleton with steps 1-3"
```

---

### Task 13: `MaterialPickerGui` and `VanillaPotionPickerGui`

**Files:**
- Create: `common/src/main/java/org/aincraft/gui/wizard/MaterialPickerGui.java`
- Create: `common/src/main/java/org/aincraft/gui/wizard/VanillaPotionPickerGui.java`

Both are paginated 6-row chest GUIs.

- [ ] **Step 1: Implement `MaterialPickerGui`**

```java
package org.aincraft.gui.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class MaterialPickerGui implements AlchemicaGui {

    private static final List<Material> MATERIALS;

    static {
        List<Material> list = new ArrayList<>();
        for (Material m : Material.values()) {
            if (m.isItem() && m != Material.AIR && !m.isLegacy()) {
                list.add(m);
            }
        }
        MATERIALS = List.copyOf(list);
    }

    private static final int PAGE_SIZE = 45; // 5 rows × 9

    private final Plugin plugin;
    private final Player player;
    private final Consumer<Material> onSelect;
    private final Runnable onBack;
    private int page = 0;
    private Inventory currentInventory;

    public MaterialPickerGui(Plugin plugin, Player player,
            Consumer<Material> onSelect, Runnable onBack) {
        this.plugin = plugin;
        this.player = player;
        this.onSelect = onSelect;
        this.onBack = onBack;
    }

    public void open() {
        buildAndOpen(0);
    }

    private void buildAndOpen(int page) {
        this.page = page;
        Inventory inv = Bukkit.createInventory(this, 54, "Select Ingredient");
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < MATERIALS.size(); i++) {
            Material m = MATERIALS.get(start + i);
            inv.setItem(i, GuiUtils.named(m, "&f" + m.name().toLowerCase()
                .replace('_', ' ')));
        }
        // Controls row (row 6)
        if (page > 0) {
            inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Previous"));
        }
        inv.setItem(49, GuiUtils.named(Material.BARRIER, "&cBack"));
        if (start + PAGE_SIZE < MATERIALS.size()) {
            inv.setItem(53, GuiUtils.named(Material.ARROW, "&7Next"));
        }
        GuiUtils.fillRow(inv, 5);
        currentInventory = inv;
        player.openInventory(inv);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) { onBack.run(); return; }
        if (slot == 45 && page > 0) { buildAndOpen(page - 1); return; }
        if (slot == 53 && (page + 1) * PAGE_SIZE < MATERIALS.size()) {
            buildAndOpen(page + 1); return;
        }
        if (slot < 45) {
            int idx = page * PAGE_SIZE + slot;
            if (idx < MATERIALS.size()) {
                onSelect.accept(MATERIALS.get(idx));
            }
        }
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
```

- [ ] **Step 2: Implement `VanillaPotionPickerGui`**

Same pattern as `MaterialPickerGui` but iterates `PotionType.values()`. Each item is a `POTION` with `PotionMeta` set via `PotionMeta#setBasePotionData` (or `setBasePotionType` on 1.20.5+). Use a try/catch for version compatibility.

```java
package org.aincraft.gui.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

public final class VanillaPotionPickerGui implements AlchemicaGui {

    private static final List<PotionType> TYPES;
    static {
        List<PotionType> list = new ArrayList<>();
        for (PotionType t : PotionType.values()) {
            list.add(t);
        }
        TYPES = List.copyOf(list);
    }

    private static final int PAGE_SIZE = 45;

    private final Plugin plugin;
    private final Player player;
    private final Consumer<PotionType> onSelect;
    private final Runnable onBack;
    private int page = 0;
    private Inventory currentInventory;

    public VanillaPotionPickerGui(Plugin plugin, Player player,
            Consumer<PotionType> onSelect, Runnable onBack) {
        this.plugin = plugin;
        this.player = player;
        this.onSelect = onSelect;
        this.onBack = onBack;
    }

    public void open() { buildAndOpen(0); }

    private void buildAndOpen(int p) {
        this.page = p;
        Inventory inv = Bukkit.createInventory(this, 54, "Select Potion Type");
        int start = p * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < TYPES.size(); i++) {
            PotionType type = TYPES.get(start + i);
            ItemStack stack = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) stack.getItemMeta();
            if (meta != null) {
                try {
                    meta.setBasePotionType(type);
                } catch (NoSuchMethodError ignored) {
                    // pre-1.20.5 fallback
                    try {
                        meta.getClass().getMethod("setBasePotionData",
                            org.bukkit.potion.PotionData.class)
                            .invoke(meta, new org.bukkit.potion.PotionData(type));
                    } catch (Exception e2) { /* ignore */ }
                }
                meta.setDisplayName("&f" + type.name().toLowerCase().replace('_', ' '));
                stack.setItemMeta(meta);
            }
            inv.setItem(i, stack);
        }
        if (p > 0) inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Previous"));
        inv.setItem(49, GuiUtils.named(Material.BARRIER, "&cBack"));
        if (start + PAGE_SIZE < TYPES.size())
            inv.setItem(53, GuiUtils.named(Material.ARROW, "&7Next"));
        GuiUtils.fillRow(inv, 5);
        currentInventory = inv;
        player.openInventory(inv);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) { onBack.run(); return; }
        if (slot == 45 && page > 0) { buildAndOpen(page - 1); return; }
        if (slot == 53 && (page + 1) * PAGE_SIZE < TYPES.size()) {
            buildAndOpen(page + 1); return;
        }
        if (slot < 45) {
            int idx = page * PAGE_SIZE + slot;
            if (idx < TYPES.size()) onSelect.accept(TYPES.get(idx));
        }
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
```

- [ ] **Step 3: Run tests**

```
./gradlew :common:test
```
Expected: all pass.

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/org/aincraft/gui/wizard/MaterialPickerGui.java \
        common/src/main/java/org/aincraft/gui/wizard/VanillaPotionPickerGui.java
git commit -m "feat: add MaterialPickerGui and VanillaPotionPickerGui"
```

---

### Task 14: Wizard Step 4 — Custom Properties + Effects

**Files:**
- Modify: `common/src/main/java/org/aincraft/gui/wizard/RecipeWizardGui.java`
- Create: `common/src/main/java/org/aincraft/gui/wizard/EffectPickerGui.java`

- [ ] **Step 1: Implement `openCustomPropertiesStep()` in `RecipeWizardGui`**

Fill in the previously stubbed method:
```java
private void openCustomPropertiesStep() {
    Inventory inv = Bukkit.createInventory(this, 54,
        buildTitle(session.resultType == RecipeResultType.CUSTOM ? 4 : 4));
    // Row 1: property buttons
    String nameLabel = session.name != null
        ? "&aName: " + session.name : "&7Set Name (optional)";
    String colorLabel = session.color != null
        ? "&aColor: #" + session.color : "&7Set Color (optional)";
    String loreLabel = (session.lore != null && !session.lore.isEmpty())
        ? "&aLore: " + session.lore.size() + " line(s)" : "&7Set Lore (optional)";
    inv.setItem(0, GuiUtils.named(Material.NAME_TAG, nameLabel));
    inv.setItem(1, GuiUtils.named(Material.CYAN_DYE, colorLabel));
    inv.setItem(2, GuiUtils.named(Material.WRITABLE_BOOK, loreLabel));
    // Rows 2-4: effects list
    for (int i = 0; i < session.effects.size() && i < 27; i++) {
        WizardSession.EffectEntry e = session.effects.get(i);
        inv.setItem(9 + i, GuiUtils.named(Material.BREWING_STAND,
            "&e" + e.type.getKey().getKey(),
            "&7Amplifier: " + e.amplifier,
            "&7Duration: " + formatTicks(e.durationTicks),
            "&cShift-click to remove"));
    }
    // Row 6: controls
    inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Back"));
    inv.setItem(47, GuiUtils.named(Material.LIME_DYE, "&aAdd Effect"));
    inv.setItem(49, GuiUtils.named(Material.BARRIER, "&cCancel"));
    inv.setItem(53, GuiUtils.named(Material.ARROW, "&aNext"));
    GuiUtils.fillRow(inv, 5);
    currentInventory = inv;
    player.openInventory(inv);
}

private String formatTicks(int ticks) {
    int seconds = ticks / 20;
    return (seconds / 60) + "m " + (seconds % 60) + "s";
}
```

Handle clicks in `RecipeWizardGui.onClick`:
```java
case CUSTOM_PROPERTIES -> handleCustomPropertiesClick(slot, event);
```

```java
private void handleCustomPropertiesClick(int slot, InventoryClickEvent event) {
    if (slot == 45) { back(); return; }
    if (slot == 49) { cancel(); return; }
    if (slot == 53) { openStep(WizardStep.MODIFIERS); return; }
    if (slot == 47) {
        new EffectPickerGui(plugin, player, session, () -> openStep(WizardStep.CUSTOM_PROPERTIES))
            .open();
        return;
    }
    if (slot == 0) { openNameAnvil(); return; }
    if (slot == 1) { openColorAnvil(); return; }
    if (slot == 2) { startLoreCapture(); return; }
    // Shift-click effect to remove
    if (slot >= 9 && slot < 36 && event.isShiftClick()) {
        int idx = slot - 9;
        if (idx < session.effects.size()) {
            session.effects.remove(idx);
            openStep(WizardStep.CUSTOM_PROPERTIES);
        }
    }
}
```

**Anvil for name/color:** Use `org.bukkit.inventory.view.AnvilView` or the legacy approach. Since anvil GUI input is complex to implement cleanly, use a chat-input fallback for the MVP:

```java
private void openNameAnvil() {
    player.closeInventory();
    player.sendMessage("[Alchemica] Type the potion name (supports &color codes). Send blank to cancel.");
    startChatInput(input -> {
        if (!input.isBlank()) session.name = input;
        openStep(WizardStep.CUSTOM_PROPERTIES);
    });
}

private void openColorAnvil() {
    player.closeInventory();
    player.sendMessage("[Alchemica] Type the color as a 6-char hex (e.g. FF4500). Send blank to cancel.");
    startChatInput(input -> {
        if (!input.isBlank()) {
            if (RecipeKeyValidator.isValidHex(input)) {
                session.color = input.toUpperCase(java.util.Locale.ENGLISH);
            } else {
                player.sendMessage(ChatColor.RED + "Invalid hex color. Must be 6 hex chars, no #.");
            }
        }
        openStep(WizardStep.CUSTOM_PROPERTIES);
    });
}

private void startLoreCapture() {
    loreCaptureManager.start(player, lines -> {
        session.lore = lines.isEmpty() ? null : lines;
        openStep(WizardStep.CUSTOM_PROPERTIES);
    });
}

/**
 * Single-line chat input: captures the next non-empty chat message from this player.
 * Piggy-backs on {@link LoreCaptureManager}; the player sends a blank line to cancel.
 * {@code callback} receives the first captured line, or "" on cancel.
 */
private void startChatInput(java.util.function.Consumer<String> callback) {
    loreCaptureManager.start(player, lines ->
        callback.accept(lines.isEmpty() ? "" : lines.get(0)));
}
```

The `RecipeWizardGui` constructor needs a reference to `LoreCaptureManager`. Add it.

- [ ] **Step 2: Implement `EffectPickerGui`**

```java
package org.aincraft.gui.wizard;

import java.util.ArrayList;
import java.util.List;
import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.aincraft.wizard.WizardSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public final class EffectPickerGui implements AlchemicaGui {

    private static final List<PotionEffectType> TYPES;
    static {
        List<PotionEffectType> list = new ArrayList<>();
        for (PotionEffectType t : PotionEffectType.values()) {
            if (t != null) list.add(t);
        }
        TYPES = List.copyOf(list);
    }

    private static final int PAGE_SIZE = 45;

    private final Plugin plugin;
    private final Player player;
    private final WizardSession session;
    private final Runnable onBack;
    private int page = 0;
    private Inventory currentInventory;

    public EffectPickerGui(Plugin plugin, Player player, WizardSession session, Runnable onBack) {
        this.plugin = plugin;
        this.player = player;
        this.session = session;
        this.onBack = onBack;
    }

    public void open() { buildAndOpen(0); }

    private void buildAndOpen(int p) {
        this.page = p;
        Inventory inv = Bukkit.createInventory(this, 54, "Select Effect");
        int start = p * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < TYPES.size(); i++) {
            PotionEffectType type = TYPES.get(start + i);
            inv.setItem(i, GuiUtils.named(Material.POTION,
                "&b" + type.getKey().getKey().replace('_', ' ')));
        }
        if (p > 0) inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Previous"));
        inv.setItem(49, GuiUtils.named(Material.BARRIER, "&cBack"));
        if (start + PAGE_SIZE < TYPES.size())
            inv.setItem(53, GuiUtils.named(Material.ARROW, "&7Next"));
        GuiUtils.fillRow(inv, 5);
        currentInventory = inv;
        player.openInventory(inv);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) { onBack.run(); return; }
        if (slot == 45 && page > 0) { buildAndOpen(page - 1); return; }
        if (slot == 53 && (page + 1) * PAGE_SIZE < TYPES.size()) {
            buildAndOpen(page + 1); return;
        }
        if (slot < 45) {
            int idx = page * PAGE_SIZE + slot;
            if (idx >= TYPES.size()) return;
            PotionEffectType type = TYPES.get(idx);
            // Prompt for amplifier then duration via chat
            player.closeInventory();
            player.sendMessage("[Alchemica] Enter amplifier (0 = level 1). Send blank for default (0).");
            promptAmplifier(type);
        }
    }

    private void promptAmplifier(PotionEffectType type) {
        // Use chat input for amplifier
        plugin.getServer().getPluginManager().registerEvents(
            new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
                    if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                    e.setCancelled(true);
                    org.bukkit.event.HandlerList.unregisterAll(this);
                    String msg = e.getMessage().trim();
                    int amp = 0;
                    if (!msg.isEmpty()) {
                        try { amp = Integer.parseInt(msg); } catch (NumberFormatException ex) {
                            player.sendMessage(ChatColor.RED + "Invalid number, using 0.");
                        }
                    }
                    final int finalAmp = Math.max(0, amp);
                    plugin.getServer().getScheduler().runTask(plugin,
                        () -> promptDuration(type, finalAmp));
                }
            }, plugin);
    }

    private void promptDuration(PotionEffectType type, int amplifier) {
        player.sendMessage("[Alchemica] Enter duration in ticks. Send blank for default (600).");
        plugin.getServer().getPluginManager().registerEvents(
            new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
                    if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                    e.setCancelled(true);
                    org.bukkit.event.HandlerList.unregisterAll(this);
                    String msg = e.getMessage().trim();
                    int dur = 600;
                    if (!msg.isEmpty()) {
                        try { dur = Integer.parseInt(msg); } catch (NumberFormatException ex) {
                            player.sendMessage(ChatColor.RED + "Invalid number, using 600.");
                        }
                    }
                    final int finalDur = Math.max(1, dur);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        session.effects.add(new WizardSession.EffectEntry(type, amplifier, finalDur));
                        onBack.run();
                    });
                }
            }, plugin);
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
```

- [ ] **Step 3: Run tests**

```
./gradlew :common:test
```
Expected: all pass.

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/org/aincraft/gui/wizard/RecipeWizardGui.java \
        common/src/main/java/org/aincraft/gui/wizard/EffectPickerGui.java
git commit -m "feat: implement wizard step 4 (custom properties + effects)"
```

---

## Chunk 5: Wizard Steps 5–6, Browser, Detail, Delete

### Task 15: Wizard Step 5 — Modifiers

**Files:**
- Modify: `common/src/main/java/org/aincraft/gui/wizard/RecipeWizardGui.java`

Fill in `openModifiersStep()`. The wizard needs access to the current list of modifier keys from `general.yml`. Pass a `List<String> modifierKeys` to the constructor.

- [ ] **Step 1: Update `RecipeWizardGui` constructor to accept `List<String> modifierKeys`**

Add field `private final List<String> modifierKeys` and update constructor.

- [ ] **Step 2: Implement `openModifiersStep()`**

```java
private void openModifiersStep() {
    if (modifierKeys.isEmpty()) {
        // Skip to confirm
        openStep(WizardStep.CONFIRM);
        return;
    }
    // Initialise toggle state for any missing keys (default: enabled)
    for (String key : modifierKeys) {
        session.modifierToggles.putIfAbsent(key, true);
    }
    int rows = Math.max(2, (int) Math.ceil((modifierKeys.size() + 9) / 9.0) + 1);
    Inventory inv = Bukkit.createInventory(this, rows * 9, buildTitle(
        session.resultType == RecipeResultType.CUSTOM ? 5 : 4));
    for (int i = 0; i < modifierKeys.size(); i++) {
        String key = modifierKeys.get(i);
        boolean enabled = session.modifierToggles.getOrDefault(key, true);
        Material icon = enabled ? Material.LIME_STAINED_GLASS_PANE
                                : Material.RED_STAINED_GLASS_PANE;
        String status = enabled ? "&aEnabled" : "&cDisabled";
        inv.setItem(i, GuiUtils.named(icon, "&f" + key, status));
    }
    int last = rows * 9;
    inv.setItem(last - 9, GuiUtils.named(Material.ARROW, "&7Back"));
    inv.setItem(last - 1, GuiUtils.named(Material.ARROW, "&aNext"));
    inv.setItem(last - 5, GuiUtils.named(Material.BARRIER, "&cCancel"));
    GuiUtils.fillRow(inv, rows - 1);
    currentInventory = inv;
    player.openInventory(inv);
}
```

Handle clicks:
```java
case MODIFIERS -> handleModifiersClick(slot, event);
```

```java
private void handleModifiersClick(int slot, InventoryClickEvent event) {
    int invSize = currentInventory.getSize();
    int backSlot = invSize - 9;
    int cancelSlot = invSize - 5;
    int nextSlot = invSize - 1;
    if (slot == backSlot) { back(); return; }
    if (slot == cancelSlot) { cancel(); return; }
    if (slot == nextSlot) { openStep(WizardStep.CONFIRM); return; }
    if (slot < modifierKeys.size()) {
        String key = modifierKeys.get(slot);
        boolean current = session.modifierToggles.getOrDefault(key, true);
        session.modifierToggles.put(key, !current);
        openStep(WizardStep.MODIFIERS);
    }
}
```

- [ ] **Step 3: Run tests**

```
./gradlew :common:test
```

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/org/aincraft/gui/wizard/RecipeWizardGui.java
git commit -m "feat: implement wizard step 5 (modifiers)"
```

---

### Task 16: Wizard Step 6 — Confirm & Save

**Files:**
- Modify: `common/src/main/java/org/aincraft/gui/wizard/RecipeWizardGui.java`

`RecipeWizardGui` needs a reference to `RecipeYmlWriter` and `Brew#refresh()`. Pass a `Runnable onRefresh` to the constructor.

- [ ] **Step 1: Implement `openConfirmStep()`**

```java
private void openConfirmStep() {
    Inventory inv = Bukkit.createInventory(this, 54, "Confirm Recipe");
    // Summary items
    inv.setItem(0, GuiUtils.named(Material.PAPER, "&f" + session.key,
        "&7Recipe key"));
    for (int i = 0; i < session.ingredients.size() && i < 9; i++) {
        inv.setItem(9 + i, new ItemStack(session.ingredients.get(i)));
    }
    // Result
    Material resultMat = session.resultType == RecipeResultType.CUSTOM
        ? Material.SPLASH_POTION : Material.POTION;
    String resultLabel = session.resultType == RecipeResultType.CUSTOM
        ? (session.name != null ? "&d" + session.name : "&dCustom Potion")
        : "&b" + (session.potionType != null ? session.potionType.name().toLowerCase() : "?");
    inv.setItem(22, GuiUtils.named(resultMat, resultLabel));
    // Effects (custom)
    if (session.resultType == RecipeResultType.CUSTOM) {
        for (int i = 0; i < session.effects.size() && i < 9; i++) {
            WizardSession.EffectEntry e = session.effects.get(i);
            inv.setItem(27 + i, GuiUtils.named(Material.BREWING_STAND,
                "&e" + e.type.getKey().getKey(),
                "&7Amp: " + e.amplifier + " Dur: " + e.durationTicks + "t"));
        }
    }
    // Modifier summary
    long disabled = session.modifierToggles.values().stream().filter(v -> !v).count();
    inv.setItem(39, GuiUtils.named(Material.COMPARATOR,
        "&7Modifiers",
        "&a" + (session.modifierToggles.size() - disabled) + " enabled",
        "&c" + disabled + " disabled"));

    inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Back"));
    inv.setItem(49, GuiUtils.named(Material.LIME_WOOL, "&aSave Recipe"));
    GuiUtils.fillEmpty(inv);
    currentInventory = inv;
    player.openInventory(inv);
}
```

Handle confirm click:
```java
case CONFIRM -> handleConfirmClick(slot, event);
```

```java
private void handleConfirmClick(int slot, InventoryClickEvent event) {
    if (slot == 45) { back(); return; }
    if (slot == 49) { saveRecipe(); }
}

private void saveRecipe() {
    player.closeInventory();
    try {
        ymlWriter.write(session);
        onRefresh.run();
        sessionManager.remove(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Recipe '" + session.key + "' saved.");
        onCancel.run(); // return to hub
    } catch (java.io.IOException e) {
        player.sendMessage(ChatColor.RED
            + "Failed to save recipe: " + e.getMessage()
            + ". Check console for details.");
        plugin.getLogger().severe("[Alchemica] Failed to save recipe: " + e.getMessage());
    }
}
```

- [ ] **Step 2: Run tests**

```
./gradlew :common:test
```

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/org/aincraft/gui/wizard/RecipeWizardGui.java
git commit -m "feat: implement wizard step 6 (confirm and save)"
```

---

### Task 17: `RecipeBrowserGui`

**Files:**
- Create: `common/src/main/java/org/aincraft/gui/browser/RecipeBrowserGui.java`

Paginated browser reading from the live `RecipeRegistry`. Each recipe slot opens `RecipeDetailGui`.

- [ ] **Step 1: Implement `RecipeBrowserGui`**

```java
package org.aincraft.gui.browser;

import java.util.List;
import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.aincraft.internal.RecipeRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public final class RecipeBrowserGui implements AlchemicaGui {

    private static final int PAGE_SIZE = 45;

    private final Plugin plugin;
    private final Player player;
    private final List<String> recipeKeys;   // ordered list from registry
    private final Runnable onBack;           // returns to hub
    private final java.util.function.Function<String, Void> openDetail;
    private int page = 0;
    private Inventory currentInventory;

    public RecipeBrowserGui(Plugin plugin, Player player, List<String> recipeKeys,
            Runnable onBack, java.util.function.Function<String, Void> openDetail) {
        this.plugin = plugin;
        this.player = player;
        this.recipeKeys = recipeKeys;
        this.onBack = onBack;
        this.openDetail = openDetail;
    }

    public void open() { buildAndOpen(0); }

    private void buildAndOpen(int p) {
        this.page = p;
        Inventory inv = Bukkit.createInventory(this, 54, "Recipes — Page " + (p + 1));
        int start = p * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < recipeKeys.size(); i++) {
            String key = recipeKeys.get(start + i);
            inv.setItem(i, GuiUtils.named(Material.POTION, "&f" + key));
        }
        if (p > 0) inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Previous"));
        inv.setItem(49, GuiUtils.named(Material.BARRIER, "&cBack to Hub"));
        if (start + PAGE_SIZE < recipeKeys.size())
            inv.setItem(53, GuiUtils.named(Material.ARROW, "&7Next"));
        GuiUtils.fillRow(inv, 5);
        currentInventory = inv;
        player.openInventory(inv);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) { onBack.run(); return; }
        if (slot == 45 && page > 0) { buildAndOpen(page - 1); return; }
        if (slot == 53 && (page + 1) * PAGE_SIZE < recipeKeys.size()) {
            buildAndOpen(page + 1); return;
        }
        if (slot < 45) {
            int idx = page * PAGE_SIZE + slot;
            if (idx < recipeKeys.size()) openDetail.apply(recipeKeys.get(idx));
        }
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
```

- [ ] **Step 2: Run tests**

```
./gradlew :common:test
```

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/org/aincraft/gui/browser/RecipeBrowserGui.java
git commit -m "feat: add RecipeBrowserGui"
```

---

### Task 18: `RecipeDetailGui`, `RecipeDeleteConfirmGui`, and final wiring

**Files:**
- Create: `common/src/main/java/org/aincraft/gui/browser/RecipeDetailGui.java`
- Create: `common/src/main/java/org/aincraft/gui/browser/RecipeDeleteConfirmGui.java`
- Modify: `common/src/main/java/org/aincraft/internal/Brew.java`

- [ ] **Step 1: Implement `RecipeDetailGui`**

```java
package org.aincraft.gui.browser;

import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.aincraft.wizard.WizardSession;
import org.aincraft.wizard.WizardSessionFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public final class RecipeDetailGui implements AlchemicaGui {

    private final Plugin plugin;
    private final Player player;
    private final String recipeKey;
    private final WizardSessionFactory sessionFactory;
    private final Runnable onBack;
    private final java.util.function.Consumer<WizardSession> onEdit;
    private final Runnable onDelete;
    private Inventory currentInventory;

    public RecipeDetailGui(Plugin plugin, Player player, String recipeKey,
            WizardSessionFactory sessionFactory, Runnable onBack,
            java.util.function.Consumer<WizardSession> onEdit, Runnable onDelete) {
        this.plugin = plugin;
        this.player = player;
        this.recipeKey = recipeKey;
        this.sessionFactory = sessionFactory;
        this.onBack = onBack;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(this, 54, "Recipe: " + recipeKey);
        inv.setItem(20, GuiUtils.named(Material.WRITABLE_BOOK, "&aEdit",
            "&7Modify this recipe"));
        inv.setItem(24, GuiUtils.named(Material.BARRIER, "&cDelete",
            "&7Permanently remove this recipe"));
        inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Back"));
        GuiUtils.fillEmpty(inv);
        currentInventory = inv;
        player.openInventory(inv);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 45) { onBack.run(); return; }
        if (slot == 20) {
            WizardSession session = sessionFactory.fromYml(recipeKey);
            if (session == null) {
                player.sendMessage(ChatColor.RED
                    + "Could not load recipe '" + recipeKey + "'. Check console.");
                return;
            }
            onEdit.accept(session);
            return;
        }
        if (slot == 24) { onDelete.run(); }
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
```

- [ ] **Step 2: Implement `RecipeDeleteConfirmGui`**

```java
package org.aincraft.gui.browser;

import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.aincraft.io.RecipeYmlWriter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class RecipeDeleteConfirmGui implements AlchemicaGui {

    private final Player player;
    private final String recipeKey;
    private final boolean isCustom;
    private final RecipeYmlWriter writer;
    private final Runnable onRefresh;
    private final Runnable onReturn; // returns to browser
    private Inventory currentInventory;

    public RecipeDeleteConfirmGui(Player player, String recipeKey, boolean isCustom,
            RecipeYmlWriter writer, Runnable onRefresh, Runnable onReturn) {
        this.player = player;
        this.recipeKey = recipeKey;
        this.isCustom = isCustom;
        this.writer = writer;
        this.onRefresh = onRefresh;
        this.onReturn = onReturn;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(this, 27, "Delete '" + recipeKey + "'?");
        inv.setItem(11, GuiUtils.named(Material.LIME_DYE, "&aConfirm Delete",
            "&7This cannot be undone."));
        inv.setItem(15, GuiUtils.named(Material.RED_DYE, "&cCancel"));
        GuiUtils.fillEmpty(inv);
        currentInventory = inv;
        player.openInventory(inv);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 15) { onReturn.run(); return; }
        if (slot == 11) {
            try {
                writer.delete(recipeKey, isCustom);
                onRefresh.run();
                player.sendMessage(ChatColor.GREEN + "Recipe '" + recipeKey + "' deleted.");
            } catch (java.io.IOException e) {
                player.sendMessage(ChatColor.RED + "Failed to delete recipe: " + e.getMessage());
            }
            onReturn.run();
        }
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
```

- [ ] **Step 3: Complete wiring in `Brew.enable()`**

Replace the `null` placeholders for wizard and browser openers. `Brew.enable()` has access to `Internal` (package-private), so it can wire the `Consumer<Player>` lambdas there.

Add three private helper methods to `Brew.java`:

```java
// In Brew.java (package-private, same package as Internal):

private void openHubForPlayer(Player player, Internal internal) {
    RecipeHubGui hub = new RecipeHubGui(player, internal.wizardSessionManager,
        () -> openWizardForPlayer(player, internal),
        () -> openBrowserForPlayer(player, internal));
    player.openInventory(hub.getInventory());
}

private void openWizardForPlayer(Player player, Internal internal) {
    openWizardForPlayer(player, internal, new WizardSession());
}

private void openWizardForPlayer(Player player, Internal internal, WizardSession session) {
    org.bukkit.configuration.file.YamlConfiguration gen =
        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
            internal.recipeYmlWriter.getGeneralFile());
    org.bukkit.configuration.ConfigurationSection modSec =
        gen.getConfigurationSection("modifiers");
    List<String> modKeys = modSec == null ? List.of() : List.copyOf(modSec.getKeys(false));
    org.bukkit.configuration.ConfigurationSection recSec =
        gen.getConfigurationSection("recipes");
    Set<String> existingKeys = recSec == null ? new java.util.HashSet<>()
        : new java.util.HashSet<>(recSec.getKeys(false));
    if (session.originalKey != null) existingKeys.remove(session.originalKey); // allow own key in edit
    new RecipeWizardGui(plugin, player, session,
        internal.wizardSessionManager,
        internal.loreCaptureManager,
        internal.recipeYmlWriter,
        modKeys, existingKeys,
        () -> openHubForPlayer(player, internal)).open();
}

private void openBrowserForPlayer(Player player, Internal internal) {
    new RecipeBrowserGui(plugin, player,
        internal.wizardSessionFactory,
        internal.recipeYmlWriter,
        () -> openHubForPlayer(player, internal),
        editSession -> openWizardForPlayer(player, internal, editSession)).open();
}
```

Then update the `Brew.enable()` wiring to use these helpers:

```java
// Replace the null-placeholder BrewCommand registration in Task 11 with:
BrewCommand brewCommand = new BrewCommand(
    plugin,
    internal.wizardSessionManager,
    internal.loreCaptureManager,
    p -> openWizardForPlayer(p, internal),
    p -> openBrowserForPlayer(p, internal)
);
Bukkit.getPluginCommand("brew").setExecutor(brewCommand);
```

Also add the needed imports to `Brew.java`:
```java
import java.util.List;
import java.util.Set;
import org.aincraft.command.BrewCommand;
import org.aincraft.gui.RecipeHubGui;
import org.aincraft.gui.browser.RecipeBrowserGui;
import org.aincraft.gui.wizard.RecipeWizardGui;
import org.aincraft.wizard.WizardSession;
import org.bukkit.entity.Player;
```

- [ ] **Step 4: Run full test suite**

```
./gradlew :common:test
```
Expected: all pass.

- [ ] **Step 5: Build the shadow jar and smoke test**

```
./gradlew :common:shadowJar
```
Expected: JAR built with no compilation errors. Deploy to test server and verify:
- `/brew` without `alchemica.admin` → denied
- `/brew` with `alchemica.admin` → hub opens
- Create vanilla recipe end-to-end → entry in `general.yml`
- Create custom recipe → entries in both files
- Edit recipe → yml updated, other recipes unchanged
- Delete recipe → entry removed

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/org/aincraft/gui/browser/ \
        common/src/main/java/org/aincraft/internal/Brew.java \
        common/src/main/java/org/aincraft/internal/Internal.java \
        common/src/main/java/org/aincraft/command/BrewCommand.java
git commit -m "feat: add RecipeDetailGui, RecipeDeleteConfirmGui, complete BrewCommand wiring"
```

---

### Task 19: Final cleanup and integration test

- [ ] **Step 1: Run full test suite one last time**

```
./gradlew :common:test
```
Expected: all pass.

- [ ] **Step 2: Manual UAT per spec checklist**

See spec at `docs/superpowers/specs/2026-03-27-admin-recipe-manager-design.md`, section "Manual UAT checklist".

- [ ] **Step 3: Commit any cleanup**

```bash
git add -A
git commit -m "feat: admin recipe manager complete"
```
