package org.aincraft.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.aincraft.CauldronIngredient;
import org.aincraft.IPotionResult;
import org.aincraft.providers.ICauldronProvider;
import org.aincraft.providers.IMaterialProvider;
import org.aincraft.providers.IVersionProviders;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent.ChangeReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

final class CauldronListener implements Listener {

  private static final Set<Material> STIRRER;

  static {
    STIRRER = new HashSet<>();
    STIRRER.add(Material.BLAZE_ROD);
  }

  private final Brew brew;

  public CauldronListener(Brew brew) {
    this.brew = brew;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPlayerClickCauldron(final PlayerInteractEvent event) {
    Action action = event.getAction();
    EquipmentSlot hand = event.getHand();
    Block block = event.getClickedBlock();
    ItemStack item = event.getItem();
    if (!isRightClick(action) || hand != EquipmentSlot.HAND || block == null || item == null) {
      return;
    }
    Internal internal = brew.getInternal();
    final IVersionProviders versionProviders = internal.getVersionProviders();
    final ICauldronProvider cauldronProvider = versionProviders.getCauldronProvider();
    if (!cauldronProvider.isWaterCauldron(block)) {
      return;
    }
    Material itemMaterial = item.getType();
    if (itemMaterial == Material.AIR || itemMaterial == Material.BUCKET
        || itemMaterial == Material.GLASS_BOTTLE || itemMaterial == Material.POTION) {
      return;
    }
    Location location = block.getLocation();
    final CauldronDao cauldronDao = internal.getCauldronDao();
    Cauldron cauldron = null;
    try {
      cauldron = cauldronDao.getCauldron(location, () -> Cauldron.create(location));
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (cauldron == null || cauldron.isCompleted()) {
      return;
    }
    Player player = event.getPlayer();
    event.setCancelled(true);
    if (STIRRER.contains(itemMaterial)) {
      cauldronProvider.playStirEffect(block, player);
      cauldron.setCompleted(true);
      cauldronDao.updateCauldron(cauldron);
    } else {
      IMaterialProvider materialProvider = versionProviders.getMaterialProvider();
      NamespacedKey materialKey = materialProvider.getMaterialKey(item);
      if (materialKey == null) {
        return;
      }
      int added = 1;
      if (player.getGameMode() != GameMode.CREATIVE) {
        item.setAmount(item.getAmount() - added);
      }
      cauldronProvider.playAddIngredientEffect(block, player);
      cauldron.addIngredient(new CauldronIngredient(materialKey, 1));
      cauldronDao.updateCauldron(cauldron);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void cauldronChangeLevel(final CauldronLevelChangeEvent event) {
    Block block = event.getBlock();
    Location location = block.getLocation();

    CauldronDao cauldronDao = brew.getInternal().getCauldronDao();
    ICauldronProvider cauldronProvider = brew.getInternal().getVersionProviders()
        .getCauldronProvider();
    int oldLevel = cauldronProvider.getOldLevel(event);
    int newLevel = cauldronProvider.getNewLevel(event);
    if (cauldronDao.hasCauldron(location) && newLevel > oldLevel) {
      cauldronDao.deleteCauldron(location);
    }

    if (event.getReason() != ChangeReason.BOTTLE_FILL) {
      return;
    }

    if (!cauldronDao.hasCauldron(location)) {
      return;
    }
    Entity entity = event.getEntity();
    if (!(entity instanceof Player)) {
      return;
    }
    Player player = (Player) entity;
    event.setCancelled(true);
    player.playSound(location, Sound.ITEM_BOTTLE_FILL, 1.0f, 1.0f);
    PlayerInventory inventory = player.getInventory();
    if (player.getGameMode() != GameMode.CREATIVE) {
      ItemStack hand = inventory.getItemInMainHand();
      ItemStack bottle =
          hand.getType() != Material.GLASS_BOTTLE ? inventory.getItemInOffHand()
              : hand;
      int amount = bottle.getAmount();
      bottle.setAmount(amount - 1);
    }
    try {
      Cauldron cauldron = cauldronDao.getCauldronIfExists(location);
      if (cauldron.isCompleted()) {
        Trie potionTrie = brew.getInternal().getPotionTrie();
        List<CauldronIngredient> ingredients = cauldron.getIngredients();
        IPotionResult result = potionTrie.search(ingredients);
        if (result != null) {
          ItemStack stack = result.getStack();
          if (stack != null) {
            inventory.addItem(stack);
          }
        }
      }
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
    int old = cauldronProvider.getCauldronLevel(block);
    int level = old - 1;
    cauldronProvider.setCauldronLevel(block, level);
    if (level == 0) {
      cauldronDao.deleteCauldron(location);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onDestroyCauldron(final BlockBreakEvent event) {
    Block block = event.getBlock();
    removeCauldronAsync(block.getLocation());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPistonMoveCauldron(final BlockPistonExtendEvent event) {
    List<Block> blocks = event.getBlocks();
    blocks.forEach(block -> {
      this.removeCauldronAsync(block.getLocation());
    });
  }

  private void removeCauldronAsync(Location location) {
    Internal internal = brew.getInternal();
    CauldronDao cauldronDao = internal.getCauldronDao();
    CompletableFuture.runAsync(() -> {
      if (!cauldronDao.hasCauldron(location)) {
        return;
      }
      cauldronDao.deleteCauldron(location);
    });
  }

  private static boolean isRightClick(Action action) {
    return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
  }

}
