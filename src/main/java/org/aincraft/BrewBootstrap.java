package org.aincraft;

import org.aincraft.config.PluginConfigurationFactory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BrewBootstrap extends JavaPlugin {

  private static Plugin plugin;
  private static Brew instance;
  @Override
  public void onEnable() {
    plugin = this;
    instance = new Brew(plugin, new PluginConfigurationFactory(plugin).create());
    instance.load();
    instance.enable();
  }
}
