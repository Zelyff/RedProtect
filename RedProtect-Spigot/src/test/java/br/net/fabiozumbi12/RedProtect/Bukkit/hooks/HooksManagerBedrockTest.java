package br.net.fabiozumbi12.RedProtect.Bukkit.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class HooksManagerBedrockTest {

    @Test
    void checkFloodgateAndIsBedrockPlayer_returnFalseSafelyWhenPluginAbsent() {
        PluginManager pluginManager = mock(PluginManager.class);
        Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.getPlugin("floodgate")).thenReturn(null);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        HooksManager hooks = new HooksManager();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            assertFalse(hooks.checkFloodgate());
            assertFalse(hooks.isBedrockPlayer(player));
            assertFalse(hooks.isBedrockPlayer(null));
        }
    }

    @Test
    void checkFloodgate_returnsTrueOnlyWhenFloodgatePluginIsEnabled() {
        PluginManager pluginManager = mock(PluginManager.class);
        Plugin floodgatePlugin = mock(Plugin.class);
        when(pluginManager.getPlugin("floodgate")).thenReturn(floodgatePlugin);
        when(floodgatePlugin.isEnabled()).thenReturn(true);

        HooksManager hooks = new HooksManager();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            assertTrue(hooks.checkFloodgate());

            when(floodgatePlugin.isEnabled()).thenReturn(false);
            assertFalse(hooks.checkFloodgate());
        }
    }
}
