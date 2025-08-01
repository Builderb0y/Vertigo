package builderb0y.vertigo;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.*;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//*cries in WeakHashMap not being identity-based and IdentityHashMap not being weak*
public class WeakIdentityHashMap<K, V> implements Map<K, V> {

	public static final Hash.Strategy<Object> STRATEGY = new Hash.Strategy<>() {

		@Override
		public int hashCode(Object object) {
			if (object instanceof WeakIdentityReference<?>) {
				return object.hashCode();
			}
			else {
				return System.identityHashCode(object);
			}
		}

		@Override
		public boolean equals(Object a, Object b) {
			if (a != null && b != null) {
				if (a instanceof WeakIdentityReference<?> reference) {
					a = reference.get();
				}
				if (b instanceof WeakIdentityReference<?> reference) {
					b = reference.get();
				}
			}
			return a == b;
		}
	};

	/**
	actually of type Object2ObjectOpenCustomHashMap<WeakIdentityReference<K>, V>,
	but erased to type Object2ObjectOpenCustomHashMap<Object, V>
	to allow querying (like {@link #containsKey(Object)})
	without wrapping the key in a WeakIdentityReference.
	a custom Hash.Strategy counts wrapped and unwrapped keys as equal.
	*/
	public final Object2ObjectOpenCustomHashMap<Object, V> delegate;
	public final ReferenceQueue<K> queue = new ReferenceQueue<>();
	public KeySet keySet;
	public EntrySet entrySet;

	public WeakIdentityHashMap() {
		this.delegate = new Object2ObjectOpenCustomHashMap<>(STRATEGY);
	}

	public WeakIdentityHashMap(int capacity) {
		this.delegate = new Object2ObjectOpenCustomHashMap<>(capacity, STRATEGY);
	}

	public void purge() {
		for (Reference<? extends K> reference; (reference = this.queue.poll()) != null; this.delegate.remove(reference));
	}

	@Override
	public int size() {
		this.purge();
		return this.delegate.size();
	}

	@Override
	public boolean isEmpty() {
		this.purge();
		return this.delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		this.purge();
		if (key == null) return false;
		return this.delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		this.purge();
		if (value == null) return false;
		return this.delegate.containsValue(value);
	}

	@Override
	public V get(Object key) {
		this.purge();
		if (key == null) return null;
		return this.delegate.get(key);
	}

