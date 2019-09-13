package mx.dev.franco.automusictagfixer.persistence.cache;


import android.support.v4.util.ArrayMap;

import java.util.Map;

import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.interfaces.Cache;

/**
 * Created by Franco Castillo on 13/04/2018.
 */
public class DownloadedTrackDataCacheImpl implements Cache<String, Identifier.IdentificationResults> {
    private Map<String, Identifier.IdentificationResults> cache;

    public DownloadedTrackDataCacheImpl() {
        cache = new ArrayMap<>();
    }

    @Override
    public void add(String key, Identifier.IdentificationResults value) {
        cache.put(key, value);
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public Identifier.IdentificationResults load(String key) {
        return cache.get(key);
    }

    @Override
    public void deleteAll() {
        cache.clear();
    }
}
