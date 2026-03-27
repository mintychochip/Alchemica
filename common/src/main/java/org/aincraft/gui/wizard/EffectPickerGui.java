package org.aincraft.gui.wizard;

import java.util.ArrayList;
import java.util.List;
import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.aincraft.wizard.WizardSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

/**
 * A paginated inventory GUI that lets the player select a {@link PotionEffectType},
 * then prompts for amplifier and duration via chat before adding the effect to the
 * {@link WizardSession}.
 */
public final class EffectPickerGui implements AlchemicaGui {

    private static final List<PotionEffectType> TYPES;
    static {
        List<PotionEffectType> list = new ArrayList<>();
        for (PotionEffectType t : PotionEffectType.values()) {
            if (t != null) list.add(t);
        }
        TYPES = List.copyOf(list);
    }

    private static final int PAGE_SIZE = 45;

    private final Plugin plugin;
    private final Player player;
    private final WizardSession session;
    private final Runnable onBack;
    private int page = 0;
    private Inventory currentInventory;

    public EffectPickerGui(Plugin plugin, Player player, WizardSession session, Runnable onBack) {
        this.plugin = plugin;
        this.player = player;
        this.session = session;
        this.onBack = onBack;
    }

    public void open() { buildAndOpen(0); }

    private void buildAndOpen(int p) {
        this.page = p;
        Inventory inv = Bukkit.createInventory(this, 54, "Select Effect");
        int start = p * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < TYPES.size(); i++) {
            PotionEffectType type = TYPES.get(start + i);
            inv.setItem(i, GuiUtils.named(Material.POTION,
                "&b" + type.getKey().getKey().replace('_', ' ')));
        }
        if (p > 0) inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Previous"));
        inv.setItem(49, GuiUtils.named(Material.BARRIER, "&cBack"));
        if (start + PAGE_SIZE < TYPES.size())
            inv.setItem(53, GuiUtils.named(Material.ARROW, "&7Next"));
        GuiUtils.fillRow(inv, 5);
        currentInventory = inv;
        player.openInventory(inv);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) { onBack.run(); return; }
        if (slot == 45 && page > 0) { buildAndOpen(page - 1); return; }
        if (slot == 53 && (page + 1) * PAGE_SIZE < TYPES.size()) {
            buildAndOpen(page + 1); return;
        }
        if (slot < 45) {
            int idx = page * PAGE_SIZE + slot;
            if (idx >= TYPES.size()) return;
            PotionEffectType type = TYPES.get(idx);
            // Prompt for amplifier then duration via chat
            player.closeInventory();
            player.sendMessage("[Alchemica] Enter amplifier (0 = level 1). Send blank for default (0).");
            promptAmplifier(type);
        }
    }

    private void promptAmplifier(PotionEffectType type) {
        plugin.getServer().getPluginManager().registerEvents(
            new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
                    if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                    e.setCancelled(true);
                    org.bukkit.event.HandlerList.unregisterAll(this);
                    String msg = e.getMessage().trim();
                    int amp = 0;
                    if (!msg.isEmpty()) {
                        try { amp = Integer.parseInt(msg); } catch (NumberFormatException ex) {
                            player.sendMessage(ChatColor.RED + "Invalid number, using 0.");
                        }
                    }
                    final int finalAmp = Math.max(0, amp);
                    plugin.getServer().getScheduler().runTask(plugin,
                        () -> promptDuration(type, finalAmp));
                }
            }, plugin);
    }

    private void promptDuration(PotionEffectType type, int amplifier) {
        player.sendMessage("[Alchemica] Enter duration in ticks. Send blank for default (600).");
        plugin.getServer().getPluginManager().registerEvents(
            new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
                    if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                    e.setCancelled(true);
                    org.bukkit.event.HandlerList.unregisterAll(this);
                    String msg = e.getMessage().trim();
                    int dur = 600;
                    if (!msg.isEmpty()) {
                        try { dur = Integer.parseInt(msg); } catch (NumberFormatException ex) {
                            player.sendMessage(ChatColor.RED + "Invalid number, using 600.");
                        }
                    }
                    final int finalDur = Math.max(1, dur);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        session.effects.add(new WizardSession.EffectEntry(type, amplifier, finalDur));
                        onBack.run();
                    });
                }
            }, plugin);
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
