package org.aincraft.wizard;

import java.io.File;
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

/**
 * Reconstructs a {@link WizardSession} from persisted YAML files so that
 * an existing recipe can be loaded into the wizard for editing.
 */
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
     * <p>Modifier convention: only <em>disabled</em> modifiers are stored in the session
     * ({@code false}). Enabled modifiers are absent ({@code null}). The Modifiers wizard
     * step seeds all global modifiers to {@code true}, then overrides with the values
     * returned here, so {@code null} is equivalent to enabled.
     *
     * @param key the recipe key to look up
     * @return populated session with {@code editMode=true}, or {@code null} if the
     *         recipe is not found or the YAML cannot be parsed
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

        // Ingredients — stored as material key strings (e.g. "nether_wart")
        for (String mat : recipeSection.getStringList("ingredients")) {
            Material material = Material.matchMaterial(mat.toUpperCase(Locale.ENGLISH));
            if (material != null) {
                session.ingredients.add(material);
            }
        }

        // Determine result type by the presence of a "result" field vs "potion-type"
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

        // Modifier toggles — only disabled modifiers are stored; null == enabled by convention
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
            PotionEffectType type = org.bukkit.Registry.EFFECT.get(
                NamespacedKey.minecraft(typeStr.toLowerCase(Locale.ENGLISH)));
            if (type == null) continue;
            int amplifier = effectMap.containsKey("amplifier")
                ? ((Number) effectMap.get("amplifier")).intValue() : 0;
            int duration = effectMap.containsKey("duration")
                ? ((Number) effectMap.get("duration")).intValue() : 600;
            session.effects.add(new WizardSession.EffectEntry(type, amplifier, duration));
        }
    }
}
