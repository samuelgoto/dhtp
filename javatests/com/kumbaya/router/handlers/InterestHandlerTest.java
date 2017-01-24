package com.kumbaya.router.handlers;

import com.google.common.base.Optional;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.router.Kumbaya;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.TcpServer.Handler;
import com.kumbaya.router.TcpServer.Interface;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;

public class InterestHandlerTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final Kumbaya kumbaya = control.createMock(Kumbaya.class);
  private final Interface queue = control.createMock(Interface.class);
  
  @Override
  public void setUp() {
    control.reset();
  }
  
  @Override
  public void tearDown() {
    control.verify();
  }
  
  public void testForwardingInterest_andGettingEmptyResponse() throws Exception {
    kumbaya.send(eq(InetSocketAddresses.parse("localhost:8082")), isA(Interest.class));
    expectLastCall().andReturn(Optional.absent());
    
    queue.close();
    
    control.replay();
    
    Handler<Interest> handler = new InterestHandler(kumbaya);
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
    
    Handler<Interest> handler = new InterestHandler(kumbaya);
    Interest request = new Interest();
    request.getName().setName("/hello/world");
    handler.handle(request, queue);
  }
}

