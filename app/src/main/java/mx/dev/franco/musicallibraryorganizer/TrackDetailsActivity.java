package mx.dev.franco.musicallibraryorganizer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
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
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagOptionSingleton;
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
import mx.dev.franco.musicallibraryorganizer.services.GnService;
import mx.dev.franco.musicallibraryorganizer.services.Job;
import mx.dev.franco.musicallibraryorganizer.transitions.DetailsTransition;
import mx.dev.franco.musicallibraryorganizer.utilities.CustomMediaPlayer;
import mx.dev.franco.musicallibraryorganizer.utilities.FileSaver;
import mx.dev.franco.musicallibraryorganizer.utilities.GlideApp;
import mx.dev.franco.musicallibraryorganizer.utilities.StringUtilities;

import static android.view.View.GONE;
import static mx.dev.franco.musicallibraryorganizer.services.GnService.API_INITIALIZED;
import static mx.dev.franco.musicallibraryorganizer.services.GnService.apiInitialized;

/**
 * Created by franco on 22/07/17.
 */

public class TrackDetailsActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {


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
    private static final int ACTION_VIEW_COVER = 32;
    private static final int DURATION = 200;


    //flag to indicate that is just required to download
    //the coverart
    private boolean onlyCoverArt = false;
    //Id from audio item_list
    private long currentItemId;
    //flag when user is editing info
    private boolean editMode = false;
    //A reference to database connection
    private DataTrackDbHelper dbHelper;
    private boolean manualMode = false;
    //rootview
    private View viewDetailsTrack;
    //References to elements inside the layout
    private FloatingActionButton editButton;
    private FloatingActionButton downloadCoverButton;
    private FloatingActionButton autoFixButton;
    private FloatingActionButton extractCoverButton;
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
    //reference to current audio item_list being edited
    private AudioItem currentAudioItem = null;
    //audio item_list to store response data of making a trackId inside this activity
    private AudioItem trackIdAudioItem = null;

    //Broadcast manager to manage the response from FixerTrackService intent service
    private LocalBroadcastManager localBroadcastManager;
    //Filter only certain responses from FixerTrackService
    private IntentFilter filterActionCompleteTask;
    private IntentFilter filterActionApiInitialized;
    private IntentFilter filterActionNotFound;
    private IntentFilter filterActionDoneDetails;
    //Receiver to handle responses
    private ResponseReceiver receiver;

    //Flag for saving the result of validating the fields of layout
    private boolean isDataValid = false;
    private boolean isFABOpen = false;
    private boolean isLookingUpInfo = false;

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
    private TextView trackIdCoverArtDimensions;
    private TrackAdapter trackAdapter;
    private String frequencyVal;
    private int resolutionVal;
    private String channelsVal;
    private String fileType;
    private AudioFile audioTaggerFile;
    private Tag tag;
    private AudioHeader audioHeader;


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
        setContentView(R.layout.activity_track_details);

        //Create receiver and filters to handle responses from FixerTrackService
        filterActionDoneDetails = new IntentFilter(FixerTrackService.ACTION_DONE_DETAILS);
        filterActionCompleteTask = new IntentFilter(FixerTrackService.ACTION_COMPLETE_TASK);
        filterActionApiInitialized = new IntentFilter(GnService.API_INITIALIZED);
        filterActionNotFound = new IntentFilter(FixerTrackService.ACTION_NOT_FOUND);
        receiver = new ResponseReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        //main layout or root view
        viewDetailsTrack = findViewById(R.id.rootContainerDetails);
        //We get the current instance of CustomMediaPlayer object
        player = CustomMediaPlayer.getInstance(getApplicationContext());
        trackAdapter = (TrackAdapter) player.getAdapter();

        //if this intent comes from dialog when it touches any element from list of MainActivity
        manualMode = getIntent().getBooleanExtra(FixerTrackService.MANUAL_MODE,false);
        //currentId of audioItem
        currentItemId = getIntent().getLongExtra(FixerTrackService.MEDIASTORE_ID,-1);


