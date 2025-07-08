package org.aincraft;

import org.aincraft.IConfiguration.IYamlConfiguration;

public interface IPluginConfiguration {
  IYamlConfiguration get(String configurationKey);
  void reload();
}
