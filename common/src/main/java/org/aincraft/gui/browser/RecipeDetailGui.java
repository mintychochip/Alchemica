package org.aincraft.gui.browser;

import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.aincraft.io.RecipeYmlWriter;
import org.aincraft.wizard.WizardSession;
import org.aincraft.wizard.WizardSessionFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public final class RecipeDetailGui implements AlchemicaGui {

    private final Plugin plugin;
    private final Player player;
    private final String recipeKey;
    private final WizardSessionFactory sessionFactory;
    private final Runnable onBack;
    private final java.util.function.Consumer<WizardSession> onEdit;
    private final Runnable onDelete;
    private final RecipeYmlWriter writer;
    private Inventory currentInventory;

    public RecipeDetailGui(Plugin plugin, Player player, String recipeKey,
            WizardSessionFactory sessionFactory, Runnable onBack,
            java.util.function.Consumer<WizardSession> onEdit,
            Runnable onDelete, RecipeYmlWriter writer) {
        this.plugin = plugin;
        this.player = player;
        this.recipeKey = recipeKey;
        this.sessionFactory = sessionFactory;
        this.onBack = onBack;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.writer = writer;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(this, 54, "Recipe: " + recipeKey);
        inv.setItem(20, GuiUtils.named(Material.WRITABLE_BOOK, "&aEdit",
            "&7Modify this recipe"));
        inv.setItem(24, GuiUtils.named(Material.BARRIER, "&cDelete",
            "&7Permanently remove this recipe"));
        inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Back"));
        GuiUtils.fillEmpty(inv);
        currentInventory = inv;
        player.openInventory(inv);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 45) { onBack.run(); return; }
        if (slot == 20) {
            WizardSession session = sessionFactory.fromYml(recipeKey);
            if (session == null) {
                player.sendMessage(ChatColor.RED
                    + "Could not load recipe '" + recipeKey + "'. Check console.");
                return;
            }
            onEdit.accept(session);
            return;
        }
        if (slot == 24) {
            boolean isCustom = isCustomRecipe();
            new RecipeDeleteConfirmGui(player, recipeKey, isCustom, writer, () -> {}, onDelete)
                .open();
        }
    }

    private boolean isCustomRecipe() {
        WizardSession session = sessionFactory.fromYml(recipeKey);
        return session != null
            && session.resultType == org.aincraft.wizard.RecipeResultType.CUSTOM;
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