	@Override
	public @Nullable V put(K key, V value) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(value, "value");
		this.purge();
		return this.delegate.put(new WeakIdentityReference<>(this.queue, key), value);
	}

	@Override
	public V remove(Object key) {
		this.purge();
		if (key == null) return null;
		return this.delegate.remove(key);
	}

	@Override
	public void putAll(@NotNull Map<? extends K, ? extends V> map) {
		this.purge();
		for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
			K key = Objects.requireNonNull(entry.getKey(), "map contains null key");
			V value = Objects.requireNonNull(entry.getValue(), "map contains null value");
			this.delegate.put(new WeakIdentityReference<>(this.queue, key), value);
		}
	}

	@Override
	public void clear() {
		this.delegate.clear();
		while (this.queue.poll() != null);
	}

	@Override
	public @NotNull Set<K> keySet() {
		return this.keySet == null ? this.keySet = this.new KeySet() : this.keySet;
	}

	@Override
	public @NotNull Collection<V> values() {
		return this.delegate.values();
	}

	@Override
	public @NotNull Set<Entry<K, V>> entrySet() {
		return this.entrySet == null ? this.entrySet = this.new EntrySet() : this.entrySet;
	}

	@Override
	public V getOrDefault(Object key, V defaultValue) {
		this.purge();
		return this.delegate.getOrDefault(key, defaultValue);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void forEach(BiConsumer<? super K, ? super V> action) {
		this.purge();
		this.delegate.forEach((Object reference, V value) -> {
			K key = ((WeakIdentityReference<K>)(reference)).get();
			if (key != null) action.accept(key, value);
		});
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		class Wrapper implements BiFunction<Object, V, V> {

			public boolean containedNull;

			@Override
			@SuppressWarnings("unchecked")
			public V apply(Object reference, V value) {
				K key = ((WeakIdentityReference<K>)(reference)).get();
				if (key != null) return function.apply(key, value);
				this.containedNull = true;
				return null;

			}
		}
		this.purge();
		Wrapper wrapper = new Wrapper();
		this.delegate.replaceAll(wrapper);
		if (wrapper.containedNull) this.delegate.values().removeIf(Objects::isNull);
	}

	@Override
	public @Nullable V putIfAbsent(K key, V value) {
		this.purge();
		return this.delegate.putIfAbsent(new WeakIdentityReference<>(this.queue, key), value);
	}

	@Override
	public boolean remove(Object key, Object value) {
		this.purge();
		return this.delegate.remove(key, value);
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		this.purge();
		return this.delegate.replace(new WeakIdentityReference<>(this.queue, key), oldValue, newValue);
	}

	@Override
	public @Nullable V replace(K key, V value) {
		this.purge();
		return this.delegate.replace(new WeakIdentityReference<>(this.queue, key), value);
	}

	@Override
	public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
		this.purge();
		return this.delegate.computeIfAbsent(new WeakIdentityReference<>(this.queue, key), (Object reference) -> {
			return mappingFunction.apply(key);
		});
	}

	@Override
	public V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		this.purge();
		return this.delegate.computeIfPresent(new WeakIdentityReference<>(this.queue, key), (Object reference, V value) -> {
			return remappingFunction.apply(key, value);
		});
	}

	@Override
	public V compute(K key, @NotNull BiFunction<? super K, ? super @Nullable V, ? extends V> remappingFunction) {
		this.purge();
		return this.delegate.compute(new WeakIdentityReference<>(this.queue, key), (Object reference, V value) -> {
			return remappingFunction.apply(key, value);
		});
	}

	@Override
	public V merge(K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		this.purge();
		return this.delegate.merge(new WeakIdentityReference<>(this.queue, key), value, remappingFunction);
	}

	public static class WeakIdentityReference<K> extends WeakReference<K> {

		public final int hashCode;

		public WeakIdentityReference(ReferenceQueue<? super K> queue, K key) {
			super(key, queue);
			this.hashCode = System.identityHashCode(key);
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof WeakIdentityReference<?> that && this.get() == that.get();
		}
	}

	public class KeySet implements Set<K> {

		@Override
		public int size() {
			return WeakIdentityHashMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return WeakIdentityHashMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object object) {
			return WeakIdentityHashMap.this.containsKey(object);
		}

		@Override
		public @NotNull Iterator<K> iterator() {
			return new KeyIterator<>(WeakIdentityHashMap.this.delegate.keySet().iterator());
		}

		@Override
		public @NotNull Object[] toArray() {
			ArrayList<Object> list = new ArrayList<>(this.size());
			for (Object object : this) {
				list.add(object);
			}
			return list.toArray();
		}

		@Override
		public @NotNull <T> T[] toArray(@NotNull T[] array) {
			ArrayList<Object> list = new ArrayList<>(this.size());
			for (Object object : this) {
				list.add(object);
			}
			return list.toArray(array);
		}

		@Override
		public boolean add(K k) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object object) {
			return WeakIdentityHashMap.this.remove(object) != null;
		}

		@Override
		public boolean containsAll(@NotNull Collection<?> collection) {
			WeakIdentityHashMap.this.purge();
			Object2ObjectOpenCustomHashMap<Object, V> delegate = WeakIdentityHashMap.this.delegate;
			for (Object object : collection) {
				if (!delegate.containsKey(object)) return false;
			}
			return true;
		}

		@Override
		public boolean addAll(@NotNull Collection<? extends K> collection) {
			throw new UnsupportedOperationException();
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean retainAll(@NotNull Collection<?> collection) {
			return WeakIdentityHashMap.this.delegate.keySet().removeIf((Object reference) -> {
				K key = ((WeakIdentityReference<K>)(reference)).get();
				return key == null || !collection.contains(key);
			});
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean removeAll(@NotNull Collection<?> collection) {
			return WeakIdentityHashMap.this.delegate.keySet().removeIf((Object reference) -> {
				K key = ((WeakIdentityReference<K>)(reference)).get();
				return key == null || collection.contains(key);
			});
		}

		@Override
		public void clear() {
			WeakIdentityHashMap.this.clear();
		}

		@Override
		public <T> T[] toArray(@NotNull IntFunction<T[]> generator) {
			ArrayList<Object> list = new ArrayList<>(this.size());
			for (Object object : this) {
				list.add(object);
			}
			return list.toArray(generator.apply(list.size()));
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean removeIf(@NotNull Predicate<? super K> filter) {
			return WeakIdentityHashMap.this.delegate.keySet().removeIf((Object reference) -> {
				K key = ((WeakIdentityReference<K>)(reference)).get();
				return key == null || filter.test(key);
			});
		}

		@Override
		@SuppressWarnings("unchecked")
		public void forEach(Consumer<? super K> action) {
			for (Iterator<Object> iterator = WeakIdentityHashMap.this.delegate.keySet().iterator(); iterator.hasNext();) {
				K next = ((WeakIdentityReference<K>)(iterator.next())).get();
				if (next != null) action.accept(next);
				else iterator.remove();
			}
		}
	}

	public static class KeyIterator<K> implements Iterator<K> {

		public final Iterator<Object> delegate;
		public K next; //hold strong reference.

		public KeyIterator(Iterator<Object> delegate) {
			this.delegate = delegate;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean hasNext() {
			while (true) {
				if (this.next != null) return true;
				if (!this.delegate.hasNext()) return false;
				this.next = ((WeakIdentityReference<K>)(this.delegate.next())).get();
			}
		}

		@Override
		public K next() {
			if (this.hasNext()) {
				K next = this.next;
				this.next = null;
				return next;
			}
			else {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			this.delegate.remove();
		}
	}

	public class EntrySet implements Set<Map.Entry<K, V>> {

		@Override
		public int size() {
			return WeakIdentityHashMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return WeakIdentityHashMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object object) {
			if (object instanceof Map.Entry<?, ?> entry) {
				WeakIdentityHashMap.this.purge();
				return (
					WeakIdentityHashMap.this.delegate.containsKey(entry.getKey()) &&
					WeakIdentityHashMap.this.delegate.containsValue(entry.getValue())
				);
			}
			return false;
		}

		@Override
		public @NotNull Iterator<Entry<K, V>> iterator() {
			return new EntryIterator<>(WeakIdentityHashMap.this.delegate.entrySet().iterator());
		}

		@Override
		public @NotNull Object[] toArray() {
			ArrayList<Object> list = new ArrayList<>(this.size());
			for (Entry<K, V> entry : this) {
				list.add(entry);
			}
			return list.toArray();
		}

		@Override
		public @NotNull <T> T[] toArray(@NotNull T[] array) {
			ArrayList<Object> list = new ArrayList<>(this.size());
			for (Entry<K, V> entry : this) {
				list.add(entry);
			}
			return list.toArray(array);
		}

		@Override
		public boolean add(Entry<K, V> entry) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object object) {
			return object instanceof Map.Entry<?, ?> entry && WeakIdentityHashMap.this.remove(entry.getKey(), entry.getValue());
		}

		@Override
		public boolean containsAll(@NotNull Collection<?> collection) {
			WeakIdentityHashMap.this.purge();
			Object2ObjectOpenCustomHashMap<Object, V> delegate = WeakIdentityHashMap.this.delegate;
			for (Object object : collection) {
				if (!(object instanceof Map.Entry<?, ?> entry) || !delegate.containsKey(entry.getKey()) || !delegate.containsValue(entry.getValue())) return false;
			}
			return true;
		}

		@Override
		public boolean addAll(@NotNull Collection<? extends Entry<K, V>> collection) {
			throw new UnsupportedOperationException();
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean retainAll(@NotNull Collection<?> collection) {
			return WeakIdentityHashMap.this.delegate.entrySet().removeIf((Map.Entry<Object, V> entry) -> {
				K key = ((WeakIdentityReference<K>)(entry.getKey())).get();
				return key == null || !collection.contains(Map.entry(key, entry.getValue()));
			});
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean removeAll(@NotNull Collection<?> collection) {
			return WeakIdentityHashMap.this.delegate.entrySet().removeIf((Map.Entry<Object, V> entry) -> {
				K key = ((WeakIdentityReference<K>)(entry.getKey())).get();
				return key == null || collection.contains(Map.entry(key, entry.getValue()));
			});
		}

		@Override
		public void clear() {
			WeakIdentityHashMap.this.clear();
		}

		@Override
		public <T> T[] toArray(@NotNull IntFunction<T[]> generator) {
			ArrayList<Object> list = new ArrayList<>(this.size());
			for (Object object : this) {
				list.add(object);
			}
			return list.toArray(generator.apply(list.size()));
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean removeIf(@NotNull Predicate<? super Entry<K, V>> filter) {
			return WeakIdentityHashMap.this.delegate.entrySet().removeIf((Map.Entry<Object, V> entry) -> {
				K key = ((WeakIdentityReference<K>)(entry.getKey())).get();
				return key == null || !filter.test(Map.entry(key, entry.getValue()));
			});

		}

		@Override
		@SuppressWarnings("unchecked")
		public void forEach(Consumer<? super Entry<K, V>> action) {
			for (ObjectIterator<Entry<Object, V>> iterator = WeakIdentityHashMap.this.delegate.entrySet().iterator(); iterator.hasNext();) {
				Entry<Object, V> entry = iterator.next();
				K key = ((WeakIdentityReference<K>)(entry.getKey())).get();
				if (key != null) action.accept(Map.entry(key, entry.getValue()));
				else iterator.remove();
			}
		}
	}

	public static class EntryIterator<K, V> implements Iterator<Map.Entry<K, V>> {

		public final Iterator<Map.Entry<Object, V>> delegate;
		public Map.Entry<K, V> next; //hold strong reference.

		public EntryIterator(Iterator<Map.Entry<Object, V>> delegate) {
			this.delegate = delegate;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean hasNext() {
			while (true) {
				if (this.next != null) return true;
				if (!this.delegate.hasNext()) return false;
				Map.Entry<Object, V> next = this.delegate.next();
				K key = ((WeakIdentityReference<K>)(next.getKey())).get();
				if (key != null) {
					V value = next.getValue();
					this.next = Map.entry(key, value);
				}
			}
		}

		@Override
		public Entry<K, V> next() {
			if (this.hasNext()) {
				Entry<K, V> next = this.next;
				this.next = null;
				return next;
			}
			else {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			this.delegate.remove();
		}
	}
}