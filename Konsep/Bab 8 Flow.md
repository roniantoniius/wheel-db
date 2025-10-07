# Materi Kurikulum Wheel DB - Bab 8: Alur Hidup Sebuah Query dan Kesimpulan

## Pendahuluan - Merangkai Puzzle Database

Bab ini adalah puncak dari perjalanan kita membangun Wheel DB. Di bab-bab sebelumnya, kita telah membangun komponen-komponen individual: `Storage Layer` yang andal untuk menyimpan data di disk, `Indexing` dengan `B+Tree` untuk mempercepat pencarian, dan berbagai kerangka dasar lainnya. Sekarang, saatnya kita melihat bagaimana semua potongan puzzle ini bekerja sama secara harmonis untuk menjawab satu pertanyaan fundamental: "Bagaimana sebuah perintah SQL dieksekusi?"

Bayangkan kita telah membuat mesin, roda, dan sasis mobil secara terpisah. Di bab ini, kita akan merakitnya menjadi satu, menyalakan mesin dengan kunci kontak (perintah SQL), dan melihatnya berjalan dari titik A ke titik B. Perjalanan ini, dari sebuah string teks sederhana hingga menjadi tabel hasil yang informatif, adalah inti dari setiap sistem database. Memahaminya berarti memahami jantung dari cara kerja database.

Di akhir bab ini, Anda akan dapat:

1. Menjelaskan alur hidup lengkap dari sebuah query `SELECT` dari awal hingga akhir.
    
2. Memahami peran krusial dari setiap komponen utama: `Parser`, `Optimizer`, `Query Engine`, `Buffer Pool`, dan `Storage Layer`.
    
3. Menganalisis arsitektur Wheel DB secara holistik.
    
4. Mengidentifikasi langkah-langkah selanjutnya untuk mengembangkan Wheel DB menjadi sistem yang lebih kuat dan kaya fitur.
    

---

## 8.1 Alur Hidup Sebuah Query: Dari Teks Hingga Hasil

Untuk memahami proses ini, kita akan mengikuti perjalanan sebuah query sederhana. Anggap saja kita memiliki tabel `users` dengan kolom `id`, `name`, dan `age`. Query yang akan menjadi studi kasus kita adalah:

```sql
SELECT id, name FROM users WHERE age = 30;
```

Perintah ini terlihat sederhana bagi manusia, tetapi bagi database, ini adalah serangkaian instruksi kompleks yang harus diurai, direncanakan, dan dieksekusi dengan efisien. Kita akan membedah setiap tahap yang dilalui query ini di dalam Wheel DB. Untuk memberikan gambaran besar, berikut adalah peta perjalanan yang akan kita telusuri.

| Tahap                | Komponen Utama                  | Input                | Output                              | Tujuan Utama                                     |
| -------------------- | ------------------------------- | -------------------- | ----------------------------------- | ------------------------------------------------ |
| 1. Parsing           | `Parser (ANTLR)`                | `String SQL`         | `Abstract Syntax Tree (AST)`        | Memahami tata bahasa dan struktur query.         |
| 2. Optimasi          | `Optimizer (Calcite)`           | `AST`                | `Logical Plan` (Relational Algebra) | Menganalisis query dan membuat rencana logis.    |
| 3. Perencanaan Fisik | `Planner (Calcite)`             | `Logical Plan`       | `Physical Plan (Pohon Operator)`    | Memilih cara terbaik untuk mengeksekusi rencana. |
| 4. Eksekusi          | `Query Engine`                  | `Pohon Operator`     | `Tuple` / Baris Data                | Menjalankan rencana dan mengambil data.          |
| 5. Akses Data        | `Buffer Pool` & `Storage Layer` | Permintaan `Page`    | `Page` berisi data                  | Mengambil data dari disk secara efisien.         |
| 6. Hasil             | `Client / CLI`                  | `Tuple` / Baris Data | Tampilan Hasil                      | Menyajikan data kepada pengguna.                 |

### Langkah 1: `String SQL` -> `Parser (ANTLR)` - Menerjemahkan Perintah

