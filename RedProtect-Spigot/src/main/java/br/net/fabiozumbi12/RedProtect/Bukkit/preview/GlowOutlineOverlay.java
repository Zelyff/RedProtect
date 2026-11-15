/*
 * Copyright (c) 2012-2026 - @FabioZumbi12
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 * damages arising from the use of this class.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.preview;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional Java-only (Bukkit/Paper 1.19.4+) glowing {@link BlockDisplay} outline overlay visible through blocks.
 * Lazy inner-class isolation guarantees zero {@link NoClassDefFoundError} on older servers where {@code BlockDisplay} does not exist.
 */
public final class GlowOutlineOverlay implements GlowOverlayBridge {

    private static final boolean SUPPORTED = detectSupport();

    @Override
    public boolean isSupported() {
        return SUPPORTED;
    }

    @Override
    public List<Entity> spawn(Plugin plugin, Player viewer, World world, List<GlowMarkerSpec> markers, Color glowColor) {
        if (!SUPPORTED || plugin == null || viewer == null || world == null || markers == null || markers.isEmpty()) {
            return List.of();
        }
        return Adapter.spawnAll(plugin, viewer, world, markers, glowColor);
    }

    @Override
    public void removeAll(Plugin plugin, Player viewer, List<Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (Entity entity : entities) {
            if (entity == null) {
                continue;
            }
            if (viewer != null && viewer.isOnline() && plugin != null) {
                try {
                    viewer.hideEntity(plugin, entity);
                } catch (Throwable ignored) {
                }
            }
            try {
                entity.remove();
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean detectSupport() {
        try {
            Class.forName("org.bukkit.entity.BlockDisplay");
            Player.class.getMethod("showEntity", Plugin.class, Entity.class);
            Entity.class.getMethod("setVisibleByDefault", boolean.class);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class Adapter {
        private static List<Entity> spawnAll(
                Plugin plugin,
                Player viewer,
                World world,
                List<GlowMarkerSpec> markers,
                Color glowColor) {
            List<Entity> spawned = new ArrayList<>(markers.size());
            Display.Brightness fullBright = new Display.Brightness(15, 15);

            for (GlowMarkerSpec spec : markers) {
                if (!world.isChunkLoaded(spec.x() >> 4, spec.z() >> 4)) {
                    continue;
                }
                Location loc = new Location(world, spec.x(), spec.y(), spec.z());
                try {
                    BlockDisplay display = world.spawn(loc, BlockDisplay.class, entity -> {
                        entity.setBlock(spec.blockData());
                        entity.setVisibleByDefault(false);
                        entity.setPersistent(false);
                        entity.setGlowing(true);
                        if (glowColor != null) {
                            entity.setGlowColorOverride(glowColor);
                        }
                        entity.setViewRange(2.0f);
                        entity.setBrightness(fullBright);
                    });
                    if (display != null) {
                        viewer.showEntity(plugin, display);
                        spawned.add(display);
                    }
                } catch (Throwable ignored) {
                    // Graceful degradation if runtime server/mock does not implement BlockDisplay spawning
                    break;
                }
            }
            return spawned;
        }
    }
}
