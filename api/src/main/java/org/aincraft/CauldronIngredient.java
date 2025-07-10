package org.aincraft;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public final class CauldronIngredient {

  @Expose
  @NotNull
  @SerializedName("item-key")
  private NamespacedKey itemKey;

  @Expose
  @SerializedName("amount")
  private int amount;

  public CauldronIngredient(@NotNull NamespacedKey itemKey,
      int amount) {
    this.itemKey = itemKey;
    this.amount = amount;
  }

  public CauldronIngredient(@NotNull NamespacedKey itemKey) {
    this(itemKey,1);
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

    return amount == that.amount && itemKey.equals(((CauldronIngredient) obj).itemKey);
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  @Override
  public String toString() {
    return "Ingredient{" +
        "item-key=" + itemKey +
        ", amount=" + amount +
        '}';
  }

  public CauldronIngredient deepCopy() {
    return new CauldronIngredient(itemKey, amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itemKey, amount);
  }

  public int getAmount() {
    return amount;
  }

  public @NotNull NamespacedKey getItemKey() {
    return itemKey;
  }

  public boolean isSimilar(@NotNull CauldronIngredient other) {
    return other.getItemKey().equals(itemKey);
  }

  public boolean isMaterial() {
    return NamespacedKey.MINECRAFT.equals(itemKey.getNamespace());
  }
}
