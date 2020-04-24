package mx.dev.franco.automusictagfixer.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import mx.dev.franco.automusictagfixer.identifier.ApiInitializerService;
import mx.dev.franco.automusictagfixer.ui.intro.IntroActivity;
import mx.dev.franco.automusictagfixer.ui.main.MainActivity;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Settings;

/**
 * Created by franco on 6/11/16.
 */

public class SplashActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Intent apiIntent = new Intent(this, ApiInitializerService.class);
        startService(apiIntent);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String imageSizeSaved = preferences.getString("key_size_album_art","1000");
        Settings.SETTING_SIZE_ALBUM_ART = Settings.setValueImageSize(imageSizeSaved);
        String language = preferences.getString("key_language","0");
        Settings.SETTING_LANGUAGE = Settings.setValueLanguage(language);

        //Is first use of app?
        preferences =  getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
        boolean firstTime  = preferences.getBoolean("first", true);

        Intent intent;
            if (firstTime) {
                //Is first app use
                intent = new Intent(this, IntroActivity.class);
            }
            else {
                intent = new Intent(this, MainActivity.class);
            }

        finishAfterTransition();
        startActivity(intent);
    }
}
