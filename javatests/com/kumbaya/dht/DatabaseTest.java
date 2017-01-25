package com.kumbaya.dht;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.mojito.io.Tag;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.ContactUtils;

import com.google.common.collect.ImmutableMap;

public class DatabaseTest {
	private IMocksControl control = EasyMock.createControl();
	Database database = control.createMock(Database.class);

	@Before
	public void setUp() {
		NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
		NetworkSettings.FILTER_CLASS_C.setValue(false);
		ContactUtils.setNetworkInstanceUtils(new SimpleNetworkInstanceUtils(false));
	}

	@Test
	public void testStartingAndStopping() throws Exception {
		database.clear();
		
		control.replay();
		
		MojitoDHT dht = MojitoFactory.createDHT("bootstrap");
		dht.setDatabase(database);
		dht.bind(new InetSocketAddress("localhost", 8080));
		dht.start();
		dht.close();
	}
	
	@Test
	public void testStoringRemotely() throws Exception {
		database.clear();
		
		final Capture<DHTValueEntity> aValue = new Capture<DHTValueEntity>();
		expect(database.store(capture(aValue))).andReturn(true);
		
		final Capture<KUID> id = new Capture<KUID>();
		expect(database.get(capture(id))).andAnswer(
				new IAnswer<Map<KUID, DHTValueEntity>>() {
					@Override
					public Map<KUID, DHTValueEntity> answer() throws Throwable {
						return ImmutableMap.of(id.getValue(), aValue.getValue());
					}
				});
		
		expect(database.getRequestLoad(Keys.of("key"), true)).andReturn(0.0f);
		
		control.replay();
		
		MojitoDHT dht = MojitoFactory.createDHT("bootstrap");
		dht.setDatabase(database);
		dht.bind(new InetSocketAddress("localhost", 8080));
		dht.start();

		MojitoDHT node = MojitoFactory.createDHT("node");
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

	@Test
	public void testStoringLocally() throws Exception {
		database.clear();
		
		final Capture<DHTValueEntity> aValue = new Capture<DHTValueEntity>();
		expect(database.store(capture(aValue))).andReturn(true);
		
		final Capture<KUID> id = new Capture<KUID>();
		expect(database.get(capture(id))).andAnswer(
				new IAnswer<Map<KUID, DHTValueEntity>>() {
					@Override
					public Map<KUID, DHTValueEntity> answer() throws Throwable {
						return ImmutableMap.of(id.getValue(), aValue.getValue());
					}
				});
		
		expect(database.getRequestLoad(Keys.of("key"), true)).andReturn(0.0f);
		
		control.replay();
		
		MojitoDHT dht = MojitoFactory.createDHT("bootstrap");
		dht.bind(new InetSocketAddress("localhost", 8080));
		dht.start();

		MojitoDHT node = MojitoFactory.createDHT("node");
		node.setDatabase(database);
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
