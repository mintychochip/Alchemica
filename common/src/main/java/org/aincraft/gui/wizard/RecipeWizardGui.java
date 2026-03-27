package org.aincraft.gui.wizard;

import java.util.List;
import java.util.Set;
import org.aincraft.gui.AlchemicaGui;
import org.aincraft.gui.GuiUtils;
import org.aincraft.io.RecipeYmlWriter;
import org.aincraft.wizard.LoreCaptureManager;
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
    private final LoreCaptureManager loreCaptureManager;
    private final RecipeYmlWriter ymlWriter;
    private final Runnable onRefresh;

    private WizardStep currentStep = WizardStep.KEY;
    private Inventory currentInventory;

    public RecipeWizardGui(Plugin plugin, Player player, WizardSession session,
            WizardSessionManager sessionManager, Runnable onCancel,
            Set<String> existingKeys, LoreCaptureManager loreCaptureManager,
            List<String> modifierKeys, RecipeYmlWriter ymlWriter, Runnable onRefresh) {
        this.plugin = plugin;
        this.player = player;
        this.session = session;
        this.sessionManager = sessionManager;
        this.onCancel = onCancel;
        this.existingKeys = existingKeys;
        this.loreCaptureManager = loreCaptureManager;
        this.modifierKeys = modifierKeys;
        this.ymlWriter = ymlWriter;
        this.onRefresh = onRefresh;
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

    private void openCustomPropertiesStep() {
        Inventory inv = Bukkit.createInventory(this, 54, buildTitle(4));
        String nameLabel = session.name != null
            ? "&aName: " + session.name : "&7Set Name (optional)";
        String colorLabel = session.color != null
            ? "&aColor: #" + session.color : "&7Set Color (optional)";
        String loreLabel = (session.lore != null && !session.lore.isEmpty())
            ? "&aLore: " + session.lore.size() + " line(s)" : "&7Set Lore (optional)";
        inv.setItem(0, GuiUtils.named(Material.NAME_TAG, nameLabel));
        inv.setItem(1, GuiUtils.named(Material.CYAN_DYE, colorLabel));
        inv.setItem(2, GuiUtils.named(Material.WRITABLE_BOOK, loreLabel));
        // Rows 2-4: effects list (slots 9-35)
        for (int i = 0; i < session.effects.size() && i < 27; i++) {
            WizardSession.EffectEntry e = session.effects.get(i);
            inv.setItem(9 + i, GuiUtils.named(Material.BREWING_STAND,
                "&e" + e.type.getKey().getKey(),
                "&7Amplifier: " + e.amplifier,
                "&7Duration: " + formatTicks(e.durationTicks),
                "&cShift-click to remove"));
        }
        // Row 6: controls
        inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Back"));
        inv.setItem(47, GuiUtils.named(Material.LIME_DYE, "&aAdd Effect"));
        inv.setItem(49, GuiUtils.named(Material.BARRIER, "&cCancel"));
        inv.setItem(53, GuiUtils.named(Material.ARROW, "&aNext"));
        GuiUtils.fillRow(inv, 5);
        currentInventory = inv;
        player.openInventory(inv);
    }

    private String formatTicks(int ticks) {
        int seconds = ticks / 20;
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }

    private void openModifiersStep() {
        if (modifierKeys.isEmpty()) {
            // Skip to confirm
            openStep(WizardStep.CONFIRM);
            return;
        }
        // Initialise toggle state for any missing keys (default: enabled)
        for (String key : modifierKeys) {
            session.modifierToggles.putIfAbsent(key, true);
        }
        int rows = Math.max(2, (int) Math.ceil((modifierKeys.size() + 9) / 9.0) + 1);
        Inventory inv = Bukkit.createInventory(this, rows * 9, buildTitle(
            session.resultType == org.aincraft.wizard.RecipeResultType.CUSTOM ? 5 : 4));
        for (int i = 0; i < modifierKeys.size(); i++) {
            String key = modifierKeys.get(i);
            boolean enabled = session.modifierToggles.getOrDefault(key, true);
            Material icon = enabled ? Material.LIME_STAINED_GLASS_PANE
                                    : Material.RED_STAINED_GLASS_PANE;
            String status = enabled ? "&aEnabled" : "&cDisabled";
            inv.setItem(i, GuiUtils.named(icon, "&f" + key, status));
        }
        int last = rows * 9;
        inv.setItem(last - 9, GuiUtils.named(Material.ARROW, "&7Back"));
        inv.setItem(last - 1, GuiUtils.named(Material.ARROW, "&aNext"));
        inv.setItem(last - 5, GuiUtils.named(Material.BARRIER, "&cCancel"));
        GuiUtils.fillRow(inv, rows - 1);
        currentInventory = inv;
        player.openInventory(inv);
    }
    private void openConfirmStep() {
        Inventory inv = Bukkit.createInventory(this, 54, "Confirm Recipe");
        inv.setItem(0, GuiUtils.named(Material.PAPER, "&f" + session.key,
            "&7Recipe key"));
        for (int i = 0; i < session.ingredients.size() && i < 9; i++) {
            inv.setItem(9 + i, new ItemStack(session.ingredients.get(i)));
        }
        // Result
        Material resultMat = session.resultType == org.aincraft.wizard.RecipeResultType.CUSTOM
            ? Material.SPLASH_POTION : Material.POTION;
        String resultLabel = session.resultType == org.aincraft.wizard.RecipeResultType.CUSTOM
            ? (session.name != null ? "&d" + session.name : "&dCustom Potion")
            : "&b" + (session.potionType != null ? session.potionType.name().toLowerCase() : "?");
        inv.setItem(22, GuiUtils.named(resultMat, resultLabel));
        // Effects (custom)
        if (session.resultType == org.aincraft.wizard.RecipeResultType.CUSTOM) {
            for (int i = 0; i < session.effects.size() && i < 9; i++) {
                WizardSession.EffectEntry e = session.effects.get(i);
                inv.setItem(27 + i, GuiUtils.named(Material.BREWING_STAND,
                    "&e" + e.type.getKey().getKey(),
                    "&7Amp: " + e.amplifier + " Dur: " + e.durationTicks + "t"));
            }
        }
        // Modifier summary
        long disabled = session.modifierToggles.values().stream().filter(v -> !v).count();
        inv.setItem(39, GuiUtils.named(Material.COMPARATOR,
            "&7Modifiers",
            "&a" + (session.modifierToggles.size() - disabled) + " enabled",
            "&c" + disabled + " disabled"));

        inv.setItem(45, GuiUtils.named(Material.ARROW, "&7Back"));
        inv.setItem(49, GuiUtils.named(Material.LIME_WOOL, "&aSave Recipe"));
        GuiUtils.fillEmpty(inv);
        currentInventory = inv;
        player.openInventory(inv);
    }

    // ---- Click handling ----

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        switch (currentStep) {
            case KEY -> handleKeyClick(slot, event);
            case INGREDIENTS -> handleIngredientsClick(slot, event);
            case RESULT_TYPE -> handleResultTypeClick(slot, event);
            case CUSTOM_PROPERTIES -> handleCustomPropertiesClick(slot, event);
            case MODIFIERS -> handleModifiersClick(slot, event);
            case CONFIRM -> handleConfirmClick(slot, event);
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

    private void handleCustomPropertiesClick(int slot, InventoryClickEvent event) {
        if (slot == 45) { back(); return; }
        if (slot == 49) { cancel(); return; }
        if (slot == 53) { openStep(WizardStep.MODIFIERS); return; }
        if (slot == 47) {
            new EffectPickerGui(plugin, player, session, () -> openStep(WizardStep.CUSTOM_PROPERTIES))
                .open();
            return;
        }
        if (slot == 0) { openNameInput(); return; }
        if (slot == 1) { openColorInput(); return; }
        if (slot == 2) { startLoreCapture(); return; }
        // Shift-click effect slot to remove
        if (slot >= 9 && slot < 36 && event.isShiftClick()) {
            int idx = slot - 9;
            if (idx < session.effects.size()) {
                session.effects.remove(idx);
                openStep(WizardStep.CUSTOM_PROPERTIES);
            }
        }
    }

    private void handleModifiersClick(int slot, InventoryClickEvent event) {
        int invSize = currentInventory.getSize();
        int backSlot = invSize - 9;
        int cancelSlot = invSize - 5;
        int nextSlot = invSize - 1;
        if (slot == backSlot) { back(); return; }
        if (slot == cancelSlot) { cancel(); return; }
        if (slot == nextSlot) { openStep(WizardStep.CONFIRM); return; }
        if (slot < modifierKeys.size()) {
            String key = modifierKeys.get(slot);
            boolean current = session.modifierToggles.getOrDefault(key, true);
            session.modifierToggles.put(key, !current);
            openStep(WizardStep.MODIFIERS);
        }
    }

    private void handleConfirmClick(int slot, InventoryClickEvent event) {
        if (slot == 45) { back(); return; }
        if (slot == 49) { saveRecipe(); }
    }

    private void saveRecipe() {
        player.closeInventory();
        try {
            ymlWriter.write(session);
            onRefresh.run();
            sessionManager.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Recipe '" + session.key + "' saved.");
            onCancel.run(); // return to hub
        } catch (java.io.IOException e) {
            player.sendMessage(ChatColor.RED
                + "Failed to save recipe: " + e.getMessage()
                + ". Check console for details.");
            plugin.getLogger().severe("[Alchemica] Failed to save recipe: " + e.getMessage());
        }
    }

    private void openNameInput() {
        player.closeInventory();
        player.sendMessage("[Alchemica] Type the potion name (supports &color codes). Send blank to cancel.");
        startChatInput(input -> {
            if (!input.isBlank()) session.name = input;
            openStep(WizardStep.CUSTOM_PROPERTIES);
        });
    }

    private void openColorInput() {
        player.closeInventory();
        player.sendMessage("[Alchemica] Type the color as a 6-char hex (e.g. FF4500). Send blank to cancel.");
        startChatInput(input -> {
            if (!input.isBlank()) {
                if (RecipeKeyValidator.isValidHex(input)) {
                    session.color = input.toUpperCase(java.util.Locale.ENGLISH);
                } else {
                    player.sendMessage(ChatColor.RED + "Invalid hex color. Must be 6 hex chars, no #.");
                }
            }
            openStep(WizardStep.CUSTOM_PROPERTIES);
        });
    }

    private void startLoreCapture() {
        loreCaptureManager.start(player, lines -> {
            session.lore = lines.isEmpty() ? null : lines;
            openStep(WizardStep.CUSTOM_PROPERTIES);
        });
    }

    /**
     * Single-line chat input: captures the first non-empty chat message.
     * Piggy-backs on LoreCaptureManager; player sends blank line to cancel.
     */
    private void startChatInput(java.util.function.Consumer<String> callback) {
        loreCaptureManager.start(player, lines ->
            callback.accept(lines.isEmpty() ? "" : lines.get(0)));
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

    private final List<String> modifierKeys;

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
