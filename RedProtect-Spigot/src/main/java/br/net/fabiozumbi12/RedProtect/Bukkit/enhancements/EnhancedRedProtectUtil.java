/* Zelyff fork additions, GPL-3.0. Wrap only the native border entrypoint. */
package br.net.fabiozumbi12.RedProtect.Bukkit.enhancements;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Bukkit.helpers.RedProtectUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class EnhancedRedProtectUtil extends RedProtectUtil {
  private final RedProtect owner;

  public EnhancedRedProtectUtil(RedProtect plugin) {
    super(plugin);
    owner = plugin;
  }

  @Override
  public void addBorder(Player player, Region region) {
    Enhancements extras = owner.getEnhancements();
    if (extras != null
        && extras.enabled()
        && extras.settings().border.enabled
        && !Bukkit.isPrimaryThread()) {
      Bukkit.getScheduler()
          .runTask(
              owner,
              () -> {
                if (player.isOnline() && owner.getEnhancements() == extras)
                  addBorder(player, region);
              });
      return;
    }
    if (extras != null && extras.borders().show(player, region)) return;
    super.addBorder(player, region);
  }
}
