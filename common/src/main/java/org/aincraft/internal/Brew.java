package org.aincraft.internal;

import com.google.common.base.Preconditions;
import org.aincraft.IBrew;
import org.aincraft.IPluginConfiguration;
import org.aincraft.IStorage;
import org.aincraft.command.BrewCommand;
import org.aincraft.command.Reload;
import org.aincraft.gui.GuiListener;
import org.aincraft.providers.VersionProviderFactory;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public final class Brew implements IBrew {

  private final Plugin plugin;
  static Internal internal;
  private final IPluginConfiguration pluginConfiguration;
  private final VersionProviderFactory versionProviderFactory;

  public Brew(Plugin plugin,
      IPluginConfiguration pluginConfiguration, VersionProviderFactory versionProviderFactory) {
    this.plugin = plugin;
    this.pluginConfiguration = pluginConfiguration;
    this.versionProviderFactory = versionProviderFactory;
    internal = Internal.create(this);
  }

  public VersionProviderFactory getVersionProviderFactory() {
    return versionProviderFactory;
  }

  @Override
  public void load() {

  }

  public void refresh() {
    IStorage database = internal.getDatabase();
    try {
      database.shutdown();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    pluginConfiguration.reload();
    internal = Internal.create(this);
  }

  @Override
  public void enable() {
    PluginManager pm = Bukkit.getPluginManager();
    pm.registerEvents(new CauldronListener(this), plugin);
    // CauldronLevelChangeEvent was added in Bukkit 1.9.  Only register the listener
    // that depends on it when running on a 1.9+ server.
    int[] ver = VersionProviderFactory.getVersion();
    if (ver[1] >= 9) {
      pm.registerEvents(new CauldronLevelListener(this), plugin);
    }
    Bukkit.getPluginCommand("creload").setExecutor(new Reload(this));

    pm.registerEvents(new GuiListener(), plugin);
    pm.registerEvents(internal.loreCaptureManager, plugin);

    BrewCommand brewCommand = new BrewCommand(
        plugin,
        internal.wizardSessionManager,
        internal.loreCaptureManager,
        p -> openWizardForPlayer(p, internal),
        p -> openBrowserForPlayer(p, internal)
    );
    Bukkit.getPluginCommand("brew").setExecutor(brewCommand);

    pm.registerEvents(new org.bukkit.event.Listener() {
      @org.bukkit.event.EventHandler
      public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        internal.wizardSessionManager.remove(e.getPlayer().getUniqueId());
      }
    }, plugin);
  }

  @Override
  public void disable() {
    IStorage database = internal.getDatabase();
    try {
      database.shutdown();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Plugin getPlugin() {
    return plugin;
  }

  public IPluginConfiguration getPluginConfiguration() {
    return pluginConfiguration;
  }

  Internal getInternal() {
    return internal;
  }

  private void openHubForPlayer(Player player, Internal internal) {
    org.aincraft.gui.RecipeHubGui hub = new org.aincraft.gui.RecipeHubGui(
        player, internal.wizardSessionManager,
        () -> openWizardForPlayer(player, internal),
        () -> openBrowserForPlayer(player, internal));
    player.openInventory(hub.getInventory());
  }

  private void openWizardForPlayer(Player player, Internal internal) {
    openWizardForPlayer(player, internal, new org.aincraft.wizard.WizardSession());
  }

  private void openWizardForPlayer(Player player, Internal internal,
      org.aincraft.wizard.WizardSession session) {
    org.bukkit.configuration.file.YamlConfiguration gen =
        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
            internal.recipeYmlWriter.getGeneralFile());
    org.bukkit.configuration.ConfigurationSection modSec =
        gen.getConfigurationSection("modifiers");
    java.util.List<String> modKeys = modSec == null ? java.util.List.of()
        : java.util.List.copyOf(modSec.getKeys(false));
    org.bukkit.configuration.ConfigurationSection recSec =
        gen.getConfigurationSection("recipes");
    java.util.Set<String> existingKeys = recSec == null ? new java.util.HashSet<>()
        : new java.util.HashSet<>(recSec.getKeys(false));
    if (session.originalKey != null) existingKeys.remove(session.originalKey);
    new org.aincraft.gui.wizard.RecipeWizardGui(plugin, player, session,
        internal.wizardSessionManager,
        () -> openHubForPlayer(player, internal),
        existingKeys,
        internal.loreCaptureManager,
        modKeys,
        internal.recipeYmlWriter,
        this::refresh
    ).open();
  }

  private void openBrowserForPlayer(Player player, Internal internal) {
    new org.aincraft.gui.browser.RecipeBrowserGui(plugin, player,
        internal.wizardSessionFactory,
        internal.recipeYmlWriter,
        () -> openHubForPlayer(player, internal),
        editSession -> openWizardForPlayer(player, internal, editSession)).open();
  }

  static NamespacedKey createKey(String keyString) throws IllegalArgumentException {
    Preconditions.checkArgument(keyString != null && !keyString.isEmpty(), "key string must not be empty");
    String[] split = keyString.split(":");
    Preconditions.checkArgument(split.length <= 2, "invalid key (too many ':' separators): %s", keyString);
    if (split.length == 1) {
      return NamespacedKey.minecraft(split[0]);
    }
    return new NamespacedKey(split[0], split[1]);
  }
}
