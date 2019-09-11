package mx.dev.franco.automusictagfixer.identifier;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.List;

import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class ApiInitializerService extends Service {
    public ApiInitializerService() {
        super();
    }

    @Override
    public void onCreate() {
        GnApiService.init(this);
        Thread thread = new Thread(() -> {
            GnApiService.getInstance().initializeAPI();
            Handler handler = new Handler(Looper.getMainLooper());
            //identity();
            handler.post(this::identity);
        });
        thread.start();
    }

    private void identity() {
        //InputStream inputStream = getAssets().open("6_AM.mp3");
        //File file = new File("6AM.mp3");
        //writeBytesToFile(inputStream, file);
        //String abs = file.getAbsolutePath();
        IdentifierFactory factory = new IdentifierFactory(GnApiService.getInstance(), null);
        Identifier<Track, List<GnIdentifier.IdentificationResults>> identifier = factory.create(IdentifierFactory.FINGERPRINT_IDENTIFIER);
        Track track = new Track("Me faltas tu", "Los temerarios", "", "/storage/emulated/0/Download/#Selfie(Hottest Hits Ever).mp3");
        identifier.registerCallback(new Identifier.IdentificationListener<List<GnIdentifier.IdentificationResults>, Track>() {
            @Override
            public void onIdentificationStart(Track file) {

            }

            @Override
            public void onIdentificationFinished(List<GnIdentifier.IdentificationResults> result) {
                Log.d("Results", result.size() +"");
            }

            @Override
            public void onIdentificationError(Track file, String error) {

            }

            @Override
            public void onIdentificationCancelled(Track file) {

            }

            @Override
            public void onIdentificationNotFound(Track file) {

            }
        });
        identifier.identify(track);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {

    }

}
