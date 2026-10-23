# Migrasi produksi dan rollback — RedProtect → Zelyff Fork

> **Status: RANCANGAN WAJIB, BUKAN SOP YANG SUDAH DIUJI.** Server operator telah memakai RedProtect ~1 bulan. Tidak ada kredensial/database produksi yang diminta atau diperlukan sekarang. Prosedur final wajib disesuaikan dengan **source, JAR, konfigurasi, jenis storage, dan restore drill yang terbukti** pada fase implementasi. Jangan menganggap memasang JAR fork otomatis kompatibel.

## 0. Kontrak keselamatan — berlaku untuk semua fase

1. **NO DATA MIGRATION (v1):** jangan konversi, mengganti nama, menormalisasi, atau menghapus region storage, tabel/kolom DB, ID/UUID, world key, priority, parent-child relationship, flags, daftar member, batas wilayah, atau informasi player. RedProtect original tetap sumber kebenaran.
2. **NO SCHEMA CHANGE:** tidak ada DDL/ALTER/CREATE/DROP/migrasi skema yang ditambahkan oleh fork. Source audit harus memverifikasi apakah *upstream asli* punya auto-upgrade/migration/normalizer on startup. Jika ada, **blokir cutover** sampai perilakunya dipahami dan tervalidasi untuk versi dan backend operator.
3. **NO UNPLANNED WRITE:** proses validasi/dry-run tidak boleh melakukan perubahan pada storage produksi. Startup normal suatu plugin mungkin melakukan penulisan; catat jalur tulis dan ukur perbedaan pada salinan staging terlebih dahulu. Kegagalan pembuktian = **NO-GO**.
4. **CONF→YAML hanya konfigurasi admin** yang tervalidasi satu-per-satu, **bukan data claim**. Jangan menebak file `.conf` itu config atau data dari ekstensi saja; telusuri jalur baca/tulis di source.
5. **BACKUP + RESTORE DRILL lebih dulu.** Keberadaan file ZIP/dump tanpa demonstrasi restore dan verifikasi claims **tidak dihitung** sebagai backup yang tervalidasi.
6. **Satu plugin aktif:** satu JAR dengan identitas `RedProtect`; simpan JAR lama di luar folder plugin. Tidak ada hot-swap, `/reload`, atau plugin manager reload untuk transisi storage.
7. **PROTEKSI WAJIB AKTIF:** jika fork gagal load, database rusak/putus, atau parity gagal, **jangan buka server ke publik**. Tetap maintenance/offline sampai versi lama dipulihkan atau masalah diverifikasi. Plugin gagal start bukan fail-closed jika pemain masih dapat masuk dan berinteraksi.
8. **Version-pair rollback:** rollback memulihkan pasangan yang cocok: *JAR upstream asal* + *konfigurasi `.conf` asal* + *snapshot storage/data* sesuai checkpoint. Jangan asal mengembalikan JAR lama ke database yang sudah ditulis fork tanpa membuktikan kompatibilitas mundur.
9. **Jangan pernah menyambungkan staging ke DB produksi.** DB uji harus clone independen; bila memungkinkan blokir koneksi jaringan dari staging ke alamat DB produksi untuk pencegahan salah konfigurasi.
10. **Status klaim:** hingga semua gate lulus, tulis `PLANNED/NOT TESTED`. Hindari kata "seamless", "tanpa risiko", "database pasti aman".

## 1. Ruang lingkup yang masuk dan tidak masuk

| Aset | Strategi | Alasan |
|---|---|---|
| Region file/DB, owner/roles, flags, limits, priority, child/subregion | **Keep as-is**; baca via loader RedProtect existing | Menghindari transformasi data yang tak perlu |
| Database schema dan connection settings | **Keep as-is**; audit per backend | Membatasi risiko korupsi/ketidakcocokan |
| `*.conf` *admin configuration* yang terbukti | Migrasi semantik one-to-one ke `*.yml`; backup original | UX config tanpa perubahan perilaku |
| Default config, per-world override, secret config | Validasi typed/effective parity dan redaksi laporan | Mencegah proteksi/DB connection berubah |
| JAR, metadata, dependencies, plugin hooks | Pertahankan identitas/kontrak; ganti JAR **saat offline** | Tidak mematahkan integrasi |
| World data | Tidak diubah fork; backup terkait untuk pemulihan konsisten | Jika rollback mengubah data proteksi, world juga harus cocok |
| Data/claim baru setelah upgrade | **Tidak otomatis tercover snapshot lama**; butuh kebijakan cutover/rekonsiliasi | Mencegah kehilangan progress saat rollback |

