/*
 * Copyright (c) 2012-2025 - @FabioZumbi12
 * Last Modified: 2025
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 *  damages arising from the use of this class.
 *
 * Permission is granted to anyone to use this class for any purpose, including commercial plugins, and to alter it and
 * redistribute it freely, subject to the following restrictions:
 * 1 - The origin of this class must not be misrepresented; you must not claim that you wrote the original software. If you
 * use this class in other plugins, an acknowledgment in the plugin documentation would be appreciated but is not required.
 * 2 - Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
 * 3 - This notice may not be removed or altered from any source distribution.
 *
 * Esta classe é fornecida "como está", sem qualquer garantia expressa ou implícita. Em nenhum caso os autores serão
 * responsabilizados por quaisquer danos decorrentes do uso desta classe.
 *
 * É concedida permissão a qualquer pessoa para usar esta classe para qualquer finalidade, incluindo plugins comerciais, e para
 * alterá-la e redistribuí-la livremente, sujeito às seguintes restrições:
 * 1 - A origem desta classe não deve ser deturpada; você não pode afirmar que escreveu o software original. Se você usar esta
 * classe em outros plugins, um reconhecimento na documentação do plugin seria apreciado, mas não é obrigatório.
 * 2 - Versões de origem alteradas devem ser claramente marcadas como tal e não devem ser deturpadas como sendo o software original.
 * 3 - Este aviso não pode ser removido ou alterado de qualquer distribuição de origem.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.actions;

import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import org.bukkit.Material;

import java.math.BigInteger;
import java.util.Set;

/**
 * Pure geometry, economy delta, and interactive-block helpers shared across region creation,
 * redefinition, and claim-wand listeners.
 */
public final class RegionCreationSupport {

    private static final BigInteger BIG_LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger BIG_LONG_MIN = BIG_LONG_MAX.negate();

    private static final Set<String> EXACT_INTERACTIVE_MATERIALS = Set.of(
            "CHEST",
            "TRAPPED_CHEST",
            "ENDER_CHEST",
            "BARREL",
            "SHULKER_BOX",
            "FURNACE",
            "BLAST_FURNACE",
            "SMOKER",
            "DISPENSER",
            "DROPPER",
            "HOPPER",
            "BREWING_STAND",
            "BEACON",
            "CRAFTER",
            "CHISELED_BOOKSHELF",
            "DECORATED_POT",
            "CRAFTING_TABLE",
            "CARTOGRAPHY_TABLE",
            "FLETCHING_TABLE",
            "GRINDSTONE",
            "LOOM",
            "SMITHING_TABLE",
            "STONECUTTER",
            "ENCHANTING_TABLE",
            "ANVIL",
            "CHIPPED_ANVIL",
            "DAMAGED_ANVIL",
            "LECTERN",
            "LEVER",
            "NOTE_BLOCK",
            "JUKEBOX",
            "REPEATER",
            "COMPARATOR",
            "DAYLIGHT_DETECTOR",
            "BELL",
            "CAKE",
            "CAMPFIRE",
            "SOUL_CAMPFIRE",
            "RESPAWN_ANCHOR"
    );

    private RegionCreationSupport() {
    }

    /**
     * Checks whether two axis-aligned integer cuboids intersect in 3D space (inclusive bounds).
     */
    public static boolean cuboidsIntersect(
            int aX1, int aX2, int aY1, int aY2, int aZ1, int aZ2,
            int bX1, int bX2, int bY1, int bY2, int bZ1, int bZ2) {
        int aMinX = Math.min(aX1, aX2);
        int aMaxX = Math.max(aX1, aX2);
        int aMinY = Math.min(aY1, aY2);
        int aMaxY = Math.max(aY1, aY2);
        int aMinZ = Math.min(aZ1, aZ2);
        int aMaxZ = Math.max(aZ1, aZ2);

        int bMinX = Math.min(bX1, bX2);
        int bMaxX = Math.max(bX1, bX2);
        int bMinY = Math.min(bY1, bY2);
        int bMaxY = Math.max(bY1, bY2);
        int bMinZ = Math.min(bZ1, bZ2);
        int bMaxZ = Math.max(bZ1, bZ2);

        return aMinX <= bMaxX && aMaxX >= bMinX
                && aMinY <= bMaxY && aMaxY >= bMinY
                && aMinZ <= bMaxZ && aMaxZ >= bMinZ;
    }

