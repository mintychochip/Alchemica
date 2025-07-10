package org.aincraft.dao;

import java.sql.ResultSet;
import java.util.function.Function;
import org.aincraft.IStorage;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface IDaoQueryObject<T extends IDaoObject<K>, K> {

  IStorage getStorage();

  String getSelectQuery();

  String getInsertQuery();

  String getDeleteQuery();

  String getUpdateQuery();

  String getExistsQuery();

  Object[] getExistsQueryArguments(K key);

  Object[] insertQueryArguments(T object);

  Object[] getDeleteQueryArguments(K key);

  Object[] getUpdateQueryArguments(T object);

  Object[] getSelectQueryArguments(K key);

  Function<ResultSet, T> getSelectIfExists(K key);
}
