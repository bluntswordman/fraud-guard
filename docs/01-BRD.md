# 01 — Business Requirements Document (BRD)

**Audience**: Product Manager, Tech Lead, dan Engineer yang ingin memahami *mengapa* sistem ini dibangun.

---

## 1.1 Executive Summary

**FraudGuard** adalah sistem deteksi anomali transaksi keuangan secara **real-time**. Sistem ini dibuat untuk mengatasi kelemahan sistem deteksi tradisional yang masih menggunakan **batch processing** dengan latensi tinggi — bisa sampai 24 jam.

Dengan arsitektur **mini-microservices**, proses deteksi dipisahkan dari sistem inti (ERP). Artinya, meskipun proses analisis berat atau service deteksi mengalami masalah, sistem inti tetap berjalan dan transaksi tetap tercatat.

> 💡 **Kenapa ini penting?** Semakin cepat deteksi fraud, semakin kecil kemungkinan uang hilang ke tangan penipu.

---

## 1.2 Business Problems

### 1. High Latency (Deteksi Terlambat)

**Masalah**: Sistem lama memproses data dalam batch, misalnya sekali sehari.

**Dampak**: Ketika transaksi fraud terdeteksi, dana sudah ditransfer ke rekening penipu dan sulit ditarik kembali.

**Analogi sederhana**: Seperti baru mengetahui rumah kebakaran setelah 24 jam — api sudah padam, tapi rumah sudah hangus.

---

### 2. Tight Coupling (Logic Deteksi Menempel di Sistem Utama)

**Masalah**: Logic deteksi fraud berada di dalam sistem ERP utama.

**Dampak**: Jika proses analisis fraud berat atau error, sistem ERP ikut lambat atau bahkan down. Ini berarti seluruh operasi bisnis bisa terganggu.

**Analogi sederhana**: Seperti AC rumah dan kompor gas menggunakan listrik dari satu sekring. Jika AC bermasalah, kompor ikut mati.

---

### 3. Data Inaccuracy (Ketidakakuratan Perhitungan Keuangan)

**Masalah**: Banyak sistem masih menggunakan tipe data floating-point (`float` / `double`) untuk uang.

**Dampak**: Floating-point bisa menghasilkan kesalahan kecil dalam perhitungan, misalnya `0.1 + 0.2 ≠ 0.3` di komputer. Dalam jutaan transaksi, kesalahan kecil ini bisa jadi besar.

**Analogi sederhana**: Seperti timbangan yang selalu sedikit meleset. Untuk transaksi harian mungkin tidak masalah, tapi untuk keuangan skala besar sangat berbahaya.

---

## 1.3 Business Objectives & KPIs

| No | Objective (Tujuan Bisnis) | Target KPI | Measurement Method (Cara Ukur) |
|---|---|---|---|
| 1 | Mempercepat deteksi fraud | **< 1 detik** dari waktu data masuk | Monitoring latensi Redis Pub/Sub |
| 2 | Mengurangi False Positive | **Maksimal 10%** dari total flag | SQL query membandingkan `is_flagged_fraud` vs `is_fraud` |
| 3 | Zero Downtime Sistem Inti | **100% isolasi** | Detection service crash tidak menyeret database utama |
| 4 | Presisi Finansial | **Zero floating-point error** | Menggunakan tipe data PostgreSQL `NUMERIC` |

### Penjelasan Tiap KPI

#### 1. Latency < 1 detik

Artinya dari data transaksi masuk, dalam waktu kurang dari 1 detik sistem sudah bisa menandai apakah transaksi tersebut mencurigakan.

#### 2. False Positive ≤ 10%

**False positive** = transaksi yang sebenarnya normal, tapi sistem menandainya sebagai fraud. Jika terlalu banyak, tim analis akan kewalahan dan user bisa frustrasi.

#### 3. Zero Downtime Sistem Inti

Sistem ERP tidak boleh ikut mati jika service deteksi bermasalah. Ini dicapai dengan memisahkan service-servicenya.

#### 4. Presisi Finansial

Semua perhitungan uang harus akurat tanpa kesalahan desimal. PostgreSQL `NUMERIC` menyimpan angka sebagai nilai eksak, bukan perkiraan.

---

## 1.4 Success Criteria

Proyek dianggap sukses jika:

1. Kedua service (Ingestion & Detection) bisa berjalan secara mandiri dalam Docker.
2. CSV dengan ribuan baris bisa diproses dan disimpan ke PostgreSQL.
3. Detection service bisa menerima event dan menandai transaksi fraud.
4. Tidak ada penggunaan tipe data floating-point untuk kolom uang.
5. Sistem inti tetap bisa menerima transaksi meski detection service sedang restart.

---

## 1.5 Kesimpulan

FraudGuard dirancang untuk:

- **Mencegah kerugian finansial** dengan deteksi lebih cepat.
- **Menjaga stabilitas sistem inti** dengan memisahkan proses deteksi.
- **Menjamin akurasi keuangan** dengan tipe data yang tepat.

Dokumen selanjutnya akan menjelaskan *apa yang harus dibangun* (PRD) dan *bagaimana cara membangunnya* (TDD / Arsitektur).
