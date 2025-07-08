package org.aincraft;

import org.bukkit.Keyed;

public interface IDurationStage extends Keyed {

  int getTicks();

  IDurationStage next() throws IllegalStateException;

  IDurationStage previous() throws IllegalStateException;

  boolean hasNext();

  boolean hasPrevious();
}
