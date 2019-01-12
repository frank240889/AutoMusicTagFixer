package mx.dev.franco.automusictagfixer.modelsUI.track_detail;

import android.content.Context;
import android.os.AsyncTask;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.Fixer;
import mx.dev.franco.automusictagfixer.identifier.GnResponseListener;
import mx.dev.franco.automusictagfixer.identifier.GnService;
import mx.dev.franco.automusictagfixer.identifier.TrackIdentifier;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.interfaces.EditableView;
import mx.dev.franco.automusictagfixer.network.ConnectivityDetector;
import mx.dev.franco.automusictagfixer.persistence.cache.DownloadedTrackDataCacheImpl;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.StringUtilities;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.TrackUtils;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

import static mx.dev.franco.automusictagfixer.UI.track_detail.TrackDetailFragment.INTENT_GET_AND_UPDATE_FROM_GALLERY;

public class TrackDetailPresenter implements
        Destructible, GnResponseListener.GnListener,
        AsyncOperation<Void, TrackDataLoader.TrackDataItem, Void, String>,
        Fixer.OnCorrectionListener,
        AsyncFileSaver.OnSaveListener {
    public enum LifeCycleState {
        RESUMED,
        PAUSED
    }


    private static final String TAG = TrackDetailPresenter.class.getName();
    private LifeCycleState mLifeCycleState;
    private boolean mPendingResultsDelivery = false;
    private EditableView mView;
    private TrackDetailInteractor mInteractor;
    private TrackDataLoader.TrackDataItem mCurrentTrackDataItem;
    private TrackDataLoader.TrackDataItem mLastTrackDataItem;
    //Required by TrackIdentifier
    private Track mCurrentTrack;
    private TrackIdentifier mIdentifier;
    private int mCurrentId;
    private Cache<Integer, GnResponseListener.IdentificationResults> mCache = new DownloadedTrackDataCacheImpl.Builder().build();
    private Fixer mFixer;
    private AsyncFileSaver mFileSaver;
    private int mCorrectionMode = Constants.CorrectionModes.VIEW_INFO;
    private int mRecognition = TrackIdentifier.ALL_TAGS;
    private int mDataFrom;
    private boolean mIsFloatingActionMenuOpen;
    private boolean mIsInEditMode = false;
    private boolean mPendingIdentification = false;
    private byte[] mCurrentCover;
    @Inject
    public ResourceManager resourceManager;
    @Inject
    public ConnectivityDetector connectivityDetector;
    @Inject
    public GnService gnService;
    @Inject
    public TrackRepository trackRepository;

    public TrackDetailPresenter(EditableView view, TrackDetailInteractor interactor){
        AutoMusicTagFixer.getContextComponent().inject(this);
        mView = view;
        mInteractor = interactor;
        mInteractor.setLoaderListener(this);
    }

    public void setCorrectionMode(int correctionMode){
        mCorrectionMode = correctionMode;
    }

    public void loadInfoTrack(int id){
        mCurrentId = id;
        mInteractor.loadInfoTrack(id);
    }

    /*Loading track data callbacks*/

    /**
     * Callback when the track data reading starts.
     * @param params Void params
     */
    @Override
    public void onAsyncOperationStarted(Void params) {
        if(mView != null)
            mView.loading(true);
    }

    /**
     * Callback when the track data reading finishes.
     * @param result The info of track.
     */
    @Override
    public void onAsyncOperationFinished(TrackDataLoader.TrackDataItem result) {
        mCurrentTrackDataItem = result;
        try {
            mLastTrackDataItem = (TrackDataLoader.TrackDataItem) mCurrentTrackDataItem.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        setDataTempTrack(result);

        if(mView != null) {
            setTags(result);
            if (mCorrectionMode == Constants.CorrectionModes.SEMI_AUTOMATIC) {
                startIdentification(TrackIdentifier.ALL_TAGS);
            }
        }
    }

    /**
     * Callback when the track data reading is cancelled.
     * @param cancellation Void params.
     */
    @Override
    public void onAsyncOperationCancelled(Void cancellation) {
        if(mView != null)
            mView.loading(false);
    }

    /**
     * Callback when the track data reading has encountered an error..
     * @param error Void params.
     */
    @Override
    public void onAsyncOperationError(String error) {
        if(mView != null) {
            mView.loading(false);
            mView.onLoadError(error);
        }
    }
    /*******************************************************************/

    private void setTags(TrackDataLoader.TrackDataItem trackDataItem){
        setToolbarBottomInfo(trackDataItem);
        setEditableInfo(trackDataItem);
        setAdditionalInfo(trackDataItem);
        if(mView != null) {
            mView.loading(false);
            mView.onSuccessLoad(mCurrentTrack.getPath());
            mView.onEnableFabs();
        }
    }

    private void setEditableInfo(TrackDataLoader.TrackDataItem trackDataItem){
        if(mView != null) {
            mView.setTrackTitle(trackDataItem.title);
            mView.setArtist(trackDataItem.artist);
            mView.setAlbum(trackDataItem.album);
            mView.setTrackNumber(trackDataItem.trackNumber);
            mView.setTrackYear(trackDataItem.trackYear);
            mView.setGenre(trackDataItem.genre);
            mView.setCover(trackDataItem.cover);
        }
    }

    private void setDataTempTrack(TrackDataLoader.TrackDataItem trackDataItem){
        String fullpath = trackDataItem.path + "/" + trackDataItem.fileName;
        mCurrentTrack = new Track(trackDataItem.title, trackDataItem.artist, trackDataItem.album, fullpath);
        mCurrentTrack.setMediaStoreId(mCurrentId);
    }

    private void setAdditionalInfo(TrackDataLoader.TrackDataItem trackDataItem){
        if(mView != null) {
            mView.setExtension(trackDataItem.extension);
            mView.setMimeType(trackDataItem.mimeType);
            mView.setDuration(trackDataItem.duration);
            mView.setBitrate(trackDataItem.bitrate);
            mView.setFrequency(trackDataItem.frequency);
            mView.setResolution(trackDataItem.resolution);
            mView.setChannels(trackDataItem.channels);
            mView.setFiletype(trackDataItem.fileType);
            mView.setFilesize(trackDataItem.fileSize);
            mView.setImageSize(trackDataItem.imageSize);
        }
    }

    private void setToolbarBottomInfo(TrackDataLoader.TrackDataItem trackDataItem){
        if(mView != null) {
            mView.setFilename(trackDataItem.fileName);
            mView.setPath(trackDataItem.path);
        }
    }

    public void restorePreviousValues(){
        if(mView != null && mIsInEditMode) {
            setEditableInfo(mCurrentTrackDataItem);
            setAdditionalInfo(mCurrentTrackDataItem);
        }
    }

    public void saveAsImageFileFrom(int from){
        closeFabMenu();
        byte[] cover;
        String title, artist, album;
        cancelIdentification();
        if(from == Constants.CACHED){
            cover = mCache.load(mCurrentId).cover;
            title = mCache.load(mCurrentId).title;
            artist = mCache.load(mCurrentId).artist;
            album = mCache.load(mCurrentId).album;
        }
        else {
            cover = mCurrentTrackDataItem.cover;
            if(cover == null){
                if(mView != null){
                    mView.onCorrectionError(resourceManager.getString(R.string.does_not_exist_cover), null);
                    mView.onEnableFabs();
                }
                return;
            }
            title = mView.getTrackTitle();
            artist = mView.getArtist();
            album = mView.getAlbum();
        }
        mFileSaver = new AsyncFileSaver(cover, title, artist, album);
        mFileSaver.setOnSavingListener(this);
        mFileSaver.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void validateInputData(){
        boolean isDataValid = validateInputs();
        if(isDataValid){
            mView.onDataValid();
        }
    }

    private GnResponseListener.IdentificationResults createResultsFromInputData(){
        GnResponseListener.IdentificationResults results = new GnResponseListener.IdentificationResults();

        results.title = StringUtilities.trimString(mView.getTrackTitle());
        results.artist = StringUtilities.trimString(mView.getArtist());
        results.album = StringUtilities.trimString(mView.getAlbum());
        results.trackNumber = StringUtilities.trimString(mView.getTrackNumber());
        results.trackYear = StringUtilities.trimString(mView.getTrackYear());
        results.genre = StringUtilities.trimString(mView.getGenre());
        results.cover = mCurrentCover;
        return results;
    }


    @Override
    public void destroy() {
        mCache.getCache().delete(mCurrentId);
        if(mIdentifier != null){
            mIdentifier.cancelIdentification();
        }

        if(mFixer != null){
            mFixer.cancel(true);
        }
        mCache = null;
        mFixer = null;
        mIdentifier = null;
        mView = null;
        resourceManager = null;
        connectivityDetector = null;
        gnService = null;
        mInteractor.destroy();
    }

    /************************Identification*************************/


    public void startIdentification(int recognitionType){
        closeFabMenu();
        mRecognition = recognitionType;
        GnResponseListener.IdentificationResults results = mCache.getCache().load(mCurrentId);
        if(mView != null) {
            mView.onDisableFabs();
            //If there are cached results
            if(results != null){
                mView.setMessageStatus("");
                mView.loading(false);
                //Type of recognition: only cover, or search all tags.
                if(mRecognition == TrackIdentifier.ALL_TAGS) {
                    if(mLifeCycleState == LifeCycleState.RESUMED) {
                        mPendingResultsDelivery = false;
                        mView.onLoadIdentificationResults(results);
                    }
                    else {
                        mPendingResultsDelivery = true;
                    }
                }
                else{
                    if(results.cover == null) {
                        mView.onCorrectionError(resourceManager.getString(R.string.no_cover_art_found),
                                resourceManager.getString(R.string.add_manual));
                        mView.onEnableFabs();
                    }
                    else {
                        if(mLifeCycleState == LifeCycleState.RESUMED) {
                            mPendingResultsDelivery = false;
                            mView.onLoadCoverIdentificationResults(results);
                        }
                        else {
                            mPendingResultsDelivery = true;
                        }
                    }
                }
                mView.onHideFabMenu();
                mView.onIdentificationComplete(results);
                mView.onEnableFabs();
            }
            //There are no cached results, make the request
            else {
                //Check connectivity
                if(!ConnectivityDetector.sIsConnected){
                    String message;

                    if(mRecognition == TrackIdentifier.ALL_TAGS) {
                        message = resourceManager.getString(R.string.no_internet_connection_semi_automatic_mode);
                    }
                    else {
                        message = resourceManager.getString(R.string.no_internet_connection_download_cover);
                    }

                    if(mView != null) {
                        mView.onCorrectionError(message, resourceManager.getString(R.string.add_manual));
                        mView.onEnableFabs();
                    }
                    connectivityDetector.onStartTestingNetwork();
                    mPendingIdentification = true;
                    return;
                }

                //Check if API is available
                if(!GnService.sApiInitialized || GnService.sIsInitializing) {
                    if(mView != null) {
                        mView.onCorrectionError(resourceManager.getString(R.string.initializing_recognition_api), null);
                        mView.onEnableFabs();
                    }

                    gnService.initializeAPI();
                    mPendingIdentification = true;
                    return;
                }
                mIdentifier = new TrackIdentifier();
                mIdentifier.setResourceManager(resourceManager);
                mIdentifier.setTrack(mCurrentTrack);
                mIdentifier.setGnListener(this);
                mIdentifier.execute();
            }
        }
    }

    @Override
    public void statusIdentification(String status, Track track) {
        if(mView != null)
            mView.setMessageStatus(status);
    }

    @Override
    public void gatheringFingerprint(Track track) {
        String msg = String.format(resourceManager.getString(R.string.gathering_fingerprint), TrackUtils.getPath(track.getPath()));
        if(mView != null)
            mView.setMessageStatus(msg);
    }

    @Override
    public void identificationError(String error, Track track) {
        if(mView != null) {
            mView.hideStatus();
            mView.setMessageStatus("");
            mView.loading(false);
            mView.onIdentificationError(error);
            mView.onEnableFabs();

        }
        mIdentifier = null;
    }

    @Override
    public void identificationNotFound(Track track) {
        if(mView != null) {
            mView.onCorrectionError(resourceManager.getString(R.string.no_found_tags), resourceManager.getString(R.string.add_manual));
            mView.setMessageStatus("");
            mView.loading(false);
            mView.onIdentificationNotFound();
            mView.onEnableFabs();
        }
        mIdentifier = null;
    }

    @Override
    public void identificationFound(GnResponseListener.IdentificationResults results, Track track) {
        hideFabMenu();
        if(mView != null) {
            mCache.getCache().add(mCurrentId, results);
            mView.loading(false);
            if(mRecognition == TrackIdentifier.ALL_TAGS){
                if(mLifeCycleState == LifeCycleState.RESUMED) {
                    mPendingResultsDelivery = false;
                    mView.onLoadIdentificationResults(results);
                }
                else {
                    mPendingResultsDelivery = true;
                }
            }
            else {
                if(results.cover == null){
                    mView.onCorrectionError(resourceManager.getString(R.string.no_cover_art_found), null);
                    mView.onEnableFabs();
                }
                else {
                    if(mLifeCycleState == LifeCycleState.RESUMED) {
                        mPendingResultsDelivery = false;
                        mView.onLoadCoverIdentificationResults(results);
                    }
                    else {
                        mPendingResultsDelivery = true;
                    }
                }
            }
            mView.onIdentificationComplete(results);
            mView.onEnableFabs();

        }
        mIdentifier = null;
    }

    @Override
    public void identificationCompleted(Track track) {
        if(mView != null)
            mView.loading(false);
        mIdentifier = null;
    }

    @Override
    public void onStartIdentification(Track track) {
        if(mView != null) {
            mView.setMessageStatus("");
            mView.showStatus();
            mView.loading(true);
            mView.setMessageStatus(resourceManager.getString(R.string.starting_correction));
        }
    }

    @Override
    public void onIdentificationCancelled(String cancelledReason, Track track) {
        if(mView != null) {
            mView.loading(false);
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.onIdentificationCancelled();
            mView.onEnableFabs();
        }
        mIdentifier = null;
    }

    @Override
    public void status(String message) {
        if(mView != null)
            mView.setMessageStatus(message);
    }

    public void cancelIdentification(){
        if(mIdentifier != null){
            mIdentifier.cancelIdentification();
        }
        mIdentifier = null;
    }

    /**************************************************************************************/

    /************************Correction*************************/
    public void performCorrection(Fixer.CorrectionParams correctionParams){
        GnResponseListener.IdentificationResults results;
        mFixer = new Fixer(this);

        mFixer.setTrack(mCurrentTrack);
        mFixer.setTask(correctionParams.mode);
        mFixer.setShouldRename(correctionParams.shouldRename);
        mFixer.renameTo(correctionParams.newName);

        mDataFrom = correctionParams.dataFrom;
        if(mDataFrom == Constants.CACHED) {
            results = mCache.load(mCurrentId);
        }
        else {
            results = createResultsFromInputData();
        }
        mFixer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, results);
    }

    @Override
    public void onCorrectionStarted(Track track) {
        if(mView != null) {
            mView.setMessageStatus(resourceManager.getString(R.string.correction_in_progress));
            mView.showStatus();
            mView.loading(true);
        }
    }

    @Override
    public void onCorrectionCompleted(Tagger.ResultCorrection resultCorrection, Track track) {
        if(mView != null) {
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.loading(false);
        }
        mFixer = null;
        //loadInfoTrack(mCurrentId);
        switch (resultCorrection.code){
            case Tagger.APPLIED_SAME_COVER:
            case Tagger.NEW_COVER_APPLIED:
            case Tagger.CURRENT_COVER_REMOVED:
                    updateTempCurrentTrackDataCover(resultCorrection);
                break;
            case Tagger.APPLIED_ALL_TAGS:
                    updateAppliedAllTagsView(resultCorrection);
                break;
            case Tagger.APPLIED_ONLY_MISSING_TAGS:
                    updateAppliedMissingTagsView(resultCorrection);
                break;
            case Tagger.APPLIED_SAME_TAGS:
                    updateAppliedSameTagsView(resultCorrection);
                break;
        }
    }

    @Override
    public void onCorrectionCancelled(Track track) {
        if(mView != null) {
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.loading(false);
        }
        mFixer = null;
    }

    @Override
    public void onCorrectionError(Tagger.ResultCorrection resultCorrection, Track track) {
        String errorMessage = Fixer.ERROR_CODES.getErrorMessage(resourceManager,resultCorrection.code);
        String action = resultCorrection.code == Tagger.COULD_NOT_GET_URI_SD_ROOT_TREE ?
                resourceManager.getString(R.string.get_permission):null;
        if(mView != null) {
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.loading(false);
            mView.onCorrectionError(errorMessage, action);
            mView.onEnableFabs();
        }
    }

    /***********************************************************/


    /**
     * Method to validate if current track has cover
     * when user tries to remove cover
     */
    public void removeCover(){
        closeFabMenu();
        if(mView != null) {
            if (mCurrentTrackDataItem.cover == null) {
                mView.onTrackHasNoCover();
            } else {
                mView.onConfirmRemovingCover();
            }
        }
    }

    public void confirmRemoveCover(){
        mFixer = new Fixer(this);
        mFixer.setTrack(mCurrentTrack);
        mFixer.setTask(Tagger.MODE_REMOVE_COVER);
        mFixer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }


    private boolean validateInputs(){
        String title = mView.getTrackTitle();
        String artist = mView.getArtist();
        String album = mView.getAlbum();
        String trackYear = mView.getTrackYear();
        String trackNumber = mView.getTrackNumber();
        String genre = mView.getGenre();

        int field = -1;
        if(title != null){
            field = R.id.track_name_details;
           if(StringUtilities.isFieldEmpty(title)) {
                mView.alertInvalidData(resourceManager.getString(R.string.empty_tag), field);
                return false;
            }
            if(StringUtilities.isTooLong(field, title)) {
                mView.alertInvalidData(resourceManager.getString(R.string.tag_too_long), field);
                return false;
            }
            title = StringUtilities.trimString(title);
        }

        if(artist != null){
            field = R.id.artist_name_details;
            if(StringUtilities.isFieldEmpty(artist)) {
                mView.alertInvalidData(resourceManager.getString(R.string.empty_tag), field);
                return false;
            }
            if(StringUtilities.isTooLong(field, artist)) {
                mView.alertInvalidData(resourceManager.getString(R.string.tag_too_long), field);
                return false;
            }
            artist = StringUtilities.trimString(artist);
        }

        if(album != null){
            field = R.id.album_name_details;
            if(StringUtilities.isFieldEmpty(album)) {
                mView.alertInvalidData(resourceManager.getString(R.string.empty_tag), field);
                return false;
            }
            if(StringUtilities.isTooLong(field, album)) {
                mView.alertInvalidData(resourceManager.getString(R.string.tag_too_long), field);
                return false;
            }
            album = StringUtilities.trimString(album);
        }

        if(trackYear != null){
            field = R.id.track_year;
            if(StringUtilities.isFieldEmpty(trackYear)) {
                mView.alertInvalidData(resourceManager.getString(R.string.empty_tag), field);
                return false;
            }
            if(StringUtilities.isTooLong(field, trackYear)) {
                mView.alertInvalidData(resourceManager.getString(R.string.tag_too_long), field);
                return false;
            }
            trackYear = StringUtilities.trimString(trackYear);
        }

        if(trackNumber != null){
            field = R.id.track_number;
            if(StringUtilities.isFieldEmpty(trackNumber)) {
                mView.alertInvalidData(resourceManager.getString(R.string.empty_tag), field);
                return false;
            }
            if(StringUtilities.isTooLong(field, trackNumber)) {
                mView.alertInvalidData(resourceManager.getString(R.string.tag_too_long), field);
                return false;
            }
            trackNumber = StringUtilities.trimString(trackNumber);
        }

        if(genre != null){
            field = R.id.track_genre;
            if(StringUtilities.isFieldEmpty(genre)) {
                mView.alertInvalidData(resourceManager.getString(R.string.empty_tag), field);
                return false;
            }
            if(StringUtilities.isTooLong(field, genre)) {
                mView.alertInvalidData(resourceManager.getString(R.string.tag_too_long), field);
                return false;
            }
            genre = StringUtilities.trimString(genre);
        }

        return true;
    }

    private void updateTempCurrentTrackDataCover(Tagger.ResultCorrection resultCorrection ){
        if(resultCorrection.code != Tagger.CURRENT_COVER_REMOVED) {
            if (mDataFrom == Constants.CACHED) {
                GnResponseListener.IdentificationResults results = mCache.getCache().load(mCurrentId);
                mView.setCover(results.cover);
                mView.setImageSize(TrackUtils.getStringImageSize(results.cover, resourceManager));
                mCurrentTrackDataItem.cover = results.cover;
                mCurrentCover = results.cover;

            }
            else {
                mView.setImageSize(TrackUtils.getStringImageSize(mCurrentCover, resourceManager));
                mView.setCover(mCurrentCover);
                mCurrentTrackDataItem.cover = mCurrentCover;
                disableEditMode();
            }
            mView.onSuccessfullyCorrection(resourceManager.getString(R.string.cover_updated));
        }
        else {
            mView.setCover(null);
            mCurrentTrackDataItem.cover = null;
            mCurrentCover = null;
            mView.setImageSize(TrackUtils.getStringImageSize(null, resourceManager));
            mView.setFilesize(TrackUtils.getFileSize(mCurrentTrack.getPath()));
            mView.onSuccessfullyCorrection(resourceManager.getString(R.string.cover_removed));
        }

        if(mView != null)
            mView.onEnableFabs();

        trackRepository.update(resultCorrection.track);
    }

    /**
     * Updates the internal data to update the values of views.
     * @param resultCorrection The result of correction.
     */
    private void updateAppliedAllTagsView(Tagger.ResultCorrection resultCorrection){
        //File was renamed
        if(resultCorrection.pathTofileUpdated != null) {
            mView.setFilename(resultCorrection.pathTofileUpdated);
            mView.setFilesize(TrackUtils.getFileSize(resultCorrection.pathTofileUpdated));
            mCurrentTrack.setPath(resultCorrection.pathTofileUpdated);
        }

        //Take the values from cache or from input values entered by user.
        if (mDataFrom == Constants.CACHED) {
            GnResponseListener.IdentificationResults results = mCache.getCache().load(mCurrentId);

            if(results.title != null && !results.title.isEmpty())
                mCurrentTrackDataItem.title = results.title;
            if(results.artist != null && !results.artist.isEmpty())
                mCurrentTrackDataItem.artist = results.artist;
            if(results.album != null && !results.album.isEmpty())
                mCurrentTrackDataItem.album = results.album;
            if(results.genre != null && !results.genre.isEmpty())
                mCurrentTrackDataItem.genre = results.genre;
            if(results.trackNumber != null && !results.trackNumber.isEmpty())
                mCurrentTrackDataItem.trackNumber = results.trackNumber;
            if(results.trackYear != null && !results.trackYear.isEmpty())
                mCurrentTrackDataItem.trackYear = results.trackYear;
            if(results.cover != null) {
                mCurrentTrackDataItem.cover = results.cover;
                mCurrentCover = results.cover;
                mView.setCover(results.cover);
                mView.setImageSize(TrackUtils.getStringImageSize(results.cover, resourceManager));
            }
            setEditableInfo(mCurrentTrackDataItem);
            setAdditionalInfo(mCurrentTrackDataItem);
        }
        else {
            mView.setCover(mCurrentCover);
            mView.setImageSize(TrackUtils.getStringImageSize(mCurrentCover, resourceManager));
            mCurrentTrackDataItem.title = mView.getTrackTitle();
            mCurrentTrackDataItem.artist = mView.getArtist();
            mCurrentTrackDataItem.album = mView.getAlbum();
            mCurrentTrackDataItem.genre = mView.getGenre();
            mCurrentTrackDataItem.trackNumber = mView.getTrackNumber();
            mCurrentTrackDataItem.trackYear = mView.getTrackYear();
            mCurrentTrackDataItem.cover = mCurrentCover;
            disableEditMode();
        }

        if(!resultCorrection.track.getTitle().isEmpty())
            mCurrentTrack.setTitle(mCurrentTrackDataItem.title);
        if(!resultCorrection.track.getArtist().isEmpty())
            mCurrentTrack.setArtist(mCurrentTrackDataItem.artist);
        if(!resultCorrection.track.getAlbum().isEmpty())
            mCurrentTrack.setAlbum(mCurrentTrackDataItem.album);

        mView.onSuccessfullyCorrection(resourceManager.getString(R.string.successfully_applied_tags));

        if(mView != null)
            mView.onEnableFabs();

        trackRepository.update(resultCorrection.track);
    }

    private void updateAppliedSameTagsView(Tagger.ResultCorrection resultCorrection){
        //File was renamed
        if(resultCorrection.pathTofileUpdated != null) {
            mView.setFilename(resultCorrection.pathTofileUpdated);
            mView.setFilesize(TrackUtils.getFileSize(resultCorrection.pathTofileUpdated));
            mCurrentTrack.setPath(resultCorrection.pathTofileUpdated);
        }

        setEditableInfo(mCurrentTrackDataItem);
        setAdditionalInfo(mCurrentTrackDataItem);
        disableEditMode();
        mView.onSuccessfullyCorrection(resourceManager.getString(R.string.successfully_applied_tags));

        if(mView != null)
            mView.onEnableFabs();

        trackRepository.update(resultCorrection.track);
    }


    private void updateAppliedMissingTagsView(Tagger.ResultCorrection resultCorrection){

        //File was renamed
        if(resultCorrection.pathTofileUpdated != null) {
            mView.setFilename(resultCorrection.pathTofileUpdated);
            mView.setFilesize(TrackUtils.getFileSize(resultCorrection.pathTofileUpdated));
            mCurrentTrack.setPath(resultCorrection.pathTofileUpdated);
        }

        if (mDataFrom == Constants.CACHED) {
            GnResponseListener.IdentificationResults results = mCache.getCache().load(mCurrentId);

            if(results.title != null && mView.getTrackTitle().isEmpty())
                mCurrentTrackDataItem.title = results.title;
            if(results.artist != null && mView.getArtist().isEmpty())
                mCurrentTrackDataItem.artist = results.artist;
            if(results.album != null && mView.getAlbum().isEmpty())
                mCurrentTrackDataItem.album = results.album;
            if(results.genre != null && mView.getGenre().isEmpty())
                mCurrentTrackDataItem.genre = results.genre;
            if(results.trackNumber != null && mView.getTrackNumber().isEmpty())
                mCurrentTrackDataItem.trackNumber = results.trackNumber;
            if(results.trackYear != null && mView.getTrackYear().isEmpty())
                mCurrentTrackDataItem.trackYear = results.trackYear;
            if(results.cover != null && mCurrentCover == null) {
                mCurrentTrackDataItem.cover = results.cover;
                mCurrentCover = results.cover;
                mView.setCover(results.cover);
                mView.setImageSize(TrackUtils.getStringImageSize(results.cover, resourceManager));
            }
            setEditableInfo(mCurrentTrackDataItem);
            setAdditionalInfo(mCurrentTrackDataItem);

        }
        else {
            mView.setCover(mCurrentCover);
            mView.setImageSize(TrackUtils.getStringImageSize(mCurrentCover, resourceManager));
            mCurrentTrackDataItem.title = mView.getTrackTitle();
            mCurrentTrackDataItem.artist = mView.getArtist();
            mCurrentTrackDataItem.album = mView.getAlbum();
            mCurrentTrackDataItem.genre = mView.getGenre();
            mCurrentTrackDataItem.trackNumber = mView.getTrackNumber();
            mCurrentTrackDataItem.trackYear = mView.getTrackYear();
            mCurrentTrackDataItem.cover = mCurrentCover;
            disableEditMode();
        }
        mCurrentTrack.setTitle(resultCorrection.track.getTitle());
        mCurrentTrack.setArtist(resultCorrection.track.getArtist());
        mCurrentTrack.setAlbum(resultCorrection.track.getAlbum());

        mView.onSuccessfullyCorrection(resourceManager.getString(R.string.successfully_applied_tags));

        if(mView != null)
            mView.onEnableFabs();

        trackRepository.update(resultCorrection.track);

    }

    @Override
    public void onSavingStart() {
        if(mView != null) {
            mView.showStatus();
            mView.setMessageStatus(resourceManager.getString(R.string.saving_cover));
            mView.loading(true);
        }
    }

    @Override
    public void onSavingFinished(String newPath) {
        if(mView != null) {
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.loading(false);
            mView.onSuccessfullyFileSaved(newPath);
            mView.onEnableFabs();
        }
    }

    @Override
    public void onSavingError(String error) {
        if(mView != null){
            mView.loading(false);
            mView.onCorrectionError(resourceManager.getString(R.string.cover_not_saved), null);
            mView.onEnableFabs();
        }
    }

    public void validateImageSize(ImageSize imageSize) {
        if(mView != null) {
            if (imageSize.height <= 2000 || imageSize.width <= 2000) {
                setNewCoverFromGallery(imageSize);
            } else {
                mView.onInvalidImage();
            }
        }
    }

    /**
     * Updates the cover
     * @param imageSize The wrapper containing the image
     */
    private void setNewCoverFromGallery(ImageSize imageSize){
        mCurrentCover = AndroidUtils.generateCover(imageSize.bitmap);
        if (imageSize.requestCode == INTENT_GET_AND_UPDATE_FROM_GALLERY) {
            Fixer.CorrectionParams correctionParams = new Fixer.CorrectionParams();
            correctionParams.dataFrom = Constants.MANUAL;
            correctionParams.mode = Tagger.MODE_ADD_COVER;
            cancelIdentification();
            performCorrection(correctionParams);
        }
        else {
            if(mView != null)
                mView.setCover(mCurrentCover);
        }
    }

    public void openInExternalApp(Context applicationContext) {
        AndroidUtils.openInExternalApp(mCurrentTrack.getPath(), applicationContext);
    }

    public void enableEditMode() {
        closeFabMenu();
        if(mView != null) {
            mView.onEnableEditMode();
            mIsInEditMode = true;
        }
    }

    public void toggleFabMenu() {
        if(!mIsFloatingActionMenuOpen) {
            openFabMenu();
        }
        else {
            closeFabMenu();
        }
    }

    public void hideFabMenu(){
        closeFabMenu();
    }
    private void openFabMenu() {
        if(mView != null) {
            mView.onShowFabMenu();
            mIsFloatingActionMenuOpen = true;
        }
    }
    private void closeFabMenu() {
        if(mView != null) {
            mView.onHideFabMenu();
            mIsFloatingActionMenuOpen = false;
        }
    }

    private void confirmExit(){
        if(mView != null) {
            mView.onConfirmExit();
        }
    }

    private void disableEditModeAndRestore(){
        mIsInEditMode = false;
        if(mView != null) {
            setEditableInfo(mCurrentTrackDataItem);
            setAdditionalInfo(mCurrentTrackDataItem);
            mView.onDisableEditModeAndRestore();
        }
    }

    private void disableEditMode(){
        mIsInEditMode = false;
        if(mView != null) {
            setEditableInfo(mCurrentTrackDataItem);
            setAdditionalInfo(mCurrentTrackDataItem);
            mView.onDisableEditMode();
        }
    }

    public void onBackPressed() {
        if(mIsFloatingActionMenuOpen){
            closeFabMenu();
        }
        else if(mIsInEditMode) {
            disableEditModeAndRestore();
        }
        else {
            confirmExit();
        }

    }

    /**
     * Handle the state of fragment when configuration
     * (rotation, language change, etc) changes.
     */
    public void handleConfigurationChange() {
        //mPendingResultsDelivery = true;
    }

    public void onStart() {
        mLifeCycleState = LifeCycleState.RESUMED;
        if(mPendingResultsDelivery){
            startIdentification(mRecognition);
        }
    }

    public void onStop(){
        mLifeCycleState = LifeCycleState.PAUSED;
    }

    public void onApiInitialized() {
        if(mPendingIdentification){
            startIdentification(mRecognition);
            mPendingIdentification = false;
        }
    }
}
