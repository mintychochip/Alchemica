package org.aincraft.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.aincraft.container.IPotionModifier;
import org.aincraft.ingredient.CauldronIngredient;
import org.aincraft.potion.IPotionResult.IPotionResultBuilder;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

class PotionNode implements Consumer<IPotionResultBuilder> {

  private final Key key;
  private final CauldronIngredient ingredient;
  private final Set<PotionNode> children = new HashSet<>();

  private final Consumer<IPotionResultBuilder> consumer;

  public PotionNode(Key key, CauldronIngredient ingredient,
      Consumer<IPotionResultBuilder> consumer) {
    this.key = key;
    this.ingredient = ingredient;
    this.consumer = consumer;
  }

  public Consumer<IPotionResultBuilder> getConsumer() {
    return consumer;
  }

  public void addChild(PotionNode node) {
    children.add(node);
  }

  @Nullable
  public PotionNode search(CauldronIngredient ingredient) {
    for (PotionNode child : children) {
      if (child.getIngredient().equals(ingredient)) {
        return child;
      }
    }
    return null;
  }

  public Set<PotionNode> getChildren() {
    return children;
  }

  public CauldronIngredient getIngredient() {
    return ingredient;
  }

  @Override
  public void accept(IPotionResultBuilder IPotionResultBuilder) {
    consumer.accept(IPotionResultBuilder);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PotionNode node)) {
      return false;
    }
    return this.key.equals(node.key);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("PotionNode{key=").append(key);

    if (!children.isEmpty()) {
      builder.append(", children=[");
      boolean first = true;
      for (PotionNode node : children) {
        if (!first) {
          builder.append(", ");
        }
        builder.append(node.getKey().toString())
            .append(" -> ")
            .append(node);
        first = false;
      }
      builder.append("]");
    }

    builder.append("}");
    return builder.toString();
  }

  public Key getKey() {
    return key;
  }

  static final class ModifierNode extends PotionNode {

    private final List<IPotionModifier> modifiers;

    public ModifierNode(Key key, CauldronIngredient ingredient, List<IPotionModifier> modifiers) {
      super(key, ingredient, container -> modifiers.forEach(container::addModifier));
      this.modifiers = modifiers;
    }

    public List<IPotionModifier> getModifiers() {
      return modifiers;
    }
  }

  static final class EffectNode extends PotionNode {

    public EffectNode(Key key, CauldronIngredient ingredient, PotionEffectType effectType) {
      super(key, ingredient, container -> container.addType(effectType));

    }
  }

}
