# Bab 7: Parsing & Optimisasi Query

## Pendahuluan: Membangun Otak Database Wheel DB

Selamat datang di Bab 7. Pada bab-bab sebelumnya, kita telah membangun komponen fundamental dari Wheel DB, yaitu "tangan" dan "kaki" dari sistem kita: _Query Engine_ yang mampu mengeksekusi perintah-perintah sederhana dan _Storage Engine_ yang bertugas menyimpan dan mengambil data secara fisik dari disk. Kini, saatnya kita membangun komponen yang paling krusial dan cerdas: "otak" dari database kita.

Bab ini akan mengupas tuntas proses ajaib yang terjadi di balik layar ketika sebuah _query_ SQL dieksekusi. Kita akan menelusuri perjalanan sebuah string teks sederhana, seperti `SELECT nama FROM user WHERE umur > 25;`, dari bentuk mentahnya hingga menjadi sebuah rencana eksekusi (_execution plan_) yang sangat efisien dan bisa dimengerti oleh mesin. Proses ini terbagi menjadi dua tahap utama yang fundamental:

1. **Parsing (Penguraian):** Tahap di mana database berusaha memahami _apa_ yang diinginkan oleh pengguna. Ini melibatkan pemeriksaan tata bahasa (sintaksis) dari _query_ SQL dan mengubahnya menjadi sebuah struktur data yang terorganisir.
    
2. **Optimization (Optimisasi):** Setelah memahami keinginan pengguna, database harus menentukan _cara terbaik_ untuk memenuhi permintaan tersebut. Tahap ini melibatkan analisis berbagai strategi untuk mengambil data dan memilih satu yang paling efisien dalam hal waktu dan sumber daya.
    

Untuk membangun otak yang canggih ini, kita tidak akan memulai dari nol. Sebagaimana proyek `tiny-db` yang menjadi referensi kita, Wheel DB akan berdiri di atas pundak raksasa dengan mengintegrasikan dua _framework open-source_ yang sangat kuat dan telah teruji di industri 1:

- **ANTLR (ANother Tool for Language Recognition):** Sebuah _parser generator_ yang akan kita gunakan untuk memahami dan menguraikan bahasa SQL.
    
- **Apache Calcite:** Sebuah _framework_ optimisasi _query_ yang fleksibel dan komprehensif, yang sering disebut sebagai "database brain-in-a-box".2
    

Dengan menyelesaikan bab ini, Anda akan memiliki pemahaman mendalam tentang bagaimana sebuah sistem database modern mengubah perintah deklaratif (memberi tahu _apa_ yang diinginkan) menjadi sebuah rencana prosedural yang imperatif (langkah-langkah _bagaimana_ melakukannya), sebuah fondasi penting dalam ilmu sistem manajemen data.

---

## 7.1 Peran Parser: Memahami Bahasa SQL

Sebelum sebuah database dapat menjalankan sebuah perintah, ia harus terlebih dahulu memahaminya. Sama seperti manusia yang harus mengerti arti sebuah kalimat sebelum bisa mengikuti instruksi, database memerlukan sebuah komponen bernama **Parser** untuk menafsirkan _query_ SQL. Tugas utama Parser adalah untuk memvalidasi apakah _query_ yang diberikan sesuai dengan aturan tata bahasa SQL, dan jika valid, mengubahnya dari sebatas string teks menjadi representasi data terstruktur yang disebut **Parse Tree** atau **Abstract Syntax Tree (AST)**.

Proses ini sendiri umumnya terbagi menjadi dua sub-proses yang berurutan:

### Pipeline Parsing: Lexer dan Parser

1. **Analisis Leksikal (Lexical Analysis):** Langkah pertama ini dilakukan oleh komponen yang disebut **Lexer** (atau _tokenizer_). Tugas Lexer adalah memecah string SQL mentah menjadi serangkaian "token" atau unit leksikal—mirip dengan memecah kalimat menjadi kata-kata. Sebagai contoh, untuk _query_ `SELECT nama FROM user;`, Lexer akan menghasilkan urutan token seperti: `SELECT` (keyword), `nama` (identifier), `FROM` (keyword), `user` (identifier), dan `;` (terminator).
    