        currentAudioItem = trackAdapter.getItemByIdOrPath(currentItemId, null); //getIntent().getParcelableExtra(FixerTrackService.AUDIO_ITEM); //MainActivity.getItemByIdOrPath(currentItemId,null);


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
        TextView tv = (TextView) this.snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.primaryLightColor));
        tv.setTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));
        snackbar.setActionTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));
    }

    /**
     * Shows the snackbar with the params received
     * @param duration how long is displayed the snackbar
     * @param msg message to display
     * @param action action to execute
     * @param path absolute path of file
     */
    private void showSnackBar(int duration, String msg, int action, final String path){
        if(snackbar != null){
            snackbar = null;
            createSnackBar();
        }

        if(action == ACTION_NONE){
            snackbar.setText(msg);
            snackbar.setDuration(duration);
            snackbar.setAction("",null);
        }
        else if(action == ACTION_ADD_COVER) {
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
        else{
            snackbar.setText(msg);
            snackbar.setDuration(duration);
            snackbar.setAction(R.string.view_cover, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openInExternalApp(path);
                }
            });
        }


        snackbar.show();
    }

    private void openInExternalApp(String path){
        File file = new File(path);
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        String type = mime.getMimeTypeFromExtension(ext);
        try {

            Intent intent = new Intent();
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


    private void showSnackBar(int reason){
        String msg = "";
        switch (reason){
            case MainActivity.NO_INTERNET_CONNECTION:
                msg = getString(R.string.no_internet_connection_semi_automatic_mode);
                break;
            case NO_INTERNET_CONNECTION_COVER_ART:
                msg = getString(R.string.no_internet_connection_download_cover);
                break;
            case MainActivity.NO_INITIALIZED_API:
                msg = getString(R.string.initializing_recognition_api);
                break;
        }
        showSnackBar(Snackbar.LENGTH_SHORT, msg, ACTION_NONE, null);
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

        MenuItem removeItem = menu.findItem(R.id.action_remove_cover);
        removeItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                if(currentCoverArt == null){
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

        MenuItem searchInWebItem = menu.findItem(R.id.action_web_search);
        searchInWebItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String queryString = currentTitle + (!currentArtist.isEmpty() ? (" " + currentArtist) : "");
                String query = "http://www.google.com/#q=" + queryString;
                Uri uri = Uri.parse(query);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return false;
            }
        });

        MenuItem playOn = menu.findItem(R.id.action_play_on);
        playOn.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                openInExternalApp(trackPath);
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
        super.onBackPressed();
    }

    private void disableLayout(){

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == INTENT_OPEN_GALLERY && data != null){
            Uri imageData = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageData);
                if(bitmap.getHeight() > 1080 || bitmap.getWidth() > 1080){

                    showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.image_too_big),ACTION_NONE, null);
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
                showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.error_load_image),ACTION_NONE, null);
                newCoverArt = currentCoverArt;
                newCoverArtLength = currentCoverArtLength;
            }
        }
    }

    @Override
    public void onDestroy(){
        dbHelper = null;
        titleField = null;
        artistField = null;
        albumField = null;
        numberField = null;
        yearField = null;
        genreField = null;
        audioTaggerFile = null;
        tag = null;
        audioHeader = null;
        currentCoverArt = null;
        trackAdapter = null;
        viewDetailsTrack = null;
        editButton = null;
        extractCoverButton = null;
        player = null;
        progressBar = null;
        currentAudioItem = null;
        trackIdAudioItem = null;
        filterActionDoneDetails = null;
        filterActionCompleteTask = null;
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
        extractCoverButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.extractCover);
        editButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.editTrackInfo);
        autoFixButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.autofix);
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
        trackIdCoverArtDimensions = (TextView) viewDetailsTrack.findViewById(R.id.trackidCoverArtDimensions);
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

        TagOptionSingleton.getInstance().setAndroid(true);
        try {
            audioTaggerFile = AudioFileIO.read(new File(trackPath));
            tag = audioTaggerFile.getTag();
            audioHeader = audioTaggerFile.getAudioHeader();


        currentCoverArt = tag.getFirstArtwork() == null ? null : tag.getFirstArtwork().getBinaryData();//mediaMetadataRetriever.getEmbeddedPicture();
        currentCoverArtLength = currentCoverArt == null ? 0 :currentCoverArt.length;

        //initial values of new cover art will be the same of current cover art
        newCoverArt = currentCoverArt;
        newCoverArtLength = currentCoverArtLength;

        currentTitle = tag.getFirst(FieldKey.TITLE);
        currentArtist = tag.getFirst(FieldKey.ARTIST);
        currentAlbum = tag.getFirst(FieldKey.ALBUM);
        currentNumber = tag.getFirst(FieldKey.TRACK);
        currentYear = tag.getFirst(FieldKey.YEAR);
        currentGenre = tag.getFirst(FieldKey.GENRE);
        currentDuration = audioHeader.getTrackLength()+"";
        bitrate = audioHeader.getBitRate();
        frequencyVal = audioHeader.getSampleRate();
        resolutionVal = audioHeader.getBitsPerSample();
        channelsVal = audioHeader.getChannels();
        fileType = audioHeader.getFormat();
        audioFile = new File(trackPath);
        setCurrentValues();
        } catch (CannotReadException | IOException | ReadOnlyFileException | InvalidAudioFrameException | TagException e) {
            e.printStackTrace();
            showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.could_not_read_file),ACTION_NONE,null);
        }

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
        trackType.setText(fileType);

        titleLayer.setText(audioFile.getName());
        subtitleLayer.setText(AudioItem.getRelativePath(audioFile.getParent()));
        imageSize.setText(AudioItem.getStringImageSize(currentCoverArt));
        fileSize.setText(AudioItem.getFileSize(audioFile.length()));
        trackLength.setText(AudioItem.getHumanReadableDuration(currentDuration));
        status.setText(getStatusText());
        status.setCompoundDrawablesWithIntrinsicBounds(getStatusDrawable(currentAudioItem.getStatus()),null,null,null);
        bitrateField.setText(AudioItem.getBitrate(bitrate));
        frequency.setText(AudioItem.getFrequency(frequencyVal));
        resolution.setText(AudioItem.getResolution(resolutionVal));
        channels.setText(channelsVal);

        if(manualMode){
            enableFieldsToEdit();
        }

        addActionListeners();
    }


    private void addActionListeners(){

        extractCoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                if(currentCoverArt == null){
                    showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.can_not_extract_null_cover),ACTION_NONE,null);
                    return;
                }

                String newImageAbsolutePath = FileSaver.saveFile(currentCoverArt, currentTitle, currentArtist, currentAlbum);
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

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                editInfoTrack();
            }
        });

        autoFixButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();

                //This function requires some contidions to work, check them before
                int canContinue = allowExecute();
                if(canContinue != 0) {
                    showSnackBar(canContinue);
                    return;
                }

                showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.downloading_tags),ACTION_NONE, null);

                progressBar.setVisibility(View.VISIBLE);

                enableMiniFabs(false);

                //Check if exist trackidItem cached from previous request
                //this with the objective of saving data
                if(trackIdAudioItem != null) {
                    setDownloadedValues();
                }
                else {
                    toolbarCover.setEnabled(false);
                    isLookingUpInfo = true;
                    Intent intent = new Intent(TrackDetailsActivity.this, FixerTrackService.class);
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
                //continue
                int canContinue = allowExecute();

                if(canContinue != 0) {
                    showSnackBar(canContinue);
                    return;
                }

                showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.downloading_cover),ACTION_NONE, null);

                progressBar.setVisibility(View.VISIBLE);
                enableMiniFabs(false);
                onlyCoverArt = true;
                //Check if exist trackidItem cached from previous request
                //before making a request
                if(trackIdAudioItem != null){
                    handleCoverArt();
                }
                else{
                    toolbarCover.setEnabled(false);
                    isLookingUpInfo = true;
                    Intent intent = new Intent(TrackDetailsActivity.this,FixerTrackService.class);
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
                if(isFABOpen)
                    closeFABMenu();

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
                //no cover art was found?
                if(trackIdAudioItem.getCoverArt() == null)
                    return;

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
        autoFixButton.setEnabled(enable);
        //ask if we are going to enable or disable mini fabs,
        //if we are are going to disable, lets disable all,
        //else, enable it or disable it depending on if exist cover art
        extractCoverButton.setEnabled(enable);
    }

    private void showFABMenu(){
        isFABOpen = true;

        floatingActionMenu.animate().rotation(-400);

        autoFixButton.animate().translationY(-getResources().getDimension(R.dimen.standard_55));

        editButton.animate().translationY(-getResources().getDimension(R.dimen.standard_105));

        downloadCoverButton.animate().translationY(-getResources().getDimension(R.dimen.standard_155));

        extractCoverButton.animate().translationY(-getResources().getDimension(R.dimen.standard_205));

    }

    private void closeFABMenu(){
        isFABOpen = false;

        floatingActionMenu.animate().rotation(0);

        autoFixButton.animate().translationY(0);

        editButton.animate().translationY(0);

        downloadCoverButton.animate().translationY(0);

        extractCoverButton.animate().translationY(0);


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

        currentCoverArt =  tag.getFirstArtwork() == null ? null :tag.getFirstArtwork().getBinaryData();
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
            player.onCompletePlayback();
            //player.stop();
            //player.reset();
        }
    }

    /**
     * Enter to edit mode, for manually
     * modifying the information about the song
     */
    private void enableFieldsToEdit(){
        if(!manualMode)
            appBarLayout.setExpanded(false);

        //floatingActionMenu.hide();
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

                        AlertDialog.Builder builder = new AlertDialog.Builder(TrackDetailsActivity.this);
                        builder.setTitle(R.string.apply_tags);
                        builder.setMessage(R.string.message_apply_new_tags);
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                disableFields();
                                appBarLayout.setExpanded(true);
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
        private boolean overwriteAllTags = false;


        AsyncUpdateData(int operationType, boolean overwriteAllTags){
            this.operationType = operationType;
            this.overwriteAllTags = overwriteAllTags;
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
            toolbarCover.setEnabled(false);
        }

        private void updateCoverArt() {
            //Is priority update the metadata first, in case there are errors when
            //the data is set on item_list and database
            try {
                ContentValues contentValues = new ContentValues();

                boolean isSameCoverArt = currentCoverArtLength == newCoverArtLength;


                //Here we update the data in case there have had changes
                if (!isSameCoverArt && newCoverArt != null && newCoverArtLength > 0) {
                    Artwork artwork = new AndroidArtwork();
                    artwork.setBinaryData(newCoverArt);
                    tag.setField(artwork);
                    audioTaggerFile.commit();
                    //Then, is necessary update the data in database,
                    //because we obtain this info when the app starts (after first time),
                    //besides, database operations can take a long time
                    contentValues.put(TrackContract.TrackData.STATUS, AudioItem.FILE_STATUS_EDIT_BY_USER);
                    dbHelper.updateData(TrackDetailsActivity.this.currentItemId, contentValues);

                    //Update the data of item_list from list
                    currentAudioItem.setStatus(AudioItem.FILE_STATUS_EDIT_BY_USER);

                    Log.d("only_cover_art", "updated");
                }
                dataUpdated = true;
            }
            catch (CannotWriteException | TagException e){
                dataUpdated = false;
                e.printStackTrace();
            }

        }

        private void removeCoverArt() {
            //Is priority update the metadata first, in case there are errors when
            //the data is set on item_list and database
            try {

                ContentValues contentValues = new ContentValues();
                tag.deleteArtworkField();
                audioTaggerFile.commit();

                //Then, is necessary update the data in database,
                //because we obtain this info when the app starts (after first time),
                //besides, database operations can take a long time
                contentValues.put(TrackContract.TrackData.STATUS,AudioItem.FILE_STATUS_EDIT_BY_USER);
                dbHelper.updateData(TrackDetailsActivity.this.currentItemId,contentValues);

                //Update the data of item_list from list
                currentAudioItem.setStatus(AudioItem.FILE_STATUS_EDIT_BY_USER);
                dataUpdated = true;

                Log.d("remove_cover_art", "updated");



            } catch (CannotWriteException e) {
                dataUpdated = false;
                e.printStackTrace();
            }
        }


        private void updateMetadata(){
            //Is priority update the metadata first, in case there are errors when
            //the data is set on item_list and database
            try {

                audioFile = new File(trackPath);
                String currentFileName = audioFile.getName();
                String currentFileNameWithoutExt = currentFileName.substring(0,currentFileName.length()-4);

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

                //Verify if new values are not empties, because from trackid can come empty values
                if(!isSameTitle && !newTitle.isEmpty()) {
                    tag.setField(FieldKey.TITLE, newTitle);
                    contentValues.put(TrackContract.TrackData.TITLE,newTitle);
                    currentAudioItem.setTitle(newTitle);
                    Log.d("title", "updated");
                }
                if(!isSameArtist && !newArtist.isEmpty()) {
                    tag.setField(FieldKey.ARTIST, newArtist);
                    contentValues.put(TrackContract.TrackData.ARTIST,newArtist);
                    currentAudioItem.setArtist(newArtist);
                    Log.d("artist", "updated");
                }
                if(!isSameAlbum && !newAlbum.isEmpty()) {
                    tag.setField(FieldKey.ALBUM, newAlbum);
                    contentValues.put(TrackContract.TrackData.ALBUM,newAlbum);
                    currentAudioItem.setAlbum(newAlbum);
                    Log.d("album", "updated");
                }
                if(!isSameTrackNumber && !newNumber.isEmpty()) {
                    tag.setField(FieldKey.TRACK, newNumber);
                    Log.d("number", "updated");
                }
                if(!isSameTrackYear && !newYear.isEmpty()) {
                    tag.setField(FieldKey.YEAR, newYear);
                    Log.d("year", "updated");
                }

                if(!isSameGenre && !newGenre.isEmpty()) {
                    tag.setField(FieldKey.GENRE, newGenre);
                    Log.d("genre", "updated");
                }

                if(!isSameCoverArt && newCoverArt != null && newCoverArt.length > 0){
                    Artwork artwork = new AndroidArtwork();
                    artwork.setBinaryData(newCoverArt);
                    tag.setField(artwork);

                    Log.d("coverart", "updated");
                }

                //Here we update the data in case there have had changes
                if(hasChanges) {
                    audioTaggerFile.commit();
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
                //Update the data of item_list from list
                currentAudioItem.setStatus(AudioItem.FILE_STATUS_EDIT_BY_USER);

                dbHelper.updateData(currentItemId,contentValues);
                dataUpdated = true;


            }
            catch ( CannotWriteException | TagException e) {
                e.printStackTrace();
                dataUpdated = false;
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
                            content.setVisibility(View.VISIBLE);
                            content.animate().setDuration(DURATION).alpha(1).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {

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
            }
            else {
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
            toolbarCover.setEnabled(true);
            trackAdapter.notifyDataSetChanged();
            showSnackBar(Snackbar.LENGTH_SHORT, msg,ACTION_NONE, null);

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
        localBroadcastManager.registerReceiver(receiver, filterActionDoneDetails);
        localBroadcastManager.registerReceiver(receiver, filterActionCompleteTask);
        localBroadcastManager.registerReceiver(receiver, filterActionApiInitialized);
        localBroadcastManager.registerReceiver(receiver, filterActionNotFound);
    }

    private void setDownloadedValues(){
        progressBar.setVisibility(View.INVISIBLE);
        isLookingUpInfo = false;
        toolbarCover.setEnabled(true);
        if(isFABOpen)
            closeFABMenu();
        //if there are no results, only notify to user
        if(trackIdAudioItem == null){
            showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.file_status_bad),ACTION_NONE, null);
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
                AlertDialog.Builder builder = new AlertDialog.Builder(TrackDetailsActivity.this);
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
                        AsyncUpdateData asyncUpdateData = new AsyncUpdateData(UPDATE_ALL_METADATA, false);
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
                        trackIdCoverArtDimensions.setVisibility(View.VISIBLE);

                        if(newCoverArt != null) {
                            GlideApp.with(viewDetailsTrack).
                                    load(newCoverArt)
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                    .apply(RequestOptions.skipMemoryCacheOf(true))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .fitCenter()
                                    .into(trackIdCover);

                            trackIdCoverArtDimensions.setText(AudioItem.getStringImageSize(newCoverArt));

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
                        showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.info_found),ACTION_NONE, null);

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
        toolbarCover.setEnabled(true);
        isLookingUpInfo = false;
        if(isFABOpen)
            closeFABMenu();
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(TrackDetailsActivity.this);
                    builder.setTitle(getString(R.string.title_downloaded_cover_art_dialog));
                    builder.setPositiveButton(getString(R.string.as_cover_art), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AsyncUpdateData asyncUpdateData = new AsyncUpdateData(UPDATE_COVER, false);
                            asyncUpdateData.execute();
                        }
                    });
                    builder.setNegativeButton(getString(R.string.as_file), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //if was successful saved, then
                            String newImageAbsolutePath = FileSaver.saveFile(newCoverArt, trackIdAudioItem.getTitle(), trackIdAudioItem.getArtist(), trackIdAudioItem.getAlbum());
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

                            trackIdCoverArtDimensions.setText(AudioItem.getStringImageSize(newCoverArt));

                            showSnackBar(Snackbar.LENGTH_SHORT, finalMsg,ACTION_NONE, null);

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
            showSnackBar(Snackbar.LENGTH_LONG, msg,ACTION_ADD_COVER, null);
            enableMiniFabs(true);

        }

        progressBar.setVisibility(View.INVISIBLE);

    }

    private int allowExecute(){

        //API not initialized
        if(!apiInitialized){
            Job.scheduleJob(getApplicationContext());
            return MainActivity.NO_INITIALIZED_API;
        }


        //No internet connection
        if(!DetectorInternetConnection.isConnected(getApplicationContext())){
            return MainActivity.NO_INTERNET_CONNECTION;
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

            isLookingUpInfo = false;
            Log.d("action_received",action);
            switch (action){
                case FixerTrackService.ACTION_COMPLETE_TASK:
                case FixerTrackService.ACTION_DONE_DETAILS:
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
                case API_INITIALIZED:
                    showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.api_initialized),ACTION_NONE,null);
                    break;
                case FixerTrackService.ACTION_NOT_FOUND:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.file_status_bad),ACTION_NONE, null);
                        }
                    });

                    break;
                default:
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
