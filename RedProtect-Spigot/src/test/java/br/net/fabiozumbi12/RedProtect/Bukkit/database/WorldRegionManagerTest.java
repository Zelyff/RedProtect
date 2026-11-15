package br.net.fabiozumbi12.RedProtect.Bukkit.database;

import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorldRegionManagerTest {

    private static Region mockRegion(String name, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, int prior, int area) {
        Region r = Mockito.mock(Region.class);
        when(r.getName()).thenReturn(name);
        when(r.getMinMbrX()).thenReturn(minX);
        when(r.getMaxMbrX()).thenReturn(maxX);
        when(r.getMinY()).thenReturn(minY);
        when(r.getMaxY()).thenReturn(maxY);
        when(r.getMinMbrZ()).thenReturn(minZ);
        when(r.getMaxMbrZ()).thenReturn(maxZ);
        when(r.getPrior()).thenReturn(prior);
        when(r.getArea()).thenReturn(area);
        return r;
    }

    @Test
    void precedenceTieBreaking_prefersSmallerAreaOnTopWithoutMutatingPriority() {
        Region large = mockRegion("large", 0, 100, -64, 320, 0, 100, 5, 10000);
        Region small = mockRegion("small", 10, 20, -64, 320, 10, 20, 5, 100);

        Region top = WorldRegionManager.selectTopRegion(List.of(large, small), 15, 64, 15);
        Region low = WorldRegionManager.selectLowRegion(List.of(large, small), 15, 64, 15);

        assertSame(small, top);
        assertSame(large, low);

        Map<Integer, Region> grouped = WorldRegionManager.buildGroupRegionMap(List.of(large, small), 15, 64, 15);
        assertEquals(2, grouped.size());
        assertSame(large, grouped.get(5));
        assertSame(small, grouped.get(6));

        verify(large, never()).setPrior(anyInt());
        verify(small, never()).setPrior(anyInt());
    }

    @Test
    void containsPoint_checksInclusive3DBounds() {
        Region r = mockRegion("box", 0, 10, 60, 70, 0, 10, 0, 121);
        assertTrue(WorldRegionManager.containsPoint(r, 0, 60, 0));
        assertTrue(WorldRegionManager.containsPoint(r, 10, 70, 10));
        assertFalse(WorldRegionManager.containsPoint(r, 10, 71, 10));
        assertFalse(WorldRegionManager.containsPoint(r, -1, 65, 5));
    }
}
