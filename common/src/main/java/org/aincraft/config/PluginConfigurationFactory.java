package org.aincraft.config;

import java.util.HashMap;
import java.util.Map;
import org.aincraft.ConfigurationFactory;
import org.aincraft.IConfiguration;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.aincraft.IPluginConfiguration;
import org.bukkit.plugin.Plugin;

public final class PluginConfigurationFactory {

  private final Plugin plugin;

  public PluginConfigurationFactory(Plugin plugin) {
    this.plugin = plugin;
  }

  public IPluginConfiguration create() {
    ConfigurationFactory factory = new ConfigurationFactory(plugin);
    Map<String,IYamlConfiguration> configurations = new HashMap<>();
    configurations.put("general",factory.yaml("general.yml"));
    configurations.put("database",factory.yaml("database.yml"));
    configurations.put("legacy",factory.yaml("legacy.yml"));
    return new PluginConfiguration(configurations);
  }

  private static final class PluginConfiguration implements
      IPluginConfiguration {

    private final Map<String, IYamlConfiguration> configurations;

    private PluginConfiguration(Map<String, IYamlConfiguration> configurations) {
      this.configurations = configurations;
    }

    @Override
    public IYamlConfiguration get(String configurationKey) {
      return configurations.get(configurationKey);
    }

    @Override
    public void reload() {
      configurations.values().forEach(IConfiguration::reload);
    }
  }
}
