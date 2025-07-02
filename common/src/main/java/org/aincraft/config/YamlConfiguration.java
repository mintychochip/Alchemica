package org.aincraft.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

final class YamlConfiguration implements IYamlConfiguration {

  private final String path;
  private final Plugin plugin;
  private org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
  private File configFile;

  public YamlConfiguration(String path, Plugin plugin) {
    this.path = path;
    this.plugin = plugin;
    this.configFile = new File(plugin.getDataFolder(),path);
    if (!configFile.exists()) {
      plugin.saveResource(path,false);
    }
    assert(configFile != null);
    config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
  }

  @Override
  public void reload() {
    try {
      configFile = new File(plugin.getDataFolder(),path);
      config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
    } catch (NullPointerException | IllegalArgumentException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save() {
    try {
      config.save(configFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean contains(String path) {
    return config.contains(path);
  }

  @Override
  public Set<String> getKeys(boolean deep) {
    return config.getKeys(deep);
  }

  @Override
  public String getString(String path) {
    return config.getString(path);
  }

  @Override
  public String getString(String path, String def) {
    return config.getString(path,def);
  }

  @Override
  public int getInt(String path) {
    return config.getInt(path);
  }

  @Override
  public int getInt(String path, int def) {
    return config.getInt(path,def);
  }

  @Override
  public boolean getBoolean(String path) {
    return config.getBoolean(path);
  }

  @Override
  public boolean getBoolean(String path, boolean def) {
    return config.getBoolean(path,def);
  }

  @Override
  public double getDouble(String path) {
    return config.getDouble(path);
  }

  @Override
  public double getDouble(String path, double def) {
    return config.getDouble(path,def);
  }

  @Override
  public List<String> getStringList(String path) {
    return config.getStringList(path);
  }

  @Override
  public ConfigurationSection getConfigurationSection(String path) {
    return config.getConfigurationSection(path);
  }
}
