package mx.dev.franco.musicallibraryorganizer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.transition.TransitionManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.images.AndroidArtwork;
import org.jaudiotagger.tag.images.Artwork;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import mx.dev.franco.musicallibraryorganizer.database.DataTrackDbHelper;
import mx.dev.franco.musicallibraryorganizer.database.TrackContract;
import mx.dev.franco.musicallibraryorganizer.list.AudioItem;
import mx.dev.franco.musicallibraryorganizer.list.TrackAdapter;
import mx.dev.franco.musicallibraryorganizer.services.DetectorInternetConnection;
import mx.dev.franco.musicallibraryorganizer.services.FixerTrackService;
import mx.dev.franco.musicallibraryorganizer.services.Job;
import mx.dev.franco.musicallibraryorganizer.transitions.DetailsTransition;
import mx.dev.franco.musicallibraryorganizer.utilities.Constants;
import mx.dev.franco.musicallibraryorganizer.utilities.FileSaver;
import mx.dev.franco.musicallibraryorganizer.utilities.GlideApp;
import mx.dev.franco.musicallibraryorganizer.utilities.SimpleMediaPlayer;
import mx.dev.franco.musicallibraryorganizer.utilities.StringUtilities;

import static android.view.View.GONE;
import static mx.dev.franco.musicallibraryorganizer.services.GnService.apiInitialized;

/**
 * Created by franco on 22/07/17.
 */

public class TrackDetailsActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {



    //Intent type
    private static final int INTENT_OPEN_GALLERY = 1;
    private static final int INTENT_GET_AND_UPDATE_FROM_GALLERY = 2;
    //codes for determining the type of error when validating the fields
    private static final int HAS_EMPTY_FIELDS = 11;
    private static final int DATA_IS_TOO_LONG = 12;
    private static final int HAS_NOT_ALLOWED_CHARACTERS = 13;
    private static final int FILE_IS_PROCESSING = 14;
    private static final int REMOVE_COVER = 15;
    private static final int ADD_COVER = 16;
    private static final int UPDATE_ALL_METADATA = 17;
    private static final int UPDATE_TRACKED_ID_TAGS = 18;
    private static final int NO_INTERNET_CONNECTION_COVER_ART = 28;
    private static final int ACTION_NONE = 30;
    private static final int ACTION_ADD_COVER = 31 ;
    private static final int ACTION_VIEW_COVER = 32;
    private static final int DURATION = 200;


    //flag to indicate that is just required to download
    //the coverart
    private boolean mOnlyCoverArt = false;
    //Id from audio item_list
    private long mCurrentItemId;
    //flag when user is editing info
    private boolean mEditMode = false;
    //A reference to database connection
    private DataTrackDbHelper mDbHelper;
    private int mCorrectionMode = Constants.CorrectionModes.MANUAL;
    //rootview
    private View mViewDetailsTrack;
    //References to elements inside the layout
    private FloatingActionButton mEditButton;
    private FloatingActionButton mDownloadCoverButton;
    private FloatingActionButton mAutoFixButton;
    private FloatingActionButton mExtractCoverButton;
    private FloatingActionButton mSaveButton;
    private FloatingActionButton mFloatingActionMenu;

    private String mNewTitle;
    private String mNewArtist;
    private String mNewAlbum;
    private String mNewNumber;
    private String mNewYear;
    private String mNewGenre;
    private String mTrackPath;
    private String mCurrentDuration;
    private EditText mTitleField;
    private String mCurrentTitle;
    private EditText mArtistField;
    private String mCurrentArtist;
    private EditText mAlbumField;
    private String mCurrentAlbum;
    private EditText mNumberField;
    private String mCurrentNumber;
    private EditText mYearField;
    private String mCurrentYear;
    private EditText mGenreField;
    private String mCurrentGenre;
    private TextView mBitrateField;
    private MenuItem mPlayPreviewButton;
    private TextView mTitleLayer, mSubtitleLayer;
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
    private ImageView mToolbarCover;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private AppBarLayout mAppBarLayout;
    private ActionBar mActionBar;
    private NestedScrollView mContent;
    private Snackbar mSnackbar;
    private File mAudioFile;

    //temporal references to new values
    private byte[] mCurrentCoverArt;
    private byte[] mNewCoverArt;
    private int mCurrentCoverArtLength;
    private int mNewCoverArtLength = 0;

    //Reference to custom media mPlayer.
    private SimpleMediaPlayer mPlayer;
    //flag to set if data could be updated or not and inform to user
    private boolean mDataUpdated = false;
    //reference to current audio item_list being edited
    private AudioItem mCurrentAudioItem = null;
    //audio item_list to store response data of making a trackId inside this activity
    private AudioItem mTrackIdAudioItem = null;

    //Broadcast manager to manage the response from FixerTrackService intent service
    private LocalBroadcastManager mLocalBroadcastManager;
    //Filter only certain responses from FixerTrackService
    private IntentFilter mFilterActionCompleteTask;
    private IntentFilter mFilterActionApiInitialized;
    private IntentFilter mFilterActionNotFound;
    private IntentFilter mFilterActionDoneDetails;
    private IntentFilter mFilterActionConnectionLost;
    //Receiver to handle responses
    private ResponseReceiver mReceiver;

    //Flag for saving the result of validating the fields of layout
    private boolean mIsFloatingActionMenuOpen = false;

