package mx.dev.franco.automusictagfixer.audioplayer

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.os.PowerManager
import java.io.File
import java.io.IOException

/**
 * Created by franco on 29/03/17.
 */
class SimpleMediaPlayer
private constructor(
        private val application: Context
) : MediaPlayer(), OnCompletionListener, MediaPlayer.OnErrorListener {

    interface OnMediaPlayerEventListener {
        fun onStartPlaying()
        fun onStopPlaying()
        fun onCompletedPlaying()
        fun onErrorPlaying(what: Int, extra: Int)
    }

    private val listeners: MutableList<OnMediaPlayerEventListener> by lazy {
        ArrayList()
    }

    private var currentPath: String? = null

    /**
     * Don't let instantiate this class, we need only one instance,
     * so we use a singleton.
     */
    init {
        setVolume(1f, 1f)
        setWakeMode(application.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        setOnCompletionListener(this)
        setOnErrorListener(this)
    }

    fun addListener(listener: OnMediaPlayerEventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OnMediaPlayerEventListener) {
        listeners.remove(listener)
    }

    fun removeListeners() {
        listeners.clear()
    }

    /**
     * Play a preview of audiofile.
     * @param path The path of the file to play
     * @throws IOException
     */
    @Throws(IOException::class)
    fun playPreview(path: String?) {
        if (path == null || path == "") return

        val file = File(path)
        if (!file.exists() || !file.canRead() || file.length() == 0L) return

        //Stops current audio
        setDataSource(path)
        prepare()
        start()
        for (listener in listeners) listener.onStartPlaying()
    }

    @Throws(IOException::class)
    fun playPreview() {
        if (currentPath == null || currentPath == "") {
            listeners.forEach {
                //it.onInvalidAudioFile(application.getString(R.string.invalid_audio))
            }
            return
        }
        val file = File(currentPath)
        if (!file.exists() || !file.canRead() || file.length() == 0L) return

        //Stops current audio
        setDataSource(currentPath)
        prepare()
        start()
        for (listener in listeners) listener.onStartPlaying()
    }

    fun setPath(path: String?) {
        currentPath = path
    }

    fun stopPreview() {
        if (isPlaying) {
            stop()
            reset()
            for (listener in listeners) listener.onStopPlaying()
        }
    }

    /**
     * Implementation of completion interface for
     * handling correctly the ends of song if is playing.
     * @param mp
     */
    override fun onCompletion(mp: MediaPlayer) {
        stop()
        reset()
        for (listener in listeners) listener.onCompletedPlaying()
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        for (listener in listeners) listener.onErrorPlaying(what, extra)
        return false
    }

    companion object {
        private var mediaPlayer: SimpleMediaPlayer? = null

        /**
         *
         * @param application The context, needed for access Android resources
         * @return An unique instance of SimpleMediaPlayer.
         */
        @JvmStatic
        fun getInstance(application: Context) = run {
            mediaPlayer?.run {
                SimpleMediaPlayer(application)
            }
        }
    }
}