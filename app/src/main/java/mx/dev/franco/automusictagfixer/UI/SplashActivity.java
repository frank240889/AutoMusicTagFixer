package mx.dev.franco.automusictagfixer.UI;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.UI.intro.IntroActivity;
import mx.dev.franco.automusictagfixer.UI.main.MainActivity;
import mx.dev.franco.automusictagfixer.network.ConnectivityDetector;
import mx.dev.franco.automusictagfixer.utilities.Constants;
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
        String imageSizeSaved = preferences.getString("key_size_album_art","1000");
        Settings.SETTING_SIZE_ALBUM_ART = Settings.setValueImageSize(imageSizeSaved);

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

    @Override
    public void onDestroy(){
        connectivityDetector = null;
        super.onDestroy();
    }
}
