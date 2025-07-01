package org.aincraft;

import org.aincraft.config.IPluginConfiguration;
import org.aincraft.container.IPotionTrie;
import org.aincraft.container.IRegistry;
import org.aincraft.internal.IPotionDurationMap;
import org.aincraft.internal.Internal;
import org.aincraft.internal.PotionResultBuilderFactory;
import org.aincraft.container.SimpleRegistry;
import org.aincraft.listener.CauldronListener;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class Brew {

  private final Plugin plugin;
  private final IPluginConfiguration pluginConfiguration;

  public Brew(Plugin plugin, IPluginConfiguration pluginConfiguration) {
    this.plugin = plugin;
    this.pluginConfiguration = pluginConfiguration;
  }

  void load() {
    IRegistry<ItemStack> itemRegistry = new SimpleRegistry<>();
  }

  void enable() {
    Internal internal = Internal.create(this);
    IPotionDurationMap potionDurationMap = internal.getPotionDurationMap();
    IPotionTrie potionTrie = internal.getPotionTrie();
    CauldronListener listener = new CauldronListener(potionTrie,
        new PotionResultBuilderFactory(internal.getPotionDurationMap()));
    Bukkit.getPluginManager().registerEvents(listener,plugin);
  }

  void disable() {

  }

  public Plugin getPlugin() {
    return plugin;
  }

  public IPluginConfiguration getPluginConfiguration() {
    return pluginConfiguration;
  }
}
