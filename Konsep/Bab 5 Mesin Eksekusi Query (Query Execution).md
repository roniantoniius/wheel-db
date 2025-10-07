# Bab 5: Mesin Eksekusi Query (Query Execution)

## Pendahuluan Bab: Jembatan antara Planner dan Storage

Mesin Eksekusi Query (QEE) merupakan komponen fundamental dalam arsitektur database, yang berfungsi sebagai perantara kritis antara Rencana Eksekusi Fisik—yang dihasilkan oleh Query Optimizer (Bab 4)—dengan Lapisan Penyimpanan (Storage Engine, Bab 3). Dalam proyek Wheel DB yang diimplementasikan menggunakan Java, QEE bertugas mengubah ekspresi Aljabar Relasional yang telah dioptimalkan menjadi serangkaian operasi diskrit yang diterapkan pada aliran data mentah (_tuple_).

Arsitektur QEE yang diadopsi oleh Wheel DB, selaras dengan banyak sistem manajemen basis data relasional tradisional, didasarkan pada Model Iterator (Volcano Model). Pilihan arsitektur ini memberikan modularitas tinggi dan memungkinkan _pipelining_ data antar operator. Bab ini akan mengurai secara rinci bagaimana model ini bekerja, bagaimana setiap operator diimplementasikan sebagai iterator yang otonom, dan bagaimana operator akses data dasar berinteraksi langsung dengan struktur penyimpanan seperti _HeapFile_ untuk membaca dan memodifikasi _tuple_. Pemahaman mendalam tentang QEE ini sangat penting, karena ia menentukan efisiensi dan performa aktual dari setiap kueri yang dieksekusi.

## 5.1 Model Iterator (Volcano Model): Paradigma Pipelined

Model Iterator, yang pertama kali dipublikasikan pada awal 1990-an dan secara luas dikenal sebagai Volcano Model, mendefinisikan strategi evaluasi kueri yang klasik dan sangat modular `[1]`. Prinsip inti dari model ini adalah bahwa setiap operator relasional fisik diimplementasikan sebagai algoritma evaluasi mandiri yang mengadopsi antarmuka iterator universal `[2, 3]`.

### 5.1.1 Konsep Dasar dan Prinsip _Pull-Based_

Volcano Model beroperasi berdasarkan prinsip _pull-based_ (berbasis tarik). Dalam mekanisme ini, kendali dan permintaan data dimulai dari simpul akar Pohon Eksekusi dan bergerak ke bawah, sementara data ditarik kembali ke atas, secara berurutan.

Setiap operator fisik yang mengimplementasikan operator aljabar relasional logis `[2]` memiliki tiga metode utama yang menjadi inti dari API iterator: `open()`, `getNext()`, dan `close()` `[3]`. Ketika operator teratas (akar) meminta _tuple_ berikutnya melalui `getNext()`, permintaan ini akan merambat ke bawah melalui panggilan rekursif `next-next-next` hingga mencapai simpul daun yang dapat mengakses data fisik, seperti `SeqScan` `[1]`.

Keunggulan utama model ini terletak pada _pipelining_ data `[4]`. Karena operator memproses _tuple_ satu per satu (_tuple-at-a-time_), hasil antara dapat segera diteruskan ke operator di atasnya tanpa perlu disimpan secara massal di disk atau memori. Ini secara signifikan mengurangi latensi dan kebutuhan penyimpanan sementara. Meskipun demikian, operasi _tuple-at-a-time_ ini menciptakan biaya kinerja yang signifikan: _overhead pemanggilan fungsi virtual_ (VFC) terjadi untuk setiap _tuple_ yang melewati setiap operator dalam _pipeline_. Biaya VFC ini merupakan alasan mengapa Model Iterator disebut sebagai _precursor_ dari teknik eksekusi database yang lebih modern, seperti pemrosesan tervektorisasi, yang dirancang untuk mengatasi biaya per _tuple_ ini dengan memproses data dalam _batch_ `[1]`.

### 5.1.2 Interface Universal Operator (`OpIterator` Java)

