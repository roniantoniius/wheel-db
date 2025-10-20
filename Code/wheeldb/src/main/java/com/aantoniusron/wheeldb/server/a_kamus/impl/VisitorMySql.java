package com.aantoniusron.wheeldb.server.a_kamus.impl;
import java.util.ArrayList;
import java.util.List;

import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementBaseVisitor;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser;

import com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands.CreateIndex;
import com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands.CreateTableData;
import com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands.DeleteData;
import com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands.InsertData;
import com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands.QueryData;
import com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands.UpdateData;
import com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa.Ekspresi;
import com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa.Ketentuan;
import com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa.Konstan;
import com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa.Predikat;
import com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel.DefinisiTabel;


/*
 * lexer dan parser dari ANTLR terhadap input query statement
 */
public class VisitorMySql extends MySQLStatementBaseVisitor{
	
	enum TIPE_COMMAND {
		QUERY, MODIFY, INSERT, DELETE, CREATE_TABLE, CREATE_INDEX
	}
	
	private TIPE_COMMAND commandType;
	private MySQLStatementParser parser;
	
	private String namaTabel;
	private Predikat predikat;
	
	// select
	private List<String> selectFields;
	
	// index
	private String namaIndeks;
	private String namaIndeksKolom;
	
	// insert
	private List<String> insertFields;
	private List<Konstan> insertNilai;
	
	//modif
	private Ekspresi updateNilaiField;
	private String updateNamaField;
	
	// create
	private DefinisiTabel skema;
	
	public VisitorMySql(MySQLStatementParser parser) {
		this.parser = parser;
		
		this.namaTabel = "";
		this.predikat = new Predikat();
		
		this.selectFields = new ArrayList<>();
		
		this.namaIndeks = "";
		this.namaIndeksKolom = "";
		
		this.insertFields = new ArrayList<>();
		this.insertNilai = new ArrayList<>();
		
		this.updateNamaField = "";
		
		this.skema = new DefinisiTabel();
	}
	
	// cmnd type
	@Override
	public Object visitSelect(MySQLStatementParser.SelectContext konteks) {
		TIPE_COMMAND tipeCommand = TIPE_COMMAND.QUERY;
		return super.visitSelect(konteks);
	}
	
	@Override
	public Object visitCreateTable(MySQLStatementParser.CreateTableContext konteks) {
		TIPE_COMMAND tipeCommand = TIPE_COMMAND.CREATE_TABLE;
		return super.visitCreateTable(konteks);
	}
	
	@Override
	public Object visitCreateIndex(MySQLStatementParser.CreateIndexContext konteks) {
		TIPE_COMMAND tipeCommand = TIPE_COMMAND.CREATE_INDEX;
		return super.visitCreateIndex(konteks);
	}
	
	@Override
	public Object visitInsert(MySQLStatementParser.InsertContext konteks) {
		TIPE_COMMAND tipeCommand = TIPE_COMMAND.INSERT;
		return super.visitInsert(konteks);
	}
	
	@Override
	public Object visitUpdate(MySQLStatementParser.UpdateContext konteks) {
		TIPE_COMMAND tipeCommand = TIPE_COMMAND.MODIFY;
		return super.visitUpdate(konteks);
	}
	
	@Override
	public Object visitDelete(MySQLStatementParser.DeleteContext konteks) {
		TIPE_COMMAND tipeCommand = TIPE_COMMAND.DELETE;
		return super.visitDelete(konteks);
	}
	
	
	// cmd atribut untuk query, delete
	@Override
	public Object visitTableName(MySQLStatementParser.TableNameContext konteks) {
		this.namaTabel = konteks.name().getText();
		return super.visitTableName(konteks);
	}
	
	@Override
	public Object visitProjection(MySQLStatementParser.ProjectionContext konteks) {
		this.selectFields.add(konteks.expr().getText());
		return super.visitProjection(konteks);
	}
	
