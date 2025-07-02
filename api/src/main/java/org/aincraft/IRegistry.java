package org.aincraft;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

public interface IRegistry<T extends Keyed> extends Iterable<T> {

  T get(Key key);

  boolean isRegistered(Key key);
}
