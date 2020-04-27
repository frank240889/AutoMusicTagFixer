package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.audioplayer.SimpleMediaPlayer;
import mx.dev.franco.automusictagfixer.databinding.ActivityTrackDetailBinding;
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams;
import mx.dev.franco.automusictagfixer.identifier.IdentificationManager;
import mx.dev.franco.automusictagfixer.ui.AndroidViewModelFactory;
import mx.dev.franco.automusictagfixer.ui.InformativeFragmentDialog;
import mx.dev.franco.automusictagfixer.ui.sdcardinstructions.SdCardInstructionsActivity;
import mx.dev.franco.automusictagfixer.utilities.ActionableMessage;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Message;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static mx.dev.franco.automusictagfixer.utilities.Constants.CorrectionActions.MODE;
import static mx.dev.franco.automusictagfixer.utilities.Constants.MEDIA_STORE_ID;

public class TrackDetailActivity extends AppCompatActivity implements ManualCorrectionDialogFragment.OnManualCorrectionListener,
        CoverIdentificationResultsFragment.OnCoverCorrectionListener,
        SemiAutoCorrectionDialogFragment.OnSemiAutoCorrectionListener,
        HasSupportFragmentInjector {

    public static final int INTENT_OPEN_GALLERY = 1;
    public static final int INTENT_GET_AND_UPDATE_FROM_GALLERY = 2;
    public static final String TRACK_DATA = BuildConfig.APPLICATION_ID + ".track_data";

    @Inject
    DispatchingAndroidInjector<Fragment> fragmentDispatchingAndroidInjector;
    @Inject
    AndroidViewModelFactory androidViewModelFactory;
    @Inject
    SimpleMediaPlayer mPlayer;
    @Inject
    IdentificationManager mIdentificationManager;

    private MenuItem mPlayPreviewMenuItem;
    private MenuItem mManualEditMenuItem;
    private MenuItem mSearchInWebMenuItem;
    private MenuItem mTrackDetailsMenuItem;

    private TrackDetailFragment mTrackDetailFragment;
    private TrackDetailViewModel mTrackDetailViewModel;
    private ActivityTrackDetailBinding mViewDataBinding;
    private ActionBar mActionBar;
    boolean mEditMode = false;
    private Snackbar mNoDismissibleSnackbar;
    private MenuItem mRenameTrackItem;
    private AppBarLayout.OnOffsetChangedListener mOffsetChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        mViewDataBinding = DataBindingUtil.
                setContentView(this, R.layout.activity_track_detail);

        mViewDataBinding.setLifecycleOwner(this);

        mTrackDetailViewModel = new ViewModelProvider(this, androidViewModelFactory).
                get(TrackDetailViewModel.class);

        mViewDataBinding.setViewModel(mTrackDetailViewModel);
        setSupportActionBar(mViewDataBinding.toolbar);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setTitle(R.string.details);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
        mViewDataBinding.collapsingToolbarLayout.setTitleEnabled(false);
        hideFabs();

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra(TrackDetailActivity.TRACK_DATA);

        mTrackDetailFragment = (TrackDetailFragment) getSupportFragmentManager().
                findFragmentByTag(TrackDetailFragment.class.getName());

        if(mTrackDetailFragment == null)
            mTrackDetailFragment = TrackDetailFragment.newInstance(
                    bundle.getInt(MEDIA_STORE_ID),
                    bundle.getInt(MODE));

        getSupportFragmentManager().beginTransaction().
                replace(R.id.track_detail_container_fragments, mTrackDetailFragment).
                commit();

        setupMediaPlayer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_details_track_dialog, menu);
        mPlayPreviewMenuItem = menu.findItem(R.id.action_play);
        mManualEditMenuItem = menu.findItem(R.id.action_edit_manual);
        mSearchInWebMenuItem = menu.findItem(R.id.action_web_search);
        mTrackDetailsMenuItem = menu.findItem(R.id.action_details);
        mRenameTrackItem = menu.findItem(R.id.action_rename);

        mTrackDetailViewModel.observeLoadingState().observe(this, this::loading);
        mTrackDetailViewModel.observeLoadingMessage().observe(this, this::onLoadingMessage);
        mTrackDetailViewModel.observeReadingResult().observe(this, this::onSuccessLoad);
        mTrackDetailViewModel.observeAudioData().observe(this, aVoid -> {});
        mTrackDetailViewModel.observeWritingFinishedEvent().observe(this, this::onWritingResult);
        mTrackDetailViewModel.observeTrackLoaded().observe(this, track ->
                mIdentificationManager.
                        setIdentificationType(IdentificationManager.ALL_TAGS).
                        startIdentification(track));

        mTrackDetailViewModel.observeConfirmationRemoveCover().observe(this, this::onConfirmRemovingCover);
        mTrackDetailViewModel.observeCoverSavingResult().observe(this, this::onActionableMessage);
        mTrackDetailViewModel.onMessage().observe(this, this::onLoadingMessage);
        setupIdentificationObserves();
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
            mViewDataBinding.appBarLayout.removeOnOffsetChangedListener(mOffsetChangeListener);
            mOffsetChangeListener = (appBarLayout, verticalOffset) -> {
                if (verticalOffset == 0) {
                    mViewDataBinding.appBarLayout.removeOnOffsetChangedListener(mOffsetChangeListener);
                    TrackDetailActivity.super.onBackPressed();
                }
            };
            mViewDataBinding.appBarLayout.addOnOffsetChangedListener(mOffsetChangeListener);
            mViewDataBinding.appBarLayout.setExpanded(true, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIdentificationManager.cancel();
        if (mNoDismissibleSnackbar != null)
            mNoDismissibleSnackbar.dismiss();
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
                        asyncBitmapDecoder.decodeBitmap(source, getBitmapDecoderCallback(requestCode));
                    }
                    else {
                        asyncBitmapDecoder.decodeBitmap(getApplicationContext().getContentResolver(),
                                imageData, getBitmapDecoderCallback(requestCode) );
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
    public void onMissingTagsButton(CorrectionParams correctionParams) {
        mPlayer.stopPreview();
        mTrackDetailViewModel.performCorrection(correctionParams);
    }

    /**
     * Callback from {@link SemiAutoCorrectionDialogFragment} when
     * user pressed apply all tags button
     */
    @Override
    public void onOverwriteTagsButton(CorrectionParams correctionParams) {
        mPlayer.stopPreview();
        mTrackDetailViewModel.performCorrection(correctionParams);
    }

    @Override
    public void onManualCorrection(CorrectionParams correctionParams) {
        mPlayer.stopPreview();
        mTrackDetailViewModel.performCorrection(correctionParams);
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
    public void saveAsImageButton(String id) {
        mTrackDetailViewModel.saveAsImageFileFrom(id);
    }

    @Override
    public void saveAsCover(CorrectionParams coverCorrectionParams) {
        mPlayer.stopPreview();
        mTrackDetailViewModel.performCorrection(coverCorrectionParams);
    }

    private AndroidUtils.AsyncBitmapDecoder.AsyncBitmapDecoderCallback getBitmapDecoderCallback(int requestCode){
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
        mViewDataBinding.fabAutofix.setOnClickListener(v -> {

            mIdentificationManager
                    .setIdentificationType(IdentificationManager.ALL_TAGS)
                    .startIdentification(mTrackDetailViewModel.getCurrentTrack());
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
        mOffsetChangeListener = new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset < 0) {
                    if (!mViewDataBinding.fabAutofix.isExtended()) {
                        mViewDataBinding.fabAutofix.extend();
                        mViewDataBinding.fabSaveInfo.extend();
                    }
                } else {
                    if (mViewDataBinding.fabAutofix.isExtended()) {
                        mViewDataBinding.fabAutofix.shrink();
                        mViewDataBinding.fabSaveInfo.shrink();
                    }
                }

                //set alpha of cover depending on offset of expanded toolbar cover height,
                mViewDataBinding.cardContainerCover.setAlpha(1.0f - Math.abs(verticalOffset / (float) appBarLayout.getTotalScrollRange()));
            }
        };
        mViewDataBinding.appBarLayout.addOnOffsetChangedListener(mOffsetChangeListener);
    }

    private void onWritingResult(Void voids) {
        enableEditModeElements();
        showFabs();
        enableAppBarLayout();
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
    }

    private void disableEditModeElements() {
        mManualEditMenuItem.setEnabled(false);
        mViewDataBinding.coverArtMenu.setEnabled(false);
        mViewDataBinding.coverArtMenu.setVisibility(GONE);
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
     * @param voids null object.
     */
    private void onSuccessLoad(Void voids) {
        mPlayer.setPath(mTrackDetailViewModel.getCurrentTrack().getPath());
        addFloatingActionButtonListeners();
        addAppBarOffsetListener();
        addToolbarButtonsListeners();
        addListenerCoverMenu();
        showFabs();
        mViewDataBinding.fabSaveInfo.shrink();
        mViewDataBinding.fabAutofix.shrink();
    }

    /**
     * Add the observers for identification events.
     */
    private void setupIdentificationObserves() {
        mIdentificationManager.observeIdentificationEvent().observe(this, identificationEvent -> {
            Log.d(identificationEvent.getClass().getName(), identificationEvent.isIdentifying() + "");
            if (identificationEvent.isIdentifying()) {

                mViewDataBinding.progressView.setVisibility(VISIBLE);
                disableEditModeElements();

                 mNoDismissibleSnackbar = AndroidUtils.createNoDismissibleSnackbar(
                        mViewDataBinding.rootContainerDetails,
                        identificationEvent.getMessage()
                );
                 mNoDismissibleSnackbar.setAction(R.string.cancel, v -> mIdentificationManager.cancel());
                 mNoDismissibleSnackbar.show();
            }
            else {

                mViewDataBinding.progressView.setVisibility(GONE);
                if (mNoDismissibleSnackbar != null) {
                    mNoDismissibleSnackbar.dismiss();
                }

                enableEditModeElements();
            }
        });

        mIdentificationManager.observeSuccessIdentification().observe(this, identificationType -> {
            if (identificationType == IdentificationManager.ALL_TAGS) {
                SemiAutoCorrectionDialogFragment semiAutoCorrectionDialogFragment =
                        (SemiAutoCorrectionDialogFragment) getSupportFragmentManager().
                                findFragmentByTag(SemiAutoCorrectionDialogFragment.class.getCanonicalName());

                if (semiAutoCorrectionDialogFragment == null)
                    semiAutoCorrectionDialogFragment = SemiAutoCorrectionDialogFragment.
                            newInstance(mTrackDetailViewModel.getCurrentTrack().getMediaStoreId()+"");

                if (!semiAutoCorrectionDialogFragment.isAdded())
                    semiAutoCorrectionDialogFragment.show(getSupportFragmentManager(),
                        semiAutoCorrectionDialogFragment.getClass().getCanonicalName());
            }
            else {
                CoverIdentificationResultsFragment coverIdentificationResultsFragment =
                        CoverIdentificationResultsFragment.newInstance(mTrackDetailViewModel.getCurrentTrack().getMediaStoreId()+"");
                coverIdentificationResultsFragment.show(getSupportFragmentManager(),
                        coverIdentificationResultsFragment.getClass().getCanonicalName());
            }
        });

        mIdentificationManager.observeMessage().observe(this, this::showInformativeMessage);
    }

    /**
     * Set the listeners to toolbar buttons.
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

        mTrackDetailsMenuItem.setOnMenuItemClickListener(item -> {

            MetadataDetailsFragment metadataDetailsFragment =
                    (MetadataDetailsFragment) getSupportFragmentManager().
                            findFragmentByTag(MetadataDetailsFragment.class.getName());

            if(metadataDetailsFragment == null)
                metadataDetailsFragment = MetadataDetailsFragment.newInstance();

            metadataDetailsFragment.show(getSupportFragmentManager(),
                    metadataDetailsFragment.getClass().getName());
            return false;
        });

        mRenameTrackItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ChangeFilenameDialogFragment changeFilenameDialogFragment =
                        ChangeFilenameDialogFragment.newInstance(mTrackDetailViewModel.getCurrentTrack().getMediaStoreId()+"");
                changeFilenameDialogFragment.
                        setOnChangeNameListener(new ChangeFilenameDialogFragment.OnChangeNameListener() {
                    @Override
                    public void onAcceptNewName(CorrectionParams inputParams) {
                        mTrackDetailViewModel.renameFile(inputParams);
                    }
                    @Override
                    public void onCancelRename() {
                        changeFilenameDialogFragment.dismiss();
                    }
                });
                changeFilenameDialogFragment.show(getSupportFragmentManager(),
                        changeFilenameDialogFragment.getClass().getName());
                return false;
            }
        });
    }

    /**
     * Set the listener to create the pop up cover art menu, and respond to
     * these actions.
     */
    private void addListenerCoverMenu() {
        mViewDataBinding.coverArtMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(TrackDetailActivity.this, v);
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            menuInflater.inflate(R.menu.menu_cover_art_options, popupMenu.getMenu());
            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(item -> {

                switch (item.getItemId()) {
                    case R.id.action_identify_cover:
                            mIdentificationManager.
                                    setIdentificationType(IdentificationManager.ONLY_COVER).
                                    startIdentification(mTrackDetailViewModel.getCurrentTrack());
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

    private void onLoadingMessage(String s) {
        Snackbar snackbar = AndroidUtils.createSnackbar(mViewDataBinding.rootContainerDetails,
                s);
        snackbar.setDuration(Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    private void onLoadingMessage(Integer message) {
        onLoadingMessage(getString(message));
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

    private void showInformativeMessage(String message) {
        Snackbar snackbar = AndroidUtils.createSnackbar(
                mViewDataBinding.rootContainerDetails,
                message
        );
        snackbar.setDuration(Snackbar.LENGTH_SHORT);
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
            mViewDataBinding.progressView.setVisibility(GONE);
            enableEditModeElements();
        }
    }
}
