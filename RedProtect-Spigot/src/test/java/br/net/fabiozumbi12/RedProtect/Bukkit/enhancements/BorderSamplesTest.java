package br.net.fabiozumbi12.RedProtect.Bukkit.enhancements;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class BorderSamplesTest {
  @Test
  public void allFourRealCornersAreIncludedWhenVisible() {
    List<BorderSamples.Point> p = BorderSamples.rectangle(-10, 10, -10, 10, 0, 0, 64, 32);
    assertEquals(4, p.stream().filter(BorderSamples.Point::corner).count());
  }

  @Test
  public void hugeRegionDoesNotPaintAFalseBorderAroundViewer() {
    assertTrue(
        BorderSamples.rectangle(-30000000, 30000000, -30000000, 30000000, 0, 0, 64, 32).isEmpty());
  }

  @Test
  public void onlyRealEdgesAreSampledAndWorkIsCapped() {
    List<BorderSamples.Point> p =
        BorderSamples.rectangle(-30000000, 30000000, -10, 30000000, 0, 0, 64, 32);
    assertTrue(p.size() <= 32);
    assertFalse(p.isEmpty());
    for (BorderSamples.Point point : p) {
      assertEquals(-10, point.z());
      assertTrue(Math.abs(point.x()) <= 64);
      assertFalse(point.corner());
    }
  }

  @Test
  public void integerExtremesDoNotOverflow() {
    List<BorderSamples.Point> p =
        BorderSamples.rectangle(
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            64,
            8);
    assertTrue(p.size() <= 8);
    assertTrue(p.stream().anyMatch(BorderSamples.Point::corner));
  }

  @Test
  public void repeatedCornersAndTinyRegionsAreDeduplicated() {
    List<BorderSamples.Point> p = BorderSamples.rectangle(1, 1, 1, 1, 1, 1, 64, 32);
    assertEquals(1, p.size());
    assertTrue(p.get(0).corner());
  }

  @Test
  public void pointsAlwaysLieOnTheOriginalPerimeter() {
    for (int budget = 1; budget <= 64; budget++) {
      List<BorderSamples.Point> p = BorderSamples.rectangle(-4, 13, -7, 15, 2, 2, 64, budget);
      assertTrue(p.size() <= budget);
      Set<String> unique = new HashSet<>();
      for (BorderSamples.Point point : p) {
        assertTrue(point.x() == -4 || point.x() == 13 || point.z() == -7 || point.z() == 15);
        assertTrue(unique.add(point.x() + ":" + point.z()));
      }
    }
  }

  @Test
  public void invalidBoundsAndEmptyBudgetsProduceNothing() {
    assertTrue(BorderSamples.rectangle(10, 0, 0, 10, 0, 0, 64, 32).isEmpty());
    assertTrue(BorderSamples.rectangle(0, 10, 0, 10, 0, 0, 64, 0).isEmpty());
  }

  @Test
  public void samplingNeverExceedsItsHardCap() {
    assertTrue(
        BorderSamples.rectangle(-500, 500, -500, 500, 0, 0, 1000, Integer.MAX_VALUE).size() <= 512);
  }
}
