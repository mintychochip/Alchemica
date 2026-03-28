package org.aincraft.internal;

import static org.junit.jupiter.api.Assertions.*;
import org.aincraft.CustomRecipe;
import org.aincraft.IBrewAPI;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class BrewAPIImplTest {

  private BrewAPIImpl api;

  @BeforeEach
  void setUp() {
    MockBukkit.mock();
    api = new BrewAPIImpl();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private CustomRecipe recipe(String key) {
    return new CustomRecipe.Builder(NamespacedKey.minecraft(key))
        .ingredients("minecraft:nether_wart")
        .result(ctx -> new ItemStack(Material.NETHER_WART))
        .build();
  }

  @Test
  void registerRecipe_storesRecipe() {
    CustomRecipe r = recipe("test");
    api.registerRecipe(r);
    assertTrue(api.getCustomRecipes().containsKey(NamespacedKey.minecraft("test")));
  }

  @Test
  void registerRecipe_replacesOnDuplicateKey() {
    CustomRecipe r1 = recipe("test");
    CustomRecipe r2 = recipe("test");
    api.registerRecipe(r1);
    api.registerRecipe(r2);
    assertSame(r2, api.getCustomRecipes().get(NamespacedKey.minecraft("test")));
  }

  @Test
  void unregisterRecipe_removesRecipe() {
    api.registerRecipe(recipe("test"));
    api.unregisterRecipe(NamespacedKey.minecraft("test"));
    assertFalse(api.getCustomRecipes().containsKey(NamespacedKey.minecraft("test")));
  }

  @Test
  void unregisterRecipe_unknownKey_doesNotThrow() {
    assertDoesNotThrow(() -> api.unregisterRecipe(NamespacedKey.minecraft("missing")));
  }

  @Test
  void getCustomRecipes_returnsAllRecipes() {
    api.registerRecipe(recipe("a"));
    api.registerRecipe(recipe("b"));
    assertEquals(2, api.getCustomRecipes().size());
  }
}
