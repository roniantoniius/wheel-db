# Wheel DB: Bab 3 - Lapisan Penyimpanan (Storage Layer)

## Pendahuluan: Memahami Lapisan Penyimpanan dan Pilihan Arsitektur

### Peran Krusial Lapisan Penyimpanan dalam Sistem Basis Data

Lapisan penyimpanan, atau yang dikenal sebagai _storage layer_, merupakan fondasi arsitektural dari setiap sistem manajemen basis data (DBMS). Lapisan ini bertanggung jawab untuk mengelola interaksi fundamental antara memori utama (RAM) dan memori sekunder (disk), sebuah interaksi yang sering kali menjadi hambatan kinerja terbesar dalam sistem basis data modern. Data yang tersimpan di disk harus dibaca ke dalam memori utama untuk diproses, dan setiap perubahan harus ditulis kembali ke disk untuk memastikan persistensi. Konsep ini menjadi sangat krusial karena sebagian besar basis data dunia nyata memiliki ukuran yang jauh melebihi kapasitas memori utama, sehingga seluruh data tidak dapat dimuat sekaligus ke dalam RAM.1

Dengan demikian, kinerja suatu basis data sangat bergantung pada seberapa efisiennya ia mengelola operasi input/output (I/O) disk. Desain lapisan penyimpanan harus mengoptimalkan setiap akses disk dengan meminimalkan jumlah operasi I/O yang diperlukan untuk melakukan tugas-tugas dasar seperti pencarian, penyisipan, dan penghapusan data. Pemahaman mendalam tentang bagaimana data diatur, disimpan, dan diambil dari disk adalah prasyarat untuk membangun sistem basis data yang tangguh dan berkinerja tinggi.

### Memilih Referensi: Mengapa `simpledb` Lebih Relevan dari `tiny-db`

Pengembangan "Wheel DB" sebagai proyek basis data dari nol memerlukan referensi yang sesuai dengan arsitektur yang akan dibangun. Referensi yang diberikan oleh pengguna, `tiny-db` dari `dborchard/tiny-db`, menunjukkan kebutuhan akan model studi yang ringkas dan fungsional. Namun, analisis terhadap repositori `tiny-db` dan proyek serupa, seperti `msiemens/tinydb`, mengungkapkan bahwa mereka adalah basis data berorientasi dokumen yang menyimpan data dalam file JSON.3 Pendekatan ini secara fundamental berbeda dari konsep basis data relasional yang mendasari

`HeapFile` dan `B+ Tree` yang menjadi topik bab ini. Implementasi `tiny-db` pada umumnya tidak melibatkan struktur data seperti `HeapFile` atau `B+ Tree` dalam pengertian yang sama dengan basis data relasional tradisional.

Mengingat perbedaan arsitektural ini, sebuah proyek dari University of Washington yang disebut `simpledb` menjadi referensi yang jauh lebih relevan.6 Proyek ini dirancang secara eksplisit untuk tujuan edukasi dan mengimplementasikan konsep-konsep inti basis data relasional dari nol menggunakan Java, termasuk

`HeapFile.java` dan interaksinya dengan komponen-komponen lain seperti `BufferPool`.6 Oleh karena itu, laporan ini akan mengalihkan fokus dari

`tiny-db` ke `simpledb` sebagai model implementasi yang lebih tepat dan sesuai dengan kurikulum yang diusulkan. Pendekatan ini tidak hanya menjawab pertanyaan secara langsung, tetapi juga memberikan pemahaman arsitektural yang lebih akurat dan mendalam, yang merupakan bagian esensial dari pengembangan sistem basis data.

### Ikhtisar Kurikulum Bab 3

Bab 3 ini akan memandu proses pembangunan fondasi penyimpanan data untuk "Wheel DB." Dimulai dengan memahami unit data terkecil yang dapat diakses dari disk, yaitu halaman (`Page`), dan pengenalnya (`PageId`). Kemudian, pembahasan akan beralih ke metode organisasi data yang paling sederhana, yaitu `Heap File`, yang menjadi titik awal yang ideal untuk operasi baca-tulis. Terakhir, laporan akan menganalisis `B+ Tree`, sebuah struktur data yang sangat optimal untuk mengelola data di disk, yang akan melengkapi `Heap File` dengan kemampuan pencarian dan kueri rentang yang sangat efisien. Setiap bagian akan menggabungkan teori dengan contoh praktis dan diskusi desain yang relevan untuk proyek "Wheel DB."

