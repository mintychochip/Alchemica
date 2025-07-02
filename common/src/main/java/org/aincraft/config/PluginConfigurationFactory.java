package org.aincraft.config;

import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.aincraft.IFactory;
import org.bukkit.plugin.Plugin;

public final class PluginConfigurationFactory implements IFactory<IPluginConfiguration> {

  private final Plugin plugin;

  public PluginConfigurationFactory(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public IPluginConfiguration create() {
    ConfigurationFactory factory = new ConfigurationFactory(plugin);
    return new PluginConfiguration(factory.yaml("general.yml"), factory.yaml("database.yml"));
  }

  private record PluginConfiguration(IYamlConfiguration general, IYamlConfiguration database) implements
      IPluginConfiguration {

    @Override
    public IYamlConfiguration getGeneralConfiguration() {
      return general;
    }

    @Override
    public IYamlConfiguration getDatabaseConfiguration() {
      return database;
    }

    @Override
    public void reload() {
      general.reload();
      database.reload();
    }
  }
}
