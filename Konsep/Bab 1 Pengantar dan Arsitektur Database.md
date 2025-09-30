- MIT Database Simple Java: https://github.com/MIT-DB-Class/simple-db-hw-2021/tree/master
- https://github.com/dborchard/tiny-db (utama ini)
## Pendahuluan: Memulai Perjalanan Membangun "Mesin" Data

Selamat datang di bab pertama dalam perjalanan kita membangun "Wheel DB", sebuah database relasional dari nol. Proyek ini lebih dari sekadar latihan pemrograman; ini adalah sebuah ekspedisi ke jantung sistem data modern. Hampir setiap aplikasi yang kita gunakan sehari-hari, mulai dari platform media sosial hingga sistem perbankan global, ditenagai oleh database. Memahami cara kerja internalnya adalah pembeda antara seorang pengembang aplikasi dan seorang arsitek sistem yang andal.

Analogi yang tepat untuk proyek ini adalah membangun mesin mobil. Banyak orang bisa mengemudikan mobil (menggunakan database), tetapi hanya segelintir yang benar-benar memahami cara kerja mesin di balik kapnya. Proyek "Wheel DB" akan mengajak Anda untuk membuka kap tersebut, membongkar setiap komponen, dan merakitnya kembali satu per satu. Di akhir perjalanan ini, Anda tidak hanya akan memiliki sebuah database yang berfungsi, tetapi juga pemahaman mendalam dan intuisi yang kuat tentang bagaimana data disimpan, diakses, dan dikelola secara efisien dan andal.

Mari kita mulai dengan meletakkan fondasi yang kokoh: menetapkan tujuan, mendefinisikan arsitektur, dan memahami peran setiap komponen yang akan kita bangun.

---

## 1.1 Tujuan Proyek "Wheel DB"

Sebelum menulis baris kode pertama, sangat penting untuk mendefinisikan dengan jelas apa yang ingin kita capai, batasan-batasan proyek, dan pengetahuan apa yang diperlukan untuk berhasil. Bagian ini akan menetapkan ekspektasi yang realistis dan memastikan kita semua memulai dari titik yang sama.

### 1.1.1 Memahami Tujuan Akhir: Belajar Melalui Praktik

Tujuan utama dari proyek "Wheel DB" bukanlah untuk menciptakan pesaing komersial bagi PostgreSQL atau MySQL. Sebaliknya, tujuan utamanya adalah **edukasi**. Kita akan membangun sebuah database relasional sederhana dari awal untuk memahami secara mendalam prinsip-prinsip fundamental yang mendasari semua sistem manajemen database (DBMS).

Melalui proses ini, kita akan menjawab pertanyaan-pertanyaan mendasar seperti:

- Bagaimana sebuah perintah `SELECT * FROM users;` diterjemahkan dari teks menjadi data aktual yang diambil dari sebuah file di hard drive?
    
- Bagaimana database memastikan bahwa data tetap konsisten bahkan jika terjadi kegagalan daya di tengah-tengah operasi penulisan?
    
- Strategi apa yang digunakan database untuk mengeksekusi query yang melibatkan banyak tabel (join) secara efisien?
    

Dengan membangun setiap komponen sendiri, kita akan mengubah konsep-konsep abstrak ini menjadi implementasi konkret dalam bahasa Java. Hasil akhir yang paling berharga dari proyek ini bukanlah perangkat lunak "Wheel DB" itu sendiri, melainkan **model mental yang kaya dan akurat** tentang cara kerja sistem data yang akan Anda bawa sepanjang karier rekayasa perangkat lunak Anda. Setiap tantangan dan penyederhanaan yang kita hadapi bukan merupakan keterbatasan, melainkan pilihan pedagogis yang disengaja untuk memfokuskan pembelajaran pada konsep inti tertentu.

### 1.1.2 Menjelaskan _Scope_ Proyek: Fitur yang Akan Dibuat dan yang Tidak

Manajemen ekspektasi adalah kunci keberhasilan proyek dengan skala seperti ini. Dengan merujuk pada struktur dan fitur dari proyek `tiny-db` yang menjadi inspirasi kita 1, kita dapat menetapkan batasan yang jelas antara apa yang akan kita implementasikan dan apa yang hanya akan kita pelajari secara teoretis pada tahap awal.

