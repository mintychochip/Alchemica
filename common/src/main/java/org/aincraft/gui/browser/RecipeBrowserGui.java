package org.aincraft.gui.browser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.aincraft.io.RecipeYmlWriter;
import org.aincraft.wizard.WizardSession;
import org.aincraft.wizard.WizardSessionFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public final class RecipeBrowserGui implements AlchemicaGui {

    private static final int PAGE_SIZE = 45;

    private final Plugin plugin;
    private final Player player;
    private final WizardSessionFactory sessionFactory;
    private final RecipeYmlWriter recipeYmlWriter;
    private final Runnable onBack;
    private final java.util.function.Consumer<WizardSession> onEditSession;
    private List<String> recipeKeys;
    private int page = 0;
    private Inventory currentInventory;

    public RecipeBrowserGui(Plugin plugin, Player player,
            WizardSessionFactory sessionFactory,
            RecipeYmlWriter recipeYmlWriter,
            Runnable onBack,
            java.util.function.Consumer<WizardSession> onEditSession) {
        this.plugin = plugin;
        this.player = player;
        this.sessionFactory = sessionFactory;
        this.recipeYmlWriter = recipeYmlWriter;
        this.onBack = onBack;
        this.onEditSession = onEditSession;
        this.recipeKeys = loadKeys();
    }

    private List<String> loadKeys() {
        File f = recipeYmlWriter.getGeneralFile();
        if (!f.exists()) return List.of();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("recipes");
        if (sec == null) return List.of();
        return List.copyOf(sec.getKeys(false));
    }

    public void open() { buildAndOpen(0); }

    private void buildAndOpen(int p) {
        this.page = p;
        Inventory inv = Bukkit.createInventory(this, 54, "Recipes \u2014 Page " + (p + 1));
        int start = p * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < recipeKeys.size(); i++) {
            String key = recipeKeys.get(start + i);
            inv.setItem(i, GuiUtils.named(Material.POTION, "&f" + key));
        }
        if (p > 0) inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Previous"));
        inv.setItem(49, GuiUtils.named(Material.BARRIER, "&cBack to Hub"));
        if (start + PAGE_SIZE < recipeKeys.size())
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
        if (slot == 53 && (page + 1) * PAGE_SIZE < recipeKeys.size()) {
            buildAndOpen(page + 1); return;
        }
        if (slot < 45) {
            int idx = page * PAGE_SIZE + slot;
            if (idx < recipeKeys.size()) {
                String key = recipeKeys.get(idx);
                // Open detail view for this recipe
                new RecipeDetailGui(plugin, player, key, sessionFactory,
                    () -> {
                        // Refresh keys on return from detail
                        this.recipeKeys = loadKeys();
                        buildAndOpen(Math.min(page, Math.max(0,
                            (recipeKeys.size() - 1) / PAGE_SIZE)));
                    },
                    onEditSession,
                    () -> {
                        // Refresh keys after delete
                        this.recipeKeys = loadKeys();
                        buildAndOpen(Math.min(page, Math.max(0,
                            recipeKeys.isEmpty() ? 0 : (recipeKeys.size() - 1) / PAGE_SIZE)));
                    },
                    recipeYmlWriter
                ).open();
            }
        }
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
