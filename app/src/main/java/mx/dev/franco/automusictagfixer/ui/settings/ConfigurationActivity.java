package mx.dev.franco.automusictagfixer.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

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

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SwitchPreferenceCompat mSDCardAccess = null;
        SharedPreferences mSharedPreferences;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.pref_general);
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mSharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
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
                    else {
                        switch (key) {
                            case "key_size_album_art":
                                String opt = mSharedPreferences.getString(key, "-1");
                                Settings.SETTING_SIZE_ALBUM_ART = Settings.setValueImageSize(opt);
                                break;
                            case "key_background_service":
                                Intent intent = new Intent(Constants.Actions.ACTION_SHOW_NOTIFICATION);
                                if (mSharedPreferences.getBoolean(key, true)) {
                                    intent.putExtra(Constants.Actions.ACTION_SHOW_NOTIFICATION, true);
                                } else {
                                    intent.putExtra(Constants.Actions.ACTION_SHOW_NOTIFICATION, false);
                                }
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                break;

                            case "key_enable_sd_card_access":
                                if (mSharedPreferences.getBoolean(key, true)) {
                                    requestAccessToSD();
                                } else {
                                    revokePermission();
                                }
                                break;
                            case "key_language":
                                String language = mSharedPreferences.getString(key, "0");
                                Settings.SETTING_LANGUAGE = Settings.setValueLanguage(language);
                                break;
                        }
                    }
                }
            });
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
    }
}