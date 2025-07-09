package org.aincraft.providers;

import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public interface IPotionProvider {

  Iterable<PotionType> getPotionTypes();

  Iterable<PotionEffectType> getPotionEffectTypes();

  NamespacedKey getKey(PotionType potionType);

  NamespacedKey getKey(PotionEffectType effectType);

  PotionType getType(NamespacedKey key) throws IllegalArgumentException;

  PotionEffectType getEffectType(NamespacedKey key) throws IllegalArgumentException;

  List<PotionEffect> getEffects(PotionType type);

  void setBasePotionType(PotionMeta meta, PotionType type);

  void setDisplayName(PotionMeta meta, String displayName);

}
