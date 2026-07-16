# 11 — API Specification: Ingestion & Detection Service

**Audience**: Engineer, QA, Frontend/Integrator, dan siapa saja yang perlu mengonsumsi API FraudGuard.

---

## 11.1 Base URLs

```text
Ingestion Service:  http://localhost:7777
Detection Service:  http://localhost:7778
```

Di environment Docker Compose, service ini di-expose ke port `7777` dan `7778`.

---

## 11.2 Endpoint Overview

| Method | Endpoint | Service | Fungsi |
|---|---|---|---|
| `POST` | `/api/transactions/upload` | Ingestion | Upload file CSV transaksi |
| `POST` | `/api/transactions` | Ingestion | Ingest satu transaksi via JSON |
| `GET` | `/api/transactions/{id}` | Ingestion | Cek detail + status fraud per transaksi |
| `GET` | `/api/transactions/stats?tenantId=` | Ingestion | Statistik + fraud breakdown |
| `GET` | `/api/transactions/evaluation?tenantId=` | Ingestion | Metrik akurasi deteksi |
| `POST` | `/api/webhooks` | Detection | Register webhook URL per tenant |
| `GET` | `/api/webhooks?tenantId=` | Detection | List registered webhooks |
| `DELETE` | `/api/webhooks/{id}` | Detection | Remove a webhook |

---

## 11.3 POST /api/transactions/upload

### Deskripsi

Menerima file CSV berisi data transaksi, memprosesnya dengan Java Stream API, menyimpannya ke PostgreSQL, dan mempublish ID transaksi ke Redis Pub/Sub.

### Request

- **Content-Type**: `multipart/form-data`
- **Parameter**: `file` (file CSV)

#### Contoh Request (curl)

```bash
curl -X POST http://localhost:7777/api/transactions/upload \
  -H "Content-Type: multipart/form-data" \
  -F "file=@dataset/paysim.csv"
```

### Response Sukses — `200 OK`

```json
{
  "status": "success",
  "message": "CSV processed successfully",
  "data": {
    "fileName": "paysim.csv",
    "totalRows": 50000,
    "processedRows": 49890,
    "skippedRows": 110,
    "publishedEvents": 49890
  }
}
```

#### Penjelasan Field

| Field | Tipe | Keterangan |
|---|---|---|
| `status` | string | Status umum request. |
| `message` | string | Pesan singkat. |
| `fileName` | string | Nama file yang diupload. |
| `totalRows` | integer | Total baris di CSV (termasuk header). |
| `processedRows` | integer | Baris yang berhasil diproses dan disimpan. |
| `skippedRows` | integer | Baris yang dilewati karena tidak valid. |
| `publishedEvents` | integer | Jumlah event yang dipublish ke Redis. |

### Response Error — `400 Bad Request`

```json
{
  "status": "error",
  "message": "Invalid file format. Please upload a CSV file.",
  "errorCode": "ING-001"
}
```

### Response Error — `500 Internal Server Error`

```json
{
  "status": "error",
  "message": "Failed to process CSV. Please check the logs.",
  "errorCode": "ING-999"
}
```

---

## 11.4 POST /api/transactions

### Deskripsi

Menerima satu transaksi via JSON request body. Cocok untuk integrasi real-time dari sistem eksternal (ERP, membership, payment gateway, dll).

### Request

- **Content-Type**: `application/json`

```json
{
  "step": 1,
  "type": "TRANSFER",
  "amount": 5000000.00,
  "nameOrig": "C123",
  "nameDest": "M456",
  "oldBalanceOrg": 5100000.00,
  "newBalanceOrig": 100000.00,
  "isFraud": false,
  "tenantId": "ERP_BANK_X",
  "rules": ["velocityCheck", "balanceAnomaly"]
}
```

| Field | Tipe | Wajib | Keterangan |
|---|---|---|---|
| `step` | int | Tidak | Unit waktu (default 0). |
| `type` | string | **Ya** | Jenis: `TRANSFER`, `CASH_OUT`, `PAYMENT`, `CASH_IN`, `DEBIT`. |
| `amount` | decimal | **Ya** | Nilai transaksi (> 0). |
| `nameOrig` | string | **Ya** | ID akun pengirim. |
| `nameDest` | string | **Ya** | ID akun penerima. |
| `oldBalanceOrg` | decimal | Tidak | Saldo sebelum transaksi. |
| `newBalanceOrig` | decimal | Tidak | Saldo setelah transaksi. |
| `isFraud` | boolean | Tidak | Label fraud dari sistem asal (default false). |
| `tenantId` | string | **Ya** | Identitas ERP/vendor pengirim. |
| `rules` | string[] | Tidak | Strategy spesifik yang dijalankan. Kosong/null = semua strategy. |

