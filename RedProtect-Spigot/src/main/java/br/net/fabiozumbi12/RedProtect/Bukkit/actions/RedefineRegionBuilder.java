/*
 * Copyright (c) 2012-2024 - @FabioZumbi12
 * Last Modified: 26/11/2024 17:51
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

package br.net.fabiozumbi12.RedProtect.Bukkit.actions;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Bukkit.API.events.RedefineRegionEvent;
import br.net.fabiozumbi12.RedProtect.Bukkit.hooks.WEHook;
import br.net.fabiozumbi12.RedProtect.Bukkit.region.RegionBuilder;
import br.net.fabiozumbi12.RedProtect.Core.helpers.Replacer;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class RedefineRegionBuilder extends RegionBuilder {

    public RedefineRegionBuilder(Player p, Region old, Location loc1, Location loc2) {
        this(p, old, loc1, loc2, false);
    }

    public RedefineRegionBuilder(Player p, Region old, Location loc1, Location loc2, boolean preserveVerticalBounds) {
        if (old == null || p == null) {
            return;
        }
        if (loc1 == null || loc2 == null) {
            if (RedProtect.get().hooks.checkWe()) {
                Location[] pos = callSync(() -> WEHook.getWESelection(p));
                if (pos != null) {
                    loc1 = pos[0];
                    loc2 = pos[1];
                } else {
                    this.setError(p, RedProtect.get().getLanguageManager().get("regionbuilder.selection.notset"));
                    return;
                }
            } else {
                this.setError(p, RedProtect.get().getLanguageManager().get("regionbuilder.selection.notset"));
                return;
            }
        }

        World w = p.getWorld();
        if (loc1.getWorld() == null || loc2.getWorld() == null
                || !loc1.getWorld().equals(loc2.getWorld())
                || !loc1.getWorld().equals(w)
                || (old.getWorld() != null && !old.getWorld().equalsIgnoreCase(w.getName()))) {
            this.setError(p, RedProtect.get().getLanguageManager().get("regionbuilder.selection.notset"));
            return;
        }

        // fix y inverted
        Location tempLoc1 = loc1;
        Location tempLoc2 = loc2;

        if (loc1.getBlockY() > loc2.getBlockY()) {
            loc1 = tempLoc2;
            loc2 = tempLoc1;
        }

        //check if distance allowed
        if (new Region(null, loc1, loc2, null).getArea() > RedProtect.get().getConfigManager().configRoot().region_settings.max_scan && !RedProtect.get().getPermissionHandler().hasPerm(p, "redprotect.bypass.define-max-distance")) {
            double dist = new Region(null, loc1, loc2, null).getArea();
            this.setError(p, String.format(RedProtect.get().getLanguageManager().get("regionbuilder.selection.maxdefine"), RedProtect.get().getConfigManager().configRoot().region_settings.max_scan, dist));
            return;
        }

        int[] yBounds = resolveVerticalBounds(
                loc1.getBlockY(),
                loc2.getBlockY(),
                old.getMinY(),
                old.getMaxY(),
                w.getMinHeight(),
                w.getMaxHeight(),
                RedProtect.get().getConfigManager().configRoot().region_settings.claim.miny,
                RedProtect.get().getConfigManager().configRoot().region_settings.claim.maxy,
                RedProtect.get().getConfigManager().configRoot().region_settings.autoexpandvert_ondefine,
                preserveVerticalBounds
        );
        int miny = yBounds[0];
        int maxy = yBounds[1];

        String worldName = w.getName();
        Region newRegion = new Region(old.getName(), old.getAdmins(), old.getMembers(), old.getLeaders(), new int[]{loc1.getBlockX(), loc1.getBlockX(), loc2.getBlockX(), loc2.getBlockX()}, new int[]{loc1.getBlockZ(), loc1.getBlockZ(), loc2.getBlockZ(), loc2.getBlockZ()}, miny, maxy, old.getPrior(), worldName, old.getDate(), old.getFlags(), old.getWelcome(), old.getValue(), old.getTPPoint(), old.canDelete(), old.canPurge());

        if (!RegionCreationSupport.cuboidsIntersect(newRegion, old)) {
            this.setError(p, RedProtect.get().getLanguageManager().get("regionbuilder.needinside"));
            return;
        }

        Set<String> othersName = new HashSet<>();
        boolean bypassAll = p.hasPermission("redprotect.bypass");

        // Full 3D overlap and child-region containment validation
        for (Region other : RedProtect.get().getRegionManager().getRegionsByWorld(worldName)) {
            if (checkID(newRegion, other)) {
                continue;
            }
            if (!bypassAll && RegionCreationSupport.cuboidContains(old, other)
                    && !RegionCreationSupport.cuboidContains(newRegion, other)) {
                this.setError(p, RedProtect.get().getLanguageManager().get("regionbuilder.region.overlapping")
                        .replace("{location}", "x: " + other.getCenterX() + ", z: " + other.getCenterZ())
                        .replace("{player}", other.getLeadersDesc()));
                return;
            }
            if (!RegionCreationSupport.cuboidsIntersect(newRegion, other)) {
                continue;
            }
            boolean samePlanarBounds = other.getMinMbrX() == newRegion.getMinMbrX()
                    && other.getMaxMbrX() == newRegion.getMaxMbrX()
                    && other.getMinMbrZ() == newRegion.getMinMbrZ()
                    && other.getMaxMbrZ() == newRegion.getMaxMbrZ();
            if (samePlanarBounds || (!other.isLeader(p) && !bypassAll)) {
                this.setError(p, RedProtect.get().getLanguageManager().get("regionbuilder.region.overlapping")
                        .replace("{location}", "x: " + other.getCenterX() + ", z: " + other.getCenterZ())
                        .replace("{player}", other.getLeadersDesc()));
                return;
            }
            othersName.add(other.getName());
        }

        newRegion.setPrior(RedProtect.get().getUtil().getUpdatedPrior(newRegion));

        String pName = p.getUniqueId().toString();
        int pLimit = RedProtect.get().getPermissionHandler().getPlayerBlockLimit(p);
        int totalArea = RedProtect.get().getRegionManager().getTotalRegionSize(pName, worldName);
        boolean areaUnlimited = RedProtect.get().getPermissionHandler().hasPerm(p, "redprotect.limits.blocks.unlimited");
        int regionArea = RedProtect.get().getUtil().simuleTotalRegionSize(p.getUniqueId().toString(), newRegion);
        int actualArea = 0;
        if (regionArea > 0) {
            actualArea = totalArea - old.getArea() + regionArea;
        }
        if (pLimit >= 0 && actualArea > pLimit && !areaUnlimited) {
            this.setError(p, RedProtect.get().getLanguageManager().get("regionbuilder.reach.limit"));
            return;
        }

        boolean chargeEconomy = RedProtect.get().getConfigManager().ecoRoot().claim_cost_per_block.enable
                && RedProtect.get().hooks.checkVault()
                && !p.hasPermission("redprotect.eco.bypass");
        long reco = chargeEconomy
                ? RegionCreationSupport.calculateRedefineCostDelta(
                        old.getArea(),
                        old.getMinY(),
                        old.getMaxY(),
                        newRegion.getArea(),
                        newRegion.getMinY(),
                        newRegion.getMaxY(),
                        RedProtect.get().getConfigManager().ecoRoot().claim_cost_per_block.cost_per_block,
                        RedProtect.get().getConfigManager().ecoRoot().claim_cost_per_block.y_is_free)
                : 0L;

        Region candidateRegion = newRegion;
        Region committedRegion = callSync(() -> {
            Region liveOld = RedProtect.get().getRegionManager().getRegionById(old.getID());
            if (liveOld == null) {
                this.setError(p, RedProtect.get().getLanguageManager().get("cmdmanager.region.doesntexist"));
                return null;
            }
            if (chargeEconomy && reco > 0) {
                double peco = RedProtect.get().economy.getBalance(p);
                if (peco < reco) {
                    RedProtect.get().getLanguageManager().sendMessage(p, "regionbuilder.notenought.money",
                            new Replacer[]{new Replacer("{price}", RedProtect.get().getConfigManager().ecoRoot().economy_symbol + reco)});
                    return null;
                }
            }

            RedefineRegionEvent event = new RedefineRegionEvent(liveOld, candidateRegion, p);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return null;
            }

            Region finalNew = event.getNewRegion();
            if (finalNew == null) {
                return null;
            }

            if (chargeEconomy && reco != 0) {
                String formattedPrice = RedProtect.get().getConfigManager().ecoRoot().economy_symbol
                        + Math.abs(reco) + " " + RedProtect.get().getConfigManager().ecoRoot().economy_name;
                if (reco > 0) {
                    EconomyResponse resp = RedProtect.get().economy.withdrawPlayer(p, Math.abs(reco));
                    if (resp == null || !resp.transactionSuccess()) {
                        RedProtect.get().getLanguageManager().sendMessage(p, "regionbuilder.notenought.money",
                                new Replacer[]{new Replacer("{price}", RedProtect.get().getConfigManager().ecoRoot().economy_symbol + reco)});
                        return null;
                    }
                    if (p.isOnline()) {
                        p.sendMessage(RedProtect.get().getLanguageManager().get("economy.region.claimed").replace("{price}", formattedPrice));
                    }
                } else {
                    EconomyResponse resp = RedProtect.get().economy.depositPlayer(p, Math.abs(reco));
                    if (resp != null && resp.transactionSuccess() && p.isOnline()) {
                        p.sendMessage(RedProtect.get().getLanguageManager().get("economy.region.deleted").replace("{price}", formattedPrice));
                    }
                }
            }

            RedProtect.get().getRegionManager().replace(liveOld, finalNew, worldName);
            this.committed = true;
            return finalNew;
        });

        if (committedRegion == null) {
            return;
        }
        newRegion = committedRegion;

        if (p.isOnline()) {
            int claimLimit = RedProtect.get().getPermissionHandler().getPlayerClaimLimit(p);
            int claimused = RedProtect.get().getRegionManager().getPlayerRegions(p.getUniqueId().toString(), worldName);
            boolean claimUnlimited = RedProtect.get().getPermissionHandler().hasPerm(p, "redprotect.limits.claim.unlimited");

            p.sendMessage(RedProtect.get().getLanguageManager().get("general.color") + "------------------------------------");
            p.sendMessage(RedProtect.get().getLanguageManager().get("regionbuilder.claim.left") + claimused + RedProtect.get().getLanguageManager().get("general.color") + "/" + (claimUnlimited ? RedProtect.get().getLanguageManager().get("regionbuilder.area.unlimited") : claimLimit));
            p.sendMessage(RedProtect.get().getLanguageManager().get("regionbuilder.area.used") + " " + (regionArea == 0 ? ChatColor.GREEN + "" + regionArea : ChatColor.RED + "- " + regionArea) + "\n" +
                    RedProtect.get().getLanguageManager().get("regionbuilder.area.left") + " " + (areaUnlimited ? RedProtect.get().getLanguageManager().get("regionbuilder.area.unlimited") : (pLimit - actualArea)));
            p.sendMessage(RedProtect.get().getLanguageManager().get("cmdmanager.region.priority.set").replace("{region}", newRegion.getName()) + " " + newRegion.getPrior());
            if (RedProtect.get().getConfigManager().ecoRoot().claim_cost_per_block.enable)
                p.sendMessage(RedProtect.get().getLanguageManager().get("regionbuilder.block.cost") + reco);
            p.sendMessage(RedProtect.get().getLanguageManager().get("general.color") + "------------------------------------");
            if (!othersName.isEmpty()) {
                p.sendMessage(RedProtect.get().getLanguageManager().get("general.color") + "------------------------------------");
                p.sendMessage(RedProtect.get().getLanguageManager().get("regionbuilder.overlapping"));
                p.sendMessage(RedProtect.get().getLanguageManager().get("region.regions") + " " + othersName);
            }
        }

        this.r = newRegion;
        RedProtect.get().logger.addLog("(World " + newRegion.getWorld() + ") Player " + p.getName() + " REDEFINED newRegion " + newRegion.getName());
    }

    private boolean checkID(Region newr, Region oldr) {
        return newr.getID().equals(oldr.getID());
    }

    public static int[] resolveVerticalBounds(
            int loc1Y,
            int loc2Y,
            int oldMinY,
            int oldMaxY,
            int worldMinY,
            int worldMaxY,
            int cfgMinY,
            int cfgMaxY,
            boolean autoExpandVert,
            boolean preserveVerticalBounds) {
        if (preserveVerticalBounds) {
            return new int[]{oldMinY, oldMaxY};
        }
        int miny = Math.min(loc1Y, loc2Y);
        int maxy = Math.max(loc1Y, loc2Y);
        if (autoExpandVert) {
            miny = cfgMinY != -1 ? cfgMinY : worldMinY;
            maxy = cfgMaxY != -1 ? cfgMaxY : worldMaxY;
        }
        return new int[]{miny, maxy};
    }
}
