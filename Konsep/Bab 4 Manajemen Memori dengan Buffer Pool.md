# Bab 4: Manajemen Memori dengan Buffer Pool

## Pendahuluan: Menjembatani Jurang Kecepatan dan Peran Kritis Buffer Pool

Selamat datang kembali di perjalanan membangun Wheel DB. Pada bab sebelumnya, kita telah berhasil membangun `StorageEngine`, sebuah komponen fundamental yang memberi kita kemampuan untuk membaca dan menulis blok data mentah—yang akan kita sebut `Page`—dari dan ke media penyimpanan permanen `Disk` . `StorageEngine` adalah fondasi kita, gerbang menuju persistensi data. Namun, jika kita membangun seluruh database hanya di atas `StorageEngine`, sistem kita akan sangat lambat, terbelenggu oleh kecepatan mekanis atau latensi elektronik dari disk.

Tujuan dari bab ini adalah untuk membangun komponen yang bisa dibilang paling krusial untuk performa sebuah sistem database: `BufferPoolManager`. Komponen ini bertindak sebagai jembatan cerdas yang berdiri di antara kecepatan pemrosesan CPU yang luar biasa tinggi dan kecepatan I/O disk yang relatif lambat. Ia adalah lapisan abstraksi di atas `StorageEngine` yang dirancang dengan satu tujuan utama: meminimalkan interaksi yang mahal dengan disk sebanyak mungkin.

Untuk mencapai ini, kita akan menyelami konsep-konsep inti dalam manajemen memori database. Kita akan mulai dengan memahami _mengapa_ kita sangat perlu menghindari akses disk, dengan menjelajahi jurang performa yang menganga antara RAM dan disk. Selanjutnya, kita akan mengimplementasikan sebuah _cache_ untuk halaman-halaman disk, yang dikenal sebagai `Buffer Pool`. Kita akan merancang mekanisme untuk mengambil halaman, baik yang sudah ada di _cache_ maupun yang harus diambil dari disk. Ketika _cache_ penuh, kita memerlukan strategi cerdas untuk memutuskan halaman mana yang harus "diusir", yang membawa kita pada implementasi _page replacement policy_ seperti _Least Recently Used (LRU)_. Terakhir, kita akan menangani bagaimana perubahan data di memori (yang menghasilkan _dirty pages_) dikelola dan ditulis kembali ke disk secara efisien untuk memastikan data tetap persisten.

Memahami dan mengimplementasikan `BufferPoolManager` secara benar adalah pembeda antara database mainan yang hanya berfungsi secara teoretis dan sistem yang andal serta berkinerja tinggi di dunia nyata. Mari kita mulai membangun mesin performa untuk Wheel DB.

---

## 4.1 Peran dan Pentingnya Buffer Pool

Sebelum kita menulis satu baris kode pun, sangat penting untuk memahami "mengapa" di balik `Buffer Pool`. Jawabannya terletak pada satu fakta fundamental dalam arsitektur komputer: ada perbedaan kecepatan yang luar biasa besar antara mengakses data dari memori utama (RAM) dan dari penyimpanan permanen (disk).

### 4.1.1 Kecepatan Adalah Segalanya: Perbandingan Kinerja Memori vs. Disk

Mengakses data dari RAM secara fundamental jauh lebih cepat daripada mengaksesnya dari penyimpanan persisten seperti Solid-State Drive (SSD), dan perbedaannya menjadi lebih dramatis jika dibandingkan dengan Hard Disk Drive (HDD) [1]. Perbedaan ini bukan sekadar beberapa persen lebih cepat, melainkan dalam hitungan _orde besaran_ (_orders of magnitude_).

Untuk memberikan intuisi yang kuat tentang skala perbedaan ini, kita bisa menggunakan sebuah analogi. Bayangkan Anda sedang melakukan suatu pekerjaan:

