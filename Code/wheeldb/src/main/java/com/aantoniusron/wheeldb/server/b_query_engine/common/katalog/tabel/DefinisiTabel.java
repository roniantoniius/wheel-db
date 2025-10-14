package com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.ToString;

import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;


/**
 * Skema Record dari suatu Tabel
 * Skema mengandung nama dan tipe data dari setiap field/kolom,
 * beserta panjang dari Varcharnya
 */

@ToString
public class DefinisiTabel {
	
	// class punya Field: tipe data dan oanjang
	@ToString
	class InfoField{
		int tipe;
		int panjang;
		
		public InfoField(int tipe, int panjang) {
			this.tipe = tipe;
			this.panjang = panjang;
		}
	}
	
	// atribut
	private final List<String> daftarField = new ArrayList<>();
	private final Map<String, InfoField> info = new HashMap<>();
	
	/**
	 * tambah field baru ke suatu Skema
	 *   beserta info seperti nama, tipe data, dan panjang
	 *   kecuali kalau tipe datanya Integer, itu harusnya bersifat tetap
	 *   
	 * param tipe itu tipe data suatu kolom berdasarkan di kelas Konstan
	 */
	public void addField(String namaField, int tipe, int panjang) {
		daftarField.add(namaField);
		info.put(namaField, new InfoField(tipe, panjang));
	}
	
	/**
	 * tambah field dengan tipe data int
	 */
	
	public void addIntField(String namaField) {
		addField(namaField, INTEGER, 0);
	}
	
	
	public void addStrField(String namaField, int panjang) {
		addField(namaField, VARCHAR, panjang);
	}
	
	/*
	 * tambah field ke skema sekarang,
	 * tapi tipe dan panjangnya dari schema yang lain
	 */
	public void add(String namaField, DefinisiTabel skema) {
		int tipe = skema.tipe(namaField);
		int panjang = skema.panjang(namaField);
		addField(namaField, tipe, panjang);
	}
	
	/*
	 * return tipe dari suatu field,
	 * by java.sql.Types
	 */
	public int tipe(String namaField) {
		return info.get(namaField).tipe;
	}
	
	public int panjang(String namaField) {
		return info.get(namaField).panjang;
	}
	
	/*
	 * tambah semua field dri suatu skema ke skema yang sekarang
	 */
	public void addAll(DefinisiTabel skema) {
		for (String namaField: skema.fields()) {
			add(namaField, skema);
		}
	}
	
	public List<String> fields(){
		return daftarField;
	}
	
	/*
	 * validasi apakah field ada di skema
	 */
	public boolean hasField(String namaField) {
		return daftarField.contains(namaField);
	}
}
