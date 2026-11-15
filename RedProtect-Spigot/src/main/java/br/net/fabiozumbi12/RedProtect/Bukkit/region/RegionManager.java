/*
 * Copyright (c) 2012-2024 - @FabioZumbi12
 * Last Modified: 26/06/2024 18:07
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 *  damages arising from the use of this class.
 *
 * Permission is granted to anyone to use this class for any purpose, including commercial plugins, and to alter it and
 * redistribute it freely, subject to the following restrictions:
 * 1 - The origin of this class must not be misrepresented; you must not claim that you wrote the original software. If you
 * use this class in other plugins, an acknowledgment in the plugin documentation would be appreciated but is not required.
 * 2 - Altered source versions must be plainly marked as such, and must not be misrepresented as being the original class.
 * 3 - This notice may not be removed or altered from any source distribution.
 *
 * Esta classe é fornecida "como está", sem qualquer garantia expressa ou implícita. Em nenhum caso os autores serão
 * responsabilizados por quaisquer danos decorrentes do uso desta classe.
 *
 * É concedida permissão a qualquer pessoa para usar esta classe para qualquer finalidade, incluindo plugins pagos, e para
 * alterá-lo e redistribuí-lo livremente, sujeito às seguintes restrições:
 * 1 - A origem desta classe não deve ser deturpada; você não deve afirmar que escreveu a classe original. Se você usar esta
 *  classe em um plugin, uma confirmação de autoria na documentação do plugin será apreciada, mas não é necessária.
 * 2 - Versões de origem alteradas devem ser claramente marcadas como tal e não devem ser deturpadas como sendo a
 * classe original.
 * 3 - Este aviso não pode ser removido ou alterado de qualquer distribuição de origem.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.region;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Bukkit.database.WorldFlatFileRegionManager;
import br.net.fabiozumbi12.RedProtect.Bukkit.database.WorldMySQLRegionManager;
import br.net.fabiozumbi12.RedProtect.Bukkit.database.WorldRegionManager;
import br.net.fabiozumbi12.RedProtect.Bukkit.hooks.WEHook;
import br.net.fabiozumbi12.RedProtect.Core.helpers.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Get the region database from here. All functions for manage regions can be found in this class.
 */
public class RegionManager {

    private static final int MAX_LOCATION_CACHE_SIZE = 4096;
    private static final long LOCATION_CACHE_TTL_MS = 10_000L;

    private static final class CachedRegionEntry {
        private final Region region;
        private final String world;
        private final long expiresAtMs;

        private CachedRegionEntry(Region region, String world, long expiresAtMs) {
            this.region = region;
            this.world = world;
            this.expiresAtMs = expiresAtMs;
        }
    }

    private final Map<String, WorldRegionManager> regionManagers;
    private final Map<String, CachedRegionEntry> bLoc;

