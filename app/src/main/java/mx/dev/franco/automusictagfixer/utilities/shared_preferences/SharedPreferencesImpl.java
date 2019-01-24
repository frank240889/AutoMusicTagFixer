package mx.dev.franco.automusictagfixer.utilities.shared_preferences;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.utilities.Constants;


public class SharedPreferencesImpl extends AbstractSharedPreferences {
    @Inject
    SharedPreferences mSharedPreferences;

    public SharedPreferencesImpl(Context context){
        mSharedPreferences = context.getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void putInt(String key, int value) {
        mSharedPreferences.edit().putInt(key,value);
        mSharedPreferences.edit().apply();
    }

    @Override
    public void putString(String key, String value) {
        mSharedPreferences.edit().putString(key,value).apply();
    }

    @Override
    public void putLong(String key, long value) {
        mSharedPreferences.edit().putLong(key,value).apply();
    }

    @Override
    public void putFloat(String key, float value) {
        mSharedPreferences.edit().putFloat(key,value).apply();
    }

    @Override
    public void putBoolean(String key, boolean value) {
        mSharedPreferences.edit().putBoolean(key,value).apply();
    }

    @Override
    public int getInt(String key) {
        return mSharedPreferences.getInt(key,-1);
    }

    @Override
    public String getString(String key) {
        return mSharedPreferences.getString(key, null);
    }

    @Override
    public long getLong(String key) {
        return mSharedPreferences.getLong(key,-1);
    }

    @Override
    public float getFloat(String key) {
        return mSharedPreferences.getFloat(key,-1);
    }

    @Override
    public boolean getBoolean(String key) {
        return mSharedPreferences.getBoolean(key,false);
    }

    @Override
    public void remove(String key) {
        mSharedPreferences.edit().remove(key).apply();
    }
}
