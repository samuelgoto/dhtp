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

import static com.kumbaya.router.TypeLengthValues.encode;
import static com.kumbaya.router.TypeLengthValues.decode;

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
    Type annotation = object.getClass().getAnnotation(Type.class);
    Preconditions.checkNotNull(annotation, "Object being serialized isn't annotated with @Type: " + object.getClass());
    int container = annotation.value();

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
      if (optional) {
        // Optional properties are allowed to have nulls: it will be assumed to be absent.
        if (property.get(object) == null ||
            !((Optional<?>) property.get(object)).isPresent()) {
          continue;
        }
      }


      Object value = (optional ? ((Optional<?>) property.get(object)).get() : property.get(object));
      if (type.equals(String.class)) {
        Preconditions.checkNotNull(value, "Can't serialize null fields: " + property.getName());
        Marshaller.marshall(content, TLV.of(field.value(), ((String) value).getBytes()));
      } else if (type.equals(boolean.class)) {
        byte[] bool = ((boolean) value) ? new byte[] {(byte) 0x1} : new byte[] {};
        Marshaller.marshall(content, TLV.of(field.value(), bool));
      } else if (type.equals(int.class)) {
        Marshaller.marshall(content, TLV.of(field.value(), ByteBuffer.allocate(4).putInt((Integer) value).array()));
      } else if (type.equals(long.class)) {
        Marshaller.marshall(content, TLV.of(field.value(), ByteBuffer.allocate(8).putLong((Long) value).array()));
      } else {
        // NOTE(goto): perhaps there is a way to avoid the extra creation of the byte buffer here.
        // ByteArrayOutputStream nested = new ByteArrayOutputStream();
        serialize(content, value);
      }
    }

    Marshaller.marshall(stream, TLV.of(container, content.toByteArray()));
  }

  static <T> T unserialize(Class<T> clazz, byte[] content) 
      throws IllegalArgumentException, IllegalAccessException, InstantiationException {
    return unserialize(clazz, new ByteArrayInputStream(content));
  }


  static <T> T unserialize(Class<T> clazz, ByteArrayInputStream stream) 
      throws IllegalArgumentException, IllegalAccessException, InstantiationException {

    Type annotation = clazz.getAnnotation(Type.class);
    Preconditions.checkNotNull(annotation, "Object being unserialized isn't annotated with @Type: " + clazz);
    int container = annotation.value();

    TLV data = Marshaller.unmarshall(stream);

    Preconditions.checkArgument(data.type == container, 
        "Serialized data incompatible with the class type, got: " + data.type + ", expecting: " + container);

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
        int match;

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
