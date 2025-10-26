package com.aantoniusron.wheeldb.server.b_query_engine.common.katalog.tabel;

import com.aantoniusron.wheeldb.storage_engine.common.transaksi.Transaksi;

/*
 * Tabel Manager, punya method seperti:
 * - bikin tabel baru
 * - simpan Meta Data pada Katalog
 * - ambil meta data dari tabel yang sebelumnya baru dibuat
 */
public class TabelManager {
	// max char dri nama tabel atau nama suatu field
	public static final int MAX_NAMA = 16;
	private TabelLayoutPhysical nilaiRecordTabelKatalogLayout, nilaiRecordFieldKatalogLayout;
	
	/*
	 * construct manager katalog untuk sistem DB
	 * kalau databasenya baru maka bikin dua katalog tabel
	 * 
	 * @transaksi adalah transaksi yang baru dimulai
	 */
	public TabelManager(boolean isNew, Transaksi transaksi) {
		DefinisiTabel tabelKatalogDefinisiTabel = new DefinisiTabel(); 
		tabelKatalogDefinisiTabel.addStrField("namatabel", MAX_NAMA);
		tabelKatalogDefinisiTabel.addIntField("ukuranslot");
		nilaiRecordTabelKatalogLayout = new TabelLayoutPhysical(tabelKatalogDefinisiTabel);
		
		DefinisiTabel fieldKatalogDefinisiTabel = new DefinisiTabel();
		fieldKatalogDefinisiTabel.addStrField("namatabel", MAX_NAMA);
		fieldKatalogDefinisiTabel.addStrField("namafield", MAX_NAMA);
		fieldKatalogDefinisiTabel.addIntField("tipe");
		fieldKatalogDefinisiTabel.addIntField("panjang");
		fieldKatalogDefinisiTabel.addIntField("offset");
		nilaiRecordFieldKatalogLayout = new TabelLayoutPhysical(fieldKatalogDefinisiTabel);
		
		if (isNew) {
			buatTabel();
		}
	}
	
	/*
	 * buat tabel baru dengan nama dan skema tertentu
	 */
	public void buatTabel(String namaTabel, DefinisiTabel skema, Transaksi transaksi) {
		TabelLayoutPhysical nilaiRecordLayout = new TabelLayoutPhysical(skema);
		
		// need an catalog
		
	}
}
