package mx.dev.franco.automusictagfixer.utilities.shared_preferences;

public abstract class AbstractSharedPreferences {
    public abstract void putInt(String key, int value);
    public abstract void putString(String key, String value);
    public abstract void putLong(String key, long value);
    public abstract void putFloat(String key, float value);
    public abstract void putBoolean(String key, boolean value);
    public abstract int getInt(String key);
    public abstract String getString(String key);
    public abstract long getLong(String key);
    public abstract float getFloat(String key);
    public abstract boolean getBoolean(String key);
    public abstract void remove(String key);
}
