package mx.dev.franco.musicallibraryorganizer;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.ImageData;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * Created by franco on 2/04/17.
 */

public class DetailsTrackDialog extends DialogFragment implements MediaPlayer.OnCompletionListener{
    //For debugging purposes only
    static String FRAGMENT_NAME = DetailsTrackDialog.class.getName();
    //Id from audio item
    private long p;
    private boolean editMode = false;
    //A reference to list of audio items
    private ArrayAdapter<AudioItem> filesAdapter;
    //Reference to objects whos make possible the edit of metadata from audio files
    private FFmpegMediaMetadataRetriever mediaMetadataRetriever2 = null;
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
    private Bitmap oldImageBitmap;
    private ContentValues newData;
    private View viewDetailsTrack;
    private Button editOrSaveButton;
    private Button cancelButton;
    private Button downloadCoverButton;
    private Button playPreviewButton;
    private TextView coverEditMessage;
    private CustomMediaPlayer player;
    private TextListener titleListener, artistListener, albumListener,numberListener, yearListener, genreListener;
    private String resultMessage = "";
    private boolean dataUpdated = false;

    //Flag for saving the result of validating the fields of layout
    private boolean isDataValid = false;
    //static reference for saving the new cover art, if user select it
    static byte[] newAlbumArt = null;
    //static code for determining the intent type
    static int INTENT_OPEN_GALLERY = 1;
    //codes for determining the type of error when validating the fields
    private static final int HAS_EMPTY_FIELDS = 11;
    private static final int DATA_IS_TOO_LONG = 12;
    private static final int HAS_NOT_ALLOWED_CHARACTERS = 13;
    private static final int FILE_IS_PROCESSING = 14;

    public DetailsTrackDialog(){
    }

    /**
     * This method sets the necessary references
     * to audio file selected from its caller activity
     * @param path absolute path to audio file
     * @param filesAdapter reference to list of audio items
     * @param p the id from the audio file
     * @param dbHelper a reference to the database connection
     */
    public void setData(String path, ArrayAdapter<AudioItem> filesAdapter, long p, DataTrackDbHelper dbHelper){
        this.trackPath = path;
        this.filesAdapter = filesAdapter;
        this.p = p;
        this.dbHelper = dbHelper;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        Log.d("ON_CREATE","called on create");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        viewDetailsTrack = inflater.inflate(R.layout.details_track_layout,null,false);
        builder.setView(viewDetailsTrack);

        //We get the current instance of CustomMediaPlayer object
        player = CustomMediaPlayer.getInstance(getActivity().getApplicationContext());
        player.setOnCompletionListener(this);

        extractAndCacheData();

        setupFields();

        createListeners();

        addListeners();

        return builder.create();
    }

    @Override
    public void onStart(){
        super.onStart();

        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public void onCancel(DialogInterface dialogInterface){
        //Release the resources used by this objects when cancel the fragment dialog.
        if(mediaMetadataRetriever2 != null){
            mediaMetadataRetriever2.release();
            mediaMetadataRetriever2 = null;
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
        imageButtonField = null;
        oldImageBitmap = null;
        newData = null;
        viewDetailsTrack = null;
        editOrSaveButton = null;
        cancelButton = null;
        downloadCoverButton = null;
        playPreviewButton = null;
        coverEditMessage = null;
        player = null;
        titleListener = null;
        artistListener = null;
        albumListener = null;
        numberListener = null;
        yearListener = null;
        genreListener = null;

        System.gc();
    }


    /**
     * Here we create listeners for edit text objects
     */
    private void createListeners(){
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
        mediaMetadataRetriever2 = new FFmpegMediaMetadataRetriever();
        mediaMetadataRetriever2.setDataSource(trackPath);
        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(trackPath);
        if(mediaMetadataRetriever.getEmbeddedPicture() != null) {
            ((ImageButton) viewDetailsTrack.findViewById(R.id.albumArt)).setImageBitmap(BitmapFactory.decodeByteArray(mediaMetadataRetriever.getEmbeddedPicture(), 0, mediaMetadataRetriever.getEmbeddedPicture().length));
        }
        oldImageBitmap = ((BitmapDrawable)((ImageButton)viewDetailsTrack.findViewById(R.id.albumArt)).getDrawable()).getBitmap();

        oldNameField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE):"";

        oldArtistField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST):"";

        oldAlbumField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM):"";

