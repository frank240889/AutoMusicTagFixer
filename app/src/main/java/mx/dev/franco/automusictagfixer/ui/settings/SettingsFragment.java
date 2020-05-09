package mx.dev.franco.automusictagfixer.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.HasSupportFragmentInjector;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.identifier.ApiInitializerService;
import mx.dev.franco.automusictagfixer.persistence.cache.IdentificationResultsCache;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;
import mx.dev.franco.automusictagfixer.utilities.Settings;

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener, HasSupportFragmentInjector {
    SwitchPreference mSDCardAccess = null;
    SharedPreferences mSharedPreferences;
    @Inject
    IdentificationResultsCache mIdentificationResultsCache;
    @Inject
    ServiceUtils mServiceUtils;
    @Inject
    DispatchingAndroidInjector<Fragment> mDispatchingAndroidInjector;

    @Override
    public void onAttach(@NonNull Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

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
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        boolean hasSd = AudioTagger.
                StorageHelper.
                getInstance(requireActivity()).
                isPresentRemovableStorage();

        if (!hasSd) {
            mSDCardAccess.setEnabled(false);
            mSDCardAccess.setSummary(R.string.removable_media_no_detected);
        }
        else {
            mSDCardAccess.setEnabled(true);
        }


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
                        mSharedPreferences.edit().
                                putString(key, preference.getString(key,"kImageSizeXLarge")).apply();
                    break;
                case "key_rename_file_automatic_mode":
                case "key_overwrite_all_tags_automatic_mode":
                case "key_use_embed_player":
                        mSharedPreferences.edit().
                                putBoolean(key, preference.getBoolean(key,true)).apply();
                    break;
                case "key_enable_sd_card_access":
                        if (preference.getBoolean(key, true)) {
                            requestAccessToSD();
                        }
                        else {
                            revokePermission();
                        }
                    break;
                case "key_language":
                        String language = preference.getString(key, "0");
                        Settings.SETTING_LANGUAGE = Settings.setValueLanguage(language);
                        Intent apiIntent = new Intent(requireActivity(), ApiInitializerService.class);
                        mIdentificationResultsCache.deleteAll();
                        requireActivity().startService(apiIntent);
                    break;
                case "key_identification_strategy":
                        mIdentificationResultsCache.deleteAll();
                    break;
            }
        }
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return mDispatchingAndroidInjector;
    }
}
