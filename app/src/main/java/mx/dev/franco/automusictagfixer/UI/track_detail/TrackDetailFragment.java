package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.BaseFragment;
import mx.dev.franco.automusictagfixer.UI.main.MainActivity;
import mx.dev.franco.automusictagfixer.UI.sd_card_instructions.SdCardInstructionsActivity;
import mx.dev.franco.automusictagfixer.fixer.Fixer;
import mx.dev.franco.automusictagfixer.identifier.GnResponseListener;
import mx.dev.franco.automusictagfixer.identifier.TrackIdentifier;
import mx.dev.franco.automusictagfixer.interfaces.EditableView;
import mx.dev.franco.automusictagfixer.modelsUI.track_detail.ImageSize;
import mx.dev.franco.automusictagfixer.modelsUI.track_detail.TrackDetailInteractor;
import mx.dev.franco.automusictagfixer.modelsUI.track_detail.TrackDetailPresenter;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.SimpleMediaPlayer;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.TrackUtils;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.DefaultSharedPreferencesImpl;

/**
 * Use the {@link TrackDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackDetailFragment extends BaseFragment implements EditableView,
        IdentificationResultsFragment.OnBottomSheetFragmentInteraction,
        SimpleMediaPlayer.OnEventDispatchedListener {

    public static final String TAG = TrackDetailFragment.class.getName();
    private static final int CROSS_FADE_DURATION = 200;
    //Intent type for pick an image
    public static final int INTENT_OPEN_GALLERY = 1;
    public static final int INTENT_GET_AND_UPDATE_FROM_GALLERY = 2;

    //rootview
    private View mLayout;
    //Editable data
    private EditText mTitleField;
    private EditText mArtistField;
    private EditText mAlbumField;
    private EditText mNumberField;
    private EditText mYearField;
    private EditText mGenreField;

    //Additional data.
    private TextView mBitrateField;
    private TextView mSubtitleLayer;
    private TextView mImageSize;
    private TextView mChangeImage;
    private TextView mFileSize;
    private TextView mTrackLength;
    private TextView mFrequency;
    private TextView mResolution;
    private TextView mChannels;
    private TextView mTrackType;
    private TextView mStatus;

    //Menu items
    private MenuItem mPlayPreviewButton;
    private MenuItem mUpdateCoverButton;
    private MenuItem mExtractCoverButton;
    private MenuItem removeItem;
    private MenuItem searchInWebItem;

    //Title in bottom toolbar of appbar layout
    private TextView mTitleBottomTransparentLayer;
    private Toolbar mToolbar;
    private ImageView mToolbarCover;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private AppBarLayout mAppBarLayout;
    private ActionBar mActionBar;

    //Reference to custom media mPlayer.
    private SimpleMediaPlayer mPlayer;

    private ConstraintLayout mProgressContainer;

    private TrackDetailPresenter mTrackDetailPresenter;
    private Button mCancelIdentification;

    //Fabs to create a fab menu
    FloatingActionButton mEditButton;
    FloatingActionButton mDownloadCoverButton;
    FloatingActionButton mAutoFixButton;
    FloatingActionButton mSaveButton;
    FloatingActionButton mFloatingActionMenu;

    @Inject
    public AbstractSharedPreferences abstractSharedPreferences;
    @Inject
    public DefaultSharedPreferencesImpl defaultSharedPreferences;
    private ConstraintLayout mEditableFieldsContainer;

    public TrackDetailFragment() {}

    public static TrackDetailFragment newInstance(int idTrack, int correctionMode) {
        TrackDetailFragment fragment = new TrackDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.MEDIA_STORE_ID, idTrack);
        bundle.putInt(Constants.CorrectionModes.MODE, correctionMode);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mTrackDetailPresenter =  new TrackDetailPresenter(this, new TrackDetailInteractor());
        mPlayer = SimpleMediaPlayer.getInstance(context);
        mPlayer.addListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if(bundle != null)
            mTrackDetailPresenter.setCorrectionMode(bundle.getInt(Constants.CorrectionModes.MODE,Constants.CorrectionModes.VIEW_INFO));
        else
            mTrackDetailPresenter.setCorrectionMode(Constants.CorrectionModes.VIEW_INFO);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mLayout = inflater.inflate(R.layout.fragment_track_detail, container, false);
        mEditableFieldsContainer = mLayout.findViewById(R.id.editable_data_container);
        //collapsible toolbar
        mToolbar = mLayout.findViewById(R.id.toolbar);
        ((MainActivity)getActivity()).setSupportActionBar(mToolbar);
        mCollapsingToolbarLayout = mLayout.findViewById(R.id.collapsing_toolbar_layout);
        mAppBarLayout = mLayout.findViewById(R.id.app_bar_layout);
        mCollapsingToolbarLayout.setTitleEnabled(false);
        mActionBar = ((MainActivity)getActivity()).getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(false);

        setHasOptionsMenu(true);
        setupFields();
        setupDataInfoFields();
        return mLayout;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mTrackDetailPresenter.handleConfigurationChange();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        //Both fragments have the same menu
        inflater.inflate(R.menu.menu_details_track_dialog, menu);
        mPlayPreviewButton = menu.findItem(R.id.action_play);
        mExtractCoverButton = menu.findItem(R.id.action_extract_cover);
        mUpdateCoverButton = menu.findItem(R.id.action_update_cover);
        removeItem = menu.findItem(R.id.action_remove_cover);
        searchInWebItem = menu.findItem(R.id.action_web_search);

        Bundle bundle = getArguments();
        if(bundle != null)
            mTrackDetailPresenter.loadInfoTrack(bundle.getInt(Constants.MEDIA_STORE_ID,-1));
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
        switch (requestCode){
            case INTENT_GET_AND_UPDATE_FROM_GALLERY:
            case INTENT_OPEN_GALLERY:
                if (data != null){
                    try {
                        Uri imageData = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageData);
                        ImageSize imageSize = new ImageSize();
                        imageSize.width = bitmap.getWidth();
                        imageSize.height = bitmap.getHeight();
                        imageSize.bitmap = bitmap;
                        imageSize.requestCode = requestCode;
                        mTrackDetailPresenter.validateImageSize(imageSize);
                    } catch(IOException e){
                        e.printStackTrace();
                        Snackbar snackbar = AndroidUtils.getSnackbar(mEditableFieldsContainer, getActivity().getApplicationContext());
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
     * Callback when user tried to set new cover from gallery but was not valid.
     */
    @Override
    public void onInvalidImage() {
        Snackbar snackbar = AndroidUtils.getSnackbar(mEditableFieldsContainer, getActivity().getApplicationContext());
        snackbar.setText(getString(R.string.image_too_big));
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPlayer.removeListener();
        mTitleField = null;
        mArtistField = null;
        mAlbumField = null;
        mNumberField = null;
        mYearField = null;
        mGenreField = null;
        mBitrateField = null;
        mSubtitleLayer = null;
        mImageSize = null;
        mFileSize = null;
        mTrackLength = null;
        mFrequency = null;
        mResolution = null;
        mChannels = null;
        mTrackType = null;
        mStatus = null;
        mPlayer = null;
        mTrackDetailPresenter = null;
    }

    /**
     * Release resources in this last callback
     * received in activity before is destroyed
     *
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
        mPlayer.stopPreview();
        mTrackDetailPresenter.destroy();
    }

    /**
     * Callback when user tried to remove the cover but current track
     * has no cover.
     */
    @Override
    public void onTrackHasNoCover() {
        Snackbar snackbar = AndroidUtils.getSnackbar(mEditableFieldsContainer, getActivity().getApplicationContext());
        snackbar.setText(getString(R.string.does_not_exist_cover));
        snackbar.setDuration(Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    /**
     * Callback to confirm the deletion of current cover;
     */
    @Override
    public void onConfirmRemovingCover() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.message_remove_cover_art_dialog);
        builder.setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.cancel());
        builder.setPositiveButton(R.string.accept, (dialog, which) -> mTrackDetailPresenter.confirmRemoveCover());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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
        onCoverChanged(value);
        mImageSize.setText(TrackUtils.getStringImageSize(value, getActivity().getApplicationContext()));
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
    public void setFilename(String value) {
        mTitleBottomTransparentLayer.setText(TrackUtils.getFilename(value));
    }

    @Override
    public void setPath(String value) {
        mSubtitleLayer.setText(value);
        setupMediaPlayer(value);
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
        mImageSize.setText(value);
    }

    @Override
    public void setStateMessage(String message, boolean visible) {
        mStatus.setVisibility(View.VISIBLE);
        mStatus.setText(message);
    }

    @Override
    public void loading(boolean showProgress) {
        if(showProgress) {
            mPlayer.stopPreview();
            mProgressContainer.setVisibility(View.VISIBLE);
        }
        else {
            mProgressContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Callback when could not read the track.
     * @param error The reason why could not read track.
     */
    @Override
    public void onLoadError(String error) {
        //pressing back from toolbar, close activity
        mToolbar.setNavigationOnClickListener(v -> onConfirmExit());
        mChangeImage.setVisibility(View.GONE);
        Toast t = AndroidUtils.getToast(getActivity());
        t.setText(R.string.could_not_read_file);
        t.setDuration(Toast.LENGTH_SHORT);
        t.show();
    }


    /**
     * Callback when data from track is completely
     * loaded.
     * @param path The path of the file loaded
     */
    @Override
    public void onSuccessLoad(String path) {
        //pressing back from toolbar, close activity
        mToolbar.setNavigationOnClickListener(v -> onConfirmExit());
        mChangeImage.setOnClickListener(v -> editCover(INTENT_OPEN_GALLERY));

        addFloatingActionButtonListeners();
        addAppBarOffsetListener();
        addToolbarButtonsListeners();
        showFabs();
        setupMediaPlayer(path);

        //Set action for "X" button
        mCancelIdentification.setOnClickListener(v -> mTrackDetailPresenter.cancelIdentification());
    }

    /**
     * Loads the identification results and shows to the user.
     * @param results The object containing the data.
     */
    @Override
    public void onLoadIdentificationResults(GnResponseListener.IdentificationResults results) {
        IdentificationResultsFragment identificationResultsFragment =
                IdentificationResultsFragment.newInstance(results, false);
        identificationResultsFragment.show(getChildFragmentManager(),
                IdentificationResultsFragment.class.getName());
    }

    /**
     * Callback from {@link IdentificationResultsFragment} when
     * user pressed apply only missing tags button
     */
    @Override
    public void onMissingTagsButton(Fixer.CorrectionParams correctionParams) {
        mTrackDetailPresenter.performCorrection(correctionParams);
    }

    /**
     * Callback from {@link IdentificationResultsFragment} when
     * user pressed apply all tags button
     */
    @Override
    public void onOverwriteTagsButton(Fixer.CorrectionParams correctionParams) {
        mTrackDetailPresenter.performCorrection(correctionParams);
    }

    /**
     * Callback from {@link IdentificationResultsFragment} when
     * user pressed save cover as image button
     */
    @Override
    public void onSaveAsImageFile() {
        mTrackDetailPresenter.saveAsImageFileFrom(Constants.CACHED);
    }

    /**
     * Loads the identification results and shows to the user.
     * @param results The object containing the data.
     */
    @Override
    public void onLoadCoverIdentificationResults(GnResponseListener.IdentificationResults results) {
        IdentificationResultsFragment identificationResultsFragment =
                IdentificationResultsFragment.newInstance(results, true);
        identificationResultsFragment.show(getChildFragmentManager(),
                IdentificationResultsFragment.class.getName());
    }

    /**
     * Callback when idenfitication process finishes.
     * @param identificationResults The object containing the results.
     */
    @Override
    public void onIdentificationComplete(GnResponseListener.IdentificationResults identificationResults) {
    }

    /**
     * Callback when user cancelled the identification process.
     */
    @Override
    public void onIdentificationCancelled() {
        Snackbar snackbar = AndroidUtils.getSnackbar(mEditableFieldsContainer, getActivity().getApplicationContext());
        if (snackbar != null) {
            snackbar.setText(R.string.identification_interrupted);
            snackbar.show();
        }
    }


    /**
     * Callback when no results from identification process were found.
     */
    @Override
    public void onIdentificationNotFound() {

    }

    /**
     * Callback when identification process error ocurred.
     */
    @Override
    public void onIdentificationError(String error) {
    }

    /**
     * Callback when correction process has successfully finished.
     */
    @Override
    public void onSuccessfullyCorrection(String message) {
        Snackbar snackbar = AndroidUtils.getSnackbar(mEditableFieldsContainer, getActivity().getApplicationContext());
        snackbar.setText(message);
        snackbar.show();
    }


    @Override
    public void onEnableFabs() {
        enableMiniFabs(true);
    }

    @Override
    public void onDisableFabs() {
        enableMiniFabs(false);
    }

    /**
     * Callback when a correction process error ocurred.
     * @param message The message of error.
     * @param action The action to perform on snackbar.
     */
    @Override
    public void onCorrectionError(String message, String action) {
        Snackbar snackbar = AndroidUtils.getSnackbar(mEditableFieldsContainer, getActivity().getApplicationContext());
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        if (action != null && action.equals(getString(R.string.get_permission))){
            snackbar.setAction(action, v -> getActivity().startActivity(new Intent(getActivity(), SdCardInstructionsActivity.class)));
        }
        else if(action != null && action.equals(getString(R.string.add_manual))){
            snackbar.setAction(action, v -> mTrackDetailPresenter.enableEditMode());
        }

        snackbar.setText(message);
        snackbar.show();
    }

    /**
     * Callback when saving cover as jpg image process has successfully finished.
     */
    @Override
    public void onSuccessfullyFileSaved(final String message) {
        Snackbar snackbar = AndroidUtils.getSnackbar(mEditableFieldsContainer, getActivity().getApplicationContext());
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        snackbar.setText(String.format(getString(R.string.cover_saved), message));
        snackbar.setAction(R.string.watch, v -> AndroidUtils.openInExternalApp(message, getActivity().getApplicationContext()));
        snackbar.show();
    }

    @Override
    public void onEnableEditMode() {
        disableAppBarLayout();
        enableFieldsToEdit();
    }

    @Override
    public void onDisableEditMode() {
        disableFields();
    }

    @Override
    public void onDisableEditModeAndRestore() {
        onDisableEditMode();
    }

    /**
     * Callback when user pressed mSaveButton and input data is invalid.
     */
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

    /**
     * Callback when user pressed mSaveButton and input data is valid.
     */
    @Override
    public void onDataValid() {
        Fixer.CorrectionParams correctionParams = new Fixer.CorrectionParams();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.rename_file_layout, null);
        builder.setView(view);

        final CheckBox checkBox = view.findViewById(R.id.manual_checkbox_rename);
        TextInputLayout textInputLayout = view.findViewById(R.id.manual_label_rename_to);
        EditText editText = view.findViewById(R.id.manual_rename_to);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                correctionParams.newName = editText.getText().toString();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        TextView textView = view.findViewById(R.id.manual_message_rename_hint);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
        });
        builder.setTitle(R.string.manual);
        builder.setMessage(R.string.message_apply_new_tags);
        builder.setNegativeButton(R.string.cancel_button, (dialog, which) -> {
            dialog.dismiss();
            disableFields();
            mTrackDetailPresenter.restorePreviousValues();
        });
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            correctionParams.dataFrom = Constants.MANUAL;
            correctionParams.mode = Tagger.MODE_OVERWRITE_ALL_TAGS;
            mTrackDetailPresenter.performCorrection(correctionParams);
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onStartPlaying() {
        mPlayPreviewButton.setIcon(R.drawable.ic_stop_white_24px);
        addStopAction();
    }

    @Override
    public void onStopPlaying() {
        mPlayPreviewButton.setIcon(R.drawable.ic_play_arrow_white_24px);
        addPlayAction();
    }

    @Override
    public void onCompletionPlaying() {
        mPlayPreviewButton.setIcon(R.drawable.ic_play_arrow_white_24px);
    }

    @Override
    public void onErrorPlaying(int what, int extra) {
        mPlayPreviewButton.setEnabled(false);
    }

    @Override
    public void onShowFabMenu() {
        showFABMenu();
    }

    @Override
    public void onHideFabMenu() {
        closeFABMenu();
    }

    @Override
    protected void callSuperOnBackPressed() {
        mTrackDetailPresenter.onBackPressed();
    }

    @Override
    public void onConfirmExit() {
        super.callSuperOnBackPressed();
    }

    @Override
    public void onStart() {
        super.onStart();
        mTrackDetailPresenter.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mTrackDetailPresenter.onStop();
    }

    @Override
    public void setCancelTaskEnabled(boolean enableCancelView) {
        mCancelIdentification.setVisibility(enableCancelView ? View.VISIBLE : View.GONE);
    }

    /**
     * Enables and disables fabs
     * @param enable true for enable, false to disable
     */
    private void enableMiniFabs(boolean enable){
        mUpdateCoverButton.setEnabled(enable);
        mToolbarCover.setEnabled(enable);
        mDownloadCoverButton.setEnabled(enable);
        mEditButton.setEnabled(enable);
        mAutoFixButton.setEnabled(enable);
    }

    /**
     * Shows mini fabs
     */
    private void showFABMenu(){
        mFloatingActionMenu.animate().rotation(-400);
        mAutoFixButton.animate().translationY(-getResources().getDimension(R.dimen.standard_55));
        mEditButton.animate().translationY(-getResources().getDimension(R.dimen.standard_105));
        mDownloadCoverButton.animate().translationY(-getResources().getDimension(R.dimen.standard_155));
    }

    /**
     * Hides mini fabs
     */
    private void closeFABMenu() {
        mFloatingActionMenu.animate().rotation(0);
        mAutoFixButton.animate().translationY(0);
        mEditButton.animate().translationY(0);
        mDownloadCoverButton.animate().translationY(0);
    }

    /**
     * This method creates the references to visual elements
     * in layout
     */
    private void setupFields(){
        mToolbarCover = mLayout.findViewById(R.id.toolbar_cover_art);

        mTitleBottomTransparentLayer = mLayout.findViewById(R.id.title_bottom_transparent_layer);

        //Floating action buttons
        mDownloadCoverButton = mLayout.findViewById(R.id.fab_download_cover);
        mEditButton = mLayout.findViewById(R.id.fab_edit_track_info);
        mAutoFixButton = mLayout.findViewById(R.id.fab_autofix);
        mFloatingActionMenu = mLayout.findViewById(R.id.fab_menu);
        mSaveButton = mLayout.findViewById(R.id.fab_save_info);
        mDownloadCoverButton.hide();
        mEditButton.hide();
        mAutoFixButton.hide();
        mFloatingActionMenu.hide();
        mSaveButton.hide();
    }

    /**
     * Add listeners for corresponding objects to
     * respond to user interactions
     */

    private void addFloatingActionButtonListeners(){
        //enable manual mode
        mEditButton.setOnClickListener(v -> {
            mTrackDetailPresenter.enableEditMode();
        });

        //runs track id
        mAutoFixButton.setOnClickListener(v -> {
            mTrackDetailPresenter.startIdentification(TrackIdentifier.ALL_TAGS);
        });

        mDownloadCoverButton.setOnClickListener(v -> {
            mTrackDetailPresenter.startIdentification(TrackIdentifier.JUST_COVER);
        });

        //shows or hides mini fabs
        mFloatingActionMenu.setOnClickListener(view -> {
            mTrackDetailPresenter.toggleFabMenu();
        });

        //updates only cover art
        mToolbarCover.setOnClickListener(v -> {
            mTrackDetailPresenter.hideFabMenu();
            editCover(TrackDetailFragment.INTENT_GET_AND_UPDATE_FROM_GALLERY);
        });

    }

    private void addAppBarOffsetListener(){
        mAppBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            //set alpha of cover depending on offset of expanded toolbar cover height,
            mToolbarCover.setAlpha(1.0f - Math.abs(verticalOffset/(float)appBarLayout.getTotalScrollRange()));
            //when toolbar is fully collapsed show name of audio file in toolbar and back button
            if(Math.abs(verticalOffset)-appBarLayout.getTotalScrollRange() == 0) {
                mCollapsingToolbarLayout.setTitleEnabled(true);
                mCollapsingToolbarLayout.setTitle(mTitleBottomTransparentLayer.getText().toString());
                mActionBar.setDisplayShowTitleEnabled(true);
                mActionBar.setDisplayHomeAsUpEnabled(true);
                mActionBar.setDisplayShowHomeEnabled(true);
            }
            //hides title of toolbar and back button if toolbar is fully expanded
            else {
                mCollapsingToolbarLayout.setTitleEnabled(false);
                mActionBar.setDisplayShowTitleEnabled(false);
                mActionBar.setDisplayHomeAsUpEnabled(false);
                mActionBar.setDisplayShowHomeEnabled(false);
            }
        });
    }


    private void showFabs(){
        mSaveButton.hide();
        mDownloadCoverButton.show();
        mEditButton.show();
        mAutoFixButton.show();
        mFloatingActionMenu.show();
    }

    private void editMode(){
        mDownloadCoverButton.hide();
        mEditButton.hide();
        mAutoFixButton.hide();
        mFloatingActionMenu.hide();
        mSaveButton.show();
    }

    /**
     * Enters edit mode, for modify manually
     * the information about the song
     */
    private void disableAppBarLayout(){
        //shrink toolbar to make it easy to user
        //focus in editing tags
        mAppBarLayout.setExpanded(false);

        mFloatingActionMenu.animate().rotation(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFloatingActionMenu.hide();
                mSaveButton.show();
                mSaveButton.setOnClickListener(null);
                mSaveButton.setOnClickListener(v -> mTrackDetailPresenter.validateInputData());
                mToolbarCover.setEnabled(false);
                mUpdateCoverButton.setEnabled(false);
                editMode();
            }
        });
    }


    /**
     * Initializes MediaPlayer and setup
     * of fields
     */
    private void setupMediaPlayer(String path){
        mPlayer.setPath(path);
    }

    @Override
    public void onBackPressed(){
        if(mTrackDetailPresenter != null)
            mTrackDetailPresenter.onBackPressed();
    }

    private void onCoverChanged(byte[] bytes) {
        mAppBarLayout.setExpanded(true);
        GlideApp.with(this).
                load(bytes)
                .error(R.drawable.ic_album_white_48px)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                .fitCenter()
                .into(mToolbarCover);
    }

    /**
     * This method creates the references to visual elements
     * in layout
     */
    private void setupDataInfoFields(){
        //editable edit texts from song
        mTitleField = mLayout.findViewById(R.id.track_name_details);
        mArtistField = mLayout.findViewById(R.id.artist_name_details);
        mAlbumField = mLayout.findViewById(R.id.album_name_details);
        mNumberField = mLayout.findViewById(R.id.track_number);
        mYearField = mLayout.findViewById(R.id.track_year);
        mGenreField = mLayout.findViewById(R.id.track_genre);

        //Additional data fields
        mSubtitleLayer = mLayout.findViewById(R.id.track_path);
        mImageSize = mLayout.findViewById(R.id.imageSize);
        mChangeImage = mLayout.findViewById(R.id.change_image_button);
        mFileSize = mLayout.findViewById(R.id.file_size);
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

    public void cancelIdentification(){
        mTrackDetailPresenter.cancelIdentification();
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
        //Shrink toolbar to make it easy to user
        //focus in editing tags

        //Enable edit text for edit them
        mTitleField.setEnabled(true);
        mArtistField.setEnabled(true);
        mAlbumField.setEnabled(true);
        mNumberField.setEnabled(true);
        mYearField.setEnabled(true);
        mGenreField.setEnabled(true);

        mImageSize.setVisibility(View.GONE);
        mChangeImage.setVisibility(View.VISIBLE);

        mTitleField.requestFocus();
        InputMethodManager imm =(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mTitleField,InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Remove onIdentificationError tags
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

        mImageSize.setVisibility(View.VISIBLE);
        mChangeImage.setVisibility(View.GONE);
        mToolbarCover.setEnabled(true);
        //to hide it, call the method again
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            assert imm != null;
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        showFabs();
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


    private void addToolbarButtonsListeners(){
        mExtractCoverButton.setOnMenuItemClickListener(menuItem -> {
            mTrackDetailPresenter.saveAsImageFileFrom(Constants.MANUAL);
            return false;
        });

        mUpdateCoverButton.setOnMenuItemClickListener(menuItem -> {
            mTrackDetailPresenter.hideFabMenu();
            editCover(TrackDetailFragment.INTENT_GET_AND_UPDATE_FROM_GALLERY);
            return false;
        });

        addPlayAction();

        removeItem.setOnMenuItemClickListener(item -> {
            mTrackDetailPresenter.removeCover();
            return false;
        });

        //performs a web search in navigator
        //using the title and artist name
        searchInWebItem.setOnMenuItemClickListener(item -> {
            searchInfoForTrack();
            return false;
        });
    }

    private void addPlayAction(){
        mPlayPreviewButton.setOnMenuItemClickListener(null);
        mPlayPreviewButton.setOnMenuItemClickListener(item -> {
            try {
                if(PreferenceManager.getDefaultSharedPreferences(getActivity().
                        getApplicationContext()).getBoolean("key_use_embed_player",true))
                    mPlayer.playPreview();
                else
                    mTrackDetailPresenter.openInExternalApp(getActivity().getApplicationContext());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    private void addStopAction(){
        mPlayPreviewButton.setOnMenuItemClickListener(null);
        mPlayPreviewButton.setOnMenuItemClickListener(item -> {
            mPlayer.stopPreview();
            return false;
        });
    }

    @Override
    public void onApiInitialized() {
        Snackbar snackbar;
        snackbar = AndroidUtils.getSnackbar(mLayout, getActivity().getApplicationContext());
        snackbar.setText(R.string.api_initialized2);
        snackbar.show();
        mTrackDetailPresenter.onApiInitialized();
    }

    @Override
    public void onNetworkConnected(Void param) {
        //Do nothing
    }

    @Override
    public void onNetworkDisconnected(Void param) {
        mTrackDetailPresenter.cancelIdentification();
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation animation = super.onCreateAnimation(transit, enter, nextAnim);

        if (animation == null && nextAnim != 0) {
            animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
        }

        if (animation != null && getView() != null)
            getView().setLayerType(View.LAYER_TYPE_HARDWARE, null);

        if(animation != null)
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(getView() != null)
                getView().setLayerType(View.LAYER_TYPE_NONE, null);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        return animation;
    }
}
