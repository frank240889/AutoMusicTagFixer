package mx.dev.franco.automusictagfixer.cache;


import android.support.v4.util.LruCache;

import java.util.HashMap;

import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;

/**
 * Created by Franco Castillo on 13/04/2018.
 */

public class DownloadedTrackDataCacheImpl extends LruCache<Integer, GnResponseListener.IdentificationResults> implements Cache<Integer, GnResponseListener.IdentificationResults> {
    private static DownloadedTrackDataCacheImpl sInstance;
    // Get max available VM memory, exceeding this amount will throw an
    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
    // int in its constructor.
    private static final int sMaxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    // Use 1/8th of the available memory for this memory cache.
    private static final int sCacheSize = sMaxMemory / 8;

    private DownloadedTrackDataCacheImpl(int size){
        super(size);
    }

    @Override
    public void add(Integer key, GnResponseListener.IdentificationResults value) {
        if(sInstance != null)
            sInstance.put(key, value);
    }

    @Override
    public void delete(Integer key) {
        if(sInstance != null)
            sInstance.remove(key);
    }

    @Override
    public GnResponseListener.IdentificationResults load(Integer key) {
        if(sInstance == null)
            return null;
        return sInstance.get(key);
    }

    @Override
    public void deleteAll() {

    }

    @Override
    public synchronized Cache<Integer, GnResponseListener.IdentificationResults> getCache() {
        return sInstance;
    }

    /**
     * Gets the cache through the builder;
     * creates one if doesn't exist. This implementation
     * can be changed if necessary inside the build method.
     */
    public static class Builder{
        public Builder(){

        }

        public Cache<Integer, GnResponseListener.IdentificationResults> build(){
            if(sInstance == null){
                sInstance = new DownloadedTrackDataCacheImpl(sCacheSize);
            }
            return sInstance;
        }
    }
}
