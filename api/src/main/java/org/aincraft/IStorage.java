package org.aincraft;

public interface IStorage {
  boolean isClosed();
  void shutdown() throws Exception;
  IExecutor getExecutor();
}
