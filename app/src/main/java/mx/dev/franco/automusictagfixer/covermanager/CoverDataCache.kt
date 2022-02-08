package mx.dev.franco.automusictagfixer.covermanager

import androidx.collection.ArrayMap
import mx.dev.franco.automusictagfixer.interfaces.Cache

/**
 * Created by Franco Castillo on 13/04/2018.
 */
class CoverDataCache : Cache<String?, ByteArray?> {
    private val cache = ArrayMap<String?, ByteArray?>()
    override fun add(key: String?, value: ByteArray?) {
        cache[key] = value
    }

    override fun delete(key: String?) {
        cache.remove(key)
    }


    override fun load(key: String?) = cache[key]


    override fun deleteAll() = cache.clear()
}