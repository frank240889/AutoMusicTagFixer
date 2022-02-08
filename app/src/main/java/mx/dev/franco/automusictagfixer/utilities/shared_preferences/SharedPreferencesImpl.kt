package mx.dev.franco.automusictagfixer.utilities.shared_preferences

import android.content.Context
import android.content.SharedPreferences
import mx.dev.franco.automusictagfixer.utilities.Constants

class SharedPreferencesImpl() : AbstractSharedPreferences() {
    private var mSharedPreferences: SharedPreferences? = null

    constructor(context: Context) : this() {
        mSharedPreferences = context.getSharedPreferences(
            Constants.Application.FULL_QUALIFIED_NAME,
            Context.MODE_PRIVATE
        )
    }

    override fun putInt(key: String?, value: Int) {
        mSharedPreferences!!.edit().putInt(key, value).apply()
    }

    override fun putString(key: String?, value: String?) {
        mSharedPreferences!!.edit().putString(key, value).apply()
    }

    override fun putLong(key: String?, value: Long) {
        mSharedPreferences!!.edit().putLong(key, value).apply()
    }

    override fun putFloat(key: String?, value: Float) {
        mSharedPreferences!!.edit().putFloat(key, value).apply()
    }

    override fun putBoolean(key: String?, value: Boolean) {
        mSharedPreferences!!.edit().putBoolean(key, value).apply()
    }

    override fun getInt(key: String?): Int {
        return mSharedPreferences!!.getInt(key, -1)
    }

    override fun getString(key: String?): String? {
        return mSharedPreferences!!.getString(key, null)
    }

    override fun getLong(key: String?): Long {
        return mSharedPreferences!!.getLong(key, -1)
    }

    override fun getFloat(key: String?): Float {
        return mSharedPreferences!!.getFloat(key, -1f)
    }

    override fun getBoolean(key: String?): Boolean {
        return mSharedPreferences!!.getBoolean(key, false)
    }

    override fun remove(key: String?) {
        mSharedPreferences!!.edit().remove(key).apply()
    }
}