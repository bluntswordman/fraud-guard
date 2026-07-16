# 08 — Intermediate SQL Queries

**Audience**: Backend Engineer dan Fraud Analyst yang ingin memahami query SQL lanjutan yang digunakan untuk analisis fraud.

---

## 8.1 Mengapa Query SQL Ini Penting?

Aturan deteksi real-time berguna untuk menangkap fraud sederhana. Namun, banyak pola fraud hanya terlihat jika kita melihat **sejarah transaksi** — bukan hanya satu transaksi.

Query di bawah ini menggunakan fitur PostgreSQL:

- **CTE (`WITH`)** — query sementara yang membuat SQL lebih mudah dibaca.
- **Window Function** — fungsi yang beroperasi di sekumpulan baris terkait.

---

## 8.2 Fitur 1: Menemukan Lonjakan Transaksi (Window Function `LAG`)

### Tujuan

Menemukan transaksi di mana jumlah uang melonjak drastis dibandingkan transaksi sebelumnya oleh akun yang sama.

### Query

```sql
WITH TxHistory AS (
    SELECT
        id,
        name_orig,
        amount,
        step,
        LAG(amount) OVER (
            PARTITION BY name_orig
            ORDER BY step
        ) AS prev_amount
    FROM transactions
    WHERE type IN ('TRANSFER', 'CASH_OUT')
      AND is_fraud = FALSE
)
SELECT *
FROM TxHistory
WHERE prev_amount IS NOT NULL
  AND amount > (prev_amount * 10);
```

### Penjelasan Per Baris

| Baris | Penjelasan |
|---|---|
| `WITH TxHistory AS (...)` | Membuat CTE bernama `TxHistory`, yaitu hasil query sementara. |
| `LAG(amount) OVER (...)` | Mengambil nilai `amount` dari baris sebelumnya. |
| `PARTITION BY name_orig` | Jendela/window dibuat per akun pengirim. Jadi `LAG` hanya melihat histori akun yang sama. |
| `ORDER BY step` | Mengurutkan transaksi berdasarkan waktu (`step`). |
| `WHERE type IN ('TRANSFER', 'CASH_OUT')` | Hanya fokus ke tipe transaksi berisiko tinggi. |
| `AND is_fraud = FALSE` | Hindari menganalisis transaksi yang sudah diketahui fraud. |
| `amount > (prev_amount * 10)` | Filter: transaksi saat ini 10x lebih besar dari transaksi sebelumnya. |

### Contoh Hasil

| id | name_orig | amount | step | prev_amount |
|---|---|---|---|---|
| 102 | C123 | 1,000,000.00 | 50 | 50,000.00 |

Interpretasi: Akun C123 melakukan transaksi 1 juta, padahal transaksi sebelumnya hanya 50 ribu. Ini sangat mencurigakan.

---

## 8.3 Fitur 2: Pembersihan Log Duplikat (Updatable CTE)

### Tujuan

Menghapus laporan fraud yang duplikat, menyisakan hanya laporan terbaru per transaksi.

### Query

```sql
DELETE FROM fraud_reports
WHERE id IN (
    WITH RankedReports AS (
        SELECT
            id,
            ROW_NUMBER() OVER (
                PARTITION BY transaction_id
                ORDER BY created_at DESC
            ) AS rn
        FROM fraud_reports
    )
    SELECT id
    FROM RankedReports
    WHERE rn > 1
);
```

### Penjelasan Per Baris

| Baris | Penjelasan |
|---|---|
| `WITH RankedReports AS (...)` | Membuat CTE yang memberi nomor urut per transaksi. |
| `ROW_NUMBER() OVER (...)` | Memberi nomor 1, 2, 3, ... per kelompok. |
| `PARTITION BY transaction_id` | Pengelompokan berdasarkan ID transaksi. |
| `ORDER BY created_at DESC` | Laporan terbaru mendapat nomor 1. |
| `WHERE rn > 1` | Pilih semua laporan kecuali yang terbaru. |
| `DELETE FROM fraud_reports WHERE id IN (...)` | Hapus laporan duplikat. |

### Keunggulan PostgreSQL

Query ini menggunakan **Updatable CTE**. PostgreSQL mengizinkan `DELETE` langsung dari hasil CTE yang berisi Window Function.

MySQL tidak mengizinkan hal ini secara langsung. Di MySQL, biasanya perlu 2 langkah:

1. Simpan hasil CTE ke tabel sementara.
2. Baru lakukan `DELETE` berdasarkan tabel sementara tersebut.

---

## 8.4 Window Function Lain yang Bermanfaat

Berikut beberapa Window Function yang bisa digunakan untuk analisis fraud:

| Fungsi | Kegunaan |
|---|---|
| `LAG(column)` | Mengambil nilai baris sebelumnya. |
| `LEAD(column)` | Mengambil nilai baris berikutnya. |
| `ROW_NUMBER()` | Memberi nomor unik per kelompok. |
| `RANK()` | Memberi ranking dengan loncatan untuk nilai sama. |
| `DENSE_RANK()` | Memberi ranking tanpa loncatan. |
| `SUM(...) OVER (...)` | Menghitung total berjalan (running total). |
| `AVG(...) OVER (...)` | Menghitung rata-rata berjalan. |

---

## 8.5 Contoh Query: Running Total per Akun

```sql
SELECT
    id,
    name_orig,
    amount,
    step,
    SUM(amount) OVER (
        PARTITION BY name_orig
        ORDER BY step
    ) AS running_total
FROM transactions
WHERE type = 'TRANSFER';
```

Query ini berguna untuk melihat total transfer yang dilakukan sebuah akun seiring waktu.

---

## 8.6 Cara Menjalankan Query

### Via psql di dalam Container

```bash
docker exec -it fraudguard-postgres psql -U postgres -d fraudguard
```

Lalu jalankan query:

```sql
WITH TxHistory AS (...)
SELECT * FROM TxHistory WHERE ...;
```

### Via Aplikasi Spring Boot

Query ini juga bisa dijalankan dari Java menggunakan `JdbcTemplate`:

```java
String sql = """
    WITH TxHistory AS (
        SELECT id, name_orig, amount, step,
               LAG(amount) OVER (PARTITION BY name_orig ORDER BY step) as prev_amount
        FROM transactions
        WHERE type IN ('TRANSFER', 'CASH_OUT') AND is_fraud = FALSE
    )
    SELECT * FROM TxHistory WHERE prev_amount IS NOT NULL AND amount > (prev_amount * 10)
    """;

List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
```

---

## 8.7 Kesimpulan

Query SQL lanjutan memungkinkan FraudGuard melakukan analisis historis yang tidak mungkin hanya dengan aturan real-time sederhana.

| Fitur | Teknik | Manfaat |
|---|---|---|
| Lonjakan transaksi | `LAG` + CTE | Menemukan akun dengan transaksi tiba-tiba besar |
| Pembersihan duplikat | `ROW_NUMBER` + Updatable CTE | Menjaga data laporan tetap rapi |
| Running total | `SUM OVER` | Melihat akumulasi transaksi per akun |

Semua query ini didukung native oleh PostgreSQL, yang menjadi salah satu alasan pemilihan stack.
