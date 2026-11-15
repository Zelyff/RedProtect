/*
 * Copyright (c) 2012-2026 - @FabioZumbi12
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 * damages arising from the use of this class.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.claim;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Manages temporary GriefPrevention-style claim and corner-resize sessions cleanly separated from
 * RedProtect's native {@code firstLocationSelections}/{@code secondLocationSelections} wand maps.
 */
public final class ClaimSessionManager {

    public enum SessionType {
        CLAIM,
        RESIZE
    }

    public record ClaimSession(
            SessionType type,
            String worldName,
            String regionId,
            String regionName,
            int anchorX,
            int anchorY,
            int anchorZ,
            int lockedMinY,
            int lockedMaxY,
            long startedAtMillis) {
    }

    public record Bounds2D(int minX, int maxX, int minZ, int maxZ) {
    }

    public record Bounds3D(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        public long area() {
            return (long) (Math.max(0, maxX - minX) + 1) * (long) (Math.max(0, maxZ - minZ) + 1);
        }
    }

    public record CornerAnchor(int oppositeX, int oppositeZ) {
    }

    private final Map<UUID, ClaimSession> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    public static boolean isGpDirectMode(String configuredWandMode) {
        if (configuredWandMode == null || configuredWandMode.isBlank()) {
            return true;
        }
        return "GP_DIRECT".equalsIgnoreCase(configuredWandMode.trim());
    }

    public static boolean isCorner(int clickX, int clickZ, int minX, int maxX, int minZ, int maxZ) {
        return (clickX == minX || clickX == maxX) && (clickZ == minZ || clickZ == maxZ);
    }

    public static boolean isCorner(
            int clickX,
            int clickY,
            int clickZ,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ) {
        return isCorner(clickX, clickZ, minX, maxX, minZ, maxZ)
                && clickY >= Math.min(minY, maxY)
                && clickY <= Math.max(minY, maxY);
    }

    public static CornerAnchor computeOppositeAnchor(int clickX, int clickZ, int minX, int maxX, int minZ, int maxZ) {
        int oppX = (clickX == minX) ? maxX : minX;
        int oppZ = (clickZ == minZ) ? maxZ : minZ;
        return new CornerAnchor(oppX, oppZ);
    }

    /**
     * Finds the highest-priority editable candidate whose 2D corner matches {@code (clickX, clickZ)}.
     */
    public static <T> T pickEditableCornerRegion(
            Iterable<T> candidates,
            Function<T, Bounds2D> boundsExtractor,
            Predicate<T> editablePredicate,
            ToIntFunction<T> priorityExtractor,
            int clickX,
            int clickZ) {
        if (candidates == null) {
            return null;
        }
        T best = null;
        int bestPriority = Integer.MIN_VALUE;
        for (T candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            Bounds2D bounds = boundsExtractor.apply(candidate);
            if (bounds == null) {
                continue;
            }
            if (!isCorner(clickX, clickZ, bounds.minX(), bounds.maxX(), bounds.minZ(), bounds.maxZ())) {
                continue;
            }
            if (!editablePredicate.test(candidate)) {
                continue;
            }
            int priority = priorityExtractor.applyAsInt(candidate);
            if (best == null || priority > bestPriority) {
                best = candidate;
                bestPriority = priority;
            }
        }
        return best;
    }

    /**
     * Finds the highest-priority (tie-broken by smallest footprint area) editable candidate whose 3D corner
     * matches {@code (clickX, clickY, clickZ)}.
     */
    public static <T> T pickEditableCornerRegion(
            Iterable<T> candidates,
            Function<T, Bounds3D> boundsExtractor,
            Predicate<T> editablePredicate,
            ToIntFunction<T> priorityExtractor,
            int clickX,
            int clickY,
            int clickZ) {
        if (candidates == null) {
            return null;
        }
        T best = null;
        int bestPriority = Integer.MIN_VALUE;
        long bestArea = Long.MAX_VALUE;
        for (T candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            Bounds3D bounds = boundsExtractor.apply(candidate);
            if (bounds == null) {
                continue;
            }
            if (!isCorner(clickX, clickY, clickZ, bounds.minX(), bounds.maxX(), bounds.minY(), bounds.maxY(), bounds.minZ(), bounds.maxZ())) {
                continue;
            }
            if (!editablePredicate.test(candidate)) {
                continue;
            }
            int priority = priorityExtractor.applyAsInt(candidate);
            long area = bounds.area();
            if (best == null || priority > bestPriority || (priority == bestPriority && area < bestArea)) {
                best = candidate;
                bestPriority = priority;
                bestArea = area;
            }
        }
        return best;
    }

    public void startClaimSession(UUID playerId, String worldName, int x, int y, int z) {
        if (playerId == null || worldName == null) {
            return;
        }
        sessions.put(playerId, new ClaimSession(
                SessionType.CLAIM,
                worldName,
                null,
                null,
                x,
                y,
                z,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                System.currentTimeMillis()
        ));
    }

    public void startResizeSession(
            UUID playerId,
            String worldName,
            String regionId,
            String regionName,
            int oppositeX,
            int anchorY,
            int oppositeZ,
            int lockedMinY,
            int lockedMaxY) {
        if (playerId == null || worldName == null) {
            return;
        }
        sessions.put(playerId, new ClaimSession(
                SessionType.RESIZE,
                worldName,
                regionId,
                regionName,
                oppositeX,
                anchorY,
                oppositeZ,
                lockedMinY,
                lockedMaxY,
                System.currentTimeMillis()
        ));
    }

    public ClaimSession getSession(UUID playerId) {
        return playerId == null ? null : sessions.get(playerId);
    }

    public boolean hasSession(UUID playerId) {
        return playerId != null && sessions.containsKey(playerId);
    }

    public ClaimSession consumeSession(UUID playerId) {
        return playerId == null ? null : sessions.remove(playerId);
    }

    public boolean clearSession(UUID playerId) {
        return playerId != null && sessions.remove(playerId) != null;
    }

    public boolean tryAcquireInFlight(UUID playerId) {
        return playerId != null && inFlight.add(playerId);
    }

    public boolean isInFlight(UUID playerId) {
        return playerId != null && inFlight.contains(playerId);
    }

    public void releaseInFlight(UUID playerId) {
        if (playerId != null) {
            inFlight.remove(playerId);
        }
    }

    public void clearAll() {
        sessions.clear();
        inFlight.clear();
    }
}
