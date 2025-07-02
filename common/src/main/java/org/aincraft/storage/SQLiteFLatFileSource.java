package org.aincraft.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

final class SQLiteFLatFileSource extends FlatFileSource {

  public SQLiteFLatFileSource(Plugin plugin, Logger logger, Path parentDir) {
    super(plugin, logger, parentDir);
  }

  @Override
  public Connection getConnection() {
    Connection connection = super.getConnection();
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON;");
      statement.execute("PRAGMA journal_mode = WAL;");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return connection;
  }

  @Override
  public DatabaseType getType() {
    return DatabaseType.SQLITE;
  }
}