Tahap pertama adalah mengubah perintah SQL yang berupa teks mentah—sebuah `String`—menjadi struktur data yang dapat dipahami oleh mesin. Proses ini disebut _parsing_. Tanpa parsing, query `SELECT...` hanyalah sekumpulan karakter yang tidak berarti bagi database.

**Implementasi di `tiny-db`**

Membangun sebuah parser dari nol adalah tugas yang sangat rumit dan rentan kesalahan. Ini setara dengan menulis compiler untuk sebuah bahasa pemrograman. Oleh karena itu, proyek `tiny-db` mengambil pendekatan yang cerdas dan pragmatis: menggunakan _parser generator_ bernama **ANTLR** (ANother Tool for Language Recognition).1

ANTLR bekerja dengan cara membaca sebuah file definisi tata bahasa (grammar) dengan ekstensi `.g4`. File ini mendefinisikan semua aturan sintaksis SQL—apa saja kata kunci yang valid (`SELECT`, `FROM`, `WHERE`), bagaimana urutannya, di mana koma boleh diletakkan, dan seterusnya. Berdasarkan file `.g4` ini, ANTLR secara otomatis menghasilkan kode sumber Java untuk dua komponen penting:

1. **Lexer (Tokenizer):** Komponen ini adalah garda terdepan. Ia memindai string SQL mentah dan memecahnya menjadi potongan-potongan kecil yang disebut _token_. Untuk query kita, _lexer_ akan menghasilkan aliran token seperti: `SELECT`, `id`, `,`, `name`, `FROM`, `users`, `WHERE`, `age`, `=`, `30`, `;`.
    
2. **Parser:** Komponen ini menerima aliran token dari _lexer_. Tugasnya adalah memverifikasi apakah urutan token tersebut sesuai dengan aturan tata bahasa yang didefinisikan di file `.g4`. Jika urutannya valid, _parser_ akan membangun sebuah struktur data berbentuk pohon yang disebut **Abstract Syntax Tree (AST)**. Jika tidak valid (misalnya, `FROM SELCET...`), ia akan melaporkan kesalahan sintaks.
    

AST adalah representasi hierarkis dari struktur query. Ia menangkap esensi logis dari perintah SQL, mengabaikan detail-detail seperti spasi atau huruf besar/kecil. Untuk query `SELECT id, name FROM users WHERE age = 30;`, AST-nya secara konseptual akan terlihat seperti ini:

```
        SelectNode
        / | \
       / | \
Columns   From      Where

| | |
[id, name] TableNode(users) ConditionNode
                          / | \
                         / | \
                       Column   Op     Value

| | |
                        age     =       30
```

Pohon ini jauh lebih mudah untuk diolah oleh komponen database selanjutnya dibandingkan dengan string teks asli.

Satu hal yang sangat penting untuk dicatat adalah pilihan `tiny-db` untuk tidak menulis grammar SQL dari awal. Repositori tersebut menyebutkan penggunaan "ANTLR MySQL Parser (Apache ShardingSphere Parser Library)".1 Ini adalah sebuah keputusan desain yang sangat matang. Menulis grammar SQL yang komprehensif dan benar adalah pekerjaan monumental yang bisa memakan waktu berbulan-bulan atau bahkan bertahun-tahun. Dengan mengintegrasikan grammar yang sudah ada dan teruji dari proyek open-source lain yang matang seperti Apache ShardingSphere, pengembang dapat fokus pada masalah arsitektur database yang lebih inti. Ini adalah pelajaran rekayasa perangkat lunak yang berharga: manfaatkan alat dan pustaka yang sudah ada untuk membangun sistem yang kompleks secara efisien, daripada mencoba menciptakan kembali semuanya dari awal.

### Langkah 2 & 3: `AST` -> `Optimizer (Calcite)` -> `Pohon Operator` - Merancang Strategi Terbaik

Setelah database memahami _apa_ yang diminta oleh pengguna melalui AST, pertanyaan selanjutnya adalah _bagaimana_ cara terbaik untuk melakukannya. Untuk query kita, ada beberapa cara untuk mendapatkan hasilnya:

- Apakah kita harus membaca seluruh tabel `users` dari awal sampai akhir, baris per baris, dan memeriksa kolom `age` di setiap baris? Ini disebut _Full Table Scan_.
    
- Atau, apakah ada jalan pintas? Jika kita memiliki `index` pada kolom `age`, mungkin kita bisa langsung melompat ke data yang kita butuhkan tanpa memindai seluruh tabel. Ini disebut _Index Scan_.
    

Memilih strategi terbaik adalah tugas dari `Query Optimizer` dan `Planner`. Sekali lagi, ini adalah salah satu bagian paling kompleks dari sebuah sistem database. Oleh karena itu, Wheel DB, seperti halnya `tiny-db`, mendelegasikan tugas berat ini kepada sebuah kerangka kerja khusus bernama **Apache Calcite**.1

Calcite adalah sebuah fondasi perangkat lunak yang sangat kuat untuk membangun sistem pemrosesan data. Ia tidak peduli dari mana data berasal atau di mana data disimpan; fokus utamanya adalah pada parsing, validasi, dan, yang terpenting, optimasi query.

Proses di dalam Calcite berlangsung dalam beberapa tahap:

1. AST ke Logical Plan: Calcite mengambil AST yang dihasilkan oleh ANTLR dan mengubahnya menjadi representasi yang lebih formal yang disebut Logical Plan (Rencana Logis). Rencana ini biasanya diekspresikan dalam bentuk relational algebra, sebuah "bahasa" matematika untuk memanipulasi set data. Untuk query kita, rencana logisnya mungkin terlihat seperti ini:
    
    $Project(\pi)_{id, name} \rightarrow Filter(\sigma)_{age = 30} \rightarrow TableScan(users)$
    
    Ini dibaca dari kanan ke kiri: Lakukan scan pada tabel users, kemudian terapkan filter dengan kondisi age = 30, dan terakhir, lakukan project (ambil) hanya kolom id dan name. Rencana ini masih logis karena belum menentukan bagaimana TableScan atau Filter akan diimplementasikan secara fisik.
    
2. **Optimasi Berbasis Aturan (Rule-Based Optimization):** Calcite kemudian menerapkan serangkaian aturan transformasi pada rencana logis ini untuk mengubahnya menjadi rencana lain yang ekuivalen tetapi berpotensi lebih efisien. Proyek `tiny-db` secara spesifik menyebutkan penggunaan "Rule Based Planners".1 Salah satu aturan optimasi yang paling umum adalah
    
    _Predicate Pushdown_. Aturan ini bertujuan untuk "mendorong" operasi filter (`WHERE`) sedekat mungkin ke sumber data. Tujuannya adalah untuk mengurangi jumlah data yang harus diproses di tahap-tahap selanjutnya. Dalam kasus sederhana kita, rencana logisnya sudah optimal karena filter sudah berada tepat setelah scan.
    
3. **Logical Plan ke Physical Plan (Pohon Operator):** Setelah rencana logis dioptimalkan, `Planner` di dalam Calcite mengubahnya menjadi satu atau lebih _Physical Plan_ (Rencana Fisik). Di sinilah pertimbangan dunia nyata, seperti keberadaan `index`, masuk. Jika `Planner` mengetahui bahwa ada `B+Tree` index pada kolom `age`, ia akan menghasilkan setidaknya dua kemungkinan rencana fisik:
    
    - **Rencana A (Full Table Scan):** Menggunakan operator fisik `TableScan` untuk membaca seluruh tabel.
        
    - **Rencana B (Index Scan):** Menggunakan operator fisik `IndexScan` yang memanfaatkan `B+Tree` untuk secara efisien menemukan hanya baris-baris yang memiliki `age = 30`.
        
