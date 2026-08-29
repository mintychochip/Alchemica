package org.aincraft.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseFactoryTest {

  @Test
  void newInstallationUsesLegacyBrewDatabaseFilename(@TempDir Path dataFolder) {
    assertEquals(dataFolder.resolve("brew-sqlite.db"), DatabaseFactory.resolveDatabaseFile(dataFolder));
  }

  @Test
  void existingLegacyDatabaseWinsOverRenamedDatabase(@TempDir Path dataFolder) throws Exception {
    Path legacy = Files.createFile(dataFolder.resolve("brew-sqlite.db"));
    Files.createFile(dataFolder.resolve("alchemica-sqlite.db"));

    assertEquals(legacy, DatabaseFactory.resolveDatabaseFile(dataFolder));
  }

  @Test
  void existingRenamedDatabaseRemainsUsable(@TempDir Path dataFolder) throws Exception {
    Path renamed = Files.createFile(dataFolder.resolve("alchemica-sqlite.db"));

    assertEquals(renamed, DatabaseFactory.resolveDatabaseFile(dataFolder));
  }
}
