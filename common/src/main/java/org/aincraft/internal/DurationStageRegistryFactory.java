package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.aincraft.IDurationStage;
import org.aincraft.IFactory;
import org.aincraft.container.IDurationStageRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
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
    List<IDurationStage> stages = new ArrayList<>();
    ConfigurationSection durationSection = configuration.getConfigurationSection("duration-stages");
    for (String durationStageKey : durationSection.getKeys(false)) {
      int ticks = durationSection.getInt(durationStageKey);
      IDurationStage stage = new DurationStage(new NamespacedKey(plugin, durationStageKey), ticks,
          stages, stages.size());
      stages.add(stage);
    }
    return new DurationStageRegistry(stages);
  }

  private static final class DurationStage implements IDurationStage {

    private final NamespacedKey key;
    private final int ticks;
    private final List<IDurationStage> stages;
    private final int index;

    DurationStage(NamespacedKey key, int ticks, List<IDurationStage> stages, int index) {
      this.key = key;
      this.ticks = ticks;
      this.stages = stages;
      this.index = index;
    }

    @Override
    public int getTicks() {
      return ticks;
    }

    @Override
    public IDurationStage next() throws IllegalStateException {
      Preconditions.checkArgument(hasNext());
      return stages.get(index + 1);
    }

    @Override
    public IDurationStage previous() throws IllegalStateException {
      Preconditions.checkArgument(hasPrevious());
      return stages.get(index - 1);
    }

    @Override
    public boolean hasNext() {
      return index + 1 < stages.size();
    }

    @Override
    public boolean hasPrevious() {
      return index - 1 > 0;
    }

    @Override
    public NamespacedKey getKey() {
      return key;
    }
  }

  private static final class DurationStageRegistry implements
      IDurationStageRegistry {

    private final List<IDurationStage> stages;

    private DurationStageRegistry(List<IDurationStage> stages) {
      this.stages = stages;
    }

    @NotNull
    @Override
    public Iterator<IDurationStage> iterator() {
      return stages.iterator();
    }

    @Override
    public IDurationStage get(NamespacedKey key) {
      return stages.stream().filter(stage -> stage.getKey().equals(key)).findFirst().orElse(null);
    }

    @Override
    public boolean isRegistered(NamespacedKey key) {
      return stages.stream().anyMatch(stage -> stage.getKey().equals(key));
    }
  }
}
