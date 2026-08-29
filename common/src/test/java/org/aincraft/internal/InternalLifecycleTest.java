package org.aincraft.internal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.aincraft.IExecutor;
import org.aincraft.IStorage;
import org.junit.jupiter.api.Test;

class InternalLifecycleTest {

  @Test
  void databaseClosesWhenPostCreationInitializationFails() {
    AtomicBoolean shutdown = new AtomicBoolean();
    IStorage database = new TestStorage(shutdown);
    IllegalStateException failure = new IllegalStateException("invalid later configuration");

    IllegalStateException thrown =
        assertThrows(
            IllegalStateException.class,
            () -> Internal.withDatabase(database, () -> { throw failure; }));

    assertSame(failure, thrown);
    assertTrue(shutdown.get());
  }

  private record TestStorage(AtomicBoolean shutdownState) implements IStorage {
    @Override
    public boolean isClosed() {
      return shutdownState.get();
    }

    @Override
    public void shutdown() {
      shutdownState.set(true);
    }

    @Override
    public IExecutor getExecutor() {
      return null;
    }
  }
}
