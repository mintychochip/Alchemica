package org.aincraft.dao;

import java.util.concurrent.Callable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public interface IDao<T extends IDaoObject<K>, K> {

  @NotNull
  T get(K key, Callable<? extends T> loader) throws Exception;

  @Nullable
  T getIfExists(@NotNull K key);

  boolean has(@NotNull K key);

  boolean update(@NotNull T object);

  boolean insert(@NotNull T object);

  boolean remove(@NotNull K key);

}
