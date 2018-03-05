package mx.dev.franco.automusictagfixer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
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
import android.support.v4.app.ActivityOptionsCompat;
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
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.crashlytics.android.Crashlytics;

import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mx.dev.franco.automusictagfixer.database.DataTrackDbHelper;
import mx.dev.franco.automusictagfixer.database.TrackContract;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.list.TrackAdapter;
import mx.dev.franco.automusictagfixer.services.ConnectivityDetector;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.services.Job;
import mx.dev.franco.automusictagfixer.services.ServiceHelper;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.FileSaver;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.Settings;
import mx.dev.franco.automusictagfixer.utilities.SimpleMediaPlayer;
import mx.dev.franco.automusictagfixer.utilities.StringUtilities;
import mx.dev.franco.automusictagfixer.utilities.TaggerHelper;

import static android.view.View.GONE;
import static mx.dev.franco.automusictagfixer.services.GnService.sApiInitialized;

/**
 * Created by franco on 22/07/17.
 */

public class TrackDetailsActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    private static final String TAG = TrackDetailsActivity.class.getName();

    //Intent type for pick an image
    private static final int INTENT_OPEN_GALLERY = 1;
    private static final int INTENT_GET_AND_UPDATE_FROM_GALLERY = 2;
    //codes for determining the type of error when validating the fields
    private static final int HAS_EMPTY_FIELDS = 11;
    private static final int DATA_IS_TOO_LONG = 12;
    private static final int HAS_NOT_ALLOWED_CHARACTERS = 13;
    //Indicates to user that actual audio files is processing
    //Indicates to user that actual audio files is processing
    //in case the user tries to make another task while this happen
    private static final int FILE_IS_PROCESSING = 14;
    //Indicates that there is no internet connection to perform a download cover mode
    private static final int NO_INTERNET_CONNECTION_COVER_ART = 28;
    //Actions to show in snackbar
    private static final int ACTION_NONE = 30;
    private static final int ACTION_ADD_COVER = 31 ;
    private static final int ACTION_VIEW_COVER = 32;
    private static final int ACTION_SD_WRITING_ENABLE_PERMISSION = 35;
    private static final int ACTION_START_TRACKID = 33 ;

    //Duration of animations
    private static final int DURATION = 150;

    //flag to indicate that is just required to download
    //the coverart
    private boolean mOnlyCoverArt = false;
    //Id from audio item list
    private long mCurrentItemId;
    //flag when user is editing info
    private boolean mEditMode = false;
    //A reference to database connection
    private DataTrackDbHelper mDbHelper;
    //Deafult action when activity is opened
    private int mCorrectionMode = Constants.CorrectionModes.VIEW_INFO;
    //rootview
    private View mViewDetailsTrack;
    //Fabs to create a fab menu
    private FloatingActionButton mEditButton;
    private FloatingActionButton mDownloadCoverButton;
    private FloatingActionButton mAutoFixButton;
    private MenuItem mExtractCoverButton;
    private FloatingActionButton mSaveButton;
    private FloatingActionButton mFloatingActionMenu;

    //Current data song
    private String mNewTitle;
    private String mNewArtist;
    private String mNewAlbum;
    private String mNewNumber;
    private String mNewYear;
    private String mNewGenre;
    private String mTrackPath;
    private String mCurrentDuration;
    private String mCurrentTitle;
    private String mCurrentArtist;
    private String mCurrentAlbum;
    private String mCurrentNumber;
    private String mCurrentYear;
    private String mCurrentGenre;
    //References to elements that show
    //current data song
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
    private ImageView mToolbarCover;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private AppBarLayout mAppBarLayout;
    private ActionBar mActionBar;
    private NestedScrollView mContent;
    private Snackbar mSnackbar;
    //File object to read some data
    private File mCurrentFile;

    //temporal references to new and current cover art
    private byte[] mCurrentCoverArt;
    private byte[] mNewCoverArt;
    private int mCurrentCoverArtLength;
    private int mNewCoverArtLength = 0;

    //Reference to custom media mPlayer.
    private SimpleMediaPlayer mPlayer;
    //reference to current audio item_list being edited
    private AudioItem mCurrentAudioItem = null;
    //audio item list to cache data response of making a trackId inside this activity
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

    //Flag to indicate if mini fabs are shown or hidden
    private boolean mIsFloatingActionMenuOpen = false;

    //references to visual elements of card view
    //that shows tags found when is made trackId task
    private CardView mTrackIdCard;
    private ImageView mTrackIdCover;
    private TextView mTrackIdTitle;
    private TextView mTrackIdArtist;
    private TextView mTrackIdAlbum;
    private TextView mTrackIdGenre;
    private TextView mTrackIdNumber;
    private TextView mTrackIdYear;
    private TextView mTrackIdCoverArtDimensions;

    //reference to adapter
    private TrackAdapter mTrackAdapter;
    //additional data from audio file
    private String mFrequencyVal;
    private int mResolutionVal;
    private String mChannelsVal = "0";
    private String mMimeType = "";
    private String mExtension = "";
    private String mFileType = "audio";

    private int mCurrentPosition;
    //flag to indicate if any error occurred during tag reading
    private boolean mError = false;
    //Buttons of toolbar menu
    private MenuItem removeItem;
    private MenuItem searchInWebItem;

    private AsyncUpdateData mAsyncUpdateData;

    private static final int CROSS_FADE_DURATION = 200;
    private AsyncSaveFile mAsyncSaveFile;
    private TaggerHelper mTaggerHelper;

    /**
     * Callback when is created the activity
     * @param savedInstanceState
     */

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
        mReceiver = new ResponseReceiver(this);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        //main layout or root view
        mViewDetailsTrack = findViewById(R.id.rootContainerDetails);
        //We get the current instance of SimpleMediaPlayer object
        mPlayer = SimpleMediaPlayer.getInstance(getApplicationContext());
        mTrackAdapter = (TrackAdapter) mPlayer.getAdapter();

        //if this intent comes when is touched any element from dialog in list of MainActivity
        mCorrectionMode = getIntent().getIntExtra(Constants.CorrectionModes.MODE,Constants.CorrectionModes.VIEW_INFO);
        //currentId of audioItem
        mCurrentItemId = getIntent().getLongExtra(Constants.MEDIA_STORE_ID,-1);

        mCurrentPosition = getIntent().getIntExtra(Constants.POSITION,0);
        mCurrentAudioItem = mTrackAdapter.getAudioItemByIdOrPath(mCurrentItemId);

        //current path to file
        mTrackPath = mCurrentAudioItem.getAbsolutePath();

        Log.d("Trackpath",mTrackPath);

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
        mSnackbar = Snackbar.make(mViewDetailsTrack,"",Snackbar.LENGTH_SHORT);
        TextView tv = (TextView) this.mSnackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        mSnackbar.getView().setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.primaryLightColor));
        tv.setTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));
        mSnackbar.setActionTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));
    }

    /**
     * Shows the snackbar with information
     * about current operation
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
        else if(action == ACTION_START_TRACKID){
            mSnackbar.setText(msg);
            mSnackbar.setDuration(duration);
            mSnackbar.setAction(R.string.start, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    performTrackId();
                }
            });

        }
        else if(action == ACTION_SD_WRITING_ENABLE_PERMISSION){
            mSnackbar.setText(msg);
            mSnackbar.setDuration(duration);
            mSnackbar.setAction(R.string.enable, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Request permission to access SD card
                    //through Storage Access Framework
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    startActivityForResult(intent, RequiredPermissions.REQUEST_PERMISSION_SAF);
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
            //For android >7 we need a file provider to open
            //files in external app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "mx.dev.franco.automusictagfixer.fileProvider", file);
                intent.setDataAndType(contentUri, type);
            } else {
                intent.setDataAndType(Uri.fromFile(file), type);
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }
    }

    /**
     *Sets the string message depending on code passed as parameter
     * @param reason numeric code of string message
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

        mExtractCoverButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                closeFABMenu();
                //current song has no cover
                if(mCurrentCoverArt == null){
                    showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.can_not_extract_null_cover),ACTION_NONE,null);
                    return false;
                }

                //Save extracted image file
                mAsyncSaveFile = new AsyncSaveFile(TrackDetailsActivity.this, mCurrentCoverArt, mCurrentTitle, mCurrentArtist, mCurrentAlbum);
                mAsyncSaveFile.execute();
                return false;
            }
        });
        mPlayPreviewButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //it this option is enabled in Settings, current
                //will play with integrated media player
                if(Settings.SETTING_USE_EMBED_PLAYER) {
                    try {
                        playPreview();
                    } catch (IOException | InterruptedException e) {
                        Crashlytics.logException(e);
                        e.printStackTrace();
                    }
                }
                //if not, then select an external application
                //for playing it
                else {
                    openInExternalApp(mTrackPath);
                }
                return false;
            }
        });

        //references to toolbar menu buttons

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
                        mAsyncUpdateData = new AsyncUpdateData(TaggerHelper.MODE_REMOVE_COVER, TrackDetailsActivity.this);
                        mAsyncUpdateData.execute();
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return false;
            }
        });

        searchInWebItem = menu.findItem(R.id.action_web_search);
        //performs a web search in navigator
        //using the title and artist name
        searchInWebItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(mError)
                    return false;

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

    /**
     * This callback handles
     * the back button pressed from
     * Android system
     */
    @Override
    public void onBackPressed() {
        //hides fabs if are open
        if(mIsFloatingActionMenuOpen){
            closeFABMenu();
            return;
        }

        //Exits from edit mode
        if (mEditMode) {
            hideTrackedIdResultsLayout();
            disableFields();
            setPreviousValues();
            return;
        }

        //destroys activity
        dismiss();
        super.onBackPressed();
    }

    /**
     * Hides tracked id card view with smooth
     * animation
     */
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

    /**
     * Reset values and hide elements of tracked id card view
     */
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
        Log.d(TAG, "request code " + requestCode + " - resultCode" + resultCode + " resultData is null " + (data == null));

        switch (requestCode){
            case INTENT_GET_AND_UPDATE_FROM_GALLERY:
            case INTENT_OPEN_GALLERY:
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
                                    .error(R.drawable.ic_album_white_48px)
                                    .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .fitCenter()
                                    .placeholder(R.drawable.ic_album_white_48px)
                                    .into(mToolbarCover);
                            if (requestCode == INTENT_OPEN_GALLERY) {
                                //show the new cover in mToolbar cover
                                if (!mEditMode) {
                                    enableFieldsToEdit();
                                }
                            } else {
                                mAsyncUpdateData = new AsyncUpdateData(TaggerHelper.MODE_ADD_COVER, this);
                                mAsyncUpdateData.execute();
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
                break;

            case RequiredPermissions.REQUEST_PERMISSION_SAF:
                String msg = "";
                if (resultCode == Activity.RESULT_OK) {
                    // The document selected by the user won't be returned in the intent.
                    // Instead, a URI to that document will be contained in the return intent
                    // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
                    if (data!= null) {
                        Log.i(TAG, "Uri: " + data.getData().toString());

                        //Save root Uri of SD card
                        Constants.URI_SD_CARD = data.getData();
                        msg = getString(R.string.toast_apply_tags_again);
                        // Persist access permissions.
                /*final int takeFlags = resultData.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(Constants.URI_SD_CARD, takeFlags);*/

                    }
                }
                else {
                    msg = getString(R.string.permission_denied);
                }

                Toast toast = Toast.makeText(this.getApplicationContext(), "", Toast.LENGTH_LONG);
                toast.setText(msg);
                View view = toast.getView();
                TextView text = (TextView) view.findViewById(android.R.id.message);
                text.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.grey_900));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    text.setTextAppearance(R.style.CustomToast);
                } else {
                    text.setTextAppearance(this.getApplicationContext(), R.style.CustomToast);
                }
                view.setBackground(ContextCompat.getDrawable(this.getApplicationContext(), R.drawable.background_custom_toast));
                toast.setGravity(Gravity.CENTER, 0, 0);
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

        mTaggerHelper.releaseResources();
        mTaggerHelper = null;

        mDbHelper = null;
        mTitleField = null;
        mArtistField = null;
        mAlbumField = null;
        mNumberField = null;
        mYearField = null;
        mGenreField = null;
        mCurrentCoverArt = null;
        mTrackAdapter = null;
        mViewDetailsTrack = null;
        mEditButton = null;
        mExtractCoverButton = null;
        mDownloadCoverButton = null;
        mAutoFixButton = null;
        mSaveButton = null;
        mFloatingActionMenu = null;

        mPlayer = null;
        mProgressBar = null;
        mCurrentAudioItem.setPosition(-1);
        mCurrentAudioItem = null;
        mTrackIdAudioItem = null;
        mFilterActionDoneDetails = null;
        mFilterActionCompleteTask = null;
        mReceiver.releaseContext();
        mReceiver = null;
        mLocalBroadcastManager = null;

        mTrackIdCard = null;
        mTrackIdCover = null;
        mTrackIdTitle = null;
        mTrackIdArtist = null;
        mTrackIdAlbum = null;
        mTrackIdGenre = null;
        mTrackIdNumber = null;
        mTrackIdYear = null;
        mTrackIdCoverArtDimensions = null;

        mNewTitle = null;
        mNewArtist = null;
        mNewAlbum = null;
        mNewNumber = null;
        mNewYear = null;
        mNewGenre = null;
        mTrackPath = null;
        mCurrentDuration = null;
        mCurrentTitle = null;
        mCurrentArtist = null;
        mCurrentAlbum = null;
        mCurrentNumber = null;
        mCurrentYear = null;
        mCurrentGenre = null;

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
        mToolbar = null;
        mToolbarCover = null;
        mCollapsingToolbarLayout = null;
        mAppBarLayout = null;
        mActionBar = null;
        mContent = null;
        mSnackbar = null;
        mCurrentFile = null;

        mCurrentCoverArt = null;
        mNewCoverArt = null;

        System.gc();
        super.onDestroy();
    }

    /**
     * Stops FixerTrackService, playback and
     * receivers and finishes current activity
     */
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

        if(mAsyncUpdateData != null){
            mAsyncUpdateData.cancel(true);
            mAsyncUpdateData = null;
        }

        mPlayer.setOnCompletionListener(null);
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
        finishAfterTransition();

        System.gc();
    }

    /**
     * Initializes MediaPlayer and setup
     * of fields
     */
    private void init(){
        mPlayer.setOnCompletionListener(this);
        setupFields();
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

        //Scrollable content
        mContent = mViewDetailsTrack.findViewById(R.id.contentContainer);
        mProgressBar = mViewDetailsTrack.findViewById(R.id.progressSavingData);
        //editable edit texts from song
        mToolbarCover = mViewDetailsTrack.findViewById(R.id.toolbarCover);
        mTitleField = mViewDetailsTrack.findViewById(R.id.track_name_details);
        mArtistField = mViewDetailsTrack.findViewById(R.id.artist_name_details);
        mAlbumField = mViewDetailsTrack.findViewById(R.id.album_name_details);
        mNumberField = mViewDetailsTrack.findViewById(R.id.track_number);
        mYearField = mViewDetailsTrack.findViewById(R.id.track_year);
        mGenreField = mViewDetailsTrack.findViewById(R.id.track_genre);


        //Floating action buttons
        mDownloadCoverButton = mViewDetailsTrack.findViewById(R.id.fabDownloadCover);
        mEditButton = mViewDetailsTrack.findViewById(R.id.fabEditTrackInfo);
        mAutoFixButton = mViewDetailsTrack.findViewById(R.id.fabAutofix);
        mFloatingActionMenu = mViewDetailsTrack.findViewById(R.id.fabMenu);
        mSaveButton = mViewDetailsTrack.findViewById(R.id.fabSaveInfo);
        mSaveButton.hide();


        //Additional data fields
        mLayerFileName = mViewDetailsTrack.findViewById(R.id.titleTransparentLayer);
        mSubtitleLayer = mViewDetailsTrack.findViewById(R.id.subtitleTransparentLayer);
        mImageSize = mViewDetailsTrack.findViewById(R.id.imageSize);
        mFileSize = mViewDetailsTrack.findViewById(R.id.fileSize);
        mTrackLength = mViewDetailsTrack.findViewById(R.id.trackLength);
        mBitrateField = mViewDetailsTrack.findViewById(R.id.bitrate);
        mTrackType = mViewDetailsTrack.findViewById(R.id.track_type);
        mFrequency = mViewDetailsTrack.findViewById(R.id.frequency);
        mResolution = mViewDetailsTrack.findViewById(R.id.resolution);
        mChannels = mViewDetailsTrack.findViewById(R.id.channels);
        mStatus = mViewDetailsTrack.findViewById(R.id.status);

        //references to elements of trackid results card view
        mTrackIdCard = mViewDetailsTrack.findViewById(R.id.trackIdCard);
        mTrackIdCover = mViewDetailsTrack.findViewById(R.id.trackidCoverArt);
        mTrackIdCoverArtDimensions = mViewDetailsTrack.findViewById(R.id.trackidCoverArtDimensions);
        mTrackIdTitle = mViewDetailsTrack.findViewById(R.id.trackidTitle);
        mTrackIdArtist = mViewDetailsTrack.findViewById(R.id.trackidArtist);
        mTrackIdAlbum = mViewDetailsTrack.findViewById(R.id.trackidAlbum);
        mTrackIdGenre = mViewDetailsTrack.findViewById(R.id.trackidGenre);
        mTrackIdNumber = mViewDetailsTrack.findViewById(R.id.trackidNumber);
        mTrackIdYear = mViewDetailsTrack.findViewById(R.id.trackidYear);

        mTaggerHelper = TaggerHelper.getInstance(this);

        extractAndCacheData();
    }


    private boolean extractData(){
        mCurrentFile = new File(mTrackPath);
        //Pass this file to tagger helper to delegate tagging
        //operations
        mTaggerHelper.setSourceFile(mCurrentFile);
        try {

            mTaggerHelper.initSourceFile();
            //get extension and type of current song
            mExtension = AudioItem.getExtension(mTrackPath);
            mMimeType = AudioItem.getMimeType(mTrackPath);
            Tag tag = mTaggerHelper.getCurrentTag();
            AudioHeader audioHeader = mTaggerHelper.getAudioHeader();
            //Get header info and current tags
            mCurrentDuration = audioHeader.getTrackLength() + "";
            mBitrate = audioHeader.getBitRate();
            mFrequencyVal = audioHeader.getSampleRate();
            mResolutionVal = audioHeader.getBitsPerSample();
            mChannelsVal = audioHeader.getChannels();
            mFileType = audioHeader.getFormat();





            //get cover art and save reference to it
            mCurrentCoverArt = mTaggerHelper.getCurrentArtwork();

            mCurrentCoverArtLength = mCurrentCoverArt == null ? 0 : mCurrentCoverArt.length;
            //initial values of new cover art will be the same of current cover art
            mNewCoverArt = mCurrentCoverArt;
            mNewCoverArtLength = mCurrentCoverArtLength;

            //set global references to metadata;
            //this is useful for avoid reading tags every time
            //we need them, besides, we use them when we are about
            //to apply changes to file tags, saving only those
            //that have changed and not every tag, or in case
            //no one have changed, don't apply any, saving access
            //to file system and battery
            mCurrentTitle = tag.getFirst(FieldKey.TITLE);
            mCurrentArtist = tag.getFirst(FieldKey.ARTIST);
            mCurrentAlbum = tag.getFirst(FieldKey.ALBUM);
            mCurrentNumber = tag.getFirst(FieldKey.TRACK);
            mCurrentYear = tag.getFirst(FieldKey.YEAR);
            mCurrentGenre = tag.getFirst(FieldKey.GENRE);
            return true;
        }
        catch (ReadOnlyFileException | IOException | InvalidAudioFrameException | CannotReadException | TagException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            return false;
        }
    }

    /**
     * Extracts data from audio file, then caches
     * these values and set these values into edit text fields
     */
    private void extractAndCacheData(){
        boolean success = extractData();
        if(!success){
            mCurrentAudioItem.setStatus(AudioItem.FILE_ERROR_READ);
            showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.could_not_read_file), ACTION_NONE, null);
            mLayerFileName.setText(mCurrentFile.getName());
            mSubtitleLayer.setText(mCurrentFile.getAbsolutePath());
            mError = true;
            mFloatingActionMenu.hide();
            mDownloadCoverButton.hide();
            mEditButton.hide();
            mAutoFixButton.hide();
            mFloatingActionMenu.hide();
            mSaveButton.hide();
            mSaveButton.hide();
        }

        setCurrentValues(success);
    }

    /**
     * Sets the extracted values into corresponding
     * View objects
     */
    private void setCurrentValues(boolean successRead){

        //file name over toolbar cover
        mLayerFileName.setText(mCurrentFile.getName());
        //path to file
        mSubtitleLayer.setText(mCurrentFile.getParent());

        //set values of additional info about song
        mTrackType.setText(mFileType);
        mImageSize.setText(AudioItem.getStringImageSize(mCurrentCoverArt));
        mFileSize.setText(AudioItem.getFileSize(mCurrentFile.length()));
        mTrackLength.setText(AudioItem.getHumanReadableDuration(mCurrentDuration));
        mStatus.setText(getStatusText());
        mStatus.setCompoundDrawablesWithIntrinsicBounds(getStatusDrawable(),null,null,null);
        mBitrateField.setText(AudioItem.getBitrate(mBitrate));
        mFrequency.setText(AudioItem.getFrequency(mFrequencyVal));
        mResolution.setText(AudioItem.getResolution(mResolutionVal));
        mChannels.setText(mChannelsVal);

        //Has occurred any error while reading file or tags, then
        //do not enable to make any operations to file, only playing functionality
        if(!successRead)
            return;

        //load a placeholder in case cover art is not present
        GlideApp.with(mViewDetailsTrack).
                load(mCurrentCoverArt)
                .error(R.drawable.ic_album_white_48px)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                .fitCenter()
                .placeholder(R.drawable.ic_album_white_48px)
                .into(mToolbarCover);

        //set values into edit text objects
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
        //enable manual mode
        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                editInfoTrack();
            }
        });

        //runs track id
        mAutoFixButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnlyCoverArt = false;
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

                performTrackId();
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

                editCover(INTENT_GET_AND_UPDATE_FROM_GALLERY);
            }
        });

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                //set alpha of cover depending on offset of expanded toolbar cover height,
                mToolbarCover.setAlpha(1.0f - Math.abs(verticalOffset/(float)appBarLayout.getTotalScrollRange()));
                //when toolbar is fully collapsed show name of audio file in toolbar and back button
                if(Math.abs(verticalOffset)-appBarLayout.getTotalScrollRange() == 0) {
                    mCollapsingToolbarLayout.setTitleEnabled(true);
                    mCollapsingToolbarLayout.setTitle(mCurrentFile.getName());
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

        mTrackIdCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //no cover art was found?
                if(mTrackIdAudioItem.getCoverArt() == null)
                    return;

                Intent intent = new Intent(TrackDetailsActivity.this, FullscreenViewerActivity.class);
                intent.putExtra("cover", mTrackIdAudioItem.getCoverArt());
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                TrackDetailsActivity.this,
                mTrackIdCover,
                ViewCompat.getTransitionName(mTrackIdCover));

                startActivity(intent, options.toBundle());
            }
        });


        //pressing back from toolbar, close activity
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        //if semiautomatic mode was selected, run correction task
        //but don't apply tags until user does.
        if(mCorrectionMode == Constants.CorrectionModes.SEMI_AUTOMATIC){
            performTrackId();
        }


    }

    /**
     * Executes track id for semiautomatic mode
     */
    private void performTrackId(){
        //hide fabs
        closeFABMenu();


        //Check if exist trackidItem cached from previous request
        //this with the objective of saving data
        if(mTrackIdAudioItem != null) {
            setDownloadedValues();
            return;
        }

        //This function requires some contidions to work, check them before
        //continue
        int canContinue = allowExecute();

        if(canContinue != 0) {
            setSnackBarMessage(canContinue);
            return;
        }

        //inform to FixerTrackService correction mode
        if(!mOnlyCoverArt) {
            showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.downloading_tags), ACTION_NONE, null);
        }
        else {
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.downloading_cover),ACTION_NONE, null);
        }

        enableMiniFabs(false);
        mToolbarCover.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);

        //prepare data to identify this song and start request
        Intent intent = new Intent(this,FixerTrackService.class);
        intent.putExtra(Constants.Activities.FROM_EDIT_MODE, Constants.Activities.DETAILS_ACTIVITY);
        intent.putExtra(Constants.MEDIA_STORE_ID, mCurrentItemId);
        startService(intent);
        System.gc();
    }

    /**
     * Converts from numeric status code
     * to corresponding drawable
     * @return Drawable is the image corresponding to that status code
     */
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

    /**
     * Converts from status code from audioitem object
     * to human readable status text
     * @return msg Is the string code
     */

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
            case AudioItem.STATUS_FILE_IN_SD_WITHOUT_PERMISSION:
                msg = getString(R.string.file_status_in_sd_without_permission);
                break;
            case AudioItem.STATUS_COULD_NOT_APPLIED_CHANGES:
                msg = getString(R.string.could_not_apply_changes);
                break;
            case AudioItem.STATUS_COULD_RESTORE_FILE_TO_ITS_LOCATION:
                msg = getString(R.string.could_not_copy_to_its_original_location);
                break;
            case AudioItem.STATUS_COULD_NOT_CREATE_AUDIOFILE:
                msg = getString(R.string.could_not_create_audiofile);
                break;
            case AudioItem.STATUS_COULD_NOT_CREATE_TEMP_FILE:
                msg = getString(R.string.could_not_create_temp_file);
                break;
            default:
                msg = getResources().getString(R.string.file_status_no_processed);
                break;
        }

        return msg;
    }

    /**
     * Enables and disables fabs
     * @param enable true for enable, false to disable
     */
    private void enableMiniFabs(boolean enable){

        mDownloadCoverButton.setEnabled(enable);
        mEditButton.setEnabled(enable);
        mAutoFixButton.setEnabled(enable);
    }

    /**
     * Shows mini fabs
     */
    private void showFABMenu(){
        mIsFloatingActionMenuOpen = true;

        mFloatingActionMenu.animate().rotation(-400);

        mAutoFixButton.animate().translationY(-getResources().getDimension(R.dimen.standard_55));

        mEditButton.animate().translationY(-getResources().getDimension(R.dimen.standard_105));

        mDownloadCoverButton.animate().translationY(-getResources().getDimension(R.dimen.standard_155));

    }

    /**
     * Hides mini fabs
     */
    private void closeFABMenu(){
        mIsFloatingActionMenuOpen = false;

        mFloatingActionMenu.animate().rotation(0);

        mAutoFixButton.animate().translationY(0);

        mEditButton.animate().translationY(0);

        mDownloadCoverButton.animate().translationY(0);

    }


    /**
     * Play a preview of current song,
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
     * This method enable fields for being edited
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
     * Caches the initial values
     * when the activity is created,
     * in case the user cancel edit mode.
     */
    private void cachingCurrentValues(){
        Tag tag = mTaggerHelper.getCurrentTag();
        mCurrentCoverArt =  tag.getFirstArtwork() == null ? null : tag.getFirstArtwork().getBinaryData();
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
     * Validates data entered by the user, there 3 important validations:
     * if field is empty, if data entered is too long and if data has
     * strange characters.
     * @return boolean isDataValid Returns true if data is valid, false otherwise
     */
    private boolean isDataValid(){
        //Get all edit text descendants of main container;
        ArrayList<View> fields = mViewDetailsTrack.getFocusables(View.FOCUS_DOWN);
        boolean isDataValid = false;
        int numElements = fields.size();

        for (int i = 0 ; i < numElements ; i++){
            if(fields.get(i) instanceof EditText){
                ((EditText) fields.get(i)).setError(null);

                //Verify if any field is empty
                if(StringUtilities.isFieldEmpty(((EditText) fields.get(i)).getText().toString())){
                    showWarning((EditText) fields.get(i), HAS_EMPTY_FIELDS);
                    isDataValid = false;
                    break;
                }

                //Verify if some input data is too long
                if(StringUtilities.isTooLong(fields.get(i).getId(), ((EditText) fields.get(i)).getText().toString() )){
                    showWarning((EditText) fields.get(i), DATA_IS_TOO_LONG);
                    isDataValid = false;
                    break;
                }
                //remove blank spaces
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


                //If value of this option is enabled on Settings, sanitize data
                // by replacing automatically strange characters
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
                //else, then show edit text error label from field that has the wrong data
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
     * Shows a warning indicating that data entered by user is not valid,
     * blinks the field where error is located and put the cursor into that field
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
            assert imm != null;
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
        else {

            showSnackBar(Snackbar.LENGTH_SHORT,msg,ACTION_NONE, null);
        }

    }

    /**
     * Set the previous values(including cover art),
     * when user press back and cancel editing mode
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
                        .error(R.drawable.ic_album_white_48px)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .apply(RequestOptions.skipMemoryCacheOf(true))
                        .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                        .fitCenter()
                        .placeholder(R.drawable.ic_album_white_48px)
                        .into(mToolbarCover);
                mCurrentCoverArtLength = mCurrentCoverArt.length;
            return;
        }

        if(mCurrentCoverArt == null){
                GlideApp.with(mViewDetailsTrack).
                        load((Bitmap) null)
                        .error(R.drawable.ic_album_white_48px)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .apply(RequestOptions.skipMemoryCacheOf(true))
                        .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                        .fitCenter()
                        .placeholder(R.drawable.ic_album_white_48px)
                        .into(mToolbarCover);
                mCurrentCoverArtLength = 0;
        }
    }

    /**
     * Stops playback
     * @throws IOException
     * @throws InterruptedException
     */
    private void stopPlayback() throws IOException, InterruptedException {
        mPlayPreviewButton.setIcon(R.drawable.ic_play_arrow_white_24px);
        if(mPlayer != null && mPlayer.isPlaying() && this.mCurrentPosition == mPlayer.getCurrentPosition2()){
            mPlayer.onCompletePlayback();
        }
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
                                    //we update data in async task
                                    mAsyncUpdateData = new AsyncUpdateData(TaggerHelper.MODE_OVERWRITE_ALL_TAGS, TrackDetailsActivity.this);
                                    mAsyncUpdateData.execute(Constants.CorrectionModes.MANUAL);
                                }
                            });
                            AlertDialog alertDialog = builder.create();
                            alertDialog.show();
                        }
                    }
                });


                //Enable edit text for edit them
                mTitleField.setEnabled(true);
                mArtistField.setEnabled(true);
                mAlbumField.setEnabled(true);
                mNumberField.setEnabled(true);
                mYearField.setEnabled(true);
                mGenreField.setEnabled(true);

                mImageSize.setText(getString(R.string.edit_cover));
                mImageSize.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_add_to_photos_white_24px),null,null,null);
                //Enabled "Añadir carátula de galería" to add cover when is pressed
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

    /**
     * Remove error tags
     */
    private void removeErrorTags(){
        //get descendants instances of edit text
        ArrayList<View> fields = mViewDetailsTrack.getFocusables(View.FOCUS_DOWN);
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

        mSaveButton.hide();
        mSaveButton.setOnClickListener(null);

        mFloatingActionMenu.show();
        mImageSize.setText(AudioItem.getStringImageSize(mCurrentCoverArt));
        mImageSize.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_photo_white_24px),null,null,null);
        mImageSize.setOnClickListener(null);
        mToolbarCover.setEnabled(true);
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
     * We implement this method for handling
     * correctly when a song finishes
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("onCompletePlayback","OnFragment");
        mPlayPreviewButton.setIcon(R.drawable.ic_play_arrow_white_24px);
        mPlayer.onCompletePlayback();


    }

    /**
     * Instances of this class, handles the changes made
     * to audio file in background thread,
     * for not blocking the UI thread
     */
    private static class AsyncUpdateData extends AsyncTask<Integer, Void, Void> {

        private final String TAG = AsyncUpdateData.class.getName();
        private int mOperationType;
        private boolean mDataUpdated = false;
        private WeakReference<TrackDetailsActivity> mWeakRef;
        private int mMessageCode = -2;

        /**
         * Set these 2 params in constructor
         * @param operationType Correct all fields or only missing
         * @param trackDetailsActivity reference to activity
         */
        AsyncUpdateData(int operationType, TrackDetailsActivity trackDetailsActivity){
            this.mOperationType = operationType;
            this.mWeakRef = new WeakReference<>(trackDetailsActivity);

        }

        /**
         * Callback that executes before
         * our task, here we show for examploe
         * the progress bar and disable mini fabs
         */
        @Override
        protected void onPreExecute(){
            mWeakRef.get().mProgressBar.setVisibility(View.VISIBLE);
            try {
                mWeakRef.get().stopPlayback();
            } catch (IOException | InterruptedException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            }
            mWeakRef.get().enableMiniFabs(false);
            mWeakRef.get().mToolbarCover.setEnabled(false);
        }

        /**
         * Updates cover art by removing or replacing
         * the existent one
         */
        private void updateCoverArt() {
            if(mOperationType == TaggerHelper.MODE_ADD_COVER){
            //Here we update cover
                mDataUpdated = mWeakRef.get().mTaggerHelper.applyCover(mWeakRef.get().mNewCoverArt);
            }
            //remove cover
            else if(mOperationType == TaggerHelper.MODE_REMOVE_COVER){
                mDataUpdated = mWeakRef.get().mTaggerHelper.applyCover(null);
            }
            mMessageCode = mWeakRef.get().mTaggerHelper.getMessageCode();
        }

        /**
         * Updates tag fields with the values retrieved from
         * query Gracenote service
         * @param correctionMode
         */
        private void updateTags(int correctionMode){

            ContentValues contentValues = new ContentValues();

            HashMap<FieldKey, Object> tagsToApply = new HashMap<>();
            if(!mWeakRef.get().mNewTitle.isEmpty())
                tagsToApply.put(FieldKey.TITLE,mWeakRef.get().mNewTitle);
            if(!mWeakRef.get().mNewArtist.isEmpty())
                tagsToApply.put(FieldKey.ARTIST,mWeakRef.get().mNewArtist);
            if(!mWeakRef.get().mNewArtist.isEmpty())
                tagsToApply.put(FieldKey.ALBUM,mWeakRef.get().mNewAlbum);
            if(!mWeakRef.get().mNewGenre.isEmpty())
                tagsToApply.put(FieldKey.GENRE,mWeakRef.get().mNewGenre);
            if(!mWeakRef.get().mNewNumber.isEmpty())
                tagsToApply.put(FieldKey.TRACK,mWeakRef.get().mNewNumber);
            if(!mWeakRef.get().mNewYear.isEmpty())
                tagsToApply.put(FieldKey.YEAR,mWeakRef.get().mNewYear);
            if(mWeakRef.get().mNewCoverArt != null && mWeakRef.get().mNewCoverArt.length > 0)
                tagsToApply.put(FieldKey.COVER_ART,mWeakRef.get().mNewCoverArt);

            mWeakRef.get().mTaggerHelper.setTags(tagsToApply);
            mDataUpdated = mWeakRef.get().mTaggerHelper.applyTags(mOperationType);

            //Data updated successfully
            if (mDataUpdated) {
                if(mWeakRef.get().mTaggerHelper.getUpdatedFields().size() > 0) {
                    for (Map.Entry entry : mWeakRef.get().mTaggerHelper.getUpdatedFields().entrySet()) {
                        switch ((FieldKey) entry.getKey()) {
                            case TITLE:
                                //set value to our DB
                                contentValues.put(TrackContract.TrackData.TITLE, (String) entry.getValue());
                                //set value to item list
                                mWeakRef.get().mCurrentAudioItem.setTitle((String) entry.getValue());
                                break;
                            case ARTIST:
                                contentValues.put(TrackContract.TrackData.ARTIST, (String) entry.getValue());
                                mWeakRef.get().mCurrentAudioItem.setArtist((String) entry.getValue());
                                break;
                            case ALBUM:
                                contentValues.put(TrackContract.TrackData.ALBUM, (String) entry.getValue());
                                mWeakRef.get().mCurrentAudioItem.setAlbum((String) entry.getValue());
                                break;
                        }
                    }
                    mWeakRef.get().mTaggerHelper.getUpdatedFields().clear();
                }

                //Then, is necessary update the data in database,
                //because we obtain this info when the app starts (after first time),
                if(correctionMode == Constants.CorrectionModes.SEMI_AUTOMATIC) {
                    contentValues.put(TrackContract.TrackData.STATUS, AudioItem.STATUS_TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE);
                    //Update the data of list item
                    mWeakRef.get().mCurrentAudioItem.setStatus(AudioItem.STATUS_TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE);
                }
                else {
                    contentValues.put(TrackContract.TrackData.STATUS, AudioItem.STATUS_TAGS_EDITED_BY_USER);
                    //Update the data of list item
                    mWeakRef.get().mCurrentAudioItem.setStatus(AudioItem.STATUS_TAGS_EDITED_BY_USER);
                }

                mWeakRef.get().mCurrentFile = new File(mWeakRef.get().mTrackPath);
            }


            //If this option is enabled in Settings,
            //then rename file
            if((correctionMode == Constants.CorrectionModes.MANUAL && Settings.SETTING_RENAME_FILE_MANUAL_MODE) ||
                    ((correctionMode == Constants.CorrectionModes.SEMI_AUTOMATIC && Settings.SETTING_RENAME_FILE_SEMI_AUTOMATIC_MODE))){

                String newAbsolutePath = TaggerHelper.renameFile(mWeakRef.get().mCurrentFile,
                        mWeakRef.get().getApplicationContext(),
                        mWeakRef.get().mNewTitle,
                        mWeakRef.get().mNewArtist);

                if(newAbsolutePath != null){
                    contentValues.put(TrackContract.TrackData.DATA, newAbsolutePath);

                    Log.d(TAG, "newAbsolutePath " + newAbsolutePath);
                    //Inform to system that one file has change
                    ContentValues values = new ContentValues();
                    String selection = MediaStore.MediaColumns.DATA + "= ?";
                    String selectionArgs[] = {mWeakRef.get().mTrackPath}; //old path
                    values.put(MediaStore.MediaColumns.DATA, newAbsolutePath); //DATA is the absolute path to file
                    boolean successMediaStore = mWeakRef.get().getContentResolver().
                            update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    values,
                                    selection,
                                    selectionArgs) == 1;

                    mWeakRef.get().mCurrentAudioItem.setAbsolutePath(newAbsolutePath);
                    mWeakRef.get().mTrackPath = newAbsolutePath;
                    mWeakRef.get().mCurrentFile = new File(mWeakRef.get().mTrackPath);
                    Log.d("media store success", successMediaStore + " path: " + mWeakRef.get().mTrackPath);
                }

            }

            //UPdate data in DB
            if(contentValues.size() > 0){
                mWeakRef.get().mDbHelper.updateData(mWeakRef.get().mCurrentItemId,contentValues);
            }

            mMessageCode = mWeakRef.get().mTaggerHelper.getMessageCode();


        }

        /**
         * Executes in background Thread,
         * preventing to block UI thread.
         * @param modes Mode that request apply tags
         * @return
         */
        @Override
        protected Void doInBackground(Integer... modes) {
            switch (mOperationType){
                case TaggerHelper.MODE_ADD_COVER:
                case TaggerHelper.MODE_REMOVE_COVER:
                    updateCoverArt();
                    break;
                case TaggerHelper.MODE_WRITE_ONLY_MISSING:
                case TaggerHelper.MODE_OVERWRITE_ALL_TAGS:
                    updateTags(modes[0]);
                    break;
            }

            return null;
        }

        /**
         * Once doInBackground has finished,
         * report results to UI thread
         * @param result
         */
        @Override
        protected void onPostExecute(Void result){

            try {
                hideLayoutTrackIdResults();
            } catch (ReadOnlyFileException | CannotReadException | InvalidAudioFrameException | IOException | TagException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCancelled(Void result) {
            if (mWeakRef != null){
                mWeakRef.get().mAsyncUpdateData = null;
                mWeakRef.clear();
                mWeakRef = null;
            }
            System.gc();
        }

        /**
         * Hides with smooth animation the
         * card view that holds the tracked id results
         */
        private void hideLayoutTrackIdResults() throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {
            final Tag tag = mWeakRef.get().mTaggerHelper.getCurrentTag();
            mWeakRef.get().extractData();

            mWeakRef.get().mTrackIdCard.animate().setDuration(DURATION).alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mWeakRef.get().mContent.setEnabled(true);
                    mWeakRef.get().mTrackIdCard.setVisibility(GONE);
                    mWeakRef.get().mContent.setVisibility(View.VISIBLE);
                    mWeakRef.get().mContent.animate().setDuration(DURATION).alpha(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            String msg = "";

                            //If data was update successfully
                            if(mDataUpdated){
                                //If only cover art was updated, added or removed
                                if(mOperationType == TaggerHelper.MODE_ADD_COVER || mOperationType == TaggerHelper.MODE_REMOVE_COVER){
                                    if(mMessageCode == TaggerHelper.NEW_COVER_APPLIED || mMessageCode == TaggerHelper.APPLIED_SAME_COVER){
                                        msg = mWeakRef.get().getString(R.string.cover_updated);
                                        mWeakRef.get().mImageSize.setText(AudioItem.getStringImageSize(mWeakRef.get().mCurrentCoverArt));
                                    }
                                    else if(mMessageCode == TaggerHelper.CURRENT_COVER_REMOVED){
                                        msg = mWeakRef.get().getString(R.string.cover_removed);
                                        mWeakRef.get().mImageSize.setText(AudioItem.getStringImageSize(null));
                                    }

                                    mWeakRef.get().mFileSize.setText(AudioItem.getFileSize(mWeakRef.get().mCurrentFile.length()));


                                    GlideApp.with(mWeakRef.get().mViewDetailsTrack).
                                            load(mWeakRef.get().mCurrentCoverArt)
                                            .error(R.drawable.ic_album_white_48px)
                                            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                            .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                                            .apply(RequestOptions.skipMemoryCacheOf(true))
                                            .fitCenter()
                                            .placeholder(R.drawable.ic_album_white_48px)
                                            .into(mWeakRef.get().mToolbarCover);
                                }
                                //If all tags ro only missing were applied
                                else if(mOperationType == TaggerHelper.MODE_OVERWRITE_ALL_TAGS || mOperationType == TaggerHelper.MODE_WRITE_ONLY_MISSING) {
                                    //all or missing data was updated
                                    if(mMessageCode == TaggerHelper.APPLIED_ALL_TAGS) {
                                        msg = mWeakRef.get().getString(R.string.message_all_data_updated);
                                    }
                                    else if(mMessageCode == TaggerHelper.APPLIED_ONLY_MISSING_TAGS){
                                        msg = mWeakRef.get().getString(R.string.message_missing_data_updated);
                                    }
                                    else if(mMessageCode == TaggerHelper.APPLIED_SAME_TAGS){
                                        msg = mWeakRef.get().getString(R.string.message_same_tags_applied);
                                    }

                                    if(mWeakRef.get().mTaggerHelper.getCurrentArtwork() != null) {
                                        GlideApp.with(mWeakRef.get().mViewDetailsTrack).
                                                load(mWeakRef.get().mTaggerHelper.getCurrentArtwork())
                                                .error(R.drawable.ic_album_white_48px)
                                                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                                .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                                                .apply(RequestOptions.skipMemoryCacheOf(true))
                                                .fitCenter()
                                                .placeholder(R.drawable.ic_album_white_48px)
                                                .into(mWeakRef.get().mToolbarCover);
                                        mWeakRef.get().mFileSize.setText(AudioItem.getFileSize(mWeakRef.get().mCurrentFile.length()));
                                        mWeakRef.get().mImageSize.setText(AudioItem.getStringImageSize(mWeakRef.get().mNewCoverArt));
                                    }


                                    mWeakRef.get().mLayerFileName.setText(mWeakRef.get().mCurrentFile.getName());
                                    mWeakRef.get().mTitleField.setText(mWeakRef.get().mCurrentTitle);
                                    mWeakRef.get().mArtistField.setText(mWeakRef.get().mCurrentArtist);
                                    mWeakRef.get().mAlbumField.setText(mWeakRef.get().mCurrentAlbum);
                                    mWeakRef.get().mGenreField.setText(mWeakRef.get().mCurrentGenre);
                                    mWeakRef.get().mNumberField.setText(mWeakRef.get().mCurrentNumber);
                                    mWeakRef.get().mYearField.setText(mWeakRef.get().mCurrentYear);
                                    mWeakRef.get().mStatus.setText(mWeakRef.get().getStatusText());
                                    mWeakRef.get().mStatus.setCompoundDrawablesWithIntrinsicBounds(mWeakRef.get().getStatusDrawable(),null,null,null);
                                }
                                //notify to adapter that one item has changed
                                mWeakRef.get().mTrackAdapter.notifyItemChanged(mWeakRef.get().mCurrentAudioItem.getPosition());

                            }
                            //If data could not updated
                            else {
                                    msg = mWeakRef.get().mTaggerHelper.getMessage();
                                    mWeakRef.get().setPreviousValues();
                            }


                            int action = mMessageCode == TaggerHelper.COULD_NOT_GET_URI_SD_ROOT_TREE ? ACTION_SD_WRITING_ENABLE_PERMISSION : ACTION_NONE;

                            mWeakRef.get().showSnackBar(7000, msg,action, null);
                            mWeakRef.get().mFloatingActionMenu.show();
                            mWeakRef.get().mSaveButton.hide();
                            mWeakRef.get().mToolbarCover.setEnabled(true);
                            mWeakRef.get().mAppBarLayout.setExpanded(true);

                            mWeakRef.get().cachingCurrentValues();
                            mWeakRef.get().mOnlyCoverArt = false;
                            mDataUpdated = false;
                            mWeakRef.get().mEditMode = false;
                            mWeakRef.get().disableFields();
                            mWeakRef.get().enableMiniFabs(true);

                            mWeakRef.get().mProgressBar.setVisibility(View.GONE);

                            mWeakRef.get().mAsyncUpdateData = null;
                            mWeakRef.clear();
                            mWeakRef = null;
                            System.gc();

                        }
                    });

                }
            });



        }

    }

    /**
     * Callback when activities enters to pause state,
     * remove receivers if FixerTrackService is not processing any task
     * to save battery
     */
    @Override
    protected void onPause(){
        super.onPause();
        if(!ServiceHelper.withContext(getApplicationContext()).withService(FixerTrackService.CLASS_NAME).isServiceRunning())
            mLocalBroadcastManager.unregisterReceiver(mReceiver);
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
        registerReceivers();
    }

    /**
     * Register filters to handle intents from FixerTrackService
     */
    private void registerReceivers(){
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionDoneDetails);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionCompleteTask);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionApiInitialized);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionNotFound);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionConnectionLost );
    }

    /**
     * Shows tracked id results card view
     * to decide if apply new tags or not
     */
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


        //saves new values when user press
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TrackDetailsActivity.this);
                builder.setTitle(R.string.apply_tags);
                builder.setMessage(R.string.message_apply_found_tags);
                builder.setNegativeButton(getString(R.string.all_tags), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAsyncUpdateData = new AsyncUpdateData(TaggerHelper.MODE_OVERWRITE_ALL_TAGS, TrackDetailsActivity.this);
                        mAsyncUpdateData.execute(Constants.CorrectionModes.SEMI_AUTOMATIC);
                    }
                });
                builder.setPositiveButton(getString(R.string.missing_tags), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAsyncUpdateData = new AsyncUpdateData(TaggerHelper.MODE_WRITE_ONLY_MISSING, TrackDetailsActivity.this);
                        mAsyncUpdateData.execute(Constants.CorrectionModes.SEMI_AUTOMATIC);
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });


        //Smooth animation to show tracked id results card view
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

                        //Shows a preview of cover, if was found
                        if(mNewCoverArt != null) {
                            GlideApp.with(mViewDetailsTrack).
                                    load(mNewCoverArt)
                                    .error(R.drawable.ic_album_white_48px)
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                                    .fitCenter()
                                    .placeholder(R.drawable.ic_album_white_48px)
                                    .into(mTrackIdCover);

                            mTrackIdCoverArtDimensions.setText(AudioItem.getStringImageSize(mNewCoverArt));

                        }
                        else{
                            GlideApp.with(mViewDetailsTrack).
                                    load((Bitmap) null)
                                    .error(R.drawable.ic_album_white_48px)
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                                    .placeholder(R.drawable.ic_album_white_48px)
                                    .fitCenter()
                                    .into(mTrackIdCover);
                        }



                        //make visible only values found
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

                        mFloatingActionMenu.hide();
                        //let the user apply this changes showing a different floating action button
                        mSaveButton.show();

                        mActionBar.show();
                        mEditMode = true;
                    }
                });

            }
        });

    }

    /**
     * Handles the download cover mode,
     * if cover was found show it in
     * tracked id results card view
     */

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

            //Shows button to apply changes
            mSaveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(TrackDetailsActivity.this);
                    builder.setTitle(getString(R.string.title_downloaded_cover_art_dialog));
                    //save cover as embed cover
                    builder.setPositiveButton(getString(R.string.as_cover_art), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mAsyncUpdateData = new AsyncUpdateData(TaggerHelper.MODE_ADD_COVER, TrackDetailsActivity.this);
                            mAsyncUpdateData.execute();
                        }
                    });
                    //save cover as image file
                    builder.setNegativeButton(getString(R.string.as_file), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mAsyncSaveFile = new AsyncSaveFile(TrackDetailsActivity.this, mNewCoverArt, mTrackIdAudioItem.getTitle(), mTrackIdAudioItem.getArtist(), mTrackIdAudioItem.getAlbum());
                            mAsyncSaveFile.execute();
                        }
                    });

                    builder.setMessage(R.string.description_downloaded_cover_art_dialog);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setCancelable(true);
                    alertDialog.show();
                }
            });

            //shows tracked id results card view if cover was found
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
                                    .error(R.drawable.ic_album_white_48px)
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
                                    .fitCenter()
                                    .into(mTrackIdCover);

                            mTrackIdCoverArtDimensions.setText(AudioItem.getStringImageSize(mNewCoverArt));

                            showSnackBar(Snackbar.LENGTH_SHORT, finalMsg,ACTION_NONE, null);

                            mFloatingActionMenu.hide();
                            mSaveButton.show();
                            mEditMode = true;
                            enableMiniFabs(true);
                        }
                    });
                }
            });


        }
        //No cover found
        else{
            mOnlyCoverArt = false;
            mNewCoverArt = mCurrentCoverArt;
            mNewCoverArtLength = mCurrentCoverArtLength;
            msg = getString(R.string.no_cover_art_found);
            showSnackBar(7000, msg,ACTION_ADD_COVER, null);
        }

        mProgressBar.setVisibility(View.INVISIBLE);

    }

    /**
     * To start executing any correction task
     * is necessary check if API from Gracenote is
     * initialized and if there is internet connection,
     * this method checks this.
     * @return
     */
    private int allowExecute(){

        //No internet connection
        if(!ConnectivityDetector.sIsConnected){
            ConnectivityDetector.withContext(this).startCheckingConnection();
            return Constants.Conditions.NO_INTERNET_CONNECTION;
        }

        //API not initialized
        if(!sApiInitialized){
            Job.scheduleJob(getApplicationContext());
            return Constants.Conditions.NO_INITIALIZED_API;
        }
        //All has gone fine
        return 0;
    }

    /**
     * Instance of this Receiver handles the intents
     * received from FixerTrackService
     */
    public static class ResponseReceiver extends BroadcastReceiver {
        private WeakReference<TrackDetailsActivity> mWeakRef;

        public ResponseReceiver(TrackDetailsActivity trackDetailsActivity){
            mWeakRef = new WeakReference<>(trackDetailsActivity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action;
            action = intent.getAction();

            //enable fabs and hide progress bar
            mWeakRef.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWeakRef.get().enableMiniFabs(true);
                    mWeakRef.get().mProgressBar.setVisibility(View.INVISIBLE);
                }
            });
            Log.d("ACTION",action);
            switch (action){
                //TrackId found tags
                case Constants.Actions.ACTION_DONE_DETAILS:
                    mWeakRef.get().mTrackIdAudioItem = intent.getParcelableExtra(Constants.AUDIO_ITEM);
                    mWeakRef.get().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Only cover art was requested
                            if (mWeakRef.get().mOnlyCoverArt) {
                                mWeakRef.get().handleCoverArt();
                            }
                            //All info was requested
                            else {
                                mWeakRef.get().setDownloadedValues();
                            }
                        }
                    });

                    break;
                //API is no initialized
                case Constants.GnServiceActions.ACTION_API_INITIALIZED:
                    mWeakRef.get().showSnackBar(Snackbar.LENGTH_LONG, mWeakRef.get().getString(R.string.api_initialized2),
                                                                                                ACTION_START_TRACKID,null);
                    break;
                //No match results were found for this song
                case Constants.Actions.ACTION_NOT_FOUND:
                    mWeakRef.get().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWeakRef.get().showSnackBar(Snackbar.LENGTH_LONG, mWeakRef.get().getString(R.string.file_status_bad),ACTION_NONE, null);
                        }
                    });

                    break;
                //Any other response, maybe an error
                default:
                    mWeakRef.get().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWeakRef.get().showSnackBar(Snackbar.LENGTH_SHORT, mWeakRef.get().getString(R.string.error),ACTION_NONE, null);
                        }
                    });
                    break;
            }
        }

        public void releaseContext(){
            if(mWeakRef != null) {
                mWeakRef.clear();
                mWeakRef = null;
                System.gc();
            }
        }

    }

    public static class AsyncSaveFile extends AsyncTask<Void, Void, Boolean>{
        private byte[] mDataImage;
        private String mTitle, mArtist, mAlbum, mPathToFile;
        private WeakReference<TrackDetailsActivity> mWeakRef;
        public AsyncSaveFile(TrackDetailsActivity trackDetailsActivity, byte[] dataImage, String... data){
            mWeakRef = new WeakReference<>(trackDetailsActivity);
            mDataImage = dataImage;
            mTitle = data[0];
            mArtist = data[1];
            mAlbum = data[2];
        }

        @Override
        protected void onPreExecute(){
            mWeakRef.get().mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean success = false;
            try {
                mPathToFile = FileSaver.saveImageFile(mDataImage, mTitle, mArtist, mAlbum);
                //if was successful saved, then
                //inform to system that one file has been created
                if(mPathToFile != null && !mPathToFile.equals(FileSaver.NULL_DATA)
                        && !mPathToFile.equals(FileSaver.NO_EXTERNAL_STORAGE_WRITABLE)
                        && !mPathToFile.equals(FileSaver.INPUT_OUTPUT_ERROR)) {
                    MediaScannerConnection.scanFile(
                            mWeakRef.get().getApplicationContext(),
                            new String[]{mPathToFile},
                            new String[]{MimeTypeMap.getFileExtensionFromUrl(mPathToFile)},
                            null);
                    success = true;
                }

            } catch (IOException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                success = false;
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean res){
            mWeakRef.get().mProgressBar.setVisibility(View.INVISIBLE);
            if(res){
                mWeakRef.get().
                        showSnackBar(7000, mWeakRef.get().getString(R.string.cover_saved) + " " + AudioItem.getRelativePath(mPathToFile) + ".", ACTION_VIEW_COVER, mPathToFile);
            }
            else {
                mWeakRef.get().
                        showSnackBar(Snackbar.LENGTH_LONG, mWeakRef.get().getString(R.string.cover_not_saved), ACTION_NONE, null);
            }
            mWeakRef.get().hideTrackedIdResultsLayout();
            mWeakRef.get().mAsyncSaveFile = null;
            mWeakRef.clear();
            mWeakRef = null;
        }

        @Override
        protected void onCancelled(Boolean res){
            mWeakRef.get().mProgressBar.setVisibility(View.INVISIBLE);
            if(res){
                mWeakRef.get().
                        showSnackBar(7000, mWeakRef.get().getString(R.string.cover_saved) + " " + AudioItem.getRelativePath(mPathToFile) + ".", ACTION_VIEW_COVER, mPathToFile);
            }
            else {
                mWeakRef.get().
                        showSnackBar(Snackbar.LENGTH_LONG, mWeakRef.get().getString(R.string.cover_not_saved), ACTION_NONE, null);
            }
            mWeakRef.get().hideTrackedIdResultsLayout();
            mWeakRef.get().mAsyncSaveFile = null;
            mWeakRef.clear();
            mWeakRef = null;
        }

    }

}
