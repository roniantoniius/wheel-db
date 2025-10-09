package com.aantoniusron.wheeldb.server.b_query_engine;

import com.aantoniusron.wheeldb.server.b_query_engine.common.TableDto;

public interface IQueryEngine {
	public TableDto queryRun(String sql);
	public TableDto queryUpdate(String sql);
	public void tutup();
}