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
    public static final int CONTENT_TYPE = 10;
  }
  
  // TODO(goto): figure out a more foolproof way to get this done. Not ideal.
  public static void register() {
    Serializer.register(Interest.class);
    Serializer.register(Data.class);
  }
  
  @Type(Registry.NAME)
  public static class Name {
    @Field(Registry.NAME_COMPONENT)
    String name;
    
    public Name setName(String name) {
      this.name = name;
      return this;
    }
    
    public String getName() {
      return this.name;
    }
  }
  
  @Type(Registry.INTEREST)
  public static class Interest {
    static {
      Serializer.register(Interest.class);
    }

    Name name = new Name();
    @Field(Registry.INTEREST_LIFETIME)
    int lifetime;
    
    public Interest setName(Name name) {
      this.name = name;
      return this;
    }
    
    public Name getName() {
      return this.name;
    }

    public Interest setLifetime(int lifetime) {
      this.lifetime = lifetime;
      return this;
    }
    
    public int getLifetime() {
      return this.lifetime;
    }
  }
  
  @Type(Registry.DATA)
  public static class Data {
    static {
      Serializer.register(Data.class);
    }
    
    Name name = new Name();
    MetaInfo metadata = new MetaInfo();
    @Field(Registry.CONTENT)
    byte[] content;

    public Name getName() {
      return name;
    }
    
    public Data setName(Name name) {
      this.name = name;
      return this;
    }
    
    public MetaInfo getMetadata() {
      return metadata;
    }
    
    public Data setMetadata(MetaInfo metadata) {
      this.metadata = metadata;
      return this;
    }
    
    public byte[] getContent() {
      return content;
    }
    
    public Data setContent(byte[] content) {
      this.content = content;
      return this;
    }
  }
  
  @Type(Registry.META_INFO)
  public static class MetaInfo {
    @Field(Registry.FRESHNESS_PERIOD)
    int freshnessPeriod;

    @Field(Registry.CONTENT_TYPE)
    String contentType;

    public int getFreshnessPeriod() {
      return freshnessPeriod;
    }

    public MetaInfo setFreshnessPeriod(int freshnessPeriod) {
      this.freshnessPeriod = freshnessPeriod;
      return this;
    }

    public String getContentType() {
      return contentType;
    }

    public MetaInfo setContentType(String contentType) {
      this.contentType = contentType;
      return this;
    }
  }
}
