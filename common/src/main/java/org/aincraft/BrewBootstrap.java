package org.aincraft;

import java.lang.reflect.InvocationTargetException;
import org.aincraft.config.PluginConfigurationFactory;
import org.aincraft.internal.Brew;
import org.aincraft.providers.VersionProviderFactory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class BrewBootstrap extends JavaPlugin {

  private static IBrew instance;

  @Override
  public void onEnable() {
    PluginConfigurationFactory factory = new PluginConfigurationFactory(this);
    IPluginConfiguration pluginConfiguration = factory.create();
    instance = new Brew(this, pluginConfiguration, getVersionProvider(this,pluginConfiguration));
    instance.load();
    instance.enable();
  }

  @Override
  public void onDisable() {
    instance.disable();
  }

  private static VersionProviderFactory getVersionProvider(Plugin plugin,
      IPluginConfiguration pluginConfiguration) {
    try {
      Class<?> provider = Class.forName("org.aincraft.providers.VersionProviderFactory");
      return (VersionProviderFactory) provider.getDeclaredConstructor(Plugin.class,
          IPluginConfiguration.class).newInstance(plugin, pluginConfiguration);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
}
