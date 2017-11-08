package mx.dev.franco.musicallibraryorganizer;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity{
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */

    private boolean updateUI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = "";

            if (preference instanceof ListPreference) {
                stringValue = value.toString();
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                // Set the summary to reflect the new value.
                preference.setSummary( index >= 0 ? listPreference.getEntries()[index] : null);
            }
            else if(preference instanceof MultiSelectListPreference) {
                MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) preference;
                String summary = "";
                String separator = "";
                //Get the current values selected, convert to string and replace braces
                String str = value.toString().replace("[", "").replace("]", "");
                //if no values were selected, then we have empty character so set summary to "Ninguno",
                //otherwise split this string to string array and get every value
                if(!str.equals("")){
                String[] strArr = str.split(",");
                    for (String val : strArr) {
                        // For each value retrieve index
                        //trim the string, because after first element, there is a space before the element
                        //for example "value, value2", before value2 there is one space
                        int index = multiSelectListPreference.findIndexOfValue(val.trim());
                        // Retrieve entry from index
                        CharSequence mEntry = index >= 0 ? multiSelectListPreference.getEntries()[index] : null;
                        if (mEntry != null) {
                            summary = summary + separator + mEntry;
                            separator = ", ";
                        }
                    }
                }
                else{
                    summary = "Ninguno";
                }


                multiSelectListPreference.setSummary(summary);
            }

            else {
                stringValue = value.toString();
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "key_size_album_art":
                String opt = sharedPreferences.getString(key, "-1");
                Settings.SETTING_SIZE_ALBUM_ART = Settings.setValueImageSize(opt);
                Log.d(key, opt);
                break;
            case "key_rename_file_automatic_mode":
                Settings.SETTING_RENAME_FILE_AUTOMATIC_MODE = sharedPreferences.getBoolean(key,true);
                Log.d(key,  Settings.SETTING_RENAME_FILE_AUTOMATIC_MODE +"");
                break;
            case "key_rename_file_manual_mode":
                Settings.SETTING_RENAME_FILE_MANUAL_MODE = sharedPreferences.getBoolean(key, true);
                Log.d(key, Settings.SETTING_RENAME_FILE_MANUAL_MODE +"");
                break;
            case "key_rename_file_semi_automatic_mode":
                Settings.SETTING_RENAME_FILE_SEMI_AUTOMATIC_MODE = sharedPreferences.getBoolean(key, true);
                Log.d(key, Settings.SETTING_RENAME_FILE_SEMI_AUTOMATIC_MODE +"");
                break;
            case "key_replace_strange_chars_manual_mode":
                Settings.SETTING_REPLACE_STRANGE_CHARS_MANUAL_MODE = sharedPreferences.getBoolean(key,true);
                Log.d(key, Settings.SETTING_REPLACE_STRANGE_CHARS_MANUAL_MODE +"");
                break;
            case "key_use_embed_player":
                Settings.SETTING_USE_EMBED_PLAYER = sharedPreferences.getBoolean(key,true);
                break;
            case "key_overwrite_all_tags_automatic_mode":
                Settings.SETTING_OVERWRITE_ALL_TAGS_AUTOMATIC_MODE = sharedPreferences.getBoolean(key, true);
                break;
            case "key_background_service":
                Settings.BACKGROUND_CORRECTION = sharedPreferences.getBoolean(key, false);
                break;
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("key_size_album_art"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
