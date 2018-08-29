package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.sd_card_instructions.TransparentActivity;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.services.Fixer.Fixer;
import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.DefaultSharedPreferencesImpl;

/**
 * Use the {@link TrackDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackDetailFragment extends Fragment implements EditableView, ResultsTrackIdFragment.OnResultsTrackIdFragmentInteractionListener {
    private OnFragmentInteractionListener mListener;
    public interface OnFragmentInteractionListener {
        void onDataReady(String path);
        void onDataError();
        void onCancel();
        void onEditMode();
        void onUnedit();
        void onPerformingTask();
        void onFinishedTask();

        void onTitleToolbarChanged(String filename);
        void onCoverChanged(byte[] cover);
    }


    public static final String TAG = TrackDetailFragment.class.getName();
    //Intent type for pick an image
    public static final int INTENT_OPEN_GALLERY = 1;
    public static final int INTENT_GET_AND_UPDATE_FROM_GALLERY = 2;

    //Id from audio item list
    private int mCurrentItemId;
    //flag when user is editing info
    private boolean mEditMode = false;

    //Deafult action when activity is opened
    private int mCorrectionMode = Constants.CorrectionModes.VIEW_INFO;
    //rootview
    private View mLayout;
    //Editable data
    private EditText mTitleField;
    private EditText mArtistField;
    private EditText mAlbumField;
    private EditText mNumberField;
    private EditText mYearField;
    private EditText mGenreField;

    private TextView mBitrateField;
    private MenuItem mPlayPreviewButton;
    private TextView mLayerFileName, mSubtitleLayer;
    private TextView mImageSize;
    private TextView mFileSize;
    private TextView mTrackLength;
    private TextView mFrequency;
    private TextView mResolution;
    private TextView mChannels;
    private TextView mTrackType;
    private String mBitrate;
    private TextView mStatus;
    private ProgressBar mProgressBar;
    private Toolbar mToolbar;
    private AlertDialog mResultsDialog;


    //temporal references to new and current cover art
    private byte[] mCurrentCoverArt;

    private LinearLayout mProgressContainer;

    private static final int CROSS_FADE_DURATION = 200;
    private TrackDetailPresenter mTrackDetailPresenter;
    private ImageButton mCancelIdentification;
    @Inject
    AbstractSharedPreferences abstractSharedPreferences;
    @Inject
    DefaultSharedPreferencesImpl defaultSharedPreferences;
    private RelativeLayout mEditableFieldsContainer;

    public TrackDetailFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static TrackDetailFragment newInstance(int idTrack, int correctionMode) {
        TrackDetailFragment fragment = new TrackDetailFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(Constants.MEDIA_STORE_ID, idTrack);
        bundle.putInt(Constants.CorrectionModes.MODE, correctionMode);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //currentId of audioItem
        Bundle bundle = getArguments();

        mCurrentItemId = bundle.getInt(Constants.MEDIA_STORE_ID,-1);
        mCorrectionMode = bundle.getInt(Constants.CorrectionModes.MODE,Constants.CorrectionModes.VIEW_INFO);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mLayout = inflater.inflate(R.layout.fragment_track_detail, container, false);
        mEditableFieldsContainer = mLayout.findViewById(R.id.editable_data_container);
        setupFields();

        mTrackDetailPresenter =  new TrackDetailPresenter(this, new TrackDetailInteractor());
        mTrackDetailPresenter.setCorrectionMode(mCorrectionMode);
        mTrackDetailPresenter.loadInfoTrack(mCurrentItemId);
        return mLayout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnResultsTrackIdFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onBackPressed(){
            mTrackDetailPresenter.restorePreviousValues();
    }

    public void extractCover(){
        mTrackDetailPresenter.saveAsImageFileFrom(Constants.MANUAL);
    }

    /**
     * Callback for processing the result from startActivityForResult call,
     * here we process the image picked by user and apply to audio file
     * @param requestCode is the code from what component
     *                    makes the request this can be snack bar,
     *                    toolbar or text "Añadir caratula de galería"
     * @param resultCode result code is the action requested, in this case
     *                   Intent.ACTION_PICK
     * @param data Data received, can be null
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "request code " + requestCode + " - resultCode" + resultCode + " resultData is null " + (data == null));

        switch (requestCode){
            case INTENT_GET_AND_UPDATE_FROM_GALLERY:
            case INTENT_OPEN_GALLERY:
                if (data != null){
                    try {
                        Uri imageData = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageData);

                        if (bitmap.getHeight() > 2000 || bitmap.getWidth() > 2000) {
                            Snackbar snackbar = AndroidUtils.getSnackbar(mLayout, getActivity().getApplicationContext());
                            snackbar.setText(getString(R.string.image_too_big));
                            snackbar.setDuration(Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                        else {
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

                            mCurrentCoverArt = byteArrayOutputStream.toByteArray();
                            if (requestCode == INTENT_GET_AND_UPDATE_FROM_GALLERY) {
                                Fixer.CorrectionParams correctionParams = new Fixer.CorrectionParams();
                                correctionParams.dataFrom = Constants.MANUAL;
                                correctionParams.mode = Tagger.MODE_ADD_COVER;
                                mTrackDetailPresenter.performCorrection(correctionParams);
                            }
                            else {
                                if(mListener != null)
                                    mListener.onCoverChanged(byteArrayOutputStream.toByteArray());
                            }
                        }
                    } catch(IOException e){
                        e.printStackTrace();
                        Snackbar snackbar = AndroidUtils.getSnackbar(mLayout, getActivity().getApplicationContext());
                        snackbar.setText(getString(R.string.error_load_image));
                        snackbar.setDuration(Snackbar.LENGTH_SHORT);
                        snackbar.show();
                    }

                }
                break;

            case RequiredPermissions.REQUEST_PERMISSION_SAF:
                String msg = "";
                if (resultCode == Activity.RESULT_OK) {
                    // The document selected by the user won't be returned in the intent.
                    // Instead, a URI to that document will be contained in the return intent
                    // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
                    boolean res = AndroidUtils.grantPermissionSD(getActivity().getApplicationContext(), data);;
                    if (res) {
                        msg = getString(R.string.toast_apply_tags_again);
                    }
                    else {
                        msg = getString(R.string.could_not_get_permission);
                    }
                }
                else {
                    msg = getString(R.string.saf_denied);
                }

                Toast toast = AndroidUtils.getToast(getActivity().getApplicationContext());
                toast.setText(msg);
                toast.show();
                break;
        }

    }

    /**
     * Release resources in this last callback
     * received in activity before is destroyed
     *
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
        mTrackDetailPresenter.destroy();
        mTrackDetailPresenter = null;
        mTitleField = null;
        mArtistField = null;
        mAlbumField = null;
        mNumberField = null;
        mYearField = null;
        mGenreField = null;
        mCurrentCoverArt = null;
        mBitrateField = null;
        mPlayPreviewButton = null;
        mLayerFileName = null;
        mSubtitleLayer = null;
        mImageSize = null;
        mFileSize = null;
        mTrackLength = null;
        mFrequency = null;
        mResolution = null;
        mChannels = null;
        mTrackType = null;
        mBitrate = null;
        mStatus = null;
        mProgressBar = null;
        if(mResultsDialog != null)
            mResultsDialog.dismiss();
        System.gc();
    }

    /**
     * This method creates the references to visual elements
     * in layout
     */
    private void setupFields(){

        mProgressBar = mLayout.findViewById(R.id.progress_bar);
        //editable edit texts from song
        mTitleField = mLayout.findViewById(R.id.track_name_details);
        mArtistField = mLayout.findViewById(R.id.artist_name_details);
        mAlbumField = mLayout.findViewById(R.id.album_name_details);
        mNumberField = mLayout.findViewById(R.id.track_number);
        mYearField = mLayout.findViewById(R.id.track_year);
        mGenreField = mLayout.findViewById(R.id.track_genre);

        //Additional data fields
        mLayerFileName = mLayout.findViewById(R.id.titleTransparentLayer);
        mSubtitleLayer = mLayout.findViewById(R.id.subtitleTransparentLayer);
        mImageSize = mLayout.findViewById(R.id.imageSize);
        mFileSize = mLayout.findViewById(R.id.fileSize);
        mTrackLength = mLayout.findViewById(R.id.trackLength);
        mBitrateField = mLayout.findViewById(R.id.bitrate);
        mTrackType = mLayout.findViewById(R.id.track_type);
        mFrequency = mLayout.findViewById(R.id.frequency);
        mResolution = mLayout.findViewById(R.id.resolution);
        mChannels = mLayout.findViewById(R.id.channels);
        mStatus = mLayout.findViewById(R.id.status_message);

        mProgressContainer = mLayout.findViewById(R.id.progress_container);
        mCancelIdentification = mLayout.findViewById(R.id.cancel_identification);
    }

    public void startIdentification(int whatToIdentify){
        mTrackDetailPresenter.startIdentification(whatToIdentify);
    }

    public void cancelIdentification(){
        mTrackDetailPresenter.cancelIdentification();
    }

    public void removeCover(){
        if(getCover() == null){
            Snackbar snackbar = AndroidUtils.getSnackbar(mLayout, getActivity().getApplicationContext());
            snackbar.setText(getString(R.string.does_not_exist_cover));
            snackbar.setDuration(Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
        else {

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.message_remove_cover_art_dialog);
            builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mTrackDetailPresenter.removeCover();
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    public void searchInfoForTrack(){
        String queryString = getTrackTitle() + (!getArtist().isEmpty() ? (" " + getArtist()) : "");
        String query = "https://www.google.com/#q=" + queryString;
        Uri uri = Uri.parse(query);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    /**
     * Enters edit mode, for modify manually
     * the information about the song
     */
    private void enableFieldsToEdit(){
        //shrink toolbar to make it easy to user
        //focus in editing tags


        //Enable edit text for edit them
        mTitleField.setEnabled(true);
        mArtistField.setEnabled(true);
        mAlbumField.setEnabled(true);
        mNumberField.setEnabled(true);
        mYearField.setEnabled(true);
        mGenreField.setEnabled(true);

        mImageSize.setText(getString(R.string.edit_cover));
        mImageSize.setCompoundDrawablesWithIntrinsicBounds(getActivity().getDrawable(R.drawable.ic_autorenew_black_24dp),null,null,null);
        //Enabled "Añadir carátula de galería" to add cover when is pressed
        mImageSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editCover(INTENT_OPEN_GALLERY);
            }
        });
        mTitleField.requestFocus();
        InputMethodManager imm =(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mTitleField,InputMethodManager.SHOW_IMPLICIT);
        if(mListener != null)
            mListener.onEditMode();

        mEditMode = true;
    }

    /**
     * Remove identificationError tags
     */
    private void removeErrorTags(){
        //get descendants instances of edit text
        ArrayList<View> fields = mLayout.getFocusables(View.FOCUS_DOWN);
        int numElements = fields.size();

        for (int i = 0 ; i < numElements ; i++) {
            if (fields.get(i) instanceof EditText) {
                ((EditText) fields.get(i)).setError(null);
            }
        }
    }

    /**
     * Disables the fields and
     * leaves out from edit mode
     */

    private void disableFields(){

        removeErrorTags();

        mTitleField.clearFocus();
        mTitleField.setEnabled(false);
        mArtistField.clearFocus();
        mArtistField.setEnabled(false);
        mAlbumField.clearFocus();
        mAlbumField.setEnabled(false);
        mNumberField.clearFocus();
        mNumberField.setEnabled(false);
        mYearField.clearFocus();
        mYearField.setEnabled(false);
        mGenreField.clearFocus();
        mGenreField.setEnabled(false);

        mImageSize.setText(AudioItem.getStringImageSize(mCurrentCoverArt));
        mImageSize.setCompoundDrawablesWithIntrinsicBounds(getActivity().getDrawable(R.drawable.ic_photo_white_24px),null,null,null);
        mImageSize.setOnClickListener(null);

        //to hide it, call the method again
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            assert imm != null;
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        if(mListener != null)
            mListener.onUnedit();

        mEditMode = false;

    }

    /**
     * Opens a dialog to select a image
     * to apply as new embed cover art.
     * @param codeIntent The code to distinguish if we pressed the cover toolbar,
     *                   the action button "Galería" from snackbar or "Añadir carátula de galería"
     *                   from main container.
     */
    private void editCover(int codeIntent){
        Intent selectorImageIntent = new Intent(Intent.ACTION_PICK);
        selectorImageIntent.setType("image/*");
        startActivityForResult(selectorImageIntent,codeIntent);
    }

    /**
     * Callback when activities enters to pause state,
     * remove receivers if FixerTrackService is not processing any task
     * to save battery
     */
    @Override
    public void onPause(){
        super.onPause();
    }

    /**
     * Callback when user starts interacting
     * with activity.
     * Here register receivers for handling
     * responses from FixerTrackService
     */
    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void setTrackTitle(String value) {
        mTitleField.setText(value);
    }

    @Override
    public void setArtist(String value) {
        mArtistField.setText(value);
    }

    @Override
    public void setAlbum(String value) {
        mAlbumField.setText(value);
    }

    @Override
    public void setGenre(String value) {
        mGenreField.setText(value);
    }

    @Override
    public void setTrackNumber(String value) {
        mNumberField.setText(value);
    }

    @Override
    public void setTrackYear(String value) {
        mYearField.setText(value);
    }

    @Override
    public void setCover(byte[] value) {
        mCurrentCoverArt = value;
        if(mListener != null){
            mListener.onCoverChanged(value);
        }
    }

    @Override
    public String getTrackTitle() {
        return mTitleField.getText().toString();
    }

    @Override
    public String getArtist() {
        return mArtistField.getText().toString();
    }

    @Override
    public String getAlbum() {
        return mAlbumField.getText().toString();
    }

    @Override
    public String getGenre() {
        return mGenreField.getText().toString();
    }

    @Override
    public String getTrackNumber() {
        return mNumberField.getText().toString();
    }

    @Override
    public String getTrackYear() {
        return mYearField.getText().toString();
    }

    @Override
    public byte[] getCover() {
        return mCurrentCoverArt;
    }

    @Override
    public void setFilename(String value) {
        if(mListener != null){
            mListener.onTitleToolbarChanged(value);
        }
    }

    @Override
    public void setPath(String value) {
        mSubtitleLayer.setText(value);
    }

    @Override
    public void setDuration(String value) {
        mTrackLength.setText(value);
    }

    @Override
    public void setBitrate(String value) {
        mBitrateField.setText(value);
    }

    @Override
    public void setFrequency(String value) {
        mFrequency.setText(value);
    }

    @Override
    public void setResolution(String value) {
        mResolution.setText(value);
    }

    @Override
    public void setFiletype(String value) {
        mTrackType.setText(value);
    }

    @Override
    public void setChannels(String value) {
        mChannels.setText(value);
    }

    @Override
    public void setExtension(String value) {

    }

    @Override
    public void setMimeType(String value) {

    }

    @Override
    public void setFilesize(String value) {
            mFileSize.setText(value);
    }

    @Override
    public void setImageSize(String value) {
        if(!mEditMode)
            mImageSize.setText(value);
    }

    @Override
    public void showStatus() {
        mStatus.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideStatus() {
        mStatus.setVisibility(View.INVISIBLE);
    }

    @Override
    public void setMessageStatus(String status) {
        mStatus.setText(status);
    }

    @Override
    public void showProgress() {
        mListener.onPerformingTask();
        mProgressContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {

        mProgressContainer.setVisibility(View.GONE);
    }

    @Override
    public void onLoadError(String error) {
        if(mListener != null) {
            mListener.onDataError();
        }
    }

    @Override
    public void onSuccessLoad(String path) {

        if(mListener != null)
            mListener.onDataReady(path);

        mCancelIdentification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTrackDetailPresenter.cancelIdentification();
            }
        });

        //when intent brings correction mode  == MANUAL, enable fields
        //to start immediately editing it
        if (mCorrectionMode == Constants.CorrectionModes.MANUAL) {
            enableFieldsToEdit();
        }
    }

    @Override
    public void loadIdentificationResults(GnResponseListener.IdentificationResults results) {
        Fixer.CorrectionParams correctionParams = new Fixer.CorrectionParams();

        DialogInterface.OnClickListener positiveButtonListener = (dialog, which) -> {
            correctionParams.dataFrom = Constants.CACHED;
            correctionParams.mode = Tagger.MODE_OVERWRITE_ALL_TAGS;
            mTrackDetailPresenter.performCorrection(correctionParams);
        };

        DialogInterface.OnClickListener negativeButtonListener = (dialog, which) -> {
            correctionParams.dataFrom = Constants.CACHED;
            correctionParams.mode = Tagger.MODE_WRITE_ONLY_MISSING;
            mTrackDetailPresenter.performCorrection(correctionParams);
        };
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_results_track_id, null);

        final CheckBox checkBox = view.findViewById(R.id.checkbox_rename);
        TextInputLayout textInputLayout = view.findViewById(R.id.label_rename_to);
        EditText editText = view.findViewById(R.id.rename_to);
        TextView textView = view.findViewById(R.id.message_rename_hint);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                correctionParams.newName = editText.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Log.d("checked", (isChecked) + "");
                if(!isChecked){
                    textInputLayout.setVisibility(View.GONE);
                    textView.setVisibility(View.GONE);
                    editText.setText("");
                    correctionParams.newName = "";
                    correctionParams.shouldRename = false;
                }
                else{
                    textInputLayout.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.VISIBLE);
                    correctionParams.newName = editText.getText().toString();
                    correctionParams.shouldRename = true;
                }
            }
        });

        mResultsDialog = AndroidUtils.createResultsDialog(getActivity(),results, R.string.message_results, true, view, positiveButtonListener,negativeButtonListener );
        mResultsDialog.show();
    }

    @Override
    public void loadCoverIdentificationResults(GnResponseListener.IdentificationResults results) {

        DialogInterface.OnClickListener positiveButtonListener = (dialog, which) -> {
            Fixer.CorrectionParams correctionParams = new Fixer.CorrectionParams();
            correctionParams.dataFrom = Constants.CACHED;
            correctionParams.mode = Tagger.MODE_ADD_COVER;
            mTrackDetailPresenter.performCorrection(correctionParams);
        };

        DialogInterface.OnClickListener negativeButtonListener = (dialog, which) -> {
            mTrackDetailPresenter.saveAsImageFileFrom(Constants.CACHED);
        };
        mResultsDialog = AndroidUtils.createResultsDialog(getActivity(),results, R.string.message_results_cover, false, positiveButtonListener,negativeButtonListener );
        mResultsDialog.show();
        mResultsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.as_cover_art);
        mResultsDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(R.string.as_file);
    }

    @Override
    public void identificationComplete(GnResponseListener.IdentificationResults identificationResults) {
        if(mListener != null)
            mListener.onFinishedTask();
    }

    @Override
    public void identificationCancelled() {
        Snackbar snackbar = AndroidUtils.getSnackbar(mLayout, getActivity().getApplicationContext());
        snackbar.setText(R.string.identification_interrupted);
        snackbar.show();
        if(mListener != null)
            mListener.onFinishedTask();
    }

    @Override
    public void identificationNotFound() {
        if(mListener != null)
            mListener.onFinishedTask();
    }

    @Override
    public void identificationError(String error) {
        if(mListener != null)
            mListener.onFinishedTask();
    }

    @Override
    public void onSuccessfullyCorrection(String message) {
        Snackbar snackbar = AndroidUtils.getSnackbar(mLayout, getActivity().getApplicationContext());
        snackbar.setText(message);
        snackbar.show();

        if(!mEditMode) {
            Intent intent = new Intent(Constants.Actions.FINISH_TRACK_PROCESSING);
            intent.putExtra("should_reload_cover", true);
            intent.putExtra(Constants.MEDIA_STORE_ID, mCurrentItemId);
            LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).sendBroadcast(intent);

            if (mListener != null)
                mListener.onFinishedTask();
        }
    }

    @Override
    public void onSuccessfullyFileSaved(final String message) {
        Snackbar snackbar = AndroidUtils.getSnackbar(mLayout, getActivity().getApplicationContext());
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        snackbar.setText(String.format(getString(R.string.cover_saved), message));
        snackbar.setAction(R.string.watch, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndroidUtils.openInExternalApp(message, getActivity().getApplicationContext());
            }
        });
        snackbar.show();
        if(mListener != null)
            mListener.onFinishedTask();
    }

    @Override
    public void onCorrectionError(String message, String action) {
        Snackbar snackbar = AndroidUtils.getSnackbar(mLayout, getActivity().getApplicationContext());
        if(action != null && action.equals(getString(R.string.get_permission)))
            snackbar.setAction(action, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().startActivity(new Intent(getActivity(), TransparentActivity.class));
                    //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    //startActivityForResult(intent, RequiredPermissions.REQUEST_PERMISSION_SAF);
                }
            });

        snackbar.setText(message);
        snackbar.show();
        if(mListener != null)
            mListener.onFinishedTask();
    }

    @Override
    public void enableEditMode() {
        enableFieldsToEdit();
    }

    @Override
    public void disableEditMode() {
        disableFields();
    }

    @Override
    public void disableEditModeAndRestore() {
        disableFields();
        mTrackDetailPresenter.restorePreviousValues();
    }

    @Override
    public void alertInvalidData(String message, int field) {
        EditText editText = mLayout.findViewById(field);
        Animation animation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.blink);
        editText.requestFocus();
        editText.setError(message);
        editText.setAnimation(animation);
        editText.startAnimation(animation);
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onDataValid() {
        Fixer.CorrectionParams correctionParams = new Fixer.CorrectionParams();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.manual_correction_layout, null);
        builder.setView(view);

        final CheckBox checkBox = view.findViewById(R.id.manual_checkbox_rename);
        TextInputLayout textInputLayout = view.findViewById(R.id.manual_label_rename_to);
        EditText editText = view.findViewById(R.id.manual_rename_to);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                correctionParams.newName = editText.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        TextView textView = view.findViewById(R.id.manual_message_rename_hint);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Log.d("checked", (isChecked) + "");
                if(!isChecked){
                    textInputLayout.setVisibility(View.GONE);
                    textView.setVisibility(View.GONE);
                    editText.setText("");
                    correctionParams.newName = "";
                    correctionParams.shouldRename = false;
                }
                else{
                    textInputLayout.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.VISIBLE);
                    correctionParams.newName = editText.getText().toString();
                    correctionParams.shouldRename = true;
                }
            }
        });

        builder.setMessage(R.string.message_apply_new_tags);
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                disableFields();
                mTrackDetailPresenter.restorePreviousValues();
            }
        });
        builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                correctionParams.dataFrom = Constants.MANUAL;
                correctionParams.mode = Tagger.MODE_OVERWRITE_ALL_TAGS;
                mTrackDetailPresenter.performCorrection(correctionParams);
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }


    public void validateInputData(){
        mTrackDetailPresenter.validateInputData();
    }
}