**Fitur In-Scope (Yang Akan Kita Bangun):**

- **Storage Engine Sederhana:** Kita akan membangun sebuah _storage engine_ berbasis file, yang sering disebut sebagai _Heap File_. Komponen ini akan bertanggung jawab untuk mengelola data dalam unit-unit yang disebut _pages_ atau _blocks_ di dalam file fisik. Ini adalah lapisan paling dasar, di mana _tuple_ (baris) disimpan secara sekuensial.1
    
- **Query Engine dengan Model Iterator:** Implementasi mesin eksekusi query akan mengikuti _Volcano_ atau _Iterator Model_. Kita akan membuat operator-operator dasar seperti `Projection` (untuk memilih kolom) dan `Selection` (untuk memfilter baris dengan klausa `WHERE`).1
    
- **Parser SQL Sederhana:** Kita akan membuat parser yang dapat memahami dan menerjemahkan perintah DDL (`CREATE TABLE`) dan DML (`INSERT`, `SELECT`) yang paling dasar.
    
- **Manajemen Katalog (Metadata):** Sebuah mekanisme sederhana untuk melacak informasi tentang tabel yang ada, skema (nama dan tipe kolom), dan lokasi file datanya.
    

**Fitur Out-of-Scope (Yang Akan Dipelajari Teorinya, Namun Tidak Diimplementasikan di Awal):**

- **Transaction Manager (ACID Penuh):** Implementasi transaksi yang menjamin properti ACID (Atomicity, Consistency, Isolation, Durability) secara penuh adalah tugas yang sangat kompleks. Ini memerlukan komponen tambahan seperti _Concurrency Manager_ (untuk isolasi) dan _Recovery Manager_ (untuk atomisitas dan durabilitas), yang tercantum dalam daftar "TODO" pada proyek `tiny-db`.1 Kita akan membahas teori di baliknya secara mendalam di bagian 1.2, tetapi implementasinya akan ditunda ke bab-bab selanjutnya.
    
- **Buffer Manager:** Komponen ini berfungsi sebagai cache untuk halaman data di memori, yang krusial untuk performa.2 Namun, ia menambahkan lapisan kompleksitas yang signifikan pada interaksi dengan disk. Untuk memulai, kita akan menyederhanakannya dengan membaca dan menulis halaman langsung dari dan ke disk. Ini juga merupakan fitur "TODO" di
    
    `tiny-db`.1
    
- **Struktur Indeks Lanjutan (B+ Tree):** Meskipun `tiny-db` menyebutkan B+ Tree 1, kita akan memulai dengan pendekatan paling dasar:
    
    _full table scan_ (membaca seluruh tabel untuk setiap query). Ini akan membangun fondasi yang kuat untuk memahami mengapa indeks sangat penting, yang akan kita bahas dan implementasikan di bab selanjutnya.
    
- **Query Optimizer Canggih:** Database modern menggunakan _Cost-Based Optimizer_ (CBO) yang canggih untuk memilih rencana eksekusi query terbaik.3 Kita akan memulai dengan
    
    _Rule-Based Optimizer_ (RBO) yang jauh lebih sederhana.
    

Daftar "TODO" pada proyek `tiny-db` bukanlah sebuah kekurangan, melainkan sebuah peta jalan pembelajaran yang sangat logis. Ia secara alami memisahkan komponen-komponen fundamental (penyimpanan, eksekusi dasar) dari komponen-komponen lanjutan yang memberikan Keandalan (_Reliability_), Konkurensi (_Concurrency_), dan Kinerja (_Performance_). Kurikulum "Wheel DB" akan mengadopsi pendekatan berlapis yang telah terbukti ini.

### 1.1.3 Prasyarat: Bekal yang Anda Butuhkan

Untuk dapat mengikuti proyek ini dengan baik, ada beberapa pengetahuan dan perangkat yang perlu Anda siapkan:

- **Pemrograman Java (Tingkat Menengah ke Mahir):** Anda harus sudah nyaman dengan konsep-konsep inti Java, termasuk Object-Oriented Programming (OOP), struktur data standar (Map, List, Array), operasi File I/O (Input/Output), dan manajemen memori dasar.
    
