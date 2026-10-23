# Jalur source RedProtect yang dipakai fork

Status: **SOURCE-OBSERVED**, belum bukti perilaku runtime. Seluruh referensi mengacu
commit `b097dfe00b18576256df55faf1e2e090e98b85a0` (tag `v8.1.4`).
Path Java di bawah relatif terhadap
`RedProtect-Spigot/src/main/java/br/net/fabiozumbi12/RedProtect/Bukkit/`.

## Claim biasa

1. `commands/CommandHandler.java:288–304`: `onPreCommand` memeriksa permission
   subcommand pada event chat command `/rp` dan `/redprotect`.
2. `commands/CommandHandler.java:351–354`: `onCommand` meneruskan ke SubCommand;
   jalur ini sendiri tidak mengulang semua pemeriksaan dari preprocess.
3. `commands/SubCommands/RegionHandlers/ClaimCommand.java:49–84`: periksa mode
   world, buat nama dan jalankan `DefineRegionBuilder` secara asynchronous,
   mengambil kedua selection dari map global plugin.
4. `actions/DefineRegionBuilder.java:52–174`: allowed world, selection/WorldEdit,
   nama, batas scan, tinggi native, overlap/prioritas, jumlah claim, batas area.
5. `actions/DefineRegionBuilder.java:177–201`: balance/cost dan `withdrawPlayer`.
6. `actions/DefineRegionBuilder.java:204–221`: `CreateRegionEvent` dijadwalkan
   lewat `callSyncMethod(...).get()`, pembatalan menghentikan constructor.
7. `ClaimCommand`: jika builder `ready`, `build`, kemudian
   `RegionManager.add(region, world)` dan hapus selection lama.
8. `region/RegionManager.java:271–282`: delegasi ke backend dan Dynmap.

**Batas adaptor:** jangan langsung memanggil `RedProtectAPI.addRegion` atau
`RegionManager.add`; keduanya bukan validator lengkap. Jangan mengganti actor
menjadi console. Permission command harus dicek secara eksplisit karena input
shovel/Forms tidak melewati `PlayerCommandPreprocessEvent`.

**Blocker yang ditemukan:** charge terjadi sebelum cancellable event dan tidak
terlihat refund pada cabang cancel. Builder juga mengakses Bukkit/player/world
dari task async. Memanggil builder yang sama di main thread tanpa penyesuaian
berisiko menunggu task sync yang sedang terhalang. Ini temuan source, bukan
reproduksi bug runtime; perlu regression fixture sebelum perbaikan sempit.

## Resize

`RedefineCommand.onCommand` → resolve region dari posisi/nama →
`PermissionHandler.hasRegionPermLeader(player,"redefine",region)` → async
`RedefineRegionBuilder` → geometry/area/overlap/biaya →
`RedefineRegionEvent` → **remove old dalam constructor** → builder ready →
`RegionManager.add(newRegion,world)` dari command.

Referensi: `commands/SubCommands/RegionHandlers/RedefineCommand.java:49–101`,
`actions/RedefineRegionBuilder.java:53–248`,
`helpers/PermissionHandler.java:90–100`.

Jangan menganggap constructor ini operasi read-only atau transaksi atomik.
Permission saat memulai command tidak membuktikan permission masih sah ketika
task async melakukan write. Adaptor perlu commit context yang aman, tanpa
mengganti engine/serializer.

## Inspect dan nearby

- Native info wand: `listeners/PlayerListener.java:215–231`; info lengkap hanya
  jika `hasRegionPermMember(...,"infowand",region)`, lainnya ringkasan native.
- Border: `BorderCommand` → `RedProtectUtil.addBorder`.
- Nearby: `NearCommand` → `RegionManager.getRegionsNear` → backend.
- Flat-file `getRegionsNear` (`database/WorldFlatFileRegionManager.java:372–383`)
  menelusuri **semua region di world** dan membandingkan posisi tengah.
- MySQL `getRegionsNear` (`database/WorldMySQLRegionManager.java:600–620`)
  menjalankan query `ABS(centerX-?)`/`ABS(centerZ-?)`, tanpa LIMIT.

Membatasi output sesudah pemanggilan native **tidak** membatasi kerja lookup.
Perlu pilihan bounded lookup yang dibuktikan dari source; jangan menyatakan
ada spatial index efisien hanya karena method nearby tersedia.

## Border native

`helpers/RedProtectUtil.java:845–912` membuat himpunan titik memakai nested loop
X dan Z, memfilter tepi, lalu mengirim partikel berkala. `VersionHelperLatest`
memanggil `World.spawnParticle`, sehingga partikel native tidak khusus requester.
Expiry task lama mengambil task terbaru dari map berdasarkan nama player;
refresh sesi perlu pemeriksaan generation agar timer lama tidak mematikan sesi baru.

Tidak ditemukan mode enum tiga pilihan, fake-block renderer, atau glow renderer
pada jalur border ini. Preserve jalur legacy ketika enhancement OFF; renderer
baru perlu viewer isolation, pembatasan sampling, dan cleanup sendiri.

## Forms: jalur authority yang ditemukan

- Flag: `FlagCommand` → `CommandHandlers.handleFlag` → `Region.setFlag` →
  `ChangeRegionFlagEvent` → backend flag update.
- Delete: `DeleteCommand` → `CommandHandlers.handleDelete/handleDeleteName` →
  permission + `DeleteRegionEvent` → `RegionManager.remove`.
- Rename: `RenameCommand` → `CommandHandlers.handleRename` →
  `RenameRegionEvent` → `RegionManager.renameRegion`.
- Role operations tetap melalui handler asli dan pemeriksaan role/permission.

Source belum menyediakan satu shared service transaction yang membuktikan
semua operasi aman dipanggil langsung dari Forms. Buttons harus ditambahkan
setelah konteks target dan authority yang tepat dibuktikan; jangan dispatch
command raw dari teks respons Forms.

## Identitas dan stale response

`RedProtect-Core/.../region/CoreRegion.java:245–263` mendefinisikan ID sebagai
`name + "@" + world`; ID bukan UUID permanen dan berubah ketika rename.
Tidak ditemukan revision counter authoritative yang siap dipakai.
Nonce saja tidak mendeteksi perubahan dari command/API/addon. Desain revision
harus ditentukan dari state aktual tanpa menambah field DB v1.

## Lifecycle dan writes startup

`RedProtect.startLoad` (`RedProtect.java:201–236`) → `ConfigManager` →
`BlockConfig` → commands/listeners/hooks → `RegionManager.loadAll` →
`RedProtectUtil.ReadAllDB` → `saveAll(false)`.

`ReadAllDB` (`helpers/RedProtectUtil.java:293–448`) dapat mengubah tanggal,
memperbarui sign, purge/regenerate region, menjual region, dan membersihkan flag
clan menurut config/hook. Jadi startup bukan dry-run read-only.

MySQL constructor (`database/WorldMySQLRegionManager.java:51–140`) memeriksa tabel,
menjalankan CREATE jika dianggap belum ada dan ALTER untuk tiga kolom legacy
jika tidak ditemukan. Flat-file loader dapat rename file lama dan normalisasi
minY/roles/flags; save menulis ulang file dan dapat membersihkan file region
yang tak ada dalam map. Semua tetap source upstream asli, belum dimodifikasi.

`onEnable` menangkap sebagian exception, menyalakan whitelist dan kick pemain.
Whitelist saja tidak menjamin pemain whitelist tidak masuk. ConfigManager juga
menangkap IOException internal. Admission maintenance tetap wajib sesuai G7.
