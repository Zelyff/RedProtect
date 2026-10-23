/* Zelyff fork additions, GPL-3.0. Optional Floodgate/Cumulus adapter. */
package br.net.fabiozumbi12.RedProtect.Bukkit.enhancements;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Bukkit.commands.CommandHandlers;
import br.net.fabiozumbi12.RedProtect.Bukkit.commands.SubCommands.RegionHandlers.FlagCommand;
import br.net.fabiozumbi12.RedProtect.Core.config.CoreConfigManager;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Reflection avoids a hard link to Floodgate when it is not installed. */
public final class BedrockForms {
  private final RedProtect plugin;
  private final Enhancements owner;
  private final Map<UUID, Session> sessions = new HashMap<>();
  private Object api;
  private ClassLoader loader;
  private boolean available, closed;

  public BedrockForms(RedProtect plugin, Enhancements owner) {
    this.plugin = plugin;
    this.owner = owner;
    Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
    if (floodgate == null || !floodgate.isEnabled()) return;
    try {
      loader = floodgate.getClass().getClassLoader();
      api =
          Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, loader)
              .getMethod("getInstance")
              .invoke(null);
      Class.forName("org.geysermc.cumulus.form.SimpleForm", true, loader);
      available = api != null;
    } catch (ReflectiveOperationException | LinkageError failure) {
      plugin.logger.warning(
          "Floodgate Forms API unavailable; legacy RedProtect commands remain available.");
    }
  }

  public boolean isBedrock(UUID id) {
    if (api == null) return false;
    try {
      return Boolean.TRUE.equals(call(api, "isFloodgatePlayer", id));
    } catch (ReflectiveOperationException | RuntimeException failure) {
      return false;
    }
  }

  public boolean usable(Player p) {
    return !closed
        && available
        && owner.enabled()
        && owner.settings().bedrock.enabled
        && isBedrock(p.getUniqueId());
  }

  public boolean open(Player p) {
    if (!usable(p)) return false;
    if (!plugin.getPermissionHandler().hasCommandPerm(p, "info")
        && !plugin.getPermissionHandler().hasCommandPerm(p, "list")) return false;
    List<String> labels = new ArrayList<>();
    List<Runnable> actions = new ArrayList<>();
    Region current = plugin.getRegionManager().getTopRegion(p.getLocation());
    if (current != null) {
      labels.add("Claim di lokasimu");
      actions.add(() -> summary(p, current));
    }
    if (plugin.getPermissionHandler().hasCommandPerm(p, "list")) {
      labels.add("Claim milikku");
      actions.add(() -> claims(p, 0));
    }
    labels.add("Panduan claim");
    actions.add(
        () ->
            simple(
                p,
                null,
                "Panduan claim",
                "Golden shovel: dua sudut untuk claim. Klik sudut claim untuk resize, pilih lokasi"
                    + " baru, lalu klik lagi untuk konfirmasi. Stick: inspect. Sneak + stick:"
                    + " sekitar. /rp cancelclaim: batal.",
                List.of("Kembali"),
                List.of(() -> open(p))));
    simple(p, null, "RedProtect", "Kelola claim dan lihat batas wilayah.", labels, actions);
    return true;
  }

  private void claims(Player p, int page) {
    if (!plugin.getPermissionHandler().hasCommandPerm(p, "list")) return;
    List<Region> list =
        new ArrayList<>(plugin.getRegionManager().getLeaderRegions(p.getUniqueId().toString()));
    list.sort(Comparator.comparing(Region::getWorld).thenComparing(Region::getName));
    int size = Math.max(1, Math.min(owner.settings().bedrock.pageSize, 20));
    int start = Math.min(Math.max(0, page * size), Math.max(0, list.size() - 1));
    List<String> labels = new ArrayList<>();
    List<Runnable> actions = new ArrayList<>();
    for (int i = start; i < Math.min(start + size, list.size()); i++) {
      Region target = list.get(i);
      labels.add(target.getName() + " · " + target.getWorld());
      String id = target.getID();
      actions.add(
          () -> {
            Region live = plugin.getRegionManager().getRegionById(id);
            if (live != null && live.isLeader(p)) summary(p, live);
          });
    }
    if (page > 0) {
      labels.add("Halaman sebelumnya");
      actions.add(() -> claims(p, page - 1));
    }
    if (start + size < list.size()) {
      labels.add("Halaman berikutnya");
      actions.add(() -> claims(p, page + 1));
    }
    labels.add("Kembali");
    actions.add(() -> open(p));
    simple(
        p,
        null,
        "Claim milikku",
        list.isEmpty() ? "Kamu belum memiliki claim." : "Halaman " + (page + 1),
        labels,
        actions);
  }

  private void summary(Player p, Region region) {
    Region live = plugin.getRegionManager().getRegionById(region.getID());
    if (live != region) return;
    List<String> labels = new ArrayList<>();
    List<Runnable> actions = new ArrayList<>();
    if (plugin.getPermissionHandler().hasCommandPerm(p, "border")) {
      labels.add("Lihat batas");
      actions.add(
          () -> {
            plugin.getUtil().addBorder(p, live);
          });
    }
    boolean local = isCurrent(p, live);
    if (local && plugin.getPermissionHandler().hasRegionPermLeader(p, "rename", live)) {
      labels.add("Ganti nama");
      actions.add(() -> rename(p, live));
    }
    if (local
        && (live.isLeader(p)
            || live.isAdmin(p)
            || plugin.getPermissionHandler().hasPerm(p, "redprotect.command.admin.flag"))
        && plugin.getPermissionHandler().hasCommandPerm(p, "flag")) {
      labels.add("Atur flags");
      actions.add(() -> flags(p, live, 0));
    }
    if (local
        && (plugin.getPermissionHandler().hasRegionPermAdmin(p, "addmember", live)
            || plugin.getPermissionHandler().hasRegionPermLeader(p, "addleader", live))) {
      labels.add("Anggota dan peran");
      actions.add(() -> roles(p, live));
    }
    if (local
        && owner.settings().tools.enabled
        && owner.settings().tools.resize
        && plugin.getPermissionHandler().hasRegionPermLeader(p, "redefine", live)) {
      labels.add("Resize claim");
      actions.add(() -> owner.tools().beginResize(p, live));
    }
    if (local && plugin.getPermissionHandler().hasRegionPermLeader(p, "delete", live)) {
      labels.add("Hapus claim");
      actions.add(() -> confirmDelete(p, live));
    }
    labels.add("Kembali");
    actions.add(() -> open(p));
    String text =
        live.getName() + "\nWorld: " + live.getWorld() + "\nPemilik: " + live.getLeadersDesc();
    if (plugin.getPermissionHandler().hasRegionPermMember(p, "info", live))
      text +=
          "\nBatas X: "
              + live.getMinMbrX()
              + " sampai "
              + live.getMaxMbrX()
              + "\nBatas Z: "
              + live.getMinMbrZ()
              + " sampai "
              + live.getMaxMbrZ()
              + "\nY: "
              + live.getMinY()
              + " sampai "
              + live.getMaxY();
    if (!local) text += "\nMasuk ke claim ini untuk mengubah pengaturannya.";
    simple(p, live, "Ringkasan claim", text, labels, actions);
  }

  private void rename(Player p, Region region) {
    input(
        p,
        region,
        "Ganti nama claim",
        "Nama baru",
        text -> {
          if (isCurrent(p, region)
              && plugin.getPermissionHandler().hasRegionPermLeader(p, "rename", region))
            CommandHandlers.handleRename(p, text);
        });
  }

  private void confirmDelete(Player p, Region region) {
    simple(
        p,
        region,
        "Konfirmasi hapus",
        "Hapus "
            + region.getName()
            + " di "
            + region.getWorld()
            + "? Proteksi wilayah ini akan dihapus.",
        List.of("Batal", "Ya, hapus claim ini"),
        List.of(
            () -> summary(p, region),
            () -> {
              if (isCurrent(p, region)
                  && plugin.getPermissionHandler().hasRegionPermLeader(p, "delete", region))
                CommandHandlers.handleDelete(p);
            }));
  }

  private void flags(Player p, Region region, int page) {
    if (!isCurrent(p, region) || !plugin.getPermissionHandler().hasCommandPerm(p, "flag")) return;
    List<String> keys = new ArrayList<>();
    for (String flag : plugin.getConfigManager().getDefFlags())
      if (region.getFlags().get(flag) instanceof Boolean
          && plugin.getPermissionHandler().hasFlagPerm(p, flag)
          && (!CoreConfigManager.ADMIN_FLAGS.contains(flag)
              || plugin.getPermissionHandler().hasPerm(p, "redprotect.flag.admin." + flag)))
        keys.add(flag);
    int size = 10, start = Math.max(0, page) * size;
    List<String> labels = new ArrayList<>();
    List<Runnable> actions = new ArrayList<>();
    for (int i = start; i < Math.min(start + size, keys.size()); i++) {
      String key = keys.get(i);
      labels.add(key + ": " + region.getFlagString(key));
      actions.add(
          () -> {
            if (isCurrent(p, region)
                && plugin.getPermissionHandler().hasCommandPerm(p, "flag")
                && plugin.getPermissionHandler().hasFlagPerm(p, key))
              new FlagCommand()
                  .onCommand(p, plugin.getCommand("redprotect"), "redprotect", new String[] {key});
            summary(p, region);
          });
    }
    if (page > 0) {
      labels.add("Sebelumnya");
      actions.add(() -> flags(p, region, page - 1));
    }
    if (start + size < keys.size()) {
      labels.add("Berikutnya");
      actions.add(() -> flags(p, region, page + 1));
    }
    labels.add("Kembali");
    actions.add(() -> summary(p, region));
    simple(
        p,
        region,
        "Flags claim",
        "Pilih flag boolean untuk mengganti nilainya. Jenis flag lain tetap tersedia melalui /rp"
            + " flag.",
        labels,
        actions);
  }

  private boolean roleAllowed(Player p, String action, Region r) {
    return action.endsWith("member")
        ? plugin.getPermissionHandler().hasRegionPermAdmin(p, action, r)
        : plugin.getPermissionHandler().hasRegionPermLeader(p, action, r);
  }

  private void roles(Player p, Region r) {
    if (!isCurrent(p, r)) return;
    String[] operations = {
      "addmember", "removemember", "addadmin", "removeadmin", "addleader", "removeleader"
    };
    String[] captions = {
      "Tambah member",
      "Hapus member",
      "Tambah admin",
      "Turunkan admin",
      "Tambah leader",
      "Turunkan leader"
    };
    List<String> labels = new ArrayList<>();
    List<Runnable> actions = new ArrayList<>();
    for (int i = 0; i < operations.length; i++)
      if (roleAllowed(p, operations[i], r)) {
        String action = operations[i], caption = captions[i];
        labels.add(caption);
        actions.add(
            () ->
                input(
                    p,
                    r,
                    caption,
                    "Nama pemain",
                    name -> {
                      if (!isCurrent(p, r) || !roleAllowed(p, action, r)) return;
                      switch (action) {
                        case "addmember" -> CommandHandlers.handleAddMember(p, name, r);
                        case "removemember" -> CommandHandlers.handleRemoveMember(p, name, r);
                        case "addadmin" -> CommandHandlers.handleAddAdmin(p, name, r);
                        case "removeadmin" -> CommandHandlers.handleRemoveAdmin(p, name, r);
                        case "addleader" -> CommandHandlers.handleAddLeader(p, name, r);
                        case "removeleader" -> CommandHandlers.handleRemoveLeader(p, name, r);
                      }
                    }));
      }
    labels.add("Kembali");
    actions.add(() -> summary(p, r));
    simple(
        p,
        r,
        "Anggota dan peran",
        "Member: " + r.getMembersDesc() + "\nLeader: " + r.getLeadersDesc(),
        labels,
        actions);
  }

  private void simple(
      Player p,
      Region region,
      String title,
      String text,
      List<String> labels,
      List<Runnable> actions) {
    if (!usable(p)) return;
    try {
      Object builder =
          Class.forName("org.geysermc.cumulus.form.SimpleForm", true, loader)
              .getMethod("builder")
              .invoke(null);
      call(builder, "title", title);
      call(builder, "content", text);
      for (String label : labels) call(builder, "button", label);
      Session session = newSession(p, region);
      BiConsumer<Object, Object> handler =
          (form, response) ->
              onServer(
                  p,
                  session,
                  () -> {
                    try {
                      int index = ((Number) call(response, "clickedButtonId")).intValue();
                      if (index >= 0 && index < actions.size()) actions.get(index).run();
                    } catch (ReflectiveOperationException failure) {
                      unavailable();
                    }
                  });
      call(builder, "validResultHandler", handler);
      call(builder, "closedOrInvalidResultHandler", (Runnable) () -> closeResponse(p, session));
      send(p, call(builder, "build"));
    } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
      unavailable();
      p.sendMessage("§eMenu Bedrock belum tersedia. Gunakan /rp ?.");
    }
  }

  private void input(Player p, Region region, String title, String label, Consumer<String> action) {
    if (!usable(p)) return;
    try {
      Object builder =
          Class.forName("org.geysermc.cumulus.form.CustomForm", true, loader)
              .getMethod("builder")
              .invoke(null);
      call(builder, "title", title);
      call(builder, "input", label, "", "");
      Session session = newSession(p, region);
      BiConsumer<Object, Object> handler =
          (form, response) ->
              onServer(
                  p,
                  session,
                  () -> {
                    try {
                      String value = (String) call(response, "asInput", 0);
                      if (value == null
                          || value.isBlank()
                          || value.length() > 64
                          || value.chars().anyMatch(Character::isISOControl)) {
                        p.sendMessage("§cInput tidak valid.");
                        return;
                      }
                      action.accept(value.trim());
                    } catch (ReflectiveOperationException failure) {
                      unavailable();
                    }
                  });
      call(builder, "validResultHandler", handler);
      call(builder, "closedOrInvalidResultHandler", (Runnable) () -> closeResponse(p, session));
      send(p, call(builder, "build"));
    } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
      unavailable();
      p.sendMessage("§eGunakan command RedProtect untuk tindakan ini.");
    }
  }

  private Session newSession(Player p, Region r) {
    Session result =
        new Session(
            p,
            r,
            System.nanoTime()
                + Math.max(10, Math.min(owner.settings().bedrock.timeout, 300)) * 1_000_000_000L);
    sessions.put(p.getUniqueId(), result);
    return result;
  }

  private void onServer(Player p, Session session, Runnable action) {
    if (closed || !plugin.isEnabled()) return;
    try {
      Bukkit.getScheduler()
          .runTask(
              plugin,
              () -> {
                if (closed
                    || plugin.getEnhancements() != owner
                    || !usable(p)
                    || !p.isOnline()
                    || sessions.get(p.getUniqueId()) != session) return;
                sessions.remove(p.getUniqueId()); // nonce consumed before executing any mutation
                Region region =
                    session.id == null ? null : plugin.getRegionManager().getRegionById(session.id);
                if (System.nanoTime() > session.expires
                    || !p.getWorld().getUID().equals(session.world)
                    || (session.id != null
                        && (region == null || !session.snapshot.matches(region)))) {
                  p.sendMessage("§eMenu kedaluwarsa atau claim berubah. Buka /rp claimmenu lagi.");
                  return;
                }
                action.run();
              });
    } catch (IllegalStateException ignored) {
      /* Plugin stopped while a client was answering. */
    }
  }

  private boolean isCurrent(Player p, Region r) {
    return r != null
        && r.getWorld().equals(p.getWorld().getName())
        && plugin.getRegionManager().getTopRegion(p.getLocation()) == r;
  }

  private void closeResponse(Player player, Session session) {
    if (closed || !plugin.isEnabled()) return;
    try {
      Bukkit.getScheduler().runTask(plugin, () -> sessions.remove(player.getUniqueId(), session));
    } catch (IllegalStateException ignored) {
      /* No mutation after shutdown. */
    }
  }

  private void send(Player p, Object form) throws ReflectiveOperationException {
    Object result = call(api, "sendForm", p.getUniqueId(), form);
    if (Boolean.FALSE.equals(result)) throw new IllegalStateException("Floodgate rejected form");
  }

  private static Object call(Object target, String name, Object... args)
      throws ReflectiveOperationException {
    for (Method method : target.getClass().getMethods())
      if (method.getName().equals(name) && method.getParameterCount() == args.length) {
        Class<?>[] types = method.getParameterTypes();
        boolean fits = true;
        for (int i = 0; i < types.length; i++)
          if (args[i] != null
              && !types[i].isInstance(args[i])
              && !(types[i] == int.class && args[i] instanceof Integer)) fits = false;
        if (fits) {
          if (!method.canAccess(target)) method.trySetAccessible();
          return method.invoke(target, args);
        }
      }
    throw new NoSuchMethodException(target.getClass().getName() + "." + name);
  }

  private void unavailable() {
    if (available)
      plugin.logger.warning(
          "Bedrock Forms adapter disabled after API error; use legacy /rp commands.");
    available = false;
    sessions.clear();
  }

  public void invalidate(UUID id) {
    sessions.remove(id);
  }

  public void purge() {
    long now = System.nanoTime();
    sessions.values().removeIf(s -> now > s.expires);
  }

  public void close() {
    closed = true;
    sessions.clear();
  }

  private static final class Session {
    final UUID world;
    final long expires;
    final String id;
    final RegionSnapshot snapshot;

    Session(Player p, Region r, long expires) {
      world = p.getWorld().getUID();
      this.expires = expires;
      id = r == null ? null : r.getID();
      snapshot = r == null ? null : new RegionSnapshot(r);
    }
  }
}
