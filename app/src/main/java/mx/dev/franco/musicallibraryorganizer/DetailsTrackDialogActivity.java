package mx.dev.franco.musicallibraryorganizer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.transition.TransitionManager;
import android.support.v4.content.ContextCompat;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.ImageData;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import mx.dev.franco.musicallibraryorganizer.database.DataTrackDbHelper;
import mx.dev.franco.musicallibraryorganizer.database.TrackContract;
import mx.dev.franco.musicallibraryorganizer.list.AudioItem;
import mx.dev.franco.musicallibraryorganizer.list.TrackAdapter;
import mx.dev.franco.musicallibraryorganizer.services.DetectorInternetConnection;
import mx.dev.franco.musicallibraryorganizer.services.FixerTrackService;
import mx.dev.franco.musicallibraryorganizer.services.Job;
import mx.dev.franco.musicallibraryorganizer.transitions.DetailsTransition;
import mx.dev.franco.musicallibraryorganizer.utilities.CustomMediaPlayer;
import mx.dev.franco.musicallibraryorganizer.utilities.FileSaver;
import mx.dev.franco.musicallibraryorganizer.utilities.GlideApp;
import mx.dev.franco.musicallibraryorganizer.utilities.StringUtilities;
import wseemann.media.FFmpegMediaMetadataRetriever;

import static android.view.View.GONE;
import static mx.dev.franco.musicallibraryorganizer.services.GnService.apiInitialized;

//import com.github.clans.fab.FloatingActionButton;

/**
 * Created by franco on 22/07/17.
 */

