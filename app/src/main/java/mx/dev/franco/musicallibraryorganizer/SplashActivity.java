package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;

/**
 * Created by franco on 6/11/16.
 */

public class SplashActivity extends AppCompatActivity{
    //SharedPreferences lo utilizmos para guardar datos de la aplicacion, por ejemplo, para guardar la primer apertura, solo son datos accesibles por la app
    protected static SharedPreferences sharedPreferences;
    protected static SharedPreferences.Editor editor;
    protected static boolean existDatabase = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("ShaPreferences", Context.MODE_PRIVATE);
        boolean firstTime= sharedPreferences.getBoolean("first", true);

        if(firstTime) {
            Log.d("EXISTE DB","false");
            Intent intent = new Intent(this, ScreenSlidePagerActivity.class);
            startActivity(intent);
        }
        else
        {
            Log.d("EXISTE DB","true");
            File db = getApplicationContext().getDatabasePath(DataTrackDbHelper.DATABASE_NAME);

            if(db.exists()){
                existDatabase = true;
            }

            /*new Thread(new Runnable() {
                @Override
                public void run() {
                    Intent mserviceIntent = new Intent(SplashActivity.this, NewFilesScannerService.class);
                    //mserviceIntent.setData("Hola");
                    getApplicationContext().startService(mserviceIntent);
                }
            }).start();*/

            Intent intent = new Intent(this, SelectFolderActivity.class);
            startActivity(intent);

        }
        finish();
    }

}
