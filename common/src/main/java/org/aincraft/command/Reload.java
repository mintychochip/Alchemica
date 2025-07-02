package org.aincraft.command;

import org.aincraft.internal.Brew;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class Reload implements CommandExecutor {

  private final Brew brew;

  public Reload(Brew brew) {
    this.brew = brew;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String label, @NotNull String[] args) {
    brew.refresh();
    return false;
  }
}