    public RegionManager() {
        this.bLoc = Collections.synchronizedMap(new LinkedHashMap<String, CachedRegionEntry>(512, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedRegionEntry> eldest) {
                return size() > MAX_LOCATION_CACHE_SIZE;
            }
        });
        this.regionManagers = new ConcurrentHashMap<>();
    }

    public void loadAll() throws Exception {
        for (World w : Bukkit.getWorlds()) {
            load(w.getName());
        }
    }

    public void load(String w) throws Exception {
        if (this.regionManagers.containsKey(w) && this.regionManagers.get(w) != null) {
            return;
        }
        WorldRegionManager mgr;
        if (RedProtect.get().getConfigManager().configRoot().file_type.equalsIgnoreCase("mysql")) {
            mgr = new WorldMySQLRegionManager(w);
        } else {
            mgr = new WorldFlatFileRegionManager(w);
        }
        mgr.load();
        this.regionManagers.put(w, mgr);
    }

    public void unloadAll() {
        for (String w : this.regionManagers.keySet()) {
            regionManagers.get(w).clearRegions();
            if (RedProtect.get().hooks.checkDyn() && RedProtect.get().getConfigManager().configRoot().hooks.dynmap.enable) {
                try {
                    RedProtect.get().hooks.dynmapHook.removeAll(w);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        this.regionManagers.clear();
        this.bLoc.clear();
    }

    public void unload(String w) {
        if (w == null || !this.regionManagers.containsKey(w)) {
            return;
        }
        WorldRegionManager mgr = this.regionManagers.remove(w);
        if (mgr != null) {
            mgr.save(false);
            mgr.closeConn();
        }
        removeWorldCache(w);
    }

    public int saveAll(boolean force) {
        int saved = 0;
        for (WorldRegionManager worldRegionManager : this.regionManagers.values()) {
            saved = worldRegionManager.save(force) + saved;
        }
        if (force && RedProtect.get().getConfigManager().configRoot().flat_file.backup && !RedProtect.get().getConfigManager().configRoot().file_type.equalsIgnoreCase("mysql")) {
            RedProtect.get().getUtil().backupRegions();
        }
        return saved;
    }

    public Region getRegionById(String rid) {
        if (rid == null) return null;
        int sep = rid.indexOf('@');
        if (sep <= 0 || sep >= rid.length() - 1) {
            return null;
        }
        String rname = rid.substring(0, sep);
        String world = rid.substring(sep + 1);
        WorldRegionManager mgr = this.regionManagers.get(world);
        return mgr != null ? mgr.getRegion(rname) : null;
    }

    public Region getRegion(String rname, String w) {
        if (rname == null || w == null) return null;
        WorldRegionManager mgr = this.regionManagers.get(w);
        return mgr != null ? mgr.getRegion(rname) : null;
    }

    public int getTotalRegionSize(String uuid, String world) {
        if (uuid == null || world == null) {
            return 0;
        }
        World w = Bukkit.getWorld(world);
        if (w == null) {
            return 0;
        }
        int size = 0;
        if (RedProtect.get().getConfigManager().configRoot().region_settings.blocklimit_per_world) {
            WorldRegionManager rms = this.regionManagers.get(world);
            if (rms != null) {
                size = rms.getTotalRegionSize(uuid);
            }
        } else {
            for (World wr : Bukkit.getWorlds()) {
                WorldRegionManager rms = this.regionManagers.get(wr.getName());
                if (rms == null) {
                    try {
                        this.load(wr.getName());
                        rms = this.regionManagers.get(wr.getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (rms != null) {
                    size += rms.getTotalRegionSize(uuid);
                }
            }
        }
        return size;
    }

    /**
     * Return a set of regions by player UUID or Name if this player is Leader;
     * <p>
     * This will return player regions based on raw UUID or Player name, depending on server is running in Online or Offline mode;
     *
     * @param uuid the UUID of the player.
     * @param world world name of region.
     * @return {@code Set<Region>} with regions
     */
    public Set<Region> getLeaderRegions(String uuid, String world) {
        WorldRegionManager mgr = this.regionManagers.get(world);
        return mgr != null ? mgr.getLeaderRegions(uuid) : new HashSet<>();
    }

    /**
     * Return a set of regions by player UUID or Name if this player is Admin or Leader;
     * <p>
     * This will return player regions based on raw UUID or Player name, depending on server is running in Online;
     *
     * @param uuid the UUID of the player.
     * @param world world name of region.
     * @return {@code Set<Region>} with regions
     */
    public Set<Region> getAdminRegions(String uuid, String world) {
        WorldRegionManager mgr = this.regionManagers.get(world);
        return mgr != null ? mgr.getAdminRegions(uuid) : new HashSet<>();
    }

    /**
     * Return a set of regions by player UUID or Name if this player is Member, Admin or Leader;
     * <p>
     * This will return player regions based on raw UUID or Player name, depending on server is running in Online;
     *
     * @param uuid the UUID of the player.
     * @param world world name of region.
     * @return {@code Set<Region>} with regions
     */
    public Set<Region> getMemberRegions(String uuid, String world) {
        WorldRegionManager mgr = this.regionManagers.get(world);
        return mgr != null ? mgr.getMemberRegions(uuid) : new HashSet<>();
    }

    /**
     * Return a set of regions by player UUID or Name if this player is Leader;
     * <p>
     * This will return player regions based on raw UUID or Player name, depending on server is running in Online or Offline mode;
     *
     * @param uuid the UUID of the player.
     * @return {@code Set<Region>} with regions
     */
    public Set<Region> getLeaderRegions(String uuid) {
        Set<Region> ret = new HashSet<>();
        for (WorldRegionManager worldRegionManager : this.regionManagers.values()) {
            ret.addAll(worldRegionManager.getLeaderRegions(uuid));
        }
        return ret;
    }

    /**
     * Return a set of regions by player UUID or Name if this player is Admin or Leader;
     * <p>
     * This will return player regions based on raw UUID or Player name, depending on server is running in Online;
     *
     * @param uuid the UUID of the player.
     * @return {@code Set<Region>} with regions
     */
    public Set<Region> getAdminRegions(String uuid) {
        Set<Region> ret = new HashSet<>();
        for (WorldRegionManager worldRegionManager : this.regionManagers.values()) {
            ret.addAll(worldRegionManager.getAdminRegions(uuid));
        }
        return ret;
    }

    /**
     * Return a set of regions by player UUID or Name if this player is Member, Admin or Leader;
     * <p>
     * This will return player regions based on raw UUID or Player name, depending on server is running in Online;
     *
     * @param uuid the UUID of the player.
     * @return {@code Set<Region>} with regions
     */
    public Set<Region> getMemberRegions(String uuid) {
        Set<Region> ret = new HashSet<>();
        for (WorldRegionManager worldRegionManager : this.regionManagers.values()) {
            ret.addAll(worldRegionManager.getMemberRegions(uuid));
        }
        return ret;
    }

    public Set<Region> getRegionsForChunk(Chunk chunk) {
        Set<Region> regions = new HashSet<>();
        for (Region region : RedProtect.get().getRegionManager().getRegionsByWorld(chunk.getWorld().getName())) {
            int minChunkX = (int) Math.floor(region.getMinMbrX() / 16f);
            int maxChunkX = (int) Math.floor(region.getMaxMbrX() / 16f);
            int minChunkZ = (int) Math.floor(region.getMinMbrZ() / 16f);
            int maxChunkZ = (int) Math.floor(region.getMaxMbrZ() / 16f);
            if (chunk.getX() >= minChunkX && chunk.getX() <= maxChunkX && chunk.getZ() >= minChunkZ && chunk.getZ() <= maxChunkZ) {
                regions.add(region);
            }
        }
        return regions;
    }

    public Set<Region> getRegionsNear(Player player, int i) {
        if (player == null || player.getWorld() == null) {
            return new HashSet<>();
        }
        WorldRegionManager mgr = regionManagers.get(player.getWorld().getName());
        return mgr != null ? mgr.getRegionsNear(player.getLocation().getBlockX(), player.getLocation().getBlockZ(), i) : new HashSet<>();
    }

    public Set<Region> getRegions(String player, String w) {
        WorldRegionManager mgr = regionManagers.get(w);
        if (mgr != null)
            return mgr.getLeaderRegions(player);
        return new HashSet<>();
    }

    public int getPlayerRegions(String player, String w) {
        int size;
        if (RedProtect.get().getConfigManager().configRoot().region_settings.claim.claimlimit_per_world) {
            size = getRegions(player, w).size();
        } else {
            size = getLeaderRegions(player).size();
        }
        return size;
    }

    public void add(Region r, String w) {
        if (r == null || w == null) {
            return;
        }
        WorldRegionManager rms = this.regionManagers.get(w);
        if (rms == null) {
            return;
        }
        rms.add(r);
        removeWorldCache(w);
        if (RedProtect.get().hooks.checkDyn() && RedProtect.get().getConfigManager().configRoot().hooks.dynmap.enable) {
            try {
                RedProtect.get().hooks.dynmapHook.addMark(r);
            } catch (Exception ex) {
                ex.printStackTrace();
                RedProtect.get().logger.severe("Problems when add marks to Dynmap. Dynmap is updated?");
            }
        }
    }

    public void replace(Region oldRegion, Region newRegion, String w) {
        if (oldRegion != null) {
            remove(oldRegion, w);
        }
        if (newRegion != null) {
            add(newRegion, w);
        }
    }

    public long getCanPurgePlayer(String player, String world) {
        if (RedProtect.get().getConfigManager().configRoot().purge.purge_limit_perworld) {
            WorldRegionManager mgr = regionManagers.get(world);
            return mgr != null ? mgr.getCanPurgeCount(player, false) : 0;
        } else {
            long total = 0;
            for (World wr : Bukkit.getWorlds()) {
                WorldRegionManager mgr = regionManagers.get(wr.getName());
                if (mgr != null) {
                    total += mgr.getCanPurgeCount(player, false);
                }
            }
            return total;
        }
    }

    public void save(String w) {
        WorldRegionManager mgr = this.regionManagers.get(w);
        if (mgr != null) {
            mgr.save(false);
        }
    }

    public void remove(Region r, String w) {
        if (r == null || w == null) {
            return;
        }
        r.notifyRemove();
        WorldRegionManager rms = this.regionManagers.get(w);
        if (rms != null) {
            rms.remove(r);
        }
        if (RedProtect.get().hooks.checkDyn() && RedProtect.get().getConfigManager().configRoot().hooks.dynmap.enable) {
            try {
                RedProtect.get().hooks.dynmapHook.removeMark(r);
            } catch (Exception ex) {
                RedProtect.get().logger.severe("Problems when remove marks to Dynmap. Is Dynmap updated?");
                ex.printStackTrace();
            }
        }
        removeCache(r);
    }

    public Set<Region> getRegions(Player p, int x, int y, int z) {
        if (p == null || p.getWorld() == null) {
            return new HashSet<>();
        }
        WorldRegionManager mgr = this.regionManagers.get(p.getWorld().getName());
        return mgr != null ? mgr.getRegions(x, y, z) : new HashSet<>();
    }

    public Set<Region> getInnerRegions(Region region, String w) {
        if (region == null || w == null) {
            return new HashSet<>();
        }
        WorldRegionManager mgr = this.regionManagers.get(w);
        return mgr != null ? mgr.getInnerRegions(region) : new HashSet<>();
    }

    private void removeCache(Region r) {
        if (r == null) {
            return;
        }
        try {
            synchronized (bLoc) {
                bLoc.values().removeIf(v -> v.region == r);
            }
        } catch (Exception ex) {
            RedProtect.get().logger.severe("Problems when remove cache for region " + r.getName() + ": " + ex.getLocalizedMessage());
        }
    }

    private void removeWorldCache(String world) {
        if (world == null) {
            return;
        }
        synchronized (bLoc) {
            bLoc.values().removeIf(v -> world.equals(v.world));
        }
    }

    public int removeAll(String player) {
        int qtd = 0;
        for (WorldRegionManager wrm : this.regionManagers.values()) {
            for (Region r : wrm.getLeaderRegions(player)) {
                r.notifyRemove();
                wrm.remove(r);
                removeCache(r);
                qtd++;
            }
        }
        return qtd;
    }

    public int regenAll(String player) {
        int delay = 0;
        for (Region r : getLeaderRegions(player)) {
            if (r.getArea() <= RedProtect.get().getConfigManager().configRoot().purge.regen.max_area_regen) {
                WEHook.regenRegion(r, Bukkit.getWorld(r.getWorld()), r.getMaxLocation(), r.getMinLocation(), delay, null, true);
                delay = delay + 10;
            }
        }
        return delay / 10;
    }

    /**
     * Get the high priority region in a group region. If no other regions, return the unique region on location.
     *
     * @param loc Location
     * @return {@code Region} - Or null if no regions on this location.
     */
    public Region getTopRegion(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        return getTopRegion(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /**
     * Get the high priority region in a group region. If no other regions, return the unique region on location.
     *
     * @param w World
     * @param x Location x
     * @param y Location y
     * @param z Location z
     * @return {@code Region} - Or null if no regions on this location.
     */
    public Region getTopRegion(String w, int x, int y, int z) {
        if (w == null) {
            return null;
        }
        String cacheKey = w + ":" + x + ":" + y + ":" + z;
        long now = System.currentTimeMillis();
        synchronized (bLoc) {
            CachedRegionEntry cached = bLoc.get(cacheKey);
            if (cached != null) {
                if (now < cached.expiresAtMs) {
                    RedProtect.get().logger.debug(LogLevel.DEFAULT, "Get from cache");
                    return cached.region;
                }
                bLoc.remove(cacheKey);
            }
        }

        WorldRegionManager rm = this.regionManagers.get(w);
        if (rm == null) {
            return null;
        }

        Region region = rm.getTopRegion(x, y, z);
        if (region != null) {
            synchronized (bLoc) {
                bLoc.put(cacheKey, new CachedRegionEntry(region, w, now + LOCATION_CACHE_TTL_MS));
            }
            RedProtect.get().logger.debug(LogLevel.DEFAULT, "Get from DB");
        }
        return region;
    }

    /**
     * Get the low priority region in a group region. If no other regions, return the unique region on location.
     *
     * @param w World
     * @param x Location x
     * @param y Location y
     * @param z Location z
     * @return {@code Region} - Or null if no regions on this location.
     */
    public Region getLowRegion(String w, int x, int y, int z) {
        if (w == null) {
            return null;
        }
        WorldRegionManager rm = this.regionManagers.get(w);
        return rm != null ? rm.getLowRegion(x, y, z) : null;
    }

    /**
     * Get the low priority region in a group region. If no other regions, return the unique region on location.
     *
     * @param loc location to get region.
     * @return {@code Region} - Or null if no regions on this location.
     */
    public Region getLowRegion(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        WorldRegionManager rm = this.regionManagers.get(loc.getWorld().getName());
        return rm != null ? rm.getLowRegion(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) : null;
    }

    /**
     * Get regions in a group region. If no other regions, return the unique region on location.
     *
     * @param w World
     * @param x Location x
     * @param y Location y
     * @param z Location z
     * @return {@code Map<Integer,Region>} - Indexed by priority
     */
    public Map<Integer, Region> getGroupRegion(String w, int x, int y, int z) {
        if (w == null) {
            return null;
        }
        WorldRegionManager rm = this.regionManagers.get(w);
        return rm != null ? rm.getGroupRegion(x, y, z) : null;
    }

    /**
     * Get regions in a group region. If no other regions, return the unique region on location.
     *
     * @return {@code Map<Integer,Region>} - Indexed by priority
     */
    public Map<Integer, Region> getGroupRegion(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        WorldRegionManager rm = this.regionManagers.get(loc.getWorld().getName());
        return rm != null ? rm.getGroupRegion(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) : null;
    }

    public Set<Region> getAllRegions() {
        Set<Region> regions = new HashSet<>();
        for (World w : RedProtect.get().getServer().getWorlds()) {
            WorldRegionManager rm = this.regionManagers.get(w.getName());
            if (rm != null) {
                regions.addAll(rm.getAllRegions());
            }
        }
        return regions;
    }

    public Set<Region> getRegionsByWorld(String w) {
        WorldRegionManager rm = this.regionManagers.get(w);
        return rm != null ? rm.getAllRegions() : new HashSet<>();
    }

    public void clearDB() {
        for (WorldRegionManager rm : this.regionManagers.values()) {
            if (rm != null) {
                rm.clearRegions();
            }
        }
        this.regionManagers.clear();
        this.bLoc.clear();
    }

    public void updateLiveRegion(Region r, String columm, Object value) {
        if (r == null) return;
        WorldRegionManager rm = this.regionManagers.get(r.getWorld());
        if (rm != null) {
            rm.updateLiveRegion(r.getName(), columm, value);
        }
    }

    public void updateLiveFlags(Region r, String flag, String value) {
        if (r == null) return;
        WorldRegionManager rm = this.regionManagers.get(r.getWorld());
        if (rm != null) {
            rm.updateLiveFlags(r.getName(), flag, value);
        }
    }

    public void removeLiveFlags(Region r, String flag) {
        if (r == null) return;
        WorldRegionManager rm = this.regionManagers.get(r.getWorld());
        if (rm != null) {
            rm.removeLiveFlags(r.getName(), flag);
        }
    }

    public void updateLiveMembers(Region r) {
        if (r == null) return;
        WorldRegionManager rm = this.regionManagers.get(r.getWorld());
        if (rm != null) {
            rm.updateLiveMembers(r.getName(), r.getLeadersString(), r.getAdminString(), r.getMembersString());
        }
    }

    public int getTotalRegionsNum() {
        int total = 0;
        for (World w : RedProtect.get().getServer().getWorlds()) {
            WorldRegionManager rm = this.regionManagers.get(w.getName());
            if (rm != null) {
                total = total + rm.getTotalRegionNum();
            }
        }
        return total;
    }

    public Region renameRegion(String newName, Region old) {
        Region newr = new Region(newName, old.getAdmins(), old.getMembers(), old.getLeaders(), new int[]{old.getMinMbrX(), old.getMinMbrX(), old.getMaxMbrX(), old.getMaxMbrX()},
                new int[]{old.getMinMbrZ(), old.getMinMbrZ(), old.getMaxMbrZ(), old.getMaxMbrZ()}, old.getMinY(), old.getMaxY(), old.getPrior(), old.getWorld(), old.getDate(), old.getFlags(), old.getWelcome(), old.getValue(), old.getTPPoint(), old.canDelete(), old.canPurge());

        this.add(newr, newr.getWorld());
        this.remove(old, old.getWorld());
        return newr;
    }

}
