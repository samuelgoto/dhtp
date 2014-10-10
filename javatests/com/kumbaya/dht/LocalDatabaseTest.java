package com.kumbaya.dht;

import static org.junit.Assert.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.DatabaseSecurityConstraint;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.Contact.State;
import org.limewire.mojito.routing.impl.RemoteContact;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.ContactUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import ca.odell.glazedlists.impl.Preconditions;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

public class LocalDatabaseTest {
	@Before
	public void setUp() {
		NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
		NetworkSettings.FILTER_CLASS_C.setValue(false);
		ContactUtils.setNetworkInstanceUtils(new SimpleNetworkInstanceUtils(false));
	}
	
	private Contact remote(SocketAddress address) {
		return new RemoteContact(
				address,
				Vendor.UNKNOWN, Version.ZERO,
				Keys.of("localhost:8080"),
				address,
				0, 0, State.ALIVE);
	}
	
	private InetSocketAddress ip(String hostname, int port) {
		return InetSocketAddress.createUnresolved(hostname, port);
	}
	
	private DHTValueEntity value(SocketAddress address, KUID key, DHTValue value) {
		return DHTValueEntity.createFromRemote(
				remote(address), remote(address), key, value);
	}
	
	@Test
	public void testDatabase() {
		String file = createFile();
		LocalDatabase db = new LocalDatabase(db(file));
		db.store(value(ip("localhost", 8080), Keys.of("key"), Values.of("value")));
		
		LocalDatabase db2 = new LocalDatabase(db(file));
		assertTrue(db2.contains(Keys.of("key"), null));
		assertEquals(
				Values.of("value"),
				db2.get(Keys.of("key")).get(Keys.of("key")).getValue());
	}
	
	private String createFile() {
		int id = new Random().nextInt();
		return "/tmp/test-" + id + ".db";
	}
	
	private DB db(String name) {
		return DBMaker.newFileDB(new File(name))
				.closeOnJvmShutdown()
				.make();
	}
	
	@Test
	public void testDHT() throws Exception {
		MojitoDHT dht = MojitoFactory.createDHT("bootstrap");
		dht.setDatabase(new LocalDatabase(db(createFile())));
		dht.bind(new InetSocketAddress("localhost", 8080));
		dht.start();

		MojitoDHT node = MojitoFactory.createDHT("node");
		node.setDatabase(new LocalDatabase(db(createFile())));
		node.bind(new InetSocketAddress("localhost", 8081));
		node.start();
		node.bootstrap(new InetSocketAddress("localhost", 8080)).get();
		assertTrue(node.isBootstrapped());

		DHTValueImpl value = new DHTValueImpl(
				DHTValueType.TEXT, Version.ZERO, "hello world".getBytes());

		node.put(Keys.of("key"), value).get();

		FindValueResult result = node.get(EntityKey.createEntityKey(
				Keys.of("key"), DHTValueType.TEXT)).get();

		assertTrue(result.isSuccess());
		assertEquals(1, result.getEntities().size());
		assertEquals("hello world",
				new String(result.getEntities().iterator().next().getValue().getValue()));

		node.close();
		dht.close();
	}
}
