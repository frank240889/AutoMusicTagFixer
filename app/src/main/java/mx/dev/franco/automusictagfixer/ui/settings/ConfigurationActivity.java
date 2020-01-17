package mx.dev.franco.automusictagfixer.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.Settings;

public class ConfigurationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        SwitchPreference mSDCardAccess = null;
        SharedPreferences mSharedPreferences;

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {

            String msg;
            if (requestCode == RequiredPermissions.REQUEST_PERMISSION_SAF && resultCode == Activity.RESULT_OK && resultData != null) {
                // The document selected by the user won't be returned in the intent.
                // Instead, a URI to that document will be contained in the return intent
                // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
                //Save root Uri of SD card
                boolean res = AndroidUtils.grantPermissionSD(getActivity(), resultData);
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

            Toast toast = AndroidUtils.getToast(getActivity());
            toast.setText(msg);
            toast.show();

        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.pref_general);
            mSDCardAccess = findPreference("key_enable_sd_card_access");
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mSharedPreferences.registerOnSharedPreferenceChangeListener(this);


        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        private void revokePermission() {
            if(AndroidUtils.getUriSD(getActivity()) != null) {
                AndroidUtils.revokePermissionSD(getActivity());

                Toast toast = AndroidUtils.getToast(getActivity());Toast.makeText(getActivity(),
                        getString(R.string.permission_revoked), Toast.LENGTH_LONG);
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

        @Override
        public void onSharedPreferenceChanged(SharedPreferences preference, String key) {
            Log.w(getClass().getName(), "key: " + key);
            if (preference instanceof ListPreference) {
                String stringValue = ((androidx.preference.ListPreference) preference).getValue();
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                // Set the summary to reflect the new value.
                ((androidx.preference.ListPreference) preference).setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            }
            else if (preference instanceof MultiSelectListPreference) {
                MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) preference;
                String summary = "";
                String separator = "";
                //Get the current values selected, convert to string and replace braces
                String str = preference.getString(key, "").replace("[", "").replace("]", "");
                //if no values were selected, then we have empty character so set summary to "Ninguno",
                //otherwise split this string to string array and get every value
                if (!str.equals("")) {
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
                } else {
                    summary = "";//"Ninguno" ;
                }


                multiSelectListPreference.setSummary(summary);
            }
            else {
                switch (key) {
                    case "key_size_album_art":
                        String opt = mSharedPreferences.getString(key, "-1");
                        Settings.SETTING_SIZE_ALBUM_ART = Settings.setValueImageSize(opt);
                        break;
                    case "key_background_service":
                        Intent intent = new Intent(Constants.Actions.ACTION_SHOW_NOTIFICATION);
                        if (preference.getBoolean(key, true)) {
                            intent.putExtra(Constants.Actions.ACTION_SHOW_NOTIFICATION, true);
                        } else {
                            intent.putExtra(Constants.Actions.ACTION_SHOW_NOTIFICATION, false);
                        }
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                        break;

                    case "key_enable_sd_card_access":
                        if (preference.getBoolean(key, true)) {
                            requestAccessToSD();
                        } else {
                            revokePermission();
                        }
                        break;
                    case "key_language":
                        String language = preference.getString(key, "0");
                        Settings.SETTING_LANGUAGE = Settings.setValueLanguage(language);
                        break;
                }
            }
        }
    }
}