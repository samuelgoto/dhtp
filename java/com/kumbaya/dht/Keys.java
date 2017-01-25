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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueType;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Keys {
  private static final Log logger = LogFactory.getLog(Keys.class);

  public static KUID of(String key) {            
    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");
      KUID result = KUID.createWithBytes(md.digest(key.getBytes("UTF-8")));
      md.reset();
      return result;
    } catch (UnsupportedEncodingException e) {
      logger.warn("failed to encode key: " + key, e);
      return null;
    } catch (NoSuchAlgorithmException e) {
      logger.warn("failed to encode key: " + key, e);
      return null;
    }
  }

  public static EntityKey as(KUID key) {
    return as(key, DHTValueType.TEXT);
  }

  public static EntityKey as(KUID key, DHTValueType type) {
    return EntityKey.createEntityKey(key, type);
  }
}