- Mengakses data dari **CPU Cache** (L1/L2) setara dengan mengambil alat yang sudah ada di tangan Anda. Prosesnya instan.
- Mengakses data dari **RAM** setara dengan mengambil alat dari meja di sebelah Anda. Cepat, hanya butuh sepersekian detik.
- Mengakses data dari **NVMe SSD** setara dengan harus berjalan ke ruangan lain untuk mengambil alat. Proses ini memakan waktu yang jauh lebih lama.
- Mengakses data dari **HDD** setara dengan harus pergi ke gedung lain untuk mengambil alat tersebut. Latensinya sangat besar.

Analogi ini secara efektif mengkomunikasikan skala latensi yang terlibat dalam setiap lapisan hirarki memori [2].

**Membedah Metrik Kinerja: _Latency_ dan _Throughput_**

Untuk memahami performa secara teknis, kita perlu membedakan dua metrik utama: _latency_ dan _throughput_ [3].

- **_Latency_** adalah waktu tunda dari saat permintaan data dibuat hingga byte pertama data diterima. Ini adalah ukuran "responsivitas".
- **_Throughput_** adalah laju transfer data setelah koneksi dibuat. Ini adalah ukuran "kapasitas pipa".

Dalam konteks database, _latency_ yang rendah sangat krusial untuk beban kerja _Online Transaction Processing (OLTP)_, yang terdiri dari banyak query kecil dan acak. Di sisi lain, _throughput_ yang tinggi penting untuk beban kerja analitik (OLAP), yang sering kali melibatkan pemindaian data dalam jumlah besar secara sekuensial.

**Tabel 4.1: Perbandingan Kinerja Media Penyimpanan**

Tabel ini memberikan gambaran yang jelas tentang jurang kinerja antara berbagai lapisan memori. Ini adalah justifikasi utama mengapa `Buffer Pool` harus ada.

| Media Penyimpanan | Latency (Perkiraan) | Throughput (Perkiraan) | Analogi Skala Waktu (jika L1 = 1 detik) |
| --- | --- | --- | --- |
| CPU L1 Cache | ~1 ns | >100 GB/s | 1 detik |
| RAM (DDR4/5) | ~60−100 ns | ~40 GB/s | ~1-2 menit |
| NVMe SSD | ~15,000−100,000 ns (15−100 µs) | ~3−7 GB/s | ~4-27 jam |
| Hard Disk Drive (HDD) | ~10,000,000 ns (10 ms) | ~150 MB/s | ~4 bulan |

*Sumber data disintesis dari berbagai sumber teknis [1, 2, 3].*

**Akses Acak vs. Sekuensial: Penalti Tersembunyi dari Disk**

Kinerja disk, terutama SSD, sangat bergantung pada pola akses. Pembacaan sekuensial (membaca blok data yang berurutan) jauh lebih cepat daripada pembacaan acak (melompat dari satu lokasi ke lokasi lain). Operasi database sering kali menghasilkan pola I/O yang sangat acak. `Buffer Pool` memiliki peran ganda: tidak hanya mengurangi _jumlah_ I/O disk, tetapi juga berfungsi sebagai **transformator pola akses**. Dengan menahan halaman di RAM, `Buffer Pool` memungkinkan CPU melakukan akses acak secepat kilat di memori, menghindari penalti performa yang sangat besar dari akses acak di disk.

### 4.1.2 Memperkenalkan Buffer Pool: Cache Cerdas untuk Database Anda

Secara formal, `Buffer Pool` adalah sebuah area di memori utama (RAM) yang dialokasikan oleh sistem database untuk menjadi _cache_ bagi halaman-halaman (_pages_) yang dibaca dari disk [4]. Ia berfungsi sebagai perantara antara _Query Processor_ (yang meminta data) dan `StorageEngine` (yang mengambil data dari penyimpanan fisik).

Mekanisme kerjanya pada dasarnya sederhana:

