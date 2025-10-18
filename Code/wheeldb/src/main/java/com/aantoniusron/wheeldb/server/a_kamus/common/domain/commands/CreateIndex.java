package com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands;

import lombok.ToString;

/*
 * kamus Parser untuk handle command 'CREATE INDEX'
 */

@ToString
public class CreateIndex {
	private String namaIndeks, namaTabel, namaField;
	
	public CreateIndex(String indeks, String tabel, String field) {
		this.namaIndeks = indeks;
		this.namaTabel = tabel;
		this.namaField = field;
	}
	
	public String getNamaIndeks() {
		return this.namaField;
	}
	
	public String getNamaTabel() {return this.namaTabel;}
	
	public String getNamaField() {return this.namaField;}
}
