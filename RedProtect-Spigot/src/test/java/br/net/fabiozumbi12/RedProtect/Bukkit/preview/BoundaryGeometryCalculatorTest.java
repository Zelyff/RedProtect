package br.net.fabiozumbi12.RedProtect.Bukkit.preview;

import br.net.fabiozumbi12.RedProtect.Bukkit.preview.BoundaryGeometryCalculator.BlockMarker;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BoundaryGeometryCalculatorTest {

    private final BoundaryGeometryCalculator calculator = new BoundaryGeometryCalculator();

    @Test
    void singlePointProducesSingleCornerMarker() {
        List<BlockMarker> markers = calculator.computeMarkers(10, 10, 20, 20, 10, 20, 10, 75, 400);
        assertEquals(List.of(new BlockMarker(10, 20, true)), markers);
    }

    @Test
    void threeByThreeClaimProducesCornersAndMidpointsWithoutDuplicates() {
        List<BlockMarker> markers = calculator.computeMarkers(0, 2, 0, 2, 1, 1, 10, 75, 400);
        assertEquals(8, markers.size());

        long cornerCount = markers.stream().filter(BlockMarker::corner).count();
        assertEquals(4, cornerCount);

        Set<BlockMarker> unique = new HashSet<>(markers);
        assertEquals(8, unique.size());
    }

    @Test
    void mediumClaimIncludesAdjacentAndSteppedSideMarkers() {
        List<BlockMarker> markers = calculator.computeMarkers(0, 30, 0, 30, 15, 15, 10, 75, 400);

        // 4 corners + 8 corner-adjacent (1 and 29 on each side) + 8 stepped (10 and 20 on each side) = 20
        assertEquals(20, markers.size());
        assertEquals(4, markers.stream().filter(BlockMarker::corner).count());
        assertTrue(markers.contains(new BlockMarker(0, 0, true)));
        assertTrue(markers.contains(new BlockMarker(1, 0, false)));
        assertTrue(markers.contains(new BlockMarker(10, 0, false)));
        assertTrue(markers.contains(new BlockMarker(20, 30, false)));
        assertTrue(markers.contains(new BlockMarker(30, 29, false)));
    }

    @Test
    void giantClaimIsClippedToRadiusAndRespectsElementCap() {
        List<BlockMarker> markers = calculator.computeMarkers(-500, 500, -500, 500, 490, 490, 10, 75, 20);

        assertFalse(markers.isEmpty());
        assertTrue(markers.size() <= 20);
        // Corner (500, 500) is inside display radius and must survive prioritization
        assertTrue(markers.contains(new BlockMarker(500, 500, true)));
        // All returned markers must fall inside [viewer - 75, viewer + 75]
        for (BlockMarker marker : markers) {
            assertTrue(marker.x() >= 415 && marker.x() <= 565);
            assertTrue(marker.z() >= 415 && marker.z() <= 565);
        }
    }

    @Test
    void claimOutsideViewerRadiusReturnsEmpty() {
        List<BlockMarker> markers = calculator.computeMarkers(0, 20, 0, 20, 500, 500, 10, 75, 400);
        assertTrue(markers.isEmpty());
    }

    @Test
    void selectGlowMarkersKeepsCornersFirstAndRespectsMaxCap() {
        List<BlockMarker> markers = calculator.computeMarkers(0, 200, 0, 200, 100, 100, 5, 120, 400);
        assertTrue(markers.size() > 48);

        List<BlockMarker> glowMarkers = calculator.selectGlowMarkers(markers, 48);
        assertEquals(48, glowMarkers.size());
        assertEquals(4, glowMarkers.stream().filter(BlockMarker::corner).count());
    }
}
