package mx.dev.franco.automusictagfixer.persistence.cache;


import androidx.collection.ArrayMap;

import java.util.List;
import java.util.Map;

import mx.dev.franco.automusictagfixer.identifier.TrackIdentificationResult;
import mx.dev.franco.automusictagfixer.interfaces.Cache;

/**
 * Created by Franco Castillo on 13/04/2018.
 */
public class TrackResultsCache implements Cache<String, List<TrackIdentificationResult>> {
    private Map<String, List<TrackIdentificationResult>> cache;

    public TrackResultsCache() {
        cache = new ArrayMap<>();
    }

    @Override
    public void add(String key, List<TrackIdentificationResult> value) {
        cache.put(key, value);
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public List<TrackIdentificationResult> load(String key) {
        return cache.get(key);
    }

    @Override
    public void deleteAll() {
        cache.clear();
    }
}
