/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kumbaya.dht;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.Storable;
import org.limewire.mojito.db.StorableModel;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.DatabaseUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Singleton
public class Dht {
	@Inject private Context dht;
	@Inject private Model model;
	@Inject private Provider<MessageDispatcherFactoryImpl> dispatcher;
	@Inject private Provider<MessageDispatcher> messageDispatcher;
	@Inject(optional = true) private LocalDatabase localDb;

	public void setId(String id) {
		dht.getLocalNode().setNodeID(Keys.of(id));
	}
	
	public StoreResult put(String key, String value) throws InterruptedException, ExecutionException {
		return put(DHTValueEntity.createFromValue(dht, Keys.of(key), Values.of(value)));
	}

	public StoreResult put(KUID key, DHTValue value) throws InterruptedException, ExecutionException {
		return put(DHTValueEntity.createFromValue(dht, key, value));
	}

	public List<DHTValueEntity> get(String key, int timeoutMs)
			throws InterruptedException, ExecutionException, TimeoutException {
		return get(Keys.as(Keys.of(key)), timeoutMs);
	}

	public List<DHTValueEntity> get(KUID key, DHTValueType keyType, int timeoutMs)
			throws InterruptedException, ExecutionException, TimeoutException {
		return get(Keys.as(key, keyType), timeoutMs);
	}
	
	public List<DHTValueEntity> get(EntityKey entityKey, int timeoutMs)
			throws InterruptedException, ExecutionException, TimeoutException {
      // TODO(goto): decrement the timeout between calls.
		List<DHTValueEntity> all = new ArrayList<DHTValueEntity>();
		FindValueResult result = dht.get(entityKey).get(
		    timeoutMs, TimeUnit.MILLISECONDS);

		for (DHTValueEntity entry : result.getEntities()) {
			all.add(entry);
		}

		for (EntityKey entry : result.getEntityKeys()) {
			FindValueResult entries = dht.get(entry).get(
			    timeoutMs, TimeUnit.MILLISECONDS);
			for (DHTValueEntity entity : entries.getEntities()) {
				all.add(entity);
			}
		}

		return all;
	}

	public void remove(KUID key) throws InterruptedException, ExecutionException {
		dht.remove(key).get();
	}

	private StoreResult put(DHTValueEntity value)
			throws InterruptedException, ExecutionException {
		model.add(value);
		StoreResult result = dht.put(value.getPrimaryKey(), value.getValue()).get();
		return result;
	}

	public Dht start(String hostname, int port) throws IOException, NumberFormatException {
		// Starts the dht when the public port is equal to the internal port.
		return start(hostname, port, port);
	}

	public Dht start(String hostname, int port, int proxy) throws IOException, NumberFormatException {
		// The following lines allows the DHT to connect to local ip addresses
		// which is sometimes useful for debugging purposes. Production binaries
		// should probably not set these flags though. Leaving them as comments
		// for convenience.
		// NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
		// NetworkSettings.FILTER_CLASS_C.setValue(false);
		ContactUtils.setNetworkInstanceUtils(new SimpleNetworkInstanceUtils(false));

		NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
		NetworkSettings.FILTER_CLASS_C.setValue(false);
		ContactUtils.setNetworkInstanceUtils(new SimpleNetworkInstanceUtils(false));

		dht.getStorableModelManager().addStorableModel(
				DHTValueType.TEXT, model);

		dht.setMessageDispatcher(dispatcher.get());
		
		if (localDb != null) {
			dht.setDatabase(localDb);
		}
		
		dht.bind(new InetSocketAddress(hostname, port));
		dht.setExternalAddress(InetSocketAddress.createUnresolved(hostname,  proxy));
		dht.start();

		return this;
	}

	public DHTFuture<BootstrapResult> bootstrap(String hostname, int port) {
		DHTFuture<BootstrapResult> result = dht.bootstrap(
				InetSocketAddress.createUnresolved(hostname,port));
		return result;
	}

	public boolean isBootstraped() {
		return dht.isBootstrapped();
	}

	public boolean isBound() {
		return dht.isBound();
	}

	public Dht stop() {
		dht.close();
		return this;
	}

	@Singleton
	static class Model implements StorableModel {
		private static final Log log = LogFactory.getLog(Model.class);

		private final Set<DHTValueEntity> values = Sets.newHashSet();

		public Model add(DHTValueEntity value) {
			values.add(value);
			return this;
		}

		@Override
		public Collection<Storable> getStorables() {
			Set<Storable> result = Sets.newHashSet();

			for (DHTValueEntity value : values) {
				Storable storable = new Storable(value.getPrimaryKey(), value.getValue());
				if (DatabaseUtils.isPublishingRequired(storable)) {
					result.add(storable);
				}
			}

			return result;
		}

		@Override
		public void handleStoreResult(Storable value, StoreResult result) {
			log.info("A value was re-published");
		}

		@Override
		public void handleContactChange() {
		}
	}
}
