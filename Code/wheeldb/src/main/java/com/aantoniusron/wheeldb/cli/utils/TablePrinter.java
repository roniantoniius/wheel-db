package com.aantoniusron.wheeldb.cli.utils;

import java.util.ArrayList;
import java.util.List;

// class deskripsi suatu Table

public class TablePrinter {
	// an static class for storing Kolom dan Baris
	public static class Kolom {
		private String nama;
		private int lebar;
		
		public Kolom(final String namaKolom) {
			this.nama = namaKolom;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "Kolom{" + "nama='" + nama + '\'' + ", width=" + lebar + '}';
		}
	}
	
	public static class Baris {
		private final List<String> nilaiNilai = new ArrayList<>();
		
		public List<String> getValues(){
			return this.nilaiNilai;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "Baris{" + "values=" + this.nilaiNilai + "}";
		}
	}
	
	// TablePrinter variables
	private final List<Kolom> kolomKolom = new ArrayList<>();
	private final List<Baris> barisData = new ArrayList<>();
	private int maksLebarKolom = Integer.MAX_VALUE;
	
	// get lebar kolom
	public void hitungLebarKolom() {
		
		// update lebar dri setiap kolom
		for (Kolom kolom: kolomKolom) {
			kolom.lebar = kolom.nama.length() + 1;
		}
		
		// updae lebar kolom melalui perbandingan terhadap isi data di koloom tsb
		for (Baris baris: barisData) {
			int indeksKol = 0;
			
			for (String isi: baris.nilaiNilai) {
				Kolom kolom = kolomKolom.get(indeksKol);
				
				if (isi == null) continue;
				
				kolom.lebar = Math.max(kolom.lebar, isi.length() + 1);
				indeksKol++;
			}
		}
		
		// ambil lebar suatu kolom jika dibandingkan lebar maksimum integer
		for (Kolom kolom: kolomKolom) {
			kolom.lebar = Math.min(kolom.lebar, maksLebarKolom);
		}
	}
	
	// other method soon
}
