package org.aincraft.container;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public class SimpleRegistry<T> implements IRegistry<T> {

  private final Map<Key, T> map = new HashMap<>();

  public void register(Key key, T object) {
    map.put(key, object);
  }

  @Override
  public T get(Key key) {
    return map.get(key);
  }

  @Override
  public boolean isRegistered(Key key) {
    return map.containsKey(key);
  }

  @NotNull
  @Override
  public Iterator<T> iterator() {
    return map.values().iterator();
  }
}
