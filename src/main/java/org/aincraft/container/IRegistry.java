package org.aincraft.container;

import net.kyori.adventure.key.Key;

public interface IRegistry<T> extends Iterable<T> {

  T get(Key key);

  boolean isRegistered(Key key);
}
