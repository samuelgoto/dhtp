package com.kumbaya.router;

import com.kumbaya.router.Serializer.Field;
import com.kumbaya.router.Serializer.Type;
import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;

public class PacketTest extends TestCase {
  public static class Registry {
    public static final int NAME = 1;
    public static final int NAME_COMPONENT = 2;
    public static final int IMPLICIT_SHA256_DIGEST_COMPONENT = 3;
    public static final int INTEREST = 4;
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
    Name name;
  }
  
  public void testSerializingPackets() throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Name packet = new Name();
    packet.name = "foo";
    packet.sha256 = "bar";
    Serializer.serialize(stream, packet);
    Name result = Serializer.unserialize(Name.class, stream.toByteArray());
    assertEquals("foo", result.name);
    assertEquals("bar", result.sha256);
  }
}