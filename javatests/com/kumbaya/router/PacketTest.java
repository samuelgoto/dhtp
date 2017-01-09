package com.kumbaya.router;

import com.kumbaya.router.Serializer.Field;
import com.kumbaya.router.Serializer.Type;
import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;
import org.junit.Assert;

public class PacketTest extends TestCase {
  
  // Registers typical packet types.
  {
    Serializer.register(Interest.class);
    Serializer.register(Data.class);
  }
  
  public static class Registry {
    public static final int NAME = 1;
    public static final int NAME_COMPONENT = 2;
    public static final int IMPLICIT_SHA256_DIGEST_COMPONENT = 3;
    
    public static final int INTEREST = 4;    
    public static final int NONCE = 5;
    public static final int INTEREST_LIFETIME = 6;
    
    public static final int DATA = 7;
    public static final int META_INFO = 8;
    public static final int FRESHNESS_PERIOD = 9;
    public static final int CONTENT = 10;
  }
 
  @Type(Registry.NAME)
  static class Name {
    @Field(Registry.NAME_COMPONENT)
    String name;
    @Field(Registry.IMPLICIT_SHA256_DIGEST_COMPONENT)
    String sha256;
  }
  
  @Type(Registry.INTEREST)
  static class Interest {
    Name name = new Name();
    @Field(Registry.NONCE)
    long nonce;
    @Field(Registry.INTEREST_LIFETIME)
    int lifetime;
  }
  
  @Type(Registry.DATA)
  static class Data {
    Name name = new Name();
    MetaInfo metadata = new MetaInfo();
    @Field(Registry.CONTENT)
    byte[] content;
  }
  
  @Type(Registry.META_INFO)
  static class MetaInfo {
    @Field(Registry.FRESHNESS_PERIOD)
    int freshnessPeriod;
  }
  
  public void testSerializingInterest() throws Exception {
    Interest packet = new Interest();
    packet.name.name = "foo";
    packet.name.sha256 = "bar";

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, packet);
    Interest result = Serializer.unserialize(stream.toByteArray());
    
    assertEquals("foo", result.name.name);
    assertEquals("bar", result.name.sha256);
  }

  public void testSerializingData() throws Exception {
    Data packet = new Data();
    packet.name.name = "foo";
    packet.name.sha256 = "bar";
    packet.metadata.freshnessPeriod = 2;
    packet.content = "hello world".getBytes();

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, packet);
    Data result = Serializer.unserialize(stream.toByteArray());
    
    assertEquals("foo", result.name.name);
    assertEquals("bar", result.name.sha256);
    assertEquals(2, result.metadata.freshnessPeriod);
    Assert.assertArrayEquals("hello world".getBytes(), result.content);
  }
  
  public void testStreamOfPackets() throws Exception {
    Data packet = new Data();
    packet.name.name = "foo";
    packet.name.sha256 = "bar";
    packet.metadata.freshnessPeriod = 2;
    packet.content = "hello world".getBytes();

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, packet);
    Object result = Serializer.unserialize(stream.toByteArray());
    
    assertTrue(result instanceof Data);
    assertEquals("foo", ((Data) result).name.name);
    assertEquals("bar", ((Data) result).name.sha256);
    assertEquals(2, ((Data) result).metadata.freshnessPeriod);
    Assert.assertArrayEquals("hello world".getBytes(), ((Data) result).content);
  }

}