package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.aincraft.container.IDurationStageRegistry;
import org.aincraft.container.IFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

final class DurationStageRegistryFactory implements IFactory<IDurationStageRegistry> {

  private final Plugin plugin;
  private final IYamlConfiguration configuration;

  DurationStageRegistryFactory(Plugin plugin, IYamlConfiguration configuration) {
    this.plugin = plugin;
    this.configuration = configuration;
  }

  @Override
  public IDurationStageRegistry create() {
    Preconditions.checkArgument(configuration.contains("duration-stages"));
    DurationStageRegistry registry = new DurationStageRegistry();
    ConfigurationSection durationSection = configuration.getConfigurationSection("duration-stages");
    for (String durationStageKey : durationSection.getKeys(false)) {
      int ticks = durationSection.getInt(durationStageKey);
      IDurationStage stage = new DurationStage(new NamespacedKey(plugin, durationStageKey), ticks);
      registry.stages.add(stage);
    }
    return registry;
  }

  private record DurationStage(Key key, int ticks) implements IDurationStage {

    @Override
    public int getTicks() {
      return ticks;
    }
  }

  private static final class DurationStageRegistry implements
      IDurationStageRegistry {

    private final List<IDurationStage> stages = new ArrayList<>();

    @Override
    public IDurationStage step(IDurationStage current, int steps) {
      int index = stages.indexOf(current);
      if (index == -1) {
        return current;
      }
      int newIndex = Math.max(0, Math.min(index + steps, stages.size() - 1));
      return stages.get(newIndex);
    }

    @Override
    public IDurationStage get(Key key) {
      return stages.stream().filter(stage -> stage.key().equals(key)).findFirst().orElse(null);
    }

    @Override
    public boolean isRegistered(Key key) {
      return stages.stream().anyMatch(stage -> stage.key().equals(key));
    }

    @NotNull
    @Override
    public Iterator<IDurationStage> iterator() {
      return stages.iterator();
    }
  }
}
