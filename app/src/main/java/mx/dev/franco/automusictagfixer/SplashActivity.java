package mx.dev.franco.automusictagfixer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import mx.dev.franco.automusictagfixer.services.DetectorInternetConnection;
import mx.dev.franco.automusictagfixer.services.GnService;
import mx.dev.franco.automusictagfixer.services.Job;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.Settings;

/**
 * Created by franco on 6/11/16.
 */

public class SplashActivity extends AppCompatActivity{

    public static final String APP_SHARED_PREFERENCES = "AutoMusicTagFixer";
    public static boolean DEBUG_MODE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //We check first if there is internet connection for initialize api
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isConnected = DetectorInternetConnection.isConnected(getApplicationContext());
                if(isConnected) {
                    //We set context and initialize the GNSDK API.
                    GnService.withContext(getApplicationContext()).initializeAPI(GnService.API_INITIALIZED_FROM_SPLASH);
                }
                //No internet connection, then we schedule the job for initialize API
                //when internet connection restores it
                else {
                    Job.scheduleJob(getApplicationContext());
                }
                Thread.interrupted();
            }
        }).start();



        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //Get default or saved values of settings
        Settings.SETTING_RENAME_FILE_AUTOMATIC_MODE = preferences.getBoolean("key_rename_file_automatic_mode", true);
        Settings.SETTING_RENAME_FILE_MANUAL_MODE = preferences.getBoolean("key_rename_file_manual_mode", true);
        Settings.SETTING_REPLACE_STRANGE_CHARS_MANUAL_MODE = preferences.getBoolean("key_replace_strange_chars_manual_mode",true);
        Settings.ALL_SELECTED = preferences.getBoolean("allSelected",false);
        Settings.SETTING_SORT = preferences.getInt("key_default_sort", 0);
        Settings.SETTING_OVERWRITE_ALL_TAGS_AUTOMATIC_MODE = preferences.getBoolean("key_overwrite_all_tags_automatic_mode", true);
        Settings.SETTING_RENAME_FILE_SEMI_AUTOMATIC_MODE = preferences.getBoolean("key_rename_file_semi_automatic_mode", true);
        Settings.SETTING_USE_EMBED_PLAYER = preferences.getBoolean("key_use_embed_player",true);
        Settings.BACKGROUND_CORRECTION = preferences.getBoolean("key_background_service", true);
        String imageSizeSaved = preferences.getString("key_size_album_art","1000");
        Settings.SETTING_SIZE_ALBUM_ART = Settings.setValueImageSize(imageSizeSaved);


        preferences = null;

        //Is first use of app?
        preferences =  getSharedPreferences(APP_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        boolean firstTime= preferences.getBoolean("first", true);
        //do we have permission to access files?
        RequiredPermissions.ACCESS_GRANTED_FILES = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if(!DEBUG_MODE) {

            if (firstTime) {
                //Is first app use
                Intent intent = new Intent(this, ScreenSlidePagerActivity.class);
                startActivity(intent);
            } else {

                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);

            }

        }
        else {
            Intent intent = new Intent(this, ScreenSlidePagerActivity.class);
            startActivity(intent);
        }
        preferences = null;
        finish();
    }
}
