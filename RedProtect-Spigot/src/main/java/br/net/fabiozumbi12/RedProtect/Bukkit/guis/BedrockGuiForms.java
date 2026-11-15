/*
 * Copyright (c) 2012-2026 - @FabioZumbi12
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 * damages arising from the use of this class.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.guis;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Bukkit.commands.CommandHandlers;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.FlagGuiCategory;
import br.net.fabiozumbi12.RedProtect.Core.config.CoreConfigManager;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Floodgate + Cumulus Forms presentation equivalents for RedProtect's Java chest GUIs
 * ({@link FlagGui} player &amp; edit modes, {@link ChestFlagGui}, {@link MobFlagGui}, {@link ItemFlagGui}).
 * <p>
 * Java clients never invoke this class; Bedrock clients only reach it when
 * {@code hooks.bedrock-gui.enabled} is true and Floodgate is active.
 */
public final class BedrockGuiForms {

    private static final int FORM_PAGE_SIZE = BedrockFormStateHelper.DEFAULT_PAGE_SIZE;
    private static final AtomicLong FORM_SEQ = new AtomicLong();
    private static final Map<UUID, Long> ACTIVE_TOKENS = new ConcurrentHashMap<>();

    private BedrockGuiForms() {
    }

    /**
     * Opens the Bedrock Forms equivalent of {@link FlagGui} (either player flag menu or admin slot editor).
     */
    public static void openFlagGui(Player player, Region region, boolean editable, int maxSlots) {
        if (player == null || region == null) {
            return;
        }
        if (editable) {
            openFlagEditList(player, region.getID(), maxSlots);
        } else {
            openFlagList(player, region.getID(), maxSlots);
        }
    }

    /**
     * Opens the Bedrock Forms equivalent of {@link ChestFlagGui}.
     */
    public static void openChestFlagGui(Player player, Region region, String flag) {
        if (player == null || region == null || flag == null) {
            return;
        }
        openCollectionMenu(player, region.getID(), flag, true, 0);
    }

    /**
     * Opens the Bedrock Forms equivalent of {@link MobFlagGui}.
     */
    public static void openMobFlagGui(Player player, Region region, String flag) {
        if (player == null || region == null || flag == null) {
            return;
        }
        openCollectionMenu(player, region.getID(), flag, false, 0);
    }

