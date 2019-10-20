package mx.dev.franco.automusictagfixer.persistence.cache;


import androidx.collection.ArrayMap;

import java.util.List;
import java.util.Map;

import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.interfaces.Cache;

/**
 * Created by Franco Castillo on 13/04/2018.
 */
public class DownloadedTrackDataCacheImpl implements Cache<String, List<Identifier.IdentificationResults>> {
    private Map<String, List<Identifier.IdentificationResults>> cache;

    public DownloadedTrackDataCacheImpl() {
        cache = new ArrayMap<>();
    }

    @Override
    public void add(String key, List<Identifier.IdentificationResults> value) {
        cache.put(key, value);
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public List<Identifier.IdentificationResults> load(String key) {
        return cache.get(key);
    }

    @Override
    public void deleteAll() {
        cache.clear();
    }
}
