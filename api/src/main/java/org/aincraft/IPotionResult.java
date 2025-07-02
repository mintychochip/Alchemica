package org.aincraft;

import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

public interface IPotionResult {

  ItemStack stack();

  @Nullable
  Key itemKey();

  List<IPotionModifier> modifiers();

  interface IPotionResultBuilder {

    IPotionResultBuilder addType(PotionEffectType type) throws IllegalArgumentException;

    IPotionResultBuilder addModifier(IPotionModifier modifier);

    IPotionResultBuilder setBaseType(PotionType type) throws IllegalArgumentException;

    IPotionResultBuilder setMeta(Consumer<PotionMeta> metaConsumer);

    IPotionResultBuilder setItem(Key itemKey);

    IPotionResultBuilder setType(Material potionMaterial) throws IllegalArgumentException;

    IPotionResultBuilder setCorrupted(boolean corrupted);

    IPotionResult build();
  }

  interface IPotionResultBuilderFactory {

    IPotionResultBuilder create() throws IllegalArgumentException;
  }
}
