package com.kumbaya.dht;

import java.io.IOException;
import java.net.SocketAddress;

public interface Server {
	public void bind(SocketAddress address) throws IOException;
	void close();
}
