package org.aincraft.gui.wizard;

import java.util.List;
import java.util.Set;
import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.aincraft.wizard.RecipeKeyValidator;
import org.aincraft.wizard.RecipeResultType;
import org.aincraft.wizard.WizardSession;
import org.aincraft.wizard.WizardSessionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Controls all wizard step screens for one player.
 * Call {@link #open()} to start from Step 1 (or from the current session state if editing).
 */
public final class RecipeWizardGui implements AlchemicaGui {

    private final Plugin plugin;
    private final Player player;
    private final WizardSession session;
    private final WizardSessionManager sessionManager;
    private final Runnable onCancel;   // returns player to hub
    /** Keys that already exist in the live registry. Used to reject duplicates in Step 1. */
    private final Set<String> existingKeys;

    private WizardStep currentStep = WizardStep.KEY;
    private Inventory currentInventory;

    public RecipeWizardGui(Plugin plugin, Player player, WizardSession session,
            WizardSessionManager sessionManager, Runnable onCancel,
            Set<String> existingKeys) {
        this.plugin = plugin;
        this.player = player;
        this.session = session;
        this.sessionManager = sessionManager;
        this.onCancel = onCancel;
        this.existingKeys = existingKeys;
        sessionManager.put(player.getUniqueId(), session);
    }

    public void open() {
        openStep(WizardStep.KEY);
    }

    private void openStep(WizardStep step) {
        currentStep = step;
        switch (step) {
            case KEY -> openKeyStep();
            case INGREDIENTS -> openIngredientsStep();
            case RESULT_TYPE -> openResultTypeStep();
            case CUSTOM_PROPERTIES -> openCustomPropertiesStep();
            case MODIFIERS -> openModifiersStep();
            case CONFIRM -> openConfirmStep();
        }
    }

    // ---- Step 1: KEY ----

    private void openKeyStep() {
        var anvil = Bukkit.createInventory(this, org.bukkit.event.inventory.InventoryType.ANVIL,
            buildTitle(1));
        ItemStack paper = GuiUtils.named(Material.PAPER,
            session.key != null ? session.key : "Enter recipe key...");
        anvil.setItem(0, paper);
        currentInventory = anvil;
        player.openInventory(anvil);
    }

    // ---- Step 2: INGREDIENTS ----

    private void openIngredientsStep() {
        Inventory inv = Bukkit.createInventory(this, 27, buildTitle(2));
        // Place existing ingredients in slots 0-17
        for (int i = 0; i < session.ingredients.size() && i < 18; i++) {
            inv.setItem(i, new ItemStack(session.ingredients.get(i)));
        }
        // Bottom row controls
        inv.setItem(18, GuiUtils.named(Material.LIME_DYE, "&aAdd Ingredient"));
        inv.setItem(20, GuiUtils.named(Material.RED_DYE, "&cClear All"));
        ItemStack next = session.ingredients.isEmpty()
            ? GuiUtils.named(Material.GRAY_STAINED_GLASS_PANE, "&7Next (add an ingredient first)")
            : GuiUtils.named(Material.ARROW, "&aNext");
        inv.setItem(26, next);
        inv.setItem(19, GuiUtils.named(Material.ARROW, "&7Back"));
        inv.setItem(25, GuiUtils.named(Material.BARRIER, "&cCancel"));
        GuiUtils.fillEmpty(inv);
        currentInventory = inv;
        player.openInventory(inv);
    }

    // ---- Step 3: RESULT TYPE ----

    private void openResultTypeStep() {
        Inventory inv = Bukkit.createInventory(this, 18, buildTitle(3));
        inv.setItem(2, GuiUtils.named(Material.POTION, "&bVanilla Potion",
            "&7Choose a built-in potion type"));
        inv.setItem(6, GuiUtils.named(Material.CAULDRON, "&dCustom Potion",
            "&7Define your own effects, name, and color"));
        inv.setItem(0, GuiUtils.named(Material.ARROW, "&7Back"));
        inv.setItem(8, GuiUtils.named(Material.BARRIER, "&cCancel"));
        GuiUtils.fillEmpty(inv);
        currentInventory = inv;
        player.openInventory(inv);
    }

    // ---- Steps 4-6 (implemented in Tasks 14-16) ----

    private void openCustomPropertiesStep() { /* Task 14 */ }
    private void openModifiersStep()         { /* Task 15 */ }
    private void openConfirmStep()           { /* Task 16 */ }

    // ---- Click handling ----

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        switch (currentStep) {
            case KEY -> handleKeyClick(slot, event);
            case INGREDIENTS -> handleIngredientsClick(slot, event);
            case RESULT_TYPE -> handleResultTypeClick(slot, event);
            default -> {}
        }
    }

    private void handleKeyClick(int slot, InventoryClickEvent event) {
        // Anvil output slot = 2
        if (slot == 2) {
            ItemStack result = event.getCurrentItem();
            if (result == null || result.getItemMeta() == null) return;
            String raw = result.getItemMeta().getDisplayName();
            String normalized = RecipeKeyValidator.normalize(
                ChatColor.stripColor(raw));
            if (!RecipeKeyValidator.isValid(normalized)) {
                player.sendMessage(ChatColor.RED
                    + "Invalid key. Use only letters, numbers, and hyphens.");
                return;
            }
            // Reject duplicate keys (allow own key in edit mode)
            if (existingKeys.contains(normalized)
                    && !normalized.equals(session.originalKey)) {
                player.sendMessage(ChatColor.RED
                    + "A recipe named '" + normalized + "' already exists.");
                return;
            }
            session.key = normalized;
            openStep(WizardStep.INGREDIENTS);
        }
    }

    private void handleIngredientsClick(int slot, InventoryClickEvent event) {
        if (slot == 19) { back(); return; }
        if (slot == 25) { cancel(); return; }
        if (slot == 18) { openMaterialPicker(); return; }
        if (slot == 20) { session.ingredients.clear(); openStep(WizardStep.INGREDIENTS); return; }
        if (slot == 26 && !session.ingredients.isEmpty()) {
            openStep(WizardStep.RESULT_TYPE); return;
        }
        // Shift-click ingredient slot to remove
        if (slot < 18 && event.isShiftClick()) {
            int idx = slot;
            if (idx < session.ingredients.size()) {
                session.ingredients.remove(idx);
                openStep(WizardStep.INGREDIENTS);
            }
        }
    }

    private void handleResultTypeClick(int slot, InventoryClickEvent event) {
        if (slot == 0) { back(); return; }
        if (slot == 8) { cancel(); return; }
        if (slot == 2) { openVanillaPotionPicker(); return; }
        if (slot == 6) {
            session.resultType = RecipeResultType.CUSTOM;
            openStep(WizardStep.CUSTOM_PROPERTIES);
        }
    }

    private void openMaterialPicker() {
        new MaterialPickerGui(plugin, player, material -> {
            if (!session.ingredients.contains(material)) {
                session.ingredients.add(material);
            }
            openStep(WizardStep.INGREDIENTS);
        }, () -> openStep(WizardStep.INGREDIENTS)).open();
    }

    private void openVanillaPotionPicker() {
        new VanillaPotionPickerGui(plugin, player, potionType -> {
            session.potionType = potionType;
            session.resultType = RecipeResultType.VANILLA;
            openStep(WizardStep.MODIFIERS);
        }, () -> openStep(WizardStep.RESULT_TYPE)).open();
    }

    // modifierKeys field placeholder — needed for back() logic; filled in Task 15
    private final List<String> modifierKeys = java.util.List.of();

    private void back() {
        switch (currentStep) {
            case KEY -> cancel();
            case INGREDIENTS -> openStep(WizardStep.KEY);
            case RESULT_TYPE -> openStep(WizardStep.INGREDIENTS);
            case CUSTOM_PROPERTIES -> openStep(WizardStep.RESULT_TYPE);
            case MODIFIERS -> {
                if (session.resultType == RecipeResultType.CUSTOM) {
                    openStep(WizardStep.CUSTOM_PROPERTIES);
                } else {
                    openStep(WizardStep.RESULT_TYPE);
                }
            }
            case CONFIRM -> {
                // If there are no modifiers, skip past the Modifiers step
                if (!modifierKeys.isEmpty()) {
                    openStep(WizardStep.MODIFIERS);
                } else if (session.resultType == RecipeResultType.CUSTOM) {
                    openStep(WizardStep.CUSTOM_PROPERTIES);
                } else {
                    openStep(WizardStep.RESULT_TYPE);
                }
            }
        }
    }

    private void cancel() {
        sessionManager.remove(player.getUniqueId());
        player.closeInventory();
        onCancel.run();
    }

    private String buildTitle(int stepNum) {
        return "Create Recipe \u2014 Step " + stepNum + " / ?";
    }

    @Override
    public Inventory getInventory() { return currentInventory; }
}
