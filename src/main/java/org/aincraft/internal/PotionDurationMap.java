package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.Map;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

final class PotionDurationMap implements IPotionDurationMap {

  private final Map<@NotNull PotionEffectType, @NotNull IDurationStage> durationMap;

  PotionDurationMap(Map<PotionEffectType, IDurationStage> durationMap) {
    this.durationMap = durationMap;
  }

  @Override
  public IDurationStage getDuration(PotionEffectType type) throws IllegalArgumentException {
    Preconditions.checkArgument(durationMap.containsKey(type));
    return durationMap.get(type);
  }

  @Override
  public boolean hasDuration(PotionEffectType type) {
    return durationMap.containsKey(type);
  }
}
