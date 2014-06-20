package com.kumbaya.dht;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;

import org.junit.Test;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;

public class MessageSerializationTest {

	@Test
	public void testSerializeResolved() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		MessageOutputStream message = new MessageOutputStream(bytes);
		message.writeSocketAddress(new InetSocketAddress("192.168.0.10", 80));
		message.close();
		
		MessageInputStream is = new MessageInputStream(
				new ByteArrayInputStream(bytes.toByteArray()), null);
		
		InetSocketAddress result = is.readSocketAddress();
		is.close();
		
		assertEquals("192.168.0.10:80", result.toString());
	}

	@Test
	public void testSerializeUnresolved() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		MessageOutputStream message = new MessageOutputStream(bytes);
		message.writeSocketAddress(InetSocketAddress.createUnresolved(
				"sgo.to", 80));
		message.close();
		
		MessageInputStream is = new MessageInputStream(
				new ByteArrayInputStream(bytes.toByteArray()), null);
		
		InetSocketAddress result = is.readSocketAddress();
		is.close();

		assertEquals("sgo.to", result.getHostName());
		
	}
}
