package com.aantoniusron.wheeldb.server.b_query_engine.common.katalog;

import com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.indeks.IndeksManager;
import com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.stats.StatsManager;
import com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel.TabelManager;

/*
 * manager untuk metadata global (skema, tabel, indeks, stats)
 */

public class MetaDataManager {
	private static TabelManager tabelManager;
	private static IndeksManager indeksManager;
	private static StatsManager statsManager;
}
