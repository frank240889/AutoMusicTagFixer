package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.databinding.ActivityTrackDetailBinding;
import mx.dev.franco.automusictagfixer.identifier.IdentificationParams;
import mx.dev.franco.automusictagfixer.ui.AndroidViewModelFactory;
import mx.dev.franco.automusictagfixer.ui.InformativeFragmentDialog;
import mx.dev.franco.automusictagfixer.ui.sdcardinstructions.SdCardInstructionsActivity;
import mx.dev.franco.automusictagfixer.utilities.ActionableMessage;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Message;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.SimpleMediaPlayer;
import mx.dev.franco.automusictagfixer.utilities.SuccessIdentification;

import static android.view.View.VISIBLE;

public class TrackDetailActivity extends AppCompatActivity implements ManualCorrectionDialogFragment.OnManualCorrectionListener,
        CoverIdentificationResultsFragmentBase.OnCoverCorrectionListener,
        SemiAutoCorrectionDialogFragment.OnSemiAutoCorrectionListener,
        HasSupportFragmentInjector {
    public static final int INTENT_OPEN_GALLERY = 1;
    public static final int INTENT_GET_AND_UPDATE_FROM_GALLERY = 2;
    @Inject
    DispatchingAndroidInjector<Fragment> fragmentDispatchingAndroidInjector;
    @Inject
    AndroidViewModelFactory androidViewModelFactory;
    @Inject
    SimpleMediaPlayer mPlayer;

    private MenuItem mPlayPreviewMenuItem;
    private MenuItem mManualEditMenuItem;
    private MenuItem mSearchInWebMenuItem;
    private TrackDetailFragment mTrackDetailFragment;
    private TrackDetailViewModel mTrackDetailViewModel;
    private Intent mIntent;
    private ActivityTrackDetailBinding mViewDataBinding;
    ActionBar mActionBar;
    boolean mEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        mViewDataBinding = DataBindingUtil.
                setContentView(this, R.layout.activity_track_detail);
        mViewDataBinding.setLifecycleOwner(this);
        mTrackDetailViewModel = ViewModelProviders.of(this, androidViewModelFactory).get(TrackDetailViewModel.class);
        mViewDataBinding.setViewModel(mTrackDetailViewModel);
        mViewDataBinding.progressView.setVisibility(VISIBLE);
        setSupportActionBar(mViewDataBinding.toolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setTitle(R.string.details);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
        mViewDataBinding.collapsingToolbarLayout.setTitleEnabled(false);
        hideFabs();

        mIntent = getIntent();
        Bundle bundle = mIntent.getBundleExtra("track_data");

        mTrackDetailFragment = (TrackDetailFragment) getSupportFragmentManager().
                findFragmentByTag(TrackDetailFragment.class.getName());

        if(mTrackDetailFragment == null)
            mTrackDetailFragment = TrackDetailFragment.newInstance(
                    bundle.getInt(Constants.MEDIA_STORE_ID),
                    bundle.getInt(Constants.CorrectionActions.MODE));

        getSupportFragmentManager().beginTransaction().
                replace(R.id.track_detail_container_fragments, mTrackDetailFragment).
                commit();

    }

    private void setupObservers() {
        mTrackDetailViewModel.observeLoadingState().observe(this, this::loading);
        mTrackDetailViewModel.observeActionableMessage().observe(this, this::onActionableMessage);
        mTrackDetailViewModel.observeMessage().observe(this, this::onMessage);
        mTrackDetailViewModel.observeSuccessIdentification().observe(this, this::onIdentificationResults);
        mTrackDetailViewModel.observeCachedIdentification().observe(this, this::onIdentificationResults);
        mTrackDetailViewModel.observeFailIdentification().observe(this, this::onActionableMessage);
        mTrackDetailViewModel.observeConfirmationRemoveCover().observe(this, this::onConfirmRemovingCover);
        mTrackDetailViewModel.observeRenamingResult().observe(this, this::onMessage);
        mTrackDetailViewModel.observeCoverSavingResult().observe(this, this::onActionableMessage);
        mTrackDetailViewModel.observeTrack().observe(this, track -> mPlayer.setPath(track.getPath()));
        mTrackDetailViewModel.observeLoadingMessage().observe(this, this::onLoadingMessage);
        mTrackDetailViewModel.observeWritingResult().observe(this, this::onWritingResult);
        mTrackDetailViewModel.observeReadingResult().observe(this, message -> {
            if(message == null) {
                onSuccessLoad(null);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_details_track_dialog, menu);
        mPlayPreviewMenuItem = menu.findItem(R.id.action_play);
        mManualEditMenuItem = menu.findItem(R.id.action_edit_manual);
        mSearchInWebMenuItem = menu.findItem(R.id.action_web_search);
        setupMediaPlayer();
        setupObservers();
        return true;
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentDispatchingAndroidInjector;
    }

    @Override
    public void onStop() {
        super.onStop();
        mPlayer.stopPreview();
    }

    @Override
    public void onBackPressed() {
        if(mEditMode) {
            enableEditModeElements();
            showFabs();
            enableAppBarLayout();
            mTrackDetailFragment.disableFields();
            mTrackDetailViewModel.restorePreviousValues();
        }
        else {
            super.onBackPressed();
        }
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
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case INTENT_GET_AND_UPDATE_FROM_GALLERY:
            case INTENT_OPEN_GALLERY:
                if (data != null){
                    Uri imageData = data.getData();
                    AndroidUtils.AsyncBitmapDecoder asyncBitmapDecoder = new AndroidUtils.AsyncBitmapDecoder();
                    mViewDataBinding.appBarLayout.setExpanded(true, true);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        ImageDecoder.Source source = ImageDecoder.
                                createSource(getApplicationContext().getContentResolver(), imageData);
                        asyncBitmapDecoder.decodeBitmap(source, getCallback(requestCode));
                    }
                    else {
                        asyncBitmapDecoder.decodeBitmap(getApplicationContext().getContentResolver(),
                                imageData, getCallback(requestCode) );
                    }
                }
                break;

            case RequiredPermissions.REQUEST_PERMISSION_SAF:
                String msg;
                if (resultCode == Activity.RESULT_OK) {
                    // The document selected by the user won't be returned in the intent.
                    // Instead, a URI to that document will be contained in the return intent
                    // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
                    boolean res = AndroidUtils.grantPermissionSD(getApplicationContext(), data);
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

                AndroidUtils.createSnackbar(mViewDataBinding.rootContainerDetails, msg).show();
                break;
        }

    }

    private AndroidUtils.AsyncBitmapDecoder.AsyncBitmapDecoderCallback getCallback(int requestCode){
        return new AndroidUtils.AsyncBitmapDecoder.AsyncBitmapDecoderCallback() {
            @Override
            public void onBitmapDecoded(Bitmap bitmap) {
                ImageWrapper imageWrapper = new ImageWrapper();
                imageWrapper.width = bitmap.getWidth();
                imageWrapper.height = bitmap.getHeight();
                imageWrapper.bitmap = bitmap;
                imageWrapper.requestCode = requestCode;

                mTrackDetailViewModel.fastCoverChange(imageWrapper);
            }

            @Override
            public void onDecodingError(Throwable throwable) {
                Snackbar snackbar = AndroidUtils.getSnackbar(
                        mViewDataBinding.rootContainerDetails,
                        mViewDataBinding.rootContainerDetails.getContext());
                String msg = getString(R.string.error_load_image) + ": " + throwable.getMessage();
                snackbar.setText(msg);
                snackbar.setDuration(Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        };
    }

    private void setupMediaPlayer() {
        mPlayer.addListener(new SimpleMediaPlayer.OnMediaPlayerEventListener() {
            @Override
            public void onStartPlaying() {
                mPlayPreviewMenuItem.setIcon(
                        ContextCompat.getDrawable(TrackDetailActivity.this, R.drawable.ic_stop_white_24dp));
                addStopAction();
            }
            @Override
            public void onStopPlaying() {
                mPlayPreviewMenuItem.setIcon(
                        ContextCompat.getDrawable(TrackDetailActivity.this,R.drawable.ic_play_arrow_white_24px));
                addPlayAction();
            }
            @Override
            public void onCompletedPlaying() {
                mPlayPreviewMenuItem.setIcon(
                        ContextCompat.getDrawable(TrackDetailActivity.this,R.drawable.ic_play_arrow_white_24px));
                addPlayAction();
            }
            @Override
            public void onErrorPlaying(int what, int extra) {
                mPlayPreviewMenuItem.setEnabled(false);
            }
        });
    }

    /**
     * Alternates the stop to the play action.
     */
    private void addPlayAction(){
        mPlayPreviewMenuItem.setOnMenuItemClickListener(null);
        mPlayPreviewMenuItem.setOnMenuItemClickListener(item -> {
            if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("key_use_embed_player",true)) {
                try {
                    mPlayer.playPreview();
                } catch (IOException e) {
                    Snackbar snackbar = AndroidUtils.createSnackbar(mViewDataBinding.rootContainerDetails,
                            R.string.cannot_play_track);
                    snackbar.show();
                }
            }
            else {
                AndroidUtils.openInExternalApp(mTrackDetailViewModel.absolutePath.getValue(), this);
            }
            return false;
        });
    }

    /**
     * Alternates the play to the stop action.
     */
    private void addStopAction(){
        mPlayPreviewMenuItem.setOnMenuItemClickListener(null);
        mPlayPreviewMenuItem.setOnMenuItemClickListener(item -> {
            mPlayer.stopPreview();
            return false;
        });
    }


    /**
     * This method creates the references to visual elements
     * in layout
     */
    private void hideFabs(){
        mViewDataBinding.fabAutofix.hide();
        mViewDataBinding.fabSaveInfo.hide();
    }

    /**
     * Add listeners for corresponding objects to
     * respond to user interactions
     */

    private void addFloatingActionButtonListeners(){
        //runs track id
        mViewDataBinding.fabAutofix.setOnClickListener(v ->{
            mTrackDetailViewModel.startIdentification(new IdentificationParams(IdentificationParams.ALL_TAGS));
        });

        mViewDataBinding.fabSaveInfo.setOnClickListener(v -> {
            ManualCorrectionDialogFragment manualCorrectionDialogFragment =
                    ManualCorrectionDialogFragment.newInstance(mTrackDetailViewModel.title.getValue());
            manualCorrectionDialogFragment.show(getSupportFragmentManager(),
                    manualCorrectionDialogFragment.getClass().getCanonicalName());
        });
    }

    /**
     * Adds a effect to fading down the cover when user scroll up and fading up to the cover when user
     * scrolls down.
     */
    private void addAppBarOffsetListener(){
        mViewDataBinding.appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if(verticalOffset < 0) {
                if(!mViewDataBinding.fabAutofix.isExtended()) {
                    mViewDataBinding.fabAutofix.extend();
                    mViewDataBinding.fabSaveInfo.extend();
                }
            }
            else {
                if(mViewDataBinding.fabAutofix.isExtended()) {
                    mViewDataBinding.fabAutofix.shrink();
                    mViewDataBinding.fabSaveInfo.shrink();
                }
            }

            //set alpha of cover depending on offset of expanded toolbar cover height,
            mViewDataBinding.cardContainerCover.setAlpha(1.0f - Math.abs(verticalOffset/(float)appBarLayout.getTotalScrollRange()));
        });
    }

    private void onWritingResult(Message actionableMessage) {
        enableEditModeElements();
        showFabs();
        enableAppBarLayout();
        if(actionableMessage instanceof ActionableMessage) {
            onActionableMessage((ActionableMessage) actionableMessage);
        }
        else{
            onMessage(actionableMessage);
        }
    }

    /**
     * Disable the Save Fab button.
     */
    private void showFabs(){
        mViewDataBinding.fabAutofix.show();
        mViewDataBinding.fabSaveInfo.hide();
    }


    /**
     * Enable the Save Fab button.
     */
    private void editMode(){
        disableAppBarLayout();
        disableEditModeElements();
        mViewDataBinding.fabAutofix.hide();
        mViewDataBinding.fabSaveInfo.show();
        mTrackDetailFragment.enableFieldsToEdit();
        mEditMode = true;
    }

    private void disableEditModeElements() {
        mManualEditMenuItem.setEnabled(false);
        mViewDataBinding.coverArtMenu.setEnabled(false);
        mViewDataBinding.coverArtMenu.setVisibility(View.GONE);
        mViewDataBinding.fabAutofix.hide();
    }

    private void enableEditModeElements() {
        mManualEditMenuItem.setEnabled(true);
        mViewDataBinding.coverArtMenu.setEnabled(true);
        mViewDataBinding.coverArtMenu.setVisibility(VISIBLE);
        mViewDataBinding.fabAutofix.show();
    }

    /**
     * Enters edit mode, for modify manually
     * the information about the song
     */
    private void disableAppBarLayout() {
        mViewDataBinding.appBarLayout.setExpanded(false);
    }

    /**
     * Exits edit mode, for modify manually
     * the information about the song
     */
    private void enableAppBarLayout() {
        //shrink toolbar to make it easy to user
        //focus in editing tags
        mViewDataBinding.appBarLayout.setExpanded(true);
    }

    /**
     * Starts a external app to search info about the current track.
     */
    private void searchInfoForTrack(){
        mTrackDetailFragment.searchInfoForTrack();
    }

    /**
     * Callback when data from track is completely
     * loaded.
     * @param message The message to show.
     */
    private void onSuccessLoad(Message message) {
        addFloatingActionButtonListeners();
        addAppBarOffsetListener();
        addToolbarButtonsListeners();
        addCoverMenu();
        mViewDataBinding.fabSaveInfo.shrink();
        mViewDataBinding.fabAutofix.shrink();
        showFabs();
        mViewDataBinding.progressView.findViewById(R.id.cancel_button).
                setOnClickListener(v -> mTrackDetailViewModel.cancelTasks());
    }


    /**
     * Set the listeners to FAB buttons.
     */
    private void addToolbarButtonsListeners(){
        addPlayAction();

        //performs a web trackSearch in navigator
        //using the title and artist name
        mSearchInWebMenuItem.setOnMenuItemClickListener(item -> {
            searchInfoForTrack();
            return false;
        });

        mManualEditMenuItem.setOnMenuItemClickListener(item -> {
            editMode();
            return false;
        });
    }

    private void addCoverMenu() {
        mViewDataBinding.coverArtMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(TrackDetailActivity.this, v);
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            menuInflater.inflate(R.menu.menu_cover_art_options, popupMenu.getMenu());
            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.action_identify_cover:
                        mTrackDetailViewModel.startIdentification(
                                new IdentificationParams(IdentificationParams.ONLY_COVER));
                        break;
                    case R.id.action_update_cover:
                        editCover(TrackDetailFragment.INTENT_GET_AND_UPDATE_FROM_GALLERY);
                        break;
                    case R.id.action_extract_cover:
                        mTrackDetailViewModel.extractCover();
                        break;
                    case R.id.action_remove_cover:
                        mTrackDetailViewModel.removeCover();
                        break;
                }
                return false;
            });
        });
    }

    /**
     * Creates a OnClickListener object to respond according to an Action object.
     * @param action The action to execute.
     * @return A OnclickListener object.
     */
    private View.OnClickListener createOnClickListener (ActionableMessage action) {
        switch (action.getAction()) {
            case URI_ERROR:
                return view -> startActivity(new Intent(this, SdCardInstructionsActivity.class));
            case MANUAL_CORRECTION:
                return view -> editMode();
            case WATCH_IMAGE:
                return view -> AndroidUtils.openInExternalApp(action.getDetails(), view.getContext());
        }
        return null;
    }

    private void onLoadingMessage(Integer message) {
        ((TextView)mViewDataBinding.progressView.findViewById(R.id.status_message)).setText(message);
    }

    private void onIdentificationResults(SuccessIdentification successIdentification) {
        if(successIdentification.getIdentificationType() == SuccessIdentification.ALL_TAGS){
            SemiAutoCorrectionDialogFragment semiAutoCorrectionDialogFragment =
                    SemiAutoCorrectionDialogFragment.newInstance(successIdentification.getMediaStoreId());
            semiAutoCorrectionDialogFragment.show(getSupportFragmentManager(),
                    semiAutoCorrectionDialogFragment.getClass().getCanonicalName());

        }
        else {
            CoverIdentificationResultsFragmentBase coverIdentificationResultsFragmentBase =
                    CoverIdentificationResultsFragmentBase.newInstance(successIdentification.getMediaStoreId());
            coverIdentificationResultsFragmentBase.show(getSupportFragmentManager(),
                    coverIdentificationResultsFragmentBase.getClass().getCanonicalName());
        }
    }

    /**
     * Opens a dialog to select a image
     * to apply as new embed cover art.
     * @param codeIntent The code to distinguish if we pressed the cover toolbar,
     *                   the action button "Galería" from snackbar or "Añadir carátula de galería"
     *                   from main container.
     */
    public void editCover(int codeIntent){
        Intent selectorImageIntent = new Intent(Intent.ACTION_PICK);
        selectorImageIntent.setType("image/*");
        startActivityForResult(selectorImageIntent,codeIntent);
    }

    /**
     * Callback from {@link SemiAutoCorrectionDialogFragment} when
     * user pressed apply only missing tags button
     */
    @Override
    public void onMissingTagsButton(SemiAutoCorrectionParams semiAutoCorrectionParams) {
        mPlayer.stopPreview();
        mTrackDetailViewModel.performCorrection(semiAutoCorrectionParams);
    }

    /**
     * Callback from {@link SemiAutoCorrectionDialogFragment} when
     * user pressed apply all tags button
     */
    @Override
    public void onOverwriteTagsButton(SemiAutoCorrectionParams semiAutoCorrectionParams) {
        mPlayer.stopPreview();
        mTrackDetailViewModel.performCorrection(semiAutoCorrectionParams);
    }

    @Override
    public void onManualCorrection(ManualCorrectionParams inputParams) {
        mPlayer.stopPreview();
        mTrackDetailViewModel.performCorrection(inputParams);
    }

    @Override
    public void onCancelManualCorrection() {
        enableEditModeElements();
        mViewDataBinding.toolbarCoverArt.setEnabled(true);
        mTrackDetailFragment.disableFields();
        enableAppBarLayout();
        mTrackDetailViewModel.restorePreviousValues();
    }

    @Override
    public void saveAsImageButton(CoverCorrectionParams coverCorrectionParams) {
        mTrackDetailViewModel.saveAsImageFileFrom(coverCorrectionParams);
    }

    @Override
    public void saveAsCover(CoverCorrectionParams coverCorrectionParams) {
        mPlayer.stopPreview();
        mTrackDetailViewModel.performCorrection(coverCorrectionParams);
    }

    /**
     * Callback to confirm the deletion of current cover;
     */
    private void onConfirmRemovingCover(Void voids) {
        InformativeFragmentDialog informativeFragmentDialog = InformativeFragmentDialog.
                newInstance(R.string.attention,
                        R.string.message_remove_cover_art_dialog,
                        R.string.accept, R.string.cancel_button, this);
        informativeFragmentDialog.showNow(getSupportFragmentManager(),
                informativeFragmentDialog.getClass().getCanonicalName());

        informativeFragmentDialog.setOnClickBasicFragmentDialogListener(
                new InformativeFragmentDialog.OnClickBasicFragmentDialogListener() {
                    @Override
                    public void onPositiveButton() {
                        informativeFragmentDialog.dismiss();
                        mTrackDetailViewModel.confirmRemoveCover();
                    }

                    @Override
                    public void onNegativeButton() {
                        informativeFragmentDialog.dismiss();
                    }
                }
        );
    }


    /**
     * Shows a message into a Snackbar
     * @param message The message object to show.
     */
    protected void onMessage(Message message){
        if(message == null)
            return;

        Snackbar snackbar = AndroidUtils.createSnackbar(
                mViewDataBinding.rootContainerDetails,
                message
        );
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    /**
     * Shows a message and an action into a Snackbar.
     * @param actionableMessage The message object with action to take.
     */
    protected void onActionableMessage(Message actionableMessage) {
        if(actionableMessage == null)
            return;

        Snackbar snackbar = AndroidUtils.createActionableSnackbar(
                mViewDataBinding.rootContainerDetails,
                actionableMessage,
                createOnClickListener((ActionableMessage) actionableMessage));
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    protected void loading(boolean showProgress) {
        if(showProgress) {
            mViewDataBinding.progressView.setVisibility(VISIBLE);
            disableEditModeElements();
        }
        else {
            mViewDataBinding.progressView.setVisibility(View.GONE);
            enableEditModeElements();
        }
    }
}
