package com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands;

import java.util.List;

import com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa.Predikat;

import lombok.ToString;

// Class yang simpan data dari 'SELECT' statement (Predikat Select)
// Predikat tentu difollow oleh beberapa class seperti Ketentuan (Term) kayak isi dari 'WHERE'
// `field` adalah kolom yang dipilih pada statement query

@ToString
public class QueryData {
	public List<String> fields;
	public String tabel;
	public Predikat predikat;
	
	
	// simpan isi query dari nama kolom, tabel, dan predikat
	public QueryData(List<String> inputFields, String inputTabel, Predikat inputPredikat) {
		this.fields = inputFields;
		this.tabel = inputTabel;
		this.predikat = inputPredikat;
	}
	
	public List<String> getFields(){
		return fields;
	}
	
	public String getTabel() {
		return tabel;
	}
	
	public Predikat getPredikat() {
		return predikat;
	}
}
