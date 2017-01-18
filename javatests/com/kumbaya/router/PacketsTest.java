package com.kumbaya.router;

import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;
import org.junit.Assert;

public class PacketsTest extends TestCase {
  
  public void testSerializingInterest() throws Exception {
    Interest packet = new Interest();
    packet.getName().setName("foo");

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, packet);
    Interest result = Serializer.unserialize(stream.toByteArray());
    
    assertEquals("foo", result.getName().getName());
  }

  public void testSerializingData() throws Exception {
    Data packet = new Data();
    packet.getName().setName("foo");
    packet.getMetadata().setFreshnessPeriod(2);
    packet.getMetadata().setContentType("text/html");
    packet.setContent("hello world".getBytes());

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, packet);
    Data result = Serializer.unserialize(stream.toByteArray());
    
    assertEquals("foo", result.getName().getName());
    assertEquals(2, result.getMetadata().getFreshnessPeriod());
    Assert.assertArrayEquals("hello world".getBytes(), result.getContent());
  }
  
  public void testStreamOfPackets() throws Exception {
    Data packet = new Data();
    packet.getName().setName("foo");
    packet.getMetadata().setFreshnessPeriod(2);
    packet.getMetadata().setContentType("text/html");
    packet.setContent("hello world".getBytes());

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, packet);
    Object result = Serializer.unserialize(stream.toByteArray());
    
    assertTrue(result instanceof Data);
    assertEquals("foo", ((Data) result).getName().getName());
    assertEquals(2, ((Data) result).getMetadata().getFreshnessPeriod());
    Assert.assertArrayEquals("hello world".getBytes(), ((Data) result).getContent());
  }

}