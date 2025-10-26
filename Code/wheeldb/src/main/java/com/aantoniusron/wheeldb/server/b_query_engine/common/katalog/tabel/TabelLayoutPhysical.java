package com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel;

import java.util.HashMap;
import java.util.Map;
import static java.sql.Types.INTEGER;


import com.aantoniusron.wheeldb.storage_engine.common.file.Page;

/*
 * struktur dari satu baris
 * nama, tipe data, panjang dan batas setiap field
 */
public class TabelLayoutPhysical {
	private DefinisiTabel definisiTabel;
	private Map<String, Integer> offsets;
	private int ukuranSlot;
	
	/*
	 * constructor dipakai ketika tabel baru dibuat
	 * dia bikin objek Layout dari sebuah skema
	 * dan nentuin lebar untuk setiap field
	 */
	public TabelLayoutPhysical(DefinisiTabel definisiTabel) {
		this.definisiTabel = definisiTabel;
		offsets = new HashMap<String, Integer>();
		int posisi = Integer.BYTES; // untuk empty flag
		for (String namaField : definisiTabel.fields()) {
			offsets.put(namaField, posisi);
			posisi += panjangDalamByte(namaField);
		}
		ukuranSlot = posisi;
	}
	
	/*
	 * bikin objek Layout dari suatu Meta Data yang diambil dari katalog
	 */
	public TabelLayoutPhysical(DefinisiTabel definisiTabel, Map<String, Integer> offsets, int ukuranSlot) {
		this.definisiTabel = definisiTabel;
		this.offsets = offsets;
		this.ukuranSlot = ukuranSlot;
	}
	
	public DefinisiTabel getSkema() {
		return this.definisiTabel;
	}
	
	public int getOffset(String namaField) {
		return this.offsets.get(namaField);
	}
	
	public int getUkuranSlot() {
		return this.ukuranSlot;
	}
	
	
	private int panjangDalamByte(String namaField) {
		int tipeDataField = definisiTabel.tipe(namaField);
		if (tipeDataField == INTEGER) {
			return Integer.BYTES; // int selalu tetap bytenya yaitu 4
		} else {
			return Page.maksByteDiperlukanUntukString(tipeDataField);
		}
	}
	
	
}
