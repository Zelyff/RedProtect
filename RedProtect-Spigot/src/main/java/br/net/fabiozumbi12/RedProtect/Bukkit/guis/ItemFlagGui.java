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

package br.net.fabiozumbi12.RedProtect.Bukkit.guis;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Core.helpers.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemFlagGui implements Listener {

    static final int GUI_SIZE = 54;
    static final int ITEMS_PER_PAGE = 45;
    static final int NAV_PREV_SLOT = 45;
    static final int NAV_INFO_SLOT = 49;
    static final int NAV_NEXT_SLOT = 53;

    private final String flag;
    private final Player player;
    private final List<String> selectedMaterials = new ArrayList<>();
    private Region region;
    private Inventory inv;
    private int page = 0;
    private boolean closed = false;

    public ItemFlagGui(Player player, Region region, String flag) {
        this.player = player;
        this.region = region;
        this.flag = flag;
        parseFlagItems(region != null ? region.getFlagString(flag) : "");
    }

    /**
     * Parses CSV material names into sorted distinct uppercase entries, keeping both items and block-only materials.
     */
    static List<String> parseMaterials(String rawValue) {
        List<String> result = new ArrayList<>();
        if (rawValue == null || rawValue.isBlank()) {
            return result;
        }
        for (String token : rawValue.split(",")) {
            String trimmed = token.trim().toUpperCase();
            if (trimmed.isEmpty() || "TRUE".equals(trimmed) || "FALSE".equals(trimmed)) {
                continue;
            }
            Material mat = Material.getMaterial(trimmed);
            if (mat != null && mat != Material.AIR && !result.contains(mat.name())) {
                result.add(mat.name());
            }
        }
        Collections.sort(result);
        return result;
    }

    private void parseFlagItems(String rawValue) {
        selectedMaterials.clear();
        selectedMaterials.addAll(parseMaterials(rawValue));
    }

    int getTotalPages() {
        return Math.max(1, (selectedMaterials.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
    }

    int getPage() {
        return page;
    }

    List<String> getSelectedMaterials() {
        return Collections.unmodifiableList(selectedMaterials);
    }

    boolean addMaterial(String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return false;
        }
        Material mat = Material.getMaterial(materialName.trim().toUpperCase());
        if (mat == null || mat == Material.AIR || selectedMaterials.contains(mat.name())) {
            return false;
        }
        selectedMaterials.add(mat.name());
        Collections.sort(selectedMaterials);
        return true;
    }

    boolean removeAtDisplaySlot(int slot) {
        if (slot < 0 || slot >= ITEMS_PER_PAGE) {
            return false;
        }
        int idx = page * ITEMS_PER_PAGE + slot;
        if (idx < 0 || idx >= selectedMaterials.size()) {
            return false;
        }
        selectedMaterials.remove(idx);
        if (page > 0 && page >= getTotalPages()) {
            page = getTotalPages() - 1;
        }
        return true;
    }

    String formatFlagValue() {
        return String.join(",", selectedMaterials);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onInventoryClick(InventoryClickEvent event) {
        if (this.inv == null || !event.getView().getTopInventory().equals(this.inv)) {
            return;
        }
        event.setCancelled(true);
        if (!event.getWhoClicked().equals(this.player) || this.closed) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < ITEMS_PER_PAGE) {
            if (removeAtDisplaySlot(rawSlot)) {
                renderPage();
            }
            return;
        }
        if (rawSlot == NAV_PREV_SLOT && page > 0) {
            page--;
            renderPage();
            return;
        }
        if (rawSlot == NAV_NEXT_SLOT && (page + 1) < getTotalPages()) {
            page++;
            renderPage();
            return;
        }
        if (rawSlot >= GUI_SIZE) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR && addMaterial(clicked.getType().name())) {
                renderPage();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onInventoryDrag(InventoryDragEvent event) {
        if (this.inv != null && event.getView().getTopInventory().equals(this.inv)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onInventoryCreative(InventoryCreativeEvent event) {
        if (this.inv != null && event.getView().getTopInventory().equals(this.inv)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onInventoryClose(InventoryCloseEvent event) {
        if (this.closed || this.inv == null || !event.getInventory().equals(this.inv) || !event.getView().getPlayer().equals(this.player)) {
            return;
        }

        if (this.region != null) {
            String value = formatFlagValue();
            if (!value.isEmpty()) {
                if (region.setFlag(event.getPlayer(), flag, value)) {
                    RedProtect.get().getLanguageManager().sendMessage(player, RedProtect.get().getLanguageManager().get("cmdmanager.region.flag.set").replace("{flag}", "'" + flag + "'") + " " + region.getFlagString(flag));
                    RedProtect.get().logger.addLog("(World " + region.getWorld() + ") Player " + player.getName() + " SET FLAG " + flag + " of region " + region.getName() + " to " + region.getFlagString(flag));
                }
            } else {
                if (region.removeFlag(event.getPlayer(), flag)) {
                    RedProtect.get().getLanguageManager().sendMessage(player, RedProtect.get().getLanguageManager().get("cmdmanager.region.flag.removed").replace("{flag}", flag).replace("{region}", region.getName()));
                    RedProtect.get().logger.addLog("(World " + region.getWorld() + ") Player " + player.getName() + " REMOVED FLAG " + flag + " of region " + region.getName());
                }
            }
        }

        close(false);
    }

    @EventHandler
    void onDeath(PlayerDeathEvent event) {
        if (event.getEntity().equals(this.player)) {
            close(true);
        }
    }

    @EventHandler
    void onPlayerLogout(PlayerQuitEvent event) {
        if (event.getPlayer().equals(this.player)) {
            close(true);
        }
    }

    @EventHandler
    void onPluginDisable(PluginDisableEvent event) {
        if (!event.getPlugin().equals(RedProtect.get())) {
            return;
        }
        RedProtect.get().logger.debug(LogLevel.DEFAULT, "Is PluginDisableEvent event.");
        close(true);
    }

    private void close(boolean closeInventory) {
        if (this.closed) {
            return;
        }
        this.closed = true;
        HandlerList.unregisterAll(this);

        if (closeInventory && this.player.isOnline()) {
            this.player.closeInventory();
        }
        if (RedProtect.get() != null && RedProtect.get().isEnabled() && this.player.isOnline()) {
            this.player.updateInventory();
            Bukkit.getScheduler().runTaskLater(RedProtect.get(), this.player::updateInventory, 1);
        }

        this.inv = null;
        this.region = null;
    }

    public void open() {
        RedProtect plugin = RedProtect.get();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        String title = flag;
        if (plugin.guiLang != null) {
            String localized = plugin.guiLang.getFlagName(flag);
            String defaultName = plugin.guiLang.getFlagName("default");
            if (localized != null && !localized.isEmpty() && !localized.equals(defaultName)) {
                title = localized;
            }
        }
        this.inv = Bukkit.createInventory(player, GUI_SIZE, title);
        renderPage();
        player.openInventory(this.inv);
    }

    private void renderPage() {
        if (this.inv == null) {
            return;
        }
        int totalPages = getTotalPages();
        if (page >= totalPages) {
            page = totalPages - 1;
        }

        ItemStack[] contents = new ItemStack[GUI_SIZE];
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(selectedMaterials.size(), start + ITEMS_PER_PAGE);
        for (int i = start; i < end; i++) {
            contents[i - start] = createEntryItem(selectedMaterials.get(i));
        }

        ItemStack separator = RedProtect.get().getConfigManager().getGuiSeparator();
        for (int slot = ITEMS_PER_PAGE; slot < GUI_SIZE; slot++) {
            contents[slot] = separator;
        }

        if (page > 0) {
            contents[NAV_PREV_SLOT] = createNavItem(Material.ARROW,
                    RedProtect.get().getLanguageManager().get("gui.page.previous"));
        }
        String pageLabel = RedProtect.get().getLanguageManager().get("gui.page.number")
                .replace("{page}", String.valueOf(page + 1))
                .replace("{total}", String.valueOf(totalPages));
        contents[NAV_INFO_SLOT] = createNavItem(Material.BOOK, pageLabel);

        if ((page + 1) < totalPages) {
            contents[NAV_NEXT_SLOT] = createNavItem(Material.ARROW,
                    RedProtect.get().getLanguageManager().get("gui.page.next"));
        }

        this.inv.setContents(contents);
    }

    private ItemStack createEntryItem(String materialName) {
        Material mat = Material.getMaterial(materialName);
        if (mat != null && mat.isItem()) {
            return new ItemStack(mat);
        }
        ItemStack icon = new ItemStack(Material.PAPER);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + materialName);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack createNavItem(Material mat, String displayName) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            item.setItemMeta(meta);
        }
        return item;
    }
}