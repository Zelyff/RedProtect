package br.net.fabiozumbi12.RedProtect.Bukkit.preview;

import br.net.fabiozumbi12.RedProtect.Bukkit.preview.GlowOverlayBridge.GlowMarkerSpec;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.RegionPreviewService.BlockDataFactory;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.RegionPreviewService.ParticleBridge;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.RegionPreviewService.PreviewSettings;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.RegionPreviewService.SchedulerBridge;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegionPreviewServiceTest {

    private Plugin plugin;
    private World world;
    private Player player;
    private UUID playerId;
    private BlockData originalGroundData;
    private BlockData cornerFakeData;
    private BlockData sideFakeData;
    private FakeScheduler scheduler;
    private GlowOverlayBridge glowOverlay;
    private ParticleBridge particleBridge;
    private PreviewSettings currentSettings;
    private boolean viewerIsBedrock;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        world = mock(World.class);
        player = mock(Player.class);
        playerId = UUID.randomUUID();
        originalGroundData = mock(BlockData.class);
        cornerFakeData = mock(BlockData.class);
        sideFakeData = mock(BlockData.class);
        scheduler = new FakeScheduler();
        glowOverlay = mock(GlowOverlayBridge.class);
        particleBridge = mock(ParticleBridge.class);
        viewerIsBedrock = false;

        when(glowOverlay.isSupported()).thenReturn(true);
        when(glowOverlay.spawn(any(), any(), any(), anyList(), any())).thenAnswer(inv -> {
            List<GlowMarkerSpec> specs = inv.getArgument(3);
            List<Entity> created = new ArrayList<>(specs.size());
            for (int i = 0; i < specs.size(); i++) {
                created.add(mock(Entity.class));
            }
            return created;
        });

        when(particleBridge.resolveParticleName(anyString())).thenAnswer(inv -> inv.getArgument(0));

        when(world.getName()).thenReturn("world");
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);

        when(player.getUniqueId()).thenReturn(playerId);
        when(player.isOnline()).thenReturn(true);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 5, 65, 5));

        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
            int x = inv.getArgument(0);
            int y = inv.getArgument(1);
            int z = inv.getArgument(2);
            return mockSurfaceColumn(x, y, z, 64);
        });

        currentSettings = settings(PreviewRenderMode.BLOCK, true);
    }

    private RegionPreviewService createService() {
        BlockDataFactory factory = (type, corner) -> corner ? cornerFakeData : sideFakeData;
        return new RegionPreviewService(
                plugin,
                () -> currentSettings,
                scheduler,
                particleBridge,
                glowOverlay,
                factory,
                uuid -> viewerIsBedrock,
                (block, waterTransparent) -> {
                    Material type = block.getType();
                    return type == Material.AIR || (waterTransparent && type == Material.WATER);
                }
        );
    }

    @Test
    void blockModeSendsFakeBlocksSpawnsGlowAndRestoresOriginalDataOnTimeout() {
        RegionPreviewService service = createService();
        service.showBoundsPreview(player, world, 0, 2, 0, 2, 65, PreviewVisualType.CLAIM);

        assertTrue(service.hasActivePreview(playerId));
        var active = service.getActivePreview(playerId);
        assertEquals(8, active.fakeBlocks().size());
        assertEquals(8, active.glowEntities().size());
        assertEquals(-1, active.particleTaskId());
        assertTrue(active.timeoutTaskId() > 0);

        verify(player, times(4)).sendBlockChange(any(Location.class), eq(cornerFakeData));
        verify(player, times(4)).sendBlockChange(any(Location.class), eq(sideFakeData));
        verify(glowOverlay).spawn(eq(plugin), eq(player), eq(world), argThat(list -> list.size() == 8), eq(PreviewVisualType.CLAIM.getGlowColor()));

        // Trigger scheduled auto-timeout
        scheduler.runLater(active.timeoutTaskId());

        assertFalse(service.hasActivePreview(playerId));
        verify(player, times(8)).sendBlockChange(any(Location.class), eq(originalGroundData));
        verify(glowOverlay).removeAll(eq(plugin), eq(player), eq(active.glowEntities()));
    }

    @Test
    void replacingPreviewRestoresOldBlocksAndIgnoresStaleTimeoutToken() {
        RegionPreviewService service = createService();
        service.showBoundsPreview(player, world, 0, 2, 0, 2, 65, PreviewVisualType.INITIALIZE);
        var first = service.getActivePreview(playerId);
        int firstTimeout = first.timeoutTaskId();

        // Replace with a second preview before timeout fires
        service.showBoundsPreview(player, world, 10, 10, 10, 10, 65, PreviewVisualType.CONFLICT);
        var second = service.getActivePreview(playerId);

        assertTrue(scheduler.cancelledTasks.contains(firstTimeout));
        verify(player, times(8)).sendBlockChange(any(Location.class), eq(originalGroundData));
        verify(glowOverlay).removeAll(eq(plugin), eq(player), eq(first.glowEntities()));
        assertEquals(1, second.fakeBlocks().size());

        // Even if stale timeout runnable is executed manually, second session must stay alive
        scheduler.runLaterUnchecked(firstTimeout);
        assertTrue(service.hasActivePreview(playerId));
        assertEquals(second.token(), service.getActivePreview(playerId).token());
    }

    @Test
    void particleModeCancelsRepeatingTaskAndNeverSendsFakeBlocksOrGlow() {
        currentSettings = settings(PreviewRenderMode.PARTICLE, true);
        RegionPreviewService service = createService();

        service.showBoundsPreview(player, world, 0, 4, 0, 4, 65, PreviewVisualType.CLAIM);
        var active = service.getActivePreview(playerId);

        assertTrue(active.fakeBlocks().isEmpty());
        assertTrue(active.glowEntities().isEmpty());
        assertTrue(active.particleTaskId() > 0);
        verify(player, never()).sendBlockChange(any(), any(BlockData.class));
        verify(glowOverlay, never()).spawn(any(), any(), any(), any(), any());

        scheduler.runRepeating(active.particleTaskId());
        verify(particleBridge, atLeastOnce()).spawn(eq(world), eq("FLAME"), anyDouble(), anyDouble(), anyDouble());

        int particleTaskId = active.particleTaskId();
        service.clearPreview(player);
        assertFalse(service.hasActivePreview(playerId));
        assertTrue(scheduler.cancelledTasks.contains(particleTaskId));
    }

    @Test
    void hybridModeRendersFakeBlocksGlowAndParticlesTogether() {
        currentSettings = settings(PreviewRenderMode.HYBRID, true);
        RegionPreviewService service = createService();

        service.showBoundsPreview(player, world, 0, 2, 0, 2, 65, PreviewVisualType.CLAIM);
        var active = service.getActivePreview(playerId);
        int particleTaskId = active.particleTaskId();
        int timeoutTaskId = active.timeoutTaskId();

        assertEquals(8, active.fakeBlocks().size());
        assertEquals(8, active.glowEntities().size());
        assertTrue(particleTaskId > 0);

        service.clearAll();
        assertFalse(service.hasActivePreview(playerId));
        assertTrue(scheduler.cancelledTasks.contains(particleTaskId));
        assertTrue(scheduler.cancelledTasks.contains(timeoutTaskId));
        verify(player, times(8)).sendBlockChange(any(Location.class), eq(originalGroundData));
        verify(glowOverlay).removeAll(eq(plugin), eq(player), eq(active.glowEntities()));
    }

    @Test
    void bedrockViewerSkipsGlowEntitiesButReceivesFakeBlocks() {
        viewerIsBedrock = true;
        RegionPreviewService service = createService();

        service.showBoundsPreview(player, world, 0, 2, 0, 2, 65, PreviewVisualType.CLAIM);
        var active = service.getActivePreview(playerId);

        assertEquals(8, active.fakeBlocks().size());
        assertTrue(active.glowEntities().isEmpty());
        verify(glowOverlay, never()).spawn(any(), any(), any(), any(), any());
    }

    @Test
    void cleanupRestoresRemainingBlocksAndGlowEvenWhenOneRestoreThrows() {
        RegionPreviewService service = createService();
        service.showBoundsPreview(player, world, 0, 1, 0, 1, 65, PreviewVisualType.CLAIM);
        var active = service.getActivePreview(playerId);

        // Make restoration throw on one location
        doThrow(new RuntimeException("Simulated client block sync error"))
                .when(player).sendBlockChange(eq(new Location(world, 0, 64, 1)), eq(originalGroundData));

        service.clearPreview(player);

        assertFalse(service.hasActivePreview(playerId));
        // Other 3 corners + glow cleanup still ran
        verify(player, times(4)).sendBlockChange(any(Location.class), eq(originalGroundData));
        verify(glowOverlay).removeAll(eq(plugin), eq(player), eq(active.glowEntities()));
    }

    @Test
    void unloadedChunksAreSkippedWithoutReadingBlocks() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        RegionPreviewService service = createService();

        service.showBoundsPreview(player, world, 0, 2, 0, 2, 65, PreviewVisualType.CLAIM);
        assertTrue(service.getActivePreview(playerId).fakeBlocks().isEmpty());
        verify(player, never()).sendBlockChange(any(), any(BlockData.class));
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }

    @Test
    void worldUnloadAndChunkUnloadClearAffectedPreviews() {
        RegionPreviewService service = createService();
        service.showBoundsPreview(player, world, 0, 2, 0, 2, 65, PreviewVisualType.CLAIM);

        service.clearChunk(world, 50, 50); // unrelated chunk
        assertTrue(service.hasActivePreview(playerId));

        service.clearChunk(world, 0, 0); // chunk containing markers (0..2, 0..2)
        assertFalse(service.hasActivePreview(playerId));

        service.showBoundsPreview(player, world, 0, 2, 0, 2, 65, PreviewVisualType.CLAIM);
        service.clearWorld("world");
        assertFalse(service.hasActivePreview(playerId));
    }

    private Block mockSurfaceColumn(int x, int startY, int z, int groundY) {
        Map<Integer, Block> cache = new HashMap<>();
        return blockAt(cache, x, startY, z, groundY);
    }

    private Block blockAt(Map<Integer, Block> cache, int x, int y, int z, int groundY) {
        return cache.computeIfAbsent(y, keyY -> {
            Block b = mock(Block.class);
            when(b.getX()).thenReturn(x);
            when(b.getY()).thenReturn(keyY);
            when(b.getZ()).thenReturn(z);
            when(b.getLocation()).thenReturn(new Location(world, x, keyY, z));
            Material mat = keyY > groundY ? Material.AIR : Material.STONE;
            when(b.getType()).thenReturn(mat);
            when(b.getBlockData()).thenReturn(originalGroundData);
            when(b.getRelative(any(BlockFace.class))).thenAnswer(inv -> {
                BlockFace face = inv.getArgument(0);
                return blockAt(cache, x + face.getModX(), keyY + face.getModY(), z + face.getModZ(), groundY);
            });
            return b;
        });
    }

    private static PreviewSettings settings(PreviewRenderMode mode, boolean glowOutline) {
        return new PreviewSettings(
                mode,
                "FLAME",
                10,
                5,
                "NORMAL",
                glowOutline,
                10,
                75,
                400,
                48,
                "#server#"
        );
    }

    private static final class FakeScheduler implements SchedulerBridge {
        private final AtomicInteger nextId = new AtomicInteger(1);
        private final Map<Integer, Runnable> repeating = new HashMap<>();
        private final Map<Integer, Runnable> delayed = new HashMap<>();
        private final List<Integer> cancelledTasks = new ArrayList<>();

        @Override
        public int scheduleRepeating(long delayTicks, long periodTicks, Runnable task) {
            int id = nextId.getAndIncrement();
            repeating.put(id, task);
            return id;
        }

        @Override
        public int scheduleLater(long delayTicks, Runnable task) {
            int id = nextId.getAndIncrement();
            delayed.put(id, task);
            return id;
        }

        @Override
        public void cancelTask(int taskId) {
            cancelledTasks.add(taskId);
            repeating.remove(taskId);
        }

        void runRepeating(int taskId) {
            Runnable r = repeating.get(taskId);
            if (r != null) {
                r.run();
            }
        }

        void runLater(int taskId) {
            Runnable r = delayed.remove(taskId);
            if (r != null && !cancelledTasks.contains(taskId)) {
                r.run();
            }
        }

        void runLaterUnchecked(int taskId) {
            Runnable r = delayed.get(taskId);
            if (r != null) {
                r.run();
            }
        }
    }
}
