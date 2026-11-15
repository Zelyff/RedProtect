/*
 * Copyright (c) 2012-2026 - @FabioZumbi12
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 * damages arising from the use of this class.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.preview;

import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.BoundaryGeometryCalculator.BlockMarker;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.GlowOverlayBridge.GlowMarkerSpec;
import br.net.fabiozumbi12.RedProtect.Core.region.PlayerRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Manages per-viewer region boundary and claim-selection previews across {@code PARTICLE},
 * GriefPrevention-style {@code BLOCK} fake blocks, {@code HYBRID}, and optional Java-only {@code BlockDisplay} glow overlays.
 * Guarantees exact restoration of client-side fake blocks and removal of per-viewer display entities on all end paths.
 */
public final class RegionPreviewService {

    public interface SchedulerBridge {
        int scheduleRepeating(long delayTicks, long periodTicks, Runnable task);

        int scheduleLater(long delayTicks, Runnable task);

        void cancelTask(int taskId);
    }

    public interface ParticleBridge {
        String resolveParticleName(String configuredName);

        void spawn(World world, String particleName, double x, double y, double z);
    }

    @FunctionalInterface
    public interface BlockDataFactory {
        BlockData create(PreviewVisualType visualType, boolean corner);
    }

    @FunctionalInterface
    public interface BlockTransparencyChecker {
        boolean isTransparent(Block block, boolean waterTransparent);
    }

    public record PreviewSettings(
            PreviewRenderMode mode,
            String particle,
            int timeShowingSeconds,
            int borderHeight,
            String intensity,
            boolean glowOutline,
            int fakeBlockStep,
            int fakeBlockRadius,
            int fakeBlockMaxElements,
            int glowMaxEntities,
            String defaultLeader) {
    }

    public record FakeBlockEntry(int x, int y, int z, BlockData originalData, BlockData fakeData) {
    }

    public static final class ActivePreview {
        private final long token;
        private final UUID playerId;
        private final WeakReference<Player> playerRef;
        private final String worldName;
        private final PreviewRenderMode mode;
        private final PreviewVisualType visualType;
        private final List<FakeBlockEntry> fakeBlocks;
        private final List<Entity> glowEntities;
        private volatile int particleTaskId = -1;
        private volatile int timeoutTaskId = -1;

        ActivePreview(
                long token,
                Player player,
                String worldName,
                PreviewRenderMode mode,
                PreviewVisualType visualType,
                List<FakeBlockEntry> fakeBlocks,
                List<Entity> glowEntities) {
            this.token = token;
            this.playerId = player.getUniqueId();
            this.playerRef = new WeakReference<>(player);
            this.worldName = worldName;
            this.mode = mode;
            this.visualType = visualType;
            this.fakeBlocks = fakeBlocks;
            this.glowEntities = glowEntities;
        }

        Player resolveViewer() {
            Player cached = playerRef.get();
            if (cached != null) {
                return cached;
            }
            return Bukkit.getServer() != null ? Bukkit.getPlayer(playerId) : null;
        }

        public long token() {
            return token;
        }

        public UUID playerId() {
            return playerId;
        }

        public String worldName() {
            return worldName;
        }

        public PreviewRenderMode mode() {
            return mode;
        }

        public PreviewVisualType visualType() {
            return visualType;
        }

        public List<FakeBlockEntry> fakeBlocks() {
            return Collections.unmodifiableList(fakeBlocks);
        }

        public List<Entity> glowEntities() {
            return Collections.unmodifiableList(glowEntities);
        }

        public int particleTaskId() {
            return particleTaskId;
        }

        public int timeoutTaskId() {
            return timeoutTaskId;
        }
    }

    private final Plugin plugin;
    private final Supplier<PreviewSettings> settingsSupplier;
    private final SchedulerBridge scheduler;
    private final ParticleBridge particleBridge;
    private final GlowOverlayBridge glowOverlay;
    private final BlockDataFactory blockDataFactory;
    private final Predicate<UUID> bedrockPlayerCheck;
    private final BlockTransparencyChecker transparencyChecker;
    private final BoundaryGeometryCalculator geometry = new BoundaryGeometryCalculator();
    private final Map<UUID, ActivePreview> activePreviews = new ConcurrentHashMap<>();
    private final AtomicLong tokenGenerator = new AtomicLong();

