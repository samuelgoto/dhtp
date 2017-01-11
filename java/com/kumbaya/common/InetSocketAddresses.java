package com.kumbaya.common;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;

public class InetSocketAddresses {
  public static InetSocketAddress parse(String address) {
    String[] parts = address.split(":");
    Preconditions.checkArgument(parts.length == 2, "Invalid format, expected host:port got " + address);
    return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
  }
}
