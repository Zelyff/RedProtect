/* Zelyff fork additions, GPL-3.0. Bounded edge sampling, independent of Bukkit. */
package br.net.fabiozumbi12.RedProtect.Bukkit.enhancements;

import java.util.*;

public final class BorderSamples {
  private BorderSamples() {}

  public record Point(int x, int z, boolean corner) {}

  public static List<Point> rectangle(
      int minX, int maxX, int minZ, int maxZ, int viewerX, int viewerZ, int radius, int budget) {
    if (minX > maxX || minZ > maxZ || radius < 1 || budget < 1) return List.of();
    budget = Math.min(budget, 512);
    long left = (long) viewerX - radius, right = (long) viewerX + radius;
    long bottom = (long) viewerZ - radius, top = (long) viewerZ + radius;
    Map<String, Point> result = new LinkedHashMap<>();
    for (int x : new int[] {minX, maxX})
      for (int z : new int[] {minZ, maxZ})
        if (x >= left && x <= right && z >= bottom && z <= top) put(result, x, z, true, budget);
    int perEdge = Math.max(2, budget / 4);
    if (minZ >= bottom && minZ <= top)
      edge(
          result,
          Math.max(minX, left),
          Math.min(maxX, right),
          minZ,
          true,
          minX,
          maxX,
          minZ,
          maxZ,
          perEdge,
          budget);
    if (maxZ >= bottom && maxZ <= top)
      edge(
          result,
          Math.max(minX, left),
          Math.min(maxX, right),
          maxZ,
          true,
          minX,
          maxX,
          minZ,
          maxZ,
          perEdge,
          budget);
    if (minX >= left && minX <= right)
      edge(
          result,
          Math.max(minZ, bottom),
          Math.min(maxZ, top),
          minX,
          false,
          minX,
          maxX,
          minZ,
          maxZ,
          perEdge,
          budget);
    if (maxX >= left && maxX <= right)
      edge(
          result,
          Math.max(minZ, bottom),
          Math.min(maxZ, top),
          maxX,
          false,
          minX,
          maxX,
          minZ,
          maxZ,
          perEdge,
          budget);
    return new ArrayList<>(result.values());
  }

  private static void edge(
      Map<String, Point> out,
      long from,
      long to,
      int fixed,
      boolean horizontal,
      int minX,
      int maxX,
      int minZ,
      int maxZ,
      int samples,
      int budget) {
    if (from > to) return;
    for (int i = 0; i < samples && out.size() < budget; i++) {
      long value = from + Math.round((to - from) * (double) i / (samples - 1));
      int x = horizontal ? (int) value : fixed, z = horizontal ? fixed : (int) value;
      put(out, x, z, (x == minX || x == maxX) && (z == minZ || z == maxZ), budget);
    }
  }

  private static void put(Map<String, Point> out, int x, int z, boolean corner, int budget) {
    if (out.size() < budget) out.putIfAbsent(x + ":" + z, new Point(x, z, corner));
  }
}
