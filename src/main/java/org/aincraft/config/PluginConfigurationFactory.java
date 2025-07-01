package org.aincraft.config;

import java.util.Map;
import java.util.Map.Entry;
import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.aincraft.container.IFactory;
import org.bukkit.plugin.Plugin;

public final class PluginConfigurationFactory implements IFactory<IPluginConfiguration> {

  private final Plugin plugin;

  public PluginConfigurationFactory(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public IPluginConfiguration create() {
    ConfigurationFactory factory = new ConfigurationFactory(plugin);
    return new PluginConfiguration(factory.yaml("general.yml"));
  }

  private record PluginConfiguration(IYamlConfiguration general) implements
      IPluginConfiguration {

    @Override
    public IYamlConfiguration getGeneralConfiguration() {
      return general;
    }
  }
}
