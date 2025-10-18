package com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands;

import com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa.Predikat;

import lombok.ToString;

/*
 * parser 'DELETE'
 */

@ToString
public class DeleteData {
	private String namaTabel;
	private Predikat statementPredikat;
	
	public DeleteData(String tabel, Predikat predikat) {
		this.namaTabel = tabel;
		this.statementPredikat = predikat;
	}
	
	public String getNamaTabel() {return this.namaTabel;}
	
	public Predikat getPredikat() {return this.statementPredikat;}
}
