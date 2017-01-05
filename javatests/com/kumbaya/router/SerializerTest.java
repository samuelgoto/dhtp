package com.kumbaya.router;

import com.google.common.base.Preconditions;
import com.kumbaya.router.Marshall.TLV;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

public class SerializerTest extends TestCase {
  static class Foo {
    @Field(1)
    String hello;
    @Field(2)
    String world;
    @Field(3)
    int foo;
    @Field(4)
    long bar;
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
      
      if (property.getType().equals(String.class)) {
        String value = (String) property.get(object);
        Preconditions.checkNotNull(value, "Can't serialize null fields: " + property.getName());
        values.add(TLV.of(field.value(), value.getBytes()));        
      } else if (property.getType().equals(int.class)) {
        Integer value = (Integer) property.get(object);
        values.add(TLV.of(field.value(), ByteBuffer.allocate(4).putInt(value).array()));
      } else if (property.getType().equals(long.class)) {
        Long value = (Long) property.get(object);
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
      
      for (TLV value : values) {
        if (value.type == field.value()) {
          if (property.getType().equals(String.class)) {
            property.set(result, new String(value.content));
          } else if (property.getType().equals(int.class)) {
            property.set(result, ByteBuffer.wrap(value.content).getInt());
          } else if (property.getType().equals(long.class)) {
            property.set(result, ByteBuffer.wrap(value.content).getLong());
          } else {
            throw new UnsupportedOperationException(
                "Unsupported field type: " + property.getType());
          }
          
        }
      }
    }
    
    return result;
  }
  
  public void testReflection() throws IllegalArgumentException, IllegalAccessException, IOException, InstantiationException {
    Foo foo = new Foo();
    foo.hello = "hello";
    foo.world = "world";
    byte[] serialized = serialize(foo);
    Foo bar = unserialize(Foo.class, new ByteArrayInputStream(serialized));
    assertEquals(foo.hello, bar.hello);
    assertEquals(foo.world, bar.world);
    assertEquals(foo.foo, bar.foo);
    assertEquals(foo.bar, bar.bar);
  }
}

