package com.kumbaya.common;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Provider;

public class Flags {

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.PARAMETER})
  @BindingAnnotation
  public @interface Flag {
    public String value();
  }

  private static class FlagImpl implements Flag {
    @SuppressWarnings("unused")
    private static final long serialVersionUID = 0;

    private final String value;

    public FlagImpl(String value) {
      this.value = value;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return Flag.class;
    }

    @Override
    public String value() {
      return value;
    }

    @Override
    public int hashCode() {
      // This is specified in java.lang.Annotation.
      return (127 * "value".hashCode()) ^ value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Flag)) {
        return false;
      }

      Flag other = (Flag) o;
      return value.equals(other.value());
    }

    @Override
    public String toString() {
      return "@" + Flag.class.getName() + "(value=" + value + ")";
    }
  }
  
  public static AbstractModule asModule(final String[] args) {
    return new AbstractModule() {
      @Override
      protected void configure() {
        for (final String arg : args) {
          String[] argv = arg.split("=");
          Preconditions.checkArgument(arg.startsWith("--"), 
              "Invalid format for argument, must start with -- " + arg);
          // Binds the value to ints.
          bind(Key.get(int.class, new FlagImpl(argv[0].substring(2)))).toProvider(new Provider<Integer>() {
            @Override
            public Integer get() {
              Preconditions.checkArgument(argv.length == 2, "Invalid flag format for " + arg);
              return Integer.parseInt(argv[1]);
            }
          });

          // Binds to Strings.
          bind(Key.get(String.class, new FlagImpl(argv[0].substring(2)))).toProvider(new Provider<String>() {
            @Override
            public String get() {
              Preconditions.checkArgument(argv.length == 2, "Invalid flag format for " + arg);
              return argv[1];
            }
          });
        }

      }
    };
  }
}
