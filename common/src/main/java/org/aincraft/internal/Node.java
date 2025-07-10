package org.aincraft.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.aincraft.CauldronIngredient;
import org.aincraft.internal.PotionResult.PotionContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
final class Node {

  @NotNull
  private final Set<Node> children = new HashSet<>();

  @NotNull
  private final NodeType nodeType;

  @NotNull
  private final CauldronIngredient ingredient;

  @NotNull
  private final Consumer<PotionContext> consumer;

  @NotNull
  private final String permission;

  Node(@NotNull NodeType nodeType, @NotNull CauldronIngredient ingredient,
      @NotNull Consumer<PotionContext> consumer, @NotNull String permission) {
    this.nodeType = nodeType;
    this.ingredient = ingredient;
    this.consumer = consumer;
    this.permission = permission;
  }

  @NotNull
  Consumer<PotionContext> getConsumer() {
    return consumer;
  }

  void addChild(Node node) {
    children.add(node);
  }

  @NotNull
  Set<Node> getChildren() {
    return children;
  }

  @NotNull
  String getPermission() {
    return permission;
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

  @NotNull
  CauldronIngredient getIngredient() {
    return ingredient;
  }
}
