package com.kumbaya.dht;

import java.io.File;
import java.util.Map;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.impl.BaseDatabase;
import org.limewire.mojito.db.impl.DHTValueEntityBag;
import org.mapdb.DB;
import org.mapdb.DBMaker;


import com.google.inject.Inject;

@SuppressWarnings("serial")
class LocalDatabase extends BaseDatabase {
	private final DB db;
	
	@Inject
	LocalDatabase(DB db) {
		this.db = db;
	}
	
	@Override
	public boolean store(DHTValueEntity entity) {
		boolean result = super.store(entity);
		if (result) {
			db.commit();
		}
		
		System.out.println(database());
		
		return result;
	}

	@Override
	public DHTValueEntity remove(KUID primaryKey, KUID secondaryKey) {
		DHTValueEntity result = super.remove(primaryKey, secondaryKey);
		db.commit();
		return result;
	}

	@Override
	protected Map<KUID, DHTValueEntityBag> database() {
		return db.getTreeMap("default");
	}
}
