# Bab 6: Transaksi dan Konkurensi

## Pendahuluan: Dari Penyimpanan Fisik ke Jaminan Logis

Pada bab-bab sebelumnya, kita telah membangun fondasi dari "Wheel DB", berfokus pada bagaimana data secara fisik disimpan dan diakses. Kita telah membahas _Storage Manager_ yang berinteraksi dengan disk, _Buffer Manager_ yang secara cerdas mengelola data di memori, dan struktur data seperti B+ Tree untuk pengindeksan yang efisien. Namun, memiliki kemampuan untuk menyimpan dan mengambil data dari disk hanyalah setengah dari cerita. Sebuah sistem database yang andal dan kuat harus menyediakan lebih dari sekadar penyimpanan; ia harus menawarkan jaminan logis tentang bagaimana data tersebut dimodifikasi dan dibaca.

Bayangkan sebuah sistem perbankan. Operasi "transfer uang" bukanlah satu perintah tunggal, melainkan serangkaian langkah: (1) Periksa saldo di akun A, (2) Kurangi saldo di akun A, (3) Tambahkan saldo ke akun B. Apa yang terjadi jika sistem mengalami kegagalan daya setelah langkah (2) tetapi sebelum langkah (3)? Uang akan hilang begitu saja. Untuk mencegah anomali semacam ini, database memperkenalkan konsep **transaksi**: sebuah unit kerja logis yang membungkus serangkaian operasi, yang harus berhasil atau gagal sebagai satu kesatuan yang utuh.

Dalam bab ini, kita akan memasuki dunia manajemen transaksi dan kontrol konkurensi. Ini adalah komponen-komponen yang mengubah kumpulan data mentah menjadi sistem database yang dapat diandalkan untuk aplikasi-aplikasi kritis.

### Mengatasi Keterbatasan Repositori Referensi

Penting untuk dicatat bahwa repositori referensi utama kita, `dborchard/tiny-db`, secara eksplisit menyatakan bahwa _Transactions_ dan _Concurrency Manager_ masih berada dalam daftar "TODO" mereka.1 Ini berarti kita tidak akan menemukan implementasi

`Transaction.java` atau _lock manager_ di dalam kode sumber tersebut.

Namun, ini bukanlah sebuah halangan, melainkan sebuah peluang belajar yang unik. Alih-alih hanya menganalisis dan membedah kode yang sudah ada, kita akan mengambil peran sebagai arsitek sistem. Kita akan merancang dan membangun komponen-komponen krusial ini dari prinsip-prinsip pertama, khusus untuk "Wheel DB". Pendekatan ini akan memberikan pemahaman yang jauh lebih dalam dan pengalaman praktis yang relevan dengan tujuan akhir proyek ini: membangun database dari awal.

## 6.1 Properti ACID: Fondasi Keandalan Database

Inti dari keandalan transaksi dalam sistem database modern dirangkum oleh akronim **ACID**. ACID adalah seperangkat empat properti—_Atomicity_, _Consistency_, _Isolation_, dan _Durability_—yang berfungsi sebagai kontrak atau jaminan yang diberikan oleh sistem database kepada aplikasi.2 Ketika serangkaian operasi dibungkus dalam sebuah transaksi, database berjanji untuk menegakkan keempat properti ini, memastikan integritas data bahkan dalam menghadapi kesalahan, kegagalan sistem, dan akses bersamaan.

### A - Atomicity (Atomisitas)

- **Konsep**: Atomisitas menerapkan prinsip "semua atau tidak sama sekali" (_all-or-nothing_). Ini menjamin bahwa sebuah transaksi diperlakukan sebagai satu unit kerja tunggal yang tidak dapat dibagi (_indivisible_).2 Jika salah satu operasi dalam transaksi gagal karena alasan apa pun—baik itu kesalahan perangkat keras,
    
    _crash_ perangkat lunak, atau pelanggaran batasan—seluruh transaksi akan dibatalkan, dan database akan dikembalikan ke keadaan sebelum transaksi dimulai. Proses ini dikenal sebagai _rollback_ atau _abort_.4
    
- **Contoh Praktis**: Kembali ke contoh transfer bank. Transaksi transfer terdiri dari debit dari akun A dan kredit ke akun B. Jika sistem _crash_ setelah berhasil mendebit akun A tetapi sebelum mengkredit akun B, atomisitas memastikan bahwa saat sistem pulih, operasi debit tersebut akan dibatalkan. Tidak ada uang yang "hilang" karena transaksi tidak pernah selesai sebagian; ia gagal secara keseluruhan.5
    

