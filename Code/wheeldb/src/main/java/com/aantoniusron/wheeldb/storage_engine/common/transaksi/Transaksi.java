package com.aantoniusron.wheeldb.storage_engine.common.transaksi;

import java.awt.print.Pageable;

import com.aantoniusron.wheeldb.storage_engine.common.file.BlockId;
import com.aantoniusron.wheeldb.storage_engine.common.file.FileMgr;
import com.aantoniusron.wheeldb.storage_engine.common.file.Page;
import com.aantoniusron.wheeldb.storage_engine.common.transaksi.Buffer.BufferManager;

/*
 * salah satu prinsip ACID
 * pastiin suatu proses query dalam satu Transaksi
 * biar dalam keadaan serialisasi dan dapat dipulihkan
 */
public class Transaksi {
	private static final int ENF_OF_FILE = -1;
	private static int nextTransaksiNum = 0;
	private FileMgr fileManager;
	private BufferManager bufferManager;
	private int nilaiTransaksi;
	
	public Transaksi(FileMgr fileMgr) {
		this.fileManager = fileMgr;
		nilaiTransaksi = nextNilaiTransaksi();
	}
	
	private static synchronized int nextNilaiTransaksi() {
		nextTransaksiNum++;
		return nextTransaksiNum;
	}
	
	private void commit() {}
	private void rollback() {}
	private void recover() {}
	
	/*
	 * pin suatu blok
	 * untuk memposisikan blok ini di memory dan transaksi tahu hal ini
	 */
	public void unpin(BlockId blok) {}
	
	public synchronized int getInt(BlockId blok, int offset) {
		Page kontenPage = new Page(fileManager.getUkuranBlok());
		fileManager.read(blok, kontenPage);
		return kontenPage.getInt(offset);
	}
	
	public synchronized String getString(BlockId blok, int offset) {
		Page kontenPage	= new Page(fileManager.getUkuranBlok());
		fileManager.read(blok, kontenPage);
		return kontenPage.getString(offset);
	}
	
	/*
	 * simpan int terhadap offset tertentu, dalam block tertentu
	 * - ambil Exclusive Lock (X-Lock) dari Blok
	 * - baca nilai saat ini pada offset tsb,
	 * - ambil nilai dan simpan ke update log record, dan
	 * - tulis record tersebut ke dalam log
	 * Jadi ini tuh buffer untuk store value, sehingga ada dua log (LSN) yang kasih log record dan id transaski
	 */
	public synchronized void setInt(BlockId blok, int offset, int nilai) {
		Page kontenPage = new Page(fileManager.getUkuranBlok());
		fileManager.read(blok, kontenPage);
		
		kontenPage.setInt(offset, nilai);
		fileManager.write(blok, kontenPage);
	}
	
	/*
	 * sama kayak setInt(), tapi kita simpan tipe data String
	 */
	public synchronized void setString(BlockId blok, int offset, String nilai) {
		Page kontenPage = new Page(fileManager.getUkuranBlok());
		fileManager.read(blok, kontenPage);
		
		kontenPage.setString(offset, nilai);
		fileManager.write(blok, kontenPage);
	}
	
	/*
	 * jumlah Blok pada suatu Heap Fille
	 * - ambil Shared Lock yang biasanya di "akhir file", sebelum minta ke file manager,
	 *   untuk ambil size dari file
	 */
	public synchronized int jumlahBlok(String namafile) {
		return fileManager.hitungBlok(namafile);
	}
	
	/*
	 * tambah Block baru di ujung File dan return referencenya
	 * - ambil XLock di "akhir file" dulu
	 */
	public synchronized BlockId append(String namaFile) {
		return fileManager.append(namaFile);
	}
	
	public synchronized int ukuranBlok() {
		return fileManager.getUkuranBlok();
	}
}
