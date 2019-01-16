package mx.dev.franco.automusictagfixer.UI.settings;


import android.annotation.TargetApi;
import android.app.Activity;
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
import android.preference.SwitchPreference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.Settings;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;

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
    private static final String TAG = SettingsActivity.class.getName();
    public SwitchPreference mSDCardAccess = null;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */

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

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
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
                summary = "";//"Ninguno" ;
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
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager.getDefaultSharedPreferences(preference.getContext()).
                        getString(preference.getKey(), ""));

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
                break;
            case "key_rename_file_automatic_mode":
                break;
            case "key_replace_strange_chars_manual_mode":
                break;
            case "key_use_embed_player":
                break;
            case "key_overwrite_all_tags_automatic_mode":
                break;
            case "key_background_service":
                    Intent intent = new Intent(Constants.Actions.ACTION_SHOW_NOTIFICATION);
                    if(sharedPreferences.getBoolean(key, true)) {
                        intent.putExtra(Constants.Actions.ACTION_SHOW_NOTIFICATION, true);
                    }
                    else {
                        intent.putExtra(Constants.Actions.ACTION_SHOW_NOTIFICATION, false);
                    }
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                break;

            case "key_enable_sd_card_access":
                if(sharedPreferences.getBoolean(key, true)){
                    requestAccessToSD();
                }
                else {
                    revokePermission();
                }
                break;
            case "key_language":
                String language = sharedPreferences.getString(key, "0");
                Settings.SETTING_LANGUAGE = Settings.setValueLanguage(language);
                Log.d("Lenguaje", Settings.SETTING_LANGUAGE.name());
                break;
        }
    }

    private void revokePermission() {
        if(AndroidUtils.getUriSD(getApplicationContext()) != null) {
            AndroidUtils.revokePermissionSD(getApplicationContext());

            Toast toast = AndroidUtils.getToast(getApplicationContext());Toast.makeText(getApplicationContext(), getString(R.string.permission_revoked), Toast.LENGTH_LONG);
            toast.setText(getString(R.string.permission_revoked));
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();
            mSDCardAccess.setChecked(false);
        }
    }

    public void requestAccessToSD() {
        //Request permission to access SD card
        //through Storage Access Framework
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, RequiredPermissions.REQUEST_PERMISSION_SAF);
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
            ((SettingsActivity)getActivity()).mSDCardAccess = (SwitchPreference) findPreference("key_enable_sd_card_access");
            //Disable URI SD request if no removable media is detected
            if(StorageHelper.getInstance(getActivity().getApplicationContext()).getBasePaths().size()<2){

                ((SettingsActivity)getActivity()).mSDCardAccess.setEnabled(false);
                ((SettingsActivity)getActivity()).mSDCardAccess.setSummary(getString(R.string.removable_media_no_detected));

            }
            else {

                ((SettingsActivity)getActivity()).mSDCardAccess.setChecked((AndroidUtils.getUriSD(getActivity().getApplicationContext()) != null));
            }
            setHasOptionsMenu(true);
            bindPreferenceSummaryToValue(findPreference("key_language"));
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().finish();
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
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        String msg;
        if (requestCode == RequiredPermissions.REQUEST_PERMISSION_SAF && resultCode == Activity.RESULT_OK && resultData != null) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
                //Save root Uri of SD card
                boolean res = AndroidUtils.grantPermissionSD(getApplicationContext(), resultData);
                if(res){
                    mSDCardAccess.setChecked(true);
                    msg = getString(R.string.permission_granted);
                }
                else {
                    msg = getString(R.string.could_not_get_permission);
                }
        }
        else {
            msg = getString(R.string.permission_denied);
            mSDCardAccess.setChecked(false);
        }

        Toast toast = AndroidUtils.getToast(this);
        toast.setText(msg);
        toast.show();

    }

}
