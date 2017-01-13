package com.kumbaya.router;

import com.google.common.base.Optional;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Serializer.Field;
import com.kumbaya.router.Serializer.Type;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

import junit.framework.TestCase;

import org.junit.Assert;

public class SerializerTest extends TestCase {
  
  @Type(0)
  static class TypeWithPrimitives {
    @Field(1)
    String hello;
    @Field(2)
    String world;
    @Field(3)
    int foo;
    @Field(4)
    long bar;
    @Field(5)
    boolean hi;
    @Field(6)
    boolean bye;
    @Field(7)
    byte[] content;
  }
  
  public void testPrimitives() throws Exception {
    TypeWithPrimitives foo = new TypeWithPrimitives();
    foo.hello = "hello";
    foo.world = "world";
    foo.foo = 1;
    foo.bar = 2;
    foo.hi = true;
    foo.content = "cafebabe".getBytes();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, foo);
    Serializer.register(TypeWithPrimitives.class);
    TypeWithPrimitives bar = Serializer.unserialize(stream.toByteArray());
    assertEquals(foo.hello, bar.hello);
    assertEquals(foo.world, bar.world);
    assertEquals(foo.foo, bar.foo);
    assertEquals(foo.bar, bar.bar);
    assertTrue(bar.hi);
    assertFalse(bar.bye);
    Assert.assertArrayEquals("cafebabe".getBytes(), bar.content);
  }

  @Type(0)
  static class TypeWithOptional {
    @Field(1)
    Optional<String> foo;
  }
  
  public void testOptionalFields() throws Exception {
    TypeWithOptional foo = new TypeWithOptional();
    foo.foo = Optional.of("hello world");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, foo);
    Serializer.register(TypeWithOptional.class);
    TypeWithOptional bar = Serializer.unserialize(stream.toByteArray());
    assertTrue(bar.foo.isPresent());
    assertEquals("hello world", bar.foo.get());
  }
  
  public void testOptionalFields_absent() throws Exception {
    TypeWithOptional foo = new TypeWithOptional();
    foo.foo = Optional.absent();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, foo);
    Serializer.register(TypeWithOptional.class);
    TypeWithOptional bar = Serializer.unserialize(stream.toByteArray());
    assertFalse(bar.foo.isPresent());
  }
  
  public void testOptionalFields_emptyString() throws Exception {
    TypeWithOptional data = new TypeWithOptional();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    Serializer.register(TypeWithOptional.class);
    TypeWithOptional result = Serializer.unserialize(stream.toByteArray());
    assertFalse(result.foo.isPresent());
  }
  
  @Type(0)
  static class TypeWithNesting {
    TypeBeingNested foo;
  }
  
  @Type(1)
  static class TypeBeingNested {
    @Field(2)
    String hello;
  }
  
  public void testNestedTypes() throws Exception {
    TypeWithNesting foo = new TypeWithNesting();
    TypeBeingNested bar = new TypeBeingNested();
    foo.foo = bar;
    bar.hello = "world";
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, foo);
    Serializer.register(TypeWithNesting.class);
    TypeWithNesting result = Serializer.unserialize(stream.toByteArray());
    assertEquals("world", result.foo.hello);
  }
  
  @Type(0)
  static class TwoOptionalFields {
    @Field(1)
    Optional<String> foo;
    @Field(2)
    Optional<String> bar;
  }
  
  public void testTwoOptionalFields_bothAbsent() throws Exception {
    TwoOptionalFields data = new TwoOptionalFields();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    Serializer.register(TwoOptionalFields.class);
    TwoOptionalFields result = Serializer.unserialize(stream.toByteArray());
    assertFalse(result.foo.isPresent());
    assertFalse(result.bar.isPresent());
  }

  public void testTwoOptionalFields_firstPresent() throws Exception {
    TwoOptionalFields data = new TwoOptionalFields();
    data.foo = Optional.of("hello world");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    Serializer.register(TwoOptionalFields.class);
    TwoOptionalFields result = Serializer.unserialize(stream.toByteArray());
    assertTrue(result.foo.isPresent());
    assertEquals(result.foo.get(), "hello world");
    assertFalse(result.bar.isPresent());
  }

  public void testTwoOptionalFields_secondPresent() throws Exception {
    TwoOptionalFields data = new TwoOptionalFields();
    data.bar= Optional.of("hello world");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    Serializer.register(TwoOptionalFields.class);
    TwoOptionalFields result = Serializer.unserialize(stream.toByteArray());
    assertFalse(result.foo.isPresent());
    assertTrue(result.bar.isPresent());
    assertEquals(result.bar.get(), "hello world");
  }

  public void testTwoOptionalFields_bothPresent() throws Exception {
    TwoOptionalFields data = new TwoOptionalFields();
    data.foo = Optional.of("hello");
    data.bar= Optional.of("world");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    Serializer.register(TwoOptionalFields.class);
    TwoOptionalFields result = Serializer.unserialize(stream.toByteArray());
    assertTrue(result.foo.isPresent());
    assertEquals(result.foo.get(), "hello");
    assertTrue(result.bar.isPresent());
    assertEquals(result.bar.get(), "world");
  }
  
  @Type(0)
  static class TwoOptionalNestedFields {
    Optional<TypeWithNesting> foo;
    Optional<TypeWithPrimitives> bar;
  }
  
  public void testTwoOptionalNestedFields_bothAbsent() throws Exception {
    TwoOptionalNestedFields data = new TwoOptionalNestedFields();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    Serializer.register(TwoOptionalNestedFields.class);
    TwoOptionalNestedFields result = Serializer.unserialize(
        stream.toByteArray());
    assertFalse(result.foo.isPresent());
    assertFalse(result.bar.isPresent());
  }

  public void testTwoOptionalNestedFields_firstPresent() throws Exception {
    TwoOptionalNestedFields data = new TwoOptionalNestedFields();
    TypeWithNesting nested = new TypeWithNesting();
    TypeBeingNested leaf = new TypeBeingNested();
    data.foo = Optional.of(nested);
    nested.foo = leaf;
    leaf.hello = "hello world";
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    Serializer.register(TwoOptionalNestedFields.class);
    TwoOptionalNestedFields result = Serializer.unserialize(
        stream.toByteArray());
    assertTrue(result.foo.isPresent());
    assertEquals("hello world", result.foo.get().foo.hello);
  }

  @Type(1)
  static class SimplestType {
    @Field(2)
    String foo;
  }
  
  public void testTypeWithContainer() throws Exception {
    SimplestType data = new SimplestType();
    data.foo = "bar";
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    Serializer.register(SimplestType.class);
    SimplestType result = Serializer.unserialize(stream.toByteArray());
    assertEquals("bar", result.foo);
  }
  
  public void testSerializeLargeDataPacket() throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    
    Data data = new Data();
    data.getName().setName("http://example.com/bigfile");
    data.setContent(new byte[100 * 1000 * 1000]);
    Serializer.serialize(stream, data);
    
    ByteArrayInputStream incoming = new ByteArrayInputStream(stream.toByteArray());
    Data result = Serializer.unserialize(incoming);
    assertEquals(100 * 1000 * 1000, result.getContent().length);
    
    assertEquals(0, incoming.available());
  }
}