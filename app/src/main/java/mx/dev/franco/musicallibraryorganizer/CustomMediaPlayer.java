package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by franco on 29/03/17.
 */

public final class CustomMediaPlayer extends MediaPlayer implements MediaPlayer.OnCompletionListener {

    private static CustomMediaPlayer mediaPlayer;
    private int rangeToPlay;
    private ArrayAdapter<AudioItem> filesAdapter;
    private AudioItem currentAudioItem;
    private static long currentId = -1;
    private Context context;

    /**
     * Don't let instantiate this class, we need only one instance,
     * so we use a singleton pattern in order to make this.
     * @param context
     */
    private CustomMediaPlayer(Context context){
        super();
        this.context = context;
        this.setVolume(1f,1f);
        setOnCompletionListener(this);
    }

    void setParameters(ArrayAdapter<AudioItem> filesAdapter){
        if(this.filesAdapter == null) {
            this.filesAdapter = filesAdapter;
        }
    }

    /**
     *
     * @param context
     * @return An unique instance of CustomMediaPlayer.
     */
    static CustomMediaPlayer getInstance(Context context){
        if(mediaPlayer == null){
            mediaPlayer = new CustomMediaPlayer(context.getApplicationContext());
        }
        mediaPlayer.setWakeMode(context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        return mediaPlayer;
    }

    /**
     * Play a preview of audiofile.
     * @param id The current id item that was pressed in listview
     * @throws IOException
     * @throws InterruptedException
     */

    void playPreview(long id) throws IOException, InterruptedException {

        // Was pressed the same item? then stop
        if(currentId == id && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            currentAudioItem.setPlayingAudio(false);
            SelectFolderActivity.audioItemArrayAdapterAdapter.notifyDataSetChanged();
            return;
        }

        // playing any item while pressing another item? then stop previous
        // and then play the new item pressed
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            mediaPlayer.reset();
            currentAudioItem.setPlayingAudio(false);
        }


        currentId = id;

        currentAudioItem = SelectFolderActivity.selectItemByIdOrPath(currentId, "");

        if(currentAudioItem.isProcessing()){
            Toast toast = Toast.makeText(context, context.getString(R.string.snackbar_message_track_is_processing),Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER,0,0);
            toast.show();
            return;
        }
        try{
            Log.d("path", currentAudioItem.getNewAbsolutePath());
        }catch (Exception e){
            e.printStackTrace();
        }

        mediaPlayer.setDataSource(currentAudioItem.getNewAbsolutePath());
        mediaPlayer.prepare();
        if(getDuration() > 30000) { // if audio is not too short
            rangeToPlay = (getDuration() / 2) - 15;
            mediaPlayer.seekTo(rangeToPlay);
        }
        currentAudioItem.setPlayingAudio(true);
        currentAudioItem.setStatusText(context.getString(R.string.snackbar_message_track_preview));
        //listView.getChildAt(currentAudioItem.getPosition()).startAnimation(AnimationUtils.loadAnimation(context.getApplicationContext(), R.anim.blink_playing_preview));
        //listView.getSelectedView().startAnimation(AnimationUtils.loadAnimation(context.getApplicationContext(), R.anim.blink_playing_preview));

        mediaPlayer.start();
        SelectFolderActivity.audioItemArrayAdapterAdapter.notifyDataSetChanged();
    }


    long getCurrentId() {
        return currentId;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("OnCompletion","OnCompletion");
        onCompletePlayback(currentId);
    }

    static void onCompletePlayback(long id){
        mediaPlayer.stop();
        mediaPlayer.reset();
        AudioItem audioItem = SelectFolderActivity.selectItemByIdOrPath(id,"");
        audioItem.setPlayingAudio(false);
        SelectFolderActivity.audioItemArrayAdapterAdapter.notifyDataSetChanged();
    }
}
