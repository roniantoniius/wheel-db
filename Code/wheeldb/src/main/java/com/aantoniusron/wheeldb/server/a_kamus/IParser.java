package com.aantoniusron.wheeldb.server.a_kamus;

import com.aantoniusron.wheeldb.server.a_kamus.common.domain.commands.QueryData;

// parser: tokenize (lexer) & parser itself
public interface IParser {
	// interface parser untuk parse si SQL Query Text terhadap sebuah QueryEngine
	// hasil parser akan diberikan kepada QueryEngine, biasanya dalam bentuk AST
	
	public QueryData queryCommand();
	public Object updateQueryCommand();
}