1.  Ketika sebuah query meminta data, database manager pertama-tama akan mencari halaman yang berisi data tersebut di dalam `Buffer Pool`.
2.  **Cache Hit:** Jika halaman ditemukan, data dapat langsung diambil dari memori. Operasi ini disebut **_logical I/O_** dan sangat cepat.
3.  **Cache Miss:** Jika halaman tidak ditemukan, terjadi _cache miss_. Database manager kemudian harus meminta `StorageEngine` untuk membaca halaman tersebut dari disk dan memuatnya ke dalam sebuah slot kosong (disebut _frame_) di `Buffer Pool`. Operasi ini disebut **_physical I/O_** dan sangat lambat. Setelah halaman berada di `Buffer Pool`, data baru dapat dikembalikan ke peminta.

Tujuan utama `Buffer Pool` adalah **memaksimalkan rasio _cache hit_**. Dengan menjaga halaman yang sering diakses (_hot pages_) tetap berada di memori, database dapat beroperasi dengan kecepatan yang mendekati kecepatan RAM, bukan terikat oleh kecepatan disk yang lambat [5].

## 4.2 Implementasi Buffer Pool di Wheel DB

Sekarang kita akan beralih dari teori ke praktik. Membangun `BufferPoolManager` yang fungsional melibatkan beberapa komponen yang bekerja sama secara harmonis.

### 4.2.1 Anatomi `BufferPoolManager`

`BufferPoolManager` adalah orkestrator utama dalam manajemen memori. Untuk dapat berfungsi, ia memerlukan beberapa struktur data dan komponen pembantu.

**Komponen Inti:**

1.  `pages`: Sebuah array dari objek `Page`. Array ini adalah representasi fisik dari `Buffer Pool` itu sendiri. Setiap elemen dalam array ini disebut `frame`.
2.  `page_table`: Sebuah `HashMap<PageId, FrameId>` untuk pemetaan cepat dari `PageId` ke `FrameId` (indeks dalam array `pages`).
3.  `free_list`: Sebuah `Queue` atau `Stack` yang berisi `FrameId` dari frame yang saat ini kosong.
4.  `replacer`: Sebuah objek yang mengimplementasikan kebijakan penggantian halaman, seperti `LRUReplacer`.
5.  `disk_manager`: Referensi ke `StorageEngine` untuk melakukan operasi baca dan tulis fisik ke disk.

**Struktur `Page`:**

Objek `Page` itu sendiri membawa metadata penting:

-   `data`: Sebuah `byte[]` yang berisi konten sebenarnya dari halaman.
-   `page_id`: Pengenal unik halaman.
-   `pin_count`: Sebuah _integer_ yang melacak berapa banyak _thread_ yang sedang "menyematkan" (_pinning_) halaman ini. Halaman dengan `pin_count > 0` tidak boleh diusir.
-   `is_dirty`: Sebuah _boolean flag_ yang menandakan apakah konten halaman di memori telah diubah tetapi belum ditulis kembali ke disk.

### 4.2.2 Mekanisme Pengambilan `Page` (Logika `fetchPage`)

Metode `fetchPage(pageId)` adalah jantung dari `BufferPoolManager`. Alur logikanya adalah sebagai berikut:

1.  **Cari di Cache:** Periksa `page_table` untuk `pageId` yang diminta.
2.  **Cache Hit:** Jika ditemukan:
    *   Ambil `frameId` dari `page_table`.
    *   Tingkatkan `pin_count` halaman di `pages[frameId]`.
    *   Beri tahu `replacer` bahwa halaman ini di-_pin_ (`replacer.pin(frameId)`).
    *   Kembalikan `pages[frameId]`.