2. **Analisis Sintaksis (Syntactic Analysis):** Setelah mendapatkan aliran token dari Lexer, **Parser** akan mengambil alih. Parser memeriksa apakah urutan token ini membentuk sebuah "kalimat" yang valid sesuai dengan aturan tata bahasa (grammar) SQL. Misalnya, aturan mungkin menyatakan bahwa _keyword_ `SELECT` harus diikuti oleh daftar kolom, diikuti oleh _keyword_ `FROM`, dan seterusnya. Jika urutan token valid, Parser akan menyusunnya menjadi sebuah struktur data hierarkis (pohon), yaitu AST, yang merepresentasikan struktur logis dari _query_ tersebut.3 Jika tidak valid (misalnya,
    
    `SELECT FROM user nama;`), Parser akan melaporkan _syntax error_.
    

### Pengenalan ANTLR: Sang Generator Parser

Menulis Lexer dan Parser dari nol secara manual untuk bahasa sekompleks SQL adalah pekerjaan yang sangat sulit, memakan waktu, dan rentan terhadap kesalahan. Oleh karena itu, kita menggunakan sebuah _tool_ bernama **ANTLR (ANother Tool for Language Recognition)**, yang merupakan sebuah _parser generator_.3

Cara kerja ANTLR sangat efisien. Kita cukup mendefinisikan tata bahasa (grammar) dari bahasa yang ingin kita proses dalam sebuah file khusus dengan ekstensi `.g4`. File grammar ini berisi aturan-aturan formal yang mendeskripsikan sintaksis SQL. Setelah itu, kita menjalankan _tool_ ANTLR, yang akan membaca file `.g4` ini dan secara otomatis menghasilkan kode sumber (dalam kasus kita, file Java) untuk Lexer dan Parser yang sesuai.5

Namun, di sinilah proyek `tiny-db` dan Wheel DB membuat sebuah keputusan rekayasa perangkat lunak yang sangat cerdas dan pragmatis. Menulis grammar SQL yang lengkap dan benar adalah sebuah proyek masif tersendiri. SQL memiliki banyak dialek (MySQL, PostgreSQL, Oracle) dengan fitur dan sintaksis yang sedikit berbeda. Alih-alih mencoba menulis grammar ini dari awal, `tiny-db` memanfaatkan parser yang sudah matang dan teruji dari proyek **Apache ShardingSphere**.1 ShardingSphere adalah sebuah ekosistem middleware database terdistribusi yang juga menggunakan ANTLR untuk membangun parser SQL-nya yang sangat kuat dan mendukung banyak dialek.7

Dengan menggunakan _library_ parser dari ShardingSphere, kita mengadopsi prinsip "berdiri di atas pundak raksasa". Kita tidak perlu membuang waktu untuk menyelesaikan masalah yang sudah diselesaikan dengan baik oleh orang lain. Sebaliknya, kita bisa fokus pada aspek-aspek unik dari Wheel DB, seperti _query engine_ dan _storage engine_. Tugas kita pun bergeser dari _menciptakan_ parser menjadi _mengintegrasikan_ parser yang sudah ada.

Hasil akhir dari tahap ini adalah sebuah **Abstract Syntax Tree (AST)**. AST ini adalah representasi terstruktur dari _query_ SQL yang siap untuk diproses lebih lanjut oleh komponen berikutnya: sang Optimizer.

---

## 7.2 Peran Optimizer: Mencari Jalan Tercepat

Setelah Parser berhasil menerjemahkan _query_ SQL menjadi sebuah AST, database kini tahu _apa_ yang diinginkan pengguna. Langkah selanjutnya adalah tugas **Optimizer**, yaitu untuk mencari tahu _cara terbaik dan tercepat_ untuk mendapatkan data tersebut. Untuk _query_ sederhana, mungkin hanya ada satu cara eksekusi. Namun, untuk _query_ yang lebih kompleks, terutama yang melibatkan `JOIN` antar beberapa tabel, bisa ada puluhan atau bahkan ribuan "rencana eksekusi" (_execution plan_) yang berbeda. Peran Optimizer adalah untuk mengevaluasi berbagai alternatif ini dan memilih rencana yang paling efisien—yaitu yang paling cepat dieksekusi dengan penggunaan sumber daya (CPU, I/O disk) yang paling minimal.

### Pengenalan Apache Calcite: Otak Database Siap Pakai

Untuk tugas optimisasi ini, kita akan menggunakan **Apache Calcite**, sebuah _framework_ canggih yang menyediakan fondasi untuk membangun sistem pemrosesan data.1 Calcite bersifat independen dari

