package org.aincraft;

@FunctionalInterface
public interface IFactory<T> {
  T create();
}
