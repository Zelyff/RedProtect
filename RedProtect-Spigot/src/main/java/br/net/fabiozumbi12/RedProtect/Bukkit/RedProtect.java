/*
 * Copyright (c) 2012-2025 - @FabioZumbi12
 * Last Modified: 19/08/2025 15:34
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

package br.net.fabiozumbi12.RedProtect.Bukkit;

import br.net.fabiozumbi12.RedProtect.Bukkit.API.RedProtectAPI;
import br.net.fabiozumbi12.RedProtect.Bukkit.claim.ClaimSessionManager;
import br.net.fabiozumbi12.RedProtect.Bukkit.claim.GpClaimInteractionHandler;
import br.net.fabiozumbi12.RedProtect.Bukkit.config.LangGuiManager;
import br.net.fabiozumbi12.RedProtect.Bukkit.config.LangManager;
import br.net.fabiozumbi12.RedProtect.Bukkit.commands.CommandHandler;
import br.net.fabiozumbi12.RedProtect.Bukkit.config.BlockConfig;
import br.net.fabiozumbi12.RedProtect.Bukkit.config.ConfigManager;
import br.net.fabiozumbi12.RedProtect.Bukkit.helpers.*;
import br.net.fabiozumbi12.RedProtect.Bukkit.listeners.*;
import br.net.fabiozumbi12.RedProtect.Bukkit.hooks.HooksManager;
import br.net.fabiozumbi12.RedProtect.Bukkit.metrics.Metrics;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.GlowOutlineOverlay;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.PreviewRenderMode;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.PreviewVisualType;
import br.net.fabiozumbi12.RedProtect.Bukkit.preview.RegionPreviewService;
import br.net.fabiozumbi12.RedProtect.Bukkit.region.RegionManager;
import br.net.fabiozumbi12.RedProtect.Bukkit.schematics.RPSchematics;
import br.net.fabiozumbi12.RedProtect.Core.helpers.CoreUtil;
import br.net.fabiozumbi12.RedProtect.Core.helpers.LogLevel;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class RedProtect extends JavaPlugin {
    private static RedProtect plugin;
    public final List<String> confirmStart = new ArrayList<>();
    public final HashMap<String, List<String>> denyEnter = new HashMap<>();
    public final HashMap<Player, Location> firstLocationSelections = new HashMap<>();
    public final HashMap<Player, Location> secondLocationSelections = new HashMap<>();
    public final HashMap<Player, String> alWait = new HashMap<>();
    public final List<String> changeWait = new ArrayList<>();
    public final List<String> tpWait = new ArrayList<>();
    public final HooksManager hooks = new HooksManager();
    public final RedProtectLogger logger = new RedProtectLogger();
    public final List<String> teleportDelay = new ArrayList<>();
    public RPSchematics schematic;
    public String bukkitVersion;
    public Economy economy;
    public Permission permission;
    public LangGuiManager guiLang;
    protected CommandHandler cmdHandler;
    private RedProtectUtil redProtectUtil = new RedProtectUtil(this);
    private RedProtectAPI redProtectAPI;
    private int autoSaveID;
    private VersionHelper rpvHelper;
    private ModListener modListener;
    private RegionManager rm;
    private PermissionHandler ph;
    private LangManager lang;
    private ConfigManager config;
    private BlockConfig blockConfig;
    private RegionPreviewService previewService;
    private ClaimSessionManager claimSessionManager;
    private GpClaimInteractionHandler gpClaimHandler;

    public static RedProtect get() {
        return plugin;
    }

    public RegionPreviewService getPreviewService() {
        return this.previewService;
    }

    public ClaimSessionManager getClaimSessionManager() {
        return this.claimSessionManager;
    }

    public GpClaimInteractionHandler getGpClaimHandler() {
        return this.gpClaimHandler;
    }

    public RegionManager getRegionManager() {
        return this.rm;
    }

    public PermissionHandler getPermissionHandler() {
        return this.ph;
    }

    public LangManager getLanguageManager() {
        return this.lang;
    }

    public ConfigManager getConfigManager() {
        return this.config;
    }

    public BlockConfig getBlockManager() {
        return this.blockConfig;
    }

    public VersionHelper getVersionHelper() {
        return rpvHelper;
    }

    public RedProtectAPI getAPI() {
        return redProtectAPI;
    }

    public RedProtectUtil getUtil() {
        return this.redProtectUtil;
    }

    public void onDisable() {
        shutDown();
    }

    public void onEnable() {
        try {
            plugin = this;

            ph = new PermissionHandler();
            rm = new RegionManager();

            // Init schematic
            schematic = new RPSchematics();

            //Init config, lang, listeners and flags
            startLoad();

            logger.info("Loading API...");
            this.redProtectAPI = new RedProtectAPI();
            logger.info("API Loaded!");

            logger.clear("&4 _   _  _  &c _   _   _  _ _  _  _ _ _  _");
            logger.clear("&4|_| |_ | \\ &c|_| |_| | |  |  |_ |   |  |_|");
            logger.clear("&4| \\ |_ |_/ &c|   | \\ |_|  |  |_ |_  |  |_|");
            logger.clear("&a» " + getDescription().getFullName() + " &7(Version by &bZelyff&7) &aenabled");
            logger.clear("");

            if (config.configRoot().enable_addons){
                AddonsManager.EnableAddons();
            }

        } catch (Exception e) {
            CoreUtil.printJarVersion();
            e.printStackTrace();

            getServer().setWhitelist(true);
            getServer().getOnlinePlayers().forEach(p -> p.kickPlayer("The server has been whitelisted due to an error while loading plugins!"));
            this.setEnabled(false);

            logger.severe("Due to an error in RedProtect loading, the whitelist has been turned on and every player has been kicked.");
            logger.severe("DO NOT LET ANYONE ENTER before fixing the problem, otherwise you risk losing protected regions.");
        }
    }

    public void reload() {
        try {
            //shutdown
            shutDown();

            //start
            startLoad();
        } catch (Exception e) {
            CoreUtil.printJarVersion();
            e.printStackTrace();
        }
    }

    public void reloadConfigs() {
        if (claimSessionManager != null) {
            claimSessionManager.clearAll();
        }
        if (previewService != null) {
            previewService.clearAll();
        }
        br.net.fabiozumbi12.RedProtect.Bukkit.guis.BedrockGuiForms.clearAllTokens();
        br.net.fabiozumbi12.RedProtect.Bukkit.guis.FlagGui.clearOpenGuis();
        cmdHandler.unregisterAll();

        try {
            config = new ConfigManager();
        } catch (ObjectMappingException e) {
            CoreUtil.printJarVersion();
            e.printStackTrace();
        }

        logger.info("Loading language files...");
        lang = new LangManager();
        guiLang = new LangGuiManager();
        blockConfig = new BlockConfig();

        logger.info("Re-registering commands...");
        cmdHandler = new CommandHandler(this);
    }

    private void startLoad() throws Exception {
        bukkitVersion = getBukkitVersion();
        redProtectUtil = new RedProtectUtil(this);
        config = new ConfigManager();
        lang = new LangManager();
        blockConfig = new BlockConfig();
        if (claimSessionManager != null) {
            claimSessionManager.clearAll();
        }
        if (previewService != null) {
            previewService.clearAll();
        }
        br.net.fabiozumbi12.RedProtect.Bukkit.guis.BedrockGuiForms.clearAllTokens();
        br.net.fabiozumbi12.RedProtect.Bukkit.guis.FlagGui.clearOpenGuis();
        previewService = createPreviewService();
        claimSessionManager = new ClaimSessionManager();
        gpClaimHandler = new GpClaimInteractionHandler(this, claimSessionManager);

        if (config.configRoot().purge.regen.enabled && config.configRoot().purge.regen.enable_whitelist_regen && Bukkit.getServer().hasWhitelist()) {
            Bukkit.getServer().setWhitelist(false);
            RedProtect.get().logger.success("Whitelist disabled!");
        }

        logger.info("Registering commands...");
        cmdHandler = new CommandHandler(this);

        logger.info("Registering listeners...");
        getServer().getPluginManager().registerEvents(cmdHandler, this);
        getServer().getPluginManager().registerEvents(new GlobalListener(), this);
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new EntityListener(), this);
        getServer().getPluginManager().registerEvents(new WorldListener(), this);

        // Register hooks
        hooks.registerHooksFirst();

        rm = new RegionManager();
        rm.loadAll();

        redProtectUtil.ReadAllDB(rm.getAllRegions());

        rm.saveAll(false);

        if (!config.configRoot().file_type.equalsIgnoreCase("mysql")) {
            startAutoSave();
        }
        logger.info("There are " + rm.getTotalRegionsNum() + " regions on (" + config.configRoot().file_type + ") database!");

        // Register hooks
        hooks.registerHooksLast();

        // Load Gui lang file
        guiLang = new LangGuiManager();

        // Update Manager (disabled on custom Zelyff fork to prevent upstream SpigotMC jar overwrite)
        if (config.configRoot().update.enable) {
            logger.info("Spiget auto-updater is disabled on this custom build (Version by Zelyff) to prevent overwriting custom features.");
        }

        bukkitVersion = getBukkitVersion();
        logger.debug(LogLevel.DEFAULT, "Version String: " + bukkitVersion);

        // Load all version compats classes
        getServer().getPluginManager().registerEvents(new Compat18(), this);
        getServer().getPluginManager().registerEvents(new Compat19(), this);
        getServer().getPluginManager().registerEvents(new Compat111(), this);
        getServer().getPluginManager().registerEvents(new Compat114(), this);

        if (isBukkitVersionAtLeast(1, 19)) {
            rpvHelper = new VersionHelperLatest();
        } else {
            logger.severe("RedProtect 8+ is not compatible with version 1.18 or lower. Download the latest RedProtect 7 from spigot or dev builds.");
            shutDown();
            return;
        }
        // Register as listener
        getServer().getPluginManager().registerEvents(rpvHelper, this);

        try {
            Class.forName("io.papermc.paper.event.entity.EntityKnockbackEvent");
            getServer().getPluginManager().registerEvents(new PaperListener(), this);
        } catch (ClassNotFoundException ignored) {}

        // Print helper version
        logger.info("Helper version: " + rpvHelper.getVersion());

        // Load gui items for Minecraft version
        config.setGuiRoot(rpvHelper.setGuiItems(config.guiRoot()));

        // Load mod permissions
        if (config.configRoot().server_protection.modsPermissionsEnable)
            modListener = new ModListener(this);

        // Metrics
        try {
            Metrics metrics = new Metrics(this);
            metrics.addCustomChart(new Metrics.SingleLineChart("server_regions", () -> rm.getAllRegions().size()));
            if (metrics.isEnabled())
                logger.info("Metrics enabled! See our stats here: https://bstats.org/plugin/bukkit/RedProtect");
        } catch (Exception ex) {
            logger.info("Metrics not enabled due errors: " + ex.getLocalizedMessage());
        }
    }

    private void shutDown() {
        if (claimSessionManager != null) {
            claimSessionManager.clearAll();
        }
        if (previewService != null) {
            previewService.clearAll();
        }
        br.net.fabiozumbi12.RedProtect.Bukkit.guis.BedrockGuiForms.clearAllTokens();
        br.net.fabiozumbi12.RedProtect.Bukkit.guis.FlagGui.clearOpenGuis();

        // Unload messages listener
        if (modListener != null)
            modListener.unload();

        // Unregister commands
        cmdHandler.unregisterAll();

        // Save and unload all regions
        rm.saveAll(true);
        rm.unloadAll();

        // Cancel tasks from bukkit scheduler and save logs
        Bukkit.getScheduler().cancelTasks(this);
        logger.saveLogs();

        // Unregister listeners
        logger.info("Unregistering listeners...");
        HandlerList.unregisterAll(this);

        getServer().getScheduler().cancelTasks(this);

        logger.info(getDescription().getFullName() + " (Version by Zelyff) turned off...");
    }

    public boolean denyEnterRegion(String rid, String player) {
        if (denyEnter.containsKey(player)) {
            if (denyEnter.get(player).contains(rid)) {
                return false;
            }
            List<String> regs = denyEnter.get(player);
            regs.add(rid);
            denyEnter.put(player, regs);
        } else {
            denyEnter.put(player, new LinkedList<>(Collections.singletonList(rid)));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (denyEnter.containsKey(player)) {
                List<String> regs = denyEnter.get(player);
                regs.remove(rid);
                if (regs.isEmpty()) {
                    denyEnter.remove(player);
                } else {
                    denyEnter.put(player, regs);
                }
            }
        }, config.configRoot().region_settings.delay_after_kick_region * 20L);
        return true;
    }

    private String getBukkitVersion() {
        String raw = Bukkit.getServer().getBukkitVersion().split("-")[0];
        String[] parts = raw.split("\\.");
        int len = Math.min(parts.length, 3);
        return String.join(".", java.util.Arrays.copyOf(parts, len));
    }

    public boolean isBukkitVersionAtLeast(int major, int minor) {
        if (this.bukkitVersion == null) {
            this.bukkitVersion = getBukkitVersion();
        }
        String[] parts = bukkitVersion.split("\\.");
        int m = Integer.parseInt(parts[0]);
        int n = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return m > major || (m == major && n >= minor);
    }

    private void startAutoSave() {
        Bukkit.getScheduler().cancelTask(autoSaveID);
        if (config.configRoot().flat_file.auto_save_interval_seconds != 0) {
            logger.info("Auto-save Scheduler: Saving " + config.configRoot().file_type + " database every " + config.configRoot().flat_file.auto_save_interval_seconds / 60 + " minutes!");

            autoSaveID = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                logger.debug(LogLevel.DEFAULT, "Auto-save Scheduler: Saving " + config.configRoot().file_type + " database!");
                rm.saveAll(config.configRoot().flat_file.backup_on_save);
            }, config.configRoot().flat_file.auto_save_interval_seconds * 20L, config.configRoot().flat_file.auto_save_interval_seconds * 20L).getTaskId();

        } else {
            logger.info("Auto-save Scheduler: Disabled");
        }
    }

    private RegionPreviewService createPreviewService() {
        return new RegionPreviewService(
                this,
                () -> {
                    var border = getConfigManager().configRoot().region_settings.border;
                    return new RegionPreviewService.PreviewSettings(
                            PreviewRenderMode.fromConfig(border.mode, PreviewRenderMode.BLOCK),
                            border.particle,
                            border.time_showing,
                            border.height,
                            border.intensity,
                            border.glow_outline,
                            border.fake_block_step,
                            border.fake_block_radius,
                            border.fake_block_max_elements,
                            border.glow_max_entities,
                            getConfigManager().configRoot().region_settings.default_leader
                    );
                },
                new RegionPreviewService.SchedulerBridge() {
                    @Override
                    public int scheduleRepeating(long delayTicks, long periodTicks, Runnable task) {
                        return Bukkit.getScheduler().scheduleSyncRepeatingTask(RedProtect.this, task, delayTicks, periodTicks);
                    }

                    @Override
                    public int scheduleLater(long delayTicks, Runnable task) {
                        return Bukkit.getScheduler().runTaskLater(RedProtect.this, task, delayTicks).getTaskId();
                    }

                    @Override
                    public void cancelTask(int taskId) {
                        Bukkit.getScheduler().cancelTask(taskId);
                    }
                },
                new RegionPreviewService.ParticleBridge() {
                    @Override
                    public String resolveParticleName(String configuredName) {
                        return rpvHelper != null && rpvHelper.existParticle(configuredName) ? configuredName : "FLAME";
                    }

                    @Override
                    public void spawn(org.bukkit.World world, String particleName, double x, double y, double z) {
                        if (rpvHelper != null) {
                            rpvHelper.spawnParticle(world, particleName, x, y, z);
                        }
                    }
                },
                new GlowOutlineOverlay(),
                PreviewVisualType::createBlockData,
                RegionPreviewService::isFloodgatePlayer
        );
    }
}