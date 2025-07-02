package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.aincraft.CauldronIngredient;
import org.aincraft.IPotionResult.IPotionResultBuilder;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

class Node {

  enum NodeType {
    BASE,
    EFFECT,
    MODIFIER
  }

  protected final Set<ConsumerNode> children = new HashSet<>();

  public Set<ConsumerNode> getChildren() {
    return children;
  }

  public ConsumerNode search(CauldronIngredient ingredient) {
    for (ConsumerNode child : children) {
      if (child.ingredient.key().equals(ingredient.key()) &&
          child.ingredient.getAmount() <= ingredient.getAmount()) {
        return child;
      }
    }
    return null;
  }

  public Node add(ConsumerNode node) {
    children.add(node);
    return this;
  }

  static final class ConsumerNode extends Node {

    private final NodeType nodeType;
    private final CauldronIngredient ingredient;

    ConsumerNode(NodeType nodeType, CauldronIngredient ingredient,
        Consumer<IPotionResultBuilder> consumer) {
      this.nodeType = nodeType;
      this.ingredient = ingredient;
      this.consumer = consumer;
    }

    private final Consumer<IPotionResultBuilder> consumer;

    public Consumer<IPotionResultBuilder> getConsumer() {
      return consumer;
    }

    @Override
    public ConsumerNode add(ConsumerNode node) {
      super.add(node);
      return this;
    }

    public NodeType getType() {
      return nodeType;
    }

    public CauldronIngredient getIngredient() {
      return ingredient;
    }

    static ConsumerNode effect(PotionEffectType effectType, CauldronIngredient ingredient) {
      return new ConsumerNode(NodeType.EFFECT, ingredient,
          container -> container.addType(effectType));
    }

    static ConsumerNode base(PotionType type, CauldronIngredient ingredient) throws IllegalArgumentException {
      Preconditions.checkArgument(
          type == PotionType.AWKWARD || type == PotionType.WATER || type == PotionType.THICK
              || type == PotionType.MUNDANE);
      return new ConsumerNode(NodeType.BASE,ingredient,container -> container.setBaseType(type));
    }
  }
}


