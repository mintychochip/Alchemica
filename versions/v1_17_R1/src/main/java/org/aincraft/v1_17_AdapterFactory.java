package org.aincraft;

import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.bukkit.potion.PotionEffectType;

public final class v1_17_AdapterFactory implements IFactory<IVersionAdapter> {

  @Override
  public IVersionAdapter create() {
    return new v1_17_Adapter();
  }

  private static final class v1_17_Adapter implements IVersionAdapter {

    @Override
    public Iterator<PotionEffectType> getEffectTypes() {
      return List.of(PotionEffectType.values()).iterator();
    }

    @Override
    public PotionEffectType getByKey(Key key) {
      String name = key.value();
      if ("slowness".equals(name)) {
        return PotionEffectType.SLOW;
      }
      if ("jump_boost".equals(name)) {
        return PotionEffectType.JUMP;
      }
      if ("instant_health".equals(name)) {
        return PotionEffectType.HEAL;
      }
      if ("instant_damage".equals(name)) {
        return PotionEffectType.HARM;
      }
      return PotionEffectType.getByName(name);
    }
  }
}
