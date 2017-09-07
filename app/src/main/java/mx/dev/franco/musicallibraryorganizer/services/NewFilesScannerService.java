package mx.dev.franco.musicallibraryorganizer.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import mx.dev.franco.musicallibraryorganizer.R;

/**
 * Created by franco on 18/04/17.
 */

public class NewFilesScannerService extends Service {
    private final static String TAG = NewFilesScannerService.class.getName();
    private final IBinder serviceBinder = new BinderService();

    @Override
    public void onCreate(){
        super.onCreate();
        startForeground(R.string.app_name, new Notification());
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent,flags,startId);
        Toast.makeText(getApplicationContext(), "service starting", Toast.LENGTH_SHORT).show();
        return START_NOT_STICKY;
    }



    public class BinderService extends Binder{
        public NewFilesScannerService getService(){
            return NewFilesScannerService.this;
        }
    }

}
