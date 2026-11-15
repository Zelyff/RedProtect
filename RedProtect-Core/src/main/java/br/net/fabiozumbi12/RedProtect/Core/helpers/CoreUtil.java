/*
 * Copyright (c) 2012-2023 - @FabioZumbi12
 * Last Modified: 02/10/2023 22:15
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

package br.net.fabiozumbi12.RedProtect.Core.helpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CoreUtil {
    public boolean stopRegen;
    protected final HashMap<String, Object> borderPlayers;
    protected final HashMap<String, String> cachedUUIDs;

    public CoreUtil() {
        borderPlayers = new HashMap<>();
        cachedUUIDs = new HashMap<>();
    }

    public static void printJarVersion() {
        String jarVersion = new File(CoreUtil.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
        System.out.print("RedProtect jar: " + jarVersion);
    }

    protected String dateNow(String format) {
        DateFormat df = new SimpleDateFormat(format);
        Date today = Calendar.getInstance().getTime();
        return df.format(today);
    }

    public String hourNow() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min = Calendar.getInstance().get(Calendar.MINUTE);
        int sec = Calendar.getInstance().get(Calendar.SECOND);
        return "[" + hour + ":" + min + ":" + sec + "]";
    }

    public void saveSBToZip(File file, StringBuilder builder) {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            ZipEntry e = new ZipEntry("RedProtectLogs.txt");
            out.putNextEntry(e);

            byte[] data = builder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.write(data, 0, data.length);
            out.closeEntry();
        } catch (Exception e) {
            printJarVersion();
            e.printStackTrace();
        }
    }

    public static int compareVersions(String v1, String v2) {
        int[] p1 = parseVersionParts(v1);
        int[] p2 = parseVersionParts(v2);
        int max = Math.max(p1.length, p2.length);
        for (int i = 0; i < max; i++) {
            int a = i < p1.length ? p1[i] : 0;
            int b = i < p2.length ? p2[i] : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    private static int[] parseVersionParts(String version) {
        if (version == null || version.isBlank()) {
            return new int[]{0};
        }
        String core = version.trim().split("-", 2)[0];
        String[] rawParts = core.split("\\.");
        int[] nums = new int[rawParts.length];
        for (int i = 0; i < rawParts.length; i++) {
            String digits = rawParts[i].replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    nums[i] = Integer.parseInt(digits);
                } catch (NumberFormatException ignored) {
                    nums[i] = 0;
                }
            }
        }
        return nums;
    }

    protected File genFileName(String Path, Boolean isBackup, int maxbackups, String dateFormat) {
        int count = 1;
        String date = dateFormat.replace("/", "-");
        File logfile = new File(Path + date + "-" + count + ".zip");
        File[] files = new File(Path).listFiles();
        if (files != null && isBackup && maxbackups > 0 && files.length >= maxbackups) {
            Arrays.stream(files)
                    .filter(File::isFile)
                    .min(Comparator.comparingLong(File::lastModified))
                    .ifPresent(File::delete);
        }

        while (logfile.exists()) {
            count++;
            logfile = new File(Path + date + "-" + count + ".zip");
        }

        return logfile;
    }

    public boolean isUUIDs(String uuid) {
        if (uuid == null) {
            return false;
        }

        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public Object parseObject(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(value);
            }
            if (value.equalsIgnoreCase("allow")) {
                return true;
            }
            if (value.equalsIgnoreCase("deny")) {
                return false;
            }
        }
        return value;
    }

    protected String StripName(String pRName) {
        String regionName;
        if (pRName.length() > 13) {
            regionName = pRName.substring(0, 13);
        } else {
            regionName = pRName;
        }
        return regionName;
    }

    public void zipFolder(String sourceDirPath, String zipFilePath) throws IOException {
        Path p = Files.createFile(Paths.get(zipFilePath));
        Path pp = Paths.get(sourceDirPath);
        if (!Files.exists(pp)) {
            return;
        }
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p));
             java.util.stream.Stream<Path> walk = Files.walk(pp)) {
            Iterator<Path> it = walk.filter(path -> !Files.isDirectory(path)).iterator();
            while (it.hasNext()) {
                Path path = it.next();
                String entryName = pp.relativize(path).toString().replace('\\', '/');
                ZipEntry zipEntry = new ZipEntry(entryName);
                zs.putNextEntry(zipEntry);
                Files.copy(path, zs);
                zs.closeEntry();
            }
        }
    }
}
