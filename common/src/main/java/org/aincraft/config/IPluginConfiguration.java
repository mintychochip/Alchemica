package org.aincraft.config;

import org.aincraft.config.IConfiguration.IYamlConfiguration;

public interface IPluginConfiguration {
  IYamlConfiguration getGeneralConfiguration();
  IYamlConfiguration getDatabaseConfiguration();
  void reload();
}
