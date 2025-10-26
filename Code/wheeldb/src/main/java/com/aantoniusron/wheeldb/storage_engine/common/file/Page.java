package com.aantoniusron.wheeldb.storage_engine.common.file;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/* 
 * Page adalah blok untuk simpan database (in memory utk sekarang)
 * Page punya ByteBuffer, yaitu data dalam Blok
 */
public class Page {
	public static Charset CHARSET = StandardCharsets.US_ASCII;
	private ByteBuffer byteBuffer;
	
	public Page(int ukuranBlok) {
		byteBuffer = ByteBuffer.allocateDirect(ukuranBlok);
	}
	
	public static int maksByteDiperlukanUntukString(int panjangStr) {
		float bytePerKarakter = CHARSET.newEncoder().maxBytesPerChar();
		return Integer.BYTES + (panjangStr * (int) bytePerKarakter);
	}
	
	public int getInt(int offset) {
		return byteBuffer.getInt(offset);
	}
	
	public void setInt(int offset, int n) {
		byteBuffer.putInt(offset, n);
	}
	
	public byte[] getBytes(int offset) {
		byteBuffer.position(offset);
		int panjang = byteBuffer.getInt(offset);
		byte[] bait = new byte[panjang];
		byteBuffer.get(bait);
		return bait;
	}
	
	public void setBytes(int offset, byte[] bait) {
		byteBuffer.position(offset);
		byteBuffer.putInt(bait.length);
		byteBuffer.put(bait);
	}
	
	public String getString(int offset) {
		byte[] bait = getBytes(offset);
		return new String(bait, CHARSET);
	}
	
	public void setString(int offset, String s) {
		byte[] bait = s.getBytes(CHARSET);
		setBytes(offset, bait);
	}
	
	ByteBuffer contents() {
		byteBuffer.position(0);
		return byteBuffer;
	}
}