- **Apache Maven:** Memiliki pengalaman dalam menggunakan Maven untuk mengelola dependensi proyek dan proses _build_.
    
- **Konsep Database Umum:** Anda harus memahami database dari sudut pandang pengguna. Ini berarti Anda familiar dengan SQL dan konsep seperti tabel, baris (row), kolom (column), _primary key_, dan cara kerja query `SELECT`, `INSERT`, serta `CREATE TABLE`. Pengetahuan tentang internal database tidak diperlukan.
    
- **Lingkungan Pengembangan:** Sebuah Integrated Development Environment (IDE) seperti IntelliJ IDEA atau Eclipse, sistem kontrol versi Git, dan kenyamanan menggunakan terminal atau _command line_.
    

---

## 1.2 Gambaran Umum Arsitektur RDBMS

Setiap _Relational Database Management System_ (RDBMS), dari SQLite yang ringan hingga Oracle yang masif, dibangun di atas serangkaian komponen arsitektural yang serupa. Memahami komponen-komponen ini dan interaksinya adalah langkah pertama untuk membangun database kita sendiri. Bayangkan sebuah query SQL memulai perjalanannya dari aplikasi Anda; ia akan melewati beberapa lapisan logis sebelum hasilnya kembali kepada Anda.

Secara umum, arsitektur RDBMS dapat dibagi menjadi beberapa komponen utama yang saling bekerja sama:

1. **Parser & Optimizer:** Menerima string SQL, memverifikasi sintaksnya, dan mengubahnya menjadi rencana eksekusi yang efisien.
    
2. **Query Engine (Executor):** Mengambil rencana eksekusi dan menjalankan serangkaian operasi untuk menghasilkan data yang diminta.
    
3. **Transaction Manager:** Memastikan bahwa operasi yang mengubah data mematuhi properti ACID, menjaga integritas data.
    
4. **Buffer Manager:** Mengelola cache data di dalam memori (RAM) untuk meminimalkan akses disk yang lambat.
    
5. **Storage Engine:** Lapisan paling bawah yang bertanggung jawab untuk menulis dan membaca data secara fisik ke dan dari media penyimpanan permanen (seperti SSD atau HDD).
    

Mari kita bedah setiap komponen ini secara lebih mendalam.

### 1.2.1 Storage Engine: Sang Penjaga Gudang Data

_Storage Engine_ adalah fondasi dari setiap database. Ia adalah komponen perangkat lunak yang bertanggung jawab penuh atas manajemen data pada tingkat fisik—bagaimana data disimpan, diorganisir, dan diakses dari media penyimpanan permanen.5 Ketika lapisan yang lebih tinggi meminta "baris dengan ID=123",

_Storage Engine_-lah yang menerjemahkan permintaan logis tersebut menjadi operasi fisik seperti "baca 16 kilobyte dari file `tabel_produk.dat` pada offset 65536".

**Struktur dan Abstraksi**

_Storage Engine_ menyembunyikan kompleksitas perangkat keras penyimpanan dan menyediakan abstraksi yang bersih. Data biasanya diorganisir dalam hierarki berikut:

- **File:** Data untuk sebuah tabel seringkali disimpan dalam satu atau beberapa file di sistem operasi.
    
- **Page (atau Block):** File tersebut dibagi menjadi blok-blok berukuran tetap, yang disebut _pages_. Ukuran _page_ umumnya berkisar antara 4KB hingga 16KB. _Page_ adalah unit transfer data minimum antara disk dan memori.
    
- **Tuple (atau Row):** Di dalam sebuah _page_, data disimpan sebagai baris-baris individual atau _tuple_. _Storage Engine_ juga mengelola metadata di dalam _page_ untuk melacak di mana setiap _tuple_ berada dan berapa banyak ruang kosong yang tersisa.
    

Dalam proyek `tiny-db`, abstraksi ini diwakili oleh kelas-kelas seperti `FileManager`, `BlockId`, dan `Page` 1, yang akan kita implementasikan juga di "Wheel DB".

**Paradigma Desain Storage Engine**

