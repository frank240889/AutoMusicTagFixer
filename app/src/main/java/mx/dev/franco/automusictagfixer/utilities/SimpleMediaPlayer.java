package mx.dev.franco.automusictagfixer.utilities;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.list.TrackAdapter;

/**
 * Created by franco on 29/03/17.
 */

public final class SimpleMediaPlayer extends MediaPlayer implements MediaPlayer.OnCompletionListener {

    private static SimpleMediaPlayer mediaPlayer;
    private AudioItem currentAudioItem;
    private long currentPos = -1;
    private TrackAdapter adapter;

    /**
     * Don't let instantiate this class, we need only one instance,
     * so we use a singleton pattern in order to make this.
     */
    private SimpleMediaPlayer(){
        super();
        this.setVolume(1f,1f);
        setOnCompletionListener(this);
    }

    /**
     *
     * @param context
     * @return An unique instance of SimpleMediaPlayer.
     */
    public static SimpleMediaPlayer getInstance(Context context){
        if(mediaPlayer == null){
            mediaPlayer = new SimpleMediaPlayer();
            mediaPlayer.setWakeMode(context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }

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
     * @param position
     * @throws IOException
     * @throws InterruptedException
     */

    public void playPreview(int position) throws IOException, InterruptedException {
        // Was pressed the same item_list? then stop
        if(currentPos == position && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            currentAudioItem.setPlayingAudio(false);
            return;
        }

        currentPos = position;

        currentAudioItem = adapter.getAudioItemByPosition(position);

        mediaPlayer.setDataSource(currentAudioItem.getAbsolutePath());
        mediaPlayer.prepare();
        currentAudioItem.setPlayingAudio(true);

        mediaPlayer.start();
    }


    /**
     * Get the position from current item_list playing.
     * @return
     */
    public long getCurrentPos() {
        return currentPos;
    }
    /**
     * Implementation of completion interface for
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
     */

    public void onCompletePlayback(){
        currentAudioItem.setPlayingAudio(false);
        adapter.notifyItemChanged(getCurrentPosition());
        mediaPlayer.stop();
        mediaPlayer.reset();
        currentPos = -1;
    }
}
