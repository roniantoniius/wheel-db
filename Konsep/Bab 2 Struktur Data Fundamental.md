# Bab 2: Struktur Data Fundamental

https://github.com/dborchard/tiny-db
## Pendahuluan: Fondasi Representasi Data di Wheel DB

Bab ini akan membedah komponen-komponen inti yang bertanggung jawab untuk merepresentasikan data di dalam mesin Wheel DB. Kekuatan, efisiensi, dan batasan dari sebuah sistem database secara fundamental ditentukan oleh bagaimana ia menstrukturkan data pada level terendah. Pemahaman mendalam terhadap struktur data ini adalah prasyarat untuk membangun dan mengoptimalkan fungsionalitas yang lebih tinggi seperti _query processing_, manajemen transaksi, dan penyimpanan fisik.

Analisis akan berpusat pada empat kelas atau entitas utama yang membentuk hierarki abstraksi data: `$Type$`, `$Field$`, `$TupleDesc$`, dan `$Tuple$`. Keempatnya bekerja secara sinergis untuk memodelkan data relasional. Peran masing-masing dapat diringkas sebagai berikut:

- `$Type$`: Sebuah enumerasi yang mendefinisikan tipe-tipe data primitif yang didukung oleh database, seperti bilangan bulat (_integer_) dan _string_.
    
- `$Field$`: Sebuah _interface_ yang mengabstraksikan dan mengenkapsulasi sebuah nilai data tunggal, seperti angka 42 atau teks 'Alice'.
    
- `$TupleDesc$`: Sebuah kelas yang berfungsi sebagai deskriptor skema (_schema descriptor_). Ia mendefinisikan struktur dari sebuah _record_ atau baris, termasuk jumlah kolom, serta tipe dan nama dari setiap kolom tersebut.
    
- `$Tuple$`: Entitas data utama yang merepresentasikan satu baris (_row_) atau _record_ dalam sebuah tabel. Ia merupakan kontainer untuk sekumpulan objek `$Field$` yang strukturnya diatur oleh sebuah objek `$TupleDesc$`.
    

Hubungan antara komponen-komponen ini dapat divisualisasikan secara konseptual: sebuah objek `$Tuple$` berisi beberapa objek `$Field$`. Struktur dari `$Tuple$` ini didefinisikan secara ketat oleh satu objek `$TupleDesc$`. Objek `$TupleDesc$` itu sendiri menyimpan metadata (dalam bentuk objek internal `$TDItem$`) yang merinci `$Type$` dari setiap `$Field$` yang terkandung di dalam `$Tuple$`.

Penting untuk dicatat bahwa desain struktur data dalam sistem ini didasarkan pada sebuah prinsip pedagogis yang esensial: kejelasan dan kemudahan pemahaman untuk tujuan pembelajaran.1 Hal ini terlihat jelas dari pilihan desain yang secara sengaja menyederhanakan beberapa aspek. Misalnya, sistem ini secara eksklusif mendukung

_tuple_ dengan panjang tetap (_fixed-length_) dan hanya menyediakan dua tipe data dasar: _integer_ dan _string_ dengan panjang yang juga tetap.2 Keputusan ini bukanlah sebuah kelemahan desain, melainkan sebuah pilihan sadar untuk memitigasi kompleksitas. Penggunaan data dengan panjang variabel akan memperkenalkan tantangan signifikan dalam manajemen halaman disk (

_disk page management_), alokasi memori di dalam _buffer pool_, dan proses serialisasi. Topik-topik kompleks seperti _slotted pages_ atau fragmentasi data akan mengalihkan fokus dari tujuan pembelajaran inti. Oleh karena itu, batasan-batasan yang tampak pada model data ini harus dipandang sebagai alat bantu instruksional yang memungkinkan pengembang untuk fokus pada konsep fundamental arsitektur database.

## 2.1 Tipe Data Primitif: Enumerasi `Type`

