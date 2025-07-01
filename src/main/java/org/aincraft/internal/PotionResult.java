package org.aincraft.internal;

import com.google.common.base.Preconditions;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.aincraft.container.IPotionEffectMeta;
import org.aincraft.container.IPotionModifier;
import org.aincraft.container.PotionBaseType;
import org.aincraft.potion.IPotionResult;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

record PotionResult(ItemStack stack, @Nullable Key itemKey,
                           List<IPotionModifier> modifiers) implements IPotionResult {

  static final class PotionResultBuilder implements IPotionResultBuilder {

    private final PotionEffectMetaFactory metaFactory;
    private final ItemStack stack;
    private Key itemKey;
    private final List<IPotionModifier> modifiers;
    private final Map<PotionEffectType, IPotionEffectMeta> potionMetaMap;

    PotionResultBuilder(PotionEffectMetaFactory metaFactory, ItemStack stack, Key itemKey,
        List<IPotionModifier> modifiers,
        Map<PotionEffectType, IPotionEffectMeta> potionMetaMap) {
      this.metaFactory = metaFactory;
      this.stack = stack;
      this.itemKey = itemKey;
      this.modifiers = modifiers;
      this.potionMetaMap = potionMetaMap;
    }

    @Override
    public IPotionResultBuilder addType(PotionEffectType type) throws IllegalArgumentException {
      Preconditions.checkArgument(!potionMetaMap.containsKey(type));
      IPotionEffectMeta meta = metaFactory.create(type);
      potionMetaMap.put(type, meta);
      return this;
    }

    @Override
    public IPotionResultBuilder addModifier(IPotionModifier modifier) {
      modifiers.add(modifier);
      return this;
    }

    @Override
    public IPotionResultBuilder setBaseType(PotionBaseType baseType) {
      return setMeta(meta -> meta.setBasePotionType(baseType.getPotionType()));
    }

    @Override
    public IPotionResultBuilder setMeta(Consumer<PotionMeta> metaConsumer) {
      PotionMeta meta = (PotionMeta) stack.getItemMeta();
      metaConsumer.accept(meta);
      stack.setItemMeta(meta);
      return this;
    }

    @Override
    public IPotionResultBuilder setItem(Key itemKey) {
      this.itemKey = itemKey;
      return this;
    }


    @SuppressWarnings("UnstableApiUsage")
    @Override
    public IPotionResult build() {
      if (itemKey != null) {
        return new PotionResult(stack, itemKey, modifiers);
      }
      PotionContents.Builder builder = PotionContents.potionContents();
      for (Entry<PotionEffectType, IPotionEffectMeta> entry : potionMetaMap.entrySet()) {
        IPotionEffectMeta meta = entry.getValue();
        PotionEffectType type = entry.getKey();
        for (IPotionModifier modifier : modifiers) {
          modifier.modify(meta);
        }
        builder.addCustomEffect(new PotionEffect(type,
            meta.getDuration().getTicks(),
            meta.getAmplifier(),
            meta.isAmbient(),
            meta.hasParticles()));
      }
      stack.setData(DataComponentTypes.POTION_CONTENTS, builder);
      return new PotionResult(stack, null, modifiers);
    }
  }
}
