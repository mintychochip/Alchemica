package org.aincraft.container;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.inventory.ItemStack;

public record RegistrableItem(Key key, ItemStack stack) implements Keyed {

}