### C - Consistency (Konsistensi)

- **Konsep**: Konsistensi memastikan bahwa setiap transaksi akan membawa database dari satu keadaan yang valid ke keadaan valid lainnya.2 "Validitas" di sini didefinisikan oleh semua aturan dan batasan (
    
    _constraints_) yang telah ditetapkan pada skema database, seperti tipe data, batasan `NOT NULL`, kunci unik (_unique keys_), dan integritas referensial (_foreign keys_).8 Jika sebuah transaksi akan melanggar salah satu dari aturan ini, transaksi tersebut akan di-
    
    _rollback_ untuk menjaga integritas data.
    
- **Contoh Praktis**: Misalkan sebuah tabel rekening bank memiliki _constraint_ bahwa kolom `saldo` tidak boleh bernilai negatif. Jika sebuah transaksi mencoba menarik $200 dari akun yang hanya memiliki saldo $150, transaksi tersebut akan gagal. Properti konsistensi mencegah operasi ini, memastikan database tidak pernah memasuki keadaan yang tidak valid (saldo negatif).2
    

Penting untuk memahami bahwa konsistensi dalam konteks ACID memiliki nuansa. Properti ini adalah tanggung jawab bersama antara database dan aplikasi. Aplikasi mendefinisikan apa yang dimaksud dengan "konsisten" melalui skema dan logika bisnis (misalnya, "total semua saldo di semua akun harus tetap konstan setelah transfer internal").9 Database, pada gilirannya, menyediakan mekanisme melalui properti A, I, dan D untuk memungkinkan pengembang aplikasi menegakkan konsistensi tersebut. Tanpa atomisitas dan isolasi, bahkan logika aplikasi yang paling sempurna pun dapat gagal dalam lingkungan yang konkuren, yang mengarah pada keadaan data yang tidak konsisten.

### I - Isolation (Isolasi)

- **Konsep**: Isolasi menjamin bahwa eksekusi transaksi secara bersamaan (_concurrent_) akan menghasilkan keadaan database yang sama seolah-olah transaksi-transaksi tersebut dieksekusi secara berurutan (_serially_).2 Dengan kata lain, setiap transaksi berjalan dalam "gelembung"-nya sendiri, tidak menyadari transaksi lain yang mungkin sedang berjalan. Perubahan parsial yang dibuat oleh transaksi yang sedang berjalan tidak terlihat oleh transaksi lain sampai transaksi tersebut berhasil di-
    
    _commit_.3
    
- **Contoh Praktis**: Bayangkan dua pelanggan mencoba memesan kursi terakhir di sebuah penerbangan secara bersamaan melalui situs web yang berbeda. Isolasi memastikan bahwa kedua transaksi ini tidak saling mengganggu. Sistem akan memprosesnya seolah-olah satu transaksi terjadi sepenuhnya sebelum yang lain. Hanya satu transaksi yang akan berhasil mendapatkan kursi; yang lain akan melihat bahwa kursi tersebut sudah tidak tersedia, sehingga mencegah masalah _double-booking_.5
    

### D - Durability (Daya Tahan)

- **Konsep**: Durabilitas menjamin bahwa setelah sebuah transaksi berhasil di-_commit_, semua perubahannya bersifat permanen dan akan bertahan bahkan jika terjadi kegagalan sistem, seperti mati listrik atau _crash_ server.3
    
- **Contoh Praktis**: Setelah Anda menerima notifikasi bahwa pembayaran tagihan kartu kredit Anda berhasil, properti durabilitas memastikan bahwa transaksi tersebut telah dicatat secara permanen. Bahkan jika server bank mengalami kegagalan satu detik kemudian, catatan pembayaran Anda tidak akan hilang saat sistem dinyalakan kembali. Jaminan ini biasanya dicapai dengan menulis perubahan ke media penyimpanan non-volatil seperti SSD atau HDD sebelum melaporkan keberhasilan _commit_.4
    

### Mengapa Properti ACID Krusial?