Untuk memastikan bahwa operator dapat dipertukarkan dan dirangkai secara fleksibel menjadi pohon, setiap operator fisik di Wheel DB harus mematuhi kontrak antarmuka yang ketat. Dalam konteks proyek database Java sejenis (seperti SimpleDB), antarmuka ini dikenal sebagai `OpIterator` `[5]`.

Kontrak `OpIterator` memastikan bahwa setiap operator dapat menginisialisasi dirinya, menghasilkan data, dan membersihkan sumber daya.

Tabel 5.1: Kontrak Interface OpIterator (Volcano API)

|**Metode Java**|**Tujuan**|**Aliran Kontrol Utama**|**Keterangan Implementasi**|
|---|---|---|---|
|`open()`|Menginisialisasi _state_ operator dan sumber daya eksternal.|Kontrol ke Bawah|Harus membuka _file scan_ atau mengalokasikan memori internal `[2]`.|
|`getNext()`|Mengambil _tuple_ berikutnya yang telah diproses.|Menarik Data dari Bawah|Logika pemrosesan utama (filtering, joining) `[1, 2]`.|
|`close()`|Melepaskan semua sumber daya dan membersihkan _state_.|Kontrol ke Bawah|Memastikan pelepasan _handle_ file dan sumber daya memori `[2]`.|
|`rewind()`|Mengatur ulang iterator ke awal _stream_ data.|Kontrol ke Bawah|Penting untuk operasi yang memerlukan _pass_ ganda pada input `[6]`.|
|`getTupleDesc()`|Mengembalikan skema output _tuple_.|-|Digunakan operator di atasnya untuk validasi skema.|

Ketersediaan metode `rewind()` `[6]` dalam antarmuka ini menunjukkan kemampuan arsitektur yang diperlukan untuk menangani operator biner yang mungkin perlu mengulang iterasi input relasi dalamnya, seperti _Nested Loop Join_ (NLJ). Tanpa kemampuan ini, operator gabungan akan dipaksa untuk menyimpan seluruh input relasi dalam di memori (_buffering_ penuh), yang akan menghilangkan manfaat _pipelining_ sebagian dan membatasi skalabilitas operator terhadap ukuran relasi. Dengan adanya `rewind()`, operator dapat memulai ulang iterasi dengan menarik data langsung dari _Storage Engine_ lagi (meskipun ini dapat meningkatkan I/O), memberikan fleksibilitas implementasi yang lebih besar.

### 5.1.3 Pohon Eksekusi (_Execution Tree_) dan Aliran Data

Operator yang dirangkai dalam Volcano Model secara struktural membentuk Pohon Eksekusi (juga dikenal sebagai _Query Evaluation Tree_) `[7, 8]`. Struktur ini memvisualisasikan urutan operasi aljabar relasional `[9]`.

Dalam pohon ini, simpul daun (seperti `SeqScan`) mewakili akses data dasar dari relasi fisik, sementara simpul internal mewakili operator aljabar relasional yang memproses data dari anak-anaknya (`Filter`, `Join`, `Aggregate`) `[8]`. Simpul akar pohon memberikan hasil akhir dari kueri `[7]`.

Mekanisme eksekusi bekerja melalui proses tarik yang terstruktur: klien memulai dengan memanggil `getNext()` pada operator akar. Operator ini menjalankan logikanya (misalnya, menghitung _projection_), dan ketika membutuhkan data input, ia memanggil `getNext()` pada anak-anaknya. Rantai panggilan yang terpropagasi ke bawah ini berlanjut sampai `SeqScan` dipanggil. `SeqScan` kemudian berinteraksi dengan _Storage Engine_ untuk mendapatkan _tuple_ dari disk, yang kemudian ditarik ke atas melalui _pipeline_ operator, diproses di setiap tingkat, hingga mencapai klien. Struktur hierarkis ini tidak hanya mempermudah pemahaman urutan operasi `[8]` tetapi juga memungkinkan Query Optimizer untuk menerapkan teknik seperti _join reordering_ atau _predicate pushdown_ demi meningkatkan kinerja `[8]`.

## 5.2 Implementasi Operator Akses: `SeqScan`

