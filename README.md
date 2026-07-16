# FraudGuard

Real-time fraud detection platform dengan arsitektur mini-microservices. Sistem menerima transaksi via CSV batch atau REST JSON, menyimpannya ke PostgreSQL, lalu mendeteksi anomali secara otomatis melalui Redis Pub/Sub. Hasil deteksi dikirim balik via **Redis result channel** (internal) atau **webhook HTTP** (eksternal).

## Cara Menjalankan

```bash
docker compose up --build -d
```

Service yang berjalan:
- **Ingestion Service**: port `7777` (menerima CSV + REST JSON + statistik)
- **Detection Service**: port `7778` (subscriber Redis + webhook management)
- **PostgreSQL 15**: port `5433` di host
- **Redis 7**: port `6380` di host

## Alur Data

```
POST /api/transactions  →  Ingestion Service (7777)  →  PostgreSQL
                                      ↓ publish txId
                                    Redis Pub/Sub
                                  channel: fraudguard.tx.channel
                                      ↓ subscribe
                             Detection Service (7778)
                                      ↓
                             jalankan 3 strategy:
                             VelocityCheck + BalanceAnomaly + LifecycleCheck
                                      ↓
                             hasil deteksi dikirim via:
                           ┌──────────┴──────────┐
                           │                      │
                    Redis result channel    Webhook HTTP POST
                  (internal services)     (external systems)
```

## API Endpoints

### Ingestion Service — Port 7777

| Method | Endpoint | Fungsi |
|---|---|---|
| `POST` | `/api/transactions/upload` | Upload CSV batch |
| `POST` | `/api/transactions` | Kirim 1 transaksi real-time (JSON) |
| `GET` | `/api/transactions/{id}` | Cek status fraud per transaksi |
| `GET` | `/api/transactions/stats?tenantId=` | Statistik + fraud breakdown |
| `GET` | `/api/transactions/evaluation?tenantId=` | Akurasi model (precision, recall, F1) |

### Detection Service — Port 7778

| Method | Endpoint | Fungsi |
|---|---|---|
| `POST` | `/api/webhooks` | Register webhook per tenant |
| `GET` | `/api/webhooks?tenantId=` | List webhook |
| `DELETE` | `/api/webhooks/{id}` | Hapus webhook |

### 1. Upload CSV
```
POST /api/transactions/upload
Content-Type: multipart/form-data
Parameter: file (CSV)
```
Response: `{ totalRows, processedRows, skippedRows, publishedEvents }`

### 2. Kirim Transaksi JSON
```
POST /api/transactions
Content-Type: application/json

{
  "step": 1,             // unit waktu (opsional)
  "type": "REDEEM",      // TOPUP | REDEEM | EARN | TRANSFER | CASH_OUT | PAYMENT
  "amount": 250000.00,   // > 0 (wajib)
  "nameOrig": "USER_A",  // akun pengirim (wajib)
  "nameDest": "PRODUCT", // akun penerima (wajib)
  "tenantId": "MEMBERSHIP", // identitas tenant (WAJIB)
  "rules": ["lifecycleCheck"] // opsional — kosong/null = semua strategy
}
```
> ⚠️ Response selalu `200 success`. Deteksi berjalan **async** via Redis — hasilnya dicek lewat `GET /api/transactions/{id}` atau stats.

### 3. Cek Status Fraud per Transaksi
```
GET /api/transactions/{id}
```
Response:
```json
{
  "data": {
    "id": 5, "type": "REDEEM", "amount": 100000,
    "nameOrig": "HACKER_1", "nameDest": "PRODUCT",
    "tenantId": "MEMBERSHIP",
    "isFraud": false,           // ground truth (dari dataset)
    "isFlaggedFraud": true      // hasil deteksi sistem ← yang dicek
  }
}
```

### 4. Statistik per Tenant
```
GET /api/transactions/stats                          // semua tenant
GET /api/transactions/stats?tenantId=MEMBERSHIP      // per tenant
```
Response:
```json
{
  "data": {
    "totalTransactions": 4,
    "totalFlaggedFraud": 1,     // jumlah yang kena flag
    "totalClean": 3,            // jumlah yang bersih
    "totalAmount": 855000.00,
    "transactionsByType": { "TOPUP": 1, "REDEEM": 2, "EARN": 1 },
    "flaggedFraudByType": { "REDEEM": 1 }
  }
}
```

