package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.aincraft.IDurationStage;
import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.aincraft.CauldronIngredient;
import org.aincraft.container.IDurationStageRegistry;
import org.aincraft.IFactory;
import org.aincraft.IPotionEffectMeta;
import org.aincraft.IPotionModifier;
import org.aincraft.IPotionModifier.ModifierType;
import org.aincraft.internal.Node.ConsumerNode;
import org.aincraft.internal.Node.NodeType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

final class ModifierNodeRegistryFactory implements IFactory<List<ConsumerNode>> {

  private final ModifierNodeFactory modifierNodeFactory;
  private final IYamlConfiguration configuration;

  ModifierNodeRegistryFactory(Plugin plugin,
      IYamlConfiguration configuration, KeyParser parser,
      IDurationStageRegistry durationRegistry) {
    this.modifierNodeFactory = new ModifierNodeFactory(plugin, parser, durationRegistry);
    this.configuration = configuration;
  }

  private static final class ModifierNodeFactory {

    private final ModifierFactory modifierFactory;
    private final Plugin plugin;
    private final KeyParser parser;

    ModifierNodeFactory(Plugin plugin, KeyParser parser, IDurationStageRegistry durationRegistry) {
      this.modifierFactory = new ModifierFactory(durationRegistry);
      this.plugin = plugin;
      this.parser = parser;
    }

    public ConsumerNode create(@NotNull ConfigurationSection section)
        throws IllegalArgumentException {
      Preconditions.checkArgument(section.contains("item", false));
      Preconditions.checkArgument(section.contains("amount", false));
      Preconditions.checkArgument(section.contains("modifiers", false));
      String keyString = section.getName();
      String itemKeyString = section.getString("item");
      assert itemKeyString != null;
      Key parsed = parser.parse(itemKeyString);
      int amount = section.getInt("amount");
      List<IPotionModifier> modifiers = new ArrayList<>();
      ConfigurationSection modifierSection = section.getConfigurationSection("modifiers");
      for (String modifierKey : modifierSection.getKeys(false)) {
        if ("duration".equals(modifierKey)) {
          int steps = modifierSection.getInt(modifierKey);
          IPotionModifier durationModifier = modifierFactory.createDurationModifier(steps);
          modifiers.add(durationModifier);
        }
        if ("amplifier".equals(modifierKey)) {
          int steps = modifierSection.getInt(modifierKey);
          IPotionModifier amplifierModifier = modifierFactory.createAmplifierModifier(steps);
          modifiers.add(amplifierModifier);
        }
        if ("ambient".equals(modifierKey)) {
          boolean ambient = modifierSection.getBoolean(modifierKey);
          IPotionModifier ambientModifier = modifierFactory.createAmbientModifier(ambient);
          modifiers.add(ambientModifier);
        }
        if ("particles".equals(modifierKey)) {
          boolean particles = modifierSection.getBoolean(modifierKey);
          IPotionModifier particlesModifier = modifierFactory.createParticlesModifier(particles);
          modifiers.add(particlesModifier);
        }
      }
      return new ConsumerNode(NodeType.MODIFIER,
          new CauldronIngredient(parsed, amount),
          builder -> modifiers.forEach(builder::addModifier));
    }
  }

  private record ModifierFactory(IDurationStageRegistry durationRegistry) {

    private record PotionModifier(ModifierType type,
                                  Consumer<IPotionEffectMeta> metaConsumer) implements
        IPotionModifier {

      @Override
      public void modify(IPotionEffectMeta meta) {
        metaConsumer.accept(meta);
      }
    }

    IPotionModifier createDurationModifier(int steps) {
      return new PotionModifier(ModifierType.DURATION, meta -> {
        meta.getDuration();
        IDurationStage durationStage = meta.getDuration();
        meta.setDuration(durationRegistry.step(durationStage, steps));
      });
    }

    IPotionModifier createAmplifierModifier(int steps) {
      return new PotionModifier(ModifierType.AMPLIFIER, meta -> {
        int amplifier = meta.getAmplifier();
        meta.setAmplifier(amplifier + steps);
      });
    }

    IPotionModifier createAmbientModifier(boolean ambient) {
      return new PotionModifier(ModifierType.AMBIENT, meta -> meta.setAmbient(ambient));
    }

    IPotionModifier createParticlesModifier(boolean particles) {
      return new PotionModifier(ModifierType.PARTICLES, meta -> meta.setParticles(particles));
    }
  }


  @Override
  public List<ConsumerNode> create() {
    Preconditions.checkArgument(configuration.contains("modifiers"));
    ConfigurationSection modifierSection = configuration.getConfigurationSection("modifiers");
    List<ConsumerNode> modifierNodes = new ArrayList<>();
    for (String modifierNodeKey : modifierSection.getKeys(false)) {
      ConfigurationSection modifierNodeSection = modifierSection.getConfigurationSection(
          modifierNodeKey);
      if (modifierNodeSection == null) {
        Bukkit.getLogger().info("modifier section is null: %s".formatted(modifierNodeKey));
        continue;
      }
      try {
        ConsumerNode modifierNode = modifierNodeFactory.create(modifierNodeSection);
        modifierNodes.add(modifierNode);
      } catch (IllegalArgumentException ex) {
        Bukkit.getLogger().info(ex.getMessage());
      }
    }
    return modifierNodes;
  }
}
