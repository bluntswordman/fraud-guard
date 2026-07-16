# 03 — Non-Functional Requirements (NFR)

**Audience**: Tech Lead, Architect, dan Senior Engineer yang mengevaluasi kualitas sistem secara keseluruhan.

---

## 3.1 Portability (Dapat Dijalankan di Mana Saja)

### Requirement

Seluruh infrastruktur — database, cache, dan aplikasi — harus berjalan dalam container Docker.

### Mengapa Penting?

- Developer tidak perlu menginstal PostgreSQL, Redis, atau Java secara manual di laptop masing-masing.
- Environment di laptop developer, staging, dan produksi menjadi sama persis.
- Deployment ke server baru menjadi lebih mudah dan cepat.

### Cara Pencapaian

- Gunakan `Docker Compose` untuk menjalankan semua service sekaligus.
- Setiap service punya `Dockerfile` sendiri.
- Konfigurasi koneksi database dan Redis diambil dari environment variable.

### Acceptance Criteria

- Baru bergabungnya developer bisa menjalankan sistem hanya dengan `docker compose up`.
- Tidak ada langkah instalasi manual seperti "install PostgreSQL dulu".

---

## 3.2 Extensibility (Mudah Dikembangkan)

### Requirement

Penambahan aturan deteksi baru **tidak boleh** memerlukan perubahan kode pada core engine.

### Mengapa Penting?

- Pola fraud terus berkembang. Aturan baru akan sering ditambahkan.
- Jika setiap tambahan aturan harus mengubah engine, risiko bug dan regression akan tinggi.

### Cara Pencapaian

- Menggunakan **Strategy Pattern** dan **Spring IoC**.
- Core engine hanya tahu interface `DetectionStrategy`.
- Spring secara otomatis mengumpulkan semua implementasi strategy dan menginject-nya ke engine.

### Cara Menambah Aturan Baru

1. Buat class baru yang implementasi `DetectionStrategy`.
2. Beri annotation `@Component("namaStrategyBaru")`.
3. Engine akan otomatis menjalankannya tanpa perubahan kode.

```java
@Component("newFraudRule")
public class NewFraudRuleStrategy implements DetectionStrategy {
    @Override
    public String getName() {
        return "NewFraudRule";
    }

    @Override
    public int detectFraud(Transaction tx) {
        // logic deteksi baru
        return 0; // atau skor fraud
    }
}
```

### Acceptance Criteria

- Menambah 1 aturan baru tidak mengubah file `DetectionEngine.java`.
- Tidak ada perubahan pada unit test yang sudah ada (kecuali test untuk strategy baru).

---

## 3.3 Reliability (Keandalan)

### Requirement

Jika Detection Service down, sistem inti tetap bisa menerima dan menyimpan transaksi.

### Mengapa Penting?

- Transaksi bisnis tidak boleh terhenti karena masalah deteksi.
- Redis Pub/Sub tidak mengharuskan consumer selalu online.

### Acceptance Criteria

- Ingestion Service tetap bisa menerima CSV dan menyimpan ke PostgreSQL meski Detection Service mati.
- Saat Detection Service menyala kembali, dia mulai memproses event baru.

---

## 3.4 Maintainability (Mudah Dirawat)

### Requirement

Kode harus mudah dibaca, dipahami, dan diuji.

### Cara Pencapaian

- Setiap service punya tanggung jawab tunggal.
- Nama class dan method deskriptif.
- Penggunaan interface untuk memudahkan mocking saat test.
- Dokumentasi ini menyertai setiap bagian penting.

---

## 3.5 Observability (Dapat Diawasi)

### Requirement

Sistem harus menyediakan cara untuk memantau kondisinya.

### Cara Pencapaian

- Log yang jelas di setiap service.
- Endpoint health check di Spring Boot Actuator.
- Log alur publish/subscribe Redis agar bisa ditrace.

### Acceptance Criteria

- Developer bisa melihat log "Published txId=123 to Redis" di Ingestion Service.
- Developer bisa melihat log "Received txId=123, running strategies" di Detection Service.

---

## 3.6 Performance Target

| Metrik | Target |
|---|---|
| CSV Processing Speed | > 5,000 rows/sec |
| Inter-Service Communication | < 50 ms |
| Intermediate SQL Execution | < 200 ms pada 1 juta+ baris |

---

## 3.7 Kesimpulan

NFR FraudGuard menekankan pada:

- **Portability** → Docker everywhere.
- **Extensibility** → Strategy Pattern + Spring IoC.
- **Reliability** → isolasi antara ingestion dan detection.
- **Maintainability** → kode bersih dan terdokumentasi.
- **Observability** → log dan health check yang jelas.
