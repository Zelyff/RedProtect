/*
 * Copyright (c) 2012-2026 - @FabioZumbi12
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 * damages arising from the use of this class.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.guis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure state-mapping, pagination, and input-validation helpers for Bedrock Cumulus Forms.
 * Kept independent of Bukkit registries so all serialization and validation paths can be unit-tested cleanly.
 */
public final class BedrockFormStateHelper {

    public static final int DEFAULT_PAGE_SIZE = 20;

    private static final Set<String> VALUE_ADMIN_FLAGS = Set.of(
            "particles",
            "gamemode",
            "set-portal",
            "respawn-location",
            "max-players",
            "cmd-onhealth",
            "allow-cmds",
            "deny-cmds",
            "effects",
            "player-enter-command",
            "server-enter-command",
            "player-exit-command",
            "server-exit-command"
    );

    private BedrockFormStateHelper() {
    }

    /**
     * Returns whether an admin flag expects non-boolean command parameters rather than a boolean toggle.
     */
    public static boolean isValueAdminFlag(String flag) {
        return flag != null && VALUE_ADMIN_FLAGS.contains(flag.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Returns whether a normalized material name represents a valid non-air Bukkit material.
     */
    public static boolean isValidNonAirMaterialName(String materialName) {
        if (materialName == null) {
            return false;
        }
        String normalized = materialName.trim().toUpperCase(Locale.ROOT);
        return !normalized.isEmpty()
                && !normalized.equals("AIR")
                && !normalized.equals("CAVE_AIR")
                && !normalized.equals("VOID_AIR");
    }

    /**
     * Parses a CSV flag value into an ordered set of uppercase entry names.
     * Treats boolean literals ("true"/"false") and blank strings as empty CSV sets.
     */
    public static Set<String> parseCsvSet(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptySet();
        }
        String text = rawValue.toString().trim();
        if (text.isEmpty() || text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String token : text.split(",")) {
            String normalized = token.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return result;
    }

    /**
     * Determines whether an individual container or mob entry is enabled under the current flag value.
     */
    public static boolean isContainerOrMobSelected(Object rawFlagValue, String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return false;
        }
        if (rawFlagValue instanceof Boolean bool) {
            return bool;
        }
        if (rawFlagValue == null) {
            return false;
        }
        String text = rawFlagValue.toString().trim();
        if (text.equalsIgnoreCase("true")) {
            return true;
        }
        if (text.equalsIgnoreCase("false") || text.isEmpty()) {
            return false;
        }
        String target = entryName.trim().toUpperCase(Locale.ROOT);
        for (String token : text.split(",")) {
            if (token.trim().toUpperCase(Locale.ROOT).equals(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the initial selected-entry set across all available container or mob keys.
     */
    public static Set<String> resolveSelectedEntries(List<String> allAvailable, Object rawFlagValue) {
        Set<String> selected = new LinkedHashSet<>();
        if (allAvailable == null || allAvailable.isEmpty()) {
            return selected;
        }
        for (String entry : allAvailable) {
            if (entry != null && isContainerOrMobSelected(rawFlagValue, entry)) {
                selected.add(entry.trim().toUpperCase(Locale.ROOT));
            }
        }
        return selected;
    }

    /**
     * Applies toggled values for one page onto the full selected set without disturbing other pages.
     */
    public static Set<String> applyPageSelection(List<String> allAvailable, Set<String> currentSelected, List<String> pageEntries, List<Boolean> pageToggles) {
        Set<String> next = new LinkedHashSet<>();
        if (currentSelected != null) {
            for (String s : currentSelected) {
                if (s != null && !s.isBlank()) {
                    next.add(s.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        if (pageEntries != null && pageToggles != null) {
            int limit = Math.min(pageEntries.size(), pageToggles.size());
            for (int i = 0; i < limit; i++) {
                String entry = pageEntries.get(i);
                if (entry == null || entry.isBlank()) {
                    continue;
                }
                String upper = entry.trim().toUpperCase(Locale.ROOT);
                if (Boolean.TRUE.equals(pageToggles.get(i))) {
                    next.add(upper);
                } else {
                    next.remove(upper);
                }
            }
        }
        if (allAvailable != null) {
            Set<String> validSet = new LinkedHashSet<>();
            for (String s : allAvailable) {
                if (s != null) {
                    validSet.add(s.trim().toUpperCase(Locale.ROOT));
                }
            }
            next.retainAll(validSet);
        }
        return next;
    }

    /**
     * Serializes selected container or mob entries matching RedProtect Java GUIs:
     * all selected -> Boolean.TRUE, empty -> Boolean.FALSE (or "false" for MobFlagGui), subset -> CSV.
     */
    public static Object serializeContainerOrMobSelection(Collection<String> allAvailable, Collection<String> selected, boolean falseAsBooleanObject) {
        if (allAvailable == null || allAvailable.isEmpty() || selected == null || selected.isEmpty()) {
            return falseAsBooleanObject ? Boolean.FALSE : "false";
        }
        Set<String> normalizedSelected = new LinkedHashSet<>();
        for (String s : selected) {
            if (s != null && !s.isBlank()) {
                normalizedSelected.add(s.trim().toUpperCase(Locale.ROOT));
            }
        }
        if (normalizedSelected.isEmpty()) {
            return falseAsBooleanObject ? Boolean.FALSE : "false";
        }
        Set<String> normalizedAll = new LinkedHashSet<>();
        for (String s : allAvailable) {
            if (s != null && !s.isBlank()) {
                normalizedAll.add(s.trim().toUpperCase(Locale.ROOT));
            }
        }
        if (!normalizedAll.isEmpty() && normalizedSelected.containsAll(normalizedAll)) {
            return Boolean.TRUE;
        }
        List<String> ordered = new ArrayList<>();
        for (String available : normalizedAll) {
            if (normalizedSelected.contains(available)) {
                ordered.add(available);
            }
        }
        for (String extra : normalizedSelected) {
            if (!normalizedAll.contains(extra)) {
                ordered.add(extra);
            }
        }
        return String.join(",", ordered);
    }

    /**
     * Serializes registered item material names into CSV or {@code null} when empty (signaling {@code region.removeFlag}).
     */
    public static String serializeItemFlagList(Collection<String> materialNames) {
        if (materialNames == null || materialNames.isEmpty()) {
            return null;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String name : materialNames) {
            if (name == null) {
                continue;
            }
            String normalized = name.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isEmpty() && !normalized.equals("AIR") && !normalized.equals("CAVE_AIR") && !normalized.equals("VOID_AIR")) {
                unique.add(normalized);
            }
        }
        if (unique.isEmpty()) {
            return null;
        }
        return String.join(",", unique);
    }

    /**
     * Parses an item-flag CSV value into an ordered list of non-AIR material names.
     */
    public static List<String> parseItemFlagList(Object rawValue) {
        if (rawValue == null) {
            return new ArrayList<>();
        }
        String text = rawValue.toString().trim();
        if (text.isEmpty() || text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
            return new ArrayList<>();
        }
        Set<String> items = new LinkedHashSet<>();
        for (String token : text.split(",")) {
            String normalized = token.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isEmpty() && !normalized.equals("AIR")) {
                items.add(normalized);
            }
        }
        return new ArrayList<>(items);
    }

    /**
     * Computes total page count (>= 1) for a list and page size.
     */
    public static int computeTotalPages(int totalItems, int pageSize) {
        if (pageSize <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil((double) Math.max(0, totalItems) / pageSize));
    }

    /**
     * Returns an unmodifiable slice of {@code items} for the requested 0-based {@code page}.
     */
    public static <T> List<T> paginate(List<T> items, int page, int pageSize) {
        if (items == null || items.isEmpty() || pageSize <= 0) {
            return Collections.emptyList();
        }
        int totalPages = computeTotalPages(items.size(), pageSize);
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        int start = clampedPage * pageSize;
        int end = Math.min(start + pageSize, items.size());
        if (start >= end) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(items.subList(start, end)));
    }

    /**
     * Validates a GUI slot index against the active GUI slot limit.
     */
    public static boolean isValidGuiSlot(int slot, int maxSlots) {
        int clampedMax = Math.max(9, Math.min(54, maxSlots));
        return slot >= 0 && slot < clampedMax;
    }

    /**
     * Strips legacy Minecraft section/ampersand color formatting codes for clean Bedrock form labels.
     */
    public static String stripColorCodes(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[§&][0-9a-fA-Fk-oK-OrRxX]", "").trim();
    }

    /**
     * Normalizes user-entered material names (trim, spaces -> underscores, uppercase).
     */
    public static String normalizeMaterialQuery(String rawInput) {
        if (rawInput == null) {
            return "";
        }
        return rawInput.trim().replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
