package com.kumbaya.router;

import com.kumbaya.router.Serializer.Field;
import com.kumbaya.router.Serializer.Type;

public class Packets {

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
    static {
      Serializer.register(Interest.class);
    }

    Name name = new Name();
    @Field(Registry.NONCE)
    long nonce;
    @Field(Registry.INTEREST_LIFETIME)
    int lifetime;
  }
  
  @Type(Registry.DATA)
  static class Data {
    static {
      Serializer.register(Data.class);
    }
    
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
}
