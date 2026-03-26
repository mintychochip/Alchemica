package org.aincraft.internal;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.google.common.primitives.UnsignedInteger;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.aincraft.CauldronIngredient;
import org.aincraft.IPotionResult;
import org.aincraft.IPotionResult.Status;
import org.aincraft.dao.IPlayerSettings;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RecipeRegistry}.
 *
 * <p>RecipeRegistry is package-private, so these tests live in the same package.
 * MockBukkit is used to satisfy {@code Bukkit.getPlayer(uuid)} lookups inside
 * {@link RecipeRegistry#search}.
 */
class RecipeRegistryTest {

    // NamespacedKey constants used to identify ingredients across tests
    private static final NamespacedKey KEY_A = new NamespacedKey("test", "ingredient_a");
    private static final NamespacedKey KEY_B = new NamespacedKey("test", "ingredient_b");
    private static final NamespacedKey KEY_C = new NamespacedKey("test", "ingredient_c");
    private static final NamespacedKey KEY_EFFECT = new NamespacedKey("test", "effect_ingredient");
    private static final NamespacedKey KEY_MOD = new NamespacedKey("test", "modifier_ingredient");
    private static final NamespacedKey KEY_UNKNOWN = new NamespacedKey("test", "unknown_ingredient");

    // Permission string granted to the player in all default tests
    private static final String PERM_BASE = "brew.recipe.base";
    private static final String PERM_EFFECT = "brew.effect.one";
    private static final String PERM_MOD = "brew.mod.one";
    private static final String PERM_DENIED = "brew.recipe.denied";

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        // Grant the standard permissions used by base recipe, effect and modifier
        player.addAttachment(MockBukkit.createMockPlugin(), PERM_BASE, true);
        player.addAttachment(MockBukkit.createMockPlugin(), PERM_EFFECT, true);
        player.addAttachment(MockBukkit.createMockPlugin(), PERM_MOD, true);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ------------------------------------------------------------------
    // Helper builders
    // ------------------------------------------------------------------

    /** Creates a {@link CauldronIngredient} with amount=1 for the given key. */
    private static CauldronIngredient ingredient(NamespacedKey key) {
        return new CauldronIngredient(key, 1);
    }

    /**
     * Creates a no-op {@link RecipeRegistry.BaseRecipe} whose consumer sets a
     * {@code potionkey} on the context so that {@code buildResult()} takes the
     * {@code potionkey != null} branch and avoids the legacy {@code getBasePotionData()}
     * path which is unavailable in Paper/MockBukkit 1.21.
     */
    private static RecipeRegistry.BaseRecipe baseRecipe(List<CauldronIngredient> ingredients,
            String permission) {
        NamespacedKey dummyKey = new NamespacedKey("test", "water");
        return new RecipeRegistry.BaseRecipe(ingredients, ctx -> ctx.potionkey = dummyKey, permission);
    }

    /** Creates a no-op {@link RecipeRegistry.RegistryStep}. */
    private static RecipeRegistry.RegistryStep step(CauldronIngredient ingredient, String permission) {
        return new RecipeRegistry.RegistryStep(ingredient, ctx -> {}, permission);
    }

    /**
     * Builds an {@link IPlayerSettings} backed by the given {@link PlayerMock}.
     * {@code effectCount} and {@code modifierCount} are the slot budgets available to this player.
     */
    private static IPlayerSettings settings(PlayerMock player,
            int effectCount, int modifierCount) {
        UUID uuid = player.getUniqueId();
        return new IPlayerSettings() {
            private UnsignedInteger effects = UnsignedInteger.fromIntBits(effectCount);
            private UnsignedInteger mods = UnsignedInteger.fromIntBits(modifierCount);

            @Override public UUID getKey() { return uuid; }
            @Override public OfflinePlayer getPlayer() { return null; }
            @Override public UnsignedInteger getEffectCount() { return effects; }
            @Override public UnsignedInteger getModifierCount() { return mods; }
            @Override public void setEffectCount(UnsignedInteger v) { effects = v; }
            @Override public void setModifierCount(UnsignedInteger v) { mods = v; }
        };
    }

    // ------------------------------------------------------------------
    // getSuggestions() tests
    // ------------------------------------------------------------------

    /**
     * With an empty ingredient list, the registry should return the first
     * ingredient of every registered base recipe as a suggestion.
     */
    @Test
    void getSuggestions_emptyIngredients_returnsFirstStepOfAllRecipes() {
        CauldronIngredient a = ingredient(KEY_A);
        CauldronIngredient b = ingredient(KEY_B);

        RecipeRegistry.BaseRecipe recipeA = baseRecipe(List.of(a), PERM_BASE);
        RecipeRegistry.BaseRecipe recipeB = baseRecipe(List.of(b), PERM_BASE);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(recipeA, recipeB),
                Collections.emptyList(),
                Collections.emptyList()
        );

        Set<CauldronIngredient> suggestions = registry.getSuggestions(Collections.emptyList());

        assertEquals(2, suggestions.size());
        assertTrue(suggestions.stream().anyMatch(i -> i.isSimilar(a)));
        assertTrue(suggestions.stream().anyMatch(i -> i.isSimilar(b)));
    }

    /**
     * With ingredients that partially match a recipe prefix, only the next
     * ingredient for the matching recipes should be suggested.
     */
    @Test
    void getSuggestions_partialPrefix_returnsNextStepsOfMatchingRecipes() {
        CauldronIngredient a = ingredient(KEY_A);
        CauldronIngredient b = ingredient(KEY_B);
        CauldronIngredient c = ingredient(KEY_C);

        // Recipe [A, B] and unrelated recipe [C]
        RecipeRegistry.BaseRecipe recipeAB = baseRecipe(List.of(a, b), PERM_BASE);
        RecipeRegistry.BaseRecipe recipeC  = baseRecipe(List.of(c), PERM_BASE);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(recipeAB, recipeC),
                Collections.emptyList(),
                Collections.emptyList()
        );

        // Current input is [A] — should suggest B (next in [A,B]) but NOT C
        Set<CauldronIngredient> suggestions = registry.getSuggestions(List.of(a));

        assertEquals(1, suggestions.size());
        assertTrue(suggestions.stream().anyMatch(i -> i.isSimilar(b)));
    }

    /**
     * Once a complete base recipe is matched, all effects and modifiers should
     * be offered as suggestions in addition to any next recipe steps.
     */
    @Test
    void getSuggestions_completeBaseMatch_includesEffectsAndModifiers() {
        CauldronIngredient a       = ingredient(KEY_A);
        CauldronIngredient effect  = ingredient(KEY_EFFECT);
        CauldronIngredient mod     = ingredient(KEY_MOD);

        RecipeRegistry.BaseRecipe recipe  = baseRecipe(List.of(a), PERM_BASE);
        RecipeRegistry.RegistryStep eff   = step(effect, PERM_EFFECT);
        RecipeRegistry.RegistryStep modif = step(mod, PERM_MOD);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(recipe),
                List.of(eff),
                List.of(modif)
        );

        // Ingredients exactly match the base recipe [A]
        Set<CauldronIngredient> suggestions = registry.getSuggestions(List.of(a));

        assertTrue(suggestions.stream().anyMatch(i -> i.isSimilar(effect)));
        assertTrue(suggestions.stream().anyMatch(i -> i.isSimilar(mod)));
    }

    // ------------------------------------------------------------------
    // search() — BAD_RECIPE_PATH cases
    // ------------------------------------------------------------------

    /**
     * A completely empty registry should return BAD_RECIPE_PATH for any input.
     */
    @Test
    void search_noMatchingRecipe_returnsBadRecipePath() {
        RecipeRegistry registry = new RecipeRegistry(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        IPotionResult result = registry.search(
                settings(player, 3, 3),
                List.of(ingredient(KEY_A))
        );

        assertEquals(Status.BAD_RECIPE_PATH, result.getStatus());
    }

    /**
     * An ingredient list that does not match any registered recipe prefix
     * should return BAD_RECIPE_PATH.
     */
    @Test
    void search_ingredientsMismatch_returnsBadRecipePath() {
        RecipeRegistry.BaseRecipe recipe = baseRecipe(List.of(ingredient(KEY_A)), PERM_BASE);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(recipe),
                Collections.emptyList(),
                Collections.emptyList()
        );

        // KEY_B does not match the recipe which expects KEY_A
        IPotionResult result = registry.search(
                settings(player, 3, 3),
                List.of(ingredient(KEY_B))
        );

        assertEquals(Status.BAD_RECIPE_PATH, result.getStatus());
    }

    /**
     * After a base recipe is matched, an ingredient that is neither an effect
     * nor a modifier should return BAD_RECIPE_PATH.
     */
    @Test
    void search_unknownIngredientAfterBase_returnsBadRecipePath() {
        CauldronIngredient a = ingredient(KEY_A);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(baseRecipe(List.of(a), PERM_BASE)),
                Collections.emptyList(),
                Collections.emptyList()
        );

        // [A, UNKNOWN] — A matches, UNKNOWN is not an effect or modifier
        IPotionResult result = registry.search(
                settings(player, 3, 3),
                List.of(a, ingredient(KEY_UNKNOWN))
        );

        assertEquals(Status.BAD_RECIPE_PATH, result.getStatus());
    }

    // ------------------------------------------------------------------
    // search() — SUCCESS cases
    // ------------------------------------------------------------------

    /**
     * An ingredient list that exactly matches a base recipe should return SUCCESS.
     */
    @Test
    void search_exactBaseMatch_returnsSuccess() {
        CauldronIngredient a = ingredient(KEY_A);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(baseRecipe(List.of(a), PERM_BASE)),
                Collections.emptyList(),
                Collections.emptyList()
        );

        IPotionResult result = registry.search(
                settings(player, 1, 1),
                List.of(a)
        );

        assertEquals(Status.SUCCESS, result.getStatus());
    }

    /**
     * A base recipe followed by one recognised effect should return SUCCESS.
     */
    @Test
    void search_baseWithEffect_returnsSuccess() {
        CauldronIngredient a      = ingredient(KEY_A);
        CauldronIngredient effect = ingredient(KEY_EFFECT);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(baseRecipe(List.of(a), PERM_BASE)),
                List.of(step(effect, PERM_EFFECT)),
                Collections.emptyList()
        );

        IPotionResult result = registry.search(
                settings(player, 1, 1),
                List.of(a, effect)
        );

        assertEquals(Status.SUCCESS, result.getStatus());
    }

    /**
     * A base recipe followed by one recognised modifier should return SUCCESS.
     */
    @Test
    void search_baseWithModifier_returnsSuccess() {
        CauldronIngredient a   = ingredient(KEY_A);
        CauldronIngredient mod = ingredient(KEY_MOD);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(baseRecipe(List.of(a), PERM_BASE)),
                Collections.emptyList(),
                List.of(step(mod, PERM_MOD))
        );

        IPotionResult result = registry.search(
                settings(player, 1, 1),
                List.of(a, mod)
        );

        assertEquals(Status.SUCCESS, result.getStatus());
    }

    // ------------------------------------------------------------------
    // search() — NO_PERMISSION cases
    // ------------------------------------------------------------------

    /**
     * If the player lacks the permission attached to the matched base recipe,
     * the result should be NO_PERMISSION.
     */
    @Test
    void search_noPermissionForBaseRecipe_returnsNoPermission() {
        CauldronIngredient a = ingredient(KEY_A);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(baseRecipe(List.of(a), PERM_DENIED)),
                Collections.emptyList(),
                Collections.emptyList()
        );

        // player does NOT have PERM_DENIED
        IPotionResult result = registry.search(
                settings(player, 1, 1),
                List.of(a)
        );

        assertEquals(Status.NO_PERMISSION, result.getStatus());
    }

    // ------------------------------------------------------------------
    // search() — MANY_EFFECTS / MANY_MODS cases
    // ------------------------------------------------------------------

    /**
     * If the player's effect slot count would drop below zero, MANY_EFFECTS is returned.
     */
    @Test
    void search_tooManyEffects_returnsManyEffects() {
        CauldronIngredient a      = ingredient(KEY_A);
        CauldronIngredient effect = ingredient(KEY_EFFECT);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(baseRecipe(List.of(a), PERM_BASE)),
                List.of(step(effect, PERM_EFFECT)),
                Collections.emptyList()
        );

        // effectCount = 0, so adding one effect makes it go to -1
        IPotionResult result = registry.search(
                settings(player, 0, 3),
                List.of(a, effect)
        );

        assertEquals(Status.MANY_EFFECTS, result.getStatus());
    }

    /**
     * If the player's modifier slot count would drop below zero, MANY_MODS is returned.
     */
    @Test
    void search_tooManyModifiers_returnsManyMods() {
        CauldronIngredient a   = ingredient(KEY_A);
        CauldronIngredient mod = ingredient(KEY_MOD);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(baseRecipe(List.of(a), PERM_BASE)),
                Collections.emptyList(),
                List.of(step(mod, PERM_MOD))
        );

        // modifierCount = 0, so adding one modifier makes it go to -1
        IPotionResult result = registry.search(
                settings(player, 3, 0),
                List.of(a, mod)
        );

        assertEquals(Status.MANY_MODS, result.getStatus());
    }

    // ------------------------------------------------------------------
    // search() — longest prefix wins
    // ------------------------------------------------------------------

    /**
     * When two recipes share a prefix, the longer matching recipe should win.
     * Recipe [A] and recipe [A, B]: feeding [A, B] must match [A, B] (not [A]).
     */
    @Test
    void search_longestPrefixWins() {
        CauldronIngredient a = ingredient(KEY_A);
        CauldronIngredient b = ingredient(KEY_B);

        // Short recipe permission is denied; long recipe permission is granted.
        // If the short recipe were selected, the result would be NO_PERMISSION.
        // If the long recipe is correctly selected, the result should be SUCCESS.
        RecipeRegistry.BaseRecipe shortRecipe = baseRecipe(List.of(a), PERM_DENIED);
        RecipeRegistry.BaseRecipe longRecipe  = baseRecipe(List.of(a, b), PERM_BASE);

        RecipeRegistry registry = new RecipeRegistry(
                List.of(shortRecipe, longRecipe),
                Collections.emptyList(),
                Collections.emptyList()
        );

        IPotionResult result = registry.search(
                settings(player, 1, 1),
                List.of(a, b)
        );

        // The long recipe [A, B] should have been chosen over short [A]
        assertEquals(Status.SUCCESS, result.getStatus());
    }
}
