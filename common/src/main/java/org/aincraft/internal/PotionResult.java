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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PotionResult implements IPotionResult {

  private final Status status;
  private final ItemStack stack;
  @Nullable
  private final NamespacedKey itemKey;

  PotionResult(Status status, ItemStack stack, @Nullable NamespacedKey itemKey) {
    this.status = status;
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

  @Override
  public @NotNull Status getStatus() {
    return status;
  }

  @ApiStatus.Internal
  static final class PotionContext {
    NamespacedKey potionkey = null;

    Material potionMaterial = Material.POTION;

    Consumer<PotionMeta> potionTypeConsumer = meta -> {
    };
    Consumer<PotionEffectMeta> metaConsumer = meta -> {
    };
    final Map<PotionEffectType, PotionEffectMeta> potionMetaMap = new HashMap<>();
  }
}
