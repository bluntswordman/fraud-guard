# 05 — Technology Stack Justification

**Audience**: Engineer yang ingin tahu *mengapa* teknologi-teknologi ini dipilih, dan interviewer yang menanyakan alasan keputusan arsitektur.

---

## 5.1 Daftar Teknologi

| Layer | Teknologi | Versi | Fungsi |
|---|---|---|---|
| Bahasa Pemrograman | Java | 17 | Backend service |
| Framework | Spring Boot | 3.x | Microservice, IoC, REST API |
| Build Tool | Maven | 3.x | Build dan dependency management |
| Database | PostgreSQL | 15 | Penyimpanan data transaksi |
| Cache / Broker | Redis | 7 | Pub/Sub message broker + cache |
| Container | Docker + Docker Compose | Latest | Portability dan deployment |

---

## 5.2 PostgreSQL 15

### Alasan Memilih PostgreSQL

PostgreSQL dipilih daripada MySQL untuk proyek ini karena 3 alasan utama:

#### 1. Tipe Data `NUMERIC` untuk Uang

```sql
amount NUMERIC(18,2) NOT NULL
```

- `NUMERIC` menyimpan angka dengan presisi tetap (fixed-point).
- Tidak ada floating-point error seperti pada `FLOAT` atau `DOUBLE`.
- Sangat penting untuk perhitungan keuangan.

> 💡 **Interview tip**: "Saya menggunakan `NUMERIC(18,2)` di PostgreSQL untuk menghindari floating-point error. Jumlah digit total 18, dengan 2 digit di belakang koma."

#### 2. Tipe Data `BOOLEAN` Native

```sql
is_fraud BOOLEAN NOT NULL DEFAULT FALSE
```

- PostgreSQL memiliki tipe `BOOLEAN` asli.
- Lebih jelas dan efisien dibandingkan menyimpan boolean sebagai `TINYINT(1)` di MySQL.

#### 3. Updatable CTE

PostgreSQL mendukung operasi `DELETE` atau `UPDATE` langsung dari hasil CTE (Common Table Expression). Fitur ini sangat berguna untuk query pembersihan data duplikat berdasarkan Window Function.

```sql
DELETE FROM fraud_reports
WHERE id IN (
    WITH RankedReports AS (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY transaction_id ORDER BY created_at DESC) as rn
        FROM fraud_reports
    )
    SELECT id FROM RankedReports WHERE rn > 1
);
```

MySQL tidak mengizinkan `DELETE` langsung dari hasil CTE/Window Function seperti ini.

---

## 5.3 Redis 7

### Fungsi Ganda

Redis di FraudGuard memiliki 2 fungsi utama:

1. **Message Broker (Pub/Sub)** — komunikasi antar microservice.
2. **In-Memory Cache** — untuk caching data sering diakses (opsional di v1).

### Alasan Memilih Redis

- **Ringan**: Hanya butuh satu container.
- **Cepat**: Operasi Pub/Sub di Redis sangat cepat (microsecond latency).
- **Mudah**: Tidak perlu setup cluster seperti Kafka.
- **Familiar**: Banyak engineer sudah mengenal Redis.

### Kapan Harus Ganti ke Kafka?

Gunakan Kafka jika:

- Jumlah event per detik sangat besar (ratusan ribu atau jutaan).
- Pesan harus persist dan tidak boleh hilang saat consumer offline.
- Butuh consumer group yang kompleks.

Untuk scope mini-project, Redis Pub/Sub sudah lebih dari cukup.

---

## 5.4 Docker Compose

### Alasan Menggunakan Docker Compose

- **Consistency**: Environment di laptop developer, staging, dan produksi sama.
- **Ease of Setup**: Cukup `docker compose up` untuk menjalankan semua service.
- **Isolation**: Setiap service berjalan di container sendiri.
- **Reproducibility**: Konfigurasi tersimpan di `docker-compose.yml`, mudah direview.

### Manfaat untuk Tim

- Developer baru tidak perlu menghabiskan waktu lama untuk setup environment.
- CI/CD pipeline bisa menjalankan test dengan environment yang sama persis.
- Rollback ke versi sebelumnya lebih mudah dengan image Docker.

---

## 5.5 Java 17 + Spring Boot

### Alasan Memilih Java

- **Stabil dan matang** — banyak library dan komunitas.
- **Performa tinggi** — JVM sudah dioptimalkan untuk workload backend.
- **Stream API** — sangat cocok untuk transformasi data seperti di Ingestion Service.

### Alasan Memilih Spring Boot

- **Rapid development** — banyak starter dependency.
- **Spring IoC** — memudahkan Dependency Injection dan Strategy Pattern.
- **Built-in actuator** — health check dan monitoring siap pakai.
- **Integrasi mudah** — dengan PostgreSQL, Redis, JPA, JDBC, dll.

---

## 5.6 Maven

Maven digunakan sebagai build tool karena:

- Standar di ekosistem Java.
- Mudah mengatur dependency.
- `mvn package` menghasilkan file JAR yang siap dijalankan.
- Kompatibel dengan Spring Boot dan Docker multi-stage build.

---

## 5.7 Kesimpulan Pemilihan Stack

| Teknologi | Alasan Utama |
|---|---|
| PostgreSQL | Presisi uang (`NUMERIC`), boolean native, Updatable CTE |
| Redis | Broker ringan, cepat, dan familiar |
| Docker Compose | Portability dan konsistensi environment |
| Java 17 | Stabil, performa tinggi, Stream API |
| Spring Boot | IoC, Strategy Pattern, development cepat |
| Maven | Standar build tool Java |

Pemilihan stack ini sengaja dijaga **sederhana tapi produktif**, agar proyek mini tetap bisa mengilustrasikan best practice arsitektur enterprise.