## Bab 3: Lapisan Penyimpanan (Storage Layer)

### 3.1 Konsep Halaman (Page) dan PageId

#### Definisi dan Peran Fundamental Halaman sebagai Unit I/O

Dalam arsitektur basis data, halaman (`Page`) adalah unit data fundamental dan atomik. Konsep ini muncul dari kenyataan bahwa operasi I/O pada disk dilakukan dalam blok-blok data berukuran tetap, bukan byte individual.9 Sebagian besar sistem basis data, termasuk SQL Server, menggunakan halaman berukuran 8 KB sebagai unit standar untuk membaca atau menulis data ke disk.9 Keputusan desain ini memiliki implikasi besar terhadap kinerja. Jika sebuah operasi kueri membutuhkan satu baris data yang ukurannya hanya beberapa puluh byte, seluruh halaman (8 KB) yang berisi baris tersebut harus dibaca ke dalam memori. Demikian pula, setiap modifikasi data di dalam satu halaman akan memerlukan penulisan ulang seluruh halaman tersebut ke disk. Memahami bahwa operasi I/O terjadi pada tingkat halaman, bukan pada tingkat baris data, adalah kunci untuk merancang sistem basis data yang efisien.

#### Struktur Internal Sebuah Halaman

Meskipun ukuran halaman tetap, struktur internalnya bersifat hierarkis dan terorganisir untuk memaksimalkan efisiensi. Sebuah halaman data biasanya terdiri dari tiga komponen utama 9:

- **Header Halaman**: Bagian pertama dari halaman, biasanya berukuran 96 byte, yang berisi metadata penting tentang halaman itu sendiri.9 Metadata ini mencakup informasi seperti
    
`PageId`, tipe halaman (misalnya, halaman data, halaman indeks), jumlah ruang kosong yang tersedia, dan ID objek yang memiliki halaman tersebut. Header berfungsi sebagai "kartu identitas" dan ringkasan kondisi halaman, memungkinkan sistem basis data untuk dengan cepat menentukan karakteristiknya tanpa harus memindai seluruh konten.
    
- **Baris Data (`Data Rows`)**: Area di dalam halaman tempat data aktual dari tuple atau dokumen disimpan.10 Untuk tabel dengan kolom berukuran tetap, setiap baris akan menempati jumlah ruang yang sama. Namun, jika ada kolom dengan ukuran variabel (seperti
    
`VARCHAR` atau `VARBINARY`), jumlah baris yang dapat disimpan dalam satu halaman akan bervariasi tergantung pada panjang data aktual.10 Baris-baris data ini biasanya diisi secara serial, dimulai dari akhir header.9
    
- **Tabel Offset Baris (`Row Offset Table`)**: Sebuah struktur yang terletak di bagian paling akhir halaman yang menyimpan entri untuk setiap baris data.9 Setiap entri offset berisi informasi tentang seberapa jauh byte pertama dari sebuah baris dari awal halaman. Ketika sebuah baris data baru ditambahkan, entri baru ditambahkan ke tabel offset, dan ruang kosong di antara baris data dan tabel offset berkurang. Struktur ini memungkinkan sistem untuk menemukan baris data tertentu dengan cepat tanpa harus memindai seluruh halaman.
    

#### Mengapa Konsep PageId Sangat Penting: Identifikasi Unik dan Alamat

`PageId` adalah pengenal unik untuk setiap halaman di dalam basis data.10 Lebih dari sekadar label,

`PageId` berfungsi sebagai alamat fisik logis halaman di disk. `PageId` biasanya merupakan kombinasi dari ID file basis data dan nomor halaman di dalam file tersebut.10 Misalnya, dalam sebuah basis data dengan banyak file, sebuah

`PageId` dapat secara unik mengidentifikasi halaman ke-100 di dalam file kedua.

