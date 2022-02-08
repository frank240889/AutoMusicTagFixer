package mx.dev.franco.automusictagfixer.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.fixer.AudioTagger
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository
import mx.dev.franco.automusictagfixer.persistence.room.Track
import mx.dev.franco.automusictagfixer.ui.main.ViewWrapper
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager
import javax.inject.Inject

class SearchListViewModel
@Inject
constructor(
    private val trackRepository: TrackRepository,
    private val resourceManager: ResourceManager
) : ViewModel() {
    //MutableLiveData objects to respond to user interactions.
    private val mTrack = MutableLiveData<ViewWrapper>()

    //The list of tracks.
    val searchResults: LiveData<List<Track>> = trackRepository.observeResultSearch()
    private val mTrackIsProcessing = MutableLiveData<String>()
    private val mTrackInaccessible = MutableLiveData<ViewWrapper>()
    private val mShowProgress = MutableLiveData<Boolean>()
    fun search(query: String?) {
        if (query == null || query == "") return
        val q = "%$query%"
        trackRepository.trackSearch(q)
    }

    fun onItemClick(viewWrapper: ViewWrapper) {
        val tracks = trackRepository.resultSearchTracks()
        val track = tracks!![viewWrapper.position]
        if (track != null) {
            val isAccessible = AudioTagger.checkFileIntegrity(track.path)
            viewWrapper.track = track
            if (!isAccessible) {
                mTrackInaccessible.setValue(viewWrapper)
            } else if (viewWrapper.track!!.processing() == 1) {
                mTrackIsProcessing.setValue(resourceManager.getString(R.string.current_file_processing))
            } else {
                mTrack.setValue(viewWrapper)
            }
        }
    }

    override fun onCleared() {
        trackRepository.clearResults()
    }

    fun removeTrack(position: Int) {
        //trackRepository.delete(position);
    }

    fun setProgress(showProgress: Boolean) {
        mShowProgress.value = showProgress
    }

    val isTrackProcessing: LiveData<String>
        get() = mTrackIsProcessing

    fun actionTrackEvaluatedSuccessfully(): LiveData<ViewWrapper> {
        return mTrack
    }

    fun actionIsTrackInaccessible(): LiveData<ViewWrapper> {
        return mTrackInaccessible
    }

    fun showProgress(): LiveData<Boolean> {
        return mShowProgress
    }

    fun cancelSearch() = trackRepository.cancelSearch()

    companion object {
        private val TAG = SearchListViewModel::class.java.name
    }

}