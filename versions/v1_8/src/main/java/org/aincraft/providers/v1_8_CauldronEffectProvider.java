package org.aincraft.providers;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * CauldronEffectProvider for Minecraft 1.8.
 *
 * <p>The {@link Particle} enum and {@link Player#spawnParticle} were added in 1.9. On 1.8
 * we use {@link org.bukkit.World#playEffect(Location, Effect, int)} for particles instead.
 *
 * <p>The Bukkit {@link Sound} enum was completely renamed between 1.8 and 1.9. This class
 * therefore resolves sound names via {@link Sound#valueOf(String)} at runtime, trying the
 * 1.9+ name first and falling back to the 1.8 name, so that the code compiles cleanly
 * against any API version without hard-coding enum constants that may not exist at compile
 * time.
 *
 * <p>The two abstract methods {@link AbstractCauldronEffectProvider#getAddIngredientParticle()}
 * and {@link AbstractCauldronEffectProvider#getAddIngredientSound()} are only invoked by the
 * base class's {@link #playAddIngredientEffect} implementation, which we override completely.
 * Stub implementations returning {@code null} are provided to satisfy the compiler.
 */
public final class v1_8_CauldronEffectProvider extends AbstractCauldronEffectProvider {

    // -------------------------------------------------------------------------
    // Required stubs — never invoked because we override both public methods
    // -------------------------------------------------------------------------

    @Override
    protected Particle getAddIngredientParticle() {
        return null; // never called; playAddIngredientEffect is fully overridden
    }

    @Override
    protected Sound getAddIngredientSound() {
        return null; // never called; playAddIngredientEffect is fully overridden
    }

    // -------------------------------------------------------------------------
    // 1.8-compatible effect implementations
    // -------------------------------------------------------------------------

    /**
     * Plays a witch-magic particle effect and a splash/potion sound when an ingredient is
     * added to the cauldron.
     *
     * <p>Particle: {@link Effect#WITCH_MAGIC} (ID 2002) — present in Bukkit since 1.7.
     * Sound: attempts {@code ENTITY_WITCH_AMBIENT} (1.9+ name), then falls back to
     * {@code WITCH_IDLE} (1.8 name). Both trigger the witch ambient cackle.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void playAddIngredientEffect(Block block, Player player) {
        Location center = block.getLocation().clone().add(0.5, 1.0, 0.5);
        // WITCH_MAGIC (effect ID 2002) plays the coloured-particle "witch spell" effect.
        // This Effect constant exists in Bukkit 1.7+.
        block.getWorld().playEffect(center, Effect.WITCH_MAGIC, 0);
        Sound splashSound = resolveSound("ENTITY_PLAYER_SPLASH", "SPLASH");
        if (splashSound != null) {
            player.playSound(block.getLocation(), splashSound, 0.6f, 1.2f);
        }
    }

    /**
     * Plays a sound at the player when a brew completes or fails.
     *
     * <p>Success: attempts {@code BLOCK_FIRE_EXTINGUISH} (1.9+ name), then {@code FIZZ}
     * (1.8 name) — the fire-extinguish hiss, a reasonable analogue to the brewing sound.
     * Failure: attempts {@code ENTITY_VILLAGER_NO} (1.9+ name), then {@code VILLAGER_NO}
     * (1.8 name).
     */
    @Override
    public void playStirEffect(Block block, Player player, boolean success) {
        Sound sound;
        if (success) {
            sound = resolveSound("BLOCK_FIRE_EXTINGUISH", "FIZZ");
        } else {
            sound = resolveSound("ENTITY_VILLAGER_NO", "VILLAGER_NO");
        }
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    /**
     * Tries to resolve a {@link Sound} by name, checking {@code modernName} first (1.9+)
     * and falling back to {@code legacyName} (1.8). Returns {@code null} if neither exists.
     */
    private static Sound resolveSound(String modernName, String legacyName) {
        try {
            return Sound.valueOf(modernName);
        } catch (IllegalArgumentException ignored) {
            // Not a 1.9+ server name; try the 1.8 name
        }
        try {
            return Sound.valueOf(legacyName);
        } catch (IllegalArgumentException ignored) {
            // Neither name found; return null to skip sound
        }
        return null;
    }
}
