package mx.dev.franco.automusictagfixer.interfaces;

/**
 * Created by Franco Castillo on 13/04/2018.
 * Interface to create a cache that can
 * hold temporally some data.
 */

public interface Cache<K,V> {
    void add(K key, V value);
    void delete(K key);
    V load(K key);
    void deleteAll();
    Cache<K,V> getCache();
}
