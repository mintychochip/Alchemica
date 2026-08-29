package org.aincraft.storage;

import java.sql.ResultSet;
import java.util.List;
import java.util.function.Function;
import org.aincraft.IExecutor;
import org.aincraft.db.sql.SqlDatabase;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.Update;
final class JdbiExecutor implements IExecutor {

  private final SqlDatabase database;

  JdbiExecutor(SqlDatabase database) {
    this.database = database;
  }

  @Override
  public boolean executeUpdate(String sql, Object... args) {
    if (database.closed()) {
      return false;
    }
    return database.jdbi().withHandle(handle -> {
      Update update = handle.createUpdate(sql);
      bind(update, args);
      return update.execute() > 0;
    });
  }

  @Override
  public <T> T queryRow(Function<ResultSet, T> scanner, String sql, Object... args) {
    if (database.closed()) {
      return null;
    }
    return database.jdbi().withHandle(handle -> {
      Query query = handle.createQuery(sql);
      bind(query, args);
      return query.map((resultSet, context) -> scanner.apply(resultSet)).findFirst().orElse(null);
    });
  }

  @Override
  public <T> List<T> queryTable(Function<ResultSet, T> scanner, String sql, Object... args) {
    if (database.closed()) {
      return null;
    }
    return database.jdbi().withHandle(handle -> {
      Query query = handle.createQuery(sql);
      bind(query, args);
      return query.map((resultSet, context) -> scanner.apply(resultSet)).list();
    });
  }

  private static void bind(SqlStatement<?> statement, Object[] args) {
    for (int index = 0; index < args.length; index++) {
      statement.bind(index, args[index]);
    }
  }
}