Pentingnya `PageId` terletak pada kemampuannya untuk mengubah pencarian data yang lambat dan sekuensial menjadi operasi pencarian langsung yang efisien. Tanpa `PageId` atau mekanisme penunjuk yang serupa, sistem basis data akan dipaksa untuk memindai seluruh file dari awal untuk menemukan halaman yang benar. Dengan `PageId`, sistem dapat secara langsung menghitung lokasi fisik halaman di disk (`offset = page_number * page_size`) dan melompat ke lokasi tersebut untuk melakukan operasi I/O, secara signifikan mengurangi latensi. Selain itu, `PageId` juga berfungsi sebagai kunci utama yang digunakan oleh `BufferPool` untuk mengelola cache halaman di memori utama. `BufferPool` dapat dengan cepat memetakan permintaan halaman logis (berdasarkan `PageId`) ke salinan halaman yang sudah di-cache di RAM, menghindari operasi I/O disk yang mahal jika halaman sudah tersedia.

#### Implikasi Desain untuk `Wheel DB`

Untuk proyek "Wheel DB," langkah pertama yang fundamental adalah mendefinisikan kelas `Page` dan `PageId` yang akan menjadi fondasi dari seluruh lapisan penyimpanan. Kelas `PageId` harus mencakup setidaknya `fileId` dan `pageNumber` untuk memastikan setiap halaman dapat diidentifikasi secara unik. Kelas `Page` harus memiliki metode untuk membaca dan menulis data ke dalam struktur internalnya, serta mengelola ruang kosong yang tersedia. Struktur ini akan menjadi cetak biru yang akan digunakan oleh semua metode organisasi file, seperti `Heap File` dan `B+ Tree`, di seluruh sistem.

### 3.2 Organisasi File Berbasis Heap (Heap File)

#### Apa Itu Heap File: Penyimpanan Data Tanpa Urutan Khusus

Organisasi file berbasis _heap_ adalah metode penyimpanan data yang paling sederhana dalam sistem basis data. Dalam metode ini, tuple atau rekaman data disimpan di halaman-halaman tanpa urutan atau penyortiran tertentu.11 Rekaman baru dapat ditambahkan ke halaman mana pun yang memiliki ruang kosong, atau, jika tidak ada halaman yang tersedia, rekaman tersebut akan ditambahkan ke halaman baru di akhir file. Sifatnya yang tidak terurut ini memberikan keuntungan yang signifikan pada operasi tertentu.

#### Analisis Kelebihan dan Kekurangan (Trade-offs)

Seperti halnya setiap pilihan arsitektur, `Heap File` memiliki keunggulan dan kelemahan yang perlu dipertimbangkan:

- **Kelebihan**: Keuntungan utama dari `Heap File` adalah kecepatan luar biasa untuk operasi penyisipan (`insertion`) dan penghapusan (`deletion`).12 Karena tidak ada persyaratan untuk mempertahankan urutan, data baru dapat langsung ditambahkan ke lokasi yang tersedia tanpa perlu reorganisasi atau pergeseran data. Ini menjadikannya pilihan ideal untuk log data, tabel sementara, atau kasus penggunaan di mana operasi tulis jauh lebih sering daripada operasi baca.12
    
- **Kekurangan**: Kelemahan terbesar adalah kinerja yang buruk untuk operasi pencarian dan kueri yang memerlukan pemindaian subset data.11 Untuk menemukan rekaman tertentu atau mengambil semua rekaman, sistem harus melakukan pemindaian penuh dari seluruh file dari awal hingga akhir (
    
    _full file scan_). Proses ini sangat tidak efisien untuk database yang besar dan dapat memakan waktu yang lama.11 Kekurangan lainnya adalah potensi terjadinya fragmentasi, di mana penghapusan rekaman dapat meninggalkan ruang kosong di tengah halaman atau file.12 Jika tidak dikelola dengan baik, fragmentasi dapat mengurangi efisiensi penyimpanan dan akhirnya memerlukan proses defragmentasi.7
    

#### Studi Kasus: Anatomi Kelas `HeapFile.java` dari `simpledb`

