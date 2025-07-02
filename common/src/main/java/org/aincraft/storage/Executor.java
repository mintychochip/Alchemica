package org.aincraft.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import org.aincraft.IExecutor;

final class Executor implements IExecutor {

  private final IConnectionSource source;
  private final Logger logger;

  public Executor(IConnectionSource source, Logger logger) {
    this.source = source;
    this.logger = logger;
  }

  public boolean execute(String sql) throws SQLException {
    if (source.isClosed()) {
      return false;
    }
    try (Connection connection = source.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(sql);
      return ps.execute();
    }
  }

  public void executeBulk(String... sql) throws SQLException {
    if (source.isClosed()) {
      return;
    }
    try (Connection connection = source.getConnection()) {
      connection.setAutoCommit(false);
      Savepoint savepoint = connection.setSavepoint();

      try (Statement stmt = connection.createStatement()) {
        for (String query : sql) {
          stmt.addBatch(query);
        }
        stmt.executeBatch();
        connection.commit();
      } catch (SQLException e) {
        connection.rollback(savepoint);
        throw new SQLException("Error executing bulk SQL", e);
      }
    }
  }


  public boolean executeUpdate(String query, Object... args) {
    if (source.isClosed()) {
      return false;
    }
    try (Connection connection = source.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(query);
      int count = 1;
      for (Object arg : args) {
        if (arg instanceof Timestamp timestamp) {
          ps.setTimestamp(count++, timestamp);
          continue;
        }
        ps.setObject(count++, arg);
      }
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> List<T> queryTable(Function<ResultSet, T> scanner, String query, Object... args) {
    if (source.isClosed()) {
      return null;
    }
    try (Connection connection = source.getConnection();
        PreparedStatement ps = connection.prepareStatement(query)) {
      int count = 1;
      for (Object arg : args) {
        ps.setObject(count++, arg);
      }
      try (ResultSet rs = ps.executeQuery()) {
        List<T> results = new ArrayList<>();
        while (rs.next()) {
          results.add(scanner.apply(rs));
        }
        return results;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> T queryRow(Function<ResultSet, T> scanner, String query, Object... args) {
    if (source.isClosed()) {
      return null;
    }
    try (Connection connection = source.getConnection();
        PreparedStatement ps = connection.prepareStatement(query)) {
      int count = 1;
      for (Object arg : args) {
        ps.setObject(count++, arg);
      }
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return scanner.apply(rs);
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