Secara kolektif, keempat properti ini adalah pilar keandalan database. Mereka memberikan prediktabilitas dan integritas data yang sangat penting untuk hampir semua aplikasi modern. Dalam sektor-sektor seperti perbankan, e-commerce, dan rekam medis, di mana akurasi data adalah hal yang mutlak, jaminan ACID bukanlah sebuah kemewahan, melainkan sebuah kebutuhan fundamental.3 Tanpa ACID, pengembang harus membangun logika yang sangat kompleks di tingkat aplikasi untuk menangani setiap kemungkinan kegagalan dan kondisi balapan (

_race condition_), sebuah tugas yang sangat sulit dan rentan terhadap kesalahan.

## 6.2 Manajemen Transaksi: Membangun Kerangka untuk Wheel DB

Setelah memahami jaminan teoretis yang diberikan oleh ACID, langkah selanjutnya adalah mulai membangun kerangka kerja untuk mengelola transaksi di Wheel DB. Ini melibatkan pembuatan representasi konkret dari sebuah transaksi di dalam sistem kita. Setiap transaksi yang aktif perlu diidentifikasi secara unik dan statusnya perlu dilacak sepanjang siklus hidupnya: dimulai, menjalankan operasi, dan akhirnya di-_commit_ (jika berhasil) atau di-_abort_ (jika gagal).4

### Blueprint Implementasi `TransactionId.java`

Setiap transaksi yang berjalan di dalam database harus memiliki pengenal yang unik. ID ini akan menjadi "paspor" transaksi, digunakan di seluruh sistem—mulai dari _Lock Manager_ untuk melacak kunci hingga _Log Manager_ untuk mencatat perubahan. Karena beberapa transaksi dapat dimulai secara bersamaan oleh _thread_ yang berbeda, pembuatan ID ini harus bersifat _thread-safe_.

Berikut adalah desain sederhana namun kuat untuk kelas `TransactionId`:

```java
import java.util.concurrent.atomic.AtomicLong;

/**
 * TransactionId adalah kelas untuk mengidentifikasi transaksi secara unik.
 */
public class TransactionId {
    // Sebuah counter statis yang thread-safe untuk menghasilkan ID unik.
    private static final AtomicLong counter = new AtomicLong(0);
    
    // ID unik untuk instance TransactionId ini.
    private final long id;

    public TransactionId() {
        // incrementAndGet() secara atomik menaikkan nilai dan mengembalikannya.
        this.id = counter.incrementAndGet();
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass()!= o.getClass()) return false;
        TransactionId that = (TransactionId) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
```

Dalam implementasi ini, kita menggunakan `java.util.concurrent.atomic.AtomicLong`. Kelas ini menyediakan cara yang sangat efisien dan _thread-safe_ untuk menghasilkan nomor urut. Metode `incrementAndGet()` dijamin bersifat atomik, artinya bahkan jika ratusan _thread_ memanggil konstruktor `TransactionId` secara bersamaan, masing-masing akan menerima nilai ID yang unik tanpa risiko _race condition_ dan tanpa memerlukan mekanisme _locking_ eksplisit yang lebih mahal. Implementasi `equals()` dan `hashCode()` yang benar juga sangat penting, karena `TransactionId` akan sering digunakan sebagai kunci dalam struktur data seperti `HashMap` di komponen lain (misalnya, _Lock Manager_).

### Blueprint Implementasi `Transaction.java`

Selanjutnya, kita memerlukan sebuah objek untuk merepresentasikan transaksi itu sendiri. Objek ini akan menampung `TransactionId`-nya dan melacak statusnya saat ini.

Berikut adalah kerangka untuk kelas `Transaction`:

```java
/**
 * Transaction merepresentasikan satu unit kerja logis dalam database.
 */
public class Transaction {
    
    /**
     * Status yang mungkin dari sebuah transaksi.
     */
    public enum Status {
        RUNNING,   // Transaksi sedang berjalan aktif.
        COMMITTED, // Transaksi telah berhasil diselesaikan.
        ABORTED    // Transaksi telah dibatalkan.
    }

    private final TransactionId tid;
    // 'volatile' memastikan bahwa perubahan status terlihat oleh semua thread.
    private volatile Status status;

    public Transaction() {
        this.tid = new TransactionId();
        this.status = Status.RUNNING;
    }

    public TransactionId getId() {
        return tid;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Menandai transaksi ini sebagai committed.
     * Logika sebenarnya untuk commit (misalnya, menulis log commit)
     * akan ditangani oleh Transaction Manager atau Recovery Manager.
     */
    public void commit() {
        // Implementasi detail akan dibahas di bab Recovery.
        this.status = Status.COMMITTED;
    }

    /**
     * Menandai transaksi ini sebagai aborted.
     * Logika sebenarnya untuk abort (misalnya, melakukan rollback perubahan)
     * akan ditangani oleh Transaction Manager atau Recovery Manager.
     */
    public void abort() {
        // Implementasi detail akan dibahas di bab Recovery.
        this.status = Status.ABORTED;
    }
}
```

