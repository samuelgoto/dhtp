package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.kumbaya.router.Marshaller.TLV;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.kumbaya.router.TypeLengthValues.encode;
import static com.kumbaya.router.TypeLengthValues.decode;

class Serializer {
  private static final Map<Long, Class<?>> registry = new HashMap<Long, Class<?>>();
  
  static void register(Class<?> clazz) {
    Type annotation = clazz.getAnnotation(Type.class);
    Preconditions.checkNotNull(annotation, "Object being registered isn't annotated with @Type: " + clazz);
    long container = annotation.value();
    registry.put(container, clazz);
  }
  
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Field {
    long value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface Type {
    long value();
  }

  static <T> void serialize(OutputStream stream, T object) throws IOException {
    Type annotation = object.getClass().getAnnotation(Type.class);
    Preconditions.checkNotNull(annotation, "Object being serialized isn't annotated with @Type: " + object.getClass());
    long container = annotation.value();

    ByteArrayOutputStream content = new ByteArrayOutputStream();

    for (java.lang.reflect.Field property : object.getClass().getDeclaredFields()) {
      Field field = property.getAnnotation(Field.class);
      boolean optional = property.getType().equals(Optional.class);
      final Class<?> type = optional ? (Class<?>) ((ParameterizedType) (property.getGenericType())).getActualTypeArguments()[0] :
        property.getType();          

      if (field == null) {
        if (type.getAnnotation(Type.class) == null) {
          throw new UnsupportedOperationException(
              "Can't serialize a class with fields that are not annotated with @Field" +
              "or that are not of a class that is annotated with @Type: " + property);
        }
      } else if (type.getAnnotation(Type.class) != null) {
        throw new UnsupportedOperationException("You can't set a @Field annotation in a property " +
            "that has a type with a @Type annotation");        
      }

      // TODO(goto): we should probably assert somewhere that type ids are unique in the
      // object.
      // If this is an optional field and it is not present, just fully skip it.
      final Object p;
      try {
        p = property.get(object);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException("Can't serialize object", e);
      }
      
      if (optional) {
        // Optional properties are allowed to have nulls: it will be assumed to be absent.
        if (p == null || !((Optional<?>) p).isPresent()) {
          continue;
        }
      }

      Object value = (optional ? ((Optional<?>) p).get() : p);
      if (type == String.class) {
        Preconditions.checkNotNull(value, "Can't serialize null fields: " + property.getName());
        Marshaller.marshall(content, TLV.of(field.value(), ((String) value).getBytes()));
      } else if (type == boolean.class) {
        byte[] bool = ((boolean) value) ? new byte[] {(byte) 0x1} : new byte[] {};
        Marshaller.marshall(content, TLV.of(field.value(), bool));
      } else if (type == int.class) {
        Marshaller.marshall(content, TLV.of(field.value(), ByteBuffer.allocate(4).putInt((Integer) value).array()));
      } else if (type == long.class) {
        Marshaller.marshall(content, TLV.of(field.value(), ByteBuffer.allocate(8).putLong((Long) value).array()));
      } else if (type.isArray() & type.getComponentType() == byte.class) {
        Marshaller.marshall(content, TLV.of(field.value(), (byte[]) value));
      } else {
        // NOTE(goto): perhaps there is a way to avoid the extra creation of the byte buffer here.
        // ByteArrayOutputStream nested = new ByteArrayOutputStream();
        serialize(content, value);
      }
    }

    Marshaller.marshall(stream, TLV.of(container, content.toByteArray()));
  }
  
  static <T> T unserialize(InputStream stream)
      throws IllegalArgumentException, IllegalAccessException, InstantiationException, IOException {
    return unserialize(null, stream);
  }

  static <T> T unserialize(byte[] content)
      throws IllegalArgumentException, IllegalAccessException, InstantiationException, IOException {
    return unserialize(null, new ByteArrayInputStream(content));
  }

  @SuppressWarnings("unchecked")
  private static <T> T unserialize(Class<T> clazz, InputStream stream) 
      throws IllegalArgumentException, IllegalAccessException, InstantiationException, IOException {
    
    TLV data = Marshaller.unmarshall(stream);

    if (clazz == null) {      
      clazz = (Class<T>) registry.get(data.type);
      
      Preconditions.checkNotNull(clazz, "The packet type must be registered previously: " + data.type);
    } else {
      Type annotation = clazz.getAnnotation(Type.class);
      Preconditions.checkNotNull(annotation, "Object being unserialized isn't annotated with @Type: " + clazz);
      long container = annotation.value();
      Preconditions.checkArgument(data.type == container, 
          "Serialized data incompatible with the class type, got: " + data.type + ", expecting: " + container);
    }

    T result = clazz.newInstance();

    ByteArrayInputStream body = new ByteArrayInputStream(data.content);

    for (java.lang.reflect.Field property : result.getClass().getDeclaredFields()) {
      boolean optional = property.getType().equals(Optional.class);
      if (optional) {
        // Sets by default optional fields as absents.
        property.set(result, Optional.absent());
      }
    }

    while (body.available() != 0) {
      long id = decode(body);
      int length = (int) decode(body);
      ByteBuffer buffer = ByteBuffer.allocate(length);
      body.read(buffer.array(), 0, length);
      byte[] content = buffer.array();

      for (java.lang.reflect.Field property : result.getClass().getDeclaredFields()) {
        long match;

        Field field = property.getAnnotation(Field.class);

        boolean optional = property.getType().equals(Optional.class);

        Class<?> type = optional ? (Class<?>) ((ParameterizedType) (property.getGenericType())).getActualTypeArguments()[0] :
          property.getType();          


        if (field != null) {
          match = field.value();
          Preconditions.checkArgument(property.getType().getAnnotation(Type.class) == null,
              "You can't have @Field annotations in non-primitive types");          
        } else if (type != null) {
          match = type.getAnnotation(Type.class).value();
        } else {
          throw new UnsupportedOperationException(
              "Can't unserialize a class with fields that are not annotated with @Field or" + 
                  "of a type of a Class that is annotated with @Type" + property);
        }

        if (match != id) {
          continue;
        }

        if (type == String.class) {
          String value = new String(content);
          property.set(result, !optional ? value : Optional.of(value));
        } else if (type == boolean.class) {
          boolean value = content.length > 0;
          property.set(result, !optional ? value : Optional.of(value));
        } else if (type == int.class) {
          int value = ByteBuffer.wrap(content).getInt();
          property.set(result, !optional ? value : Optional.of(value));
        } else if (type == long.class) {
          long value = ByteBuffer.wrap(content).getLong();
          property.set(result, !optional ? value : Optional.of(value));
        } else if (type.isArray() & type.getComponentType() == byte.class) {
          byte[] value = content;
          property.set(result, !optional ? value : Optional.of(value));
        } else {
          // This is probably highly inefficient, but we reconstruct the original frame around
          // the content to enable the recursion to be agnostic of nested fields.
          byte[] header1 = encode(id);
          byte[] header2 = encode(length);
          ByteBuffer buf = ByteBuffer.allocate(header1.length + header2.length + content.length);
          buf.put(header1);
          buf.put(header2);
          buf.put(content);
          Object value = unserialize(type, new ByteArrayInputStream(buf.array()));
          property.set(result, !optional ? value : Optional.of(value));
        }
      }
    }


    return result;
  }
}