    //references to visual elements of container
    //that shows tags found when it makes trackId
    private CardView mTrackIdCard;
    private ImageView mTrackIdCover;
    private TextView mTrackIdTitle;
    private TextView mTrackIdArtist;
    private TextView mTrackIdAlbum;
    private TextView mTrackIdGenre;
    private TextView mTrackIdNumber;
    private TextView mTrackIdYear;
    private TextView mTrackIdCoverArtDimensions;
    private TrackAdapter mTrackAdapter;
    private String mFrequencyVal;
    private int mResolutionVal;
    private String mChannelsVal = "0";
    private String mFileType = "audio";
    private AudioFile mAudioTaggerFile;
    private Tag mTag;
    private AudioHeader mAudioHeader;
    private int mCurrentPosition;
    private boolean mError = false;
    private MenuItem removeItem;
    private MenuItem searchInWebItem;
    private String mMimeType = "";
    private String mExtension = "";
    private boolean mIsMp3 = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set mStatus bar translucent, these calls to window object must be done before setContentView
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);
        window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        //set the layout to this activity
        setContentView(R.layout.activity_track_details);

        //Create mReceiver and filters to handle responses from FixerTrackService
        mFilterActionDoneDetails = new IntentFilter(Constants.Actions.ACTION_DONE_DETAILS);
        mFilterActionCompleteTask = new IntentFilter(Constants.Actions.ACTION_COMPLETE_TASK);
        mFilterActionApiInitialized = new IntentFilter(Constants.GnServiceActions.ACTION_API_INITIALIZED);
        mFilterActionNotFound = new IntentFilter(Constants.Actions.ACTION_NOT_FOUND);
        mFilterActionConnectionLost = new IntentFilter(Constants.Actions.ACTION_CONNECTION_LOST);
        mReceiver = new ResponseReceiver();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        //main layout or root view
        mViewDetailsTrack = findViewById(R.id.rootContainerDetails);
        //We get the current instance of SimpleMediaPlayer object
        mPlayer = SimpleMediaPlayer.getInstance(getApplicationContext());
        mTrackAdapter = (TrackAdapter) mPlayer.getAdapter();

        //if this intent comes from dialog when it touches any element from list of MainActivity
        mCorrectionMode = getIntent().getIntExtra(Constants.CorrectionModes.MODE,Constants.CorrectionModes.VIEW_INFO);
        //currentId of audioItem
        mCurrentItemId = getIntent().getLongExtra(Constants.MEDIASTORE_ID,-1);

        mCurrentPosition = getIntent().getIntExtra(Constants.POSITION,0);
        mCurrentAudioItem = mTrackAdapter.getAudioItemByIdOrPath(mCurrentItemId, null); //getIntent().getParcelableExtra(FixerTrackService.AUDIO_ITEM); //MainActivity.getAudioItemByIdOrPath(mCurrentItemId,null);

        //current path to file
        mTrackPath = mCurrentAudioItem.getAbsolutePath();

        mExtension = AudioItem.getExtension(mTrackPath);
        mMimeType = AudioItem.getMimeType(mTrackPath);

        //get an instance of connection to DB
        mDbHelper = DataTrackDbHelper.getInstance(getApplicationContext());

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


        createSnackBar();

        init();
    }

    /**
     * Create a general snackbar for reuse
     */

    private void createSnackBar() {
        this.mSnackbar = Snackbar.make(mViewDetailsTrack,"",Snackbar.LENGTH_SHORT);
        TextView tv = (TextView) this.mSnackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        mSnackbar.getView().setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.primaryLightColor));
        tv.setTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));
        mSnackbar.setActionTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));
    }

    /**
     * Shows the mSnackbar with the params received
     * @param duration how long is displayed the snackbar
     * @param msg message to display
     * @param action action to execute
     * @param path absolute path to file
     */
    private void showSnackBar(int duration, String msg, int action, final String path){
        if(mSnackbar != null){
            mSnackbar = null;
            createSnackBar();
        }

        if(action == ACTION_NONE){
            mSnackbar.setText(msg);
            mSnackbar.setDuration(duration);
            mSnackbar.setAction("",null);
        }
        else if(action == ACTION_ADD_COVER) {
            mSnackbar.setText(msg);
            mSnackbar.setDuration(duration);
            mSnackbar.setAction(R.string.add_cover, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editCover(INTENT_GET_AND_UPDATE_FROM_GALLERY);
                }
            });
        }
        else{
            mSnackbar.setText(msg);
            mSnackbar.setDuration(duration);
            mSnackbar.setAction(R.string.view_cover, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openInExternalApp(path);
                }
            });
        }


        mSnackbar.show();
    }

    /**
     * Open files in external app
     * @param path
     */
    private void openInExternalApp(String path){
        File file = new File(path);
        String type = AudioItem.getMimeType(path);
        try {

            Intent intent = new Intent();
            //default action is ACTION_VIEW
            intent.setAction(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "mx.dev.franco.musicallibraryorganizer.fileProvider", file);
                intent.setDataAndType(contentUri, type);
            } else {
                intent.setDataAndType(Uri.fromFile(file), type);
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param reason
     */

    private void setSnackBarMessage(int reason){
        String msg = "";
        switch (reason){
            case Constants.Conditions.NO_INTERNET_CONNECTION:
                msg = getString(R.string.no_internet_connection_semi_automatic_mode);
                break;
            case NO_INTERNET_CONNECTION_COVER_ART:
                msg = getString(R.string.no_internet_connection_download_cover);
                break;
            case Constants.Conditions.NO_INITIALIZED_API:
                msg = getString(R.string.initializing_recognition_api);
                break;
        }
        showSnackBar(Snackbar.LENGTH_SHORT, msg, ACTION_NONE, null);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_details_track_dialog, menu);
        mPlayPreviewButton = menu.findItem(R.id.action_play);
        mPlayPreviewButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

            if(Settings.SETTING_USE_EMBED_PLAYER) {
                try {
                    playPreview();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                openInExternalApp(mTrackPath);
            }
                return false;
            }
        });

        removeItem = menu.findItem(R.id.action_remove_cover);
        removeItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(mError)return false;

                if(mCurrentCoverArt == null){
                    showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.does_not_exist_cover),ACTION_NONE, null);
                    return false;
                }
                final AlertDialog.Builder builder = new AlertDialog.Builder(TrackDetailsActivity.this);
                builder.setTitle(R.string.title_remove_cover_art_dialog);
                builder.setMessage(R.string.message_remove_cover_art_dialog);
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AsyncUpdateData asyncUpdateData = new AsyncUpdateData(REMOVE_COVER, false);
                        asyncUpdateData.execute();
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return false;
            }
        });

        searchInWebItem = menu.findItem(R.id.action_web_search);
        searchInWebItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(mError) return false;
                String queryString = mCurrentTitle + (!mCurrentArtist.isEmpty() ? (" " + mCurrentArtist) : "");
                String query = "https://www.google.com/#q=" + queryString;
                Uri uri = Uri.parse(query);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return false;
            }
        });
        return true;
    }


    @Override
    public void onBackPressed() {

        if(getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
            return;
        }

        if(mIsFloatingActionMenuOpen){
            closeFABMenu();
            return;
        }

        Log.d("mEditMode",mEditMode+"");
        if (mEditMode) {
            hideTrackedIdResultsLayout();
            disableFields();
            setPreviousValues();
            return;
        }


        dismiss();
        super.onBackPressed();
    }

    private void hideTrackedIdResultsLayout(){
        if(mTrackIdCard.getVisibility() == View.VISIBLE){
            mTrackIdCard.animate().setDuration(DURATION).alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mContent.setEnabled(true);
                    mTrackIdCard.setVisibility(GONE);
                    mContent.setVisibility(View.VISIBLE);
                    mFloatingActionMenu.show();
                    mSaveButton.hide();
                    mContent.animate().setDuration(DURATION).alpha(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            setPreviousValues();
                            disableFields();
                            enableMiniFabs(true);
                            mActionBar.show();
                        }
                    });

                }
            });
        }
        resetValues();
        mAppBarLayout.setExpanded(true);
    }


    private void resetValues(){
        //reset flags and
        mEditMode = false;
        mOnlyCoverArt = false;
        mNewCoverArt = mCurrentCoverArt;
        mNewCoverArtLength = mCurrentCoverArtLength;
        mNewTitle = "";
        mNewArtist = "";
        mNewAlbum = "";
        mNewGenre = "";
        mNewNumber = "";
        mNewYear = "";
        //make visible only fields when trackid value is available
        mTrackIdTitle.setText("");
        mTrackIdTitle.setVisibility(GONE);

        mTrackIdArtist.setText("");
        mTrackIdArtist.setVisibility(GONE);

        mTrackIdAlbum.setText("");
        mTrackIdAlbum.setVisibility(GONE);

        mTrackIdGenre.setText("");
        mTrackIdGenre.setVisibility(GONE);

        mTrackIdNumber.setText("");
        mTrackIdNumber.setVisibility(GONE);

        mTrackIdYear.setText("");
        mTrackIdYear.setVisibility(GONE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null){
                try {
                    Uri imageData = data.getData();
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageData);
                        if (bitmap.getHeight() > 1080 || bitmap.getWidth() > 1080) {
                            showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.image_too_big), ACTION_NONE, null);
                            mNewCoverArt = mCurrentCoverArt;
                            mCurrentCoverArtLength = mNewCoverArtLength;
                        }
                        else {
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                            mNewCoverArt = byteArrayOutputStream.toByteArray();
                            mNewCoverArtLength = mNewCoverArt.length;
                            GlideApp.with(mViewDetailsTrack).
                                    load(mNewCoverArt)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .fitCenter()
                                    .into(mToolbarCover);
                            if (requestCode == INTENT_OPEN_GALLERY) {
                                //show the new cover in mToolbar cover
                                if (!mEditMode) {
                                    enableFieldsToEdit();
                                }
                            } else {
                                AsyncUpdateData asyncUpdateData = new AsyncUpdateData(ADD_COVER, false);
                                asyncUpdateData.execute();
                            }
                            mAppBarLayout.setExpanded(true);
                        }
                    } catch(IOException e){
                        e.printStackTrace();
                        showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.error_load_image), ACTION_NONE, null);
                        mNewCoverArt = mCurrentCoverArt;
                        mNewCoverArtLength = mCurrentCoverArtLength;
                    }

                }
    }

    @Override
    public void onDestroy(){
        mDbHelper = null;
        mTitleField = null;
        mArtistField = null;
        mAlbumField = null;
        mNumberField = null;
        mYearField = null;
        mGenreField = null;
        mAudioTaggerFile = null;
        mTag = null;
        mAudioHeader = null;
        mCurrentCoverArt = null;
        mTrackAdapter = null;
        mViewDetailsTrack = null;
        mEditButton = null;
        mExtractCoverButton = null;
        mPlayer = null;
        mProgressBar = null;
        mCurrentAudioItem.setPosition(-1);
        mCurrentAudioItem = null;
        mTrackIdAudioItem = null;
        mFilterActionDoneDetails = null;
        mFilterActionCompleteTask = null;
        mReceiver = null;
        mLocalBroadcastManager = null;
        super.onDestroy();
    }

    private void dismiss() {
        if(ServiceHelper.withContext(getApplicationContext()).withService(FixerTrackService.CLASS_NAME).isServiceRunning()) {
            Intent intent = new Intent(this, FixerTrackService.class);
            stopService(intent);
        }
        try {
            stopPlayback();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
        finishAfterTransition();

        System.gc();
    }

    private void init(){
        mPlayer.setOnCompletionListener(this);
        setupFields();
    }

    /**
     * This method create the references to visual elements
     * in layout
     */
    private void setupFields(){

        //mToolbar and other properties
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
        mCollapsingToolbarLayout.setTitleEnabled(false);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(false);

        //details track elements
        mContent = (NestedScrollView) mViewDetailsTrack.findViewById(R.id.contentContainer);
        mProgressBar = (ProgressBar) mViewDetailsTrack.findViewById(R.id.progressSavingData);
        mTitleField = (EditText) mViewDetailsTrack.findViewById(R.id.track_name_details);
        mArtistField = (EditText) mViewDetailsTrack.findViewById(R.id.artist_name_details);
        mAlbumField = (EditText) mViewDetailsTrack.findViewById(R.id.album_name_details);
        mNumberField = (EditText) mViewDetailsTrack.findViewById(R.id.track_number);
        mYearField = (EditText) mViewDetailsTrack.findViewById(R.id.track_year);
        mGenreField = (EditText) mViewDetailsTrack.findViewById(R.id.track_genre);
        mTrackType = (TextView) mViewDetailsTrack.findViewById(R.id.track_type);
        mFrequency = (TextView) mViewDetailsTrack.findViewById(R.id.frequency);
        mResolution = (TextView) mViewDetailsTrack.findViewById(R.id.resolution);
        mChannels = (TextView) mViewDetailsTrack.findViewById(R.id.channels);
        mStatus = (TextView) mViewDetailsTrack.findViewById(R.id.status);

        //Floating action buttons
        mDownloadCoverButton = (FloatingActionButton) mViewDetailsTrack.findViewById(R.id.downloadCover);
        mExtractCoverButton = (FloatingActionButton) mViewDetailsTrack.findViewById(R.id.extractCover);
        mEditButton = (FloatingActionButton) mViewDetailsTrack.findViewById(R.id.editTrackInfo);
        mAutoFixButton = (FloatingActionButton) mViewDetailsTrack.findViewById(R.id.autofix);
        mFloatingActionMenu = (FloatingActionButton) mViewDetailsTrack.findViewById(R.id.floatingActionMenu);
        mSaveButton = (FloatingActionButton) mViewDetailsTrack.findViewById(R.id.saveInfo);
        mSaveButton.hide();


        mToolbarCover = (ImageView) mViewDetailsTrack.findViewById(R.id.toolbarCover);
        mTitleLayer = (TextView) mViewDetailsTrack.findViewById(R.id.titleTransparentLayer);
        mSubtitleLayer = (TextView) mViewDetailsTrack.findViewById(R.id.subtitleTransparentLayer);
        mImageSize = (TextView) mViewDetailsTrack.findViewById(R.id.imageSize);
        mFileSize = (TextView) mViewDetailsTrack.findViewById(R.id.fileSize);
        mTrackLength = (TextView) mViewDetailsTrack.findViewById(R.id.trackLength);
        mBitrateField = (TextView) mViewDetailsTrack.findViewById(R.id.bitrate);

        //references to elements of results layout when makes trackid
        mTrackIdCard = (CardView) mViewDetailsTrack.findViewById(R.id.trackIdCard);
        mTrackIdCover = (ImageView) mViewDetailsTrack.findViewById(R.id.trackidCoverArt);
        mTrackIdCoverArtDimensions = (TextView) mViewDetailsTrack.findViewById(R.id.trackidCoverArtDimensions);
        mTrackIdTitle = (TextView) mViewDetailsTrack.findViewById(R.id.trackidTitle);
        mTrackIdArtist = (TextView) mViewDetailsTrack.findViewById(R.id.trackidArtist);
        mTrackIdAlbum = (TextView) mViewDetailsTrack.findViewById(R.id.trackidAlbum);
        mTrackIdGenre = (TextView) mViewDetailsTrack.findViewById(R.id.trackidGenre);
        mTrackIdNumber = (TextView) mViewDetailsTrack.findViewById(R.id.trackidNumber);
        mTrackIdYear = (TextView) mViewDetailsTrack.findViewById(R.id.trackidYear);

        extractAndCacheData();
    }


    /**
     * Here it extracts data from audio file, then stores
     * for caching purpose in variables,
     * and sets these values into no editable text fields
     */
    private void extractAndCacheData(){
        boolean error = false;
        TagOptionSingleton.getInstance().setAndroid(true);
        TagOptionSingleton.getInstance().setWriteMp3GenresAsText(true);
        TagOptionSingleton.getInstance().setWriteMp4GenresAsText(true);
        String title, artist, album, number, year, genre;
        mIsMp3 = (mMimeType.equals("audio/mpeg_mp3") || mMimeType.equals("audio/mpeg") ) && mExtension.toLowerCase().equals("mp3");
        mAudioFile = new File(mTrackPath);
            try {
                mAudioTaggerFile = AudioFileIO.read(new File(mTrackPath));

                if(mIsMp3){
                    mAudioHeader = ((MP3File)mAudioTaggerFile).getMP3AudioHeader();
                    //get info from header before trying to read tags
                    mCurrentDuration = mAudioHeader.getTrackLength() + "";
                    mBitrate = mAudioHeader.getBitRate();
                    mFrequencyVal = mAudioHeader.getSampleRate();
                    mResolutionVal = mAudioHeader.getBitsPerSample();
                    mChannelsVal = mAudioHeader.getChannels();
                    mFileType = mAudioHeader.getFormat();
                    //Log.d("hasID3v1Tag",((MP3File)mAudioTaggerFile).hasID3v1Tag() + "");
                    //Log.d("hasID3v2Tag",((MP3File)mAudioTaggerFile).hasID3v2Tag() + "");
                    if(((MP3File)mAudioTaggerFile).hasID3v1Tag() && !((MP3File) mAudioTaggerFile).hasID3v2Tag()){
                        //create new version of ID3v2
                        ID3v24Tag id3v24Tag = new ID3v24Tag( ((MP3File)mAudioTaggerFile).getID3v1Tag() );
                        mAudioTaggerFile.setTag(id3v24Tag);
                        mTag = ((MP3File) mAudioTaggerFile).getID3v2TagAsv24();
                        //Log.d("converted_tag","converted_tag");
                    }
                    else {
                        if(((MP3File) mAudioTaggerFile).hasID3v2Tag()) {
                            mTag = ((MP3File) mAudioTaggerFile).getID3v2Tag();
                            //Log.d("get_v24_tag","get_v24_tag");
                        }
                        //Has no tags? create a new one, but no save until
                        //user apply changes
                        else {
                            ID3v24Tag id3v24Tag = new ID3v24Tag();
                            ((MP3File) mAudioTaggerFile).setID3v2Tag(id3v24Tag);
                            mTag = ((MP3File) mAudioTaggerFile).getID3v2Tag();
                            //Log.d("create_v24_tag","create_v24_tag");
                        }
                    }


                }
                else {
                    mAudioHeader = mAudioTaggerFile.getAudioHeader();
                    //get info from header before trying to read tags
                    mCurrentDuration = mAudioHeader.getTrackLength() + "";
                    mBitrate = mAudioHeader.getBitRate();
                    mFrequencyVal = mAudioHeader.getSampleRate();
                    mResolutionVal = mAudioHeader.getBitsPerSample();
                    mChannelsVal = mAudioHeader.getChannels();
                    mFileType = mAudioHeader.getFormat();

                    mTag = mAudioTaggerFile.getTag() == null ? mAudioTaggerFile.createDefaultTag() : mAudioTaggerFile.getTag();
                }


                //get cover art and save reference to it
                mCurrentCoverArt = mTag.getFirstArtwork() == null ? null : mTag.getFirstArtwork().getBinaryData();
                mCurrentCoverArtLength = mCurrentCoverArt == null ? 0 : mCurrentCoverArt.length;
                //initial values of new cover art will be the same of current cover art
                mNewCoverArt = mCurrentCoverArt;
                mNewCoverArtLength = mCurrentCoverArtLength;

                //set global references to metadata;
                //this is useful for avoid reading tags every time
                //we need them, besides, we use them when we are about
                //to commit changes to file tags, saving only those
                //that have changed and not every tag, or in case
                //no one have changed, don't commit anything, saving access
                //to file system
                mCurrentTitle = mTag.getFirst(FieldKey.TITLE);
                mCurrentArtist = mTag.getFirst(FieldKey.ARTIST);
                mCurrentAlbum = mTag.getFirst(FieldKey.ALBUM);
                mCurrentNumber = mTag.getFirst(FieldKey.TRACK);
                mCurrentYear = mTag.getFirst(FieldKey.YEAR);
                mCurrentGenre = mTag.getFirst(FieldKey.GENRE);


                error = false;
            } catch (CannotReadException | IOException | ReadOnlyFileException | InvalidAudioFrameException | TagException e) {
                e.printStackTrace();

                mCurrentAudioItem.setStatus(AudioItem.FILE_ERROR_READ);
                showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.could_not_read_file) + " " + e.getMessage(), ACTION_NONE, null);
                mTitleLayer.setText(mAudioFile.getName());
                mSubtitleLayer.setText(AudioItem.getRelativePath(mAudioFile.getParent()));
                mError = true;
                mFloatingActionMenu.hide();
                mDownloadCoverButton.hide();
                mExtractCoverButton.hide();
                mEditButton.hide();
                mAutoFixButton.hide();
                mFloatingActionMenu.hide();
                mSaveButton.hide();
                mSaveButton.hide();
                error = true;

            }
            finally {

                setCurrentValues(error);
            }


    }

    /**
     * Sets the extracted values in the corresponding
     * View objects
     */
    private void setCurrentValues(boolean error){

        //file name over cover
        mTitleLayer.setText(mAudioFile.getName());
        //path to file over cover
        mSubtitleLayer.setText(AudioItem.getRelativePath(mAudioFile.getParent()));

        //set values of additional info about song
        mTrackType.setText(mFileType);
        mImageSize.setText(AudioItem.getStringImageSize(mCurrentCoverArt));
        mFileSize.setText(AudioItem.getFileSize(mAudioFile.length()));
        mTrackLength.setText(AudioItem.getHumanReadableDuration(mCurrentDuration));
        mStatus.setText(getStatusText());
        mStatus.setCompoundDrawablesWithIntrinsicBounds(getStatusDrawable(),null,null,null);
        mBitrateField.setText(AudioItem.getBitrate(mBitrate));
        mFrequency.setText(AudioItem.getFrequency(mFrequencyVal));
        mResolution.setText(AudioItem.getResolution(mResolutionVal));
        mChannels.setText(mChannelsVal);

        if(error)
            return;

        //load a placeholder in case cover is not present
        GlideApp.with(mViewDetailsTrack).
                load(mCurrentCoverArt != null ? mCurrentCoverArt : getDrawable(R.drawable.ic_album_white_48px))
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .transition(DrawableTransitionOptions.withCrossFade())
                .fitCenter()
                .placeholder(R.drawable.ic_album_white_48px)
                .into(mToolbarCover);

        //set values on edit text objects
        mTitleField.setText(mCurrentTitle);
        mArtistField.setText(mCurrentArtist);
        mAlbumField.setText(mCurrentAlbum);
        mNumberField.setText(mCurrentNumber);
        mYearField.setText(mCurrentYear);
        mGenreField.setText(mCurrentGenre);


        //when intent brings correction mode  == MANUAL, enable fields
        //to start immediately editing it
        if (mCorrectionMode == Constants.CorrectionModes.MANUAL) {
            enableFieldsToEdit();
        }

        addActionListeners();
    }

    /**
     * Add listeners for corresponding objects to
     * respond to user interactions
     */

    private void addActionListeners(){

        mExtractCoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                if(mCurrentCoverArt == null){
                    showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.can_not_extract_null_cover),ACTION_NONE,null);
                    return;
                }

                String newImageAbsolutePath = FileSaver.saveFile(mCurrentCoverArt, mCurrentTitle, mCurrentArtist, mCurrentAlbum);
                if(!newImageAbsolutePath.equals(FileSaver.NULL_DATA) && !newImageAbsolutePath.equals(FileSaver.NO_EXTERNAL_STORAGE_WRITABLE) && !newImageAbsolutePath.equals(FileSaver.INPUT_OUTPUT_ERROR)) {

                    showSnackBar(7000, getString(R.string.cover_extracted), ACTION_VIEW_COVER, newImageAbsolutePath);
                    //lets inform to system that one file has been created
                    MediaScannerConnection.scanFile(
                                                    getApplicationContext(),
                                                    new String[]{newImageAbsolutePath},
                                                    new String[]{MimeTypeMap.getFileExtensionFromUrl(newImageAbsolutePath)},
                                                    null);
                }
                else {
                    showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.cover_not_saved), ACTION_NONE, null);
                }

            }
        });

        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                editInfoTrack();
            }
        });

        mAutoFixButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTrackId();
            }
        });

        mDownloadCoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();

                mOnlyCoverArt = true;

                //Check if exist trackidItem cached from previous request
                //before making a request for saving data,
                //this object only persist while the activity is open
                if(mTrackIdAudioItem != null){
                    handleCoverArt();
                    return;
                }

                //This function requires some contidions to work, check them before
                //continue

                //we put this in AsyncTask because the isConnected method
                //makes a network operation which blocks UI if we execute
                //from UI Thread
                new AsyncTask<Void,Void, Integer>(){
                    @Override
                    protected Integer doInBackground(Void... voids){
                        int canContinue = allowExecute();
                        return canContinue;
                    }
                    @Override
                    protected void onPostExecute(Integer canContinue){
                        if(canContinue != 0) {
                            setSnackBarMessage(canContinue);
                            return;
                        }

                        showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.downloading_cover),ACTION_NONE, null);
                        mProgressBar.setVisibility(View.VISIBLE);

                        enableMiniFabs(false);
                        mToolbarCover.setEnabled(false);
                        Intent intent = new Intent(TrackDetailsActivity.this,FixerTrackService.class);
                        intent.putExtra(Constants.Activities.FROM_EDIT_MODE, Constants.Activities.DETAILS_ACTIVITY);
                        intent.putExtra(Constants.MEDIASTORE_ID, mCurrentItemId);
                        startService(intent);
                    }
                }.execute();

            }
        });

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

        mToolbarCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsFloatingActionMenuOpen)
                    closeFABMenu();

                editCover(INTENT_GET_AND_UPDATE_FROM_GALLERY);
            }
        });

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                mToolbarCover.setAlpha(1.0f - Math.abs(verticalOffset/(float)appBarLayout.getTotalScrollRange()));
                if(Math.abs(verticalOffset)-appBarLayout.getTotalScrollRange() == 0) {
                    mCollapsingToolbarLayout.setTitleEnabled(true);
                    mCollapsingToolbarLayout.setTitle(mAudioFile.getName());
                    mActionBar.setDisplayShowTitleEnabled(true);
                    mActionBar.setDisplayHomeAsUpEnabled(true);
                    mActionBar.setDisplayShowHomeEnabled(true);
                }
                else {
                    mCollapsingToolbarLayout.setTitleEnabled(false);
                    mActionBar.setDisplayShowTitleEnabled(false);
                    mActionBar.setDisplayHomeAsUpEnabled(false);
                    mActionBar.setDisplayShowHomeEnabled(false);
                }
            }
        });

        mTrackIdCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //no cover art was found?
                if(mTrackIdAudioItem.getCoverArt() == null)
                    return;

                TransitionManager.beginDelayedTransition((ViewGroup) mViewDetailsTrack);


                Bundle bundle = new Bundle();
                bundle.putByteArray("cover", mTrackIdAudioItem.getCoverArt());
                ImageViewer imageViewer = new ImageViewer();
                imageViewer.setArguments(bundle);
                imageViewer.setSharedElementEnterTransition(new DetailsTransition());
                imageViewer.setEnterTransition(new Fade());
                imageViewer.setSharedElementReturnTransition(new DetailsTransition());


                ViewCompat.setTransitionName(mToolbarCover, "transition_"+ mCurrentAudioItem.getId());

                getSupportFragmentManager()
                        .beginTransaction()
                        .addSharedElement(mToolbarCover, "transitionFragment")
                        .add(R.id.containerFragment,imageViewer)
                        .addToBackStack(null)
                        .commit();
            }
        });


        //pressing back from mToolbar, close activity
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        if(mCorrectionMode == Constants.CorrectionModes.SEMI_AUTOMATIC){
            performTrackId();
        }


    }


    private void performTrackId(){
        closeFABMenu();


        //Check if exist trackidItem cached from previous request
        //this with the objective of saving data
        if(mTrackIdAudioItem != null) {
            setDownloadedValues();
            return;
        }


        //This functionality requires some conditions to work, check them before

        //we put this in AsyncTask because the isConnected method
        //makes a network operation which blocks UI if we execute
        //from UI Thread
        new AsyncTask<Void,Void, Integer>(){
            @Override
            protected Integer doInBackground(Void... voids){
                int canContinue = allowExecute();
                return canContinue;
            }
            @Override
            protected void onPostExecute(Integer canContinue){
                if(canContinue != 0) {
                    setSnackBarMessage(canContinue);
                    return;
                }

                if(mCorrectionMode != Constants.CorrectionModes.SEMI_AUTOMATIC)
                    showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.downloading_tags),ACTION_NONE, null);

                mProgressBar.setVisibility(View.VISIBLE);

                enableMiniFabs(false);

                mToolbarCover.setEnabled(false);
                Intent intent = new Intent(TrackDetailsActivity.this, FixerTrackService.class);
                intent.putExtra(Constants.Activities.FROM_EDIT_MODE, Constants.Activities.DETAILS_ACTIVITY);
                intent.putExtra(Constants.MEDIASTORE_ID, mCurrentItemId);
                startService(intent);
            }
        }.execute();
    }

    private Drawable getStatusDrawable(){
        int status = mCurrentAudioItem.getStatus();
        Drawable drawable = null;
        switch (status){
            case AudioItem.STATUS_ALL_TAGS_FOUND:
            case AudioItem.STATUS_TAGS_EDITED_BY_USER:
                drawable = getResources().getDrawable(R.drawable.ic_done_all_white_24px,null);
                break;
            case AudioItem.STATUS_ALL_TAGS_NOT_FOUND:
                drawable = getResources().getDrawable(R.drawable.ic_done_white_24px,null);
                break;
            case AudioItem.STATUS_NO_TAGS_FOUND:
                drawable = getResources().getDrawable(R.drawable.ic_error_outline_white_24px,null);
                break;
            case AudioItem.FILE_ERROR_READ:
                drawable = getResources().getDrawable(R.drawable.ic_highlight_off_white_24px,null);
                break;
            default:
                drawable = null;
                break;
        }

        return drawable;
    }


    private String getStatusText(){
        int status = mCurrentAudioItem.getStatus();
        String msg = "";
        switch (status){
            case AudioItem.STATUS_ALL_TAGS_FOUND:
                msg = getResources().getString(R.string.file_status_ok);
                break;
            case AudioItem.STATUS_ALL_TAGS_NOT_FOUND:
                msg = getResources().getString(R.string.file_status_incomplete);
                break;
            case AudioItem.STATUS_NO_TAGS_FOUND:
                msg = getResources().getString(R.string.file_status_bad);
                break;
            case AudioItem.STATUS_TAGS_EDITED_BY_USER:
                msg = getResources().getString(R.string.file_status_edit_by_user);
                break;
            case AudioItem.FILE_ERROR_READ:
                msg = getString(R.string.file_status_error_read);
                break;
            case AudioItem.STATUS_TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE:
                msg = getString(R.string.file_status_corrected_by_semiautomatic_mode);
                break;
            default:
                msg = getResources().getString(R.string.file_status_no_processed);
                break;
        }

        return msg;
    }

    private void enableMiniFabs(boolean enable){

        mDownloadCoverButton.setEnabled(enable);
        mEditButton.setEnabled(enable);
        mAutoFixButton.setEnabled(enable);
        //ask if we are going to enable or disable mini fabs,
        //if we are are going to disable, lets disable all,
        //else, enable it or disable it depending on if exist cover art
        mExtractCoverButton.setEnabled(enable);
    }

    private void showFABMenu(){
        mIsFloatingActionMenuOpen = true;

        mFloatingActionMenu.animate().rotation(-400);

        mAutoFixButton.animate().translationY(-getResources().getDimension(R.dimen.standard_55));

        mEditButton.animate().translationY(-getResources().getDimension(R.dimen.standard_105));

        mDownloadCoverButton.animate().translationY(-getResources().getDimension(R.dimen.standard_155));

        mExtractCoverButton.animate().translationY(-getResources().getDimension(R.dimen.standard_205));

    }

    private void closeFABMenu(){
        mIsFloatingActionMenuOpen = false;

        mFloatingActionMenu.animate().rotation(0);

        mAutoFixButton.animate().translationY(0);

        mEditButton.animate().translationY(0);

        mDownloadCoverButton.animate().translationY(0);

        mExtractCoverButton.animate().translationY(0);


    }


    /**
     * This method help us to play a preview of current song,
     * using the current instance of SimpleMediaPlayer
     * @throws IOException
     * @throws InterruptedException
     */
    private void playPreview() throws IOException, InterruptedException {
        mPlayer.playPreview(mCurrentPosition);
        if(mPlayer.isPlaying()){
            mPlayPreviewButton.setIcon(R.drawable.ic_stop_white_24px);
        }
        else {
            mPlayPreviewButton.setIcon(R.drawable.ic_play_arrow_white_24px);
        }
    }

    /**
     * This method enable the fields for being edited
     * by user, or disable in case the user cancel
     * the operation or when finish the edition
     */
    private void editInfoTrack(){
        if(!mEditMode) {
            enableFieldsToEdit();
         }
        else {
            disableFields();
        }
    }

    /**
     * This method saves the initial values
     * when the activity is created,
     * in case the user cancel the edition of metadata
     * these data is used indicating that
     * the user did not modify anything
     */
    private void cachingCurrentValues(){

        mCurrentCoverArt =  mTag.getFirstArtwork() == null ? null : mTag.getFirstArtwork().getBinaryData();
        mCurrentCoverArtLength = mCurrentCoverArt == null ? 0 : mCurrentCoverArt.length;
        //check if new cover art has value assigned
        //from previous trackid operation or updated cover
        if(mNewCoverArt == null) {
            mNewCoverArt = mCurrentCoverArt;
            mNewCoverArtLength = mCurrentCoverArtLength;
        }

        mCurrentTitle = mTitleField.getText().toString().trim();
        mCurrentArtist = mArtistField.getText().toString().trim();
        mCurrentAlbum = mAlbumField.getText().toString().trim();
        mCurrentNumber = mNumberField.getText().toString().trim();
        mCurrentYear = mYearField.getText().toString().trim();
        mCurrentGenre = mGenreField.getText().toString().trim();
    }


    /**
     * We validate the data entered by the user, there 3 important validations:
     * if field is empty, if data entered is too long and if data has
     * strange characters, in this case, they will be replace by empty character.
     * @return boolean mIsDataValid
     */
    private boolean isDataValid(){
        //Get all descendants of this swipeRefreshLayout;
        ArrayList<View> fields = mViewDetailsTrack.getFocusables(View.FOCUS_DOWN);
        boolean isDataValid = false;
        int numElements = fields.size();

        for (int i = 0 ; i < numElements ; i++){
            if(fields.get(i) instanceof EditText){
                ((EditText) fields.get(i)).setError(null);
                if(StringUtilities.isFieldEmpty(((EditText) fields.get(i)).getText().toString())){
                    showWarning((EditText) fields.get(i), HAS_EMPTY_FIELDS);
                    isDataValid = false;
                    break;
                }
                if(StringUtilities.isTooLong(fields.get(i).getId(), ((EditText) fields.get(i)).getText().toString() )){
                    showWarning((EditText) fields.get(i), DATA_IS_TOO_LONG);
                    isDataValid = false;
                    break;
                }

                switch (fields.get(i).getId()){
                    case R.id.track_name_details:
                        mNewTitle = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                    case R.id.artist_name_details:
                        mNewArtist = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                    case R.id.album_name_details:
                        mNewAlbum = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                    case R.id.track_genre:
                        mNewGenre = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                    case R.id.track_number:
                        mNewNumber = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                    case R.id.track_year:
                        mNewYear = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                }

                //at this point the data has been evaluated as true
                isDataValid = true;


                //If value of this option from app settings is true, replace automatically strange characters
                if(Settings.SETTING_REPLACE_STRANGE_CHARS_MANUAL_MODE){
                    switch (fields.get(i).getId()){
                        case R.id.track_name_details:
                            mNewTitle = StringUtilities.sanitizeString(mNewTitle);
                            break;
                        case R.id.artist_name_details:
                            mNewArtist = StringUtilities.sanitizeString(mNewArtist);
                            break;
                        case R.id.album_name_details:
                            mNewAlbum = StringUtilities.sanitizeString(mNewAlbum);
                            break;
                        case R.id.track_genre:
                            mNewGenre = StringUtilities.sanitizeString(mNewGenre);
                            break;
                    }
                    isDataValid = true;
                }
                //else, then show error label property from edit text field that has the error in data entered
                else if(StringUtilities.hasNotAllowedCharacters( ((EditText) fields.get(i)).getText().toString() ) ){
                    showWarning((EditText) fields.get(i), HAS_NOT_ALLOWED_CHARACTERS);
                    isDataValid = false;
                    break;
                }

            }
        }
        return isDataValid;
    }

    /**
     * Show a toast indicating that was not valid the data entered by user,
     * blinks the field where error is located and put the cursor in that field
     * @param editText the field where the error is located
     * @param cause the cause of the error.
     */

    private void showWarning(EditText editText, int cause) {
        String msg = "";
        switch (cause) {
            case HAS_EMPTY_FIELDS:
                msg = getString(R.string.empty_message);
                break;
            case DATA_IS_TOO_LONG:
                msg = getString(R.string.data_too_long_message);
                break;
            case HAS_NOT_ALLOWED_CHARACTERS:
                msg = getString(R.string.not_allowed_characters_message);
                break;
            case FILE_IS_PROCESSING:
                msg = getString(R.string.file_is_processing);
                break;
        }

        if (editText != null){
            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
            editText.requestFocus();
            editText.setError(msg);
            editText.setAnimation(animation);
            editText.startAnimation(animation);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
        else {

            showSnackBar(Snackbar.LENGTH_SHORT,msg,ACTION_NONE, null);
        }

    }


    /**
     * This method updates metadata song.
     * @throws IOException
     */
    private void updateData() throws IOException {
        //we update the data creating another thread because the database operation can take a long time
        AsyncUpdateData asyncUpdateData = new AsyncUpdateData(UPDATE_ALL_METADATA, false);
        asyncUpdateData.execute();
    }

    /**
     * If user is in edit mode and cancel without modify the information,
     * then set the previous values, including the album cover
     */
    private void setPreviousValues(){
        mTitleField.setText(mCurrentTitle);
        mArtistField.setText(mCurrentArtist);
        mAlbumField.setText(mCurrentAlbum);
        mNumberField.setText(mCurrentNumber);
        mYearField.setText(mCurrentYear);
        mGenreField.setText(mCurrentGenre);

        if(mCurrentCoverArt != null && (mCurrentCoverArtLength != mNewCoverArtLength) ) {
                GlideApp.with(mViewDetailsTrack).
                        load(mCurrentCoverArt)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .apply(RequestOptions.skipMemoryCacheOf(true))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .fitCenter()
                        .into(mToolbarCover);
                mCurrentCoverArtLength = mCurrentCoverArt.length;
            return;
        }

        if(mCurrentCoverArt == null){
                GlideApp.with(mViewDetailsTrack).
                        load(R.drawable.ic_album_white_48px)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .apply(RequestOptions.skipMemoryCacheOf(true))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .fitCenter()
                        .placeholder(R.drawable.ic_album_white_48px)
                        .into(mToolbarCover);
                mCurrentCoverArtLength = 0;
        }
    }


    private void stopPlayback() throws IOException, InterruptedException {
        mPlayPreviewButton.setIcon(R.drawable.ic_play_arrow_white_24px);
        if(mPlayer != null && mPlayer.isPlaying() && this.mCurrentPosition == mPlayer.getCurrentPos()){
            mPlayer.onCompletePlayback();
        }
    }

    /**
     * Enter to edit mode, for manually
     * modifying the information about the song
     */
    private void enableFieldsToEdit(){
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

                        //first validate inputs
                        boolean validInputs = isDataValid();

                        if (validInputs){

                            AlertDialog.Builder builder = new AlertDialog.Builder(TrackDetailsActivity.this);
                            builder.setTitle(R.string.apply_tags);
                            builder.setMessage(R.string.message_apply_new_tags);
                            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    disableFields();
                                    mAppBarLayout.setExpanded(true);
                                }
                            });
                            builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        updateData();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            AlertDialog alertDialog = builder.create();
                            alertDialog.show();
                        }
                    }
                });



                mTitleField.setEnabled(true);
                mArtistField.setEnabled(true);
                mAlbumField.setEnabled(true);
                mNumberField.setEnabled(true);
                mYearField.setEnabled(true);
                mGenreField.setEnabled(true);

                mImageSize.setText(getString(R.string.edit_cover));
                mImageSize.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_add_to_photos_white_24px),null,null,null);
                mImageSize.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editCover(INTENT_OPEN_GALLERY);
                    }
                });



                mToolbarCover.setEnabled(false);
                mTitleField.requestFocus();
                InputMethodManager imm =(InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mTitleField,InputMethodManager.SHOW_IMPLICIT);
                mEditMode = true;
            }
        });
    }

    private void removeErrorTags(){
        ArrayList<View> fields = mViewDetailsTrack.getFocusables(View.FOCUS_DOWN);
        int numElements = fields.size();

        for (int i = 0 ; i < numElements ; i++) {
            if (fields.get(i) instanceof EditText) {
                ((EditText) fields.get(i)).setError(null);
            }
        }
    }

    /**
     * This method disable the fields and
     * leaves from edit mode
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

        mSaveButton.hide();
        mSaveButton.setOnClickListener(null);

        mFloatingActionMenu.show();
        mImageSize.setText(AudioItem.getStringImageSize(mCurrentCoverArt));
        mImageSize.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_photo_white_24px),null,null,null);
        mImageSize.setOnClickListener(null);
        mToolbarCover.setEnabled(true);
        mEditMode = false;

   }



    private void editCover(int codeIntent){
        Intent selectorImageIntent = new Intent(Intent.ACTION_PICK);
        selectorImageIntent.setType("image/*");
        startActivityForResult(selectorImageIntent,codeIntent);
    }

    /**
     * We implement this method for handling
     * correctly in case the song playback be completed
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("onCompletePlayback","OnFragmnet");
        mPlayPreviewButton.setIcon(R.drawable.ic_play_arrow_white_24px);
        mPlayer.onCompletePlayback();


    }

    private class AsyncUpdateData extends AsyncTask<Void, Void, Void> {
        private final String TAG = AsyncUpdateData.class.getName();
        private int mOperationType;
        private boolean mOverwriteAllTags = false;
        private String causeError = "";


        AsyncUpdateData(int operationType, boolean overwriteAllTags){
            this.mOperationType = operationType;
            this.mOverwriteAllTags = overwriteAllTags;
        }
        @Override
        protected void onPreExecute(){
            mProgressBar.setVisibility(View.VISIBLE);
            try {
                stopPlayback();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            enableMiniFabs(false);
            mToolbarCover.setEnabled(false);
        }

        private void updateCoverArt() {
            //Is priority update the metadata first, in case there are errors when
            //the data is set on item_list and database
            ContentValues contentValues = new ContentValues();
            try {
                if(mOperationType == ADD_COVER){
                    boolean isSameCoverArt = mCurrentCoverArt == mNewCoverArt;
                    //Here we update the data in case there have had changes
                    if (!isSameCoverArt && mNewCoverArt != null && mNewCoverArtLength > 0) {
                        Artwork artwork = new AndroidArtwork();
                        artwork.setBinaryData(mNewCoverArt);
                        mTag.deleteArtworkField();
                        mTag.setField(artwork);
                        //Then, is necessary update the data in database,
                        //because we obtain this info when the app starts (after first time),
                        //besides, database operations can take a long time
                        contentValues.put(TrackContract.TrackData.STATUS, AudioItem.STATUS_TAGS_EDITED_BY_USER);

                        //Update mStatus item_list from list
                        mCurrentAudioItem.setStatus(AudioItem.STATUS_TAGS_EDITED_BY_USER);

                        Log.d("only_cover_art", "updated");
                    }
                }
                else if(mOperationType == REMOVE_COVER){
                    mTag.deleteArtworkField();
                }

                mAudioTaggerFile.commit();
                //update state of item in our track after save changes to audio file
                //to ensure that will not have inconsistence between info in our DB
                //and the audio file itself
                mDbHelper.updateData(TrackDetailsActivity.this.mCurrentItemId, contentValues);
                mDataUpdated = true;
            }
            catch (CannotWriteException | TagException e){
                e.printStackTrace();
                mDataUpdated = false;
                causeError = e.getMessage();
            }

        }

        private void updateTrackedIdTags(){
            //Is priority update the metadata first, in case there are errors when
            //the data is set on item_list and database
            try {

                mAudioFile = new File(mTrackPath);
                String currentFileName = mAudioFile.getName();
                String currentFileNameWithoutExt = currentFileName.substring(0,currentFileName.length()-4);

                ContentValues contentValues = new ContentValues();

                boolean isTitleSameThanFilename = currentFileNameWithoutExt.equals(mNewTitle);

                //overwrite all tags
                if(mOverwriteAllTags) {
                    //we verify these because we don't need write every time
                    //the tags when the current is equal than new
                    boolean isSameTitle = mCurrentTitle.equals(mNewTitle);
                    boolean isSameArtist = mCurrentArtist.equals(mNewArtist);
                    boolean isSameAlbum = mCurrentAlbum.equals(mNewAlbum);
                    boolean isSameGenre = mCurrentGenre.equals(mNewGenre);
                    boolean isSameTrackNumber = mCurrentNumber.equals(mNewNumber);
                    boolean isSameTrackYear = mCurrentYear.equals(mNewYear);
                    boolean isSameCoverArt = mCurrentCoverArtLength == mNewCoverArtLength;
                    //this flag is used in case the user did not do any changes
                    //and pressed Save button, so reallyu we don't write any mTag
                    boolean hasChanges = !(isSameTitle && isSameArtist && isSameAlbum && isSameGenre && isSameTrackNumber && isSameTrackYear && isSameCoverArt);

                    //Verify that current value is not the same than new, besides
                    //also it verifies if new value is not empty (because from trackId can come empty values)
                    //so, if empty then will not write the mTag.
                    if (!isSameTitle && !mNewTitle.isEmpty()) {
                        mTag.setField(FieldKey.TITLE, mNewTitle);
                        contentValues.put(TrackContract.TrackData.TITLE, mNewTitle);
                        mCurrentAudioItem.setTitle(mNewTitle);
                        Log.d("title", "updated");
                    }

                    if (!isSameArtist && !mNewArtist.isEmpty()) {
                        mTag.setField(FieldKey.ARTIST, mNewArtist);
                        contentValues.put(TrackContract.TrackData.ARTIST, mNewArtist);
                        mCurrentAudioItem.setArtist(mNewArtist);
                        Log.d("artist", "updated");
                    }
                    if (!isSameAlbum && !mNewAlbum.isEmpty()) {
                        mTag.setField(FieldKey.ALBUM, mNewAlbum);
                        contentValues.put(TrackContract.TrackData.ALBUM, mNewAlbum);
                        mCurrentAudioItem.setAlbum(mNewAlbum);
                        Log.d("album", "updated");
                    }
                    if (!isSameTrackNumber && !mNewNumber.isEmpty()) {
                        mTag.setField(FieldKey.TRACK, mNewNumber);
                        Log.d("number", "updated");
                    }
                    if (!isSameTrackYear && !mNewYear.isEmpty()) {
                        mTag.setField(FieldKey.YEAR, mNewYear);
                        Log.d("year", "updated");
                    }

                    if (!isSameGenre && !mNewGenre.isEmpty()) {
                        mTag.setField(FieldKey.GENRE, mNewGenre);
                        Log.d("genre", "updated");
                    }

                    if (!isSameCoverArt && mNewCoverArt != null) {
                        Artwork artwork = new AndroidArtwork();
                        artwork.setBinaryData(mNewCoverArt);
                        //in accordance to library, is necessary
                        //first delete the artwork before
                        //write the new one
                        mTag.deleteArtworkField();
                        mTag.setField(artwork);
                        Log.d("coverart", "updated");
                    }

                    //Here we update the data in case there have had changes
                    //if not, no case to write any mTag
                    if (hasChanges) {
                        mAudioTaggerFile.commit();
                        Log.d("overwrite", "updated_all");
                    }
                }
                //write only missing tags
                else {

                    //Flag to indicate that is necessary commit the changes
                    //to audiofile in case any tag is new
                    boolean hasToUpdate = false;

                    //verify if this current mTag value is empty (we are only writing empty tags), also
                    //verify if new value is not empty, because from trackId can come empty values
                    if (mCurrentTitle.isEmpty() && !mNewTitle.isEmpty()) {
                        mTag.setField(FieldKey.TITLE, mNewTitle);
                        contentValues.put(TrackContract.TrackData.TITLE, mNewTitle);
                        mCurrentAudioItem.setTitle(mNewTitle);
                        hasToUpdate = true;
                        Log.d("title", "updated");
                    }

                    if (mCurrentArtist.isEmpty() && !mNewArtist.isEmpty()) {
                        mTag.setField(FieldKey.ARTIST, mNewArtist);
                        contentValues.put(TrackContract.TrackData.ARTIST, mNewArtist);
                        mCurrentAudioItem.setArtist(mNewArtist);
                        hasToUpdate = true;
                        Log.d("artist", "updated");
                    }

                    if (mCurrentAlbum.isEmpty() && !mNewAlbum.isEmpty()) {
                        mTag.setField(FieldKey.ALBUM, mNewAlbum);
                        contentValues.put(TrackContract.TrackData.ALBUM, mNewAlbum);
                        mCurrentAudioItem.setAlbum(mNewAlbum);
                        hasToUpdate = true;
                        Log.d("album", "updated");
                    }

                    if (mCurrentNumber.isEmpty() && !mNewNumber.isEmpty()) {
                        mTag.setField(FieldKey.TRACK, mNewNumber);
                        hasToUpdate = true;
                        Log.d("number", "updated");
                    }
                    if (mCurrentYear.isEmpty() && !mNewYear.isEmpty()) {
                        mTag.setField(FieldKey.YEAR, mNewYear);
                        hasToUpdate = true;
                        Log.d("year", "updated");
                    }

                    if (mCurrentGenre.isEmpty() && !mNewGenre.isEmpty()) {
                        mTag.setField(FieldKey.GENRE, mNewGenre);
                        hasToUpdate = true;
                        Log.d("genre", "updated");
                    }

                    if (mCurrentCoverArt == null && mNewCoverArt != null) {
                        Artwork artwork = new AndroidArtwork();
                        artwork.setBinaryData(mNewCoverArt);
                        mTag.setField(artwork);
                        hasToUpdate = true;
                        Log.d("coverart", "updated");
                    }

                    //Here we update the data in case there have had changes
                    //if not, no case to write any mTag
                    if (hasToUpdate) {
                        if((mMimeType.equals("audio/mpeg_mp3") || mMimeType.equals("audio/mpeg")) && mExtension.equals("mp3") && ((MP3File)mAudioTaggerFile).hasID3v1Tag()){
                            //remove old version of ID3 tags
                            Log.d("removed ID3v1","remove ID3v1");
                            ((MP3File) mAudioTaggerFile).delete( ((MP3File)mAudioTaggerFile).getID3v1Tag() );
                        }
                        mAudioTaggerFile.commit();

                        Log.d("missing", "updated_missing");
                    }
                }
                //We check if this option is selected in settings,
                //and rename file if necessary
                if(Settings.SETTING_RENAME_FILE_SEMI_AUTOMATIC_MODE){

                    //new title is not the same than old title? then rename file
                    if(!isTitleSameThanFilename) {
                        String newAbsolutePath = AudioItem.renameFile(mCurrentAudioItem.getAbsolutePath(), mNewTitle, mNewArtist);
                        mCurrentAudioItem.setAbsolutePath(newAbsolutePath);
                        contentValues.put(TrackContract.TrackData.DATA, newAbsolutePath);

                        //lets inform to system that one file has change
                        ContentValues values = new ContentValues();
                        String selection = MediaStore.MediaColumns.DATA + "= ?";
                        String selectionArgs[] = {mTrackPath}; //old path
                        values.put(MediaStore.MediaColumns.DATA, newAbsolutePath); //DATA is the absolute path to file
                        boolean successMediaStore = getContentResolver().
                                                    update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                                            values,
                                                            selection,
                                                            selectionArgs) == 1;

                        mCurrentAudioItem.setAbsolutePath(newAbsolutePath);
                        mTrackPath = newAbsolutePath;
                        //if file was renamed, we need a new reference to renamed file
                        //in case the user needs to make additional corrections
                        //re read file and tags written
                        mAudioTaggerFile = AudioFileIO.read(new File(mTrackPath));
                        //get an empty tag or its current values
                        if(mIsMp3){
                            mAudioHeader = ((MP3File)mAudioTaggerFile).getMP3AudioHeader();
                            mTag = ((MP3File) mAudioTaggerFile).getID3v2Tag();
                        }
                        else {
                            //get info from header before trying to read tags
                            mAudioHeader = mAudioTaggerFile.getAudioHeader();
                            mTag = mAudioTaggerFile.getTag() == null ? mAudioTaggerFile.createDefaultTag() : mAudioTaggerFile.getTag();
                        }

                        Log.d("media store success", successMediaStore+" path: " + mTrackPath);
                    }

                }

                //Then, is necessary update the data in database,
                //because we obtain this info when the app starts (after first time),
                contentValues.put(TrackContract.TrackData.STATUS,AudioItem.STATUS_TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE);

                //Update the data of item_list from list
                mCurrentAudioItem.setStatus(AudioItem.STATUS_TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE);

                mDbHelper.updateData(mCurrentItemId,contentValues);
                //all has gone fine? then update this flag value
                mDataUpdated = true;
            }
            catch ( CannotWriteException | TagException | ReadOnlyFileException | CannotReadException | IOException | InvalidAudioFrameException e)  {
                e.printStackTrace();
                mDataUpdated = false;
                causeError = e.getMessage();
                //restore previous values to item
                mCurrentAudioItem.setTitle(mCurrentTitle);
                mCurrentAudioItem.setArtist(mCurrentArtist);
                mCurrentAudioItem.setAlbum(mCurrentAlbum);
            }
        }

        private void updateEditedTags(){
            //Is priority update the metadata first
            try {
                mAudioFile = new File(mTrackPath);
                String currentFileName = mAudioFile.getName();
                String currentFileNameWithoutExt = currentFileName.substring(0,currentFileName.length()-4);

                ContentValues contentValues = new ContentValues();

                //we verify these because we don't need write every time
                //the tags when the current is equal than new
                boolean isTitleSameThanFilename = currentFileNameWithoutExt.equals(mNewTitle);
                boolean isSameTitle = mCurrentTitle.equals(mNewTitle);
                boolean isSameArtist = mCurrentArtist.equals(mNewArtist);
                boolean isSameAlbum = mCurrentAlbum.equals(mNewAlbum);
                boolean isSameGenre = mCurrentGenre.equals(mNewGenre);
                boolean isSameTrackNumber = mCurrentNumber.equals(mNewNumber);
                boolean isSameTrackYear = mCurrentYear.equals(mNewYear);
                boolean isSameCoverArt = mCurrentCoverArtLength == mNewCoverArtLength;
                //this flag is used in case the user did not do any changes
                //and pressed Save button, so reallyu we don't write any mTag
                boolean hasChanges = !(isSameTitle && isSameArtist && isSameAlbum && isSameGenre && isSameTrackNumber && isSameTrackYear && isSameCoverArt);

                    //Verify that current value is not the same than new.
                    //Also verify if new value is not empty, because from trackId can come empty values
                    if (!isSameTitle && !mNewTitle.isEmpty()) {
                        mTag.setField(FieldKey.TITLE, mNewTitle);
                        contentValues.put(TrackContract.TrackData.TITLE, mNewTitle);
                        mCurrentAudioItem.setTitle(mNewTitle);
                        Log.d("title", "updated");
                    }
                    if (!isSameArtist && !mNewArtist.isEmpty()) {
                        mTag.setField(FieldKey.ARTIST, mNewArtist);
                        contentValues.put(TrackContract.TrackData.ARTIST, mNewArtist);
                        mCurrentAudioItem.setArtist(mNewArtist);
                        Log.d("artist", "updated");
                    }
                    if (!isSameAlbum && !mNewAlbum.isEmpty()) {
                        mTag.setField(FieldKey.ALBUM, mNewAlbum);
                        contentValues.put(TrackContract.TrackData.ALBUM, mNewAlbum);
                        mCurrentAudioItem.setAlbum(mNewAlbum);
                        Log.d("album", "updated");
                    }
                    if (!isSameTrackNumber && !mNewNumber.isEmpty()) {
                        mTag.setField(FieldKey.TRACK, mNewNumber);
                        Log.d("number", "updated");
                    }
                    if (!isSameTrackYear && !mNewYear.isEmpty()) {
                        mTag.setField(FieldKey.YEAR, mNewYear);
                        Log.d("year", "updated");
                    }

                    if (!isSameGenre && !mNewGenre.isEmpty()) {
                        mTag.setField(FieldKey.GENRE, mNewGenre);
                        Log.d("genre", "updated " + mNewGenre);
                    }

                    if (!isSameCoverArt && mNewCoverArt != null && mNewCoverArt.length > 0) {
                        Artwork artwork = new AndroidArtwork();
                        artwork.setBinaryData(mNewCoverArt);
                        mTag.deleteArtworkField();
                        mTag.setField(artwork);
                        Log.d("coverart", "updated");
                    }

                    //Here we update the data in case there have had changes
                    //if not, no case to write any Tag
                    if (hasChanges) {
                        if((mMimeType.equals("audio/mpeg_mp3") || mMimeType.equals("audio/mpeg")) && mExtension.equals("mp3") && ((MP3File)mAudioTaggerFile).hasID3v1Tag()){
                            //remove old version of ID3 tags
                            Log.d("removed ID3v1","remove ID3v1");
                            ((MP3File) mAudioTaggerFile).delete( ((MP3File)mAudioTaggerFile).getID3v1Tag() );
                        }


                        mAudioTaggerFile.commit();
                        Log.d("all_edited_metadata", "updated");
                    }


                //We check if this option is selected in settings
                //and update filename if is necessary
                if(Settings.SETTING_RENAME_FILE_MANUAL_MODE){
                    //new title is not the same than old title? then rename file

                    if(!isTitleSameThanFilename) {
                        String newAbsolutePath = AudioItem.renameFile(mCurrentAudioItem.getAbsolutePath(), mNewTitle, mNewArtist);
                        mCurrentAudioItem.setAbsolutePath(newAbsolutePath);
                        contentValues.put(TrackContract.TrackData.DATA, newAbsolutePath);

                        //lets inform to system that one file has change
                        ContentValues values = new ContentValues();
                        String selection = MediaStore.MediaColumns.DATA + "= ?";
                        String selectionArgs[] = {mTrackPath}; //old path
                        values.put(MediaStore.MediaColumns.DATA, newAbsolutePath); //new path
                        boolean successMediaStore = getContentResolver().
                                update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        values,
                                        selection,
                                        selectionArgs) == 1;

                        mCurrentAudioItem.setAbsolutePath(newAbsolutePath);
                        //if file was renamed, we need a new reference to renamed file
                        //in case the user needs to make additional corrections
                        mTrackPath = newAbsolutePath;
                        //re read file and tags written
                        mAudioTaggerFile = AudioFileIO.read(new File(mTrackPath));
                        //get an empty tag or its current values
                        if(mIsMp3){
                            mAudioHeader = ((MP3File)mAudioTaggerFile).getMP3AudioHeader();
                            mTag = ((MP3File) mAudioTaggerFile).getID3v2Tag();
                        }
                        else {
                            //get info from header before trying to read tags
                            mAudioHeader = mAudioTaggerFile.getAudioHeader();
                            mTag = mAudioTaggerFile.getTag() == null ? mAudioTaggerFile.createDefaultTag() : mAudioTaggerFile.getTag();
                        }

                        values.clear();
                        values = null;
                        Log.d("media store success", successMediaStore+" path: " + mTrackPath);
                    }

                }

                //Then, is necessary update the data in database,
                //because we obtain this info when the app starts (after first time)
                contentValues.put(TrackContract.TrackData.STATUS,AudioItem.STATUS_TAGS_EDITED_BY_USER);
                //Update the data of item_list from list
                mCurrentAudioItem.setStatus(AudioItem.STATUS_TAGS_EDITED_BY_USER);
                mDbHelper.updateData(mCurrentItemId,contentValues);
                // all has gone fine? then change this flag value
                mDataUpdated = true;


            }
            catch ( CannotWriteException | TagException | ReadOnlyFileException | CannotReadException | IOException | InvalidAudioFrameException e) {
                e.printStackTrace();
                mDataUpdated = false;
                causeError = e.getMessage();
                //restore previous values to item
                mCurrentAudioItem.setTitle(mCurrentTitle);
                mCurrentAudioItem.setArtist(mCurrentArtist);
                mCurrentAudioItem.setAlbum(mCurrentAlbum);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (mOperationType == ADD_COVER || mOperationType == REMOVE_COVER) {
                updateCoverArt();
            }
            else if (mOperationType == UPDATE_TRACKED_ID_TAGS){
                updateTrackedIdTags();
            }
            else{
                updateEditedTags();
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void result){
            mProgressBar.setVisibility(View.INVISIBLE);
            if(mOperationType != REMOVE_COVER){
                hideLayoutTrackIdResults();
            }
            else {
                redrawCover();
            }
            if(mTrackIdCard.getVisibility() != View.VISIBLE) {
                disableFields();
                enableMiniFabs(true);
            }
        }

        private void hideLayoutTrackIdResults(){

            mAudioFile = new File(mTrackPath);

            mTrackIdCard.animate().setDuration(DURATION).alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mContent.setEnabled(true);
                    mTrackIdCard.setVisibility(GONE);
                    mContent.setVisibility(View.VISIBLE);
                    mContent.animate().setDuration(DURATION).alpha(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            String msg = "";
                            final boolean isSameCoverArt = mCurrentCoverArtLength == mNewCoverArtLength;
                            //If data was update successfully
                            if(mDataUpdated){

                                //update image of mToolbar cover only if operation ADD_COVER is true and new cover is not null and is not the same cover than previous
                                if(mOperationType == ADD_COVER && !isSameCoverArt && mNewCoverArt != null) {
                                    msg = getString(R.string.cover_updated);
                                    GlideApp.with(mViewDetailsTrack).
                                            load(mNewCoverArt)
                                            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .apply(RequestOptions.skipMemoryCacheOf(true))
                                            .fitCenter()
                                            .into(mToolbarCover);
                                    //update the fields of mStatus, filesize (the bigger is, the more space in SD takes) and
                                    //image size
                                    mStatus.setText(getStatusText());
                                    mStatus.setCompoundDrawablesWithIntrinsicBounds(getStatusDrawable(),null,null,null);
                                    mFileSize.setText(AudioItem.getFileSize(mAudioFile.length()));
                                    mImageSize.setText(AudioItem.getStringImageSize(mNewCoverArt));
                                }
                                else {
                                    if(mOperationType == UPDATE_ALL_METADATA || mOverwriteAllTags) {
                                        msg = getString(R.string.message_data_update);
                                    }
                                    else{
                                        msg = getString(R.string.update_missing);
                                    }
                                    //Update fields in case we have replaced the strange chars
                                    if(!isSameCoverArt && mNewCoverArt != null) {
                                        GlideApp.with(mViewDetailsTrack).
                                                load(mNewCoverArt)
                                                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                                .transition(DrawableTransitionOptions.withCrossFade())
                                                .apply(RequestOptions.skipMemoryCacheOf(true))
                                                .fitCenter()
                                                .into(mToolbarCover);
                                        mFileSize.setText(AudioItem.getFileSize(mAudioFile.length()));
                                        mImageSize.setText(AudioItem.getStringImageSize(mNewCoverArt));
                                    }

                                    mTitleLayer.setText(mAudioFile.getName());
                                    mTitleField.setText(mTag.getFirst(FieldKey.TITLE));
                                    mArtistField.setText(mTag.getFirst(FieldKey.ARTIST));
                                    mAlbumField.setText(mTag.getFirst(FieldKey.ALBUM));
                                    mGenreField.setText(mTag.getFirst(FieldKey.GENRE));
                                    mNumberField.setText(mTag.getFirst(FieldKey.TRACK));
                                    mYearField.setText(mTag.getFirst(FieldKey.YEAR));
                                    mStatus.setText(getStatusText());
                                    mStatus.setCompoundDrawablesWithIntrinsicBounds(getStatusDrawable(),null,null,null);
                                }
                                //notify to adapter that one item has changed
                                mTrackAdapter.notifyItemChanged(mCurrentAudioItem.getPosition());

                            }
                            //If data was not updated
                            else {
                                msg = getString(R.string.message_no_data_updated) + ": " + causeError + ".";
                                setPreviousValues();
                            }


                            showSnackBar(7000, msg,ACTION_NONE, null);
                            mFloatingActionMenu.show();
                            mSaveButton.hide();
                            mToolbarCover.setEnabled(true);
                            mAppBarLayout.setExpanded(true);

                            cachingCurrentValues();
                            mOnlyCoverArt = false;
                            mDataUpdated = false;
                            mEditMode = false;

                        }
                    });

                }
            });



        }

        private void redrawCover() {
            //update the fields of mStatus, filesize (the smaller is, the less space in SD takes) and
            //image size
            mAudioFile = new File(mTrackPath);
            mFileSize.setText(AudioItem.getFileSize(mAudioFile.length()));
            mImageSize.setText(AudioItem.getStringImageSize(null));
            //set the generic cover art
            GlideApp.with(mViewDetailsTrack).
                    load(R.drawable.ic_album_white_48px)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .apply(RequestOptions.skipMemoryCacheOf(true))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .fitCenter()
                    .placeholder(R.drawable.ic_album_white_48px)
                    .into(mToolbarCover);
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.cover_removed),ACTION_NONE, null);
            mTrackAdapter.notifyItemChanged(mCurrentAudioItem.getPosition());
            cachingCurrentValues();
        }

    }





    @Override
    protected void onPause(){
        super.onPause();
        if(!ServiceHelper.withContext(getApplicationContext()).withService(FixerTrackService.CLASS_NAME).isServiceRunning())
            mLocalBroadcastManager.unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceivers();
    }

    private void registerReceivers(){
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionDoneDetails);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionCompleteTask);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionApiInitialized);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionNotFound);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionConnectionLost );
    }

    private void setDownloadedValues(){
        mProgressBar.setVisibility(View.INVISIBLE);
        mToolbarCover.setEnabled(true);
        mActionBar.hide();
        if(mIsFloatingActionMenuOpen)
            closeFABMenu();

        //get new values from trackIdItem
        mNewTitle = mTrackIdAudioItem.getTitle();
        mNewArtist = mTrackIdAudioItem.getArtist();
        mNewAlbum = mTrackIdAudioItem.getAlbum();
        mNewGenre = mTrackIdAudioItem.getGenre();
        mNewNumber = mTrackIdAudioItem.getTrackNumber();
        mNewYear = mTrackIdAudioItem.getTrackYear();

        mNewCoverArt = mTrackIdAudioItem.getCoverArt();
        mNewCoverArtLength = mNewCoverArt == null ? 0 : mNewCoverArt.length;



        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TrackDetailsActivity.this);
                builder.setTitle(R.string.apply_tags);
                builder.setMessage(R.string.message_apply_found_tags);
                builder.setNegativeButton("Todas", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AsyncUpdateData asyncUpdateData = new AsyncUpdateData(UPDATE_TRACKED_ID_TAGS, true);
                        asyncUpdateData.execute();
                    }
                });
                builder.setPositiveButton("Faltantes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AsyncUpdateData asyncUpdateData = new AsyncUpdateData(UPDATE_TRACKED_ID_TAGS, false);
                        asyncUpdateData.execute();
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });



        mContent.animate().setDuration(DURATION).alpha(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mContent.setVisibility(GONE);
                mAppBarLayout.setExpanded(false);
                mContent.setEnabled(false);
                mTrackIdCard.animate().setDuration(DURATION).alpha(1).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mTrackIdCard.setVisibility(View.VISIBLE);

                        //make visible the trackidcover if available
                        mTrackIdCover.setVisibility(View.VISIBLE);
                        mTrackIdCoverArtDimensions.setVisibility(View.VISIBLE);

                        if(mNewCoverArt != null) {
                            GlideApp.with(mViewDetailsTrack).
                                    load(mNewCoverArt)
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .fitCenter()
                                    .into(mTrackIdCover);

                            mTrackIdCoverArtDimensions.setText(AudioItem.getStringImageSize(mNewCoverArt));

                        }
                        else{
                            GlideApp.with(mViewDetailsTrack).
                                    load(getDrawable(R.drawable.ic_album_white_48px))
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .placeholder(R.drawable.ic_album_white_48px)
                                    .fitCenter()
                                    .into(mTrackIdCover);
                        }



                        //make visible only fields when trackid value is available
                        mTrackIdTitle.setText(mNewTitle);
                        mTrackIdTitle.setVisibility(mNewTitle.equals("")?GONE:View.VISIBLE);

                        mTrackIdArtist.setText(mNewArtist);
                        mTrackIdArtist.setVisibility(mNewArtist.equals("")?GONE:View.VISIBLE);

                        mTrackIdAlbum.setText(mNewAlbum);
                        mTrackIdAlbum.setVisibility(mNewAlbum.equals("")?GONE:View.VISIBLE);

                        mTrackIdGenre.setText(mNewGenre);
                        mTrackIdGenre.setVisibility(mNewGenre.equals("")?GONE:View.VISIBLE);

                        mTrackIdNumber.setText(mNewNumber);
                        mTrackIdNumber.setVisibility(mNewNumber.equals("")?GONE:View.VISIBLE);

                        mTrackIdYear.setText(mNewYear);
                        mTrackIdYear.setVisibility(mNewYear.equals("")?GONE:View.VISIBLE);

                        //inform to user the meta tags found
                        showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.info_found),ACTION_NONE, null);

                        //let the user apply this changes
                        mFloatingActionMenu.hide();
                        //mFloatingActionMenu.setVisibility(GONE);

                        mSaveButton.show();
                        //mSaveButton.setVisibility(View.VISIBLE);
                        mActionBar.show();
                        mEditMode = true;
                    }
                });

            }
        });

    }


    private void handleCoverArt() {
        mToolbarCover.setEnabled(true);
        if(mIsFloatingActionMenuOpen)
            closeFABMenu();
        String msg = "";

        mNewCoverArt = mTrackIdAudioItem.getCoverArt();
        mNewCoverArtLength = mNewCoverArt == null ? 0 : mNewCoverArt.length;


        if(mNewCoverArt != null){
            msg = getString(R.string.cover_art_found);
            mAppBarLayout.setExpanded(false);


            mSaveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(TrackDetailsActivity.this);
                    builder.setTitle(getString(R.string.title_downloaded_cover_art_dialog));
                    builder.setPositiveButton(getString(R.string.as_cover_art), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AsyncUpdateData asyncUpdateData = new AsyncUpdateData(ADD_COVER, false);
                            asyncUpdateData.execute();
                        }
                    });
                    builder.setNegativeButton(getString(R.string.as_file), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //if was successful saved, then
                            String newImageAbsolutePath = FileSaver.saveFile(mNewCoverArt, mTrackIdAudioItem.getTitle(), mTrackIdAudioItem.getArtist(), mTrackIdAudioItem.getAlbum());
                            if(!newImageAbsolutePath.equals(FileSaver.NULL_DATA) && !newImageAbsolutePath.equals(FileSaver.NO_EXTERNAL_STORAGE_WRITABLE) && !newImageAbsolutePath.equals(FileSaver.INPUT_OUTPUT_ERROR)) {

                                showSnackBar(7000, getString(R.string.cover_saved) + " " + AudioItem.getRelativePath(newImageAbsolutePath) + ".", ACTION_VIEW_COVER, newImageAbsolutePath);
                                //lets inform to system that one file has been created
                                MediaScannerConnection.scanFile(
                                                                getApplicationContext(),
                                                                new String[]{newImageAbsolutePath},
                                                                new String[]{MimeTypeMap.getFileExtensionFromUrl(newImageAbsolutePath)},
                                                                null);
                            }
                            else {
                                showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.cover_not_saved), ACTION_NONE, null);
                            }

                            dialog.cancel();
                            hideTrackedIdResultsLayout();
                        }
                    });
                    builder.setMessage(R.string.description_downloaded_cover_art_dialog);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setCancelable(true);
                    alertDialog.show();
                }
            });

            final String finalMsg = msg;
            mContent.animate().setDuration(DURATION).alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mContent.setVisibility(View.GONE);
                    mContent.setEnabled(false);
                    mTrackIdCard.animate().setDuration(DURATION).alpha(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {

                            mTrackIdCard.setVisibility(View.VISIBLE);

                            GlideApp.with(mViewDetailsTrack).
                                    load(mNewCoverArt)
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .fitCenter()
                                    .into(mTrackIdCover);

                            mTrackIdCoverArtDimensions.setText(AudioItem.getStringImageSize(mNewCoverArt));

                            showSnackBar(Snackbar.LENGTH_SHORT, finalMsg,ACTION_NONE, null);

                            mFloatingActionMenu.hide();
                            mSaveButton.show();
                            mEditMode = true;
                        }
                    });
                }
            });


        }
        else{
            msg = getString(R.string.no_cover_art_found);
            mOnlyCoverArt = false;
            mNewCoverArt = mCurrentCoverArt;
            mNewCoverArtLength = mCurrentCoverArtLength;
            showSnackBar(Snackbar.LENGTH_LONG, msg,ACTION_ADD_COVER, null);
            enableMiniFabs(true);

        }

        mProgressBar.setVisibility(View.INVISIBLE);

    }

    private int allowExecute(){

        //No internet connection
        if(!DetectorInternetConnection.isConnected(getApplicationContext())){
            return Constants.Conditions.NO_INTERNET_CONNECTION;
        }

        //API not initialized
        if(!apiInitialized){
            Job.scheduleJob(getApplicationContext());
            return Constants.Conditions.NO_INITIALIZED_API;
        }

        return 0;
    }

    public class ResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();
            String action;
            action = intent.getAction();


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    enableMiniFabs(true);
                    mProgressBar.setVisibility(View.INVISIBLE);
                }
            });

            Log.d("action_received",action);
            switch (action){
                case Constants.Actions.ACTION_DONE_DETAILS:
                    //TrackId found tags
                    mTrackIdAudioItem = intent.getParcelableExtra(Constants.AUDIO_ITEM);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mOnlyCoverArt) {
                                handleCoverArt();
                            }
                            else {
                                setDownloadedValues();
                            }
                        }
                    });

                    break;
                case Constants.GnServiceActions.ACTION_API_INITIALIZED:
                    showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.api_initialized),ACTION_NONE,null);
                    break;
                case Constants.Actions.ACTION_NOT_FOUND:
                    //No results from trackId
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.file_status_bad),ACTION_NONE, null);
                        }
                    });

                    break;
                case Constants.Actions.ACTION_CONNECTION_LOST:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.connection_lost),ACTION_NONE, null);
                        }
                    });
                    break;
                default:
                    //Any other response, maybe an error
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.error),ACTION_NONE, null);
                        }
                    });
                    break;
            }




        }
    }

}
