package org.aincraft.internal;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.aincraft.IDurationStage;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class ModifierFactories {

  private ModifierFactories() {
    throw new UnsupportedOperationException("This is an unsupported operation.");
  }

  static final ModifierFactory<Integer, PotionEffectMeta> DURATION = createModifier((meta, steps) -> {
    IDurationStage duration = meta.getDuration();
    int s = steps;
    if (s == 0) {
      return;
    }
    if (s > 0) {
      while (s > 0 && duration.hasNext()) {
        duration = duration.next();
        s--;
      }
    } else {
      while (s < 0 && duration.hasPrevious()) {
        duration = duration.previous();
        s++;
      }
    }
    meta.setDuration(duration);
  });

  static final ModifierFactory<Integer, PotionEffectMeta> AMPLIFIER = createModifier((meta, steps) -> {
    meta.setAmplifier(meta.getAmplifier() + steps);
  });

  static final ModifierFactory<Boolean, PotionEffectMeta> AMBIENT = createModifier(
      PotionEffectMeta::setAmbient);

  static final ModifierFactory<Boolean, PotionEffectMeta> PARTICLES = createModifier(
      PotionEffectMeta::setParticles);

  private static <T> ModifierFactory<T, PotionEffectMeta> createModifier(
      BiConsumer<PotionEffectMeta, T> func) {
    return object -> meta -> {
      func.accept(meta, object);
    };
  }

  @FunctionalInterface
  interface ModifierFactory<T, M> {

    Consumer<M> create(T object);

  }
}
