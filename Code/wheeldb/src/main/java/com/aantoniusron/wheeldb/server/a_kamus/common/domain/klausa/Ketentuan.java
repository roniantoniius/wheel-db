package com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa;

import com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel.DefinisiTabel;
import com.aantoniusron.wheeldb.storage_engine.RecordScan;

/*
 * Ketentuan itu perbandingan antara 2 Kondisi (kolom, fungsi, atau literal kompleks) -- WHERE
 * WHERE kolomA = kolomB
 * and kolomC = 'wheel'
 */
public class Ketentuan {
	private Ekspresi kanan, kiri;
	
	// constructor untuk bandingin syarat/ekspresi kanan dan kiri
	public Ketentuan(Ekspresi kananEkspresi, Ekspresi kiriEkspresi) {
		this.kanan = kananEkspresi;
		this.kiri = kiriEkspresi;
	}
	
	/*
	 * return true if both Ekspresi punya Konstan yang sama dalam scan baris saat ini
	 */
	public boolean sudahTerpenuhi(RecordScan scan) {
		Konstan nilaiEkspresiKanan = kanan.evaluasi(scan);
		Konstan nilaiEkspresiKiri = kiri.evaluasi(scan);
		return nilaiEkspresiKiri.equals(nilaiEkspresiKanan);
	}
	
	/*
	 * handle bentuk Ketentuan seperti "F=c"
	 * di mana c adalah suatu nilai, while F adalah field
	 * kalau Ketentuan yang didapat adalah "F=c" maka return Konstan tersebut
	 */
	public Konstan samaDenganKonstan(String namaField) {
		if (kiri.isFieldName() && kiri.asFieldName().equals(namaField) && !kanan.isFieldName()) {
			return kanan.asKonstan();
		} else if (kanan.isFieldName() && kanan.asFieldName().equals(namaField) && !kiri.isFieldName()) {
			return kiri.asKonstan();
		} else {
			return null;
		}
	}
	
	/*
	 * handle bentuk Ketentuan seperti "F1=F2"
	 * kalau kondisi tersebut terpenuhi maka return nama field tersebut
	 */
	public String samaDenganField(String namaField) {
		if (kiri.isFieldName() && kiri.asFieldName().equals(namaField) && !kanan.isFieldName()) {
			return kanan.asFieldName();
		} else if (kanan.isFieldName() && kanan.asFieldName().equals(namaField) && !kiri.isFieldName()) {
			return kiri.asFieldName();
		} else {
			return null;
		}
	}
	
	/*
	 * validasi jika Ketentuan dan Ekspresinya sudah diterapkan di Skema
	 */
	public boolean appliesTo(DefinisiTabel skema) {
		return kiri.appliesTo(skema) && kanan.appliesTo(skema);
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return kiri.toString() + " = " + kanan.toString();
	}
}