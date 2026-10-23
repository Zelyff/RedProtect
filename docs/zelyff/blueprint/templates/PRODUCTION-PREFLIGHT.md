# Production Preflight & Go/No-Go — template (jangan isi dengan rahasia)

> Dokumen ini harus diisi oleh operator/coding agent **ketika implementasi dan staging siap**. Tidak perlu data server saat fase perencanaan sekarang.

## Identitas build & storage
- Tanggal / operator / environment:
- Original plugin filename/version/full SHA-256:
- Fork JAR full SHA-256/source commit:
- Minecraft/Paper/AdvancedSlimePaper/Java versions:
- Geyser/Floodgate/other hook versions:
- Region backend (FILE / SQL / OTHER; berdasarkan bukti):
- DB engine/schema version **tanpa host/secret**:
- Config inventory & parser evidence location:
- Known source storage writer/auto-upgrade hooks and outcomes:

## Backup & restore proof
| Check | PASS/FAIL/BLOCKED | Bukti | Owner |
|---|---|---|---|
| Snapshot A dibuat ketika server sepenuhnya berhenti/consistent |  |  |  |
| Snapshot B tepat sebelum production cutover |  |  |  |
| JAR asli + CONF + region data/DB tersimpan dalam satu manifest |  |  |  |
| Hash/ukuran/timestamp terverifikasi |  |  |  |
| Staging DB terisolasi dari produksi |  |  |  |
| Restore rehearsal berhasil dengan upstream |  |  |  |

## Parity & regression
| Check | PASS/FAIL/BLOCKED | Bukti | Owner |
|---|---|---|---|
| Region keys/count/canonical content sama |  |  |  |
| Owner/UUID/member/flag/priority/parent-child sama |  |  |  |
| Konfigurasi typed+effective CONF/YAML sama |  |  |  |
| Tidak ada schema/data writes tak terjelaskan |  |  |  |
| Plugin protection enabled; hooks dan perms lulus |  |  |  |
| Semua test produksi relevan T-035..T-046 lulus |  |  |  |
| Feature-off behavior == upstream |  |  |  |

## Keputusan
- **Verdict:** [ ] GO [ ] NO-GO (default NO-GO)
- Blocker terbuka:
- Rollback checkpoint & location (no secrets):
- Rollback rehearsal evidence:
- Operator approval:
- Tanggal/timezone:

**Aturan:** satu item kritis FAIL/BLOCKED → NO-GO. Jangan restart produksi untuk ‘coba dulu’.
