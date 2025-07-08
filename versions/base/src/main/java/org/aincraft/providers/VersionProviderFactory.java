package org.aincraft.providers;

import java.lang.reflect.InvocationTargetException;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.aincraft.IPluginConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class VersionProviderFactory {

  private final Plugin plugin;
  private final IPluginConfiguration pluginConfiguration;

  public VersionProviderFactory(Plugin plugin, IPluginConfiguration pluginConfiguration) {
    this.plugin = plugin;
    this.pluginConfiguration = pluginConfiguration;
  }

  public IVersionProviders create() {
    return new IVersionProviders() {
      @Override
      public IPotionProvider getPotionProvider() {
        return createProvider(IPotionProvider.class, "org.aincraft.providers.PotionProvider",
            "org.aincraft.providers.PotionProvider", "org.aincraft.providers.LegacyPotionProvider");
      }

      @Override
      public IMaterialProvider getMaterialProvider() {
        return createMaterialProvider();
      }

      @Override
      public ICauldronProvider getCauldronProvider() {
        return createProvider(ICauldronProvider.class, "org.aincraft.providers.CauldronProvider",
            "org.aincraft.providers.CauldronProvider", "org.aincraft.providers.LegacyCauldronProvider");
      }
    };
  }

  private IMaterialProvider createMaterialProvider() {
    Class<?> providerClazz;
    try {
      if (!isLegacyVersion()) {
        providerClazz = Class.forName("org.aincraft.providers.MaterialProvider");
        return (IMaterialProvider) providerClazz.getDeclaredConstructor().newInstance();
      } else {
        providerClazz = Class.forName("org.aincraft.providers.LegacyMaterialProvider");
        return (IMaterialProvider) providerClazz.getDeclaredConstructor(IYamlConfiguration.class)
            .newInstance(pluginConfiguration.get("legacy"));
      }
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

  private <T> T createProvider(Class<T> baseProviderClazz, String paperName, String modernName,
      String legacyName) {
    Class<T> providerClazz;
    try {
      if (isPaperServer()) {
        providerClazz = (Class<T>) Class.forName(paperName);
      } else if (!isLegacyVersion()) {
        providerClazz = (Class<T>) Class.forName(modernName);
      } else {
        providerClazz = (Class<T>) Class.forName(legacyName);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    try {
      return providerClazz.getDeclaredConstructor().newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  static boolean isLegacyVersion() {
    String version = Bukkit.getBukkitVersion();
    String[] parts = version.split("-")[0].split("\\.");
    try {
      int major = Integer.parseInt(parts[0]);
      int minor = Integer.parseInt(parts[1]);
      return major <= 1 && minor < 13;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  static boolean isPaperServer() {
    try {
      // Paper 1.19+ has this class
      Class.forName("io.papermc.paper.configuration.Configuration");
      return true;
    } catch (ClassNotFoundException e1) {
      try {
        // Paper 1.8 to 1.18 had this class
        Class.forName("com.destroystokyo.paper.PaperConfig");
        return true;
      } catch (ClassNotFoundException e2) {
        return false;
      }
    }
  }
}
