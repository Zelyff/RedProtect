# Panduan fitur fork

## Claim dan resize

Material default: `GOLDEN_SHOVEL` dan `STICK`, dapat diatur di
`zelyff-enhancements.tools.claim-item` dan `inspect-item`. Ketika tools aktif,
material tersebut memakai adaptor baru; material wand native yang berbeda
tetap memakai alur lama. Saat tools nonaktif, handler native berlaku kembali.

Claim dua sudut memakai `DefineRegionBuilder` asli dengan actor pemain,
permission, allowed worlds, overlap, limits, economy dan cancellable event.
Tidak menambah pembatasan mode world lebih ketat daripada `/rp claim`.
State per UUID, main-hand saja, debounce 150 ms, timeout 90 detik default,
reset saat quit/teleport/world change/cancel.

Resize memakai `RedefineRegionBuilder` asli, permission native `redefine`,
snapshot target, preview dan klik konfirmasi. Tinggi mengikuti kebijakan RP.
Perubahan target/roles/flags membatalkan pemilihan. Sudut berimpit tidak dipilih
diam-diam; Forms dapat menetapkan target atau gunakan command native.

Constructor builder lama tetap tersedia. Overload guarded berjalan pada server
thread, memproses event sebelum economy, dan menunda penggantian region lama
sampai draft siap. Setter draft tidak menulis live backend atau menggambar VFX.
Draft hanya state memori; serializer dan schema tetap native. Ini bukan
transaksi ACID baru untuk database/Vault.

## Inspect dan nearby

Stick mengikuti izin info native: detail penuh untuk role yang berhak,
ringkasan owner/name untuk kasus lainnya. Nearby mengukur jarak ke batas claim.

Lookup dibatasi `nearby-radius`, `nearby-limit`, dan
`nearby-max-regions-checked` (maksimum efektif 4096). Ia membaca cache backend
tanpa query database baru atau memuat chunk. Hasil dapat tidak lengkap jika
batas pemeriksaan terlampaui; region MySQL yang tidak ada di cache belum ikut.
`/rp near` native tetap tersedia.

## Border

| Mode | Tampilan |
|---|---|
| PARTICLE | Partikel khusus viewer dengan budget |
| BLOCK | Fake block gold pada tepi, glowstone pada sudut |
| HYBRID | Partikel dan fake block |

Tidak menempatkan block ke world. Sampling dibatasi radius/budget dan chunk
termuat. Tinggi berada pada bidang player Y dalam rentang region. Region besar
tidak diberi kotak batas palsu di sekitar viewer: hanya tepi aslinya yang
terjangkau radius yang ditampilkan.

Sesi memakai identity render agar timer lama tidak menghentikan sesi baru.
Cleanup menghapus overlay, membatalkan task dan memulihkan fake block dengan
block data server terkini saat timeout/quit/teleport/world change/reload.

Overlay glow memakai BlockDisplay nonpersistent dengan visibility default OFF,
lalu ditunjukkan ke viewer. Putih adalah default; tidak memakai team scoreboard.
Floodgate Bedrock mendapat fallback block/particle. Tampilan di balik wall,
shader/culling dan translasi Geyser masih memerlukan pengujian client.

## Forms Bedrock

Floodgate/Cumulus dideteksi melalui bridge opsional. Kegagalan API menonaktifkan
Forms, sementara command RedProtect tetap tersedia. Daftar claim dipaginasi.
Ringkasan/border dapat dibuka dari daftar; perubahan berbasis posisi native
memerlukan pemain berada pada claim tersebut.

Flags boolean diteruskan ke `FlagCommand`, termasuk aturan delay. Jenis flag
lain tersedia melalui command native. Peran memakai handler member/admin/leader
asli; rename/delete memakai handler asli dan delete meminta konfirmasi.

Sesi terikat actor/world/target/snapshot/TTL. Response kembali ke server thread,
dikonsumsi sekali, dan memeriksa permission ulang. Closed/invalid response,
quit, teleport, world change dan reload membatalkan sesi. Input tidak dirangkai
menjadi command console.

## YAML

Pasangan satu-per-satu: `config`, `globalflags`, `guiconfig`, `economy`,
`textures`, dari `.conf` ke `.yml`. Semua kandidat diperiksa roundtrip sebelum
publikasi. CONF asli dipertahankan; backup/checksum/journal berada dalam
`.zelyff-config-migration`.

Boot terputus dideteksi journal. Kandidat yang terbukti masih sama dapat
dibatalkan/diulang; target yang sudah berubah dipertahankan untuk pemulihan
manual. Setelah sukses, YAML menjadi sumber admin. Edit YAML, bukan CONF arsip.

Save memakai file sementara, read-back typed compare, fsync dan atomic move;
edit eksternal sejak load ditolak sampai reload. Unknown nodes dipertahankan
oleh mapping yang diuji. Payload mod dengan karakter kontrol ditulis sebagai
escaped strings, sehingga tidak berubah menjadi `!!binary`.

HOCON include/substitution belum dikonversi otomatis. Migrasi yang belum dapat
membuktikan kesetaraan dihentikan dengan file asal utuh. Duplicate YAML keys,
tag objek, alias rekursif, konflik awal CONF/YAML dan syntax error ditolak.
Header dipertahankan; komentar inline HOCON tidak dijamin tersalin.

`blocks.conf` menyimpan jatah blok pemain, `signs.conf` menyimpan lokasi sign
region; keduanya tetap native. Data region dan SQL tidak dikonversi.
