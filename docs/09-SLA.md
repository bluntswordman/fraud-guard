# 09 — Service Level Agreement (SLA)

**Audience**: Tech Lead, SRE, dan Stakeholder yang ingin tahu target ketersediaan, performa, dan konsistensi sistem.

> **Catatan**: SLA ini adalah target arsitektur untuk scope mini-project. Untuk production sebenarnya, angka-angka ini harus diukur dan disepakati bersama tim operasional.

---

## 9.1 Availability & Isolation

| Komponen | Target Availability | Justifikasi |
|---|---|---|
| **Ingestion Service** | 99.9% | Jika down, data baru tidak bisa masuk. Service ini stateless, jadi bisa di-restart tanpa kehilangan data. |
| **Detection Service** | 99.9% | Jika down, event dari Redis tidak diproses sementara waktu. Sistem inti tetap berjalan karena tidak blocking. |
| **Database (PostgreSQL)** | 99.99% | Single source of truth. Data disimpan di Docker volume untuk persistensi. |

### Apa Itu Availability?

**Availability** adalah persentase waktu sistem bisa diakses dan berjalan normal.

- 99.9% = maksimal downtime ~8.7 jam per tahun.
- 99.99% = maksimal downtime ~52 menit per tahun.

Untuk mini-project, target di atas sudah cukup realistis.

---

## 9.2 Performance & Latency Targets

| Metrik | Target | Titik Pengukuran |
|---|---|---|
| **CSV Processing Speed** | > 5,000 rows/sec | Proses batching dengan Java Stream di Ingestion Service |
| **Inter-Service Communication** | < 50 ms | Latency publish/subscribe Redis Pub/Sub |
| **Intermediate SQL Execution** | < 200 ms | Eksekusi query Window Function pada tabel 1 juta+ baris |

### Penjelasan Target

#### CSV Processing Speed

Target 5.000 baris per detik berarti file CSV dengan 50.000 baris bisa diproses dalam ~10 detik. Ini tergantung juga pada:

- Spesifikasi hardware.
- Kompleksitas transformasi.
- Kecepatan write ke PostgreSQL.

Untuk meningkatkan kecepatan, bisa digunakan:

- Batch insert (bukan insert satu per satu).
- Multithreading atau parallel stream.
- Index yang minimal saat bulk insert.

#### Inter-Service Communication

Redis Pub/Sub biasanya memiliki latency di bawah 1 ms dalam satu network lokal. Target < 50 ms sangat aman dan memberi ruang untuk overhead aplikasi.

#### Intermediate SQL Execution

Window Function pada tabel besar bisa lambat jika tidak ada index yang tepat. Index `idx_txn_orig_step` sangat membantu query yang mempartisi berdasarkan `name_orig` dan mengurutkan berdasarkan `step`.

---

## 9.3 Data Consistency

### ACID Compliance: 100%

PostgreSQL menjamin ACID untuk setiap transaksi:

| Aspek | Jaminan |
|---|---|
| **Atomicity** | Sebuah transaksi database either semua berhasil atau semua gagal. Tidak ada setengah berhasil. |
| **Consistency** | Database selalu dalam keadaan valid setelah transaksi. |
| **Isolation** | Transaksi yang berjalan bersamaan tidak saling mengganggu. |
| **Durability** | Data yang sudah commit tidak hilang meski sistem crash. |

> 💡 **Kenapa ini penting?** Karena kita menulis data transaksi keuangan. Tidak boleh ada transaksi setengah tersimpan atau hilang.

---

### Idempotency di Detection Service

**Idempotency** berarti menjalankan operasi yang sama berkali-kali menghasilkan hasil yang sama.

Detection Service harus idempoten. Caranya: sebelum memproses event dari Redis, periksa dulu apakah transaksi sudah ditandai:

```sql
UPDATE transactions
SET is_flagged_fraud = TRUE
WHERE id = ?
  AND is_flagged_fraud = FALSE;
```

Atau di Java:

```java
Transaction tx = transactionRepository.findById(txId).orElseThrow();

if (Boolean.TRUE.equals(tx.getIsFlaggedFraud())) {
    log.info("Transaction {} already flagged, skipping", txId);
    return;
}

// jalankan strategi
// update flag
```

#### Mengapa Idempotency Penting?

- Jika Detection Service di-restart, dia mungkin menerima event yang sama lagi.
- Tanpa idempotency, transaksi yang sama bisa diproses berulang kali dan menimbulkan duplikat log/report.
- Dengan idempotency, hasil akhir tetap konsisten.

---

## 9.4 Failure Scenarios

| Skenario | Dampak | Mitigasi |
|---|---|---|
| Ingestion Service down | Data CSV tidak bisa masuk | Restart service; data CSV bisa diupload ulang |
| Detection Service down | Event tidak diproses sementara | Restart service; Ingestion tetap berjalan |
| Redis down | Komunikasi antar service terputus | Restart container Redis; service menunggu koneksi |
| PostgreSQL down | Tidak bisa menyimpan/membaca data | Restart container; data tetap ada karena Docker volume |

---

## 9.5 Kesimpulan

SLA FraudGuard menekankan 3 hal:

1. **Ketersediaan** — target 99.9% untuk service dan 99.99% untuk database.
2. **Performa** — deteksi cepat dengan latensi rendah.
3. **Konsistensi** — ACID di PostgreSQL dan idempotency di Detection Service.

Target-target ini menjadi acuan saat mengimplementasikan, men-deploy, dan memantau sistem.
