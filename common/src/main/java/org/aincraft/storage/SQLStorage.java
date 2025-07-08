package org.aincraft.storage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.aincraft.IStorage;

final class SQLStorage implements IStorage {

  private final Logger logger;
  private final IConnectionSource source;
  private final Executor executor;
  private final Extractor extractor;

  public SQLStorage(Logger logger, IConnectionSource source, Extractor extractor) {
    this.logger = logger;
    this.source = source;
    this.extractor = extractor;
    this.executor = new Executor(source, logger);
    if (!isSetup()) {
      String[] tables = this.getSqlTables().toArray(new String[0]);
      try {
        executor.executeBulk(tables);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
      logger.info("Successfully added tables to the database");
    }
  }

  @Override
  public void shutdown() throws SQLException {
    source.shutdown();
  }

  @Override
  public Executor getExecutor() {
    return executor;
  }

  @Override
  public boolean isClosed() {
    return source.isClosed();
  }

  private boolean isSetup() {
    String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE '%matcha%'";
    if (source.getType() == DatabaseType.SQLITE) {
      query = "SELECT 1 FROM sqlite_master WHERE type='table' LIMIT 1";
    }
    try (Connection connection = source.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(query);
      ResultSet rs = ps.executeQuery();
      return rs.next();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private List<String> getSqlTables() {
    try (InputStream resourceStream = extractor.getResourceStream(
        String.format("sql/%s.sql", source.getType().getIdentifier()));
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {

      String tables = reader.lines().collect(Collectors.joining("\n"));

      return Arrays.stream(tables.split(";"))
          .map(s -> s.trim() + ";")
          .filter(s -> !s.equals(";"))
          .collect(Collectors.toList());

    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
