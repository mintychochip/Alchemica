package org.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.aincraft.CauldronIngredient;
import org.aincraft.IPotionResult;
import org.aincraft.IPotionResult.Status;
import org.aincraft.dao.IPlayerSettings;
import org.aincraft.internal.PotionResult.PotionContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
final class Trie {

  private static final Function<Status, IPotionResult> FAILED_RESULT = status -> new PotionResult(
      status, null, null);
  private final Node root;

  Trie(Node root) {
    this.root = root;
  }

  @NotNull
  public IPotionResult search(IPlayerSettings playerSettings,
      Collection<CauldronIngredient> ingredients)
      throws IllegalArgumentException {
    Player player = Bukkit.getPlayer(playerSettings.getPlayerId());
    Preconditions.checkArgument(player != null);
    int effectCount = playerSettings.getEffectCount().intValue();
    int modifierCount = playerSettings.getModifierCount().intValue();
    Node node = root;
    PotionContext context = new PotionContext();
    List<CauldronIngredient> working = ingredients.stream().map(CauldronIngredient::deepCopy)
        .collect(Collectors.toList());
    int index = 0;
    while (index < working.size()) {
      CauldronIngredient current = working.get(index);
      Node child = node.search(current);
      if (child == null) {
        return FAILED_RESULT.apply(Status.BAD_RECIPE_PATH);
      }

      @Nullable String permission = child.getPermission();
      if (!player.hasPermission(permission)) {
        return FAILED_RESULT.apply(Status.NO_PERMISSION);
      }
      NodeType type = child.getType();
      if (type == NodeType.EFFECT) {
        effectCount--;
      }
      if (type == NodeType.MODIFIER) {
        modifierCount--;
      }
      if (effectCount < 0) {
        return FAILED_RESULT.apply(Status.MANY_EFFECTS);
      }
      if (modifierCount < 0) {
        return FAILED_RESULT.apply(Status.MANY_MODS);
      }
      CauldronIngredient required = child.getIngredient();
      current.setAmount(current.getAmount() - required.getAmount());
      child.getConsumer().accept(context);
      node = child;
      if (current.getAmount() == 0) {
        index++;
      }
    }
    return fromContext(context);
  }

  private static PotionResult fromContext(@NotNull PotionResult.PotionContext context) {
    ItemStack itemStack = new ItemStack(context.potionMaterial);
    ItemMeta itemMeta = itemStack.getItemMeta();

    PotionMeta potionMeta = (PotionMeta) itemMeta;
    if (context.potionTypeConsumer != null) {
      context.potionTypeConsumer.accept(potionMeta);
    }

    String prefix = context.potionMaterial == Material.SPLASH_POTION ? "Splash"
        : context.potionMaterial == Material.LINGERING_POTION ? "Lingering" : "";
    if (context.potionkey != null) {
      String base =
          createPotionName(context.potionkey);
      if (!prefix.isEmpty()) {
        base = prefix + " " + base;
      }
      potionMeta.setDisplayName(base);
    } else {
      PotionData basePotionData = potionMeta.getBasePotionData();
      if (basePotionData != null) {
        PotionType potionType = basePotionData.getType();
        if (potionType != PotionType.WATER) {
          String typeString = potionType.toString().toLowerCase(Locale.ENGLISH);
          String potionName = Character.toUpperCase(typeString.charAt(0))
              + typeString.substring(1)
              + ' '
              + prefix
              + "Potion";
          potionMeta.setDisplayName(potionName);
        } else {
          potionMeta.setDisplayName(prefix + " Water Bottle");
        }
      }
    }

    for (Entry<PotionEffectType, PotionEffectMeta> entry : context.potionMetaMap.entrySet()) {
      PotionEffectMeta meta = entry.getValue();
      PotionEffectType type = entry.getKey();
      context.metaConsumer.accept(meta);
      potionMeta.addCustomEffect(new PotionEffect(type,
          meta.getDuration().getTicks(),
          meta.getAmplifier(),
          meta.isAmbient(),
          meta.isParticles()), true);
    }
    itemStack.setItemMeta(potionMeta);
    return new PotionResult(Status.SUCCESS, itemStack, null);
  }

  private static String createPotionName(@NotNull NamespacedKey potionKey) {
    String name = potionKey.getKey();
    String[] splitName = name.split("_");
    StringBuilder potionNameBuilder = new StringBuilder("Potion of ");
    if ("turtle_master".equals(name)) {
      potionNameBuilder.append("the ");
    }
    for (String fragment : splitName) {
      potionNameBuilder.append(Character.toUpperCase(fragment.charAt(0)))
          .append(fragment.substring(1)).append(' ');
    }
    String potionName = potionNameBuilder.toString();
    return potionName.substring(0, potionName.length() - 1);
  }
}
