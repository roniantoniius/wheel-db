package com.aantoniusron.wheeldb.storage_engine.impl.data.heap.page;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/*
 * identifier untuk suatu record,
 * id mengandung nomor Block dan lokasi suatu record pada Block tersebut
 */
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class RecordKey implements Serializable {
	private static final long serialVersionUID = 1L;
	private int nomorBlok;
	private int nomorSlot;
}
