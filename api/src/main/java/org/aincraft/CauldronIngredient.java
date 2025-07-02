package org.aincraft;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public final class CauldronIngredient implements
    Keyed {

  @Expose
  @SerializedName("item-key")
  private final Key key;
  @Expose
  @SerializedName("amount")
  private int amount;

  public CauldronIngredient(Key key,
      int amount) {
    this.key = key;
    this.amount = amount;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    CauldronIngredient that = (CauldronIngredient) obj;

    return amount == that.amount && key.equals(((CauldronIngredient) obj).key);
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  @Override
  public String toString() {
    return "Ingredient{" +
        "key=" + key +
        ", amount=" + amount +
        '}';
  }

  public static CauldronIngredient fromMaterial(Material material) {
    return new CauldronIngredient(material.getKey(), 1);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, amount);
  }

  @Override
  public @NotNull Key key() {
    return key;
  }

  public void updateAmount(Function<Integer,Integer> update) {
    amount = update.apply(amount);
  }

  public int getAmount() {
    return amount;
  }

  public CauldronIngredient deepCopy() {
    return new CauldronIngredient(key,amount);
  }
}
