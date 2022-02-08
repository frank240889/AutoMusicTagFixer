package mx.dev.franco.automusictagfixer.ui.main

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.dev.franco.automusictagfixer.fixer.AudioTagger
import mx.dev.franco.automusictagfixer.fixer.StorageHelper
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreAccess
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository
import mx.dev.franco.automusictagfixer.persistence.room.Track
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent
import mx.dev.franco.automusictagfixer.utilities.Resource
import javax.inject.Inject

class ListViewModel
@Inject
constructor(
    private val trackRepository: TrackRepository,
    private val mMediaStoreAccess: MediaStoreAccess,
    private val storageHelper: StorageHelper
) : ViewModel() {

    val tracks: MediatorLiveData<List<Track>> by lazy {
        MediatorLiveData()
    }

    private val observableInaccessibleTrack: SingleLiveEvent<Track> by lazy {
        SingleLiveEvent()
    }

    private val observableOpenTrackDetails: SingleLiveEvent<Track> by lazy {
        SingleLiveEvent()
    }

    private val loading by lazy {
        MediatorLiveData<Boolean>()
    }

    init {
        tracks.addSource(trackRepository.observeTracks) {
            loading.value = (it.status == Resource.Status.LOADING)
            tracks.value = it.data
        }
    }

    override fun onCleared() {
        tracks.removeSource(trackRepository.observeTracks)
    }


    fun observeIsTrackInaccessible() = observableInaccessibleTrack as LiveData<Track>

    fun observeLoadingState() = loading as LiveData<Boolean>

    fun observeActionCanOpenDetails() = observableOpenTrackDetails as LiveData<Track>

    fun isPresentRemovableStorage() = storageHelper.isPresentRemovableStorage

    /**
     * Removes data of track from DB of app.
     * @param position The track to remove from local DB.
     */
    fun removeTrack(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            trackRepository.delete(track)
        }
    }

    /**
     * Handles the click for items in list.
     * @param viewWrapper A [ViewWrapper] object containing th info if the item.
     */
    fun onItemClick(position: Int) {
        val track = trackRepository.tracks()!![position]
        val isAccessible = AudioTagger.checkFileIntegrity(track.path)
        if (!isAccessible) {
            observableInaccessibleTrack.setValue(track)
        }
        else {
            observableOpenTrackDetails.setValue(track)
        }
    }

    fun fetchAudioFilesFromStorage() {
        loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val trackList = mMediaStoreAccess.fetchAudioFiles()

            if (trackList.isNotEmpty()) {
                trackRepository.addTracks(trackList)
            }
            else {
                tracks.postValue(trackList)
            }
            loading.postValue(false)
        }
    }

    fun sort(sort: TrackRepository.Sort?) {
        trackRepository.sortTracks(sort!!)
    }


    /*fun addMediaObserver() {
        if (mHasAccessStoragePermission) mMediaStoreFetcher.registerMediaContentObserver()
    }

    override fun onCleared() {
        if (mHasAccessStoragePermission) mMediaStoreFetcher.unregisterMediaContentObserver()
    }*/

    companion object {
        private val TAG = ListViewModel::class.java.name
    }
}