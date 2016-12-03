package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by franco on 6/11/16.
 */

public class SplashActivity extends AppCompatActivity {
    //SharedPreferences lo utilizmos para guardar datos de la aplicacion, por ejemplo, para guardar la primer apertura, solo son datos accesibles por la app
    protected static SharedPreferences sharedPreferences;
    protected static SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("ShaPreferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        boolean firstTime=sharedPreferences.getBoolean("first", true);
        if(firstTime) {
            editor.putBoolean("first",false);
            editor.apply();
            Intent intent = new Intent(this, ScreenSlidePagerActivity.class);
            startActivity(intent);
        }
        else
        {
            Intent intent = new Intent(this, SelectFolderActivity.class);
            startActivity(intent);
        }
        finish();
    }

}
