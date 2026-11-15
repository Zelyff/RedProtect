/*
 * Copyright (c) 2012-2026 - @FabioZumbi12
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 * damages arising from the use of this class.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.preview;

import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Abstracts optional Java-only glow outline rendering so tests and legacy servers remain decoupled from NMS/1.19.4+ classes.
 */
public interface GlowOverlayBridge {

    record GlowMarkerSpec(int x, int y, int z, BlockData blockData) {
    }

    boolean isSupported();

    List<Entity> spawn(Plugin plugin, Player viewer, World world, List<GlowMarkerSpec> markers, Color glowColor);

    void removeAll(Plugin plugin, Player viewer, List<Entity> entities);
}
