package com.aantoniusron.wheeldb.storage_engine.common.file;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/*
 * identifier for an Page (Block) in a db heapfile
 */
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class BlockId {
	private final String namaFile;
	private final int angkaBlock;
}