`SeqScan` (Sequential Scan) adalah jenis operator yang paling mendasar dan biasanya berfungsi sebagai simpul daun (kecuali untuk operasi DML). Peran utamanya adalah menyediakan akses _tuple-at-a-time_ ke seluruh data dalam suatu tabel.

### 5.2.1 Peran dan Inisialisasi `SeqScan.java`

Operator `SeqScan` mengimplementasikan metode akses _brute-force_ O(N) dengan membaca setiap _tuple_ dalam tabel, tanpa memanfaatkan indeks `[10]`. Tugas vitalnya adalah bertindak sebagai adaptor yang menjembatani antarmuka `OpIterator` yang tinggi dengan abstraksi penyimpanan data yang rendah, seperti `HeapFile`.

Proyek `dborchard/tiny-db` mencantumkan _Storage Engine_ yang mencakup komponen dasar seperti `File Manager, Block, Page` `[11]`. `SeqScan` adalah pengguna utama dari API _Storage Engine_ ini. Ketika operator diinisialisasi, ia harus mengidentifikasi tabel targetnya melalui ID Tabel dan menyiapkan mekanisme untuk membaca data.

### 5.2.2 Menggunakan Iterator `HeapFile`

`SeqScan` tidak menangani I/O halaman disk secara langsung. Sebaliknya, ia mendelegasikan tugas ekstraksi data dari disk ke iterator khusus yang disediakan oleh lapisan penyimpanan, yang dikenal sebagai `DbFileIterator` atau, lebih spesifik, `HeapFileIterator` `[12]`.

Tujuan dari `HeapFileIterator` (yang umumnya merupakan kelas internal dari implementasi _HeapFile_ atau _DbFile_) adalah:

1. Mengelola navigasi logis di antara halaman-halaman disk (_Page_) yang membentuk file data.
    
2. Mengekstrak data biner dari _slot_ _tuple_ yang valid di setiap halaman yang telah dimuat melalui _Buffer Manager_.
    
3. Mengubah data biner mentah tersebut menjadi objek `Tuple` yang terstruktur, sesuai dengan skema tabel yang didefinisikan oleh `TupleDesc` `[12]`.
    

Saat `SeqScan.open()` dipanggil, operator harus meminta instance `HeapFileIterator` baru dari _HeapFile_ yang sesuai. Permintaan ini harus selalu menyertakan ID Transaksi (`TransactionId` atau `tid`). Kebutuhan untuk menyediakan ID transaksi adalah hal yang sangat penting. Hal ini menunjukkan bahwa proses eksekusi kueri terikat erat dengan manajemen transaksi (Bab 7). Dengan memberikan `tid` ke _iterator_ penyimpanan, sistem memastikan bahwa `SeqScan` hanya membaca versi data yang sah dan terlihat oleh transaksi yang sedang berjalan, sesuai dengan tingkat isolasi yang ditetapkan.

### 5.2.3 Contoh Pseudo-Code Java untuk `SeqScan`

Meskipun akses ke kode sumber `SeqScan.java` spesifik dari `dborchard/tiny-db` tidak dapat dikonfirmasi `[13]`, arsitektur fungsionalnya dapat direkonstruksi berdasarkan model database minimalis yang serupa, menunjukkan perannya sebagai _wrapper_ untuk _iterator_ penyimpanan:

```java
public class SeqScan implements OpIterator {
    private final int tableId;
    private DbFileIterator heapFileIterator; 
    private TransactionId tid; 

    // Konstruktor menerima ID Tabel dan ID Transaksi (TID)
    public SeqScan(TransactionId tid, int tableId, String tableAlias) {
        this.tid = tid;
        this.tableId = tableId;
        DbFile file = Database.getCatalog().getDatabaseFile(tableId);
        // Meminta iterator file dengan konteks Transaksi
        this.heapFileIterator = file.iterator(tid); 
    }

    @Override
    public void open() throws DbException, TransactionAbortedException {
        // Meneruskan panggilan inisialisasi ke iterator lapisan penyimpanan
        heapFileIterator.open(); 
    }

    @Override
    public Tuple next() throws DbException, TransactionAbortedException {
        // Menarik satu tuple dari bawah (HeapFileIterator)
        if (heapFileIterator.hasNext()) {
            return heapFileIterator.next(); 
        }
        return null; // Stream berakhir
    }

    @Override
    public void close() {
        if (heapFileIterator!= null) {
            heapFileIterator.close();
        }
    }
    
    //... Implementasi rewind() dan getTupleDesc()...
}
```

