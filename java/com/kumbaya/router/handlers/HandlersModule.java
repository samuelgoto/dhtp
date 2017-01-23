package com.kumbaya.router.handlers;

import com.kumbaya.router.TcpServer;
import com.kumbaya.router.Packets.Interest;

public class HandlersModule extends TcpServer.HandlerModule {
  @Override
  protected void register() {
    addHandler(Interest.class, InterestHandler.class);
  }
}