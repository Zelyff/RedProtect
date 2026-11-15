package br.net.fabiozumbi12.RedProtect.Core.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class LocalizationParityTest {

    private static final Pattern BRACE_PLACEHOLDER = Pattern.compile("\\{[^}]+}");
    private static final Pattern PRINTF_PLACEHOLDER = Pattern.compile("%[sd]");

    private static final List<String> REQUIRED_NEW_KEYS = List.of(
            "gp.claim.firstcorner",
            "gp.claim.created",
            "gp.resize.started",
            "gp.resize.invalidregion",
            "gp.selection.cancelled",
            "bedrock.gui.back",
            "bedrock.gui.flags.subtitle",
            "bedrock.gui.edit.select",
            "bedrock.gui.edit.slot-label",
            "bedrock.gui.collection.summary",
            "bedrock.gui.collection.edit-page",
            "bedrock.gui.items.title",
            "bedrock.gui.items.subtitle",
            "bedrock.gui.items.add-held",
            "bedrock.gui.items.add-pick",
            "bedrock.gui.items.clear",
            "bedrock.gui.items.invalid",
            "bedrock.gui.items.add-title",
            "bedrock.gui.items.inv-label",
            "bedrock.gui.items.input-label"
    );

    @Test
    void langIdMatchesLangEnUsParityAndPlaceholders() throws IOException {
        Properties en = loadProperties("/assets/redprotect/langEN-US.properties");
        Properties id = loadProperties("/assets/redprotect/langID-ID.properties");

        assertParity("lang", en, id);

        for (String requiredKey : REQUIRED_NEW_KEYS) {
            assertTrue(en.containsKey(requiredKey), "langEN-US.properties missing new key: " + requiredKey);
            assertTrue(id.containsKey(requiredKey), "langID-ID.properties missing new key: " + requiredKey);
        }
    }

    @Test
    void guiIdMatchesGuiEnUsParityAndPlaceholders() throws IOException {
        Properties en = loadProperties("/assets/redprotect/guiEN-US.properties");
        Properties id = loadProperties("/assets/redprotect/guiID-ID.properties");

        assertParity("gui", en, id);
    }

    private static Properties loadProperties(String resourcePath) throws IOException {
        try (InputStream stream = LocalizationParityTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(stream, "Missing bundled localization resource: " + resourcePath);
            Properties props = new Properties();
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
            for (String key : props.stringPropertyNames()) {
                assertFalse(key.startsWith("\uFEFF"), "UTF-8 BOM detected on property key in " + resourcePath + ": " + key);
            }
            return props;
        }
    }

    private static void assertParity(String kind, Properties en, Properties id) {
        Set<String> enKeys = new TreeSet<>(en.stringPropertyNames());
        Set<String> idKeys = new TreeSet<>(id.stringPropertyNames());

        Set<String> missingInId = new TreeSet<>(enKeys);
        missingInId.removeAll(idKeys);
        assertTrue(missingInId.isEmpty(), kind + "ID-ID is missing keys from EN-US: " + missingInId);

        Set<String> extraInId = new TreeSet<>(idKeys);
        extraInId.removeAll(enKeys);
        assertTrue(extraInId.isEmpty(), kind + "ID-ID has extra keys absent in EN-US: " + extraInId);

        for (String key : enKeys) {
            String enVal = en.getProperty(key);
            String idVal = id.getProperty(key);
            assertNotNull(idVal, kind + "ID-ID null value for key: " + key);
            assertFalse(idVal.isBlank(), kind + "ID-ID blank value for key: " + key);

            List<String> enBraces = extractTokens(BRACE_PLACEHOLDER, enVal);
            List<String> idBraces = extractTokens(BRACE_PLACEHOLDER, idVal);
            assertEquals(enBraces, idBraces, "Brace placeholder mismatch on key " + key);

            List<String> enPrintf = extractTokens(PRINTF_PLACEHOLDER, enVal);
            List<String> idPrintf = extractTokens(PRINTF_PLACEHOLDER, idVal);
            assertEquals(enPrintf, idPrintf, "Printf placeholder mismatch on key " + key);
        }
    }

    private static List<String> extractTokens(Pattern pattern, String input) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        Collections.sort(tokens);
        return tokens;
    }
}
