package org.aincraft;

import java.lang.reflect.InvocationTargetException;
import org.aincraft.config.PluginConfigurationFactory;
import org.aincraft.internal.Brew;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BrewBootstrap extends JavaPlugin {

  private static IBrew instance;

  @Override
  public void onEnable() {
    instance = new Brew(this, new PluginConfigurationFactory(this).create(), createAdapter());
    instance.load();
    instance.enable();
  }

  @Override
  public void onDisable() {
    instance.disable();
  }

  @SuppressWarnings("unchecked")
  private IVersionAdapter createAdapter() {
    String version = Bukkit.getBukkitVersion();
    Class<?> factoryClazz;
    try {
      if (version.startsWith("1.21")) {
        factoryClazz = Class.forName("org.aincraft.v1_21_AdapterFactory");
      } else if (version.startsWith("1.17")) {
        factoryClazz = Class.forName("org.aincraft.v1_17_AdapterFactory");
      } else {
        throw new UnsupportedOperationException("unsupported version: " + version);
      }
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
    try {
      IFactory<IVersionAdapter> factory = (IFactory<IVersionAdapter>) factoryClazz.getDeclaredConstructor().newInstance();
      return factory.create();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
