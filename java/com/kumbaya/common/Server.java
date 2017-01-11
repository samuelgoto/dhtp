package com.kumbaya.common;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface Server {
	public void bind(InetSocketAddress address) throws IOException;
	void close() throws IOException;
}