Pilihan desain inti dalam sebuah _storage engine_ sangat memengaruhi karakteristik kinerjanya. Ada dua paradigma dominan di industri saat ini:

1. **Berbasis B-Tree (Dioptimalkan untuk Baca):** Digunakan oleh RDBMS tradisional seperti MySQL (dengan InnoDB) dan PostgreSQL.7 Data dan indeks disimpan dalam struktur data pohon yang menyeimbangkan diri sendiri (B-Tree atau B+Tree). Struktur ini memungkinkan pencarian data spesifik (
    
    _point queries_) dan pencarian dalam rentang (_range queries_) dengan sangat cepat, biasanya dalam waktu logaritmik. Namun, kelemahannya terletak pada operasi tulis. Memperbarui sebuah baris seringkali berarti melakukan operasi _update-in-place_, yang dapat menyebabkan penulisan acak (_random writes_) ke disk, sebuah operasi yang secara inheren lambat.7
    
2. **Berbasis LSM-Tree (Dioptimalkan untuk Tulis):** _Log-Structured Merge-Tree_ (LSM-Tree) diadopsi oleh sistem yang perlu menangani volume tulis yang sangat tinggi, seperti Apache Cassandra, RocksDB, dan Google Bigtable.7 Ide utamanya adalah menghindari
    
    _random writes_ sama sekali. Semua operasi tulis (insert, update, delete) pertama-tama ditulis secara sekuensial ke struktur di dalam memori yang disebut _MemTable_. Ketika _MemTable_ penuh, ia diurutkan dan ditulis ke disk sebagai file baru yang tidak dapat diubah (_immutable_) yang disebut _SSTable_ (Sorted String Table). Proses ini sangat cepat karena hanya melibatkan penulisan sekuensial. Tantangannya muncul saat membaca data, karena sebuah baris mungkin ada di _MemTable_ dan beberapa _SSTable_ yang berbeda. Database harus menggabungkan data dari semua sumber ini, sebuah proses yang dikenal sebagai _read amplification_.8
    

Pilihan antara B-Tree dan LSM-Tree merepresentasikan sebuah _trade-off_ fundamental dalam desain sistem: apakah Anda mengoptimalkan untuk beban kerja baca (seperti aplikasi e-commerce) atau beban kerja tulis (seperti sistem pencatatan log atau data IoT)?

Untuk "Wheel DB", kita akan memulai dengan implementasi yang paling sederhana: **Heap File**. Ini adalah sebuah file di mana _tuple_ baru ditambahkan di akhir, tanpa pengurutan apa pun. Meskipun tidak efisien karena selalu memerlukan pemindaian seluruh tabel (_full table scan_), ini adalah titik awal yang sangat baik untuk memahami dasar-dasar manajemen _page_ dan _tuple_ sebelum kita beralih ke struktur yang lebih kompleks seperti indeks.

| Karakteristik              | InnoDB (B-Tree)                      | RocksDB (LSM-Tree)                        | Wheel DB (Heap File)           |
| -------------------------- | ------------------------------------ | ----------------------------------------- | ------------------------------ |
| **Optimasi Utama**         | Baca (Read)                          | Tulis (Write)                             | Kesederhanaan (Simplicity)     |
| **Kasus Penggunaan Ideal** | OLTP, aplikasi web transaksional     | Ingest data bervolume tinggi, log         | Proyek edukasi, prototipe awal |
| **Struktur Data Inti**     | B+Tree                               | MemTable (di memori) & SSTables (di disk) | File tidak terurut             |
| **Kompleksitas Update**    | Update-in-place (potensi random I/O) | Append-only (selalu sekuensial)           | Append-only                    |
| **Kompleksitas Baca**      | Cepat (logaritmik)                   | Potensial lambat (read amplification)     | Lambat (linear, full scan)     |

### 1.2.2 Query Engine (Processing Layer): Sang Pelaksana Rencana

Jika _Storage Engine_ adalah gudang, maka _Query Engine_ (juga dikenal sebagai _Execution Engine_ atau _Relational Engine_) adalah tim pekerja yang menjalankan perintah untuk mengambil barang dari gudang tersebut.6 Komponen ini menerima sebuah

