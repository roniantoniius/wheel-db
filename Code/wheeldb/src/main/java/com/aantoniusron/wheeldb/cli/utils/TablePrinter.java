package com.aantoniusron.wheeldb.cli.utils;

import java.util.ArrayList;
import java.util.List;

import com.aantoniusron.wheeldb.server.b_query_engine.common.TableDto;

// an class for printing Kolom dan Baris dalam format ascii

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
	public void render() {
		StringBuilder strBuilder = new StringBuilder();
		
		tulisGarisBatas(kolomKolom, strBuilder);
		
		tulisNamaKolum(kolomKolom, strBuilder);
		
		tulisGarisBatas(kolomKolom, strBuilder);
		
		tulisNilai(barisData, kolomKolom, strBuilder);
		
		
		
	}
	
	private void tulisNamaKolum(final List<Kolom> koloms, final StringBuilder strBuilder) {
		strBuilder.append('|');
		
		for (Kolom kolom: koloms) {
			// string formating to list of tables divided by vertical line
			strBuilder.append(String.format(" %-" + kolom.lebar + "s", kolom.nama));
			strBuilder.append("|");
		}
		
		strBuilder.append('\n');
	}
	
	private void tulisGarisBatas(final List<Kolom> koloms, final StringBuilder strBuilder) {
		// untuk garis horizontal pembatas
		strBuilder.append('+');
		
		for (Kolom kolom: koloms) {
			strBuilder.append(String.format("%-" + (kolom.lebar + 1) + "s", "").replace(' ', '-'));
			strBuilder.append("+");
		}
		
		strBuilder.append("+");
	}
	
	
	private void tulisNilai(final List<Baris> barisData, final List<Kolom> daftarKolom, final StringBuilder strBuilder) {
		// i want to write eaach value in this cli
		for (Baris baris: barisData) {
			int indeks = 0;
			strBuilder.append("|");
			
			for (String isi: baris.nilaiNilai) {
				if(isi != null && isi.length() > maksLebarKolom) {
					isi = isi.substring(0, maksLebarKolom - 1);
				}
				strBuilder.append(String.format(" %-" + daftarKolom.get(indeks).lebar + "s", isi));
				
				strBuilder.append("|");
				
				indeks++;
			}
			
			strBuilder.append("\n");
		}
	}
		
	// list of method for pring Tablenya
	public void print(List<String> daftarKolom, List<List<String>> daftarBaris) {
		TablePrinter tabel = new TablePrinter();
		tabel.ubahMaksLebarKolom(45);
		
		// get every single column
		for (String namaKolom: daftarKolom) {
			tabel.ambilKolom().add(new TablePrinter.Kolom(namaKolom));
		}
		
		// get every single of row, and every single of data in that row in that column
		for (List<String> nilaiBaris: daftarBaris) {
			
			TablePrinter.Baris barisKosong = new TablePrinter.Baris();
			tabel.ambilBaris().add(barisKosong);
			
			for (String baris: nilaiBaris) {
				barisKosong.nilaiNilai.add(baris);
			}
		}
		
		tabel.hitungLebarKolom();
		tabel.render();
		
	}
	
	public void ubahMaksLebarKolom(final int maksLebarKoloms) {
		this.maksLebarKolom = maksLebarKoloms;
	}
	
	public int getMaksLebarKolom() {
		return maksLebarKolom;
	}
	
	public List<Kolom> ambilKolom(){
		return kolomKolom;
	}
	
	public List<Baris> ambilBaris(){
		return barisData;
	}
}