3.  **Cache Miss:** Jika tidak ditemukan:
    *   **Cari Frame Target:** Cari `frameId` yang tersedia, baik dari `free_list` atau dengan mengusir halaman lain melalui `replacer.victim()`.
    *   Jika tidak ada frame yang bisa digunakan (semua di-_pin_), kembalikan `null`.
    *   **Tangani Dirty Page:** Jika halaman korban (_victim_) `is_dirty`, tulis perubahannya ke disk menggunakan `disk_manager` sebelum menggunakannya.
    *   Hapus entri halaman korban dari `page_table`.
    *   **Muat Halaman Baru:** Baca data dari disk ke `pages[frameId]` menggunakan `disk_manager`.
    *   Perbarui metadata halaman baru (`page_id`, `pin_count = 1`, `is_dirty = false`).
    *   Tambahkan entri baru ke `page_table`.
    *   Beri tahu `replacer` bahwa halaman ini di-_pin_.
    *   Kembalikan `pages[frameId]`.

### 4.2.3 Implementasi Kebijakan Penggantian Halaman (LRU)

Ketika Buffer Pool penuh, kita perlu kebijakan untuk memilih halaman mana yang akan diganti. **Least Recently Used (LRU)** adalah pilihan yang populer. Kebijakan ini mengganti halaman yang paling lama tidak digunakan [6].

Implementasi LRU yang efisien biasanya menggunakan kombinasi `HashMap` dan `Doubly-Linked List`:

-   **`HashMap<FrameId, Node>`:** Untuk akses O(1) ke node di dalam list.
-   **`Doubly-Linked List`:** Untuk menjaga urutan akses. Halaman yang baru diakses dipindahkan ke kepala (_head_) list. Halaman yang akan diusir diambil dari ekor (_tail_) list.

**Operasi `LRUReplacer`:**

-   `victim()`: Mengambil dan menghapus `frameId` dari ekor list.
-   `pin(frameId)`: Menghapus `frameId` dari list karena sedang digunakan (tidak bisa diusir).
-   `unpin(frameId)`: Menambahkan `frameId` ke kepala list, menjadikannya kandidat untuk diusir di masa depan.

### 4.2.4 Mengelola Halaman yang "Kotor" (Dirty Pages)

Sebuah halaman disebut "kotor" (_dirty_) jika isinya telah dimodifikasi di memori tetapi perubahan tersebut belum ditulis kembali ke disk. Mengelola _dirty pages_ sangat penting untuk menjaga konsistensi data dan durabilitas [7].

**Mekanisme pengelolaan dirty pages:**

-   **Flag `is_dirty`:** Setiap `Page` memiliki flag ini. Ketika sebuah halaman dimodifikasi, flag ini diatur ke `true`.
-   **Write-Back Policy:** Perubahan pada _dirty pages_ tidak langsung ditulis ke disk. Sebaliknya, mereka di-buffer di memori dan ditulis kembali ke disk pada waktu yang tepat, misalnya:
    *   Ketika halaman dipilih sebagai _victim_ oleh kebijakan penggantian halaman.
    *   Secara berkala oleh _background process_ (misalnya, _checkpointing_).
    *   Ketika transaksi di-_commit_.

### 4.2.5 Contoh Struktur Kode (Konseptual)

Meskipun `BufferMgr.java` di `tiny-db` masih kosong, kita bisa membayangkan struktur kelas `BufferPoolManager` konseptual dalam Java:

```java
package com.arjunsk.wheel_db.server.d_storage_engine.common.transaction.d_buffer_mgr;

import com.arjunsk.wheel_db.server.d_storage_engine.common.Page;
import com.arjunsk.wheel_db.server.d_storage_engine.common.PageId;
import com.arjunsk.wheel_db.server.d_storage_engine.common.file.FileManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class BufferPoolManager {

    private final int poolSize;
    private final Page[] pages;
    private final Map<PageId, Integer> pageTable; // Memetakan PageId ke FrameId
    private final Queue<Integer> freeList; // Daftar FrameId yang kosong
    private final LRUReplacer replacer;
    private final FileManager fileManager;

    public BufferPoolManager(int poolSize, FileManager fileManager) {
        this.poolSize = poolSize;
        this.pages = new Page[poolSize];
        this.pageTable = new HashMap<>();
        this.freeList = new LinkedList<>();
        for (int i = 0; i < poolSize; i++) {
            this.freeList.add(i);
            this.pages[i] = new Page(); // Inisialisasi objek Page
        }
        this.replacer = new LRUReplacer(poolSize);
        this.fileManager = fileManager;
    }

    public Page fetchPage(PageId pageId) throws Exception {
        // 1. Cari di page table (cache hit)
        if (pageTable.containsKey(pageId)) {
            int frameId = pageTable.get(pageId);
            Page page = pages[frameId];
            page.incrementPinCount();
            replacer.pin(frameId);
            return page;
        }

        // 2. Cache miss: cari frame yang tersedia
        Integer frameId = getAvailableFrame();
        if (frameId == null) {
            return null; // Tidak ada frame yang tersedia
        }

        // 3. Muat page baru
        Page page = pages[frameId];
        page.setPageId(pageId);
        page.setPinCount(1);
        page.setDirty(false);
        
        fileManager.readPage(pageId, page.getData()); // Baca dari disk ke buffer page

        pageTable.put(pageId, frameId);
        replacer.pin(frameId);

        return page;
    }

    private Integer getAvailableFrame() throws Exception {
        // Coba dari free list dulu
        if (!freeList.isEmpty()) {
            return freeList.poll();
        }

        // Jika tidak ada, coba dari replacer
        Integer victimFrameId = replacer.victim();
        if (victimFrameId == null) {
            return null; // Semua page di-pin
        }

        // Tangani dirty page dari victim
        Page victimPage = pages[victimFrameId];
        if (victimPage.isDirty()) {
            fileManager.writePage(victimPage.getPageId(), victimPage.getData());
        }

        // Hapus victim dari page table
        pageTable.remove(victimPage.getPageId());

        return victimFrameId;
    }
    
    // Metode lain seperti unpinPage, flushPage, flushAllPages, dll.
}
```

Kode konseptual di atas menunjukkan bagaimana kelas `BufferPoolManager` dapat mengelola _pages_, menangani _cache hit_ dan _miss_, menerapkan kebijakan LRU, dan mengelola _dirty pages_. `FileManager` diasumsikan sebagai komponen yang bertanggung jawab untuk interaksi langsung dengan disk.

---

### Referensi

[1] Sorial, S. (2022, October 24). *Database Buffer Pool - Part 1*. Samuel Sorial's Blog. [https://samuel-sorial.hashnode.dev/database-buffer-pool-part-1](https://samuel-sorial.hashnode.dev/database-buffer-pool-part-1)
[2] Jeffries, R. (2001). *Essential Lightwave 3D 8*. Wordware Publishing, Inc.
[3] Silberschatz, A., Galvin, P. B., & Gagne, G. (2018). *Operating System Concepts*. Wiley.
[4] IBM. *Buffer pools - Db2*. [https://www.ibm.com/docs/en/db2/11.5.x?topic=databases-buffer-pools](https://www.ibm.com/docs/en/db2/11.5.x?topic=databases-buffer-pools)
[5] Carnegie Mellon University. (2024). *15-445/645 Database Systems (Spring 2024) - 06 Buffer Pools*. [https://15445.courses.cs.cmu.edu/spring2024/notes/06-bufferpool.pdf](https://15445.courses.cs.cmu.edu/spring2024/notes/06-bufferpool.pdf)
[6] GeeksforGeeks. (2025, July 23). *Program for Least Recently Used (LRU) Page Replacement Algorithm*. [https://www.geeksforgeeks.org/dsa/program-for-least-recently-used-lru-page-replacement-algorithm/](https://www.geeksforgeeks.org/dsa/program-for-least-recently-used-lru-page-replacement-algorithm/)
[7] Alibaba Cloud. (2024, May 29). *An In-Depth Analysis of Buffer Pool in InnoDB*. [https://www.alibabacloud.com/blog/an-in-depth-analysis-of-buffer-pool-in-innodb_601216](https://www.alibabacloud.com/blog/an-in-depth-analysis-of-buffer-pool-in-innodb_601216)

