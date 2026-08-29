package org.aincraft.storage;

import org.aincraft.IExecutor;
import org.aincraft.IStorage;
import org.aincraft.db.sql.SqlDatabase;

final class SqlStorage implements IStorage {

  private final SqlDatabase database;
  private final IExecutor executor;

  SqlStorage(SqlDatabase database) {
    this.database = database;
    this.executor = new JdbiExecutor(database);
  }

  @Override
  public boolean isClosed() {
    return database.closed();
  }

  @Override
  public void shutdown() {
    database.close();
  }

  @Override
  public IExecutor getExecutor() {
    return executor;
  }
}
