package mx.dev.franco.automusictagfixer.utilities.shared_preferences


abstract class AbstractSharedPreferences {
    abstract fun putInt(key: String?, value: Int)
    abstract fun putString(key: String?, value: String?)
    abstract fun putLong(key: String?, value: Long)
    abstract fun putFloat(key: String?, value: Float)
    abstract fun putBoolean(key: String?, value: Boolean)
    abstract fun getInt(key: String?): Int
    abstract fun getString(key: String?): String?
    abstract fun getLong(key: String?): Long
    abstract fun getFloat(key: String?): Float
    abstract fun getBoolean(key: String?): Boolean
    abstract fun remove(key: String?)
}