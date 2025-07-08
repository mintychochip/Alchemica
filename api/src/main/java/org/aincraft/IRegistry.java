package org.aincraft;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;

public interface IRegistry<T extends Keyed> extends Iterable<T> {

  T get(NamespacedKey key);

  boolean isRegistered(NamespacedKey key);
}
