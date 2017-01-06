package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.kumbaya.router.Marshall.TLV;
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
  
  static class TypeWithOptional {
    @Field(1)
    Optional<String> foo;
  }
  
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
      if (optional && !((Optional<?>) property.get(object)).isPresent()) {
        continue;
      }
      
      final Class<?> type = optional ? 
          (Class<?>) ((ParameterizedType) (property.getGenericType())).getActualTypeArguments()[0] :
            property.getType();
          

      Object raw = property.get(object);
      if (type.equals(String.class)) {
        String value = (String) (optional ? ((Optional<?>) raw).get() : raw);
        Preconditions.checkNotNull(value, "Can't serialize null fields: " + property.getName());
        values.add(TLV.of(field.value(), value.getBytes()));        
      } else if (type.equals(int.class)) {
        Integer value = (Integer) (optional ? ((Optional<?>) raw).get() : raw);
        values.add(TLV.of(field.value(), ByteBuffer.allocate(4).putInt(value).array()));
      } else if (type.equals(long.class)) {
        Long value = (Long) (optional ? ((Optional<?>) raw).get() : raw);
        values.add(TLV.of(field.value(), ByteBuffer.allocate(8).putLong(value).array()));
      } else {
        throw new UnsupportedOperationException(
            "Unsupported field type: " + property.getType());
      }
    }
    
    return Marshall.marshall(values);
  }
  
  private <T> T unserialize(Class<T> clazz, ByteArrayInputStream stream) 
      throws IllegalArgumentException, IllegalAccessException, InstantiationException {
    List<TLV> values = Marshall.unmarshall(stream);
    
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
            throw new UnsupportedOperationException(
                "Unsupported field type: " + property.getType());
          }
          
          found = true;
          
          break;
        }
      }
      
      if (!found) {
        if (optional) {
          property.set(result, Optional.absent());
        } else {
          throw new UnsupportedOperationException("Non-optional field without a value");
        }
      }
    }
    
    return result;
  }
  
  public void testReflection() throws Exception {
    TypeWithPrimitives foo = new TypeWithPrimitives();
    foo.hello = "hello";
    foo.world = "world";
    byte[] serialized = serialize(foo);
    TypeWithPrimitives bar = unserialize(TypeWithPrimitives.class, new ByteArrayInputStream(serialized));
    assertEquals(foo.hello, bar.hello);
    assertEquals(foo.world, bar.world);
    assertEquals(foo.foo, bar.foo);
    assertEquals(foo.bar, bar.bar);
  }
  
  public void testOptionalFields() throws Exception {
    TypeWithOptional foo = new TypeWithOptional();
    foo.foo = Optional.of("hello world");
    byte[] serialized = serialize(foo);
    TypeWithOptional bar = unserialize(TypeWithOptional.class, new ByteArrayInputStream(serialized));
    assertTrue(bar.foo.isPresent());
    assertEquals("hello world", bar.foo.get());
  }
  
  public void testOptionalFields_absent() throws Exception {
    TypeWithOptional foo = new TypeWithOptional();
    foo.foo = Optional.absent();
    byte[] serialized = serialize(foo);
    TypeWithOptional bar = unserialize(TypeWithOptional.class, new ByteArrayInputStream(serialized));
    assertFalse(bar.foo.isPresent());
  }  
}