### 5. Evaluasi Akurasi per Tenant
```
GET /api/transactions/evaluation?tenantId=MEMBERSHIP
```
Membandingkan `isFraud` (ground truth) vs `isFlaggedFraud` (hasil deteksi).
Response: `{ truePositives, falsePositives, trueNegatives, falseNegatives, precision, recall, f1Score, accuracy }`

## Aturan Deteksi

| Strategy | Scope | Aturan |
|---|---|---|
| **VelocityCheck** | `TRANSFER`, `CASH_OUT` | > 3 transaksi per akun dalam 2 step → fraud |
| **BalanceAnomaly** | `TRANSFER`, `CASH_OUT` | Saldo habis total ATAU saldo nol tapi transfer > 500rb → fraud |
| **LifecycleCheck** | `TOPUP`, `REDEEM`, `EARN` | REDEEM tanpa TOPUP duluan ATAU EARN tanpa REDEEM duluan → fraud |

## Hasil Deteksi (Dual Output)

Setelah selesai analisis, Detection Service mengirim hasil via dua jalur:

### Redis Result Channel (Internal)
```
redis-cli -p 6380 SUBSCRIBE fraudguard.result.channel
```
Payload:
```json
{
  "txId": 12345, "tenantId": "MEMBERSHIP",
  "isFlaggedFraud": true,
  "triggeredRules": ["LifecycleCheck"],
  "evaluatedAt": "2026-07-16T14:32:41Z"
}
```
Cocok untuk: service internal satu network (ERP, membership backend).

### Webhook HTTP (External)
Register URL callback:
```
POST /api/webhooks
{ "tenantId": "MEMBERSHIP", "url": "https://api.example.com/webhook", "secret": "sec-123" }
```
Setiap selesai deteksi, FraudGuard akan `POST` payload JSON ke URL terdaftar.
- **HTTPS only** — HTTP ditolak (SSRF protection)
- **Async** (`@Async`) — tidak blocking detection pipeline
- **Disabled by default** — set `fraudguard.webhook.enabled=true` untuk mengaktifkan

Cocok untuk: sistem eksternal pihak ketiga yang tidak bisa akses Redis.

## Mode Training vs Production

- **Training**: isi `isFraud` (ground truth) saat kirim data. Gunakan `/evaluation` untuk hitung akurasi.
- **Production**: `isFraud` tidak perlu diisi. Gunakan stats untuk pantau flaggedFraud per tenant.

## Multi-Tenant

Setiap transaksi memiliki `tenantId`. Query stats/evaluation bisa difilter per tenant via `?tenantId=`.
Data tenant A tidak bocor ke tenant B — semua query discope `tenantId` di repository layer.

## Outbox & Recovery

- Saat detection service **down**, transaksi tetap tersimpan dan dipublish ke Redis.
- Tabel `pending_detections` mencatat semua transaksi yang belum diproses.
- Setiap 30 detik, **RecoveryJob** memproses ulang transaksi PENDING + publish hasil ke result channel.

## Struktur Database

| Tabel | Fungsi |
|---|---|
| `transactions` | Data transaksi (tenantId, amount NUMERIC(18,2), BOOLEAN native) |
| `pending_detections` | Outbox — transaksi yang menunggu diproses detection |
| `webhook_configs` | URL webhook per tenant + secret key |

## Strategi Deteksi Selektif (`rules`)

Via REST JSON, bisa memilih rule yang dijalankan:
```json
{ "rules": ["lifecycleCheck"] }
```
Array kosong/null = semua strategy.

## Menambah Aturan Baru

1. Buat class implements `DetectionStrategy` + `@Component("namaStrategy")`
2. Restart Detection Service
3. Engine otomatis menjalankan strategy baru tanpa ubah kode engine

## Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Java 17 |
| Framework | Spring Boot 4.1.0 |
| Database | PostgreSQL 15 (`NUMERIC(18,2)`, `BOOLEAN`) |
| Message Broker | Redis 7 Pub/Sub |
| Build | Maven wrapper (`./mvnw`) |
| Container | Docker Compose |
