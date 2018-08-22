package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.receivers.ResponseReceiver;
import mx.dev.franco.automusictagfixer.services.TrackIdentifier;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;
import mx.dev.franco.automusictagfixer.utilities.SimpleMediaPlayer;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

/**
 * Created by franco on 22/07/17.
 */

public class TrackDetailsActivity extends AppCompatActivity implements SimpleMediaPlayer.OnEventDispatchedListener,
        ResponseReceiver.OnResponse, TrackDetailFragment.OnFragmentInteractionListener {

    private static final String TAG = TrackDetailsActivity.class.getName();

    //Id from audio item list
    private int mCurrentItemId;
    //flag when user is editing info
    private boolean mEditMode = false;
    //Deafult action when activity is opened
    private int mCorrectionMode;
    //rootview
    private View mViewDetailsTrack;
    //Fabs to create a fab menu
    FloatingActionButton mEditButton;
    FloatingActionButton mDownloadCoverButton;
    FloatingActionButton mAutoFixButton;
    MenuItem mExtractCoverButton;
    FloatingActionButton mSaveButton;
    FloatingActionButton mFloatingActionMenu;

    private MenuItem mPlayPreviewButton;
    private TextView mLayerFileName;
    private Toolbar mToolbar;
    private ImageView mToolbarCover;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private AppBarLayout mAppBarLayout;
    private ActionBar mActionBar;
    //Reference to custom media mPlayer.
    private SimpleMediaPlayer mPlayer;

    //Receiver to handle responses
    private ResponseReceiver mReceiver;

    //Flag to indicate if mini fabs are shown or hidden
    private boolean mIsFloatingActionMenuOpen = false;

    //Buttons of toolbar menu
    private MenuItem removeItem;
    private MenuItem searchInWebItem;

    private static final int CROSS_FADE_DURATION = 200;
    private TrackDetailFragment mTrackDetailFragment;
    private String mCurrentPath;
    /**
     * Callback when is created the activity
     * @param savedInstanceState
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AutoMusicTagFixer.getContextComponent().inject(this);
        //windows is the top level in the view hierarchy,
        //it has a single Surface in which the contents of the window is rendered
        //A Surface is an object holding pixels that are being composited to the screen.
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);
        window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        //set the layout to this activity
        setContentView(R.layout.activity_track_details);

        //main layout or root view
        mViewDetailsTrack = findViewById(R.id.root_container_details);
        //We get the current instance of SimpleMediaPlayer object


        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setupMediaPlayer();
        setupFields();

        //if this intent comes when is touched any element from dialog in list of MainActivity
        mCorrectionMode = getIntent().getIntExtra(Constants.CorrectionModes.MODE,Constants.CorrectionModes.VIEW_INFO);
        //currentId of audioItem
        mCurrentItemId = getIntent().getIntExtra(Constants.MEDIA_STORE_ID,-1);

        registerReceivers();

    }

    /**
     * Callback to create menu in toolbar
     * @param menu the reference to created menu
     * @return
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_details_track_dialog, menu);
        mPlayPreviewButton = menu.findItem(R.id.action_play);
        mExtractCoverButton = menu.findItem(R.id.action_extract_cover);
        removeItem = menu.findItem(R.id.action_remove_cover);
        searchInWebItem = menu.findItem(R.id.action_web_search);
        mTrackDetailFragment = TrackDetailFragment.newInstance(mCurrentItemId, mCorrectionMode);
        getSupportFragmentManager().beginTransaction().
                replace(R.id.container_fragment_detail, mTrackDetailFragment).
                commit();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * This callback handles
     * the back button pressed from
     * Android system
     */
    @Override
    public void onBackPressed() {
        //hides fabs if are open
        if(mEditMode){
            mTrackDetailFragment.disableEditMode();
            return;
        }

        if(mIsFloatingActionMenuOpen){
            closeFABMenu();
            return;
        }

        //destroys activity
        dismiss();
        super.onBackPressed();
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
        mPlayer.removeListener();
        mViewDetailsTrack = null;
        mEditButton = null;
        mExtractCoverButton = null;
        mDownloadCoverButton = null;
        mAutoFixButton = null;
        mSaveButton = null;
        mFloatingActionMenu = null;

        mPlayer = null;
        mReceiver = null;
        mPlayPreviewButton = null;
        mLayerFileName = null;
        mToolbar = null;
        mToolbarCover = null;
        mCollapsingToolbarLayout = null;
        mAppBarLayout = null;
        mActionBar = null;

        System.gc();
    }

    /**
     * Callback when activities enters to pause state,
     * remove receivers if FixerTrackService is not processing any task
     * to save battery
     */
    @Override
    protected void onPause(){
        super.onPause();
    }

    /**
     * Callback when user starts interacting
     * with activity.
     * Here register receivers for handling
     * responses from FixerTrackService
     */
    @Override
    protected void onResume(){
        super.onResume();
    }

    /**
     * Register filters to handle intents from FixerTrackService
     */
    private void registerReceivers(){
        IntentFilter apiInitialized = new IntentFilter(Constants.GnServiceActions.ACTION_API_INITIALIZED);
        IntentFilter connectionLost = new IntentFilter(Constants.Actions.ACTION_CONNECTION_LOST);
        mReceiver = new ResponseReceiver(this, new Handler());
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver,apiInitialized);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver,connectionLost);
    }

    @Override
    public void onDataReady(String path) {
        addActionListeners();
        addToolbarButtonsListeners();
        additionalToolbarListeners();
        showFabs();
        enableMiniFabs(true);
        mCurrentPath = path;
    }

    @Override
    public void onDataError() {
        addToolbarButtonsListeners();
        Toast t = AndroidUtils.getToast(getApplicationContext());
        t.setText(R.string.could_not_read_file);
        t.setDuration(Toast.LENGTH_SHORT);
        t.show();
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onEditMode() {
        enableFieldsToEdit();
    }

    @Override
    public void onUnedit() {
        viewMode();
    }

    @Override
    public void onPerformingTask() {
        enableMiniFabs(false);
    }

    @Override
    public void onFinishedTask() {
        enableMiniFabs(true);
    }

    @Override
    public void onTitleToolbarChanged(String filename) {
        mCurrentPath = filename;
        mLayerFileName.setText(AudioItem.getFilename(filename));

    }

    @Override
    public void onCoverChanged(byte[] cover) {
        Log.d(TAG, "cover is null " + (cover == null));
        GlideApp.with(this).
                load(cover)
                .error(R.drawable.ic_album_white_48px)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                .fitCenter()
                .into(mToolbarCover);
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

    private void viewMode(){
        showFabs();
        disableFields();
    }

    /**
     * Enters edit mode, for modify manually
     * the information about the song
     */
    private void enableFieldsToEdit(){
        //shrink toolbar to make it easy to user
        //focus in editing tags
        mAppBarLayout.setExpanded(false);

        mFloatingActionMenu.animate().rotation(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mFloatingActionMenu.hide();
                mSaveButton.show();
                mSaveButton.setOnClickListener(null);
                mSaveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTrackDetailFragment.validateInputData();
                    }
                });
                mToolbarCover.setEnabled(false);
                editMode();
                mEditMode = true;
            }
        });
    }

    /**
     * Disables the fields and
     * leaves out from edit mode
     */

    private void disableFields(){
        mToolbarCover.setEnabled(true);
        mAppBarLayout.setExpanded(true);
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
     * Stops FixerTrackService, playback and
     * receivers and finishes current activity
     */
    private void dismiss() {
        mPlayer.stopPreview();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        mReceiver = null;
        finishAfterTransition();
        System.gc();
    }

    /**
     * Initializes MediaPlayer and setup
     * of fields
     */
    private void setupMediaPlayer(){
        mPlayer = SimpleMediaPlayer.getInstance(getApplicationContext());
        mPlayer.addListener(this);
    }

    /**
     * This method creates the references to visual elements
     * in layout
     */
    private void setupFields(){

        //collapsable toolbar
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mCollapsingToolbarLayout = findViewById(R.id.collapsingToolbarLayout);
        mAppBarLayout = findViewById(R.id.appBarLayout);
        mCollapsingToolbarLayout.setTitleEnabled(false);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(false);
        mToolbarCover = findViewById(R.id.toolbarCover);

        mLayerFileName = findViewById(R.id.titleTransparentLayer);

        //Floating action buttons
        mDownloadCoverButton = mViewDetailsTrack.findViewById(R.id.fabDownloadCover);
        mEditButton = mViewDetailsTrack.findViewById(R.id.fabEditTrackInfo);
        mAutoFixButton = mViewDetailsTrack.findViewById(R.id.fabAutofix);
        mFloatingActionMenu = mViewDetailsTrack.findViewById(R.id.fabMenu);
        mSaveButton = mViewDetailsTrack.findViewById(R.id.fabSaveInfo);
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

    private void addActionListeners(){
        //enable manual mode
        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                mTrackDetailFragment.enableEditMode();
            }
        });

        //runs track id
        mAutoFixButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                enableMiniFabs(false);
                mTrackDetailFragment.startIdentification(TrackIdentifier.ALL_TAGS);
            }
        });

        mDownloadCoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                mTrackDetailFragment.startIdentification(TrackIdentifier.JUST_COVER);
            }
        });

        //shows or hides mini fabs
        mFloatingActionMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mIsFloatingActionMenuOpen){
                    showFABMenu();
                }else{
                    closeFABMenu();
                }
            }
        });

        //updates only cover art
        mToolbarCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsFloatingActionMenuOpen)
                    closeFABMenu();

                editCover(TrackDetailFragment.INTENT_GET_AND_UPDATE_FROM_GALLERY);
            }
        });

    }

    private void addToolbarButtonsListeners(){
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                //set alpha of cover depending on offset of expanded toolbar cover height,
                mToolbarCover.setAlpha(1.0f - Math.abs(verticalOffset/(float)appBarLayout.getTotalScrollRange()));
                //when toolbar is fully collapsed show name of audio file in toolbar and back button
                if(Math.abs(verticalOffset)-appBarLayout.getTotalScrollRange() == 0) {
                    mCollapsingToolbarLayout.setTitleEnabled(true);
                    mCollapsingToolbarLayout.setTitle(mLayerFileName.getText().toString());
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
            }
        });

        //pressing back from toolbar, close activity
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mTrackDetailFragment.onActivityResult(requestCode, resultCode, data);
    }

    private void additionalToolbarListeners(){
        mExtractCoverButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                closeFABMenu();
                mTrackDetailFragment.extractCover();
                return false;
            }
        });

        addPlayAction();

        removeItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mTrackDetailFragment.removeCover();
                return false;
            }
        });

        //performs a web search in navigator
        //using the title and artist name
        searchInWebItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mTrackDetailFragment.searchInfoForTrack();
                return false;
            }
        });
    }

    /**
     * Enables and disables fabs
     * @param enable true for enable, false to disable
     */
    private void enableMiniFabs(boolean enable){
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
        mIsFloatingActionMenuOpen = true;
    }

    /**
     * Hides mini fabs
     */
    private void closeFABMenu() {
        mFloatingActionMenu.animate().rotation(0);
        mAutoFixButton.animate().translationY(0);
        mEditButton.animate().translationY(0);
        mDownloadCoverButton.animate().translationY(0);
        mIsFloatingActionMenuOpen = false;
    }

    private void addPlayAction(){
        mPlayPreviewButton.setOnMenuItemClickListener(null);
        mPlayPreviewButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    mPlayer.playPreview(mCurrentPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    private void addStopAction(){
        mPlayPreviewButton.setOnMenuItemClickListener(null);
        mPlayPreviewButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mPlayer.stopPreview();
                return false;
            }
        });
    }

    @Override
    public void onResponse(Intent intent) {
        String action = intent.getAction();
        Snackbar snackbar;
        switch (action) {
            //API is no initialized
            case Constants.GnServiceActions.ACTION_API_INITIALIZED:
                snackbar = AndroidUtils.getSnackbar(mViewDetailsTrack, getApplicationContext());
                snackbar.setText(R.string.api_initialized2);
                snackbar.show();
                break;
            case Constants.Actions.ACTION_CONNECTION_LOST:
                mTrackDetailFragment.cancelIdentification();
                break;
            default:
                break;
        }
    }
}