Proyek `simpledb` menyediakan implementasi `HeapFile.java` yang berfungsi sebagai contoh ideal untuk "Wheel DB".6 Kelas ini bertugas mengelola koleksi halaman `HeapPage` yang membentuk sebuah tabel.6 Penting untuk dipahami bahwa `HeapFile` di `simpledb` tidak berfungsi secara mandiri; ia memiliki ketergantungan erat pada kelas lain dalam sistem, yang menunjukkan desain arsitektur berlapis yang baik. Sebagai contoh, `HeapFile` tidak secara langsung membaca halaman dari disk; ia mendelegasikan tugas ini kepada `BufferPool` melalui metode `BufferPool.getPage()`.6 Hal ini memungkinkan sistem untuk memanfaatkan mekanisme caching yang diimplementasikan oleh

`BufferPool`, sehingga halaman yang sering diakses tidak perlu dibaca berulang kali dari disk.7 Hubungan ini menunjukkan pentingnya desain modular di mana setiap komponen memiliki peran spesifik dan berinteraksi melalui antarmuka yang jelas.

#### Implementasi Metode Kunci: `insertTuple`, `deleteTuple`, `iterator`

Kelas `HeapFile` mencakup serangkaian metode inti yang mendefinisikan perilakunya:

- `insertTuple(TransactionId tid, Tuple t)`: Metode ini bertanggung jawab untuk menyisipkan tuple baru ke dalam file.6 Implementasinya harus mencari halaman yang memiliki ruang kosong. Jika ditemukan, tuple akan disisipkan ke halaman tersebut. Jika tidak ada halaman yang cukup kosong,
    
  `HeapFile` harus membuat halaman baru di akhir file dan menyisipkan tuple di sana.
    
- `deleteTuple(TransactionId tid, Tuple t)`: Metode ini menghapus tuple yang ditentukan dari file.6 Prosesnya dimulai dengan menemukan halaman yang berisi tuple tersebut, kemudian menandai tuple sebagai terhapus. Meskipun rekaman tidak secara fisik dihapus dari disk, ruang yang ditempatinya dapat digunakan kembali.
    
- `iterator(TransactionId tid)`: Metode ini mengembalikan iterator yang memungkinkan untuk menelusuri semua tuple dalam `HeapFile` secara sekuensial.6 Implementasinya harus menggunakan
    
`BufferPool.getPage()` untuk mendapatkan setiap halaman secara berurutan, dari halaman pertama hingga terakhir, dan kemudian mengiterasi setiap tuple di dalam setiap halaman.
    

#### Contoh Kode dan Penjelasan

Berikut adalah contoh pseudo-code yang menyederhanakan cara kerja sebuah `HeapFile` di "Wheel DB," menunjukkan bagaimana ia berinteraksi dengan `Page` dan `PageId`.

```java
// Kelas PageId untuk mengidentifikasi halaman secara unik
class PageId {
    int fileId;
    int pageNo;
}

// Interface dasar untuk file database
interface DbFile {
    Page readPage(PageId pid);
    void writePage(Page page);
}

// Implementasi HeapFile
class HeapFile implements DbFile {
    File f;
    
    // Metode untuk menyisipkan tuple
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t) {
        // 1. Iterasi melalui semua halaman untuk mencari yang kosong
        for (int i = 0; i < numPages(); i++) {
            PageId pid = new PageId(this.getId(), i);
            HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
            
            // 2. Jika ada ruang, sisipkan tuple dan kembalikan
            if (page.getNumEmptySlots() > 0) {
                page.insertTuple(t);
                return ArrayList<Page>(page);
            }
        }
        
        // 3. Jika semua halaman penuh, buat halaman baru
        PageId newPid = new PageId(this.getId(), numPages());
        HeapPage newPage = new HeapPage(newPid);
        newPage.insertTuple(t);
        writePage(newPage);
        return ArrayList<Page>(newPage);
    }
    
    // Metode untuk mengiterasi semua tuple
    public DbFileIterator iterator(TransactionId tid) {
        // Implementasi iterator yang memindai setiap halaman dan tuple
        return new HeapFileIterator(this, tid);
    }
}
```

Tabel berikut meringkas peran metode-metode kunci dalam `HeapFile.java`:

Metode Kunci dan Peran dalam `HeapFile.java`

