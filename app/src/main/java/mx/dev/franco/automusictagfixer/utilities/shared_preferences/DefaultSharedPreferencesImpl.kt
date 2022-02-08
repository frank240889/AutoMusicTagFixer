package mx.dev.franco.automusictagfixer.utilities.shared_preferences

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class DefaultSharedPreferencesImpl(context: Context?) : AbstractSharedPreferences() {
    private val mDefaultSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    override fun putInt(key: String?, value: Int) {
        mDefaultSharedPreferences.edit().putInt(key, value).apply()
    }

    override fun putString(key: String?, value: String?) {
        mDefaultSharedPreferences.edit().putString(key, value).apply()
    }

    override fun putLong(key: String?, value: Long) {
        mDefaultSharedPreferences.edit().putLong(key, value).apply()
    }

    override fun putFloat(key: String?, value: Float) {
        mDefaultSharedPreferences.edit().putFloat(key, value).apply()
    }

    override fun putBoolean(key: String?, value: Boolean) {
        mDefaultSharedPreferences.edit().putBoolean(key, value).apply()
    }

    override fun getInt(key: String?): Int {
        return mDefaultSharedPreferences.getInt(key, -1)
    }

    override fun getString(key: String?): String? {
        return mDefaultSharedPreferences.getString(key, null)
    }

    override fun getLong(key: String?): Long {
        return mDefaultSharedPreferences.getLong(key, -1)
    }

    override fun getFloat(key: String?): Float {
        return mDefaultSharedPreferences.getFloat(key, -1f)
    }

    override fun getBoolean(key: String?): Boolean {
        return mDefaultSharedPreferences.getBoolean(key, false)
    }

    override fun remove(key: String?) {
        mDefaultSharedPreferences.edit().remove(key).apply()
    }

}