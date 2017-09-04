package mx.dev.franco.musicallibraryorganizer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.gracenote.gnsdk.GnAudioFile;

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

import mx.dev.franco.musicallibraryorganizer.services.FixerTrackService;
import mx.dev.franco.musicallibraryorganizer.utilities.StringUtilities;
import wseemann.media.FFmpegMediaMetadataRetriever;

import static android.view.View.GONE;

/**
 * Created by franco on 22/07/17.
 */

public class DetailsTrackDialogActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    //flag to indicate that is just required to download
    //the coverart
    private boolean onlyCoverArt = false;
    //Id from audio item
    private long p;
    //flag when user is editing info
    private boolean editMode = false;
    //Reference to objects that make possible to edit metadata from mp3 audio files
    private FFmpegMediaMetadataRetriever fFmpegMediaMetadataRetriever = null;
    private MediaMetadataRetriever mediaMetadataRetriever = null;
    //A reference to database connection
    private DataTrackDbHelper dbHelper;
    //References to elements inside the layout
    private String newTitle;
    private String newArtist;
    private String newAlbum;
    private int newNumber;
    private int newYear;
    private String newGenre;
    private String trackPath;
    private EditText nameField;
    private String oldNameField;
    private EditText artistField;
    private String oldArtistField;
    private EditText albumField;
    private String oldAlbumField;
    private EditText numberField;
    private String oldNumberField;
    private EditText yearField;
    private String oldYearField;
    private EditText genreField;
    private String oldGenreField;
    private ImageButton imageButtonField;
    private byte[] oldCoverArt;
    private ContentValues newData;
    private View viewDetailsTrack;
    private FloatingActionButton editOrSaveButton;
    //private ImageButton cancelButton;
    private FloatingActionButton downloadCoverButton;
    private FloatingActionButton autofixButton;
    private FloatingActionButton playPreviewButton;
    private FloatingActionButton saveButton;
    private FloatingActionMenu floatingActionMenu;
    private TextView coverEditMessage;
    private TextView trackType;
    private TextView trackFullpath;
    private TextView titleLayer, subtitleLayer;
    private TextView imageSize;
    private TextView fileSize;
    private TextView trackLength;
    private TextView frequency;
    private TextView resolution;
    private TextView channels;
    private TextView status;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private ImageView toolbarCover;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private AppBarLayout appBarLayout;
    private ImageButton albumArtButton;
    private CardView cardView;

    //Reference to custom media player.
    private CustomMediaPlayer player;
    //listeners to retrieve values from edit text
    private TextListener titleListener, artistListener, albumListener,numberListener, yearListener, genreListener;
    private String resultMessage = "";
    //flag to set if data could be updated or not and inform to user
    private boolean dataUpdated = false;
    //reference to current audio item being edited
    private AudioItem currentAudioItem = null;
    //Object from GNDK that help us to retrieve some metadata like sample rate or frequency
    private GnAudioFile gnAudioFile;
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
    //static reference for saving the new cover art, if user select it
    private byte[] newAlbumArt = null;
    //Intent type
    private static int INTENT_OPEN_GALLERY = 1;
    //codes for determining the type of error when validating the fields
    private static final int HAS_EMPTY_FIELDS = 11;
    private static final int DATA_IS_TOO_LONG = 12;
    private static final int HAS_NOT_ALLOWED_CHARACTERS = 13;
    private static final int FILE_IS_PROCESSING = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set status bar tarnslucent, these calls to window object must be done before setContentView
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
        intentFilter.addAction(FixerTrackService.ACTION_DONE);
        receiver = new ResponseReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());



        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
        appBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
        viewDetailsTrack = findViewById(R.id.container);

        p = getIntent().getLongExtra("itemId",-1);
        currentAudioItem = SelectFolderActivity.getItemByIdOrPath(p,null);
        currentAudioItem.setContext(getApplicationContext());
        trackPath = currentAudioItem.getNewAbsolutePath();
        collapsingToolbarLayout.setTitleEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        dbHelper = DataTrackDbHelper.getInstance(getApplicationContext());
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        init();
    }


    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
            return;
        }

        if(floatingActionMenu.isOpened()){
            floatingActionMenu.close(true);
            return;
        }

        if (editMode) {
            setPreviousValues();
            disableFields();
            if (newAlbumArt != null) {
                newAlbumArt = null;
            }

            autofixButton.setEnabled(true);
            editOrSaveButton.setEnabled(true);
            downloadCoverButton.setEnabled(true);
            return;
        }
        if(onlyCoverArt){
            onlyCoverArt = false;
            cardView.setVisibility(GONE);
            disableFields();
            autofixButton.setEnabled(false);
            editOrSaveButton.setEnabled(false);
            downloadCoverButton.setEnabled(false);
            return;
        }

        dismiss();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("RESULT_ACTIVITY", requestCode + "");
        if (requestCode == INTENT_OPEN_GALLERY && data != null){
            Uri imageData = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageData);
                if(bitmap.getHeight() > 1080 || bitmap.getWidth() > 1080){
                    Toast toast = Toast.makeText(this, getString(R.string.image_too_big),Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                }
                else {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
                    newAlbumArt = byteArrayOutputStream.toByteArray();
                    GlideApp.with(viewDetailsTrack).
                            load(newAlbumArt)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                            .apply(RequestOptions.skipMemoryCacheOf(true))
                            .centerCrop()
                            .into(toolbarCover);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //Release the resources used by this objects when cancel the this activity.
        if(fFmpegMediaMetadataRetriever != null){
            fFmpegMediaMetadataRetriever.release();
            fFmpegMediaMetadataRetriever = null;
        }

        if(mediaMetadataRetriever != null){
            mediaMetadataRetriever.release();
            mediaMetadataRetriever = null;
        }
        dbHelper = null;
        nameField = null;
        artistField = null;
        albumField = null;
        numberField = null;
        yearField = null;
        genreField = null;
        //imageButtonField = null;
        oldCoverArt = null;
        newData = null;
        viewDetailsTrack = null;
        editOrSaveButton = null;
        //downloadCoverButton = null;
        playPreviewButton = null;
        coverEditMessage = null;
        player = null;
        titleListener = null;
        artistListener = null;
        albumListener = null;
        numberListener = null;
        yearListener = null;
        genreListener = null;
        progressBar = null;
        currentAudioItem = null;
        newAlbumArt = null;
        gnAudioFile = null;
        trackIdAudioItem = null;
        intentFilter = null;
        localBroadcastManager.unregisterReceiver(receiver);
        receiver = null;
        localBroadcastManager = null;
    }

    private void dismiss() {
        if(receiver.isOrderedBroadcast()) {
            receiver.abortBroadcast();
            receiver.clearAbortBroadcast();
        }
        currentAudioItem.setProcessing(false);
        FixerTrackService.cancelGnMusicIdFileProcessing();
        SelectFolderActivity.shouldContinue = false;
        Log.d("isProcessing",currentAudioItem.isProcessing()+"");
        currentAudioItem.clearContext();
        setResult(Activity.RESULT_CANCELED);
        finishAfterTransition();
        //System.gc();
    }

    private void init(){
        //We get the current instance of CustomMediaPlayer object
        player = CustomMediaPlayer.getInstance(this);
        player.setOnCompletionListener(this);
        setupFields();
    }

    /**
     * Here we create listeners for edit text objects
     */
    private void createEditTextListeners(){
        titleListener = new TextListener(nameField);
        artistListener = new TextListener(artistField);
        albumListener = new TextListener(albumField);
        numberListener = new TextListener(numberField);
        yearListener = new TextListener(yearField);
        genreListener = new TextListener(genreField);
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
        gnAudioFile = new GnAudioFile(new File(trackPath));
        gnAudioFile.sourceInit();

        oldCoverArt = currentAudioItem.getCoverArt();

        oldNameField = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE):"";

        oldArtistField = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST):"";

        oldAlbumField = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM):"";

        oldNumberField = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TRACK) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TRACK):"";

        oldYearField = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DATE) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DATE):"";

        oldGenreField = fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_GENRE) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_GENRE):"";

        float freq = ((float)gnAudioFile.samplesPerSecond()/1000f);
        String freqStr = freq + " Khz";
        frequency.setText(freqStr);

        long res = gnAudioFile.sampleSizeInBits();
        String resolutionStr = res + " bits";
        resolution.setText(resolutionStr);

        long cha = gnAudioFile.numberOfChannels();
        String chaStr = cha == 1 ? "Mono":(cha == 2 ? "Estéreo":"Surround");
        channels.setText(chaStr);
        gnAudioFile.sourceClose();


        setCurrentValues();
    }

    private void setCurrentValues(){

        if(oldCoverArt != null) {
            GlideApp.with(viewDetailsTrack).
                    load(oldCoverArt)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(RequestOptions.skipMemoryCacheOf(true))
                    .centerCrop()
                    .into(toolbarCover);

        }
        else {
            GlideApp.with(viewDetailsTrack).
                    load(null)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .apply(RequestOptions.skipMemoryCacheOf(true))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .placeholder(R.drawable.nocoverart)
                    .into(toolbarCover);

        }

        nameField.setText(oldNameField);
        artistField.setText(oldArtistField);
        albumField.setText(oldAlbumField);
        numberField.setText(oldNumberField);
        yearField.setText(oldYearField);
        genreField.setText(oldGenreField);
        trackType.setText("mp3"); //hardcoded because for now only mp3 is supported //trackType.setText(fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_AUDIO_CODEC) != null ? fFmpegMediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_AUDIO_CODEC):"");
        trackFullpath.setText(AudioItem.getRelativePath(trackPath));
        titleLayer.setText(currentAudioItem.getFileName());
        subtitleLayer.setText(AudioItem.getRelativePath(currentAudioItem.getPath()));
        imageSize.setText(currentAudioItem.getStringImageSize());
        fileSize.setText(currentAudioItem.getConvertedFileSize());
        trackLength.setText(currentAudioItem.getHumanReadableDuration());
        status.setText(currentAudioItem.getStatusText());
        status.setCompoundDrawablesWithIntrinsicBounds(currentAudioItem.getStatusDrawable(),null,null,null);
        addActionListeners();
    }

    private void setDownloadedValues(){
        if(trackIdAudioItem == null){
            Toast.makeText(DetailsTrackDialogActivity.this,getText(R.string.file_status_bad),Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("coverart",(trackIdAudioItem.getCoverArt() == null)+"");
        if(trackIdAudioItem.getCoverArt() != null) {
            GlideApp.with(viewDetailsTrack).
                    load(trackIdAudioItem.getCoverArt())
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .apply(RequestOptions.skipMemoryCacheOf(true))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(toolbarCover);

            newAlbumArt = trackIdAudioItem.getCoverArt();
        }
        nameField.setText(trackIdAudioItem.getTitle());
        artistField.setText(trackIdAudioItem.getArtist());
        albumField.setText(trackIdAudioItem.getAlbum());
        numberField.setText(trackIdAudioItem.getTrackNumber());
        yearField.setText(trackIdAudioItem.getTrackYear());
        genreField.setText(trackIdAudioItem.getGenre());
        Toast toast = Toast.makeText(DetailsTrackDialogActivity.this,"Se encontró la siguiente información. Toca el boton rojo para guardar.",Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
    }


    /**
     * This method create the references to edit text
     * and button elements in layout
     */
    private void setupFields(){

        progressBar = (ProgressBar) viewDetailsTrack.findViewById(R.id.progressSavingData);
        //imageButtonField = (ImageButton) viewDetailsTrack.findViewById(R.id.albumArt);
        nameField = (EditText)viewDetailsTrack.findViewById(R.id.track_name_details);
        artistField = (EditText)viewDetailsTrack.findViewById(R.id.artist_name_details);
        albumField = (EditText)viewDetailsTrack.findViewById(R.id.album_name_details);
        numberField = (EditText)viewDetailsTrack.findViewById(R.id.track_number);
        yearField = (EditText)viewDetailsTrack.findViewById(R.id.track_year);
        genreField = (EditText)viewDetailsTrack.findViewById(R.id.track_genre);
        trackType = (TextView)viewDetailsTrack.findViewById(R.id.track_type);
        trackFullpath = (TextView)viewDetailsTrack.findViewById(R.id.track_path);
        trackFullpath.setSelected(true);
        frequency = (TextView) viewDetailsTrack.findViewById(R.id.frequency);
        resolution = (TextView) viewDetailsTrack.findViewById(R.id.resolution);
        channels = (TextView) viewDetailsTrack.findViewById(R.id.channels);
        status = (TextView) viewDetailsTrack.findViewById(R.id.status);
        cardView = (CardView) viewDetailsTrack.findViewById(R.id.downloadedCoverArt);

        //cancelButton = (ImageButton) viewDetailsTrack.findViewById(R.id.cancel);

        coverEditMessage = (TextView) viewDetailsTrack.findViewById(R.id.coverEditMessage);

        downloadCoverButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.downloadCover);
        playPreviewButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.playPreview);
        editOrSaveButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.editTrackInfo);
        autofixButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.autoFix);
        floatingActionMenu = (FloatingActionMenu) viewDetailsTrack.findViewById(R.id.floatingActionMenu);
        floatingActionMenu.setClosedOnTouchOutside(true);

        saveButton = (FloatingActionButton) viewDetailsTrack.findViewById(R.id.saveInfo);


        toolbarCover = (ImageView) viewDetailsTrack.findViewById(R.id.toolbarCover);
        titleLayer = (TextView) viewDetailsTrack.findViewById(R.id.titleTransparentLayer);
        subtitleLayer = (TextView) viewDetailsTrack.findViewById(R.id.subtitleTransparentLayer);
        albumArtButton = (ImageButton) viewDetailsTrack.findViewById(R.id.albumArtLittle);
        imageSize = (TextView) viewDetailsTrack.findViewById(R.id.imageSize);
        fileSize = (TextView) viewDetailsTrack.findViewById(R.id.fileSize);
        trackLength = (TextView) viewDetailsTrack.findViewById(R.id.trackLength);
        Log.d("setupfields","true");
        extractAndCacheData();
    }

    private void addActionListeners(){
        playPreviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("press","pressed");
                try {
                    playPreview();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        editOrSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editInfoTrack();
            }
        });

        autofixButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                currentAudioItem.setProcessing(true);
                floatingActionMenu.close(true);
                autofixButton.setEnabled(false);
                editOrSaveButton.setEnabled(false);

                if(trackIdAudioItem == null) {
                    SelectFolderActivity.audioItemArrayAdapter.notifyItemChanged(currentAudioItem.getPosition());
                    Intent intent = new Intent(DetailsTrackDialogActivity.this, FixerTrackService.class);
                    intent.putExtra("singleTrack", true);
                    intent.putExtra("fromDetailsTrackDialog", true);
                    intent.putExtra("id", currentAudioItem.getId());
                    startService(intent);

                }
                else {
                    setDownloadedValues();
                    progressBar.setVisibility(View.GONE);
                    currentAudioItem.setProcessing(false);
                    autofixButton.setEnabled(true);
                    enableFieldsToEdit();
                    editMode = true;
                }
            }
        });

        downloadCoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadCover();
            }
        });

        floatingActionMenu.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                if(opened)
                    appBarLayout.setExpanded(false);
                else
                    appBarLayout.setExpanded(true);
            }
        });
        toolbarCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewCover();
            }
        });

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                toolbarCover.setAlpha(1.0f - Math.abs(verticalOffset/(float)appBarLayout.getTotalScrollRange()));
                if(Math.abs(verticalOffset)-appBarLayout.getTotalScrollRange() == 0) {
                    collapsingToolbarLayout.setTitleEnabled(true);
                    collapsingToolbarLayout.setTitle(currentAudioItem.getFileName());
                    getSupportActionBar().setDisplayShowTitleEnabled(true);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                }
                else {
                    collapsingToolbarLayout.setTitleEnabled(false);
                    getSupportActionBar().setDisplayShowTitleEnabled(false);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    getSupportActionBar().setDisplayShowHomeEnabled(false);
                }
            }
        });

        if(player != null && player.isPlaying() && player.getCurrentId() == this.p){
            playPreviewButton.setImageResource(R.drawable.ic_stop_white_24px);
            playPreviewButton.setLabelText("Detener");
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        createEditTextListeners();
    }

    /**
     * This method adds listeners to edit text objects
     * for retrieving the current values while user
     * is typing
     */
    private void addEditTextListeners(){
        nameField.addTextChangedListener(titleListener);
        artistField.addTextChangedListener(artistListener);
        albumField.addTextChangedListener(albumListener);
        numberField.addTextChangedListener(numberListener);
        yearField.addTextChangedListener(yearListener);
        genreField.addTextChangedListener(genreListener);
    }

    /**
     * Remove text listeners when edit text objects are disabled
     */
    private void removeEditTextListeners(){
        nameField.removeTextChangedListener(titleListener);
        artistField.removeTextChangedListener(artistListener);
        albumField.removeTextChangedListener(albumListener);
        numberField.removeTextChangedListener(numberListener);
        yearField.removeTextChangedListener(yearListener);
        genreField.removeTextChangedListener(genreListener);
    }

    /**
     * This method help us to play a preview of current song,
     * using the current instance of CustomMediaPlayer
     * @throws IOException
     * @throws InterruptedException
     */
    private void playPreview() throws IOException, InterruptedException {
        player.playPreview(p);
        if(player.isPlaying()){
            playPreviewButton.setImageResource(R.drawable.ic_stop_white_24px);
            playPreviewButton.setLabelText("Detener");
        }
        else {
            playPreviewButton.setImageResource(R.drawable.ic_play_arrow_white_24px);
            playPreviewButton.setLabelText("Escuchar");
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
            editMode = true;
        }
        else {
            disableFields();
            if(newAlbumArt != null){
                newAlbumArt = null;
            }
        }
    }

    /**
     * This method saves the initial values
     * when the activity is created,
     * in case the user cancel the edition of metadata
     * these data is used indicating that
     * the user did not modify anything
     */
    private void cachingPreviousValues(){
        oldCoverArt = currentAudioItem.getCoverArt();
        oldNameField = nameField.getText().toString().trim();
        oldArtistField = artistField.getText().toString().trim();
        oldAlbumField = albumField.getText().toString().trim();
        oldNumberField = numberField.getText().toString().trim();
        oldYearField = yearField.getText().toString().trim();
        oldGenreField = genreField.getText().toString().trim();
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
                if(StringUtilities.isFieldEmpty((EditText) fields.get(i))){
                    showWarning((EditText) fields.get(i), HAS_EMPTY_FIELDS);
                    isDataValid = false;
                    break;
                }
                if(StringUtilities.isTooLong((EditText) fields.get(i))){
                    showWarning((EditText) fields.get(i), DATA_IS_TOO_LONG);
                    isDataValid = false;
                    break;
                }

                switch (fields.get(i).getId()){
                    case R.id.track_name_details:
                        newTitle = StringUtilities.trimString((EditText) fields.get(i));
                        break;
                    case R.id.artist_name_details:
                        newArtist = StringUtilities.trimString((EditText) fields.get(i));
                        break;
                    case R.id.album_name_details:
                        newAlbum = StringUtilities.trimString((EditText) fields.get(i));
                        break;
                    case R.id.track_genre:
                        newGenre = StringUtilities.trimString((EditText) fields.get(i));
                        break;
                    case R.id.track_number:
                        newNumber = Integer.parseInt(StringUtilities.trimString((EditText) fields.get(i)));
                        break;
                    case R.id.track_year:
                        newYear = Integer.parseInt(StringUtilities.trimString((EditText) fields.get(i)));
                        break;
                }

                //at this point the data has been evaluated as true
                isDataValid = true;


                //If value of this option from app settings is true, run this code
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
                else if(StringUtilities.hasNotAllowedCharacters(((EditText) fields.get(i)))){//Here it goes the validation of setting "Eliminar automaticamente caracteres inválidos"
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
            editText.setAnimation(animation);
            editText.startAnimation(animation);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }

        Toast toast = Toast.makeText(getApplicationContext(),msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();

    }


    /**
     * This method updates metadata song.
     * @throws IOException
     * @throws ID3WriteException
     */
    private void updateData() throws IOException, ID3WriteException {
        //we update the data creating another thread because the database operation can take a long time
        AsyncUpdateData asyncUpdateData = new AsyncUpdateData();
        asyncUpdateData.execute();
    }

    /**
     * If user is in edit mode and cancel without modify the information,
     * then set the previous values, including the album cover
     */
    private void setPreviousValues(){
        nameField.setText(oldNameField);
        artistField.setText(oldArtistField);
        albumField.setText(oldAlbumField);
        numberField.setText(oldNumberField);
        yearField.setText(oldYearField);
        genreField.setText(oldGenreField);
        if(oldCoverArt != null) {
            GlideApp.with(viewDetailsTrack).
                    load(oldCoverArt)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .apply(RequestOptions.skipMemoryCacheOf(true))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(toolbarCover);
        }
        else{
            GlideApp.with(viewDetailsTrack).
                    load(null)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .apply(RequestOptions.skipMemoryCacheOf(true))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .placeholder(R.drawable.nocoverart)
                    .into(toolbarCover);
        }
    }


    /**
     * We put the values in a ContentValues object for writing
     * these to database
     */
    private void putValuesToRecord(){
        newData = new ContentValues();
        newData.put(TrackContract.TrackData.COLUMN_NAME_TITLE,newTitle);
        newData.put(TrackContract.TrackData.COLUMN_NAME_ARTIST,newArtist);
        newData.put(TrackContract.TrackData.COLUMN_NAME_ALBUM,newAlbum);
        newData.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_EDIT_BY_USER);
    }

    private void stopPlayback(){
        if(player != null && player.isPlaying() && p == player.getCurrentId()){
            player.stop();
            player.reset();
            CustomMediaPlayer.onCompletePlayback(p);
            playPreviewButton.setImageResource(R.drawable.ic_play_arrow_white_24px);
            playPreviewButton.setLabelText("Escuchar");
        }
    }

    /**
     * Enter to edit mode, for manually
     * modifying the information about the song
     */
    private void enableFieldsToEdit(){
        floatingActionMenu.close(true);
        appBarLayout.setExpanded(true);

        floatingActionMenu.hideMenu(true);
        saveButton.setVisibility(View.VISIBLE);
        nameField.setEnabled(true);
        artistField.setEnabled(true);
        albumField.setEnabled(true);
        numberField.setEnabled(true);
        yearField.setEnabled(true);
        genreField.setEnabled(true);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentAudioItem.isProcessing()){
                    showWarning(null, FILE_IS_PROCESSING);
                    return;
                }

                boolean validInputs = isDataValid();

                if (validInputs){
                    putValuesToRecord();
                    try {
                        updateData();
                    } catch (IOException | ID3WriteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        albumArtButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectorImageIntent = new Intent(Intent.ACTION_PICK);
                selectorImageIntent.setType("image/*");
                startActivityForResult(selectorImageIntent,INTENT_OPEN_GALLERY);
            }
        });
        imageSize.setText("Editar Carátula");
        coverEditMessage.setVisibility(View.VISIBLE);
        editOrSaveButton.setOnClickListener(null);
        editOrSaveButton.setImageResource(R.drawable.ic_save_white_24px);
        editOrSaveButton.setLabelText("Guardar");
        editOrSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(currentAudioItem.isProcessing()){
                    showWarning(null, FILE_IS_PROCESSING);
                    return;
                }

                boolean validInputs = isDataValid();

                if (validInputs){
                    putValuesToRecord();
                    try {
                        updateData();
                    } catch (IOException | ID3WriteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        InputMethodManager imm =(InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(nameField,InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * This method disable the fields and
     * leaves from edit mode
     */

    private void disableFields(){
        nameField.setEnabled(false);
        artistField.setEnabled(false);
        albumField.setEnabled(false);
        numberField.setEnabled(false);
        yearField.setEnabled(false);
        genreField.setEnabled(false);
        removeEditTextListeners();
        floatingActionMenu.showMenu(true);
        saveButton.setVisibility(GONE);
        saveButton.setOnClickListener(null);
        albumArtButton.setOnClickListener(null);
        imageSize.setText(currentAudioItem.getStringImageSize());
        coverEditMessage.setVisibility(GONE);
        editOrSaveButton.setOnClickListener(null);
        editOrSaveButton.setImageResource(R.drawable.ic_edit_white_24px);
        editOrSaveButton.setLabelText("Editar");
        editOrSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEditTextListeners();
                editInfoTrack();
            }
        });

        editMode = false;

   }

    /**
     * This method helps to rename the file,
     * in case is set from options in settings activity
     * @param path
     * @return String[]
     */
    private String[] renameFile(String path){

        boolean couldBeRenamed = false;
        String[] paths = new String[2];
        File currentFile = new File(path), renamedFile;
        String newPath = currentFile.getParent();
        String newFilename = newTitle + ".mp3";
        String newCompleteFilename= newPath + "/" + newFilename;
        renamedFile = new File(newCompleteFilename);
        if(!renamedFile.exists()) {
            couldBeRenamed = currentFile.renameTo(renamedFile);
        }else {
            newFilename = newTitle +"("+ (int)Math.floor((Math.random()*10)+ 1) +")"+".mp3";
            newCompleteFilename = newPath + "/" + newFilename;
            renamedFile = new File(newCompleteFilename);
            couldBeRenamed = currentFile.renameTo(renamedFile);
        }

        Log.d("MANUAL_RENAMED",couldBeRenamed+"");
        paths[0] = renamedFile.getAbsolutePath();
        paths[1] = renamedFile.getParent();
        return paths;
    }

    private void viewCover(){
        //Intent intent = new Intent();
        //TransitionManager.beginDelayedTransition((ViewGroup) viewDetailsTrack);


        //Bundle bundle = new Bundle();
        ///bundle.putLong("id",currentAudioItem.getId());
        //imageViewer.setArguments(bundle);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        //    imageViewer.setSharedElementEnterTransition(new DetailsTransition());
        //    imageViewer.setSharedElementReturnTransition(new DetailsTransition());
        //}

        //ViewCompat.setTransitionName(toolbarCover, "transition_"+currentAudioItem.getId());

        /*getSupportFragmentManager()
                .beginTransaction()
                .addSharedElement(toolbarCover, "transitionFragment")
                .replace(R.id.containerFragment,imageViewer)
                .addToBackStack("imageViewer")
                .commit();*/
    }

    /**
     * This method download the cover art only
     */
    private void downloadCover(){
        progressBar.setVisibility(View.VISIBLE);
        currentAudioItem.setProcessing(true);
        floatingActionMenu.close(true);

        autofixButton.setEnabled(false);
        editOrSaveButton.setEnabled(false);
        downloadCoverButton.setEnabled(false);

        if(trackIdAudioItem != null){
            handleCoverArt();
        }
        else{
            onlyCoverArt = true;
            Intent intent = new Intent(this,FixerTrackService.class);
            intent.putExtra("fromDetailsTrackDialog",true);
            intent.putExtra("singleTrack",true);
            intent.putExtra("id", currentAudioItem.getId());
            startService(intent);
        }





        /*Toast toast = Toast.makeText(getApplicationContext(),getString(R.string.snackbar_message_in_development),Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();*/
    }

    /**
     * We implement this method for handling
     * correctly in case the song playback be completed
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("onCompletePlayback","OnFragmnet");
        CustomMediaPlayer.onCompletePlayback(this.p);
        try {
            playPreviewButton.setImageResource(R.drawable.ic_play_arrow_white_24px);
            playPreviewButton.setLabelText("Escuchar");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private class TextListener implements TextWatcher {
        private EditText field;


        TextListener(EditText field){
            this.field = field;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            switch(field.getId()){
                case R.id.track_name_details:
                    newTitle = s.toString();
                    Log.d("newTitle",newTitle);
                    break;
                case R.id.artist_name_details:
                    newArtist = s.toString();
                    Log.d("newArtist",newArtist);
                    break;
                case R.id.album_name_details:
                    newAlbum = s.toString();
                    Log.d("newAlbum",newAlbum);
                    break;
                case R.id.track_genre:
                    newGenre = s.toString();
                    Log.d("newGenre",newGenre);
                    break;
                case R.id.track_number:
                    try {
                        newNumber = Integer.parseInt(s.toString());
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    finally {
                        Log.d("newNumber",newNumber+"");
                    }

                    break;
                case R.id.track_year:
                    try {
                        newYear = Integer.parseInt(s.toString());
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    finally {
                        Log.d("newYear",newYear+"");
                    }

                    break;
            }

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }


    private class AsyncUpdateData extends AsyncTask<Void, Void, Void> {
        private final String TAG = AsyncUpdateData.class.getName();
        @Override
        protected void onPreExecute(){
            progressBar.setVisibility(View.VISIBLE);
            stopPlayback();
            playPreviewButton.setEnabled(false);
            disableFields();
        }

        private void updateCoverArt() {
            MyID3 myID3 = new MyID3();
            File tempFile = new File(trackPath);
            MusicMetadataSet musicMetadataSet = null;
            try {
                musicMetadataSet = myID3.read(tempFile);
                MusicMetadata iMusicMetadata = (MusicMetadata) musicMetadataSet.getSimplified();
                Log.d(TAG,"newAlbumArt: "+ (newAlbumArt != null));
                if (newAlbumArt != null) {
                    Vector<ImageData> imageDataVector = new Vector<>();
                    ImageData imageData = new ImageData(newAlbumArt, "", "", 3);
                    imageDataVector.add(imageData);
                    iMusicMetadata.setPictureList(imageDataVector);
                    newData.put(TrackContract.TrackData.COLUMN_NAME_COVER_ART, newAlbumArt);

                    //update filesize and coverart property because the new embed cover
                    float fileSizeInMb = (float) tempFile.length() / AudioItem.KILOBYTE;
                    currentAudioItem.setSize(fileSizeInMb);
                    currentAudioItem.setCoverArt(newAlbumArt);
                    newData.put(TrackContract.TrackData.COLUMN_NAME_FILE_SIZE, fileSizeInMb);
                    myID3.update(tempFile, musicMetadataSet, iMusicMetadata);
                    dbHelper.updateData(DetailsTrackDialogActivity.this.p,newData);
                    dataUpdated = true;
                }

            }
             catch (IOException | ID3WriteException e) {
                e.printStackTrace();
                 dataUpdated = false;
            }
        }

        private void updateMetadata(){
            //Is priority update the metadata first, in case there are errors when
            //the data is set on item and database
            MyID3 myID3 = new MyID3();
            File tempFile = new File(trackPath);
            MusicMetadataSet musicMetadataSet = null;
            try {
                musicMetadataSet = myID3.read(tempFile);
                MusicMetadata iMusicMetadata = (MusicMetadata) musicMetadataSet.getSimplified();
                iMusicMetadata.setSongTitle(newTitle);
                iMusicMetadata.setArtist(newArtist);
                iMusicMetadata.setAlbum(newAlbum);
                iMusicMetadata.setTrackNumber(newNumber);
                iMusicMetadata.setYear(newYear+"");
                iMusicMetadata.setGenre(newGenre);

                if(newAlbumArt != null){
                    Vector<ImageData> imageDataVector = new Vector<>();
                    ImageData imageData = new ImageData(newAlbumArt, "", "", 3);
                    imageDataVector.add(imageData);
                    iMusicMetadata.setPictureList(imageDataVector);
                    newData.put(TrackContract.TrackData.COLUMN_NAME_COVER_ART,newAlbumArt);
                    currentAudioItem.setCoverArt(newAlbumArt);

                    //update filesize and coverart property because the new embed cover
                    float fileSizeInMb = (float)tempFile.length() / AudioItem.KILOBYTE;
                    currentAudioItem.setSize(fileSizeInMb);
                    currentAudioItem.setCoverArt(newAlbumArt);
                    newData.put(TrackContract.TrackData.COLUMN_NAME_FILE_SIZE,fileSizeInMb);

                }

                try {
                    //Here we update the data
                    myID3.update(tempFile, musicMetadataSet, iMusicMetadata);

                    //Update the data of item from list

                    currentAudioItem.setTitle(newTitle)
                            .setArtist(newArtist)
                            .setAlbum(newAlbum)
                            .setGenre(newGenre)
                            .setStatus(AudioItem.FILE_STATUS_EDIT_BY_USER);



                    //We check if this option is selected in settings,
                    //before writing to database
                    Log.d("EQUALS", oldNameField.equals(newTitle)+"");
                    if(SelectedOptions.MANUAL_CHANGE_FILE){
                        //new title is not the same old title?
                        //if(!oldNameField.equals(newTitle)) {
                        String[] paths = renameFile(currentAudioItem.getNewAbsolutePath());
                        currentAudioItem.setNewAbsolutePath(paths[0]);
                        currentAudioItem.setPath(paths[1]);
                        currentAudioItem.setFileName(newTitle+".mp3");
                        newData.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_PATH, paths[1]);
                        newData.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME, newTitle+".mp3");
                        newData.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH, paths[0]);
                        trackPath = paths[0];
                        //}


                        //Finally, we need to update the file in media store database,
                        //in case we reload the list, it can take the file path changes
                        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{tempFile.getAbsolutePath(), new File(trackPath).getAbsolutePath()},
                                null, null);
                    }


                    //Then, is necessary update the data in database,
                    //because we obtain this info when the app starts (after first time),
                    //besides, database operations can take a long time
                    newData.put(TrackContract.TrackData.COLUMN_NAME_STATUS,AudioItem.FILE_STATUS_EDIT_BY_USER);
                    dbHelper.updateData(DetailsTrackDialogActivity.this.p,newData);
                    dataUpdated = true;




                } catch (IOException | ID3WriteException e) {
                    dataUpdated = false;
                    e.printStackTrace();
                    resultMessage = e.getMessage();
                }


            } catch (IOException e) {
                e.printStackTrace();
                dataUpdated = false;
                e.printStackTrace();
                resultMessage = e.getMessage();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            if(onlyCoverArt)
                updateCoverArt();
            else
                updateMetadata();

            return null;
        }
        @Override
        protected void onPostExecute(Void result){
            if(onlyCoverArt)
                doAfterUpdate();
            else
                doAtFinal();

            autofixButton.setEnabled(true);
            editOrSaveButton.setEnabled(true);
            downloadCoverButton.setEnabled(true);
        }
        @Override
        protected void onCancelled(){
            doAtFinal();
        }

    }

    private void doAfterUpdate(){
        playPreviewButton.setEnabled(true);
        SelectFolderActivity.audioItemArrayAdapter.notifyItemChanged(currentAudioItem.getPosition());
        fileSize.setText(currentAudioItem.getConvertedFileSize());
        imageSize.setText(currentAudioItem.getStringImageSize());
        progressBar.setVisibility(GONE);
        cardView.setVisibility(GONE);
        appBarLayout.setExpanded(true);
        Toast toast;
        if(dataUpdated){
            toast = Toast.makeText(getApplicationContext(), getString(R.string.message_data_update), Toast.LENGTH_SHORT);
        }
        else {
            toast = Toast.makeText(getApplicationContext(), getString(R.string.message_no_data_updated) + ": " +resultMessage, Toast.LENGTH_SHORT);
        }
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();

        newAlbumArt = null;
        onlyCoverArt = false;
    }

    private void doAtFinal(){
        ((TextView)viewDetailsTrack.findViewById(R.id.track_path)).setText(trackPath);
        playPreviewButton.setEnabled(true);
        SelectFolderActivity.audioItemArrayAdapter.notifyItemChanged(SelectFolderActivity.getItemByIdOrPath(this.p,"").getPosition());
        Toast toast;
        if(dataUpdated){
            toast = Toast.makeText(getApplicationContext(), getString(R.string.message_data_update), Toast.LENGTH_SHORT);
        }
        else {
            toast = Toast.makeText(getApplicationContext(), getString(R.string.message_no_data_updated) + ": " +resultMessage, Toast.LENGTH_SHORT);
        }
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();

        //Update fields in casewe have replaced the strange chars
        titleLayer.setText(currentAudioItem.getFileName());
        status.setText(currentAudioItem.getStatusText());
        nameField.setText(currentAudioItem.getTitle());
        artistField.setText(currentAudioItem.getArtist());
        albumField.setText(currentAudioItem.getAlbum());
        genreField.setText(currentAudioItem.getGenre());
        status.setText(currentAudioItem.getStatusText());
        status.setCompoundDrawablesWithIntrinsicBounds(currentAudioItem.getStatusDrawable(),null,null,null);


        if(newAlbumArt != null) {
            fileSize.setText(currentAudioItem.getConvertedFileSize());
            imageSize.setText(currentAudioItem.getStringImageSize());
            newAlbumArt = null;
        }


        cachingPreviousValues();
        progressBar.setVisibility(GONE);
        appBarLayout.setExpanded(true);

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
    }

    private class ResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action;
            action = intent.getAction();
            Log.d("action_received",action);
            switch (action){
                case FixerTrackService.ACTION_DONE:
                    DetailsTrackDialogActivity.this.trackIdAudioItem = intent.getParcelableExtra("audioItem");
                    if (onlyCoverArt) {
                        DetailsTrackDialogActivity.this.handleCoverArt();
                    }
                    else {
                        DetailsTrackDialogActivity.this.enableFieldsToEdit();
                        DetailsTrackDialogActivity.this.setDownloadedValues();
                        DetailsTrackDialogActivity.this.editMode = true;
                        DetailsTrackDialogActivity.this.appBarLayout.setExpanded(true);
                    }
                    break;
                case FixerTrackService.ACTION_CANCEL:
                case FixerTrackService.ACTION_COMPLETE_TASK:
                    break;
                case FixerTrackService.ACTION_FAIL:
                    Toast toast = Toast.makeText(DetailsTrackDialogActivity.this,getText(R.string.file_status_bad),Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                    break;
            }
            autofixButton.setEnabled(true);
            editOrSaveButton.setEnabled(true);
            DetailsTrackDialogActivity.this.progressBar.setVisibility(GONE);
            DetailsTrackDialogActivity.this.autofixButton.setEnabled(true);



        }
    }

    private void handleCoverArt() {
        byte[] cover = trackIdAudioItem.getCoverArt();
        Toast t = Toast.makeText(getApplicationContext(), "",Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER,0,0);
        if(cover != null){
            t.setText(getString(R.string.cover_art_found));
            cardView.setVisibility(View.VISIBLE);
            floatingActionMenu.hideMenu(true);
            floatingActionMenu.close(true);

            appBarLayout.setExpanded(false);

            saveButton.setVisibility(View.VISIBLE);

            GlideApp.with(viewDetailsTrack).
                    load(cover)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(RequestOptions.skipMemoryCacheOf(true))
                    .centerCrop()
                    .into((ImageView) findViewById(R.id.imageViewDownloadedCover));

            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DetailsTrackDialogActivity.this);
                    builder.setTitle(getString(R.string.title_downloaded_cover_art_dialog));
                    builder.setNegativeButton(getString(R.string.as_cover_art), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AsyncUpdateData asyncUpdateData = new AsyncUpdateData();
                            asyncUpdateData.execute();
                        }
                    });
                    builder.setPositiveButton(getString(R.string.as_file), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.setMessage(R.string.description_downloaded_cover_art_dialog);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setCancelable(true);
                    alertDialog.show();
                }
            });
        }
        else{
            t.setText(getString(R.string.no_cover_art_found));
            onlyCoverArt = false;
        }
        progressBar.setVisibility(GONE);
        t.show();
    }

}
