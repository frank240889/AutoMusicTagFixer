package mx.dev.franco.automusictagfixer.persistence.cache;


import androidx.collection.ArrayMap;

import java.util.Map;

import mx.dev.franco.automusictagfixer.interfaces.Cache;

/**
 * Created by Franco Castillo on 13/04/2018.
 */
public class CoverDataCache implements Cache<String, byte[]> {
    private Map<String, byte[]> cache;

    public CoverDataCache() {
        cache = new ArrayMap<>();
    }

    @Override
    public void add(String key, byte[] value) {
        cache.put(key, value);
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public byte[] load(String key) {
        return cache.get(key);
    }

    @Override
    public void deleteAll() {
        cache.clear();
    }
}