| **Nama Metode**                           | **Deskripsi Fungsionalitas**                                                    | **Keterkaitan Arsitektur**                                                                                |
| ----------------------------------------- | ------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| `numPages()`                              | Mengembalikan jumlah total halaman dalam file.                                  | Digunakan oleh iterator untuk menentukan batas pemindaian.                                                |
| `readPage(PageId pid)`                    | Membaca halaman yang ditentukan dari disk.                                      | Berinteraksi langsung dengan file di disk, namun biasanya dienkapsulasi oleh `BufferPool`.                |
| `writePage(Page page)`                    | Mendorong halaman ke disk, menuliskannya di offset yang sesuai dengan `PageId`. | Memastikan data yang diubah di memori utama (RAM) bertahan di memori sekunder (disk).                     |
| `insertTuple(TransactionId tid, Tuple t)` | Menyisipkan tuple baru ke halaman dengan ruang kosong.                          | Harus menemukan halaman yang cocok. Menggunakan `BufferPool.getPage()` untuk mengelola halaman.           |
| `deleteTuple(TransactionId tid, Tuple t)` | Menghapus tuple yang ditentukan dari file.                                      | Membutuhkan pencarian tuple terlebih dahulu, yang bisa lambat di Heap File.                               |
| `iterator(TransactionId tid)`             | Mengembalikan iterator yang memindai semua tuple di semua halaman.              | Kunci untuk kueri `SELECT *`. Menggunakan `BufferPool.getPage()` untuk mendapatkan halaman satu per satu. |

### 3.3 Struktur Indeks B+ Tree

#### Pendahuluan: Mengapa Indeks Diperlukan dalam Lingkungan Disk?

Kelemahan utama dari `Heap File` adalah ketidakmampuannya untuk melakukan pencarian yang efisien, terutama untuk data dalam jumlah besar.11 Pencarian rekaman tertentu akan selalu memerlukan pemindaian seluruh file. Ini tidak dapat diterima untuk sebagian besar aplikasi database. Untuk mengatasi masalah ini, sistem basis data menggunakan indeks, yang merupakan struktur data sekunder yang dirancang khusus untuk mempercepat operasi pencarian, pembaruan, dan penghapusan.1

Alasan mendasar mengapa indeks seperti B+ Tree begitu penting adalah karena mereka mengoptimalkan I/O disk. Akses disk adalah operasi yang sangat lambat, sering kali memerlukan waktu ribuan kali lebih lama dibandingkan akses ke memori utama. `B+ Tree` dirancang sedemikian rupa sehingga setiap node di dalam pohon berukuran sama dengan satu blok disk (atau kelipatannya), yang berarti setiap langkah traversal ke node anak hanya memerlukan satu operasi I/O disk.14 Selain itu,

`B+ Tree` memiliki _fanout_ yang sangat tinggi—jumlah pointer ke node anak dalam sebuah node, yang bisa mencapai 100 atau lebih.2 Fanout yang tinggi ini secara langsung mengurangi tinggi pohon secara logaritmik, yang pada gilirannya mengurangi jumlah operasi I/O disk yang diperlukan untuk menemukan rekaman apa pun di dalam pohon, bahkan untuk miliaran rekaman (

`log_100(N)`).2

fan out merupakan pola distribusi data dari satu sumber ke banyak tujuan secara bersamaan,

jadi semakin banyak fan out bisa jadi banyak rute (jalan pintas)

#### Perbandingan Heap File vs. B+ Tree

Tabel berikut menyajikan perbandingan antara dua metode organisasi file ini, menyoroti trade-off utama dalam desain sistem basis data.

Perbandingan Organisasi File: Heap File vs. Indeks B+ Tree

| **Karakteristik**            | **Organisasi File Berbasis Heap** | **Struktur Indeks B+ Tree**             |
| ---------------------------- | --------------------------------- | --------------------------------------- |
| **Urutan Data**              | Tidak terurut                     | Terurut pada node Tree                  |
| **Pencarian**                | Lambat (memerlukan _full scan_)   | Sangat cepat (logaritmik)               |
| **Penyisipan & Penghapusan** | Sangat cepat                      | Cepat (logaritmik)                      |
| **Kueri Rentang**            | Lambat (tidak efisien)            | Sangat cepat (_traversal_ node Tree)    |
| **Contoh Penggunaan**        | Log data, tabel sementara         | Indeks utama dan sekunder               |
| **Optimal untuk**            | Operasi tulis intensif            | Operasi baca dan kueri rentang intensif |

