package org.aincraft.command;

import java.util.function.Consumer;
import org.aincraft.gui.RecipeHubGui;
import org.aincraft.wizard.LoreCaptureManager;
import org.aincraft.wizard.WizardSessionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles the {@code /brew} command.
 *
 * <p>Requires the {@code alchemica.admin} permission. Opens the {@link RecipeHubGui}
 * for the executing player, discarding any in-progress session beforehand.
 * Blocked while a lore-capture session is active for that player.
 */
public final class BrewCommand implements CommandExecutor {

    private final Plugin plugin;
    private final WizardSessionManager sessionManager;
    private final LoreCaptureManager loreCaptureManager;

    /** Opens the recipe-creation wizard for the given player; null in unit tests. */
    @Nullable private final Consumer<Player> openWizardForPlayer;

    /** Opens the recipe browser for the given player; null in unit tests. */
    @Nullable private final Consumer<Player> openBrowserForPlayer;

    public BrewCommand(Plugin plugin,
            WizardSessionManager sessionManager,
            LoreCaptureManager loreCaptureManager,
            @Nullable Consumer<Player> openWizardForPlayer,
            @Nullable Consumer<Player> openBrowserForPlayer) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.loreCaptureManager = loreCaptureManager;
        this.openWizardForPlayer = openWizardForPlayer;
        this.openBrowserForPlayer = openBrowserForPlayer;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @Nullable Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /brew.");
            return true;
        }
        if (!player.hasPermission("alchemica.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use /brew.");
            return false;
        }
        if (loreCaptureManager.isCapturing(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW
                + "Finish entering lore first (send a blank line to stop).");
            return false;
        }
        // Discard any existing in-progress session so the hub always starts clean
        if (sessionManager.has(player.getUniqueId())) {
            sessionManager.remove(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Previous recipe creation discarded.");
        }

        if (openWizardForPlayer == null || openBrowserForPlayer == null) {
            // Unit-test path — no GUI to open
            return false;
        }

        RecipeHubGui hub = new RecipeHubGui(player, sessionManager,
            () -> openWizardForPlayer.accept(player),
            () -> openBrowserForPlayer.accept(player));
        player.openInventory(hub.getInventory());
        return true;
    }
}
