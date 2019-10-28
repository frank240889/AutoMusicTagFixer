package mx.dev.franco.automusictagfixer.persistence.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import org.jaudiotagger.tag.FieldKey;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.fixer.AbstractMetadataFixer;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.FileRenamer;
import mx.dev.franco.automusictagfixer.fixer.MetadataReader;
import mx.dev.franco.automusictagfixer.fixer.MetadataReaderResult;
import mx.dev.franco.automusictagfixer.fixer.MetadataWriter;
import mx.dev.franco.automusictagfixer.fixer.MetadataWriterResult;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.cache.IdentificationResultsCache;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackUpdater;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.ui.trackdetail.InputCorrectionParams;
import mx.dev.franco.automusictagfixer.utilities.Resource;

public class DataTrackManager {
    //The information of track.
    private Track mTrack;
    //Helper object to read metadata asynchronously.
    private AbstractMetadataFixer<Void, Void, AudioTagger.AudioFields> mMetadataReader;
    //Helper object to write metadata asynchronously.
    private AbstractMetadataFixer<Context, Void, AudioTagger.ResultCorrection> mMetadataWriter;
    //Helper object rename file asynchronously.
    private AbstractMetadataFixer<Void, Void, AudioTagger.ResultRename> mFileRenamer;
    //Interface to rename audio files and read/write their metadata.
    private AudioMetadataTagger mMetadataTagger;
    //The database where is temporally stored the information about tracks.
    private TrackRoomDatabase mTrackRoomDatabase;
    //Live data objects that only dispatch a state change when its method "setVlue()" is called explicitly.
    private SingleLiveEvent<Resource<MetadataReaderResult>> mMetadataReaderResultLiveData;
    private SingleLiveEvent<Resource<MetadataWriterResult>> mMetadataWriterResultLiveData;
    private SingleLiveEvent<Resource<AudioTagger.ResultRename>> mFileRenamerLiveData;
    //Live data to inform the progress of task.
    private SingleLiveEvent<Boolean> mLoadingStateLiveData;
    //The cache where are stored temporally the identification results.
    private Cache<String, List<Identifier.IdentificationResults>> mResultsCache;
    private MediatorLiveData<Track> mMediatorLiveDataTrack = new MediatorLiveData<>();
    /**
     * The context required by {@link #mMetadataWriter}
     */
    private Context mContext;

    /**
     * Inject all dependencies into constructor.
     * @param fileTagger Interface to rename audio files and read/write their metadata.
     * @param trackRoomDatabase
     * @param cache
     * @param context
     */
    @Inject
    public DataTrackManager(@NonNull AudioMetadataTagger fileTagger,
                            @NonNull TrackRoomDatabase trackRoomDatabase,
                            @NonNull IdentificationResultsCache cache,
                            @NonNull Context context) {
        mMetadataTagger = fileTagger;
        mTrackRoomDatabase = trackRoomDatabase;
        mResultsCache = cache;
        mContext = context;

        mMetadataReaderResultLiveData = new SingleLiveEvent<>();
        mMetadataWriterResultLiveData = new SingleLiveEvent<>();
        mFileRenamerLiveData = new SingleLiveEvent<>();
        mLoadingStateLiveData = new SingleLiveEvent<>();
    }

    public void setId(int id) {
        mLoadingStateLiveData.setValue(true);
        LiveData<Track> liveTrack = mTrackRoomDatabase.trackDao().search(id);
        mMediatorLiveDataTrack.addSource(liveTrack, track -> {
            mTrack = track;
            mLoadingStateLiveData.setValue(false);
            mMediatorLiveDataTrack.setValue(track);
        });
    }

    public LiveData<Track> observeTrack() {
        return mMediatorLiveDataTrack;
    }

    public LiveData<Resource<MetadataReaderResult>> getResultReader() {
        return mMetadataReaderResultLiveData;
    }

    public LiveData<Resource<MetadataWriterResult>> getResultWriter() {
        return mMetadataWriterResultLiveData;
    }