#### Struktur B+ Tree: Node Internal vs. Node Tree

`B+ Tree` adalah struktur pohon yang seimbang dan terdiri dari dua jenis node utama 2:

- **Node Internal (`Internal Nodes`)**: Node-node ini tidak menyimpan data rekaman, melainkan hanya menyimpan kunci pencarian dan pointer ke node anak. Kunci-kunci ini berfungsi sebagai panduan, mengarahkan pencarian ke jalur yang benar menuju node Tree yang sesuai.2
    
- **Node Tree (`Leaf Nodes`)**: Semua data rekaman disimpan secara eksklusif di node Tree.2 Yang paling membedakan
    
`B+ Tree` dari `B-Tree` adalah bahwa semua node Tree terhubung satu sama lain dalam sebuah linked list (_linked list_).2 Semua node Tree berada pada kedalaman yang sama dari akar, yang menjamin bahwa pohon tetap seimbang dan waktu pencarian untuk setiap rekaman adalah konsisten.2
    

#### Operasi Dasar dan Analisis Kompleksitas

Operasi pada `B+ Tree` dirancang untuk meminimalkan I/O disk dan mempertahankan keseimbangan pohon 2:

- **Pencarian (`Search`)**: Untuk menemukan sebuah rekaman, pencarian dimulai dari akar pohon. Pada setiap node internal, algoritma memindai kunci pencarian untuk menentukan pointer anak mana yang harus diikuti.2 Proses ini berlanjut sampai node Tree yang benar ditemukan. Di node Tree, pencarian linier dilakukan untuk menemukan rekaman yang diinginkan. Kompleksitas waktunya adalah logaritmik, yaitu
    
    O (logb​ N), di mana b adalah faktor percabangan dan N adalah jumlah total rekaman.2
    
- **Penyisipan (`Insertion`)**: Untuk menyisipkan rekaman baru, algoritma pertama-tama melakukan pencarian untuk menemukan node Tree yang seharusnya menampung rekaman tersebut.2 Jika node Tree memiliki ruang, rekaman ditambahkan dalam urutan yang benar. Jika node penuh, node akan dibagi menjadi dua, dan kunci terkecil dari node yang baru akan disalin ke node induk. Proses pembagian ini dapat menyebar ke atas ke node induk, dan jika akar terbagi, tinggi pohon akan bertambah.2
    
- **Penghapusan (`Deletion`)**: Penghapusan juga dimulai dengan pencarian. Setelah rekaman ditemukan dan dihapus dari node Tree, algoritma memeriksa apakah node tersebut masih memiliki jumlah kunci minimum yang diperlukan.2 Jika node berada dalam kondisi
    
`underflow`, sistem akan mencoba meminjam kunci dari node saudara yang berdekatan. Jika itu tidak memungkinkan, node akan digabungkan dengan saudaranya, dan kunci yang memisahkan mereka akan dihapus dari node induk. Proses ini juga dapat menyebar ke atas pohon jika node induk menjadi `underflow`.2

> `underflow`adalah kondisi dalam komputasi di mana hasil perhitungan berupa nilai yang terlalu kecil untuk dapat direpresentasikan secara akurat oleh memori komputer atau perangkat keras, sering kali mengakibatkan nilai tersebut disimpan sebagai nol atau terdeteksi sebagai kesalahan


konsep:
- kalau nyelam, gaboleh terlalu ringan jadi perlu tambah bobot



#### Keunggulan B+ Tree untuk Range Queries

Salah satu keunggulan terbesar dan paling praktis dari `B+ Tree` adalah efisiensinya dalam melakukan kueri rentang (`range queries`).2 Contohnya, kueri seperti

`SELECT * FROM users WHERE age BETWEEN 20 AND 30` akan menjadi operasi yang sangat mahal pada `Heap File` karena memerlukan pemindaian penuh.