_storage_ dan sangat fleksibel, membuatnya menjadi pilihan populer untuk banyak proyek data besar seperti Apache Hive, Flink, dan Druid.9 Calcite menyediakan semua komponen yang kita butuhkan untuk optimisasi: sebuah sistem untuk merepresentasikan

_query_ dalam bentuk aljabar relasional, sebuah _engine_ berbasis aturan (_rule-based_) untuk mentransformasi _query_, dan sebuah model biaya (_cost model_) untuk membandingkan efisiensi dari berbagai rencana eksekusi.10

### Konsep Inti: Logical Plan vs. Physical Plan

Konsep paling fundamental dalam optimisasi _query_ adalah perbedaan antara **Logical Plan** dan **Physical Plan**. Memahami perbedaan ini adalah kunci untuk mengerti cara kerja Optimizer.

- **Logical Plan:** Merepresentasikan **apa** yang harus dilakukan, yaitu semantik atau logika dari _query_. Ini adalah representasi abstrak tingkat tinggi dari operasi-operasi yang diperlukan, tanpa memikirkan bagaimana operasi tersebut akan diimplementasikan secara fisik. Operator dalam _logical plan_ bersifat abstrak, seperti `LogicalTableScan` (baca seluruh tabel), `LogicalFilter` (terapkan kondisi filter), atau `LogicalJoin` (gabungkan dua tabel). Rencana ini independen dari _storage engine_ atau algoritma spesifik yang akan digunakan.11
    
- **Physical Plan:** Merepresentasikan **bagaimana** cara melakukannya, yaitu implementasi konkret dari _logical plan_. Rencana ini sangat detail dan spesifik, menentukan algoritma dan metode akses data yang akan digunakan oleh _query engine_. Operator dalam _physical plan_ bersifat konkret, seperti `IndexScan` (baca tabel menggunakan B+Tree index), `TableScan` (baca tabel secara sekuensial), `HashJoin` (menggunakan algoritma hash join), atau `SortMergeJoin` (menggunakan algoritma sort-merge join). Rencana ini sangat bergantung pada kapabilitas _engine_ dan karakteristik fisik data (misalnya, ketersediaan indeks).11
    

Tabel berikut merangkum perbedaan utama antara keduanya:

|Aspek|Logical Plan|Physical Plan|
|---|---|---|
|**Fokus**|Semantik & Logika (APA yang harus dilakukan)|Implementasi & Algoritma (BAGAIMANA cara melakukannya)|
|**Tingkat Abstraksi**|Tinggi. Independen dari _storage engine_.|Rendah. Spesifik untuk _storage engine_ dan kapabilitasnya.|
|**Contoh Operator**|`LogicalJoin`, `LogicalFilter`, `LogicalScan`|`HashJoin`, `SortMergeJoin`, `IndexScan`, `TableScan`|
|**Tujuan**|Merepresentasikan _query_ secara akurat dan benar.|Menghasilkan rencana eksekusi yang paling efisien (biaya terendah).|
|**Contoh**|"Join tabel A dan B, lalu filter hasilnya."|"Gunakan Broadcast Hash Join untuk A dan B, lalu filter."|

### Bagaimana Calcite Melakukan Optimisasi

Proses optimisasi di Calcite adalah sebuah sistem berbasis aturan (_rule-based_). Calcite mengambil _logical plan_ awal yang dihasilkan dari AST, lalu menerapkan ratusan aturan transformasi untuk menghasilkan banyak variasi _logical plan_ lain yang secara semantik ekuivalen. Sebagai contoh, sebuah aturan yang sangat umum adalah _predicate pushdown_. Aturan ini akan "mendorong" operasi `FILTER` (klausa `WHERE`) sedekat mungkin dengan sumber data. Melakukan filter lebih awal akan secara drastis mengurangi jumlah data yang perlu diproses oleh operasi berikutnya seperti `JOIN`, sehingga meningkatkan performa secara keseluruhan.13

Setelah menghasilkan berbagai kemungkinan rencana, Calcite menggunakan _cost model_ untuk memperkirakan "biaya" (estimasi penggunaan CPU, I/O, dan memori) dari setiap rencana fisik yang mungkin. Berdasarkan estimasi biaya ini, Calcite akan memilih rencana dengan biaya terendah. Inilah cara Calcite secara cerdas dapat memutuskan kapan harus menggunakan sebuah indeks, urutan `JOIN` mana yang terbaik, atau algoritma `JOIN` mana yang paling sesuai untuk situasi tertentu.

