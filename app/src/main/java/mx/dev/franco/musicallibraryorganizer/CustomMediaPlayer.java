package mx.dev.franco.musicallibraryorganizer;

import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

/**
 * Created by franco on 29/03/17.
 */

public final class CustomMediaPlayer extends MediaPlayer {
    private static CustomMediaPlayer mediaPlayer;
    private int currentPositionAudioSource=-1;
    private final int PREVIEW_LENGTH = 10;
    private int rangeToPlay;
    private Handler handler = null, myProgressHandler = null;
    private MyTimer timeOut = null;
    private ArrayAdapter<AudioItem> filesAdapter;
    private ProgressBar progressBar;
    private int progressStatus = 0;
    private String lastPlayedPath = "", currentPlayedPath;
    private AudioItem lastAudioItem, currentAudioItem;
    private View lastView, currentView;
    private Thread threadProgressBar;
    private ProgressBarUpdater progressBarUpdater;
    private MyCounter myCounter;
    private Context context;

    private CustomMediaPlayer(){
        super();
    }

    void setParameters(ArrayAdapter<AudioItem> filesAdapter, Context appContext){
        this.setVolume(1f,1f);
        this.filesAdapter = filesAdapter;
        context = appContext;
    }

    static CustomMediaPlayer getInstance(){
        if(mediaPlayer == null){
            mediaPlayer = new CustomMediaPlayer();
        }
        return mediaPlayer;
    }

    /**
     * Play a preview of 10 seconds of the audiofile.
     * @param view A reference to the current view that was pressed in listview
     * @throws IOException
     * @throws InterruptedException
     */

    void playPreview(View view) throws IOException, InterruptedException {
        String trackPath =  (String)(view.findViewById(R.id.path)).getTag();
        SelectFolderActivity selectFolderActivity = ((SelectFolderActivity) context);
        //Not playing any file
            if (!isPlaying()) {


                currentAudioItem = selectFolderActivity.selectItemByIdOrPath(-1,trackPath);
                lastAudioItem =  currentAudioItem;

                currentPlayedPath = trackPath;
                lastPlayedPath = currentPlayedPath;


                if(currentView != lastView){
                    lastView = currentView;
                    currentView =  view;
                }
                else {
                    currentView =  view;
                    lastView = currentView;
                }

                reset();
                setDataSource(currentPlayedPath);
                setWakeMode(context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

                prepare();
                rangeToPlay = (getDuration()/2)-15;
                seekTo(rangeToPlay);
                currentAudioItem.setPlayingAudio(true);

                //Set the timeout for playing only 10 seconds
                handler = new Handler();
                timeOut = new MyTimer();
                handler.postDelayed(timeOut,PREVIEW_LENGTH*1000);
                progressBar = (ProgressBar) currentView.findViewById(R.id.progressBarPlaying);
                progressBar.setVisibility(View.VISIBLE);

                //Set the thread that execute the update of the progressbar
                progressBarUpdater = new ProgressBarUpdater();
                myProgressHandler = new Handler();
                myCounter = new MyCounter();
                threadProgressBar =  new Thread(myCounter);
                threadProgressBar.start();

                //Starts playing the audiofile
                this.start();

                //Notify to the adapter for redrawing the visible items
                filesAdapter.notifyDataSetChanged();
            }
            //Playing an audiofile
            else {
                myCounter.playRunning(false);
                try {
                    handler.removeCallbacks(timeOut);
                    myProgressHandler.removeCallbacks(progressBarUpdater);
                    threadProgressBar.join();
                    handler = null;
                    myProgressHandler = null;
                    myCounter = null;
                    timeOut = null;
                }catch (Exception e){
                    e.printStackTrace();
                }

                lastPlayedPath = currentPlayedPath;
                currentPlayedPath = trackPath;

                //Is the same audiofile?
                if(lastPlayedPath.equals(currentPlayedPath)){
                    currentAudioItem = selectFolderActivity.selectItemByIdOrPath(-1,currentPlayedPath);
                    lastAudioItem = currentAudioItem;

                    lastView = view;
                    currentView =  view;

                    currentAudioItem.setPlayingAudio(false);
                    progressBar = (ProgressBar) currentView.findViewById(R.id.progressBarPlaying);
                    progressBar.setVisibility(View.GONE);
                    progressBar.setProgress(0);
                    stop();
                    return;
                }
                else{
                    lastAudioItem = currentAudioItem;
                    currentAudioItem = selectFolderActivity.selectItemByIdOrPath(-1,currentPlayedPath);

                    lastView = currentView;
                    currentView =  view;

                    lastAudioItem.setPlayingAudio(false);
                    lastView.findViewById(R.id.progressBarPlaying).setVisibility(View.GONE);
                    ((ProgressBar)lastView.findViewById(R.id.progressBarPlaying)).setProgress(0);
                    progressBar = (ProgressBar) currentView.findViewById(R.id.progressBarPlaying);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(0);
                }


                //if is not the same audiofile
                stop();
                reset();

                setDataSource(currentPlayedPath);
                prepare();
                rangeToPlay = (this.getDuration()/2)-15;
                seekTo(this.rangeToPlay);

                currentAudioItem.setPlayingAudio(true);

                handler = new Handler();
                timeOut = new MyTimer();
                handler.postDelayed(timeOut,PREVIEW_LENGTH*1000);

                progressBarUpdater = new ProgressBarUpdater();
                myProgressHandler = new Handler();
                myCounter = new MyCounter();
                threadProgressBar =  new Thread(myCounter);
                threadProgressBar.start();

                this.start();

                filesAdapter.notifyDataSetChanged();
            }
    }


    private class MyTimer implements Runnable {
        @Override
        public void run() {
            currentAudioItem.setPlayingAudio(false);
            CustomMediaPlayer.this.stop();
            CustomMediaPlayer.this.progressBar.setVisibility(View.GONE);
        }
    }

    private class MyCounter implements Runnable{
        private boolean running = true;

            @Override
            public void run() {
                progressStatus = 0;
                Log.d("UPDATING_running",progressStatus+"");
                while(progressStatus < 100 && running){
                    //Se actualiza el progreso de la barra.
                    progressStatus += 1;

                    // Esperamos un segundo para que se vuelva a incrementar el progreso,
                    //lo que nos pemitira ver el avance del preview de la cancion
                    try{
                        Thread.sleep(100);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }


                    Log.d("UPDATING_PROGRESSING_1",progressStatus+"");
                    myProgressHandler.post(progressBarUpdater);
                }
            }

            void playRunning(boolean stop){
                this.running = stop;
            }
    }


    private class ProgressBarUpdater implements Runnable{
        @Override
        public void run() {
            progressBar.setProgress(progressStatus);
        }
    }
}