_execution plan_ (rencana eksekusi) dari _Optimizer_ dan secara fisik menjalankannya. Ia tidak membuat keputusan strategis tentang _cara terbaik_ untuk mendapatkan data, tetapi ia sangat efisien dalam mengeksekusi serangkaian instruksi yang telah ditentukan.

**Model Eksekusi Volcano (Iterator Model)**

Model arsitektur yang paling umum dan elegan untuk _query engine_ adalah _Volcano Model_, atau lebih dikenal sebagai _Iterator Model_. Dalam model ini, setiap operasi dalam sebuah query (seperti memindai tabel, memfilter baris, atau menggabungkan tabel) diimplementasikan sebagai sebuah **Operator**.

Setiap operator mengekspos antarmuka (interface) yang sederhana, biasanya terdiri dari tiga metode: `open()`, `next()`, dan `close()`.

- `open()`: Menginisialisasi operator, misalnya membuka file atau mengalokasikan memori.
    
- `next()`: Metode inti. Ketika dipanggil, operator akan melakukan tugasnya dan mengembalikan satu _tuple_ hasil. Jika tidak ada lagi _tuple_ yang bisa dihasilkan, ia akan mengembalikan `null`.
    
- `close()`: Membersihkan sumber daya yang digunakan oleh operator.
    

Operator-operator ini kemudian disusun dalam sebuah pohon rencana eksekusi. Data "ditarik" (_pulled_) dari bawah ke atas. Sebagai contoh, untuk query `SELECT name FROM users WHERE age > 30;`, pohon operatornya mungkin terlihat seperti ini:

1. **ProjectionOperator (`name`)**: Di puncak pohon. Setiap kali `next()` dipanggil, ia akan memanggil `next()` pada operator di bawahnya, mengambil _tuple_ lengkap, mengekstrak kolom `name`, dan mengembalikannya.
    
2. **FilterOperator (`age > 30`)**: Di tengah. Setiap kali `next()` dipanggil, ia akan terus memanggil `next()` pada operator di bawahnya sampai menemukan _tuple_ yang memenuhi kondisi `age > 30`, lalu mengembalikannya ke atas.
    
3. **TableScanOperator (`users`)**: Di dasar pohon. Setiap kali `next()` dipanggil, ia akan membaca _tuple_ berikutnya secara sekuensial dari file data tabel `users` dan mengembalikannya.
    

Kekuatan model ini terletak pada **komposabilitas dan modularitasnya**. Setiap operator adalah komponen mandiri yang dapat digunakan kembali. Kita dapat membangun rencana query yang sangat kompleks hanya dengan menyusun operator-operator sederhana ini. Menambahkan fungsionalitas baru, seperti pengurutan (`SortOperator`), menjadi sangat mudah: kita hanya perlu mengimplementasikan antarmuka operator yang sama dan memasukkannya ke dalam pohon rencana.

Untuk "Wheel DB", kita akan mengadopsi model ini. Kerangka dasar untuk operator kita bisa terlihat seperti ini dalam Java:

Java

```
public interface Operator {
    /**
     * Mempersiapkan operator untuk dieksekusi.
     */
    void open();

    /**
     * Mengembalikan tuple berikutnya dalam hasil.
     * @return Tuple berikutnya, atau null jika tidak ada lagi.
     */
    Tuple next();

    /**
     * Membersihkan sumber daya setelah eksekusi selesai.
     */
    void close();
}
```

### 1.2.3 Parser & Optimizer: Sang Penerjemah dan Ahli Strategi

Komponen ini adalah "otak" dari database.4 Ia bertanggung jawab untuk mengambil string SQL yang ditulis oleh manusia—yang bersifat deklaratif ("apa yang saya inginkan")—dan mengubahnya menjadi rencana eksekusi prosedural yang paling efisien ("bagaimana cara mendapatkannya") untuk dijalankan oleh

_Query Engine_.3 Proses ini biasanya terjadi dalam dua fase utama:

_Parsing_ dan _Optimization_.

**1. Parsing**

Fase pertama adalah menerjemahkan string SQL mentah menjadi struktur data terprogram yang dapat dipahami oleh database. Proses ini mirip dengan cara kerja kompilator bahasa pemrograman.