    public RegionPreviewService(
            Plugin plugin,
            Supplier<PreviewSettings> settingsSupplier,
            SchedulerBridge scheduler,
            ParticleBridge particleBridge,
            GlowOverlayBridge glowOverlay,
            BlockDataFactory blockDataFactory,
            Predicate<UUID> bedrockPlayerCheck) {
        this(plugin, settingsSupplier, scheduler, particleBridge, glowOverlay, blockDataFactory, bedrockPlayerCheck, RegionPreviewService::isTransparent);
    }

    public RegionPreviewService(
            Plugin plugin,
            Supplier<PreviewSettings> settingsSupplier,
            SchedulerBridge scheduler,
            ParticleBridge particleBridge,
            GlowOverlayBridge glowOverlay,
            BlockDataFactory blockDataFactory,
            Predicate<UUID> bedrockPlayerCheck,
            BlockTransparencyChecker transparencyChecker) {
        this.plugin = plugin;
        this.settingsSupplier = Objects.requireNonNull(settingsSupplier, "settingsSupplier");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.particleBridge = Objects.requireNonNull(particleBridge, "particleBridge");
        this.glowOverlay = Objects.requireNonNull(glowOverlay, "glowOverlay");
        this.blockDataFactory = Objects.requireNonNull(blockDataFactory, "blockDataFactory");
        this.bedrockPlayerCheck = bedrockPlayerCheck != null ? bedrockPlayerCheck : uuid -> false;
        this.transparencyChecker = transparencyChecker != null ? transparencyChecker : RegionPreviewService::isTransparent;
    }

