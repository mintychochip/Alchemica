//package org.aincraft.internal;
//
//import net.kyori.adventure.key.Key;
//import org.aincraft.IRegistry;
//import org.bukkit.Material;
//import org.bukkit.NamespacedKey;
//import org.bukkit.Registry;
//import org.bukkit.inventory.ItemStack;
//import org.jetbrains.annotations.NotNull;
//
//final class ItemParser {
//
//  private final IRegistry<ItemStack> itemRegistry;
//
//  ItemParser(IRegistry<ItemStack> itemRegistry) {
//    this.itemRegistry = itemRegistry;
//  }
//
//  @NotNull
//  ItemStack parse(Key key) throws IllegalArgumentException {
//    if ("minecraft".equals(key.namespace())) {
//      Material material = Registry.MATERIAL.get(key);
//      if (material == null || !material.isItem()) {
//        throw new IllegalArgumentException("key is invalid material: %s".formatted(key));
//      }
//      return ItemStack.of(material);
//    }
//    if (!itemRegistry.isRegistered(key)) {
//      throw new IllegalArgumentException("unable to locate item");
//    }
//    return itemRegistry.get(key);
//  }
//
//  @NotNull
//  ItemStack parse(String keyString) throws IllegalArgumentException {
//    String lowerCase = keyString.toLowerCase();
//    String[] split = lowerCase.split(":");
//    if (!(split.length == 2 || split.length == 1)) {
//      throw new IllegalArgumentException("the key is invalid");
//    }
//    if (split.length == 1) {
//      return parse(new NamespacedKey("minecraft", split[0]));
//    }
//    return parse(new NamespacedKey(split[0], split[1]));
//  }
//}