        oldNumberField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TRACK) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TRACK):"";

        oldYearField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DATE) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DATE):"";

        oldGenreField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_GENRE) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_GENRE):"";

        MediaExtractor mex = new MediaExtractor();
        int sampleRate = 0;
        int channelCount = 0;
        try {
            mex.setDataSource(trackPath);// the address of the file on storage

            MediaFormat mf = mex.getTrackFormat(0);

            sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            channelCount = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        } catch (IOException e) {
            sampleRate = 0;
            channelCount = 0;
            e.printStackTrace();
        }

        ((TextView)viewDetailsTrack.findViewById(R.id.track_type)).setText(mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_AUDIO_CODEC) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_AUDIO_CODEC):"");
        ((TextView)viewDetailsTrack.findViewById(R.id.track_duration_details)).setText(AudioItem.getHumanReadableDuration(Integer.parseInt(mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION))));
        ((TextView)viewDetailsTrack.findViewById(R.id.track_sampling_rate)).setText(String.valueOf(sampleRate == 0?sampleRate:sampleRate/1000f));
        ((TextView)viewDetailsTrack.findViewById(R.id.track_channels)).setText(AudioItem.getChannelsMode(channelCount+""));
        ((TextView)viewDetailsTrack.findViewById(R.id.track_speed)).setText(AudioItem.convertSpeed(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)));
        ((TextView)viewDetailsTrack.findViewById(R.id.track_size)).setText(AudioItem.convertSpeed(String.valueOf(new File(trackPath).length())));
        ((TextView)viewDetailsTrack.findViewById(R.id.track_path)).setText(trackPath);
        viewDetailsTrack.findViewById(R.id.track_path).setSelected(true);

    }

    /**
     * This method create the references to edit text
     * and button elements in layout
     */
    private void setupFields(){
        nameField = (EditText)viewDetailsTrack.findViewById(R.id.track_name_details);
        nameField.setText(oldNameField);

        artistField = (EditText)viewDetailsTrack.findViewById(R.id.artist_name_details);
        artistField.setText(oldArtistField);

        albumField = (EditText)viewDetailsTrack.findViewById(R.id.album_name_details);
        albumField.setText(oldAlbumField);

        numberField = (EditText)viewDetailsTrack.findViewById(R.id.track_number);
        numberField.setText(oldNumberField);

        yearField = (EditText)viewDetailsTrack.findViewById(R.id.track_year);
        yearField.setText(oldYearField);

        genreField = (EditText)viewDetailsTrack.findViewById(R.id.track_genre);
        genreField.setText(oldGenreField);


        imageButtonField = (ImageButton) viewDetailsTrack.findViewById(R.id.albumArt);
        playPreviewButton = (Button) viewDetailsTrack.findViewById(R.id.playPreview);

        downloadCoverButton = (Button) viewDetailsTrack.findViewById(R.id.downloadCover);
        cancelButton = (Button) viewDetailsTrack.findViewById(R.id.cancel);
        cancelButton.setText("Cerrar");
        cancelButton.setEnabled(true);
        coverEditMessage = (TextView) viewDetailsTrack.findViewById(R.id.coverEditMessage);
        editOrSaveButton = (Button) viewDetailsTrack.findViewById(R.id.editTrackInfo);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
            }
        });

        //In case we open the fragment dialog for current audio item playing
        //change the background for indicate stop playback
        if(player.isPlaying() && p == player.getCurrentId()){
            playPreviewButton.setBackground(getResources().getDrawable(R.drawable.ic_stop_black_24dp,null));
        }

        playPreviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    }

    /**
     * This method adds listeners to edit text objects
     * for retrieving the current values while user
     * is typing
     */
    private void addListeners(){
        nameField.addTextChangedListener(titleListener);
        artistField.addTextChangedListener(artistListener);
        albumField.addTextChangedListener(albumListener);
        numberField.addTextChangedListener(numberListener);
        yearField.addTextChangedListener(yearListener);
        genreField.addTextChangedListener(genreListener);
    }

    /**
     * Remove listeners when edit text objects are disabled
     */
    private void removeListeners(){
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
            playPreviewButton.setBackground(getResources().getDrawable(R.drawable.ic_stop_black_24dp,null));
        }
        else {
            playPreviewButton.setBackground(getResources().getDrawable(R.drawable.ic_play_circle_filled_black_24dp,null));
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
        }
    }

    /**
     * This method saves the initial values
     * when the fragment dialog is created
     * in case the user cancel the edition of metadata,
     * these data is used indicating that
     * the user did not modify anything
     */
    private void cachingPreviousValues(){
        oldImageBitmap = ((BitmapDrawable)((ImageButton)viewDetailsTrack.findViewById(R.id.albumArt)).getDrawable()).getBitmap();
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
        //Get all descendants of this view;
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
            Animation animation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.blink);
            editText.requestFocus();
            editText.setAnimation(animation);
            editText.startAnimation(animation);
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }

        Toast toast = Toast.makeText(getActivity(),msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();

    }


    /**
     * This method updates metadata song.
     * @throws IOException
     * @throws ID3WriteException
     */
    private void updateData() throws IOException, ID3WriteException {
        DetailsTrackDialog.this.getDialog().findViewById(R.id.progressSavingData).setVisibility(View.VISIBLE);
        //we update the data creating another thread because the database operation can take a long time
        new Thread(new Runnable() {
            @Override
            public void run() {
                DetailsTrackDialog.this.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopPlayback();
                    }
                });

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
                        newAlbumArt = null;
                    }

                    try {
                        //Here we update the data
                        myID3.update(tempFile, musicMetadataSet, iMusicMetadata);

                        //Then we set the data on the item, because it needs to update the info
                        //shown to user
                        AudioItem audioItem = SelectFolderActivity.selectItemByIdOrPath(p,"");
                        //Update the data of item from list
                        audioItem.setTitle(newTitle).setArtist(newArtist).setAlbum(newAlbum).setStatus(AudioItem.FILE_STATUS_EDIT_BY_USER);


                        //We check if this option is selected in settings,
                        //before writing to database
                        Log.d("EQUALS", oldNameField.equals(newTitle)+"");
                        if(SelectedOptions.MANUAL_CHANGE_FILE && (!oldNameField.equals(newTitle))){
                            String[] paths = renameFile(audioItem.getNewAbsolutePath());
                            audioItem.setNewAbsolutePath(paths[0]);
                            newData.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_PATH, paths[1]);
                            trackPath = paths[0];
                        }

                        //Last, is necessary update the data in database,
                        //because we obtain this info when the app starts (after first time),
                        //besides, database operations can take a long time
                        DetailsTrackDialog.this.dbHelper.updateData(DetailsTrackDialog.this.p,newData);
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

                //Finally, if data has been updated or not
                //we need to update the fragment dialog UI.
                finally {
                    DetailsTrackDialog.this.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            filesAdapter.notifyDataSetChanged();
                            ((TextView)viewDetailsTrack.findViewById(R.id.track_path)).setText(trackPath);
                            playPreviewButton.setEnabled(true);
                            Toast toast;
                            if(dataUpdated){
                                toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.message_data_update), Toast.LENGTH_SHORT);
                            }
                            else {
                                toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.message_no_data_updated) + ": " +resultMessage, Toast.LENGTH_SHORT);
                            }
                            toast.setGravity(Gravity.CENTER,0,0);
                            toast.show();

                            disableFields();
                            cachingPreviousValues();

                            DetailsTrackDialog.this.getDialog().findViewById(R.id.progressSavingData).setVisibility(View.GONE);
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * If we are in edit mode, then we cancel without modify the information,
     *set the previous values, including the album cover
     */
    private void setPreviousValues(){
        nameField.setText(oldNameField);
        artistField.setText(oldArtistField);
        albumField.setText(oldAlbumField);
        numberField.setText(oldNumberField);
        yearField.setText(oldYearField);
        genreField.setText(oldGenreField);
        imageButtonField.setImageBitmap(oldImageBitmap);
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
        if(player.isPlaying() && p == player.getCurrentId()){
            player.stop();
            player.reset();
            playPreviewButton.setBackground(getResources().getDrawable(R.drawable.ic_play_circle_filled_black_24dp,null));
            CustomMediaPlayer.onCompletePlayback(p);
        }
    }

    /**
     * Enter to edit mode, for manually
     * modifying the information about the song
     */
    private void enableFieldsToEdit(){
        stopPlayback();

        nameField.setEnabled(true);
        artistField.setEnabled(true);
        albumField.setEnabled(true);
        numberField.setEnabled(true);
        yearField.setEnabled(true);
        genreField.setEnabled(true);
        imageButtonField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectorImageIntent = new Intent(Intent.ACTION_PICK);
                selectorImageIntent.setType("image/*");
                getActivity().startActivityForResult(selectorImageIntent,INTENT_OPEN_GALLERY);
            }
        });
        coverEditMessage.setAlpha(1);
        cancelButton.setEnabled(true);
        cancelButton.setOnClickListener(null);
        cancelButton.setText("Cancelar");
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeListeners();
                //cancelButton.setOnClickListener(null);
                setPreviousValues();
                disableFields();
            }
        });
        editOrSaveButton.setOnClickListener(null);
        editOrSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioItem audioItem = SelectFolderActivity.selectItemByIdOrPath(p,"");

                if(audioItem.isProcessing()){
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
        downloadCoverButton.setEnabled(true);
        downloadCoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadCover();
            }
        });
        editOrSaveButton.setText(getText(R.string.save_info_track));

        InputMethodManager imm =(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(nameField,InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * This method disable the fields and
     * take us out from edit mode
     */

    private void disableFields(){
        nameField.setEnabled(false);
        artistField.setEnabled(false);
        albumField.setEnabled(false);
        numberField.setEnabled(false);
        yearField.setEnabled(false);
        genreField.setEnabled(false);
        imageButtonField.setOnClickListener(null);
        coverEditMessage.setAlpha(0);
        editOrSaveButton.setOnClickListener(null);
        editOrSaveButton.setText(getText(R.string.edit_info_track));
        editOrSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addListeners();
                editInfoTrack();
            }
        });
        cancelButton.setText("Cerrar");
        cancelButton.setOnClickListener(null);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
            }
        });
        downloadCoverButton.setOnClickListener(null);
        downloadCoverButton.setEnabled(false);
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

    /**
     * This method download the cover art only
     */
    private void downloadCover(){
        Toast toast = Toast.makeText(getActivity().getApplicationContext(),getString(R.string.snackbar_message_in_development),Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
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
        if(player != null && player.getCurrentId() == this.p && getActivity() != null){
            playPreviewButton.setBackground(getResources().getDrawable(R.drawable.ic_play_circle_filled_black_24dp,null));
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
}
