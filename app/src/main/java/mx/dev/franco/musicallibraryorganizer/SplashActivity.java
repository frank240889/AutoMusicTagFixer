package mx.dev.franco.musicallibraryorganizer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.Set;

import mx.dev.franco.musicallibraryorganizer.services.DetectorInternetConnection;
import mx.dev.franco.musicallibraryorganizer.services.GnService;
import mx.dev.franco.musicallibraryorganizer.services.Job;
import mx.dev.franco.musicallibraryorganizer.utilities.RequiredPermissions;

/**
 * Created by franco on 6/11/16.
 */

public class SplashActivity extends AppCompatActivity{
    //SharedPreferences we use for saving data from app, this data is only accessible for this app.
    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor editor;
    public static final String APP_SHARED_PREFERENCES = "AutoMusicTagFixer";
    public static boolean DEBUG_MODE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //We check first if there is internet connection for initialize api
        boolean isConnected = DetectorInternetConnection.isConnected(getApplicationContext());
        if(isConnected) {
            //We set context and initialize the GNSDK API.
            GnService.setAppContext(this);
            GnService.initializeAPI(GnService.API_INITIALIZED_FROM_SPLASH);

        }
        //No internet connection, then we schedule the job for initialize API
        //when internet connection restores it
        else {
            Job.scheduleJob(getApplicationContext());
        }


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //We set the default values of settings in case the user had not modified them or first use
        SelectedOptions.AUTOMATIC_CHANGE_FILENAME = preferences.getBoolean("title_service_change_switch", true);
        SelectedOptions.MANUAL_CHANGE_FILE = preferences.getBoolean("title_manual_change_switch", true);
        SelectedOptions.AUTOMATICALLY_REPLACE_STRANGE_CHARACTERS = preferences.getBoolean("title_automatically_replace_strange_chars",true);
        SelectedOptions.ALL_SELECTED = preferences.getBoolean("allSelected",false);
        SelectedOptions.SHOW_SEPARATORS = preferences.getBoolean("show_separators",true);

        //Retrieve the string value saved in shared preferences first
        String imageSizeSaved = preferences.getString("size_album_art","1000");
        SelectedOptions.ALBUM_ART_SIZE = SelectedOptions.setValueImageSize(imageSizeSaved);

        Set<String> stringSet = preferences.getStringSet("data_to_download",null);
        SelectedOptions.setValuesExtraDataToDownload(stringSet);

        //We set if is the first use of app
        sharedPreferences = getSharedPreferences(APP_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        boolean firstTime= sharedPreferences.getBoolean("first", true);
        SelectFolderActivity.isProcessingTask = sharedPreferences.getBoolean(SelectFolderActivity.IS_PROCESSING_TASK,false);
        RequiredPermissions.ACCESS_GRANTED_FILES = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if(!DEBUG_MODE) {

            if (firstTime) {
                //Is first app use
                Intent intent = new Intent(this, ScreenSlidePagerActivity.class);
                startActivity(intent);
            } else {

                Intent intent = new Intent(this, SelectFolderActivity.class);
                startActivity(intent);

            }

        }
        else {
            Intent intent = new Intent(this, ScreenSlidePagerActivity.class);
            startActivity(intent);
        }
        finish();
    }
}
