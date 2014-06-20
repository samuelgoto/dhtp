package com.kumbaya.dht;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Before;
import org.junit.Test;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.StatusCode;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.Tag;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.FindNodeRequest;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.MessageFormatException;
import org.limewire.mojito.messages.PingRequest;
import org.limewire.mojito.messages.PingResponse;
import org.limewire.mojito.messages.StoreRequest;
import org.limewire.mojito.messages.StoreResponse;
import org.limewire.mojito.messages.impl.FindNodeResponseImpl;
import org.limewire.mojito.messages.impl.PingResponseImpl;
import org.limewire.mojito.messages.impl.StoreResponseImpl;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.BootstrapResult.ResultType;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Contact.State;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.impl.RemoteContact;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;

import com.google.common.collect.ImmutableSet;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageDispatcherTest {
	private IMocksControl control = EasyMock.createControl();
	MessageDispatcherFactory messageFactory = control.createMock(
			MessageDispatcherFactory.class);
	Dispatcher messageDispatcher = control.createMock(
			Dispatcher.class);

	@Before
	public void setUp() {
		control.reset();
		NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
		NetworkSettings.FILTER_CLASS_C.setValue(false);
		ContactUtils.setNetworkInstanceUtils(new SimpleNetworkInstanceUtils(false));
	}

	private HttpMessageDispatcher messageFactory(MojitoDHT node) {
		HttpMessageDispatcher httpMessageDispatcher = new HttpMessageDispatcher(
				(Context) node, messageDispatcher);
		expect(messageFactory.create(isA(Context.class))).andReturn(
				httpMessageDispatcher);
		return httpMessageDispatcher;
	}

	@Test
	public void testBootstrapingANodeAndStoringAValueWithMockBootstrap() throws Exception {
		MojitoDHT node = MojitoFactory.createDHT("local test node");

		HttpMessageDispatcher httpMessageDispatcher = messageFactory(
				node);

		messageDispatcher.bind(isA(SocketAddress.class));

		// While bootstraping, the node pings and sends a find node request
		// to the bootstrap node.
		Capture<Tag> pingRequestTag = new Capture<Tag>();
		expect(messageDispatcher.submit(capture(pingRequestTag))).andReturn(true);

		Capture<Tag> findNodeRequestTag = new Capture<Tag>();
		expect(messageDispatcher.submit(capture(findNodeRequestTag))).andReturn(true);

		// While storing a result, the node sends a find node request,
		// to which the bootstrap node replies.
		Capture<Tag> findNodeRequestTag2 = new Capture<Tag>();
		expect(messageDispatcher.submit(capture(findNodeRequestTag2))).andReturn(true);

		Capture<Tag> storeRequestTag = new Capture<Tag>();
		expect(messageDispatcher.submit(capture(storeRequestTag))).andReturn(true);

		control.replay();

		node.setMessageDispatcher(messageFactory);
		InetSocketAddress local = InetSocketAddress.createUnresolved("localhost", 8081);
		node.bind(local);
		node.start();
		InetSocketAddress bootstrap = InetSocketAddress.createUnresolved("localhost", 8080);
		DHTFuture<BootstrapResult> result = node.bootstrap(bootstrap);

		// Allows the message executor to catch up.
		Thread.sleep(200);

		PingRequest pingRequest = (PingRequest) pingRequestTag.getValue().getMessage();

		Contact contact = new RemoteContact(
				bootstrap,
				Vendor.UNKNOWN, Version.ZERO, 
				KUID.createRandomID(),
				bootstrap, 
				1, 0, State.ALIVE);

		PingResponse pingResponse = new PingResponseImpl(
				(Context) node, contact, pingRequest.getMessageID(), 
				local, 
				BigInteger.ONE);

		httpMessageDispatcher.handleMessage(pingResponse);

		Thread.sleep(200);

		FindNodeRequest findNodeRequest = (FindNodeRequest) findNodeRequestTag.getValue().getMessage();

		FindNodeResponse findNodeResponse = new FindNodeResponseImpl(
				(Context) node, contact, findNodeRequest.getMessageID(), 
				null, Collections.<Contact>emptySet());

		httpMessageDispatcher.handleMessage(findNodeResponse);

		Thread.sleep(200);

		assertEquals(ResultType.BOOTSTRAP_SUCCEEDED, result.get().getResultType());

		assertTrue(node.isBootstrapped());

		DHTFuture<StoreResult> storeResult = node.put(
				Keys.of("foo"), Values.of("bar"));

		Thread.sleep(200);

		FindNodeRequest findNodeRequest2 = (FindNodeRequest) findNodeRequestTag2.getValue().getMessage();

		FindNodeResponse findNodeResponse2 = new FindNodeResponseImpl(
				(Context) node, contact, findNodeRequest2.getMessageID(), 
				null, Collections.<Contact>emptySet());

		Thread.sleep(200);

		httpMessageDispatcher.handleMessage(findNodeResponse2);

		StoreRequest storeRequest = (StoreRequest) storeRequestTag.getValue().getMessage();

		StoreResponse storeResponse = new StoreResponseImpl(
				(Context) node, contact, storeRequest.getMessageID(),
				ImmutableSet.of(new StoreResponse.StoreStatusCode(
						Keys.of("foo"), ((Context) node).getLocalNodeID(), 
						StoreResponse.OK)));

		httpMessageDispatcher.handleMessage(storeResponse);

		assertEquals(1, storeResult.get().getValues().size());
		assertEquals(Values.of("bar"),
				storeResult.get().getValues().iterator().next().getValue());
	}

	@Test
	public void testDHTWithMockDispatchers() throws Exception {
		final Context dht = (Context) MojitoFactory.createDHT("bootstrap");
		final Context node = (Context) MojitoFactory.createDHT("node");

		final InetSocketAddress dhtIp = new InetSocketAddress("localhost", 8080);
		final InetSocketAddress nodeIp = new InetSocketAddress("localhost", 8081);

		final HttpMessageDispatcher httpMessageDispatcher = messageFactory(
				dht);

		MessageDispatcherFactory messageFactory2 = control.createMock(
				MessageDispatcherFactory.class);
		Dispatcher messageDispatcher2 = control.createMock(
				Dispatcher.class);

		final HttpMessageDispatcher httpMessageDispatcher2 = new HttpMessageDispatcher(
				(Context) node, messageDispatcher2);
		expect(messageFactory2.create(isA(Context.class))).andReturn(
				httpMessageDispatcher2);

		messageDispatcher.bind(isA(SocketAddress.class));
		messageDispatcher2.bind(isA(SocketAddress.class));

		// All messages that gets submitted to the first dispatcher
		// get directly pushed to the second dispatcher, and vice versa.
		final Capture<Tag> tag = new Capture<Tag>();
		expect(messageDispatcher.submit(capture(tag))).andAnswer(new IAnswer<Boolean>() {
			@Override
			public Boolean answer() throws Throwable {
				DHTMessage source = tag.getValue().getMessage();
				final ByteBuffer data = dht.getMessageFactory().writeMessage(nodeIp, source);

				// the data gets transmitted over the wire.
				node.getDHTExecutorService().execute(new Runnable() {
					@Override
					public void run() {
						try {
							DHTMessage destination = node.getMessageFactory()
									.createMessage(dhtIp, data);
							httpMessageDispatcher2.handleMessage(destination);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});

				return true;
			}
		}).anyTimes();

		final Capture<Tag> tag2 = new Capture<Tag>();
		expect(messageDispatcher2.submit(capture(tag2))).andAnswer(new IAnswer<Boolean>() {
			@Override
			public Boolean answer() throws Throwable {
				DHTMessage source = tag2.getValue().getMessage();
				final ByteBuffer data = node.getMessageFactory()
						.writeMessage(dhtIp, source);

				// the data gets transmitted over the wire.
				dht.getDHTExecutorService().execute(new Runnable() {
					@Override
					public void run() {
						try {
							DHTMessage destination = dht.getMessageFactory()
									.createMessage(nodeIp, data);
							httpMessageDispatcher.handleMessage(destination);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				return true;
			}
		}).anyTimes();

		control.replay();

		dht.setMessageDispatcher(messageFactory);
		dht.bind(dhtIp);
		dht.start();

		node.setMessageDispatcher(messageFactory2);
		node.bind(nodeIp);
		node.start();
		node.bootstrap(dhtIp).get();
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
	public void testDHTWithHttpDispatchers() throws Exception {
		control.replay();

		Context dht = (Context) MojitoFactory.createDHT("bootstrap");
		Context node = (Context) MojitoFactory.createDHT("node");
		dht.setMessageDispatcher(new MessageDispatcherFactoryImpl());
		dht.bind(new InetSocketAddress("localhost", 8080));
		dht.start();

		node.setMessageDispatcher(new MessageDispatcherFactoryImpl());
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
		
		System.out.println(new InetSocketAddress("sgoto-dht.herokuapp.com", 80));
	}
}
