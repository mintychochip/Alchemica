package org.aincraft.internal;

import net.kyori.adventure.key.Key;
import org.aincraft.IRegistry;
import org.aincraft.container.RegistrableItem;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

final class KeyParser {

  private final IRegistry<RegistrableItem> itemRegistry;

  KeyParser(IRegistry<RegistrableItem> itemRegistry) {
    this.itemRegistry = itemRegistry;
  }

  Key parse(@NotNull String keyString) throws IllegalArgumentException {
    String lowerCase = keyString.toLowerCase();
    String[] split = lowerCase.split(":");
    if (!(split.length == 2 || split.length == 1)) {
      throw new IllegalArgumentException("the key is invalid");
    }
    if (split.length == 1) {
      return NamespacedKey.minecraft(split[0]);
    }
    return new NamespacedKey(split[0], split[1]);
  }

  Key fromItem(@NotNull ItemStack stack) {
    for (RegistrableItem registrableItem : itemRegistry) {
      if (registrableItem.stack().isSimilar(stack)) {
        return registrableItem.key();
      }
    }
    return stack.getType().getKey();
  }
}
