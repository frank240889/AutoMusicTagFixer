package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.BaseFragment;
import mx.dev.franco.automusictagfixer.UI.MainActivity;
import mx.dev.franco.automusictagfixer.UI.results.IdentificationResultsFragment;
import mx.dev.franco.automusictagfixer.UI.sd_card_instructions.SdCardInstructionsActivity;
import mx.dev.franco.automusictagfixer.common.Action;
import mx.dev.franco.automusictagfixer.databinding.FragmentTrackDetailBinding;
import mx.dev.franco.automusictagfixer.utilities.ActionableMessage;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Constants.CorrectionActions;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;
import mx.dev.franco.automusictagfixer.utilities.Message;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.SimpleMediaPlayer;
import mx.dev.franco.automusictagfixer.utilities.SimpleMediaPlayer.OnMediaPlayerEventListener;

import static mx.dev.franco.automusictagfixer.utilities.Constants.GOOGLE_SEARCH;

/**
 * Use the {@link TrackDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackDetailFragment extends BaseFragment<TrackDetailViewModel> implements
    ManualCorrectionDialogFragment.OnManualCorrectionListener {

    public static final String TAG = TrackDetailFragment.class.getName();

    //Menu items
    private MenuItem mPlayPreviewMenuItem;
    private MenuItem mUpdateCoverMenuItem;
    private MenuItem mExtractCoverMenuItem;
    private MenuItem mRemoveMenuItem;
    private MenuItem mSearchInWebMenuItem;
    private ActionBar mActionBar;
    private FragmentTrackDetailBinding mFragmentTrackDetailBinding;
    @Inject
    SimpleMediaPlayer mPlayer;

    public TrackDetailFragment() {}

    public static TrackDetailFragment newInstance(int idTrack, int correctionMode) {
        TrackDetailFragment fragment = new TrackDetailFragment();
        Bundle bundle = AndroidUtils.getBundle(idTrack, correctionMode);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPlayer.addListener(new OnMediaPlayerEventListener() {
            @Override
            public void onStartPlaying() {
                mPlayPreviewMenuItem.setIcon(R.drawable.ic_stop_white_24px);
                addStopAction();
            }
            @Override
            public void onStopPlaying() {
                mPlayPreviewMenuItem.setIcon(R.drawable.ic_play_arrow_white_24px);
                addPlayAction();
            }
            @Override
            public void onCompletedPlaying() {
                mPlayPreviewMenuItem.setIcon(R.drawable.ic_play_arrow_white_24px);
            }
            @Override
            public void onErrorPlaying(int what, int extra) {
                mPlayPreviewMenuItem.setEnabled(false);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if(bundle != null)
            mViewModel.setInitialAction(
                bundle.getInt(CorrectionActions.MODE, CorrectionActions.VIEW_INFO));
        else
            mViewModel.setInitialAction(CorrectionActions.VIEW_INFO);

        setHasOptionsMenu(true);
    }

    @Override
    public TrackDetailViewModel getViewModel() {
        return ViewModelProviders.
            of(this, androidViewModelFactory).
            get(TrackDetailViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFragmentTrackDetailBinding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_track_detail,
                container,
                false);
        mFragmentTrackDetailBinding.setLifecycleOwner(this);
        mFragmentTrackDetailBinding.setViewModel(mViewModel);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.setViewModel(mViewModel);
        return mFragmentTrackDetailBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ((MainActivity)getActivity()).setSupportActionBar(mFragmentTrackDetailBinding.toolbar);
        mFragmentTrackDetailBinding.collapsingToolbarLayout.setTitleEnabled(false);
        mActionBar = ((MainActivity)getActivity()).getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(false);
        hideAllFabs();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_details_track_dialog, menu);
        mPlayPreviewMenuItem = menu.findItem(R.id.action_play);
        mExtractCoverMenuItem = menu.findItem(R.id.action_extract_cover);
        mUpdateCoverMenuItem = menu.findItem(R.id.action_update_cover);
        mRemoveMenuItem = menu.findItem(R.id.action_remove_cover);
        mSearchInWebMenuItem = menu.findItem(R.id.action_web_search);
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
                        ImageWrapper imageWrapper = new ImageWrapper();
                        imageWrapper.width = bitmap.getWidth();
                        imageWrapper.height = bitmap.getHeight();
                        imageWrapper.bitmap = bitmap;
                        imageWrapper.requestCode = requestCode;
                        mViewModel.validateImageSize(imageWrapper);
                    } catch(IOException e){
                        e.printStackTrace();
                        Snackbar snackbar = AndroidUtils.getSnackbar(
                                mFragmentTrackDetailBinding.rootContainerDetails, getActivity().getApplicationContext());
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
                    boolean res = AndroidUtils.grantPermissionSD(getActivity().getApplicationContext(), data);
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

                AndroidUtils.showToast(msg, getActivity());
                break;
        }

    }

    /**
     * Callback when user tried to set new cover from gallery but was not valid.
     */
    public void onInvalidImage() {
        Snackbar snackbar = AndroidUtils.getSnackbar(
                mFragmentTrackDetailBinding.rootContainerDetails, getActivity().getApplicationContext());
        snackbar.setText(getString(R.string.image_too_big));
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPlayer.removeListener();
        mPlayer = null;
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
    }

    /**
     * Callback when user tried to remove the cover but current track
     * has no cover.
     */
    public void onTrackHasNoCover() {
        Snackbar snackbar = AndroidUtils.getSnackbar(
                mFragmentTrackDetailBinding.rootContainerDetails, getActivity().getApplicationContext());
        snackbar.setText(getString(R.string.does_not_exist_cover));
        snackbar.setDuration(Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    /**
     * Callback to confirm the deletion of current cover;
     */
    public void onConfirmRemovingCover() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.message_remove_cover_art_dialog);
        builder.setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.cancel());
        builder.setPositiveButton(R.string.accept, (dialog, which) -> mViewModel.confirmRemoveCover());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }



    public void setCover(byte[] value) {
        onCoverChanged(value);
    }

    public void setStateMessage(String message, boolean visible) {
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.statusMessage.setVisibility(View.VISIBLE);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.statusMessage.setText(message);
    }

    public void loading(boolean showProgress) {
        if(showProgress) {
            mFragmentTrackDetailBinding.
                    layoutContentDetailsTrack.progressContainer.setVisibility(View.VISIBLE);
        }
        else {
            mFragmentTrackDetailBinding.
                    layoutContentDetailsTrack.progressContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Callback when could not read the track.
     * @param error The reason why could not read track.
     */
    public void onLoadError(String error) {
        //pressing back from toolbar, close activity
        mFragmentTrackDetailBinding.toolbar.setNavigationOnClickListener(v -> onConfirmExit());
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.changeImageButton.setVisibility(View.GONE);
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
    public void onSuccessLoad(String path) {
        //pressing back from toolbar, close activity
        mFragmentTrackDetailBinding.toolbar.setNavigationOnClickListener(v -> onConfirmExit());
        mFragmentTrackDetailBinding.
                layoutContentDetailsTrack.
                changeImageButton.setOnClickListener(v -> editCover(INTENT_OPEN_GALLERY));

        addFloatingActionButtonListeners();
        addAppBarOffsetListener();
        addToolbarButtonsListeners();
        showFabs();
        setupMediaPlayer(path);

        //Set action for "X" button
        mFragmentTrackDetailBinding.
                layoutContentDetailsTrack.
                cancelIdentification.
                setOnClickListener(v -> mViewModel.cancelIdentification());
    }

    /**
     * Loads the identification results and shows to the user.
     */
    public void onLoadIdentificationResults() {
        IdentificationResultsFragment identificationResultsFragment =
                IdentificationResultsFragment.newInstance(mViewModel.getTrackId());
        identificationResultsFragment.show(getChildFragmentManager(),
                IdentificationResultsFragment.class.getName());
    }

    /**
     * Callback from {@link IdentificationResultsFragment} when
     * user pressed apply only missing tags button
     */
    public void onMissingTagsButton(CorrectionParams correctionParams) {
        mViewModel.performCorrection(correctionParams);
    }

    /**
     * Callback from {@link IdentificationResultsFragment} when
     * user pressed apply all tags button
     */
    public void onOverwriteTagsButton(CorrectionParams correctionParams) {
        mViewModel.performCorrection(correctionParams);
    }

    /**
     * Callback from {@link IdentificationResultsFragment} when
     * user pressed save cover as image button
     */
    public void onSaveAsImageFile() {
        mViewModel.saveAsImageFileFrom(Constants.CACHED);
    }

    /**
     * Loads the identification results and shows to the user.
     * @param results The object containing the data.
     */
    public void onLoadCoverIdentificationResults(GnResponseListener.IdentificationResults results) {
        IdentificationResultsFragment identificationResultsFragment =
                IdentificationResultsFragment.newInstance(results, true);
        identificationResultsFragment.show(getChildFragmentManager(),
                IdentificationResultsFragment.class.getName());
    }

    /**
     * Callback when user cancelled the identification process.
     */
    public void onIdentificationCancelled() {
        Snackbar snackbar = AndroidUtils.getSnackbar(
                mFragmentTrackDetailBinding.rootContainerDetails, getActivity().getApplicationContext());
        if (snackbar != null) {
            snackbar.setText(R.string.identification_interrupted);
            snackbar.show();
        }
    }

    /**
     * Callback when correction process has successfully finished.
     */
    public void onSuccessfullyCorrection(String message) {
        Snackbar snackbar = AndroidUtils.getSnackbar(
                mFragmentTrackDetailBinding.rootContainerDetails, getActivity().getApplicationContext());
        snackbar.setText(message);
        snackbar.show();
    }

    public void onEnableFabs() {
        enableMiniFabs(true);
    }

    public void onDisableFabs() {
        enableMiniFabs(false);
    }

    /**
     * Callback when a correction process error ocurred.
     * @param message The message of error.
     * @param action The action to perform on snackbar.
     */
    public void onCorrectionError(String message, String action) {
        Snackbar snackbar = AndroidUtils.getSnackbar(
                mFragmentTrackDetailBinding.rootContainerDetails, getActivity().getApplicationContext());
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        if (action != null && action.equals(getString(R.string.get_permission))){
            snackbar.setAction(action, v -> getActivity().startActivity(new Intent(getActivity(), SdCardInstructionsActivity.class)));
        }
        else if(action != null && action.equals(getString(R.string.add_manual))){
            snackbar.setAction(action, v -> mViewModel.enableEditMode());
        }

        snackbar.setText(message);
        snackbar.show();
    }

    /**
     * Callback when saving cover as jpg image process has successfully finished.
     */
    public void onSuccessfullyFileSaved(final String message) {
        Snackbar snackbar = AndroidUtils.getSnackbar(
                mFragmentTrackDetailBinding.rootContainerDetails, getActivity().getApplicationContext());
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        snackbar.setText(String.format(getString(R.string.cover_saved), message));
        snackbar.setAction(R.string.watch, v -> AndroidUtils.openInExternalApp(message, getActivity().getApplicationContext()));
        snackbar.show();
    }

    public void onEnableEditMode() {
        disableAppBarLayout();
        enableFieldsToEdit();
    }

    public void onDisableEditMode() {
        disableFields();
    }

    public void onDisableEditModeAndRestore() {
        onDisableEditMode();
    }

    /**
     * Callback when user pressed mSaveButton and input data is invalid.
     */
    public void alertInvalidData(String message, int field) {
        EditText editText = mFragmentTrackDetailBinding.getRoot().findViewById(field);
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
    public void onDataValid() {
        ManualCorrectionDialogFragment manualCorrectionDialogFragment =
                ManualCorrectionDialogFragment.newInstance();

        manualCorrectionDialogFragment.showNow(getChildFragmentManager(),
                manualCorrectionDialogFragment.getClass().getName());
    }

    public void onShowFabMenu() {
        showFABMenu();
    }

    public void onHideFabMenu() {
        closeFABMenu();
    }

    @Override
    protected void callSuperOnBackPressed() {
        mViewModel.onBackPressed();
    }

    public void onConfirmExit() {
        super.callSuperOnBackPressed();
    }


    public void setCancelTaskEnabled(boolean enableCancelView) {
        mFragmentTrackDetailBinding.
                layoutContentDetailsTrack.
                cancelIdentification.
                setVisibility(enableCancelView ? View.VISIBLE : View.GONE);
    }

    /**
     * Enables and disables fabs
     * @param enable true for enable, false to disable
     */
    private void enableMiniFabs(boolean enable){
        mUpdateCoverMenuItem.setEnabled(enable);
        mFragmentTrackDetailBinding.toolbarCoverArt.setEnabled(enable);
        mFragmentTrackDetailBinding.fabDownloadCover.setEnabled(enable);
        mFragmentTrackDetailBinding.fabEditTrackInfo.setEnabled(enable);
        mFragmentTrackDetailBinding.fabAutofix.setEnabled(enable);
    }

    /**
     * Shows mini fabs
     */
    private void showFABMenu(){
        mFragmentTrackDetailBinding.fabMenu.animate().rotation(-400);
        mFragmentTrackDetailBinding.fabAutofix.animate().translationY(-getResources().getDimension(R.dimen.standard_55));
        mFragmentTrackDetailBinding.fabEditTrackInfo.animate().translationY(-getResources().getDimension(R.dimen.standard_105));
        mFragmentTrackDetailBinding.fabDownloadCover.animate().translationY(-getResources().getDimension(R.dimen.standard_155));
    }

    /**
     * Hides mini fabs
     */
    private void closeFABMenu() {
        mFragmentTrackDetailBinding.fabMenu.animate().rotation(0);
        mFragmentTrackDetailBinding.fabAutofix.animate().translationY(0);
        mFragmentTrackDetailBinding.fabEditTrackInfo.animate().translationY(0);
        mFragmentTrackDetailBinding.fabDownloadCover.animate().translationY(0);
    }

    /**
     * This method creates the references to visual elements
     * in layout
     */
    private void hideAllFabs(){
        mFragmentTrackDetailBinding.fabDownloadCover.hide();
        mFragmentTrackDetailBinding.fabEditTrackInfo.hide();
        mFragmentTrackDetailBinding.fabAutofix.hide();
        mFragmentTrackDetailBinding.fabMenu.hide();
        mFragmentTrackDetailBinding.fabSaveInfo.hide();
    }

    /**
     * Add listeners for corresponding objects to
     * respond to user interactions
     */

    private void addFloatingActionButtonListeners(){
        //enable manual mode
        mFragmentTrackDetailBinding.fabEditTrackInfo.setOnClickListener(v -> {
            mViewModel.enableEditMode();
        });

        //runs track id
        mFragmentTrackDetailBinding.fabAutofix.setOnClickListener(v -> {
            mViewModel.startIdentification();
        });

        mFragmentTrackDetailBinding.fabDownloadCover.setOnClickListener(v -> {
            mViewModel.startIdentification();
        });

        //shows or hides mini fabs
        mFragmentTrackDetailBinding.fabMenu.setOnClickListener(view -> {
            mViewModel.toggleFabMenu();
        });

        //updates only cover art
        mFragmentTrackDetailBinding.toolbarCoverArt.setOnClickListener(v -> {
            mViewModel.hideFabMenu();
            editCover(TrackDetailFragment.INTENT_GET_AND_UPDATE_FROM_GALLERY);
        });

    }

    private void addAppBarOffsetListener(){
        mFragmentTrackDetailBinding.appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            //set alpha of cover depending on offset of expanded toolbar cover height,
            mFragmentTrackDetailBinding.toolbarCoverArt.setAlpha(1.0f - Math.abs(verticalOffset/(float)appBarLayout.getTotalScrollRange()));
            //when toolbar is fully collapsed show name of audio file in toolbar and back button
            if(Math.abs(verticalOffset) - appBarLayout.getTotalScrollRange() == 0) {
                mFragmentTrackDetailBinding.collapsingToolbarLayout.setTitleEnabled(true);
                mFragmentTrackDetailBinding.collapsingToolbarLayout.setTitle(
                        mFragmentTrackDetailBinding.titleBottomTransparentLayer.getText().toString());
                mActionBar.setDisplayShowTitleEnabled(true);
                mActionBar.setDisplayHomeAsUpEnabled(true);
                mActionBar.setDisplayShowHomeEnabled(true);
            }
            //hides title of toolbar and back button if toolbar is fully expanded
            else {
                mFragmentTrackDetailBinding.collapsingToolbarLayout.setTitleEnabled(false);
                mActionBar.setDisplayShowTitleEnabled(false);
                mActionBar.setDisplayHomeAsUpEnabled(false);
                mActionBar.setDisplayShowHomeEnabled(false);
            }
        });
    }


    private void showFabs(){
        mFragmentTrackDetailBinding.fabDownloadCover.show();
        mFragmentTrackDetailBinding.fabEditTrackInfo.show();
        mFragmentTrackDetailBinding.fabAutofix.show();
        mFragmentTrackDetailBinding.fabMenu.show();
        mFragmentTrackDetailBinding.fabSaveInfo.hide();
    }

    private void editMode(){
        mFragmentTrackDetailBinding.fabDownloadCover.hide();
        mFragmentTrackDetailBinding.fabEditTrackInfo.hide();
        mFragmentTrackDetailBinding.fabAutofix.hide();
        mFragmentTrackDetailBinding.fabMenu.hide();
        mFragmentTrackDetailBinding.fabSaveInfo.show();
    }

    /**
     * Enters edit mode, for modify manually
     * the information about the song
     */
    private void disableAppBarLayout(){
        //shrink toolbar to make it easy to user
        //focus in editing tags
        mFragmentTrackDetailBinding.appBarLayout.setExpanded(false);

        mFragmentTrackDetailBinding.fabMenu.animate().rotation(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFragmentTrackDetailBinding.fabMenu.hide();
                mFragmentTrackDetailBinding.fabSaveInfo.show();
                mFragmentTrackDetailBinding.fabSaveInfo.setOnClickListener(null);
                mFragmentTrackDetailBinding.fabSaveInfo.setOnClickListener(v -> mViewModel.validateInputData());
                mFragmentTrackDetailBinding.toolbarCoverArt.setEnabled(false);
                mUpdateCoverMenuItem.setEnabled(false);
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
        mViewModel.onBackPressed();
    }

    private void onCoverChanged(byte[] bytes) {
        mFragmentTrackDetailBinding.appBarLayout.setExpanded(true);
        GlideApp.with(this).
                load(bytes)
                .error(R.drawable.ic_album_white_48px)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                .fitCenter()
                .into(mFragmentTrackDetailBinding.toolbarCoverArt);
    }

    private void searchInfoForTrack(){
        String title = mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackNameDetails.
                getText().toString();
        String artist = mFragmentTrackDetailBinding.layoutContentDetailsTrack.artistNameDetails.
                getText().toString();

        String queryString = title + (!artist.isEmpty() ? (" " + artist) : "");
        String query = GOOGLE_SEARCH + queryString;
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
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackNameDetails.setEnabled(true);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.artistNameDetails.setEnabled(true);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.albumNameDetails.setEnabled(true);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackNumber.setEnabled(true);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackYear.setEnabled(true);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackGenre.setEnabled(true);

        mFragmentTrackDetailBinding.layoutContentDetailsTrack.imageSize.setVisibility(View.GONE);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.changeImageButton.setVisibility(View.VISIBLE);

        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackNameDetails.requestFocus();
        InputMethodManager imm =(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackNameDetails,
                InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Remove onIdentificationError tags
     */
    private void removeErrorTags(){
        //get descendants instances of edit text
        ArrayList<View> fields = mFragmentTrackDetailBinding.getRoot().getFocusables(View.FOCUS_DOWN);
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

        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackNameDetails.clearFocus();
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackNameDetails.setEnabled(false);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.artistNameDetails.clearFocus();
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.artistNameDetails.setEnabled(false);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.albumNameDetails.clearFocus();
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.albumNameDetails.setEnabled(false);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackNumber.clearFocus();
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackNumber.setEnabled(false);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackYear.clearFocus();
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackYear.setEnabled(false);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackGenre.clearFocus();
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.trackGenre.setEnabled(false);

        mFragmentTrackDetailBinding.layoutContentDetailsTrack.imageSize.setVisibility(View.VISIBLE);
        mFragmentTrackDetailBinding.layoutContentDetailsTrack.changeImageButton.setVisibility(View.GONE);
        mFragmentTrackDetailBinding.toolbarCoverArt.setEnabled(true);
        //to hide it, call the method again
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            assert imm != null;
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
        catch (Exception ignored){}

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
        mExtractCoverMenuItem.setOnMenuItemClickListener(menuItem -> {
            mViewModel.saveAsImageFileFrom(Constants.MANUAL);
            return false;
        });

        mUpdateCoverMenuItem.setOnMenuItemClickListener(menuItem -> {
            mViewModel.hideFabMenu();
            editCover(TrackDetailFragment.INTENT_GET_AND_UPDATE_FROM_GALLERY);
            return false;
        });

        addPlayAction();

        mRemoveMenuItem.setOnMenuItemClickListener(item -> {
            mViewModel.removeCover();
            return false;
        });

        //performs a web trackSearch in navigator
        //using the title and artist name
        mSearchInWebMenuItem.setOnMenuItemClickListener(item -> {
            searchInfoForTrack();
            return false;
        });
    }

    private void addPlayAction(){
        mPlayPreviewMenuItem.setOnMenuItemClickListener(null);
        mPlayPreviewMenuItem.setOnMenuItemClickListener(item -> {
                if(PreferenceManager.getDefaultSharedPreferences(getActivity().
                        getApplicationContext()).getBoolean("key_use_embed_player",true)) {
                    try {
                        mPlayer.playPreview();
                    } catch (IOException e) {
                        Snackbar snackbar = AndroidUtils.createSnackbar(mFragmentTrackDetailBinding.rootContainerDetails,
                                R.string.cannot_play_track);
                        snackbar.show();
                    }
                }
                else {
                    mViewModel.openInExternalApp(getActivity().getApplicationContext());
                }
            return false;
        });
    }

    private void addStopAction(){
        mPlayPreviewMenuItem.setOnMenuItemClickListener(null);
        mPlayPreviewMenuItem.setOnMenuItemClickListener(item -> {
            mPlayer.stopPreview();
            return false;
        });
    }

    public void load(Bundle inputBundle){
        Bundle bundle = inputBundle == null ? getArguments() : inputBundle;
        if(bundle != null)
            mViewModel.loadInfoTrack(bundle.getInt(Constants.MEDIA_STORE_ID,-1));

    }

    @Override
    protected void onMessage(Message message){
        Snackbar snackbar = AndroidUtils.createSnackbar(
            mFragmentTrackDetailBinding.rootContainerDetails,
            message
            );
        snackbar.show();
    }

    @Override
    protected void onActionableMessage(ActionableMessage actionableMessage) {
        Snackbar snackbar = AndroidUtils.createActionableSnackbar(
            mFragmentTrackDetailBinding.rootContainerDetails,
            actionableMessage,
            createOnClickListener(actionableMessage.getAction()));
        snackbar.show();
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
                load(null);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        return animation;
    }

    private OnClickListener createOnClickListener (Action action) {

        return null;
    }

    @Override
    public void onAccept(ManualCorrectionDialogFragment.UIInputParams inputParams) {
        mViewModel.performCorrection(inputParams);
    }

    @Override
    public void onCancel() {
        disableFields();
        mViewModel.restorePreviousValues();
    }
}
