package com.kumbaya.router.handlers;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;

import com.google.common.base.Optional;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.dht.Dht;
import com.kumbaya.router.Kumbaya;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Handler;
import com.kumbaya.router.TcpServer.Interface;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InterestHandlerTest {
  private final IMocksControl control = EasyMock.createControl();
  private final Kumbaya kumbaya = control.createMock(Kumbaya.class);
  private final Dht dht = control.createMock(Dht.class);
  private final Interface queue = control.createMock(Interface.class);

  @Before
  public void setUp() {
    control.reset();
  }

  @After
  public void tearDown() {
    control.verify();
  }

  @Test
  public void testFoo() {
    control.replay();
  }

  public void testForwardingInterest_andGettingEmptyResponse() throws Exception {
    kumbaya.send(eq(InetSocketAddresses.parse("localhost:8082")), isA(Interest.class));
    expectLastCall().andReturn(Optional.absent());

    queue.close();

    control.replay();

    Handler<Interest> handler = new InterestHandler(kumbaya, dht);
    Interest request = new Interest();
    request.getName().setName("/foo/bar");
    handler.handle(request, queue);
  }

  public void testForwardingInterest_andGettingAResponse() throws Exception {
    Data data = new Data();
    data.getName().setName("/hello/world");
    data.setContent("helloworld".getBytes());

    kumbaya.send(eq(InetSocketAddresses.parse("localhost:8082")), isA(Interest.class));
    expectLastCall().andReturn(Optional.of(data));

    queue.push(isA(Data.class));
    expectLastCall().andReturn(queue);

    queue.close();

    control.replay();

    Handler<Interest> handler = new InterestHandler(kumbaya, dht);
    Interest request = new Interest();
    request.getName().setName("/hello/world");
    handler.handle(request, queue);
  }
}

