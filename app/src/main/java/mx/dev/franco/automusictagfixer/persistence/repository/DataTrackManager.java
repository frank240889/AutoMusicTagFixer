package mx.dev.franco.automusictagfixer.persistence.repository;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.jaudiotagger.tag.FieldKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.fixer.AbstractMetadataFixer;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.FileRenamer;
import mx.dev.franco.automusictagfixer.fixer.MetadataReader;
import mx.dev.franco.automusictagfixer.fixer.MetadataReaderResult;
import mx.dev.franco.automusictagfixer.fixer.MetadataWriter;
import mx.dev.franco.automusictagfixer.fixer.MetadataWriterResult;
import mx.dev.franco.automusictagfixer.fixer.TrackInformationLoader;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.cache.DownloadedTrackDataCacheImpl;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackUpdater;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.ui.trackdetail.ImageWrapper;
import mx.dev.franco.automusictagfixer.ui.trackdetail.InputCorrectionParams;
import mx.dev.franco.automusictagfixer.ui.trackdetail.SemiAutoCorrectionParams;
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
    //Helper object to read from Room Database asynchronously
    private TrackInformationLoader mTrackInformationLoader;
    //The database where is temporally stored the information about tracks.
    private TrackRoomDatabase mTrackRoomDatabase;
    //Live data objects that only dispatch a state change when its method "setVlue()" is called explicitly.
    private SingleLiveEvent<Resource<MetadataReaderResult>> mMetadataReaderResultLiveData;
    private SingleLiveEvent<Resource<MetadataWriterResult>> mMetadataWriterResultLiveData;
    private SingleLiveEvent<Resource<AudioTagger.ResultRename>> mFileRenamerLiveData;
    //Live data to inform the progress of task.
    private MutableLiveData<Boolean> mLoadingStateLiveData;
    //The cache where are stored temporally the identification results.
    private Cache<String, List<Identifier.IdentificationResults>> mResultsCache;
    private LiveData<Track> mLiveTrack;
    private SingleLiveEvent<Track> mSingleLiveEventTrack = new SingleLiveEvent<>();
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
                            @NonNull DownloadedTrackDataCacheImpl cache,
                            @NonNull Context context) {
        mMetadataTagger = fileTagger;
        mTrackRoomDatabase = trackRoomDatabase;
        mResultsCache = cache;
        mContext = context;

        mMetadataReaderResultLiveData = new SingleLiveEvent<>();
        mMetadataWriterResultLiveData = new SingleLiveEvent<>();
        mFileRenamerLiveData = new SingleLiveEvent<>();
        mLoadingStateLiveData = new MutableLiveData<>();
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

    public void loadDataTrack(Track track) {
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
        /*mTrackInformationLoader = new TrackInformationLoader(new AsyncOperation<Void, List<Track>, Void, Void>() {

            @Override
            public void onAsyncOperationStarted(Void params) {
                mLoadingStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(List<Track> result) {
                mLoadingStateLiveData.setValue(false);
                Track track = result.get(0);
                mTrack = track;
                readTrackInformation();
            }

        }, mTrackRoomDatabase);
        mTrackInformationLoader.executeOnExecutor(Executors.newCachedThreadPool(), trackId);*/
    }

    /**
     * Read the metadata from audio track.
     */
    /*private void readTrackInformation() {
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
        }, mMetadataTagger, mTrack);

        mMetadataReader.executeOnExecutor(Executors.newCachedThreadPool());
    }*/

    /**
     * Renames the audio file.
     * @param correctionParams The params required by {@link AudioTagger}
     */
    public void renameFile(AudioMetadataTagger.InputParams correctionParams) {
        SemiAutoCorrectionParams uiInputParams = (SemiAutoCorrectionParams) correctionParams;
        if(uiInputParams.renameFile() && uiInputParams.getNewName().isEmpty())

        mFileRenamer = new FileRenamer(new AsyncOperation<Track, AudioTagger.ResultRename, Track, AudioTagger.ResultRename>() {
            @Override
            public void onAsyncOperationStarted(Track params) {
                mLoadingStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(AudioTagger.ResultRename result) {
                mLoadingStateLiveData.setValue(false);
                mFileRenamerLiveData.setValue(Resource.success(result));
                //loadDataTrack(mTrack.getMediaStoreId());
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
        }, mMetadataTagger, mTrack, uiInputParams.getNewName());

        mFileRenamer.executeOnExecutor(Executors.newCachedThreadPool());
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
                //loadDataTrack(mTrack.getMediaStoreId());
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
        mMetadataWriter.executeOnExecutor(Executors.newCachedThreadPool(), mContext);
    }


    public void removeCover(InputCorrectionParams inputParams) {
        inputParams.setRenameFile(false);
        performCorrection(inputParams);
    }

    public void changeCover(ImageWrapper imageWrapper) {

    }

    public void updateTrack(Map<FieldKey, Object> tags) {
        String title = (String) tags.get(FieldKey.TITLE);
        String artist = (String) tags.get(FieldKey.ARTIST);
        String album = (String) tags.get(FieldKey.ALBUM);

        if (title != null && !title.isEmpty()) {
            mTrack.setTitle(title);
        }
        if (artist != null && !artist.isEmpty()) {
            mTrack.setArtist(artist);
        }
        if (album != null && !album.isEmpty()) {
            mTrack.setAlbum(album);
        }
        new TrackUpdater(mTrackRoomDatabase.trackDao()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,mTrack);
    }

    public void updateTrack(String newPath) {
        mTrack.setPath(newPath);
        new TrackUpdater(mTrackRoomDatabase.trackDao()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,mTrack);
    }
}