Di dasar hierarki struktur data Wheel DB terletak enumerasi `$Type$`. Entitas ini adalah blok bangunan paling fundamental yang mendefinisikan "kosakata" tipe data yang sah di seluruh sistem. Meskipun merupakan komponen yang paling sederhana, perannya sangat krusial dalam menyediakan fondasi untuk keamanan tipe (_type safety_) dan manajemen penyimpanan fisik. Berdasarkan penggunaannya di dalam kelas `$TupleDesc$` 4, dapat disimpulkan bahwa

`$Type$` adalah sebuah `enum` Java yang setidaknya mengandung dua nilai: `$INT_TYPE$` untuk merepresentasikan bilangan bulat dan `$STRING_TYPE$` untuk merepresentasikan data teks.

Fungsi paling vital dari `enum` `$Type$` adalah metode `$getLen()$`. Metode ini dipanggil oleh `$TupleDesc.getSize()$` untuk menghitung total ukuran sebuah _tuple_ dalam satuan _byte_. Setiap anggota `enum` `$Type$` mengembalikan sebuah nilai konstan yang merepresentasikan ukuran fisiknya di dalam penyimpanan. Sebagai contoh, `$INT_TYPE$` akan selalu mengembalikan 4, sesuai dengan ukuran standar sebuah _integer_ 32-bit dalam Java. Sementara itu, `$STRING_TYPE$` akan mengembalikan sebuah nilai konstan yang telah ditentukan sebelumnya, yang mencerminkan desain sistem yang hanya mendukung _string_ dengan panjang tetap. Metode `$getLen()$`, oleh karena itu, adalah pilar utama dari desain _tuple_ berukuran tetap yang menjadi ciri khas sistem ini.

Selain manajemen ukuran, `enum` `$Type$` juga berperan penting dalam memastikan integritas data di seluruh mesin database. Dengan menyediakan satu set tipe data yang terbatas dan terdefinisi dengan baik, ia mencegah masuknya tipe data yang tidak didukung. Hal ini memungkinkan komponen-komponen tingkat lebih tinggi, seperti operator _query_, untuk secara andal memeriksa tipe data sebuah kolom dan melakukan operasi yang sesuai tanpa perlu khawatir akan tipe data yang tidak terduga.

Tabel berikut merangkum tipe-tipe data yang didukung oleh sistem, beserta properti-properti kuncinya.

| Nilai Enum      | Ukuran (Bytes) | Kelas Implementasi Field | Deskripsi                                                                                                            |
| --------------- | -------------- | ------------------------ | -------------------------------------------------------------------------------------------------------------------- |
| `$INT_TYPE$`    | 4              | `$IntField$`             | Merepresentasikan nilai bilangan bulat (integer) 32-bit.                                                             |
| `$STRING_TYPE$` | 128 (Contoh)   | `$StringField$`          | Merepresentasikan nilai teks (string) dengan panjang tetap. Ukuran pastinya adalah konstanta yang ditentukan sistem. |

Tabel ini berfungsi sebagai referensi tunggal yang menyatukan tiga aspek penting dari setiap tipe data: representasi logisnya (nilai `enum`), jejak fisiknya di penyimpanan (ukuran dalam _byte_), dan kelas Java yang bertanggung jawab untuk mengimplementasikan perilakunya (`$Field$`). Konsolidasi informasi ini sangat penting bagi pengembang yang perlu memahami bagaimana data direpresentasikan baik secara konseptual maupun fisik.

## 2.2 Representasi Nilai Individual: Interface `Field`

Satu tingkat di atas `$Type$` dalam hierarki abstraksi adalah _interface_ `$Field$`. Jika `$Type$` mendefinisikan _jenis_ data, maka `$Field$` adalah abstraksi yang merepresentasikan _sebuah nilai data aktual_ dari jenis tersebut. Ia berfungsi sebagai pembungkus (_wrapper_) untuk nilai-nilai individual di dalam sebuah _tuple_, seperti angka 101 atau _string_ "Database Systems". Sebagai sebuah _interface_, `$Field$` mendefinisikan sebuah kontrak yang harus dipenuhi oleh semua kelas yang merepresentasikan tipe data konkret.1

