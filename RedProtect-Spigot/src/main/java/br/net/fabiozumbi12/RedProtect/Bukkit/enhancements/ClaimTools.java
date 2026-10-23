/* Zelyff fork additions, GPL-3.0. Golden shovel/stick input adapter only. */
package br.net.fabiozumbi12.RedProtect.Bukkit.enhancements;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.EnhancementCategory;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;

public final class ClaimTools implements Listener {
  private final RedProtect plugin;
  private final Enhancements owner;
  private final ClaimActions actions;
  private final Map<UUID, Selection> selections = new HashMap<>();
  private final Map<UUID, Selection> resizeTargets = new HashMap<>();
  private final Map<UUID, Long> lastClick = new HashMap<>();
  private final BukkitTask cleanup;

  public ClaimTools(RedProtect plugin, Enhancements owner) {
    this.plugin = plugin;
    this.owner = owner;
    actions = new ClaimActions(plugin);
    cleanup =
        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  long now = System.nanoTime();
                  for (UUID id : new ArrayList<>(selections.keySet()))
                    if (!owner.enabled() || now > selections.get(id).expires) {
                      cancel(id);
                      Player p = Bukkit.getPlayer(id);
                      if (p != null)
                        p.sendMessage("§ePemilihan claim berakhir. Pilih sudut pertama lagi.");
                    }
                  resizeTargets
                      .entrySet()
                      .removeIf(e -> !owner.enabled() || now > e.getValue().expires);
                  owner.forms().purge();
                  lastClick.entrySet().removeIf(e -> now - e.getValue() > 5_000_000_000L);
                },
                20,
                20);
  }

  public boolean usesItem(Material material) {
    return owner.enabled()
        && owner.settings().tools.enabled
        && (material == Material.matchMaterial(owner.settings().tools.claimItem)
            || material == Material.matchMaterial(owner.settings().tools.inspectItem));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void click(PlayerInteractEvent event) {
    if (!owner.enabled()
        || !owner.settings().tools.enabled
        || event.getHand() != EquipmentSlot.HAND
        || event.getAction() != Action.RIGHT_CLICK_BLOCK
        || event.getClickedBlock() == null) return;
    Player p = event.getPlayer();
    Material material = p.getInventory().getItemInMainHand().getType();
    EnhancementCategory.Tools cfg = owner.settings().tools;
    Material shovel = Material.matchMaterial(cfg.claimItem),
        stick = Material.matchMaterial(cfg.inspectItem);
    if (material != shovel && material != stick) return;
    long now = System.nanoTime();
    Long previous = lastClick.put(p.getUniqueId(), now);
    event.setCancelled(true);
    if (previous != null && now - previous < 150_000_000L) return;
    Location at = event.getClickedBlock().getLocation();
    if (material == stick) {
      inspect(p, at, cfg.nearby && p.isSneaking());
      return;
    }
    Selection state = selections.get(p.getUniqueId());
    if (state != null && (now > state.expires || !state.world.equals(p.getWorld().getUID()))) {
      cancel(p.getUniqueId());
      state = null;
    }
    if (state == null) {
      Selection target = resizeTargets.get(p.getUniqueId());
      if (target != null) {
        Region chosen = plugin.getRegionManager().getRegionById(target.regionId);
        if (now > target.expires
            || !target.world.equals(p.getWorld().getUID())
            || chosen == null
            || !target.snapshot.matches(chosen)
            || !plugin.getPermissionHandler().hasRegionPermLeader(p, "redefine", chosen)) {
          cancel(p.getUniqueId());
          p.sendMessage("§ePemilihan resize kedaluwarsa atau region berubah.");
          return;
        }
        if (!isCorner(chosen, at)) {
          p.sendMessage("§eKlik sudut region " + chosen.getName() + " yang dipilih di menu.");
          return;
        }
        resizeTargets.remove(p.getUniqueId());
        startResize(p, chosen, at);
        return;
      }
      List<Region> candidates = new ArrayList<>();
      boolean deniedCorner = false;
      if (cfg.resize)
        for (Region region :
            plugin
                .getRegionManager()
                .getRegions(p, at.getBlockX(), at.getBlockY(), at.getBlockZ())) {
          if (!isCorner(region, at)) continue;
          if (plugin.getPermissionHandler().hasRegionPermLeader(p, "redefine", region))
            candidates.add(region);
          else deniedCorner = true;
        }
      if (candidates.size() > 1) {
        p.sendMessage(
            "§eAda beberapa sudut region di titik ini. Gunakan /rp claimmenu atau /rp redefine"
                + " untuk memilih dengan jelas.");
        return;
      }
      if (candidates.size() == 1) {
        startResize(p, candidates.get(0), at);
        return;
      }
      if (deniedCorner) {
        p.sendMessage(
            "§cSudut ini milik claim yang tidak dapat kamu resize. Pemilik memerlukan izin"
                + " redprotect.command.redefine.");
        return;
      }
      if (!cfg.claim) return;
      if (!plugin.getPermissionHandler().hasCommandPerm(p, "claim")) {
        p.sendMessage("§cKamu tidak memiliki izin claim.");
        return;
      }
      if (!plugin.getConfigManager().isAllowedWorld(p) || !plugin.getUtil().canBuildNear(p, at))
        return;
      selections.put(p.getUniqueId(), new Selection(at, expiry(), null, null, null));
      p.sendMessage(
          "§aSudut pertama dipilih. Klik sudut berlawanan. §7/rp cancelclaim untuk batal.");
      return;
    }
    if (state.regionId == null) {
      if (!cfg.claim) {
        cancel(p.getUniqueId());
        return;
      }
      if (at.getBlockX() == state.first.getBlockX() && at.getBlockZ() == state.first.getBlockZ()) {
        p.sendMessage("§ePilih titik yang berbeda untuk sudut kedua.");
        return;
      }
      if (actions.create(p, state.first, at)) {
        selections.remove(p.getUniqueId());
      }
      return;
    }
    if (!cfg.resize) {
      cancel(p.getUniqueId());
      return;
    }
    Region region = plugin.getRegionManager().getRegionById(state.regionId);
    if (region == null
        || !state.snapshot.matches(region)
        || !plugin.getPermissionHandler().hasRegionPermLeader(p, "redefine", region)) {
      cancel(p.getUniqueId());
      p.sendMessage("§cRegion atau izin berubah. Ulangi pemilihan.");
      return;
    }
    if (state.pending != null && sameBlock(state.pending, at)) {
      Location a =
          new Location(
              p.getWorld(), state.opposite.getX(), region.getMinY(), state.opposite.getZ());
      Location b = new Location(p.getWorld(), at.getX(), region.getMaxY(), at.getZ());
      if (actions.resize(p, state.regionId, state.snapshot, a, b))
        selections.remove(p.getUniqueId());
      return;
    }
    state.pending = at.clone();
    Region preview =
        new Region(
            "",
            new Location(
                p.getWorld(), state.opposite.getX(), region.getMinY(), state.opposite.getZ()),
            new Location(p.getWorld(), at.getX(), region.getMaxY(), at.getZ()),
            p.getWorld().getName());
    plugin.getUtil().addBorder(p, preview);
    p.sendMessage(
        "§ePreview resize. Klik titik yang sama sekali lagi untuk konfirmasi; /rp cancelclaim untuk"
            + " batal.");
  }

  public void beginResize(Player player, Region region) {
    if (!owner.enabled()
        || !owner.settings().tools.enabled
        || !owner.settings().tools.resize
        || !plugin.getPermissionHandler().hasRegionPermLeader(player, "redefine", region)) return;
    cancel(player.getUniqueId());
    resizeTargets.put(
        player.getUniqueId(),
        new Selection(
            player.getLocation(), expiry(), region.getID(), new RegionSnapshot(region), null));
    player.sendMessage(
        "§ePegang "
            + owner.settings().tools.claimItem
            + " lalu klik sudut region yang ingin dipindah.");
    plugin.getUtil().addBorder(player, region);
  }

  private void startResize(Player player, Region region, Location corner) {
    int x = corner.getBlockX() == region.getMinMbrX() ? region.getMaxMbrX() : region.getMinMbrX();
    int z = corner.getBlockZ() == region.getMinMbrZ() ? region.getMaxMbrZ() : region.getMinMbrZ();
    Location opposite = new Location(player.getWorld(), x, region.getMinY(), z);
    selections.put(
        player.getUniqueId(),
        new Selection(corner, expiry(), region.getID(), new RegionSnapshot(region), opposite));
    plugin.getUtil().addBorder(player, region);
    player.sendMessage("§aSudut resize dipilih. Klik lokasi baru untuk preview.");
  }

  private void inspect(Player p, Location at, boolean nearby) {
    if (!plugin.getPermissionHandler().hasCommandPerm(p, "infowand")) {
      p.sendMessage("§cKamu tidak memiliki izin inspect.");
      return;
    }
    if (nearby) {
      if (!plugin.getPermissionHandler().hasCommandPerm(p, "near")) return;
      int radius = Math.max(8, Math.min(owner.settings().tools.nearbyRadius, 128));
      int budget = Math.max(1, Math.min(owner.settings().tools.nearbySamples, 4096));
      List<Region> result = new ArrayList<>();
      for (Region r :
          plugin.getRegionManager().getCachedRegionsForInspection(p.getWorld().getName(), budget)) {
        double dx =
            Math.max(0, Math.max((double) r.getMinMbrX() - at.getX(), at.getX() - r.getMaxMbrX()));
        double dz =
            Math.max(0, Math.max((double) r.getMinMbrZ() - at.getZ(), at.getZ() - r.getMaxMbrZ()));
        if (Math.hypot(dx, dz) <= radius) result.add(r);
      }
      result.sort(
          Comparator.comparingDouble(
              r -> {
                double dx =
                    Math.max(
                        0,
                        Math.max((double) r.getMinMbrX() - at.getX(), at.getX() - r.getMaxMbrX()));
                double dz =
                    Math.max(
                        0,
                        Math.max((double) r.getMinMbrZ() - at.getZ(), at.getZ() - r.getMaxMbrZ()));
                return Math.hypot(dx, dz);
              }));
      int limit = Math.max(1, Math.min(owner.settings().tools.nearbyLimit, 8));
      if (result.size() > limit) result = new ArrayList<>(result.subList(0, limit));
      if (result.isEmpty()) {
        p.sendMessage("§7Tidak ada claim ditemukan dalam pemeriksaan cache yang dibatasi.");
        return;
      }
      for (Region r : result) summary(p, r);
      if (!owner.borders().show(p, result)) plugin.getUtil().addBorder(p, result.get(0));
      return;
    }
    Region region = plugin.getRegionManager().getTopRegion(at);
    if (region == null) {
      plugin.getLanguageManager().sendMessage(p, "playerlistener.noregion.atblock");
      return;
    }
    summary(p, region);
    plugin.getUtil().addBorder(p, region);
  }

  private void summary(Player player, Region region) {
    if (plugin.getPermissionHandler().hasRegionPermMember(player, "infowand", region))
      player.sendMessage(region.info());
    else
      player.sendMessage(
          plugin
              .getLanguageManager()
              .get("playerlistener.region.entered")
              .replace("{region}", region.getName())
              .replace("{leaders}", region.getLeadersDesc()));
  }

  private boolean isCorner(Region r, Location l) {
    return (l.getBlockX() == r.getMinMbrX() || l.getBlockX() == r.getMaxMbrX())
        && (l.getBlockZ() == r.getMinMbrZ() || l.getBlockZ() == r.getMaxMbrZ());
  }

  private boolean sameBlock(Location a, Location b) {
    return a.getBlockX() == b.getBlockX()
        && a.getBlockY() == b.getBlockY()
        && a.getBlockZ() == b.getBlockZ();
  }

  private long expiry() {
    return System.nanoTime()
        + Math.max(10, Math.min(owner.settings().tools.timeout, 600)) * 1_000_000_000L;
  }

  public void cancel(UUID id) {
    selections.remove(id);
    resizeTargets.remove(id);
    lastClick.remove(id);
    owner.borders().clear(id);
  }

  @EventHandler
  public void quit(PlayerQuitEvent e) {
    cancel(e.getPlayer().getUniqueId());
    owner.forms().invalidate(e.getPlayer().getUniqueId());
  }

  @EventHandler(ignoreCancelled = true)
  public void teleport(PlayerTeleportEvent e) {
    cancel(e.getPlayer().getUniqueId());
    owner.forms().invalidate(e.getPlayer().getUniqueId());
  }

  @EventHandler
  public void world(PlayerChangedWorldEvent e) {
    cancel(e.getPlayer().getUniqueId());
    owner.forms().invalidate(e.getPlayer().getUniqueId());
  }

  public void close() {
    cleanup.cancel();
    for (UUID id : new ArrayList<>(selections.keySet())) cancel(id);
    resizeTargets.clear();
    lastClick.clear();
    HandlerList.unregisterAll(this);
  }

  private static final class Selection {
    final Location first, opposite;
    final UUID world;
    final long expires;
    final String regionId;
    final RegionSnapshot snapshot;
    Location pending;

    Selection(Location first, long expires, String id, RegionSnapshot snapshot, Location opposite) {
      this.first = first.clone();
      world = first.getWorld().getUID();
      this.expires = expires;
      regionId = id;
      this.snapshot = snapshot;
      this.opposite = opposite;
    }
  }
}
