package org.aincraft.internal;

import org.aincraft.IDurationStage;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class RawDurationStage implements IDurationStage {

  private static final NamespacedKey KEY = new NamespacedKey("alchemica", "raw");
  private final int ticks;

  RawDurationStage(int ticks) {
    this.ticks = ticks;
  }

  @Override public int getTicks() { return ticks; }
  @Override public NamespacedKey getKey() { return KEY; }
  @Override public boolean hasNext() { return false; }
  @Override public boolean hasPrevious() { return false; }
  @Override public IDurationStage next() { throw new IllegalStateException("RawDurationStage has no next stage"); }
  @Override public IDurationStage previous() { throw new IllegalStateException("RawDurationStage has no previous stage"); }
}
