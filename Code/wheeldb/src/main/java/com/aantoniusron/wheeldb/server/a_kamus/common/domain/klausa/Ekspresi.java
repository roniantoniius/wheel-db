package com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa;

import com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel.DefinisiTabel;
import com.aantoniusron.wheeldb.storage_engine.RecordScan;

// untuk ekspresi kayak operator biner, unary, atau nested
public class Ekspresi {
	private Konstan nilaiKonstan = null;
	private String fldname = null;
	
	public Ekspresi(Konstan nilaiKonstan) {
		this.nilaiKonstan = nilaiKonstan;
	}
	
	public Ekspresi(String fldname) {
		this.fldname = fldname;
	}
	
	// method untuk evaluasi ekspresi yang sedang digunakan di Baris / Record saat ini
	public Konstan evaluasi(RecordScan scan) {
		return (nilaiKonstan != null) ? nilaiKonstan : scan.getValue(fldname);
	}
	
	// validasi apakah ekspresi merupakan sebuah field
	public boolean isFieldName() {
		return fldname != null;
	}
	
	// method untuk ubah objek Konstan menjadi sebuah Ekspresi Konstan,
	// atau null kalau ekspresi bukan meruapkan kosntan
	public Konstan asKonstan() {
		return nilaiKonstan;
	}
	
	// ubah String nama field menjadi sebuah Ekspresi
	public String asFieldName() {
		return fldname;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return (nilaiKonstan != null) ? nilaiKonstan.toString() : fldname;
	}
	
	// tentuin apakah semua field dalam ekspresi itu digunakan dalam satu schema?
	// return true kalau iya
	public boolean appliesTo(DefinisiTabel skema) {
		return (nilaiKonstan != null) ? true : skema.hasField(fldname);
	}
	
	
}
