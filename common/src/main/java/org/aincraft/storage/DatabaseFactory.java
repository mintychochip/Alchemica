package org.aincraft.storage;

import com.zaxxer.hikari.HikariConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.aincraft.IFactory;
import org.aincraft.IStorage;
import org.aincraft.db.sql.SqlDatabase;
import org.bukkit.plugin.Plugin;

public final class DatabaseFactory implements IFactory<IStorage> {
  private static final String MIGRATION_LOCATION = "classpath:db/migration";
  private static final String LEGACY_DATABASE_FILENAME = "brew-sqlite.db";
  private static final String RENAMED_DATABASE_FILENAME = "alchemica-sqlite.db";

  private static final String LEGACY_BASELINE_SQL =
      "CREATE TABLE IF NOT EXISTS flyway_schema_history ("
          + "installed_rank INTEGER NOT NULL,"
          + "version VARCHAR(50),"
          + "description VARCHAR(200) NOT NULL,"
          + "type VARCHAR(20) NOT NULL,"
          + "script VARCHAR(1000) NOT NULL,"
          + "checksum INTEGER,"
          + "installed_by VARCHAR(100) NOT NULL,"
          + "installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
          + "execution_time INTEGER NOT NULL,"
          + "success BOOLEAN NOT NULL,"
          + "CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank))";
  private static final String LEGACY_BASELINE_INDEX_SQL =
      "CREATE INDEX IF NOT EXISTS flyway_schema_history_s_idx "
          + "ON flyway_schema_history (success)";
  private static final String LEGACY_BASELINE_INSERT_SQL =
      "INSERT INTO flyway_schema_history "
          + "(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success) "
          + "VALUES (1,'0','<< Flyway Baseline >>','BASELINE','<< Flyway Baseline >>',NULL,'Alchemica',0,1)";

  private final Logger logger;
  private final Plugin plugin;
  private final IYamlConfiguration dbConfiguration;

  public DatabaseFactory(Logger logger, Plugin plugin, IYamlConfiguration databaseConfiguration) {
    this.logger = logger;
    this.plugin = plugin;
    this.dbConfiguration = databaseConfiguration;
  }

  @Override
  public IStorage create() {
    DatabaseType type = DatabaseType.fromIdentifier(dbConfiguration.getString("type"));
    if (type != DatabaseType.SQLITE) {
      throw new IllegalStateException("unsupported Alchemica database type: " + type);
    }

    Path dataFolder = plugin.getDataFolder().toPath();
    try {
      Files.createDirectories(dataFolder);
    } catch (IOException e) {
      throw new IllegalStateException("failed to create plugin data directory", e);
    }

    // Keep the pre-migration filename so existing cauldrons and player settings remain in place.
    Path databaseFile = resolveDatabaseFile(dataFolder);

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());
    hikariConfig.setDriverClassName(type.getClassName());
    hikariConfig.setMaximumPoolSize(1);
    hikariConfig.setConnectionInitSql("PRAGMA foreign_keys = ON");

    ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(plugin.getClass().getClassLoader());
    SqlDatabase database = null;
    try {
      logger.info("connecting to Alchemica database file: " + databaseFile);
      database = SqlDatabase.create(hikariConfig, MIGRATION_LOCATION);
      initializeSqlite(database);
      baselineLegacyDatabase(database);
      database.migrate();
      return new SqlStorage(database);
    } catch (RuntimeException e) {
      if (database != null) {
        database.close();
      }
      throw new IllegalStateException("failed to migrate Alchemica SQLite database", e);
    } finally {
      Thread.currentThread().setContextClassLoader(previousClassLoader);
    }
  }

  static Path resolveDatabaseFile(Path dataFolder) {
    Path legacyDatabase = dataFolder.resolve(LEGACY_DATABASE_FILENAME);
    if (Files.exists(legacyDatabase)) {
      return legacyDatabase;
    }
    Path renamedDatabase = dataFolder.resolve(RENAMED_DATABASE_FILENAME);
    return Files.exists(renamedDatabase) ? renamedDatabase : legacyDatabase;
  }

  private static void initializeSqlite(SqlDatabase database) {
    database.jdbi().useHandle(handle -> handle.execute("PRAGMA journal_mode = WAL"));
  }

  private static void baselineLegacyDatabase(SqlDatabase database) {
    database.jdbi().useTransaction(handle -> {
      boolean hasHistory = handle.createQuery(
              "SELECT 1 FROM sqlite_master WHERE type='table' AND name='flyway_schema_history'")
          .mapTo(Integer.class)
          .findFirst()
          .isPresent();
      boolean hasApplicationTable = handle.createQuery(
              "SELECT 1 FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' "
                  + "AND name <> 'flyway_schema_history' LIMIT 1")
          .mapTo(Integer.class)
          .findFirst()
          .isPresent();
      if (!hasHistory && hasApplicationTable) {
        handle.execute(LEGACY_BASELINE_SQL);
        handle.execute(LEGACY_BASELINE_INDEX_SQL);
        handle.execute(LEGACY_BASELINE_INSERT_SQL);
      }
    });
  }
}
