/* Zelyff fork additions, GPL-3.0. Temporary, viewer-scoped border rendering. */
package br.net.fabiozumbi12.RedProtect.Bukkit.enhancements;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.EnhancementCategory;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class BorderRenderer {
  public enum Mode {
    PARTICLE,
    BLOCK,
    HYBRID
  }

  private final RedProtect plugin;
  private final Enhancements owner;
  private final Map<UUID, Render> active = new HashMap<>();
  private boolean glowAvailable = true;

  public BorderRenderer(RedProtect plugin, Enhancements owner) {
    this.plugin = plugin;
    this.owner = owner;
  }

  public boolean show(Player player, Region region) {
    return show(player, List.of(region));
  }

  public boolean show(Player player, Collection<Region> regions) {
    if (!owner.enabled()) return false;
    EnhancementCategory.Border cfg = owner.settings().border;
    if (cfg == null || !cfg.enabled) return false;
    clear(player.getUniqueId());
    if (active.size() >= Math.max(1, Math.min(cfg.viewers, 100))) {
      player.sendMessage("§eVisualisasi sedang penuh. Coba sebentar lagi.");
      return true;
    }
    Mode mode;
    try {
      mode = Mode.valueOf(cfg.mode.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException bad) {
      player.sendMessage("§cMode border harus PARTICLE, BLOCK, atau HYBRID.");
      return true;
    }
    int radius = Math.max(8, Math.min(cfg.radius, 256));
    int cap = Math.max(4, Math.min(cfg.markers, 128));
    int particleCap = Math.max(4, Math.min(cfg.particles / 2, 256));
    Render render = new Render(player.getWorld());
    Location eye = player.getLocation();
    int remaining = Math.max(cap, particleCap),
        perRegion = Math.max(4, remaining / Math.max(1, regions.size()));
    for (Region r : regions) {
      if (!r.getWorld().equals(player.getWorld().getName())) continue;
      int y =
          Math.max(
              player.getWorld().getMinHeight(),
              Math.min(
                  player.getWorld().getMaxHeight() - 1,
                  Math.max(r.getMinY(), Math.min(r.getMaxY(), eye.getBlockY()))));
      for (BorderSamples.Point point :
          BorderSamples.rectangle(
              r.getMinMbrX(),
              r.getMaxMbrX(),
              r.getMinMbrZ(),
              r.getMaxMbrZ(),
              eye.getBlockX(),
              eye.getBlockZ(),
              radius,
              perRegion)) {
        if (render.points.size() >= remaining) break;
        if (!render.world.isChunkLoaded(point.x() >> 4, point.z() >> 4)) continue;
        Location at = new Location(render.world, point.x(), y, point.z());
        render.points.putIfAbsent(at, point.corner());
      }
    }
    active.put(player.getUniqueId(), render);
    boolean blocks = mode != Mode.PARTICLE;
    boolean particles = mode != Mode.BLOCK;
    Material corner = block(cfg.cornerMaterial, Material.GLOWSTONE),
        edge = block(cfg.edgeMaterial, Material.GOLD_BLOCK);
    int markers = 0, glows = 0;
    boolean canGlow =
        blocks
            && cfg.glow.enabled
            && glowAvailable
            && !owner.forms().isBedrock(player.getUniqueId());
    for (Map.Entry<Location, Boolean> entry : markerPoints(render.points, cap)) {
      if (!blocks || markers++ >= cap) break;
      Location at = entry.getKey();
      BlockData data = (entry.getValue() ? corner : edge).createBlockData();
      render.fake.put(at, data);
      player.sendBlockChange(at, data);
      if (canGlow
          && (!cfg.glow.cornersOnly || entry.getValue())
          && glows < Math.max(1, Math.min(cfg.glow.markers, cap))) {
        try {
          BlockDisplay display =
              render.world.spawn(
                  at,
                  BlockDisplay.class,
                  d -> {
                    d.setVisibleByDefault(false);
                    d.setPersistent(false);
                    d.setBlock(data);
                    d.setGlowing(true);
                    d.setGlowColorOverride(glowColor(cfg.glow.color));
                    d.setDisplayWidth(2);
                    d.setDisplayHeight(2);
                    d.setViewRange(Math.max(1, radius / 64f));
                  });
          render.entities.add(display);
          player.showEntity(plugin, display);
          glows++;
        } catch (LinkageError | RuntimeException unsupported) {
          glowAvailable = false;
          canGlow = false;
          plugin.logger.warning(
              "Java border glow unavailable; block/particle fallback remains active.");
        }
      }
    }
    Particle particle;
    try {
      particle =
          Particle.valueOf(
              plugin
                  .getConfigManager()
                  .configRoot()
                  .region_settings
                  .border
                  .particle
                  .toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException bad) {
      particle = Particle.FLAME;
    }
    final Particle selected = particle;
    render.task =
        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  if (active.get(player.getUniqueId()) != render) return;
                  if (!player.isOnline()
                      || !player.getWorld().equals(render.world)
                      || !owner.enabled()
                      || !owner.settings().border.enabled) {
                    clear(player.getUniqueId());
                    return;
                  }
                  render.fake.forEach(
                      (loc, data) -> {
                        if (render.world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4))
                          player.sendBlockChange(loc, data);
                      });
                  if (particles) {
                    int count = 0;
                    for (Location loc : render.points.keySet()) {
                      if (count++ >= particleCap) break;
                      if (render.world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4))
                        player.spawnParticle(selected, loc.clone().add(.5, .15, .5), 1, 0, 0, 0, 0);
                    }
                  }
                },
                1L,
                10L);
    render.expiry =
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  if (active.get(player.getUniqueId()) == render) clear(player.getUniqueId());
                },
                Math.max(1, Math.min(cfg.duration, 120)) * 20L);
    return true;
  }

  public void clear(UUID id) {
    Render render = active.remove(id);
    if (render == null) return;
    if (render.task != null) render.task.cancel();
    if (render.expiry != null) render.expiry.cancel();
    render.entities.forEach(
        entity -> {
          if (entity.isValid()) entity.remove();
        });
    Player viewer = Bukkit.getPlayer(id);
    if (viewer != null && viewer.isOnline() && viewer.getWorld().equals(render.world))
      for (Location at : render.fake.keySet())
        if (render.world.isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4))
          viewer.sendBlockChange(at, render.world.getBlockAt(at).getBlockData());
  }

  public void close() {
    for (UUID id : new ArrayList<>(active.keySet())) clear(id);
  }

  private List<Map.Entry<Location, Boolean>> markerPoints(
      Map<Location, Boolean> points, int limit) {
    List<Map.Entry<Location, Boolean>> result = new ArrayList<>(), edges = new ArrayList<>();
    for (Map.Entry<Location, Boolean> entry : points.entrySet()) {
      if (entry.getValue() && result.size() < limit) result.add(entry);
      else edges.add(entry);
    }
    int slots = Math.min(limit - result.size(), edges.size());
    for (int i = 0; i < slots; i++) {
      int index =
          slots == 1 ? edges.size() / 2 : (int) ((long) i * (edges.size() - 1) / (slots - 1));
      result.add(edges.get(index));
    }
    return result;
  }

  private Color glowColor(String value) {
    try {
      return Color.fromRGB(Integer.parseInt(value.replace("#", ""), 16));
    } catch (IllegalArgumentException bad) {
      return Color.WHITE;
    }
  }

  private Material block(String name, Material fallback) {
    Material m = Material.matchMaterial(name);
    return m != null && m.isBlock() && !m.isAir() ? m : fallback;
  }

  private static final class Render {
    final World world;
    final Map<Location, Boolean> points = new LinkedHashMap<>();
    final Map<Location, BlockData> fake = new LinkedHashMap<>();
    final List<BlockDisplay> entities = new ArrayList<>();
    BukkitTask task, expiry;

    Render(World world) {
      this.world = world;
    }
  }
}
