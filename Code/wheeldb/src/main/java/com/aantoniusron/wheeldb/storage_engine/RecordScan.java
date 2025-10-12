package com.aantoniusron.wheeldb.storage_engine;

import com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa.Konstan;

// interfacce untuk proses scan oleh query
// ada class Scan utk setiap relasional dalam bentuk operasi aljabar (in query exection)
public interface RecordScan {
	
	// posisi proses Scan sebelum memproses data baris pertama
	// Jadi proses Scan selanjutnya akan mengembalikan baris pertama
	public void lihatScanMulaiKueri();
	
	// pindah proses Scan ke baris selanjutya. return False kalau ngga ada data lagi
	public boolean next();
	
	// ambil isi/nilai bersifat Integer dari suatu field dalam baris saat ini
	public int getInt(String fldname);
	
	// ambil nilai bersifat String dri suatu field pada baris ini
	public String getString(String fldname);
	
	// ambil nilai dengan suatu tipe data pada baris ini
	public Konstan getValue(String fldname);
	
	// method untuk validasi apakah scan punya field tertentu
	public boolean hasField(String fldname);
	
	// close proses Scan dan subscan yang sedang berjalan
	public void close();
}