Desain `B+ Tree` secara sengaja memasukkan fitur linked list pada node Tree. Untuk kueri rentang, sistem pertama-tama melakukan pencarian standar untuk menemukan rekaman pertama dalam rentang (`age = 20`). Setelah rekaman ini ditemukan, sistem dapat dengan mudah menelusuri linked list node Tree secara sekuensial, mengumpulkan semua rekaman dalam rentang tersebut, hingga mencapai batas akhir (`age = 30`).2 Proses ini menghindari pemindaian sisa pohon atau file data yang tidak relevan, menjadikannya sangat efisien dan merupakan alasan utama mengapa

`B+ Tree` menjadi standar industri untuk indeks basis data.

## Penutup dan Rekomendasi Lanjutan

### Ringkasan Konsep Lapisan Penyimpanan

Bab 3 telah meletakkan fondasi kritis untuk proyek "Wheel DB" dengan menjelaskan tiga konsep fundamental: halaman (`Page`), `Heap File`, dan `B+ Tree`. Halaman adalah unit I/O atomik yang mengelola interaksi dengan disk, dan `PageId` adalah alamat logis uniknya. `Heap File` menawarkan metode penyimpanan yang sederhana dan cepat untuk operasi tulis, menjadikannya titik awal yang baik. Namun, keterbatasan kinerjanya dalam pencarian, terutama untuk database besar, menyoroti kebutuhan akan struktur data yang lebih canggih. `B+ Tree` muncul sebagai solusi yang sangat dioptimalkan untuk lingkungan disk, menggunakan `fanout` yang tinggi dan struktur node Tree yang terhubung untuk memastikan pencarian dan kueri rentang yang sangat efisien.

### Jalur Selanjutnya untuk `Wheel DB`

Setelah fondasi `Heap File` dan `B+ Tree` dipahami, langkah logis berikutnya untuk proyek "Wheel DB" adalah mengintegrasikan struktur ini ke dalam sistem yang lebih besar:

- **Implementasi `BufferPool`**: Komponen krusial berikutnya adalah `BufferPool`, yang bertanggung jawab untuk caching halaman di memori utama.7
    
`BufferPool` akan mengelola semua permintaan `readPage` dan `writePage` dari `HeapFile` dan `B+ Tree`, sehingga semua interaksi dengan disk dapat dikoordinasikan secara efisien.
    
- **Implementasi `Catalog`**: Untuk mengelola skema dan metadata tabel, diperlukan sebuah komponen `Catalog`.7
    
`Catalog` akan menyimpan deskripsi tuple (`TupleDesc`) dan informasi tentang indeks yang ada, memungkinkan kueri untuk menemukan dan mengidentifikasi tabel dan datanya.
    
- **Integrasi Indeks**: Setelah `B+ Tree` diimplementasikan, ia dapat digunakan sebagai indeks di atas `HeapFile`. Untuk kueri yang memerlukan pencarian cepat, sistem dapat menggunakan indeks `B+ Tree` untuk menemukan lokasi halaman yang benar di `HeapFile`, alih-alih melakukan pemindaian penuh.
    

### Rekomendasi untuk Optimalisasi dan Fitur Masa Depan

Seiring dengan pertumbuhan proyek "Wheel DB," beberapa area dapat dieksplorasi untuk meningkatkan kinerja dan fungsionalitas:

- **Defragmentasi**: Pertimbangkan untuk menambahkan mekanisme defragmentasi untuk `Heap File` untuk mengkonsolidasikan ruang kosong yang tercipta dari operasi penghapusan.7
    
- **Ukuran Tuple Dinamis**: Dukungan untuk tuple dengan ukuran dinamis, bukan hanya ukuran tetap, akan meningkatkan fleksibilitas sistem.7
    
- **Manajemen Transaksi dan Penguncian (`Locking`)**: Untuk mendukung operasi bersamaan (`concurrency`), implementasi mekanisme `locking` di tingkat halaman akan mencegah _data race_ dan memastikan konsistensi data.7
    

Membangun basis data dari nol adalah proyek yang menantang namun sangat bermanfaat. Setiap lapisan yang dibangun, dari halaman hingga indeks, memberikan pemahaman mendalam tentang arsitektur perangkat lunak dan komputasi yang efisien. Pendekatan berbasis `simpledb` yang dibahas dalam laporan ini menyediakan peta jalan yang jelas dan teruji untuk mencapai tujuan ini.