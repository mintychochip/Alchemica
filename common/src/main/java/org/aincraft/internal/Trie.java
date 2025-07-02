package org.aincraft.internal;

import java.util.Collection;
import java.util.List;
import org.aincraft.CauldronIngredient;
import org.aincraft.IPotionResult;
import org.aincraft.IPotionResult.IPotionResultBuilder;
import org.aincraft.IPotionTrie;
import org.aincraft.internal.Node.ConsumerNode;

final class Trie implements IPotionTrie {

  private final Node root;

  Trie(Node root) {
    this.root = root;
  }

  @Override
  public IPotionResult search(IPotionResultBuilder builder, Collection<CauldronIngredient> ingredients) {
    Node node = root;
    List<CauldronIngredient> working = ingredients.stream().map(CauldronIngredient::deepCopy)
        .toList();
    int index = 0;
    while (index < working.size()) {
      CauldronIngredient current = working.get(index);
      ConsumerNode child = node.search(current);
      if (child == null) {
        return null;
      }
      CauldronIngredient required = child.getIngredient();
      current.updateAmount(amount -> amount - required.getAmount());
      child.getConsumer().accept(builder);
      node = child;
      if (current.getAmount() == 0) {
        index++;
      }
    }
    return builder.build();
  }
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toStringRecursive(root, sb, 0, 3);
    return sb.toString();
  }

  private void toStringRecursive(Node node, StringBuilder sb, int depth, int maxDepth) {
    if (depth >= maxDepth) {
      indent(sb, depth);
      sb.append("... (max depth reached)\n");
      return;
    }

    for (Node.ConsumerNode child : node.getChildren()) {
      indent(sb, depth);
      sb.append("- [")
          .append(child.getType())
          .append("] ")
          .append(" <- ")
          .append(child.getIngredient().toString())
          .append("\n");

      toStringRecursive(child, sb, depth + 1, maxDepth);
    }
  }

  private void indent(StringBuilder sb, int depth) {
    sb.append("  ".repeat(depth));
  }

}
