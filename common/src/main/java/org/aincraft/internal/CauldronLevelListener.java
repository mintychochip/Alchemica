package org.aincraft.internal;

import java.util.HashMap;
import org.aincraft.IPotionResult;
import org.aincraft.IPotionResult.Status;
import org.aincraft.container.LocationKey;
import org.aincraft.dao.ICauldron;
import org.aincraft.dao.IDao;
import org.aincraft.dao.IPlayerSettings;
import org.aincraft.providers.ICauldronProvider;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent.ChangeReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.google.common.primitives.UnsignedInteger;

/**
 * Handles {@link CauldronLevelChangeEvent}-based logic for servers running Minecraft 1.9+.
 *
 * <p>This listener is registered only when the server version is 1.9 or newer, because
 * {@code CauldronLevelChangeEvent} was added in Bukkit 1.9. On 1.8 servers, the bottle-fill
 * dispensing logic is handled inside {@link CauldronListener} via
 * {@link org.bukkit.event.player.PlayerInteractEvent} instead.
 */
final class CauldronLevelListener implements Listener {

    private final Brew brew;

    CauldronLevelListener(Brew brew) {
        this.brew = brew;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void checkWaterLevelIsGoingDown(final CauldronLevelChangeEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        LocationKey locationKey = new LocationKey(location);
        Internal internal = brew.getInternal();
        IDao<ICauldron, LocationKey> cauldronDao = internal.cauldronDao;
        ICauldronProvider cauldronProvider = internal.getVersionProviders().getCauldronProvider();
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
                || !(entity instanceof Player)) {
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
            IPotionResult result = cauldron.getCachedResult();
            if (result == null) {
                try {
                    IPlayerSettings playerSettings = getPlayerSettings(player);
                    result = internal.recipeRegistry.search(playerSettings,
                            cauldron.getIngredients());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (result.getStatus() == Status.SUCCESS) {
                ItemStack stack = result.getStack();
                assert stack != null;
                HashMap<Integer, ItemStack> cannotFit = inventory.addItem(stack);
                if (!cannotFit.isEmpty()) {
                    cannotFit.values().forEach(
                            s -> location.getWorld().dropItemNaturally(player.getLocation(), s));
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

    private IPlayerSettings getPlayerSettings(Player player) throws Exception {
        Internal internal = brew.getInternal();
        return internal.playerSettingsDao.get(player.getUniqueId(),
                () -> new PlayerSettings(player.getUniqueId(),
                        UnsignedInteger.fromIntBits(3),
                        UnsignedInteger.fromIntBits(3)));
    }
}
