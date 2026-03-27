package org.aincraft.wizard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-safe registry of active {@link WizardSession} objects keyed by player UUID.
 */
public final class WizardSessionManager {

    private final Map<UUID, WizardSession> sessions = new HashMap<>();

    public void put(UUID playerId, WizardSession session) {
        sessions.put(playerId, session);
    }

    @Nullable
    public WizardSession get(UUID playerId) {
        return sessions.get(playerId);
    }

    public boolean has(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public void remove(UUID playerId) {
        sessions.remove(playerId);
    }
}
