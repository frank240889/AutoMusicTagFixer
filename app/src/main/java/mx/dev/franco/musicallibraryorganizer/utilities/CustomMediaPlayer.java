package mx.dev.franco.musicallibraryorganizer.utilities;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.io.IOException;

import mx.dev.franco.musicallibraryorganizer.list.AudioItem;
import mx.dev.franco.musicallibraryorganizer.list.TrackAdapter;

/**
 * Created by franco on 29/03/17.
 */

public final class CustomMediaPlayer extends MediaPlayer implements MediaPlayer.OnCompletionListener {

    private static CustomMediaPlayer mediaPlayer;
    private int rangeToPlay;
    private AudioItem currentAudioItem;
    private long currentId = -1;
    private Context context;
    private String currentPath = "";
    private TrackAdapter adapter;

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

    /**
     *
     * @param context
     * @return An unique instance of CustomMediaPlayer.
     */
    public static CustomMediaPlayer getInstance(Context context){
        if(mediaPlayer == null){
            mediaPlayer = new CustomMediaPlayer(context.getApplicationContext());
        }
        mediaPlayer.setWakeMode(context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        return mediaPlayer;
    }

    public void setAdapter(TrackAdapter adapter){
        this.adapter = adapter;
    }

    public RecyclerView.Adapter<TrackAdapter.AudioItemHolder> getAdapter(){
        return this.adapter;
    }

    /**
     * Play a preview of audiofile.
     * @param id The current id item_list that was pressed in listview
     * @throws IOException
     * @throws InterruptedException
     */

    public void playPreview(long id) throws IOException, InterruptedException {


        // Was pressed the same item_list? then stop
        if(currentId == id && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            currentAudioItem.setPlayingAudio(false);
            adapter.notifyItemChanged(currentAudioItem.getPosition());
            currentPath = "";
            return;
        }

        // playing any item_list while pressing another item_list? then stop previous
        // and then play the new item_list pressed
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            mediaPlayer.reset();
            currentAudioItem.setPlayingAudio(false);
            adapter.notifyItemChanged(currentAudioItem.getPosition());
        }


        currentId = id;

        currentAudioItem = adapter.getItemByIdOrPath(currentId, "");
        currentPath = currentAudioItem.getAbsolutePath();

        try{
            Log.d("path", currentAudioItem.getAbsolutePath());
        }catch (Exception e){
            e.printStackTrace();
        }

        mediaPlayer.setDataSource(currentAudioItem.getAbsolutePath());
        mediaPlayer.prepare();
        /*if(getDuration() > 30000) { // if audio is not too short
            rangeToPlay = (getDuration() / 2) - 15;
            mediaPlayer.seekTo(rangeToPlay);
        }*/
        currentAudioItem.setPlayingAudio(true);

        mediaPlayer.start();
        adapter.notifyItemChanged(currentAudioItem.getPosition());
    }


    /**
     * Get the Id from current item_list playing.
     * @return
     */
    public long getCurrentId() {
        return currentId;
    }

    public String getCurrentPath(){
        return currentPath;
    }

    public AudioItem getCurrentAudioItem(){
        return currentAudioItem;
    }

    /**
     * Implementatation of completion interface for
     * handling correctly the ends of song if is playing.
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("OnCompletion","OnCompletion");
        onCompletePlayback();
    }

    /**
     * If song reachs its end, then we stop and reset player
     * for having it ready for next playback, and doesn't throw
     * any error.
     * @param id
     */

    public void onCompletePlayback(){
        currentAudioItem.setPlayingAudio(false);
        adapter.notifyItemChanged(currentAudioItem.getPosition());
        mediaPlayer.stop();
        mediaPlayer.reset();
        currentId = -1;
    }
}