## 2. Inventaris minimum saat tahap implementasi (tidak diminta sekarang)

Catat `server_build`, `java_version`, `upstream_plugin_version`, full JAR SHA-256, fork commit SHA, daftar world, plugin hooks/addons, seluruh path plugin, backend storage sebenarnya, versi/driver/namespace DB bila ada, pengaturan koneksi (tanpa password), skema saat ini, lokasi backup dan recovery owner. Gunakan `data/PRODUCTION-INVENTORY.template.json` dan `templates/PRODUCTION-PREFLIGHT.md`.

**Jangan berasumsi**: format database bisa berbeda menurut versi/konfigurasi; nama tabel/path storage hanya boleh ditulis setelah terbukti dari source atau environment. Log dan artifacts wajib **tanpa password/token**.

## 3. Snapshot pembanding (golden state) sebelum menyentuh produksi

Buat snapshot struktural pada salinan data, minimal:

```
region_key = (world_identifier, region_identifier)
attributes = {
  bounds and vertical policy, owner UUID/identifier, roles/member UUIDs,
  flags including defaults/inheritance, priority, parent-child/subregion,
  creation/metadata where semantically relevant
}
```

Tambahkan data khusus upstream yang ditemukan saat audit, termasuk claim cost/limits/persisted economy metadata jika disimpan. Serialize **kanonik** (urutkan keys, sets, role lists yang tak berurutan; jaga tipe numeric/string) sebelum membandingkan hash per-region dan keseluruhan. Jangan hanya membandingkan jumlah claim: satu region hilang + satu region baru bisa menghasilkan count sama. Identitas region juga **tidak boleh berubah**. Untuk konektor database, periksa schema/indices serta efek side-write pada staging (query audit/diff), bukan hanya struktur object ketika dibaca.

Golden fixtures harus mencakup world normal, Nether/End jika dipakai, region biasa, overlap yang legal, child region, custom flag, UUID player offline, multiworld, pengguna dengan leader/member/admin, dan region edge-case. Data hasil ekspor hanya di environment aman; versi yang dibagikan ke publik harus dianonimkan.

## 4. Runbook bertahap (staging dahulu)

### S0 — Source compatibility & storage audit [WAJIB]

- Kunci *persis* versi produksi lama dan full SHA JAR (tidak menebak versi dari latest GitHub).
- Audit semua loader, serializer, migration hook, `onLoad/onEnable/onDisable`, auto-save, database initialization, backup routines, async persistence, cache invalidation, per-world path. Dokumentasikan query mutasi & schema jika ada.
- Pastikan fork tidak menambah skema, tidak mengganti serialization format, tidak mengubah ID/UUID, tidak melakukan CREATE/UPDATE/DELETE/ALTER di path upgrade tanpa persetujuan dan validasi yang eksplisit.
- Selesaikan dependency/compatibility audit per backend sebelum pengujian.

**STOP** jika versi storage atau jalur auto-upgrade tidak dipahami.

### S1 — Backup dingin konsisten (checkpoint **A**, sebelum upgrade)

1. Jadwalkan maintenance; blokir join publik/masuk ke world sebelum backup.
2. Jika versi old plugin memiliki mekanisme save/flush resmi yang terbukti, gunakan itu. Tunggu proses save selesai.
3. **Stop server seluruhnya**; hindari `/reload`. Pastikan tak ada proses penulisan data plugin yang masih berjalan.
4. Backup **JAR lama**, seluruh folder data RedProtect beserta `.conf`, file world relevan, dependencies/config kaitannya, dan **snapshot database eksternal** bila dipakai. Pastikan snapshot database transaksional/konsisten menurut engine-nya; jangan mengandalkan sekadar menyalin file DB yang masih terbuka.
5. Simpan manifest versi/ukuran/SHA, timestamp/checkpoint, lokasi restore, serta bukti verifikasi checksum. Backup harus terpisah dari direktori yang akan di-upgrade.

