# 02 — Product Requirements Document (PRD)

**Audience**: Engineer, QA, Data Analyst, dan Product Manager yang ingin tahu *apa saja fitur* yang harus ada.

---

## 2.1 User Personas

| Persona | Peran | Apa yang Ingin Dilakukan |
|---|---|---|
| **Data Engineer** | Mengelola data masuk | Mengunggah dataset CSV (misal PaySim dari Kaggle) ke sistem agar bisa diproses dan disimpan. |
| **Fraud Analyst** | Menganalisis pola fraud | Menjalankan query SQL lanjutan (Window Function, CTE) untuk menemukan pola anomali. |
| **System (Internal)** | Consumer event | Service lain yang perlu tahu status fraud sebuah transaksi secara real-time dari message broker. |

---

## 2.2 Functional Requirements

### Epic 1: Data Ingestion & Processing

**Tujuan Epic**: Membaca file CSV, membersihkan dan mentransformasi data, lalu menyimpannya ke database dan memancarkan event.

#### US-1.1: Membaca File CSV

- **Sebagai** Data Engineer
- **Saya ingin** sistem bisa membaca file CSV dataset keuangan (contoh: PaySim dari Kaggle)
- **Sehingga** data transaksi bisa masuk ke sistem tanpa harus input manual

**Acceptance Criteria**:

- Endpoint menerima upload file CSV.
- Sistem membaca setiap baris CSV.
- Baris yang kosong atau tidak valid dilewati (tidak menyebabkan error total).

---

#### US-1.2: Transformasi Data dengan Java Stream API

- **Sebagai** Data Engineer
- **Saya ingin** data mentah diproses menggunakan Java Stream API sebelum disimpan
- **Sehingga** data bersih, terstruktur, dan siap untuk analisis

**Acceptance Criteria**:

- Setiap baris CSV dipetakan menjadi objek transaksi.
- Dilakukan filtering untuk nilai yang tidak masuk akal, misalnya `amount <= 0`.
- Bisa dilakukan agregasi dasar per tipe transaksi untuk statistik.

**Contoh transformasi**:

```java
Map<String, DoubleSummaryStatistics> statsByType = rawData.stream()
    .filter(tx -> tx.getAmount() > 0)
    .map(this::mapToEntity)
    .collect(Collectors.groupingBy(
        Transaction::getType,
        Collectors.summarizingDouble(Transaction::getAmount)
    ));
```

> 💡 **Kenapa Java Stream?** Karena lebih mudah dibaca dan lebih fungsional dibanding loop manual. Cocok untuk transformasi data dalam jumlah besar.

---

#### US-1.3: Publish Event Setelah Data Tersimpan

- **Sebagai** System
- **Saya ingin** setiap transaksi yang berhasil disimpan memancarkan event ID ke message broker
- **Sehingga** service lain bisa langsung bereaksi secara real-time

**Acceptance Criteria**:

- Setelah transaksi tersimpan di PostgreSQL, ID transaksi dipublish ke Redis channel.
- Format pesan jelas dan ringan, contohnya hanya ID transaksi.
- Publish dilakukan setelah commit database berhasil.

---

### Epic 2: Real-time Detection Engine

**Tujuan Epic**: Mendengarkan event transaksi baru, lalu menjalankan aturan deteksi fraud secara otomatis.

#### US-2.1: Subscribe Event Transaksi Baru

- **Sebagai** Detection Service
- **Saya ingin** mendengarkan event transaksi baru secara asynchronous
- **Sehingga** saya bisa memproses transaksi begitu tersedia tanpa memblokir service lain

**Acceptance Criteria**:

- Detection service subscribe ke channel Redis yang sama dengan Ingestion service.
- Proses subscribe berjalan di thread terpisah (asynchronous).
- Service bisa menerima banyak event tanpa harus menunggu satu per satu selesai.

---

#### US-2.2: Menjalankan Aturan Deteksi Otomatis

- **Sebagai** Fraud Analyst
- **Saya ingin** sistem menjalankan serangkaian aturan deteksi secara otomatis
- **Sehingga** transaksi mencurigakan bisa ditandai tanpa intervensi manual

**Acceptance Criteria**:

- Engine deteksi menerima daftar aturan dari Spring IoC.
- Setiap aturan mengimplementasikan interface `DetectionStrategy`.
- Engine menjalankan semua aturan terhadap satu transaksi.
- Jika aturan mendeteksi fraud, kolom `is_flagged_fraud` diupdate menjadi `TRUE`.

**Contoh aturan yang tersedia**:

| Nama Strategy | Fungsi |
|---|---|---|
| `VelocityStrategy` | Mendeteksi lonjakan jumlah transaksi dalam waktu singkat. |
| `BalanceAnomalyStrategy` | Mendeteksi perubahan saldo yang tidak wajar. |
| `LifecycleStrategy` | Mendeteksi pelanggaran urutan transaksi (TOPUP → REDEEM → EARN). REDEEM tanpa prior TOPUP atau EARN tanpa prior REDEEM = fraud. Cocok untuk membership/loyalty point system. |

> 💡 **Kenapa Strategy Pattern?** Karena menambah aturan baru tidak perlu mengubah kode engine. Cukup buat class baru yang implementasi interface.

---

#### US-2.3: Analisis Pola Historis dengan Intermediate SQL

- **Sebagai** Fraud Analyst
- **Saya ingin** sistem bisa menganalisis pola historis menggunakan SQL lanjutan
- **Sehingga** saya bisa menemukan anomali yang tidak terlihat hanya dari satu transaksi

**Acceptance Criteria**:

- Tersedia query SQL dengan Window Function (`LAG`, `ROW_NUMBER`, dll.).
- Tersedia query SQL dengan CTE (`WITH`).
- Query bisa dijalankan langsung di PostgreSQL untuk analisis ad-hoc.

---

## 2.3 Fitur Tambahan (Out of Scope untuk v1)

Fitur berikut **tidak wajib** di versi 1.0, tapi bisa dikembangkan di versi berikutnya:

- UI Dashboard untuk Fraud Analyst.
- Machine Learning model untuk deteksi fraud.
- Notifikasi email/SMS saat fraud terdeteksi.
- Audit log lengkap untuk setiap keputusan deteksi.

---

## 2.4 Ringkasan Alur Fitur

```text
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│ Upload CSV  │────▶│ Ingestion Service│────▶│ PostgreSQL  │
└─────────────┘     └──────────────────┘     └──────┬──────┘
                                                    │
                                                    ▼
                                            ┌───────────────┐
                                            │ Publish ID    │
                                            │ ke Redis      │
                                            └───────┬───────┘
                                                    │
                                                    ▼
                                            ┌───────────────┐
                                            │ Detection Svc │
                                            │ Subscribe     │
                                            └───────┬───────┘
                                                    │
                                                    ▼
                                            ┌───────────────┐
                                            │ Jalankan      │
                                            │ Strategies    │
                                            └───────┬───────┘
                                                    │
                                                    ▼
                                            ┌───────────────┐
                                            │ Update flag   │
                                            │ di PostgreSQL │
                                            └───────────────┘
```

---

## 2.5 Kesimpulan

PRD ini menjelaskan bahwa FraudGuard memiliki 2 epic utama:

1. **Data Ingestion** — membaca, membersihkan, menyimpan, dan memancarkan event.
2. **Real-time Detection** — menerima event, menjalankan aturan, dan menandai fraud.

Semua fitur dirancang agar bisa dijalankan dalam Docker dan mudah diperluas di masa depan.
