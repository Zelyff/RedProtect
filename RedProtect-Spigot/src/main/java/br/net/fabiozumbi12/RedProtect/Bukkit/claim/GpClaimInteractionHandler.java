/*
 * Copyright (c) 2012-2026 - @FabioZumbi12
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 * damages arising from the use of this class.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.claim;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Bukkit.actions.DefineRegionBuilder;
import br.net.fabiozumbi12.RedProtect.Bukkit.actions.RedefineRegionBuilder;
import br.net.fabiozumbi12.RedProtect.Bukkit.actions.RegionCreationSupport;
import br.net.fabiozumbi12.RedProtect.Bukkit.claim.ClaimSessionManager.Bounds3D;
import br.net.fabiozumbi12.RedProtect.Bukkit.claim.ClaimSessionManager.ClaimSession;
import br.net.fabiozumbi12.RedProtect.Bukkit.claim.ClaimSessionManager.CornerAnchor;
import br.net.fabiozumbi12.RedProtect.Bukkit.claim.ClaimSessionManager.SessionType;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.PreviewVisualType;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.RegionPreviewService;
import br.net.fabiozumbi12.RedProtect.Bukkit.region.RegionManager;
import br.net.fabiozumbi12.RedProtect.Core.region.PlayerRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles GriefPrevention-style direct right-click + right-click claiming and corner resizing
 * on top of RedProtect's {@link DefineRegionBuilder} and {@link RedefineRegionBuilder}.
 */
public final class GpClaimInteractionHandler {

    private final RedProtect plugin;
    private final ClaimSessionManager sessions;

    public GpClaimInteractionHandler(RedProtect plugin, ClaimSessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
    }

    /**
     * Processes a wand interaction event when {@code region-settings.claim.wand-mode} is {@code GP_DIRECT}.
     * Returns {@code true} if GP_DIRECT mode handled (or suppressed) the click so native wand logic does not run.
     */
    public boolean handleWandInteract(PlayerInteractEvent event, Player player, ItemStack itemInHand) {
        if (event == null || player == null || itemInHand == null) {
            return false;
        }
        String wandMode = plugin.getConfigManager().configRoot().region_settings.claim.wand_mode;
        if (!ClaimSessionManager.isGpDirectMode(wandMode)) {
            return false;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return false;
        }

        Action action = event.getAction();
        boolean isBlockClick = action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK;
        if (isBlockClick && event.useInteractedBlock() == Event.Result.DENY) {
            return false;
        }
        if (!isBlockClick && event.useItemInHand() == Event.Result.DENY) {
            return false;
        }

        if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
            if (player.isSneaking() && sessions.hasSession(player.getUniqueId())) {
                cancelSelection(player, true);
                event.setCancelled(true);
                return true;
            }
            return false;
        }

        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        Block clicked = event.getClickedBlock();
        if (action == Action.RIGHT_CLICK_BLOCK
                && clicked != null
                && !player.isSneaking()
                && !sessions.hasSession(playerId)
                && RegionCreationSupport.isInteractiveBlock(clicked.getType())) {
            return false;
        }

        event.setCancelled(true);
        Location clickLoc = clicked != null ? clicked.getLocation() : player.getLocation();

        if (sessions.isInFlight(playerId)) {
            return true;
        }

