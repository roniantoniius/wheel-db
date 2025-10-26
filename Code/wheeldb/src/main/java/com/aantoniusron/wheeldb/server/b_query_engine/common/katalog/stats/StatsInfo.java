package com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.stats;

/*
 * simpan informasi statistik dari suatu tabel, e.g:
 * - jumlah block
 * - jumlah record
 * - jumlah nilai unik (distinct) utk setiap field
 * or struktur data statistik
 */
public class StatsInfo {
	private int jumlahBlok;
	private int jumlahRecord;
	
	/*
	 * constructor yg sengaja nggak masukin jml nilai distinct
	 */
	public StatsInfo(int jmlBlok, int jmlRec) {
		this.jumlahBlok = jmlBlok;
		this.jumlahRecord = jmlRec;
	}
	
	/*
	 * return estimasi jumlah dari record pd suatu tabel
	 */
	public int blokEstimasiTerakses() {
		return this.jumlahBlok;
	}
	
	/*
	 * return jml record
	 */
	public int recordsOutput() {
		return this.jumlahRecord;
	}
	
	/*
	 * asumsi sederhana untuk mencari jumlah nilai unik
	 */
	public int nilaiDistinct(String namaField) {
		return 1 + (this.jumlahRecord / 3);
	}
}
