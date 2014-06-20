/**
 * Copyright 2010, 2011 Google Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.kumbaya.dht.Dht;
import com.kumbaya.dht.Keys;

import org.junit.Before;
import org.junit.Test;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.ContactUtils;

import java.net.InetSocketAddress;

public class DhtTest {

  @Before
  public void setUp() {
    NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
    NetworkSettings.FILTER_CLASS_C.setValue(false);
    ContactUtils.setNetworkInstanceUtils(new SimpleNetworkInstanceUtils(false));
  }

  @Test
  public void testDHT() throws Exception {
    MojitoDHT dht = MojitoFactory.createDHT("bootstrap");
    dht.bind(new InetSocketAddress("localhost", 8080));
    dht.start();

    MojitoDHT node = MojitoFactory.createDHT("node");
    node.bind(new InetSocketAddress("localhost", 8081));
    node.start();
    node.bootstrap(new InetSocketAddress("localhost", 8080)).get();
    assertTrue(node.isBootstrapped());

    DHTValueImpl value = new DHTValueImpl(
        DHTValueType.TEXT, Version.ZERO, "hello world".getBytes());

    node.put(Keys.of("key"), value).get();

    FindValueResult result = node.get(EntityKey.createEntityKey(
        Keys.of("key"), DHTValueType.TEXT)).get();

    assertTrue(result.isSuccess());
    assertEquals(1, result.getEntities().size());
    assertEquals("hello world",
        new String(result.getEntities().iterator().next().getValue().getValue()));

    node.close();
    dht.close();
  }

  @Test
  public void testStorables() throws Exception {
    Context dht = (Context) MojitoFactory.createDHT("bootstrap");
    dht.getStorableModelManager().addStorableModel(
        DHTValueType.TEXT, new Dht.Model());
    dht.bind(new InetSocketAddress("localhost", 8080));
    dht.start();

    Context node = (Context) MojitoFactory.createDHT("node");
    node.getStorableModelManager().addStorableModel(
        DHTValueType.TEXT, new Dht.Model());
    node.bind(new InetSocketAddress("localhost", 8081));
    node.start();
    node.bootstrap(new InetSocketAddress("localhost", 8080)).get();
    assertTrue(node.isBootstrapped());

    DHTValueImpl value = new DHTValueImpl(
        DHTValueType.TEXT, Version.ZERO, "hello world".getBytes());

    node.put(Keys.of("key"), value).get();

    FindValueResult result = node.get(EntityKey.createEntityKey(
        Keys.of("key"), DHTValueType.TEXT)).get();

    assertTrue(result.isSuccess());
    assertEquals(1, result.getEntities().size());
    assertEquals("hello world",
        new String(result.getEntities().iterator().next().getValue().getValue()));

    node.close();
    dht.close();
  }

  @Test
  public void testSettingTheBootstrapNodeAsBootStrapped() throws Exception {
    Context dht = (Context) MojitoFactory.createDHT("bootstrap");
    dht.bind(new InetSocketAddress("localhost", 8080));
    dht.setBootstrapped(true);
    dht.start();

    MojitoDHT node = MojitoFactory.createDHT("node");
    node.bind(new InetSocketAddress("localhost", 8081));
    node.start();
    node.bootstrap(new InetSocketAddress("localhost", 8080)).get();
    assertTrue(node.isBootstrapped());

    DHTValueImpl value = new DHTValueImpl(
        DHTValueType.TEXT, Version.ZERO, "hello world".getBytes());

    node.put(Keys.of("key"), value).get();

    FindValueResult result = dht.get(EntityKey.createEntityKey(
        Keys.of("key"), DHTValueType.TEXT)).get();

    assertTrue(result.isSuccess());
    assertEquals(1, result.getEntities().size());
    assertEquals("hello world",
        new String(result.getEntities().iterator().next().getValue().getValue()));

    node.close();
    dht.close();
  }

  @Test
  public void testCreatorAddressIsCorrect() throws Exception {
    Context root = (Context) MojitoFactory.createDHT("bootstrap");
    root.bind(new InetSocketAddress("localhost", 8081));
    root.start();

    Context dht = (Context) MojitoFactory.createDHT("dht");
    dht.bind(new InetSocketAddress("localhost", 8080));
    dht.start();
    dht.bootstrap(new InetSocketAddress("localhost", 8081)).get();
    assertTrue(dht.isBootstrapped());

    DHTValueImpl value = new DHTValueImpl(
        DHTValueType.TEXT, Version.ZERO, "hello world".getBytes());

    StoreResult store = dht.put(Keys.of("key"), value).get();

    assertEquals(2, store.getLocations().size());

    FindValueResult result = dht.get(EntityKey.createEntityKey(
        Keys.of("key"), DHTValueType.TEXT)).get();

    assertTrue(result.isSuccess());
    assertEquals(1, result.getEntities().size());
    DHTValueEntity entity = result.getEntities().iterator().next();
    assertEquals(new InetSocketAddress("localhost", 8080),
        entity.getCreator().getContactAddress());
    assertEquals(new InetSocketAddress("localhost", 8081),
        entity.getSender().getContactAddress());

    dht.close();
  }
}
