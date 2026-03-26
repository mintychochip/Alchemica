package org.aincraft.internal;

import com.google.common.primitives.UnsignedInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.aincraft.CauldronIngredient;
import org.aincraft.IPotionResult;
import org.aincraft.IPotionResult.Status;
import org.aincraft.container.LocationKey;
import org.aincraft.dao.ICauldron;
import org.aincraft.dao.IDao;
import org.aincraft.dao.IPlayerSettings;
import org.aincraft.providers.ICauldronProvider;
import org.aincraft.providers.IVersionProviders;
import org.aincraft.providers.VersionProviderFactory;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

final class CauldronListener implements Listener {

  private final Brew brew;
  /** True when the server version is 1.9 or newer (dual-wield / EquipmentSlot available). */
  private final boolean hasDualWield;
  /** True when the server version is 1.9 or newer (sendActionBar and new Sound names). */
  private final boolean hasActionBar;

  public CauldronListener(Brew brew) {
    this.brew = brew;
    int[] ver = VersionProviderFactory.getVersion();
    this.hasDualWield = ver[1] >= 9;
    this.hasActionBar = ver[1] >= 9;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPlayerClickCauldron(final PlayerInteractEvent event) {
    if (!isRightClick(event.getAction())) {
      return;
    }
    // EquipmentSlot / getHand() was added in 1.9.  On 1.8 we only have the main hand,
    // so we skip this check entirely — all right-clicks are treated as main-hand.
    if (hasDualWield) {
      try {
        org.bukkit.inventory.EquipmentSlot hand = event.getHand();
        if (hand != org.bukkit.inventory.EquipmentSlot.HAND) {
          return;
        }
      } catch (NoSuchMethodError ignored) {
        // Running on a server build older than 1.9 that somehow has the class but not the method
      }
    }

    ItemStack item = event.getItem();
    Block block = event.getClickedBlock();
    if (block == null || item == null) {
      return;
    }
    Material type = item.getType();
    if (type == Material.AIR || type == Material.BUCKET || type == Material.POTION) {
      return;
    }

    Internal internal = brew.getInternal();
    IVersionProviders version = internal.getVersionProviders();
    ICauldronProvider cauldronProvider = version.getCauldronProvider();

    if (!cauldronProvider.isWaterCauldron(block)) {
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

    // On 1.8 servers, CauldronLevelChangeEvent (BOTTLE_FILL) is not fired when a player
    // right-clicks a cauldron with a glass bottle.  We handle that case here instead.
    if (type == Material.GLASS_BOTTLE) {
      if (!hasDualWield) {
        // 1.8: manually dispense the potion / decrement cauldron water level
        handleBottleFill18(event, block, location, key, cauldron, internal, cauldronProvider);
      }
      // On 1.9+ the CauldronLevelChangeEvent (BOTTLE_FILL) handles this; skip here.
      return;
    }

    if (cauldron.isCompleted()) {
      return;
    }

    Player player = event.getPlayer();
    event.setCancelled(true);
    if (internal.stirrers.contains(type)) {
      try {
        IPlayerSettings settings = getPlayerSettings(player);
        IPotionResult result = internal.getRecipeRegistry().search(settings,
            cauldron.getIngredients());
        boolean success = result.getStatus() == Status.SUCCESS;
        version.getEffectProvider().playStirEffect(block, player, success);
        cauldron.setCachedResult(result);
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
      Set<CauldronIngredient> suggestions = internal.getRecipeRegistry()
          .getSuggestions(cauldron.getIngredients());
      if (!suggestions.isEmpty()) {
        String suggestionList = suggestions.stream()
            .map(i -> i.getItemKey().getKey().replace('_', ' '))
            .collect(Collectors.joining(", "));
        sendSuggestion(player, ChatColor.YELLOW + "Next: " + suggestionList);
      }
    }
  }

  /**
   * Handles the glass-bottle-fill interaction on 1.8 servers, replicating the logic that
   * {@code CauldronLevelChangeEvent (BOTTLE_FILL)} handles on 1.9+ servers.
   */
  private void handleBottleFill18(
      PlayerInteractEvent event,
      Block block,
      Location location,
      LocationKey key,
      ICauldron cauldron,
      Internal internal,
      ICauldronProvider cauldronProvider) {

    // Only act on completed brews; non-brew cauldrons behave normally (vanilla fills bottle)
    if (!cauldron.isCompleted()) {
      return;
    }

    event.setCancelled(true);
    Player player = event.getPlayer();

    // Play fill sound. Try the 1.9+ enum name first; fall back to the 1.8 name.
    Sound fillSound = null;
    try {
      fillSound = Sound.valueOf("ITEM_BOTTLE_FILL");
    } catch (IllegalArgumentException ignored) {
      try {
        fillSound = Sound.valueOf("SPLASH");
      } catch (IllegalArgumentException ignored2) {
        // No matching sound constant found; skip sound
      }
    }
    if (fillSound != null) {
      player.playSound(location, fillSound, 1.0f, 1.0f);
    }

    PlayerInventory inventory = player.getInventory();
    if (player.getGameMode() != GameMode.CREATIVE) {
      ItemStack hand = inventory.getItemInHand();
      if (hand != null && hand.getType() == Material.GLASS_BOTTLE) {
        int amount = hand.getAmount();
        hand.setAmount(amount - 1);
      }
    }

    IPotionResult result = cauldron.getCachedResult();
    if (result == null) {
      try {
        IPlayerSettings playerSettings = getPlayerSettings(player);
        result = internal.recipeRegistry.search(playerSettings, cauldron.getIngredients());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    if (result.getStatus() == Status.SUCCESS) {
      ItemStack stack = result.getStack();
      if (stack != null) {
        HashMap<Integer, ItemStack> cannotFit = inventory.addItem(stack);
        if (!cannotFit.isEmpty()) {
          cannotFit.values()
              .forEach(s -> location.getWorld().dropItemNaturally(player.getLocation(), s));
        }
      }
    }

    int currentLevel = cauldronProvider.getCauldronLevel(block);
    int newLevel = currentLevel - 1;
    cauldronProvider.setCauldronLevel(block, newLevel);
    if (newLevel == 0) {
      internal.cauldronDao.remove(key);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onDestroyCauldron(final BlockBreakEvent event) {
    Block block = event.getBlock();
    removeCauldronIfExists(block.getLocation());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPistonMoveCauldron(final BlockPistonExtendEvent event) {
    List<Block> blocks = event.getBlocks();
    blocks.forEach(block -> {
      this.removeCauldronIfExists(block.getLocation());
    });
  }

  private void removeCauldronIfExists(Location location) {
    Internal internal = brew.getInternal();
    IDao<ICauldron, LocationKey> cauldronDao = internal.cauldronDao;
    LocationKey key = new LocationKey(location);
    if (cauldronDao.has(key)) {
      cauldronDao.remove(key);
    }
  }

  private IPlayerSettings getPlayerSettings(Player player) throws Exception {
    Internal internal = brew.getInternal();
    return internal.playerSettingsDao.get(player.getUniqueId(),
        () -> new PlayerSettings(player.getUniqueId(), UnsignedInteger.fromIntBits(3),
            UnsignedInteger.fromIntBits(3)));
  }

  /**
   * Sends a suggestion message to the player.
   *
   * <p>On 1.9+ (Spigot), uses the action bar for a non-intrusive display.
   * On 1.8, falls back to a chat message since {@code sendActionBar} is not available
   * in the Bukkit 1.8 API (it was a Spigot extension added later).
   */
  private void sendSuggestion(Player player, String message) {
    if (hasActionBar) {
      try {
        player.sendActionBar(message);
        return;
      } catch (NoSuchMethodError ignored) {
        // Guard: if somehow the method is absent at runtime, fall through to chat
      }
    }
    player.sendMessage(message);
  }

  private static boolean isRightClick(Action action) {
    return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
  }
}