Kelas `Transaction` ini sederhana namun fungsional. Ia memiliki `TransactionId` untuk identifikasi unik dan `status` untuk melacak siklus hidupnya. Penggunaan _keyword_ `volatile` pada variabel `status` sangat penting dalam lingkungan multi-threaded. Ini memastikan bahwa setiap kali satu _thread_ mengubah status transaksi (misalnya, dari `RUNNING` ke `COMMITTED`), perubahan tersebut segera terlihat oleh semua _thread_ lain dalam sistem.

	Kedua kelas ini, `TransactionId` dan `Transaction`, membentuk fondasi untuk semua modul yang lebih canggih. Setiap tindakan yang dilakukan atas nama transaksi—meminta _lock_, memodifikasi halaman di _buffer pool_, menulis ke log—akan dicap dengan `TransactionId`-nya. Ini menciptakan jejak audit yang jelas yang sangat penting untuk mengelola konkurensi dan memastikan pemulihan yang benar jika terjadi kegagalan.

## 6.3 Kontrol Konkurensi (Locking)

Dari keempat properti ACID, Isolasi ('I') seringkali menjadi yang paling menantang untuk diimplementasikan dengan benar dan efisien. Tujuannya adalah untuk memungkinkan banyak transaksi berjalan secara bersamaan—untuk memaksimalkan _throughput_ sistem—sambil tetap menjaga ilusi bahwa setiap transaksi berjalan sendirian. Ketika isolasi gagal, berbagai anomali data yang aneh dan merusak dapat terjadi.

### Masalah yang Muncul Akibat Konkurensi

Berikut adalah tiga masalah konkurensi klasik yang harus dicegah oleh sistem database yang baik 4:

1. **_Dirty Read_**: Terjadi ketika Transaksi A membaca data yang telah dimodifikasi oleh Transaksi B, tetapi Transaksi B belum melakukan _commit_. Jika Transaksi B kemudian memutuskan untuk _rollback_, Transaksi A sekarang memegang data "kotor" atau "hantu" yang secara teknis tidak pernah ada di database secara permanen. Keputusan yang dibuat berdasarkan data ini bisa jadi salah besar.10
    
2. **_Non-Repeatable Read_**: Terjadi ketika sebuah transaksi membaca baris data yang sama dua kali dalam siklus hidupnya, tetapi mendapatkan nilai yang berbeda setiap kali. Ini terjadi karena transaksi lain memodifikasi dan me-_commit_ baris tersebut di antara dua operasi baca. Hal ini dapat merusak konsistensi perhitungan atau validasi yang dilakukan dalam transaksi pertama.12
    
3. **_Phantom Read_**: Mirip dengan _non-repeatable read_, tetapi melibatkan _kumpulan_ baris, bukan satu baris. Ini terjadi ketika sebuah transaksi menjalankan _query_ yang sama dua kali, tetapi mendapatkan jumlah baris yang berbeda. Hal ini disebabkan oleh transaksi lain yang menyisipkan atau menghapus baris yang cocok dengan kriteria _query_ di antara dua eksekusi. Ini bisa menyebabkan masalah dalam laporan agregat atau operasi _batch_.13
    

### Implementasi Locking Sederhana

Mekanisme yang paling umum digunakan untuk menegakkan isolasi dan mencegah anomali-anomali ini adalah _locking_. Idenya sederhana: sebelum sebuah transaksi dapat mengakses (membaca atau menulis) sepotong data, ia harus terlebih dahulu memperoleh "kunci" (_lock_) pada data tersebut. Kunci ini mencegah transaksi lain untuk melakukan operasi yang berpotensi konflik.

Untuk Wheel DB, kita akan mengimplementasikan dua jenis _lock_ dasar:

- **_Shared Lock (S-lock)_**: Juga dikenal sebagai _read lock_. Beberapa transaksi dapat memegang S-lock pada item data yang sama secara bersamaan. Sebuah transaksi harus memegang S-lock untuk membaca data.
    
