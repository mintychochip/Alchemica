package org.aincraft.internal;

import java.util.HashMap;
import java.util.Map;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.aincraft.IDurationStage;
import org.aincraft.IFactory;
import org.aincraft.container.IDurationStageRegistry;
import org.aincraft.providers.IPotionProvider;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

final class PotionDurationMapFactory implements
    IFactory<IPotionDurationMap> {

  private final Plugin plugin;
  private final IYamlConfiguration configuration;
  private final IDurationStageRegistry durationStages;
  private final IPotionProvider potionProvider;
  public PotionDurationMapFactory(Plugin plugin, IYamlConfiguration configuration,
      IDurationStageRegistry durationStages, IPotionProvider potionProvider) {
    this.plugin = plugin;
    this.configuration = configuration;
    this.durationStages = durationStages;
    this.potionProvider = potionProvider;
  }

  @Override
  public IPotionDurationMap create() {
    ConfigurationSection durationSection = configuration.getConfigurationSection(
        "effect_durations");
    Map<PotionEffectType, IDurationStage> durationMap = new HashMap<>();
    for (PotionEffectType effectType : potionProvider.getPotionEffectTypes()) {
      if (effectType == null) {
        continue;
      }
      NamespacedKey key = potionProvider.getKey(effectType);
      String effectString = key.getKey();
      String durationStageString = durationSection.getString(effectString);
      NamespacedKey durationStageKey = new NamespacedKey(plugin, durationStageString);
      IDurationStage stage = durationStages.get(durationStageKey);
      durationMap.put(effectType, stage);
    }
    return new PotionDurationMap(durationMap);
  }
}
