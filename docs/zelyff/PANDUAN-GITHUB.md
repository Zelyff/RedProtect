# Panduan fork dan push untuk Zelyff

Ini panduan untuk saat checkpoint ingin disimpan ke GitHub. Menyimpan source
ke GitHub tidak membuat plugin otomatis siap dipasang. Status pekerjaan tetap
mengikuti `docs/zelyff/IMPLEMENTATION-STATUS.md`.

## 1. Buat salinan repository di akun sendiri

1. Buka https://github.com/FabioZumbi12/RedProtect.
2. Klik **Fork**, pilih akun lu sebagai owner, lalu buat repository.
3. Pada repository milik lu, klik **Code** dan salin URL HTTPS-nya.
4. Simpan project di drive yang cukup lega, misalnya `D:\Projects`.

Fork mempertahankan riwayat dan hubungan upstream. Kredit author asli dan
LICENSE tetap ada; modifikasi kita dicatat atas nama Zelyff.

## 2. Clone lalu pilih dasar source yang sama

Gunakan Git di terminal. Ganti `URL_FORK_LU` dengan URL HTTPS dari langkah 3;
itu placeholder, bukan teks yang diketik persis.

```sh
git clone URL_FORK_LU RedProtect-Zelyff
cd RedProtect-Zelyff
git fetch origin --tags
git switch -c zelyff-fork b097dfe00b18576256df55faf1e2e090e98b85a0
git rev-parse HEAD
```

Perintah terakhir harus menghasilkan SHA panjang yang sama. Jika commit tidak
dikenal karena fork belum mengambil history-nya:

```sh
git remote add upstream https://github.com/FabioZumbi12/RedProtect.git
git fetch upstream tag v8.1.4
git switch -c zelyff-fork v8.1.4
```

Jalankan blok alternatif hanya jika blok sebelumnya gagal membuat branch.
Jika remote `upstream` sudah ada, cek `git remote -v`; jangan tambah lagi.

## 3. Masukkan source implementasi

Ekstrak paket checkpoint. Salin **isi folder** source ke folder hasil clone,
sehingga `pom.xml` berada tepat di root hasil clone. Salin file dengan nama
sama ketika diminta. Folder `.git` milik hasil clone tetap dipertahankan;
paket checkpoint tidak berisi `.git`.

Kemudian:

```sh
git status --short
git diff --stat
```

Source sekarang mencakup implementasi fitur pada Core dan Spigot. Daftar yang
berubah tercantum di `docs/zelyff/IMPLEMENTATION-STATUS.md`. Folder baseline dan
blueprint dipertahankan sebagai riwayat keputusan, dengan status implementasi
terbaru pada dokumen tersebut.

## 4. Commit dan push branch sendiri

Sesuaikan pesan commit dengan status nyata yang tertulis di checkpoint.

```sh
git add -A
git diff --cached --stat
git commit -m "Implement Zelyff tools borders YAML and optional Bedrock Forms"
git push -u origin zelyff-fork
```

Log build di `evidence` sudah dikecualikan secara khusus dari aturan ignore
log umum, sehingga dapat ikut commit. Jangan masukkan folder `target`, data
server, password database, atau file credentials.

Branch `zelyff-fork` dipakai untuk pengerjaan ini. Workflow upstream yang ada
di checkout menjalankan publikasi release saat push `master` atau tag; branch
pengerjaan ini membantu menyimpan progres tanpa memicu jalur release tersebut.

## 5. Untuk update source berikutnya

Di clone dan branch yang sama: salin perubahan baru, baca status checkpoint,
jalankan pemeriksaan yang disebut di sana, lalu commit dan push lagi. Tidak
perlu membuat repository atau fork baru setiap update.

Jika muncul error, salin pesan error lengkap beserta perintahnya. Jangan
jalankan force-push, hapus folder `.git`, atau mengganti backend database untuk
mengatasi error Git/build.

## 6. Build lewat GitHub

Setelah push ke branch `zelyff-fork`, workflow **Zelyff build** akan menjalankan
Maven dan tes. Buka tab **Actions**, pilih run tersebut, lalu baca hasilnya.
Jika berhasil, unduh artifact **RedProtect-Zelyff-development** dan ekstrak
JAR-nya. Workflow ini hanya membuat artifact pengembangan; hasil tes client
Java/Bedrock tetap mengikuti status implementasi.

Jika run gagal, buka langkah **Compile and test** untuk melihat error yang
sebenarnya. Workflow ini belum dijalankan pada GitHub oleh asisten.
