package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.os.AsyncTask;
import android.util.Log;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.cache.Cache;
import mx.dev.franco.automusictagfixer.cache.DownloadedTrackDataCacheImpl;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.media_store_retriever.AsyncFileReader;
import mx.dev.franco.automusictagfixer.network.ConnectivityDetector;
import mx.dev.franco.automusictagfixer.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.services.Fixer.Fixer;
import mx.dev.franco.automusictagfixer.services.TrackIdentifier;
import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;
import mx.dev.franco.automusictagfixer.services.gnservice.GnService;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.StringUtilities;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

public class TrackDetailPresenter implements TrackDataLoader.TrackLoader,
        Destructible, GnResponseListener.GnListener,
        Fixer.OnCorrectionListener, AsyncFileReader.IRetriever, AsyncFileSaver.OnSaveListener {
    private static final String TAG = TrackDetailPresenter.class.getName();
    private EditableView mView;
    private TrackDetailInteractor mInteractor;
    private TrackDataLoader.TrackDataItem mCurrentTrackDataItem;
    private Track mCurrentTrack;
    private static TrackIdentifier sIdentifier;
    private int mCurrentId;
    private Cache<Integer, GnResponseListener.IdentificationResults> mCache = new DownloadedTrackDataCacheImpl.Builder().build();
    private static Fixer sFixer;
    private static AsyncFileSaver sFileSaver;
    private int mCorrectionMode = Constants.CorrectionModes.VIEW_INFO;
    private int mRecognition = TrackIdentifier.ALL_TAGS;
    private int mDataFrom;
    @Inject
    ResourceManager resourceManager;
    @Inject
    ConnectivityDetector connectivityDetector;
    @Inject
    GnService gnService;
    @Inject
    TrackRepository trackRepository;

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

    @Override
    public void onStartedLoad() {
        mView.showProgress();
    }

    @Override
    public void onFinishedLoad(TrackDataLoader.TrackDataItem trackDataItem) {
        mCurrentTrackDataItem = trackDataItem;
        if(mView != null)
            setTags(trackDataItem);

        Log.d(TAG, "Coorrection mode: " + mCorrectionMode);
        if(mCorrectionMode == Constants.CorrectionModes.MANUAL){
            mView.enableEditMode();
        }
        else if(mCorrectionMode == Constants.CorrectionModes.SEMI_AUTOMATIC){
            startIdentification(TrackIdentifier.ALL_TAGS);
        }

    }

    public void updateCover(byte[] cover){
        mView.setCover(cover);
    }

    @Override
    public void onCancelledLoad() {
        if(mView != null) {
            mView.hideProgress();
        }
    }

    @Override
    public void onLoadError(String error) {
        if(mView != null) {
            mView.hideProgress();
            mView.onLoadError(error);
        }
    }

    private void setTags(TrackDataLoader.TrackDataItem trackDataItem){
        setInfoForError(trackDataItem);
        setEditableInfo(trackDataItem);
        setAdditionalInfo(trackDataItem);
        setDataTempTrack(trackDataItem);
        if(mView != null) {
            mView.hideProgress();
            mView.onSuccessLoad(trackDataItem.path + "/" + trackDataItem.fileName);
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
        String fullpath =trackDataItem.path + "/" + trackDataItem.fileName;
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

    private void setInfoForError(TrackDataLoader.TrackDataItem trackDataItem){
        if(mView != null) {
            mView.setFilename(trackDataItem.fileName);
            mView.setPath(trackDataItem.path);
        }
    }

    public void restorePreviousValues(){
        if(mView != null) {
            setEditableInfo(mCurrentTrackDataItem);
            setAdditionalInfo(mCurrentTrackDataItem);
        }
    }

    public void saveAsImageFileFrom(int from){
        byte[] cover;
        String title, artist, album;
        if(from == Constants.CACHED){
            cover = mCache.load(mCurrentId).cover;
            title = mCache.load(mCurrentId).title;
            artist = mCache.load(mCurrentId).artist;
            album = mCache.load(mCurrentId).album;
        }
        else {
            cover = mView.getCover();
            if(cover == null){
                if(mView != null){
                    mView.onCorrectionError(resourceManager.getString(R.string.can_not_extract_null_cover), null);
                }
                return;
            }
            title = mView.getTrackTitle();
            artist = mView.getArtist();
            album = mView.getAlbum();
        }
        sFileSaver = new AsyncFileSaver(cover, title, artist, album);
        sFileSaver.setOnSavingListener(this);
        sFileSaver.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void validateInputData(){
        boolean isDataValid = validateInputs();
        if(isDataValid){
            mView.onDataValid();
        }
    }

    private GnResponseListener.IdentificationResults createResultsFromInputData(){
        GnResponseListener.IdentificationResults results = new GnResponseListener.IdentificationResults();

        results.title = mView.getTrackTitle();
        results.artist = mView.getArtist();
        results.album = mView.getAlbum();
        results.trackNumber = mView.getTrackNumber();
        results.trackYear = mView.getTrackYear();
        results.genre = mView.getGenre();
        results.cover = mView.getCover();
        return results;
    }


    @Override
    public void destroy() {
        mCache.getCache().delete(mCurrentId);
        if(sIdentifier != null){
            sIdentifier.cancelIdentification();
        }

        if(sFixer != null){
            sFixer.cancel(true);
        }
        mCache = null;
        sFixer = null;
        sIdentifier = null;
        mView = null;
        resourceManager = null;
        connectivityDetector = null;
        gnService = null;
        mInteractor.destroy();
    }

    /************************Identification*************************/


    public void startIdentification(int recognitionType){
        mRecognition = recognitionType;
        GnResponseListener.IdentificationResults results = mCache.getCache().load(mCurrentId);
        if(results != null && mView != null){
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.hideProgress();
            if(mRecognition == TrackIdentifier.ALL_TAGS) {
                mView.loadIdentificationResults(results);
            }
            else{
                if(results.cover == null)
                    mView.onCorrectionError(resourceManager.getString(R.string.no_cover_art_found), null);
                else
                    mView.loadCoverIdentificationResults(results);
            }

            mView.identificationComplete(results);
        }
        else {

            if(!ConnectivityDetector.sIsConnected){
                String message;

                if(mRecognition == TrackIdentifier.ALL_TAGS) {
                    message = resourceManager.getString(R.string.no_internet_connection_semi_automatic_mode);

                }
                else {
                    message = resourceManager.getString(R.string.no_internet_connection_download_cover);
                }

                if(mView != null)
                    mView.onCorrectionError(message, null);
                connectivityDetector.onStartTestingNetwork();
                return;
            }

            if(!GnService.sApiInitialized || GnService.sIsInitializing) {
                if(mView != null)
                    mView.onCorrectionError(resourceManager.getString(R.string.initializing_recognition_api), null);

                gnService.initializeAPI();
                return;
            }

            sIdentifier = new TrackIdentifier(this);
            sIdentifier.setTrack(mCurrentTrack);
            sIdentifier.execute();
        }
    }

    public void cancelIdentification(){
        if(sIdentifier != null){
            sIdentifier.cancelIdentification();
        }
        sIdentifier = null;
    }

    @Override
    public void statusIdentification(String status, Track track) {
        if(mView != null)
            mView.setMessageStatus(status);
    }

    @Override
    public void gatheringFingerprint(Track track) {
        String msg = String.format(resourceManager.getString(R.string.gathering_fingerprint), AudioItem.getPath(track.getPath()));
        if(mView != null)
            mView.setMessageStatus(msg);
    }

    @Override
    public void identificationError(String error, Track track) {
        if(mView != null) {
            mView.hideStatus();
            mView.setMessageStatus("");
            mView.hideProgress();
            mView.identificationError(error);

        }
        sIdentifier = null;
    }

    @Override
    public void identificationNotFound(Track track) {
        if(mView != null) {
            mView.onCorrectionError(resourceManager.getString(R.string.no_found_tags), null);
            mView.setMessageStatus("");
            mView.hideProgress();
            mView.identificationNotFound();
        }
        sIdentifier = null;
    }

    @Override
    public void identificationFound(GnResponseListener.IdentificationResults results, Track track) {
        if(mView != null) {
            mCache.getCache().add(mCurrentId, results);
            mView.hideProgress();
            if(mRecognition == TrackIdentifier.ALL_TAGS){
                mView.loadIdentificationResults(results);
            }
            else {
                if(results.cover == null){
                    mView.onCorrectionError(resourceManager.getString(R.string.no_cover_art_found), null);
                }
                else {
                    mView.loadCoverIdentificationResults(results);
                }
            }
            mView.identificationComplete(results);

        }
        sIdentifier = null;
    }

    @Override
    public void identificationCompleted(Track track) {
        if(mView != null)
            mView.hideProgress();
        sIdentifier = null;
    }

    @Override
    public void onStartIdentification(Track track) {
        if(mView != null) {
            mView.setMessageStatus("");
            mView.showStatus();
            mView.showProgress();
            mView.setMessageStatus("Iniciando identificación...");
        }
    }

    @Override
    public void onIdentificationCancelled(String cancelledReason, Track track) {
        if(mView != null) {
            mView.hideProgress();
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.identificationCancelled();
        }
        sIdentifier = null;
    }

    @Override
    public void status(String message) {
        if(mView != null)
            mView.setMessageStatus(message);
    }

    /************************Correction*************************/
    //public void performCorrection(int dataFrom, int mode){
    public void performCorrection(Fixer.CorrectionParams correctionParams){
        GnResponseListener.IdentificationResults results;
        sFixer = new Fixer(this);

        sFixer.setTrack(mCurrentTrack);
        sFixer.setTask(correctionParams.mode);
        sFixer.setShouldRename(correctionParams.shouldRename);
        sFixer.renameTo(correctionParams.newName);

        mDataFrom = correctionParams.dataFrom;
        if(mDataFrom == Constants.CACHED) {
            results = mCache.load(mCurrentId);
        }
        else {
            results = createResultsFromInputData();
        }
        sFixer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, results);
    }

    public void removeCover(){
        sFixer = new Fixer(this);
        sFixer.setTrack(mCurrentTrack);
        sFixer.setTask(Tagger.MODE_REMOVE_COVER);
        sFixer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }


    @Override
    public void onCorrectionStarted(Track track) {
        if(mView != null) {
            mView.setMessageStatus("Corrigiendo, espera por favor...");
            mView.showStatus();
            mView.showProgress();
        }
    }

    @Override
    public void onCorrectionCompleted(Tagger.ResultCorrection resultCorrection, Track track) {
        if(mView != null) {
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.hideProgress();
        }
        sFixer = null;
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
                    updateAppliedAllTagsView(resultCorrection);
                break;
        }
    }

    @Override
    public void onCorrectionCancelled(Track track) {
        if(mView != null) {
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.hideProgress();
        }
        sFixer = null;
    }

    @Override
    public void onCorrectionError(Tagger.ResultCorrection resultCorrection, Track track) {
        String errorMessage = Fixer.ERROR_CODES.getErrorMessage(resourceManager,resultCorrection.code);
        String action = resultCorrection.code == Tagger.COULD_NOT_GET_URI_SD_ROOT_TREE ?
                resourceManager.getString(R.string.get_permission):null;
        if(mView != null) {
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.hideProgress();
            mView.onCorrectionError(errorMessage, action);
        }
    }

    /***********************************************************/


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
                mView.alertInvalidData("Etiqueta vacía", field);
                return false;
            }
            if(StringUtilities.isTooLong(field, title)) {
                mView.alertInvalidData("Etiqueta muy larga.", field);
                return false;
            }
            title = StringUtilities.trimString(title);
        }

        if(artist != null){
            field = R.id.artist_name_details;
            if(StringUtilities.isFieldEmpty(artist)) {
                mView.alertInvalidData("Etiqueta vacía", field);
                return false;
            }
            if(StringUtilities.isTooLong(field, artist)) {
                mView.alertInvalidData("Etiqueta muy larga.", field);
                return false;
            }
            artist = StringUtilities.trimString(artist);
        }

        if(album != null){
            field = R.id.album_name_details;
            if(StringUtilities.isFieldEmpty(album)) {
                mView.alertInvalidData("Etiqueta vacía", field);
                return false;
            }
            if(StringUtilities.isTooLong(field, album)) {
                mView.alertInvalidData("Etiqueta muy larga.", field);
                return false;
            }
            album = StringUtilities.trimString(album);
        }

        if(trackYear != null){
            field = R.id.track_year;
            if(StringUtilities.isFieldEmpty(trackYear)) {
                mView.alertInvalidData("Etiqueta vacía", field);
                return false;
            }
            if(StringUtilities.isTooLong(field, trackYear)) {
                mView.alertInvalidData("Etiqueta muy larga.", field);
                return false;
            }
            trackYear = StringUtilities.trimString(trackYear);
        }

        if(trackNumber != null){
            field = R.id.track_number;
            if(StringUtilities.isFieldEmpty(trackNumber)) {
                mView.alertInvalidData("Etiqueta vacía", field);
                return false;
            }
            if(StringUtilities.isTooLong(field, trackNumber)) {
                mView.alertInvalidData("Etiqueta muy larga.", field);
                return false;
            }
            trackNumber = StringUtilities.trimString(trackNumber);
        }

        if(genre != null){
            field = R.id.track_genre;
            if(StringUtilities.isFieldEmpty(genre)) {
                mView.alertInvalidData("Etiqueta vacía", field);
                return false;
            }
            if(StringUtilities.isTooLong(field, genre)) {
                mView.alertInvalidData("Etiqueta muy larga.", field);
                return false;
            }
            genre = StringUtilities.trimString(genre);
        }

        return true;
    }

    @Override
    public void onStart() {
        if(mView != null){
            mView.showProgress();
            mView.setMessageStatus("Actualizando lista...");
        }
    }

    @Override
    public void onFinish() {
        if(mView != null) {
            mView.hideProgress();
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.onSuccessfullyCorrection("Etiquetas aplicadas correctamente.");
        }
    }

    @Override
    public void onCancel() {

    }

    private void updateTempCurrentTrackDataCover(Tagger.ResultCorrection resultCorrection ){
        if(resultCorrection.code != Tagger.CURRENT_COVER_REMOVED) {
            if (mDataFrom == Constants.CACHED) {
                GnResponseListener.IdentificationResults results = mCache.getCache().load(mCurrentId);
                mView.setCover(results.cover);
                mView.setImageSize(AudioItem.getStringImageSize(results.cover));
                mCurrentTrackDataItem.cover = results.cover;

            } else {
                mView.setImageSize(AudioItem.getStringImageSize(mView.getCover()));
                mView.setCover(mView.getCover());
                mCurrentTrackDataItem.cover = mView.getCover();
                mView.disableEditMode();
            }
            mView.onSuccessfullyCorrection(resourceManager.getString(R.string.cover_updated));
        }
        else {
            mView.setCover(null);
            mCurrentTrackDataItem.cover = null;
            mView.setImageSize(AudioItem.getStringImageSize(null));
            mView.setFilesize(AudioItem.getFileSize(mCurrentTrack.getPath()));
            mView.onSuccessfullyCorrection(resourceManager.getString(R.string.cover_removed));
        }

        trackRepository.update(resultCorrection.track);
    }

    private void updateAppliedAllTagsView(Tagger.ResultCorrection resultCorrection){
        //File was renamed
        if(resultCorrection.pathTofileUpdated != null) {
            mView.setFilename(resultCorrection.pathTofileUpdated);
            mView.setFilesize(AudioItem.getFileSize(resultCorrection.pathTofileUpdated));
            mCurrentTrack.setPath(resultCorrection.pathTofileUpdated);
        }

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
                mView.setCover(results.cover);
                mView.setImageSize(AudioItem.getStringImageSize(results.cover));
            }
            setEditableInfo(mCurrentTrackDataItem);
            setAdditionalInfo(mCurrentTrackDataItem);
        }
        else {
            mView.setCover(mView.getCover());
            mView.setImageSize(AudioItem.getStringImageSize(mView.getCover()));
            mCurrentTrackDataItem.title = mView.getTrackTitle();
            mCurrentTrackDataItem.artist = mView.getArtist();
            mCurrentTrackDataItem.album = mView.getAlbum();
            mCurrentTrackDataItem.genre = mView.getGenre();
            mCurrentTrackDataItem.trackNumber = mView.getTrackNumber();
            mCurrentTrackDataItem.trackYear = mView.getTrackYear();
            mCurrentTrackDataItem.cover = mView.getCover();
            mView.disableEditMode();
        }

        if(!resultCorrection.track.getTitle().isEmpty())
            mCurrentTrack.setTitle(resultCorrection.track.getTitle());
        if(!resultCorrection.track.getArtist().isEmpty())
            mCurrentTrack.setArtist(resultCorrection.track.getArtist());
        if(!resultCorrection.track.getAlbum().isEmpty())
            mCurrentTrack.setAlbum(resultCorrection.track.getAlbum());

        mView.onSuccessfullyCorrection(resourceManager.getString(R.string.apply_tags));
        trackRepository.update(resultCorrection.track);
    }


    private void updateAppliedMissingTagsView(Tagger.ResultCorrection resultCorrection){

        //File was renamed
        if(resultCorrection.pathTofileUpdated != null) {
            mView.setFilename(resultCorrection.pathTofileUpdated);
            mView.setFilesize(AudioItem.getFileSize(resultCorrection.pathTofileUpdated));
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
            if(results.cover != null && mView.getCover() == null) {
                mCurrentTrackDataItem.cover = results.cover;
                mView.setCover(results.cover);
                mView.setImageSize(AudioItem.getStringImageSize(results.cover));
            }
            setEditableInfo(mCurrentTrackDataItem);
            setAdditionalInfo(mCurrentTrackDataItem);

        }
        else {
            mView.setCover(mView.getCover());
            mView.setImageSize(AudioItem.getStringImageSize(mView.getCover()));
            mCurrentTrackDataItem.title = mView.getTrackTitle();
            mCurrentTrackDataItem.artist = mView.getArtist();
            mCurrentTrackDataItem.album = mView.getAlbum();
            mCurrentTrackDataItem.genre = mView.getGenre();
            mCurrentTrackDataItem.trackNumber = mView.getTrackNumber();
            mCurrentTrackDataItem.trackYear = mView.getTrackYear();
            mCurrentTrackDataItem.cover = mView.getCover();
            mView.disableEditMode();
        }
        mCurrentTrack.setTitle(resultCorrection.track.getTitle());
        mCurrentTrack.setArtist(resultCorrection.track.getArtist());
        mCurrentTrack.setAlbum(resultCorrection.track.getAlbum());

        mView.onSuccessfullyCorrection(resourceManager.getString(R.string.apply_tags));
        trackRepository.update(resultCorrection.track);

    }

    @Override
    public void onSavingStart() {
        if(mView != null) {
            mView.showStatus();
            mView.setMessageStatus(resourceManager.getString(R.string.saving_cover));
            mView.showProgress();
        }
    }

    @Override
    public void onSavingFinished(String newPath) {
        if(mView != null) {
            mView.setMessageStatus("");
            mView.hideStatus();
            mView.hideProgress();
            mView.onSuccessfullyFileSaved(newPath);
        }
    }

    @Override
    public void onSavingError(String error) {
        if(mView != null){
            mView.onCorrectionError(resourceManager.getString(R.string.cover_not_saved), null);
        }
    }
}
