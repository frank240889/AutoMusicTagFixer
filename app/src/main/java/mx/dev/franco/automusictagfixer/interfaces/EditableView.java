package mx.dev.franco.automusictagfixer.interfaces;

public interface EditableView {
    void setTrackTitle(String value);
    void setArtist(String value);
    void setAlbum(String value);
    void setGenre(String value);
    void setTrackNumber(String value);
    void setTrackYear(String value);
    void setCover(byte[] value);

    String getTrackTitle();
    String getArtist();
    String getAlbum();
    String getGenre();
    String getTrackNumber();
    String getTrackYear();

    void setFilename(String value);
    void setPath(String value);
    void setDuration(String value);
    void setBitrate(String value);
    void setFrequency(String value);
    void setResolution(String value);
    void setFiletype(String value);
    void setChannels(String value);
    void setExtension(String value);
    void setMimeType(String value);
    void setFilesize(String value);
    void setImageSize(String value);


    void setStateMessage(String message, boolean visible);

    void loading(boolean showProgress);

    void onLoadError(String error);
    void onSuccessLoad(String path);

    void onLoadIdentificationResults(GnResponseListener.IdentificationResults results);
    void onLoadCoverIdentificationResults(GnResponseListener.IdentificationResults results);
    void onIdentificationComplete(GnResponseListener.IdentificationResults identificationResults);
    void onIdentificationCancelled();
    void onIdentificationNotFound();
    void onIdentificationError(String error);

    void onSuccessfullyCorrection(String message);
    void onSuccessfullyFileSaved(String message);
    void onCorrectionError(String message, String action);

    void onEnableEditMode();
    void onDisableEditMode();
    void onDisableEditModeAndRestore();
    void alertInvalidData(String message, int field);
    void onDataValid();

    void onTrackHasNoCover();

    void onConfirmRemovingCover();
    void onInvalidImage();

    void onShowFabMenu();
    void onHideFabMenu();

    void onConfirmExit();

    void onEnableFabs();
    void onDisableFabs();

    void setCancelTaskEnabled(boolean enableCancelView);
}
