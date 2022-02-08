package mx.dev.franco.automusictagfixer.persistence.mediastore

import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import mx.dev.franco.automusictagfixer.persistence.room.Track
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject

/**
 * This class manages some operations to MediaStore.
 */
class MediaStoreAccess
@Inject
constructor(
    private val context: Context
    ) {

    init {
        /*dataSetObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri) {
                fetchAudioFiles()
            }
        }*/
    }

    /**
     * Updates the data of media store file.
     * @param path The path of the file to scan by mediastore.
     */
    fun addFileToMediaStore(path: String?, onScanCompletedListener: MediaScannerConnection.OnScanCompletedListener?) {
        MediaStoreHelper.addFileToMediaStore(path, context, onScanCompletedListener)
    }

    /*fun registerMediaContentObserver() {
        //Select all music
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

        //Columns to retrieve
        val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.AlbumColumns.ALBUM,
                MediaStore.Audio.Media.DATA // absolute path to audio file
        )
        dataset = mContext.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null)
        dataset!!.registerContentObserver(dataSetObserver)
    }

    fun unregisterMediaContentObserver() {
        dataset!!.unregisterContentObserver(dataSetObserver)
    }*/

    /**
     * Fetch files from media store.
     */
    suspend fun fetchAudioFiles() = run {
        val cursor = MediaStoreHelper.getAllSupportedAudioFiles(context)
        val tracks: MutableList<Track> = ArrayList()
        if (cursor.count > 0) {
            while (cursor.moveToNext()) {
                val track = buildTrack(cursor)
                tracks.add(track)
            }
        }
        tracks
    }

    /**
     * Builds a Track object from cursor input
     * @param cursor The iterable data source
     * @return A Track object
     */
    private fun buildTrack(cursor: Cursor): Track {
        //mediastore id
        val mediaStoreId = cursor.getInt(0)
        var title: String? = null
        title = String(cursor.getString(1).toByteArray(), StandardCharsets.UTF_8)
        var artist: String? = null
        artist = String(cursor.getString(2).toByteArray(), StandardCharsets.UTF_8)
        var album: String? = null
        album = String(cursor.getString(3).toByteArray(), StandardCharsets.UTF_8)
        //MediaStore.Audio.Media.DATA column is the path of file
        val fullPath = Uri.parse(cursor.getString(4)).toString()
        val track = Track(title, artist, album, fullPath, 0)
        track.mediaStoreId = mediaStoreId
        return track
    }

    /*private fun removeInexistentTracks() {
        val tracksToRemove: MutableList<Track> = ArrayList()
        val currentTracks: List<Track> = mTrackDao.getTracks()
        if (currentTracks == null || currentTracks.size == 0) return
        for (track in currentTracks) {
            val file = File(track.path)
            if (!file.exists()) {
                tracksToRemove.add(track)
            }
        }
        if (tracksToRemove.size > 0) mTrackDao.deleteBatch(tracksToRemove)
    }*/
}