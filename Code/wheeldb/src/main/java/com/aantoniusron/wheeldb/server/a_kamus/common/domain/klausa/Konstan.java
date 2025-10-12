package com.aantoniusron.wheeldb.server.a_kamus.common.domain.klausa;

import java.io.Serializable;

// class yang nandain bahwa nilai sudah disimpan dalam database

// an comparable Class to its own class
// an serialisasi class so that able to be converted into byte
// benefit of serialization is: store this Class into a File, transfer object into different memory
public class Konstan implements Comparable<Konstan>, Serializable{

	private static final long serialVersionUID = 1L;
	
	private Integer integerValue;
	private String stringValue;
	
	public Konstan(Integer integer) {
		this.integerValue = integer;
	}
	
	public Konstan(String string) {
		this.stringValue = string;
	}
	
	public Integer asInt() {
		return integerValue;
	}
	
	public String asStr() {
		return stringValue;
	}
	
	public boolean equals(Object obj) {
		Konstan konstan = (Konstan) obj;
		return (integerValue != null) ? integerValue.equals(konstan.integerValue) : stringValue.equals(konstan.stringValue);
	}
	
	@Override
	public int compareTo(Konstan o) {
		// TODO Auto-generated method stub
		return (integerValue != null) ? integerValue.compareTo(o.integerValue) : stringValue.compareTo(o.stringValue);
	}
	
	@Override
	public int hashCode() {
		return (integerValue != null) ? integerValue.hashCode() : stringValue.hashCode();
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return (integerValue != null) ? integerValue.toString() : stringValue.toString();
	}
	
}
