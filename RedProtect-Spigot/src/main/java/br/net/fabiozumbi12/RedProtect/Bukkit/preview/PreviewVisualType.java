/*
 * Copyright (c) 2012-2026 - @FabioZumbi12
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 * damages arising from the use of this class.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.preview;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;

/**
 * Client-side marker block palette matching GriefPrevention Legacy v16 {@code FakeBlockVisualization}.
 */
public enum PreviewVisualType {
    CLAIM(Material.GLOWSTONE, Material.GOLD_BLOCK, false, Color.fromRGB(255, 215, 0)),
    ADMIN_CLAIM(Material.GLOWSTONE, Material.PUMPKIN, false, Color.ORANGE),
    SUBDIVISION(Material.IRON_BLOCK, Material.WHITE_WOOL, false, Color.WHITE),
    INITIALIZE(Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, false, Color.AQUA),
    CONFLICT(Material.REDSTONE_ORE, Material.NETHERRACK, true, Color.RED);

    private final Material cornerMaterial;
    private final Material sideMaterial;
    private final boolean litCorner;
    private final Color glowColor;

    PreviewVisualType(Material cornerMaterial, Material sideMaterial, boolean litCorner, Color glowColor) {
        this.cornerMaterial = cornerMaterial;
        this.sideMaterial = sideMaterial;
        this.litCorner = litCorner;
        this.glowColor = glowColor;
    }

    public Material getCornerMaterial() {
        return cornerMaterial;
    }

    public Material getSideMaterial() {
        return sideMaterial;
    }

    public boolean isLitCorner() {
        return litCorner;
    }

    public Color getGlowColor() {
        return glowColor;
    }

    /**
     * Creates Bukkit {@link BlockData} for a corner or side marker. Requires a running Bukkit/Paper server.
     */
    public BlockData createBlockData(boolean corner) {
        Material mat = corner ? cornerMaterial : sideMaterial;
        BlockData data = mat.createBlockData();
        if (corner && litCorner && data instanceof Lightable lightable) {
            lightable.setLit(true);
        }
        return data;
    }
}