Seperti yang ditunjukkan, `SeqScan` sebagian besar berfungsi sebagai adaptor pasif, menerjemahkan permintaan `OpIterator` dari QEE ke operasi fisik di lapisan `DbFileIterator`.

## 5.3 Implementasi Operator Relasional: Pemrosesan Aliran Data

Operator relasional membentuk simpul internal pohon eksekusi. Mereka bertanggung jawab untuk memproses, memfilter, atau menggabungkan aliran _tuple_ yang ditarik dari operator anak.

### 5.3.1 Operator Filter (Selection, )

Operator `Filter` mengimplementasikan operasi Selection () dari Aljabar Relasional. Operator ini adalah operator _unary_ (memiliki satu anak) dan beroperasi sebagai operator _pipelined_ murni.

`Filter` menerima _input_ berupa satu `OpIterator` (aliran data dari anak) dan objek `Predicate` yang mendefinisikan kondisi pemfiltera.

Logika inti berada dalam metode `getNext()`:

1. `Filter.getNext()` berulang kali memanggil `child.next()` untuk menarik _tuple_ satu per satu.
    
2. Setiap _tuple_ yang ditarik dievaluasi terhadap `Predicate` yang telah ditentukan.
    
3. Hanya _tuple_ yang lolos evaluasi (mengembalikan `TRUE`) yang kemudian dikembalikan sebagai _output_.
    
4. Jika _tuple_ gagal, ia dibuang, dan proses diulang hingga _tuple_ yang lolos ditemukan atau hingga aliran input anak habis.
    

Karena sifatnya yang _pipelined_, `Filter` tidak perlu menyimpan _state_ yang besar dan sangat efisien. Namun, lokasi operator ini dalam pohon eksekusi sangat menentukan kinerja kueri secara keseluruhan. Apabila Query Optimizer (Bab 6) berhasil menerapkan optimasi **Predicate Pushdown** `[8]`, operator `Filter` akan didorong sedekat mungkin ke operator akses data (`SeqScan`). Hal ini memastikan bahwa data yang tidak relevan difilter di tahap awal, membatasi volume data yang harus diproses dan dialirkan melalui operator yang lebih mahal di atasnya (terutama operator `Join`).

### 5.3.2 Operator Join: Mengombinasikan Data

Operator `Join` adalah operator _binary_ yang mengambil dua aliran _tuple_ dari anak-anaknya (Relasi Luar  dan Relasi Dalam ) dan menggabungkannya berdasarkan kondisi gabungan. Implementasi awal dan yang paling sederhana dalam database _scratch_ seperti Wheel DB adalah _Nested Loop Join_ (NLJ).

Dalam NLJ, setiap _tuple_ dari relasi luar () diulang dan dibandingkan dengan _setiap tuple_ dari relasi dalam ().

1. **Inisialisasi:** `Join` menginisialisasi dengan mengambil _tuple_ pertama dari relasi luar dan menyimpannya sebagai _current outer tuple_.
    
2. **Iterasi Dalam:** Operator kemudian mulai mengiterasi seluruh relasi dalam (). Untuk setiap _tuple_ , ia mengevaluasi kondisi gabungan (). Jika kondisi terpenuhi, _tuple_ gabungan dikeluarkan.
    
3. **Maju ke Tuple Luar:** Setelah seluruh relasi dalam selesai diiterasi, operator harus maju ke _tuple_  berikutnya dari .
    
4. **Penggulangan Relasi Dalam:** Di sinilah pentingnya metode `rewind()` `[6]` muncul. Operator harus memanggil `S.rewind()` agar iterasi di relasi dalam dapat dimulai dari awal untuk _tuple_ luar yang baru ().
    