4. **Pemilihan Rencana:** `Optimizer` kemudian harus memilih rencana fisik terbaik. Dalam sistem database sederhana seperti `tiny-db`, logikanya mungkin sangat simpel: "jika ada index yang bisa digunakan untuk klausa `WHERE`, selalu gunakan itu".1 Di sistem database yang lebih canggih,
    
    `Optimizer` akan menggunakan _Cost-Based Optimization_. Setiap operator fisik (seperti `TableScan` atau `IndexScan`) diberi "biaya" estimasi berdasarkan statistik tentang data (misalnya, berapa banyak baris dalam tabel, seberapa unik nilai dalam kolom, dll.). `Optimizer` kemudian akan menghitung total biaya untuk setiap rencana yang mungkin dan memilih yang termurah.
    

Rencana fisik yang akhirnya terpilih inilah yang disebut **Pohon Operator** (_Operator Tree_ atau _Execution Plan_). Ini adalah cetak biru langkah-demi-langkah yang akan dieksekusi oleh `Query Engine`.

### Langkah 4: `Pohon Operator` -> `Eksekusi oleh Query Engine` - Menjalankan Rencana

`Query Engine` adalah komponen yang bertindak sebagai "mandor" proyek. Ia mengambil `Pohon Operator` dari `Optimizer` dan benar-benar menjalankannya untuk menghasilkan data.

Sebagian besar `Query Engine` modern, termasuk yang diimplementasikan secara konseptual di `tiny-db`, menggunakan model eksekusi yang elegan yang dikenal sebagai **Volcano Model** atau **Iterator Model**. Dalam model ini, setiap node dalam `Pohon Operator` diimplementasikan sebagai sebuah _iterator_. Setiap iterator memiliki method `next()` yang, ketika dipanggil, akan menghasilkan satu baris data (sebuah _tuple_).

Cara kerjanya adalah data "ditarik" (_pulled_) ke atas melalui pohon, bukan "didorong" (_pushed_) dari bawah. Mari kita telusuri eksekusi untuk Rencana B (`IndexScan`):

1. Aplikasi klien (misalnya, CLI) memulai proses dengan memanggil `next()` pada operator paling atas, yaitu `Project`.
    
2. Operator `Project` tidak memiliki data sendiri. Untuk menghasilkan satu baris, ia perlu mendapatkan satu baris dari operator di bawahnya. Jadi, ia memanggil `next()` pada operator `IndexScan`.
    
3. Operator `IndexScan` adalah operator daun. Tugasnya adalah mengambil data dari lapisan penyimpanan. Ia akan menggunakan `B+Tree` index pada kolom `age` untuk menemukan _tuple_ atau baris pertama yang cocok dengan kondisi `age = 30`.
    
4. `IndexScan` kemudian mengembalikan _tuple_ lengkap ini (misalnya, `{id: 101, name: 'Alice', age: 30}`) ke atas, kepada pemanggilnya, yaitu operator `Project`.
    
5. Operator `Project` menerima _tuple_ lengkap ini. Sesuai dengan tugasnya, ia hanya akan mengambil kolom `id` dan `name`, membuat _tuple_ baru (misalnya, `{id: 101, name: 'Alice'}`), dan mengembalikannya sebagai hasil dari panggilan `next()` di Langkah 1.
    
6. Aplikasi klien menerima baris pertama dan menampilkannya. Kemudian, ia memanggil `next()` lagi pada operator `Project` untuk meminta baris berikutnya.
    
7. Proses ini berulang: `Project` memanggil `next()` pada `IndexScan`. `IndexScan` melanjutkan pencariannya di `B+Tree` untuk menemukan baris berikutnya yang cocok. Baris itu mengalir ke atas, diproyeksikan, dan dikembalikan ke klien.
    
8. Ketika `IndexScan` tidak dapat menemukan baris lain yang cocok, panggilan `next()`-nya akan mengembalikan `null` (atau penanda akhir data lainnya). Ketika `Project` menerima `null` ini, ia tahu bahwa tidak ada lagi data yang bisa diproses, jadi ia juga akan mengembalikan `null` ke klien.
    
9. Aplikasi klien, setelah menerima `null`, tahu bahwa query telah selesai dieksekusi.
    

Model iterator ini sangat kuat karena memungkinkan pemrosesan data secara _streaming_. Database tidak perlu memuat seluruh hasil query ke dalam memori sebelum menampilkannya. Setiap baris diproses dan dikirim satu per satu, membuat penggunaan memori menjadi sangat efisien.

