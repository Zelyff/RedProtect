# Status implementasi — 22 Oktober 2026

Versi source: **8.1.4-zelyff.1-dev**. Basis upstream tag `v8.1.4`, commit
`b097dfe00b18576256df55faf1e2e090e98b85a0`.

Setelah checkpoint Phase 0, operator mengarahkan agar implementasi dilanjutkan
tanpa menunggu validasi/build ulang upstream. Catatan Phase 0 adalah riwayat;
halaman ini menjelaskan source terkini.

## Implementasi source

| Bagian | Yang sudah masuk |
|---|---|
| Shovel/stick | Main-hand, dua sudut, inspect, bounded nearby, timeout/cancel |
| Resize | Native builder, preview/konfirmasi, snapshot dan permission ulang |
| Operasi | Overload guarded, economy setelah event, draft tanpa live write |
| Border | Tiga mode, fake block, viewer-only particles, budget/cleanup |
| Glow | BlockDisplay Java, putih default, viewer-isolated/nonpersistent |
| YAML | Lima file admin, typed compare, backup/journal dan atomic save |
| Forms | Daftar/ringkasan/flags/roles/rename/delete/resize; Floodgate opsional |
| Response | Server-thread, consume-once, TTL, snapshot dan authorization ulang |
| Integrasi | Enable/reload/disable, fallback native, commands/tab completion |
| Kredit | Zelyff ditambahkan; creator/upstream/NOTICE/LICENSE dipertahankan |

Perubahan file upstream terbatas pada integrasi fitur, config, overload guarded,
draft transient, getter cache terbatas, metadata dan POM. Native serializer
`RedProtectUtil` dipertahankan byte-identik dengan checkout awal. Getter cache
baru tidak menambah DDL, query mutasi atau format data.

## Pemeriksaan aktual

- Seluruh source produksi yang diubah/ditambahkan dikompilasi langsung terhadap
  Core classes, Paper API 1.21.10, dependency lokal dan JAR release upstream.
  Log: `evidence/CHANGED-CODE-COMPILE.log`.
- Tes JUnit config/migrasi dan geometri border dijalankan. Hasil akhir di
  `evidence/FORK-TEST-RESULTS.log`.
- Cakupan tes: scalar strings/angka/UTF-8, default object mapping, unknown nested
  keys, byte identity state player/region, batch lima file, conflict/duplicate
  keys, edit eksternal, journal recovery, secret redaction, cap/deduplikasi/tepi
  asli dan koordinat ekstrem pada geometri.
- API shape Floodgate/Cumulus diperiksa dari source resmi untuk bridge Forms.

Kompilasi langsung memeriksa source yang berubah; itu bukan build Maven penuh
semua modul. Build penuh belum diselesaikan karena tiga dependency upstream
belum dapat diakses di lingkungan kerja. Implementasi fitur tetap dilanjutkan.

## Batas pengujian

Belum ada tes server/client Java, Bedrock, Geyser, gameplay/economy, Forms,
profiling atau restore produksi. Tampilan glow tembus penghalang belum
dibuktikan di client. T-001..T-046 runtime tidak ditandai PASS.

Nearby memakai cache terbatas dan dapat menghilangkan hasil di luar cap/cache.
Backend/event/maintenance asal tetap memakai sistem upstream. Tidak dibuat
schema/serializer atau transaksi database baru. Ini source development dengan
fitur opt-in.

## Panduan

- `MULAI-DI-SINI.md`: aktivasi, permission, build.
- `docs/zelyff/FEATURES.md`: alur dan batas tiap fitur.
- `docs/zelyff/PANDUAN-GITHUB.md`: fork, copy, commit, push.
- `docs/zelyff/blueprint`: rancangan asli dan runbook produksi.