Jika `rewind()` tidak diimplementasikan, operator `Join` terpaksa untuk sepenuhnya _membuffer_ relasi dalam  di memori internal sebelum memulai proses gabungan, yang dapat menyebabkan masalah skalabilitas memori jika  terlalu besar. NLJ dianggap _semi-pipelined_ karena meskipun dapat menghasilkan _output tuple_ secara berkala, ia memerlukan banyak _pass_ atas relasi dalam, yang dapat memakan waktu dan I/O.

### 5.3.3 Operator Aggregate (Grouping, G)

Operator `Aggregate` bertanggung jawab untuk komputasi fungsi agregasi (seperti `SUM`, `COUNT`, `AVG`, `MAX`) dan mengimplementasikan klausa `GROUP BY`.

Secara fungsional, `Aggregate` berbeda secara signifikan dari `Filter` atau `Projection` karena ia adalah operator _blocking_ (memblokir) atau _materializing_ `[4]`. Sifat _blocking_ ini muncul karena untuk menghitung hasil agregasi yang benar, operator harus mengonsumsi **semua _tuple_ input** dari anak-anaknya sebelum ia dapat menghasilkan _tuple_ output pertama. Sebagai contoh, untuk mencari nilai maksimum (`MAX(v)`) dari sebuah tabel, operator harus memproses seluruh tabel
Strategi implementasi _Aggregate_ di lingkungan in-memory yang terbatas:

1. **State Internal:** Operator `Aggregate` menggunakan struktur data internal, umumnya `HashMap` di Java `[14, 15]`.
    
2. **Kunci dan Nilai:** Kunci _Map_ dibentuk dari kombinasi nilai kolom `GROUP BY`. Nilai _Map_ menyimpan objek yang melacak _state_ agregasi parsial (misalnya, jumlah yang sedang berjalan, hitungan total _tuple_).
    
3. **Konsumsi Penuh:** Selama fase `open()` atau dalam iterasi awal `getNext()`, `Aggregate` terus-menerus memanggil `child.next()` dan memperbarui _state_ di _Map_ hingga seluruh aliran input habis.
    

Karena implementasi agregasi bergantung pada penyimpanan semua _state_ pengelompokan di memori (`HashMap`), operator ini menimbulkan risiko utama terhadap masalah _Out-of-Memory_ (OOM) jika volume data input (khususnya jumlah grup yang unik) melebihi kapasitas memori QEE. Untuk proyek database yang lebih besar, solusi skalabilitas melibatkan penggunaan teknik agregasi eksternal yang memanfaatkan disk sebagai area _spill_ sementara.

### 5.3.4 Operator DML: Insert dan Delete

Operasi bahasa manipulasi data (DML) seperti `INSERT` dan `DELETE` juga direpresentasikan sebagai operator fisik yang biasanya berfungsi sebagai simpul akar Pohon Eksekusi. Mereka tidak berfokus pada penghasilan _tuple_ hasil kueri, tetapi pada modifikasi _state_ lapisan penyimpanan.

#### A. Operator Insert

Operator `Insert` bertanggung jawab untuk menyisipkan _tuple_ baru ke dalam tabel.

- **Input:** Bergantung pada kueri: jika kueri `INSERT INTO T VALUES (...)`, operator membuat _tuple_ literal secara internal. Jika kueri `INSERT INTO T SELECT...`, operator menerima aliran _tuple_ dari operator anak.
    
- **Eksekusi:** `Insert` memanggil fungsi tulis pada lapisan penyimpanan yang mendasarinya (`HeapFile`). Operasi penulisan ini harus selalu diarahkan melalui _Buffer Manager_ (Bab 3) dan di bawah konteks `TransactionId` `[16]`. _Buffer Manager_ memastikan halaman baru atau yang dimodifikasi dimuat ke _buffer pool_ dan ditandai sebagai _dirty_.
    
- **Output:** Sebagai standar praktik database, operator `Insert` biasanya mengembalikan satu _tuple_ tunggal yang melaporkan jumlah baris yang berhasil disisipkan (misalnya, `(1)`).
    

#### B. Operator Delete

Operator `Delete` menghapus data dari tabel.

