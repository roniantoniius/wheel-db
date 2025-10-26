package com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.stats;

import java.util.HashMap;
import java.util.Map;

import com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel.TabelLayoutPhysical;
import com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel.TabelManager;
import com.aantoniusron.wheeldb.storage_engine.common.transaksi.Transaksi;

/*
 * Manajemen statistik untuk dipakai oleh Query Engine untuk analisis Cost Base query planner
 * 
 */

public class StatsManager {
	private int jmlPanggilan;
	private Map<String, StatsInfo> statsTabel;
	private TabelManager tabelManager;
	
	public StatsManager(TabelManager tabelManager, Transaksi transaksi) {
		this.tabelManager = tabelManager;
		refreshStatistik(transaksi);
	}
	
	private synchronized void refreshStatistik(Transaksi transaksi) {
		statsTabel = new HashMap<String, StatsInfo>();
	}
	
	private synchronized StatsInfo hitungStatsTabel(String namaTabel, TabelLayoutPhysical layoutPage, Transaksi transaksi) {
		return null;
	}
	
	private synchronized StatsInfo getStatsInfo(String namaTabel, TabelLayoutPhysical layoutPage, Transaksi transaksi) {
		jmlPanggilan++;
		if (jmlPanggilan > 100) refreshStatistik(transaksi);
		
		StatsInfo infoStats = statsTabel.get(namaTabel);
		if (infoStats == null) {
			infoStats = hitungStatsTabel(namaTabel, layoutPage, transaksi);
			statsTabel.put(namaTabel, infoStats);
		}
		return infoStats;
	}
}