Kontrak yang didefinisikan oleh _interface_ `$Field$` mencakup beberapa metode esensial:

- `$getType()`: Metode ini mengembalikan `enum` `$Type$` yang sesuai dengan nilai yang dikandungnya. Ini memungkinkan sistem untuk menanyakan tipe dari sebuah `$Field$` secara dinamis.
    
- `$getValue()`: Mengembalikan objek Java mentah yang dibungkus oleh `$Field$`, misalnya sebuah objek `Integer` atau `String`.
    
- `$serialize(DataOutputStream dos)`: Metode ini bertanggung jawab untuk menulis representasi biner dari nilai yang dikandungnya ke sebuah _output stream_. Ini adalah mekanisme kunci yang digunakan untuk menyimpan data _tuple_ ke halaman disk.
    
- `$compare(Predicate.Op op, Field val)`: Metode ini mengimplementasikan logika perbandingan antara nilai `$Field$` ini dengan nilai `$Field$` lain, menggunakan sebuah operator perbandingan (misalnya, sama dengan, lebih besar dari).
    

Sistem ini menyediakan setidaknya dua implementasi konkret dari _interface_ `$Field$`:

1. `$IntField$`: Kelas ini membungkus sebuah nilai `int` primitif Java. Metode `$serialize()`-nya akan menulis 4 _byte_ representasi _integer_ tersebut, dan metode `$compare()`-nya akan melakukan perbandingan numerik.
    
2. `$StringField$`: Kelas ini membungkus sebuah nilai `String` Java. Metode `$serialize()`-nya akan menulis representasi _byte_ dari _string_ tersebut, dengan memastikan panjangnya sesuai dengan konstanta yang telah ditentukan untuk `$STRING_TYPE$`. Metode `$compare()`-nya akan melakukan perbandingan leksikografis.
    

Peran `$Field$` jauh melampaui sekadar menjadi kontainer data. Ia adalah jembatan krusial yang menghubungkan dunia fisik penyimpanan data dengan dunia logis pemrosesan _query_. Metode `$serialize()` secara langsung menangani bagaimana data logis diubah menjadi _byte_ untuk persistensi. Sebaliknya, metode `$compare()` adalah titik di mana logika abstrak dari sebuah _query_ dieksekusi terhadap data fisik.

Untuk memahami pentingnya hal ini, pertimbangkan bagaimana sebuah _query_ `SELECT * FROM users WHERE age > 30` dieksekusi.

1. _Query_ tersebut akan diurai menjadi sebuah pohon operator relasional, di mana salah satu nodenya adalah operator `$Filter$`.3
    
2. Operator `$Filter$` ini akan dikonfigurasi dengan sebuah objek `$Predicate$` yang merepresentasikan kondisi `age > 30`. `$Predicate$` ini akan menyimpan tiga informasi: **indeks kolom 'age',** operator perbandingan `$Predicate.Op.GREATER_THAN$`, dan nilai pembanding yang direpresentasikan sebagai objek `$Field$` (dalam hal ini, sebuah `$IntField$` dengan nilai 30).
    
3. Ketika operator `$Filter$` memproses setiap `$Tuple$` dari tabel `users`, ia akan mengekstrak `$Field$` dari kolom 'age' menggunakan `$tuple.getField(ageIndex)$`.
    
4. Selanjutnya, ia akan memanggil metode `$compare()` pada `$Field$` yang diekstrak tersebut: `$ageField.compare(Predicate.Op.GREATER_THAN, new IntField(30))$`.
    
5. Logika untuk benar-benar membandingkan dua nilai _integer_ sepenuhnya terkandung di dalam kelas `$IntField$`. Operator `$Filter$` itu sendiri tidak perlu tahu bagaimana cara membandingkan _integer_, _string_, atau tipe data lainnya. Ia hanya bergantung pada kontrak yang disediakan oleh _interface_ `$Field$`.
    

