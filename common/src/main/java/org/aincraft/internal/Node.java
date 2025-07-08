package org.aincraft.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.aincraft.CauldronIngredient;
import org.aincraft.internal.PotionResult.PotionResultContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
final class Node {

  private final Set<Node> children = new HashSet<>();
  private final NodeType nodeType;
  private final CauldronIngredient ingredient;
  private final Consumer<PotionResultContext> consumer;

  Node(NodeType nodeType, CauldronIngredient ingredient,
      Consumer<PotionResultContext> consumer) {
    this.nodeType = nodeType;
    this.ingredient = ingredient;
    this.consumer = consumer;
  }

  Consumer<PotionResultContext> getConsumer() {
    return consumer;
  }

  void addChild(Node node) {
    children.add(node);
  }

  Set<Node> getChildren() {
    return children;
  }

  Node search(@NotNull CauldronIngredient ingredient) {
    for (Node child : children) {
      if (child == null) {
        continue;
      }
      CauldronIngredient childIngredient = child.getIngredient();
      if (ingredient.isSimilar(childIngredient)
          && ingredient.getAmount() >= childIngredient.getAmount()) {
        return child;
      }
    }
    return null;
  }

  NodeType getType() {
    return nodeType;
  }

  CauldronIngredient getIngredient() {
    return ingredient;
  }
}
