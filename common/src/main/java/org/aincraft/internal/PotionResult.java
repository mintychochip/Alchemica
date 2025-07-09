package org.aincraft.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.aincraft.IPotionResult;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

final class PotionResult implements IPotionResult {

  private final ItemStack stack;
  @Nullable
  private final NamespacedKey itemKey;

  PotionResult(ItemStack stack, @Nullable NamespacedKey itemKey) {
    this.stack = stack;
    this.itemKey = itemKey;
  }

  @Override
  public ItemStack getStack() {
    return stack;
  }

  @Override
  public @Nullable NamespacedKey getItemKey() {
    return itemKey;
  }

  @ApiStatus.Internal
  static final class PotionResultContext {

    Material potionMaterial = Material.POTION;

    PotionNameBuilder potionNameBuilder = new PotionNameBuilder();

    Consumer<PotionMeta> potionTypeConsumer = meta -> {
    };
    Consumer<PotionEffectMeta> metaConsumer = meta -> {
    };
    final Map<PotionEffectType, PotionEffectMeta> potionMetaMap = new HashMap<>();

    static final class PotionNameBuilder {

      private String base;
      private String prefix;

      public PotionNameBuilder withBase(String base) {
        this.base = base;
        return this;
      }

      public PotionNameBuilder withPrefix(String prefix) {
        this.prefix = prefix;
        return this;
      }

      @Override
      public String toString() {
        return prefix + " " + base;
      }
    }
  }
}