- **_Exclusive Lock (X-lock)_**: Juga dikenal sebagai _write lock_. Hanya satu transaksi yang dapat memegang X-lock pada suatu item data pada satu waktu. Jika satu transaksi memegang X-lock, tidak ada transaksi lain (bahkan untuk membaca) yang dapat memperoleh _lock_ apa pun pada data tersebut. Sebuah transaksi harus memperoleh X-lock sebelum memodifikasi (menulis, memperbarui, atau menghapus) data.
    

Aturan interaksi antara kedua jenis _lock_ ini dapat diringkas dalam sebuah **tabel kompatibilitas _lock_**:

|Lock yang Diminta|Lock yang Sudah Dipegang (Shared)|Lock yang Sudah Dipegang (Exclusive)|
|---|---|---|
|**Shared**|DIIZINKAN|DITOLAK|
|**Exclusive**|DITOLAK|DITOLAK|

Tabel ini secara visual memperkuat aturan dasar konkurensi: banyak pembaca diizinkan secara bersamaan (`Shared-Shared`), tetapi begitu ada penulis, ia harus memiliki akses eksklusif.

### Blueprint untuk Lock Manager

Sekarang kita akan merancang sebuah komponen terpusat, `LockManager`, yang bertanggung jawab untuk memberikan dan melepaskan _lock_ atas nama transaksi. Untuk Wheel DB, kita akan membuat keputusan desain untuk mengunci pada tingkat **halaman** (_page_). Ini adalah pilihan yang umum karena selaras dengan cara _Buffer Manager_ mengelola data dalam unit-unit halaman.

```java
public class LockManager {

    // Tipe lock: Shared (untuk baca) dan Exclusive (untuk tulis).
    public enum LockType { SHARED, EXCLUSIVE }

    // Struktur data untuk menyimpan lock. 
    // Kunci adalah PageId, nilainya adalah objek yang mengelola lock untuk halaman itu.
    private Map<PageId, PageLockManager> lockTable;

    /**
     * Meminta lock untuk sebuah transaksi pada halaman tertentu.
     * Metode ini akan memblokir sampai lock dapat diberikan.
     * @param tid ID transaksi yang meminta lock.
     * @param pid ID halaman yang ingin di-lock.
     * @param type Tipe lock yang diminta (SHARED atau EXCLUSIVE).
     */
    public synchronized void acquireLock(TransactionId tid, PageId pid, LockType type) throws TransactionAbortedException {
        
        // 1. Dapatkan atau buat manajer lock untuk PageId 'pid'.
        // 2. Periksa apakah permintaan lock 'type' dari 'tid' kompatibel
        //    dengan lock yang sudah dipegang oleh transaksi lain pada halaman ini.
        // 3. Jika kompatibel:
        //    a. Berikan lock ke 'tid'.
        //    b. Catat bahwa 'tid' sekarang memegang lock pada 'pid'.
        // 4. Jika tidak kompatibel:
        //    a. Transaksi 'tid' harus menunggu. Gunakan mekanisme seperti wait().
        //    b. Sebelum menunggu, lakukan deteksi deadlock. Jika 'tid' menyebabkan
        //       deadlock, batalkan transaksi dengan melempar TransactionAbortedException.
        //       Strategi sederhana bisa menggunakan timeout.
    }

    /**
     * Melepaskan lock yang dipegang oleh sebuah transaksi pada halaman tertentu.
     * @param tid ID transaksi yang melepaskan lock.
     * @param pid ID halaman yang lock-nya dilepaskan.
     */
    public synchronized void releaseLock(TransactionId tid, PageId pid) {
        
        // 1. Temukan manajer lock untuk PageId 'pid'.
        // 2. Hapus lock yang dipegang oleh 'tid' dari halaman tersebut.
        // 3. Setelah lock dilepaskan, bangunkan semua thread/transaksi lain
        //    yang mungkin sedang menunggu lock pada halaman ini (menggunakan notifyAll()).
        //    Mereka sekarang dapat mencoba kembali untuk memperoleh lock.
    }
    
    /**
     * Melepaskan semua lock yang dipegang oleh sebuah transaksi.
     * Metode ini dipanggil saat transaksi commit atau abort.
     * @param tid ID transaksi yang semua lock-nya akan dilepaskan.
     */
    public synchronized void releaseAllLocks(TransactionId tid) {
        // Iterasi melalui semua lock yang dipegang oleh 'tid' dan panggil releaseLock().
    }
}
```

