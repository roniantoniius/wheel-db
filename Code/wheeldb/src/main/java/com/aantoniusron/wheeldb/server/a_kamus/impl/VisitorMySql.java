package com.aantoniusron.wheeldb.server.a_kamus.impl;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementBaseVisitor;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser;

/*
 * lexer dan parser dari ANTLR terhadap input query statement
 */
public class VisitorMySql extends MySQLStatementBaseVisitor{
	
	enum TIPE_COMMAND {
		QUERY, MODIFY, INSERT, DELETE, CREATE_TABLE, CREATE_INDEX
	}
	
	private TIPE_COMMAND commandType;
	private MySQLStatementParser parser;
	
	
}
