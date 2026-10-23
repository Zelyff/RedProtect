/* Zelyff fork additions, GPL-3.0. Adapter over the native region builders. */
package br.net.fabiozumbi12.RedProtect.Bukkit.enhancements;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Bukkit.actions.DefineRegionBuilder;
import br.net.fabiozumbi12.RedProtect.Bukkit.actions.RedefineRegionBuilder;
import br.net.fabiozumbi12.RedProtect.Core.region.PlayerRegion;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class ClaimActions {
  private final RedProtect plugin;
  private final Set<UUID> busy = new HashSet<>();

  public ClaimActions(RedProtect plugin) {
    this.plugin = plugin;
  }

  public boolean create(Player player, Location first, Location second) {
    requireThread();
    if (!validPlayer(player, first, second)
        || !plugin.getPermissionHandler().hasCommandPerm(player, "claim")) return deny(player);
    // Native /rp claim authorizes this through command.claim; do not add a stricter tool-world
    // gate.
    if (!plugin.getUtil().canBuildNear(player, first)
        || !plugin.getUtil().canBuildNear(player, second)) return false;
    if (!busy.add(player.getUniqueId())) return false;
    try {
      String name = plugin.getUtil().nameGen(player.getName(), player.getWorld().getName());
      DefineRegionBuilder builder =
          new DefineRegionBuilder(
              player,
              first.clone(),
              second.clone(),
              name,
              new PlayerRegion(player.getUniqueId().toString(), player.getName()),
              new HashSet<>(),
              false,
              () ->
                  validPlayer(player, first, second)
                      && inputAllowed(player)
                      && plugin.getPermissionHandler().hasCommandPerm(player, "claim"));
      if (!builder.ready()) return false;
      Region result = builder.build();
      plugin.getRegionManager().add(result, result.getWorld());
      result.activateDraft();
      plugin
          .getLanguageManager()
          .sendMessage(
              player,
              plugin.getLanguageManager().get("cmdmanager.region.created")
                  + " "
                  + result.getName());
      plugin.getUtil().addBorder(player, result);
      return true;
    } finally {
      busy.remove(player.getUniqueId());
    }
  }

  public boolean resize(
      Player player, String id, RegionSnapshot expected, Location first, Location second) {
    requireThread();
    Region old = plugin.getRegionManager().getRegionById(id);
    if (old == null
        || !expected.matches(old)
        || !validPlayer(player, first, second)
        || !plugin.getConfigManager().isAllowedWorld(player)
        || !plugin.getPermissionHandler().hasRegionPermLeader(player, "redefine", old))
      return deny(player);
    if (!busy.add(player.getUniqueId())) return false;
    try {
      RedefineRegionBuilder builder =
          new RedefineRegionBuilder(
              player,
              old,
              first.clone(),
              second.clone(),
              () ->
                  validPlayer(player, first, second)
                      && inputAllowed(player)
                      && expected.matches(plugin.getRegionManager().getRegionById(id))
                      && plugin
                          .getPermissionHandler()
                          .hasRegionPermLeader(player, "redefine", old));
      if (!builder.ready()) return false;
      Region result = builder.build();
      // Same native storage path, on one server-thread turn; no new serializer/schema.
      plugin.getRegionManager().remove(old, old.getWorld());
      try {
        plugin.getRegionManager().add(result, result.getWorld());
      } catch (RuntimeException failure) {
        plugin.getRegionManager().add(old, old.getWorld());
        throw failure;
      }
      result.activateDraft();
      plugin
          .getLanguageManager()
          .sendMessage(
              player,
              plugin.getLanguageManager().get("cmdmanager.region.redefined")
                  + " "
                  + result.getName());
      plugin.getUtil().addBorder(player, result);
      return true;
    } finally {
      busy.remove(player.getUniqueId());
    }
  }

  private boolean validPlayer(Player p, Location a, Location b) {
    return p.isOnline()
        && !p.isDead()
        && boundedArea(a, b)
        && a != null
        && b != null
        && a.getWorld() != null
        && a.getWorld().equals(b.getWorld())
        && a.getWorld().equals(p.getWorld())
        && plugin.getConfigManager().isAllowedWorld(p)
        && p.getWorld().getWorldBorder().isInside(a)
        && p.getWorld().getWorldBorder().isInside(b);
  }

  private boolean boundedArea(Location a, Location b) {
    if (a == null || b == null) return false;
    try {
      return Math.multiplyExact(
              Math.abs((long) a.getBlockX() - b.getBlockX()) + 1,
              Math.abs((long) a.getBlockZ() - b.getBlockZ()) + 1)
          <= Integer.MAX_VALUE;
    } catch (ArithmeticException overflow) {
      return false;
    }
  }

  private boolean inputAllowed(Player p) {
    return plugin.getEnhancements() != null
        && plugin.getEnhancements().enabled()
        && plugin.getEnhancements().settings().tools.enabled
        && p.getInventory().getItemInMainHand().getType()
            == org.bukkit.Material.matchMaterial(
                plugin.getEnhancements().settings().tools.claimItem);
  }

  private boolean deny(Player p) {
    p.sendMessage("§cClaim tidak dapat diproses. Periksa izin, world, dan perubahan wilayah.");
    return false;
  }

  private void requireThread() {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Claim adapter requires server thread");
  }
}
