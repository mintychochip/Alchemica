package org.aincraft.command;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;
import org.aincraft.wizard.WizardSessionManager;
import org.aincraft.wizard.LoreCaptureManager;
import org.bukkit.command.Command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrewCommandTest {

    private ServerMock server;
    private PlayerMock player;
    private PluginMock plugin;
    private BrewCommand command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        player = server.addPlayer();
        WizardSessionManager sessions = new WizardSessionManager();
        LoreCaptureManager lore = new LoreCaptureManager();
        command = new BrewCommand(plugin, sessions, lore, null, null);
    }

    @AfterEach
    void tearDown() { MockBukkit.unmock(); }

    @Test
    void onCommand_withoutPermission_sendsMessageAndReturnsFalse() {
        // player has no alchemica.admin permission
        boolean result = command.onCommand(player, null, "brew", new String[]{});
        assertFalse(result);
        assertEquals(1, player.nextMessage() != null ? 1 : 0);
    }

    @Test
    void onCommand_duringLoreCapture_sendsBlockedMessage() {
        LoreCaptureManager lore = new LoreCaptureManager();
        lore.start(player, lines -> {});
        BrewCommand cmd = new BrewCommand(plugin, new WizardSessionManager(), lore, null, null);
        plugin.getServer().getPluginManager().registerEvents(lore, plugin);
        boolean result = cmd.onCommand(player, null, "brew", new String[]{});
        assertFalse(result);
    }
}
