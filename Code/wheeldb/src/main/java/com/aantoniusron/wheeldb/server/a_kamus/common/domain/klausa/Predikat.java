package com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aantoniusron.wheeldb.storage_engine.RecordScan;

// Predikat itu kombinasi Boolean terhadap suatu Ketentuan (term), jdi kyk nempel

public class Predikat {
	private List<Ketentuan> daftarKetentuan = new ArrayList<Ketentuan>();
	
	// constructor to set this Predikat are `true`
	public Predikat() {};
	
	// another constructor Predikat able to add a single new Ketentuan
	public Predikat(Ketentuan ketentuan) {
		daftarKetentuan.add(ketentuan);
	}
	
	// method untuk join
	// suatu predikat dapat menjadi konjungsi antara dirinya atau Predikat lainnya
	public void konjungsiKe(Predikat predikat) {
		daftarKetentuan.addAll(predikat.daftarKetentuan);
	}
	
	// method untuk pastiin proses scan sudah selesai (true) atau belum (false)
	public boolean sudahSelesai(RecordScan scan) {
		for (Ketentuan ketentuan: daftarKetentuan) {
			if (!ketentuan.sudahSelesai(scan)) 
				return false;
		}
		return true;
	}
	
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		Iterator<Ketentuan> iterator = daftarKetentuan.iterator();
		if (!iterator.hasNext()) return "";
		
		String hasilString = iterator.next().toString();
		while (iterator.hasNext()){
			hasilString += " dan " + iterator.next().toString();
		}
		
		return hasilString;
	}
}