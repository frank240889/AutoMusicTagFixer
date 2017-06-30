package mx.dev.franco.musicallibraryorganizer;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
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
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (preference instanceof SeekBarListPreference || preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                // Set the summary to reflect the new value.
                preference.setSummary( index >= 0 ? listPreference.getEntries()[index] : null);

            }

            else {
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
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
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
        //Log.d(key,  sharedPreferences.getString(key,""));
        switch (key){
            case "size_album_art":
                SelectedOptions.ALBUM_ART_SIZE = sharedPreferences.getString(key,"-1");
                Log.d(key,  SelectedOptions.ALBUM_ART_SIZE);
                break;
            case "title_service_change_switch":
                SelectedOptions.AUTOMATIC_CHANGE_FILENAME = sharedPreferences.getBoolean(key,true);
                Log.d(key,  SelectedOptions.AUTOMATIC_CHANGE_FILENAME+"");
                break;
            case "title_manual_change_switch":
                SelectedOptions.MANUAL_CHANGE_FILE = sharedPreferences.getBoolean(key, false);

                Log.d(key,  SelectedOptions.MANUAL_CHANGE_FILE+"");
                break;
            case "title_automatically_replace_strange_chars":
                SelectedOptions.AUTOMATICALLY_REPLACE_STRANGE_CHARACTERS = sharedPreferences.getBoolean(key,false);
                Log.d(key, SelectedOptions.AUTOMATICALLY_REPLACE_STRANGE_CHARACTERS+"");
                break;
            case "time_limit":
                int durationLimit = Integer.parseInt(sharedPreferences.getString(key,"0"));
                new AsyncSetVisibility(durationLimit).execute();
                //Log.d(key,  sharedPreferences.getString(key,"0"));
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

            //bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("time_limit"));
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
            bindPreferenceSummaryToValue(findPreference("size_album_art"));
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
     * This private class update the item list shown in main activity,
     * according to selected option "Ocultar canciones en la lista"
     */

    private class AsyncSetVisibility extends AsyncTask<Void, AudioItem, Void>{
        DataTrackDbHelper dataTrackDbHelper = DataTrackDbHelper.getInstance(getApplicationContext());
        int _duration;
        AsyncSetVisibility(int duration){
            //The duration is in millis, so we need to convert to seconds
            _duration = duration == 0 ? duration : duration*1000;
        }
        @Override
        protected void onPreExecute(){
            //First we removed all elements from its current item list
            SelectFolderActivity.audioItemArrayAdapterAdapter.clear();
        }

        @Override
        protected Void doInBackground(Void... params) {

                //we set invisible all records with its duration lesser than minimum duration _duration limit
                dataTrackDbHelper.setVisibleAllItems(_duration);

            //The we get all items in database an discard
                Cursor cursor;
                cursor = dataTrackDbHelper.getDataFromDB();
                int dataLength = cursor.getCount(), i = 0;

                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {

                        //if record is invisible, the it means that its duration is lesser than minimuin required, so we  discard it
                        boolean isVisible = cursor.getInt(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_IS_VISIBLE)) == 1;
                        Log.d("NEW AUDIO", isVisible +"   " + cursor.getInt(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_IS_VISIBLE)));
                        if (isVisible){
                            boolean isSelected = cursor.getInt(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_IS_SELECTED)) != 0;
                            String title = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_TITLE)).equals("") ?
                                    "No disponible" : cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_TITLE));
                            String artist = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_ARTIST)).equals("") ?
                                    "No disponible" : cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_ARTIST));
                            String album = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_ALBUM)).equals("") ?
                                    "No disponible" : cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_ALBUM));
                            String filename = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME)).equals("") ?
                                    "No disponible" : cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME));
                            String id = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData._ID));
                            String fullPath = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH));
                            String path = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_CURRENT_PATH));
                            int totalSeconds = Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_DURATION)));
                            String sFilesizeInMb = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_FILE_SIZE));
                            float fFileSizeInMb = Float.parseFloat(sFilesizeInMb);
                            String status = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_STATUS));
                            final AudioItem audioItem = new AudioItem();
                            audioItem.setTitle(title).setArtist(artist).setAlbum(album).setDuration(totalSeconds).setHumanReadableDuration(AudioItem.getHumanReadableDuration(totalSeconds)).setId(Long.parseLong(id)).setNewAbsolutePath(fullPath).setPosition(i).setStatus(Integer.parseInt(status)).setFileName(filename).setSize(fFileSizeInMb).setVisible(true).setPath(path).setSelected(isSelected);
                            totalSeconds = 0;

                            publishProgress(audioItem);
                            i++;
                        }
                    }
                    cursor.close();
                }

            return null;
        }

        @Override
        protected void onProgressUpdate(AudioItem... audioItems){
            super.onProgressUpdate(audioItems);
            Log.d("ITEM ADDED",audioItems[0].getFileName());
            //we add the record to item list here, because this method runs on UI thread,
            //meaning that can be changes without any error.
            SelectFolderActivity.audioItemArrayAdapterAdapter.add(audioItems[0]);
            SelectFolderActivity.audioItemArrayAdapterAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void voids){
            Log.d("COUNT",SelectFolderActivity.audioItemArrayAdapterAdapter.getCount()+"");
        }

    }
}
