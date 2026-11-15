/*
 * Copyright (c) 2012-2026 - @FabioZumbi12
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 * damages arising from the use of this class.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.preview;

import java.util.Locale;

/**
 * Configures how region boundaries and claim selections are displayed to players.
 */
public enum PreviewRenderMode {
    PARTICLE,
    BLOCK,
    HYBRID;

    /**
     * Parses a configured border mode string safely, falling back to {@code fallback} when unset or invalid.
     */
    public static PreviewRenderMode fromConfig(String raw, PreviewRenderMode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback != null ? fallback : BLOCK;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (PreviewRenderMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return fallback != null ? fallback : BLOCK;
    }

    public boolean usesParticles() {
        return this == PARTICLE || this == HYBRID;
    }

    public boolean usesFakeBlocks() {
        return this == BLOCK || this == HYBRID;
    }
}
