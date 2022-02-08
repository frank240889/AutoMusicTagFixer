package mx.dev.franco.automusictagfixer.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.ListPreference
import android.preference.MultiSelectListPreference
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.fixer.StorageHelper
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener,
    HasAndroidInjector {
    var mSDCardAccess: SwitchPreference? = null
    var mSharedPreferences: SharedPreferences? = null


    @Inject
    lateinit var mDispatchingAndroidInjector: DispatchingAndroidInjector<Any>
    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        val msg: String
        if (requestCode == RequiredPermissions.REQUEST_PERMISSION_SAF && resultCode == Activity.RESULT_OK && resultData != null) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
            //Save root Uri of SD card
            val res = AndroidUtils.grantPermissionSD(activity, resultData)
            if (res) {
                mSDCardAccess!!.isChecked = true
                msg = getString(R.string.permission_granted)
            } else {
                msg = getString(R.string.could_not_get_permission)
            }
        } else {
            msg = getString(R.string.permission_denied)
            mSDCardAccess!!.isChecked = false
        }
        val toast = AndroidUtils.getToast(activity)
        toast.setText(msg)
        toast.show()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        addPreferencesFromResource(R.xml.pref_general)
        mSDCardAccess = findPreference("key_enable_sd_card_access")
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        mSharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
        val hasSd =
            StorageHelper.getInstance(requireActivity()).isPresentRemovableStorage
        if (!hasSd) {
            mSDCardAccess!!.isEnabled = false
            mSDCardAccess!!.setSummary(R.string.removable_media_no_detected)
        } else {
            mSDCardAccess!!.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun revokePermission() {
        if (AndroidUtils.getUriSD(activity) != null) {
            AndroidUtils.revokePermissionSD(activity)
            val toast = AndroidUtils.getToast(activity)
            Toast.makeText(
                activity,
                getString(R.string.permission_revoked), Toast.LENGTH_LONG
            )
            toast.setText(getString(R.string.permission_revoked))
            toast.duration = Toast.LENGTH_SHORT
            toast.show()
            mSDCardAccess!!.isChecked = false
        }
    }

    fun requestAccessToSD() {
        //Request permission to access SD card
        //through Storage Access Framework
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, RequiredPermissions.REQUEST_PERMISSION_SAF)
    }

    override fun onSharedPreferenceChanged(preference: SharedPreferences, key: String) {
        Log.w(javaClass.name, "key: $key")
        if (preference is ListPreference) {
            val stringValue = (preference as androidx.preference.ListPreference).value
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            val listPreference = preference as ListPreference
            val index = listPreference.findIndexOfValue(stringValue)
            // Set the summary to reflect the new value.
            (preference as androidx.preference.ListPreference).summary =
                if (index >= 0) listPreference.entries[index] else null
        } else if (preference is MultiSelectListPreference) {
            val multiSelectListPreference = preference as MultiSelectListPreference
            var summary = ""
            var separator = ""
            //Get the current values selected, convert to string and replace braces
            val str = preference.getString(key, "")!!
                .replace("[", "").replace("]", "")
            //if no values were selected, then we have empty character so set summary to "Ninguno",
            //otherwise split this string to string array and get every value
            if (str != "") {
                val strArr = str.split(",").toTypedArray()
                for (`val` in strArr) {
                    // For each value retrieve index
                    //trim the string, because after first element, there is a space before the element
                    //for example "value, value2", before value2 there is one space
                    val index = multiSelectListPreference.findIndexOfValue(`val`.trim { it <= ' ' })
                    // Retrieve entry from index
                    val mEntry = if (index >= 0) multiSelectListPreference.entries[index] else null
                    if (mEntry != null) {
                        summary = summary + separator + mEntry
                        separator = ", "
                    }
                }
            } else {
                summary = "" //"Ninguno" ;
            }
            multiSelectListPreference.summary = summary
        } else {
            when (key) {
                "key_enable_sd_card_access" -> if (preference.getBoolean(key, true)) {
                    requestAccessToSD()
                } else {
                    revokePermission()
                }
            }
        }
    }

    override fun androidInjector(): AndroidInjector<Any> = mDispatchingAndroidInjector
}