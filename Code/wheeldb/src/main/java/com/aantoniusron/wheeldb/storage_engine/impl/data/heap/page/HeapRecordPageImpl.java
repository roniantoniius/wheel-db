package com.aantoniusron.wheeldb.storage_engine.impl.data.heap.page;

import com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel.TabelLayoutPhysical;
import com.aantoniusron.wheeldb.storage_engine.common.file.BlockId;
import com.aantoniusron.wheeldb.storage_engine.common.transaksi.Transaksi;

/*
 * an heapfile system to interact with Page (Block)
 */
public class HeapRecordPageImpl {
	public static final int EMPTY = 0, USED = 1;
	private Transaksi transaksi;
	private BlockId blockId;
	private TabelLayoutPhysical nilaiRecordLayout;
	
	public HeapRecordPageImpl(Transaksi transaksi, BlockId blockId, TabelLayoutPhysical nilaiLayoutPhysical) {
		this.transaksi = transaksi;
		this.blockId = blockId;
		this.nilaiRecordLayout = nilaiLayoutPhysical;
	}
	
	/*
	 * ambil nilai int dari field dan slot tertentu
	 */
	public int getInt(int slot, String namafield) {
		int posisiField = offset(slot) + nilaiRecordLayout.getOffset(namafield);
		return transaksi.getInt(blockId, posisiField);
	}
	
	private int offset(int slot) {
		return slot * nilaiRecordLayout.getUkuranSlot();
	}
	
	private boolean isSlotValid(int slot) {
		return offset(slot + 1) <= transaksi.ukuranBlok();
	}
}