---

## 7.3 Integrasi ANTLR dan Calcite: Alur Kerja Lengkap

Sekarang kita akan menyatukan semua kepingan puzzle. Kita memiliki parser berbasis ANTLR dari ShardingSphere yang menghasilkan AST, dan kita memiliki Apache Calcite yang mampu mengoptimalkan sebuah rencana _query_. Bagian ini akan merinci alur kerja _end-to-end_ yang mengubah sebuah string SQL mentah menjadi sebuah _physical plan_ yang siap dieksekusi oleh _Query Engine_ kita.

Arsitektur yang digunakan dalam `tiny-db` (dan juga sistem data modern lainnya seperti Hive 9) bukanlah sebuah pipeline monolitik. Sebaliknya, ia terdiri dari tahapan-tahapan yang terpisah (

_decoupled_). Parser (ANTLR/ShardingSphere) hanya bertanggung jawab pada sintaksis dan menghasilkan AST. Optimizer (Calcite) hanya bertanggung jawab pada perencanaan dan optimisasi. Pemisahan ini adalah sebuah pola desain yang sangat kuat. Ini berarti kita bisa mengembangkan atau bahkan mengganti komponen secara independen. Misalnya, jika suatu saat Wheel DB ingin mendukung dialek PostgreSQL, secara teori kita bisa mengganti parser MySQL dari ShardingSphere dengan parser PostgreSQL tanpa harus mengubah logika optimisasi di Calcite. Modularitas ini mengurangi kompleksitas dan membuat sistem lebih mudah dipelihara, sebuah pelajaran arsitektur perangkat lunak yang sangat berharga.

Alur kerja lengkapnya dapat dipecah menjadi lima langkah utama:

#### 1. SQL String Masuk

Proses dimulai ketika sistem menerima sebuah query SQL sebagai input string mentah.

Contoh: SELECT A, B FROM T2 WHERE A=1;

#### 2. ANTLR Mem-parsing String menjadi Struktur Data (AST)

String SQL ini kemudian diteruskan ke _library_ parser Apache ShardingSphere. _Library_ ini, yang di dalamnya menggunakan parser yang dihasilkan oleh ANTLR, akan melakukan analisis leksikal dan sintaksis. Jika sintaksis _query_ valid, hasilnya adalah sebuah **Abstract Syntax Tree (AST)**. Jika ada kesalahan (misalnya, salah ketik `SELCET`), proses akan gagal di tahap ini dengan pesan _syntax error_.

#### 3. Struktur Data (AST) Diubah menjadi _Logical Plan_ oleh Calcite

Ini adalah langkah integrasi yang paling penting dan menantang. AST yang dihasilkan oleh parser ShardingSphere dan struktur data _logical plan_ yang dipahami oleh Calcite (`RelNode`) adalah dua hal yang berbeda dari dua _library_ yang berbeda. Tidak ada konversi otomatis di antara keduanya. Di sinilah kita harus menulis kode "jembatan" (_bridge_ atau _glue code_) untuk menerjemahkan satu format ke format lainnya.

Pola desain yang paling cocok untuk tugas ini adalah **Visitor Pattern**.5 Kita akan membuat sebuah

_class_ Visitor yang akan "berjalan" melintasi setiap _node_ di AST yang dihasilkan parser. Visitor ini akan memiliki metode-metode spesifik seperti `visitSelectClause`, `visitFromClause`, `visitWhereClause`, dan seterusnya. Di dalam setiap metode ini, kita akan secara terprogram membuat _node_ Calcite yang sesuai.

- Saat mengunjungi _node_ `SELECT`, kita membuat `LogicalProject` di Calcite.
    
- Saat mengunjungi _node_ `FROM`, kita membuat `LogicalTableScan`.
    
- Saat mengunjungi _node_ `WHERE`, kita membuat `LogicalFilter`.
    

Lapisan penerjemah ini adalah inti dari pekerjaan pengembangan di bab ini. Di sinilah semantik _query_ yang ditangkap oleh parser secara formal diekspresikan dalam "bahasa" yang dimengerti oleh optimizer. Hasil dari langkah ini adalah sebuah pohon _logical plan_ yang terdiri dari objek-objek `RelNode` Calcite.

