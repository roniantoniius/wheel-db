package com.aantoniusron.wheeldb.server.a_kamus.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser;
import org.apache.shardingsphere.sql.parser.mysql.parser.MySQLLexer;

/*
 * a/b test ANTLR as an parser generator from apache shardingsphere
 */
public class ParserAntlrTest {
	public static void main(String[] args) {
		
	}
	
	private static void output(String query) {
		MySQLLexer lexer = new MySQLLexer(CharStreams.fromString(query));
		MySQLStatementParser parser = new MySQLStatementParser(new CommonTokenStream(lexer));
		MySQLStatementParser.ExecuteContext execute = parser.execute();
		
		VisitorMySql visitor = new VisitorMySql(parser);
		System.out.println(execute.toStringTree(parser));
		visitor.visit(execute);
		
		System.out.println(visitor.getValue().toString());
	}
}