    /**
     * Checks whether two regions overlap in 3D world space (inclusive bounds).
     */
    public static boolean cuboidsIntersect(Region a, Region b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getWorld() != null && b.getWorld() != null && !a.getWorld().equalsIgnoreCase(b.getWorld())) {
            return false;
        }
        return cuboidsIntersect(
                a.getMinMbrX(), a.getMaxMbrX(), a.getMinY(), a.getMaxY(), a.getMinMbrZ(), a.getMaxMbrZ(),
                b.getMinMbrX(), b.getMaxMbrX(), b.getMinY(), b.getMaxY(), b.getMinMbrZ(), b.getMaxMbrZ());
    }

    /**
     * Checks whether {@code outer} completely contains {@code inner} in 3D space (inclusive bounds).
     */
    public static boolean cuboidContains(
            int outerX1, int outerX2, int outerY1, int outerY2, int outerZ1, int outerZ2,
            int innerX1, int innerX2, int innerY1, int innerY2, int innerZ1, int innerZ2) {
        int oMinX = Math.min(outerX1, outerX2);
        int oMaxX = Math.max(outerX1, outerX2);
        int oMinY = Math.min(outerY1, outerY2);
        int oMaxY = Math.max(outerY1, outerY2);
        int oMinZ = Math.min(outerZ1, outerZ2);
        int oMaxZ = Math.max(outerZ1, outerZ2);

        int iMinX = Math.min(innerX1, innerX2);
        int iMaxX = Math.max(innerX1, innerX2);
        int iMinY = Math.min(innerY1, innerY2);
        int iMaxY = Math.max(innerY1, innerY2);
        int iMinZ = Math.min(innerZ1, innerZ2);
        int iMaxZ = Math.max(innerZ1, innerZ2);

        return oMinX <= iMinX && oMaxX >= iMaxX
                && oMinY <= iMinY && oMaxY >= iMaxY
                && oMinZ <= iMinZ && oMaxZ >= iMaxZ;
    }

    /**
     * Checks whether {@code outer} completely contains {@code inner} in 3D world space.
     */
    public static boolean cuboidContains(Region outer, Region inner) {
        if (outer == null || inner == null) {
            return false;
        }
        if (outer.getWorld() != null && inner.getWorld() != null && !outer.getWorld().equalsIgnoreCase(inner.getWorld())) {
            return false;
        }
        return cuboidContains(
                outer.getMinMbrX(), outer.getMaxMbrX(), outer.getMinY(), outer.getMaxY(), outer.getMinMbrZ(), outer.getMaxMbrZ(),
                inner.getMinMbrX(), inner.getMaxMbrX(), inner.getMinY(), inner.getMaxY(), inner.getMinMbrZ(), inner.getMaxMbrZ());
    }

    /**
     * Computes inclusive claim creation cost using overflow-safe {@link BigInteger} arithmetic.
     */
    public static long calculateCreateCost(int area, int minY, int maxY, long costPerBlock, boolean yIsFree) {
        if (area <= 0 || costPerBlock <= 0) {
            return 0L;
        }
        long height = yIsFree ? 1L : ((long) Math.max(minY, maxY) - (long) Math.min(minY, maxY) + 1L);
        BigInteger total = BigInteger.valueOf(area)
                .multiply(BigInteger.valueOf(height))
                .multiply(BigInteger.valueOf(costPerBlock));
        return clampToSafeLong(total);
    }

    /**
     * Computes signed redefine fee delta (positive = charge, negative = refund) using inclusive 3D volume
     * when {@code yIsFree} is {@code false}, or 2D area delta when {@code yIsFree} is {@code true}.
     */
    public static long calculateRedefineCostDelta(
            int oldArea, int oldMinY, int oldMaxY,
            int newArea, int newMinY, int newMaxY,
            long costPerBlock,
            boolean yIsFree) {
        if (costPerBlock <= 0) {
            return 0L;
        }
        long oldHeight = yIsFree ? 1L : ((long) Math.max(oldMinY, oldMaxY) - (long) Math.min(oldMinY, oldMaxY) + 1L);
        long newHeight = yIsFree ? 1L : ((long) Math.max(newMinY, newMaxY) - (long) Math.min(newMinY, newMaxY) + 1L);

        BigInteger oldUnits = BigInteger.valueOf(Math.max(0, oldArea)).multiply(BigInteger.valueOf(oldHeight));
        BigInteger newUnits = BigInteger.valueOf(Math.max(0, newArea)).multiply(BigInteger.valueOf(newHeight));
        BigInteger delta = newUnits.subtract(oldUnits).multiply(BigInteger.valueOf(costPerBlock));
        return clampToSafeLong(delta);
    }

    /**
     * Identifies containers, doors, trapdoors, fence gates, buttons, switches, beds, and utility
     * workstations without relying on live Bukkit unsafe/block-data registries.
     */
    public static boolean isInteractiveBlock(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        if (EXACT_INTERACTIVE_MATERIALS.contains(name)) {
            return true;
        }
        return name.endsWith("_DOOR")
                || name.endsWith("_TRAPDOOR")
                || name.endsWith("_FENCE_GATE")
                || name.endsWith("_BUTTON")
                || name.endsWith("_SHULKER_BOX")
                || name.endsWith("_BED")
                || name.endsWith("_CHEST")
                || name.endsWith("_ANVIL");
    }

    private static long clampToSafeLong(BigInteger value) {
        if (value.compareTo(BIG_LONG_MAX) > 0) {
            return Long.MAX_VALUE;
        }
        if (value.compareTo(BIG_LONG_MIN) < 0) {
            return -Long.MAX_VALUE;
        }
        return value.longValue();
    }
}
