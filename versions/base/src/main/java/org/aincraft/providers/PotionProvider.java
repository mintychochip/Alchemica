package org.aincraft.providers;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class PotionProvider implements IPotionProvider {

  @Override
  public Iterable<PotionType> getPotionTypes() {
    return Registry.POTION;
  }

  @Override
  public Iterable<PotionEffectType> getPotionEffectTypes() {
    return Registry.EFFECT;
  }

  @Override
  public NamespacedKey getKey(PotionType potionType) {
    return potionType.getKey();
  }

  @Override
  public NamespacedKey getKey(PotionEffectType effectType) {
    return effectType.getKey();
  }

  @Override
  public PotionType getType(NamespacedKey key) throws IllegalArgumentException {
    PotionType type = Registry.POTION.get(key);
    if (type == null) {
      throw new IllegalArgumentException("key does not belong to a valid potion type");
    }
    return type;
  }

  @Override
  public PotionEffectType getEffectType(NamespacedKey key) throws IllegalStateException {
    for (PotionEffectType effectType : this.getPotionEffectTypes()) {
      NamespacedKey effectKey = effectType.getKey();
      if (effectKey.equals(key)) {
        return effectType;
      }
    }
    return null;
  }

  @Override
  public List<PotionEffect> getEffects(PotionType type) {
    return type.getPotionEffects();
  }

  @Override
  public void setBasePotionType(PotionMeta meta, PotionType type) {
    meta.setBasePotionType(type);
  }

  @Override
  public void setDisplayName(PotionMeta meta, String displayName) {
    meta.setDisplayName(ChatColor.RESET + displayName);
  }
}
