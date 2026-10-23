/* Zelyff fork additions, GPL-3.0. Optional adapter lifecycle. */
package br.net.fabiozumbi12.RedProtect.Bukkit.enhancements;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.EnhancementCategory;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Enhancements {
  private final RedProtect plugin;
  private final BorderRenderer borders;
  private final BedrockForms forms;
  private final ClaimTools tools;

  public Enhancements(RedProtect plugin) {
    this.plugin = plugin;
    borders = new BorderRenderer(plugin, this);
    forms = new BedrockForms(plugin, this);
    tools = new ClaimTools(plugin, this);
    Bukkit.getPluginManager().registerEvents(tools, plugin);
  }

  public EnhancementCategory settings() {
    return plugin.getConfigManager().configRoot().enhancements;
  }

  public boolean enabled() {
    return settings() != null && settings().enabled;
  }

  public BorderRenderer borders() {
    return borders;
  }

  public BedrockForms forms() {
    return forms;
  }

  public ClaimTools tools() {
    return tools;
  }

  public boolean command(CommandSender sender, String[] args) {
    if (!enabled() || !(sender instanceof Player p)) return false;
    if (args.length == 1 && args[0].equalsIgnoreCase("cancelclaim")) {
      tools.cancel(p.getUniqueId());
      forms.invalidate(p.getUniqueId());
      p.sendMessage("§7Pemilihan claim dibatalkan.");
      return true;
    }
    if (args.length == 1 && args[0].equalsIgnoreCase("claimmenu")) {
      if (!forms.open(p))
        p.sendMessage(
            "§eMenu native membutuhkan Floodgate dan forms-enabled. Gunakan /rp ? untuk command.");
      return true;
    }
    return args.length == 0 && forms.open(p);
  }

  public List<String> completions(CommandSender sender, String prefix) {
    List<String> result = new ArrayList<>();
    if (enabled() && sender instanceof Player p) {
      if (settings().tools.enabled && "cancelclaim".startsWith(prefix.toLowerCase(Locale.ROOT)))
        result.add("cancelclaim");
      if (forms.usable(p) && "claimmenu".startsWith(prefix.toLowerCase(Locale.ROOT)))
        result.add("claimmenu");
    }
    return result;
  }

  public void close() {
    tools.close();
    forms.close();
    borders.close();
  }
}