- **Analisis Leksikal & Sintaksis:** _Parser_ pertama-tama memecah string query menjadi token-token (misalnya, `SELECT`, `*`, `FROM`, `users`) dan kemudian memeriksa apakah urutan token tersebut sesuai dengan tata bahasa (grammar) SQL yang valid.10
    
- **Pembuatan Abstract Syntax Tree (AST):** Jika sintaksnya valid, _parser_ akan membangun sebuah representasi hierarkis dari query yang disebut _Abstract Syntax Tree_ (AST).11 AST ini menangkap struktur logis dari query. Misalnya, query
    
    `SELECT name FROM users WHERE id = 10` akan diubah menjadi pohon dengan node akar `SELECT`, yang memiliki anak-anak untuk daftar kolom (`name`), sumber tabel (`users`), dan klausa `WHERE`.
    

Banyak database menggunakan _parser generator_ seperti ANTLR (seperti pada `tiny-db` 1) atau Yacc/Bison untuk menghasilkan kode

_parser_ secara otomatis dari definisi grammar. Ada juga pustaka khusus seperti Apache Calcite 12 atau SQLGlot 13 yang menyediakan fungsionalitas parsing SQL yang kuat.

**2. Optimization**

Setelah query diubah menjadi AST (rencana logis), tugas _Optimizer_ adalah mengubahnya menjadi rencana eksekusi fisik yang paling efisien. Satu query logis bisa memiliki puluhan atau bahkan ribuan rencana eksekusi fisik yang setara. Misalnya, saat menggabungkan dua tabel (A dan B), database bisa memilih untuk:

- Menggunakan _Nested Loop Join_ (memindai B untuk setiap baris di A).
    
- Menggunakan _Hash Join_ (membangun hash table dari salah satu tabel).
    
- Menggunakan _Sort-Merge Join_ (mengurutkan kedua tabel terlebih dahulu).
    

Pilihan yang tepat dapat menghasilkan perbedaan kinerja ribuan kali lipat. Ada dua jenis utama _optimizer_:

- **Rule-Based Optimizer (RBO):** Pendekatan yang lebih tua dan lebih sederhana. RBO menggunakan seperangkat aturan heuristik yang telah ditentukan untuk mengubah pohon AST. Contoh aturannya adalah "Lakukan operasi filter (`WHERE`) sedini mungkin" (ini disebut _predicate pushdown_). Aturan ini masuk akal karena dengan memfilter data lebih awal, jumlah baris yang perlu diproses oleh operasi selanjutnya (seperti join) akan berkurang secara signifikan. Untuk "Wheel DB", kita akan mengimplementasikan RBO sederhana.
    
- **Cost-Based Optimizer (CBO):** Digunakan oleh hampir semua database modern.3 CBO adalah pendekatan yang jauh lebih canggih. Ia akan menghasilkan banyak kemungkinan rencana eksekusi, kemudian untuk setiap rencana, ia akan memperkirakan "biaya" eksekusinya. Biaya ini adalah fungsi dari perkiraan penggunaan sumber daya seperti I/O disk, CPU, dan memori. Untuk membuat estimasi ini, CBO sangat bergantung pada statistik yang dikumpulkan tentang data (misalnya, jumlah baris dalam tabel, jumlah nilai unik dalam sebuah kolom, histogram distribusi data). Akhirnya, CBO akan memilih rencana dengan perkiraan biaya terendah.
    

Pemisahan antara SQL yang deklaratif dan rencana eksekusi yang imperatif adalah salah satu ide paling kuat dalam desain database. Ini memungkinkan pengguna untuk fokus pada logika bisnis mereka tanpa perlu khawatir tentang detail implementasi tingkat rendah. Sementara itu, pengembang database dapat terus meningkatkan kecerdasan _optimizer_ untuk membuat query yang sama berjalan lebih cepat dari waktu ke waktu, tanpa perlu mengubah kode aplikasi.

### 1.2.4 Transaction Manager: Sang Penjamin Kepercayaan