**STOP** bila ada satu komponen belum dibackup atau dump gagal diverifikasi.

### S2 — Staging/restore rehearsal [WAJIB]

1. Restore checkpoint A ke server **terisolasi**, world dan plugin sesuai, salinan DB dengan **host/schema/credentials berbeda**. Audit koneksi sebelum startup agar tidak mungkin menyentuh DB produksi.
2. Jalankan **JAR RedProtect original** pada staging dan rekam baseline golden region/flags/commands/hook/permissions.
3. Matikan staging, copy snapshot lagi untuk ujicoba fork. Instal fork dan jalankan seluruh proses YAML one-to-one pada salinan config. Jangan menyentuh backup `.conf` baseline.
4. Dengan fitur baru `OFF`, bandingkan snapshot kanonik region/flags/permissions dan catat **seluruh storage writes** sejak startup: perubahan yang tidak terjelaskan = FAIL.
5. Nyalakan fitur baru satu per satu di staging; cek create/resize/flags/ownership; hitung diff sebelum/sesudah untuk memastikan hanya perubahan yang memang diperintahkan terjadi.
6. Lakukan **restore rehearsal**: stop staging, pulihkan JAR original + config original + data checkpoint A dan pastikan jumlah, identity, isi, flags, permissions, serta perilaku proteksi kembali sama.

**STOP** jika restore rehearsal gagal meskipun plugin fork tampak normal.

### S3 — Preflight production (checkpoint **B**, tepat sebelum cutover)

- Semua gate staging S0–S2 harus `PASS` dengan evidence.
- Lakukan maintenance baru dan ambil checkpoint **B** yang konsisten serta mutakhir; jangan memakai snapshot berumur beberapa hari ketika player sudah menambah claim.
- Bekukan penulisan dari game server; cegah plugin otomatis mencoba upgrade schema.
- Bandingkan inventory/checksums/backend/version dengan lingkungan yang diuji; jika berbeda, kembali ke audit dan test staging.
- Siapkan tombol pengambilan keputusan operator: **GO hanya jika PASS semua pemeriksaan kritis**.

### S4 — Cutover terkendali (server masih tertutup)

1. Hentikan server, singkirkan JAR lama dari folder plugins aktif, pasang **satu** JAR fork pada lokasi yang sama. Jangan rename public plugin identity/folder/API.
2. Pertahankan backend region/data *apa adanya*. Jalankan config HOCON→YAML **dengan backup dan validasi eksplisit** sesuai `YAML-MIGRATION.md`; jangan mengganti target DB, storage provider, atau credentials.
3. Jalankan server **dalam maintenance**. Pastikan RedProtect fork `ENABLED`, region dimuat sempurna (bukan 0 karena gagal baca), config parity lulus, storage/schema tidak mengalami perubahan tak terencana, semua hook relevan dan plugin proteksi aktif.
4. Dengan fitur baru tetap `OFF`, lakukan smoke test protection pada beberapa world, region, owner/member/nonmember dan custom flags.
5. Buka server hanya bila semua checks `PASS`; aktifkan shovel/border/Forms secara bertahap setelah baseline terbukti stabil.

**STOP/ROLLBACK** jika muncul region hilang, mismatch owner/flag, command/hook penting rusak, permission bypass, schema change, migration errors, startup plugin gagal, atau DB connectivity ambigu.

### S5 — Pemantauan pasca-cutover

- Amati errors/reload/save, region counts plus canonical samples, permissions, conflicts, async saves, dan integrasi. Buka fase perubahan fitur hanya satu per satu.
- Simpan checkpoint B immutable sepanjang jendela evaluasi dan setujui masa retensi sesuai kapasitas penyimpanan operator.
- Jangan menghapus `.conf` original sampai proses restore dan masa peninjauan selesai; default yang dianjurkan adalah arsip permanen dengan pengamanan kredensial.

## 5. Matriks GO / NO-GO

