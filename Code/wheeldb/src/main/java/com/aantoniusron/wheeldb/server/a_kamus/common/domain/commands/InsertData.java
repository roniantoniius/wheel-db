package com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands;

import java.util.List;

import com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa.Konstan;

/*
 * parser untuk command 'INSERT INTO'
 */
public class InsertData {
	private String namaTabel;
	private List<String> daftarField;
	private List<Konstan> daftarNilai;
	
	public InsertData(String tabel, List<String> namaField, List<Konstan> nilai) {
		this.namaTabel = tabel;
		this.daftarField = namaField;
		this.daftarNilai = nilai;
	}
	
	public String getNamaTabel() {return this.namaTabel;}
	
	public List<String> getDaftarField() {return this.daftarField;}
	
	public List<Konstan> getDaftarNilai() {return this.daftarNilai;}
	
}