### Langkah 5 & 6: `Query Engine` -> `Buffer Pool` -> `Storage Layer` - Interaksi dengan Data Fisik

Sekarang kita sampai pada lapisan terendah dari perjalanan query. Bagaimana operator daun seperti `TableScan` atau `IndexScan` sebenarnya mendapatkan data? Mereka tidak membaca file dari disk secara langsung. Sebaliknya, mereka berinteraksi dengan dua lapisan abstraksi yang sangat penting: `Storage Layer` dan `Buffer Pool`.

**`Storage Layer`**

Di `tiny-db`, `Storage Layer` (atau `Storage Engine`) terdiri dari komponen-komponen seperti `File Manager`, `Block`, dan `Page`.1 Lapisan ini bertanggung jawab atas semua detail kotor tentang bagaimana data diatur di dalam file di disk. Ia menyembunyikan kompleksitas sistem file dari seluruh bagian database lainnya. Konsep utamanya adalah

`Page`, yaitu unit transfer data berukuran tetap (misalnya, 4KB atau 8KB) antara disk dan memori. Seluruh database, dari tabel hingga `index`, disimpan sebagai kumpulan `Page`.

**`Buffer Pool` (atau `Buffer Manager`)**

Membaca data dari disk adalah salah satu operasi paling lambat di dalam komputer, bisa ribuan kali lebih lambat daripada mengakses data dari memori utama (RAM). Jika `Query Engine` harus pergi ke disk setiap kali membutuhkan sebuah `Page`, performa database akan sangat buruk.

Di sinilah `Buffer Pool` berperan. `Buffer Pool` adalah area di dalam memori utama yang berfungsi sebagai _cache_ untuk `Page` data. Ketika `IndexScan` membutuhkan `Page` tertentu (misalnya, `Page` nomor 123 dari file tabel `users`), alur kerjanya adalah sebagai berikut:

1. `IndexScan` meminta `Page` 123 dari `Buffer Pool`.
    
2. `Buffer Pool` memeriksa apakah `Page` 123 sudah ada di dalam cache memorinya.
    
    - **Cache Hit:** Jika ya, `Buffer Pool` akan langsung mengembalikan referensi ke `Page` tersebut di memori. Ini adalah operasi yang sangat cepat.
        
    - **Cache Miss:** Jika tidak, `Buffer Pool` harus mengambilnya dari disk. Ia akan meminta `Storage Layer` untuk membaca `Page` 123 dari file yang sesuai. `Storage Layer` melakukan I/O disk, dan `Page` tersebut dimuat ke dalam salah satu slot kosong (atau _frame_) di `Buffer Pool`. Setelah `Page` berada di memori, `Buffer Pool` mengembalikannya ke `IndexScan`. Ini adalah operasi yang jauh lebih lambat.
        

Penting untuk dicatat bahwa `README.md` dari proyek `tiny-db` secara eksplisit mencantumkan `Buffer Manager` sebagai item **TODO**.1 Ini adalah sebuah detail yang sangat signifikan. Tanpa

`Buffer Manager`, setiap permintaan `Page` oleh `Query Engine` kemungkinan besar akan langsung diterjemahkan menjadi operasi I/O disk oleh `Storage Layer`. Ini adalah skenario yang sangat tidak efisien dan akan membuat database menjadi sangat lambat, terutama jika data yang sama diakses berulang kali.

Namun, ketiadaan fitur ini justru menjadi sebuah kesempatan belajar yang luar biasa. Dengan memahami alur yang tidak efisien ini, kita dapat melihat dengan jelas _mengapa_ `Buffer Pool` adalah salah satu komponen paling fundamental untuk performa dalam arsitektur database. Ini mengubah "fitur yang hilang" menjadi sebuah pelajaran konkret tentang pentingnya caching dan manajemen memori dalam sistem database. Di bagian selanjutnya, implementasi `Buffer Manager` akan menjadi prioritas utama untuk pengembangan lebih lanjut.

### Langkah 7: `Hasil` - Penyajian Data