    /**
     * Opens the Bedrock Forms equivalent of {@link ItemFlagGui}.
     */
    public static void openItemFlagGui(Player player, Region region, String flag) {
        if (player == null || region == null || flag == null) {
            return;
        }
        openItemMenu(player, region.getID(), flag);
    }

    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) {
            ACTIVE_TOKENS.remove(playerId);
        }
    }

    public static void clearAllTokens() {
        ACTIVE_TOKENS.clear();
    }

    private static long nextToken(UUID playerId) {
        long token = FORM_SEQ.incrementAndGet();
        ACTIVE_TOKENS.put(playerId, token);
        return token;
    }

    private static boolean isCurrentToken(UUID playerId, long token) {
        Long current = ACTIVE_TOKENS.get(playerId);
        return current != null && current == token;
    }

    private static void runOnMain(Runnable task) {
        if (RedProtect.get().isEnabled()) {
            Bukkit.getScheduler().runTask(RedProtect.get(), task);
        }
    }

    private static void sendForm(Player player, long token, FormBuilder<?, ?, ?> builder) {
        builder.closedOrInvalidResultHandler(() -> ACTIVE_TOKENS.remove(player.getUniqueId(), token));
        try {
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder);
        } catch (Throwable t) {
            ACTIVE_TOKENS.remove(player.getUniqueId(), token);
            RedProtect.get().logger.warning("Failed to send Bedrock form to " + player.getName() + ": " + t.getMessage());
        }
    }

    private static void sendInvalidMaterialMessage(Player player, String fallback) {
        String raw = RedProtect.get().getLanguageManager().get("bedrock.gui.items.invalid");
        if (raw != null && !raw.contains("Missing language string")) {
            RedProtect.get().getLanguageManager().sendMessage(player, "bedrock.gui.items.invalid");
        } else {
            player.sendMessage(ChatColor.RED + fallback);
        }
    }

    private static String localizedText(String key, String fallback) {
        String raw = RedProtect.get().getLanguageManager().get(key);
        if (raw == null || raw.contains("Missing language string")) {
            return fallback;
        }
        return raw;
    }

    private static String cleanLocalizedText(String key, String fallback) {
        return BedrockFormStateHelper.stripColorCodes(localizedText(key, fallback));
    }

    private static boolean canEditRegionFlags(Player player, Region region) {
        return region != null && (region.isLeader(player)
                || region.isAdmin(player)
                || RedProtect.get().getPermissionHandler().hasPerm(player, "redprotect.command.admin.flag"));
    }

    private static Region revalidateRegionAccess(Player player, String regionId) {
        if (player == null || !player.isOnline()) {
            return null;
        }
        Region live = RedProtect.get().getRegionManager().getRegionById(regionId);
        if (live == null) {
            RedProtect.get().getLanguageManager().sendMessage(player, "cmdmanager.region.doesntexist");
            return null;
        }
        if (!canEditRegionFlags(player, live)) {
            RedProtect.get().getLanguageManager().sendMessage(player, "cmdmanager.region.flag.nopermregion");
            return null;
        }
        return live;
    }

    private static List<String> collectPermittedFlags(Player player) {
        List<String> permitted = new ArrayList<>();
        Map<String, FlagGuiCategory.GuiFlag> guiFlags = RedProtect.get().getConfigManager().guiRoot().gui_flags;
        for (String flag : RedProtect.get().getConfigManager().getDefFlags()) {
            if (!guiFlags.containsKey(flag)) {
                continue;
            }
            if (!RedProtect.get().getPermissionHandler().hasFlagPerm(player, flag)) {
                continue;
            }
            if (!(RedProtect.get().getConfigManager().configRoot().flags.containsKey(flag) || CoreConfigManager.ADMIN_FLAGS.contains(flag))) {
                continue;
            }
            if (flag.equals("pvp") && !RedProtect.get().getConfigManager().configRoot().flags.containsKey("pvp")) {
                continue;
            }
            permitted.add(flag);
        }
        return permitted;
    }

    private static void openFlagList(Player player, String regionId, int maxSlots) {
        Region region = revalidateRegionAccess(player, regionId);
        if (region == null) {
            return;
        }
        List<String> flags = collectPermittedFlags(player);
        if (flags.isEmpty()) {
            RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
            return;
        }

        long token = nextToken(player.getUniqueId());
        String title = BedrockFormStateHelper.stripColorCodes(RedProtect.get().getUtil().getTitleName(region));
        String content = cleanLocalizedText("bedrock.gui.flags.subtitle", "Select a flag to toggle or configure:");

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(title.isEmpty() ? "Flags" : title)
                .content(content);

        for (String flag : flags) {
            String displayName = BedrockFormStateHelper.stripColorCodes(RedProtect.get().guiLang.getFlagName(flag));
            String valueDisplay;
            if (flag.equalsIgnoreCase("clan")) {
                String clanVal = region.getFlagString(flag);
                valueDisplay = BedrockFormStateHelper.stripColorCodes(RedProtect.get().guiLang.getFlagString(clanVal.isEmpty() ? "false" : "true"));
            } else {
                Object raw = region.getFlags().get(flag);
                if (raw instanceof Boolean) {
                    valueDisplay = BedrockFormStateHelper.stripColorCodes(RedProtect.get().guiLang.getFlagString(raw.toString()));
                } else {
                    valueDisplay = BedrockFormStateHelper.stripColorCodes(RedProtect.get().guiLang.getFlagString("list"));
                }
            }
            builder.button(displayName + "\n[" + valueDisplay + "]");
        }

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            runOnMain(() -> {
                if (!isCurrentToken(player.getUniqueId(), token)) {
                    return;
                }
                if (clicked < 0 || clicked >= flags.size()) {
                    return;
                }
                Region live = revalidateRegionAccess(player, regionId);
                if (live == null) {
                    return;
                }
                String flag = flags.get(clicked);
                if (!RedProtect.get().getPermissionHandler().hasFlagPerm(player, flag)) {
                    RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
                    return;
                }

                applyBedrockFlagAction(player, live, flag, maxSlots);
            });
        });

        sendForm(player, token, builder);
    }

    private static boolean checkFlagDelayReady(Player player, Region region, String flag) {
        if (RedProtect.get().getConfigManager().configRoot().flags_configuration.change_flag_delay.enable
                && RedProtect.get().getConfigManager().configRoot().flags_configuration.change_flag_delay.flags.contains(flag)) {
            if (RedProtect.get().getUtil().isFlagWaiting(region, flag)) {
                RedProtect.get().getLanguageManager().sendMessage(player,
                        RedProtect.get().getLanguageManager().get("gui.needwait.tochange")
                                .replace("{seconds}", "" + RedProtect.get().getConfigManager().configRoot().flags_configuration.change_flag_delay.seconds));
                return false;
            }
        }
        return true;
    }

    private static void markFlagDelay(Player player, Region region, String flag) {
        if (RedProtect.get().getConfigManager().configRoot().flags_configuration.change_flag_delay.enable
                && RedProtect.get().getConfigManager().configRoot().flags_configuration.change_flag_delay.flags.contains(flag)) {
            RedProtect.get().getUtil().startFlagChanger(region, flag, player);
        }
    }

    private static void applyBedrockFlagAction(Player player, Region region, String flag, int maxSlots) {
        if (flag.equalsIgnoreCase("clan")) {
            if (RedProtect.get().hooks.clanManager == null) {
                RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
                return;
            }
            ClanPlayer cp = RedProtect.get().hooks.clanManager.getClanPlayer(player);
            if (cp == null || cp.getClan() == null) {
                RedProtect.get().getLanguageManager().sendMessage(player, "cmdmanager.region.flag.haveclan");
                openFlagList(player, region.getID(), maxSlots);
                return;
            }
            if (!cp.isLeader()) {
                RedProtect.get().getLanguageManager().sendMessage(player, "cmdmanager.region.flag.clancommand");
                openFlagList(player, region.getID(), maxSlots);
                return;
            }
            if (!checkFlagDelayReady(player, region, flag)) {
                return;
            }
            if (region.getFlagString(flag).isEmpty()) {
                if (region.setFlag(player, flag, cp.getTag())) {
                    markFlagDelay(player, region, flag);
                    RedProtect.get().getLanguageManager().sendMessage(player,
                            RedProtect.get().getLanguageManager().get("cmdmanager.region.flag.setclan")
                                    .replace("{clan}", "'" + cp.getClan().getColorTag() + "'"));
                }
            } else {
                String oldTag = region.getFlagString(flag);
                if (region.setFlag(player, flag, "")) {
                    markFlagDelay(player, region, flag);
                    RedProtect.get().getLanguageManager().sendMessage(player,
                            RedProtect.get().getLanguageManager().get("cmdmanager.region.flag.denyclan")
                                    .replace("{clan}", "'" + oldTag + "'"));
                }
            }
            openFlagList(player, region.getID(), maxSlots);
            return;
        }

        if ((flag.equalsIgnoreCase("spawn-animals") || flag.equalsIgnoreCase("spawn-monsters"))
                && RedProtect.get().getPermissionHandler().hasPerm(player, "redprotect.flag.spawn-mob-gui")) {
            openCollectionMenu(player, region.getID(), flag, false, 0);
            return;
        }

        if (isItemGuiFlag(flag) && RedProtect.get().getPermissionHandler().hasPerm(player, "redprotect.flag.item-gui")) {
            openItemMenu(player, region.getID(), flag);
            return;
        }

        if (flag.equalsIgnoreCase("chest") && RedProtect.get().getPermissionHandler().hasPerm(player, "redprotect.flag.chest-gui")) {
            openCollectionMenu(player, region.getID(), flag, true, 0);
            return;
        }

        Object currentRaw = region.getFlags().get(flag);
        boolean isNonBoolAdminFlag = BedrockFormStateHelper.isValueAdminFlag(flag)
                || (CoreConfigManager.ADMIN_FLAGS.stream().anyMatch(f -> f.equalsIgnoreCase(flag))
                && currentRaw != null && !(currentRaw instanceof Boolean));
        if (isNonBoolAdminFlag) {
            openAdminValueFlagForm(player, region.getID(), flag, maxSlots);
            return;
        }

        if (currentRaw != null && !(currentRaw instanceof Boolean)) {
            CommandHandlers.SendFlagUsageMessage(player, flag);
            openFlagList(player, region.getID(), maxSlots);
            return;
        }

        if (!checkFlagDelayReady(player, region, flag)) {
            return;
        }
        if (region.setFlag(player, flag, !region.getFlagBool(flag))) {
            markFlagDelay(player, region, flag);
            RedProtect.get().getLanguageManager().sendMessage(player,
                    RedProtect.get().getLanguageManager().get("cmdmanager.region.flag.set")
                            .replace("{flag}", "'" + flag + "'") + " " + region.getFlagBool(flag));
            RedProtect.get().logger.addLog("(World " + region.getWorld() + ") Player " + player.getName()
                    + " CHANGED flag " + flag + " of region " + region.getName() + " to " + region.getFlagBool(flag));
        }
        openFlagList(player, region.getID(), maxSlots);
    }

    private static void openAdminValueFlagForm(Player player, String regionId, String flag, int maxSlots) {
        Region region = revalidateRegionAccess(player, regionId);
        if (region == null) {
            return;
        }
        if (!RedProtect.get().getPermissionHandler().hasFlagPerm(player, flag)) {
            RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
            return;
        }

        String currentVal = region.getFlags().containsKey(flag) ? region.getFlagString(flag) : "";
        long token = nextToken(player.getUniqueId());
        CustomForm.Builder builder = CustomForm.builder()
                .title(cleanLocalizedText("bedrock.gui.adminflag.title", "Configure Flag") + ": " + flag)
                .input(cleanLocalizedText("bedrock.gui.adminflag.input", "Enter value (uses /rp flag validation)"),
                        currentVal, currentVal)
                .toggle(cleanLocalizedText("bedrock.gui.adminflag.remove", "Remove this flag from region"), false);

        builder.validResultHandler(response -> {
            String typed = response.asInput(0);
            boolean remove = response.asToggle(1);
            runOnMain(() -> {
                if (!isCurrentToken(player.getUniqueId(), token)) {
                    return;
                }
                Region live = revalidateRegionAccess(player, regionId);
                if (live == null) {
                    return;
                }
                if (!RedProtect.get().getPermissionHandler().hasFlagPerm(player, flag)) {
                    RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
                    return;
                }
                if (!checkFlagDelayReady(player, live, flag)) {
                    return;
                }
                if (remove) {
                    CommandHandlers.handleFlag(player, flag, "remove", live);
                    markFlagDelay(player, live, flag);
                } else if (typed != null && !typed.trim().isEmpty()) {
                    CommandHandlers.handleFlag(player, flag, typed.trim(), live);
                    markFlagDelay(player, live, flag);
                } else {
                    CommandHandlers.SendFlagUsageMessage(player, flag);
                }
                openFlagList(player, regionId, maxSlots);
            });
        });

        sendForm(player, token, builder);
    }

    private static boolean isItemGuiFlag(String flag) {
        return flag.equalsIgnoreCase("deny-enter-items")
                || flag.equalsIgnoreCase("allow-enter-items")
                || flag.equalsIgnoreCase("deny-exit-items")
                || flag.equalsIgnoreCase("allow-place")
                || flag.equalsIgnoreCase("allow-break")
                || flag.equalsIgnoreCase("deny-place")
                || flag.equalsIgnoreCase("deny-break");
    }

    private static void openFlagEditList(Player player, String regionId, int maxSlots) {
        if (!player.isOnline()) {
            return;
        }
        if (!RedProtect.get().getPermissionHandler().hasCommandPerm(player, "gui-edit")) {
            RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
            return;
        }
        int effectiveMax = Math.max(9, Math.min(54, maxSlots));
        List<String> editableFlags = new ArrayList<>();
        Map<String, FlagGuiCategory.GuiFlag> guiFlags = RedProtect.get().getConfigManager().guiRoot().gui_flags;
        for (String flag : RedProtect.get().getConfigManager().getDefFlags()) {
            if (guiFlags.containsKey(flag)) {
                editableFlags.add(flag);
            }
        }
        if (editableFlags.isEmpty()) {
            return;
        }

        long token = nextToken(player.getUniqueId());
        String title = cleanLocalizedText("gui.editflag", "Edit Flag GUI");
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(title)
                .content(cleanLocalizedText("bedrock.gui.edit.select", "Select a flag to change its GUI slot:"));

        for (String flag : editableFlags) {
            int slot = guiFlags.get(flag).slot;
            builder.button(flag + " (slot " + slot + ")");
        }

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            runOnMain(() -> {
                if (!isCurrentToken(player.getUniqueId(), token) || !player.isOnline()) {
                    return;
                }
                if (!RedProtect.get().getPermissionHandler().hasCommandPerm(player, "gui-edit")) {
                    RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
                    return;
                }
                if (clicked >= 0 && clicked < editableFlags.size()) {
                    openFlagSlotEditor(player, regionId, editableFlags.get(clicked), effectiveMax);
                }
            });
        });

        sendForm(player, token, builder);
    }

    private static void openFlagSlotEditor(Player player, String regionId, String flag, int maxSlots) {
        FlagGuiCategory.GuiFlag flagEntry = RedProtect.get().getConfigManager().guiRoot().gui_flags.get(flag);
        if (flagEntry == null) {
            return;
        }
        int currentSlot = Math.max(0, Math.min(maxSlots - 1, flagEntry.slot));
        long token = nextToken(player.getUniqueId());

        CustomForm.Builder builder = CustomForm.builder()
                .title(cleanLocalizedText("gui.editflag", "Edit Flag GUI") + ": " + flag)
                .slider(cleanLocalizedText("bedrock.gui.edit.slot-label", "Target slot (0-" + (maxSlots - 1) + ")"),
                        0, maxSlots - 1, 1, currentSlot);

        builder.validResultHandler(response -> {
            int targetSlot = Math.round(response.asSlider(0));
            runOnMain(() -> {
                if (!isCurrentToken(player.getUniqueId(), token) || !player.isOnline()) {
                    return;
                }
                if (!RedProtect.get().getPermissionHandler().hasCommandPerm(player, "gui-edit")) {
                    RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
                    return;
                }
                if (!BedrockFormStateHelper.isValidGuiSlot(targetSlot, maxSlots)) {
                    RedProtect.get().getLanguageManager().sendMessage(player, "gui.edit.error");
                    return;
                }
                Map<String, FlagGuiCategory.GuiFlag> guiFlags = RedProtect.get().getConfigManager().guiRoot().gui_flags;
                FlagGuiCategory.GuiFlag liveEntry = guiFlags.get(flag);
                if (liveEntry == null) {
                    RedProtect.get().getLanguageManager().sendMessage(player, "gui.edit.error");
                    return;
                }
                int oldSlot = liveEntry.slot;
                // Swap with existing occupant at targetSlot so no two flags silently overlap slots.
                for (Map.Entry<String, FlagGuiCategory.GuiFlag> entry : guiFlags.entrySet()) {
                    if (!entry.getKey().equals(flag) && entry.getValue().slot == targetSlot) {
                        entry.getValue().slot = oldSlot;
                        break;
                    }
                }
                RedProtect.get().getConfigManager().setGuiSlot(flag, targetSlot);
                RedProtect.get().getLanguageManager().sendMessage(player, "gui.edit.ok");
                openFlagEditList(player, regionId, maxSlots);
            });
        });

        sendForm(player, token, builder);
    }

    private static List<String> resolveCollectionNames(boolean isChest, String flag) {
        List<String> result = new ArrayList<>();
        if (isChest) {
            for (Material mat : ChestFlagGui.getContainerMaterials()) {
                result.add(mat.name());
            }
        } else {
            for (EntityType entityType : MobFlagGui.getMobEntities(flag)) {
                result.add(entityType.name());
            }
        }
        return result;
    }

    private static void openCollectionMenu(Player player, String regionId, String flag, boolean isChest, int page) {
        Region region = revalidateRegionAccess(player, regionId);
        if (region == null) {
            return;
        }
        String requiredPerm = isChest ? "redprotect.flag.chest-gui" : "redprotect.flag.spawn-mob-gui";
        if (!RedProtect.get().getPermissionHandler().hasPerm(player, requiredPerm)) {
            RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
            return;
        }

        List<String> allEntries = resolveCollectionNames(isChest, flag);
        int totalPages = BedrockFormStateHelper.computeTotalPages(allEntries.size(), FORM_PAGE_SIZE);
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        List<String> pageEntries = BedrockFormStateHelper.paginate(allEntries, clampedPage, FORM_PAGE_SIZE);

        long token = nextToken(player.getUniqueId());
        String titleKey = isChest ? "gui.title.chest"
                : (flag.equalsIgnoreCase("spawn-monsters") ? "gui.title.spawn-monsters" : "gui.title.spawn-animals");
        String title = BedrockFormStateHelper.stripColorCodes(RedProtect.get().getLanguageManager().get(titleKey));
        Object currentRaw = region.getFlags().get(flag);
        Set<String> selected = BedrockFormStateHelper.resolveSelectedEntries(allEntries, currentRaw);

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(title)
                .content(cleanLocalizedText("bedrock.gui.collection.summary", "Current selection: ")
                        + selected.size() + "/" + allEntries.size())
                .button(BedrockFormStateHelper.stripColorCodes(RedProtect.get().getLanguageManager().get("gui.selectall")))
                .button(BedrockFormStateHelper.stripColorCodes(RedProtect.get().getLanguageManager().get("gui.selectnone")));

        if (!pageEntries.isEmpty()) {
            builder.button(cleanLocalizedText("bedrock.gui.collection.edit-page", "Edit Page ")
                    + (clampedPage + 1) + "/" + totalPages);
        }
        if (totalPages > 1 && clampedPage > 0) {
            builder.button(BedrockFormStateHelper.stripColorCodes(RedProtect.get().getLanguageManager().get("gui.page.previous")));
        }
        if (totalPages > 1 && clampedPage < totalPages - 1) {
            builder.button(BedrockFormStateHelper.stripColorCodes(RedProtect.get().getLanguageManager().get("gui.page.next")));
        }
        builder.button(cleanLocalizedText("bedrock.gui.back", "Back"));

        final boolean hasEditButton = !pageEntries.isEmpty();
        final boolean hasPrevButton = totalPages > 1 && clampedPage > 0;
        final boolean hasNextButton = totalPages > 1 && clampedPage < totalPages - 1;

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            runOnMain(() -> {
                if (!isCurrentToken(player.getUniqueId(), token)) {
                    return;
                }
                Region live = revalidateRegionAccess(player, regionId);
                if (live == null) {
                    return;
                }
                if (!RedProtect.get().getPermissionHandler().hasPerm(player, requiredPerm)) {
                    RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
                    return;
                }

                if (clicked == 0) {
                    applyCollectionFlagValue(player, live, flag, true);
                    openCollectionMenu(player, regionId, flag, isChest, clampedPage);
                    return;
                }
                if (clicked == 1) {
                    applyCollectionFlagValue(player, live, flag, Boolean.FALSE);
                    openCollectionMenu(player, regionId, flag, isChest, clampedPage);
                    return;
                }

                int idx = 2;
                if (hasEditButton) {
                    if (clicked == idx) {
                        openCollectionPageForm(player, regionId, flag, isChest, clampedPage);
                        return;
                    }
                    idx++;
                }
                if (hasPrevButton) {
                    if (clicked == idx) {
                        openCollectionMenu(player, regionId, flag, isChest, clampedPage - 1);
                        return;
                    }
                    idx++;
                }
                if (hasNextButton) {
                    if (clicked == idx) {
                        openCollectionMenu(player, regionId, flag, isChest, clampedPage + 1);
                        return;
                    }
                    idx++;
                }
                if (clicked == idx) {
                    openFlagList(player, regionId, RedProtect.get().getConfigManager().getGuiMaxSlot());
                }
            });
        });

        sendForm(player, token, builder);
    }

    private static void openCollectionPageForm(Player player, String regionId, String flag, boolean isChest, int page) {
        Region region = revalidateRegionAccess(player, regionId);
        if (region == null) {
            return;
        }
        String requiredPerm = isChest ? "redprotect.flag.chest-gui" : "redprotect.flag.spawn-mob-gui";
        if (!RedProtect.get().getPermissionHandler().hasPerm(player, requiredPerm)) {
            RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
            return;
        }
        List<String> allEntries = resolveCollectionNames(isChest, flag);
        int totalPages = BedrockFormStateHelper.computeTotalPages(allEntries.size(), FORM_PAGE_SIZE);
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        List<String> pageEntries = BedrockFormStateHelper.paginate(allEntries, clampedPage, FORM_PAGE_SIZE);
        if (pageEntries.isEmpty()) {
            return;
        }

        Object rawValue = region.getFlags().get(flag);
        long token = nextToken(player.getUniqueId());
        String titleKey = isChest ? "gui.title.chest"
                : (flag.equalsIgnoreCase("spawn-monsters") ? "gui.title.spawn-monsters" : "gui.title.spawn-animals");
        String title = BedrockFormStateHelper.stripColorCodes(RedProtect.get().getLanguageManager().get(titleKey))
                + " (" + (clampedPage + 1) + "/" + totalPages + ")";

        CustomForm.Builder builder = CustomForm.builder().title(title);
        for (String entry : pageEntries) {
            boolean checked = BedrockFormStateHelper.isContainerOrMobSelected(rawValue, entry);
            builder.toggle(entry, checked);
        }

        builder.validResultHandler(response -> {
            List<Boolean> toggles = new ArrayList<>(pageEntries.size());
            for (int i = 0; i < pageEntries.size(); i++) {
                toggles.add(response.asToggle(i));
            }
            runOnMain(() -> {
                if (!isCurrentToken(player.getUniqueId(), token)) {
                    return;
                }
                Region live = revalidateRegionAccess(player, regionId);
                if (live == null) {
                    return;
                }
                if (!RedProtect.get().getPermissionHandler().hasPerm(player, requiredPerm)) {
                    RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
                    return;
                }
                List<String> liveAll = resolveCollectionNames(isChest, flag);
                Set<String> liveSelected = BedrockFormStateHelper.resolveSelectedEntries(liveAll, live.getFlags().get(flag));
                Set<String> updated = BedrockFormStateHelper.applyPageSelection(liveAll, liveSelected, pageEntries, toggles);
                Object serialized = BedrockFormStateHelper.serializeContainerOrMobSelection(liveAll, updated, true);
                applyCollectionFlagValue(player, live, flag, serialized);
                openCollectionMenu(player, regionId, flag, isChest, clampedPage);
            });
        });

        sendForm(player, token, builder);
    }

    private static void applyCollectionFlagValue(Player player, Region region, String flag, Object value) {
        if (!checkFlagDelayReady(player, region, flag)) {
            return;
        }
        if (region.setFlag(player, flag, value)) {
            markFlagDelay(player, region, flag);
            RedProtect.get().getLanguageManager().sendMessage(player,
                    RedProtect.get().getLanguageManager().get("cmdmanager.region.flag.set")
                            .replace("{flag}", "'" + flag + "'") + " " + region.getFlagString(flag));
            RedProtect.get().logger.addLog("(World " + region.getWorld() + ") Player " + player.getName()
                    + " SET FLAG " + flag + " of region " + region.getName() + " to " + region.getFlagString(flag));
        }
    }

    private static void openItemMenu(Player player, String regionId, String flag) {
        Region region = revalidateRegionAccess(player, regionId);
        if (region == null) {
            return;
        }
        if (!RedProtect.get().getPermissionHandler().hasPerm(player, "redprotect.flag.item-gui")) {
            RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
            return;
        }

        List<String> currentItems = BedrockFormStateHelper.parseItemFlagList(region.getFlagString(flag));
        long token = nextToken(player.getUniqueId());
        String title = cleanLocalizedText("bedrock.gui.items.title", "Item Flag") + ": " + flag;

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(title)
                .content(cleanLocalizedText("bedrock.gui.items.subtitle", "Tap an action or tap a registered item to remove it:"))
                .button(cleanLocalizedText("bedrock.gui.items.add-held", "+ Add Held Item"))
                .button(cleanLocalizedText("bedrock.gui.items.add-pick", "+ Pick From Inventory / Enter Material"))
                .button(cleanLocalizedText("bedrock.gui.items.clear", "Clear All (Remove Flag)"));

        for (String itemName : currentItems) {
            builder.button("[-] " + itemName);
        }
        builder.button(cleanLocalizedText("bedrock.gui.back", "Back"));

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            runOnMain(() -> {
                if (!isCurrentToken(player.getUniqueId(), token)) {
                    return;
                }
                Region live = revalidateRegionAccess(player, regionId);
                if (live == null) {
                    return;
                }
                if (!RedProtect.get().getPermissionHandler().hasPerm(player, "redprotect.flag.item-gui")) {
                    RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
                    return;
                }

                if (clicked == 0) {
                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (held == null || !BedrockFormStateHelper.isValidNonAirMaterialName(held.getType().name())) {
                        sendInvalidMaterialMessage(player, "Hold a valid item or enter a valid Bukkit Material name.");
                        openItemMenu(player, regionId, flag);
                        return;
                    }
                    List<String> updated = BedrockFormStateHelper.parseItemFlagList(live.getFlagString(flag));
                    if (!updated.contains(held.getType().name())) {
                        updated.add(held.getType().name());
                    }
                    saveItemFlagList(player, live, flag, updated);
                    openItemMenu(player, regionId, flag);
                    return;
                }
                if (clicked == 1) {
                    openAddItemForm(player, regionId, flag);
                    return;
                }
                if (clicked == 2) {
                    saveItemFlagList(player, live, flag, List.of());
                    openItemMenu(player, regionId, flag);
                    return;
                }

                int itemIndex = clicked - 3;
                if (itemIndex >= 0 && itemIndex < currentItems.size()) {
                    String toRemove = currentItems.get(itemIndex);
                    List<String> updated = BedrockFormStateHelper.parseItemFlagList(live.getFlagString(flag));
                    if (updated.remove(toRemove)) {
                        saveItemFlagList(player, live, flag, updated);
                    }
                    openItemMenu(player, regionId, flag);
                    return;
                }
                if (itemIndex == currentItems.size()) {
                    openFlagList(player, regionId, RedProtect.get().getConfigManager().getGuiMaxSlot());
                }
            });
        });

        sendForm(player, token, builder);
    }

    private static void openAddItemForm(Player player, String regionId, String flag) {
        Region region = revalidateRegionAccess(player, regionId);
        if (region == null) {
            return;
        }
        if (!RedProtect.get().getPermissionHandler().hasPerm(player, "redprotect.flag.item-gui")) {
            RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
            return;
        }
        List<String> invOptions = new ArrayList<>();
        invOptions.add("(Manual input below)");
        Set<String> seen = new LinkedHashSet<>();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && BedrockFormStateHelper.isValidNonAirMaterialName(stack.getType().name())) {
                if (seen.add(stack.getType().name())) {
                    invOptions.add(stack.getType().name());
                }
            }
        }

        long token = nextToken(player.getUniqueId());
        CustomForm.Builder builder = CustomForm.builder()
                .title(cleanLocalizedText("bedrock.gui.items.add-title", "Add Material") + ": " + flag)
                .dropdown(cleanLocalizedText("bedrock.gui.items.inv-label", "Pick from inventory (optional)"), invOptions, 0)
                .input(cleanLocalizedText("bedrock.gui.items.input-label", "Or enter Material name (e.g. DIAMOND_SWORD)"), "DIAMOND_SWORD", "");

        builder.validResultHandler(response -> {
            int dropIdx = response.asDropdown(0);
            String typed = response.asInput(1);
            runOnMain(() -> {
                if (!isCurrentToken(player.getUniqueId(), token)) {
                    return;
                }
                Region live = revalidateRegionAccess(player, regionId);
                if (live == null) {
                    return;
                }
                if (!RedProtect.get().getPermissionHandler().hasPerm(player, "redprotect.flag.item-gui")) {
                    RedProtect.get().getLanguageManager().sendMessage(player, "no.permission");
                    return;
                }

                String candidate = BedrockFormStateHelper.normalizeMaterialQuery(typed);
                if (candidate.isEmpty() && dropIdx > 0 && dropIdx < invOptions.size()) {
                    candidate = invOptions.get(dropIdx);
                }
                Material matched = candidate.isEmpty() ? null : Material.matchMaterial(candidate);
                if (matched == null || !BedrockFormStateHelper.isValidNonAirMaterialName(matched.name())) {
                    sendInvalidMaterialMessage(player, "Invalid Material name.");
                    openItemMenu(player, regionId, flag);
                    return;
                }
                List<String> updated = BedrockFormStateHelper.parseItemFlagList(live.getFlagString(flag));
                if (!updated.contains(matched.name())) {
                    updated.add(matched.name());
                }
                saveItemFlagList(player, live, flag, updated);
                openItemMenu(player, regionId, flag);
            });
        });

        sendForm(player, token, builder);
    }

    private static void saveItemFlagList(Player player, Region region, String flag, List<String> materialNames) {
        if (!checkFlagDelayReady(player, region, flag)) {
            return;
        }
        String serialized = BedrockFormStateHelper.serializeItemFlagList(materialNames);
        if (serialized != null) {
            if (region.setFlag(player, flag, serialized)) {
                markFlagDelay(player, region, flag);
                RedProtect.get().getLanguageManager().sendMessage(player,
                        RedProtect.get().getLanguageManager().get("cmdmanager.region.flag.set")
                                .replace("{flag}", "'" + flag + "'") + " " + region.getFlagString(flag));
                RedProtect.get().logger.addLog("(World " + region.getWorld() + ") Player " + player.getName()
                        + " SET FLAG " + flag + " of region " + region.getName() + " to " + region.getFlagString(flag));
            }
        } else {
            if (region.removeFlag(player, flag)) {
                markFlagDelay(player, region, flag);
                RedProtect.get().getLanguageManager().sendMessage(player,
                        RedProtect.get().getLanguageManager().get("cmdmanager.region.flag.removed")
                                .replace("{flag}", flag).replace("{region}", region.getName()));
                RedProtect.get().logger.addLog("(World " + region.getWorld() + ") Player " + player.getName()
                        + " REMOVED FLAG " + flag + " of region " + region.getName());
            }
        }
    }
}
