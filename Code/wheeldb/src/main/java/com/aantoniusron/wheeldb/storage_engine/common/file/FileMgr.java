package com.aantoniusron.wheeldb.storage_engine.common.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/*
 * file manager untuk semua File I/O
 * for now only for Read or Write ke File
 */
public class FileMgr {
	// impl open, read, write di sini!
	private final File direktoriDb;
	private final int ukuranBlok;
	private final boolean isNew;
	
	public FileMgr(File direktori, int ukuranblok) {
		this.direktoriDb = direktori;
		this.ukuranBlok = ukuranblok;
		
		isNew = !direktoriDb.exists();
		
		if (isNew) {
			direktoriDb.mkdir();
		}
	}
	
	public synchronized void read(BlockId idBlok, Page page) {
		try {
			RandomAccessFile file = getRandomAksesFile(idBlok.getNamaFile());
			file.seek((long) idBlok.getAngkaBlock() * ukuranBlok);
			file.getChannel().read(page.contents());
			file.close();
		} catch (Exception e) {
			// TODO: handle exception
			throw new RuntimeException("gagal membaca blok " + idBlok);
		}
	}
	
	public synchronized void write(BlockId idBlok, Page page) {
		try {
			RandomAccessFile file = getRandomAksesFile(idBlok.getNamaFile());
			file.seek((long) idBlok.getAngkaBlock() * ukuranBlok);
			file.getChannel().write(page.contents());
			file.close();
		} catch (Exception e) {
			// TODO: handle exception
			throw new RuntimeException("gagal menulis pada blok " + idBlok);
		}
	}
	
	public synchronized BlockId append(String namafile) {
		int angkaBlokBaru = hitungBlok(namafile);
		BlockId blok = new BlockId(namafile, angkaBlokBaru);
		byte[] b = new byte[ukuranBlok];
		
		try {
			RandomAccessFile file = getRandomAksesFile(blok.getNamaFile());
			file.seek((long) blok.getAngkaBlock() * ukuranBlok);
			file.write(b);
			file.close();
		} catch (IOException e) {
			// TODO: handle exception
			throw new RuntimeException("gagal menambahkan blok " + blok);
		}
		return blok;
	}
	
	public int hitungBlok(String namaFile) {
		try {
			RandomAccessFile file = getRandomAksesFile(namaFile);
			int hasil = (int) (file.length() / ukuranBlok);
			file.close();
			return hasil;
		} catch (Exception e) {
			// TODO: handle exception
			throw new RuntimeException("tidak bisa akses file " + namaFile);
		}
	}
	
	public boolean getIsNew() {
		return isNew;
	}
	
	public int getUkuranBlok() {
		return ukuranBlok;
	}
	
	private RandomAccessFile getRandomAksesFile(String namaFile) throws FileNotFoundException {
		File tabelDb = new File(direktoriDb, namaFile);
		return new RandomAccessFile(tabelDb, "rws");
	}
}
