package org.aincraft.internal;

import org.aincraft.IBrew;
import org.aincraft.command.Reload;
import org.aincraft.config.IPluginConfiguration;
import org.aincraft.IRegistry;
import org.aincraft.IStorage;
import org.aincraft.IVersionAdapter;
import org.aincraft.container.RegistrableItem;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class Brew implements IBrew {

  private final Plugin plugin;
  private Internal internal;
  private final IPluginConfiguration pluginConfiguration;
  private final IVersionAdapter adapter;

  public Brew(Plugin plugin, IPluginConfiguration pluginConfiguration, IVersionAdapter adapter) {
    this.plugin = plugin;
    this.pluginConfiguration = pluginConfiguration;
    this.adapter = adapter;

    this.internal = Internal.create(this);
  }

  @Override
  public void load() {
    IRegistry<RegistrableItem> itemRegistry = new SimpleRegistry<>();
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
    Bukkit.getPluginManager().registerEvents(new CauldronListener(this),plugin);
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

  public IVersionAdapter getVersionAdapter() {
    return adapter;
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
}