_Transaction Manager_ adalah komponen yang memberikan jaminan keandalan dan integritas data. Ia memastikan bahwa serangkaian operasi yang membentuk sebuah "transaksi" mematuhi properti yang dikenal sebagai **ACID**.14 Sebuah transaksi adalah satu unit kerja logis yang harus berhasil sepenuhnya atau gagal sepenuhnya. Contoh klasiknya adalah transfer uang antar rekening bank: operasi mengurangi saldo di rekening A dan menambah saldo di rekening B harus terjadi sebagai satu unit atomik. Jika sistem gagal setelah saldo A berkurang tetapi sebelum saldo B bertambah, uang akan hilang.

_Transaction Manager_ ada untuk mencegah skenario bencana seperti ini.16

ACID adalah akronim untuk empat properti berikut:

- **Atomicity (Atomisitas - "Semua atau Tidak Sama Sekali"):** Properti ini menjamin bahwa semua operasi dalam sebuah transaksi diperlakukan sebagai satu unit tunggal yang tidak dapat dibagi. Jika salah satu operasi gagal, seluruh transaksi akan dibatalkan (_rollback_), dan database akan dikembalikan ke keadaan sebelum transaksi dimulai.16 Ini biasanya diimplementasikan menggunakan mekanisme
    
    _logging_ seperti _undo logs_, yang mencatat cara membatalkan setiap perubahan.18
    
- **Consistency (Konsistensi - "Data Selalu Valid"):** Properti ini memastikan bahwa setiap transaksi yang berhasil akan membawa database dari satu keadaan valid ke keadaan valid lainnya. Semua aturan dan batasan yang didefinisikan pada skema (seperti `NOT NULL`, `UNIQUE`, `FOREIGN KEY`) harus tetap terpenuhi. Jika sebuah transaksi akan melanggar aturan ini, ia akan dibatalkan.14
    
- **Isolation (Isolasi - "Bekerja Sendiri"):** Properti ini mengatasi masalah yang timbul ketika banyak transaksi dijalankan secara bersamaan (_concurrently_). Isolasi menjamin bahwa eksekusi transaksi yang bersamaan akan menghasilkan keadaan sistem yang sama seolah-olah transaksi-transaksi tersebut dieksekusi secara berurutan.16 Ini mencegah anomali konkurensi seperti
    
    _dirty reads_ (membaca data yang belum di-_commit_ oleh transaksi lain) atau _lost updates_ (ketika dua transaksi menimpa perubahan satu sama lain). Implementasi umumnya menggunakan mekanisme penguncian (_locking_) atau _Multi-Version Concurrency Control_ (MVCC).15
    
- **Durability (Daya Tahan - "Perubahan yang Abadi"):** Properti ini menjamin bahwa setelah sebuah transaksi berhasil di-_commit_, perubahannya akan bersifat permanen dan tidak akan hilang, bahkan jika terjadi kegagalan sistem seperti mati listrik atau _crash_.14 Ini biasanya dicapai melalui teknik yang disebut
    
    _Write-Ahead Logging_ (WAL), di mana catatan tentang perubahan ditulis ke log yang persisten di disk _sebelum_ perubahan tersebut diterapkan pada file data utama.18
    

Penting untuk dipahami bahwa "Transaction Manager" bukanlah satu blok kode monolitik. Ia adalah sebuah konsep payung yang mencakup beberapa subsistem yang bekerja sama. Biasanya, _Recovery Manager_ (menggunakan log) bertanggung jawab atas Atomisitas dan Durabilitas, sementara _Concurrency Control Manager_ (menggunakan kunci atau MVCC) bertanggung jawab atas Isolasi.17 Kompleksitas interaksi inilah yang membuat implementasi transaksi penuh menjadi sangat menantang dan mengapa kita menundanya dalam proyek "Wheel DB".

