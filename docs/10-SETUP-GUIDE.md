# 10 — Setup & Execution Guide

**Audience**: Engineer, DevOps, dan siapa saja yang ingin menjalankan FraudGuard di lokal machine.

---

## 10.1 Prerequisites (Prasyarat)

Pastikan perangkat lunak berikut sudah terinstall:

| Software | Versi Minimal | Fungsi |
|---|---|---|
| Docker | 20.x | Menjalankan container |
| Docker Compose | 2.x | Menjalankan multi-container |
| Java | 17 | Build aplikasi dari source (opsional jika pakai image pre-built) |
| Maven | 3.9.x | Build project Spring Boot (opsional jika pakai image pre-built) |
| curl | any | Mengirim request test |

Cek versi yang terinstall:

```bash
docker --version
docker compose version
java -version
mvn -version
```

---

## 10.2 Struktur Folder yang Diharapkan

```text
mandiri/
├── docs/                           # Dokumentasi (folder ini)
├── ingestion-service/              # Project Spring Boot Ingestion
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── detection-service/              # Project Spring Boot Detection
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── docker-compose.yml              # Definisi semua service
├── dataset/                        # Folder untuk menyimpan CSV
│   └── paysim.csv
└── DOCUMENTATION.md                # Dokumentasi lama (referensi)
```

> 💡 **Tips**: Pastikan file CSV PaySim dari Kaggle diletakkan di folder `dataset/`. Jika belum punya, bisa menggunakan dataset contoh yang memiliki kolom: `step`, `type`, `amount`, `nameOrig`, `nameDest`, `oldbalanceOrg`, `newbalanceOrig`, `isFraud`, `isFlaggedFraud`.

---

## 10.3 Step 1: Build Java Applications

Jika ingin build dari source, jalankan perintah berikut dari root project:

```bash
# Build Ingestion Service
cd ingestion-service
mvn clean package -DskipTests
cd ..

# Build Detection Service
cd detection-service
mvn clean package -DskipTests
cd ..
```

Atau dari root project jika ada parent POM:

```bash
mvn clean package -DskipTests
```

Setelah build berhasil, akan muncul file JAR di folder `target/`:

```text
ingestion-service/target/ingestion-service-0.0.1-SNAPSHOT.jar
detection-service/target/detection-service-0.0.1-SNAPSHOT.jar
```

---

## 10.4 Step 2: Spin Up Infrastructure & Services

Jalankan Docker Compose dari root project:

```bash
docker compose up --build -d
```

Docker Compose akan:

1. Membuat network `fraudguard-net`.
2. Menjalankan PostgreSQL dengan health check.
3. Menjalankan Redis dengan health check.
4. Menunggu database dan cache siap.
5. Menjalankan Ingestion Service.
6. Menjalankan Detection Service.

> 💡 **Flag `--build`** akan membangun ulang image Docker terlebih dahulu. **Flag `-d`** berarti berjalan di background (detached mode).

---

## 10.5 Step 3: Verify Running Containers

Cek apakah semua container berjalan:

```bash
docker ps
```

Output yang diharapkan:

```text
CONTAINER ID   IMAGE                       STATUS          PORTS
xxxxxxxxxxxx   fraudguard-ingestion       Up 30 seconds   0.0.0.0:7777->7777/tcp
xxxxxxxxxxxx   fraudguard-detection       Up 30 seconds   0.0.0.0:7778->7778/tcp
xxxxxxxxxxxx   postgres:15                Up 35 seconds   0.0.0.0:5432->5432/tcp
xxxxxxxxxxxx   redis:7                    Up 35 seconds   0.0.0.0:6379->6379/tcp
```

Cek log masing-masing service:

```bash
# Ingestion Service
docker logs -f fraudguard-ingestion

# Detection Service
docker logs -f fraudguard-detection

# PostgreSQL
docker logs -f fraudguard-postgres

# Redis
docker logs -f fraudguard-redis
```

---

## 10.6 Step 4: Trigger Ingestion & Detection Flow

Kirim file CSV ke endpoint Ingestion Service menggunakan `curl`:

```bash
curl -X POST \
  http://localhost:7777/api/transactions/upload \
  -H "Content-Type: multipart/form-data" \
  -F "file=@dataset/paysim.csv"
```

---

## 10.7 Step 5: Behind the Scenes

Setelah request di atas dikirim, alur berikut akan terjadi:

1. **Ingestion Service** menerima file CSV.
2. CSV diproses menggunakan **Java Stream API**:
   - Filter baris tidak valid.
   - Mapping ke entity `Transaction`.
3. Data disimpan ke **PostgreSQL** dalam batch.
4. Setiap transaksi yang tersimpan publish ID ke Redis channel `fraudguard.tx.channel`.
5. **Detection Service** subscribe channel tersebut secara asynchronous.
6. Detection Service mengambil data transaksi dari PostgreSQL.
7. Semua **DetectionStrategy** dijalankan oleh Spring IoC engine.
8. Jika terdeteksi fraud, kolom `is_flagged_fraud` diupdate melalui native SQL.
9. Fraud Analyst bisa menjalankan query SQL lanjutan untuk analisis lebih dalam.

---

## 10.8 Step 6: Check Logs (Opsional)

Untuk memastikan Strategy Pattern dan Spring IoC bekerja, perhatikan log Detection Service:

```text
Received transaction id=12345 from Redis
Running strategy: VelocityCheck
Running strategy: BalanceAnomaly
Transaction 12345 flagged as fraud
```

Jika log seperti ini muncul, artinya:

- Redis Pub/Sub berfungsi.
- Spring IoC berhasil menginject strategies.
- Strategy Pattern berjalan.

---

## 10.9 Step 7: Verifikasi Data di Database

Masuk ke PostgreSQL:

```bash
docker exec -it fraudguard-postgres psql -U postgres -d fraudguard
```

Cek jumlah transaksi:

```sql
SELECT COUNT(*) FROM transactions;
```

Cek transaksi yang ditandai fraud:

```sql
SELECT * FROM transactions WHERE is_flagged_fraud = TRUE LIMIT 10;
```

Bandingkan dengan label asli dataset:

```sql
SELECT
    is_fraud,
    is_flagged_fraud,
    COUNT(*)
FROM transactions
GROUP BY is_fraud, is_flagged_fraud;
```

---

## 10.10 Troubleshooting

| Masalah | Kemungkinan Penyebab | Solusi |
|---|---|---|
| `Connection refused` ke PostgreSQL | Service app menyala sebelum DB siap | Pastikan `depends_on` + health check di `docker-compose.yml` |
| Detection Service tidak menerima event | Redis belum siap atau channel salah | Cek log Redis dan konfigurasi channel |
| CSV gagal diupload | Format CSV tidak sesuai | Pastikan header sesuai mapping di kode |
| Data tidak persist saat restart | Tidak ada Docker volume | Tambahkan volume untuk PostgreSQL di `docker-compose.yml` |
| Performa lambat saat insert | Insert satu per satu | Gunakan batch insert, minimal 100-1000 baris per batch |

---

## 10.11 Stop dan Bersihkan

Untuk menghentikan semua container:

```bash
docker compose down
```

Untuk menghentikan dan menghapus volume (data akan hilang):

```bash
docker compose down -v
```

Untuk rebuild dari awal:

```bash
docker compose down -v
docker compose up --build -d
```

---

## 10.12 Next Steps Setelah Berhasil Run

1. Tambahkan detection strategy baru.
2. Eksperimen dengan query SQL lanjutan di [08-SQL-QUERIES.md](./08-SQL-QUERIES.md).
3. Tambahkan unit test dan integration test.
4. Setup CI/CD pipeline untuk build otomatis.
5. Eksplorasi penggantian Redis Pub/Sub dengan Kafka untuk skenario production.

---

## 10.13 Kesimpulan

Dengan mengikuti panduan ini, kamu bisa:

- Menjalankan seluruh stack FraudGuard dalam Docker.
- Mengupload dataset CSV.
- Melihat alur data dari CSV → PostgreSQL → Redis → Detection Service.
- Memverifikasi hasil deteksi langsung di database.

Selamat mencoba! 🚀
