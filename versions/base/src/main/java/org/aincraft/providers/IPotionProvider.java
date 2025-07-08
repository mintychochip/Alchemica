package org.aincraft.providers;

import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

public interface IPotionProvider {

  Iterable<PotionType> getPotionTypes();

  Iterable<PotionEffectType> getPotionEffectTypes();

  NamespacedKey getKey(PotionType potionType);

  NamespacedKey getKey(PotionEffectType effectType);

  PotionType getType(NamespacedKey key) throws IllegalArgumentException;

  @Nullable
  PotionEffectType getEffectType(NamespacedKey key) throws IllegalStateException;

  List<PotionEffectType> getEffectTypes(PotionType type);

  void setBasePotionType(PotionMeta meta, PotionType type);

  void setDisplayName(PotionMeta meta, String displayName);

}
