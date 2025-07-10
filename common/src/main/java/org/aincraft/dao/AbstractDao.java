package org.aincraft.dao;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.aincraft.IExecutor;
import org.aincraft.IStorage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
abstract class AbstractDao<T extends IDaoObject<K>, K> implements IDao<T, K> {

  protected abstract IDaoQueryObject<T, K> delegate();

  private final Cache<K, T> daoCache = CacheBuilder.newBuilder().expireAfterWrite(10,
      TimeUnit.MINUTES).build();

  @Override
  public final @NotNull T get(K key, Callable<? extends T> loader) throws Exception {
    if (!has(key)) {
      T object = loader.call();
      insert(object);
      return object;
    }
    T object = getIfExists(key);
    assert object != null;
    return object;
  }

  @Override
  public final @Nullable T getIfExists(@NotNull K key) {
    try {
      return daoCache.get(key, () -> {
        IStorage storage = delegate().getStorage();
        IExecutor executor = storage.getExecutor();
        return executor.queryRow(delegate().getSelectIfExists(key), delegate().getSelectQuery(),
            delegate().getSelectQueryArguments(key));
      });
    } catch (ExecutionException e) {
      return null;
    }
  }

  @Override
  public final boolean insert(@NotNull T object) {
    if (has(object.getKey())) {
      return false;
    }
    IStorage storage = delegate().getStorage();
    IExecutor executor = storage.getExecutor();
    Object[] args = delegate().insertQueryArguments(object);
    boolean result = executor.executeUpdate(delegate().getInsertQuery(),
        args);
    if (result) {
      daoCache.put(object.getKey(), object);
      return true;
    }
    return false;
  }

  @Override
  public boolean has(@NotNull K key) {
    T object = daoCache.getIfPresent(key);
    if (object != null) {
      return true;
    }
    IStorage storage = delegate().getStorage();
    IExecutor executor = storage.getExecutor();
    return executor.queryRow(scanner -> {
      try {
        return scanner.getBoolean(1);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }, delegate().getExistsQuery(), delegate().getExistsQueryArguments(key)) != null;
  }

  @Override
  public boolean remove(@NotNull K key) {
    if (!has(key)) {
      return false;
    }
    IStorage storage = delegate().getStorage();
    IExecutor executor = storage.getExecutor();
    boolean result = executor.executeUpdate(delegate().getDeleteQuery(),
        delegate().getDeleteQueryArguments(key));
    if (result) {
      daoCache.invalidate(key);
      return true;
    }
    return false;
  }

  @Override
  public boolean update(@NotNull T object) {
    IStorage storage = delegate().getStorage();
    IExecutor executor = storage.getExecutor();
    boolean result = executor.executeUpdate(delegate().getUpdateQuery(),
        delegate().getUpdateQueryArguments(object));
    if (result) {
      daoCache.put(object.getKey(), object);
      return true;
    }
    return false;
  }
}
