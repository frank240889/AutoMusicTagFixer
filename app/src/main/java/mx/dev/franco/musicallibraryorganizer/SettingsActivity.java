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
import java.util.Set;

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


        if(preference instanceof MultiSelectListPreference){
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getStringSet(preference.getKey(), null));
        }
        else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
        }
    }

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
        Log.d("actionbar",(actionBar == null)+"");
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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
//        Log.d(key,  (sharedPreferences.getStringSet(key,null).size())+"");
        switch (key) {
            case "size_album_art":
                String opt = sharedPreferences.getString(key, "-1");
                SelectedOptions.ALBUM_ART_SIZE = SelectedOptions.setValueImageSize(opt);
                Log.d(key, opt);
                break;
            case "data_to_download":
                Set<String> set = sharedPreferences.getStringSet(key, null);
                SelectedOptions.setValuesExtraDataToDownload(set);
                break;
            case "title_service_change_switch":
                SelectedOptions.AUTOMATIC_CHANGE_FILENAME = sharedPreferences.getBoolean(key,true);
                Log.d(key,  SelectedOptions.AUTOMATIC_CHANGE_FILENAME+"");
                break;
            case "title_manual_change_switch":
                SelectedOptions.MANUAL_CHANGE_FILE = sharedPreferences.getBoolean(key, false);

                Log.d(key,SelectedOptions.MANUAL_CHANGE_FILE+"");
                break;
            case "title_automatically_replace_strange_chars":
                SelectedOptions.AUTOMATICALLY_REPLACE_STRANGE_CHARACTERS = sharedPreferences.getBoolean(key,false);
                Log.d(key, SelectedOptions.AUTOMATICALLY_REPLACE_STRANGE_CHARACTERS+"");
                break;
            case "show_separators":
                SelectedOptions.SHOW_SEPARATORS = sharedPreferences.getBoolean(key,false);
                SelectFolderActivity.audioItemArrayAdapter.notifyDataSetChanged();
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
            getActivity().setTheme(R.style.SettingsStyle_Fragment);
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.

            //bindPreferenceSummaryToValue(findPreference("example_text"));
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
            getActivity().setTheme(R.style.SettingsStyle_Fragment);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("size_album_art"));
            bindPreferenceSummaryToValue(findPreference("data_to_download"));
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