- **Input:** `Delete` menerima aliran _tuple_ dari anak-anaknya. Aliran ini biasanya dihasilkan oleh rangkaian `SeqScan` dan `Filter` yang mengidentifikasi _tuple_ target. Setiap _tuple_ input harus membawa informasi penting, yaitu **Record ID (TID)**, yang berfungsi sebagai alamat fisik _tuple_ tersebut.
    
- **Eksekusi:** Untuk setiap _tuple_ input yang diterima, `Delete` memanggil metode penghapusan yang sesuai pada _HeapFile_ target. Penghapusan dalam sistem database seringkali bersifat logis, yaitu operator menandai _slot_ _tuple_ yang bersangkutan sebagai tidak valid di dalam _Page_. Operasi modifikasi halaman ini juga harus melalui _Buffer Manager_ dan berada dalam konteks transaksi yang valid.
    
- **Output:** Operator `Delete` mengembalikan satu _tuple_ yang berisi jumlah total baris yang berhasil dihapus.
    

Keterlibatan operasi DML dalam _Buffer Manager_ menandakan pentingnya Bab 7 (_Transaction Management_). Setiap modifikasi data yang dilakukan oleh `Insert` atau `Delete` menyebabkan halaman data ditandai sebagai _dirty_. _Transaction Manager_ bertanggung jawab memastikan bahwa perubahan ini dicatat dalam Write-Ahead Log (WAL) sebelum halaman _dirty_ tersebut diizinkan untuk ditulis kembali ke disk `[11]`, menjamin durabilitas dan atomisitas.

## 6. Sintesis dan Implikasi Arsitektural

Penerapan Mesin Eksekusi Query Wheel DB berdasarkan Model Iterator menyediakan fondasi arsitektur yang kuat, modular, dan mudah dipahami, sangat ideal untuk proyek database yang dibangun dari nol. Penggunaan antarmuka `OpIterator` dengan metode `open/getNext/close` memungkinkan fleksibilitas dalam merangkai operator (seperti `SeqScan`, `Filter`, `Join`, dan `Aggregate`) menjadi Pohon Eksekusi yang representatif.

Namun, adopsi model _tuple-at-a-time_ Volcano memiliki implikasi kinerja yang perlu dicermati. Ketergantungan pada pemanggilan fungsi virtual berulang kali untuk setiap _tuple_ yang melewati setiap operator dapat menciptakan _overhead_ yang substansial saat volume data meningkat, membatasi performa puncak Wheel DB dibandingkan dengan sistem yang menggunakan eksekusi tervektorisasi.

Selain itu, sifat operator yang diimplementasikan menentukan kebutuhan sumber daya:

1. **Operator Pipelined Murni** (`Filter`, `Projection`): Efisien dalam memori, tetapi performa sangat bergantung pada keberhasilan Query Optimizer dalam mendorong predikat sedekat mungkin ke lapisan akses data.
    
2. **Operator Semi-Pipelined** (`Join/NLJ`): Memerlukan manajemen _state_ dan kemampuan untuk melakukan iterasi ulang pada anak (`rewind()`) atau terpaksa melakukan _buffering_ memori besar.
    
3. **Operator Blocking** (`Aggregate`): Menghadirkan _bottleneck_ memori paling signifikan, karena membutuhkan konsumsi penuh input untuk menghasilkan _output_ yang benar, menyoroti batasan Wheel DB saat memproses kueri agregasi pada data yang tidak muat di memori utama.
    

Secara keseluruhan, QEE Wheel DB menunjukkan hubungan kausal yang mendalam dengan komponen database lainnya. Operator akses seperti `SeqScan` membutuhkan `TransactionId` untuk menjamin visibilitas data yang konsisten, sementara operasi DML mengkonfirmasi ketergantungan QEE pada _Buffer Manager_ dan _Transaction Manager_ untuk memastikan atomisitas dan durabilitas perubahan data.

Langkah logis berikutnya dalam pengembangan Wheel DB, setelah menetapkan QEE yang fungsional, adalah beralih ke Query Optimizer (Bab 6). Optimizer inilah yang akan menggunakan model biaya untuk memilih rangkaian operator paling efisien, seperti yang telah dibahas, untuk meminimalkan _bottleneck_ I/O dan VFC yang inheren dalam arsitektur Volcano.