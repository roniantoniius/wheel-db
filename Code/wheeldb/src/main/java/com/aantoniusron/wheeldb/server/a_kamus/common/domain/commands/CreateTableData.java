package com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands;

import com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel.DefinisiTabel;

import lombok.ToString;

/*
 * kamus untuk handle parser dari 'CREATE TABLE'
 */

@ToString
public class CreateTableData {
	private String namaTabel;
	private DefinisiTabel skemaDefinisiTabel;
	
	public CreateTableData(String tabel, DefinisiTabel skema) {
		this.namaTabel = tabel;
		this.skemaDefinisiTabel = skema;
	}
	
	public String getNamaTabel() {return this.namaTabel;}
	
	public DefinisiTabel getSkemaDefinisiTabel() {return this.skemaDefinisiTabel;}
}