Mekanisme ini adalah contoh penerapan polimorfisme yang kuat. Ia memisahkan (_decouple_) mesin eksekusi _query_ dari detail implementasi tipe data spesifik. Hal ini membuat sistem menjadi lebih modular dan mudah untuk diperluas. Jika di masa depan tipe data baru (misalnya, `$DateField$`) perlu ditambahkan, pengembang hanya perlu membuat kelas implementasi `$Field$` yang baru, tanpa harus memodifikasi logika di dalam operator-operator seperti `$Filter$` atau `$Join$`. Dengan demikian, `$Field$` bukan hanya sebuah kontainer data, tetapi juga komponen arsitektural fundamental yang memungkinkan pemrosesan _query_ yang bersih dan dapat diperluas.

## 2.3 Mendefinisikan Skema Relasi: Class `TupleDesc`

Kelas `$TupleDesc$` (Tuple Descriptor) adalah representasi _in-memory_ dari skema sebuah tabel atau relasi. Ia berfungsi sebagai cetak biru (_blueprint_) yang mendefinisikan struktur dari setiap `$Tuple$` yang termasuk dalam tabel tersebut. Tanpa `$TupleDesc$`, sebuah `$Tuple$` hanyalah sekumpulan data tanpa makna; `$TupleDesc$`-lah yang memberikan konteks dengan menentukan jumlah kolom, serta tipe dan nama untuk setiap kolomnya.6

### Struktur Internal dan Desain

Berdasarkan analisis kode sumber yang tersedia 4, struktur internal

`$TupleDesc$` dirancang untuk efisiensi dan kejelasan.

- **`TDItem`**: Di dalam `$TupleDesc$`, terdapat kelas statis internal bernama `$TDItem$`. Kelas ini berfungsi sebagai struktur data sederhana (mirip `struct` di C) yang mengelompokkan dua informasi penting untuk sebuah kolom: `$Type fieldType$` (tipe data kolom) dan `$String fieldName$` (nama kolom).
    
- **`List<TDItem>`**: `$TupleDesc$` menggunakan sebuah `$List` dari `$TDItem$` sebagai struktur data utamanya untuk menyimpan informasi skema. Penggunaan `$List$` mempertahankan urutan kolom, yang merupakan aspek fundamental dari model relasional. Urutan ini penting karena data di dalam `$Tuple$` diakses berdasarkan indeks numerik.
    
- **`HashMap<String, Integer>`**: Sebagai sebuah optimisasi performa, implementasi `$TupleDesc$` juga menyertakan sebuah `$HashMap` yang memetakan nama kolom (`String`) ke indeks posisinya (`Integer`). Hashmap ini, yang diinisialisasi saat konstruktor dipanggil, memungkinkan metode `$fieldNameToIndex()` untuk menemukan indeks sebuah kolom berdasarkan namanya dalam waktu konstan, `$O(1)$`, alih-alih melakukan pemindaian linear pada `$List<TDItem>` yang akan memakan waktu `$O(n)$`, di mana `$n$` adalah jumlah kolom.
    

### Analisis Metode Kunci

Fungsionalitas `$TupleDesc$` diekspos melalui serangkaian metode yang terdefinisi dengan baik 4:

- **Konstruktor**: Terdapat dua konstruktor utama. `$TupleDesc(Type typeAr, String fieldAr)$` digunakan untuk membuat skema dengan tipe dan nama kolom yang spesifik. Konstruktor kedua, `$TupleDesc(Type typeAr)$`, digunakan untuk membuat skema di mana kolom-kolomnya tidak memiliki nama (anonim).
    
- **Metode Akses**: Metode seperti `$numFields()$`, `$getFieldName(int i)$`, dan `$getFieldType(int i)` menyediakan API dasar untuk melakukan introspeksi terhadap skema. Operator-operator _query_ menggunakan metode ini untuk memahami struktur _tuple_ yang sedang mereka proses.
    
