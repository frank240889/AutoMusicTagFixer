package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.NonNull;
import mx.dev.franco.automusictagfixer.modelsUI.track_detail.ImageWrapper;

public class TrackDetailViewModel extends AndroidViewModel {
    public MutableLiveData<String> title;
    public MutableLiveData<String> artist;
    public MutableLiveData<String> album;
    public MutableLiveData<String> number;
    public MutableLiveData<String> year;
    public MutableLiveData<String> genre;


    public MutableLiveData<String> filesize;
    public MutableLiveData<String> channels;
    public MutableLiveData<String> type;
    public MutableLiveData<String> resolution;
    public MutableLiveData<String> frequency;
    public MutableLiveData<String> bitrate;
    public MutableLiveData<String> length;
    public MutableLiveData<String> absolutePath;

    public TrackDetailViewModel(@NonNull Application application) {
        super(application);
        title = new MutableLiveData<>();
    }

    public void setInitialAction(int correctionMode) {

    }

    public void validateImageSize(ImageWrapper imageWrapper) {

    }

    public void confirmRemoveCover() {

    }

    public void cancelIdentification() {

    }

    public void performCorrection(CorrectionParams correctionParams) {

    }

    public void saveAsImageFileFrom(int cached) {

    }

    public void enableEditMode() {

    }

    public void restorePreviousValues() {

    }

    public void onBackPressed() {

    }

    public void startIdentification() {

    }

    public void toggleFabMenu() {


    }

    public void hideFabMenu() {


    }

    public void validateInputData() {

    }

    public void openInExternalApp(Context applicationContext) {

    }

    public void loadInfoTrack(int anInt) {

    }

    public void removeCover() {

    }
}
