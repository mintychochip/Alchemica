package org.aincraft.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import net.kyori.adventure.key.Key;
import org.aincraft.CauldronIngredient;
import org.aincraft.IPotionResult;
import org.aincraft.IPotionTrie;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
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
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
    if (action == Action.PHYSICAL || action == Action.LEFT_CLICK_AIR
        || action == Action.LEFT_CLICK_BLOCK) {
      return;
    }
    EquipmentSlot hand = event.getHand();
    if (hand == EquipmentSlot.OFF_HAND) {
      return;
    }
    Block block = event.getClickedBlock();
    if (block == null) {
      return;
    }
    Material material = block.getType();
    if (!(material == Material.WATER_CAULDRON)) {
      return;
    }
    ItemStack item = event.getItem();
    if (item == null) {
      return;
    }
    Material itemMaterial = item.getType();
    if (itemMaterial.isAir() || itemMaterial == Material.BUCKET
        || itemMaterial == Material.GLASS_BOTTLE || itemMaterial == Material.POTION) {
      return;
    }
    Location blockLocation = block.getLocation();
    Cauldron cauldron = null;
    final Internal internal = brew.getInternal();
    final CauldronDao cauldronDao = internal.getCauldronDao();
    final KeyParser keyParser = internal.getKeyParser();
    if (cauldronDao.hasCauldron(blockLocation)) {
      try {
        cauldron = cauldronDao.getCauldron(blockLocation);
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    } else {
      cauldron = Cauldron.create(blockLocation);
      cauldronDao.insertCauldron(cauldron);
    }
    Player player = event.getPlayer();
    if (cauldron.isCompleted()) {
      return;
    }
    if (STIRRER.contains(itemMaterial)) {
      player.playSound(blockLocation, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 1.0f, 1.0f);
      cauldron.setCompleted(true);
      cauldronDao.updateCauldron(cauldron);
    } else {
      int added = player.isSneaking() ? item.getAmount() : 1;
      Key itemKey = keyParser.fromItem(item);
      if (player.getGameMode() != GameMode.CREATIVE) {
        item.setAmount(item.getAmount() - added);
      }
      player.playSound(blockLocation, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.4f);
      cauldron.addIngredient(new CauldronIngredient(itemKey, added));
      cauldronDao.updateCauldron(cauldron);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void cauldronChangeLevel(final CauldronLevelChangeEvent event) {
    ChangeReason reason = event.getReason();
    Block block = event.getBlock();
    Location location = block.getLocation();
    Internal internal = brew.getInternal();
    CauldronDao cauldronDao = internal.getCauldronDao();
    if (cauldronDao.hasCauldron(location) && (reason == ChangeReason.BUCKET_FILL
        || reason == ChangeReason.BOTTLE_EMPTY || reason == ChangeReason.NATURAL_FILL)) {
      cauldronDao.deleteCauldron(location);
      return;
    }
    if (reason != ChangeReason.BOTTLE_FILL) {
      return;
    }
    if (!cauldronDao.hasCauldron(location)) {
      return;
    }
    Entity entity = event.getEntity();
    if (!(entity instanceof Player player)) {
      return;
    }
    event.setCancelled(true);
    player.playSound(location, Sound.ITEM_BOTTLE_FILL, 1.0f, 1.0f);
    PlayerInventory inventory = player.getInventory();
    if (player.getGameMode() != GameMode.CREATIVE) {
      ItemStack hand = inventory.getItem(EquipmentSlot.HAND);
      ItemStack bottle =
          hand.getType() != Material.GLASS_BOTTLE ? inventory.getItem(EquipmentSlot.OFF_HAND)
              : hand;
      int amount = bottle.getAmount();
      bottle.setAmount(amount - 1);
    }
    try {
      Cauldron cauldron = cauldronDao.getCauldron(location);
      if (cauldron.isCompleted()) {
        IPotionTrie potionTrie = internal.getPotionTrie();
        List<CauldronIngredient> ingredients = cauldron.getIngredients();
        Bukkit.broadcastMessage(ingredients.toString());
        IPotionResult result = potionTrie.search(
            new PotionResultBuilderFactory(internal.getPotionDurationMap(),
                brew.getVersionAdapter()).create(), ingredients);
        if (result != null) {
          ItemStack stack = result.stack();
          if (stack != null) {
            inventory.addItem(stack);
          }
        }
      }
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
    BlockData blockData = block.getBlockData();
    if (blockData instanceof Levelled levelled) {
      int old = levelled.getLevel();
      int level = old - 1;
      if (level == 0) {
        block.setType(Material.CAULDRON);
        cauldronDao.deleteCauldron(location);
      } else {
        levelled.setLevel(level);
        block.setBlockData(levelled);
      }
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

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBucketFill(final PlayerBucketFillEvent event) {
    Block block = event.getBlock();
    this.removeCauldronAsync(block.getLocation());
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
}
