package org.aincraft.providers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public final class LegacyMaterialProvider implements IMaterialProvider {

  private final BiMap<NamespacedKey, LegacyMaterialAdapter> legacyMaterialMapping;

  public LegacyMaterialProvider(IYamlConfiguration legacyConfiguration) {
    ConfigurationSection legacyMappingSection = legacyConfiguration.getConfigurationSection(
        "legacy-mapping");
    this.legacyMaterialMapping = createMaterialMapping(legacyMappingSection);
  }

  @Override
  public NamespacedKey getMaterialKey(ItemStack stack) {
    BiMap<LegacyMaterialAdapter, NamespacedKey> inverse = legacyMaterialMapping.inverse();
    MaterialData materialData = stack.getData();
    Material material = stack.getType();
    LegacyMaterialAdapter legacyAdapter = new LegacyMaterialAdapter(material,
        materialData.getData());
    NamespacedKey key = inverse.get(legacyAdapter);
    return inverse.get(legacyAdapter);
  }

  private static BiMap<NamespacedKey, LegacyMaterialAdapter> createMaterialMapping(
      ConfigurationSection legacyMappingSection) {
    BiMap<NamespacedKey, LegacyMaterialAdapter> materialMapping = HashBiMap.create();
    for (String v1_13materialString
        : legacyMappingSection.getKeys(false)) {
      NamespacedKey v1_13materialKey = NamespacedKey.minecraft(v1_13materialString);
      ConfigurationSection v1_13materialSection = legacyMappingSection.getConfigurationSection(
          v1_13materialString);
      if (!v1_13materialSection.contains("material")) {
        throw new IllegalArgumentException(
            String.format("1.13 mapped material: %s does not have a corresponding 1.12 counterpart",
                v1_13materialString));
      }
      String legacyMaterialString = v1_13materialSection.getString("material");
      Material legacyMaterial = Material.valueOf(legacyMaterialString.toUpperCase());
      byte data = (byte) v1_13materialSection.getInt("data", 0);
      LegacyMaterialAdapter legacyAdapter = new LegacyMaterialAdapter(legacyMaterial, data);
      materialMapping.put(v1_13materialKey, legacyAdapter);
    }
    return materialMapping;
  }
}
