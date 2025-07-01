package org.aincraft.internal;

import org.aincraft.container.IPotionTrie;
import org.aincraft.ingredient.CauldronIngredient;
import org.aincraft.potion.IPotionResult;
import org.aincraft.potion.IPotionResult.IPotionResultBuilder;

final class PotionTrie implements IPotionTrie {

  private final PotionNode root;

  PotionTrie(PotionNode root) {
    this.root = root;
  }

  @Override
  public IPotionResult search(IPotionResultBuilder builder, CauldronIngredient ... ingredients) {
    PotionNode current = root;
    current.getConsumer().accept(builder);
    for (CauldronIngredient ingredient : ingredients) {
      if (current == null) {
        continue;
      }
      current = current.search(ingredient);
      current.getConsumer().accept(builder);
    }
    return builder.build();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("PotionTrie:\n");
    traverse(root, builder, 0, "ROOT");
    return builder.toString();
  }

  private static final int MAX_DEPTH = 3; // change this value to limit traversal depth

  private void traverse(PotionNode node, StringBuilder builder, int depth, String via) {
    if (depth > MAX_DEPTH) return;

    String indent = "  ".repeat(depth);
    builder.append(indent)
        .append("-[")
        .append(via)
        .append("]→ ")
        .append(node.getClass().getSimpleName())
        .append("\n");

    for (PotionNode child : node.getChildren()) {
      String ingredientLabel = child.getIngredient().toString();
      traverse(child, builder, depth + 1, ingredientLabel);
    }
  }

}
