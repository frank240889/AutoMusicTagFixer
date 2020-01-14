package mx.dev.franco.automusictagfixer.ui.settings;

import android.os.Bundle;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

public class ConfigurationActivity extends AppCompatActivity {
    public SwitchPreference mSDCardAccess = null;

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
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat {


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.pref_general);
            ((ConfigurationActivity)getActivity()).mSDCardAccess = findPreference("key_enable_sd_card_access");
            //Disable URI SD request if no removable media is detected
            if(AudioTagger.StorageHelper.getInstance(getActivity().getApplicationContext()).getBasePaths().size()<2){

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
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            view.setBackgroundColor(getResources().getColor(R.color.primaryBackground));
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
}