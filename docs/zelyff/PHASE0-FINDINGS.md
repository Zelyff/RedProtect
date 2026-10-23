# Hasil eksekusi source baseline — Zelyff

Tanggal: 22 Oktober 2026 (WIB). Dokumen ini mencatat pekerjaan yang benar-benar
dilakukan; scope produk tetap blueprint v1.1. Tidak menambahkan fitur baru.

## Yang sudah terbukti

- Source asli berhasil diambil dari `FabioZumbi12/RedProtect`, tag `v8.1.4`,
  full commit `b097dfe00b18576256df55faf1e2e090e98b85a0`.
- Semua 241 file tracked upstream dicatat SHA-256. Verifier membuktikan tidak
  ada perbedaan tak terencana; satu perubahan build yang diizinkan ialah URL
  repository Paper pada POM Spigot. Source Java, metadata plugin, dan storage
  belum diubah.
- Asset release asli `RedProtect-8.1.4-b11-release.jar` diunduh untuk pembanding;
  SHA-256 `ef143cafca8c050154aa8a0d68f821a00d32cf6152f898089583a81534a16bd0`
  sama dengan digest asset dari API GitHub. Ini membuktikan identitas download,
  **bukan** kecocokan dengan JAR server operator yang belum diinventarisasi.
- POM pada tag masih bertuliskan `8.1.3-SNAPSHOT`. Workflow upstream
  `.github/workflows/build-on-tag.yml` menjalankan `versions:set` sebelum build
  tag; ini menjelaskan versi `8.1.4` pada metadata release. Tidak perlu mengganti
  engine atau menebak bahwa tag salah.
- Modul Core berhasil compile dan package pada percobaan build. Log menyebut
  `No tests to run`; jangan menyebut ini bukti semua tes lulus.
- Jalur config, claim, resize, inspect, border, permission, startup, dan backend
  sudah ditelusuri dari source. Lihat `evidence/CALL-GRAPH.md` dan inventory.

## Koreksi asumsi blueprint dari source

| Temuan | Konsekuensi implementasi |
|---|---|
| `blocks.conf` mengandung UUID/timestamp/jatah blok pemain | Tidak dikonversi sebagai admin config; simpan file ini apa adanya |
| `signs.conf` memetakan ID region ke lokasi sign | Termasuk state runtime; tidak dimigrasikan ke YAML |
| Lima config admin: config/globalflags/guiconfig/economy/textures | Itulah kandidat satu-per-satu; tetap perlu typed parity sebelum migrasi |
| Loader upstream mengimpor `.yml` lama lalu rename ke `_BKP.yml` | YAML baru tidak boleh diarahkan ke loader legacy ini begitu saja |
| Startup memuat, normalisasi, menjalankan maintenance region dan save | Plugin startup bukan dry-run; wajib salinan staging |
| Backend MySQL asli punya CREATE/ALTER otomatis | Fork tidak menambah DDL; kompatibilitas skema asal tetap perlu dibuktikan |
| ID region adalah `nama@world`, bukan UUID immutable | Respons Forms harus dibatalkan jika rename/target state berubah |
| Sebagian permission berada di event preprocess command | Shovel/Forms wajib pemeriksaan eksplisit, jangan sekadar dispatch handler |
| Claim/resize melakukan charge sebelum event cancellable | Buat reproduksi/regression test; cegah debit pada operasi yang batal |
| Builder memakai async Bukkit dan menunggu `callSyncMethod().get()` | Jangan pindahkan pemanggilan langsung ke main thread tanpa audit; risiko deadlock |
| Resize menghapus region lama sebelum command menambah pengganti | Tidak boleh dianggap satu transaksi commit aman |
| Border asli world particles dengan nested X/Z loop | Preserve feature-off; renderer baru perlu budget dan viewer isolation |
| Nearby flat-file menelusuri semua region | `limit output=3` saja bukan pembatasan biaya lookup |

Semua risiko pada tabel adalah **SOURCE-OBSERVED**. Belum ada reproduksi
in-game; tidak menyatakan terjadi insiden pada server operator.

## Perubahan yang dikerjakan

1. Satu URL repository build Paper diperbarui ke
   `https://repo.papermc.io/repository/maven-public/`. Endpoint metadata dan JAR
   diuji via HTTP; URL lama memberi 403 pada percobaan Maven.
2. Blueprint asli v1.1 disertakan di `docs/zelyff/blueprint`; AGENTS-nya dipakai
   sebagai instruksi root. Isi blueprint tidak disamakan dengan hasil tes.
3. Ditambahkan inventory/evidence dan verifier checksum yang hanya membaca
   source. Script tidak menghubungi Minecraft, database, atau data produksi.
4. Panduan GitHub untuk pemula disediakan, tanpa melakukan fork/push/publish.

## Urutan kerja setelah gate awal terbukti

Tetap gunakan irisan PR blueprint: source/build baseline → fixture/config
inventory → adapter YAML nonaktif → migrator gated → inspect → claim → resize →
border block → glow → Forms read-only → Forms mutation → regression/staging.
Temuan charge/threading ditangani hanya bila fixture membuktikan blocker,
melalui perubahan sempit yang dicatat terpisah. Tidak ada izin redesign storage.

Status build lengkap dan G0 ada di `evidence/BASELINE-RESULTS.md`; halaman ini
tidak memberikan izin deploy ke server aktif.
