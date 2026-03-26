package org.aincraft.providers;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/**
 * Potion provider for Bukkit 1.8. {@code PotionData} and
 * {@code PotionMeta#setBasePotionData} were added in 1.9; on 1.8 the
 * equivalent API is {@code PotionMeta#setMainEffect}.
 */
public final class v1_8_PotionProvider extends LegacyPotionProvider {

  @Override
  public void setBasePotionType(PotionMeta meta, PotionType type) {
    PotionEffectType effectType = type.getEffectType();
    if (effectType != null) {
      meta.setMainEffect(effectType);
    }
  }

  @Override
  public List<PotionEffect> getEffects(PotionType type) {
    List<PotionEffect> effects = new ArrayList<>();
    PotionEffectType effectType = type.getEffectType();
    if (effectType != null) {
      // Use 3-arg constructor (type, duration, amplifier) which exists in 1.8
      effects.add(new PotionEffect(effectType, 3600, 0));
    }
    return effects;
  }
}
