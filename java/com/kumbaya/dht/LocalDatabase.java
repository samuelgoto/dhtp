package com.kumbaya.dht;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.DatabaseSecurityConstraint;
import org.mapdb.DB;

import ca.odell.glazedlists.impl.Preconditions;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

@SuppressWarnings("serial")
class LocalDatabase implements Database {
	private final DB db;
	
	@Inject
	LocalDatabase(DB db) {
		this.db = db;
	}
	
	@Override
	public void setDatabaseSecurityConstraint(
			DatabaseSecurityConstraint securityConstraint) {
		throw new UnsupportedOperationException(
				"LocalDatabases don't support security constraints");
	}

	private Map<KUID, DHTValueEntity> map() {
		return db.getTreeMap("default");
	}
	
	@Override
	public boolean store(DHTValueEntity entity) {
		map().put(entity.getPrimaryKey(), entity);
		db.commit();
		return true;
	}

	@Override
	public DHTValueEntity remove(KUID primaryKey, KUID secondaryKey) {
		Preconditions.checkArgument(secondaryKey == null, 
				"secondary keys are not supported in local databases");

		DHTValueEntity result = map().remove(primaryKey);
		db.commit();
		return result;
	}

	@Override
	public boolean contains(KUID primaryKey, KUID secondaryKey) {
		Preconditions.checkArgument(secondaryKey == null, 
				"secondary keys are not supported in local databases");
		// TODO(goto): figure out how to deal with secondary keys in
		// mapdb.
		return map().containsKey(primaryKey);
	}

	@Override
	public Map<KUID, DHTValueEntity> get(KUID primaryKey) {
		return ImmutableMap.of(primaryKey, map().get(primaryKey));
	}

	@Override
	public float getRequestLoad(KUID primaryKey, boolean incrementLoad) {
		return 0;
	}

	@Override
	public Set<KUID> keySet() {
		return map().keySet();
	}

	@Override
	public Collection<DHTValueEntity> values() {
		return map().values();
	}

	@Override
	public int getKeyCount() {
		return map().size();
	}

	@Override
	public int getValueCount() {
		return map().size();
	}

	@Override
	public void clear() {
	}
}
