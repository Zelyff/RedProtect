package br.net.fabiozumbi12.RedProtect.Bukkit.claim;

import br.net.fabiozumbi12.RedProtect.Bukkit.claim.ClaimSessionManager.Bounds2D;
import br.net.fabiozumbi12.RedProtect.Bukkit.claim.ClaimSessionManager.Bounds3D;
import br.net.fabiozumbi12.RedProtect.Bukkit.claim.ClaimSessionManager.ClaimSession;
import br.net.fabiozumbi12.RedProtect.Bukkit.claim.ClaimSessionManager.CornerAnchor;
import br.net.fabiozumbi12.RedProtect.Bukkit.claim.ClaimSessionManager.SessionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClaimSessionManagerTest {

    private record StubRegion(String name, int minX, int maxX, int minZ, int maxZ, int priority, boolean editable) {
    }

    @Test
    void wandModeParsesGpDirectVsNative() {
        assertTrue(ClaimSessionManager.isGpDirectMode("GP_DIRECT"));
        assertTrue(ClaimSessionManager.isGpDirectMode("gp_direct"));
        assertTrue(ClaimSessionManager.isGpDirectMode(null));
        assertTrue(ClaimSessionManager.isGpDirectMode(" "));
        assertFalse(ClaimSessionManager.isGpDirectMode("NATIVE"));
        assertFalse(ClaimSessionManager.isGpDirectMode("native"));
    }

    @Test
    void cornerDetectionAndOppositeAnchorWorkForAllFourCorners() {
        int minX = 10, maxX = 30, minZ = -20, maxZ = 5;

        assertTrue(ClaimSessionManager.isCorner(10, -20, minX, maxX, minZ, maxZ));
        assertTrue(ClaimSessionManager.isCorner(30, -20, minX, maxX, minZ, maxZ));
        assertTrue(ClaimSessionManager.isCorner(10, 5, minX, maxX, minZ, maxZ));
        assertTrue(ClaimSessionManager.isCorner(30, 5, minX, maxX, minZ, maxZ));

        // Edge midpoints and interior are NOT corners
        assertFalse(ClaimSessionManager.isCorner(20, -20, minX, maxX, minZ, maxZ));
        assertFalse(ClaimSessionManager.isCorner(10, 0, minX, maxX, minZ, maxZ));
        assertFalse(ClaimSessionManager.isCorner(20, 0, minX, maxX, minZ, maxZ));

        assertEquals(new CornerAnchor(30, 5), ClaimSessionManager.computeOppositeAnchor(10, -20, minX, maxX, minZ, maxZ));
        assertEquals(new CornerAnchor(10, 5), ClaimSessionManager.computeOppositeAnchor(30, -20, minX, maxX, minZ, maxZ));
        assertEquals(new CornerAnchor(30, -20), ClaimSessionManager.computeOppositeAnchor(10, 5, minX, maxX, minZ, maxZ));
        assertEquals(new CornerAnchor(10, -20), ClaimSessionManager.computeOppositeAnchor(30, 5, minX, maxX, minZ, maxZ));
    }

    @Test
    void pickEditableCornerPrefersHighestPriorityEditableRegion() {
        StubRegion parent = new StubRegion("parent", 0, 100, 0, 100, 1, true);
        StubRegion childAllowed = new StubRegion("child", 0, 20, 0, 20, 10, true);
        StubRegion lockedTop = new StubRegion("admin", 0, 20, 0, 20, 50, false);

        StubRegion picked = ClaimSessionManager.pickEditableCornerRegion(
                List.of(parent, lockedTop, childAllowed),
                r -> new Bounds2D(r.minX(), r.maxX(), r.minZ(), r.maxZ()),
                StubRegion::editable,
                StubRegion::priority,
                0,
                0
        );
        assertEquals("child", picked.name());

        StubRegion none = ClaimSessionManager.pickEditableCornerRegion(
                List.of(lockedTop),
                r -> new Bounds2D(r.minX(), r.maxX(), r.minZ(), r.maxZ()),
                StubRegion::editable,
                StubRegion::priority,
                0,
                0
        );
        assertNull(none);
    }

    @Test
    void sessionConsumeClearAndInFlightGuardIsolatePlayers() {
        ClaimSessionManager manager = new ClaimSessionManager();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        manager.startClaimSession(p1, "world", 10, 64, 20);
        manager.startResizeSession(p2, "world", "id-2", "home", 50, 64, 60, 55, 75);

        assertTrue(manager.hasSession(p1));
        assertTrue(manager.hasSession(p2));

        ClaimSession consumed = manager.consumeSession(p1);
        assertNotNull(consumed);
        assertEquals(SessionType.CLAIM, consumed.type());
        assertFalse(manager.hasSession(p1));
        assertNull(manager.consumeSession(p1));

        ClaimSession resize = manager.getSession(p2);
        assertNotNull(resize);
        assertEquals(SessionType.RESIZE, resize.type());
        assertEquals(55, resize.lockedMinY());
        assertEquals(75, resize.lockedMaxY());

        assertTrue(manager.tryAcquireInFlight(p1));
        assertFalse(manager.tryAcquireInFlight(p1));
        assertTrue(manager.isInFlight(p1));
        manager.releaseInFlight(p1);
        assertFalse(manager.isInFlight(p1));

        assertTrue(manager.clearSession(p2));
        assertFalse(manager.clearSession(p2));

        manager.startClaimSession(p1, "world", 1, 2, 3);
        manager.tryAcquireInFlight(p1);
        manager.clearAll();
        assertFalse(manager.hasSession(p1));
        assertFalse(manager.isInFlight(p1));
    }

    @Test
    void pickEditableCorner3D_disambiguatesVerticalStacksAndTiesByArea() {
        record StubRegion3D(String name, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, int prior, boolean editable) {
        }

        StubRegion3D ground = new StubRegion3D("ground", 0, 20, 0, 64, 0, 20, 5, true);
        StubRegion3D sky = new StubRegion3D("sky", 0, 20, 100, 200, 0, 20, 5, true);
        StubRegion3D innerGround = new StubRegion3D("innerGround", 0, 10, 0, 64, 0, 10, 5, true);

        StubRegion3D pickedSky = ClaimSessionManager.pickEditableCornerRegion(
                List.of(ground, sky, innerGround),
                r -> new Bounds3D(r.minX(), r.maxX(), r.minY(), r.maxY(), r.minZ(), r.maxZ()),
                StubRegion3D::editable,
                StubRegion3D::prior,
                0, 150, 0
        );
        assertEquals("sky", pickedSky.name());

        StubRegion3D pickedGroundSmallestArea = ClaimSessionManager.pickEditableCornerRegion(
                List.of(ground, sky, innerGround),
                r -> new Bounds3D(r.minX(), r.maxX(), r.minY(), r.maxY(), r.minZ(), r.maxZ()),
                StubRegion3D::editable,
                StubRegion3D::prior,
                0, 30, 0
        );
        assertEquals("innerGround", pickedGroundSmallestArea.name());

        StubRegion3D outsideYBand = ClaimSessionManager.pickEditableCornerRegion(
                List.of(ground, sky),
                r -> new Bounds3D(r.minX(), r.maxX(), r.minY(), r.maxY(), r.minZ(), r.maxZ()),
                StubRegion3D::editable,
                StubRegion3D::prior,
                0, 80, 0
        );
        assertNull(outsideYBand);
    }
}