| Kriteria | Kondisi GO | Jika gagal |
|---|---|---|
| Backend/JAR asal | Versi & SHA tercatat, storage format dikenali | **NO-GO** |
| Backup B | Lengkap, checksum valid, restore drill terbukti | **NO-GO** |
| DB isolation staging | Clone berdiri sendiri, tidak pernah menjangkau prod | **NO-GO** |
| Schema & writes | Tidak ada perubahan storage tak terencana; jika upstream menulis, sudah diaudit & ekuivalen | **NO-GO** |
| Region identity/data | Semua key region cocok dengan baseline sebelum tindakan yang disengaja | **NO-GO** |
| Flags/roles/permissions | Equality semantik dan interaksi proteksi terbukti | **NO-GO** |
| YAML parity | Effective settings, credentials (terselubung), world overrides sama | **NO-GO** |
| Fork plugin | Enabled + listener/hook berfungsi, tidak ada error kritis | **NO-GO** |
| Rollback | Original JAR/config/storage bisa direstore & diverifikasi | **NO-GO** |
| Operator approval | Semua bukti terdokumentasi dan disetujui | **NO-GO** |

**Catatan:** kriteria ini sengaja ketat. `PASS` di source-level saja tidak cukup untuk memutuskan upgrade server aktif.

## 6. Rollback insiden (bukan sekadar downgrade JAR)

1. **Tutup akses player segera** dan stop server, jangan terus menguji di produksi saat proteksi tidak dipercaya.
2. Simpan salinan state fork *untuk diagnosis* secara terpisah tanpa menimpa checkpoint B, redaksi kredensial pada laporan.
3. Tentukan apakah sejak upgrade ada klaim/flags/permissions/world changes sah. **Restore snapshot B secara mentah akan menghilangkan perubahan setelah checkpoint**, jadi operator wajib memilih perawatan downtime/cutover sangat pendek atau rencana rekonsiliasi manual yang diaudit. Jangan menjanjikan nol kehilangan progres.
4. Pulihkan **satu set konsisten**: JAR original *versi asal*, `.conf` asli, data RedProtect/DB checkpoint B, dan world terkait jika konsistensi memerlukannya. Periksa plugin/integration version.
5. Jalankan dengan server masih tertutup, bandingkan golden state, verifikasi nonmember diblokir, flags dan member/owner terjaga. Setelah berhasil barulah player boleh masuk.
6. Jika backup B rusak/tidak ada, jangan coba ‘rollback’ hanya dengan mengganti JAR. Tetap maintenance, evaluasi snapshot sebelumnya dan potensi kehilangan data sebelum pemulihan.

## 7. Syarat khusus jika database dikendalikan layanan eksternal

- Akses minimal: staging menggunakan salinan DB dengan kredensial berbeda; produksi melakukan backup lewat alat resmi engine/provider.
- Verifikasi konsistensi transaksi/snapshot engine. Pada engine berbasis file, gunakan prosedur backup yang direkomendasikan engine, bukan copy live file sembarang.
- Rekam schema version dan object inventory tanpa mengekspor password.
- Jika connector fork membuka prod database sebelum migrasi YAML selesai, loader wajib menahan operasi yang berpotensi mengubah storage hingga config tervalidasi. Audit dulu apakah hal ini bisa dilakukan tanpa mengubah behavior original.
- Unsupported backend atau driver mismatch = NO-GO, bukan diam-diam pindah ke backend baru.

## 8. Batas keputusan v1

**Tidak ada perubahan otomatis pada database produksi** di blueprint ini; dokumen hanya kontrak pengembangan/deploy. Jika audit menemukan keharusan migrasi skema untuk upgrade suatu versi RedProtect, maka perlu ADR, tool konversi tersendiri, dry-run, test fixture, dan persetujuan operator; **di luar ruang lingkup v1**. Lebih baik menunda YAML atau fitur baru daripada mengorbankan data player.

## 9. Bukti minimum yang wajib dilampirkan ke laporan deploy

- Inventory redacted versi/backend/config paths dan source SHA.
- Checkpoint A/B manifests, hash, bukti restore drill & timestamp.
- Diff schema/storage (jika DB) atau manifest file (jika file backend).
- Diff seluruh region canonical dan contoh in-game nonmember/owner/flags.
- Diff typed config `.conf` vs `.yml` dan unresolved sections.
- Log startup old vs fork, test results, release GO/NO-GO yang ditandatangani operator.
- Rencana rollback dan catatan progress setelah cutover.

**Release verdict:** PENDING sampai bukti aktual tersedia; jangan tandai `PASS` berdasarkan dokumen ini.
