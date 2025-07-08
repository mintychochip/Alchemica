package org.aincraft.providers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;

public final class PaperPotionProvider extends PotionProvider {

  @Override
  public Iterable<PotionEffectType> getPotionEffectTypes() {
    return Registry.POTION_EFFECT_TYPE;
  }

  @Override
  public void setDisplayName(PotionMeta meta, String displayName) {
    meta.displayName(Component.text(displayName).decoration(TextDecoration.ITALIC, false));
  }
}
