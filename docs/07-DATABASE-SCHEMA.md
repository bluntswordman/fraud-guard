# 07 — Database Schema

**Audience**: Backend Engineer, DBA, dan Data Analyst yang ingin memahami struktur data dan alasan pemilihan tipe data.

---

## 7.1 Skema Tabel `transactions`

```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    step INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    name_orig VARCHAR(50) NOT NULL,
    name_dest VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'PAYSIM',
    old_balance_orig NUMERIC(18,2) DEFAULT 0,
    new_balance_orig NUMERIC(18,2) DEFAULT 0,
    is_fraud BOOLEAN NOT NULL DEFAULT FALSE,
    is_flagged_fraud BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_txn_orig_step ON transactions(name_orig, step);
CREATE INDEX idx_fraud_flag ON transactions(is_fraud) WHERE is_fraud = TRUE;
CREATE INDEX idx_tenant_id ON transactions(tenant_id);
```

---

## 7.2 Penjelasan Setiap Kolom

| Kolom | Tipe Data | Keterangan |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` | ID unik transaksi, auto-increment. |
| `step` | `INT NOT NULL` | Unit waktu. |
| `type` | `VARCHAR(20) NOT NULL` | Jenis: `TRANSFER`, `CASH_OUT`, `PAYMENT`, `CASH_IN`, `DEBIT`, `TOPUP`, `REDEEM`, `EARN`. |
| `amount` | `NUMERIC(18,2) NOT NULL` | Nilai transaksi — presisi tetap, 18 digit total, 2 di belakang koma. |
| `name_orig` | `VARCHAR(50) NOT NULL` | ID akun pengirim. |
| `name_dest` | `VARCHAR(50) NOT NULL` | ID akun penerima. |
| `tenant_id` | `VARCHAR(50) NOT NULL DEFAULT 'PAYSIM'` | **Multi-tenant** — identitas ERP/vendor pengirim. CSV default `PAYSIM`, REST wajib diisi. |
| `old_balance_orig` | `NUMERIC(18,2) DEFAULT 0` | Saldo sebelum transaksi. |
| `new_balance_orig` | `NUMERIC(18,2) DEFAULT 0` | Saldo setelah transaksi. |
| `is_fraud` | `BOOLEAN NOT NULL DEFAULT FALSE` | Label fraud dari dataset (ground truth). |
| `is_flagged_fraud` | `BOOLEAN NOT NULL DEFAULT FALSE` | Hasil deteksi dari Detection Service. |

---

## 7.3 Tabel `pending_detections` (Outbox)

Mencatat setiap transaksi yang perlu diproses oleh Detection Service. Berfungsi sebagai **outbox pattern** — menjamin transaksi tetap diproses meski Detection Service sempat down.

```sql
CREATE TABLE pending_detections (
    id BIGSERIAL PRIMARY KEY,
    tx_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    rules VARCHAR(500)
);
```

| Kolom | Keterangan |
|---|---|
| `tx_id` | Foreign key ke `transactions.id`. |
| `status` | `PENDING` (belum diproses) atau `DONE` (selesai). |
| `created_at` | Timestamp saat entry dibuat. |
| `rules` | Strategy filter (comma-separated), null = semua strategy. |

**RecoveryJob** berjalan setiap 30 detik: ambil `status=PENDING AND created_at < NOW() - 30s` → proses ulang → set `DONE`.

---

## 7.4 Tabel `webhook_configs` (Webhook Registration)

Menyimpan URL webhook per tenant untuk pengiriman hasil deteksi ke sistem eksternal.

```sql
CREATE TABLE webhook_configs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    secret VARCHAR(256),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

| Kolom | Keterangan |
|---|---|
| `tenant_id` | Tenant yang mendaftarkan webhook. |
| `url` | URL tujuan — **wajib HTTPS**, private IP diblok (SSRF protection). |
| `secret` | Kunci verifikasi — dikirim via header `X-Webhook-Secret`. |
| `enabled` | Status aktif/non-aktif. |

---

## 7.5 Mengapa `NUMERIC(18,2)` untuk Uang?

### Masalah dengan Floating-Point

Tipe data seperti `FLOAT` atau `DOUBLE` menyimpan angka dalam format biner dengan perkiraan. Contoh klasik:

```java
System.out.println(0.1 + 0.2);
// Output: 0.30000000000000004
```

Dalam perhitungan keuangan, error kecil seperti ini tidak bisa diterima.

### Keunggulan `NUMERIC`

```sql
amount NUMERIC(18,2)
```

- Menyimpan angka dalam format desimal presisi tetap.
- `18` = total digit yang bisa disimpan.
- `2` = digit di belakang koma.
- Cocok untuk mata uang dengan 2 desimal.
- Tidak ada error akumulasi dalam perhitungan.

### Contoh Perbandingan

| Tipe Data | Penyimpanan `0.1` | Hasil `0.1 + 0.1 + 0.1` |
|---|---|---|
| `DOUBLE` | perkiraan | `0.30000000000000004` |
| `NUMERIC(18,2)` | eksak | `0.30` |

---

## 7.6 Menggunakan `BOOLEAN` Native

PostgreSQL memiliki tipe data `BOOLEAN` asli:

```sql
is_fraud BOOLEAN NOT NULL DEFAULT FALSE
```

Keuntungan:

- Lebih jelas dibanding `TINYINT(1)` atau `CHAR(1)`.
- Lebih efisien dalam penyimpanan (1 byte).
- Query lebih mudah dibaca: `WHERE is_fraud = TRUE`.

---

## 7.7 Index untuk Performa

### Index 1: `idx_txn_orig_step`

```sql
CREATE INDEX idx_txn_orig_step ON transactions(name_orig, step);
```

**Fungsi**: Mempercepat query analisis historis per akun, terutama query Window Function yang mempartisi berdasarkan `name_orig` dan diurutkan berdasarkan `step`.

**Contoh query yang dipercepat**:

```sql
SELECT id, name_orig, amount, step,
       LAG(amount) OVER (PARTITION BY name_orig ORDER BY step) as prev_amount
FROM transactions;
```

### Index 2: `idx_fraud_flag`

```sql
CREATE INDEX idx_fraud_flag ON transactions(is_fraud) WHERE is_fraud = TRUE;
```

**Fungsi**: Mempercepat query yang hanya mencari transaksi fraud.

Ini adalah **partial index** — hanya menyimpan baris yang `is_fraud = TRUE`, sehingga lebih kecil dan lebih cepat.

**Contoh query yang dipercepat**:

```sql
SELECT * FROM transactions WHERE is_fraud = TRUE;
```

---

## 7.8 Skema Masa Depan (Opsional)

Jika sistem berkembang, beberapa tabel tambahan mungkin diperlukan:

```sql
-- Tabel untuk menyimpan hasil analisis per strategy
CREATE TABLE fraud_reports (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    strategy_name VARCHAR(50) NOT NULL,
    score INT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Tabel untuk audit log
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    payload TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## 7.9 Kesimpulan

Skema database FraudGuard dirancang dengan prinsip:

- **Presisi finansial** dengan `NUMERIC(18,2)`.
- **Kejelasan data** dengan `BOOLEAN` native.
- **Multi-tenancy** dengan `tenant_id` + index.
- **Reliability** dengan `pending_detections` outbox + RecoveryJob.
- **Extensibility** dengan `webhook_configs` per tenant.
- **Performa analisis** dengan index yang tepat.
- **Skalabilitas** dengan `BIGSERIAL` untuk ID transaksi.

Dokumen selanjutnya akan menampilkan contoh query SQL lanjutan yang memanfaatkan skema ini.
