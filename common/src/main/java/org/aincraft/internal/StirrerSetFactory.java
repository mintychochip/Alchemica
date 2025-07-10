package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.bukkit.Material;

public final class StirrerSetFactory {

  private final IYamlConfiguration configuration;

  public StirrerSetFactory(IYamlConfiguration configuration) {
    this.configuration = configuration;
  }

  Set<Material> create() throws IllegalArgumentException {
    Preconditions.checkArgument(configuration.contains("stirrers"));
    return configuration.getStringList("stirrers").stream()
        .map(stirrer -> Material.valueOf(stirrer.toUpperCase(
            Locale.ENGLISH))).collect(Collectors.toSet());
  }
}
