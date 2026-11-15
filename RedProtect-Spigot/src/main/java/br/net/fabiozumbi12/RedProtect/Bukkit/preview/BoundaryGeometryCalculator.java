/*
 * Copyright (c) 2012-2026 - @FabioZumbi12
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 * damages arising from the use of this class.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure 2D boundary marker sampling matching GriefPrevention Legacy v16 {@code BlockBoundaryVisualization} rules
 * with strict display radius and element count caps.
 */
public final class BoundaryGeometryCalculator {

    public record BlockMarker(int x, int z, boolean corner) {
    }

    private record ColumnKey(int x, int z) {
    }

    /**
     * Computes boundary fake-block marker columns inside the viewer's display radius.
     * Corners are prioritized first, followed by corner-adjacent blocks and stepped wall markers.
     */
    public List<BlockMarker> computeMarkers(
            int boundMinX,
            int boundMaxX,
            int boundMinZ,
            int boundMaxZ,
            int viewerX,
            int viewerZ,
            int step,
            int radius,
            int maxElements) {

        int minX = Math.min(boundMinX, boundMaxX);
        int maxX = Math.max(boundMinX, boundMaxX);
        int minZ = Math.min(boundMinZ, boundMaxZ);
        int maxZ = Math.max(boundMinZ, boundMaxZ);

        int safeStep = Math.max(2, step);
        int safeRadius = Math.max(1, radius);
        int safeMax = Math.max(4, maxElements);

        int zoneMinX = viewerX - safeRadius;
        int zoneMaxX = viewerX + safeRadius;
        int zoneMinZ = viewerZ - safeRadius;
        int zoneMaxZ = viewerZ + safeRadius;

        if (maxX < zoneMinX || minX > zoneMaxX || maxZ < zoneMinZ || minZ > zoneMaxZ) {
            return List.of();
        }

        Map<ColumnKey, BlockMarker> corners = new LinkedHashMap<>();
        Map<ColumnKey, BlockMarker> adjacent = new LinkedHashMap<>();
        Map<ColumnKey, BlockMarker> stepped = new LinkedHashMap<>();

        // Tier 1: Corners inside displayZone
        addIfInZone(corners, minX, maxZ, true, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
        addIfInZone(corners, maxX, maxZ, true, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
        addIfInZone(corners, minX, minZ, true, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
        addIfInZone(corners, maxX, minZ, true, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);

        // Tier 2: Corner-adjacent markers on X walls
        if (maxX - minX >= 2) {
            addIfNotCorner(adjacent, corners, minX + 1, maxZ, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
            addIfNotCorner(adjacent, corners, minX + 1, minZ, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
            addIfNotCorner(adjacent, corners, maxX - 1, maxZ, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
            addIfNotCorner(adjacent, corners, maxX - 1, minZ, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
        }

        // Tier 2: Corner-adjacent markers on Z walls
        if (maxZ - minZ >= 2) {
            addIfNotCorner(adjacent, corners, minX, minZ + 1, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
            addIfNotCorner(adjacent, corners, maxX, minZ + 1, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
            addIfNotCorner(adjacent, corners, minX, maxZ - 1, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
            addIfNotCorner(adjacent, corners, maxX, maxZ - 1, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
        }

        // Tier 3: Stepped X wall markers
        int halfStep = safeStep / 2;
        int startX = alignedStart(minX, safeStep, zoneMinX);
        int endX = Math.min(maxX - halfStep - 1, zoneMaxX);
        for (int x = startX; x <= endX; x += safeStep) {
            addIfNotPresent(stepped, corners, adjacent, x, maxZ, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
            addIfNotPresent(stepped, corners, adjacent, x, minZ, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
        }

        // Tier 3: Stepped Z wall markers
        int startZ = alignedStart(minZ, safeStep, zoneMinZ);
        int endZ = Math.min(maxZ - halfStep - 1, zoneMaxZ);
        for (int z = startZ; z <= endZ; z += safeStep) {
            addIfNotPresent(stepped, corners, adjacent, minX, z, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
            addIfNotPresent(stepped, corners, adjacent, maxX, z, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
        }

        List<BlockMarker> result = new ArrayList<>(Math.min(safeMax, corners.size() + adjacent.size() + stepped.size()));
        appendUpToCap(result, corners.values(), safeMax);
        appendUpToCap(result, adjacent.values(), safeMax);
        appendSampled(result, new ArrayList<>(stepped.values()), safeMax);

        return Collections.unmodifiableList(result);
    }

    /**
     * Selects up to {@code maxGlowEntities} markers for Java-only glow outlines, prioritizing corners first
     * and evenly striding across remaining markers if budget remains.
     */
    public List<BlockMarker> selectGlowMarkers(List<BlockMarker> markers, int maxGlowEntities) {
        if (markers == null || markers.isEmpty() || maxGlowEntities <= 0) {
            return List.of();
        }
        List<BlockMarker> corners = new ArrayList<>(4);
        List<BlockMarker> sides = new ArrayList<>(markers.size());
        for (BlockMarker marker : markers) {
            if (marker.corner()) {
                corners.add(marker);
            } else {
                sides.add(marker);
            }
        }

        List<BlockMarker> selected = new ArrayList<>(Math.min(maxGlowEntities, markers.size()));
        appendUpToCap(selected, corners, maxGlowEntities);
        appendSampled(selected, sides, maxGlowEntities);
        return Collections.unmodifiableList(selected);
    }

    private static int alignedStart(int minCoord, int step, int zoneMinCoord) {
        int first = minCoord + step;
        if (zoneMinCoord <= first) {
            return first;
        }
        int offset = zoneMinCoord - minCoord;
        int steps = (offset + step - 1) / step;
        return minCoord + steps * step;
    }

    private static void addIfInZone(
            Map<ColumnKey, BlockMarker> target,
            int x,
            int z,
            boolean corner,
            int zoneMinX,
            int zoneMaxX,
            int zoneMinZ,
            int zoneMaxZ) {
        if (x < zoneMinX || x > zoneMaxX || z < zoneMinZ || z > zoneMaxZ) {
            return;
        }
        ColumnKey key = new ColumnKey(x, z);
        BlockMarker existing = target.get(key);
        if (existing == null || (corner && !existing.corner())) {
            target.put(key, new BlockMarker(x, z, corner));
        }
    }

    private static void addIfNotCorner(
            Map<ColumnKey, BlockMarker> target,
            Map<ColumnKey, BlockMarker> corners,
            int x,
            int z,
            int zoneMinX,
            int zoneMaxX,
            int zoneMinZ,
            int zoneMaxZ) {
        ColumnKey key = new ColumnKey(x, z);
        if (!corners.containsKey(key)) {
            addIfInZone(target, x, z, false, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
        }
    }

    private static void addIfNotPresent(
            Map<ColumnKey, BlockMarker> target,
            Map<ColumnKey, BlockMarker> corners,
            Map<ColumnKey, BlockMarker> adjacent,
            int x,
            int z,
            int zoneMinX,
            int zoneMaxX,
            int zoneMinZ,
            int zoneMaxZ) {
        ColumnKey key = new ColumnKey(x, z);
        if (!corners.containsKey(key) && !adjacent.containsKey(key)) {
            addIfInZone(target, x, z, false, zoneMinX, zoneMaxX, zoneMinZ, zoneMaxZ);
        }
    }

    private static void appendUpToCap(List<BlockMarker> out, Iterable<BlockMarker> source, int cap) {
        for (BlockMarker marker : source) {
            if (out.size() >= cap) {
                return;
            }
            out.add(marker);
        }
    }

    private static void appendSampled(List<BlockMarker> out, List<BlockMarker> source, int cap) {
        int remaining = cap - out.size();
        if (remaining <= 0 || source.isEmpty()) {
            return;
        }
        if (source.size() <= remaining) {
            out.addAll(source);
            return;
        }
        double stride = (double) source.size() / remaining;
        for (int i = 0; i < remaining; i++) {
            int idx = Math.min(source.size() - 1, (int) Math.floor(i * stride));
            out.add(source.get(idx));
        }
    }
}
