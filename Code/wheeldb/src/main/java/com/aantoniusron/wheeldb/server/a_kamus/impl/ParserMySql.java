package com.aantoniusron.wheeldb.server.a_kamus.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser;
import org.apache.shardingsphere.sql.parser.mysql.parser.MySQLLexer;

import com.aantoniusron.wheeldb.server.a_kamus.IParser;
import com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands.QueryData;


/*
 * ANTLR parser untuk statement query
 */
public class ParserMySql implements IParser{
	VisitorMySql visitorMySql;
	
	public ParserMySql(String query) {
		MySQLLexer lexer = new MySQLLexer(CharStreams.fromString(query));
		MySQLStatementParser parser = new MySQLStatementParser(new CommonTokenStream(lexer));
		
		visitorMySql = new VisitorMySql(parser);
		visitorMySql.visit(parser.execute());
	}

	@Override
	public QueryData queryCommand() {
		// TODO Auto-generated method stub
		return (QueryData) visitorMySql.getValue();
	}

	@Override
	public Object updateQueryCommand() {
		// TODO Auto-generated method stub
		return visitorMySql.getValue();
	}

}
