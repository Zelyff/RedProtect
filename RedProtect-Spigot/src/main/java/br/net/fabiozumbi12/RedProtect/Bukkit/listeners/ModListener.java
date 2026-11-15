/*
 * Copyright (c) 2012-2025 - @FabioZumbi12
 * Last Modified: 24/06/2025 19:02
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 *  damages arising from the use of this class.
 *
 * Permission is granted to anyone to use this class for any purpose, including commercial plugins, and to alter it and
 * redistribute it freely, subject to the following restrictions:
 * 1 - The origin of this class must not be misrepresented; you must not claim that you wrote the original software. If you
 * use this class in other plugins, an acknowledgment in the plugin documentation would be appreciated but is not required.
 * 2 - Altered source versions must be plainly marked as such, and must not be misrepresented as being the original class.
 * 3 - This notice may not be removed or altered from any source distribution.
 *
 * Esta classe é fornecida "como está", sem qualquer garantia expressa ou implícita. Em nenhum caso os autores serão
 * responsabilizados por quaisquer danos decorrentes do uso desta classe.
 *
 * É concedida permissão a qualquer pessoa para usar esta classe para qualquer finalidade, incluindo plugins pagos, e para
 * alterá-lo e redistribuí-lo livremente, sujeito às seguintes restrições:
 * 1 - A origem desta classe não deve ser deturpada; você não deve afirmar que escreveu a classe original. Se você usar esta
 *  classe em um plugin, uma confirmação de autoria na documentação do plugin será apreciada, mas não é necessária.
 * 2 - Versões de origem alteradas devem ser claramente marcadas como tal e não devem ser deturpadas como sendo a
 * classe original.
 * 3 - Este aviso não pode ser removido ou alterado de qualquer distribuição de origem.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.listeners;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

public class ModListener implements PluginMessageListener {

    private final RedProtect plugin;

    String MCBRAND = "MC|Brand";

    public ModListener(RedProtect plugin) {
        this.plugin = plugin;

        try {
            // If newer versions
            Class.forName("org.bukkit.entity.Dolphin");
            MCBRAND = "minecraft:brand";

            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, MCBRAND, this);
            plugin.getConfigManager().configRoot().server_protection.mods_permissions.forEach((k,v) -> {
                if (v.registerPacket){
                    v.modId.forEach(i -> {
                        try{
                            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, i, this);
                            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, i);
                        } catch (Exception ex){
                            RedProtect.get().logger.severe("Error on register MOD packet listener: " + ex.getMessage());
                        }
                    });
                }
            });

            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
        } catch (ClassNotFoundException ex) {
            RedProtect.get().logger.severe("This server don't allow packet listener for MOD restrictions: " + ex.getMessage());
        }
    }

    public void unload() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] value) {
        if (player == null || channel == null || player.hasPermission("redprotect.mods.bypass")) {
            return;
        }
        final boolean isBrandChannel = channel.equalsIgnoreCase(MCBRAND);
        final String brand = isBrandChannel && value != null
                ? new String(value, StandardCharsets.UTF_8)
                : "";
        final String brandLower = brand.toLowerCase(java.util.Locale.ROOT);
        plugin.getConfigManager().configRoot().server_protection.mods_permissions.forEach((k, v) -> {
            if (!v.block || v.modId == null) {
                return;
            }
            v.modId.forEach(i -> {
                if (i == null || i.isEmpty() || player.hasPermission("redprotect.mods." + i + ".bypass")) {
                    return;
                }
                if (isBrandChannel && brandLower.contains(i.toLowerCase(java.util.Locale.ROOT))) {
                    if (v.registerPacket && v.bytes != null) {
                        v.bytes.forEach(b -> sendModPayload(player, i, b));
                    }
                    runModAction(v.action, player, i);
                } else if (!isBrandChannel && channel.equalsIgnoreCase(i)) {
                    if (v.bytes != null) {
                        v.bytes.forEach(b -> sendModPayload(player, channel, b));
                    }
                    runModAction(v.action, player, i);
                }
            });
        });
    }

    private void sendModPayload(Player player, String targetChannel, String rawSpec) {
        if (rawSpec == null || rawSpec.isEmpty() || targetChannel == null || !targetChannel.contains(":")) {
            return;
        }
        if (!plugin.getServer().getMessenger().isOutgoingChannelRegistered(plugin, targetChannel)) {
            return;
        }
        byte[] payload;
        if (rawSpec.startsWith("base64:")) {
            try {
                payload = java.util.Base64.getDecoder().decode(rawSpec.substring("base64:".length()));
            } catch (IllegalArgumentException ex) {
                payload = rawSpec.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            payload = rawSpec.getBytes(StandardCharsets.UTF_8);
        }
        player.sendPluginMessage(plugin, targetChannel, payload);
    }

    private void runModAction(String action, Player player, String modName) {
        if (action == null || action.isEmpty() || !plugin.isEnabled()) {
            return;
        }
        String resolved = action
                .replace("{p}", player.getName())
                .replace("{mod}", modName);
        if (plugin.getServer().isPrimaryThread()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), resolved);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), resolved));
        }
    }
}
