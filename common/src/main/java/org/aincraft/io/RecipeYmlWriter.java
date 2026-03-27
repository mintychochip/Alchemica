package org.aincraft.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
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
     * 3-write rename sequence:
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

        // Step 3: remove old key from potions.yml (old entry is a safe orphan before this)
        YamlConfiguration pot2 = loadOrEmpty(potionsFile);
        pot2.set(session.originalKey, null);
        saveAtomic(pot2, potionsFile);
    }

    private static void setVanillaRecipe(YamlConfiguration gen, WizardSession s) {
        String path = "recipes." + s.key;
        gen.set(path + ".ingredients", ingredientList(s));
        gen.set(path + ".potion-type", s.potionType.getKey().getKey());
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
        if (s.name != null)  pot.set(p + ".name", s.name);
        if (s.color != null) pot.set(p + ".color", s.color);
        if (s.lore != null && !s.lore.isEmpty()) pot.set(p + ".lore", s.lore);
        List<Map<String, Object>> effectMaps = new ArrayList<>();
        for (WizardSession.EffectEntry e : s.effects) {
            effectMaps.add(Map.of(
                "type",       e.type.getKey().getKey(),
                "duration",   e.durationTicks,
                "amplifier",  e.amplifier
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
     *
     * @param key      the recipe key to remove
     * @param isCustom whether the recipe has a custom potion definition in potions.yml
     * @throws IOException if a file write fails
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
