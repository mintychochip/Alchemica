package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.aincraft.IPotionEffectMeta;
import org.aincraft.IPotionModifier;
import org.aincraft.IPotionResult;
import org.aincraft.IVersionAdapter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

record PotionResult(ItemStack stack, @Nullable Key itemKey,
                    List<IPotionModifier> modifiers) implements IPotionResult {

  static final class PotionResultBuilder implements IPotionResultBuilder {

    private final PotionEffectMetaFactory metaFactory;
    private final IVersionAdapter adapter;
    private Material potionMaterial = Material.POTION;
    private Consumer<PotionMeta> metaConsumer = null;
    private Key itemKey;
    private final List<IPotionModifier> modifiers;
    private final Map<PotionEffectType, IPotionEffectMeta> potionMetaMap;
    private boolean corrupted = false;

    PotionResultBuilder(PotionEffectMetaFactory metaFactory, IVersionAdapter adapter, Key itemKey,
        List<IPotionModifier> modifiers,
        Map<PotionEffectType, IPotionEffectMeta> potionMetaMap) {
      this.metaFactory = metaFactory;
      this.adapter = adapter;
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
    public IPotionResultBuilder setBaseType(PotionType type) throws IllegalArgumentException {
      Preconditions.checkArgument(
          type == PotionType.AWKWARD || type == PotionType.WATER || type == PotionType.THICK
              || type == PotionType.MUNDANE);
      setMeta(meta -> meta.setBasePotionData(new PotionData(type)));
      return this;
    }

    @Override
    public IPotionResultBuilder setMeta(Consumer<PotionMeta> metaConsumer) {
      this.metaConsumer = this.metaConsumer == null ?
          metaConsumer : this.metaConsumer.andThen(metaConsumer);
      return this;
    }

    @Override
    public IPotionResultBuilder setItem(Key itemKey) {
      this.itemKey = itemKey;
      return this;
    }

    @Override
    public IPotionResultBuilder setType(Material potionMaterial) throws IllegalArgumentException {
      Preconditions.checkArgument(
          potionMaterial == Material.POTION || potionMaterial == Material.SPLASH_POTION
              || potionMaterial == Material.LINGERING_POTION);
      this.potionMaterial = potionMaterial;
      return this;
    }

    @Override
    public IPotionResultBuilder setCorrupted(boolean corrupted) {
      this.corrupted = corrupted;
      return this;
    }

    @Override
    public IPotionResult build() {
      if (itemKey != null) {
        return new PotionResult(null, itemKey, modifiers);
      }

      ItemStack stack = new ItemStack(potionMaterial);
      ItemMeta itemMeta = stack.getItemMeta();
      PotionMeta potionMeta = (PotionMeta) itemMeta;
      if (metaConsumer != null) {
        metaConsumer.accept(potionMeta);
      }
      for (Entry<PotionEffectType, IPotionEffectMeta> entry : potionMetaMap.entrySet()) {
        IPotionEffectMeta meta = entry.getValue();
        PotionEffectType type = entry.getKey();
        for (IPotionModifier modifier : modifiers) {
          modifier.modify(meta);
        }
        if (corrupted) {
          if (type == PotionEffectType.SPEED || type == adapter.getByKey(
              NamespacedKey.minecraft("jump_boost"))) {
            type = adapter.getByKey(NamespacedKey.minecraft("slowness"));
          }
          if (type == adapter.getByKey(NamespacedKey.minecraft("instant_health"))
              || type == PotionEffectType.POISON) {
            type = adapter.getByKey(NamespacedKey.minecraft("instant_damage"));
          }
          if (type == PotionEffectType.NIGHT_VISION) {
            type = PotionEffectType.INVISIBILITY;
          }
        }
        potionMeta.addCustomEffect(new PotionEffect(type,
            meta.getDuration().getTicks(),
            meta.getAmplifier(),
            meta.isAmbient(),
            meta.hasParticles()), true);
      }
      potionMeta.displayName(Component.text("Potion").decoration(TextDecoration.ITALIC, false));
      stack.setItemMeta(potionMeta);
      return new PotionResult(stack, null, modifiers);
    }
  }
}
