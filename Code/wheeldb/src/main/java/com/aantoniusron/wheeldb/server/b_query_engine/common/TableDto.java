package com.aantoniusron.wheeldb.server.b_query_engine.common;

import java.util.List;


// konsep Tuple itu sendiri: TupleDesc, etc
public class TableDto {
	public List<String> namaKolom;
	
	public List<List<String>> daftarIsiKolom;
	
	public String message;
	
	// constructor 1
	public TableDto(List<String> daftarKolom, List<List<String>> isiKolom) {
		this.namaKolom = daftarKolom;
		this.daftarIsiKolom = isiKolom;
		this.message = "";
	}
	
	// constructor utk set message
	public TableDto(String pesan) {
		this.message = pesan;
	}
}