Setelah `Query Engine`, melalui interaksi dengan `Buffer Pool` dan `Storage Layer`, selesai mengeksekusi seluruh `Pohon Operator`, serangkaian _tuple_ hasil telah diproduksi.

Tuple-tuple ini, yang masing-masing hanya berisi kolom `id` dan `name`, dikirim kembali ke komponen yang awalnya memanggil query. Dalam kasus `tiny-db`, ini adalah "CLI interface" (Command-Line Interface).1

CLI kemudian mengambil aliran _tuple_ ini dan memformatnya menjadi tabel yang rapi dan mudah dibaca, lengkap dengan header kolom, dan menampilkannya kepada pengguna di terminal. Pada titik ini, perjalanan panjang sebuah query, dari string teks sederhana hingga menjadi informasi yang berguna, telah selesai.

---

## 8.2 Rangkuman Proyek dan Visi ke Depan

Setelah menelusuri alur hidup sebuah query, sekarang adalah saat yang tepat untuk mundur sejenak dan melihat gambaran besar dari apa yang telah kita bangun dengan Wheel DB, serta ke mana kita bisa melangkah dari sini.

### Mereview Arsitektur Wheel DB

Kita telah berhasil membangun sebuah sistem database relasional mini yang fungsional dari awal. Arsitektur yang kita rancang, yang terinspirasi oleh `tiny-db`, mencerminkan prinsip-prinsip desain sistem database modern. Mari kita tinjau kembali komponen-komponen utamanya:

- **Frontend (Parser & Optimizer):** Kita tidak menciptakan parser dan optimizer dari nol. Sebaliknya, kita memanfaatkan kekuatan ANTLR untuk parsing dan Apache Calcite untuk optimasi. Pendekatan ini memisahkan logika "apa yang harus dilakukan" (SQL) dari "bagaimana melakukannya" (eksekusi).
    
- **Query Engine:** Komponen eksekusi kita menggunakan model iterator (Volcano Model) yang efisien dalam penggunaan memori untuk memproses data.
    
- **Indexing:** Kita mengimplementasikan `B+Tree` sebagai struktur data `index` untuk mempercepat pencarian. Keberadaan `index` ini secara cerdas dipertimbangkan oleh `Optimizer` Calcite saat membuat rencana eksekusi.
    
- **Storage Engine:** Di lapisan paling bawah, kita memiliki `Storage Engine` yang mengelola persistensi data di disk menggunakan abstraksi `Page`, `Block`, dan `File Manager`.
    

Arsitektur ini, terutama pemisahan yang jelas antara `Frontend` (yang menangani optimasi query) dan `Storage Engine` (yang menangani penyimpanan data), adalah cerminan dari arsitektur sistem data modern yang lebih besar. Sistem seperti Presto, Trino, atau Spark SQL dirancang dengan filosofi yang sama. Mereka memiliki _query engine_ yang sangat canggih yang dapat "berbicara" dengan berbagai macam _backend_ penyimpanan, seperti HDFS, Amazon S3, atau bahkan database relasional lainnya. Dengan menyelesaikan proyek Wheel DB, Anda tidak hanya belajar cara kerja database monolitik tradisional. Anda secara tidak langsung telah mempelajari prinsip desain _decoupled architecture_ yang mendasari banyak alat Big Data paling populer saat ini. Ini mengubah proyek dari sekadar "membuat database mainan" menjadi "membangun prototipe dari arsitektur _query engine_ modern."

### Membahas Potensi Pengembangan Lebih Lanjut

Proyek Wheel DB adalah fondasi yang kokoh, tetapi seperti bangunan apa pun, selalu ada ruang untuk perbaikan dan penambahan. Daftar `TODO` dari proyek referensi kita, `tiny-db`, memberikan peta jalan yang sangat baik untuk fitur-fitur selanjutnya yang akan mengubah Wheel DB dari proyek edukasi menjadi sistem yang lebih kuat dan andal.1

