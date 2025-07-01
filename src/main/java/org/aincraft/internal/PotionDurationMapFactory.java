package org.aincraft.internal;

import java.util.HashMap;
import java.util.Map;
import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.aincraft.container.IDurationStageRegistry;
import org.aincraft.container.IFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

final class PotionDurationMapFactory implements
    IFactory<IPotionDurationMap> {

  private final Plugin plugin;
  private final IYamlConfiguration configuration;
  private final IDurationStageRegistry durationStages;

  public PotionDurationMapFactory(Plugin plugin, IYamlConfiguration configuration,
      IDurationStageRegistry durationStages) {
    this.plugin = plugin;
    this.configuration = configuration;
    this.durationStages = durationStages;
  }

  @Override
  public IPotionDurationMap create() {
    ConfigurationSection ingredientSection = configuration.getConfigurationSection(
        "potion_ingredients");
    Map<PotionEffectType, IDurationStage> durationMap = new HashMap<>();
    for (PotionEffectType effectType : Registry.POTION_EFFECT_TYPE) {
      NamespacedKey effectTypeKey = effectType.getKey();
      ConfigurationSection effectSection = ingredientSection.getConfigurationSection(
          effectTypeKey.value());
      if (!effectSection.contains("duration")) {
        throw new IllegalArgumentException("effect does not contain duration section");
      }
      String durationKeyString = effectSection.getString("duration").toLowerCase();
      NamespacedKey durationStageKey = new NamespacedKey(plugin, durationKeyString);
      IDurationStage stage = durationStages.get(durationStageKey);
      durationMap.put(effectType, stage);
    }
    return new PotionDurationMap(durationMap);
  }
}
