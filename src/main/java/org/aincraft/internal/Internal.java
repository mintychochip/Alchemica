package org.aincraft.internal;

import org.aincraft.Brew;
import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.aincraft.config.IPluginConfiguration;
import org.aincraft.container.IDurationStageRegistry;
import org.aincraft.container.IFactory;
import org.aincraft.container.IPotionTrie;
import org.aincraft.container.IRegistry;
import org.aincraft.container.SimpleRegistry;
import org.aincraft.internal.PotionNode.EffectNode;
import org.aincraft.internal.PotionNode.ModifierNode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class Internal {

  private final IRegistry<ItemStack> itemRegistry;
  private final FactoryBinding<IPotionTrie> potionTrieBinding;
  private final FactoryBinding<IDurationStageRegistry> durationStageBinding;
  private final FactoryBinding<IPotionDurationMap> potionDurationMapBinding;
  private final FactoryBinding<IRegistry<EffectNode>> effectNodeRegistryBinding;
  private final FactoryBinding<IRegistry<ModifierNode>> modifierNodeRegistryBinding;

  Internal(IRegistry<ItemStack> itemRegistry, FactoryBinding<IPotionTrie> potionTrieBinding,
      FactoryBinding<IDurationStageRegistry> durationStageBinding,
      FactoryBinding<IPotionDurationMap> potionDurationMapBinding,
      FactoryBinding<IRegistry<EffectNode>> effectNodeRegistryBinding,
      FactoryBinding<IRegistry<ModifierNode>> modifierNodeRegistryBinding) {
    this.itemRegistry = itemRegistry;
    this.potionTrieBinding = potionTrieBinding;
    this.durationStageBinding = durationStageBinding;
    this.potionDurationMapBinding = potionDurationMapBinding;
    this.effectNodeRegistryBinding = effectNodeRegistryBinding;
    this.modifierNodeRegistryBinding = modifierNodeRegistryBinding;
  }

  public IDurationStageRegistry getDurationRegistry() {
    return durationStageBinding.object;
  }

  public IPotionDurationMap getPotionDurationMap() {
    return potionDurationMapBinding.object;
  }

  public IRegistry<ItemStack> getItemRegistry() {
    return itemRegistry;
  }

  IRegistry<EffectNode> getEffectNodeRegistry() {
    return effectNodeRegistryBinding.object;
  }

  IRegistry<ModifierNode> getModifierNodeRegistry() {
    return modifierNodeRegistryBinding.object;
  }
  
  public IPotionTrie getPotionTrie() {
    return potionTrieBinding.object;
  }


  public static Internal create(Brew brew) {
    Plugin plugin = brew.getPlugin();
    IPluginConfiguration pluginConfiguration = brew.getPluginConfiguration();
    IYamlConfiguration general = pluginConfiguration.getGeneralConfiguration();
    IRegistry<ItemStack> itemRegistry = new SimpleRegistry<>();
    ItemParser itemParser = new ItemParser(itemRegistry);

    FactoryBinding<IDurationStageRegistry> durationStageBinding = new FactoryBinding<>(
        new DurationStageRegistryFactory(
            plugin, general));

    IDurationStageRegistry durationStageRegistry = durationStageBinding.object;

    FactoryBinding<IPotionDurationMap> potionDurationMapBinding = new FactoryBinding<>(
        new PotionDurationMapFactory(plugin, general, durationStageRegistry));

    FactoryBinding<IRegistry<EffectNode>> effectNodeRegistryBinding = new FactoryBinding<>(
        new EffectNodeRegistryFactory(plugin, general, itemParser));

    FactoryBinding<IRegistry<ModifierNode>> modifierNodeRegistryBinding = new FactoryBinding<>(
        new ModifierNodeRegistryFactory(plugin, general, itemParser, durationStageRegistry));

    IRegistry<EffectNode> effectNodeRegistry = effectNodeRegistryBinding.object;
    IRegistry<ModifierNode> modifierNodeRegistry = modifierNodeRegistryBinding.object;

    FactoryBinding<IPotionTrie> potionTrieBinding = new FactoryBinding<>(
        new PotionTrieFactory(effectNodeRegistry, modifierNodeRegistry, general));

    return new Internal(itemRegistry, potionTrieBinding, durationStageBinding,
        potionDurationMapBinding,
        effectNodeRegistryBinding, modifierNodeRegistryBinding);
  }

  private static final class FactoryBinding<T> {

    private final IFactory<T> factory;
    private T object = null;

    private FactoryBinding(IFactory<T> factory) {
      this.factory = factory;
      this.refresh();
    }

    public void refresh() {
      object = factory.create();
    }
  }
}
