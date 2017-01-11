package com.kumbaya.common;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


public class Flags {
  private final Map<String, Object> defaultValues = Maps.newHashMap();
  private final CommandLine line;

  private Flags(Set<Flag<?>> flags, String[] args) throws ParseException {
    Options options = new Options();

    for (Flag<?> flag : flags) {
      options.addOption(flag.name, flag.hasArg, flag.description);
      defaultValues.put(flag.name, flag.defaultValue);
    }

    CommandLineParser parser = new PosixParser();
    this.line = parser.parse(options, args);
  }
  
  public static Flags parse(Set<Flag<?>> flags, String[] args) throws ParseException {
    return new Flags(flags, args);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String name) {
    Optional<String> value = Optional.absent();
    Object defaultValue = defaultValues.get(name);
    
    if (line.hasOption(name)) {
      value = Optional.of(line.getOptionValue(name));
    }

    if (defaultValue instanceof Integer) {
      Integer result = value.isPresent() ? Integer.parseInt(value.get()) : (Integer) defaultValue;
      return (T) result;
    } else if (defaultValue instanceof String) {
      String result = value.isPresent() ? value.get() : (String) defaultValue;
      return (T) result;
    } else {
      throw new UnsupportedOperationException("Unknown flag type " + name);
    }
  }

  public static class Flag<K> {
    private final String name;
    private final String description;
    private final boolean hasArg;
    private final K defaultValue;

    private Flag(String name, String description, boolean hasArg, K defaultValue) {
      this.name = name;
      this.description = description;
      this.hasArg = hasArg;
      this.defaultValue = defaultValue;
    }

    public static <K> Flag<K> of (String name, String description, boolean hasArg, K defaultValue) {
      return new Flag<K>(name, description, hasArg, defaultValue);
    }
  }
}
