package mx.dev.franco.automusictagfixer.persistence.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.jaudiotagger.tag.FieldKey;

import java.util.Map;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.covermanager.CoverLoader;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams;
import mx.dev.franco.automusictagfixer.fixer.TrackReader;
import mx.dev.franco.automusictagfixer.fixer.TrackWriter;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackUpdater;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;

public class TrackManager {
    private final AudioTagger mAudioTagger;
    //The database where is temporally stored the information about tracks.
    private TrackRoomDatabase mTrackRoomDatabase;
    private Context mContext;
    //Live data objects that only dispatch a state change when its method "setVlue()" is called explicitly.
    private MutableLiveData<AudioTagger.AudioFields> mReaderResult = new MutableLiveData<>();
    private SingleLiveEvent<AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>> mWriterResult = new SingleLiveEvent<>();
    private SingleLiveEvent<AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>> mWriterError = new SingleLiveEvent<>();
    private SingleLiveEvent<Integer> mLiveMessage = new SingleLiveEvent<>();
    private MediatorLiveData<Track> mMediatorLiveDataTrack = new MediatorLiveData<>();
    //Live data to inform the progress of task.
    private MutableLiveData<Boolean> mLoadingStateLiveData = new SingleLiveEvent<>();
    //The cache where are stored temporally the identification results.
    private TrackReader mTrackReader;
    private TrackWriter mTrackWriter;
    /**
     * Inject all dependencies into constructor.
     * @param audioTagger Interface to rename audio files and read/write their metadata.
     * @param trackRoomDatabase Database where is stored the base info of tracks.
     */
    @Inject
    public TrackManager(@NonNull TrackRoomDatabase trackRoomDatabase,
                        @NonNull AudioTagger audioTagger,
                        @NonNull Context context) {

        mTrackRoomDatabase = trackRoomDatabase;
        mAudioTagger = audioTagger;
        mContext = context;
    }

    public void getDetails(int id) {
        mMediatorLiveDataTrack.addSource(mTrackRoomDatabase.trackDao().search(id), track -> {
            mMediatorLiveDataTrack.setValue(track);
        });
    }

    public LiveData<Track> observeTrack() {
        return mMediatorLiveDataTrack;
    }

    public LiveData<Boolean> observeLoadingState() {
        return mLoadingStateLiveData;
    }

    public LiveData<AudioTagger.AudioFields> observeReadingResult() {
        return mReaderResult;
    }

    public LiveData<AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>> observeWritingResult() {
        return mWriterResult;
    }

    public LiveData<AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>> observeErrorWriting() {
        return mWriterError;
    }

    public LiveData<Integer> observeMessage() {
        return mLiveMessage;
    }

    public void readAudioFile(Track track) {
        mTrackReader = new TrackReader(mAudioTagger, new AsyncOperation<Void, AudioTagger.AudioTaggerResult, Void, AudioTagger.AudioTaggerResult>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mLoadingStateLiveData.setValue(true);
                mLiveMessage.setValue(R.string.loading_data_track);
            }

            @Override
            public void onAsyncOperationFinished(AudioTagger.AudioTaggerResult result) {
                mLoadingStateLiveData.setValue(false);
                mReaderResult.setValue((AudioTagger.AudioFields) result);
            }

            @Override
            public void onAsyncOperationError(AudioTagger.AudioTaggerResult error) {
                mLoadingStateLiveData.setValue(false);
                mReaderResult.setValue((AudioTagger.AudioFields) error);
            }
        });
        mTrackReader.executeOnExecutor(Executors.newSingleThreadExecutor(), track.getPath());
    }

    /**
     * Exposes a public method to make the correction.
     * @param correctionParams The params required by {@link AudioTagger}
     */
    public void performCorrection(CorrectionParams correctionParams) {
        correctionParams.setMediaStoreId(getCurrentTrack().getMediaStoreId()+"");
        mTrackWriter = new TrackWriter(new AsyncOperation<Void, AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>, Void, AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mLoadingStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(AudioTagger.AudioTaggerResult<Map<FieldKey, Object>> result) {
                mLoadingStateLiveData.setValue(false);
                mWriterResult.setValue(result);
                updateTrack(result, getCurrentTrack(), mTrackRoomDatabase.trackDao());
            }

            @Override
            public void onAsyncOperationError(AudioTagger.AudioTaggerResult<Map<FieldKey, Object>> error) {
                mWriterError.setValue(error);
                mLoadingStateLiveData.setValue(false);
            }
        }, mAudioTagger, correctionParams);
        mTrackWriter.executeOnExecutor(Executors.newSingleThreadExecutor(), mContext);
    }

    public Track getCurrentTrack() {
        return mMediatorLiveDataTrack.getValue();
    }

    public void updateTrack(AudioTagger.AudioTaggerResult<Map<FieldKey, Object>> result, Track track, TrackDAO trackDAO) {
        Map<FieldKey, Object> tags = result.getData();

        if (tags != null) {
            String title = (String) tags.get(FieldKey.TITLE);
            String artist = (String) tags.get(FieldKey.ARTIST);
            String album = (String) tags.get(FieldKey.ALBUM);
            if (title != null && !title.isEmpty()) {
                track.setTitle(title);
            }

            if (artist != null && !artist.isEmpty()) {
                track.setArtist(artist);
            }

            if (album != null && !album.isEmpty()) {
                track.setAlbum(album);
            }

        }

        String path = ((AudioTagger.ResultCorrection)result).getResultRename();
        if(path != null && !path.equals("")) {
            CoverLoader.removeCover(track.getMediaStoreId()+"");
            track.setPath(path);
        }

        track.setChecked(0);
        track.setProcessing(0);
        track.setVersion(track.getVersion()+1);

        new TrackUpdater(trackDAO).executeOnExecutor(AutoMusicTagFixer.getExecutorService(),track);
    }
}
