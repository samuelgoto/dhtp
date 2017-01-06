package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.kumbaya.router.Marshaller.TLV;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class Serializer {
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Field {
    int value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface Type {
    int value();
  }

  static <T> void serialize(ByteArrayOutputStream stream, T object) throws IllegalArgumentException, IllegalAccessException, IOException {
    // Type annotation = object.getClass().getAnnotation(Type.class);
    // Preconditions.checkNotNull(annotation, "Object being serialized isn't annotated with @Type: " + object.getClass());
    // int container = annotation.value();
    
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
        // NOTE(goto): perhaps there is a way to avoid the extra creation of the byte buffer here.
        ByteArrayOutputStream nested = new ByteArrayOutputStream();
        serialize(nested, value);
        values.add(TLV.of(field.value(), nested.toByteArray()));
      }
    }
    
    Marshaller.marshall(stream, values);
  }
  
  static <T> T unserialize(Class<T> clazz, byte[] content) 
      throws IllegalArgumentException, IllegalAccessException, InstantiationException {
    return unserialize(clazz, new ByteArrayInputStream(content));
  }
  
  
  static <T> T unserialize(Class<T> clazz, ByteArrayInputStream stream) 
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
}
