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
        return createPotionProvider();
      }

      @Override
      public IMaterialProvider getMaterialProvider() {
        return createMaterialProvider();
      }

      @Override
      public ICauldronProvider getCauldronProvider() {
        return createCauldronProvider();
      }

      @Override
      public ICauldronEffectProvider getEffectProvider() {
        return createEffectProvider();
      }
    };
  }

  private IMaterialProvider createMaterialProvider() {
    Class<?> providerClazz;
    int[] version = getVersion();
    try {
      if (version[1] < 13) {
        providerClazz = Class.forName("org.aincraft.providers.LegacyMaterialProvider");
        return (IMaterialProvider) providerClazz.getDeclaredConstructor(IYamlConfiguration.class)
            .newInstance(pluginConfiguration.get("legacy"));
      } else {
        providerClazz = Class.forName("org.aincraft.providers.MaterialProvider");
        return (IMaterialProvider) providerClazz.getDeclaredConstructor().newInstance();
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

  private IPotionProvider createPotionProvider() {
    Class<?> providerClazz;
    int[] version = getVersion();
    try {
      if (version[1] < 19) {
        providerClazz = Class.forName("org.aincraft.providers.LegacyPotionProvider");
      } else {
        if (isPaperServer()) {
          providerClazz = Class.forName("org.aincraft.providers.PaperPotionProvider");
        } else {
          providerClazz = Class.forName("org.aincraft.providers.PotionProvider");
        }
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    try {
      return (IPotionProvider) providerClazz.getDeclaredConstructor().newInstance();
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

  private ICauldronEffectProvider createEffectProvider() {
    Class<?> providerClazz;
    int[] version = getVersion();
    try {
      if (version[1] < 13) {
        providerClazz = Class.forName("org.aincraft.providers.LegacyCauldronEffectProvider");
      } else if (version[1] < 21) {
        providerClazz = Class.forName("org.aincraft.providers.v1_13_CauldronEffectProvider");
      } else {
        providerClazz = Class.forName("org.aincraft.providers.ModernCauldronEffectProvider");
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    try {
      return (ICauldronEffectProvider) providerClazz.getDeclaredConstructor().newInstance();
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

  private ICauldronProvider createCauldronProvider() {
    Class<?> providerClazz;
    int[] version = getVersion();
    Bukkit.getLogger().info("version: " + version[0] + " " + version[1]);
    try {
      if (version[1] < 13) {
        providerClazz = Class.forName("org.aincraft.providers.LegacyCauldronProvider");
      } else if (version[1] < 17) {
        providerClazz = Class.forName("org.aincraft.providers.v1_13_CauldronProvider");
      } else {
        providerClazz = Class.forName("org.aincraft.providers.ModernCauldronProvider");
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    try {
      return (ICauldronProvider) providerClazz.getDeclaredConstructor().newInstance();
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

  static int[] getVersion() {
    String bukkitVersion = Bukkit.getBukkitVersion();
    String[] split = bukkitVersion.split("-")[0].split("\\.");
    int[] version = new int[2];
    version[0] = Integer.parseInt(split[0]);
    version[1] = Integer.parseInt(split[1]);
    return version;
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
