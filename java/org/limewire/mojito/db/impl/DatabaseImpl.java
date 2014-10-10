/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.limewire.mojito.db.impl;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntHashMap;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.DatabaseSecurityConstraint;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.util.ContactUtils;


/*
 * Multiple values per key and one value per nodeId under a certain key
 * 
 * valueId
 *   nodeId
 *     value
 *   nodeId
 *     value
 * valueId
 *   nodeId
 *     value
 *   nodeId
 *     value
 *   nodeId
 *     value
 */

/**
 * Adds, removes and stores a {@link DHTValueEntity} to a 
 * database. Values are stored in-memory. 
 */ 
 /* TODO: For more advanced features we need some definition for
 * DHTValues (non-signed values cannot replace signed values and
 * what not).
 */
public class DatabaseImpl extends BaseDatabase {
    
    /** LOCKING: this */
    private final Map<KUID, DHTValueEntityBag> database = new HashMap<KUID, DHTValueEntityBag>();
    
    @Override
    protected Map<KUID, DHTValueEntityBag> database() {
    	return database;
    }
}
