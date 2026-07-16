# FraudGuard Documentation

Dokumentasi lengkap untuk proyek **FraudGuard** — sistem deteksi anomali transaksi keuangan real-time berbasis mini-microservices.

---

## 🎯 Tujuan Dokumentasi Ini

Dokumentasi ini dirancang agar bisa dipahami oleh engineer dari berbagai level:

- **Junior Engineer**: bisa memahami *apa* yang dibangun, *mengapa*, dan *alur data* secara umum.
- **Mid Engineer**: bisa memahami komponen, pattern, query, dan cara menjalankan sistem.
- **Senior Engineer**: bisa mengevaluasi keputusan arsitektur, trade-off, dan extensibility.

Setiap dokumen diawali dengan bagian **Audience** agar pembaca tahu apa yang diharapkan.

---

## 📚 Daftar Dokumen

| No | File | Fokus | Pembaca Utama |
|---|---|---|---|
| 1 | [01-BRD.md](./01-BRD.md) | Kebutuhan bisnis, masalah, tujuan, dan KPI | Product Manager, Tech Lead, Engineer |
| 2 | [02-PRD.md](./02-PRD.md) | User persona dan functional requirements | Engineer, QA, Analyst |
| 3 | [03-NFR.md](./03-NFR.md) | Non-functional requirements | Tech Lead, Architect |
| 4 | [04-ARCHITECTURE.md](./04-ARCHITECTURE.md) | Arsitektur sistem dan alur data | Semua level |
| 5 | [05-TECH-STACK.md](./05-TECH-STACK.md) | Alasan pemilihan teknologi | Engineer, interviewer |
| 6 | [06-DESIGN-PATTERNS.md](./06-DESIGN-PATTERNS.md) | Strategy Pattern, IoC, Java Stream | Engineer |
| 7 | [07-DATABASE-SCHEMA.md](./07-DATABASE-SCHEMA.md) | Skema database dan tipe data | Backend Engineer, DBA |
| 8 | [08-SQL-QUERIES.md](./08-SQL-QUERIES.md) | Contoh Intermediate SQL | Backend Engineer, Data Analyst |
| 9 | [09-SLA.md](./09-SLA.md) | Target ketersediaan, performa, konsistensi | Tech Lead, SRE |
| 10 | [10-SETUP-GUIDE.md](./10-SETUP-GUIDE.md) | Cara build, run, dan test sistem | Engineer, DevOps |
| 11 | [11-API-SPEC.md](./11-API-SPEC.md) | API specification untuk Ingestion Service | Engineer, QA, Integrator |

---

## 🗺️ Rekomendasi Urutan Baca

### Untuk Junior Engineer

```
README → 04-ARCHITECTURE → 02-PRD → 10-SETUP-GUIDE
```

### Untuk Mid Engineer

```
01-BRD → 02-PRD → 04-ARCHITECTURE → 05-TECH-STACK → 06-DESIGN-PATTERNS → 07-DATABASE-SCHEMA → 10-SETUP-GUIDE
```

### Untuk Senior Engineer / Architect

```
01-BRD → 03-NFR → 04-ARCHITECTURE → 05-TECH-STACK → 06-DESIGN-PATTERNS → 07-DATABASE-SCHEMA → 08-SQL-QUERIES → 09-SLA
```

---

## 📖 Glossary (Istilah Penting)

| Istilah | Penjelasan Sederhana |
|---|---|
| **Microservice** | Aplikasi dipecah menjadi service kecil yang berdiri sendiri. |
| **Latency** | Waktu tunggu dari data masuk sampai diproses. |
| **Pub/Sub** | Publish/Subscribe: satu service kirim pesan, service lain dengarkan. |
| **Strategy Pattern** | Pola desain di mana kita punya banyak algoritma dan bisa memilih/gabungkan dengan mudah. |
| **IoC (Inversion of Control)** | Spring yang mengatur objek-objek aplikasi, bukan kita yang membuat manual. |
| **NUMERIC(18,2)** | Tipe data PostgreSQL untuk angka presisi tetap, aman untuk uang. |
| **CTE (Common Table Expression)** | Query sementara dengan `WITH`, membuat SQL kompleks lebih mudah dibaca. |
| **Window Function** | Fungsi SQL yang menghitung sesuatu di sekumpulan baris (misal: `LAG`, `ROW_NUMBER`). |
| **Idempotency** | Proses yang sama dijalankan berkali-kali hasilnya tetap sama. |
| **ACID** | Atomicity, Consistency, Isolation, Durability — jaminan transaksi database aman. |

---

## 📝 Changelog

| Versi | Tanggal | Keterangan |
|---|---|---|
| 1.0.0 | 2026 | Dokumentasi awal dalam `DOCUMENTATION.md` |
| 1.1.0 | 2026 | Dipecah menjadi file-file terpisah di folder `docs/` untuk kemudahan navigasi |
| 1.2.0 | 2026 | Menambahkan root README, Mermaid diagram, dan API specification |

---

© 2026 FraudGuard Project Architecture Documentation
