package br.net.fabiozumbi12.RedProtect.Bukkit.actions;

import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RegionCreationSupportTest {

    @Test
    void cuboidsIntersect_respects3DBoundsInclusiveAndWorldNames() {
        assertTrue(RegionCreationSupport.cuboidsIntersect(0, 10, 60, 70, 0, 10, 10, 20, 70, 80, 10, 20));
        assertFalse(RegionCreationSupport.cuboidsIntersect(0, 10, 0, 60, 0, 10, 0, 10, 61, 100, 0, 10));
        assertFalse(RegionCreationSupport.cuboidsIntersect(0, 10, 0, 100, 0, 10, 11, 20, 0, 100, 0, 10));

        Region a = Mockito.mock(Region.class);
        Region b = Mockito.mock(Region.class);
        when(a.getWorld()).thenReturn("world");
        when(b.getWorld()).thenReturn("world_nether");
        when(a.getMinMbrX()).thenReturn(0);
        when(a.getMaxMbrX()).thenReturn(10);
        when(b.getMinMbrX()).thenReturn(0);
        when(b.getMaxMbrX()).thenReturn(10);
        assertFalse(RegionCreationSupport.cuboidsIntersect(a, b));
    }

    @Test
    void cuboidContains_validatesParentChildContainmentIn3D() {
        assertTrue(RegionCreationSupport.cuboidContains(0, 100, -64, 320, 0, 100, 10, 50, 60, 80, 10, 50));
        assertFalse(RegionCreationSupport.cuboidContains(10, 90, -64, 320, 0, 100, 0, 50, 60, 80, 10, 50));
        assertFalse(RegionCreationSupport.cuboidContains(0, 100, 10, 100, 0, 100, 10, 50, 5, 80, 10, 50));
    }

    @Test
    void costCalculations_useInclusiveVolumeAndClampOverflow() {
        // 10x10 area (100 blocks), height 60..69 (10 blocks inclusive) -> 1000 * 5 = 5000
        assertEquals(5000L, RegionCreationSupport.calculateCreateCost(100, 60, 69, 5L, false));
        assertEquals(500L, RegionCreationSupport.calculateCreateCost(100, 60, 69, 5L, true));

        // Expand height by 5 layers on 100 area -> +500 units * 2 = +1000
        assertEquals(1000L, RegionCreationSupport.calculateRedefineCostDelta(100, 60, 69, 100, 60, 74, 2L, false));
        // Shrink height -> negative delta refund
        assertEquals(-1000L, RegionCreationSupport.calculateRedefineCostDelta(100, 60, 74, 100, 60, 69, 2L, false));
        // Overflow clamps safely instead of wrapping negative
        assertEquals(Long.MAX_VALUE, RegionCreationSupport.calculateCreateCost(Integer.MAX_VALUE, -64, 320, Long.MAX_VALUE, false));
    }

    @Test
    void isInteractiveBlock_identifiesContainersDoorsAndSwitches() {
        assertTrue(RegionCreationSupport.isInteractiveBlock(Material.CHEST));
        assertTrue(RegionCreationSupport.isInteractiveBlock(Material.BARREL));
        assertTrue(RegionCreationSupport.isInteractiveBlock(Material.OAK_DOOR));
        assertTrue(RegionCreationSupport.isInteractiveBlock(Material.STONE_BUTTON));
        assertTrue(RegionCreationSupport.isInteractiveBlock(Material.LEVER));
        assertFalse(RegionCreationSupport.isInteractiveBlock(Material.STONE));
        assertFalse(RegionCreationSupport.isInteractiveBlock(Material.GRASS_BLOCK));
        assertFalse(RegionCreationSupport.isInteractiveBlock(null));
    }
}