    /**
     * Reflectively detects whether Floodgate reports the viewer as a Bedrock client without hard-requiring Floodgate at compile/runtime.
     */
    public static boolean isFloodgatePlayer(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object instance = apiClass.getMethod("getInstance").invoke(null);
            if (instance == null) {
                return false;
            }
            Method checkMethod = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            Object result = checkMethod.invoke(instance, uuid);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public PreviewVisualType resolveRegionVisualType(Region region, PreviewSettings settings) {
        if (region == null) {
            return PreviewVisualType.CLAIM;
        }
        String defaultLeader = settings != null && settings.defaultLeader() != null ? settings.defaultLeader() : "#server#";
        for (PlayerRegion leader : region.getLeaders()) {
            if (leader != null && defaultLeader.equalsIgnoreCase(leader.getPlayerName())) {
                return PreviewVisualType.ADMIN_CLAIM;
            }
        }
        return PreviewVisualType.CLAIM;
    }

    public void showRegionPreview(Player player, Region region) {
        if (player == null || region == null) {
            return;
        }
        PreviewSettings settings = settingsSupplier.get();
        showRegionPreview(player, region, resolveRegionVisualType(region, settings));
    }

    public void showRegionPreview(Player player, Region region, PreviewVisualType visualType) {
        if (player == null || region == null || !player.isOnline()) {
            return;
        }
        String regionWorld = region.getWorld();
        if (regionWorld != null && !regionWorld.equalsIgnoreCase(player.getWorld().getName())) {
            return;
        }
        showBoundsPreview(
                player,
                player.getWorld(),
                region.getMinMbrX(),
                region.getMaxMbrX(),
                region.getMinMbrZ(),
                region.getMaxMbrZ(),
                player.getLocation().getBlockY(),
                visualType != null ? visualType : PreviewVisualType.CLAIM);
    }

    public void showSelectionPoint(Player player, Location loc, PreviewVisualType visualType) {
        if (player == null || loc == null || loc.getWorld() == null || !player.isOnline()) {
            return;
        }
        if (!Objects.equals(player.getWorld(), loc.getWorld())) {
            return;
        }
        showBoundsPreview(
                player,
                loc.getWorld(),
                loc.getBlockX(),
                loc.getBlockX(),
                loc.getBlockZ(),
                loc.getBlockZ(),
                loc.getBlockY(),
                visualType != null ? visualType : PreviewVisualType.INITIALIZE);
    }

    public void showBoundsPreview(
            Player player,
            World world,
            int boundMinX,
            int boundMaxX,
            int boundMinZ,
            int boundMaxZ,
            int anchorY,
            PreviewVisualType visualType) {
        if (player == null || world == null || !player.isOnline()) {
            return;
        }
        if (!Objects.equals(player.getWorld(), world)) {
            return;
        }

        // Always clean previous preview before computing new surface blocks
        clearPreview(player);

        PreviewSettings settings = settingsSupplier.get();
        PreviewRenderMode mode = settings.mode() != null ? settings.mode() : PreviewRenderMode.BLOCK;
        PreviewVisualType type = visualType != null ? visualType : PreviewVisualType.CLAIM;

        int minX = Math.min(boundMinX, boundMaxX);
        int maxX = Math.max(boundMinX, boundMaxX);
        int minZ = Math.min(boundMinZ, boundMaxZ);
        int maxZ = Math.max(boundMinZ, boundMaxZ);

        List<FakeBlockEntry> fakeBlocks = new ArrayList<>();
        List<Entity> glowEntities = new ArrayList<>();

        long token = tokenGenerator.incrementAndGet();
        ActivePreview session = new ActivePreview(token, player, world.getName(), mode, type, fakeBlocks, glowEntities);
        activePreviews.put(player.getUniqueId(), session);

        try {
            if (mode.usesFakeBlocks()) {
                List<BlockMarker> markers = geometry.computeMarkers(
                        minX,
                        maxX,
                        minZ,
                        maxZ,
                        player.getLocation().getBlockX(),
                        player.getLocation().getBlockZ(),
                        settings.fakeBlockStep(),
                        settings.fakeBlockRadius(),
                        settings.fakeBlockMaxElements());

                BlockData cornerData = blockDataFactory.create(type, true);
                BlockData sideData = blockDataFactory.create(type, false);
                boolean waterTransparent = isPlayerInWater(world, player.getLocation());

                List<BlockMarker> renderedMarkers = new ArrayList<>(markers.size());
                for (BlockMarker marker : markers) {
                    if (!world.isChunkLoaded(marker.x() >> 4, marker.z() >> 4)) {
                        continue;
                    }
                    Block surface = findVisibleBlock(world, marker.x(), anchorY, marker.z(), waterTransparent);
                    if (surface == null) {
                        continue;
                    }
                    BlockData originalData = surface.getBlockData();
                    BlockData fakeData = marker.corner() ? cornerData : sideData;
                    try {
                        player.sendBlockChange(surface.getLocation(), fakeData);
                        fakeBlocks.add(new FakeBlockEntry(surface.getX(), surface.getY(), surface.getZ(), originalData, fakeData));
                        renderedMarkers.add(marker);
                    } catch (Throwable ignored) {
                        // Best-effort per block; previous blocks remain tracked for rollback
                    }
                }

                if (settings.glowOutline()
                        && glowOverlay.isSupported()
                        && !fakeBlocks.isEmpty()
                        && !bedrockPlayerCheck.test(player.getUniqueId())) {
                    List<BlockMarker> glowMarkers = geometry.selectGlowMarkers(renderedMarkers, settings.glowMaxEntities());
                    List<GlowMarkerSpec> specs = new ArrayList<>(glowMarkers.size());
                    for (BlockMarker glowMarker : glowMarkers) {
                        for (FakeBlockEntry entry : fakeBlocks) {
                            if (entry.x() == glowMarker.x() && entry.z() == glowMarker.z()) {
                                specs.add(new GlowMarkerSpec(entry.x(), entry.y(), entry.z(), entry.fakeData()));
                                break;
                            }
                        }
                    }
                    List<Entity> spawned = glowOverlay.spawn(plugin, player, world, specs, type.getGlowColor());
                    if (spawned != null && !spawned.isEmpty()) {
                        glowEntities.addAll(spawned);
                    }
                }
            }

            if (mode.usesParticles()) {
                Set<Location> particleLocations = buildParticlePerimeter(
                        world,
                        minX,
                        maxX,
                        minZ,
                        maxZ,
                        player.getLocation().getBlockX(),
                        player.getLocation().getBlockY(),
                        player.getLocation().getBlockZ(),
                        Math.max(16, settings.fakeBlockRadius()),
                        settings.borderHeight(),
                        settings.intensity());

                String particleName = particleBridge.resolveParticleName(settings.particle());
                if (!particleLocations.isEmpty()) {
                    session.particleTaskId = scheduler.scheduleRepeating(10L, 10L, () -> {
                        if (!player.isOnline() || !Objects.equals(player.getWorld(), world)) {
                            clearPreview(player);
                            return;
                        }
                        for (Location loc : particleLocations) {
                            particleBridge.spawn(world, particleName, loc.getX() + 0.500, loc.getY(), loc.getZ() + 0.500);
                        }
                    });
                }
            }

            int durationSeconds = Math.max(1, settings.timeShowingSeconds());
            session.timeoutTaskId = scheduler.scheduleLater(durationSeconds * 20L, () -> {
                ActivePreview current = activePreviews.get(player.getUniqueId());
                if (current != null && current.token() == token) {
                    clearPreview(player);
                }
            });
        } catch (Throwable ex) {
            clearPreview(player);
            throw ex;
        }
    }

    public boolean hasActivePreview(UUID playerId) {
        return playerId != null && activePreviews.containsKey(playerId);
    }

    public ActivePreview getActivePreview(UUID playerId) {
        return playerId == null ? null : activePreviews.get(playerId);
    }

    public void clearPreview(Player player) {
        if (player == null) {
            return;
        }
        clearPreview(player.getUniqueId(), player);
    }

    public void clearPreview(UUID playerId) {
        if (playerId == null) {
            return;
        }
        Player onlinePlayer = Bukkit.getServer() != null ? Bukkit.getPlayer(playerId) : null;
        clearPreview(playerId, onlinePlayer);
    }

    private void clearPreview(UUID playerId, Player playerHint) {
        ActivePreview session = activePreviews.remove(playerId);
        if (session == null) {
            return;
        }

        if (session.particleTaskId != -1) {
            try {
                scheduler.cancelTask(session.particleTaskId);
            } catch (Throwable ignored) {
            }
            session.particleTaskId = -1;
        }

        if (session.timeoutTaskId != -1) {
            try {
                scheduler.cancelTask(session.timeoutTaskId);
            } catch (Throwable ignored) {
            }
            session.timeoutTaskId = -1;
        }

        try {
            Player viewer = playerHint != null ? playerHint : session.resolveViewer();
            if (viewer != null && viewer.isOnline() && viewer.getWorld().getName().equalsIgnoreCase(session.worldName)) {
                World world = viewer.getWorld();
                for (FakeBlockEntry entry : session.fakeBlocks) {
                    try {
                        if (world.isChunkLoaded(entry.x() >> 4, entry.z() >> 4)) {
                            Block current = world.getBlockAt(entry.x(), entry.y(), entry.z());
                            BlockData restoreData = (current != null && current.getBlockData() != null)
                                    ? current.getBlockData()
                                    : entry.originalData();
                            viewer.sendBlockChange(new Location(world, entry.x(), entry.y(), entry.z()), restoreData);
                        }
                    } catch (Throwable ignored) {
                        // Continue restoring remaining entries even if a single block throws
                    }
                }
            }
        } finally {
            try {
                Player viewer = playerHint != null ? playerHint : session.resolveViewer();
                glowOverlay.removeAll(plugin, viewer, session.glowEntities);
            } catch (Throwable ignored) {
            }
        }
    }

    public void clearWorld(String worldName) {
        if (worldName == null) {
            return;
        }
        for (Map.Entry<UUID, ActivePreview> entry : new ArrayList<>(activePreviews.entrySet())) {
            if (worldName.equalsIgnoreCase(entry.getValue().worldName())) {
                clearPreview(entry.getKey());
            }
        }
    }

    public void clearChunk(World world, int chunkX, int chunkZ) {
        if (world == null || activePreviews.isEmpty()) {
            return;
        }
        String worldName = world.getName();
        for (ActivePreview session : new ArrayList<>(activePreviews.values())) {
            if (!worldName.equalsIgnoreCase(session.worldName())) {
                continue;
            }
            boolean touchesChunk = false;
            for (FakeBlockEntry entry : session.fakeBlocks) {
                if ((entry.x() >> 4) == chunkX && (entry.z() >> 4) == chunkZ) {
                    touchesChunk = true;
                    break;
                }
            }
            if (touchesChunk) {
                clearPreview(session.playerId());
            }
        }
    }

    public void clearAll() {
        for (UUID uuid : new ArrayList<>(activePreviews.keySet())) {
            clearPreview(uuid);
        }
    }

    private static boolean isPlayerInWater(World world, Location playerLoc) {
        if (playerLoc == null) {
            return false;
        }
        int x = playerLoc.getBlockX();
        int y = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, playerLoc.getBlockY()));
        int z = playerLoc.getBlockZ();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return false;
        }
        return world.getBlockAt(x, y, z).getType() == Material.WATER;
    }

    /**
     * Clings boundary block vertically to the nearest visible ground/surface block around {@code startY},
     * following GriefPrevention Legacy v16 {@code FakeBlockVisualization#getVisibleLocation}.
     */
    Block findVisibleBlock(World world, int x, int startY, int z, boolean waterTransparent) {
        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight() - 1;
        if (maxHeight < minHeight) {
            return null;
        }
        int clampedY = Math.max(minHeight, Math.min(maxHeight, startY));
        Block block = world.getBlockAt(x, clampedY, z);
        if (block == null) {
            return null;
        }

        BlockFace direction = transparencyChecker.isTransparent(block, waterTransparent) ? BlockFace.DOWN : BlockFace.UP;
        int maxSteps = Math.max(64, (maxHeight - minHeight) + 2);
        int steps = 0;

        while (steps++ < maxSteps
                && block.getY() >= minHeight
                && block.getY() < maxHeight
                && (!transparencyChecker.isTransparent(block.getRelative(BlockFace.UP), waterTransparent)
                || transparencyChecker.isTransparent(block, waterTransparent))) {
            if (direction == BlockFace.DOWN && block.getY() <= minHeight) {
                break;
            }
            if (direction == BlockFace.UP && block.getY() >= maxHeight) {
                break;
            }
            Block next = block.getRelative(direction);
            if (next == null) {
                break;
            }
            block = next;
        }

        return block;
    }

    static boolean isTransparent(Block block, boolean waterTransparent) {
        if (block == null) {
            return true;
        }
        Material material = block.getType();
        if (material == Material.WATER) {
            return waterTransparent;
        }
        if (material == Material.SNOW) {
            return false;
        }
        if (material.isAir()
                || Tag.FENCES.isTagged(material)
                || Tag.FENCE_GATES.isTagged(material)
                || Tag.SIGNS.isTagged(material)
                || Tag.WALLS.isTagged(material)
                || Tag.WALL_SIGNS.isTagged(material)) {
            return true;
        }
        return material.isTransparent();
    }

    /**
     * Builds perimeter particle locations clipped to viewer radius so huge regions never iterate O(width * length).
     */
    Set<Location> buildParticlePerimeter(
            World world,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            int viewerX,
            int viewerY,
            int viewerZ,
            int clipRadius,
            int borderHeight,
            String intensityRaw) {
        Set<Location> locations = new LinkedHashSet<>();
        String intense = intensityRaw != null ? intensityRaw : "NORMAL";
        boolean low = intense.equalsIgnoreCase("LOW");
        boolean normal = intense.equalsIgnoreCase("NORMAL");

        int minY = Math.max(world.getMinHeight(), viewerY - Math.max(1, borderHeight));
        int maxY = Math.min(world.getMaxHeight() - 1, viewerY + Math.max(1, borderHeight) - 1);

        int zoneMinX = viewerX - clipRadius;
        int zoneMaxX = viewerX + clipRadius;
        int zoneMinZ = viewerZ - clipRadius;
        int zoneMaxZ = viewerZ + clipRadius;

        if (maxX < zoneMinX || minX > zoneMaxX || maxZ < zoneMinZ || minZ > zoneMaxZ) {
            return locations;
        }

        int clampedMinX = Math.max(minX, zoneMinX);
        int clampedMaxX = Math.min(maxX, zoneMaxX);
        int clampedMinZ = Math.max(minZ, zoneMinZ);
        int clampedMaxZ = Math.min(maxZ, zoneMaxZ);

        // North & South edges (z = minZ, z = maxZ)
        for (int x = clampedMinX; x <= clampedMaxX; x++) {
            if (low && x % 2 != 0) {
                continue;
            }
            if (minZ >= zoneMinZ && minZ <= zoneMaxZ) {
                addParticleColumn(locations, world, x, minZ, minY, maxY, low || normal);
            }
            if (maxZ != minZ && maxZ >= zoneMinZ && maxZ <= zoneMaxZ) {
                addParticleColumn(locations, world, x, maxZ, minY, maxY, low || normal);
            }
        }

        // West & East edges (x = minX, x = maxX)
        for (int z = clampedMinZ; z <= clampedMaxZ; z++) {
            if (low && z % 2 != 0) {
                continue;
            }
            if (minX >= zoneMinX && minX <= zoneMaxX) {
                addParticleColumn(locations, world, minX, z, minY, maxY, low || normal);
            }
            if (maxX != minX && maxX >= zoneMinX && maxX <= zoneMaxX) {
                addParticleColumn(locations, world, maxX, z, minY, maxY, low || normal);
            }
        }

        return locations;
    }

    private static void addParticleColumn(Set<Location> locations, World world, int x, int z, int minY, int maxY, boolean evenYOnly) {
        for (int y = minY; y <= maxY; y++) {
            if (evenYOnly && y % 2 != 0) {
                continue;
            }
            locations.add(new Location(world, x, y, z));
        }
    }
}