1. **Implementasi `Buffer Manager` (Prioritas Performa):** Seperti yang telah kita bahas secara mendalam, ini adalah peningkatan paling krusial untuk performa. Mengimplementasikan `Buffer Pool` dengan kebijakan penggantian (_replacement policy_) yang cerdas, seperti LRU (Least Recently Used), akan secara drastis mengurangi I/O disk yang mahal dan mempercepat hampir semua query.
    
2. **`Recovery Manager` dengan `WAL` (Prioritas Durabilitas):** Apa yang terjadi jika listrik mati atau aplikasi _crash_ saat database sedang menulis data ke file? Ada kemungkinan file data menjadi korup dan tidak konsisten. `Recovery Manager` adalah komponen yang memastikan _durabilitas_ data. Teknik yang paling umum digunakan adalah **Write-Ahead Logging (WAL)**. Sebelum perubahan apa pun ditulis ke `Page` data utama di disk, perubahan tersebut pertama-tama dicatat dalam sebuah file log sekuensial. Jika terjadi _crash_, saat database dinyalakan kembali, `Recovery Manager` akan membaca log ini dan "memutar ulang" atau "membatalkan" perubahan yang belum selesai, memastikan database kembali ke keadaan yang konsisten.
    
3. **`Transactions` dan `Concurrency Manager` (Prioritas Multi-User):** Saat ini, Wheel DB hanya aman digunakan oleh satu pengguna pada satu waktu (_single-user_). Jika dua pengguna mencoba mengubah data yang sama secara bersamaan, hasilnya bisa kacau (misalnya, _lost updates_). Untuk mendukung banyak pengguna secara bersamaan (_multi-user_), kita memerlukan `Concurrency Manager`. Komponen ini mengelola **transaksi** dan mengimplementasikan mekanisme _locking_ (atau teknik lain seperti MVCC) untuk memastikan properti **ACID** (Atomicity, Consistency, Isolation, Durability). Ini memastikan bahwa transaksi-transaksi yang berjalan bersamaan tidak saling mengganggu.
    
4. **Tipe Data yang Lebih Kompleks dan Fitur SQL:** Saat ini, `tiny-db` memiliki batasan hanya mendukung tipe data `Varchar` dan `int`.1 Langkah alami selanjutnya adalah memperluas dukungan untuk tipe data lain seperti
    
    `FLOAT`, `DOUBLE`, `DATE`, `TIMESTAMP`, `BOOLEAN`, dll. Seiring dengan itu, kita juga perlu memperluas kemampuan `Parser` (dengan memodifikasi grammar ANTLR) dan `Query Engine` untuk mendukung operasi SQL yang lebih kompleks, terutama `JOIN` antar tabel, `GROUP BY` untuk agregasi, dan fungsi-fungsi agregat seperti `COUNT()`, `SUM()`, dan `AVG()`.
    

---

## Kesimpulan - Fondasi Seorang Arsitek Database

Melalui proyek Wheel DB, kita tidak hanya sekadar menulis kode; kita telah menjelajahi ide-ide fundamental yang telah membentuk dunia data selama beberapa dekade. Dari bagaimana byte diatur dalam sebuah file di level terendah, bagaimana `B+Tree` secara efisien mengorganisir data untuk pencarian cepat, hingga bagaimana `Optimizer` secara cerdas merancang strategi untuk menjawab pertanyaan di level tertinggi. Anda kini memiliki pemahaman langsung tentang tantangan dan solusi elegan yang ada dalam membangun sebuah sistem database.

Pengetahuan ini adalah fondasi Anda. Ketika Anda sekarang menggunakan PostgreSQL, MySQL, atau bahkan sistem data terdistribusi seperti Snowflake atau BigQuery, Anda tidak lagi melihatnya sebagai sebuah "kotak hitam" misterius. Anda akan memiliki intuisi tentang apa yang terjadi di balik layar ketika Anda menulis sebuah query. Anda akan mengerti mengapa beberapa query berjalan lambat dan yang lain cepat, mengapa `index` bisa membuat perbedaan yang dramatis, dan bagaimana transaksi menjaga data tetap aman. Anda telah mengambil langkah pertama dari seorang pengguna database menjadi seorang arsitek database. Perjalanan ini baru saja dimulai.