- **`getSize()`**: Metode ini memiliki peran krusial dalam menjembatani skema logis dengan kebutuhan penyimpanan fisik. Implementasinya sangat sederhana namun penting: ia melakukan iterasi melalui daftar `$TDItem$` dan menjumlahkan hasil panggilan `$fieldType.getLen()` untuk setiap item. Hasilnya adalah ukuran total dalam _byte_ yang dibutuhkan oleh sebuah `$Tuple$` yang sesuai dengan skema ini. Nilai ini esensial bagi _storage manager_ untuk mengalokasikan ruang di dalam halaman disk.
    
- **`merge(TupleDesc td1, TupleDesc td2)`**: Metode statis ini bukan sekadar utilitas untuk menggabungkan dua skema. Ia adalah komponen fundamental yang memungkinkan operasi relasional `$JOIN$`. Ketika dua tabel digabungkan, _tuple_ hasil gabungan akan memiliki skema baru yang merupakan gabungan dari skema kedua tabel asli. Metode `$merge()` inilah yang bertanggung jawab untuk membuat objek `$TupleDesc$` baru untuk _tuple_ hasil `$JOIN$` tersebut.
    
- **`equals(Object o)`**: Implementasi metode ini mendefinisikan konsep "kompatibilitas skema". Dua objek `$TupleDesc$` dianggap sama jika dan hanya jika mereka memiliki jumlah kolom yang sama, dan tipe data untuk setiap kolom pada posisi yang sama juga identik. Nama kolom tidak dipertimbangkan dalam perbandingan ini. Kesetaraan skema ini sangat penting bagi operator seperti `$UNION$` atau saat memverifikasi bahwa _tuple_ yang akan disisipkan ke dalam sebuah tabel memang memiliki struktur yang benar.
    

Salah satu aspek desain yang paling penting dari `$TupleDesc$` adalah sifatnya yang secara efektif tidak dapat diubah (_immutable_). Setelah sebuah objek `$TupleDesc$` dibuat melalui konstruktornya, tidak ada metode publik yang tersedia untuk menambah, menghapus, atau mengubah `$TDItem$` di dalamnya.4 Sifat

_immutable_ ini bukanlah kebetulan, melainkan sebuah keputusan desain yang kritikal untuk stabilitas sistem.

Setiap objek `$Tuple$` menyimpan sebuah referensi ke objek `$TupleDesc$`-nya. Jika objek `$TupleDesc$` ini dapat diubah, maka satu perubahan kecil—misalnya, mengubah tipe kolom kedua dari `$INT_TYPE$` menjadi `$STRING_TYPE$`—akan secara diam-diam dan katastropik mengubah makna dari setiap objek `$Tuple$` yang ada yang merujuk padanya. Hal ini akan menyebabkan ketidaksesuaian antara data yang disimpan (misalnya, sebuah `$IntField$`) dan skema yang mendefinisikannya, yang hampir pasti akan mengakibatkan kesalahan saat runtime dan potensi kerusakan data. Dengan menjadikan `$TupleDesc$` _immutable_, ia berfungsi sebagai sebuah kontrak yang stabil dan andal. Semua komponen dalam mesin database, mulai dari _access methods_ hingga _query operators_, dapat berinteraksi dengan _tuple_ dengan keyakinan penuh bahwa skemanya tidak akan berubah secara tak terduga.

## 2.4 Entitas Data Utama: Class `Tuple`

Kelas `$Tuple$` adalah representasi konkret dari satu baris atau _record_ dalam sebuah tabel. Ia adalah objek pekerja keras dalam sistem database, menjadi unit data utama yang mengalir melalui pohon operator selama eksekusi _query_. **Setiap `$Tuple$` adalah kontainer terurut untuk sekumpulan objek `$Field$`, yang strukturnya ditentukan secara ketat oleh sebuah objek `$TupleDesc$`**.7

### Struktur Internal dan Hubungannya

Dari analisis kode sumber 4, struktur internal

`$Tuple$` terdiri dari tiga atribut kunci yang mendefinisikan identitas dan isinya:

- `private TupleDesc td`: Sebuah referensi ke objek `$TupleDesc$` yang mendefinisikan skema _tuple_ ini. Atribut ini bersifat final dan diinisialisasi saat konstruksi, memastikan bahwa sebuah _tuple_ tidak dapat ada tanpa skema dan skemanya tidak dapat diubah selama masa hidupnya.
    
- `private RecordId recordId`: Sebuah objek yang menyimpan informasi lokasi fisik _tuple_ ini di dalam disk. `$RecordId$` biasanya terdiri dari `$PageId$` (yang mengidentifikasi halaman disk tempat _tuple_ berada) dan nomor slot _tuple_ di dalam halaman tersebut. Atribut ini bisa bernilai `null` jika _tuple_ tersebut baru dibuat dan belum disimpan ke disk.
    
- `private List<Field> fields`: Sebuah daftar yang berisi objek-objek `$Field$` yang merupakan data aktual dari _tuple_ ini. Urutan `$Field$` dalam daftar ini harus sesuai dengan urutan kolom yang didefinisikan dalam `$TupleDesc$`.
    

Ketiga atribut ini menunjukkan bahwa sebuah `$Tuple$` tidak hanya menyimpan data (`$fields$`), tetapi juga sadar akan strukturnya (`$td$`) dan lokasi fisiknya (`$recordId$`). Keterkaitan ini sangat penting untuk operasi database.

### Analisis Metode Kunci

Interaksi dengan objek `$Tuple$` dilakukan melalui serangkaian metode yang jelas dan bertujuan 4:

- **`Tuple(TupleDesc td)`**: Konstruktor ini menegakkan aturan bahwa setiap `$Tuple$` harus terikat pada sebuah skema. Ia menerima sebuah objek `$TupleDesc$` dan menginisialisasi _tuple_ agar sesuai dengan struktur tersebut. Pada titik ini, _tuple_ masih "kosong"; ia belum berisi nilai `$Field$` apa pun.
    
- **`getTupleDesc()`**: Metode ini mengembalikan referensi ke objek `$TupleDesc$` yang terkait, memungkinkan kode lain untuk memeriksa skema _tuple_ (misalnya, untuk mengetahui tipe data kolom ke-i).
    
- **`setField(int i, Field f)` dan `getField(int i)`**: Ini adalah metode inti untuk manipulasi data. `$setField()` digunakan untuk mengisi atau memperbarui nilai pada indeks kolom tertentu. Penting untuk dicatat bahwa metode ini tidak melakukan pengecekan tipe secara internal; ia hanya menempatkan objek `$Field$` yang diberikan ke dalam daftar. Tanggung jawab untuk memastikan bahwa tipe `$Field$` yang disisipkan sesuai dengan tipe yang didefinisikan dalam `$TupleDesc$` berada di tangan kode pemanggil (misalnya, operator `$INSERT$`). `$getField()` digunakan untuk mengambil nilai dari kolom tertentu. indeks `i` dipakai untuk lihat indeks `TupleDesc` dan `List<Field>`
    
- **`setRecordId(RecordId rid)` dan `getRecordId()`**: Metode-metode ini mengelola hubungan antara _tuple_ logis dan lokasi penyimpanannya. Ketika sebuah _tuple_ ditulis ke halaman disk oleh _buffer pool_, `$setRecordId()` akan dipanggil untuk mencatat lokasinya. Sebaliknya, ketika operator perlu memperbarui atau menghapus _tuple_ tertentu, `$getRecordId()` digunakan untuk menemukan lokasi fisiknya.
    
- **`toString()`**: Metode ini menyediakan representasi _string_ dari konten _tuple_, biasanya dengan memisahkan nilai-nilai `$Field$` dengan spasi atau tab. Ini sangat berguna untuk keperluan _debugging_ dan pengujian sistem.7
    

Tabel berikut memberikan ringkasan cepat dari API kunci untuk kelas `$Tuple$`.