### Response Sukses — `200 OK`

```json
{
  "status": "success",
  "message": "Transaction ingested successfully",
  "data": {
    "fileName": "single",
    "totalRows": 1,
    "processedRows": 1,
    "skippedRows": 0,
    "publishedEvents": 1
  }
}
```

### Response Error — `400 Bad Request`

```json
{
  "status": "error",
  "message": "tenantId is required",
  "errorCode": "ING-006"
}
```

---

## 11.5 GET /api/transactions/{id}

### Deskripsi

Mengembalikan detail transaksi berdasarkan ID, termasuk status deteksi fraud (`isFlaggedFraud`). Berguna untuk mengecek hasil deteksi setelah transaksi di-ingest (deteksi berjalan async via Redis).

### Response Sukses — `200 OK`

```json
{
  "status": "success",
  "data": {
    "id": 5,
    "step": 1,
    "type": "REDEEM",
    "amount": 100000.00,
    "nameOrig": "HACKER_1",
    "nameDest": "PRODUCT",
    "oldBalanceOrg": 0,
    "newBalanceOrig": 0,
    "tenantId": "MEMBERSHIP",
    "isFraud": false,
    "isFlaggedFraud": true
  }
}
```

| Field | Keterangan |
|---|---|
| `isFraud` | Ground truth label dari dataset (jika ada). |
| `isFlaggedFraud` | Hasil deteksi sistem — `true` jika terdeteksi fraud. |

### Response Not Found — `404`

Jika ID tidak ditemukan.

---

## 11.6 GET /api/transactions/stats

### Deskripsi

Mengembalikan statistik dasar dari transaksi yang sudah diproses, termasuk fraud breakdown. Bisa difilter per tenant via query parameter `?tenantId=`.

### Query Parameter

| Parameter | Wajib | Keterangan |
|---|---|---|
| `tenantId` | Tidak | Filter statistik untuk satu tenant. Kosong = semua tenant. |

### Response Sukses — `200 OK`

```json
{
  "status": "success",
  "data": {
    "totalTransactions": 4,
    "totalFlaggedFraud": 1,
    "totalClean": 3,
    "totalAmount": 855000.00,
    "transactionsByType": {
      "TOPUP": 1,
      "REDEEM": 2,
      "EARN": 1
    },
    "flaggedFraudByType": {
      "REDEEM": 1
    }
  }
}
```

| Field | Keterangan |
|---|---|
| `totalTransactions` | Total semua transaksi. |
| `totalFlaggedFraud` | Jumlah transaksi yang di-flag fraud oleh sistem. |
| `totalClean` | `totalTransactions - totalFlaggedFraud`. |
| `totalAmount` | Total nominal semua transaksi. |
| `transactionsByType` | Jumlah transaksi per tipe. |
| `flaggedFraudByType` | Jumlah fraud per tipe transaksi. |

---

## 11.7 GET /api/transactions/evaluation

### Deskripsi

Mengembalikan metrik evaluasi model deteksi fraud (accuracy, precision, recall, F1 score) berdasarkan ground truth label `isFraud` vs hasil deteksi `isFlaggedFraud`. Bisa difilter per tenant via `?tenantId=`.

### Query Parameter

| Parameter | Wajib | Keterangan |
|---|---|---|
| `tenantId` | Tidak | Filter evaluasi untuk satu tenant. Kosong = semua tenant. |

### Response Sukses — `200 OK`

```json
{
  "status": "success",
  "data": {
    "truePositives": 120,
    "falsePositives": 15,
    "trueNegatives": 49700,
    "falseNegatives": 55,
    "totalGroundTruth": 175,
    "precision": 0.8889,
    "recall": 0.6857,
    "f1Score": 0.7742,
    "accuracy": 0.9986
  }
}
```

---

## 11.8 POST /api/webhooks

### Deskripsi

Register webhook URL per tenant. Setelah Detection Service selesai menganalisis transaksi, FraudGuard akan mengirim HTTP POST ke URL yang terdaftar.

### Request

