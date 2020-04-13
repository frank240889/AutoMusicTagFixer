package mx.dev.franco.automusictagfixer.persistence.cache;


import androidx.collection.ArrayMap;

import java.util.List;
import java.util.Map;

import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.interfaces.Cache;

/**
 * Created by Franco Castillo on 13/04/2018.
 */
public class IdentificationResultsCache implements Cache<String, List<? extends Identifier.IdentificationResults>> {
    private Map<String, List<? extends Identifier.IdentificationResults>> cache;

    public IdentificationResultsCache() {
        cache = new ArrayMap<>();
    }

    @Override
    public void add(String key, List<? extends Identifier.IdentificationResults> value) {
        cache.put(key, value);
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public List<? extends Identifier.IdentificationResults> load(String key) {
        return cache.get(key);
    }

    @Override
    public void deleteAll() {
        cache.clear();
    }
}
