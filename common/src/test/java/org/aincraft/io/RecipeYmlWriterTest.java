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
        // effects key must always be written (even empty list)
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