- **Content-Type**: `application/json`

```json
{
  "tenantId": "ERP_BANK_X",
  "url": "https://example.com/webhook",
  "secret": "my-secret-key"
}
```

| Field | Tipe | Wajib | Keterangan |
|---|---|---|---|
| `tenantId` | string | **Ya** | ID tenant yang mendaftarkan webhook. |
| `url` | string | **Ya** | URL tujuan. **Harus HTTPS**, tidak boleh private IP (10.x, 192.168.x, 172.16.x, 127.x). |
| `secret` | string | Tidak | Secret key untuk verifikasi (dikirim via header `X-Webhook-Secret`). |

### Response Sukses — `201 Created`

```json
{
  "id": 1,
  "tenantId": "ERP_BANK_X",
  "url": "https://example.com/webhook",
  "enabled": true,
  "createdAt": "2026-07-16T13:30:00"
}
```

### Webhook Payload yang Dikirim

```json
{
  "txId": 12345,
  "tenantId": "ERP_BANK_X",
  "isFlaggedFraud": true,
  "triggeredRules": ["FraudCheck"],
  "evaluatedAt": "2026-07-16T13:30:00Z"
}
```

---

## 11.9 GET /api/webhooks

### Deskripsi

Melihat daftar webhook yang sudah didaftarkan untuk sebuah tenant.

### Query Parameter

| Parameter | Wajib | Keterangan |
|---|---|---|
| `tenantId` | **Ya** | Filter berdasarkan tenant ID. |

### Response Sukses — `200 OK`

```json
[
  {
    "id": 1,
    "tenantId": "ERP_BANK_X",
    "url": "https://example.com/webhook",
    "enabled": true,
    "createdAt": "2026-07-16T13:30:00"
  }
]
```

---

## 11.10 DELETE /api/webhooks/{id}

### Deskripsi

Menghapus webhook yang sudah terdaftar.

### Path Parameter

| Parameter | Keterangan |
|---|---|
| `id` | ID webhook yang akan dihapus. |

### Response Sukses — `204 No Content`

---

## 11.11 GET /actuator/health

### Deskripsi

Health check bawaan Spring Boot Actuator. Digunakan oleh Docker Compose atau monitoring tool untuk memastikan service siap.

### Response Sukses — `200 OK`

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

---

## 11.12 Format CSV yang Diharapkan

Ingestion Service mengharapkan file CSV dengan header berikut (contoh dataset PaySim):

```csv
step,type,amount,nameOrig,nameDest,oldbalanceOrg,newbalanceOrig,isFraud,isFlaggedFraud
1,PAYMENT,9839.64,C1231006815,M1979787155,170136.0,160296.36,0,0
1,PAYMENT,1864.28,C1669944298,M2044282225,21249.0,19384.72,0,0
1,TRANSFER,181.0,C1305486145,C553264065,181.0,0.0,1,0
```

### Mapping Kolom

| CSV Header | Kolom DB | Tipe | Keterangan |
|---|---|---|---|
| `step` | `step` | INT | Unit waktu. |
| `type` | `type` | VARCHAR(20) | Jenis transaksi. |
| `amount` | `amount` | NUMERIC(18,2) | Jumlah uang. |
| `nameOrig` | `name_orig` | VARCHAR(50) | Akun pengirim. |
| `nameDest` | `name_dest` | VARCHAR(50) | Akun penerima. |
| `oldbalanceOrg` | `old_balance_orig` | NUMERIC(18,2) | Saldo sebelum. |
| `newbalanceOrig` | `new_balance_orig` | NUMERIC(18,2) | Saldo sesudah. |
| `isFraud` | `is_fraud` | BOOLEAN | Label asli. |
| `isFlaggedFraud` | `is_flagged_fraud` | BOOLEAN | Flag hasil deteksi. |

---

## 11.13 Error Codes

| Kode | Deskripsi | Penanganan |
|---|---|---|
| `ING-001` | File bukan CSV | Pastikan file berekstensi `.csv`. |
| `ING-002` | CSV kosong atau tidak memiliki header | Periksa isi file. |
| `ING-003` | Mapping kolom gagal | Pastikan header sesuai format yang diharapkan. |
| `ING-004` | Gagal publish ke Redis | Periksa koneksi Redis. |
| `ING-999` | Error internal | Cek log Ingestion Service. |

---

## 11.14 Sequence Diagram: Upload CSV → Publish Event

