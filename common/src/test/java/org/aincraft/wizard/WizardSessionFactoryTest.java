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
