package org.aincraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public final class CustomRecipe {

  private final NamespacedKey key;
  private final List<CauldronIngredient> ingredients;
  private final Function<BrewContext, ItemStack> resultFn;
  private final String permission;
  private final Set<String> disabledModifiers;

  private CustomRecipe(Builder builder) {
    this.key = builder.key;
    this.ingredients = Collections.unmodifiableList(new ArrayList<>(builder.ingredients));
    this.resultFn = builder.resultFn;
    this.permission = builder.permission != null
        ? builder.permission
        : "alchemica:" + builder.key.getKey();
    this.disabledModifiers = Collections.unmodifiableSet(new HashSet<>(builder.disabledModifiers));
  }

  public NamespacedKey getKey() { return key; }
  public List<CauldronIngredient> getIngredients() { return ingredients; }
  public Function<BrewContext, ItemStack> getResultFn() { return resultFn; }
  public String getPermission() { return permission; }
  public Set<String> getDisabledModifiers() { return disabledModifiers; }

  public static final class Builder {

    private final NamespacedKey key;
    private final List<CauldronIngredient> ingredients = new ArrayList<>();
    private Function<BrewContext, ItemStack> resultFn;
    private String permission;
    private final Set<String> disabledModifiers = new HashSet<>();

    public Builder(NamespacedKey key) {
      this.key = key;
    }

    public Builder ingredients(String... keys) {
      for (String keyStr : keys) {
        NamespacedKey nk = NamespacedKey.fromString(keyStr);
        if (nk == null) {
          throw new IllegalArgumentException("Invalid ingredient key: " + keyStr);
        }
        ingredients.add(new CauldronIngredient(nk));
      }
      return this;
    }

    public Builder result(Function<BrewContext, ItemStack> fn) {
      this.resultFn = fn;
      return this;
    }

    public Builder permission(String permission) {
      this.permission = permission;
      return this;
    }

    public Builder disabledModifier(String modifierKey) {
      this.disabledModifiers.add(modifierKey);
      return this;
    }

    public CustomRecipe build() {
      if (resultFn == null) {
        throw new IllegalStateException("resultFn must be set");
      }
      return new CustomRecipe(this);
    }
  }
}
