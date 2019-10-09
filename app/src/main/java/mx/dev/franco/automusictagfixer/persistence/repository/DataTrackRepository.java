package mx.dev.franco.automusictagfixer.persistence.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.UI.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.UI.track_detail.ImageWrapper;
import mx.dev.franco.automusictagfixer.fixer.AbstractMetadataFixer;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.MetadataReader;
import mx.dev.franco.automusictagfixer.fixer.MetadataReaderResult;
import mx.dev.franco.automusictagfixer.fixer.TrackInformationLoader;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.utilities.Resource;

public class DataTrackRepository {
    private Track mTrack;
    private AbstractMetadataFixer<Void, Void, AudioTagger.AudioFields> mMetadataReader;
    private AudioMetadataTagger mMetadataTagger;
    private TrackInformationLoader mTrackInformationLoader;
    private TrackRoomDatabase mTrackRoomDatabase;
    private SingleLiveEvent<Resource<MetadataReaderResult>> mMetadataReaderResultLiveData;
    private MutableLiveData<Boolean> mLoadingStateLiveData = new MutableLiveData<>();

    @Inject
    public DataTrackRepository(AudioMetadataTagger fileTagger, TrackRoomDatabase trackRoomDatabase) {
        mMetadataTagger = fileTagger;
        mTrackRoomDatabase = trackRoomDatabase;
    }

    public LiveData<Resource<MetadataReaderResult>> getResultReader() {
        return mMetadataReaderResultLiveData;
    }

    public MutableLiveData<Boolean> observeLoadingState() {
        return mLoadingStateLiveData;
    }

    public void loadDataTrack(int trackId) {
        mTrackInformationLoader = new TrackInformationLoader(new AsyncOperation<Void, List<Track>, Void, Void>() {

            @Override
            public void onAsyncOperationStarted(Void params) {
                mLoadingStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(List<Track> result) {
                mLoadingStateLiveData.setValue(false);
                Track track = result.get(0);
                mTrack = track;
                readTrackInformation(track);
            }

        }, mTrackRoomDatabase);
        mTrackInformationLoader.executeOnExecutor(Executors.newCachedThreadPool(), trackId);
    }

    public void removeCover(int mTrackId) {

    }

    public void changeCover(ImageWrapper imageWrapper) {

    }

    public Track getTrack() {
        return new Track(mTrack);
    }

    private void readTrackInformation(Track track) {
        mMetadataReader = new MetadataReader(new AsyncOperation<Track, MetadataReaderResult, Track, MetadataReaderResult>() {
            @Override
            public void onAsyncOperationStarted(Track params) {
                mLoadingStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(MetadataReaderResult result) {
                mMetadataReaderResultLiveData.setValue(Resource.success(result));
                mLoadingStateLiveData.setValue(false);
            }

            @Override
            public void onAsyncOperationError(MetadataReaderResult error) {
                mMetadataReaderResultLiveData.setValue(Resource.error(error));
                mLoadingStateLiveData.setValue(false);
            }
        }, mMetadataTagger, track);

        mMetadataReader.executeOnExecutor(Executors.newCachedThreadPool());
    }

    public void fixTrack(AudioMetadataTagger.InputParams inputParams) {

    }
}
