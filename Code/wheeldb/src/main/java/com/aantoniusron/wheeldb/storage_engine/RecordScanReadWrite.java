package com.aantoniusron.wheeldb.storage_engine;

import com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa.Konstan;
import com.aantoniusron.wheeldb.storage_engine.impl.data.heap.page.RecordKey;

/*
 * updatable scan
 * *read write
 */
public interface RecordScanReadWrite extends RecordScan {
	/*
	 *  method-method untuk modifikasi nilai field dari record saat ini
	 *  sesuai jenis data
	 */
	public void setNilai(String namafield, Konstan nilai);
	public void setInt(String namaField, int nilai);
	public void setString(String namaField, String nilai);
	
	/*
	 * insert data ke record di suatu tempat pada scan
	 */
	public void lihatMulaiInsert();
	
	/*
	 * hapus record saat ini dari scan
	 */
	public void hapus();
	
	/*
	 * id dari record saat ini
	 */
	public RecordKey getRecordId();
	
	/*
	 * mempposisikan scan saat ini
	 * supaya record sekarang punya id
	 */
	public void seekTo(RecordKey recordKey);
}
