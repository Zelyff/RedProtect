package br.net.fabiozumbi12.RedProtect.Core.helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class CoreUtilTest {

    private static class ExposedCoreUtil extends CoreUtil {
        File callGenFileName(String pathPrefix, boolean isBackup, int maxBackups, String dateFormat) {
            return genFileName(pathPrefix, isBackup, maxBackups, dateFormat);
        }
    }

    @Test
    void compareVersions_handlesSemVerMultiDigitSegmentsAndTags() {
        assertTrue(CoreUtil.compareVersions("8.10.0", "8.2.0") > 0);
        assertTrue(CoreUtil.compareVersions("8.2.1-SNAPSHOT", "8.2.0") > 0);
        assertEquals(0, CoreUtil.compareVersions("8.2.0-SNAPSHOT", "8.2"));
        assertTrue(CoreUtil.compareVersions("7.9.5", "8.2.0") < 0);
        assertEquals(0, CoreUtil.compareVersions(null, ""));
    }

    @Test
    void zipFolder_usesForwardSlashesInsideEntries(@TempDir Path tempDir) throws Exception {
        Path src = tempDir.resolve("src");
        Path nested = src.resolve("world_nether");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("region.yml"), "test: 1\n", StandardCharsets.UTF_8);

        Path zipPath = tempDir.resolve("backup.zip");
        new CoreUtil().zipFolder(src.toString(), zipPath.toString());

        assertTrue(Files.isRegularFile(zipPath));
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            assertTrue(entries.hasMoreElements());
            ZipEntry entry = entries.nextElement();
            assertEquals("world_nether/region.yml", entry.getName());
            assertFalse(entry.getName().contains("\\"));
        }
    }

    @Test
    void genFileName_ignoresRotationWhenMaxBackupsZeroAndRotatesWhenPositive(@TempDir Path tempDir) throws Exception {
        ExposedCoreUtil util = new ExposedCoreUtil();
        Path oldest = tempDir.resolve("2026-01-01-1.zip");
        Path newer = tempDir.resolve("2026-01-02-1.zip");
        Files.writeString(oldest, "a");
        Files.writeString(newer, "b");
        oldest.toFile().setLastModified(1_000_000L);
        newer.toFile().setLastModified(2_000_000L);

        util.callGenFileName(tempDir.toString() + File.separator, true, 0, "2026-01-03");
        assertTrue(Files.exists(oldest));
        assertTrue(Files.exists(newer));

        util.callGenFileName(tempDir.toString() + File.separator, true, 2, "2026-01-03");
        assertFalse(Files.exists(oldest));
        assertTrue(Files.exists(newer));
    }
}
