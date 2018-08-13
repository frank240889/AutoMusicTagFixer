package mx.dev.franco.automusictagfixer.UI;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.UI.intro.IntroActivity;
import mx.dev.franco.automusictagfixer.UI.main.MainActivity;
import mx.dev.franco.automusictagfixer.network.ConnectivityDetector;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.Settings;

/**
 * Created by franco on 6/11/16.
 */

public class SplashActivity extends AppCompatActivity{
    @Inject
    public ConnectivityDetector connectivityDetector;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        AutoMusicTagFixer.getContextComponent().inject(this);
        connectivityDetector.onStartTestingNetwork();


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //Get default or saved values of settings
        Settings.SETTING_RENAME_FILE_AUTOMATIC_MODE = preferences.getBoolean("key_rename_file_automatic_mode", true);
        Settings.SETTING_RENAME_FILE_MANUAL_MODE = preferences.getBoolean("key_rename_file_manual_mode", true);
        Settings.SETTING_REPLACE_STRANGE_CHARS_MANUAL_MODE = preferences.getBoolean("key_replace_strange_chars_manual_mode",true);
        Settings.ALL_CHECKED = preferences.getBoolean(Constants.ALL_ITEMS_CHECKED,false);
        Settings.SETTING_OVERWRITE_ALL_TAGS_AUTOMATIC_MODE = preferences.getBoolean("key_overwrite_all_tags_automatic_mode", true);
        Settings.SETTING_RENAME_FILE_SEMI_AUTOMATIC_MODE = preferences.getBoolean("key_rename_file_semi_automatic_mode", true);
        Settings.SETTING_USE_EMBED_PLAYER = preferences.getBoolean("key_use_embed_player",true);
        Settings.BACKGROUND_CORRECTION = preferences.getBoolean("key_background_service", true);
        String imageSizeSaved = preferences.getString("key_size_album_art","1000");
        Settings.SETTING_SIZE_ALBUM_ART = Settings.setValueImageSize(imageSizeSaved);
        Settings.SETTING_AUTO_UPDATE_LIST = preferences.getBoolean("key_auto_update_list", false);
        Settings.ENABLE_SD_CARD_ACCESS = preferences.getBoolean("key_enable_sd_card_access", false);




        //Is first use of app?
        SharedPreferences preferences2 =  getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
        String uriString = preferences2.getString(Constants.URI_TREE, null);
        Constants.URI_SD_CARD = uriString != null ? Uri.parse(uriString) : null;
        boolean firstTime  = preferences2.getBoolean("first", true);
        Settings.SETTING_SORT = preferences2.getString(Constants.SORT_KEY, null);
        //do we have permission to access files?
        RequiredPermissions.ACCESS_GRANTED_FILES = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        Intent intent;
            if (firstTime) {
                //Is first app use
                //Intent intent = new Intent(this, ScreenSlidePagerActivity.class);
                intent = new Intent(this, IntroActivity.class);
            }
            else {
                intent = new Intent(this, MainActivity.class);
            }

        finishAfterTransition();
        startActivity(intent);
    }

    @Override
    public void onDestroy(){
        connectivityDetector = null;
        super.onDestroy();
    }


}