#### 4. Calcite Mengoptimalkan _Logical Plan_ menjadi _Physical Plan_

Pohon `RelNode` yang merepresentasikan _logical plan_ kini diserahkan kepada _planner_ Calcite (misalnya, `VolcanoPlanner` 15).

_Planner_ akan menerapkan serangkaian aturan optimisasi untuk mentransformasi pohon tersebut. Ia akan menjelajahi ruang pencarian dari berbagai rencana alternatif, berkonsultasi dengan _cost model_ untuk memperkirakan efisiensinya, dan akhirnya menghasilkan pohon `RelNode` baru yang merepresentasikan **physical plan** terbaik.

Untuk _query_ contoh kita, `SELECT A, B FROM T2 WHERE A=1;`, jika ada sebuah B+Tree _index_ pada kolom `A` di tabel `T2`, optimizer Calcite kemungkinan besar akan mengubah operator `LogicalTableScan` menjadi operator fisik `IndexScan`. Keputusan ini diambil karena mengakses data melalui _index_ (jika predikatnya sangat selektif) jauh lebih murah daripada memindai seluruh tabel.

#### 5. _Physical Plan_ Dieksekusi oleh Query Engine

_Physical plan_ yang telah dioptimalkan adalah hasil akhir dari bab ini. Rencana ini pada dasarnya adalah sebuah pohon operator yang dapat dieksekusi, di mana setiap _node_ merepresentasikan sebuah operasi fisik yang harus dilakukan oleh _Query Engine_ (yang telah kita bangun di Bab 5). Eksekusi biasanya dimulai dari _node_ akar pohon. _Node_ akar akan meminta data dari _node-node_ anaknya, yang kemudian meminta data dari anak-anak mereka, dan seterusnya, hingga mencapai _node-node_ daun (_leaf nodes_), yang biasanya merupakan operator _scan_ (`TableScan` atau `IndexScan`) yang berinteraksi langsung dengan _Storage Engine_ untuk membaca data dari disk.

---

## Kesimpulan Bab 7 dan Langkah Selanjutnya

Dalam bab ini, kita telah berhasil merancang dan membangun komponen paling cerdas dari Wheel DB: "otaknya". Kita telah menciptakan sebuah alur kerja yang canggih yang mampu menerima _query_ SQL standar, memahami maknanya, memvalidasi kebenarannya, dan yang terpenting, menyusun strategi yang sangat efisien untuk eksekusinya. Pencapaian ini tidak kita raih dengan menciptakan semuanya dari awal, melainkan dengan secara cerdas mengintegrasikan _framework_ yang kuat dan menjadi standar industri, yaitu ANTLR (melalui Apache ShardingSphere) dan Apache Calcite.

Keputusan untuk mengintegrasikan Apache Calcite bukan hanya solusi teknis untuk masalah optimisasi, tetapi juga merupakan pilihan arsitektur strategis yang membuka banyak potensi di masa depan. Calcite dirancang sebagai optimizer universal. Melalui model _adapter_-nya, Calcite dapat terhubung ke berbagai sumber data.14 Proses optimisasinya bersifat generik, dan

_physical plan_ akhir dapat disesuaikan untuk _backend_ atau "convention" eksekusi yang berbeda.15 Ini berarti, di masa depan, Wheel DB dapat diperluas untuk tidak hanya melakukan

_query_ terhadap tabelnya sendiri, tetapi juga terhadap data dalam format lain seperti file CSV, database lain melalui JDBC, atau bahkan API web. Calcite akan menangani optimisasi dan federasi _query_ di antara berbagai sumber data ini. Dengan demikian, integrasi Calcite menempatkan Wheel DB pada jalur untuk berevolusi dari sebuah database sederhana menjadi sebuah _data federation engine_ yang lebih kuat.

_Physical plan_ yang dioptimalkan adalah output final dari bab ini dan menjadi input esensial untuk proses-proses selanjutnya dalam sebuah sistem database. Bab-bab berikutnya dalam kurikulum akan membahas bagaimana rencana ini dieksekusi dalam konteks transaksional, bagaimana mengelola akses serentak (_concurrency_), dan bagaimana memastikan durabilitas data melalui mekanisme _logging_ dan _recovery_. Pekerjaan yang telah kita lakukan di sini menyediakan fondasi yang sangat diperlukan untuk semua proses tersebut.