|Metode|Parameter|Nilai Kembali|Deskripsi|
|---|---|---|---|
|`$Tuple(TupleDesc td)$`|`$TupleDesc td$`|(none)|Membuat objek `$Tuple$` baru yang kosong sesuai dengan skema yang diberikan.|
|`$getTupleDesc()`|(none)|`$TupleDesc$`|Mengembalikan skema (`$TupleDesc$`) yang mendefinisikan struktur _tuple_ ini.|
|`$getField(int i)$`|`$int i$`|`$Field$`|Mengambil nilai `$Field$` dari kolom pada indeks ke-i.|
|`$setField(int i, Field f)$`|`$int i, Field f$`|`void`|Mengatur atau memperbarui nilai `$Field$` pada kolom indeks ke-i.|
|`$getRecordId()`|(none)|`$RecordId$`|Mengembalikan ID rekaman yang menunjukkan lokasi fisik _tuple_ di disk.|
|`$setRecordId(RecordId rid)$`|`$RecordId rid$`|`void`|Mengatur ID rekaman untuk _tuple_ ini.|

Ringkasan ini menyoroti peran ganda dari kelas `$Tuple$`: ia tidak hanya sebagai pembawa data melalui metode `$getField()` dan `$setField()$`, tetapi juga sebagai entitas sadar-metadata melalui metode `$getTupleDesc()` dan `$getRecordId()$`, yang menjadikannya unit informasi yang lengkap dalam ekosistem database.

## 2.5 Sintesis: Interaksi Antar Komponen Struktur Data

Setelah membedah setiap komponen secara individual—`$Type$`, `$Field$`, `$TupleDesc$`, dan `$Tuple$`—langkah terakhir adalah mensintesis pemahaman ini untuk melihat bagaimana mereka berkolaborasi sebagai satu sistem yang kohesif untuk memodelkan data relasional. Keempat komponen ini membentuk sebuah hierarki yang terstruktur dengan baik, di mana setiap lapisan bergantung pada lapisan di bawahnya untuk menyediakan abstraksi yang diperlukan.

### Contoh Kode End-to-End

Contoh kode Java berikut mendemonstrasikan siklus hidup lengkap dari pembuatan skema hingga populasi dan pembacaan sebuah _tuple_. Ini mengilustrasikan interaksi praktis antar komponen.

```java
// 1. Mendefinisikan Tipe dan Nama Kolom untuk Skema
// Mendefinisikan tipe data untuk setiap kolom dalam tabel 'mahasiswa'.
// Kolom pertama adalah ID (integer), dan yang kedua adalah nama (string).
Type types = new Type{ Type.INT_TYPE, Type.STRING_TYPE };
// Mendefinisikan nama untuk setiap kolom yang sesuai.
String names = new String{ "mahasiswa_id", "nama_mahasiswa" };

// 2. Membuat Objek TupleDesc (Skema)
// Menginstansiasi objek TupleDesc yang akan berfungsi sebagai cetak biru
// untuk semua tuple dalam tabel 'mahasiswa'.
TupleDesc schemaMahasiswa = new TupleDesc(types, names);

// 3. Membuat Objek Tuple Baru Berdasarkan Skema
// Menginstansiasi sebuah tuple kosong yang sesuai dengan skemaMahasiswa.
// Pada titik ini, tuple sudah memiliki struktur tetapi belum memiliki data.
Tuple tupleData = new Tuple(schemaMahasiswa);

// 4. Membuat Objek Field untuk Setiap Nilai Data
// Membuat objek IntField untuk merepresentasikan nilai ID mahasiswa.
Field fieldId = new IntField(101);
// Membuat objek StringField untuk merepresentasikan nama mahasiswa.
// Panjang string harus sesuai dengan yang didefinisikan oleh STRING_TYPE.getLen().
Field fieldNama = new StringField("Budi Santoso", Type.STRING_TYPE.getLen());

// 5. Mengisi (Populate) Tuple dengan Objek Field
// Menggunakan metode setField() untuk menempatkan nilai-nilai ke dalam tuple
// pada indeks yang benar sesuai dengan skema.
tupleData.setField(0, fieldId);       // Menempatkan ID pada kolom pertama (indeks 0).
tupleData.setField(1, fieldNama);     // Menempatkan nama pada kolom kedua (indeks 1).

// 6. Mengambil dan Menggunakan Data dari Tuple
// Mengambil kembali objek Field dari tuple dan mengekstrak nilainya.
// Pertama, kita verifikasi bahwa skema yang digunakan benar.
TupleDesc retrievedSchema = tupleData.getTupleDesc();
System.out.println("Skema Tuple: " + retrievedSchema.toString());

// Mengambil nilai dari kolom pertama dan melakukan cast ke tipe yang benar.
IntField retrievedIdField = (IntField) tupleData.getField(0);
int id = retrievedIdField.getValue();

// Mengambil nilai dari kolom kedua.
StringField retrievedNamaField = (StringField) tupleData.getField(1);
String nama = retrievedNamaField.getValue();

// Menampilkan hasil yang telah diekstrak.
System.out.println("ID Mahasiswa: " + id);
System.out.println("Nama Mahasiswa: " + nama);

// Output yang diharapkan:
// Skema Tuple: mahasiswa_id(INT_TYPE),nama_mahasiswa(STRING_TYPE)
// ID Mahasiswa: 101
// Nama Mahasiswa: Budi Santoso
```

