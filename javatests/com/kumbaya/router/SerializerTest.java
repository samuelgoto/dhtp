package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.kumbaya.router.Marshaller.TLV;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

public class SerializerTest extends TestCase {
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Field {
    int value();
  }
  
  private byte[] serialize(Object object) throws IllegalArgumentException, IllegalAccessException, IOException {
    List<TLV> values = new ArrayList<TLV>();
    
    for (java.lang.reflect.Field property : object.getClass().getDeclaredFields()) {
      Field field = property.getAnnotation(Field.class);
      
      if (field == null) {
        throw new UnsupportedOperationException(
            "Can't serialize a class with fields that are not annotated with @Field");
      }
      
      // TODO(goto): we should probably assert somewhere that type ids are unique in the
      // object.
      boolean optional = property.getType().equals(Optional.class);
      // If this is an optional field and it is not present, just fully skip it.
      if (optional) {
        // Optional properties are allowed to have nulls: it will be assumed to be absent.
        if (property.get(object) == null ||
            !((Optional<?>) property.get(object)).isPresent()) {
          continue;
        }
      }
      
      final Class<?> type = optional ? 
          (Class<?>) ((ParameterizedType) (property.getGenericType())).getActualTypeArguments()[0] :
            property.getType();          

      Object value = (optional ? ((Optional<?>) property.get(object)).get() : property.get(object));
      if (type.equals(String.class)) {
        Preconditions.checkNotNull(value, "Can't serialize null fields: " + property.getName());
        values.add(TLV.of(field.value(), ((String) value).getBytes()));        
      } else if (type.equals(int.class)) {
        values.add(TLV.of(field.value(), ByteBuffer.allocate(4).putInt((Integer) value).array()));
      } else if (type.equals(long.class)) {
        values.add(TLV.of(field.value(), ByteBuffer.allocate(8).putLong((Long) value).array()));
      } else {
        values.add(TLV.of(field.value(), serialize(value)));
      }
    }
    
    return Marshaller.marshall(values);
  }
  
  private <T> T unserialize(Class<T> clazz, byte[] content) 
      throws IllegalArgumentException, IllegalAccessException, InstantiationException {
    return unserialize(clazz, new ByteArrayInputStream(content));
  }
  
  
  private <T> T unserialize(Class<T> clazz, ByteArrayInputStream stream) 
      throws IllegalArgumentException, IllegalAccessException, InstantiationException {
    List<TLV> values = Marshaller.unmarshall(stream);
    
    T result = clazz.newInstance();
    
    for (java.lang.reflect.Field property : result.getClass().getDeclaredFields()) {
      Field field = property.getAnnotation(Field.class);
      
      if (field == null) {
        throw new UnsupportedOperationException(
            "Can't serialize a class with fields that are not annotated with @Field");
      }

      boolean optional = (property.getType() == Optional.class);
      boolean found = false;
      
      for (TLV value : values) {
        if (value.type == field.value()) {
          final Class<?> type = optional ? 
              (Class<?>) ((ParameterizedType) (property.getGenericType())).getActualTypeArguments()[0] :
                property.getType();
              
          if (type == String.class) {
            String content = new String(value.content);
            property.set(result, !optional ? content : Optional.of(content));
          } else if (type == int.class) {
            int content = ByteBuffer.wrap(value.content).getInt();
            property.set(result, !optional ? content : Optional.of(content));
          } else if (type == long.class) {
            long content = ByteBuffer.wrap(value.content).getLong();
            property.set(result, !optional ? content : Optional.of(content));
          } else {
            Object content = unserialize(type, new ByteArrayInputStream(value.content));
            property.set(result, !optional ? content : Optional.of(content));
          }
          
          found = true;
          
          break;
        }
      }
      
      if (!found) {
        if (optional) {
          property.set(result, Optional.absent());
        } else {
          throw new UnsupportedOperationException("Non-optional field without a value: " + property);
        }
      }
    }
    
    return result;
  }  
  
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
    byte[] serialized = serialize(foo);
    TypeWithPrimitives bar = unserialize(TypeWithPrimitives.class, serialized);
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
    byte[] serialized = serialize(foo);
    TypeWithOptional bar = unserialize(TypeWithOptional.class, serialized);
    assertTrue(bar.foo.isPresent());
    assertEquals("hello world", bar.foo.get());
  }
  
  public void testOptionalFields_absent() throws Exception {
    TypeWithOptional foo = new TypeWithOptional();
    foo.foo = Optional.absent();
    byte[] serialized = serialize(foo);
    TypeWithOptional bar = unserialize(TypeWithOptional.class, serialized);
    assertFalse(bar.foo.isPresent());
  }
  
  public void testOptionalFields_emptyString() throws Exception {
    TypeWithOptional data = new TypeWithOptional();
    TypeWithOptional result = unserialize(TypeWithOptional.class, serialize(data));
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
    byte[] serialized = serialize(foo);
    TypeWithNesting result = unserialize(TypeWithNesting.class, serialized);
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
    TwoOptionalFields result = unserialize(TwoOptionalFields.class, serialize(data));
    assertFalse(result.foo.isPresent());
    assertFalse(result.bar.isPresent());
  }

  public void testTwoOptionalFields_firstPresent() throws Exception {
    TwoOptionalFields data = new TwoOptionalFields();
    data.foo = Optional.of("hello world");
    TwoOptionalFields result = unserialize(TwoOptionalFields.class, serialize(data));
    assertTrue(result.foo.isPresent());
    assertEquals(result.foo.get(), "hello world");
    assertFalse(result.bar.isPresent());
  }

  public void testTwoOptionalFields_secondPresent() throws Exception {
    TwoOptionalFields data = new TwoOptionalFields();
    data.bar= Optional.of("hello world");
    TwoOptionalFields result = unserialize(TwoOptionalFields.class, serialize(data));
    assertFalse(result.foo.isPresent());
    assertTrue(result.bar.isPresent());
    assertEquals(result.bar.get(), "hello world");
  }

  public void testTwoOptionalFields_bothPresent() throws Exception {
    TwoOptionalFields data = new TwoOptionalFields();
    data.foo = Optional.of("hello");
    data.bar= Optional.of("world");
    TwoOptionalFields result = unserialize(TwoOptionalFields.class, serialize(data));
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
    TwoOptionalNestedFields result = unserialize(TwoOptionalNestedFields.class, serialize(data));
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
    TwoOptionalNestedFields result = unserialize(TwoOptionalNestedFields.class, serialize(data));
    assertTrue(result.foo.isPresent());
    assertEquals("hello world", result.foo.get().foo.hello);
  }  
}


