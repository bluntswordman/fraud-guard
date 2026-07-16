# 06 — Component Design Patterns

**Audience**: Engineer yang ingin memahami pola desain yang digunakan di FraudGuard dan cara kerjanya di dalam kode.

---

## 6.1 Spring IoC + Strategy Pattern

### Apa Itu Strategy Pattern?

Strategy Pattern adalah pola desain di mana kita mendefinisikan satu **interface**, lalu membuat banyak **implementasi** yang berbeda. Kode utama (engine) hanya tahu interface-nya, tidak perlu tahu implementasi spesifik.

### Kenapa Cocok untuk Fraud Detection?

Karena aturan deteksi fraud bisa banyak dan terus bertambah. Setiap aturan adalah satu "strategy".

### Interface DetectionStrategy

```java
public interface DetectionStrategy {
    String getName();
    boolean detectFraud(Transaction tx);
}
```

Penjelasan method:

- `getName()` — mengembalikan nama strategy untuk logging dan identifikasi.
- `detectFraud(Transaction tx)` — menerima transaksi dan mengembalikan `true` jika fraud terdeteksi.

### Contoh Implementasi Strategy

```java
@Component("velocityCheck")
public class VelocityStrategy implements DetectionStrategy {
    private final TransactionRepository repository;

    @Override
    public String getName() {
        return "VelocityCheck";
    }

    @Override
    public boolean detectFraud(Transaction tx) {
        // Logic: deteksi lonjakan jumlah transaksi dalam waktu singkat
        // Query: countByNameOrigAndTypeInAndStepBetweenAndTenantId
        return false; // atau true jika fraud
    }
}
```

```java
@Component("balanceAnomaly")
public class BalanceAnomalyStrategy implements DetectionStrategy {

    @Override
    public String getName() {
        return "BalanceAnomaly";
    }

    @Override
    public boolean detectFraud(Transaction tx) {
        // Logic: deteksi perubahan saldo yang tidak wajar
        return false; // atau true jika fraud
    }
}
```

```java
@Component("lifecycleCheck")
public class LifecycleStrategy implements DetectionStrategy {
    private final TransactionRepository repository;

    // State machine: TOPUP → REDEEM → EARN
    private static final Map<String, String> REQUIRED_PRIOR = Map.of(
        "REDEEM", "TOPUP",
        "EARN", "REDEEM"
    );

    @Override
    public String getName() {
        return "LifecycleCheck";
    }

    @Override
    public boolean detectFraud(Transaction tx) {
        // Query: existsByNameOrigAndTypeAndStepLessThanAndTenantId
        // Cek apakah user punya transaksi TOPUP sebelum REDEEM,
        // atau REDEEM sebelum EARN
        return false; // atau true jika lifecycle dilanggar
    }
}
```

### Engine yang Menerima Semua Strategy

```java
@Service
public class DetectionEngine {

    // Spring secara otomatis mengumpulkan semua bean
    // yang implementasi DetectionStrategy
    private final List<DetectionStrategy> strategies;

    public DetectionEngine(List<DetectionStrategy> strategies) {
        this.strategies = strategies;
    }

    public DetectionResult analyze(Transaction tx) {
        return strategies.stream()
            .map(strategy -> {
                boolean isFraud = strategy.detectFraud(tx);
                System.out.println("Running " + strategy.getName() + " -> " + (isFraud ? "FRAUD" : "CLEAN"));
                return isFraud;
            })
            .collect(Collectors.toList());
        // ... kombinasi hasil untuk keputusan akhir
    }
}
```

> 💡 **Kenapa ini powerful?** Engine tidak pernah diubah ketika menambah aturan baru. Spring IoC yang mengatur.

---

## 6.2 Java Stream API

### Apa Itu Java Stream API?

Stream API adalah fitur Java yang memungkinkan kita memproses koleksi data secara deklaratif, mirip SQL tapi di dalam kode Java.

### Penggunaan di Ingestion Service

Stream digunakan untuk:

- **Mapping** — mengubah objek CSV mentah menjadi entity database.
- **Filtering** — membuang data tidak valid (misal amount <= 0).
- **Grouping & Aggregation** — menghitung statistik per tipe transaksi.

### Contoh Kode Transformasi & Agregasi

```java
Map<String, DoubleSummaryStatistics> statsByType = rawData.stream()
    .filter(tx -> tx.getAmount() > 0)          // hanya ambil amount positif
    .map(this::mapToEntity)                    // ubah ke entity Transaction
    .collect(Collectors.groupingBy(            // kelompokkan berdasarkan tipe
        Transaction::getType,
        Collectors.summarizingDouble(Transaction::getAmount)
    ));
```

### Penjelasan Per Method

| Method | Fungsi |
|---|---|
| `.stream()` | Mengubah collection menjadi stream. |
| `.filter(...)` | Memilih hanya elemen yang memenuhi kondisi. |
| `.map(...)` | Mengubah setiap elemen menjadi bentuk lain. |
| `.collect(...)` | Mengumpulkan hasil menjadi struktur data baru. |
| `groupingBy(...)` | Mengelompokkan elemen berdasarkan key. |
| `summarizingDouble(...)` | Menghitung count, sum, min, max, average. |

### Keuntungan Stream API

- Kode lebih ringkas dan mudah dibaca.
- Mudah diparalelkan dengan `.parallelStream()` jika data sangat besar.
- Fungsional dan deklaratif — fokus pada "apa" yang ingin dilakukan, bukan "bagaimana".

---

## 6.3 Cara Menambah Aturan Deteksi Baru

Ikuti langkah berikut untuk menambah aturan baru tanpa mengubah engine:

### Step 1: Buat Class Baru

```java
@Component("unusualHour")
public class UnusualHourStrategy implements DetectionStrategy {

    @Override
    public String getName() {
        return "UnusualHour";
    }

    @Override
    public boolean detectFraud(Transaction tx) {
        // Contoh: transaksi di luar jam kerja dinilai lebih mencurigakan
        int hour = tx.getStep() % 24;
        return (hour < 6 || hour > 23);
    }
}
```

### Step 2: Build Ulang

```bash
mvn clean package
```

### Step 3: Restart Detection Service

```bash
docker compose restart detection-service
```

### Step 4: Verifikasi

Lihat log Detection Service. Engine akan otomatis menjalankan strategy baru.

```text
Running VelocityCheck -> CLEAN
Running BalanceAnomaly -> FRAUD
Running LifecycleCheck -> CLEAN
```

---

## 6.4 Kesimpulan

| Pattern | Manfaat |
|---|---|
| **Strategy Pattern + IoC** | Aturan deteksi mudah ditambah tanpa mengubah core engine. |
| **Java Stream API** | Transformasi data jadi ringkas, mudah dibaca, dan bisa diparalelkan. |

Dengan kombinasi kedua pattern ini, FraudGuard menjadi fleksibel dan mudah dikembangkan.