public class DetailsTrackDialogActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    //Intent type
    private static int INTENT_OPEN_GALLERY = 1;
    //codes for determining the type of error when validating the fields
    private static final int HAS_EMPTY_FIELDS = 11;
    private static final int DATA_IS_TOO_LONG = 12;
    private static final int HAS_NOT_ALLOWED_CHARACTERS = 13;
    private static final int FILE_IS_PROCESSING = 14;
    private static final int REMOVE_COVER = 15;
    private static final int UPDATE_COVER = 16;
    private static final int UPDATE_ALL_METADATA = 17;
    private static final int NO_INTERNET_CONNECTION_COVER_ART = 28;
    private static final int ACTION_NONE = 30;
    private static final int ACTION_ADD_COVER = 31 ;
    private static final int DURATION = 200;


    //flag to indicate that is just required to download
    //the coverart
    private boolean onlyCoverArt = false;
    //Id from audio item
    private long currentItemId;
    //flag when user is editing info
    private boolean editMode = false;
    //Reference to objects that make possible to edit metadata from mp3 audio files
    private FFmpegMediaMetadataRetriever fFmpegMediaMetadataRetriever = null;
    private MediaMetadataRetriever mediaMetadataRetriever = null;
    //A reference to database connection
    private DataTrackDbHelper dbHelper;
    private boolean manualMode = false;
    //rootview
    private View viewDetailsTrack;
    //References to elements inside the layout
    private FloatingActionButton editButton;
    private FloatingActionButton downloadCoverButton;
    private FloatingActionButton autofixButton;
    private FloatingActionButton removeCoverButton;
    private FloatingActionButton saveButton;
    private FloatingActionButton floatingActionMenu;

    private String newTitle;
    private String newArtist;
    private String newAlbum;
    private String newNumber;
    private String newYear;
    private String newGenre;
    private String trackPath;
    private String currentDuration;
    private String[] extraData;
    private EditText titleField;
    private String currentTitle;
    private EditText artistField;
    private String currentArtist;
    private EditText albumField;
    private String currentAlbum;
    private EditText numberField;
    private String currentNumber;
    private EditText yearField;
    private String currentYear;
    private EditText genreField;
    private String currentGenre;
    private TextView bitrateField;
    private MenuItem playPreviewButton;
    private TextView titleLayer, subtitleLayer;
    private TextView imageSize;
    private TextView fileSize;
    private TextView trackLength;
    private TextView frequency;
    private TextView resolution;
    private TextView channels;
    private TextView trackType;
    private String bitrate;
    private TextView status;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private ImageView toolbarCover;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private AppBarLayout appBarLayout;
    private ActionBar actionBar;
    private NestedScrollView content;
    private Snackbar snackbar;
    private File audioFile;

    //temporal references to new values
    private byte[] currentCoverArt;
    private byte[] newCoverArt;
    private int currentCoverArtLength;
    private int newCoverArtLength = 0;

    //Reference to custom media player.
    private CustomMediaPlayer player;
    //flag to set if data could be updated or not and inform to user
    private boolean dataUpdated = false;
    //reference to current audio item being edited
    private AudioItem currentAudioItem = null;
    //audio item to store response data of making a trackId inside this activity
    private AudioItem trackIdAudioItem = null;

    //Broadcast manager to manage the response from FixerTrackService intent service
    private LocalBroadcastManager localBroadcastManager;
    //Filter only certain responses from FixerTrackService
    private IntentFilter intentFilter;
    //Receiver to handle responses
    private ResponseReceiver receiver;

    //Flag for saving the result of validating the fields of layout
    private boolean isDataValid = false;




    private boolean isFABOpen = false;
    private IntentFilter intentFilter2;

    //references to visual elements of container
    //that shows tags found when it makes trackId
    private CardView trackIdCard;
    private ImageView trackIdCover;
    private TextView trackIdTitle;
    private TextView trackIdArtist;
    private TextView trackIdAlbum;
    private TextView trackIdGenre;
    private TextView trackIdNumber;
    private TextView trackIdYear;
    private TextView trackidCoverArtDimensions;
    private int wasModified = 0;
    private TrackAdapter trackAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set status bar translucent, these calls to window object must be done before setContentView
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);
        window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        //set the layout to this activity
        setContentView(R.layout.details_track_activity_layout);

        //Create receiver and filters to handle responses from FixerTrackService
        intentFilter = new IntentFilter(FixerTrackService.ACTION_DONE);
        intentFilter2 = new IntentFilter(FixerTrackService.ACTION_COMPLETE_TASK);
        receiver = new ResponseReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        //main layout or root view
        viewDetailsTrack = findViewById(R.id.rootContainerDetails);
        //We get the current instance of CustomMediaPlayer object
        player = CustomMediaPlayer.getInstance(getApplicationContext());
        trackAdapter = (TrackAdapter) player.getAdapter();

        //if this intent comes from dialog when it touches any element from list of SelectFolderActivity
        manualMode = getIntent().getBooleanExtra(FixerTrackService.MANUAL_MODE,false);
        //currentId of audioItem
        currentItemId = getIntent().getLongExtra(FixerTrackService.MEDIASTORE_ID,-1);


        currentAudioItem = trackAdapter.getItemByIdOrPath(currentItemId, null); //getIntent().getParcelableExtra(FixerTrackService.AUDIO_ITEM); //SelectFolderActivity.getItemByIdOrPath(currentItemId,null);


        //current path to file
        trackPath = currentAudioItem.getAbsolutePath();

        Log.d("path inside", trackPath);
        //get an instance of connection to DB
        dbHelper = DataTrackDbHelper.getInstance(getApplicationContext());

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


        createSnackBar();

        init();
    }

    /**
     * Create a general snackbar for reuse
     */

    private void createSnackBar() {
        this.snackbar = Snackbar.make(viewDetailsTrack ,"",Snackbar.LENGTH_SHORT);
        /*final FrameLayout snackBarView = (FrameLayout) snackbar.getView();
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackBarView.getChildAt(0).getLayoutParams();
        params.setMargins(params.leftMargin,
                params.topMargin,
                params.rightMargin,
                (int) (params.bottomMargin + getResources().getDimension(R.dimen.margin_bottom_snackbar)));

        snackBarView.getChildAt(0).setLayoutParams(params);*/

        TextView tv = (TextView) this.snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.primaryLightColor));
        tv.setTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));
        snackbar.setActionTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));
    }

    /**
     * Shows the snackbar with the params received
     * @param action execute code depending on action
     * @param duration how long is displayed snackbar
     * @param msg message to display
     */
    private void showSnackBar(int duration, String msg, int action){
        if(snackbar != null){
            snackbar = null;
            createSnackBar();
        }

        if(action == ACTION_NONE){
            snackbar.setText(msg);
            snackbar.setDuration(duration);
            snackbar.setAction("",null);
        }
        else {
            snackbar.setText(msg);
            snackbar.setDuration(duration);
            snackbar.setAction(R.string.add_cover, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent selectorImageIntent = new Intent(Intent.ACTION_PICK);
                    selectorImageIntent.setType("image/*");
                    startActivityForResult(selectorImageIntent,INTENT_OPEN_GALLERY);
                }
            });
        }


        snackbar.show();
    }

    private void showSnackBar(int reason){
        String msg = "";
        switch (reason){
            case SelectFolderActivity.NO_INTERNET_CONNECTION:
                msg = getString(R.string.no_internet_connection_semi_automatic_mode);
                break;
            case NO_INTERNET_CONNECTION_COVER_ART:
                msg = getString(R.string.no_internet_connection_download_cover);
                break;
            case SelectFolderActivity.NO_INITIALIZED_API:
                msg = getString(R.string.initializing_recognition_api);
                break;
        }
        showSnackBar(Snackbar.LENGTH_SHORT, msg, ACTION_NONE);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_details_track_dialog, menu);
        playPreviewButton = menu.findItem(R.id.action_play);
        if(player != null && player.isPlaying() && player.getCurrentId() == this.currentItemId){
            playPreviewButton.setIcon(R.drawable.ic_stop_white_24px);
        }
        else {
            playPreviewButton.setIcon(R.drawable.ic_play_arrow_white_24px);
        }

        playPreviewButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    playPreview();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
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

        if(isFABOpen){
            closeFABMenu();
            return;
        }

        if (editMode) {
            disableLayout();
            return;
        }


        dismiss();

    }

    private void disableLayout(){
        //removeErrorLabels();
        setPreviousValues();
        disableFields();
        enableMiniFabs(true);

        if(trackIdCard.getVisibility() == View.VISIBLE){
            trackIdCard.animate().setDuration(DURATION).alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    trackIdCard.setVisibility(GONE);
                    content.setVisibility(View.VISIBLE);
                    content.animate().setDuration(DURATION).alpha(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            floatingActionMenu.show();
                            floatingActionMenu.setVisibility(View.VISIBLE);
                            saveButton.hide();
                            saveButton.setVisibility(GONE);
                        }
                    });

                }
            });
        }

        resetValues();
        appBarLayout.setExpanded(true);
    }


    private void resetValues(){
        //reset flags and
        editMode = false;
        onlyCoverArt = false;
        newCoverArt = currentCoverArt;
        newCoverArtLength = currentCoverArtLength;
        newTitle = "";
        newArtist = "";
        newAlbum = "";
        newGenre = "";
        newNumber = "";
        newYear = "";
        //make visible only fields when trackid value is available
        trackIdTitle.setText("");
        trackIdTitle.setVisibility(GONE);

        trackIdArtist.setText("");
        trackIdArtist.setVisibility(GONE);

        trackIdAlbum.setText("");
        trackIdAlbum.setVisibility(GONE);

        trackIdGenre.setText("");
        trackIdGenre.setVisibility(GONE);

        trackIdNumber.setText("");
        trackIdNumber.setVisibility(GONE);

        trackIdYear.setText("");
        trackIdYear.setVisibility(GONE);

    }
    private void removeErrorLabels() {
        ArrayList<View> fields = viewDetailsTrack.getFocusables(View.FOCUS_DOWN);
        int numElements = fields.size();
        for (int i = 0 ; i < numElements ; i++){
            if(fields.get(i) instanceof EditText){
                ((EditText) fields.get(i)).setError(null);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == INTENT_OPEN_GALLERY && data != null){
            Uri imageData = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageData);
                if(bitmap.getHeight() > 1080 || bitmap.getWidth() > 1080){

                    showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.image_too_big),ACTION_NONE);
                    newCoverArt = currentCoverArt;
                    currentCoverArtLength = newCoverArtLength;

                }
                else {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
                    newCoverArt = byteArrayOutputStream.toByteArray();
                    newCoverArtLength = newCoverArt.length;
                    GlideApp.with(viewDetailsTrack).
                            load(newCoverArt)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                            .apply(RequestOptions.skipMemoryCacheOf(true))
                            .fitCenter()
                            .into(toolbarCover);

                    //show the new cover in toolbar cover
                    if(!editMode){
                        enableFieldsToEdit();
                    }
                    appBarLayout.setExpanded(true);


                }
            } catch (IOException e) {
                e.printStackTrace();
                showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.error_load_image),ACTION_NONE);
                newCoverArt = currentCoverArt;
                newCoverArtLength = currentCoverArtLength;
            }
        }
    }

    @Override
    public void onDestroy(){

        //Release the resources used by this objects when cancel this activity.
        if(fFmpegMediaMetadataRetriever != null){
            fFmpegMediaMetadataRetriever.release();
            fFmpegMediaMetadataRetriever = null;
        }

        if(mediaMetadataRetriever != null){
            mediaMetadataRetriever.release();
            mediaMetadataRetriever = null;
        }
        dbHelper = null;
        titleField = null;
        artistField = null;
        albumField = null;
        numberField = null;
        yearField = null;
        genreField = null;

        currentCoverArt = null;

        viewDetailsTrack = null;
        editButton = null;
        removeCoverButton = null;
        player = null;
        progressBar = null;
        currentAudioItem = null;
        trackIdAudioItem = null;
        intentFilter = null;
        intentFilter2 = null;
        localBroadcastManager.unregisterReceiver(receiver);
        receiver = null;
        localBroadcastManager = null;
        super.onDestroy();
    }

    private void dismiss() {
        if(isServiceRunning()) {
            Intent intent = new Intent(this, FixerTrackService.class);
            intent.setAction(FixerTrackService.ACTION_CANCEL);
            startService(intent);
        }

        finishAfterTransition();
        super.onBackPressed();
        System.gc();
    }

    private void init(){
        player.setOnCompletionListener(this);
        setupFields();
    }

    /**
     * This method create the references to visual elements
     * in layout
     */
    private void setupFields(){

        //toolbar and other properties
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
        appBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
        collapsingToolbarLayout.setTitleEnabled(false);
        actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        //details track elements
        content = (NestedScrollView) viewDetailsTrack.findViewById(R.id.contentContainer);
        progressBar = (ProgressBar) viewDetailsTrack.findViewById(R.id.progressSavingData);
        titleField = (EditText)viewDetailsTrack.findViewById(R.id.track_name_details);
        artistField = (EditText)viewDetailsTrack.findViewById(R.id.artist_name_details);
        albumField = (EditText)viewDetailsTrack.findViewById(R.id.album_name_details);
        numberField = (EditText)viewDetailsTrack.findViewById(R.id.track_number);
        yearField = (EditText)viewDetailsTrack.findViewById(R.id.track_year);
        genreField = (EditText)viewDetailsTrack.findViewById(R.id.track_genre);
        trackType = (TextView)viewDetailsTrack.findViewById(R.id.track_type);
        frequency = (TextView) viewDetailsTrack.findViewById(R.id.frequency);
        resolution = (TextView) viewDetailsTrack.findViewById(R.id.resolution);
        channels = (TextView) viewDetailsTrack.findViewById(R.id.channels);
        status = (TextView) viewDetailsTrack.findViewById(R.id.status);

        //Floating action buttons
        downloadCoverButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.downloadCover);
        removeCoverButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.removeCover);
        editButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.editTrackInfo);
        autofixButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.autofix);
        floatingActionMenu = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.floatingActionMenu);
        saveButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.saveInfo);
        saveButton.hide();


        toolbarCover = (ImageView) viewDetailsTrack.findViewById(R.id.toolbarCover);
        titleLayer = (TextView) viewDetailsTrack.findViewById(R.id.titleTransparentLayer);
        subtitleLayer = (TextView) viewDetailsTrack.findViewById(R.id.subtitleTransparentLayer);
        imageSize = (TextView) viewDetailsTrack.findViewById(R.id.imageSize);
        fileSize = (TextView) viewDetailsTrack.findViewById(R.id.fileSize);
        trackLength = (TextView) viewDetailsTrack.findViewById(R.id.trackLength);
        bitrateField = (TextView) viewDetailsTrack.findViewById(R.id.bitrate);

        //references to elements of results layout when makes trackid
        trackIdCard = (CardView) viewDetailsTrack.findViewById(R.id.trackIdCard);
        trackIdCover = (ImageView) viewDetailsTrack.findViewById(R.id.trackidCoverArt);
        trackidCoverArtDimensions = (TextView) viewDetailsTrack.findViewById(R.id.trackidCoverArtDimensions);
        trackIdTitle = (TextView) viewDetailsTrack.findViewById(R.id.trackidTitle);
        trackIdArtist = (TextView) viewDetailsTrack.findViewById(R.id.trackidArtist);
        trackIdAlbum = (TextView) viewDetailsTrack.findViewById(R.id.trackidAlbum);
        trackIdGenre = (TextView) viewDetailsTrack.findViewById(R.id.trackidGenre);
        trackIdNumber = (TextView) viewDetailsTrack.findViewById(R.id.trackidNumber);
        trackIdYear = (TextView) viewDetailsTrack.findViewById(R.id.trackidYear);

        extractAndCacheData();
    }


    /**
     * Here it extracts data from audio file, then stores
     * for caching purpose in variables,
     * and sets these values into no editable text fields
     */
    private void extractAndCacheData(){
        fFmpegMediaMetadataRetriever = new FFmpegMediaMetadataRetriever();
        fFmpegMediaMetadataRetriever.setDataSource(trackPath);
        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(trackPath);

        currentCoverArt = mediaMetadataRetriever.getEmbeddedPicture();
        currentCoverArtLength = currentCoverArt == null ? 0 :currentCoverArt.length;

        //initial values of new cover art will be the same of current cover art
        newCoverArt = currentCoverArt;
        newCoverArtLength = currentCoverArtLength;

        currentTitle = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE):"";

        currentArtist = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST):"";

        currentAlbum = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM):"";

        currentNumber = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TRACK) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TRACK):"";

        currentYear = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DATE) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DATE):"";

        currentGenre = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_GENRE) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_GENRE):"";

        currentDuration = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION):"0";

        bitrate = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) != null ? mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE):"";

        audioFile = new File(trackPath);

        extraData = currentAudioItem.getExtraData();

        setCurrentValues();
    }

    private void setCurrentValues(){

        if(currentCoverArt != null) {
            GlideApp.with(viewDetailsTrack).
                    load(currentCoverArt)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(RequestOptions.skipMemoryCacheOf(true))
                    .fitCenter()
                    .into(toolbarCover);

        }
        else {
            GlideApp.with(viewDetailsTrack).
                    load(getDrawable(R.drawable.ic_album_white_48px))
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .apply(RequestOptions.skipMemoryCacheOf(true))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .fitCenter()
                    .placeholder(R.drawable.ic_album_white_48px)
                    .into(toolbarCover);

        }

        titleField.setText(currentTitle);
        artistField.setText(currentArtist);
        albumField.setText(currentAlbum);
        numberField.setText(currentNumber);
        yearField.setText(currentYear);
        genreField.setText(currentGenre);
        trackType.setText(R.string.mp3_type);

        titleLayer.setText(audioFile.getName());
        subtitleLayer.setText(AudioItem.getRelativePath(audioFile.getParent()));
        imageSize.setText(AudioItem.getStringImageSize(currentCoverArt));
        fileSize.setText(AudioItem.getFileSize(audioFile.length()));
        trackLength.setText(AudioItem.getHumanReadableDuration(currentDuration));
        status.setText(getStatusText());
        status.setCompoundDrawablesWithIntrinsicBounds(getStatusDrawable(currentAudioItem.getStatus()),null,null,null);
        bitrateField.setText(AudioItem.getBitrate(bitrate));

        if(extraData.length > 0) {
            frequency.setText(extraData[0]);
            resolution.setText(extraData[1]);
            channels.setText(extraData[2]);
        }

        if(manualMode){
            enableFieldsToEdit();
        }

        addActionListeners();
    }


    private void addActionListeners(){
        if(currentCoverArt == null){
            removeCoverButton.setEnabled(false);
        }

        removeCoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                final AlertDialog.Builder builder = new AlertDialog.Builder(DetailsTrackDialogActivity.this);
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
                        AsyncUpdateData asyncUpdateData = new AsyncUpdateData(REMOVE_COVER);
                        asyncUpdateData.execute();
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                editInfoTrack();
            }
        });

        autofixButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();

                //This function requires some contidions to work, check them before
                int canContinue = SelectFolderActivity.allowExecute(DetailsTrackDialogActivity.this);
                if(canContinue != 0) {
                    showSnackBar(SelectFolderActivity.NO_INTERNET_CONNECTION);
                    return;
                }

                showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.downloading_tags),ACTION_NONE);

                progressBar.setVisibility(View.VISIBLE);

                enableMiniFabs(false);

                //Check if exist trackidItem cached from previous request
                //this with the objective of saving data
                if(trackIdAudioItem != null) {
                    setDownloadedValues();
                }
                else {
                    Intent intent = new Intent(DetailsTrackDialogActivity.this, FixerTrackService.class);
                    intent.putExtra(FixerTrackService.SINGLE_TRACK, true);
                    intent.putExtra(FixerTrackService.FROM_EDIT_MODE, true);
                    intent.putExtra(FixerTrackService.AUDIO_ITEM, currentAudioItem);
                    startService(intent);
                }
            }
        });

        downloadCoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                //This function requires some contidions to work, check them before
                int canContinue = allowExecute();

                if(canContinue != 0) {
                    showSnackBar(NO_INTERNET_CONNECTION_COVER_ART);
                    return;
                }

                showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.downloading_cover),ACTION_NONE);

                progressBar.setVisibility(View.VISIBLE);
                enableMiniFabs(false);


                //Check if exist trackidItem cached from previous request
                //this with the objective of saving data

                onlyCoverArt = true;

                if(trackIdAudioItem != null){
                    handleCoverArt();
                }
                else{

                    Intent intent = new Intent(DetailsTrackDialogActivity.this,FixerTrackService.class);
                    intent.putExtra(FixerTrackService.FROM_EDIT_MODE,true);
                    intent.putExtra(FixerTrackService.SINGLE_TRACK,true);
                    intent.putExtra(FixerTrackService.AUDIO_ITEM, currentAudioItem);
                    startService(intent);
                }


            }
        });

        floatingActionMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isFABOpen){
                    showFABMenu();
                }else{
                    closeFABMenu();
                }
            }
        });

        toolbarCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editCover();
            }
        });

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                toolbarCover.setAlpha(1.0f - Math.abs(verticalOffset/(float)appBarLayout.getTotalScrollRange()));
                if(Math.abs(verticalOffset)-appBarLayout.getTotalScrollRange() == 0) {
                    collapsingToolbarLayout.setTitleEnabled(true);
                    collapsingToolbarLayout.setTitle(audioFile.getName());
                    actionBar.setDisplayShowTitleEnabled(true);
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    actionBar.setDisplayShowHomeEnabled(true);
                }
                else {
                    collapsingToolbarLayout.setTitleEnabled(false);
                    actionBar.setDisplayShowTitleEnabled(false);
                    actionBar.setDisplayHomeAsUpEnabled(false);
                    actionBar.setDisplayShowHomeEnabled(false);
                }
            }
        });

        trackIdCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransitionManager.beginDelayedTransition((ViewGroup) viewDetailsTrack);


                Bundle bundle = new Bundle();
                bundle.putByteArray("cover",trackIdAudioItem.getCoverArt());
                ImageViewer imageViewer = new ImageViewer();
                imageViewer.setArguments(bundle);
                imageViewer.setSharedElementEnterTransition(new DetailsTransition());
                imageViewer.setEnterTransition(new Fade());
                imageViewer.setSharedElementReturnTransition(new DetailsTransition());


                ViewCompat.setTransitionName(toolbarCover, "transition_"+currentAudioItem.getId());

                getSupportFragmentManager()
                        .beginTransaction()
                        .addSharedElement(toolbarCover, "transitionFragment")
                        .add(R.id.containerFragment,imageViewer)
                        .addToBackStack(null)
                        .commit();
            }
        });


        //pressing back from toolbar, close activity
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

    }

    private Drawable getStatusDrawable(int s){
        int status = s;
        Drawable drawable = null;
        switch (status){
            case AudioItem.FILE_STATUS_OK:
            case AudioItem.FILE_STATUS_EDIT_BY_USER:
                drawable = getResources().getDrawable(R.drawable.ic_done_all_white,null);
                break;
            case AudioItem.FILE_STATUS_INCOMPLETE:
                drawable = getResources().getDrawable(R.drawable.ic_done_white,null);
                break;
            case AudioItem.FILE_STATUS_BAD:
                drawable = getResources().getDrawable(R.drawable.ic_error_outline_white_24px,null);
                break;
            default:
                drawable = null;
                break;
        }

        return drawable;
    }


    private String getStatusText(){
        int status = currentAudioItem.getStatus();
        String msg = "";
        switch (status){
            case AudioItem.FILE_STATUS_OK:
                msg = getResources().getString(R.string.file_status_ok);
                break;
            case AudioItem.FILE_STATUS_INCOMPLETE:
                msg = getResources().getString(R.string.file_status_incomplete);
                break;
            case AudioItem.FILE_STATUS_BAD:
                msg = getResources().getString(R.string.file_status_bad);
                break;
            case AudioItem.FILE_STATUS_EDIT_BY_USER:
                msg = getResources().getString(R.string.file_status_edit_by_user);
                break;
            default:
                msg = getResources().getString(R.string.file_status_no_processed);
                break;
        }

        return msg;
    }

    private void enableMiniFabs(boolean enable){

        downloadCoverButton.setEnabled(enable);
        editButton.setEnabled(enable);
        autofixButton.setEnabled(enable);
        //ask if we are going to enable or disable mini fabs,
        //if we are are going to disable, lets disable all,
        //else, enable it or disable it depending on if exist cover art
        removeCoverButton.setEnabled(enable ? ( currentCoverArt != null ) : enable  );
    }

    private void showFABMenu(){
        isFABOpen = true;

        floatingActionMenu.animate().rotationBy(-400);

        autofixButton.animate().translationY(-getResources().getDimension(R.dimen.standard_55));

        editButton.animate().translationY(-getResources().getDimension(R.dimen.standard_105));

        downloadCoverButton.animate().translationY(-getResources().getDimension(R.dimen.standard_155));

        removeCoverButton.animate().translationY(-getResources().getDimension(R.dimen.standard_205));

    }

    private void closeFABMenu(){
        isFABOpen = false;

        floatingActionMenu.animate().rotationBy(400);

        autofixButton.animate().translationY(0);

        editButton.animate().translationY(0);

        downloadCoverButton.animate().translationY(0);

        removeCoverButton.animate().translationY(0);


    }


    /**
     * This method help us to play a preview of current song,
     * using the current instance of CustomMediaPlayer
     * @throws IOException
     * @throws InterruptedException
     */
    private void playPreview() throws IOException, InterruptedException {
        player.playPreview(currentItemId);
        if(player.isPlaying()){
            playPreviewButton.setIcon(R.drawable.ic_stop_white_24px);
        }
        else {
            playPreviewButton.setIcon(R.drawable.ic_play_arrow_white_24px);
        }
    }

    /**
     * This method enable the fields for being edited
     * by user, or disable in case the user cancel
     * the operation or when finish the edition
     */
    private void editInfoTrack(){
        if(!editMode) {
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
        mediaMetadataRetriever.setDataSource(trackPath);

        currentCoverArt =  mediaMetadataRetriever.getEmbeddedPicture();
        currentCoverArtLength = currentCoverArt == null ? 0 : currentCoverArt.length;
        newCoverArt = currentCoverArt;
        newCoverArtLength = currentCoverArtLength;

        currentTitle = titleField.getText().toString().trim();
        currentArtist = artistField.getText().toString().trim();
        currentAlbum = albumField.getText().toString().trim();
        currentNumber = numberField.getText().toString().trim();
        currentYear = yearField.getText().toString().trim();
        currentGenre = genreField.getText().toString().trim();
    }


    /**
     * We validate the data entered by the user, there 3 important validations:
     * if field is empty, if data entered is too long and if data has
     * strange characters, in this case, they will be replace by empty character.
     * @return boolean isDataValid
     */
    private boolean isDataValid(){
        //Get all descendants of this swipeRefreshLayout;
        ArrayList<View> fields = viewDetailsTrack.getFocusables(View.FOCUS_DOWN);
        isDataValid = false;
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
                        newTitle = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                    case R.id.artist_name_details:
                        newArtist = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                    case R.id.album_name_details:
                        newAlbum = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                    case R.id.track_genre:
                        newGenre = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                    case R.id.track_number:
                        newNumber = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                    case R.id.track_year:
                        newYear = StringUtilities.trimString(((EditText) fields.get(i)).getText().toString());
                        break;
                }

                //at this point the data has been evaluated as true
                isDataValid = true;


                //If value of this option from app settings is true, replace automatically strange characters
                if(SelectedOptions.AUTOMATICALLY_REPLACE_STRANGE_CHARACTERS){
                    switch (fields.get(i).getId()){
                        case R.id.track_name_details:
                            newTitle = StringUtilities.sanitizeString(newTitle);
                            break;
                        case R.id.artist_name_details:
                            newArtist = StringUtilities.sanitizeString(newArtist);
                            break;
                        case R.id.album_name_details:
                            newAlbum = StringUtilities.sanitizeString(newAlbum);
                            break;
                        case R.id.track_genre:
                            newGenre = StringUtilities.sanitizeString(newGenre);
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

            showSnackBar(Snackbar.LENGTH_SHORT,msg,ACTION_NONE);
        }

    }


    /**
     * This method updates metadata song.
     * @throws IOException
     * @throws ID3WriteException
     */
    private void updateData() throws IOException, ID3WriteException {
        //we update the data creating another thread because the database operation can take a long time
        AsyncUpdateData asyncUpdateData = new AsyncUpdateData(UPDATE_ALL_METADATA);
        asyncUpdateData.execute();
    }

    /**
     * If user is in edit mode and cancel without modify the information,
     * then set the previous values, including the album cover
     */
    private void setPreviousValues(){
        titleField.setText(currentTitle);
        artistField.setText(currentArtist);
        albumField.setText(currentAlbum);
        numberField.setText(currentNumber);
        yearField.setText(currentYear);
        genreField.setText(currentGenre);

        if(currentCoverArt != null && (currentCoverArtLength != newCoverArtLength) ) {
                GlideApp.with(viewDetailsTrack).
                        load(currentCoverArt)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .apply(RequestOptions.skipMemoryCacheOf(true))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .fitCenter()
                        .into(toolbarCover);
                currentCoverArtLength = currentCoverArt.length;
            return;
        }

        if(currentCoverArt == null){
                GlideApp.with(viewDetailsTrack).
                        load(R.drawable.ic_album_white_48px)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .apply(RequestOptions.skipMemoryCacheOf(true))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .fitCenter()
                        .placeholder(R.drawable.ic_album_white_48px)
                        .into(toolbarCover);
                currentCoverArtLength = 0;
        }
    }


    private void stopPlayback() throws IOException, InterruptedException {
        playPreviewButton.setIcon(R.drawable.ic_play_arrow_white_24px);
        if(player != null && player.isPlaying() && this.currentItemId == player.getCurrentId()){
            player.stop();
            player.reset();
        }
    }

    /**
     * Enter to edit mode, for manually
     * modifying the information about the song
     */
    private void enableFieldsToEdit(){
        if(!manualMode)
            appBarLayout.setExpanded(false);

        floatingActionMenu.hide();
        floatingActionMenu.setVisibility(GONE);


        saveButton.setVisibility(View.VISIBLE);
        saveButton.show();
        saveButton.setOnClickListener(null);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //first validate inputs
                boolean validInputs = isDataValid();

                if (validInputs){

                        AlertDialog.Builder builder = new AlertDialog.Builder(DetailsTrackDialogActivity.this);
                        builder.setTitle(R.string.apply_tags);
                        builder.setMessage(R.string.message_apply_new_tags);
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                disableFields();
                            }
                        });
                        builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                updateData();
                                } catch (IOException | ID3WriteException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                }
            }
        });



        titleField.setEnabled(true);
        artistField.setEnabled(true);
        albumField.setEnabled(true);
        numberField.setEnabled(true);
        yearField.setEnabled(true);
        genreField.setEnabled(true);

        imageSize.setText(getString(R.string.edit_cover));
        imageSize.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_add_to_photos_white_24px),null,null,null);
        imageSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectorImageIntent = new Intent(Intent.ACTION_PICK);
                selectorImageIntent.setType("image/*");
                startActivityForResult(selectorImageIntent,INTENT_OPEN_GALLERY);
            }
        });


        editMode = true;

        InputMethodManager imm =(InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(titleField,InputMethodManager.SHOW_IMPLICIT);

    }

    /**
     * This method disable the fields and
     * leaves from edit mode
     */

    private void disableFields(){

        titleField.clearFocus();
        titleField.setEnabled(false);
        artistField.clearFocus();
        artistField.setEnabled(false);
        albumField.clearFocus();
        albumField.setEnabled(false);
        numberField.clearFocus();
        numberField.setEnabled(false);
        yearField.clearFocus();
        yearField.setEnabled(false);
        genreField.clearFocus();
        genreField.setEnabled(false);


        saveButton.setVisibility(GONE);
        saveButton.hide();
        saveButton.setOnClickListener(null);

        floatingActionMenu.show();
        floatingActionMenu.setVisibility(View.VISIBLE);
        imageSize.setText(AudioItem.getStringImageSize(currentCoverArt));
        imageSize.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_photo_white_24px),null,null,null);
        imageSize.setOnClickListener(null);
        editMode = false;

   }



    private void editCover(){
        Intent selectorImageIntent = new Intent(Intent.ACTION_PICK);
        selectorImageIntent.setType("image/*");
        startActivityForResult(selectorImageIntent,INTENT_OPEN_GALLERY);
    }

    /**
     * We implement this method for handling
     * correctly in case the song playback be completed
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("onCompletePlayback","OnFragmnet");
        playPreviewButton.setIcon(R.drawable.ic_play_arrow_white_24px);
        player.onCompletePlayback();


    }

    private class AsyncUpdateData extends AsyncTask<Void, Void, Void> {
        private final String TAG = AsyncUpdateData.class.getName();
        private int operationType;

        AsyncUpdateData(int operationType){
            this.operationType = operationType;
        }
        @Override
        protected void onPreExecute(){
            progressBar.setVisibility(View.VISIBLE);
            try {
                stopPlayback();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            enableMiniFabs(false);
        }

        private void updateCoverArt() {
            //Is priority update the metadata first, in case there are errors when
            //the data is set on item and database
            MyID3 myID3 = new MyID3();
            File tempFile = new File(trackPath);
            MusicMetadataSet musicMetadataSet = null;
            ContentValues contentValues = new ContentValues();

            boolean isSameCoverArt = currentCoverArtLength == newCoverArtLength;

            try {
                musicMetadataSet = myID3.read(tempFile);
                MusicMetadata iMusicMetadata = (MusicMetadata) musicMetadataSet.getSimplified();

                try {

                    //Here we update the data in case there have had changes
                    if(!isSameCoverArt && newCoverArt != null && newCoverArtLength > 0){
                        Vector<ImageData> imageDataVector = new Vector<>();
                        ImageData imageData = new ImageData(newCoverArt, "", "", 3);
                        imageDataVector.add(imageData);
                        iMusicMetadata.setPictureList(imageDataVector);

                        //update genre too because
                        //there is a bug in the library
                        //that causes that if is not saved explicitly
                        //it will save null value
                        iMusicMetadata.setGenre(currentGenre);
                        Log.d("genre", "updated");

                        myID3.update(tempFile, musicMetadataSet, iMusicMetadata);


                        //Then, is necessary update the data in database,
                        //because we obtain this info when the app starts (after first time),
                        //besides, database operations can take a long time
                        contentValues.put(TrackContract.TrackData.STATUS,AudioItem.FILE_STATUS_EDIT_BY_USER);
                        dbHelper.updateData(DetailsTrackDialogActivity.this.currentItemId,contentValues);

                        //Update the data of item from list
                        currentAudioItem.setStatus(AudioItem.FILE_STATUS_EDIT_BY_USER);

                        Log.d("only_cover_art", "updated");
                        Log.d("metadata", "updated");
                    }
                    dataUpdated = true;


                } catch (IOException | ID3WriteException e) {
                    dataUpdated = false;
                    e.printStackTrace();
                }


            } catch (IOException e) {
                e.printStackTrace();
                dataUpdated = false;
                e.printStackTrace();
            }
        }

        private void removeCoverArt() {
            //Is priority update the metadata first, in case there are errors when
            //the data is set on item and database
            MyID3 myID3 = new MyID3();
            File tempFile = new File(trackPath);
            MusicMetadataSet musicMetadataSet = null;
            ContentValues contentValues = new ContentValues();

            try {
                musicMetadataSet = myID3.read(tempFile);
                MusicMetadata iMusicMetadata = (MusicMetadata) musicMetadataSet.getSimplified();

                try {

                    //Here we update the data in case there have had changes
                    iMusicMetadata.remove("pictures");
                    iMusicMetadata.clearPictureList();
                    iMusicMetadata.setPictureList(null);

                    //update genre too because
                    //there is a bug in the library
                    //that causes that if is not saved explicitly
                    //it will save null value
                    iMusicMetadata.setGenre(currentGenre);
                    Log.d("genre", "updated");

                    myID3.update(tempFile, musicMetadataSet, iMusicMetadata);

                    //Then, is necessary update the data in database,
                    //because we obtain this info when the app starts (after first time),
                    //besides, database operations can take a long time
                    contentValues.put(TrackContract.TrackData.STATUS,AudioItem.FILE_STATUS_EDIT_BY_USER);
                    dbHelper.updateData(DetailsTrackDialogActivity.this.currentItemId,contentValues);

                    //Update the data of item from list
                    currentAudioItem.setStatus(AudioItem.FILE_STATUS_EDIT_BY_USER);
                    dataUpdated = true;

                    Log.d("remove_cover_art", "updated");

                } catch (IOException | ID3WriteException e) {
                    dataUpdated = false;
                    e.printStackTrace();
                }


            } catch (IOException e) {
                e.printStackTrace();
                dataUpdated = false;
                e.printStackTrace();
            }
        }


        private void updateMetadata(){
            //Is priority update the metadata first, in case there are errors when
            //the data is set on item and database
            MyID3 myID3 = new MyID3();
            File tempFile = new File(trackPath);
            audioFile = new File(trackPath);
            String currentFileName = audioFile.getName();
            String currentFileNameWithoutExt = currentFileName.substring(0,currentFileName.length()-4);

            MusicMetadataSet musicMetadataSet = null;

            ContentValues contentValues = new ContentValues();

            Log.d("isTitleSameThanFilename", currentFileNameWithoutExt + "-" + newTitle);

            boolean isTitleSameThanFilename = currentFileNameWithoutExt.equals(newTitle);
            boolean isSameTitle = currentTitle.equals(newTitle);
            boolean isSameArtist = currentArtist.equals(newArtist);
            boolean isSameAlbum = currentAlbum.equals(newAlbum);
            boolean isSameGenre = currentGenre.equals(newGenre);
            boolean isSameTrackNumber = currentNumber.equals(newNumber);
            boolean isSameTrackYear = currentYear.equals(newYear);
            boolean isSameCoverArt = currentCoverArtLength == newCoverArtLength;
            boolean hasChanges = !(isSameTitle && isSameArtist && isSameAlbum && isSameGenre && isSameTrackNumber && isSameTrackYear && isSameCoverArt);

            try {
                musicMetadataSet = myID3.read(tempFile);
                MusicMetadata iMusicMetadata = (MusicMetadata) musicMetadataSet.getSimplified();

                //Verify if new values are not empties, because from trackid can come empty values
                if(!isSameTitle && !newTitle.isEmpty()) {
                    iMusicMetadata.setSongTitle(newTitle);
                    contentValues.put(TrackContract.TrackData.TITLE,newTitle);
                    currentAudioItem.setTitle(newTitle);
                    Log.d("title", "updated");
                }
                if(!isSameArtist && !newArtist.isEmpty()) {
                    iMusicMetadata.setArtist(newArtist);
                    contentValues.put(TrackContract.TrackData.ARTIST,newArtist);
                    currentAudioItem.setArtist(newArtist);
                    Log.d("artist", "updated");
                }
                if(!isSameAlbum && !newAlbum.isEmpty()) {
                    iMusicMetadata.setAlbum(newAlbum);
                    contentValues.put(TrackContract.TrackData.ALBUM,newAlbum);
                    currentAudioItem.setAlbum(newAlbum);
                    Log.d("album", "updated");
                }
                if(!isSameTrackNumber && !newNumber.isEmpty()) {
                    iMusicMetadata.setTrackNumber(Integer.parseInt(newNumber));
                    Log.d("number", "updated");
                }
                if(!isSameTrackYear && !newYear.isEmpty()) {
                    iMusicMetadata.setYear(newYear);
                    Log.d("year", "updated");
                }
                //Always save genre because there is a bug in the library
                //that makes null this value if is not updated
                if(!newGenre.isEmpty()) {
                    iMusicMetadata.setGenre(newGenre);
                }
                else {
                    iMusicMetadata.setGenre(currentGenre);
                }
                Log.d("genre", "updated");

                if(!isSameCoverArt && newCoverArt != null && newCoverArt.length > 0){
                    Vector<ImageData> imageDataVector = new Vector<>();
                    ImageData imageData = new ImageData(newCoverArt, "", "", 3);
                    imageDataVector.add(imageData);
                    iMusicMetadata.setPictureList(imageDataVector);

                    Log.d("coverart", "updated");
                }

                try {
                    //Here we update the data in case there have had changes
                    if(hasChanges) {
                        myID3.update(tempFile, musicMetadataSet, iMusicMetadata);
                        Log.d("all_metadata", "updated");
                    }
                    //We check if this option is selected in settings,
                    //before writing to database
                    if(SelectedOptions.MANUAL_CHANGE_FILE){
                        //new title is not the same than old title? then rename file
                        if(!isTitleSameThanFilename) {
                            String newAbsolutePath = AudioItem.renameFile(currentAudioItem.getAbsolutePath(), newTitle, newArtist);
                            currentAudioItem.setAbsolutePath(newAbsolutePath);
                            contentValues.put(TrackContract.TrackData.DATA, newAbsolutePath);

                            //lets inform to system that one file has change
                            ContentValues values = new ContentValues();
                            String selection = MediaStore.MediaColumns.DATA + "= ?";
                            String selectionArgs[] = {trackPath}; //old path
                            values.put(MediaStore.MediaColumns.DATA, newAbsolutePath); //new path
                            boolean successMediaStore = getContentResolver().
                                                        update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                                                values,
                                                                selection,
                                                                selectionArgs) == 1;

                            currentAudioItem.setAbsolutePath(newAbsolutePath);
                            trackPath = newAbsolutePath;
                            audioFile = null;
                            Log.d("media store success", successMediaStore+"");
                            Log.d("filename", "update");
                        }

                    }

                    //Then, is necessary update the data in database,
                    //because we obtain this info when the app starts (after first time),
                    //besides, database operations can take a long time
                    contentValues.put(TrackContract.TrackData.STATUS,AudioItem.FILE_STATUS_EDIT_BY_USER);
                    //Update the data of item from list
                    currentAudioItem.setStatus(AudioItem.FILE_STATUS_EDIT_BY_USER);

                    dbHelper.updateData(currentItemId,contentValues);
                    dataUpdated = true;
                } catch (IOException | ID3WriteException e) {
                    dataUpdated = false;
                    e.printStackTrace();
                }


            } catch (IOException e) {
                e.printStackTrace();
                dataUpdated = false;
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (operationType == UPDATE_COVER) {
                updateCoverArt();
            }
            else if(operationType == REMOVE_COVER){
                removeCoverArt();
            }
            else {
                updateMetadata();
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void result){
            doAtFinal();
            enableMiniFabs(true);
        }

        private void doAtFinal(){
            String msg = "";

            audioFile = new File(trackPath);
            if(dataUpdated){
                final boolean isSameCoverArt = currentCoverArtLength == newCoverArtLength;
                //update toolbar cover only if UPDATE_COVER is true, and new cover is not null
                if(operationType == UPDATE_COVER && !isSameCoverArt && newCoverArt != null && newCoverArt.length > 0){

                    msg = getString(R.string.cover_updated);
                    trackIdCard.animate().setDuration(DURATION).alpha(0).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            trackIdCard.setVisibility(GONE);
                            content.animate().setDuration(DURATION).alpha(1).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    GlideApp.with(viewDetailsTrack).
                                            load(newCoverArt)
                                            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .apply(RequestOptions.skipMemoryCacheOf(true))
                                            .fitCenter()
                                            .into(toolbarCover);

                                    content.setVisibility(View.VISIBLE);

                                    trackIdCard.setVisibility(GONE);
                                    status.setText(getStatusText());

                                    status.setCompoundDrawablesWithIntrinsicBounds(getStatusDrawable(currentAudioItem.getStatus()),null,null,null);
                                    fileSize.setText(AudioItem.getFileSize(audioFile.length()));
                                    imageSize.setText(AudioItem.getStringImageSize(newCoverArt));

                                }
                            });
                        }
                    });

                }
                //only update toolbar cover
                else if(operationType == REMOVE_COVER){

                    msg = getString(R.string.cover_removed);
                    status.setText(getStatusText());
                    status.setCompoundDrawablesWithIntrinsicBounds(getStatusDrawable(currentAudioItem.getStatus()),null,null,null);
                    fileSize.setText(AudioItem.getFileSize(audioFile.length()));
                    imageSize.setText(AudioItem.getStringImageSize(newCoverArt));

                    GlideApp.with(viewDetailsTrack).
                            load(R.drawable.ic_album_white_48px)
                            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                            .apply(RequestOptions.skipMemoryCacheOf(true))
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .fitCenter()
                            .placeholder(R.drawable.ic_album_white_48px)
                            .into(toolbarCover);

                }
                //here update all views
                else {
                    msg = getString(R.string.message_data_update);

                    //Update fields in case we have replaced the strange chars
                    titleLayer.setText(audioFile.getName());
                    titleField.setText(currentAudioItem.getTitle());
                    artistField.setText(currentAudioItem.getArtist());
                    albumField.setText(currentAudioItem.getAlbum());
                    genreField.setText(newGenre);
                    numberField.setText(newNumber);
                    yearField.setText(newYear);

                    status.setText(getStatusText());
                    status.setCompoundDrawablesWithIntrinsicBounds(getStatusDrawable(currentAudioItem.getStatus()),null,null,null);
                    fileSize.setText(AudioItem.getFileSize(audioFile.length()));
                    imageSize.setText(AudioItem.getStringImageSize(newCoverArt));


                    trackIdCard.animate().setDuration(DURATION).alpha(0).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            trackIdCard.setVisibility(GONE);

                            content.animate().setDuration(DURATION).alpha(1).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    content.setVisibility(View.VISIBLE);
                                    if(!isSameCoverArt) {
                                        GlideApp.with(viewDetailsTrack).
                                                load(newCoverArt != null ? newCoverArt : R.drawable.ic_album_white_48px)
                                                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                                .apply(RequestOptions.skipMemoryCacheOf(true))
                                                .transition(DrawableTransitionOptions.withCrossFade())
                                                .fitCenter()
                                                .into(toolbarCover);
                                    }
                                }
                            });

                        }
                    });


                }
                wasModified = 1;
            }
            else {
                wasModified = 0;
                msg = getString(R.string.message_no_data_updated);
            }

            cachingCurrentValues();
            disableFields();


            onlyCoverArt = false;
            dataUpdated = false;

            progressBar.setVisibility(View.INVISIBLE);
            appBarLayout.setExpanded(true);

            floatingActionMenu.show();
            saveButton.hide();
            trackAdapter.notifyDataSetChanged();
            showSnackBar(Snackbar.LENGTH_SHORT, msg,ACTION_NONE);

        }

    }



    @Override
    protected void onPause(){
        super.onPause();
        localBroadcastManager.unregisterReceiver(receiver);
    }

    @Override
    protected void onResume(){
        super.onResume();
        localBroadcastManager.registerReceiver(receiver,intentFilter);
        localBroadcastManager.registerReceiver(receiver,intentFilter2);
    }

    private void setDownloadedValues(){
        progressBar.setVisibility(View.INVISIBLE);

        //if there are no results, only notify to user
        if(trackIdAudioItem == null){
            showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.file_status_bad),ACTION_NONE);
            return;
        }

        //get new values from trackIdItem
        newTitle = trackIdAudioItem.getTitle();
        newArtist = trackIdAudioItem.getArtist();
        newAlbum = trackIdAudioItem.getAlbum();
        newGenre = trackIdAudioItem.getGenre();
        newNumber = trackIdAudioItem.getTrackNumber();
        newYear = trackIdAudioItem.getTrackYear();

        newCoverArt = trackIdAudioItem.getCoverArt();
        newCoverArtLength = newCoverArt == null ? 0 : newCoverArt.length;



        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DetailsTrackDialogActivity.this);
                builder.setTitle(R.string.apply_tags);
                builder.setMessage(R.string.message_apply_found_tags);
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        disableLayout();
                    }
                });
                builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AsyncUpdateData asyncUpdateData = new AsyncUpdateData(UPDATE_ALL_METADATA);
                        asyncUpdateData.execute();
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });


        editMode = true;
        content.animate().setDuration(DURATION).alpha(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                content.setVisibility(GONE);
                appBarLayout.setExpanded(false);

                trackIdCard.animate().setDuration(DURATION).alpha(1).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        trackIdCard.setVisibility(View.VISIBLE);

                        //make visible the trackidcover if available
                        trackIdCover.setVisibility(View.VISIBLE);
                        trackidCoverArtDimensions.setVisibility(View.VISIBLE);

                        if(newCoverArt != null) {
                            GlideApp.with(viewDetailsTrack).
                                    load(newCoverArt)
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .fitCenter()
                                    .into(trackIdCover);

                            trackidCoverArtDimensions.setText(AudioItem.getStringImageSize(newCoverArt));

                        }
                        else{
                            GlideApp.with(viewDetailsTrack).
                                    load(getDrawable(R.drawable.ic_album_white_48px))
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .placeholder(R.drawable.ic_album_white_48px)
                                    .fitCenter()
                                    .into(trackIdCover);
                        }



                        //make visible only fields when trackid value is available
                        trackIdTitle.setText(newTitle);
                        trackIdTitle.setVisibility(newTitle.equals("")?GONE:View.VISIBLE);

                        trackIdArtist.setText(newArtist);
                        trackIdArtist.setVisibility(newArtist.equals("")?GONE:View.VISIBLE);

                        trackIdAlbum.setText(newAlbum);
                        trackIdAlbum.setVisibility(newAlbum.equals("")?GONE:View.VISIBLE);

                        trackIdGenre.setText(newGenre);
                        trackIdGenre.setVisibility(newGenre.equals("")?GONE:View.VISIBLE);

                        trackIdNumber.setText(newNumber);
                        trackIdNumber.setVisibility(newNumber.equals("")?GONE:View.VISIBLE);

                        trackIdYear.setText(newYear);
                        trackIdYear.setVisibility(newYear.equals("")?GONE:View.VISIBLE);

                        //inform to user the meta tags found
                        showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.info_found),ACTION_NONE);

                        //let the user apply this changes
                        floatingActionMenu.hide();
                        floatingActionMenu.setVisibility(GONE);

                        saveButton.show();
                        saveButton.setVisibility(View.VISIBLE);

                    }
                });

            }
        });

    }


    private void handleCoverArt() {

        String msg = "";

        newCoverArt = trackIdAudioItem.getCoverArt();
        newCoverArtLength = newCoverArt == null ? 0 : newCoverArt.length;


        if(newCoverArt != null){
            msg = getString(R.string.cover_art_found);
            appBarLayout.setExpanded(false);
            editMode = true;

            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DetailsTrackDialogActivity.this);
                    builder.setTitle(getString(R.string.title_downloaded_cover_art_dialog));
                    builder.setNegativeButton(getString(R.string.as_cover_art), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AsyncUpdateData asyncUpdateData = new AsyncUpdateData(UPDATE_COVER);
                            asyncUpdateData.execute();
                        }
                    });
                    builder.setPositiveButton(getString(R.string.as_file), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //if was successful saved, then
                            String newImageAbsolutePath = FileSaver.saveFile(newCoverArt, trackIdAudioItem.getTitle(), trackIdAudioItem.getArtist(), trackIdAudioItem.getAlbum());
                            if(!newImageAbsolutePath.equals(FileSaver.NULL_DATA) && !newImageAbsolutePath.equals(FileSaver.NO_EXTERNAL_STORAGE_WRITABLE) && !newImageAbsolutePath.equals(FileSaver.INPUT_OUTPUT_ERROR)) {

                                showSnackBar(7000, getString(R.string.cover_saved) + " " + AudioItem.getRelativePath(newImageAbsolutePath) + ".", ACTION_NONE);
                                //lets inform to system that one file has been created
                                MediaScannerConnection.scanFile(getApplicationContext(),
                                        new String[]{newImageAbsolutePath},
                                        null,
                                        null);
                            }
                            else {
                                showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.cover_not_saved), ACTION_NONE);
                            }

                            dialog.cancel();
                            disableLayout();
                        }
                    });
                    builder.setMessage(R.string.description_downloaded_cover_art_dialog);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setCancelable(true);
                    alertDialog.show();
                }
            });

            final String finalMsg = msg;
            content.animate().setDuration(DURATION).alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    content.setVisibility(View.GONE);

                    trackIdCard.animate().setDuration(DURATION).alpha(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {

                            trackIdCard.setVisibility(View.VISIBLE);

                            GlideApp.with(viewDetailsTrack).
                                    load(newCoverArt)
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .fitCenter()
                                    .into(trackIdCover);

                            trackidCoverArtDimensions.setText(AudioItem.getStringImageSize(newCoverArt));

                            showSnackBar(Snackbar.LENGTH_SHORT, finalMsg,ACTION_NONE);

                            floatingActionMenu.hide();
                            floatingActionMenu.setVisibility(GONE);

                            saveButton.setVisibility(View.VISIBLE);
                            saveButton.show();
                        }
                    });
                }
            });


        }
        else{
            msg = getString(R.string.no_cover_art_found);
            onlyCoverArt = false;
            newCoverArt = currentCoverArt;
            newCoverArtLength = currentCoverArtLength;
            showSnackBar(Snackbar.LENGTH_LONG, msg,ACTION_ADD_COVER);
            enableMiniFabs(true);

        }

        progressBar.setVisibility(View.INVISIBLE);

    }

    private int allowExecute(){

        //API not initialized
        if(!apiInitialized){
            Job.scheduleJob(getApplicationContext());
            return SelectFolderActivity.NO_INITIALIZED_API;
        }


        //No internet connection
        if(!DetectorInternetConnection.isConnected(getApplicationContext())){
            return SelectFolderActivity.NO_INTERNET_CONNECTION;
        }



        return 0;
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(FixerTrackService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });


            Log.d("action_received",action);
            switch (action){
                case FixerTrackService.ACTION_DONE:
                    trackIdAudioItem = intent.getParcelableExtra(FixerTrackService.AUDIO_ITEM);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (onlyCoverArt) {
                                handleCoverArt();
                            }
                            else {
                                setDownloadedValues();
                            }
                        }
                    });

                    break;
                /*case FixerTrackService.ACTION_CANCEL:
                case FixerTrackService.ACTION_COMPLETE_TASK:*/
                case FixerTrackService.ACTION_FAIL:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.file_status_bad),ACTION_NONE);
                        }
                    });

                    break;
            }




        }
    }

}
