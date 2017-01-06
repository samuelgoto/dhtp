package com.kumbaya.router;

import com.google.common.base.Optional;
import com.kumbaya.router.Serializer.Field;
import com.kumbaya.router.Serializer.Type;
import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;

public class SerializerTest extends TestCase {
  static class TypeWithPrimitives {
    @Field(1)
    String hello;
    @Field(2)
    String world;
    @Field(3)
    int foo;
    @Field(4)
    long bar;
  }
  
  public void testPrimitives() throws Exception {
    TypeWithPrimitives foo = new TypeWithPrimitives();
    foo.hello = "hello";
    foo.world = "world";
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, foo);
    TypeWithPrimitives bar = Serializer.unserialize(TypeWithPrimitives.class, stream.toByteArray());
    assertEquals(foo.hello, bar.hello);
    assertEquals(foo.world, bar.world);
    assertEquals(foo.foo, bar.foo);
    assertEquals(foo.bar, bar.bar);
  }

  static class TypeWithOptional {
    @Field(1)
    Optional<String> foo;
  }
  
  public void testOptionalFields() throws Exception {
    TypeWithOptional foo = new TypeWithOptional();
    foo.foo = Optional.of("hello world");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, foo);
    TypeWithOptional bar = Serializer.unserialize(TypeWithOptional.class, stream.toByteArray());
    assertTrue(bar.foo.isPresent());
    assertEquals("hello world", bar.foo.get());
  }
  
  public void testOptionalFields_absent() throws Exception {
    TypeWithOptional foo = new TypeWithOptional();
    foo.foo = Optional.absent();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, foo);
    TypeWithOptional bar = Serializer.unserialize(TypeWithOptional.class, stream.toByteArray());
    assertFalse(bar.foo.isPresent());
  }
  
  public void testOptionalFields_emptyString() throws Exception {
    TypeWithOptional data = new TypeWithOptional();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    TypeWithOptional result = Serializer.unserialize(TypeWithOptional.class, stream.toByteArray());
    assertFalse(result.foo.isPresent());
  }
  
  static class TypeWithNesting {
    @Field(1)
    TypeBeingNested foo;
  }
  
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
    TypeWithNesting result = Serializer.unserialize(TypeWithNesting.class, stream.toByteArray());
    assertEquals("world", result.foo.hello);
  }
  
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
    TwoOptionalFields result = Serializer.unserialize(TwoOptionalFields.class, stream.toByteArray());
    assertFalse(result.foo.isPresent());
    assertFalse(result.bar.isPresent());
  }

  public void testTwoOptionalFields_firstPresent() throws Exception {
    TwoOptionalFields data = new TwoOptionalFields();
    data.foo = Optional.of("hello world");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    TwoOptionalFields result = Serializer.unserialize(TwoOptionalFields.class, stream.toByteArray());
    assertTrue(result.foo.isPresent());
    assertEquals(result.foo.get(), "hello world");
    assertFalse(result.bar.isPresent());
  }

  public void testTwoOptionalFields_secondPresent() throws Exception {
    TwoOptionalFields data = new TwoOptionalFields();
    data.bar= Optional.of("hello world");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    TwoOptionalFields result = Serializer.unserialize(TwoOptionalFields.class, stream.toByteArray());
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
    TwoOptionalFields result = Serializer.unserialize(TwoOptionalFields.class, stream.toByteArray());
    assertTrue(result.foo.isPresent());
    assertEquals(result.foo.get(), "hello");
    assertTrue(result.bar.isPresent());
    assertEquals(result.bar.get(), "world");
  }
  
  static class TwoOptionalNestedFields {
    @Field(1)
    Optional<TypeWithNesting> foo;
    @Field(2)
    Optional<TypeWithNesting> bar;
  }
  
  public void testTwoOptionalNestedFields_bothAbsent() throws Exception {
    TwoOptionalNestedFields data = new TwoOptionalNestedFields();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Serializer.serialize(stream, data);
    TwoOptionalNestedFields result = Serializer.unserialize(TwoOptionalNestedFields.class, 
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
    TwoOptionalNestedFields result = Serializer.unserialize(TwoOptionalNestedFields.class, 
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
    SimplestType result = Serializer.unserialize(
        SimplestType.class, stream.toByteArray());
    assertEquals("bar", result.foo);
  }
}