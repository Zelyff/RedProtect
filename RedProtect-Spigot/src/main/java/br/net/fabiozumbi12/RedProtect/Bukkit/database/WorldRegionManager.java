/*
 * Copyright (c) 2012-2023 - @FabioZumbi12
 * Last Modified: 02/10/2023 22:14
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

package br.net.fabiozumbi12.RedProtect.Bukkit.database;

import br.net.fabiozumbi12.RedProtect.Bukkit.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface WorldRegionManager {

    void load();

    int save(boolean force);

    Region getRegion(String rname);

    int getTotalRegionSize(String p0);

    Set<Region> getRegionsNear(int px, int pz, int p1);

    void add(Region p0);

    void remove(Region p0);

    Set<Region> getRegions(int x, int y, int z);

    Set<Region> getInnerRegions(Region region);

    Region getTopRegion(int x, int y, int z);

    Region getLowRegion(int x, int y, int z);

    Map<Integer, Region> getGroupRegion(int x, int y, int z);

    Set<Region> getAllRegions();

    void clearRegions();

    Set<Region> getLeaderRegions(String uuid);

    Set<Region> getMemberRegions(String uuid);

    Set<Region> getAdminRegions(String uuid);

    void updateLiveRegion(String rname, String column, Object value);

    void closeConn();

    int getTotalRegionNum();

    void updateLiveFlags(String rname, String flag, String value);

    void removeLiveFlags(String rname, String flag);

    long getCanPurgeCount(String uuid, boolean canpurge);

    void updateLiveMembers(String rname, String leaders, String admins, String members);

    static boolean containsPoint(Region r, int x, int y, int z) {
        return r != null
                && x <= r.getMaxMbrX() && x >= r.getMinMbrX()
                && y <= r.getMaxY() && y >= r.getMinY()
                && z <= r.getMaxMbrZ() && z >= r.getMinMbrZ();
    }

    static int compareRegionPrecedence(Region a, Region b) {
        int byPrior = Integer.compare(a.getPrior(), b.getPrior());
        if (byPrior != 0) {
            return byPrior;
        }
        int byArea = Integer.compare(b.getArea(), a.getArea());
        if (byArea != 0) {
            return byArea;
        }
        String nameA = a.getName() != null ? a.getName() : "";
        String nameB = b.getName() != null ? b.getName() : "";
        return nameA.compareTo(nameB);
    }

    static Region selectTopRegion(Collection<Region> regions, int x, int y, int z) {
        Region best = null;
        for (Region r : regions) {
            if (!containsPoint(r, x, y, z)) {
                continue;
            }
            if (best == null || compareRegionPrecedence(r, best) > 0) {
                best = r;
            }
        }
        return best;
    }

    static Region selectLowRegion(Collection<Region> regions, int x, int y, int z) {
        Region lowest = null;
        for (Region r : regions) {
            if (!containsPoint(r, x, y, z)) {
                continue;
            }
            if (lowest == null || compareRegionPrecedence(r, lowest) < 0) {
                lowest = r;
            }
        }
        return lowest;
    }

    static Map<Integer, Region> buildGroupRegionMap(Collection<Region> regions, int x, int y, int z) {
        List<Region> matching = new ArrayList<>();
        for (Region r : regions) {
            if (containsPoint(r, x, y, z)) {
                matching.add(r);
            }
        }
        matching.sort(WorldRegionManager::compareRegionPrecedence);
        Map<Integer, Region> regionList = new LinkedHashMap<>();
        for (Region r : matching) {
            int key = r.getPrior();
            while (regionList.containsKey(key)) {
                key++;
            }
            regionList.put(key, r);
        }
        return regionList;
    }

}
