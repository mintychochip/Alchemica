package org.aincraft.potion;

import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.aincraft.container.IPotionModifier;
import org.aincraft.container.PotionBaseType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public interface IPotionResult {

  ItemStack stack();

  @Nullable
  Key itemKey();

  List<IPotionModifier> modifiers();

  interface IPotionResultBuilder {

    IPotionResultBuilder addType(PotionEffectType type) throws IllegalArgumentException;

    IPotionResultBuilder addModifier(IPotionModifier modifier);

    IPotionResultBuilder setBaseType(PotionBaseType baseType);

    IPotionResultBuilder setMeta(Consumer<PotionMeta> metaConsumer);

    IPotionResultBuilder setItem(Key itemKey);

    IPotionResult build();
  }

  interface IPotionResultBuilderFactory {

    IPotionResultBuilder create() throws IllegalArgumentException;
  }
}
