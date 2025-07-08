package org.aincraft.internal;

import com.google.common.base.Preconditions;
import org.aincraft.IBrew;
import org.aincraft.IPluginConfiguration;
import org.aincraft.IStorage;
import org.aincraft.command.Reload;
import org.aincraft.providers.VersionProviderFactory;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

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
    Bukkit.getPluginManager().registerEvents(new CauldronListener(this), plugin);
    Bukkit.getPluginCommand("creload").setExecutor(new Reload(this));
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

  static NamespacedKey createKey(String keyString) throws IllegalArgumentException {
    Preconditions.checkArgument(!keyString.isEmpty());
    String[] split = keyString.split(":");
    if (split.length == 1) {
      return NamespacedKey.minecraft(split[0]);
    }
    return new NamespacedKey(split[0], split[1]);
  }
}
