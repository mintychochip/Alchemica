package org.aincraft;

import java.sql.ResultSet;
import java.util.List;
import java.util.function.Function;

public interface IExecutor {
  boolean executeUpdate(String query, Object ... args);
  <T> T queryRow(Function<ResultSet, T> scanner, String query, Object... args);
  <T> List<T> queryTable(Function<ResultSet, T> scanner, String query, Object... args);
}