### Jembatan Antara Model Logis dan Penyimpanan Fisik

Sistem struktur data yang telah diuraikan ini berfungsi sebagai jembatan fundamental antara model relasional logis yang dilihat oleh pengguna dan lapisan penyimpanan fisik yang mengelola _byte_ di disk.

1. **Konsep Logis**: Sebuah "baris" dalam tabel `mahasiswa` dengan skema `(ID INT, Nama STRING)` adalah sebuah konsep logis.

2. **Representasi In-Memory**: Konsep logis ini diwujudkan dalam memori sebagai sebuah objek `$Tuple$` yang merujuk pada sebuah objek `$TupleDesc$`.
    
3. **Kebutuhan Penyimpanan Fisik**: Kebutuhan ruang fisik untuk menyimpan baris ini dapat dihitung secara tepat dengan memanggil `$tuple.getTupleDesc().getSize()$`. Panggilan ini akan menjumlahkan ukuran dari `$INT_TYPE$` (4 _byte_) dan `$STRING_TYPE$` (misalnya, 128 _byte_), menghasilkan total ukuran 132 _byte_.
    
4. **Alokasi pada Halaman Disk**: Ukuran 132 _byte_ ini kemudian digunakan oleh komponen tingkat bawah seperti kelas `$HeapPage$` untuk menentukan berapa banyak "slot" _tuple_ yang dapat dimuat dalam satu halaman disk fisik (misalnya, berukuran 4096 _byte_).8
    
5. **Serialisasi dan Deserialisasi**: Ketika `$Tuple$` ditulis ke disk, setiap `$Field$` dalam daftarnya dipanggil metode `$serialize()`-nya secara berurutan untuk mengubahnya menjadi aliran _byte_. Ketika dibaca kembali dari disk, aliran _byte_ tersebut diurai dan digunakan untuk merekonstruksi objek-objek `$Field$` yang sesuai, sebuah proses yang dikenal sebagai deserialisasi.
    

Secara visual, hubungan ini dapat digambarkan sebagai berikut: Sebuah objek `$Tuple$` berisi sebuah `$List<Field>$` dan sebuah referensi ke satu objek `$TupleDesc$`. Objek `$TupleDesc$` ini, pada gilirannya, berisi sebuah `$List<TDItem>$`. Setiap `$TDItem$` menunjuk ke sebuah `enum` `$Type$` dan berisi sebuah `String` untuk nama kolom. Hierarki ini secara efektif memisahkan logika pemrosesan _query_, yang beroperasi pada level `$Tuple$` dan `$Field$`, dari manajemen penyimpanan, yang beroperasi pada level _byte_ dan halaman. Pemisahan yang bersih ini adalah ciri khas dari desain sistem database yang kuat dan modular, dan fondasinya diletakkan oleh struktur data fundamental yang telah dibahas dalam bab ini.