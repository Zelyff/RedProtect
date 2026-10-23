> Riwayat Phase 0. Status fitur terkini: `docs/zelyff/IMPLEMENTATION-STATUS.md`. Gate build ini tidak lagi menahan implementasi setelah arahan operator 22 Oktober 2026.

# Hasil aktual G0

Tanggal: 22 Oktober 2026 (WIB).

**Verdict: BLOCKED — G0 belum lulus. Fitur fork belum diimplementasikan.**

| Pemeriksaan | Hasil aktual | Bukti |
|---|---|---|
| Checkout source asli | VERIFIED: tag v8.1.4, full SHA dikunci | SOURCE-MANIFEST.json |
| Identitas release pembanding | VERIFIED: SHA-256 download sama dengan digest asset GitHub | SOURCE-MANIFEST.json |
| Pelestarian file upstream | VERIFIED: 241 file, 0 perbedaan tak terencana; hanya URL Paper dicatat sebagai build change | SOURCE-PRESERVATION.log dan UPSTREAM-FILES.json |
| Maven/JDK | VERIFIED: Maven 3.9.9; JDK 21.0.12 tersedia untuk percobaan akhir | ENVIRONMENT.txt |
| Build parent + Core | VERIFIED: keduanya SUCCESS pada percobaan akhir | UPSTREAM-BUILD-AFTER-DEPENDENCIES.log |
| Tes bawaan Core | Tidak tersedia: log menyatakan No tests to run | Log build; bukan PASS untuk fungsi plugin |
| Build Spigot | BLOCKED: tiga artifact provided belum terunduh | Log build dan EXTERNAL-DEPENDENCY-FETCH.json |
| Addons | SKIPPED oleh Maven karena Spigot gagal | Log build |
| Source call graph | SOURCE-OBSERVED, sudah dicatat | CALL-GRAPH.md |
| Inventory config/storage | SOURCE-OBSERVED, lima kandidat admin dan state yang dikecualikan | CONFIG-INVENTORY.csv |
| Baseline server/in-game | NOT RUN | Tidak ada klaim runtime PASS |
| T-001 sampai T-046 | NOT RUN | Matrix blueprint adalah rencana tes, bukan hasil |
| Produksi/DB operator | Tidak diakses | Tidak ada config atau data operator di checkpoint |

## Blocker tepatnya

Tiga artifact yang masih diminta POM upstream:

```text
net.sacredlabyrinth.phaed.simpleclans:SimpleClans:2.19.2
com.gmail.nossr50.mcMMO:mcMMO:2.1.206
me.clip:placeholderapi:2.11.6
```

Repository yang dicoba berdasarkan POM:

- `https://repo.roinujnosde.me/releases/` → HTTP 403.
- `https://nexus.neetgames.com/repository/maven-public/` → HTTP 403.
- `https://repo.extendedclip.com/content/repositories/placeholderapi/` → HTTP 403.

Jalur Maven Central/Paper resmi dan repository TranslationAPI berhasil
menyediakan banyak dependensi lain; checksum artifact yang diunduh langsung
diperiksa terhadap file `.sha1` repository. Detail ada di DEPENDENCY-FETCH.json
dan EXTERNAL-DEPENDENCY-FETCH.json. Tidak ada JAR stub atau API palsu dibuat.

Lookup release GitHub versi tepat juga dicoba. Release PlaceholderAPI 2.11.6
tidak memiliki asset pada respons API; tag tanpa prefix untuk dua lainnya
menghasilkan 404. Ini **bukan** bukti bahwa tidak ada distribusi lain, hanya
hasil akses yang benar-benar dilakukan. Jangan mengganti versi compile secara
diam-diam atau menghapus integrasi untuk mengaku build selesai.

## Tindakan minimum untuk melanjutkan G0

Gunakan tiga JAR versi di atas dari distribusi sah atau cache Maven yang sudah
dimiliki operator/developer. Yang diperlukan hanya JAR dependensi build;
tidak membutuhkan database, config, folder world, atau token. Verifikasi
metadata/version dan hash sebelum menginstal ke cache Maven dengan koordinat
yang sesuai. Jika hanya tersedia versi berbeda, audit kecocokan API dan catat
keputusan dependency terlebih dahulu; jangan mengaku versi itu identik.

Setelah dependency teratasi: build penuh online → catat dependency tree dan
checksums → jalankan original pada server uji terisolasi dengan fixture sintetis
→ capture baseline region/config/permissions → tentukan hasil G0. Tidak
menggunakan produksi untuk membuktikan build atau runtime.

## Alasan fitur belum masuk

`AGENTS.md` butir 2 dan `docs/zelyff/blueprint/project/PLAN.md` G0 mensyaratkan
source pin, call graph, dan clean baseline build sebelum kode fitur. Prompt
implementasi juga menetapkan tugas pertama Phase 0 saja. Karena build belum
selesai dan baseline runtime belum dijalankan, G0 tetap BLOCKED. Ini checkpoint
eksekusi yang dapat dilanjutkan, bukan pengganti implementasi atau rilis final.