|Properti|Pertanyaan yang Dijawab|Analogi (Transfer Bank)|Ancaman yang Dicegah|Komponen Penanggung Jawab|
|---|---|---|---|---|
|**Atomicity**|Apakah semua operasi berhasil sebagai satu unit?|Uang tidak bisa hanya didebit tanpa dikredit. Operasi harus lengkap atau tidak sama sekali.|Perubahan data parsial akibat kegagalan.|Transaction Manager / Recovery Manager|
|**Consistency**|Apakah data tetap valid setelah transaksi?|Saldo tidak boleh menjadi negatif jika aturan bank melarangnya.|Pelanggaran integritas data (misal, duplikat ID unik).|DBMS & Programmer Aplikasi|
|**Isolation**|Apakah transaksi yang bersamaan saling mengganggu?|Dua transfer dari rekening yang sama secara bersamaan tidak boleh menghasilkan saldo akhir yang salah.|_Dirty reads_, _lost updates_, _phantom reads_.|Concurrency Control Manager|
|**Durability**|Apakah perubahan akan bertahan setelah _commit_?|Jika bank mengonfirmasi transfer berhasil, uang harus tetap terkirim meskipun server bank mati sesaat setelahnya.|Kehilangan data akibat _system crash_ atau mati listrik.|Recovery Manager (via Logging)|

### 1.2.5 Buffer Manager: Sang Manajer Memori Cerdas

Komponen terakhir dalam arsitektur inti kita adalah _Buffer Manager_. Tujuannya sederhana namun sangat penting: meminimalkan operasi I/O disk yang lambat. Ada perbedaan kecepatan yang sangat besar antara mengakses data dari RAM (sangat cepat, dalam nanodetik) dan mengaksesnya dari disk/SSD (jauh lebih lambat, dalam mikrodetik hingga milidetik). Faktanya, akses disk adalah salah satu operasi paling mahal dan menjadi _bottleneck_ utama dalam kinerja database.

_Buffer Manager_ mengatasi masalah ini dengan mengelola sebuah area di RAM yang disebut **buffer pool**.2

_Buffer pool_ ini pada dasarnya adalah sebuah _cache_ untuk _page_ data.

**Mekanisme Kerja**

1. **Permintaan Page:** Ketika _Query Engine_ perlu membaca atau menulis data pada sebuah _page_, ia tidak langsung mengakses disk. Sebaliknya, ia meminta _page_ tersebut dari _Buffer Manager_.
    
2. **Cache Hit vs. Miss:**
    
    - Jika _page_ yang diminta sudah ada di dalam _buffer pool_ (sebuah **cache hit**), _Buffer Manager_ dapat langsung mengembalikan referensi ke _page_ tersebut di memori. Ini adalah operasi yang sangat cepat.
        
    - Jika _page_ tidak ada di _buffer pool_ (sebuah **cache miss**), _Buffer Manager_ harus melakukan pekerjaan yang lebih berat. Ia akan membaca _page_ tersebut dari disk dan memuatnya ke dalam sebuah _frame_ (slot kosong) di _buffer pool_. Setelah itu, ia baru bisa mengembalikan referensi _page_ tersebut ke _Query Engine_.
        
3. **Eviction Policy:** _Buffer pool_ memiliki ukuran yang terbatas. Ketika terjadi _cache miss_ dan _buffer pool_ sudah penuh, _Buffer Manager_ harus memilih salah satu _page_ yang ada untuk "diusir" (_evict_) agar bisa memberikan ruang bagi _page_ yang baru. Kebijakan untuk memilih _page_ mana yang akan diusir disebut **eviction policy**. Kebijakan yang umum digunakan adalah _Least Recently Used_ (LRU), yang mengusir _page_ yang paling lama tidak diakses, dengan asumsi bahwa _page_ yang baru saja diakses kemungkinan besar akan diakses lagi dalam waktu dekat.2
    

Keberadaan _Buffer Manager_ secara fundamental mengubah profil kinerja database. Tanpanya, kinerja akan sepenuhnya terikat oleh kecepatan disk (_disk-bound_). Dengan _Buffer Manager_ yang efektif, sebagian besar permintaan data dapat dilayani dari memori, membuat kinerja sistem menjadi terikat oleh kecepatan memori (_memory-bound_). Efektivitas—ukuran dan kecerdasan kebijakan pengusiran—dari _Buffer Manager_ seringkali menjadi faktor penentu tunggal terbesar untuk latensi query dalam beban kerja yang intensif data. Inilah mengapa komponen ini sangat penting untuk kinerja, tetapi juga mengapa implementasinya (yang melibatkan manajemen memori, konkurensi, dan kebijakan yang cerdas) kita simpan untuk tahap selanjutnya dalam pengembangan "Wheel DB".