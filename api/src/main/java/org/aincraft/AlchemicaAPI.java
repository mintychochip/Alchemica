package org.aincraft;

import org.jetbrains.annotations.Nullable;

public final class AlchemicaAPI {

  private static volatile IBrewAPI instance;

  private AlchemicaAPI() {}

  /**
   * Returns the IBrewAPI instance.
   *
   * @throws IllegalStateException if called before Alchemica has enabled
   */
  public static IBrewAPI getInstance() {
    IBrewAPI api = instance;
    if (api == null) {
      throw new IllegalStateException(
          "AlchemicaAPI is not available yet. Ensure Alchemica is enabled before calling this.");
    }
    return api;
  }

  /**
   * Sets or clears the IBrewAPI instance. Called by common on enable/disable.
   * Throws IllegalStateException if called with a non-null value while an instance
   * is already set, preventing external plugins from replacing it.
   */
  public static synchronized void setInstance(@Nullable IBrewAPI api) {
    if (api != null && instance != null) {
      throw new IllegalStateException(
          "AlchemicaAPI instance is already set. Do not call setInstance() from external plugins.");
    }
    instance = api;
  }
}
