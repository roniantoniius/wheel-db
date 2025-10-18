package com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands;

import com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa.Ekspresi;
import com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa.Predikat;

/*
 * parser dari 'UPDATE' statment
 */
public class UpdateData {
	private String namaTabel;
	private String namaField;
	private Ekspresi ekspresiNilaiBaru;
	private Predikat statementPredikat;
	
	public UpdateData(String tabel, String field, Ekspresi ekspresi, Predikat predikat) {
		this.namaTabel = tabel;
		this.namaField = field;
		this.ekspresiNilaiBaru = ekspresi;
		this.statementPredikat = predikat;
	}
	
	private String getNamaTabel() {return this.namaTabel;}
	private String getNamaField() {return this.namaField;}
	private Ekspresi getEkspresiNilaiBaru() {return this.ekspresiNilaiBaru;}
	private Predikat getStatementPredikat() {return this.statementPredikat;}
}