	private Ketentuan getKetentuan(MySQLStatementParser.BooleanPrimaryContext ketentuan) {
		MySQLStatementParser.BooleanPrimaryContext kiri = ketentuan.booleanPrimary();
		MySQLStatementParser.PredicateContext kanan = ketentuan.predicate();
		
		Ekspresi kiriEkspresi = new Ekspresi(kiri.getText());
		Ekspresi kananEkspresi = null;
		
		if (kanan.bitExpr() != null && 
			kanan.bitExpr(0).simpleExpr() != null &&
			kanan.bitExpr(0).simpleExpr().literals() != null &&
			kanan.bitExpr(0).simpleExpr().literals().numberLiterals() != null &&
			!kanan.bitExpr(0).simpleExpr().literals().numberLiterals().isEmpty()) {
			
			Integer numInteger = Integer.parseInt(kanan.getText());
			kananEkspresi = new Ekspresi(new Konstan(numInteger));
		} else if (kanan.bitExpr() != null &&
				   kanan.bitExpr(0).simpleExpr() != null &&
				   kanan.bitExpr(0).simpleExpr().literals() != null &&
				   kanan.bitExpr(0).simpleExpr().literals().stringLiterals() != null &&
				   !kanan.bitExpr(0).simpleExpr().literals().stringLiterals().isEmpty()) {
			kananEkspresi = new Ekspresi(new Konstan(kanan.getText())); 
		}
		return new Ketentuan(kananEkspresi, kiriEkspresi);
	}
	
	// visitEkspresi
	@Override
	public Object visitExpr(MySQLStatementParser.ExprContext konteks) {
		if (konteks.booleanPrimary() != null &&
			konteks.booleanPrimary().comparisonOperator() != null &&
			konteks.booleanPrimary().comparisonOperator().getText().equals("=")) {
			
			Ketentuan ketentuan = getKetentuan(konteks.booleanPrimary());
			predikat.konjungsiKe(new Predikat(ketentuan));
		}
		return super.visitExpr(konteks);
	}
	
	
	// cmd atribut untuk CREATE INDEX
	
	@Override
	public Object visitIndexName(MySQLStatementParser.IndexNameContext konteks) {
		this.namaIndeks =konteks.getText();
		return super.visitIndexName(konteks);
	}
	
	@Override
	public Object visitKeyPart(MySQLStatementParser.KeyPartContext konteks) {
		this.namaIndeksKolom = konteks.getText();
		return super.visitKeyPart(konteks);
	}
	
	@Override
	public Object visitInsertIdentifier(MySQLStatementParser.InsertIdentifierContext konteks) {
		this.insertFields.add(konteks.getText());
		return super.visitInsertIdentifier(konteks);
	}
	
	@Override
	public Object visitNumberLiterals(MySQLStatementParser.NumberLiteralsContext konteks) {
		this.insertNilai.add(new Konstan(Integer.parseInt(konteks.getText())));
		this.updateNilaiField = new Ekspresi(new Konstan(Integer.parseInt(konteks.getText())));
		return super.visitNumberLiterals(konteks);
	}
	
	@Override
	public Object visitStringLiterals(MySQLStatementParser.StringLiteralsContext konteks) {
		this.insertNilai.add(new Konstan(konteks.getText()));
		this.updateNilaiField = new Ekspresi(new Konstan(konteks.getText()));
		return super.visitStringLiterals(konteks);
	}
	
	// cmd atribut untuk Modify/update
	@Override
	public Object visitAssignment(MySQLStatementParser.AssignmentContext konteks) {
		this.updateNamaField = konteks.columnRef().getText();
		return super.visitAssignment(konteks);
	}
	
	// cmd atribut untuk create table
	@Override
	public Object visitColumnDefinition(MySQLStatementParser.ColumnDefinitionContext konteks) {
		String namaFieldString = konteks.column_name.getText();
		String tipeDataString = konteks.fieldDefinition().dataType().getText();
		if (tipeDataString.equals("int")) {
			skema.addIntField(namaFieldString);
		} else if (tipeDataString.equals("varchar")) {
			tipeDataString = tipeDataString.substring("varchar".length());
			tipeDataString = tipeDataString.replace("(", "");
			tipeDataString = tipeDataString.replace(")", "");
			int lebar = Integer.parseInt(tipeDataString);
			skema.addStrField(namaFieldString, lebar);
		} else {
			throw new RuntimeException("Tipe Data Kolum Tidak Didukung!");
		}
		return super.visitColumnDefinition(konteks);
	}
	
	public Object getValue() {
		switch(commandType) {
			case QUERY:
				return new QueryData(selectFields, namaTabel, predikat);
			case INSERT:
				return new InsertData(namaTabel, insertFields, insertNilai);
			case DELETE:
				return new DeleteData(namaTabel, predikat);
			case MODIFY:
				return new UpdateData(namaTabel, updateNamaField, updateNilaiField, predikat);
			case CREATE_INDEX:
				return new CreateIndex(namaIndeks, namaTabel, namaIndeksKolom);
			case CREATE_TABLE:
				return new CreateTableData(namaTabel, skema); 
		}
		return new QueryData(selectFields, namaTabel, predikat); 
	}
}