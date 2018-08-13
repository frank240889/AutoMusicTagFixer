package mx.dev.franco.automusictagfixer.UI.track_detail;

import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;

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
    byte[] getCover();

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


    void showStatus();
    void hideStatus();
    void setMessageStatus(String status);

    void showProgress();
    void hideProgress();

    void onLoadError(String error);
    void onSuccessLoad(String path);

    void loadIdentificationResults(GnResponseListener.IdentificationResults results);
    void loadCoverIdentificationResults(GnResponseListener.IdentificationResults results);
    void identificationComplete(GnResponseListener.IdentificationResults identificationResults);
    void identificationCancelled();
    void identificationNotFound();
    void identificationError(String error);

    void onSuccessfullyCorrection(String message);
    void onSuccessfullyFileSaved(String message);
    void onCorrectionError(String message);

    void enableEditMode();
    void disableEditMode();
    void alertInvalidData(String message, int field);
    void onDataValid();
}