    public LiveData<Resource<AudioTagger.ResultRename>> getResultRename() {
        return mFileRenamerLiveData;
    }

    public LiveData<Boolean> observeLoadingState() {
        return mLoadingStateLiveData;
    }

    public void readAudioFile(Track track) {
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

        mMetadataReader.executeOnExecutor(AutoMusicTagFixer.getExecutorService());
    }

    /**
     * Renames the audio file.
     * @param correctionParams The params required by {@link AudioTagger}
     */
    public void renameFile(AudioMetadataTagger.InputParams correctionParams) {

        mFileRenamer = new FileRenamer(new AsyncOperation<Track, AudioTagger.ResultRename, Track, AudioTagger.ResultRename>() {
            @Override
            public void onAsyncOperationStarted(Track params) {
                mLoadingStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(AudioTagger.ResultRename result) {
                mLoadingStateLiveData.setValue(false);
                mFileRenamerLiveData.setValue(Resource.success(result));
            }

            @Override
            public void onAsyncOperationCancelled(Track cancellation) {
                mLoadingStateLiveData.setValue(false);
            }

            @Override
            public void onAsyncOperationError(AudioTagger.ResultRename error) {
                mLoadingStateLiveData.setValue(false);
                mFileRenamerLiveData.setValue(Resource.error(error));
            }
        }, mMetadataTagger, mTrack, correctionParams.getNewName());

        mFileRenamer.executeOnExecutor(AutoMusicTagFixer.getExecutorService());
    }

    /**
     * Called when ViewModel finishes, releasing the cache of results.
     */
    public void onCleared() {
        mResultsCache.deleteAll();
    }

    /**
     * Exposes a public method to make the correction.
     * @param correctionParams The params required by {@link AudioTagger}
     */
    public void performCorrection(InputCorrectionParams correctionParams) {
        correctionParams.setTargetFile(correctionParams.getTargetFile());
        mMetadataWriter = new MetadataWriter(new AsyncOperation<Track, MetadataWriterResult, Track, MetadataWriterResult>() {
            @Override
            public void onAsyncOperationStarted(Track params) {
                mLoadingStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(MetadataWriterResult result) {
                mLoadingStateLiveData.setValue(false);
                mMetadataWriterResultLiveData.setValue(Resource.success(result));
            }

            @Override
            public void onAsyncOperationCancelled(Track cancellation) {
                mLoadingStateLiveData.setValue(false);
            }

            @Override
            public void onAsyncOperationError(MetadataWriterResult error) {
                mLoadingStateLiveData.setValue(false);
                mMetadataWriterResultLiveData.setValue(Resource.error(error));
            }
        }, mMetadataTagger, correctionParams, mTrack);
        mMetadataWriter.executeOnExecutor(AutoMusicTagFixer.getExecutorService(), mContext);
    }


    public void removeCover(InputCorrectionParams inputParams) {
        inputParams.setRenameFile(false);
        performCorrection(inputParams);
    }

    public void updateTrack(Map<FieldKey, Object> tags) {
        String title = (String) tags.get(FieldKey.TITLE);
        String artist = (String) tags.get(FieldKey.ARTIST);
        String album = (String) tags.get(FieldKey.ALBUM);
        String path = (String) tags.get(FieldKey.CUSTOM1);

        if (title != null && !title.isEmpty()) {
            mTrack.setTitle(title);
        }
        if (artist != null && !artist.isEmpty()) {
            mTrack.setArtist(artist);
        }
        if (album != null && !album.isEmpty()) {
            mTrack.setAlbum(album);
        }

        if(path != null && !path.equals(""))
            mTrack.setPath(path);

        new TrackUpdater(mTrackRoomDatabase.trackDao()).executeOnExecutor(AutoMusicTagFixer.getExecutorService(),mTrack);
    }

    public void updateTrack(Track track) {
        new TrackUpdater(mTrackRoomDatabase.trackDao()).executeOnExecutor(AutoMusicTagFixer.getExecutorService(),track);
    }
}
