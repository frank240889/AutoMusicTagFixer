package mx.dev.franco.automusictagfixer.persistence.cache;


import androidx.collection.ArrayMap;

import java.util.List;
import java.util.Map;

import mx.dev.franco.automusictagfixer.identifier.CoverIdentificationResult;
import mx.dev.franco.automusictagfixer.interfaces.Cache;

/**
 * Created by Franco Castillo on 13/04/2018.
 */
public class CoverResultsCache implements Cache<String, List<CoverIdentificationResult>> {
    private Map<String, List<CoverIdentificationResult>> cache;

    public CoverResultsCache() {
        cache = new ArrayMap<>();
    }

    @Override
    public void add(String key, List<CoverIdentificationResult> value) {
        cache.put(key, value);
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public List<CoverIdentificationResult> load(String key) {
        return cache.get(key);
    }

    @Override
    public void deleteAll() {
        cache.clear();
    }
}
