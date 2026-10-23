/* Zelyff fork additions, GPL-3.0. Ephemeral stale-action detection only. */
package br.net.fabiozumbi12.RedProtect.Bukkit.enhancements;

import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Core.region.PlayerRegion;
import java.util.*;

/** Compared on the server thread; never serialized into region/player storage. */
public final class RegionSnapshot {
  private final Region instance;
  private final List<Object> state;

  public RegionSnapshot(Region region) {
    instance = region;
    state = capture(region);
  }

  public boolean matches(Region region) {
    return region == instance && state.equals(capture(region));
  }

  public static Map<String, Object> copyFlags(Map<String, Object> flags) {
    Map<String, Object> result = new HashMap<>();
    flags.forEach((key, value) -> result.put(key, copyValue(value)));
    return result;
  }

  private static Object copyValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<Object, Object> out = new HashMap<>();
      map.forEach((k, v) -> out.put(k, copyValue(v)));
      return out;
    }
    if (value instanceof Set<?> set) {
      Set<Object> out = new HashSet<>();
      for (Object item : set) out.add(copyValue(item));
      return out;
    }
    if (value instanceof Collection<?> list) {
      List<Object> out = new ArrayList<>();
      for (Object item : list) out.add(copyValue(item));
      return out;
    }
    if (value != null && value.getClass().isArray()) {
      int length = java.lang.reflect.Array.getLength(value);
      Object out = java.lang.reflect.Array.newInstance(value.getClass().getComponentType(), length);
      for (int i = 0; i < length; i++)
        java.lang.reflect.Array.set(out, i, copyValue(java.lang.reflect.Array.get(value, i)));
      return out;
    }
    return value;
  }

  private static List<Object> capture(Region r) {
    if (r == null) return List.of();
    return Arrays.asList(
        r.getID(),
        r.getMinMbrX(),
        r.getMaxMbrX(),
        r.getMinMbrZ(),
        r.getMaxMbrZ(),
        r.getMinY(),
        r.getMaxY(),
        r.getPrior(),
        roles(r.getLeaders()),
        roles(r.getAdmins()),
        roles(r.getMembers()),
        freeze(r.getFlags()),
        r.getWelcome(),
        r.getValue(),
        r.canDelete(),
        r.canPurge(),
        r.getTPPoint() == null ? "" : r.getTPPoint().toString());
  }

  private static Set<String> roles(Set<PlayerRegion> people) {
    Set<String> copy = new TreeSet<>();
    for (PlayerRegion person : people) copy.add(person.getUUID() + "@" + person.getPlayerName());
    return copy;
  }

  private static Object freeze(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<Object, Object> copy = new HashMap<>();
      map.forEach((k, v) -> copy.put(k, freeze(v)));
      return copy;
    }
    if (value instanceof Set<?> set) {
      Set<Object> copy = new HashSet<>();
      for (Object entry : set) copy.add(freeze(entry));
      return copy;
    }
    if (value != null && value.getClass().isArray()) {
      int length = java.lang.reflect.Array.getLength(value);
      List<Object> copy = new ArrayList<>();
      copy.add(value.getClass().getName());
      for (int i = 0; i < length; i++) copy.add(freeze(java.lang.reflect.Array.get(value, i)));
      return copy;
    }
    if (value instanceof Collection<?> list) {
      List<Object> copy = new ArrayList<>();
      for (Object entry : list) copy.add(freeze(entry));
      return copy;
    }
    return value;
  }
}