```text
Data Engineer          Ingestion Service          PostgreSQL          Redis          Detection Service
     │                        │                       │                │                       │
     │  POST /api/transactions/upload                │                │                       │
     │───────────────────────▶│                       │                │                       │
     │                        │                       │                │                       │
     │                        │  parse CSV with Stream│                │                       │
     │                        │──────────────────────▶│                │                       │
     │                        │  INSERT batch         │                │                       │
     │                        │──────────────────────▶│                │                       │
     │                        │  OK                   │                │                       │
     │                        │◀──────────────────────│                │                       │
     │                        │                       │                │                       │
     │                        │  PUBLISH txId         │                │                       │
     │                        │──────────────────────────────────────▶│                       │
     │                        │                       │                │  SUBSCRIBE txId         │
     │                        │                       │                │──────────────────────▶│
     │  200 OK response       │                       │                │                       │
     │◀───────────────────────│                       │                │                       │
```

---

## 11.15 Redis Result Channel (Callback)

### Deskripsi

Setelah Detection Service selesai menganalisis transaksi, hasil deteksi dipublish ke Redis Pub/Sub channel `fraudguard.result.channel`. Sistem eksternal (ERP, membership, payment gateway) bisa subscribe channel ini untuk menerima hasil deteksi secara real-time.

### Cara Subscribe

```bash
# Via redis-cli
redis-cli SUBSCRIBE fraudguard.result.channel
```

### Payload Format

Setiap pesan adalah JSON string:

```json
{
  "txId": 12345,
  "tenantId": "ERP_BANK_X",
  "isFlaggedFraud": true,
  "triggeredRules": ["VelocityCheck", "BalanceAnomaly"],
  "evaluatedAt": "2026-07-16T13:30:00Z"
}
```

| Field | Tipe | Keterangan |
|---|---|---|
| `txId` | long | ID transaksi yang dianalisis. |
| `tenantId` | string | ID tenant pengirim (untuk routing di sisi consumer). |
| `isFlaggedFraud` | boolean | `true` jika fraud terdeteksi, `false` jika bersih. |
| `triggeredRules` | string[] | Nama strategy yang mendeteksi fraud (kosong jika bersih). |
| `evaluatedAt` | ISO-8601 | Timestamp saat evaluasi selesai. |

### Alur Lengkap (dengan callback)

```text
ERP/Membership → POST /api/transactions → Ingestion Svc → PostgreSQL → Redis (tx.channel)
                                                                    ↓
                                                Detection Svc ← Redis (tx.channel)
                                                      ↓
                                                Analyze (Strategies)
                                                      ↓
                                                UPDATE is_flagged_fraud
                                                      ↓
                                                Publish result ke Redis (result.channel)
                                                      ↓
ERP/Membership ← Redis (result.channel) ← { txId, tenantId, isFlaggedFraud, triggeredRules }
```

### Catatan

- Channel name: `fraudguard.result.channel`
- Bisa dikonfigurasi via property `fraudguard.redis.result-channel`
- Jika Redis disabled (`fraudguard.redis.enabled=false`), result channel tidak tersedia
- Consumer harus handle kemungkinan pesan duplikat (idempotent via `txId`)

---

## 11.16 Kesimpulan

API FraudGuard menyediakan:

**Ingestion:**
- `POST /api/transactions/upload` — upload CSV batch.
- `POST /api/transactions` — ingest satu transaksi via JSON.
- `GET /api/transactions/{id}` — cek detail + status fraud per transaksi.
- `GET /api/transactions/stats` — statistik + fraud breakdown per tenant.
- `GET /api/transactions/evaluation` — metrik akurasi per tenant.

**Webhook (Registration):**
- `POST /api/webhooks` — register URL callback per tenant.
- `GET /api/webhooks?tenantId=` — list webhook.
- `DELETE /api/webhooks/{id}` — hapus webhook.

**Callback (Detection → Eksternal):**
- Subscribe Redis channel `fraudguard.result.channel` untuk menerima hasil deteksi real-time.
- Atau daftarkan webhook URL untuk terima HTTP POST (async, SSRF-safe).
- Payload: `{ txId, tenantId, isFlaggedFraud, triggeredRules, evaluatedAt }`.

Untuk alur lengkap setelah API dipanggil, lihat [10-SETUP-GUIDE.md](./10-SETUP-GUIDE.md).
