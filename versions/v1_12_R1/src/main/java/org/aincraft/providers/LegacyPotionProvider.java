package org.aincraft.providers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public final class LegacyPotionProvider extends PotionProvider {

  private static final BiMap<PotionEffectType, String> LEGACY_POTION_EFFECT_MAP;
  private static final BiMap<PotionType, String> LEGACY_POTION_MAP;

  static {
    LEGACY_POTION_EFFECT_MAP = HashBiMap.create();
    LEGACY_POTION_EFFECT_MAP.put(PotionEffectType.SLOW, "slowness");
    LEGACY_POTION_EFFECT_MAP.put(PotionEffectType.FAST_DIGGING, "haste");
    LEGACY_POTION_EFFECT_MAP.put(PotionEffectType.SLOW_DIGGING, "mining_fatigue");
    LEGACY_POTION_EFFECT_MAP.put(PotionEffectType.INCREASE_DAMAGE, "strength");
    LEGACY_POTION_EFFECT_MAP.put(PotionEffectType.HEAL, "instant_health");
    LEGACY_POTION_EFFECT_MAP.put(PotionEffectType.HARM, "instant_damage");
    LEGACY_POTION_EFFECT_MAP.put(PotionEffectType.JUMP, "jump_boost");
    LEGACY_POTION_EFFECT_MAP.put(PotionEffectType.CONFUSION, "nausea");
    LEGACY_POTION_EFFECT_MAP.put(PotionEffectType.DAMAGE_RESISTANCE, "resistance");

    LEGACY_POTION_MAP = HashBiMap.create();
    LEGACY_POTION_MAP.put(PotionType.REGEN, "regeneration");
    LEGACY_POTION_MAP.put(PotionType.JUMP, "leaping");
    LEGACY_POTION_MAP.put(PotionType.SPEED, "swiftness");
    LEGACY_POTION_MAP.put(PotionType.INSTANT_HEAL, "healing");
    LEGACY_POTION_MAP.put(PotionType.INSTANT_DAMAGE, "harming");
  }

  @Override
  public Iterable<PotionType> getPotionTypes() {
    List<PotionType> types = new ArrayList<>();
    for (PotionType type : PotionType.values()) {
      if (type != null) {
        types.add(type);
      }
    }
    return types;
  }

  @Override
  public Iterable<PotionEffectType> getPotionEffectTypes() {
    List<PotionEffectType> types = new ArrayList<>();
    for (PotionEffectType type : PotionEffectType.values()) {
      if (type != null) {
        types.add(type);
      }
    }
    return types;
  }

  @Override
  public NamespacedKey getKey(PotionType potionType) {
    String name = LEGACY_POTION_MAP.getOrDefault(potionType,
        potionType.name().toLowerCase(Locale.ENGLISH));
    return NamespacedKey.minecraft(name);
  }

  @Override
  public NamespacedKey getKey(PotionEffectType effectType) {
    String name = LEGACY_POTION_EFFECT_MAP.getOrDefault(effectType,
        effectType.getName().toLowerCase());
    return NamespacedKey.minecraft(name);
  }

  @Override
  public PotionType getType(NamespacedKey key) throws IllegalArgumentException {
    String name = key.getKey().toLowerCase(Locale.ENGLISH);
    PotionType legacyType = LEGACY_POTION_MAP.inverse().get(name);
    if (legacyType != null) {
      return legacyType;
    }
    return PotionType.valueOf(name.toUpperCase(Locale.ENGLISH));
  }

  @Override
  public PotionEffectType getEffectType(NamespacedKey key) {
    String name = key.getKey();
    return LEGACY_POTION_EFFECT_MAP.inverse().getOrDefault(name, PotionEffectType.getByName(name));
  }

  @Override
  public List<PotionEffectType> getEffectTypes(PotionType type) {
    List<PotionEffectType> types = new ArrayList<>();
    types.add(type.getEffectType());
    return types;
  }

  @Override
  public void setBasePotionType(PotionMeta meta, PotionType type) {
    meta.setBasePotionData(new PotionData(type));
  }
}
