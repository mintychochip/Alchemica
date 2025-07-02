package org.aincraft;

import java.util.Iterator;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

public final class v1_21_AdapterFactory implements IFactory<IVersionAdapter> {

  @Override
  public IVersionAdapter create() {
    return new v1_21_Adapter();
  }

  private static final class v1_21_Adapter implements IVersionAdapter {

    @Override
    public Iterator<PotionEffectType> getEffectTypes() {
      return Registry.POTION_EFFECT_TYPE.iterator();
    }

    @Override
    public PotionEffectType getByKey(Key key) {
      return Registry.POTION_EFFECT_TYPE.get(key);
    }
  }
}
