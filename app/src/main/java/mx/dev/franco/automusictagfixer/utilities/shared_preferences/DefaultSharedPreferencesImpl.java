package mx.dev.franco.automusictagfixer.utilities.shared_preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Inject;


public class DefaultSharedPreferencesImpl extends AbstractSharedPreferences {
    @Inject
    SharedPreferences mDefaultSharedPreferences;

    public DefaultSharedPreferencesImpl(Context context){
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void putInt(String key, int value) {
        mDefaultSharedPreferences.edit().putInt(key,value);
        mDefaultSharedPreferences.edit().apply();
    }

    @Override
    public void putString(String key, String value) {
        mDefaultSharedPreferences.edit().putString(key,value).apply();
    }

    @Override
    public void putLong(String key, long value) {
        mDefaultSharedPreferences.edit().putLong(key,value).apply();
    }

    @Override
    public void putFloat(String key, float value) {
        mDefaultSharedPreferences.edit().putFloat(key,value).apply();
    }

    @Override
    public void putBoolean(String key, boolean value) {
        mDefaultSharedPreferences.edit().putBoolean(key,value).apply();
    }

    @Override
    public int getInt(String key) {
        return mDefaultSharedPreferences.getInt(key,-1);
    }

    @Override
    public String getString(String key) {
        return mDefaultSharedPreferences.getString(key, null);
    }

    @Override
    public long getLong(String key) {
        return mDefaultSharedPreferences.getLong(key,-1);
    }

    @Override
    public float getFloat(String key) {
        return mDefaultSharedPreferences.getFloat(key,-1);
    }

    @Override
    public boolean getBoolean(String key) {
        return mDefaultSharedPreferences.getBoolean(key,false);
    }

    @Override
    public void remove(String key) {
        mDefaultSharedPreferences.edit().remove(key).apply();
    }
}
