package org.aincraft.listener;

import org.aincraft.container.IFactory;
import org.aincraft.container.IPotionTrie;
import org.aincraft.ingredient.CauldronIngredient;
import org.aincraft.potion.IPotionResult;
import org.aincraft.potion.IPotionResult.IPotionResultBuilder;
import org.aincraft.potion.IPotionResult.IPotionResultBuilderFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class CauldronListener implements Listener {

  private final IPotionTrie potionTrie;
  private final IFactory<IPotionResultBuilder> factory;

  public CauldronListener(IPotionTrie potionTrie, IFactory<IPotionResultBuilder> factory) {
    this.potionTrie = potionTrie;
    this.factory = factory;
  }

  @EventHandler
  private void onPlayerClickWithMaterial(final PlayerInteractEvent event) {
    IPotionResultBuilder builder = factory.create();
    IPotionResult search = potionTrie.search(builder,
        CauldronIngredient.create(Material.NETHER_WART),
        CauldronIngredient.create(Material.SUGAR),
        CauldronIngredient.create(Material.CAKE),
        CauldronIngredient.create(Material.BLAZE_POWDER),
        CauldronIngredient.create(Material.BLACK_DYE),
        CauldronIngredient.create(Material.REDSTONE_BLOCK),
        CauldronIngredient.create(Material.REDSTONE_BLOCK),
        CauldronIngredient.create(Material.GLOWSTONE),
        CauldronIngredient.create(Material.GLOWSTONE_DUST));
    event.getPlayer().getInventory().addItem(search.stack());
    Bukkit.broadcastMessage(potionTrie.toString());
  }
}
