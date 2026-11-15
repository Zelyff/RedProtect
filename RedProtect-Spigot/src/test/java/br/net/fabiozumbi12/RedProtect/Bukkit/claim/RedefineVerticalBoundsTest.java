package br.net.fabiozumbi12.RedProtect.Bukkit.claim;

import br.net.fabiozumbi12.RedProtect.Bukkit.actions.RedefineRegionBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class RedefineVerticalBoundsTest {

    @Test
    void gpCornerResizePreservesCustomVerticalBoundsEvenWithAutoExpandEnabled() {
        int[] bounds = RedefineRegionBuilder.resolveVerticalBounds(
                64, 70,
                60, 80,
                -64, 320,
                -1, -1,
                true,
                true
        );
        assertArrayEquals(new int[]{60, 80}, bounds);
    }

    @Test
    void gpCornerResizePreservesCustomBoundsEvenWhenConfigClaimBoundsSet() {
        int[] bounds = RedefineRegionBuilder.resolveVerticalBounds(
                64, 70,
                50, 90,
                -64, 320,
                0, 256,
                true,
                true
        );
        assertArrayEquals(new int[]{50, 90}, bounds);
    }

    @Test
    void nativeRedefineExpandsVerticallyWhenAutoExpandIsTrue() {
        int[] bounds = RedefineRegionBuilder.resolveVerticalBounds(
                64, 70,
                60, 80,
                -64, 320,
                -1, -1,
                true,
                false
        );
        assertArrayEquals(new int[]{-64, 320}, bounds);

        int[] configured = RedefineRegionBuilder.resolveVerticalBounds(
                64, 70,
                60, 80,
                -64, 320,
                10, 200,
                true,
                false
        );
        assertArrayEquals(new int[]{10, 200}, configured);
    }

    @Test
    void nativeRedefineUsesSelectionHeightsWhenAutoExpandIsFalse() {
        int[] bounds = RedefineRegionBuilder.resolveVerticalBounds(
                85, 62,
                60, 80,
                -64, 320,
                -1, -1,
                false,
                false
        );
        assertArrayEquals(new int[]{62, 85}, bounds);
    }
}