Kerangka di atas menguraikan logika inti dari `LockManager`. Penggunaan `synchronized` pada metode-metode ini sangat penting untuk mencegah _race conditions_ di dalam _LockManager_ itu sendiri.

Namun, pengenalan _locking_ membawa konsekuensi yang tak terhindarkan: kemungkinan terjadinya **deadlock**. _Deadlock_ adalah situasi di mana dua atau lebih transaksi saling menunggu selamanya. Contohnya: Transaksi A memegang _lock_ pada Halaman 1 dan menunggu _lock_ pada Halaman 2, sementara Transaksi B memegang _lock_ pada Halaman 2 dan menunggu _lock_ pada Halaman 1. Keduanya tidak akan pernah bisa melanjutkan.

Oleh karena itu, setiap `LockManager` yang fungsional harus memiliki strategi untuk menangani _deadlock_. Beberapa pendekatan umum meliputi:

- **Pencegahan (_Prevention_)**: Merancang sistem sehingga _deadlock_ secara struktural tidak mungkin terjadi (misalnya, memaksa semua transaksi untuk meminta _lock_ dalam urutan global yang sama).
    
- **Deteksi (_Detection_)**: Secara berkala membangun grafik ketergantungan (_waits-for graph_) dan mencari siklus. Jika siklus ditemukan, salah satu transaksi dalam siklus tersebut "dikorbankan" (di-_abort_) untuk memutus kebuntuan.
    
- **_Timeouts_**: Membatalkan transaksi yang telah menunggu _lock_ lebih lama dari batas waktu yang ditentukan.
    

Untuk Wheel DB, implementasi deteksi _deadlock_ penuh dengan _waits-for graph_ mungkin terlalu kompleks untuk tahap ini. Pendekatan yang lebih praktis dan sederhana adalah menggunakan **timeouts**. Jika sebuah transaksi gagal memperoleh _lock_ dalam, katakanlah, beberapa detik, `acquireLock` akan menyerah dan melempar `TransactionAbortedException`, memaksa transaksi tersebut untuk di-_rollback_ dan dicoba lagi nanti. Ini adalah strategi yang efektif untuk menjaga sistem tetap berjalan meskipun tidak se-elegan deteksi siklus.

## Kesimpulan dan Langkah Selanjutnya

Dalam bab ini, kita telah meletakkan fondasi teoretis dan praktis untuk membuat Wheel DB menjadi sistem yang andal. Kita telah menjelajahi properti **ACID** sebagai kontrak jaminan yang mengubah operasi data sederhana menjadi transaksi yang kuat. Kita juga telah merancang komponen inti untuk **Manajemen Transaksi** (`TransactionId` dan `Transaction`) dan **Kontrol Konkurensi** (`LockManager`). Komponen-komponen ini bekerja sama untuk menegakkan integritas data, terutama properti Isolasi, dengan mencegah anomali seperti _dirty reads_ dan _non-repeatable reads_.

Dengan mengimplementasikan _locking_, kita telah memastikan bahwa beberapa pengguna dapat mengakses database secara bersamaan tanpa merusak data satu sama lain. Namun, perjalanan kita belum selesai. _Locking_ secara efektif menangani 'I' (Isolasi) dari ACID. Kita masih memerlukan mekanisme yang kuat untuk menegakkan 'A' (Atomisitas) dan 'D' (Durabilitas), terutama dalam menghadapi kegagalan sistem yang tak terduga seperti _crash_ atau mati listrik.

Ini membawa kita ke jembatan menuju bab berikutnya: **Bab 7: Pemulihan (_Recovery_)**. Di bab tersebut, kita akan membahas kebutuhan akan _logging_. Kita akan belajar tentang teknik fundamental yang disebut _Write-Ahead Logging_ (WAL), di mana setiap perubahan dicatat ke dalam log yang tahan lama (_durable_) sebelum perubahan tersebut diterapkan ke halaman data di disk. Log inilah yang akan menjadi kunci untuk:

1. Melakukan _rollback_ pada transaksi yang dibatalkan, sehingga menegakkan **Atomisitas**.
    
2. Memastikan bahwa perubahan dari transaksi yang sudah di-_commit_ tidak hilang setelah _crash_, sehingga menegakkan **Durabilitas**.
    

Dengan menggabungkan manajemen transaksi, kontrol konkurensi, dan sistem pemulihan berbasis log, kita akan melengkapi tiga pilar utama yang menjadikan sebuah sistem penyimpanan data layak disebut sebagai sistem manajemen database.