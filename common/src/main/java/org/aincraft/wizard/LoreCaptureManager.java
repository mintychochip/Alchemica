package org.aincraft.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Manages lore-line capture sessions.
 * When active, AsyncPlayerChatEvent messages are intercepted (cancelled) and
 * accumulated. A blank message ends capture and invokes the completion callback
 * with the collected lines.
 */
public final class LoreCaptureManager implements Listener {

    private static final class CaptureState {
        final List<String> lines = new ArrayList<>();
        final Consumer<List<String>> onComplete;

        CaptureState(Consumer<List<String>> onComplete) {
            this.onComplete = onComplete;
        }
    }

    private final Map<UUID, CaptureState> active = new HashMap<>();

    /**
     * Starts capture for {@code player}. {@code onComplete} is called on the main thread
     * once the player sends a blank line.
     */
    public void start(Player player, Consumer<List<String>> onComplete) {
        active.put(player.getUniqueId(), new CaptureState(onComplete));
        player.sendMessage("[Alchemica] Type lore lines. Send a blank message to finish.");
    }

    public boolean isCapturing(UUID playerId) {
        return active.containsKey(playerId);
    }

    public void cancel(UUID playerId) {
        active.remove(playerId);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        CaptureState state = active.get(id);
        if (state == null) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        if (message.isEmpty()) {
            active.remove(id);
            List<String> collected = new ArrayList<>(state.lines);
            // Schedule callback on main thread
            event.getPlayer().getServer().getScheduler().runTask(
                event.getPlayer().getServer().getPluginManager()
                    .getPlugin("Alchemica"),
                () -> state.onComplete.accept(collected)
            );
        } else {
            state.lines.add(message);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
    }
}