        ClaimSession existing = sessions.getSession(playerId);
        if (existing == null) {
            handleFirstClick(player, clickLoc);
        } else {
            handleSecondClick(player, clickLoc, existing);
        }
        return true;
    }

    private void handleFirstClick(Player player, Location clickLoc) {
        World world = player.getWorld();
        String worldName = world.getName();
        int clickX = clickLoc.getBlockX();
        int clickY = clickLoc.getBlockY();
        int clickZ = clickLoc.getBlockZ();

        Set<Region> worldRegions = plugin.getRegionManager().getRegionsByWorld(worldName);
        Region editableCornerRegion = ClaimSessionManager.pickEditableCornerRegion(
                worldRegions,
                r -> new Bounds3D(r.getMinMbrX(), r.getMaxMbrX(), r.getMinY(), r.getMaxY(), r.getMinMbrZ(), r.getMaxMbrZ()),
                r -> plugin.getPermissionHandler().hasRegionPermLeader(player, "redefine", r),
                Region::getPrior,
                clickX,
                clickY,
                clickZ
        );

        if (editableCornerRegion != null) {
            CornerAnchor anchor = ClaimSessionManager.computeOppositeAnchor(
                    clickX,
                    clickZ,
                    editableCornerRegion.getMinMbrX(),
                    editableCornerRegion.getMaxMbrX(),
                    editableCornerRegion.getMinMbrZ(),
                    editableCornerRegion.getMaxMbrZ()
            );
            sessions.startResizeSession(
                    player.getUniqueId(),
                    worldName,
                    editableCornerRegion.getID(),
                    editableCornerRegion.getName(),
                    anchor.oppositeX(),
                    clickY,
                    anchor.oppositeZ(),
                    editableCornerRegion.getMinY(),
                    editableCornerRegion.getMaxY()
            );
            if (plugin.getPreviewService() != null) {
                plugin.getPreviewService().showRegionPreview(player, editableCornerRegion);
            }
            sendMessage(
                    player,
                    "gp.resize.started",
                    "&bSelected corner of region &6{region}&b. Right-click the new corner location, or sneak + left-click / switch hotbar slot to cancel.",
                    "{region}", editableCornerRegion.getName()
            );
            return;
        }

        if (!plugin.getUtil().canBuildNear(player, clickLoc)) {
            return;
        }

        sessions.startClaimSession(player.getUniqueId(), worldName, clickX, clickY, clickZ);
        if (plugin.getPreviewService() != null) {
            plugin.getPreviewService().showSelectionPoint(player, clickLoc, PreviewVisualType.INITIALIZE);
        }
        sendMessage(
                player,
                "gp.claim.firstcorner",
                "&bFirst claim corner set at &6({x}, {y}, {z})&b. Right-click the opposite corner to claim, or sneak + left-click / switch hotbar slot to cancel.",
                "{x}", String.valueOf(clickX),
                "{y}", String.valueOf(clickY),
                "{z}", String.valueOf(clickZ)
        );
    }

    private void handleSecondClick(Player player, Location clickLoc, ClaimSession existing) {
        UUID playerId = player.getUniqueId();
        World world = player.getWorld();
        String worldName = world.getName();

        if (!worldName.equalsIgnoreCase(existing.worldName())) {
            cancelSelection(player, true);
            return;
        }

        if (existing.type() == SessionType.CLAIM && !plugin.getUtil().canBuildNear(player, clickLoc)) {
            cancelSelection(player, false);
            return;
        }

        ClaimSession consumed = sessions.consumeSession(playerId);
        if (consumed == null) {
            return;
        }

        if (consumed.type() == SessionType.RESIZE) {
            handleResizeCommit(player, world, worldName, clickLoc, consumed);
        } else {
            handleClaimCommit(player, world, worldName, clickLoc, consumed);
        }
    }

    private void handleClaimCommit(Player player, World world, String worldName, Location clickLoc, ClaimSession session) {
        UUID playerId = player.getUniqueId();
        if (!sessions.tryAcquireInFlight(playerId)) {
            return;
        }

        Location loc1 = new Location(world, session.anchorX(), session.anchorY(), session.anchorZ());
        Location loc2 = clickLoc.clone();
        String autoName = plugin.getUtil().nameGen(player.getName(), worldName);
        PlayerRegion leader = new PlayerRegion(playerId.toString(), player.getName());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Region created = null;
            boolean committedByBuilder = false;
            try {
                DefineRegionBuilder rb = new DefineRegionBuilder(player, loc1, loc2, autoName, leader, new HashSet<>(), false);
                if (rb.ready()) {
                    created = rb.build();
                    committedByBuilder = rb.isCommitted();
                }
            } finally {
                final Region finalCreated = created;
                final boolean finalCommitted = committedByBuilder;
                if (!plugin.isEnabled()) {
                    sessions.releaseInFlight(playerId);
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sessions.releaseInFlight(playerId);
                        if (finalCreated != null && !finalCommitted) {
                            plugin.getRegionManager().add(finalCreated, worldName);
                        }
                        if (!player.isOnline()) {
                            return;
                        }
                        RegionPreviewService preview = plugin.getPreviewService();
                        if (finalCreated != null) {
                            sendMessage(
                                    player,
                                    "gp.claim.created",
                                    "&aRegion &6{region}&a claimed successfully!",
                                    "{region}", finalCreated.getName()
                            );
                            if (preview != null && player.getWorld().getName().equalsIgnoreCase(worldName)) {
                                preview.showRegionPreview(player, finalCreated);
                            }
                        } else if (preview != null && player.getWorld().getName().equalsIgnoreCase(worldName)) {
                            preview.showBoundsPreview(
                                    player,
                                    world,
                                    loc1.getBlockX(),
                                    loc2.getBlockX(),
                                    loc1.getBlockZ(),
                                    loc2.getBlockZ(),
                                    loc2.getBlockY(),
                                    PreviewVisualType.CONFLICT
                            );
                        }
                    });
                }
            }
        });
    }

    private void handleResizeCommit(Player player, World world, String worldName, Location clickLoc, ClaimSession session) {
        RegionManager rm = plugin.getRegionManager();
        Region liveOld = session.regionId() != null ? rm.getRegionById(session.regionId()) : null;
        if (liveOld == null && session.regionName() != null) {
            liveOld = rm.getRegion(session.regionName(), worldName);
        }

        if (liveOld == null || !worldName.equalsIgnoreCase(liveOld.getWorld())
                || !plugin.getPermissionHandler().hasRegionPermLeader(player, "redefine", liveOld)) {
            if (plugin.getPreviewService() != null) {
                plugin.getPreviewService().clearPreview(player);
            }
            sendMessage(
                    player,
                    "gp.resize.invalidregion",
                    "&cThat region no longer exists or you no longer have permission to resize it."
            );
            return;
        }

        UUID playerId = player.getUniqueId();
        if (!sessions.tryAcquireInFlight(playerId)) {
            return;
        }

        final Region targetOld = liveOld;
        Location anchorLoc = new Location(world, session.anchorX(), session.anchorY(), session.anchorZ());
        Location newCornerLoc = clickLoc.clone();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Region updated = null;
            boolean committedByBuilder = false;
            try {
                RedefineRegionBuilder rb = new RedefineRegionBuilder(player, targetOld, anchorLoc, newCornerLoc, true);
                if (rb.ready()) {
                    updated = rb.build();
                    committedByBuilder = rb.isCommitted();
                }
            } finally {
                final Region finalUpdated = updated;
                final boolean finalCommitted = committedByBuilder;
                if (!plugin.isEnabled()) {
                    sessions.releaseInFlight(playerId);
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sessions.releaseInFlight(playerId);
                        if (finalUpdated != null && !finalCommitted) {
                            plugin.getRegionManager().replace(targetOld, finalUpdated, worldName);
                        }
                        if (!player.isOnline()) {
                            return;
                        }
                        RegionPreviewService preview = plugin.getPreviewService();
                        if (finalUpdated != null) {
                            plugin.getLanguageManager().sendMessage(player, plugin.getLanguageManager().get("cmdmanager.region.redefined") + " " + finalUpdated.getName());
                            if (preview != null && player.getWorld().getName().equalsIgnoreCase(worldName)) {
                                preview.showRegionPreview(player, finalUpdated);
                            }
                        } else if (preview != null && player.getWorld().getName().equalsIgnoreCase(worldName)) {
                            preview.showBoundsPreview(
                                    player,
                                    world,
                                    anchorLoc.getBlockX(),
                                    newCornerLoc.getBlockX(),
                                    anchorLoc.getBlockZ(),
                                    newCornerLoc.getBlockZ(),
                                    newCornerLoc.getBlockY(),
                                    PreviewVisualType.CONFLICT
                            );
                        }
                    });
                }
            }
        });
    }

    public void cancelSelection(Player player, boolean notify) {
        if (player == null) {
            return;
        }
        boolean removed = sessions.clearSession(player.getUniqueId());
        if (plugin.getPreviewService() != null) {
            plugin.getPreviewService().clearPreview(player);
        }
        if (notify && removed && player.isOnline()) {
            sendMessage(
                    player,
                    "gp.selection.cancelled",
                    "&eRegion claim/resize selection cancelled."
            );
        }
    }

    private void sendMessage(Player player, String key, String defaultTemplate, String... replacements) {
        String raw = plugin.getLanguageManager().get(key);
        if (raw == null || raw.isEmpty() || raw.contains("_" + key + "_")) {
            raw = defaultTemplate;
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        plugin.getLanguageManager().sendMessage(player, raw);
    }
}
