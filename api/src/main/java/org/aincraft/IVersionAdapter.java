package org.aincraft;

import java.util.Iterator;
import net.kyori.adventure.key.Key;
import org.bukkit.potion.PotionEffectType;

public interface IVersionAdapter {
  Iterator<PotionEffectType> getEffectTypes();
  PotionEffectType getByKey(Key key);
}
