package org.aincraft.providers;

import org.bukkit.Material;

final class LegacyMaterialAdapter {

  private final byte data;
  private final Material material;

  LegacyMaterialAdapter(Material material, byte data) {
    this.material = material;
    this.data = data;
  }

  @Override
  public int hashCode() {
    return data + material.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    LegacyMaterialAdapter other = (LegacyMaterialAdapter) obj;
    return this.data == other.data &&
        this.material.equals(other.material);
  }

  @Override
  public String toString() {
    return material.name() + " " + data;
  }
}
