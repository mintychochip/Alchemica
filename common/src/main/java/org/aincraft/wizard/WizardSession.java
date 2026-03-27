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
