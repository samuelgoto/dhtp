package com.kumbaya.router;

import com.kumbaya.router.Serializer.Field;
import junit.framework.TestCase;

public class PacketTest extends TestCase {
  public static class Type {
    public static final int NAME = 1;
    public static final int NAME_COMPONENT = 2;
    public static final int IMPLICIT_SHA256_DIGEST_COMPONENT = 3;
  }
  
  static class Name {
    @Field(Type.NAME_COMPONENT)
    String name;
    @Field(Type.IMPLICIT_SHA256_DIGEST_COMPONENT)
    String sha256;
  }
  
  public void testSerializingPackets() {
    
  }
}