package com.kumbaya.dht;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito.io.Tag;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;


interface Dispatcher {
	public void bind(SocketAddress address) throws IOException;
	boolean submit(Tag tag);
	void verify(SecureMessage secureMessage, SecureMessageCallback smc);
}
