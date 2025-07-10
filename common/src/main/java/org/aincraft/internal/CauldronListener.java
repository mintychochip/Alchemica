package org.aincraft.internal;

import com.google.common.primitives.UnsignedInteger;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.aincraft.CauldronIngredient;
import org.aincraft.IPotionResult;
import org.aincraft.IPotionResult.Status;
import org.aincraft.container.LocationKey;
import org.aincraft.dao.ICauldron;
import org.aincraft.dao.IDao;
import org.aincraft.dao.IPlayerSettings;
import org.aincraft.providers.ICauldronProvider;
import org.aincraft.providers.IVersionProviders;
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

final class CauldronListener implements Listener {

  private final Brew brew;

  public CauldronListener(Brew brew) {
    this.brew = brew;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPlayerClickCauldron(final PlayerInteractEvent event) {
    ItemStack item = event.getItem();
    Block block = event.getClickedBlock();
    if (!isRightClick(event.getAction()) || event.getHand() != EquipmentSlot.HAND || block == null
        || item == null) {
      return;
    }
    Material type = item.getType();
    if (type == Material.AIR || type == Material.BUCKET || type == Material.GLASS_BOTTLE
        || type == Material.POTION) {
      return;
    }
    Internal internal = brew.getInternal();
    IVersionProviders version = internal.getVersionProviders();
    if (!version.getCauldronProvider().isWaterCauldron(block)) {
      return;
    }
    Location location = block.getLocation();
    LocationKey key = new LocationKey(location);
    ICauldron cauldron;
    try {
      cauldron = internal.cauldronDao.get(key, () -> Cauldron.create(location));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (cauldron.isCompleted()) {
      return;
    }
    Player player = event.getPlayer();
    event.setCancelled(true);
    if (internal.stirrers.contains(type)) {
      try {
        IPlayerSettings settings = getPlayerSettings(player);
        IPotionResult result = internal.getPotionTrie().search(settings, cauldron.getIngredients());
        boolean success = result.getStatus() == Status.SUCCESS;
        version.getEffectProvider().playStirEffect(block, player, success);
        cauldron.setCompleted(success);
        if (success) {
          internal.cauldronDao.update(cauldron);
        } else {
          internal.cauldronDao.remove(key);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      NamespacedKey materialKey = version.getMaterialProvider().getMaterialKey(item);
      if (materialKey == null) {
        return;
      }
      if (player.getGameMode() != GameMode.CREATIVE) {
        item.setAmount(item.getAmount() - 1);
      }
      version.getEffectProvider().playAddIngredientEffect(block, player);
      cauldron.getIngredients().add(new CauldronIngredient(materialKey, 1));
      internal.cauldronDao.update(cauldron);
    }
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  private void checkWaterLevelIsGoingDown(final CauldronLevelChangeEvent event) {
    Block block = event.getBlock();
    Location location = block.getLocation();
    LocationKey locationKey = new LocationKey(location);
    Internal internal = brew.getInternal();
    IDao<ICauldron, LocationKey> cauldronDao = internal.cauldronDao;
    ICauldronProvider cauldronProvider = brew.getInternal().getVersionProviders()
        .getCauldronProvider();
    int oldLevel = cauldronProvider.getOldLevel(event);
    int newLevel = cauldronProvider.getNewLevel(event);
    if (cauldronDao.has(locationKey) && newLevel > oldLevel) {
      cauldronDao.remove(locationKey);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void cauldronChangeLevel(final CauldronLevelChangeEvent event) {
    Block block = event.getBlock();
    Location location = block.getLocation();
    LocationKey locationKey = new LocationKey(location);
    Internal internal = brew.getInternal();
    IDao<ICauldron, LocationKey> cauldronDao = internal.cauldronDao;
    Entity entity = event.getEntity();
    if (!(event.getReason() == ChangeReason.BOTTLE_FILL && cauldronDao.has(locationKey))
        || entity == null) {
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
    ICauldron cauldron = cauldronDao.getIfExists(new LocationKey(location));
    if (cauldron != null && cauldron.isCompleted()) {
      IPotionResult result;
      try {
        IPlayerSettings playerSettings = getPlayerSettings(player);
        result = internal.potionTrie.search(playerSettings, cauldron.getIngredients());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      if (result.getStatus() == Status.SUCCESS) {
        ItemStack stack = result.getStack();
        assert stack != null;
        HashMap<Integer, ItemStack> cannotFit = inventory.addItem(stack);
        if (!cannotFit.isEmpty()) {
          cannotFit.values()
              .forEach(s -> location.getWorld().dropItemNaturally(player.getLocation(), s));
        }
      }
    }
    ICauldronProvider cauldronProvider = internal.getVersionProviders().getCauldronProvider();
    int old = cauldronProvider.getCauldronLevel(block);
    int level = old - 1;
    cauldronProvider.setCauldronLevel(block, level);
    if (level == 0) {
      cauldronDao.remove(new LocationKey(block.getLocation()));
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
    IDao<ICauldron, LocationKey> cauldronDao = internal.cauldronDao;
    CompletableFuture.runAsync(() -> {
      if (!cauldronDao.has(new LocationKey(location))) {
        return;
      }
      cauldronDao.remove(new LocationKey(location));
    });
  }

  private IPlayerSettings getPlayerSettings(Player player) throws Exception {
    Internal internal = brew.getInternal();
    return internal.playerSettingsDao.get(player.getUniqueId(),
        () -> new PlayerSettings(player.getUniqueId(), UnsignedInteger.ONE, UnsignedInteger.ONE));
  }

  private static boolean isRightClick(Action action) {
    return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
  }
}
