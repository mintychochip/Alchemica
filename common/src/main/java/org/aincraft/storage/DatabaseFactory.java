package org.aincraft.storage;

import java.util.logging.Logger;
import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.aincraft.IFactory;
import org.aincraft.IStorage;
import org.bukkit.plugin.Plugin;

public final class DatabaseFactory implements IFactory<IStorage> {

  private final Logger logger;
  private final Plugin plugin;
  private final IYamlConfiguration dbConfiguration;
  private final Extractor extractor;

  public DatabaseFactory(Logger logger, Plugin plugin, IYamlConfiguration databaseConfiguration,
      Extractor extractor) {
    this.logger = logger;
    this.plugin = plugin;
    this.dbConfiguration = databaseConfiguration;
    this.extractor = extractor;
  }

  @Override
  public IStorage create() {
    DatabaseType type = DatabaseType.fromIdentifier(dbConfiguration.getString("type"));
    return switch (type) {
      case SQLITE -> new SQLStorage(logger,
          new SQLiteFLatFileSource(plugin, logger, plugin.getDataFolder().toPath()), extractor);
    };
  }
}

