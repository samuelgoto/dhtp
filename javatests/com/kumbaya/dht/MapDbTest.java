package com.kumbaya.dht;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;
import java.util.Random;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.google.common.collect.ImmutableSet;

public class MapDbTest {

	@Test
	public void testMapDb_helloWorld() throws Exception {
		DB db = db(createFile());

		Map<Integer,String> map = db.getTreeMap(
				"default");

		map.put(1, "one");
		map.put(2, "two");

		// persists changes to disk.
	    db.commit();

	    map.put(3, "three");

	    assertEquals("one", map.get(1));
	    assertEquals("two", map.get(2));
	    assertEquals("three", map.get(3));
	    
	    db.close();
	}

	private String createFile() {
		int id = new Random().nextInt();
		return "/tmp/test-" + id + ".db";
	}
	
	@Test
	public void testMapDb_survivesRestarts() throws Exception {
		String file = createFile();
		DB db = db(file);
		
		Map<Integer,String> map = db.getTreeMap("default");

		map.put(1, "one");
	    db.commit();
	    db.close();

		DB db2 = db(file);
		assertEquals("one", db2.getTreeMap("default").get(1));
	}

	@Test
	public void testMapDb_mustCommit() throws Exception {
		String file = createFile();
		DB db = db(file);
		
		Map<Integer,String> map = db.getTreeMap("default");

		map.put(1, "one");
	    db.close();

		DB db2 = db(file);
		assertFalse(db2.getTreeMap("default").containsKey(1));
	}
	
	@Test
	public void testMapDb_doesNotNecessarilyNeedToClose() throws Exception {
		String file = createFile();
		DB db = db(file);
		
		Map<Integer,String> map = db.getTreeMap("default");

		map.put(1, "one");
		db.commit();

		DB db2 = db(file);

		assertEquals("one", db2.getTreeMap("default").get(1));
	}

	@Test
	public void testMapDb_canCallKeys() throws Exception {
		String file = createFile();
		DB db = db(file);
		
		Map<Integer,String> map = db.getTreeMap("default");

		map.put(1, "one");
		map.put(2, "two");
		map.put(3, "three");
		db.commit();
		map.put(4, "four");

		assertEquals(ImmutableSet.of(1, 2, 3, 4), map.keySet());
		assertEquals(4, map.size());
	}


	@Test
	public void testMapDb_canCallValues() throws Exception {
		String file = createFile();
		DB db = db(file);
		
		Map<Integer,String> map = db.getTreeMap("default");

		map.put(1, "one");
		map.put(2, "two");
		map.put(3, "three");
		db.commit();
		map.put(4, "four");

		assertTrue(CollectionUtils.isEqualCollection(
				ImmutableSet.of("one", "two", "three", "four"), 
				map.values()));
		
		assertEquals(4, map.size());
	}

	@Test
	public void testMapDb_canRemove() throws Exception {
		String file = createFile();
		DB db = db(file);
		
		Map<Integer,String> map = db.getTreeMap("default");

		map.put(1, "one");
		db.commit();
		map.remove(1);
		db.commit();

		assertEquals(0, map.size());
	}

	private static DB db(String name) {
		return DBMaker.newFileDB(new File(name))
				.closeOnJvmShutdown()
				.make();
	}
	
	public static void main(String args[]) throws Exception {
		System.out.println("Testing how MapDb works under JVM restarts");
		
		DB db = db("/tmp/bootstrap.db");
		Map<Integer, String> map = db.getTreeMap("default");
		map.put(new Random().nextInt(), "hello world");
		System.out.println(map);
		db.commit();
	}
}
