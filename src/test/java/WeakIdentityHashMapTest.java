import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import builderb0y.vertigo.WeakIdentityHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class WeakIdentityHashMapTest {

	@Test
	public void testThatGCWorks() {
		if (Runtime.getRuntime().maxMemory() >= 1 << 29) {
			fail("Test environment setup incorrectly: too much memory allocated.");
		}
		WeakIdentityHashMap<Object, Object> map = new WeakIdentityHashMap<>();
		//System.gc() doesn't always work, so try to use up more memory than the JVM has available.
		//in this case set the limit to 256 MB, and try to use up 1 GB.
		for (int i = 0; i < 1024; i++) {
			map.put(new byte[1 << 20], new Object());
		}
		assertTrue(map.size() != 1024);
	}

	@Test
	public void testRetention() {
		WeakIdentityHashMap<Object, Object> map = new WeakIdentityHashMap<>();
		Object key = new Object();
		map.put(key, new Object());
		System.gc();
		assertFalse(map.isEmpty(), () -> "Map doesn't contain " + key + " after GC cycle");
		assertEquals(1, map.size(), "Wrong size???");
	}

	@Test
	public void testContainsKey() {
		WeakIdentityHashMap<Object, Object> map = new WeakIdentityHashMap<>();
		Object key = new Object();
		map.put(key, new Object());
		System.gc();
		assertTrue(map.containsKey(key), () -> "Map doesn't contain " + key + " after GC cycle");
	}

	@Test
	public void testGet1() {
		WeakIdentityHashMap<Object, Object> map = new WeakIdentityHashMap<>();
		Object key = new Object();
		Object value = new Object();
		map.put(key, value);
		System.gc();
		assertSame(value, map.get(key), () -> key + " mapped to " + value + " after GC cycle");
	}

	@Test
	public void testRemove() {
		WeakIdentityHashMap<Object, Object> map = new WeakIdentityHashMap<>();
		Object key = new Object();
		Object value = new Object();
		map.put(key, value);
		System.gc();
		assertSame(value, map.remove(key), () -> key + " mapped to " + value + " after GC cycle");
	}

	@Test
	public void testKeySetIterator() {
		WeakIdentityHashMap<Object, Object> map = new WeakIdentityHashMap<>();
		Object key = new Object();
		map.put(key, new Object());
		System.gc();
		Iterator<Object> iterator = map.keySet().iterator();
		assertSame(key, iterator.next(), () -> "Iterator didn't return " + key + " after GC cycle");
		assertFalse(iterator.hasNext(), "Iterator has another element???");
	}

	@Test
	public void testKeySetToArray() {
		WeakIdentityHashMap<Object, Object> map = new WeakIdentityHashMap<>();
		Object key = new Object();
		map.put(key, new Object());
		System.gc();
		assertArrayEquals(new Object[] { key }, map.keySet().toArray());
	}

	@Test
	public void testEntrySetIterator() {
		WeakIdentityHashMap<Object, Object> map = new WeakIdentityHashMap<>();
		Object key = new Object();
		Object value = new Object();
		map.put(key, value);
		System.gc();
		Iterator<Map.Entry<Object, Object>> iterator = map.entrySet().iterator();
		assertEquals(Map.entry(key, value), iterator.next());
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testEntrySetToArray() {
		WeakIdentityHashMap<Object, Object> map = new WeakIdentityHashMap<>();
		Object key = new Object();
		Object value = new Object();
		map.put(key, value);
		System.gc();
		assertArrayEquals(new Object[] { Map.entry(key, value) }, map.entrySet().toArray());
	}

	@Test
	public void testReplace1() {
		WeakIdentityHashMap<Object, Object> map = new WeakIdentityHashMap<>();
		Object key = new Object();
		Object value = new Object();
		map.put(key, value);
		System.gc();
		assertSame(value, map.replace(key, new Object()));
	}

	@Test
	public void testReplace2() {
		WeakIdentityHashMap<Object, Object> map = new WeakIdentityHashMap<>();
		Object key = new Object();
		Object value = new Object();
		map.put(key, value);
		System.gc();
		assertTrue(map.replace(key, value, new Object()));
	}
}