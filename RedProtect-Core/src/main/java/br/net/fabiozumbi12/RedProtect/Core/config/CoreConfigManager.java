/*
 * Copyright (c) 2012-2024 - @FabioZumbi12
 * Last Modified: 26/11/2024 18:02
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

package br.net.fabiozumbi12.RedProtect.Core.config;

import br.net.fabiozumbi12.RedProtect.Core.config.Category.EconomyCategory;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.FlagGuiCategory;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.GlobalFlagsCategory;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.HeadTexturesCategory;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.MainCategory;
import br.net.fabiozumbi12.RedProtect.Core.helpers.CoreUtil;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.reflect.TypeToken.of;

public class CoreConfigManager {

    public static final List<String> ADMIN_FLAGS = new ArrayList<>(Arrays.asList(
            "spawn-wither",
            "cropsfarm",
            "keep-inventory",
            "keep-levels",
            "can-drop",
            "can-pickup",
            "cmd-onhealth",
            "can-death",
            "max-players",
            "forcefly",
            "gamemode",
            "player-damage",
            "can-hunger",
            "can-projectiles",
            "allow-place",
            "allow-break",
            "deny-place",
            "deny-break",
            "can-pet",
            "allow-cmds",
            "deny-cmds",
            "allow-create-portal",
            "portal-exit",
            "portal-enter",
            "allow-mod",
            "allow-enter-items",
            "deny-enter-items",
            "pvparena",
            "player-enter-command",
            "server-enter-command",
            "player-exit-command",
            "server-exit-command",
            "invincible",
            "effects",
            "treefarm",
            "minefarm",
            "pvp",
            "sign",
            "enderpearl",
            "enter",
            "up-skills",
            "can-back",
            "for-sale",
            "set-portal",
            "respawn-location",
            "exit",
            "particles",
            "show-map",
            "deny-exit-items",
            "spawn-animals",
            "spawn-monsters",
            "can-move",
            "can-collide",
            "push-and-pull"));
    public final HashMap<String, String> backupGuiName = new HashMap<>();
    public final HashMap<String, String> backupGuiDescription = new HashMap<>();
    protected final String headerCfg = """
            +--------------------------------------------------------------------+ #
            <               RedProtect World configuration File                  > #
            <--------------------------------------------------------------------> #
            <       This is the configuration file, feel free to edit it.        > #
            <        For more info about cmds and flags, check our Wiki:         > #
            <         https://github.com/FabioZumbi12/RedProtect/wiki            > #
            +--------------------------------------------------------------------+ #

            Notes:
            Lists use YAML sequence syntax (- object1) or [object1, object2]
            Strings containing the char & always need to be quoted""";
    protected final String headerGf = """
            +--------------------------------------------------------------------+ #
            <          RedProtect Global Flags configuration File                > #
            <--------------------------------------------------------------------> #
            <         This is the global flags configuration file.               > #
            <                       Feel free to edit it.                        > #
            <  https://github.com/FabioZumbi12/RedProtect/wiki/(05)-Region-Flags > #
            +--------------------------------------------------------------------+ #

            Notes:
            Lists use YAML sequence syntax (- object1) or [object1, object2]
            Strings containing the char & always need to be quoted""";
    protected final String headerGui = """
            +--------------------------------------------------------------------+ #
            <             RedProtect Gui Flags configuration File                > #
            <--------------------------------------------------------------------> #
            <            This is the gui flags configuration file.               > #
            <                       Feel free to edit it.                        > #
            <  https://github.com/FabioZumbi12/RedProtect/wiki/(05)-Region-Flags > #
            +--------------------------------------------------------------------+ #
            """;
    protected final String headerEco = """
            +--------------------------------------------------------------------+ #
            <              RedProtect Economy configuration File                 > #
            <--------------------------------------------------------------------> #
            <               This is the economy file configuration               > #
            <  This file its for '/rp value' command as a reference values only  > #
            <   https://github.com/FabioZumbi12/RedProtect/wiki/(03)-Commands    > #
            +--------------------------------------------------------------------+ #

            Notes:
            Lists use YAML sequence syntax (- object1) or [object1, object2]
            Strings containing the char & always need to be quoted""";
    protected final String headerHeadtext = """
            +--------------------------------------------------------------------+ #
            <              RedProtect Head Textures configuration File                 > #
            <--------------------------------------------------------------------> #
            <               This is the Head Textures file configuration         > #
            +--------------------------------------------------------------------+ #

            Notes:
            Lists use YAML sequence syntax (- object1) or [object1, object2]
            Strings containing the char & always need to be quoted""";
    protected final File pluginFolder;
    protected File ecoFile;
    protected ConfigurationNode ecoCfgRoot;
    protected ConfigurationLoader<ConfigurationNode> ecoLoader;
    protected EconomyCategory ecoRoot;
    protected File headTextFile;
    protected ConfigurationNode headTextCfgRoot;
    protected ConfigurationLoader<ConfigurationNode> headTextLoader;
    protected HeadTexturesCategory headTextRoot;
    protected File signsFile;
    protected ConfigurationNode signCfgs;
    protected ConfigurationLoader<ConfigurationNode> signsLoader;
    protected File guiFile;
    protected ConfigurationNode guiCfgRoot;
    protected ConfigurationLoader<ConfigurationNode> guiCfgLoader;
    protected FlagGuiCategory guiRoot;
    protected File gFlagsFile;
    protected ConfigurationLoader<ConfigurationNode> gFlagsLoader;
    protected ConfigurationNode gflagsRoot;
    protected GlobalFlagsCategory globalFlagsRoot;
    protected File configFile;
    protected ConfigurationNode configRoot;
    protected ConfigurationLoader<ConfigurationNode> cfgLoader;
    protected MainCategory root;

    protected CoreConfigManager(File pluginFolder) {
        this.pluginFolder = pluginFolder;
        if (!pluginFolder.exists()) {
            pluginFolder.mkdir();
        }
        if (!new File(pluginFolder, "data").exists()) {
            new File(pluginFolder, "data").mkdir();
        }
        this.configFile = new File(pluginFolder, "config.yml");
        this.gFlagsFile = new File(pluginFolder, "globalflags.yml");
        this.guiFile = new File(pluginFolder, "guiconfig.yml");
        this.ecoFile = new File(pluginFolder, "economy.yml");
        this.headTextFile = new File(pluginFolder, "textures.yml");
        this.signsFile = new File(pluginFolder, "signs.yml");
    }

    public HashMap<String, Object> getDefFlagsValues() {
        return new HashMap<>(root.flags);
    }

    public SortedSet<String> getDefFlags() {
        return new TreeSet<>(getDefFlagsValues().keySet());
    }
    public FlagGuiCategory guiRoot() {
        return this.guiRoot;
    }

    public void setGuiRoot(FlagGuiCategory flagGuiCategory) {
        this.guiRoot = flagGuiCategory;
        saveGui();
    }

    public HeadTexturesCategory headTextRoot() {
        return this.headTextRoot;
    }

    public GlobalFlagsCategory globalFlagsRoot() {
        return this.globalFlagsRoot;
    }
    public EconomyCategory ecoRoot() {
        return this.ecoRoot;
    }

    public MainCategory configRoot() {
        return this.root;
    }

    public int getGuiSlot(String flag) {
        FlagGuiCategory.GuiFlag entry = guiRoot != null && guiRoot.gui_flags != null ? guiRoot.gui_flags.get(flag) : null;
        return entry != null ? entry.slot : 0;
    }

    public void setGuiSlot(String flag, int slot) {
        FlagGuiCategory.GuiFlag entry = guiRoot != null && guiRoot.gui_flags != null ? guiRoot.gui_flags.get(flag) : null;
        if (entry == null) {
            return;
        }
        entry.slot = slot;
        saveGui();
    }

    public int getGuiMaxSlot() {
        if (guiRoot == null || guiRoot.gui_flags == null || guiRoot.gui_flags.isEmpty()) {
            return 0;
        }
        int maxSlot = -1;
        for (FlagGuiCategory.GuiFlag key : guiRoot.gui_flags.values()) {
            if (key != null && key.slot > maxSlot) {
                maxSlot = key.slot;
            }
        }
        return maxSlot >= 0 ? maxSlot + 1 : 0;
    }


    private void saveConfig() {
        try {
            configRoot.setValue(of(MainCategory.class), root);
            YamlConfigMigrator.saveYamlWithComments(configFile, configRoot, headerCfg, MainCategory.class);
        } catch (IOException | ObjectMappingException e) {
            CoreUtil.printJarVersion();
            e.printStackTrace();
        }
    }

    protected void saveGFlags() {
        try {
            gflagsRoot.setValue(of(GlobalFlagsCategory.class), globalFlagsRoot);
            YamlConfigMigrator.saveYamlWithComments(gFlagsFile, gflagsRoot, headerGf, GlobalFlagsCategory.class);
        } catch (IOException | ObjectMappingException e) {
            CoreUtil.printJarVersion();
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            saveConfig();
            saveGFlags();

            ecoCfgRoot.setValue(of(EconomyCategory.class), ecoRoot);
            YamlConfigMigrator.saveYamlWithComments(ecoFile, ecoCfgRoot, headerEco, EconomyCategory.class);
            YamlConfigMigrator.saveYamlWithComments(signsFile, signCfgs, null, null);
            saveHeadTextures();
            saveGui();
        } catch (IOException | ObjectMappingException e) {
            CoreUtil.printJarVersion();
            e.printStackTrace();
        }
    }

    public void saveHeadTextures() {
        try {
            headTextCfgRoot.setValue(of(HeadTexturesCategory.class), headTextRoot);
            YamlConfigMigrator.saveYamlWithComments(headTextFile, headTextCfgRoot, headerHeadtext, HeadTexturesCategory.class);
        } catch (IOException | ObjectMappingException e) {
            CoreUtil.printJarVersion();
            e.printStackTrace();
        }
    }

    public void saveGui() {
        try {
            guiCfgRoot.setValue(of(FlagGuiCategory.class), guiRoot);
            YamlConfigMigrator.saveYamlWithComments(guiFile, guiCfgRoot, headerGui, FlagGuiCategory.class);
        } catch (IOException | ObjectMappingException e) {
            CoreUtil.printJarVersion();
            e.printStackTrace();
        }
    }

    public boolean addFlag(String flag, boolean defaultValue, boolean isAdmin) {
        if (isAdmin) {
            if (!ADMIN_FLAGS.contains(flag)) {
                ADMIN_FLAGS.add(flag);
                return true;
            }
        } else {
            if (!root.flags.containsKey(flag)) {
                root.flags.put(flag, defaultValue);
                saveConfig();
                return true;
            }
        }
        return false;
    }

    public boolean removeFlag(String flag, boolean isAdmin) {
        if (isAdmin) {
            return ADMIN_FLAGS.remove(flag);
        } else {
            return root.flags.remove(flag) != null;
        }
    }

    public String getWorldClaimType(String w) {
        return root.region_settings.claim.world_types.getOrDefault(w, "");
    }

}
