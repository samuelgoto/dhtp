package com.kumbaya.dht;

import static org.junit.Assert.*;

import org.junit.Test;

public class ValuesTest {

	@Test
	public void testHashingMimeTypes() {
		assertEquals(
				Values.of("value1", "text/html").getValueType().toInt(),
				Values.of("value2", "text/html").getValueType().toInt());
	}
}
