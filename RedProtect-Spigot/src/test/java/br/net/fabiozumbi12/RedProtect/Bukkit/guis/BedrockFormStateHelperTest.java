package br.net.fabiozumbi12.RedProtect.Bukkit.guis;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BedrockFormStateHelperTest {

    @Test
    void parseCsvSet_handlesBooleansBlankAndMixedCaseTokens() {
        assertTrue(BedrockFormStateHelper.parseCsvSet(null).isEmpty());
        assertTrue(BedrockFormStateHelper.parseCsvSet("true").isEmpty());
        assertTrue(BedrockFormStateHelper.parseCsvSet("FALSE").isEmpty());
        assertTrue(BedrockFormStateHelper.parseCsvSet("  ").isEmpty());

        Set<String> parsed = BedrockFormStateHelper.parseCsvSet(" chest , barrel , , SHULKER_BOX ");
        assertEquals(List.of("CHEST", "BARREL", "SHULKER_BOX"), List.copyOf(parsed));
    }

    @Test
    void containerAndMobSelection_roundTripsAndPreservesOtherPages() {
        List<String> all = List.of("CHEST", "TRAPPED_CHEST", "BARREL", "HOPPER");

        assertTrue(BedrockFormStateHelper.isContainerOrMobSelected(true, "CHEST"));
        assertTrue(BedrockFormStateHelper.isContainerOrMobSelected("true", "BARREL"));
        assertFalse(BedrockFormStateHelper.isContainerOrMobSelected("false", "CHEST"));
        assertTrue(BedrockFormStateHelper.isContainerOrMobSelected("CHEST,HOPPER", "hopper"));
        assertFalse(BedrockFormStateHelper.isContainerOrMobSelected("CHEST,HOPPER", "BARREL"));

        // Page 1 edits only CHEST & TRAPPED_CHEST; BARREL & HOPPER from page 2 stay selected.
        Set<String> initial = BedrockFormStateHelper.resolveSelectedEntries(all, "true");
        Set<String> afterPageEdit = BedrockFormStateHelper.applyPageSelection(
                all,
                initial,
                List.of("CHEST", "TRAPPED_CHEST"),
                List.of(false, true)
        );
        assertEquals(Set.of("TRAPPED_CHEST", "BARREL", "HOPPER"), afterPageEdit);
        assertEquals("TRAPPED_CHEST,BARREL,HOPPER",
                BedrockFormStateHelper.serializeContainerOrMobSelection(all, afterPageEdit, true));

        // All selected -> Boolean.TRUE
        Set<String> backToAll = BedrockFormStateHelper.applyPageSelection(
                all,
                afterPageEdit,
                List.of("CHEST"),
                List.of(true)
        );
        assertEquals(Boolean.TRUE,
                BedrockFormStateHelper.serializeContainerOrMobSelection(all, backToAll, true));

        // Empty selection -> Boolean.FALSE for chest, "false" string for mob
        assertEquals(Boolean.FALSE,
                BedrockFormStateHelper.serializeContainerOrMobSelection(all, Set.of(), true));
        assertEquals("false",
                BedrockFormStateHelper.serializeContainerOrMobSelection(all, Set.of(), false));
    }

    @Test
    void itemFlagSerialization_ignoresAirAndSignalsRemovalWhenEmpty() {
        assertNull(BedrockFormStateHelper.serializeItemFlagList(List.of("AIR", " ", "cave_air")));
        assertEquals("DIAMOND_SWORD,ENDER_PEARL",
                BedrockFormStateHelper.serializeItemFlagList(List.of("diamond_sword", "AIR", "ENDER_PEARL", "DIAMOND_SWORD")));

        List<String> parsed = BedrockFormStateHelper.parseItemFlagList("STONE, AIR , OBSIDIAN");
        assertEquals(List.of("STONE", "OBSIDIAN"), parsed);
    }

    @Test
    void paginateAndSlotValidation_clampOutOfRangeValues() {
        List<String> items = List.of("A", "B", "C", "D", "E");
        assertEquals(3, BedrockFormStateHelper.computeTotalPages(5, 2));
        assertEquals(List.of("A", "B"), BedrockFormStateHelper.paginate(items, 0, 2));
        assertEquals(List.of("E"), BedrockFormStateHelper.paginate(items, 99, 2));
        assertEquals(List.of("A", "B"), BedrockFormStateHelper.paginate(items, -5, 2));

        assertTrue(BedrockFormStateHelper.isValidGuiSlot(0, 27));
        assertTrue(BedrockFormStateHelper.isValidGuiSlot(26, 27));
        assertFalse(BedrockFormStateHelper.isValidGuiSlot(-1, 27));
        assertFalse(BedrockFormStateHelper.isValidGuiSlot(27, 27));
        assertFalse(BedrockFormStateHelper.isValidGuiSlot(54, 99));

        assertEquals("PvP", BedrockFormStateHelper.stripColorCodes("&6&lPvP§r"));
        assertEquals("DIAMOND_SWORD", BedrockFormStateHelper.normalizeMaterialQuery(" diamond sword "));
    }

    @Test
    void valueAdminFlagAndMaterialValidation_behavePredictably() {
        assertTrue(BedrockFormStateHelper.isValueAdminFlag("gamemode"));
        assertTrue(BedrockFormStateHelper.isValueAdminFlag(" effects "));
        assertTrue(BedrockFormStateHelper.isValueAdminFlag("player-enter-command"));
        assertFalse(BedrockFormStateHelper.isValueAdminFlag("pvp"));
        assertFalse(BedrockFormStateHelper.isValueAdminFlag(null));

        assertTrue(BedrockFormStateHelper.isValidNonAirMaterialName("DIAMOND_SWORD"));
        assertFalse(BedrockFormStateHelper.isValidNonAirMaterialName("AIR"));
        assertFalse(BedrockFormStateHelper.isValidNonAirMaterialName("cave_air"));
        assertFalse(BedrockFormStateHelper.isValidNonAirMaterialName("void_air"));
        assertFalse(BedrockFormStateHelper.isValidNonAirMaterialName(" "));
        assertFalse(BedrockFormStateHelper.isValidNonAirMaterialName(null));
    }
}
