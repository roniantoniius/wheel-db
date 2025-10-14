package com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa;

import com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel.DefinisiTabel;
import com.aantoniusron.wheeldb.storage_engine.RecordScan;

/*
 * untuk ekspresi kayak operator biner, unary, atau nested terhadap objek Konstan 
 */
public class Ekspresi {
	private Konstan nilaiKonstan = null;
	private String namaField = null;
	
	public Ekspresi(Konstan nilaiKonstan) {
		this.nilaiKonstan = nilaiKonstan;
	}
	
	public Ekspresi(String namaField) {
		this.namaField = namaField;
	}
	
	// method untuk evaluasi ekspresi yang sedang digunakan di Baris / Record saat ini
	public Konstan evaluasi(RecordScan scan) {
		return (nilaiKonstan != null) ? nilaiKonstan : scan.getValue(namaField);
	}
	
	// validasi apakah ekspresi merupakan sebuah field
	public boolean isFieldName() {
		return namaField != null;
	}
	
	// method untuk ubah objek Konstan menjadi sebuah Ekspresi Konstan,
	// atau null kalau ekspresi bukan meruapkan kosntan
	public Konstan asKonstan() {
		return nilaiKonstan;
	}
	
	// ubah String nama field menjadi sebuah Ekspresi
	public String asFieldName() {
		return namaField;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return (nilaiKonstan != null) ? nilaiKonstan.toString() : namaField;
	}
	
	// tentuin apakah semua field dalam ekspresi itu digunakan dalam satu schema?
	// return true kalau iya
	public boolean appliesTo(DefinisiTabel skema) {
		return (nilaiKonstan != null) ? true : skema.hasField(namaField);
	}
	
	
}
