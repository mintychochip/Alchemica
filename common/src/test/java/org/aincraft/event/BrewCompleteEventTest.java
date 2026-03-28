package org.aincraft.event;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.aincraft.CauldronIngredient;
import org.aincraft.event.BrewCompleteEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class BrewCompleteEventTest {

  private ServerMock server;
  private PlayerMock player;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    player = server.addPlayer();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private BrewCompleteEvent event(ItemStack initial) {
    return new BrewCompleteEvent(player, List.of(), initial, null);
  }

  @Test
  void getResult_returnsInitialItem() {
    ItemStack item = new ItemStack(Material.POTION);
    assertEquals(item, event(item).getResult());
  }

  @Test
  void setResult_swapsItem() {
    ItemStack original = new ItemStack(Material.POTION);
    ItemStack replacement = new ItemStack(Material.FURNACE);
    BrewCompleteEvent e = event(original);
    e.setResult(replacement);
    assertEquals(replacement, e.getResult());
  }

  @Test
  void setResult_null_givesNull() {
    BrewCompleteEvent e = event(new ItemStack(Material.POTION));
    e.setResult(null);
    assertNull(e.getResult());
  }

  @Test
  void recipeKey_nullForYamlRecipes() {
    assertNull(event(new ItemStack(Material.POTION)).getRecipeKey());
  }

  @Test
  void recipeKey_setForCustomRecipes() {
    NamespacedKey key = NamespacedKey.minecraft("test");
    BrewCompleteEvent e = new BrewCompleteEvent(player, List.of(),
        new ItemStack(Material.POTION), key);
    assertEquals(key, e.getRecipeKey());
  }

  @Test
  void isCancellable() {
    BrewCompleteEvent e = event(new ItemStack(Material.POTION));
    assertFalse(e.isCancelled());
    e.setCancelled(true);
    assertTrue(e.isCancelled());
  }
}
